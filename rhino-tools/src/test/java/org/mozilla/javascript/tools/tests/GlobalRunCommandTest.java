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

    private static final String STDIN_TO_STDOUT =
            "runCommand('/usr/bin/cat', { input: stdIn, output: stdOut, err: stdErr })";
    private static final String STDIN_TO_STDERR =
            "runCommand('/usr/bin/env', 'bash', '-c', 'cat >&2', { input: stdIn, output: stdOut, err: stdErr })";

    @Test
    public void test() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = new Global(cx);
                    var result =
                            cx.evaluateString(
                                    g,
                                    "runCommand('/usr/bin/env', 'bash', '-c', 'echo Hello World')",
                                    "test.js",
                                    1,
                                    null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(0, ((Number) result).intValue());
                    return null;
                });
    }

    @Test
    public void testReturnOutput() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = new Global(cx);
                    var result =
                            cx.evaluateString(
                                    g,
                                    "var x = { output:'alreadyThere'};  runCommand('/usr/bin/env', 'bash', '-c', 'echo Hello World', x); x.output",
                                    "test.js",
                                    1,
                                    null);
                    assertInstanceOf(String.class, result);
                    assertEquals("alreadyThereHello World\n", result);
                    return null;
                });
    }

    @Test
    public void testTimeout() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = new Global(cx);
                    long start = System.currentTimeMillis();
                    var result =
                            cx.evaluateString(
                                    g,
                                    "runCommand('/usr/bin/sleep',{ timeout: 500, args : [5] })",
                                    "test.js",
                                    1,
                                    null);
                    long duration = System.currentTimeMillis() - start;
                    assertTrue(duration >= 500);
                    assertTrue(duration <= 1500);
                    assertInstanceOf(Number.class, result);
                    assertEquals(143, ((Number) result).intValue()); // Sigterm
                    return null;
                });
    }

    @Test
    public void testWithOutputStream() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = new Global(cx);
                    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
                    g.put("stdOut", g, cx.getWrapFactory().wrap(cx,g,stdOut, OutputStream.class));
                    var result =
                            cx.evaluateString(
                                    g,
                                    "runCommand('/usr/bin/env', 'bash', '-c', 'echo Hello World', { output: stdOut})",
                                    "test.js",
                                    1,
                                    null);
                    assertInstanceOf(Number.class, result);
                    assertEquals(0, ((Number) result).intValue());
                    try {
                        stdOut.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    assertEquals("Hello World\n", stdOut.toString());
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
                    g.put("stdIn", g, cx.getWrapFactory().wrap(cx,g,stdIn, InputStream.class));
                    g.put("stdOut", g, cx.getWrapFactory().wrap(cx,g,stdOut, OutputStream.class));
                    g.put("stdErr", g, cx.getWrapFactory().wrap(cx,g,stdErr, OutputStream.class));
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

                    g.put("stdIn", g, cx.getWrapFactory().wrap(cx,g,new FakeInputStream(bytes), InputStream.class));
                    g.put("stdOut", g, cx.getWrapFactory().wrap(cx,g,stdOut, OutputStream.class));
                    g.put("stdErr", g, cx.getWrapFactory().wrap(cx,g,stdOut, OutputStream.class));
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
                    g.put("stdIn", g, cx.getWrapFactory().wrap(cx,g,new ThrowingInputStream(), InputStream.class));
                    g.put("stdOut", g, cx.getWrapFactory().wrap(cx,g,stdOut, OutputStream.class));
                    g.put("stdErr", g, cx.getWrapFactory().wrap(cx,g,stdOut, OutputStream.class));

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
     * <p>it runs about 35s and transfers 40GB of data. The expected throughput is slightly 1GB/s
     */
    @Test
    //@Disabled
    public void testStreaming10gb() {
        long tenGig = 10_000_000_000L;
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var g = new Global(cx);
                    FakeOutputStream stdOut = new FakeOutputStream();
                    FakeOutputStream stdErr = new FakeOutputStream();
                    g.put("stdIn", g, cx.getWrapFactory().wrap(cx,g,new FakeInputStream(tenGig), InputStream.class));
                    g.put("stdOut", g, cx.getWrapFactory().wrap(cx,g,stdOut, OutputStream.class));
                    g.put("stdErr", g, cx.getWrapFactory().wrap(cx,g,stdErr, OutputStream.class));
                    cx.evaluateString(g, STDIN_TO_STDOUT, "test.js", 1, null);
                    g.put("stdIn", g, cx.getWrapFactory().wrap(cx,g,new FakeInputStream(tenGig), InputStream.class));
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
