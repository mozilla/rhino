package org.mozilla.javascript.tools.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
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

    // https://stackoverflow.com/questions/52330841/command-which-copies-stdin-to-stdout
    private static final String STDIN_TO_STDOUT =
            isWindows
                    ? "runCommand('powershell.exe', '-c', '[Console]::OpenStandardInput().CopyTo([Console]::OpenStandardOutput())', { input: stdIn, output: stdOut, err: stdErr })"
                    : "runCommand('/usr/bin/cat', { input: stdIn, output: stdOut, err: stdErr })";
    private static final String STDIN_TO_STDERR =
            isWindows
                    ? "runCommand('powershell.exe', '-c', '[Console]::OpenStandardInput().CopyTo([Console]::OpenStandardError())', { input: stdIn, output: stdOut, err: stdErr })"
                    : "runCommand('/usr/bin/env', 'bash', '-c', 'cat >&2', { input: stdIn, output: stdOut, err: stdErr })";

    @Test
    public void test() {
        String cmd =
                isWindows
                        ? "runCommand('cmd.exe', '/c', 'echo Hello World')"
                        : "runCommand('/usr/bin/env', 'bash', '-c', 'echo Hello World')";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = new Global(cx);
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(0, ((Number) result).intValue());
                    return null;
                });
    }

    @Test
    public void testReturnOutput() {
        String cmd =
                isWindows
                        ? "var x = { output:'alreadyThere'}; runCommand('cmd.exe', '/c', 'echo Hello World', x); x.output"
                        : "var x = { output:'alreadyThere'};  runCommand('/usr/bin/env', 'bash', '-c', 'echo Hello World', x); x.output";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = new Global(cx);
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    assertInstanceOf(String.class, result);
                    if (isWindows) {
                        assertEquals("alreadyThereHello World\r\n", result);
                    } else {
                        assertEquals("alreadyThereHello World\n", result);
                    }
                    return null;
                });
    }

    @Test
    public void testTimeout() {
        String cmd =
                isWindows
                        ? "runCommand('ping',{ timeout: 500, args : ['127.0.0.1', '-n', 5] })"
                        : "runCommand('/usr/bin/sleep',{ timeout: 500, args : [5] })";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = new Global(cx);
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

    @Test
    public void testWithOutputStream() {
        String cmd =
                isWindows
                        ? "runCommand('cmd.exe', '/c', 'echo Hello World', { output: stdOut})"
                        : "runCommand('/usr/bin/env', 'bash', '-c', 'echo Hello World', { output: stdOut})";
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = new Global(cx);
                    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
                    g.put("stdOut", g, cx.getWrapFactory().wrap(cx, g, stdOut, OutputStream.class));
                    var result = cx.evaluateString(g, cmd, "test.js", 1, null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(0, ((Number) result).intValue());
                    try {
                        stdOut.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    if (isWindows) {
                        assertEquals("Hello World\r\n", stdOut.toString());
                    } else {
                        assertEquals("Hello World\n", stdOut.toString());
                    }
                    return null;
                });
    }

    @Test
    public void testWithInAndOut() {

        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = new Global(cx);
                    ByteArrayInputStream stdIn = new ByteArrayInputStream("Hello World".getBytes());
                    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
                    ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
                    g.put("stdIn", g, cx.getWrapFactory().wrap(cx, g, stdIn, InputStream.class));
                    g.put("stdOut", g, cx.getWrapFactory().wrap(cx, g, stdOut, OutputStream.class));
                    g.put("stdErr", g, cx.getWrapFactory().wrap(cx, g, stdErr, OutputStream.class));
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
     * in the pipeline. Note: This test runs only in interpreted mode, to saves ome time
     */
    @Test
    public void testStreamingMoreThan4G() {
        long bytes = 1L << 32; // this is 0x1_0000_0000 hex

        Utils.runWithMode(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = new Global(cx);
                    FakeOutputStream stdOut = new FakeOutputStream();

                    g.put(
                            "stdIn",
                            g,
                            cx.getWrapFactory()
                                    .wrap(cx, g, new FakeInputStream(bytes), InputStream.class));
                    g.put("stdOut", g, cx.getWrapFactory().wrap(cx, g, stdOut, OutputStream.class));
                    g.put("stdErr", g, cx.getWrapFactory().wrap(cx, g, stdOut, OutputStream.class));
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
     * This test streams 10GB through stdout + stderr in interpreted and compiled mode.
     *
     * <p>This test is disabled.
     *
     * <p>it runs about 35s and transfers 40GB of data. The expected throughput is slightly 1GB/s
     */
    @Test
    public void testThrowOnInpupt() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = new Global(cx);
                    FakeOutputStream stdOut = new FakeOutputStream();
                    g.put(
                            "stdIn",
                            g,
                            cx.getWrapFactory()
                                    .wrap(cx, g, new ThrowingInputStream(), InputStream.class));
                    g.put("stdOut", g, cx.getWrapFactory().wrap(cx, g, stdOut, OutputStream.class));
                    g.put("stdErr", g, cx.getWrapFactory().wrap(cx, g, stdOut, OutputStream.class));

                    assertThrows(
                            WrappedException.class,
                            () -> cx.evaluateString(g, STDIN_TO_STDOUT, "test.js", 1, null));

                    try {
                        stdOut.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    // assertEquals(tenGig, stdOut.bytes);
                    // assertEquals(tenGig, stdErr.bytes);
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
                    var g = new Global(cx);
                    FakeOutputStream stdOut = new FakeOutputStream();
                    FakeOutputStream stdErr = new FakeOutputStream();
                    g.put(
                            "stdIn",
                            g,
                            cx.getWrapFactory()
                                    .wrap(cx, g, new FakeInputStream(tenGig), InputStream.class));
                    g.put("stdOut", g, cx.getWrapFactory().wrap(cx, g, stdOut, OutputStream.class));
                    g.put("stdErr", g, cx.getWrapFactory().wrap(cx, g, stdErr, OutputStream.class));
                    cx.evaluateString(g, STDIN_TO_STDOUT, "test.js", 1, null);
                    g.put(
                            "stdIn",
                            g,
                            cx.getWrapFactory()
                                    .wrap(cx, g, new FakeInputStream(tenGig), InputStream.class));
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

    static class ThrowingInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            throw new IOException("ThrowingInputStream");
        }
    }

    static class ThrowingOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            throw new IOException("ThrowingInputStream");
        }
    }
}
