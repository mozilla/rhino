package org.mozilla.javascript.lc.java;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;
import org.mozilla.javascript.annotations.JSStaticFunction;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

public class LiveConnect {

    public static <T extends Scriptable> BaseFunction buildClassCtor(
            Scriptable scope, Class<T> clazz, boolean sealed, boolean mapInheritance)
            throws IllegalAccessException, InstantiationException, InvocationTargetException {
        Method[] methods = FunctionObject.getMethodList(clazz);
        for (Method method : methods) {
            if (!method.getName().equals("init")) continue;
            Class<?>[] parmTypes = method.getParameterTypes();
            if (parmTypes.length == 3
                    && parmTypes[0] == ScriptRuntime.ContextClass
                    && parmTypes[1] == ScriptRuntime.ScriptableClass
                    && parmTypes[2] == Boolean.TYPE
                    && Modifier.isStatic(method.getModifiers())) {
                Object[] args = {
                    Context.getContext(), scope, sealed ? Boolean.TRUE : Boolean.FALSE
                };
                method.invoke(null, args);
                return null;
            }
            if (parmTypes.length == 1
                    && parmTypes[0] == ScriptRuntime.ScriptableClass
                    && Modifier.isStatic(method.getModifiers())) {
                Object[] args = {scope};
                method.invoke(null, args);
                return null;
            }
        }

        // If we got here, there isn't an "init" method with the right
        // parameter types.

        Constructor<?>[] ctors = clazz.getConstructors();
        Constructor<?> protoCtor = null;
        for (Constructor<?> constructor : ctors) {
            if (constructor.getParameterTypes().length == 0) {
                protoCtor = constructor;
                break;
            }
        }
        if (protoCtor == null) {
            throw Context.reportRuntimeErrorById("msg.zero.arg.ctor", clazz.getName());
        }

        Scriptable proto = (Scriptable) protoCtor.newInstance(ScriptRuntime.emptyArgs);
        String className = proto.getClassName();

        // check for possible redefinition
        Object existing =
                ScriptableObject.getProperty(ScriptableObject.getTopLevelScope(scope), className);
        if (existing instanceof BaseFunction) {
            Object existingProto = ((BaseFunction) existing).getPrototypeProperty();
            if (existingProto != null && clazz.equals(existingProto.getClass())) {
                return (BaseFunction) existing;
            }
        }

        // Set the prototype's prototype, trying to map Java inheritance to JS
        // prototype-based inheritance if requested to do so.
        Scriptable superProto = null;
        if (mapInheritance) {
            Class<? super T> superClass = clazz.getSuperclass();
            if (ScriptRuntime.ScriptableClass.isAssignableFrom(superClass)
                    && !Modifier.isAbstract(superClass.getModifiers())) {
                Class<? extends Scriptable> superScriptable = extendsScriptable(superClass);
                String name =
                        ScriptableObject.defineClass(
                                scope, superScriptable, sealed, mapInheritance);
                if (name != null) {
                    superProto = ScriptableObject.getClassPrototype(scope, name);
                }
            }
        }
        if (superProto == null) {
            superProto = ScriptableObject.getObjectPrototype(scope);
        }
        proto.setPrototype(superProto);

        // Find out whether there are any methods that begin with
        // "js". If so, then only methods that begin with special
        // prefixes will be defined as JavaScript entities.
        final String functionPrefix = "jsFunction_";
        final String staticFunctionPrefix = "jsStaticFunction_";
        final String getterPrefix = "jsGet_";
        final String setterPrefix = "jsSet_";
        final String ctorName = "jsConstructor";

        Member ctorMember = findAnnotatedMember(methods, JSConstructor.class);
        if (ctorMember == null) {
            ctorMember = findAnnotatedMember(ctors, JSConstructor.class);
        }
        if (ctorMember == null) {
            ctorMember = FunctionObject.findSingleMethod(methods, ctorName);
        }
        if (ctorMember == null) {
            if (ctors.length == 1) {
                ctorMember = ctors[0];
            } else if (ctors.length == 2) {
                if (ctors[0].getParameterTypes().length == 0) ctorMember = ctors[1];
                else if (ctors[1].getParameterTypes().length == 0) ctorMember = ctors[0];
            }
            if (ctorMember == null) {
                throw Context.reportRuntimeErrorById("msg.ctor.multiple.parms", clazz.getName());
            }
        }

        FunctionObject ctor = new FunctionObject(className, ctorMember, scope);
        if (ctor.isVarArgsMethod()) {
            throw Context.reportRuntimeErrorById("msg.varargs.ctor", ctorMember.getName());
        }
        ctor.initAsConstructor(
                scope,
                proto,
                ScriptableObject.DONTENUM | ScriptableObject.PERMANENT | ScriptableObject.READONLY);

        Method finishInit = null;
        HashSet<String> staticNames = new HashSet<>(), instanceNames = new HashSet<>();
        for (Method method : methods) {
            if (method == ctorMember) {
                continue;
            }
            String name = method.getName();
            if (name.equals("finishInit")) {
                Class<?>[] parmTypes = method.getParameterTypes();
                if (parmTypes.length == 3
                        && parmTypes[0] == ScriptRuntime.ScriptableClass
                        && parmTypes[1] == FunctionObject.class
                        && parmTypes[2] == ScriptRuntime.ScriptableClass
                        && Modifier.isStatic(method.getModifiers())) {
                    finishInit = method;
                    continue;
                }
            }
            // ignore any compiler generated methods.
            if (name.indexOf('$') != -1) continue;
            if (name.equals(ctorName)) continue;

            Annotation annotation = null;
            String prefix = null;
            if (method.isAnnotationPresent(JSFunction.class)) {
                annotation = method.getAnnotation(JSFunction.class);
            } else if (method.isAnnotationPresent(JSStaticFunction.class)) {
                annotation = method.getAnnotation(JSStaticFunction.class);
            } else if (method.isAnnotationPresent(JSGetter.class)) {
                annotation = method.getAnnotation(JSGetter.class);
            } else if (method.isAnnotationPresent(JSSetter.class)) {
                continue;
            }

            if (annotation == null) {
                if (name.startsWith(functionPrefix)) {
                    prefix = functionPrefix;
                } else if (name.startsWith(staticFunctionPrefix)) {
                    prefix = staticFunctionPrefix;
                } else if (name.startsWith(getterPrefix)) {
                    prefix = getterPrefix;
                } else {
                    // note that setterPrefix is among the unhandled names here -
                    // we deal with that when we see the getter
                    continue;
                }
            }

            boolean isStatic =
                    annotation instanceof JSStaticFunction
                            || Objects.equals(prefix, staticFunctionPrefix);
            HashSet<String> names = isStatic ? staticNames : instanceNames;
            String propName = getPropertyName(name, prefix, annotation);
            if (names.contains(propName)) {
                throw Context.reportRuntimeErrorById("duplicate.defineClass.name", name, propName);
            }
            names.add(propName);
            name = propName;

            if (annotation instanceof JSGetter || Objects.equals(prefix, getterPrefix)) {
                if (!(proto instanceof ScriptableObject)) {
                    throw Context.reportRuntimeErrorById(
                            "msg.extend.scriptable", proto.getClass().toString(), name);
                }
                Method setter = findSetterMethod(methods, name, setterPrefix);
                int attr =
                        ScriptableObject.PERMANENT
                                | ScriptableObject.DONTENUM
                                | (setter != null ? 0 : ScriptableObject.READONLY);
                ((ScriptableObject) proto).defineProperty(name, null, method, setter, attr);
                continue;
            }

            if (isStatic && !Modifier.isStatic(method.getModifiers())) {
                throw Context.reportRuntimeError(
                        "jsStaticFunction must be used with static method.");
            }

            FunctionObject f = new FunctionObject(name, method, proto);
            if (f.isVarArgsConstructor()) {
                throw Context.reportRuntimeErrorById("msg.varargs.fun", ctorMember.getName());
            }
            ScriptableObject.defineProperty(
                    isStatic ? ctor : proto, name, f, ScriptableObject.DONTENUM);
            if (sealed) {
                f.sealObject();
            }
        }

        // Call user code to complete initialization if necessary.
        if (finishInit != null) {
            Object[] finishArgs = {scope, ctor, proto};
            finishInit.invoke(null, finishArgs);
        }

        // Seal the object if necessary.
        if (sealed) {
            ctor.sealObject();
            if (proto instanceof ScriptableObject) {
                ((ScriptableObject) proto).sealObject();
            }
        }

        return ctor;
    }

    /**
     * Define a JavaScript property with getter and setter side effects.
     *
     * <p>If the setter is not found, the attribute READONLY is added to the given attributes.
     *
     * <p>The getter must be a method with zero parameters, and the setter, if found, must be a
     * method with one parameter.
     *
     * @param propertyName the name of the property to define. This name also affects the name of
     *     the setter and getter to search for. If the propertyId is "foo", then <code>clazz</code>
     *     will be searched for "getFoo" and "setFoo" methods.
     * @param clazz the Java class to search for the getter and setter
     * @param attributes the attributes of the JavaScript property
     * @see Scriptable#put(String, Scriptable, Object)
     */
    public static void defineProperty(
            ScriptableObject target, String propertyName, Class<?> clazz, int attributes) {
        int length = propertyName.length();
        if (length == 0) throw new IllegalArgumentException();
        char[] buf = new char[3 + length];
        propertyName.getChars(0, length, buf, 3);
        buf[3] = Character.toUpperCase(buf[3]);
        buf[0] = 'g';
        buf[1] = 'e';
        buf[2] = 't';
        String getterName = new String(buf);
        buf[0] = 's';
        String setterName = new String(buf);

        Method[] methods = FunctionObject.getMethodList(clazz);
        Method getter = FunctionObject.findSingleMethod(methods, getterName);
        Method setter = FunctionObject.findSingleMethod(methods, setterName);
        if (setter == null) attributes |= ScriptableObject.READONLY;
        defineProperty(
                target, propertyName, null, getter, setter == null ? null : setter, attributes);
    }

    /**
     * Define a JavaScript property.
     *
     * <p>Use this method only if you wish to define getters and setters for a given property in a
     * ScriptableObject. To create a property without special getter or setter side effects, use
     * <code>defineProperty(String,int)</code>.
     *
     * <p>If <code>setter</code> is null, the attribute READONLY is added to the given attributes.
     *
     * <p>Several forms of getters or setters are allowed. In all cases the type of the value
     * parameter can be any one of the following types: Object, String, boolean, Scriptable, byte,
     * short, int, long, float, or double. The runtime will perform appropriate conversions based
     * upon the type of the parameter (see description in FunctionObject). The first forms are
     * nonstatic methods of the class referred to by 'this':
     *
     * <pre>
     * Object getFoo();
     *
     * void setFoo(SomeType value);
     * </pre>
     *
     * Next are static methods that may be of any class; the object whose property is being accessed
     * is passed in as an extra argument:
     *
     * <pre>
     * static Object getFoo(Scriptable obj);
     *
     * static void setFoo(Scriptable obj, SomeType value);
     * </pre>
     *
     * Finally, it is possible to delegate to another object entirely using the <code>delegateTo
     * </code> parameter. In this case the methods are nonstatic methods of the class delegated to,
     * and the object whose property is being accessed is passed in as an extra argument:
     *
     * <pre>
     * Object getFoo(Scriptable obj);
     *
     * void setFoo(Scriptable obj, SomeType value);
     * </pre>
     *
     * @param propertyName the name of the property to define.
     * @param delegateTo an object to call the getter and setter methods on, or null, depending on
     *     the form used above.
     * @param getter the method to invoke to get the value of the property
     * @param setter the method to invoke to set the value of the property
     * @param attributes the attributes of the JavaScript property
     */
    public static void defineProperty(
            ScriptableObject target,
            String propertyName,
            Object delegateTo,
            Method getter,
            Method setter,
            int attributes) {
        MemberBox getterBox = null;
        if (getter != null) {
            getterBox = new MemberBox(getter, TypeInfoFactory.get(target));

            boolean delegatedForm;
            if (!Modifier.isStatic(getter.getModifiers())) {
                delegatedForm = (delegateTo != null);
                getterBox.delegateTo = delegateTo;
            } else {
                delegatedForm = true;
                // Ignore delegateTo for static getter but store
                // non-null delegateTo indicator.
                getterBox.delegateTo = Void.TYPE;
            }

            String errorId = null;
            Class<?>[] parmTypes = getter.getParameterTypes();
            if (parmTypes.length == 0) {
                if (delegatedForm) {
                    errorId = "msg.obj.getter.parms";
                }
            } else if (parmTypes.length == 1) {
                Object argType = parmTypes[0];
                // Allow ScriptableObject for compatibility
                if (!(argType == ScriptRuntime.ScriptableClass
                        || argType == ScriptRuntime.ScriptableObjectClass)) {
                    errorId = "msg.bad.getter.parms";
                } else if (!delegatedForm) {
                    errorId = "msg.bad.getter.parms";
                }
            } else {
                errorId = "msg.bad.getter.parms";
            }
            if (errorId != null) {
                throw Context.reportRuntimeErrorById(errorId, getter.toString());
            }
        }

        MemberBox setterBox = null;
        if (setter != null) {
            if (setter.getReturnType() != Void.TYPE)
                throw Context.reportRuntimeErrorById("msg.setter.return", setter.toString());

            setterBox = new MemberBox(setter, TypeInfoFactory.get(target));

            boolean delegatedForm;
            if (!Modifier.isStatic(setter.getModifiers())) {
                delegatedForm = (delegateTo != null);
                setterBox.delegateTo = delegateTo;
            } else {
                delegatedForm = true;
                // Ignore delegateTo for static setter but store
                // non-null delegateTo indicator.
                setterBox.delegateTo = Void.TYPE;
            }

            String errorId = null;
            Class<?>[] parmTypes = setter.getParameterTypes();
            if (parmTypes.length == 1) {
                if (delegatedForm) {
                    errorId = "msg.setter2.expected";
                }
            } else if (parmTypes.length == 2) {
                Object argType = parmTypes[0];
                // Allow ScriptableObject for compatibility
                if (!(argType == ScriptRuntime.ScriptableClass
                        || argType == ScriptRuntime.ScriptableObjectClass)) {
                    errorId = "msg.setter2.parms";
                } else if (!delegatedForm) {
                    errorId = "msg.setter1.parms";
                }
            } else {
                errorId = "msg.setter.parms";
            }
            if (errorId != null) {
                throw Context.reportRuntimeErrorById(errorId, setter.toString());
            }
        }

        target.setGetterAndSetter(
                propertyName,
                0,
                getterBox == null ? null : new MemberBoxGetter(getterBox),
                setterBox == null ? null : new MemberBoxSetter(setterBox),
                attributes);
    }

    /**
     * Search for names in a class, adding the resulting methods as properties.
     *
     * <p>Uses reflection to find the methods of the given names. Then FunctionObjects are
     * constructed from the methods found, and are added to this object as properties with the given
     * names.
     *
     * @param names the names of the Methods to add as function properties
     * @param clazz the class to search for the Methods
     * @param attributes the attributes of the new properties
     * @see FunctionObject
     */
    public static void defineFunctionProperties(
            ScriptableObject target, String[] names, Class<?> clazz, int attributes) {
        Method[] methods = FunctionObject.getMethodList(clazz);
        for (String name : names) {
            Method m = FunctionObject.findSingleMethod(methods, name);
            if (m == null) {
                throw Context.reportRuntimeErrorById("msg.method.not.found", name, clazz.getName());
            }
            FunctionObject f = new FunctionObject(name, m, target);
            target.defineProperty(name, f, attributes);
        }
    }

    /**
     * Convert a JavaScript value into the desired type. Uses the semantics defined with
     * LiveConnect3 and throws an Illegal argument exception if the conversion cannot be performed.
     *
     * @param value the JavaScript value to convert
     * @param desiredType the Java type to convert to. Primitive Java types are represented using
     *     the TYPE fields in the corresponding wrapper class in java.lang.
     * @return the converted value
     * @throws EvaluatorException if the conversion cannot be performed
     */
    public static Object jsToJava(Object value, Class<?> desiredType) throws EvaluatorException {
        return jsToJava(value, TypeInfoFactory.GLOBAL.create(desiredType));
    }

    public static Object jsToJava(Object value, TypeInfo desiredType) throws EvaluatorException {
        return NativeJavaObject.coerceTypeImpl(desiredType, value);
    }

    @SuppressWarnings({"unchecked"})
    private static <T extends Scriptable> Class<T> extendsScriptable(Class<?> c) {
        if (ScriptRuntime.ScriptableClass.isAssignableFrom(c)) return (Class<T>) c;
        return null;
    }

    private static Member findAnnotatedMember(
            AccessibleObject[] members, Class<? extends Annotation> annotation) {
        for (AccessibleObject member : members) {
            if (member.isAnnotationPresent(annotation)) {
                return (Member) member;
            }
        }
        return null;
    }

    private static Method findSetterMethod(Method[] methods, String name, String prefix) {
        String newStyleName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        for (Method method : methods) {
            JSSetter annotation = method.getAnnotation(JSSetter.class);
            if (annotation != null) {
                if (name.equals(annotation.value())
                        || ("".equals(annotation.value())
                                && newStyleName.equals(method.getName()))) {
                    return method;
                }
            }
        }
        String oldStyleName = prefix + name;
        for (Method method : methods) {
            if (oldStyleName.equals(method.getName())) {
                return method;
            }
        }
        return null;
    }

    private static String getPropertyName(String methodName, String prefix, Annotation annotation) {
        if (prefix != null) {
            return methodName.substring(prefix.length());
        }
        String propName = null;
        if (annotation instanceof JSGetter) {
            propName = ((JSGetter) annotation).value();
            if (propName == null || propName.length() == 0) {
                if (methodName.length() > 3 && methodName.startsWith("get")) {
                    propName = methodName.substring(3);
                    if (Character.isUpperCase(propName.charAt(0))) {
                        if (propName.length() == 1) {
                            propName = propName.toLowerCase(Locale.ROOT);
                        } else if (!Character.isUpperCase(propName.charAt(1))) {
                            propName =
                                    Character.toLowerCase(propName.charAt(0))
                                            + propName.substring(1);
                        }
                    }
                }
            }
        } else if (annotation instanceof JSFunction) {
            propName = ((JSFunction) annotation).value();
        } else if (annotation instanceof JSStaticFunction) {
            propName = ((JSStaticFunction) annotation).value();
        }
        if (propName == null || propName.length() == 0) {
            propName = methodName;
        }
        return propName;
    }
}
