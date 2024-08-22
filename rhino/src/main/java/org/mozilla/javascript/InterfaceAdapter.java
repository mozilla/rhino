/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;

/**
 * Adapter to use JS function as implementation of Java interfaces with single method or multiple
 * methods with the same signature.
 */
public class InterfaceAdapter {
    private final Object proxyHelper;

    /**
     * Make glue object implementing interface cl that will call the supplied JS function when
     * called. Only interfaces were all methods have the same signature is supported.
     *
     * @return The glue object or null if <code>cl</code> is not interface or has methods with
     *     different signatures.
     */
    static Object create(Context cx, Class<?> cl, ScriptableObject object) {
        if (!cl.isInterface()) throw new IllegalArgumentException();

        Scriptable topScope = ScriptRuntime.getTopCallScope(cx);
        ClassCache cache = ClassCache.get(topScope);
        InterfaceAdapter adapter;
        adapter = (InterfaceAdapter) cache.getInterfaceAdapter(cl);
        ContextFactory cf = cx.getFactory();
        if (adapter == null) {
            if (object instanceof Callable) {
                Method[] methods = cl.getMethods();
                // Check if interface can be implemented by a single function.
                // We allow this if the interface has only one method or multiple
                // methods with the same name (in which case they'd result in
                // the same function to be invoked anyway).
                HashSet<String> functionalMethodNames = new HashSet<>();
                HashSet<String> defaultMethodNames = new HashSet<>();
                for (Method method : methods) {
                    // there are multiple methods in the interface we inspect
                    // only abstract ones, they must all have the same name.
                    if (isFunctionalMethodCandidate(method)) {
                        functionalMethodNames.add(method.getName());
                        if (functionalMethodNames.size() > 1) {
                            break;
                        }
                    } else {
                        defaultMethodNames.add(method.getName());
                    }
                }

                boolean canConvert =
                        (functionalMethodNames.size() == 1)
                                || (functionalMethodNames.isEmpty()
                                        && defaultMethodNames.size() == 1);
                // There is no abstract method or there are multiple methods.
                if (!canConvert) {
                    if (functionalMethodNames.isEmpty() && defaultMethodNames.isEmpty()) {
                        throw Context.reportRuntimeErrorById(
                                "msg.no.empty.interface.conversion", cl.getName());
                    } else {
                        throw Context.reportRuntimeErrorById(
                                "msg.no.function.interface.conversion", cl.getName());
                    }
                }
            }
            adapter = new InterfaceAdapter(cf, cl);
            cache.cacheInterfaceAdapter(cl, adapter);
        }
        return VMBridge.instance.newInterfaceProxy(
                adapter.proxyHelper, cf, adapter, object, topScope);
    }

    /**
     * We have to ignore java8 default methods and methods like 'equals', 'hashCode' and 'toString'
     * as it occurs for example in the Comparator interface.
     *
     * @return true, if the function
     */
    private static boolean isFunctionalMethodCandidate(Method method) {
        if (method.getName().equals("equals")
                || method.getName().equals("hashCode")
                || method.getName().equals("toString")) {
            // it should be safe to ignore them as there is also a special
            // case for these methods in VMBridge_jdk18.newInterfaceProxy
            return false;
        } else {
            return Modifier.isAbstract(method.getModifiers());
        }
    }

    private InterfaceAdapter(ContextFactory cf, Class<?> cl) {
        this.proxyHelper = VMBridge.instance.getInterfaceProxyHelper(cf, new Class[] {cl});
    }

    public Object invoke(
            ContextFactory cf,
            final Object target,
            final Scriptable topScope,
            final Object thisObject,
            final Method method,
            final Object[] args) {
        return cf.call(cx -> invokeImpl(cx, target, topScope, thisObject, method, args));
    }

    Object invokeImpl(
            Context cx,
            Object target,
            Scriptable topScope,
            Object thisObject,
            Method method,
            Object[] args) {
        Callable function;
        if (target instanceof Callable) {
            function = (Callable) target;
        } else {
            Scriptable s = (Scriptable) target;
            String methodName = method.getName();
            Object value = ScriptableObject.getProperty(s, methodName);
            if (value == Scriptable.NOT_FOUND) {
                // We really should throw an error here, but for the sake of
                // compatibility with JavaAdapter we silently ignore undefined
                // methods.
                Context.reportWarning(
                        ScriptRuntime.getMessageById(
                                "msg.undefined.function.interface", methodName));
                Class<?> resultType = method.getReturnType();
                if (resultType == Void.TYPE) {
                    return null;
                }
                return Context.jsToJava(null, resultType);
            }
            if (!(value instanceof Callable)) {
                throw Context.reportRuntimeErrorById("msg.not.function.interface", methodName);
            }
            function = (Callable) value;
        }
        WrapFactory wf = cx.getWrapFactory();
        if (args == null) {
            args = ScriptRuntime.emptyArgs;
        } else {
            for (int i = 0, N = args.length; i != N; ++i) {
                Object arg = args[i];
                // neutralize wrap factory java primitive wrap feature
                if (!(arg instanceof String || arg instanceof Number || arg instanceof Boolean)) {
                    args[i] = wf.wrap(cx, topScope, arg, null);
                }
            }
        }
        Scriptable thisObj = wf.wrapAsJavaObject(cx, topScope, thisObject, null);

        Object result = function.call(cx, topScope, thisObj, args);
        Class<?> javaResultType = method.getReturnType();
        if (javaResultType == Void.TYPE) {
            result = null;
        } else {
            result = Context.jsToJava(result, javaResultType);
        }
        return result;
    }
}
