package org.mozilla.javascript.optimizer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JSCodeExec;
import org.mozilla.javascript.JSCodeResume;
import org.mozilla.javascript.JSScript;
import org.mozilla.javascript.VarScope;

/** Subclass of {@link JSCode} for compiled Java methods. */
public class MHJSScriptCode extends MHJSCode<JSScript> {
    private final JSCodeExec<JSScript> exec;
    private final JSCodeResume<JSScript> resume;

    protected MHJSScriptCode(MethodHandle execMH, MethodHandle resumeMH) {
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
                                return execMH.invokeExact(cx, xobj, state, scope, operation, value);
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
            JSScript executableObject,
            Object newTarget,
            VarScope scope,
            Object thisObj,
            Object[] args) {
        return exec.execute(cx, executableObject, newTarget, scope, thisObj, args);
    }

    @Override
    public Object resume(
            Context cx,
            JSScript executableObject,
            Object state,
            VarScope scope,
            int operation,
            Object value) {
        return resume.resume(cx, executableObject, state, scope, operation, value);
    }

    public static class Builder extends MHJSCode.Builder<JSScript> {

        Builder(MHJSCode.BuilderEnv env) {
            super(env);
        }

        @Override
        protected MHJSCode<JSScript> buildCode(MethodHandle exec, MethodHandle resume) {
            return new MHJSScriptCode(exec, resume);
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
            var className = MHJSScriptCode.class.getName();
            var signature = className.replaceAll("\\.", "/");
            var type =
                    MethodType.methodType(
                                    MHJSScriptCode.class,
                                    Lookup.class,
                                    Class.class,
                                    String.class,
                                    String.class)
                            .toMethodDescriptorString();
            cfw.addInvoke(ByteCode.INVOKESTATIC, signature, "makeJSCode", type);
        }
    }

    public static MHJSScriptCode makeJSCode(
            Lookup lookup, Class<?> clazz, String methodName, String methodType) {
        try {
            MethodHandle exec =
                    lookup.findStatic(
                            clazz,
                            methodName,
                            MethodType.fromMethodDescriptorString(
                                    methodType, clazz.getClassLoader()));
            return new MHJSScriptCode(exec, null);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new Error("Gnerated class did not contain expected methods", e);
        }
    }
}
