/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static java.lang.reflect.Modifier.isProtected;
import static java.lang.reflect.Modifier.isPublic;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessControlContext;
import java.security.AllPermission;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.mozilla.javascript.lc.ReflectUtils;
import org.mozilla.javascript.lc.member.ExecutableBox;
import org.mozilla.javascript.lc.member.ExecutableOverload;
import org.mozilla.javascript.lc.member.NativeJavaField;
import org.mozilla.javascript.lc.type.TypeInfo;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

/**
 * @author Mike Shaver
 * @author Norris Boyd
 * @see NativeJavaObject
 * @see NativeJavaClass
 */
class JavaMembers {

    private static final boolean STRICT_REFLECTIVE_ACCESS = isModularJava();

    private static final Permission allPermission = new AllPermission();

    JavaMembers(Scriptable scope, Class<?> cl) {
        this(scope, cl, false);
    }

    JavaMembers(Scriptable scope, Class<?> cl, boolean includeProtected) {
        try (Context cx = ContextFactory.getGlobal().enterContext()) {
            ClassShutter shutter = cx.getClassShutter();
            if (shutter != null && !shutter.visibleToScripts(cl.getName())) {
                throw Context.reportRuntimeErrorById("msg.access.prohibited", cl.getName());
            }
            this.members = new HashMap<>();
            this.fieldAndMethods = new HashMap<>();
            this.staticMembers = new HashMap<>();
            this.staticFieldAndMethods = new HashMap<>();
            this.cl = cl;
            boolean includePrivate = cx.hasFeature(Context.FEATURE_ENHANCED_JAVA_ACCESS);
            reflect(cx, scope, includeProtected, includePrivate);
        }
    }

    /**
     * This method returns true if we are on a "modular" version of Java (Java 11 or up). It does
     * not use the SourceVersion class because this is not present on Android.
     */
    private static boolean isModularJava() {
        try {
            Class.class.getMethod("getModule");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    boolean has(String name, boolean isStatic) {
        Map<String, Object> ht = isStatic ? staticMembers : members;
        Object obj = ht.get(name);
        if (obj != null) {
            return true;
        }
        return findExplicitFunction(name, isStatic) != null;
    }

    Object get(Scriptable scope, String name, Object javaObject, boolean isStatic) {
        Map<String, Object> ht = isStatic ? staticMembers : members;
        Object member = ht.get(name);
        if (!isStatic && member == null) {
            // Try to get static member from instance (LC3)
            member = staticMembers.get(name);
        }
        if (member == null) {
            member =
                    this.getExplicitFunction(
                            scope, name,
                            javaObject, isStatic);
            if (member == null) return Scriptable.NOT_FOUND;
        }

        // TODO: cache instance in caller NativeJavaObject
        if (member instanceof ExecutableOverload) {
            if (member instanceof ExecutableOverload.WithField) {
                var withField = (ExecutableOverload.WithField) member;
                return new FieldAndMethods(scope, withField);
            } else {
                var method = (ExecutableOverload) member;
                var built = new NativeJavaMethod(method.methods, method.name);
                ScriptRuntime.setFunctionProtoAndParent(
                        built, Context.getCurrentContext(), scope, false);
                return built;
            }
        }

        if (member instanceof Scriptable) {
            return member;
        }
        Context cx = Context.getContext();

        if (member instanceof BeanProperty) {
            var bean = (BeanProperty) member;
            if (bean.getter == null) {
                return Scriptable.NOT_FOUND;
            }
            return bean.getter.call(cx, scope, scope, ScriptRuntime.emptyArgs);
        }

        var field = (NativeJavaField) member;
        Object got;
        try {
            got = field.get(isStatic ? null : javaObject);
        } catch (Exception ex) {
            throw Context.throwAsScriptRuntimeEx(ex);
        }
        var type = field.type();
        if (scope instanceof NativeJavaObject) {
            type =
                    TypeInfoFactory.GLOBAL.consolidateType(
                            type, ((NativeJavaObject) scope).staticType);
        }
        // Need to wrap the object before we return it.
        return cx.getWrapFactory().wrap(cx, ScriptableObject.getTopLevelScope(scope), got, type);
    }

    void put(Scriptable scope, String name, Object javaObject, Object value, boolean isStatic) {
        Map<String, Object> ht = isStatic ? staticMembers : members;
        Object member = ht.get(name);
        if (!isStatic && member == null) {
            // Try to get static member from instance (LC3)
            member = staticMembers.get(name);
        }
        if (member == null) throw reportMemberNotFound(name);
        if (member instanceof ExecutableOverload.WithField) {
            var withField = (ExecutableOverload.WithField) member;
            member = withField.field;
        }

        // Is this a bean property "set"?
        if (member instanceof BeanProperty) {
            BeanProperty bp = (BeanProperty) member;
            if (bp.setter == null) {
                throw reportMemberNotFound(name);
            }
            bp.setter.call(
                    Context.getContext(),
                    ScriptableObject.getTopLevelScope(scope),
                    scope,
                    new Object[] {value});
        } else if (member instanceof NativeJavaField) {
            var field = (NativeJavaField) member;
            var type = field.type();
            if (scope instanceof NativeJavaObject) {
                type =
                        TypeInfoFactory.GLOBAL.consolidateType(
                                type, ((NativeJavaObject) scope).staticType);
            }
            try {
                field.set(javaObject, Context.jsToJava(value, type));
            } catch (IllegalAccessException accessEx) {
                throw Context.throwAsScriptRuntimeEx(accessEx);
            } catch (IllegalArgumentException argEx) {
                throw Context.reportRuntimeErrorById(
                        "msg.java.internal.field.type",
                        value.getClass().getName(),
                        field,
                        javaObject.getClass().getName());
            }
        } else {
            String str = (member == null) ? "msg.java.internal.private" : "msg.java.method.assign";
            throw Context.reportRuntimeErrorById(str, name);
        }
    }

    Object[] getIds(boolean isStatic) {
        Map<String, Object> map = isStatic ? staticMembers : members;
        return map.keySet().toArray(new Object[0]);
    }

    static String javaSignature(Class<?> type) {
        if (!type.isArray()) {
            return type.getName();
        }
        int arrayDimension = 0;
        do {
            ++arrayDimension;
            type = type.getComponentType();
        } while (type.isArray());
        String name = type.getName();
        String suffix = "[]";
        if (arrayDimension == 1) {
            return name.concat(suffix);
        }
        int length = name.length() + arrayDimension * suffix.length();
        StringBuilder sb = new StringBuilder(length);
        sb.append(name);
        while (arrayDimension != 0) {
            --arrayDimension;
            sb.append(suffix);
        }
        return sb.toString();
    }

    static String liveConnectSignature(Class<?>[] argTypes) {
        if (argTypes.length == 0) {
            return "()";
        }

        var builder = new StringBuilder();

        builder.append('(');
        var iter = Arrays.asList(argTypes).iterator();
        if (iter.hasNext()) {
            builder.append(javaSignature(iter.next()));
            while (iter.hasNext()) {
                builder.append(',').append(javaSignature(iter.next()));
            }
        }
        builder.append(')');

        return builder.toString();
    }

    private ExecutableBox findExplicitFunction(String name, boolean isStatic) {
        int sigStart = name.indexOf('(');
        if (sigStart < 0) {
            return null;
        }

        Map<String, Object> ht = isStatic ? staticMembers : members;
        ExecutableBox[] methodsOrCtors = null;
        boolean isCtor = (isStatic && sigStart == 0);

        if (isCtor) {
            // Explicit request for an overloaded constructor
            methodsOrCtors = ctors.methods;
        } else {
            // Explicit request for an overloaded method
            String trueName = name.substring(0, sigStart);
            Object obj = ht.get(trueName);
            if (!isStatic && obj == null) {
                // Try to get static member from instance (LC3)
                obj = staticMembers.get(trueName);
            }
            if (obj instanceof ExecutableOverload) {
                methodsOrCtors = ((ExecutableOverload) obj).methods;
            }
        }

        if (methodsOrCtors != null) {
            for (var methodsOrCtor : methodsOrCtors) {
                String sig = ReflectUtils.liveConnectSignature(methodsOrCtor.getArgTypes());
                if (sigStart + sig.length() == name.length()
                        && name.regionMatches(sigStart, sig, 0, sig.length())) {
                    return methodsOrCtor;
                }
            }
        }

        return null;
    }

    private Object getExplicitFunction(
            Scriptable scope, String name, Object javaObject, boolean isStatic) {
        Map<String, Object> ht = isStatic ? staticMembers : members;
        Object member = null;
        var methodOrCtor = findExplicitFunction(name, isStatic);

        if (methodOrCtor != null) {
            Scriptable prototype = ScriptableObject.getFunctionPrototype(scope);

            if (methodOrCtor.isConstructor()) {
                NativeJavaConstructor fun = new NativeJavaConstructor(methodOrCtor);
                fun.setPrototype(prototype);
                member = fun;
                ht.put(name, fun);
            } else {
                String trueName = methodOrCtor.getName();
                member = ht.get(trueName);

                if (member instanceof ExecutableOverload
                        && ((ExecutableOverload) member).methods.length > 1) {
                    NativeJavaMethod fun = new NativeJavaMethod(methodOrCtor, name);
                    fun.setPrototype(prototype);
                    ht.put(name, fun);
                    member = fun;
                }
            }
        }

        return member;
    }

    /**
     * Retrieves mapping of methods to accessible methods for a class. In case the class is not
     * public, retrieves methods with same signature as its public methods from public superclasses
     * and interfaces (if they exist). Basically upcasts every method to the nearest accessible
     * method.
     */
    private Collection<Method> discoverAccessibleMethods(
            Class<?> clazz, boolean includeProtected, boolean includePrivate) {
        Map<MethodSignature, Method> map = new HashMap<>();
        discoverAccessibleMethods(clazz, map, includeProtected, includePrivate);
        return map.values();
    }

    @SuppressWarnings("deprecation")
    private void discoverAccessibleMethods(
            Class<?> clazz,
            Map<MethodSignature, Method> map,
            boolean includeProtected,
            boolean includePrivate) {
        if (isPublic(clazz.getModifiers()) || includePrivate) {
            try {
                if (includeProtected || includePrivate) {
                    while (clazz != null) {
                        try {
                            Method[] methods = clazz.getDeclaredMethods();
                            for (Method method : methods) {
                                int mods = method.getModifiers();

                                if (isPublic(mods) || isProtected(mods) || includePrivate) {
                                    Method registered = registerMethod(map, method);
                                    // We don't want to replace the deprecated method here
                                    // because it is not available on Android.
                                    if (includePrivate && !registered.isAccessible()) {
                                        registered.setAccessible(true);
                                    }
                                }
                            }
                            Class<?>[] interfaces = clazz.getInterfaces();
                            for (Class<?> intface : interfaces) {
                                discoverAccessibleMethods(
                                        intface, map, includeProtected, includePrivate);
                            }
                            clazz = clazz.getSuperclass();
                        } catch (SecurityException e) {
                            // Some security settings (i.e., applets) disallow
                            // access to Class.getDeclaredMethods. Fall back to
                            // Class.getMethods.
                            discoverPublicMethods(clazz, map);
                            break; // getMethods gets superclass methods, no
                            // need to loop any more
                        }
                    }
                } else {
                    discoverPublicMethods(clazz, map);
                }
                return;
            } catch (SecurityException e) {
                Context.reportWarning(
                        "Could not discover accessible methods of class "
                                + clazz.getName()
                                + " due to lack of privileges, "
                                + "attemping superclasses/interfaces.");
                // Fall through and attempt to discover superclass/interface
                // methods
            }
        }

        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> intface : interfaces) {
            discoverAccessibleMethods(intface, map, includeProtected, includePrivate);
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            discoverAccessibleMethods(superclass, map, includeProtected, includePrivate);
        }
    }

    void discoverPublicMethods(Class<?> clazz, Map<MethodSignature, Method> map) {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            registerMethod(map, method);
        }
    }

    static Method registerMethod(Map<MethodSignature, Method> map, Method method) {
        MethodSignature sig = new MethodSignature(method);
        // Array may contain methods with same parameter signature but different return value!
        // (which is allowed in bytecode, but not in JLS) we will take the best method
        return map.merge(sig, method, JavaMembers::getMoreConcreteMethod);
    }

    private static Method getMoreConcreteMethod(Method oldValue, Method newValue) {
        if (oldValue.getReturnType().equals(newValue.getReturnType())) {
            return oldValue; // same return type. Do not overwrite existing method
        } else if (oldValue.getReturnType().isAssignableFrom(newValue.getReturnType())) {
            return newValue; // more concrete return type. Replace method
        } else {
            return oldValue;
        }
    }

    static final class MethodSignature {
        private final String name;
        private final Class<?>[] args;

        private MethodSignature(String name, Class<?>[] args) {
            this.name = name;
            this.args = args;
        }

        MethodSignature(Method method) {
            this(method.getName(), method.getParameterTypes());
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MethodSignature) {
                MethodSignature ms = (MethodSignature) o;
                return ms.name.equals(name) && Arrays.equals(args, ms.args);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return name.hashCode() ^ args.length;
        }
    }

    private void reflect(
            Context cx, Scriptable scope, boolean includeProtected, boolean includePrivate) {
        var typeFactory = TypeInfoFactory.get(scope);

        var accessibleMethods = discoverAccessibleMethods(cl, includeProtected, includePrivate);
        var accessibleFields = getAccessibleFields(includeProtected, includePrivate);

        // We reflect methods first, because we want overloaded field/method
        // names to be allocated to the NativeJavaMethod before the field
        // gets in the way.
        for (int cursor = 0; cursor < 2; cursor++) {
            var isStatic = (cursor == 0);

            collectMethods(accessibleMethods, isStatic, typeFactory);

            collectFields(accessibleFields, isStatic, typeFactory);

            var table = isStatic ? staticMembers : members;
            table.putAll(extractBeaning(table, isStatic, includePrivate));
        }

        // Reflect constructors
        Constructor<?>[] constructors = getAccessibleConstructors(includePrivate);
        ExecutableBox[] ctorMembers = new ExecutableBox[constructors.length];
        for (int i = 0; i != constructors.length; ++i) {
            ctorMembers[i] = new ExecutableBox(constructors[i], typeFactory);
        }
        ctors = new NativeJavaMethod(ctorMembers, cl.getSimpleName());
    }

    /**
     * Transform discovered methods into {@link ExecutableOverload} and put into member table.
     *
     * <p>After this method call, member table have instances of: {@link ExecutableOverload}
     */
    protected void collectMethods(
            Collection<Method> methods, boolean isStatic, TypeInfoFactory typeFactory) {
        var table = isStatic ? staticMembers : members;
        var grouped =
                methods.stream()
                        .filter(m -> isStatic == Modifier.isStatic(m.getModifiers()))
                        .collect(Collectors.groupingBy(Method::getName));

        for (var entry : grouped.entrySet()) {
            var name = entry.getKey();
            var sameNameMethods = entry.getValue();

            var array = new ExecutableBox[sameNameMethods.size()];
            var i = 0;

            for (var method : sameNameMethods) {
                array[i++] = new ExecutableBox(method, typeFactory, cl);
            }

            table.put(name, new ExecutableOverload(name, array));
        }
    }

    /**
     * Transform discovered fields into {@link NativeJavaField} and put into member table.
     *
     * <p>After this method call, member table will contain instances of: {@link
     * ExecutableOverload}, {@link ExecutableOverload.WithField}, and {@link NativeJavaField}
     */
    protected void collectFields(
            Collection<Field> fields, boolean isStatic, TypeInfoFactory typeFactory) {
        var table = isStatic ? staticMembers : members;
        var grouped =
                fields.stream()
                        .filter(f -> isStatic == Modifier.isStatic(f.getModifiers()))
                        .collect(Collectors.groupingBy(Field::getName));

        for (var entry : grouped.entrySet()) {
            var name = entry.getKey();
            var selected = Collections.max(entry.getValue(), JavaMembers::compareAmbiguousField);

            NativeJavaField field;
            try {
                field = new NativeJavaField(selected, typeFactory);
            } catch (SecurityException e) {
                // This might happen when reading type of this field
                Context.reportWarning(
                        "Could not access field "
                                + name
                                + " of class "
                                + cl.getName()
                                + " due to lack of privileges.");
                continue; // skip this field
            }

            var existed = table.get(name);
            if (existed == null) {
                table.put(name, field);
            } else if (existed instanceof ExecutableOverload) {
                var withField =
                        new ExecutableOverload.WithField((ExecutableOverload) existed, field);
                table.put(name, withField);
            }
        }
    }

    private static int compareAmbiguousField(Field a, Field b) {
        // 'a' declared by a class that is superclass of declaring class of 'b', aka 'b' is at
        // subclass, thus shadows 'a'
        if (a.getDeclaringClass().isAssignableFrom(b.getDeclaringClass())) {
            return -1;
        }
        // otherwise, the first field wins. (legacy behavior)
        return 1;
    }

    private static boolean maskingExistedMember(
            boolean includePrivate, Map<String, Object> members, String beanName) {
        var existed = members.get(beanName);

        if (existed == null) { // no member
            return false;
        } else if (existed instanceof NativeJavaField) { // member is field
            // A private field shouldn't mask a public getter/setter
            return !includePrivate
                    || !Modifier.isPrivate(((NativeJavaField) existed).raw().getModifiers());
        }
        // member is NativeJavaMethod
        return true;
    }

    /**
     * @param nameComponent method name without prefix. E.g. "Value" in "getValue", and "X" in
     *     "getX"
     * @return bean property name. E.g. "value" if provided "Value", "X" if provided "X"
     */
    private static String getBeanName(String nameComponent) {
        char ch0 = nameComponent.charAt(0);
        if (Character.isUpperCase(ch0)) {
            if (nameComponent.length() == 1) {
                return nameComponent.toLowerCase(Locale.ROOT);
            }
            char ch1 = nameComponent.charAt(1);
            if (!Character.isUpperCase(ch1)) {
                return Character.toLowerCase(ch0) + nameComponent.substring(1);
            }
        }
        return nameComponent;
    }

    /**
     * Create bean properties from corresponding get/set methods
     *
     * <p>see {@link #collectFields(Collection, boolean, TypeInfoFactory)} for possible values in
     * member table at this stage
     */
    private static Map<String, BeanProperty> extractBeaning(
            Map<String, Object> members, boolean isStatic, boolean includePrivate) {
        var beans = new HashMap<String, BeanProperty>();
        for (var entry : members.entrySet()) {
            var name = entry.getKey();

            var isGetBeaning = name.startsWith("get");
            var isIsBeaning = name.startsWith("is");
            var isSetBeaning = name.startsWith("set");
            if (!isGetBeaning && !isIsBeaning && !isSetBeaning) {
                continue;
            }

            var nameComponent = name.substring(isIsBeaning ? 2 : 3);
            if (nameComponent.isEmpty() || !(entry.getValue() instanceof ExecutableOverload)) {
                continue;
            }

            var beanName = getBeanName(nameComponent);
            if (maskingExistedMember(includePrivate, members, beanName)) {
                continue;
            }

            var method = (ExecutableOverload) entry.getValue();
            if (isGetBeaning || isIsBeaning) { // getter

                var candidate = extractGetMethod(method.methods, isStatic);
                if (candidate != null) {
                    var bean = beans.computeIfAbsent(beanName, BeanProperty::new);
                    if (bean.getter == null
                            // prefer 'get' over 'is'
                            || bean.getter.getFunctionName().startsWith("is")) {
                        if (method.methods.length == 1) {
                            bean.getter = new NativeJavaMethod(method.methods, name);
                        } else {
                            bean.getter = new NativeJavaMethod(candidate, name);
                        }
                    }
                }
            } else { // isSetBeaning
                var bean = beans.computeIfAbsent(beanName, BeanProperty::new);
                // capture all possible setters for now, actual setter will be searched later
                bean.setter = new NativeJavaMethod(method.methods, name);
            }
        }

        // process setter candidates
        for (var bean : beans.values()) {
            var setterCandidates = bean.setter;
            if (setterCandidates == null) {
                continue;
            }

            ExecutableBox match;
            var getter = bean.getter;
            if (getter != null) {
                var type = getter.methods[0].getReturnType();
                // We have a getter. Now, do we have a setter with matching type?
                match = extractSetMethod(type, setterCandidates.methods, isStatic);
                if (match != null) {
                    bean.setter = new NativeJavaMethod(match, match.getName());
                    continue;
                }
            }

            // search for any valid setter
            match = extractSetMethod(setterCandidates.methods, isStatic);
            if (match == null) {
                // no valid setter st all
                bean.setter = null;
            }

            // at this stage we know there's at least one valid setter. We will let NativeJavaMethod
            // itself to pick the best one.
        }

        return beans;
    }

    private Constructor<?>[] getAccessibleConstructors(boolean includePrivate) {
        // The JVM currently doesn't allow changing access on java.lang.Class
        // constructors, so don't try
        if (includePrivate && cl != ScriptRuntime.ClassClass) {
            try {
                Constructor<?>[] cons = cl.getDeclaredConstructors();
                AccessibleObject.setAccessible(cons, true);

                return cons;
            } catch (SecurityException e) {
                // Fall through to !includePrivate case
                Context.reportWarning(
                        "Could not access constructor "
                                + " of class "
                                + cl.getName()
                                + " due to lack of privileges.");
            }
        }
        return cl.getConstructors();
    }

    @SuppressWarnings("deprecation")
    private List<Field> getAccessibleFields(boolean includeProtected, boolean includePrivate) {
        if (includePrivate || includeProtected) {
            try {
                List<Field> fieldsList = new ArrayList<>();

                // walk up superclass chain and grab fields. No need to deal specially with
                // interfaces, since they can't have fields
                for (var c = cl; c != null; c = c.getSuperclass()) {
                    // get all declared fields in this class, make them
                    // accessible, and save
                    for (Field field : c.getDeclaredFields()) {
                        int mod = field.getModifiers();
                        if (includePrivate || isPublic(mod) || isProtected(mod)) {
                            if (!field.isAccessible()) field.setAccessible(true);
                            fieldsList.add(field);
                        }
                    }
                }

                return fieldsList;
            } catch (SecurityException e) {
                // fall through to !includePrivate case
            }
        }
        return Arrays.asList(cl.getFields());
    }

    private static ExecutableBox extractGetMethod(ExecutableBox[] methods, boolean isStatic) {
        // Inspect the list of all ExecutableBox for the only one having no
        // parameters
        for (var method : methods) {
            // Does getter method have an empty parameter list with a return
            // value (eg. a getSomething() or isSomething())?
            if (method.getArgTypes().isEmpty() && (!isStatic || method.isStatic())) {
                var type = method.getReturnType();
                if (type != TypeInfo.PRIMITIVE_VOID) {
                    return method;
                }
                break;
            }
        }
        return null;
    }

    private static ExecutableBox extractSetMethod(
            TypeInfo type, ExecutableBox[] methods, boolean isStatic) {
        //
        // Note: it may be preferable to allow NativeJavaMethod.findFunction()
        //       to find the appropriate setter; unfortunately, it requires an
        //       instance of the target arg to determine that.
        //

        ExecutableBox acceptableMatch = null;
        for (var method : methods) {
            if (!isStatic || method.isStatic()) {
                var argTypes = method.getArgTypes();
                if (argTypes.size() == 1) {
                    if (type.is(argTypes.get(0).asClass())) {
                        // perfect match, no need to continue scanning
                        return method;
                    }
                    if (acceptableMatch == null
                            && argTypes.get(0).asClass().isAssignableFrom(type.asClass())) {
                        // do not return at this point, there can still be perfect match
                        acceptableMatch = method;
                    }
                }
            }
        }
        return acceptableMatch;
    }

    private static ExecutableBox extractSetMethod(ExecutableBox[] methods, boolean isStatic) {

        for (var method : methods) {
            if (!isStatic || method.isStatic()) {
                if (method.getReturnType().isVoid()) {
                    if (method.getArgTypes().size() == 1) {
                        return method;
                    }
                }
            }
        }
        return null;
    }

    Map<String, FieldAndMethods> getFieldAndMethodsObjects(
            Scriptable scope, Object javaObject, boolean isStatic) {
        var ht = isStatic ? staticFieldAndMethods : fieldAndMethods;

        var expectedCapacity = (int) Math.ceil(ht.size() / 0.75);
        var result = new HashMap<String, FieldAndMethods>(expectedCapacity);
        for (var entry : ht.entrySet()) {
            var fieldAndMethods = new FieldAndMethods(scope, entry.getValue());
            fieldAndMethods.javaObject = javaObject;

            result.put(entry.getKey(), fieldAndMethods);
        }
        return result;
    }

    static JavaMembers lookupClass(
            Scriptable scope, Class<?> dynamicType, Class<?> staticType, boolean includeProtected) {
        JavaMembers members;
        ClassCache cache = ClassCache.get(scope);
        Map<ClassCache.CacheKey, JavaMembers> ct = cache.getClassCacheMap();

        Class<?> cl = dynamicType;
        Object secCtx = getSecurityContext();
        for (; ; ) {
            members = ct.get(new ClassCache.CacheKey(cl, secCtx));
            if (members != null) {
                if (cl != dynamicType) {
                    // member lookup for the original class failed because of
                    // missing privileges, cache the result so we don't try again
                    ct.put(new ClassCache.CacheKey(dynamicType, secCtx), members);
                }
                return members;
            }
            try {
                members = createJavaMembers(cache.getAssociatedScope(), cl, includeProtected);
                break;
            } catch (SecurityException e) {
                // Reflection may fail for objects that are in a restricted
                // access package (e.g. sun.*).  If we get a security
                // exception, try again with the static type if it is interface.
                // Otherwise, try superclass
                if (staticType != null && staticType.isInterface()) {
                    cl = staticType;
                    staticType = null; // try staticType only once
                } else {
                    Class<?> parent = cl.getSuperclass();
                    if (parent == null) {
                        if (cl.isInterface()) {
                            // last resort after failed staticType interface
                            parent = ScriptRuntime.ObjectClass;
                        } else {
                            throw e;
                        }
                    }
                    cl = parent;
                }
            }
        }

        if (cache.isCachingEnabled()) {
            ct.put(new ClassCache.CacheKey(cl, secCtx), members);
            if (cl != dynamicType) {
                // member lookup for the original class failed because of
                // missing privileges, cache the result so we don't try again
                ct.put(new ClassCache.CacheKey(dynamicType, secCtx), members);
            }
        }
        return members;
    }

    private static JavaMembers createJavaMembers(
            Scriptable associatedScope, Class<?> cl, boolean includeProtected) {
        if (STRICT_REFLECTIVE_ACCESS) {
            return new JavaMembers_jdk11(associatedScope, cl, includeProtected);
        } else {
            return new JavaMembers(associatedScope, cl, includeProtected);
        }
    }

    private static Object getSecurityContext() {
        Object sec = null;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sec = sm.getSecurityContext();
            if (sec instanceof AccessControlContext) {
                try {
                    ((AccessControlContext) sec).checkPermission(allPermission);
                    // if we have allPermission, we do not need to store the
                    // security object in the cache key
                    return null;
                } catch (SecurityException e) {
                }
            }
        }
        return sec;
    }

    RuntimeException reportMemberNotFound(String memberName) {
        return Context.reportRuntimeErrorById(
                "msg.java.member.not.found", cl.getName(), memberName);
    }

    private final Class<?> cl;

    /**
     * All possible types of values in this map: {@link ExecutableOverload}, {@link
     * NativeJavaField}, {@link ExecutableOverload.WithField}, and {@link BeanProperty}
     */
    private final Map<String, Object> members;

    private final Map<String, ExecutableOverload.WithField> fieldAndMethods;

    /** All possible types of values in this map: same as {@link #members} */
    private final Map<String, Object> staticMembers;

    private final Map<String, ExecutableOverload.WithField> staticFieldAndMethods;
    NativeJavaMethod ctors; // we use NativeJavaMethod for ctor overload resolution
}

final class BeanProperty {
    BeanProperty(String name) {
        this.name = name;
    }

    final String name;
    NativeJavaMethod getter;
    NativeJavaMethod setter;
}

class FieldAndMethods extends NativeJavaMethod {
    private static final long serialVersionUID = -9222428244284796755L;

    FieldAndMethods(Scriptable scope, ExecutableOverload.WithField withField) {
        super(withField.methods, withField.name);
        this.field = withField.field;
        setParentScope(scope);
        setPrototype(ScriptableObject.getFunctionPrototype(scope));
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        if (hint == ScriptRuntime.FunctionClass) return this;
        Object rval;
        try {
            rval = field.get(javaObject);
        } catch (IllegalAccessException accEx) {
            throw Context.reportRuntimeErrorById(
                    "msg.java.internal.private", field.raw().getName());
        }
        Context cx = Context.getContext();
        rval = cx.getWrapFactory().wrap(cx, this, rval, field.type());
        if (rval instanceof Scriptable) {
            rval = ((Scriptable) rval).getDefaultValue(hint);
        }
        return rval;
    }

    NativeJavaField field;
    Object javaObject;
}
