/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.testutils.Utils;

/**
 * Verifies that the LiveConnect service lookup happens during {@link Context#initStandardObjects()}
 * and not lazily during script execution. If the lookup is deferred until a script runs under a
 * restricted access control context (as established by a {@link
 * org.mozilla.javascript.SecurityController}), the classpath read is silently denied and
 * LiveConnect appears unavailable to the script.
 */
public class LiveConnectRestrictedContextTest {

    @BeforeAll
    public static void setup() {
        // Security managers are out in Java 21, so skip.
        assumeFalse(Utils.isJavaVersionAtLeast(21), "Skipping test for Java 21");
        URL url = LiveConnectRestrictedContextTest.class.getResource("grant-all-java.policy");
        if (url != null) {
            System.setProperty("java.security.policy", url.toString());
            Policy.getPolicy().refresh();
            System.setSecurityManager(new SecurityManager());
        }
    }

    @Test
    public void liveConnectAvailableToScriptRunningRestricted() {
        // Security managers are out in Java 21, so skip.
        assumeFalse(Utils.isJavaVersionAtLeast(21), "Skipping test for Java 21");
        Utils.runWithMode(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    // The restricted context may name Java classes, but has no file read
                    // permissions, so a classpath scan inside it would find no services.
                    Permissions perms = new Permissions();
                    perms.add(new RuntimePermission("accessClassInPackage.java.lang"));
                    perms.setReadOnly();
                    ProtectionDomain restricted = new ProtectionDomain(null, perms, null, null);
                    PrivilegedAction<Object> script =
                            () ->
                                    cx.evaluateString(
                                            scope,
                                            "(function() {"
                                                    + " return String(java.lang.Class.forName('java.lang.String').getName());"
                                                    + "})()",
                                            "restricted.js",
                                            1,
                                            null);
                    Object result =
                            AccessController.doPrivileged(
                                    script,
                                    new AccessControlContext(new ProtectionDomain[] {restricted}));
                    assertEquals("java.lang.String", result);
                    return null;
                },
                Context.EvaluationMethod.Interpreter);
    }
}
