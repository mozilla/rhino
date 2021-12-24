/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tools.shell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ShellTest {

    static Global global;

    static final ByteArrayOutputStream stdoutRedirect = new ByteArrayOutputStream();
    static final ByteArrayOutputStream stderrRedirect = new ByteArrayOutputStream();

    @BeforeClass
    public static void setUpBeforeClass() {
        global = Main.getGlobal();
        global.setOut(new PrintStream(stdoutRedirect));
        global.setErr(new PrintStream(stderrRedirect));
    }

    @Before
    public void setUp() {
        stdoutRedirect.reset();
        stderrRedirect.reset();
        Main.fileList.clear();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        // Leave it ready for other tests
        Main.global = new Global();
    }

    /**
     * Test the interactive shell by executing help() on stdin.
     *
     * @throws IOException
     */
    @Test
    public void testExecInteractiveHelp() throws IOException {
        int result = interactiveTest("help()\n", 1000, 1024);

        if (result == -1) {
            // We were interrupted
            return;
        }

        global.flushConsole();
        String out = stdoutRedirect.toString("UTF-8");
        String err = stderrRedirect.toString("UTF-8");

        assertEquals(0, result);

        // Due to possibly localized messages, check for a minimum length
        assertTrue(
                "Expected a large stdout, but size is only " + out.length(), 1024 < out.length());

        // Due to possibly localized messages, check for non-translated items
        assertTrue("stdout must contain string 'help()'", out.contains("help()"));
        assertTrue("stdout must contain string 'version('", out.contains("version("));

        assertTrue(err.startsWith("Rhino "));
    }

    /**
     * Test the interactive shell by executing a shell timer test.
     *
     * @throws IOException
     */
    @Test
    public void testExecInteractiveNestedTimers() throws IOException {
        int result =
                interactiveTest(
                        "load('testsrc/assert.js');"
                                + "let count = 0;"
                                + "setTimeout(() => { assertEquals(0, count++);"
                                + "  setTimeout(() => { assertEquals(2, count++); TestsComplete = true; }, 50);"
                                + "  setTimeout(() => { assertEquals(1, count++); }, 20);"
                                + "});\n",
                        1000,
                        0);

        if (result == -1) {
            // We were interrupted
            return;
        }

        global.flushConsole();
        String out = stdoutRedirect.toString("UTF-8");
        String err = stderrRedirect.toString("UTF-8");

        assertEquals(0, result);

        assertEquals(0, out.length());

        assertFalse(err, err.contains("testsrc/assert.js"));
        assertTrue(err.startsWith("Rhino "));
    }

    /**
     * Execute a shell timer test that fails an assertion.
     *
     * @throws IOException
     */
    @Test
    public void testExecInteractiveTimerFailedAssertion() throws IOException {
        int result =
                interactiveTest(
                        "load('testsrc/assert.js'); setTimeout(() => {"
                                + " assertEquals(2, 1); TestsComplete = true; "
                                + "});\n",
                        1000,
                        0);

        if (result == -1) {
            // We were interrupted
            return;
        }

        global.flushConsole();
        String out = stdoutRedirect.toString("UTF-8");
        String err = stderrRedirect.toString("UTF-8");

        assertEquals(3, result);

        assertEquals(0, out.length());

        assertTrue(err, err.contains("testsrc/assert.js"));
        assertTrue(err.startsWith("Rhino "));
    }

    /**
     * Execute the given command in the interactive shell.
     *
     * @param cmd the command. Must end with a {@code '\n'}.
     * @param millis a running timeout in milliseconds.
     * @param minOutLen the minimum expected length of the {@code out} stream.
     * @return the result of {@link Main#exec(String[])}, or {@code -1} if the thread was
     *     interrupted.
     * @throws IOException if an I/O error occurred.
     */
    static int interactiveTest(String cmd, long millis, int minOutLen) throws IOException {
        Main.global = new Global();
        global = Main.getGlobal();

        global.setOut(new PrintStream(stdoutRedirect));
        global.setErr(new PrintStream(stderrRedirect));

        ByteArrayInputStream stdinRedirect = new ByteArrayInputStream(cmd.getBytes());
        global.setIn(stdinRedirect);

        CountDownLatch signal = new CountDownLatch(1);
        RhinoShellTask task = new RhinoShellTask(signal);
        Thread th = new Thread(task);
        th.start();

        try {
            signal.await(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            signal.countDown();
            th.interrupt();
            return -1;
        }

        global.flushConsole();

        int count = 0;
        while (stderrRedirect.size() == 0 || stdoutRedirect.size() < minOutLen) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                th.interrupt();
                return -1;
            }
            global.flushConsole();
            if (++count > 10) {
                break;
            }
        }

        if (!task.isDone()) {
            // The test is going to fail
            th.interrupt();
        }

        return task.getResult();
    }

    /**
     * Execute a sample script file.
     *
     * @throws IOException
     */
    @Test
    public void testExecFile() throws IOException {
        String[] args = {"testsrc/tests/js1_8/shell.js"};
        assertEquals(0, Main.exec(args));
        assertFalse(Main.processStdin);

        global.flushConsole();
        assertEquals(0, stdoutRedirect.size());
    }

    /**
     * Execute a sample script file setting version 180.
     *
     * @throws IOException
     */
    @Test
    public void testExecFileVersion180() throws IOException {
        String[] args = {"-version", "180", "testsrc/tests/js1_8/shell.js"};

        assertEquals(0, Main.exec(args));
        assertFalse(Main.processStdin);

        global.flushConsole();
        assertEquals(0, stdoutRedirect.size());
        assertEquals(0, stderrRedirect.size());
    }

    /**
     * Try to execute a non-existing file.
     *
     * @throws IOException
     */
    @Test
    public void testExecFileNotFound() throws IOException {
        String[] args = {"foo"};

        assertEquals(4, Main.exec(args));

        global.flushConsole();
        assertEquals(0, stdoutRedirect.size());

        String s = stderrRedirect.toString("UTF-8");
        // Due to possibly localized messages, check for non-translated items
        assertTrue("stderr must start with 'js: '", s.startsWith("js: "));
        assertTrue("stderr must contain string '\"foo\"'", s.contains("\"foo\""));
    }

    /**
     * Process an unknown option.
     *
     * @throws IOException
     */
    @Test
    public void testProcessOptionsUnknown() throws IOException {
        String[] args = {"--foo"};

        assertNull(Main.processOptions(args));
        assertEquals(1, Main.exitCode);

        global.flushConsole();
        assertEquals(0, stderrRedirect.size());

        String s = stdoutRedirect.toString("UTF-8");
        // Due to possibly localized messages, check for non-translated items
        assertTrue("stdout must contain string '\"--foo\"'", s.contains("\"--foo\""));
    }

    /**
     * Process a wrongly specified option ('-f' without a filename).
     *
     * @throws IOException
     */
    @Test
    public void testProcessOptions() throws IOException {
        String[] args = {"-f"};

        assertNull(Main.processOptions(args));
        assertEquals(1, Main.exitCode);

        global.flushConsole();
        assertEquals(0, stderrRedirect.size());

        String s = stdoutRedirect.toString("UTF-8");
        // Due to possibly localized messages, check for non-translated items
        assertTrue("stdout must contain string '-help'", s.contains("-help"));
    }

    static class RhinoShellTask implements Runnable {

        private final String[] args;

        private int result;

        private boolean done;

        private final CountDownLatch signal;

        RhinoShellTask(CountDownLatch signal) {
            this(new String[0], signal);
        }

        RhinoShellTask(String[] args, CountDownLatch signal) {
            super();
            this.args = args;
            this.signal = signal;
        }

        @Override
        public void run() {
            result = Main.exec(args);
            done = true;
            signal.countDown();
        }

        public boolean isDone() {
            return done;
        }

        public int getResult() {
            return result;
        }
    }
}
