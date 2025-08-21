package org.mozilla.javascript.tools.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.config.RhinoConfig;
import org.mozilla.javascript.testutils.Utils;
import org.mozilla.javascript.tools.shell.Global;

/**
 * Testcases for <code>global.runCommand</code>.
 *
 * <p>It uses a mock by default to test the general logic in Global.runCommand/ExecUtil
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
public class GlobalRunCommandTest {

    private static final boolean isWindows =
            System.getProperty("os.name").toLowerCase().contains("win");

    private static final int EXIT_CODE_WHEN_KILLED = isWindows ? 1 : 143;

    private static final String STDIN_TO_STDOUT =
            "runCommand('stdinToStdout', { input: stdIn, output: stdOut, err: stdErr, commandExecutor: commandExecutor })";
    private static final String STDIN_TO_STDERR =
            "runCommand('stdinToStderr', { input: stdIn, output: stdOut, err: stdErr, commandExecutor: commandExecutor })";

    private static final boolean mockOnly = RhinoConfig.get("rhino.test.runCommand.mockOnly", true);

    public static Stream<Arguments> executors() {
        if (mockOnly) {
            return Stream.of(Arguments.of(new ExecutorMock()));
        } else {
            return Stream.of(
                    Arguments.of(new ExecutorMock()),
                    Arguments.of(isWindows ? new ExecutorWindows() : new ExecutorLinux()));
        }
    }

    /**
     * returns a new global object with optional installed streams
     *
     * @param cx the context
     * @param exec the executor to use
     * @param streams the streams (stdIn, stdOut, stdErr)
     */
    private static Global getGlobal(Context cx, Global.CommandExecutor exec, Object... streams) {
        Global g = new Global(cx);
        WrapFactory wf = cx.getWrapFactory();
        g.put("commandExecutor", g, wf.wrap(cx, g, exec, Global.CommandExecutor.class));
        if (streams.length >= 1 && streams[0] != null)
            g.put("stdIn", g, wf.wrap(cx, g, streams[0], streams[0].getClass()));
        if (streams.length >= 2 && streams[1] != null)
            g.put("stdOut", g, wf.wrap(cx, g, streams[1], streams[1].getClass()));
        if (streams.length >= 3 && streams[2] != null)
            g.put("stdErr", g, wf.wrap(cx, g, streams[2], streams[2].getClass()));
        return g;
    }

    /** This is a simple 'hello world' test and tests the process return value. */
    @ParameterizedTest
    @MethodSource("executors")
    public void testHelloWorld(Global.CommandExecutor exec) {
        String cmd = "runCommand('helloWorld', { commandExecutor: commandExecutor })";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = getGlobal(cx, exec);
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(0, ((Number) result).intValue());
                    return null;
                });
    }

    /** Test env variables. */
    @ParameterizedTest
    @MethodSource("executors")
    public void testWithEnv(Global.CommandExecutor exec) {
        String cmd =
                "runCommand('env', { commandExecutor: commandExecutor, output: stdOut, env : { FOO: 'bar' } })";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
                    var g = getGlobal(cx, exec, null, stdOut);
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(0, ((Number) result).intValue());
                    assertEquals("bar" + System.lineSeparator(), stdOut.toString());
                    return null;
                });
    }

    /** Test env variables. */
    @ParameterizedTest
    @MethodSource("executors")
    public void testWithWorkingDir(Global.CommandExecutor exec) {
        String cmd =
                isWindows
                        ? "runCommand('pwd', { commandExecutor: commandExecutor, output: stdOut, dir: 'c:\\\\windows' })"
                        : "runCommand('pwd', { commandExecutor: commandExecutor, output: stdOut, dir: '/tmp' })";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
                    var g = getGlobal(cx, exec, null, stdOut);
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(0, ((Number) result).intValue());
                    if (isWindows) {
                        assertEquals(
                                "c:\\windows" + System.lineSeparator(),
                                stdOut.toString().toLowerCase());
                    } else {
                        assertEquals("/tmp" + System.lineSeparator(), stdOut.toString());
                    }
                    return null;
                });
    }

    /**
     * Tests, if we can read the stdout of the process. The stdout is stored into an existing
     * args-property.
     */
    @ParameterizedTest
    @MethodSource("executors")
    public void testReturnOutput(Global.CommandExecutor exec) {
        String cmd =
                "var x = { output:'alreadyThere', commandExecutor: commandExecutor };"
                        + "runCommand('helloWorld', x); x.output";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = getGlobal(cx, exec);
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    assertInstanceOf(String.class, result);
                    assertEquals("alreadyThereHello World" + System.lineSeparator(), result);
                    return null;
                });
    }

    /**
     * Tests, if we can read the stdout of the process. The stdout is written to an outputStream.
     */
    @ParameterizedTest
    @MethodSource("executors")
    public void testWithOutputStream(Global.CommandExecutor exec) {
        String cmd =
                "runCommand('helloWorld', { output: stdOut, commandExecutor: commandExecutor })";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
                    var g = getGlobal(cx, exec, null, stdOut);
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(0, ((Number) result).intValue());
                    try {
                        stdOut.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    assertEquals("Hello World" + System.lineSeparator(), stdOut.toString());
                    return null;
                });
    }

    /** Test, if input (stdIn) and output (stdOut/stdErr) will work. */
    @ParameterizedTest
    @MethodSource("executors")
    public void testWithInAndOut(Global.CommandExecutor exec) {

        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ByteArrayInputStream stdIn = new ByteArrayInputStream("Hello World".getBytes());
                    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
                    ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
                    var g = getGlobal(cx, exec, stdIn, stdOut, stdErr);
                    cx.evaluateString(g, STDIN_TO_STDOUT, "test.js", 1, null);
                    g.put("stdIn", g, "Some error");
                    cx.evaluateString(g, STDIN_TO_STDERR, "test.js", 1, null);

                    try {
                        stdOut.close();
                        stdErr.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    assertEquals("Hello World", stdOut.toString());
                    assertEquals("Some error", stdErr.toString());
                    return null;
                });
    }

    /**
     * Tests, if we can stream more than 4G. This ensures, that there is no limiting (32 bit) buffer
     * in the pipeline. Note: This test runs only in interpreted mode, to saves some time
     */
    @ParameterizedTest
    @MethodSource("executors")
    public void testStreamingMoreThan4G(Global.CommandExecutor exec) {
        long bytes = 1L << 32; // this is 0x1_0000_0000 hex

        Utils.runWithMode(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    FakeOutputStream stdOut = new FakeOutputStream();
                    var g = getGlobal(cx, exec, new FakeInputStream(bytes), stdOut, stdOut);
                    var result = cx.evaluateString(g, STDIN_TO_STDOUT, "test.js", 1, null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(0, ((Number) result).intValue());
                    try {
                        stdOut.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    assertEquals(bytes, stdOut.bytes);
                    return null;
                },
                true);
    }

    /** Tests, what happens, when the process closes the inputStream after a given amount of data */
    @ParameterizedTest
    @MethodSource("executors")
    public void testStreamingWithLimit(Global.CommandExecutor exec) {
        if (isWindows && exec instanceof ExecutorWindows) {
            return; // no solution for windows, yet.
        }
        doStream(exec, "stream16MB", 0, 0);
        doStream(exec, "stream16MB", 1024, 1024);
        doStream(exec, "stream16MB", 1024 * 1024, 1024 * 1024);
        doStream(
                exec,
                "stream16MB",
                4096 * 4096,
                4096 * 4096); // this is the max bytes that can be streamed
        doStream(exec, "stream16MB", 4096 * 4096 + 1, 4096 * 4096);
        doStream(
                exec,
                "stream16MB",
                8192 * 4096,
                4096 * 4096); // we do *NOT* expect any exception here

        if (exec instanceof ExecutorLinux) {
            return;
        }
        // Note: we rely on the mock here, as this behaviour is not easily to test with OS commands
        doStream(exec, "stream16MBread", 0, 0);
        doStream(exec, "stream16MBread", 1024, 1024);
        doStream(exec, "stream16MBread", 1024 * 1024, 1024 * 1024);
        doStream(
                exec,
                "stream16MBread",
                4096 * 4096,
                4096 * 4096); // this is the max bytes that can be streamed
        doStream(exec, "stream16MBread", 4096 * 4096 + 1, 4096 * 4096);
        doStream(
                exec,
                "stream16MBread",
                8192 * 4096,
                4096 * 4096); // we do *NOT* expect any exception here
    }

    private void doStream(Global.CommandExecutor exec, String alias, long bytes, long expected) {
        String cmd =
                "runCommand('"
                        + alias
                        + "', { input: stdIn, output: stdOut, err: stdErr, commandExecutor: commandExecutor })";
        Utils.runWithMode(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    FakeOutputStream stdOut = new FakeOutputStream();
                    FakeOutputStream stdErr = new FakeOutputStream();
                    var g = getGlobal(cx, exec, new FakeInputStream(bytes), stdOut, stdErr);
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(0, ((Number) result).intValue());
                    try {
                        stdOut.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    assertEquals(expected, stdOut.bytes);
                    return null;
                },
                true);
    }

    /**
     * Thests, if the timeout will work. A command, that runs ~5sec should be terminated after
     * 500ms.
     */
    @ParameterizedTest
    @MethodSource("executors")
    public void testTimeout(Global.CommandExecutor exec) {
        String cmd = "runCommand('sleep',{ timeout: 500, commandExecutor: commandExecutor })";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = getGlobal(cx, exec);
                    long start = System.currentTimeMillis();
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    long duration = System.currentTimeMillis() - start;
                    assertInstanceOf(Number.class, result);
                    assertEquals(EXIT_CODE_WHEN_KILLED, ((Number) result).intValue()); // Sigterm
                    if (exec instanceof ExecutorMock) {
                        // NOTE: We check only the return value here, which iś 1 (windows) or 143
                        // (linux) and do no time measurements.
                        // (the mock expects to be killed, otherwise, it waits forever)
                        return null;
                    }
                    assertTrue(duration >= 500);
                    assertTrue(duration <= 1500);
                    return null;
                });
    }

    /**
     * Test, if we get an exception, when the inputStream throws an error. It is expected, that the
     * exception is passed to the caller
     */
    @ParameterizedTest
    @MethodSource("executors")
    public void testThrowOnInput(Global.CommandExecutor exec) {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    FakeOutputStream stdOut = new FakeOutputStream();
                    var g = getGlobal(cx, exec, new ThrowingInputStream(), stdOut, stdOut);

                    assertThrows(
                            WrappedException.class,
                            () -> cx.evaluateString(g, STDIN_TO_STDOUT, "test.js", 1, null));

                    try {
                        stdOut.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    assertEquals(0, stdOut.bytes);
                    return null;
                });
    }

    /**
     * Test, if we get an exception, when the outputstream throws an error. It is expected, that the
     * exception is passed to the caller
     */
    @ParameterizedTest
    @MethodSource("executors")
    public void testThrowOnOutput(Global.CommandExecutor exec) {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g =
                            getGlobal(
                                    cx,
                                    exec,
                                    "hello world",
                                    new ThrowingOutputStream(),
                                    new ThrowingOutputStream());

                    assertThrows(
                            WrappedException.class,
                            () -> cx.evaluateString(g, STDIN_TO_STDOUT, "test.js", 1, null));

                    assertThrows(
                            WrappedException.class,
                            () -> cx.evaluateString(g, STDIN_TO_STDERR, "test.js", 1, null));

                    return null;
                });
    }

    /**
     * This test streams 10GB through stdout + stderr in interpreted and compiled mode.
     *
     * <p>This test is disabled.
     *
     * <p>I tried to compare <code>
     * while ((read = in.read(buffer, 0, 4096)) >= 0) {} out.write / out.flush }</code> vs <code>
     * in.transferTo(out)</code>
     *
     * <ul>
     *   <li>A buffer of 4096 gives the max. performance on <b>on linux</b> and iis ~30% faster than
     *       <code>in.transferTo</code>
     *   <li><code>in.transferTo</code> is twice as fast as using a buffer <b>on windows</b>
     * </ul>
     *
     * <p>The expected transferrate is ~1-2GB/s - the test is disabled, as it takes a long time
     */
    @ParameterizedTest
    @MethodSource("executors")
    @Disabled
    public void testStreaming10gb(Global.CommandExecutor exec) {
        long tenGig = 10_000_000_000L;
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    FakeOutputStream stdOut = new FakeOutputStream();
                    FakeOutputStream stdErr = new FakeOutputStream();
                    var g = getGlobal(cx, exec, new FakeInputStream(tenGig), stdOut, stdErr);
                    cx.evaluateString(g, STDIN_TO_STDOUT, "test.js", 1, null);

                    g = getGlobal(cx, exec, new FakeInputStream(tenGig), stdOut, stdErr);
                    cx.evaluateString(g, STDIN_TO_STDERR, "test.js", 1, null);

                    try {
                        stdOut.close();
                        stdErr.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    assertEquals(tenGig, stdOut.bytes);
                    assertEquals(tenGig, stdErr.bytes);
                    return null;
                });
    }

    /** Stream produces a stream returning 'bytes' zeros. */
    static class FakeInputStream extends InputStream {
        long bytes;

        public FakeInputStream(long bytes) {
            this.bytes = bytes;
        }

        @Override
        public int read() {
            if (bytes <= 0) {
                return -1;
            }
            bytes--;
            return 42;
        }

        @Override
        public int read(byte[] b, int off, int len) {

            if (bytes >= len) {
                bytes -= len;
                return len;
            } else if (bytes > 0) {
                len = (int) bytes;
                bytes = 0L;
                return len;
            } else {
                return -1;
            }
        }
    }

    /** Stream counts the received bytes. */
    static class FakeOutputStream extends OutputStream {
        long bytes;

        @Override
        public void write(int b) {
            bytes++;
        }

        @Override
        public void write(byte[] b, int off, int len) {
            bytes += len;
        }

        @Override
        public void close() throws IOException {
            super.close();
        }
    }

    /** Stream throws exception on read */
    static class ThrowingInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            throw new IOException("ThrowingInputStream");
        }
    }

    /** Stream throws exception on write */
    static class ThrowingOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            throw new IOException("ThrowingInputStream");
        }
    }

    /**
     * executor for linux based OS. It translates the command aliases to a specific linux command.
     */
    private static class ExecutorLinux implements Global.CommandExecutor {
        @Override
        public Process exec(String[] cmdarray, String[] envp, File dir) throws IOException {
            switch (cmdarray[0]) {
                case "helloWorld":
                    cmdarray = new String[] {"/usr/bin/env", "bash", "-c", "echo Hello World"};
                    break;
                case "env":
                    cmdarray = new String[] {"/usr/bin/env", "bash", "-c", "echo $FOO"};
                    break;
                case "pwd":
                    cmdarray = new String[] {"/usr/bin/env", "bash", "-c", "pwd"};
                    break;
                case "stdinToStdout":
                    cmdarray = new String[] {"/usr/bin/cat"};
                    break;
                case "stream16MB":
                case "stream16MBread": // technically the same (mock is more precise here)
                    cmdarray =
                            new String[] {
                                "/usr/bin/dd",
                                "bs=4096",
                                "count=4096",
                                "if=/dev/stdin",
                                "of=/dev/stdout"
                            };
                    break;
                case "stdinToStderr":
                    cmdarray = new String[] {"/usr/bin/env", "bash", "-c", "cat >&2"};
                    break;
                case "sleep":
                    cmdarray = new String[] {"/usr/bin/sleep", "5"};
                    break;
                default:
                    throw new IllegalArgumentException(cmdarray[0]);
            }
            return Runtime.getRuntime().exec(cmdarray, envp, dir);
        }

        @Override
        public String toString() {
            return "linux";
        }
    }

    /**
     * executor for windows based OS. It translates the command aliases to a specific linux command.
     */
    private static class ExecutorWindows implements Global.CommandExecutor {
        @Override
        public Process exec(String[] cmdarray, String[] envp, File dir) throws IOException {
            switch (cmdarray[0]) {
                case "helloWorld":
                    cmdarray = new String[] {"cmd.exe", "/c", "echo Hello World"};
                    break;
                case "env":
                    cmdarray = new String[] {"cmd.exe", "/c", "echo %FOO%"};
                    break;
                case "pwd":
                    cmdarray = new String[] {"cmd.exe", "/c", "cd"};
                    break;
                case "stdinToStdout":
                    // https://stackoverflow.com/questions/52330841/command-which-copies-stdin-to-stdout
                    cmdarray =
                            new String[] {
                                "powershell.exe",
                                "-c",
                                "[Console]::OpenStandardInput().CopyTo([Console]::OpenStandardOutput())"
                            };
                    break;
                case "stdinToStderr":
                    // https://stackoverflow.com/questions/52330841/command-which-copies-stdin-to-stdout
                    cmdarray =
                            new String[] {
                                "powershell.exe",
                                "-c",
                                "[Console]::OpenStandardInput().CopyTo([Console]::OpenStandardError())"
                            };
                    break;
                case "sleep":
                    cmdarray = new String[] {"ping", "127.0.0.1", "-n", "5"};
                    break;
                default:
                    throw new IllegalArgumentException(cmdarray[0]);
            }
            return Runtime.getRuntime().exec(cmdarray, envp, dir);
        }

        @Override
        public String toString() {
            return "windows";
        }
    }

    /**
     * Mocking executor. Tries to model the equivalent linux/windows commands without routing to
     * command line so that we do not depend on external binaries.
     */
    private static class ExecutorMock implements Global.CommandExecutor {
        @Override
        public Process exec(String[] cmdarray, String[] envp, File dir) throws IOException {
            Process p;
            String s;
            switch (cmdarray[0]) {
                case "helloWorld":
                    p = new MockStreamCopyProcess(false);
                    s = "Hello World" + System.lineSeparator();
                    p.getOutputStream().write(s.getBytes(StandardCharsets.UTF_8));
                    return p;

                case "env":
                    p = new MockStreamCopyProcess(false);
                    assertArrayEquals(envp, new String[] {"FOO=bar"});
                    s = "bar" + System.lineSeparator();
                    p.getOutputStream().write(s.getBytes(StandardCharsets.UTF_8));
                    return p;
                case "pwd":
                    p = new MockStreamCopyProcess(false);
                    s = dir + System.lineSeparator();
                    p.getOutputStream().write(s.getBytes(StandardCharsets.UTF_8));
                    return p;
                case "stdinToStdout":
                    return new MockStreamCopyProcess(false);
                case "stdinToStderr":
                    return new MockStreamCopyProcess(true);
                case "stream16MB":
                    return new MockStreamCopyProcess(false, 4096 * 4096, -1);
                case "stream16MBread":
                    return new MockStreamCopyProcess(false, -1, 4096 * 4096);
                case "sleep":
                    return new MockSleepProcess();
                default:
                    throw new IllegalArgumentException(cmdarray[0]);
            }
        }

        @Override
        public String toString() {
            return "mock";
        }
    }

    /** Mock, that copies stdIn to stdOut/stdErr */
    private static class MockStreamCopyProcess extends Process {
        final CountDownLatch latch = new CountDownLatch(1);
        final OutputStream stdIn;
        final InputStream stdOut;
        final InputStream stdErr;
        final int writeLimit;

        MockStreamCopyProcess(boolean toStdErr) throws IOException {
            this(toStdErr, -1, -1);
        }

        MockStreamCopyProcess(boolean toStdErr, final int writeLimit, int readLimit)
                throws IOException {
            PipedOutputStream src =
                    new PipedOutputStream() {

                        @Override
                        public void close() throws IOException {
                            latch.countDown();
                            super.close();
                        }
                    };
            // use 1Meg pipe-size to get good performance on streaming tests.
            InputStream dest =
                    new PipedInputStream(src, 1 << 20) {
                        @Override
                        public void close() throws IOException {
                            latch.countDown();
                            super.close();
                        }
                    };
            stdIn = writeLimit == -1 ? src : new SizeLimitOutputStream(src, writeLimit);
            if (readLimit != -1) {
                dest = new SizeLimitInputStream(dest, readLimit);
            }
            if (toStdErr) {
                stdErr = dest;
                stdOut = new FakeInputStream(0);
            } else {
                stdErr = new FakeInputStream(0);
                stdOut = dest;
            }
            this.writeLimit = writeLimit;
        }

        @Override
        public OutputStream getOutputStream() {
            return stdIn;
        }

        @Override
        public InputStream getInputStream() {
            return stdOut;
        }

        @Override
        public InputStream getErrorStream() {
            return stdErr;
        }

        @Override
        public int waitFor() throws InterruptedException {
            latch.await();
            return 0;
        }

        @Override
        public int exitValue() {
            if (latch.getCount() > 0) {
                throw new IllegalThreadStateException();
            }
            return 0;
        }

        @Override
        public void destroy() {
            latch.countDown();
        }
    }

    private static class SizeLimitOutputStream extends FilterOutputStream {
        private int bytesLeft;

        public SizeLimitOutputStream(OutputStream out, int bytesLeft) {
            super(out);
            this.bytesLeft = bytesLeft;
        }

        @Override
        public void write(int b) throws IOException {
            if (bytesLeft > 0) {
                bytesLeft--;
                out.write(b);
            } else {
                close();
                throw new IOException("WriteLimit reached");
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (bytesLeft > len) {
                bytesLeft -= len;
                out.write(b, off, len);

            } else if (bytesLeft > 0) {
                out.write(b, off, bytesLeft);
                bytesLeft = 0;
            } else {
                close();
                throw new IOException("WriteLimit reached");
            }
        }
    }

    private static class SizeLimitInputStream extends FilterInputStream {
        private int bytesLeft;

        public SizeLimitInputStream(InputStream in, int bytesLeft) {
            super(in);
            this.bytesLeft = bytesLeft;
        }

        @Override
        public int read() throws IOException {
            if (bytesLeft > 0) {
                int read = in.read();
                if (read == -1) {
                    bytesLeft--;
                }
                return read;
            } else {
                close();
                throw new IOException("ReadLimit reached");
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (bytesLeft > 0) {
                int ret = in.read(b, off, Math.min(len, bytesLeft));
                bytesLeft -= ret;
                return ret;
            } else {
                close();
                throw new IOException("ReadLimit reached");
            }
        }
    }

    /** Mock, that sleeps and waits for kill. */
    private static class MockSleepProcess extends Process {
        final CountDownLatch latch = new CountDownLatch(1);
        final OutputStream stdIn = new FakeOutputStream();
        final InputStream stdOut = new FakeInputStream(0);
        final InputStream stdErr = new FakeInputStream(0);
        int exitValue;

        @Override
        public OutputStream getOutputStream() {
            return stdIn;
        }

        @Override
        public InputStream getInputStream() {
            return stdOut;
        }

        @Override
        public InputStream getErrorStream() {
            return stdErr;
        }

        @Override
        public int waitFor() {
            try {
                // wait max 5 minutes. This should be enough to kill process even on very high load.
                latch.await(5, TimeUnit.MINUTES);
                // terminated by timeout. use os specific values to match with assert
                exitValue = EXIT_CODE_WHEN_KILLED;
            } catch (InterruptedException ie) {
                // process terminated normally (unexpected)
                exitValue = 0;
            }
            return exitValue;
        }

        @Override
        public int exitValue() {
            if (latch.getCount() > 0) {
                throw new IllegalThreadStateException();
            }
            return exitValue;
        }

        @Override
        public void destroy() {
            latch.countDown();
        }
    }
}
