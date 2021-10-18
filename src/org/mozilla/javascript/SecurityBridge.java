/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.security.ProtectionDomain;

/**
 * Bridge to As with JDK 17 SecurityManager is deprecated, we will need a way to switch to a
 * different implementation.
 *
 * @author Roland Praml
 */
interface SecurityBridge {

    /** @see SecurityUtilities#getSystemProperty(String) */
    public String getSystemProperty(final String name);

    /** @see SecurityUtilities#getProtectionDomain(Class) */
    public ProtectionDomain getProtectionDomain(final Class<?> clazz);

    /** @see SecurityUtilities#getScriptProtectionDomain() */
    public ProtectionDomain getScriptProtectionDomain();

    /** @see SecurityUtilities#getSecurityContext() */
    Object getSecurityContext();
}
