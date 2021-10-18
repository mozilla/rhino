/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.security.ProtectionDomain;

/**
 * SecurityUtilities will delegate method calls to {@link SecurityBridge}.
 *
 * <p>This class will check first for a class <code>org.mozilla.javascript.SecurityBridge_custom
 * </code> and then it will use <code>org.mozilla.javascript.SecurityBridge_SecurityManager</code>
 * or <code>org.mozilla.javascript.SecurityBridge_NoOp</code>, depending if <code>
 * java.lang.SecurityManager</code> is present or not.
 *
 * <ul>
 *
 * @author Attila Szegedi
 * @author Roland Praml, FOCONIS AG
 */
public abstract class SecurityUtilities {
    static final SecurityBridge bridge = makeBridge();

    private static SecurityBridge makeBridge() {

        String[] classNames = {
            "org.mozilla.javascript.SecurityBridge_custom",
            // Check, if SecurityManager class exists
            // TODO: Shoud we check JDK version here?
            Kit.classOrNull("java.lang.SecurityManager") != null
                    ? "org.mozilla.javascript.SecurityBridge_SecurityManager"
                    : "org.mozilla.javascript.SecurityBridge_NoOp",
        };
        for (int i = 0; i != classNames.length; ++i) {
            String className = classNames[i];
            Class<?> cl = Kit.classOrNull(className);
            if (cl != null) {
                SecurityBridge bridge = (SecurityBridge) Kit.newInstanceOrNull(cl);
                if (bridge != null) {
                    return bridge;
                }
            }
        }
        throw new IllegalStateException("Failed to create SecurityBridge instance");
    }
    /**
     * Retrieves a system property within a privileged block. Use it only when the property is used
     * from within Rhino code and is not passed out of it.
     *
     * @param name the name of the system property
     * @return the value of the system property
     */
    public static String getSystemProperty(final String name) {
        return bridge.getSystemProperty(name);
    }

    public static ProtectionDomain getProtectionDomain(final Class<?> clazz) {
        return bridge.getProtectionDomain(clazz);
    }

    /**
     * Look up the top-most element in the current stack representing a script and return its
     * protection domain. This relies on the system-wide SecurityManager being an instance of {@link
     * RhinoSecurityManager}, otherwise it returns <code>null</code>.
     *
     * @return The protection of the top-most script in the current stack, or null
     */
    public static ProtectionDomain getScriptProtectionDomain() {
        return bridge.getScriptProtectionDomain();
    }

    /**
     * Returns an object that reflects the current security context. This can be used as cache key,
     * in JavaMembers method scan. so that each context has exactly that methods in cache that are
     * accessible.
     *
     * @return a securityObject (e.g. AccessControlContext) or null, if no security restriction is
     *     present or security manager is active.
     */
    public static Object getSecurityContext() {
        return bridge.getSecurityContext();
    }

    /**
     * Static helper method not to break existing API.
     *
     * @deprecated See {@link RhinoSecurityManager}
     */
    @Deprecated
    public static Class<?> getCurrentScriptClass(RhinoSecurityManager sm) {
        return sm.getCurrentScriptClass();
    }
}
