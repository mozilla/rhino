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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Mike Shaver
 * @author Norris Boyd
 * @see NativeJavaObject
 * @see NativeJavaClass
 */
abstract class JavaMembers
{
    protected Class<?> cl;
    protected Map<String,Object> members;
    protected Map<String,FieldAndMethods> fieldAndMethods;
    protected Map<String,Object> staticMembers;
    protected Map<String,FieldAndMethods> staticFieldAndMethods;
    NativeJavaMethod ctors; // we use NativeJavaMethod for ctor overload resolution

    abstract boolean has(String name, boolean isStatic);

    abstract Object[] getIds(boolean isStatic);

    abstract Object get(Scriptable scope, String name, Object javaObject,
            boolean isStatic);

    abstract void put(Scriptable scope, String name, Object javaObject,
            Object value, boolean isStatic);

    final RuntimeException reportMemberNotFound(String memberName)
    {
        return Context.reportRuntimeError2(
            "msg.java.member.not.found", cl.getName(), memberName);
    }


    abstract Map<String,FieldAndMethods> getFieldAndMethodsObjects(Scriptable scope,
            Object javaObject, boolean isStatic);

    protected static final String getProperty(final String key,final String defaultValue) {
        return System.getProperty(key, defaultValue);
    }

    static String javaSignature(Class<?> type)
    {
        if (!type.isArray()) {
            return type.getName();
        } else {
            int arrayDimension = 0;
            do {
                ++arrayDimension;
                type = type.getComponentType();
            } while (type.isArray());
            String name = type.getName();
            String suffix = "[]";
            if (arrayDimension == 1) {
                return name.concat(suffix);
            } else {
                int length = name.length() + arrayDimension * suffix.length();
                StringBuilder sb = new StringBuilder(length);
                sb.append(name);
                while (arrayDimension != 0) {
                    --arrayDimension;
                    sb.append(suffix);
                }
                return sb.toString();
            }
        }
    }

    static String liveConnectSignature(Class<?>[] argTypes)
    {
        int N = argTypes.length;
        if (N == 0) { return "()"; }
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i != N; ++i) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(javaSignature(argTypes[i]));
        }
        sb.append(')');
        return sb.toString();
    }


    protected static final class ClassReflectBean {

        final NativeJavaMethod constructorMethod;
        //private final Map<String,MethodSignature> methodMap;
        final Map<MethodSignature,Method> mSignatureMap;
        final Field[] fields;
        final Map<String,Field> fieldMap;

        private final Map<String,Field> staticFieldMap;
        private final Map<String,Field> instFieldMap;

        final Map<String,String> staticBeanProperties;
        final Map<String,String> instBeanProperties;
        final Map<String,Object> members;
        final Map<String,Object> staticMembers;

        final Set<String> staticMemberNames;
        final Set<String> instMemberNames;
        final Set<String> staticFieldAndMethods;
        final Set<String> instFieldAndMethods;

        Map<String,Object> getMembers(final boolean isStatic){
            return isStatic ? staticMembers : members;
        }

        boolean has(final String key ,final boolean isStatic) {
            return  isStatic ?  staticMemberNames.contains(key) : instMemberNames.contains(key);
        }

        Object[] getIds(final boolean isStatic) {
            Map<String,Object> map = isStatic ? staticMembers : members;
            return map.keySet().toArray(new String[map.size()]);
        }
        ClassReflectBean(NativeJavaMethod constructorMethod, Map<MethodSignature, Method> mSignatureMap,Field[] fields) {
            super();
            this.staticMemberNames = new HashSet<String>();
            this.instMemberNames = new HashSet<String>();
            this.constructorMethod = constructorMethod;
            this.mSignatureMap = mSignatureMap;
            this.fields = fields;
            this.members = new HashMap<String,Object>();
            this.staticMembers = new HashMap<String,Object>();
            for(Map.Entry<MethodSignature, Method> entry : mSignatureMap.entrySet()) {
                final Method m = entry.getValue();
                final MethodSignature key = entry.getKey();
                if(key.staticMethod) {
                    staticMemberNames.add(key.name);
                }else {
                    instMemberNames.add(key.name);
                }
            }
            Set<String> staticMethodNames = new HashSet<String>(staticMemberNames);
            Set<String> instMethodNames = new HashSet<String>(instMemberNames);
            fieldMap = new HashMap<String,Field>();
            for(Field fld : fields) {
                final String name = fld.getName();
                Field oldField = (Field) fieldMap.get(name);
                if(oldField == null) {
                    fieldMap.put(name, fld);
                    continue;
                }
                // If this newly reflected field shadows an inherited field,
                // then replace it. Otherwise, since access to the field
                // would be ambiguous from Java, no field should be
                // reflected.
                // For now, the first field found wins, unless another field
                // explicitly shadows it.
                if (oldField.getDeclaringClass().
                        isAssignableFrom(fld.getDeclaringClass()) || (fld.isAccessible() && !oldField.isAccessible()) )
                {
                    fieldMap.put(name, fld);
                }
            }

            instFieldMap = new HashMap<String,Field>();
            staticFieldMap = new HashMap<String,Field>();
            for(Map.Entry<String, Field> entry : fieldMap.entrySet()) {
                final Field fld = entry.getValue();
                final String key = entry.getKey();
                if(Modifier.isStatic(fld.getModifiers())) {
                    staticFieldMap.put(key, fld);
                    staticMemberNames.add(key);
                }else {
                    instFieldMap.put(key, fld);
                    instMemberNames.add(key);
                }
            }

            staticFieldAndMethods= intersection(staticMethodNames,staticFieldMap.keySet());
            instFieldAndMethods = intersection(instMethodNames,instFieldMap.keySet());

            final Map<String, String>[] beanProperties = createBeanProperties(mSignatureMap.values());
            staticBeanProperties = beanProperties[0];
            instBeanProperties = beanProperties[1];
            staticMemberNames.addAll(staticBeanProperties.keySet());
            instMemberNames.addAll(instBeanProperties.keySet());

            for (Map.Entry<MethodSignature,Method> entry: mSignatureMap.entrySet()) {
                 Method method = entry.getValue();
             String name = method.getName();
            final MethodSignature m = entry.getKey();
             // int mods = method.getModifiers();
             final boolean isStatic = m.staticMethod;
             Map<String,Object> ht = isStatic ? staticMembers : members;
            Object value = ht.get(name);
            if (value == null) {
                ht.put(name, method);
            } else {
                ObjArray overloadedMethods;
                if (value instanceof ObjArray) {
                    overloadedMethods = (ObjArray)value;
                } else {
                    if (!(value instanceof Method)) Kit.codeBug();
                    // value should be instance of Method as at this stage
                    // staticMembers and members can only contain methods
                    overloadedMethods = new ObjArray();
                    overloadedMethods.add(value);
                    ht.put(name, overloadedMethods);
                }
                overloadedMethods.add(method);
            }
        }
        }

        Field getStaticField(final String key) {
            return staticFieldMap.get(key);
        }

        Field getInstField(final String key) {
            return instFieldMap.get(key);
        }

        private static final <T> Set<T> intersection(Collection<T> a, Collection<T> b){
            final Set<T> ret = new HashSet<T>();
            for(T v : a) {
                if(b.contains(v)) {
                    ret.add(v);
                }
            }
            return ret;
        }
    }

    private static final Map<String,Object> cache = new ConcurrentHashMap<String, Object>();
    private static final Map<Class,ClassReflectBean>[] cacheArray = new Map[]{
        new ConcurrentHashMap<Class,ClassReflectBean>(),
        new ConcurrentHashMap<Class,ClassReflectBean>(),
        new ConcurrentHashMap<Class,ClassReflectBean>()
    };

    private static Map<Class,ClassReflectBean> getCache(final Class<?> clazz, final boolean includeProtected, final boolean includePrivate){
        int i=0;
        if(includeProtected) {
            i++;
        }
        if(includePrivate) {
            i++;
        }
        return cacheArray[i];
    }

    //rhino_JavaMembers_reflect_cache_on=false for disable
    private static final boolean CACHE_ON = !"false".equals(getProperty("rhino_JavaMembers_reflect_cache_on","true"));

    protected static ClassReflectBean createClassReflectBean(Class<?> clazz,
            boolean includeProtected,
            boolean includePrivate)
    {
        final Map<Class,ClassReflectBean> cache = CACHE_ON ? getCache(clazz, includeProtected, includePrivate) : null;
        ClassReflectBean ret = null;
        if(cache != null) {
            ret = cache.get(clazz);
            if(ret != null) {
                return ret;
            }
        }
        Map<MethodSignature,Method> map = new HashMap<MethodSignature,Method>();
        discoverAccessibleMethods(clazz, map, includeProtected, includePrivate);
        Field[] fields = getAccessibleFields(clazz, includeProtected, includePrivate);
        final NativeJavaMethod constructorMethod = createConstructorMethod(clazz, includePrivate);
        ret = new ClassReflectBean(constructorMethod, map,fields);
        if(cache != null) {
            cache.put(clazz, ret);
        }
        return ret;
    }

    /**
     * Retrieves mapping of methods to accessible methods for a class.
     * In case the class is not public, retrieves methods with same
     * signature as its public methods from public superclasses and
     * interfaces (if they exist). Basically upcasts every method to the
     * nearest accessible method.
     */
    protected static void discoverAccessibleMethods(Class<?> clazz,
            Map<MethodSignature,Method> map, boolean includeProtected,
            boolean includePrivate)
    {
        if (isPublic(clazz.getModifiers()) || includePrivate) {
            try {
                if (includeProtected || includePrivate) {
                    while (clazz != null) {
                        try {
                            Method[] methods = clazz.getDeclaredMethods();
                            for (Method method : methods) {
                                int mods = method.getModifiers();

                                if (isPublic(mods)
                                        || isProtected(mods)
                                        || includePrivate) {
                                    MethodSignature sig = new MethodSignature(method);
                                    if (!map.containsKey(sig)) {
                                        if (includePrivate && !method.isAccessible())
                                            method.setAccessible(true);
                                        map.put(sig, method);
                                    }
                                }
                            }
                            clazz = clazz.getSuperclass();
                        } catch (SecurityException e) {
                            // Some security settings (i.e., applets) disallow
                            // access to Class.getDeclaredMethods. Fall back to
                            // Class.getMethods.
                            Method[] methods = clazz.getMethods();
                            for (Method method : methods) {
                                MethodSignature sig = new MethodSignature(method);
                                if (!map.containsKey(sig))
                                    map.put(sig, method);
                            }
                            break; // getMethods gets superclass methods, no
                                   // need to loop any more
                        }
                    }
                } else {
                    Method[] methods = clazz.getMethods();
                    for (Method method : methods) {
                        MethodSignature sig = new MethodSignature(method);
                        // Array may contain methods with same signature but different return value!
                        if (!map.containsKey(sig))
                            map.put(sig, method);
                    }
                }
                return;
            } catch (SecurityException e) {
                Context.reportWarning(
                        "Could not discover accessible methods of class " +
                            clazz.getName() + " due to lack of privileges, " +
                            "attemping superclasses/interfaces.");
                // Fall through and attempt to discover superclass/interface
                // methods
            }
        }

        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> intface : interfaces) {
            discoverAccessibleMethods(intface, map, includeProtected,
                    includePrivate);
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            discoverAccessibleMethods(superclass, map, includeProtected,
                    includePrivate);
        }
    }

    static final class MethodSignature
    {
        private final String name;
        private final Class<?>[] args;
        private final boolean staticMethod;
       // private final boolean privateMethod;
        private final Method method;


       private MethodSignature(Method method)
        {
           this.method = method;
            name = method.getName();
            args = method.getParameterTypes();
            int mods = method.getModifiers();
            staticMethod = Modifier.isStatic(mods);
         //   privateMethod = Modifier.isPrivate(mods);
        }

        @Override
        public boolean equals(Object o)
        {
            if(o instanceof MethodSignature)
            {
                MethodSignature ms = (MethodSignature)o;
                return ms.name.equals(name) && Arrays.equals(args, ms.args);
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return name.hashCode() ^ args.length;
        }
    }


    private static Map<String,String>[] createBeanProperties(Iterable<Method> methods) {
          final Map<String,String> cache1 = new HashMap<String,String>();
        final Map<String,String> cache2 = new HashMap<String,String>();
        // Now, For each member, make "bean" properties.
        for (Method m: methods) {
            String name = m.getName();
            // Is this a getter?
            boolean memberIsGetMethod = name.startsWith("get");
            boolean memberIsSetMethod = name.startsWith("set");
            boolean memberIsIsMethod = name.startsWith("is");
            if (memberIsGetMethod || memberIsIsMethod
                    || memberIsSetMethod) {
                // Double check name component.
                String nameComponent
                    = name.substring(memberIsIsMethod ? 2 : 3);
                if (nameComponent.length() == 0)
                    continue;

                // Make the bean property name.
                String beanPropertyName = nameComponent;
                char ch0 = nameComponent.charAt(0);
                if (Character.isUpperCase(ch0)) {
                    if (nameComponent.length() == 1) {
                        beanPropertyName = nameComponent.toLowerCase();
                    } else {
                        char ch1 = nameComponent.charAt(1);
                        if (!Character.isUpperCase(ch1)) {
                            beanPropertyName = Character.toLowerCase(ch0)
                                               +nameComponent.substring(1);
                        }
                    }
                }
                if( Modifier.isStatic(m.getModifiers())) {
                     cache1.put(beanPropertyName, nameComponent);
                }else {
                     cache2.put(beanPropertyName, nameComponent);
                }
            }
        }
        return new Map[] {cache1,cache2};
    }
    protected static NativeJavaMethod toNativeJavaMethod(Scriptable scope, Object value) {
        MemberBox[] methodBoxes;
        if (value instanceof Method) {
            methodBoxes = new MemberBox[1];
            methodBoxes[0] = new MemberBox((Method)value);
        } else {
            ObjArray overloadedMethods = (ObjArray)value;
            int N = overloadedMethods.size();
            if (N < 2) Kit.codeBug();
            methodBoxes = new MemberBox[N];
            for (int i = 0; i != N; ++i) {
                Method method = (Method)overloadedMethods.get(i);
                methodBoxes[i] = new MemberBox(method);
            }
        }
        NativeJavaMethod fun = new NativeJavaMethod(methodBoxes);
        if (scope != null) {
            ScriptRuntime.setFunctionProtoAndParent(fun, scope);
        }
        return fun;
    }


    protected static NativeJavaMethod createConstructorMethod(Class cl, boolean includePrivate) {
        NativeJavaMethod ctors;
        Constructor<?>[] constructors = getAccessibleConstructors(cl,includePrivate);
            MemberBox[] ctorMembers = new MemberBox[constructors.length];
            for (int i = 0; i != constructors.length; ++i) {
                ctorMembers[i] = new MemberBox(constructors[i]);
            }
            ctors = new NativeJavaMethod(ctorMembers, cl.getSimpleName());
        return ctors;
    }


    protected static Constructor<?>[] getAccessibleConstructors(Class cl,boolean includePrivate)
    {
      // The JVM currently doesn't allow changing access on java.lang.Class
      // constructors, so don't try
      if (includePrivate && cl != ScriptRuntime.ClassClass) {
          try {
              Constructor<?>[] cons = cl.getDeclaredConstructors();
              AccessibleObject.setAccessible(cons, true);

              return cons;
          } catch (SecurityException e) {
              // Fall through to !includePrivate case
              Context.reportWarning("Could not access constructor " +
                    " of class " + cl.getName() +
                    " due to lack of privileges.");
          }
      }
      return cl.getConstructors();
    }

    protected static Field[] getAccessibleFields(Class cl,boolean includeProtected,
                                        boolean includePrivate) {
        if (includePrivate || includeProtected) {
            try {
                List<Field> fieldsList = new ArrayList<Field>();
                Class<?> currentClass = cl;

                while (currentClass != null) {
                    // get all declared fields in this class, make them
                    // accessible, and save
                    Field[] declared = currentClass.getDeclaredFields();
                    for (Field field : declared) {
                        int mod = field.getModifiers();
                        if (includePrivate || isPublic(mod) || isProtected(mod)) {
                            if (!field.isAccessible())
                                field.setAccessible(true);
                            fieldsList.add(field);
                        }
                    }
                    // walk up superclass chain.  no need to deal specially with
                    // interfaces, since they can't have fields
                    currentClass = currentClass.getSuperclass();
                }

                return fieldsList.toArray(new Field[fieldsList.size()]);
            } catch (SecurityException e) {
                // fall through to !includePrivate case
            }
        }
        return cl.getFields();
    }

    protected static MemberBox extractGetMethod(MemberBox[] methods,
                                              boolean isStatic)
    {
        // Inspect the list of all MemberBox for the only one having no
        // parameters
        for (MemberBox method : methods) {
            // Does getter method have an empty parameter list with a return
            // value (eg. a getSomething() or isSomething())?
            if (method.argTypes.length == 0 && (!isStatic || method.isStatic())) {
                Class<?> type = method.method().getReturnType();
                if (type != Void.TYPE) {
                    return method;
                }
                break;
            }
        }
        return null;
    }

    protected static MemberBox extractSetMethod(Class<?> type, MemberBox[] methods,
                                              boolean isStatic)
    {
        //
        // Note: it may be preferable to allow NativeJavaMethod.findFunction()
        //       to find the appropriate setter; unfortunately, it requires an
        //       instance of the target arg to determine that.
        //

        // Make two passes: one to find a method with direct type assignment,
        // and one to find a widening conversion.
        for (int pass = 1; pass <= 2; ++pass) {
            for (MemberBox method : methods) {
                if (!isStatic || method.isStatic()) {
                    Class<?>[] params = method.argTypes;
                    if (params.length == 1) {
                        if (pass == 1) {
                            if (params[0] == type) {
                                return method;
                            }
                        } else {
                            if (pass != 2) Kit.codeBug();
                            if (params[0].isAssignableFrom(type)) {
                                return method;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    protected static MemberBox extractSetMethod(MemberBox[] methods,
                                              boolean isStatic)
    {

        for (MemberBox method : methods) {
            if (!isStatic || method.isStatic()) {
                if (method.method().getReturnType() == Void.TYPE) {
                    if (method.argTypes.length == 1) {
                        return method;
                    }
                }
            }
        }
        return null;
    }
    static interface JavaMembersCreateor {
        JavaMembers createJavaMembers(Scriptable scope, Class<?> cl, boolean includeProtected);
    }
    static final class OldCreator implements JavaMembersCreateor {

        @Override
        public JavaMembers createJavaMembers(Scriptable scope, Class<?> cl, boolean includeProtected) {
            return new JavaMembers4NoCacheNoLazy(scope, cl, includeProtected);
        }
    }

    static final class NewCreator implements JavaMembersCreateor {

        @Override
        public JavaMembers createJavaMembers(Scriptable scope, Class<?> cl, boolean includeProtected) {
            return new JavaMembers4CacheAndLazyWay(scope, cl, includeProtected);
        }
    }

    private static final JavaMembersCreateor CREATOR;
    static {
        //rhino_creator_4_JavaMembers=old
        final String creatorType = getProperty("rhino_creator_4_JavaMembers", "new");
        CREATOR = "old".equals(creatorType) ?  new OldCreator() : new NewCreator();
    }
    static JavaMembers lookupClass(Scriptable scope, Class<?> dynamicType,
                                   Class<?> staticType, boolean includeProtected)
    {
        JavaMembers members;
        ClassCache cache = ClassCache.get(scope);
        Map<Class<?>,JavaMembers> ct = cache.getClassCacheMap();

        Class<?> cl = dynamicType;
        for (;;) {
            members = ct.get(cl);
            if (members != null) {
                if (cl != dynamicType) {
                    // member lookup for the original class failed because of
                    // missing privileges, cache the result so we don't try again
                    ct.put(dynamicType, members);
                }
                return members;
            }
            try {
                members = CREATOR.createJavaMembers(cache.getAssociatedScope(), cl,
                        includeProtected);
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
            ct.put(cl, members);
            if (cl != dynamicType) {
                // member lookup for the original class failed because of
                // missing privileges, cache the result so we don't try again
                ct.put(dynamicType, members);
            }
        }
        return members;
    }
}

class BeanProperty
{
    BeanProperty(MemberBox getter, MemberBox setter, NativeJavaMethod setters)
    {
        this.getter = getter;
        this.setter = setter;
        this.setters = setters;
    }

    MemberBox getter;
    MemberBox setter;
    NativeJavaMethod setters;
}

class FieldAndMethods extends NativeJavaMethod
{
    static final long serialVersionUID = -9222428244284796755L;

    FieldAndMethods(Scriptable scope, MemberBox[] methods, Field field)
    {
        super(methods);
        this.field = field;
        setParentScope(scope);
        setPrototype(ScriptableObject.getFunctionPrototype(scope));
    }

    @Override
    public Object getDefaultValue(Class<?> hint)
    {
        if (hint == ScriptRuntime.FunctionClass)
            return this;
        Object rval;
        Class<?> type;
        try {
            rval = field.get(javaObject);
            type = field.getType();
        } catch (IllegalAccessException accEx) {
            throw Context.reportRuntimeError1(
                "msg.java.internal.private", field.getName());
        }
        Context cx  = Context.getContext();
        rval = cx.getWrapFactory().wrap(cx, this, rval, type);
        if (rval instanceof Scriptable) {
            rval = ((Scriptable) rval).getDefaultValue(hint);
        }
        return rval;
    }

    Field field;
    Object javaObject;
}
