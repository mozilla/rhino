/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

/**
 * Code moved from {@link SecurityUtilities}. This implementation makes use of {@link
 * SecurityManager} and {@link AccessController} and so on, which is deprecated with JDK17 (see <a
 * href='https://openjdk.java.net/jeps/411'>JEP411</a>) - so all related 'java.security' stuff
 * should be routed over this class, so that it could be easily replaced by an other implementation
 * like {@link SecurityBridge_NoOp}.
 *
 * <p>This implementation should be work up to JDK17
 *
 * @author Attila Szegedi
 * @author Roland Praml, FOCONIS AG
 */
@Deprecated
public class SecurityBridge_SecurityManager implements SecurityBridge {
    private static final Permission allPermission = new AllPermission();
    /**
     * Retrieves a system property within a privileged block. Use it only when the property is used
     * from within Rhino code and is not passed out of it.
     *
     * @param name the name of the system property
     * @return the value of the system property
     */
    @Override
    public String getSystemProperty(final String name) {
        return AccessController.doPrivileged(
                new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return System.getProperty(name);
                    }
                });
    }

    @Override
    public ProtectionDomain getProtectionDomain(final Class<?> clazz) {
        return AccessController.doPrivileged(
                new PrivilegedAction<ProtectionDomain>() {
                    @Override
                    public ProtectionDomain run() {
                        return clazz.getProtectionDomain();
                    }
                });
    }

    /**
     * Look up the top-most element in the current stack representing a script and return its
     * protection domain. This relies on the system-wide SecurityManager being an instance of {@link
     * RhinoSecurityManager}, otherwise it returns <code>null</code>.
     *
     * @return The protection of the top-most script in the current stack, or null
     */
    @Override
    public ProtectionDomain getScriptProtectionDomain() {
        final SecurityManager securityManager = System.getSecurityManager();
        if (securityManager instanceof RhinoSecurityManager) {
            return AccessController.doPrivileged(
                    new PrivilegedAction<ProtectionDomain>() {
                        @Override
                        public ProtectionDomain run() {
                            Class<?> c =
                                    SecurityUtilities.getCurrentScriptClass(
                                            (RhinoSecurityManager) securityManager);
                            return c == null ? null : c.getProtectionDomain();
                        }
                    });
        }
        return null;
    }

    @Override
    public Object getSecurityContext() {
        Object sec = null;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sec = sm.getSecurityContext();
            if (sec instanceof AccessControlContext) {
                try {
                    ((AccessControlContext) sec).checkPermission(allPermission);
                    // if we have allPermission, we do not need to store the
                    // security object in the cache key
                    return null;
                } catch (SecurityException e) {
                }
            }
        }
        return sec;
    }
}
