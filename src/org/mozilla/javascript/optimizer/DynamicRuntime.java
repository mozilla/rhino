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
        String name, MethodType mType)
          throws NoSuchMethodException, IllegalAccessException {

        GetObjectPropertySite site = new GetObjectPropertySite(mType);

        // The first method called is the "initialize" method, bound to the
        // call site itself so it can change its handle.
        MethodHandle initMethod = lookup.findVirtual(GetObjectPropertySite.class,
            "initialize", mType);
        MethodHandle init = MethodHandles.insertArguments(initMethod, 0, site);
        site.setTarget(init);

        // The generic handle to look up the property directly if the guard is true
        site.direct = lookup.findVirtual(ScriptableObject.class,
            "getFastValue",
            MethodType.methodType(Object.class, Integer.TYPE));

        // The guard method to check if the property is valid
        site.guard = lookup.findStatic(ScriptableObject.class,
            "hasFastValue",
            MethodType.methodType(Boolean.TYPE, Object.class, String.class, Integer.TYPE));

        // The initialize method replaces itself with the fallback if necessary
        site.fallback = lookup.findStatic(ScriptRuntime.class, "getObjectProp", mType);

        return site;
    }

    public static final class GetObjectPropertySite
        extends MutableCallSite
    {
        MethodHandle fallback;
        MethodHandle direct;
        MethodHandle guard;

        GetObjectPropertySite(MethodType mType) {
            super(mType);
        }

        /**
         * This is called first. If the target meets the requirements, it switches to
         * a direct invocation, and otherwise falls back to the generic method.
         */
        public Object initialize(Object obj, String propertyName,
            Context cx, Scriptable scope) {
            if (obj instanceof ScriptableObject) {
                ScriptableObject so = (ScriptableObject)obj;
                int fastIx = so.getFastIndex(propertyName);
                if (fastIx >= 0) {
                    switchToFastHandles(fastIx);
                    return so.fastValues[fastIx];
                }
            }

            setTarget(fallback);
            return ScriptRuntime.getObjectProp(obj, propertyName, cx, scope);
        }

        private void switchToFastHandles(int ix) {
            // Adapt "getFastValue" from the static object that takes a particular
            // type to a virtual call that takes a pre-bound integer.
            MethodHandle boundGet = MethodHandles.insertArguments(direct, 1, ix);
            MethodHandle castGet = MethodHandles.explicitCastArguments(boundGet,
                MethodType.methodType(Object.class, Object.class));
            MethodHandle realGet = MethodHandles.dropArguments(
                castGet,
                1,
                String.class, Context.class, Scriptable.class);

            // Adapt "hasFastValue" to the called signature and add a bound
            // index.
            MethodHandle boundGuard = MethodHandles.insertArguments(guard, 2, ix);
            MethodHandle realGuard = MethodHandles.dropArguments(
                boundGuard,
                2,
                Context.class, Scriptable.class);

            // Test with no guard for now.
            setTarget(MethodHandles.guardWithTest(realGuard, realGet, fallback));
        }
    }
}
