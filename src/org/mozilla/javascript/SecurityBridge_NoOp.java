/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.security.ProtectionDomain;

/** This is a "no-op" implementation of SecurityBridge and should work for JDK17 and beyond. */
public class SecurityBridge_NoOp implements SecurityBridge {

    @Override
    public String getSystemProperty(final String name) {
        return System.getProperty(name);
    }

    @Override
    public ProtectionDomain getProtectionDomain(final Class<?> clazz) {
        return clazz.getProtectionDomain();
    }

    @Override
    public ProtectionDomain getScriptProtectionDomain() {
        return null;
    }

    @Override
    public Object getSecurityContext() {
        return null;
    }
}
