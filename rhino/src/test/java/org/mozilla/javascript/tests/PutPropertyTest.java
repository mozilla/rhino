/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Test for setting a property defined in the prototype chain.
 *
 * @author Ronald Brill
 */
public class PutPropertyTest {

    public void setPropKnownAtPrototypeObject() throws Exception {
        final String script =
                "var WithObjectPrototype = function(array) {\n"
                        + "  this.prop = array.length;\n"
                        + "  return this;\n"
                        + "}\n"
                        + "var nlp = WithObjectPrototype.prototype = { prop: 7 };\n"
                        + "var test = new WithObjectPrototype(['abc']);\n"
                        + "'' + nlp.prop + ' # ' + test.prop";

        Utils.assertWithAllModes("7 # 1", script);
    }

    @Test
    public void setPropNotKnownAtPrototypeObject() throws Exception {
        final String script =
                "var WithObjectPrototype = function(array) {\n"
                        + "  this.prop = array.length;\n"
                        + "  return this;\n"
                        + "}\n"
                        + "var nlp = WithObjectPrototype.prototype = { length: 7 };\n"
                        + "var test = new WithObjectPrototype(['abc']);\n"
                        + "'' + nlp.prop + ' # ' + test.prop";

        Utils.assertWithAllModes("undefined # 1", script);
    }

    @Test
    public void setLengthKnownAtPrototypeObject() throws Exception {
        final String script =
                "var WithObjectPrototype = function(array) {\n"
                        + "  this.length = array.length;\n"
                        + "  return this;\n"
                        + "}\n"
                        + "var nlp = WithObjectPrototype.prototype = { length: 7 };\n"
                        + "var test = new WithObjectPrototype(['abc']);\n"
                        + "'' + nlp.length + ' # ' + test.length";

        Utils.assertWithAllModes("7 # 1", script);
    }

    @Test
    public void setLengthNotKnownAtPrototypeObject() throws Exception {
        final String script =
                "var WithObjectPrototype = function(array) {\n"
                        + "  this.length = array.length;\n"
                        + "  return this;\n"
                        + "}\n"
                        + "var nlp = WithObjectPrototype.prototype = { prop: 7 };\n"
                        + "var test = new WithObjectPrototype(['abc']);\n"
                        + "'' + nlp.length + ' # ' + test.length";

        Utils.assertWithAllModes("undefined # 1", script);
    }

    @Test
    public void setLengthKnownAtPrototypeArray() throws Exception {
        final String script =
                "var WithArrayPrototype = function(array) {\n"
                        + "  this.length = array.length;\n"
                        + "  return this;\n"
                        + "}\n"
                        + "var nlp = WithArrayPrototype.prototype = [];\n"
                        + "var test = new WithArrayPrototype(['abc']);\n"
                        + "'' + nlp.length + ' # ' + test.length";

        Utils.assertWithAllModes("0 # 1", script);
    }
}
