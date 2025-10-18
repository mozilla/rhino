package org.mozilla.javascript.tools.shell;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

/**
 * Helper class for <code>global.runCommand</code>.
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
@SuppressWarnings("AndroidJdkLibsChecker")
class ExecUtil {
    private ExecUtil() {}

    private static final Object[] emptyArray = new Object[0];

    static int runCommand(Global global, Scriptable thisObj, Object[] args) throws IOException {
        int len = args.length;
        if (len == 0 || (len == 1 && args[0] instanceof Scriptable)) {
            throw Global.reportRuntimeError("msg.runCommand.bad.args");
        }

        String[] environment = null;
        File wd = null;
        InputStream in = null;
        OutputStream out = null;
        OutputStream err = null;
        Scriptable params = null;
        Global.CommandExecutor commandExecutor = null;
        Object[] addArgs = emptyArray;
        int timeout = 0;

        if (args[len - 1] instanceof Scriptable) {
            // if the last argument is an object, parse
            // "env", "dir", "input", "output", "err", "timeout" and
            // additional "args"
            params = (Scriptable) args[--len];
            environment = parseEnvironment(params);
            wd = parseWorkingDir(params);
            in = parseInput(params);
            out = parseOutput(params, "output");
            err = parseOutput(params, "err");
            timeout = parseTimeout(params);
            addArgs = parseAddArgs(params, thisObj);
            commandExecutor = parseCommandLauncher(params);
        }

        if (out == null) {
            out = global.getOut();
        }
        if (err == null) {
            err = global.getErr();
        }
        if (commandExecutor == null) {
            commandExecutor = Runtime.getRuntime()::exec;
        }

        // If no explicit input stream, do not send any input to process,
        // in particular, do not use System.in to avoid deadlocks
        // when waiting for user input to send to process which is already
        // terminated as it is not always possible to interrupt read method.

        String[] cmd = new String[len + addArgs.length];
        for (int i = 0; i != len; ++i) {
            cmd[i] = ScriptRuntime.toString(args[i]);
        }
        for (int i = 0; i != addArgs.length; ++i) {
            cmd[len + i] = ScriptRuntime.toString(addArgs[i]);
        }
        try {
            Process p = commandExecutor.exec(cmd, environment, wd);
            return waitForProcess(p, in, out, err, timeout);
        } finally {
            if (out instanceof ReturnBuffer) {
                ScriptableObject.putProperty(params, "output", out.toString());
            }
            if (err instanceof ReturnBuffer) {
                ScriptableObject.putProperty(params, "err", out.toString());
            }
        }
    }

    private static String[] parseEnvironment(Scriptable params) {
        Object obj = ScriptableObject.getProperty(params, "env");
        if (obj == Scriptable.NOT_FOUND) {
            return null;
        }
        if (obj == null) {
            return new String[0];
        } else {
            if (!(obj instanceof Scriptable)) {
                throw Global.reportRuntimeError("msg.runCommand.bad.env");
            }
            Scriptable envHash = (Scriptable) obj;
            Object[] ids = ScriptableObject.getPropertyIds(envHash);
            String[] environment = new String[ids.length];
            for (int i = 0; i != ids.length; ++i) {
                Object keyObj = ids[i], val;
                String key;
                if (keyObj instanceof String) {
                    key = (String) keyObj;
                    val = ScriptableObject.getProperty(envHash, key);
                } else {
                    int ikey = ((Number) keyObj).intValue();
                    key = Integer.toString(ikey);
                    val = ScriptableObject.getProperty(envHash, ikey);
                }
                if (val == ScriptableObject.NOT_FOUND) {
                    val = Undefined.instance;
                }
                environment[i] = key + '=' + ScriptRuntime.toString(val);
            }
            return environment;
        }
    }

    private static File parseWorkingDir(Scriptable params) {
        Object obj = ScriptableObject.getProperty(params, "dir");
        if (obj == Scriptable.NOT_FOUND) {
            return null;
        }
        if (obj instanceof Wrapper) {
            obj = ((Wrapper) obj).unwrap();
        }
        if (obj instanceof File) {
            return (File) obj;
        }
        return new File(ScriptRuntime.toString(obj));
    }

    private static InputStream parseInput(Scriptable params) throws IOException {
        Object obj = ScriptableObject.getProperty(params, "input");
        if (obj == Scriptable.NOT_FOUND) {
            return null;
        }
        if (obj instanceof Wrapper) {
            obj = ((Wrapper) obj).unwrap();
        }
        if (obj instanceof InputStream) {
            return (InputStream) obj;
        }
        if (obj instanceof byte[]) {
            return new ByteArrayInputStream((byte[]) obj);
        }
        String s;
        if (obj instanceof Reader) {
            s = Global.readReader((Reader) obj);
        } else if (obj instanceof char[]) {
            s = new String((char[]) obj);
        } else {
            s = ScriptRuntime.toString(obj);
        }
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    private static OutputStream parseOutput(Scriptable params, String type) {
        Object obj = ScriptableObject.getProperty(params, type);
        if (obj == Scriptable.NOT_FOUND) {
            return null;
        }
        if (obj instanceof Wrapper) {
            obj = ((Wrapper) obj).unwrap();
        }
        if (obj instanceof OutputStream) {
            return (OutputStream) obj;
        }
        return new ReturnBuffer(ScriptRuntime.toString(obj));
    }

    private static int parseTimeout(Scriptable params) {
        Object obj = ScriptableObject.getProperty(params, "timeout");
        if (obj == Scriptable.NOT_FOUND) {
            return -1;
        }
        return ScriptRuntime.toInt32(obj);
    }

    private static Object[] parseAddArgs(Scriptable params, Scriptable scope) {
        Object obj = ScriptableObject.getProperty(params, "args");
        if (obj == Scriptable.NOT_FOUND) {
            return emptyArray;
        }
        Scriptable s = Context.toObject(obj, ScriptableObject.getTopLevelScope(scope));
        return ScriptRuntime.getArrayElements(s);
    }

    private static Global.CommandExecutor parseCommandLauncher(Scriptable params) {
        Object obj = ScriptableObject.getProperty(params, "commandExecutor");
        if (obj == Scriptable.NOT_FOUND) {
            return null;
        }
        if (obj instanceof Wrapper) {
            obj = ((Wrapper) obj).unwrap();
        }
        return (Global.CommandExecutor) obj;
    }

    /**
     * Waits for the process and passes input- and outputStream. If any of in, out, err is null, the
     * corresponding process stream will be closed immediately, otherwise it will be closed as soon
     * as all data will be read from/written to process
     *
     * @return Exit value of process.
     * @throws IOException If there was an error executing the process.
     */
    private static int waitForProcess(
            Process p, InputStream in, OutputStream out, OutputStream err, int timeout)
            throws IOException {

        // use try-with-resources with sophisticated error handling
        // When multiple streams will throw an error, the errors are added as suppressed exceptions
        // and do not hide the original exception.
        try (PipeThread inThread = startPipeThread(in, p.getOutputStream(), p.getOutputStream());
                PipeThread outThread =
                        startPipeThread(p.getInputStream(), out, p.getInputStream());
                PipeThread errThread =
                        startPipeThread(p.getErrorStream(), err, p.getErrorStream());
                KillThread killThread = startKillThread(p, timeout)) {

            try {
                // wait for process completion
                p.waitFor();
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }

            int exitCode = p.exitValue();
            if (killThread != null && killThread.killed) {
                // on abnormal process termination - do not throw errors of streams in
                // try-with-resources
                if (inThread != null) inThread.reportErrors = false;
                if (outThread != null) outThread.reportErrors = false;
                if (errThread != null) errThread.reportErrors = false;
            }
            return exitCode;
        } finally {
            p.destroy();
        }
    }

    /** Creates a kill-thread, that Kills the process after specified timeout. */
    private static KillThread startKillThread(Process process, int timeout) {
        if (timeout <= 0) {
            return null;
        }
        KillThread killThread = new KillThread(process, timeout);
        killThread.start();
        return killThread;
    }

    /**
     * Creates a pipe-thread, that transfers all data from <code>in</code> to <code>out</code>.
     *
     * <p>The <code>processStream</code> is closed, when everything is transferred and may signalize
     * the process, to terminate.
     */
    private static PipeThread startPipeThread(
            InputStream in, OutputStream out, Closeable processStream) throws IOException {
        if (in == null) {
            out.close();
            return null;
        } else if (out == null) {
            in.close();
            return null;
        } else {
            PipeThread pipeThread = new PipeThread(in, out, processStream);
            pipeThread.start();
            return pipeThread;
        }
    }

    /**
     * A PipeThread transfers data from <code>in</code> to <code>out</code>. When an error occurs,
     * the error is stored and thrown in close().
     */
    private static class PipeThread extends Thread implements AutoCloseable {
        private final OutputStream out;
        private final InputStream in;
        private final Closeable streamOfProcess;
        private Throwable error;
        boolean reportErrors = true;

        /**
         * Creates a new PipeThread that transfers data from <code>in</code> to <code>out</code>
         *
         * @param in the source
         * @param out the destination
         * @param streamOfProcess the stream of the process (must be either <code>in</code> or
         *     <code>out</code>) this stream will be closed and IOExceptions on this stream will be
         *     ignored. The other stream will not be closed!
         */
        PipeThread(InputStream in, OutputStream out, Closeable streamOfProcess) {
            setDaemon(true);
            this.in = in;
            this.out = out;
            this.streamOfProcess = streamOfProcess;
        }

        @Override
        public final void run() {
            byte[] buffer = new byte[8192];
            int read;
            try {
                // normally we would use in.transferTo(out), but we do not want
                // capture exceptions of the process-stream, so there are two code
                // paths to handle this
                if (in == streamOfProcess) {
                    while ((read = readNoThrow(buffer)) >= 0) {
                        out.write(buffer, 0, read);
                        out.flush();
                    }
                } else {
                    assert out == streamOfProcess;
                    while ((read = in.read(buffer, 0, buffer.length)) >= 0) {
                        try {
                            out.write(buffer, 0, read);
                            out.flush();
                        } catch (IOException e) {
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                error = t;
            } finally {
                try {
                    streamOfProcess.close();
                } catch (IOException ignore) {
                    // ignore exception at end.
                }
            }
        }

        // helper: reads from stdOut/stdErr without throwing exceptions
        private int readNoThrow(byte[] buffer) {
            try {
                return in.read(buffer, 0, buffer.length);
            } catch (IOException e) {
                return -1;
            }
        }

        @Override
        public void close() {
            for (; ; ) {
                try {
                    join();
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (reportErrors && error != null) {
                throw Context.throwAsScriptRuntimeEx(error);
            }
        }
    }

    /** Used to kill process after timeout. */
    private static class KillThread extends Thread implements AutoCloseable {
        private final Process process;
        private final int timeout;
        volatile boolean killed;

        public KillThread(Process process, int timeout) {
            setDaemon(true);
            this.process = process;
            this.timeout = timeout;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(timeout);
                killed = true;
                process.destroy();
            } catch (InterruptedException e) {
                interrupt(); // re-interrupt this thread.
            }
        }

        @Override
        public void close() {
            interrupt();
        }
    }

    /**
     * Used as marked ByteArrayOutputStream. It indicates, that the content should be returned in
     * the "args" object.
     */
    private static class ReturnBuffer extends ByteArrayOutputStream {

        public ReturnBuffer(String init) {
            writeBytes(init.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public synchronized String toString() {
            return super.toString(StandardCharsets.UTF_8);
        }
    }
}
