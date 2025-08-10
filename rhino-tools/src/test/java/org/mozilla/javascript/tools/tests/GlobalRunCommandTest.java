package org.mozilla.javascript.tools.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.testutils.Utils;
import org.mozilla.javascript.tools.shell.Global;

/**
 * Testcases for <code>global.runCommand</code>
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
public class GlobalRunCommandTest {

    private static final boolean isWindows =
            System.getProperty("os.name").toLowerCase().contains("win");

    private static final String STDIN_TO_STDOUT =
            "runCommand('stdinToStdout', { input: stdIn, output: stdOut, err: stdErr, commandExecutor: commandExecutor })";
    private static final String STDIN_TO_STDERR =
            "runCommand('stdinToStderr', { input: stdIn, output: stdOut, err: stdErr, commandExecutor: commandExecutor })";

    private static Global.CommandExecutor getExecutor() {
        return isWindows ? new ExecutorWindows() : new ExecutorLinux();
    }

    /**
     * returns a new global object with optional installed streams
     *
     * @param cx the context
     * @param streams the streams (stdIn, stdOut, stdErr)
     */
    private static Global getGlobal(Context cx, Object... streams) {
        Global g = new Global(cx);
        WrapFactory wf = cx.getWrapFactory();
        g.put("commandExecutor", g, wf.wrap(cx, g, getExecutor(), Global.CommandExecutor.class));
        if (streams.length >= 1 && streams[0] != null)
            g.put("stdIn", g, wf.wrap(cx, g, streams[0], streams[0].getClass()));
        if (streams.length >= 2 && streams[1] != null)
            g.put("stdOut", g, wf.wrap(cx, g, streams[1], streams[1].getClass()));
        if (streams.length >= 3 && streams[2] != null)
            g.put("stdErr", g, wf.wrap(cx, g, streams[2], streams[2].getClass()));
        return g;
    }

    /** This is a simple 'hello world' test and tests the process return value. */
    @Test
    public void test() {
        String cmd = "runCommand('helloWorld', { commandExecutor: commandExecutor })";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = getGlobal(cx);
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(0, ((Number) result).intValue());
                    return null;
                });
    }

    /**
     * Tests, if we can read the stdout of the process. The stdout is stored into an existing
     * args-property.
     */
    @Test
    public void testReturnOutput() {
        String cmd =
                "var x = { output:'alreadyThere', commandExecutor: commandExecutor };\n"
                        + "runCommand('helloWorld', x); x.output";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = getGlobal(cx);
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    assertInstanceOf(String.class, result);
                    result = result.toString().replace("\r\n", "\n"); // normalize crlf
                    assertEquals("alreadyThereHello World\n", result);
                    return null;
                });
    }

    /**
     * Tests, if we can read the stdout of the process. The stdout is written to an outputStream.
     */
    @Test
    public void testWithOutputStream() {
        String cmd =
                "runCommand('helloWorld', { output: stdOut, commandExecutor: commandExecutor })";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
                    var g = getGlobal(cx, null, stdOut);
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(0, ((Number) result).intValue());
                    try {
                        stdOut.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    assertEquals("Hello World\n", stdOut.toString().replace("\r\n", "\n"));
                    return null;
                });
    }

    /** Test, if input (stdIn) and output (stdOut/stdErr) will work. */
    @Test
    public void testWithInAndOut() {

        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ByteArrayInputStream stdIn = new ByteArrayInputStream("Hello World".getBytes());
                    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
                    ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
                    var g = getGlobal(cx, stdIn, stdOut, stdErr);
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
    @Test
    public void testStreamingMoreThan4G() {
        long bytes = 1L << 32; // this is 0x1_0000_0000 hex

        Utils.runWithMode(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    FakeOutputStream stdOut = new FakeOutputStream();
                    var g = getGlobal(cx, new FakeInputStream(bytes), stdOut, stdOut);
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

    /**
     * Thests, if the timeout will work. A command, that runs ~5sec should be terminated after
     * 500ms.
     */
    @Test
    public void testTimeout() {
        String cmd = "runCommand('sleep5',{ timeout: 500, commandExecutor: commandExecutor })";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = getGlobal(cx);
                    long start = System.currentTimeMillis();
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    long duration = System.currentTimeMillis() - start;
                    assertTrue(duration >= 500);
                    assertTrue(duration <= 1500);
                    assertInstanceOf(Number.class, result);
                    assertEquals(isWindows ? 1 : 143, ((Number) result).intValue()); // Sigterm
                    return null;
                });
    }

    /**
     * Test, if we get an exception, when the inputStream throws an error. It is expected, that the
     * exception is passed to the caller
     */
    @Test
    public void testThrowOnInpupt() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    FakeOutputStream stdOut = new FakeOutputStream();
                    var g = getGlobal(cx, new ThrowingInputStream(), stdOut, stdOut);

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
    @Test
    public void testThrowOnOutput() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g =
                            getGlobal(
                                    cx,
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
    @Test
    @Disabled
    public void testStreaming10gb() {
        long tenGig = 10_000_000_000L;
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    FakeOutputStream stdOut = new FakeOutputStream();
                    FakeOutputStream stdErr = new FakeOutputStream();
                    var g = getGlobal(cx, new FakeInputStream(tenGig), stdOut, stdErr);
                    cx.evaluateString(g, STDIN_TO_STDOUT, "test.js", 1, null);

                    g = getGlobal(cx, new FakeInputStream(tenGig), stdOut, stdErr);
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
                case "stdinToStdout":
                    cmdarray = new String[] {"/usr/bin/cat"};
                    break;
                case "stdinToStderr":
                    cmdarray = new String[] {"/usr/bin/env", "bash", "-c", "cat >&2"};
                    break;
                case "sleep5":
                    cmdarray = new String[] {"/usr/bin/sleep", "5"};
                    break;
                default:
                    throw new IllegalArgumentException(cmdarray[0]);
            }
            return Runtime.getRuntime().exec(cmdarray, envp, dir);
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
                case "sleep5":
                    cmdarray = new String[] {"ping", "127.0.0.1", "-n", "5"};
                    break;
                default:
                    throw new IllegalArgumentException(cmdarray[0]);
            }
            return Runtime.getRuntime().exec(cmdarray, envp, dir);
        }
    }
}
