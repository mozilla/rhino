package org.mozilla.javascript.optimizer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodType;
import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.ScriptRuntime;

/**
 * Holder of {@link ScriptRuntime} method information, to help with codegen
 *
 * <p>Each enum value is linked to a method in {@link ScriptRuntime} via {@link CodeGenMarker}. For
 * each enum value, there should be at least and at most one method linked to it, or a fatal
 * exception will be thrown at class initialization.
 *
 * <p>Calling {@link #addInvoke(ClassFileWriter)} will insert a static method invoke with class
 * being {@link ScriptRuntime}, method name and descriptor being the name and descriptor of the
 * linked method
 *
 * @author ZZZank
 */
@SuppressWarnings("ImmutableEnumChecker") // fields are mutated only once, at class initialization
public enum ScriptRuntimeMethodSig {
    padAndRestArguments,
    padArguments,
    createArrowFunctionActivation,
    createFunctionActivation,
    enterActivationFunction,
    initScript,
    exitActivationFunction,
    newCatchScope,
    enterWith,
    leaveWith,
    enumInit,
    enumNext,
    enumId,
    callRef,
    typeof,
    negate,
    refGet,
    refSet,
    refDel,
    throwDeleteOnSuperPropertyNotAllowed,
    delete,
    memberRef_member,
    memberRef_namespaceMember,
    nameRef_name,
    nameRef_namespaceName,
    escapeAttributeValue,
    escapeTextValue,
    setDefaultNamespace,
    specialRef,
    wrapInt,
    fillObjectLiteral,
    newSpecial,
    callSpecial,
    newObject,
    getElemAndThisOptional,
    getElemAndThis,
    getValueAndThisOptional,
    getValueAndThis,
    typeofName,
    addInstructionCount,
    nameIncrDecr,
    propIncrDecr,
    elemIncrDecr,
    refIncrDecr,
    subtract,
    multiply,
    divide,
    remainder,
    exponentiate,
    bitwiseNOT,
    toInt32,
    bitwiseOR,
    bitwiseXOR,
    bitwiseAND,
    signedRightShift,
    signedRightShift_primitive,
    leftShift,
    instanceOf,
    in,
    enterDotQuery,
    updateDotQuery,
    leaveDotQuery,
    wrapRegExp,
    getTemplateLiteralCallSite,
    doObjectRest,
    concat;

    private static final String CLASS_INTERNAL_NAME =
            ScriptRuntime.class.getName().replace('.', '/');

    static {
        // init
        for (var method : ScriptRuntime.class.getDeclaredMethods()) {
            var marker = method.getAnnotation(CodeGenMarker.class);
            if (marker == null) {
                continue;
            }
            var annot = marker.value();
            if (annot.methodName != null) {
                throw new IllegalStateException("found two method with same marker: " + annot);
            }
            annot.methodName = method.getName();
            annot.methodDesc =
                    MethodType.methodType(method.getReturnType(), method.getParameterTypes())
                            .toMethodDescriptorString();
        }
        // validation
        for (var value : values()) {
            if (value.methodName == null) {
                throw new IllegalStateException("not initialized: " + value);
            }
        }
    }

    private String methodName;
    private String methodDesc;

    public void addInvoke(ClassFileWriter cfw) {
        cfw.addInvoke(ByteCode.INVOKESTATIC, CLASS_INTERNAL_NAME, methodName, methodDesc);
    }

    /**
     * For linking method and corresponding enum value
     *
     * @see ScriptRuntimeMethodSig
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface CodeGenMarker {
        ScriptRuntimeMethodSig value();
    }
}
