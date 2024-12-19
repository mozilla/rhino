/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Scriptable;

/**
 * Test if XML is present. This test exists in "rhino-xml" where XML is present and in "rhino",
 * where XML is not on classpath.
 *
 * @author Roland Praml
 */
public class XMLPresentTest {

    @Test
    public void testXMLPresent() {
        try (Context cx = ContextFactory.getGlobal().enterContext()) {
            Scriptable scope = cx.initStandardObjects();
            Object result =
                    cx.evaluateString(
                            scope, "new XML('<a></a>').toXMLString();", "source", 1, null);
            fail("not expected");
        } catch (EcmaError e) {
            assertEquals("ReferenceError: \"XML\" is not defined. (source#1)", e.getMessage());
        }
    }
}
