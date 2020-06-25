/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.optimizer;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class DynamicRuntime {
    @SuppressWarnings("unused")
    public static CallSite bootstrapGetObjectProp(MethodHandles.Lookup lookup,
        String name, MethodType mType, String propertyName, int warn)
          throws NoSuchMethodException, IllegalAccessException {

        GetObjectPropertySite site = new GetObjectPropertySite(propertyName, warn != 0, mType);

        // The first method called is the "initialize" method, bound to the
        // call site itself so it can change its handle.
        MethodHandle initMethod = lookup.findVirtual(GetObjectPropertySite.class,
            "initialize", mType).bindTo(site);
        //MethodHandle init = MethodHandles.insertArguments(initMethod, 0, site);
        site.setTarget(initMethod);

        // The generic handle to look up the property directly if the guard is true
        MethodHandle lookupDirect = lookup.findVirtual(GetObjectPropertySite.class,
            "lookupDirect",
            MethodType.methodType(Object.class, Integer.TYPE,
                Object.class, Context.class, Scriptable.class)).bindTo(site);
        site.direct = lookupDirect;

        // The initialize method replaces itself with the fallback if necessary
        if (warn != 0) {
            MethodHandle getObjectProp =
                lookup.findStatic(ScriptRuntime.class, "getObjectProp",
                    MethodType.methodType(Object.class, Object.class, String.class,
                        Context.class, Scriptable.class));
            site.fallback = MethodHandles.insertArguments(getObjectProp, 1, propertyName);
        } else {
            MethodHandle getObjectProp =
                lookup.findStatic(ScriptRuntime.class, "getObjectPropNoWarn",
                    MethodType.methodType(Object.class, Object.class, String.class,
                        Context.class, Scriptable.class));
            site.fallback = MethodHandles.insertArguments(getObjectProp, 1, propertyName);
        }

        return site;
    }

    public static final class GetObjectPropertySite
        extends MutableCallSite
    {
        private static final int MAX_MISSES = 16;

        final String propertyName;
        final boolean warn;

        int misses = 0;
        MethodHandle fallback;
        MethodHandle direct;

        GetObjectPropertySite(String propertyName, boolean warn, MethodType mType) {
            super(mType);
            this.propertyName = propertyName;
            this.warn = warn;
        }

        /**
         * This is called first. If the target meets the requirements, it switches to
         * a direct invocation, and otherwise falls back to the generic method.
         */
        public Object initialize(Object obj,
            Context cx, Scriptable scope) {
            if (obj instanceof ScriptableObject) {
                ScriptableObject so = (ScriptableObject)obj;
                int fastIx = so.getFastIndex(propertyName);
                if (fastIx >= 0) {
                    switchToFastHandles(fastIx);
                    return so.getFastValue(fastIx);
                }
            }

            // Give up permanently after the first try and just use the same method that
            // we would call in the non-INDY case.
            setTarget(fallback);
            // It's hard to call the handle now and handle exceptions right, so call directly.
            return callFallback(obj, cx, scope);
        }

        /**
         * If, on the first invocation, we found that we were looking at a "fast property,"
         * optimistically assume that future invocations will find objects with the same
         * "shape" and therefore the fast property will be found at the same index.
         * So, we switch to this method rather than the first one.
         */
        public Object lookupDirect(int index, Object obj, Context cx, Scriptable scope) {
            if (ScriptableObject.hasFastValue(obj, propertyName, index)) {
                return ((ScriptableObject)obj).getFastValue(index);
            }
            if (++misses > MAX_MISSES) {
                // If the caching isn't working, give up.
                System.out.println("Fast off for " + propertyName + " (" + index + ')');
                setTarget(fallback);
            }
            return callFallback(obj, cx, scope);
        }

        private Object callFallback(Object obj, Context cx, Scriptable scope) {
            if (warn) {
                return ScriptRuntime.getObjectProp(obj, propertyName, cx, scope);
            }
            return ScriptRuntime.getObjectPropNoWarn(obj, propertyName, cx, scope);
        }

        private void switchToFastHandles(int ix) {
            System.out.println("Fast on for " + propertyName + " (" + ix + ')');
            MethodHandle bound = MethodHandles.insertArguments(direct, 0, ix);
            setTarget(bound);
        }
    }
}
