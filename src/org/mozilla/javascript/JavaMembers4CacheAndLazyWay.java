 package org.mozilla.javascript;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class JavaMembers4CacheAndLazyWay extends JavaMembers
{
    JavaMembers4CacheAndLazyWay(Scriptable scope, Class<?> cl)
    {
        this(scope, cl, false);
    }

    private FieldAndMethods copyFieldAndMethod(Scriptable scope, Object javaObject, FieldAndMethods fam) {
        FieldAndMethods famNew = new FieldAndMethods(scope, fam.methods,
                                                     fam.field);
        famNew.javaObject = javaObject;
        return famNew;
    }

    //private final boolean includeProtected;
    private final boolean includePrivate;
    private final ClassReflectBean cfCache;
    //rhino_JavaMembers_lazyInit=true for enable
    private static final boolean lazyInit = "true".equals(getProperty("rhino_JavaMembers_lazyInit","false"));
    private final Scriptable javaMemberScope;
    JavaMembers4CacheAndLazyWay(Scriptable scope, Class<?> cl, boolean includeProtected)
    {
        //this.includeProtected = includeProtected;
        try {
            this.javaMemberScope = scope;
            Context cx = ContextFactory.getGlobal().enterContext();
            ClassShutter shutter = cx.getClassShutter();
            if (shutter != null && !shutter.visibleToScripts(cl.getName())) {
                throw Context.reportRuntimeError1("msg.access.prohibited",
                                                  cl.getName());
            }
            this.members = new HashMap<String,Object>();
            this.staticMembers = new HashMap<String,Object>();
            this.cl = cl;
            includePrivate = cx.hasFeature(
                    Context.FEATURE_ENHANCED_JAVA_ACCESS);
            //lazyInit = true;
            cfCache = reflect(scope, includeProtected, includePrivate);
        } finally {
            Context.exit();
        }
    }

    boolean has(String name, boolean isStatic)
    {
        if (cfCache.has(name, isStatic)) {
            return true;
        }
        return findExplicitFunction(name, isStatic) != null;
    }

    Object get(Scriptable scope, String name, Object javaObject,
               boolean isStatic)
    {
        Object member = getMember2(scope, name, isStatic);
        if (member == null) {
            member = this.getExplicitFunction(scope, name,
                                              javaObject, isStatic);
            if (member == null)
                return Scriptable.NOT_FOUND;
        }
        if (member instanceof Scriptable) {
            return member;
        }
        Context cx = Context.getContext();
        Object rval;
        Class<?> type;
        try {
            if (member instanceof BeanProperty) {
                BeanProperty bp = (BeanProperty) member;
                if (bp.getter == null)
                    return Scriptable.NOT_FOUND;
                rval = bp.getter.invoke(javaObject, Context.emptyArgs);
                type = bp.getter.method().getReturnType();
            } else {
                Field field = (Field) member;
                rval = field.get(isStatic ? null : javaObject);
                type = field.getType();
            }
        } catch (Exception ex) {
            throw Context.throwAsScriptRuntimeEx(ex);
        }
        // Need to wrap the object before we return it.
        scope = ScriptableObject.getTopLevelScope(scope);
        return cx.getWrapFactory().wrap(cx, scope, rval, type);
    }

    private Object getMember2(Scriptable scope, String name, boolean isStatic) {
        Object member = getMember(scope, name, isStatic);
        if (member == null && !isStatic) {
            // Try to get static member from instance (LC3)
            member = getMember(scope, name, true);
            if( member == null) {
                final Map<String,Object> ht = members;
                return null;
            }
        }
        return member == Scriptable.NOT_FOUND ? null : member;
    }

    private final Object getMember(final Scriptable scope, final String name, final boolean isStatic) {
        final Map<String,Object> ht = isStatic ? staticMembers : members;
        Object member = ht.get(name);
        if(lazyInit && member == null) {
            final Object m1  = initFieldAndMethod(name,ht,isStatic);
            Map<String,String> props = isStatic ? cfCache.staticBeanProperties : cfCache.instBeanProperties;
            final String nameComponent = props.get(name);
            if(nameComponent != null) {
                member = initBeanProperty(name, nameComponent, ht, isStatic);
                if(member == null) {
                    member =m1;
                }
               } else {
                   member = m1;
               }

            if (member != null) {
                ht.put(name, member);
            }
        }
        return member;
    }

    private Object initFieldAndMethod(final String name, Map<String,Object> ht,final boolean isStatic) {
        Object member;
        member = isStatic ? cfCache.getStaticField(name) : cfCache.getInstField(name);
        final Map<String, Object> mbers = cfCache.getMembers(isStatic);
        final Object value = mbers.get(name);
        NativeJavaMethod jm = value == null ? null : toNativeJavaMethod(javaMemberScope, value);
        if(jm != null) {
            if(member != null) {
                Field fld = (Field)member;
                member = initFieldAndMethods(javaMemberScope,fld, fld.getName(), Modifier.isStatic(fld.getModifiers()), jm);
            }else {
                member = jm;
            }
        }
        if(member != null) {
            ht.put(name, member);
        }
        return member ;
    }

    void put(Scriptable scope, String name, Object javaObject,
             Object value, boolean isStatic)
    {
        Object member = getMember2(scope, name, isStatic);
        if (member == null)
            throw reportMemberNotFound(name);
        if (member instanceof FieldAndMethods) {
            FieldAndMethods fam = (FieldAndMethods) member;
            member = fam.field;
        }

        // Is this a bean property "set"?
        if (member instanceof BeanProperty) {
            BeanProperty bp = (BeanProperty)member;
            if (bp.setter == null) {
                throw reportMemberNotFound(name);
            }
            // If there's only one setter or if the value is null, use the
            // main setter. Otherwise, let the NativeJavaMethod decide which
            // setter to use:
            if (bp.setters == null || value == null) {
                Class<?> setType = bp.setter.argTypes[0];
                Object[] args = { Context.jsToJava(value, setType) };
                try {
                    bp.setter.invoke(javaObject, args);
                } catch (Exception ex) {
                  throw Context.throwAsScriptRuntimeEx(ex);
                }
            } else {
                Object[] args = { value };
                bp.setters.call(Context.getContext(),
                                ScriptableObject.getTopLevelScope(scope),
                                scope, args);
            }
        }
        else {
            if (!(member instanceof Field)) {
                String str = (member == null) ? "msg.java.internal.private"
                                              : "msg.java.method.assign";
                throw Context.reportRuntimeError1(str, name);
            }
            Field field = (Field)member;
            Object javaValue = Context.jsToJava(value, field.getType());
            try {
                field.set(javaObject, javaValue);
            } catch (IllegalAccessException accessEx) {
                if ((field.getModifiers() & Modifier.FINAL) != 0) {
                    // treat Java final the same as JavaScript [[READONLY]]
                    return;
                }
                throw Context.throwAsScriptRuntimeEx(accessEx);
            } catch (IllegalArgumentException argEx) {
                throw Context.reportRuntimeError3(
                    "msg.java.internal.field.type",
                    value.getClass().getName(), field,
                    javaObject.getClass().getName());
            }
        }
    }

    Object[] getIds(final boolean isStatic)
    {
      return cfCache.getIds(isStatic);
    }


    Map<String,FieldAndMethods> getFieldAndMethodsObjects(Scriptable scope,
            Object javaObject, boolean isStatic)
    {
        Set<String> names = isStatic ? cfCache.staticFieldAndMethods : cfCache.instFieldAndMethods;
        int len = names.size();
          if (names.size()<1) {
            return null;
        }
        Map<String,FieldAndMethods> result = new HashMap<String,FieldAndMethods>(len);
        for (String name: names) {
            final Object member = getMember(this.javaMemberScope, name, isStatic);
            if(member == null) {
                continue;
            }
            FieldAndMethods fam = (FieldAndMethods)member;
            FieldAndMethods famNew = copyFieldAndMethod(scope, javaObject, fam);
            result.put(fam.field.getName(), famNew);
        }
        return result;
    }

    private MemberBox findExplicitFunction(String name, boolean isStatic)
    {
        int sigStart = name.indexOf('(');
        if (sigStart < 0) { return null; }

        MemberBox[] methodsOrCtors = null;
        boolean isCtor = (isStatic && sigStart == 0);

        if (isCtor) {
            // Explicit request for an overloaded constructor
            methodsOrCtors = ctors.methods;
        } else {

            // Explicit request for an overloaded method
            String trueName = name.substring(0,sigStart);
            Object obj = getMember(javaMemberScope, trueName, isStatic);
            if (obj instanceof NativeJavaMethod) {
                NativeJavaMethod njm = (NativeJavaMethod)obj;
                methodsOrCtors = njm.methods;
            }
        }

        if (methodsOrCtors != null) {
            for (MemberBox methodsOrCtor : methodsOrCtors) {
                Class<?>[] type = methodsOrCtor.argTypes;
                String sig = liveConnectSignature(type);
                if (sigStart + sig.length() == name.length()
                        && name.regionMatches(sigStart, sig, 0, sig.length()))
                {
                    return methodsOrCtor;
                }
            }
        }

        return null;
    }

    private Object getExplicitFunction(Scriptable scope, String name,
                                       Object javaObject, boolean isStatic)
    {
        Map<String,Object> ht = isStatic ? staticMembers : members;
        Object member = null;
        MemberBox methodOrCtor = findExplicitFunction(name, isStatic);

        if (methodOrCtor != null) {
            Scriptable prototype =
                ScriptableObject.getFunctionPrototype(scope);

            if (methodOrCtor.isCtor()) {
                NativeJavaConstructor fun =
                    new NativeJavaConstructor(methodOrCtor);
                fun.setPrototype(prototype);
                member = fun;
                ht.put(name, fun);
            } else {
                final String trueName = methodOrCtor.getName();
                member = getMember(javaMemberScope, trueName, isStatic);

                if (member instanceof NativeJavaMethod &&
                    ((NativeJavaMethod)member).methods.length > 1 ) {
                    NativeJavaMethod fun =
                        new NativeJavaMethod(methodOrCtor, name);
                    fun.setPrototype(prototype);
                    ht.put(name, fun);
                    member = fun;
                }
            }
        }

        return member;
    }

    protected void initField(Scriptable scope, Field field) {
        String name = field.getName();
        int mods = field.getModifiers();
        try {
            boolean isStatic = Modifier.isStatic(mods);
            Map<String,Object> ht = isStatic ? staticMembers : members;
            Object member = ht.get(name);
            if (member == null) {
                ht.put(name, field);
            } else if (member instanceof NativeJavaMethod) {
                final FieldAndMethods fam = initFieldAndMethods(scope, field, name, isStatic, member);
                ht.put(name, fam);
            } else if (member instanceof Field) {
                Field oldField = (Field) member;
                // If this newly reflected field shadows an inherited field,
                // then replace it. Otherwise, since access to the field
                // would be ambiguous from Java, no field should be
                // reflected.
                // For now, the first field found wins, unless another field
                // explicitly shadows it.
                if (oldField.getDeclaringClass().
                        isAssignableFrom(field.getDeclaringClass()))
                {
                    ht.put(name, field);
                }
            } else {
                // "unknown member type"
                Kit.codeBug();
            }
        } catch (SecurityException e) {
            // skip this field
            Context.reportWarning("Could not access field "
                    + name + " of class " + cl.getName() +
                    " due to lack of privileges.");
        }
    }

    private FieldAndMethods initFieldAndMethods(Scriptable scope, Field field, String name, boolean isStatic,
            Object member) {
        NativeJavaMethod method = (NativeJavaMethod) member;
        FieldAndMethods fam
            = new FieldAndMethods(scope, method.methods, field);
        Map<String,FieldAndMethods> fmht = isStatic ? staticFieldAndMethods
                                  : fieldAndMethods;
        if (fmht == null) {
            fmht = new HashMap<String,FieldAndMethods>();
            if (isStatic) {
                staticFieldAndMethods = fmht;
            } else {
                fieldAndMethods = fmht;
            }
        }
        fmht.put(name, fam);
        return fam;
    }

    private ClassReflectBean reflect(Scriptable scope,
             boolean includeProtected, boolean includePrivate)
    {
        // We reflect methods first, because we want overloaded field/method
        // names to be allocated to the NativeJavaMethod before the field
        // gets in the way.
        final ClassReflectBean cfCache = createClassReflectBean(cl, includeProtected,
            includePrivate);
        if(!lazyInit) {
            // replace Method instances by wrapped NativeJavaMethod objects
            // first in staticMembers and then in members
            for (int tableCursor = 0; tableCursor != 2; ++tableCursor) {
                boolean isStatic = (tableCursor == 0);
                Map<String,Object> ht = isStatic ? staticMembers : members;
                final Map<String, Object> mbers = cfCache.getMembers(isStatic);
                for (Map.Entry<String, Object> entry: mbers.entrySet()) {
                    Object value = entry.getValue();
                    NativeJavaMethod fun = toNativeJavaMethod(scope, value);
                    ht.put(entry.getKey(), fun);
                }
            }
            // Reflect fields.
            Field[] fields =  cfCache.fields;
            for (Field field : fields) {
                initField(scope, field);
            }
            // Create bean properties from corresponding get/set methods first for
            // static members and then for instance members
            for (int tableCursor = 0; tableCursor != 2; ++tableCursor) {
                boolean isStatic = (tableCursor == 0);
                Map<String,Object> ht = isStatic ? staticMembers : members;
                Map<String,BeanProperty> toAdd = new HashMap<String,BeanProperty>();
                for(Map.Entry<String, String> entry : (isStatic ? cfCache.staticBeanProperties : cfCache.instBeanProperties ).entrySet()) {
                    final String beanPropertyName = entry.getKey();
                    final String nameComponent = entry.getValue();
                    // If we already have a member by this name, don't do this
                    // property.
                    if (toAdd.containsKey(beanPropertyName))
                        continue;
            
                    BeanProperty bp = initBeanProperty(beanPropertyName,nameComponent,ht,isStatic);
                    if( bp != null) {
                        toAdd.put(beanPropertyName, bp);
                    }
                }
                for(Map.Entry<String,BeanProperty> entry : toAdd.entrySet()) {
                    ht.put(entry.getKey(), entry.getValue());  // Add the new bean properties.
                }
            }
        }
        // Reflect constructors
        ctors = cfCache.constructorMethod;
        return cfCache;
    }

    private BeanProperty initBeanProperty(final String beanPropertyName,String nameComponent, Map<String,Object> ht,boolean isStatic){
        Object v = ht.get(beanPropertyName);
        if (v != null) {
            // A private field shouldn't mask a public getter/setter
            if (!includePrivate || !(v instanceof Member) ||
                !Modifier.isPrivate(((Member)v).getModifiers()))
            {
                return null;
            }
        }

        // Find the getter method, or if there is none, the is-
        // method.
        MemberBox getter = null;
        getter = findGetter(isStatic, ht, "get", nameComponent);
        // If there was no valid getter, check for an is- method.
        if (getter == null) {
            getter = findGetter(isStatic, ht, "is", nameComponent);
        }

        // setter
        MemberBox setter = null;
        NativeJavaMethod setters = null;
        String setterName = "set".concat(nameComponent);
            // Is this value a method?
            Object member = ht.get(setterName);
            if( member == null && lazyInit) {
                member = initFieldAndMethod(setterName,ht, isStatic);
            }
            if (member instanceof NativeJavaMethod) {
                NativeJavaMethod njmSet = (NativeJavaMethod)member;
                if (getter != null) {
                    // We have a getter. Now, do we have a matching
                    // setter?
                    Class<?> type = getter.method().getReturnType();
                    setter = extractSetMethod(type, njmSet.methods,
                                                isStatic);
                } else {
                    // No getter, find any set method
                    setter = extractSetMethod(njmSet.methods,
                                                isStatic);
                }
                if (njmSet.methods.length > 1) {
                    setters = njmSet;
                }
            }
        // Make the property.
        BeanProperty bp = new BeanProperty(getter, setter, setters);
        return bp;
    }

    private MemberBox findGetter(boolean isStatic, Map<String,Object> ht
        , String prefix, String propertyName)
    {
        String getterName = prefix.concat(propertyName);
        // Check that the getter is a method.
        Object member = ht.get(getterName);
        if(member == null && lazyInit) {
            member=initFieldAndMethod(getterName,ht, isStatic);
        }
        if (member instanceof NativeJavaMethod) {
            NativeJavaMethod njmGet = (NativeJavaMethod) member;
            return extractGetMethod(njmGet.methods, isStatic);
        }
        return null;
    }
}