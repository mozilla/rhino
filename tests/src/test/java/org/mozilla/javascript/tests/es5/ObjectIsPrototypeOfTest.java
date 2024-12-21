/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es5;

import org.junit.Test;
import org.mozilla.javascript.tests.Utils;

public class ObjectIsPrototypeOfTest {

    @Test
    public void isPrototypeOfUndefined() {
        final String script =
                "try { "
                        + "  Object.prototype.isPrototypeOf.call(undefined, []);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert undefined to an object.", script);
    }

    @Test
    public void isPrototypeOfNull() {
        final String script =
                "try { "
                        + "  Object.prototype.isPrototypeOf.call(null, []);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }
}
