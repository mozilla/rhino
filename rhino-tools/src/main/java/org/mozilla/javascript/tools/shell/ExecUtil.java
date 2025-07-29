package org.mozilla.javascript.tools.shell;

import org.mozilla.javascript.Context;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Helper class for <code>global.runCommand</code>.
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
class ExecUtil {
	private ExecUtil() {
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

		try (
				PipeThread inThread = createToProcessThread(in, p.getOutputStream());
				PipeThread outThread = createFromProcessthread(p.getInputStream(), out);
				PipeThread errThread = createFromProcessthread(p.getErrorStream(), err);
		) {

			try {
				// wait for process completion
				p.waitFor();
				stopThread(inThread);
				stopThread(outThread);
				stopThread(errThread);
			} catch (InterruptedException ignore) {
				Thread.currentThread().interrupt();
			}
			return p.exitValue();
		} finally {
			p.destroy();
		}
	}

	private static PipeThread createToProcessThread(InputStream in, OutputStream out) throws IOException {
		if (in == null) {
			out.close();
			return null;
		} else {
			return new ToProcessPipeThread(in, out);
		}
	}

	private static PipeThread createFromProcessthread(InputStream in, OutputStream out) throws IOException {
		if (out == null) {
			in.close();
			return null;
		} else {
			return new FromProcessPipeThread(in, out);
		}
	}

	private static void stopThread(PipeThread thread) throws InterruptedException {
		if (thread != null) {
			thread.join();
		}
	}

	private static Throwable handleException(PipeThread thread, Throwable currentException) {
		if (thread == null || thread.error == null) {
			return currentException;
		} else if (currentException == null) {
			return thread.error;
		} else {
			currentException.addSuppressed(thread.error);
			return currentException;
		}
	}

	/**
	 * A PipeThread copies data from <code>from</code> to <code>to</code>. There are two implementations which are slightly different in stream closing and exception handling
	 * <ul>
	 *     <li><b>FromProcessPipeThread:</b> Reads from the process (stdOut/stdErr)</li>
	 *     <li><b>ToProcessPipeThread:</b> Writes to the process (stdIn)</li>
	 * </ul>
	 */
	private abstract static class PipeThread extends Thread implements Closeable {

		PipeThread(InputStream from, OutputStream to) {
			setDaemon(true);
			this.from = from;
			this.to = to;
		}

		@Override
		public final void run() {
			try {
				copyStream();
				to.flush();
			} catch (Throwable t) {
				if (t instanceof InterruptedException) {
					// ignore
				} else {
					error = t;
				}
			} finally {
				try {
					closeStream();
				} catch (IOException ignore) {
					// Ignore errors on close. On Windows JVM may throw invalid
					// refrence exception if process terminates too fast.
				}
			}
		}

		abstract void copyStream() throws IOException;

		abstract void closeStream() throws IOException;

		@Override
		public void close() {
			try {
				join();
			} catch (InterruptedException e) {
				// ignore
			}
			if (error != null) {
				Context.throwAsScriptRuntimeEx(error);
			}
		}

		private static final int SIZE = 8192; // aligned with the default buffer size in BufferedWriter
		final byte[] buffer = new byte[SIZE];
		final OutputStream to;
		final InputStream from;
		Throwable error;
	}

	private static class FromProcessPipeThread extends PipeThread {
		public FromProcessPipeThread(InputStream from, OutputStream to) {
			super(from, to);
		}

		@Override
		public void copyStream() throws IOException {
			int n;
			for (; ; ) {
				try {
					n = from.read(buffer);
				} catch (IOException ex) {
					// Ignore exception as it can be cause by closed pipe
					break;
				}
				if (n < 0) {
					break;
				}
				to.write(buffer, 0, n);
			}
		}

		@Override
		void closeStream() throws IOException {
			from.close();
		}
	}

	private static class ToProcessPipeThread extends PipeThread {
		public ToProcessPipeThread(InputStream from, OutputStream to) {
			super(from, to);
		}

		@Override
		public void copyStream() throws IOException {
			int n;
			for (; ; ) {
				n = from.read(buffer);
				if (n < 0) {
					break;
				}
				try {
					to.write(buffer, 0, n);
				} catch (IOException ex) {
					// Ignore exception as it can be cause by closed pipe
					break;
				}
			}
		}

		@Override
		void closeStream() throws IOException {
			to.close();
		}
	}

}

