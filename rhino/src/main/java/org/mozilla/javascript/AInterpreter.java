package org.mozilla.javascript;

import static org.mozilla.javascript.ScriptableObject.PERMANENT;
import static org.mozilla.javascript.ScriptableObject.READONLY;
import static org.mozilla.javascript.UniqueTag.NOT_FOUND;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AInterpreter<T extends ACallFrame<T, U>, U extends ACompilerData<?, U>>
        implements Evaluator {

    protected static final class ContinuationJump<
                    T extends ACallFrame<T, U>, U extends ACompilerData<?, U>>
            implements Serializable {
        private static final long serialVersionUID = 7687739156004308247L;

        T capturedFrame;
        T branchFrame;
        Object result;
        double resultDbl;

        ContinuationJump(NativeContinuation c, T current) {
            @SuppressWarnings("unchecked")
            var cf = (T) c.getImplementation();
            capturedFrame = cf;
            if (this.capturedFrame == null || current == null) {
                // Continuation and current execution does not share
                // any frames if there is nothing to capture or
                // if there is no currently executed frames
                this.branchFrame = null;
            } else {
                // Search for branch frame where parent frame chains starting
                // from captured and current meet.
                T chain1 = this.capturedFrame;
                T chain2 = current;

                // First work parents of chain1 or chain2 until the same
                // frame depth.
                int diff = chain1.frameIndex - chain2.frameIndex;
                if (diff != 0) {
                    if (diff < 0) {
                        // swap to make sure that
                        // chain1.frameIndex > chain2.frameIndex and diff > 0
                        chain1 = current;
                        chain2 = this.capturedFrame;
                        diff = -diff;
                    }
                    do {
                        chain1 = chain1.parentFrame;
                    } while (--diff != 0);
                    if (chain1.frameIndex != chain2.frameIndex) Kit.codeBug();
                }

                // Now walk parents in parallel until a shared frame is found
                // or until the root is reached.
                while (!Objects.equals(chain1, chain2) && chain1 != null) {
                    chain1 = chain1.parentFrame;
                    chain2 = chain2.parentFrame;
                }

                this.branchFrame = chain1;
                if (this.branchFrame != null && !this.branchFrame.frozen) Kit.codeBug();
            }
        }
    }

    protected static void enterFrame(
            Context cx, ACallFrame<?, ?> frame, Object[] args, boolean continuationRestart) {
        boolean usesActivation = frame.fnOrScript.getDescriptor().requiresActivationFrame();
        boolean isDebugged = frame.debuggerFrame != null;
        if (usesActivation) {
            VarScope scope = frame.scope;
            if (scope == null) {
                Kit.codeBug();
            } else if (continuationRestart) {
                // Walk the parent chain of frame.scope until a NativeCall is
                // found. Normally, frame.scope is a NativeCall when called
                // from initFrame() for a debugged or activatable function.
                // However, when called from interpretLoop() as part of
                // restarting a continuation, it can also be a WIthScope if
                // the continuation was captured within a "with" or "catch"
                // block ("catch" implicitly uses WithScope to create a scope
                // to expose the exception variable).
                for (; ; ) {
                    if (scope instanceof WithScope) {
                        scope = scope.getParentScope();
                        if (scope == null
                                || (frame.parentFrame != null
                                        && frame.parentFrame.scope == scope)) {
                            // If we get here, we didn't find a NativeCall in
                            // the call chain before reaching parent frame's
                            // scope. This should not be possible.
                            Kit.codeBug();
                            break; // Never reached, but keeps the static analyzer
                            // happy about "scope" not being null 5 lines above.
                        }
                    } else {
                        break;
                    }
                }
            }
            if (isDebugged) {
                frame.debuggerFrame.onEnter(cx, scope, frame.thisObj, args);
            }
            ScriptRuntime.enterActivationFunction(cx, scope);
        } else if (isDebugged) {
            frame.debuggerFrame.onEnter(cx, new DebugScope(frame), frame.thisObj, args);
        }
    }

    static <T extends ACallFrame<T, U>, U extends ACompilerData<?, U>> void exitFrame(
            Context cx, T frame, Object throwable) {
        if (frame.fnOrScript.getDescriptor().requiresActivationFrame()) {
            ScriptRuntime.exitActivationFunction(cx);
        }

        if (frame.fnOrScript.getDescriptor().getFunctionType() != 0) {
            ScriptRuntime.exitFunctionStrictness(cx, frame.parentStrictness);
        }

        if (frame.debuggerFrame != null) {
            try {
                if (throwable instanceof Throwable) {
                    frame.debuggerFrame.onExit(cx, true, throwable);
                } else {
                    Object result;
                    @SuppressWarnings("unchecked")
                    var cjump = (ContinuationJump<T, U>) throwable;
                    if (cjump == null) {
                        result = frame.result;
                    } else {
                        result = cjump.result;
                    }
                    if (result == UniqueTag.DOUBLE_MARK) {
                        double resultDbl;
                        if (cjump == null) {
                            resultDbl = frame.resultDbl;
                        } else {
                            resultDbl = cjump.resultDbl;
                        }
                        result = ScriptRuntime.wrapNumber(resultDbl);
                    }
                    frame.debuggerFrame.onExit(cx, false, result);
                }
            } catch (Throwable ex) {
                System.err.println("RHINO USAGE WARNING: onExit terminated with exception");
                ex.printStackTrace(System.err);
            }
        }
    }

    /**
     * This class is intended as proxy to give {@link DebugFrame} access to the contents of local
     * variables. We take this approach rather than forcing the interpreter to introduce activation
     * frames because it is faster (assuming local variable manipulation by the interpreter is more
     * common than inspection by the debugger) and it reduces the chance that programs might
     * evexcute differently in debug mode.
     */
    protected static class DebugScope implements VarScope {
        private final ACallFrame<?, ?> frame;
        private volatile Map<String, Integer> offsets;

        /** Create a new debug scope associated with a particular call frame. */
        private DebugScope(ACallFrame<?, ?> frame) {
            this.frame = frame;
        }

        /**
         * Populate the map associating names to variable indices. Most names should have been made
         * unique as part of the compilation process, but arguments with duplicate names will not
         * have been.The map is build so that duplicate argument names resolve to the last index as
         * this is also what the compiler does - at least once we are past setting default values.
         */
        private Map<String, Integer> getOffsets() {
            if (offsets == null) {
                offsets = buildOffsets(frame);
            }
            return offsets;
        }

        private static Map<String, Integer> buildOffsets(ACallFrame<?, ?> frame) {
            var desc = frame.fnOrScript.getDescriptor();
            int varCount = desc.getParamAndVarCount();
            var map = new HashMap<String, Integer>();
            for (int i = 0; i < varCount; i++) {
                map.put(desc.getParamOrVarName(i), i);
            }
            return map;
        }

        @Override
        public void delete(String name) {
        }

        @Override
        public void delete(int index) {
        }

        @Override
        public void delete(Symbol key) {
        }

        @Override
        public Object get(String name, VarScope scope) {
            int offset = getOffsets().getOrDefault(name, -1);
            return offset >= 0 ? frame.getFromVars(offset) : NOT_FOUND;
        }

        @Override
        public Object get(int index, VarScope scope) {
            return NOT_FOUND;
        }

        @Override
        public Object get(Symbol key, VarScope scope) {
            return NOT_FOUND;
        }

        @Override
        public Object[] getIds() {
            return getOffsets().keySet().toArray();
        }

        @Override
        public VarScope getParentScope() {
            return frame.scope;
        }

        @Override
        public boolean has(String name, VarScope start) {
            return getOffsets().containsKey(name);
        }

        @Override
        public boolean has(int index, VarScope start) {
            return false;
        }

        @Override
        public boolean has(Symbol key, VarScope start) {
            return false;
        }

        @Override
        public void put(String name, VarScope start, Object value) {
            int offset = getOffsets().getOrDefault(name, -1);
            if (offset >= 0) {
                frame.setInVars(offset, value);
            }
        }

        @Override
        public void put(int index, VarScope start, Object value) {
            // Do nothing.
        }

        @Override
        public void put(Symbol key, VarScope start, Object value) {
            // Do nothing.
        }

        @Override
        public void defineConst(String name, VarScope start) {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean isConst(String name) {
            int offset = getOffsets().getOrDefault(name, -1);
            if (offset >= 0) {
                return (frame.stackAttributes[offset] & (PERMANENT | READONLY)) == (PERMANENT | READONLY);
            } else {
                return false;
            }
        }

        @Override
        public void putConst(String name, VarScope start, Object value) {
            // TODO Auto-generated method stub

        }
    }

    @Override
    public void captureStackInfo(RhinoException ex) {
        Context cx = Context.getCurrentContext();
        if (cx == null || cx.lastInterpreterFrame == null) {
            // No interpreter invocations
            ex.interpreterStackInfo = null;
        } else {
            ex.interpreterStackInfo = cx.lastInterpreterFrame;
            ex.interpreterLineData = cx.lastInterpreterFrame.getPcSourceLineStart();
        }
    }

    @Override
    public String getPatchedStack(RhinoException ex, String nativeStackTrace) {
        String tag = "org.mozilla.javascript.Interpreter.interpretLoop";
        StringBuilder sb = new StringBuilder(nativeStackTrace.length() + 1000);
        String lineSeparator = SecurityUtilities.getSystemProperty("line.separator");

        ACallFrame<?, ?> calleeFrame = null;
        ACallFrame<?, ?> frame = ex.interpreterStackInfo;
        int offset = 0;
        while (frame != null) {
            ACallFrame<?, ?> callerFrame = frame;
            int pos = nativeStackTrace.indexOf(tag, offset);
            if (pos < 0) {
                break;
            }

            // Skip tag length
            pos += tag.length();
            // Skip until the end of line
            for (; pos != nativeStackTrace.length(); ++pos) {
                char c = nativeStackTrace.charAt(pos);
                if (c == '\n' || c == '\r') {
                    break;
                }
            }
            sb.append(nativeStackTrace, offset, pos);
            offset = pos;

            while (callerFrame != null) {
                var idata = callerFrame.compilerData;
                JSDescriptor<?> desc = callerFrame.fnOrScript.getDescriptor();
                sb.append(lineSeparator);
                sb.append("\tat script");
                if (desc.getName() != null && desc.getName().length() != 0) {
                    sb.append('.');
                    sb.append(desc.getName());
                }
                sb.append('(');
                sb.append(desc.getSourceName());
                int pc = calleeFrame == null ? ex.interpreterLineData : calleeFrame.parentPC;
                if (pc >= 0) {
                    // Include line info only if available
                    sb.append(':');
                    sb.append(idata.getLineNumberFromPc(pc, pc));
                }
                sb.append(')');
                calleeFrame = callerFrame;
                callerFrame = callerFrame.parentFrame;
            }
            frame = calleeFrame.previousInterpreterFrame;
        }
        sb.append(nativeStackTrace.substring(offset));

        return sb.toString();
    }

    @Override
    public List<String> getScriptStack(RhinoException ex) {
        ScriptStackElement[][] stack = getScriptStackElements(ex);
        List<String> list = new ArrayList<>(stack.length);
        String lineSeparator = SecurityUtilities.getSystemProperty("line.separator");
        for (ScriptStackElement[] group : stack) {
            StringBuilder sb = new StringBuilder();
            for (ScriptStackElement elem : group) {
                elem.renderJavaStyle(sb);
                sb.append(lineSeparator);
            }
            list.add(sb.toString());
        }
        return list;
    }

    public ScriptStackElement[][] getScriptStackElements(RhinoException ex) {
        if (ex.interpreterStackInfo == null) {
            return null;
        }

        List<ScriptStackElement[]> list = new ArrayList<>();

        ACallFrame<?, ?> calleeFrame = null;
        ACallFrame<?, ?> frame = ex.interpreterStackInfo;
        while (frame != null) {
            ACallFrame<?, ?> callerFrame = frame;
            List<ScriptStackElement> group = new ArrayList<>();
            while (callerFrame != null) {
                var idata = callerFrame.compilerData;
                JSDescriptor<?> desc = callerFrame.fnOrScript.getDescriptor();
                String fileName = desc.getSourceName();
                String functionName = null;
                int lineNumber = -1;
                int pc = calleeFrame == null ? ex.interpreterLineData : calleeFrame.parentPC;
                if (pc >= 0) {
                    lineNumber = idata.getLineNumberFromPc(pc, pc);
                }
                if (desc.getName() != null && desc.getName().length() != 0) {
                    functionName = desc.getName();
                }
                calleeFrame = callerFrame;
                callerFrame = callerFrame.parentFrame;
                group.add(new ScriptStackElement(fileName, functionName, lineNumber));
            }
            list.add(group.toArray(new ScriptStackElement[0]));
            frame = calleeFrame.previousInterpreterFrame;
        }
        return list.toArray(new ScriptStackElement[list.size()][]);
    }

    @Override
    public final String getSourcePositionFromStack(Context cx, int[] linep) {
        ACallFrame<?, ?> frame = cx.lastInterpreterFrame;
        var data = frame.compilerData;
        JSDescriptor<?> desc = frame.fnOrScript.getDescriptor();
        linep[0] = data.getLineNumberFromPc(frame.pc, frame.getPcSourceLineStart());
        return desc.getSourceName();
    }
}
