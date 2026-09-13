/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LiveConnectSupport;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.testutils.Utils;

/**
 * These tests document what an attacker can do when rhino-reflect (LiveConnect) is on the
 * classpath. They all succeed in this module because the module is on the test classpath. The
 * matching "LiveConnectDisabledTest" in the rhino module verifies that every one of these attacks
 * fails when the module is not present.
 */
public class LiveConnectSecurityTest {

    @Test
    public void liveConnectIsAvailable() {
        assertTrue(LiveConnectSupport.get().isAvailable());
    }

    @Test
    public void scriptCanWriteToSystemOut() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(captured));
            Utils.runWithAllModes(
                    cx -> {
                        TopLevel scope = cx.initStandardObjects();
                        cx.evaluateString(
                                scope,
                                "java.lang.System.out.println('pwned-by-script')",
                                "attack.js",
                                1,
                                null);
                        return null;
                    });
        } finally {
            System.setOut(originalOut);
        }
        String output = new String(captured.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(
                output.contains("pwned-by-script"),
                "script was able to write to System.out: " + output);
    }

    @Test
    public void scriptCanExecuteArbitraryCommands(@TempDir Path tempDir) {
        Path target = tempDir.resolve("pwned-by-script");
        Utils.runWithAllModes(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    scope.put("target", scope, target.toString());
                    cx.evaluateString(
                            scope,
                            "java.lang.Runtime.getRuntime().exec('touch ' + target)",
                            "attack.js",
                            1,
                            null);
                    return null;
                });
        assertTrue(Files.exists(target), "script was able to execute an OS command");
    }

    @Test
    public void scriptCanLoadArbitraryClasses() {
        Utils.assertWithAllModes_ES6(
                "java.net.URLClassLoader",
                "String(java.lang.Class.forName('java.net.URLClassLoader').getName())");
    }

    @Test
    public void scriptCanAccessTheClassLoader() {
        Utils.assertWithAllModes_ES6(
                "classloader-accessible",
                "(function() {"
                        + "var loader = java.lang.Thread.currentThread().getContextClassLoader();"
                        + "return (loader != null && loader != undefined) ? 'classloader-accessible' : 'no-loader';"
                        + "})()");
    }

    @Test
    public void caughtJavaExceptionExposesJavaObject() {
        Utils.assertWithAllModes_ES6(
                "java.lang.NumberFormatException",
                "(function() {"
                        + "try {"
                        + "  java.lang.Integer.parseInt('not-a-number');"
                        + "  return 'no exception';"
                        + "} catch (e) {"
                        + "  return String(e.javaException.class.name);"
                        + "}"
                        + "})()");
    }

    @Test
    @Timeout(120)
    public void scriptCanCallSystemExit() throws Exception {
        ProcessBuilder builder =
                new ProcessBuilder(
                        new File(System.getProperty("java.home"), "bin/java").getPath(),
                        "-cp",
                        System.getProperty("java.class.path"),
                        SystemExitProber.class.getName());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try {
            assertTrue(
                    process.waitFor(60, TimeUnit.SECONDS), "forked JVM did not terminate in time");
            assertEquals(
                    42, process.exitValue(), "System.exit(42) should have been called from JS");
        } finally {
            process.destroyForcibly();
        }
    }

    /** Run by the forked JVM in {@link #scriptCanCallSystemExit()}. */
    public static final class SystemExitProber {
        public static void main(String[] args) {
            try (Context cx = Context.enter()) {
                cx.setLanguageVersion(Context.VERSION_ES6);
                TopLevel scope = cx.initStandardObjects();
                cx.evaluateString(scope, "java.lang.System.exit(42)", "attack.js", 1, null);
            }
        }
    }
}
