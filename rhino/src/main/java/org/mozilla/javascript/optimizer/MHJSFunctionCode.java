package org.mozilla.javascript.optimizer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JSCode;
import org.mozilla.javascript.JSCodeExec;
import org.mozilla.javascript.JSCodeResume;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.VarScope;

/** Subclass of {@link JSCode} for compiled Java methods. */
public class MHJSFunctionCode extends MHJSCode<JSFunction> {
    private final JSCodeExec<JSFunction> exec;
    private final JSCodeResume<JSFunction> resume;

    protected MHJSFunctionCode(MethodHandle execMH, MethodHandle resumeMH) {
        exec =
                (cx, xobj, newTarget, scope, thisObj, args) -> {
                    try {
                        return execMH.invokeExact(cx, xobj, newTarget, scope, thisObj, args);
                    } catch (Error e) {
                        throw e;
                    } catch (Throwable e) {
                        throw (RuntimeException) e;
                    }
                };
        resume =
                resumeMH != null
                        ? (cx, xobj, state, scope, operation, value) -> {
                            try {
                                return resumeMH.invokeExact(
                                        cx, xobj, state, scope, operation, value);
                            } catch (Error e) {
                                throw e;
                            } catch (Throwable e) {
                                throw (RuntimeException) e;
                            }
                        }
                        : (cx, xobj, state, scope, operation, value) -> {
                            throw new UnsupportedOperationException();
                        };
    }

    @Override
    public Object execute(
            Context cx,
            JSFunction executableObject,
            Object newTarget,
            VarScope scope,
            Object thisObj,
            Object[] args) {
        return exec.execute(cx, executableObject, newTarget, scope, thisObj, args);
    }

    @Override
    public Object resume(
            Context cx,
            JSFunction executableObject,
            Object state,
            VarScope scope,
            int operation,
            Object value) {
        return resume.resume(cx, executableObject, state, scope, operation, value);
    }

    public static class Builder extends MHJSCode.Builder<JSFunction> {

        Builder(MHJSCode.BuilderEnv env) {
            super(env);
        }

        @Override
        protected MHJSCode<JSFunction> buildCode(MethodHandle exec, MethodHandle resume) {
            return new MHJSFunctionCode(exec, resume);
        }

        @Override
        public void buildByteCode(ClassFileWriter cfw, String mainClass) {
            cfw.addInvoke(
                    ByteCode.INVOKESTATIC,
                    mainClass,
                    "getLookup",
                    "()Ljava/lang/invoke/MethodHandles$Lookup;");
            cfw.addLoadConstantClass(mainClass);
            cfw.addLoadConstant(methodName);
            cfw.addLoadConstant(methodType);
            if (resumeName == null) {
                cfw.add(ByteCode.ACONST_NULL);
                cfw.add(ByteCode.ACONST_NULL);
            } else {
                cfw.addLoadConstant(resumeName);
                cfw.addLoadConstant(resumeType);
            }
            var className = MHJSFunctionCode.class.getName();
            var signature = className.replaceAll("\\.", "/");
            var type =
                    MethodType.methodType(
                                    MHJSFunctionCode.class,
                                    Lookup.class,
                                    Class.class,
                                    String.class,
                                    String.class,
                                    String.class,
                                    String.class)
                            .toMethodDescriptorString();
            cfw.addInvoke(ByteCode.INVOKESTATIC, signature, "makeJSCode", type);
        }
    }

    public static MHJSFunctionCode makeJSCode(
            Lookup lookup,
            Class<?> clazz,
            String methodName,
            String methodType,
            String resumeName,
            String resumeType) {
        try {
            MethodHandle exec =
                    lookup.findStatic(
                            clazz,
                            methodName,
                            MethodType.fromMethodDescriptorString(
                                    methodType, clazz.getClassLoader()));
            MethodHandle resume = null;
            if (resumeName != null) {
                resume =
                        lookup.findStatic(
                                clazz,
                                resumeName,
                                MethodType.fromMethodDescriptorString(
                                        resumeType, clazz.getClassLoader()));
            }
            return new MHJSFunctionCode(exec, resume);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new Error("Gnerated class did not contain expected methods", e);
        }
    }
}
