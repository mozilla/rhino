/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.example.securitytest.impl;

import com.example.securitytest.SomeInterface;
import java.util.ArrayList;

/**
 * Provides an implementation for SomeInterface. Defines two methods: <code>foo</code> overridden
 * (defined by interface) and <code>bar</code> defined at this class.
 *
 * <p>If this class is excluded by the shutter, the method <code>bar</code> should not be accessible
 * in scripts.
 *
 * @author Roland Praml, FOCONIS AG
 */
public class SomeClass extends ArrayList<String> implements SomeInterface {
    private static final long serialVersionUID = 1L;

    @Override
    public String foo() {
        return "FOO";
    }

    public String bar() {
        return "BAR";
    }
}
