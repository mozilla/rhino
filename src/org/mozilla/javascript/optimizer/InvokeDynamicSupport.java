package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.classy.ClassyLayout;
import org.mozilla.javascript.classy.ClassyLayout.Mapping;
import org.mozilla.javascript.classy.ClassyScriptable;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

public class InvokeDynamicSupport {

    static class CachingCallSite extends MutableCallSite {
        final Lookup lookup;

        CachingCallSite(Lookup lookup, String name, MethodType type) {
            super(type);
            this.lookup = lookup;
        }
    }

    public static CallSite bootstrapProp0Call(MethodHandles.Lookup lookup,
                                              String name, MethodType type) {
        CachingCallSite callSite = new CachingCallSite(lookup, name, type);
        MethodHandle check = INITCALL.bindTo(callSite);
        check = check.asType(type);

        callSite.setTarget(check);
        return callSite;
    }

    public static CallSite bootstrapGetObjectProp(MethodHandles.Lookup lookup,
                                                  String name, MethodType type) {
        CachingCallSite callSite = new CachingCallSite(lookup, name, type);
        MethodHandle check = INITOBJPROP.bindTo(callSite);
        check = check.asType(type);

        callSite.setTarget(check);
        return callSite;
    }
    
    public static CallSite bootstrapSetObjectProp(MethodHandles.Lookup lookup,
    		                                      String name, MethodType type){
    	CachingCallSite callSite = new CachingCallSite(lookup, name, type);
    	MethodHandle check = SETOBJPROP.bindTo(callSite);
    	check = check.asType(type);

    	callSite.setTarget(check);
    	return callSite;
    }
    

    public static boolean checkClass(Class<?> clazz, Object receiver) {
        return receiver instanceof NativeJavaObject &&
                ((NativeJavaObject)receiver).unwrap().getClass() == clazz;
    }

    public static boolean checkLayout(ClassyLayout layout, Object receiver) {
        return receiver instanceof ClassyScriptable &&
                ((ClassyScriptable)receiver).getLayout() == layout;
    }

    public static Object unwrapObject(Object obj) {
        return ((Wrapper)obj).unwrap();
    }

    public static Object callProp0(CachingCallSite callSite, Object value,
                               String property, Context cx, Scriptable scope)
            throws Throwable {
        if (value.getClass() != NativeJavaObject.class) {
            callSite.setTarget(CALLPROP0_FALLBACK);
            return CALLPROP0_FALLBACK.invoke(value, property, cx, scope);
        }

        Object javaObject = ((NativeJavaObject)value).unwrap();
        Class<?> javaClass = javaObject.getClass();
        MethodHandle target, localTarget;
        target = callSite.lookup.unreflect(javaClass.getMethod(property));
        target = localTarget = target.asType(MethodType.genericMethodType(1));
        target = MethodHandles.filterArguments(target, 0, UNWRAP);
        target = MethodHandles.dropArguments(target, 1,
                String.class, Context.class, Scriptable.class);

        MethodHandle test = CHECK_CLASS.bindTo(javaClass);
        test = test.asType(MethodType.methodType(boolean.class, Object.class));

        MethodHandle guard = MethodHandles.guardWithTest(test, target, callSite.getTarget());
        callSite.setTarget(guard);

        return localTarget.invoke(javaObject);
    }

    public static Object getObjectProp(CachingCallSite callSite, Object value,
                               String property, Context cx, Scriptable scope)
            throws Throwable {

        if (!(value instanceof ClassyScriptable)) {
            callSite.setTarget(GETOBJPROP_FALLBACK);
            return GETOBJPROP_FALLBACK.invoke(value, property, cx, scope);
        }

        ClassyScriptable classy = (ClassyScriptable) value;
        ClassyLayout layout = classy.getLayout();

        ClassyLayout.Mapping mapping = layout.findMapping(property);
        if (mapping == null) {
            callSite.setTarget(GETOBJPROP_FALLBACK);
            return GETOBJPROP_FALLBACK.invoke(value, property, cx, scope);
        }
        int offset = mapping.offset();

        MethodHandle target = MethodHandles.insertArguments(GETFASTOBJPROP, 1,
                Integer.valueOf(offset));
        target = MethodHandles.dropArguments(target, 1, String.class,
                Context.class, Scriptable.class);

        MethodHandle test = CHECK_LAYOUT.bindTo(layout);
        test = test.asType(MethodType.methodType(boolean.class, Object.class));

        MethodHandle guard = MethodHandles.guardWithTest(test, target, callSite.getTarget());
        callSite.setTarget(guard);

        /* if (callSite.layout != layout) {
            ClassyLayout.Mapping mapping = layout.findMapping(property);
            if (mapping == null) {
                callSite.setTarget(GETOBJPROP_FALLBACK);
                return GETOBJPROP_FALLBACK.invoke(value, property, cx, scope);
            } else {
                callSite.layout = layout;
                callSite.offset = mapping.offset();
            }
        } */

        return classy.getValueAtOffset(offset, classy);
    }
    
        

    public static Object getFastObjectProp(Object obj, int offset) {
        ClassyScriptable classy = (ClassyScriptable) obj;
        return classy.getValueAtOffset(offset, classy);
    }
    
    public static Object setObjectProp(CachingCallSite callSite, Object obj,
                               String property, Object value, Context cx)
            throws Throwable {
    	
        if (!(obj instanceof ClassyScriptable)) {
            callSite.setTarget(SETOBJPROP_FALLBACK);
            return SETOBJPROP_FALLBACK.invoke(obj, property, value, cx);
        }

        ClassyScriptable classy = (ClassyScriptable) obj;
        ClassyLayout layout = classy.getLayout();
        
        Mapping mapping = layout.findMapping(property);
        int offset;
        
        if(mapping == null){
            classy.put(property, classy, value);
            layout = classy.getLayout();
            mapping = layout.findMapping(property);
            offset = mapping.offset();
        }else{
            offset = mapping.offset();
            classy.putValueAtOffset(offset, classy, value);
        }
      
        MethodHandle target = MethodHandles.insertArguments(SETFASTOBJPROP, 1,
                Integer.valueOf(offset),value);
        target = MethodHandles.dropArguments(target, 3, Context.class);
        
        MethodHandle test = CHECK_LAYOUT.bindTo(layout);
        test = test.asType(MethodType.methodType(boolean.class, Object.class));

        MethodHandle guard = MethodHandles.guardWithTest(test, target, callSite.getTarget());
        callSite.setTarget(guard);        
        	
        return value;
    }
    
    public static Object setFastObjectProp(Object obj, int offset, Object value){
    	
    	ClassyScriptable classy = (ClassyScriptable)obj;
    	classy.putValueAtOffset(offset, classy, value);
    	
    	return value;
    }
    

    private static final MethodHandle CHECK_CLASS;
    private static final MethodHandle CHECK_LAYOUT;
    private static final MethodHandle INITCALL;
    private static final MethodHandle INITOBJPROP;
    private static final MethodHandle CALLPROP0_FALLBACK;
    private static final MethodHandle GETOBJPROP_FALLBACK;
    private static final MethodHandle GETFASTOBJPROP;
    private static final MethodHandle UNWRAP;
    private static final MethodHandle SETOBJPROP_FALLBACK;
    private static final MethodHandle SETOBJPROP;
    private static final MethodHandle SETFASTOBJPROP;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            CHECK_CLASS = lookup.findStatic(InvokeDynamicSupport.class, "checkClass",
                    MethodType.methodType(boolean.class, Class.class, Object.class));
            CHECK_LAYOUT = lookup.findStatic(InvokeDynamicSupport.class, "checkLayout",
                    MethodType.methodType(boolean.class, ClassyLayout.class, Object.class));
            INITCALL = lookup.findStatic(InvokeDynamicSupport.class, "callProp0",
                    MethodType.methodType(Object.class, CachingCallSite.class,
                            Object.class, String.class, Context.class,
                            Scriptable.class));
            CALLPROP0_FALLBACK = lookup.findStatic(OptRuntime.class, "callProp0",
                    MethodType.methodType(Object.class, Object.class,
                            String.class, Context.class, Scriptable.class));
            INITOBJPROP = lookup.findStatic(InvokeDynamicSupport.class, "getObjectProp",
                    MethodType.methodType(Object.class, CachingCallSite.class,
                            Object.class, String.class, Context.class,
                            Scriptable.class));
            GETOBJPROP_FALLBACK = lookup.findStatic(ScriptRuntime.class, "getObjectProp",
                    MethodType.methodType(Object.class, Object.class,
                            String.class, Context.class, Scriptable.class));
            GETFASTOBJPROP = lookup.findStatic(InvokeDynamicSupport.class, "getFastObjectProp",
                    MethodType.methodType(Object.class, Object.class, Integer.TYPE));
            UNWRAP = lookup.findStatic(InvokeDynamicSupport.class, "unwrapObject",
                    MethodType.methodType(Object.class, Object.class));
            SETOBJPROP_FALLBACK = lookup.findStatic(ScriptRuntime.class, "setObjectProp", 
            		MethodType.methodType(Object.class, Object.class,
            				String.class, Object.class,Context.class));
            SETOBJPROP = lookup.findStatic(InvokeDynamicSupport.class, "setObjectProp", 
            		MethodType.methodType(Object.class, CachingCallSite.class,
            				Object.class, String.class, Object.class, Context.class));
            SETFASTOBJPROP = lookup.findStatic(InvokeDynamicSupport.class, "setFastObjectProp", 
                    MethodType.methodType(Object.class, Object.class, Integer.TYPE, Object.class));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
