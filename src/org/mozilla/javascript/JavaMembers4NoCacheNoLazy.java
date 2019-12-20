 package org.mozilla.javascript;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.JavaMembers.MethodSignature;

class JavaMembers4NoCacheNoLazy extends JavaMembers
{
    JavaMembers4NoCacheNoLazy(Scriptable scope, Class<?> cl)
    {
        this(scope, cl, false);
    }

    JavaMembers4NoCacheNoLazy(Scriptable scope, Class<?> cl, boolean includeProtected)
    {
        try {
            Context cx = ContextFactory.getGlobal().enterContext();
            ClassShutter shutter = cx.getClassShutter();
            if (shutter != null && !shutter.visibleToScripts(cl.getName())) {
                throw Context.reportRuntimeError1("msg.access.prohibited",
                                                  cl.getName());
            }
            this.members = new HashMap<String,Object>();
            this.staticMembers = new HashMap<String,Object>();
            this.cl = cl;
            boolean includePrivate = cx.hasFeature(
                    Context.FEATURE_ENHANCED_JAVA_ACCESS);
            reflect(scope, includeProtected, includePrivate);
        } finally {
            Context.exit();
        }
    }

    boolean has(String name, boolean isStatic)
    {
        Map<String,Object> ht = isStatic ? staticMembers : members;
        Object obj = ht.get(name);
        if (obj != null) {
            return true;
        }
        return findExplicitFunction(name, isStatic) != null;
    }

    Object get(Scriptable scope, String name, Object javaObject,
               boolean isStatic)
    {
        Map<String,Object> ht = isStatic ? staticMembers : members;
        Object member = ht.get(name);
        if (!isStatic && member == null) {
            // Try to get static member from instance (LC3)
            member = staticMembers.get(name);
        }
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

    void put(Scriptable scope, String name, Object javaObject,
             Object value, boolean isStatic)
    {
        Map<String,Object> ht = isStatic ? staticMembers : members;
        Object member = ht.get(name);
        if (!isStatic && member == null) {
            // Try to get static member from instance (LC3)
            member = staticMembers.get(name);
        }
        if (member == null)
            throw reportMemberNotFound(name);
        if (member instanceof FieldAndMethods) {
            FieldAndMethods fam = (FieldAndMethods) ht.get(name);
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

    Object[] getIds(boolean isStatic)
    {
        Map<String,Object> map = isStatic ? staticMembers : members;
        return map.keySet().toArray(new Object[map.size()]);
    }

    private MemberBox findExplicitFunction(String name, boolean isStatic)
    {
        int sigStart = name.indexOf('(');
        if (sigStart < 0) { return null; }

        Map<String,Object> ht = isStatic ? staticMembers : members;
        MemberBox[] methodsOrCtors = null;
        boolean isCtor = (isStatic && sigStart == 0);

        if (isCtor) {
            // Explicit request for an overloaded constructor
            methodsOrCtors = ctors.methods;
        } else {
            // Explicit request for an overloaded method
            String trueName = name.substring(0,sigStart);
            Object obj = ht.get(trueName);
            if (!isStatic && obj == null) {
                // Try to get static member from instance (LC3)
                obj = staticMembers.get(trueName);
            }
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
                String trueName = methodOrCtor.getName();
                member = ht.get(trueName);

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

    private void reflect(Scriptable scope,
            boolean includeProtected, boolean includePrivate)
    {
        // We reflect methods first, because we want overloaded field/method
        // names to be allocated to the NativeJavaMethod before the field
        // gets in the way.
        
        Method[] methods = discoverAccessibleMethods(cl, includeProtected,
                                                includePrivate);
        for (Method method : methods) {
            int mods = method.getModifiers();
            boolean isStatic = Modifier.isStatic(mods);
            Map<String,Object> ht = isStatic ? staticMembers : members;
            String name = method.getName();
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

        // replace Method instances by wrapped NativeJavaMethod objects
        // first in staticMembers and then in members
        for (int tableCursor = 0; tableCursor != 2; ++tableCursor) {
            boolean isStatic = (tableCursor == 0);
            Map<String,Object> ht = isStatic ? staticMembers : members;
            for (Map.Entry<String, Object> entry: ht.entrySet()) {
               MemberBox[] methodBoxes;
               Object value = entry.getValue();
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
               ht.put(entry.getKey(), fun);
            }
        }

        // Reflect fields.
        Field[] fields = getAccessibleFields(cl, includeProtected, includePrivate);
        for (Field field : fields) {
            String name = field.getName();
            int mods = field.getModifiers();
            try {
               boolean isStatic = Modifier.isStatic(mods);
               Map<String,Object> ht = isStatic ? staticMembers : members;
               Object member = ht.get(name);
               if (member == null) {
                   ht.put(name, field);
               } else if (member instanceof NativeJavaMethod) {
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
        
        // Create bean properties from corresponding get/set methods first for
        // static members and then for instance members
        for (int tableCursor = 0; tableCursor != 2; ++tableCursor) {
            boolean isStatic = (tableCursor == 0);
            Map<String,Object> ht = isStatic ? staticMembers : members;
            
            Map<String,BeanProperty> toAdd = new HashMap<String,BeanProperty>();
    
            // Now, For each member, make "bean" properties.
            for (String name: ht.keySet()) {
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
            
                   // If we already have a member by this name, don't do this
                   // property.
                   if (toAdd.containsKey(beanPropertyName))
                       continue;
                   Object v = ht.get(beanPropertyName);
                   if (v != null) {
                       // A private field shouldn't mask a public getter/setter
                       if (!includePrivate || !(v instanceof Member) ||
                           !Modifier.isPrivate(((Member)v).getModifiers()))
            
                       {
                           continue;
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
            
                   if (ht.containsKey(setterName)) {
                       // Is this value a method?
                       Object member = ht.get(setterName);
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
                   }
                   // Make the property.
                   BeanProperty bp = new BeanProperty(getter, setter,
                                                      setters);
                   toAdd.put(beanPropertyName, bp);
               }
            }
    
            // Add the new bean properties.
            for (String key: toAdd.keySet()) {
               Object value = toAdd.get(key);
               ht.put(key, value);
            }
        }

        // Reflect constructors
        Constructor<?>[] constructors = getAccessibleConstructors(cl, includePrivate);
        MemberBox[] ctorMembers = new MemberBox[constructors.length];
        for (int i = 0; i != constructors.length; ++i) {
            ctorMembers[i] = new MemberBox(constructors[i]);
        }
        ctors = new NativeJavaMethod(ctorMembers, cl.getSimpleName());
    }

    Map<String,FieldAndMethods> getFieldAndMethodsObjects(Scriptable scope,
            Object javaObject, boolean isStatic)
    {
        Map<String,FieldAndMethods> ht = isStatic ? staticFieldAndMethods : fieldAndMethods;
        if (ht == null)
            return null;
        int len = ht.size();
        Map<String,FieldAndMethods> result = new HashMap<String,FieldAndMethods>(len);
        for (FieldAndMethods fam: ht.values()) {
            FieldAndMethods famNew = new FieldAndMethods(scope, fam.methods,
                                                         fam.field);
            famNew.javaObject = javaObject;
            result.put(fam.field.getName(), famNew);
        }
        return result;
    }

    private MemberBox findGetter(boolean isStatic, Map<String,Object> ht, String prefix,
            String propertyName)
    {
        String getterName = prefix.concat(propertyName);
        if (ht.containsKey(getterName)) {
            //Check that the getter is a method.
            Object member = ht.get(getterName);
            if (member instanceof NativeJavaMethod) {
                NativeJavaMethod njmGet = (NativeJavaMethod) member;
                return extractGetMethod(njmGet.methods, isStatic);
            }
        }
        return null;
    }


    /**
     * Retrieves mapping of methods to accessible methods for a class.
     * In case the class is not public, retrieves methods with same
     * signature as its public methods from public superclasses and
     * interfaces (if they exist). Basically upcasts every method to the
     * nearest accessible method.
     */
    private  static Method[] discoverAccessibleMethods(Class<?> clazz,
                                                      boolean includeProtected,
                                                      boolean includePrivate)
    {
        Map<MethodSignature,Method> map = new HashMap<MethodSignature,Method>();
        discoverAccessibleMethods(clazz, map, includeProtected, includePrivate);
        return map.values().toArray(new Method[map.size()]);
    }
}