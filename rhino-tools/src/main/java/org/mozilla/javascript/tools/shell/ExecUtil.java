package org.mozilla.javascript.tools.shell;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.mozilla.javascript.Context;

/**
 * Helper class for Global process execution.
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
class ExecUtil {
    private ExecUtil() {}

    private static class PipeThread extends Thread {

        PipeThread(boolean fromProcess, InputStream from, OutputStream to) {
            setDaemon(true);
            this.fromProcess = fromProcess;
            this.from = from;
            this.to = to;
        }

        @Override
        public void run() {
            try {
                int n;
                do {
                    n = read();
                    if (n < 0) {
                        break;
                    }
                } while (write(n));
            } catch (IOException ex) {
                throw Context.throwAsScriptRuntimeEx(ex);

            } finally {
                close();
            }
        }

        private void close() {
            try {
                if (fromProcess) {
                    from.close();
                } else {
                    to.close();
                }
            } catch (IOException ex) {
                // Ignore errors on close. On Windows JVM may throw invalid
                // refrence exception if process terminates too fast.
            }
        }

        /**
         * reads the inputStream. If the 'from' is the a process stream (reading stdOut/stdErr from
         * process), we ignore read-errors (when process may have closed the pipe)
         *
         * @return number of read bytes or -1.
         */
        private int read() throws IOException {
            try {
                return from.read(buffer);
            } catch (IOException ex) {
                // Ignore exception as it can be cause by closed pipe
                if (fromProcess) {
                    return -1;
                } else {
                    throw ex;
                }
            }
        }

        /**
         * reads the inputStream. If the 'to' is the a process stream (writing to stdIn of process),
         * we ignore write-errors (when process may have closed the pipe)
         *
         * @return read bytes or -1.
         */
        private boolean write(int n) throws IOException {
            try {
                to.write(buffer, 0, n);
                to.flush();
            } catch (IOException ex) {
                if (fromProcess) {
                    throw ex;
                } else {
                    // to is a process stream -> Ignore exception as it can be cause by closed pipe
                    return false;
                }
            }
            return true;
        }

        private static final int SIZE = 4096;
        private final byte[] buffer = new byte[SIZE];
        private final boolean fromProcess;
        private final InputStream from;
        private final OutputStream to;
    }

    /**
     * Runs the given process using Runtime.exec(). If any of in, out, err is null, the
     * corresponding process stream will be closed immediately, otherwise it will be closed as soon
     * as all data will be read from/written to process
     *
     * @return Exit value of process.
     * @throws IOException If there was an error executing the process.
     */
    static int runProcess(
            String[] cmd,
            String[] environment,
            File wd,
            InputStream in,
            OutputStream out,
            OutputStream err)
            throws IOException {
        Process p = Runtime.getRuntime().exec(cmd, environment, wd);

        PipeThread inThread = null;
        PipeThread outThread = null;
        PipeThread errThread = null;
        try {
            if (in != null) {
                inThread = new PipeThread(false, in, p.getOutputStream());
                inThread.start();
            } else {
                p.getOutputStream().close();
            }

            if (out != null) {
                outThread = new PipeThread(true, p.getInputStream(), out);
                outThread.start();
            } else {
                p.getInputStream().close();
            }

            if (err != null) {
                errThread = new PipeThread(true, p.getErrorStream(), err);
                errThread.start();
            } else {
                p.getErrorStream().close();
            }

            try {
                // wait for process completion
                p.waitFor();
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
            return p.exitValue();
        } finally {
            for (; ; ) {
                try {
                    if (outThread != null) {
                        outThread.join();
                    }
                    if (inThread != null) {
                        inThread.join();
                    }
                    if (errThread != null) {
                        errThread.join();
                    }
                    break;
                } catch (InterruptedException ignore) {
                    Thread.currentThread().interrupt();
                }
            }
            p.destroy();
        }
    }
}
