/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import org.mozilla.javascript.lc.type.TypeInfo;

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
     * @return The glue object or null if {@code cl} is not interface or has methods with different
     *     signatures.
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
        return newInterfaceProxy(adapter.proxyHelper, cf, adapter, object, topScope);
    }

    private static Object newInterfaceProxy(
            Object proxyHelper,
            final ContextFactory cf,
            final InterfaceAdapter adapter,
            final Object target,
            final Scriptable topScope) {
        Constructor<?> c = (Constructor<?>) proxyHelper;

        InvocationHandler handler =
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        // In addition to methods declared in the interface, proxies
                        // also route some java.lang.Object methods through the
                        // invocation handler.
                        if (method.getDeclaringClass() == Object.class) {
                            String methodName = method.getName();
                            if ("equals".equals(methodName)) {
                                Object other = args[0];
                                // Note: we could compare a proxy and its wrapped function
                                // as equal here but that would break symmetry of equal().
                                // The reason == suffices here is that proxies are cached
                                // in ScriptableObject (see NativeJavaObject.coerceType())
                                return Boolean.valueOf(proxy == other);
                            }
                            if ("hashCode".equals(methodName)) {
                                return Integer.valueOf(target.hashCode());
                            }
                            if ("toString".equals(methodName)) {
                                return "Proxy[" + target.toString() + "]";
                            }
                        }
                        return adapter.invoke(cf, target, topScope, proxy, method, args);
                    }
                };
        Object proxy;
        try {
            proxy = c.newInstance(handler);
        } catch (InvocationTargetException ex) {
            throw Context.throwAsScriptRuntimeEx(ex);
        } catch (IllegalAccessException | InstantiationException ex) {
            // Should not happen
            throw new IllegalStateException(ex);
        }
        return proxy;
    }

    /**
     * We have to ignore java8 default methods and methods like 'equals', 'hashCode' and 'toString'
     * as it occurs for example in the Comparator interface.
     *
     * @return true, if the function
     */
    private static boolean isFunctionalMethodCandidate(Method method) {
        if ("equals".equals(method.getName())
                || "hashCode".equals(method.getName())
                || "toString".equals(method.getName())) {
            // it should be safe to ignore them as there is also a special
            // case for these methods in VMBridge_jdk18.newInterfaceProxy
            return false;
        } else {
            return Modifier.isAbstract(method.getModifiers());
        }
    }

    private InterfaceAdapter(ContextFactory cf, Class<?> cl) {
        this.proxyHelper = getInterfaceProxyHelper(cf, new Class[] {cl});
    }

    @SuppressWarnings("deprecation")
    private static Object getInterfaceProxyHelper(ContextFactory cf, Class<?>[] interfaces) {
        // XXX: How to handle interfaces array withclasses from different
        // class loaders? Using cf.getApplicationClassLoader() ?
        ClassLoader loader = interfaces[0].getClassLoader();
        Class<?> cl = Proxy.getProxyClass(loader, interfaces);
        Constructor<?> c;
        try {
            c = cl.getConstructor(InvocationHandler.class);
        } catch (NoSuchMethodException ex) {
            // Should not happen
            throw new IllegalStateException(ex);
        }
        return c;
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
                    args[i] = wf.wrap(cx, topScope, arg, TypeInfo.NONE);
                }
            }
        }
        Scriptable thisObj = wf.wrapAsJavaObject(cx, topScope, thisObject, TypeInfo.NONE);

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
