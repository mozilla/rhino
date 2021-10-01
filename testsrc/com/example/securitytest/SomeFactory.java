/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.example.securitytest;

/**
 * Class for SecurityControllerTest.
 *
 * @author Roland Praml, FOCONIS AG
 */
public class SomeFactory {

    public static int TEST = 42;

    public SomeInterface create() {
        try {
            return (SomeInterface)
                    Class.forName("com.example.securitytest.impl.SomeClass").newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException("Could not create impl", e);
        }
    }
}
