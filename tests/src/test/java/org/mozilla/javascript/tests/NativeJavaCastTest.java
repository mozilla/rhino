/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mozilla.javascript.tests;

import java.util.ArrayList;
import org.junit.Test;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.testutils.Utils;

/** From Roland Praml github.com/rPraml */
public class NativeJavaCastTest {

    public static class Impl extends ArrayList<Object> implements Example {
        public String implMethod() {
            return "implMethod";
        }
    }

    public interface Example {
        static Example get() {
            Impl obj = new Impl();
            obj.add("element");
            return obj;
        }

        default String ifaceMethod() {
            return "ifaceMethod";
        }
    }

    static final String INIT =
            "var Example = org.mozilla.javascript.tests.NativeJavaCastTest$Example;"
                    + "var Impl = org.mozilla.javascript.tests.NativeJavaCastTest$Impl;";

    /**
     * Access the object in "duckType", this means, rhino detects if it should be wrapped in a
     * NativeJavaList or not.
     */
    @Test
    public void testDuckType() {
        Utils.assertWithAllModes_ES6(1, INIT + "Example.get().size()");
        Utils.assertWithAllModes_ES6(1, INIT + "Example.get().length");
        Utils.assertWithAllModes_ES6("element", INIT + "Example.get()[0]");
        Utils.assertWithAllModes_ES6("ifaceMethod", INIT + "Example.get().ifaceMethod()");
        Utils.assertWithAllModes_ES6("implMethod", INIT + "Example.get().implMethod()");
    }

    @Test
    public void testListCast() {
        Utils.assertWithAllModes_ES6(1, INIT + "(java.util.List)(Example.get()).size()");
        Utils.assertWithAllModes_ES6(1, INIT + "(java.util.List)(Example.get()).length");
        Utils.assertWithAllModes_ES6("element", INIT + "(java.util.List)(Example.get())[0]");

        Utils.assertWithAllModes_ES6(
                Undefined.instance, INIT + "(java.util.List)(Example.get()).ifaceMethod");
        Utils.assertWithAllModes_ES6(
                Undefined.instance, INIT + "(java.util.List)(Example.get()).implMethod");
    }

    @Test
    public void testIFaceCast() {
        Utils.assertWithAllModes_ES6(Undefined.instance, INIT + "(Example)(Example.get()).size");
        Utils.assertWithAllModes_ES6(
                Undefined.instance, INIT + "(Example)(Example.get()).length"); // no NativeJavaList
        // Utils.assertWithAllModes_ES6(Undefined.instance, INIT + "(Example)(Example.get())[0]");
        // has no public instance field or method named "0"
        Utils.assertWithAllModes_ES6(
                "ifaceMethod", INIT + "(Example)(Example.get()).ifaceMethod()");
        // CHECKME: I would expect, that I can NOT access the methods here:
        Utils.assertWithAllModes_ES6(
                Undefined.instance, INIT + "(Example)(Example.get()).implMethod");
    }

    public void testImplCast() {
        Utils.assertWithAllModes_ES6(1, INIT + "(Impl)(Example.get()).size()");
        Utils.assertWithAllModes_ES6(
                Undefined.instance, INIT + "(Impl)(Example.get()).length"); // no NativeJavaList
        Utils.assertWithAllModes_ES6(
                Undefined.instance,
                INIT + "(Example)(Example.get())[0]"); // has no public instance field or method
        // named "0"
        Utils.assertWithAllModes_ES6(
                Undefined.instance,
                INIT + "(Impl)(Example.get()).ifaceMethod"); // CHECKME: This would work in java
        Utils.assertWithAllModes_ES6("implMethod", INIT + "(Impl)(Example.get()).implMethod()");
    }
}
