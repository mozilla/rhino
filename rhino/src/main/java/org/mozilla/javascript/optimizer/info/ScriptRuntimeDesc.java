package org.mozilla.javascript.optimizer.info;

import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.Ref;
import org.mozilla.javascript.ScriptOrFn;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

/**
 * Holder of {@link ScriptRuntime} method / field information, to help with codegen
 *
 * <p>Calling {@link #addInvoke(ClassFileWriter)} will insert a static method invoke with class
 * being {@link ScriptRuntime}, method name and descriptor being the name and descriptor of the
 * linked method
 *
 * @author ZZZank
 */
public enum ScriptRuntimeDesc {
    // --- method ---
    concat(Object.class, Object.class),
    wrapInt(int.class),
    padArguments(Object[].class, int.class),
    padAndRestArguments(Context.class, Scriptable.class, Object[].class, int.class),
    doObjectRest(Context.class, Scriptable.class, Object.class, Object[].class),
    fillObjectLiteral(
            Scriptable.class,
            Object[].class,
            Object[].class,
            int[].class,
            Context.class,
            Scriptable.class),
    toInt32(double.class),
    setDefaultNamespace(Object.class, Context.class),
    refGet(Ref.class, Context.class),
    refSet(Ref.class, Object.class, Context.class, Scriptable.class),
    refDel(Ref.class, Context.class),
    specialRef(Object.class, String.class, Context.class, Scriptable.class),
    delete(Object.class, Object.class, Context.class, Scriptable.class, boolean.class),
    callRef(Callable.class, Scriptable.class, Object[].class, Context.class),
    enumInit(Object.class, Context.class, Scriptable.class, int.class),
    enumNext(Object.class, Context.class),
    enumId(Object.class, Context.class),
    getElemAndThis(Object.class, Object.class, Context.class, Scriptable.class),
    getElemAndThisOptional(Object.class, Object.class, Context.class, Scriptable.class),
    getValueAndThis(Object.class, Context.class),
    getValueAndThisOptional(Object.class, Context.class),
    newObject(Object.class, Context.class, Scriptable.class, Object[].class),
    callSpecial(
            Context.class,
            Callable.class,
            Scriptable.class,
            Object[].class,
            Scriptable.class,
            Scriptable.class,
            int.class,
            String.class,
            int.class,
            boolean.class),
    newSpecial(Context.class, Object.class, Object[].class, Scriptable.class, int.class),
    typeof(Object.class),
    typeofName(Scriptable.class, String.class),
    subtract(Number.class, Number.class),
    multiply(Number.class, Number.class),
    divide(Number.class, Number.class),
    remainder(Number.class, Number.class),
    exponentiate(Number.class, Number.class),
    bitwiseAND(Number.class, Number.class),
    bitwiseOR(Number.class, Number.class),
    bitwiseXOR(Number.class, Number.class),
    leftShift(Number.class, Number.class),
    signedRightShift_primitive("signedRightShift", double.class, double.class),
    signedRightShift(Number.class, Number.class),
    bitwiseNOT(Number.class),
    nameIncrDecr(Scriptable.class, String.class, Context.class, int.class),
    propIncrDecr(Object.class, String.class, Context.class, Scriptable.class, int.class),
    elemIncrDecr(Object.class, Object.class, Context.class, Scriptable.class, int.class),
    refIncrDecr(Ref.class, Context.class, Scriptable.class, int.class),
    negate(Number.class),
    instanceOf(Object.class, Object.class, Context.class),
    in(Object.class, Object.class, Context.class),
    addInstructionCount(Context.class, int.class),
    initScript(ScriptOrFn.class, Scriptable.class, Context.class, Scriptable.class, boolean.class),
    createFunctionActivation(
            JSFunction.class,
            Context.class,
            Scriptable.class,
            Object[].class,
            boolean.class,
            boolean.class,
            boolean.class),
    createArrowFunctionActivation(
            JSFunction.class,
            Context.class,
            Scriptable.class,
            Object[].class,
            boolean.class,
            boolean.class,
            boolean.class),
    enterActivationFunction(Context.class, Scriptable.class),
    exitActivationFunction(Context.class),
    newCatchScope(Throwable.class, Scriptable.class, String.class, Context.class, Scriptable.class),
    enterWith(Object.class, Context.class, Scriptable.class),
    leaveWith(Scriptable.class),
    enterDotQuery(Object.class, Scriptable.class),
    updateDotQuery(boolean.class, Scriptable.class),
    leaveDotQuery(Scriptable.class),
    wrapRegExp(Context.class, Scriptable.class, Object.class),
    getTemplateLiteralCallSite(Context.class, Scriptable.class, Object[].class, int.class),
    escapeAttributeValue(Object.class, Context.class),
    escapeTextValue(Object.class, Context.class),
    memberRef_member("memberRef", Object.class, Object.class, Context.class, int.class),
    memberRef_namespaceMember(
            "memberRef", Object.class, Object.class, Object.class, Context.class, int.class),
    nameRef_name("nameRef", Object.class, Context.class, Scriptable.class, int.class),
    nameRef_namespaceName(
            "nameRef", Object.class, Object.class, Context.class, Scriptable.class, int.class),
    throwDeleteOnSuperPropertyNotAllowed(),
    // --- field ---
    emptyArgs(true),
    emptyStrings(true);

    private static final String CLASS_INTERNAL_NAME =
            ScriptRuntime.class.getName().replace('.', '/');

    public final String memberName;
    public final String desc;
    public final boolean voidReturn;
    public final int paramCount;

    ScriptRuntimeDesc(boolean isField, String name, Class<?>... paramTypes) {
        if (name == null) {
            name = this.name();
        }

        try {
            if (isField) {
                var field = ScriptRuntime.class.getField(name);
                assert Modifier.isStatic(field.getModifiers());

                this.memberName = field.getName();
                // Class#descriptorString() is added in Java 12, so we can't use it.
                // Tips: descriptor of a no-arg method is "(){descriptor of return type}"
                this.desc =
                        MethodType.methodType(field.getType())
                                .toMethodDescriptorString()
                                .substring("()".length());
                this.voidReturn = field.getType() == void.class;
                this.paramCount = -1;
            } else {
                var method = ScriptRuntime.class.getMethod(name, paramTypes);
                assert Modifier.isStatic(method.getModifiers());

                this.memberName = method.getName();
                this.desc =
                        MethodType.methodType(method.getReturnType(), paramTypes)
                                .toMethodDescriptorString();
                this.voidReturn = method.getReturnType() == void.class;
                this.paramCount = paramTypes.length;
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /** Shortcut for field desc */
    ScriptRuntimeDesc(boolean isField) {
        this(isField, null);
    }

    /** Shortcut for method desc. */
    ScriptRuntimeDesc(String name, Class<?>... paramTypes) {
        this(false, name, paramTypes);
    }

    /**
     * Shortcut for method desc. If the enum name is not the same as actual method name due to, for
     * example, handling overloaded methods, use {@link #ScriptRuntimeDesc(String, Class[])} instead
     */
    ScriptRuntimeDesc(Class<?>... paramTypes) {
        this(false, null, paramTypes);
    }

    public boolean isField() {
        return paramCount < 0;
    }

    public void getStatic(ClassFileWriter cfw) {
        if (!isField()) {
            throw new IllegalStateException("getStatic(...) called on non-field desc: " + this);
        }
        cfw.add(ByteCode.GETSTATIC, CLASS_INTERNAL_NAME, memberName, desc);
    }

    public void addInvoke(ClassFileWriter cfw, int expectedParamCount) {
        if (isField()) {
            throw new IllegalStateException("addInvoke(...) called on non-method desc: " + this);
        }
        if (expectedParamCount >= 0 && expectedParamCount != paramCount) {
            throw new IllegalStateException(
                    String.format(
                            "expectedParamCount != paramCount (%s != %s)",
                            expectedParamCount, paramCount));
        }
        cfw.addInvoke(ByteCode.INVOKESTATIC, CLASS_INTERNAL_NAME, memberName, desc);
    }

    public void addInvoke(ClassFileWriter cfw) {
        addInvoke(cfw, -1);
    }

    public void addInvokeVoid(ClassFileWriter cfw, int expectedParamCount) {
        addInvoke(cfw, expectedParamCount);
        if (!voidReturn) {
            cfw.add(ByteCode.POP);
        }
    }

    public void addInvokeVoid(ClassFileWriter cfw) {
        addInvokeVoid(cfw, -1);
    }

    public void addInvokeNonVoid(ClassFileWriter cfw, int expectedParamCount) {
        if (voidReturn) {
            throw new IllegalStateException("");
        }
        addInvoke(cfw, expectedParamCount);
    }

    public void addInvokeNonVoid(ClassFileWriter cfw) {
        addInvokeNonVoid(cfw, -1);
    }
}
