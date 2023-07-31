/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;

/**
 * Takes care that the class name of the generated class "looks like" the provided script name. See
 * https://bugzilla.mozilla.org/show_bug.cgi?id=460283
 *
 * @author Marc Guillemot
 */
public class GeneratedClassNameTest {

    @Test
    public void generatedClassName() throws Exception {
        doTest("myScript_js", "myScript.js");
        doTest("foo", "foo");
        doTest("c", "");
        doTest("_1", "1");
        doTest("_", "_");
        doTest("unnamed_script", null);
        doTest("some_dir_some_foo_js", "some/dir/some/foo.js");
        doTest("some_dir_some_foo_js", "some\\dir\\some\\foo.js");
        doTest("_12_foo_34_js", "12 foo 34.js");
    }

    private void doTest(final String expectedName, final String scriptName) throws Exception {
        final Script script =
                ContextFactory.getGlobal()
                        .call(cx -> cx.compileString("var f = 1", scriptName, 1, null));

        // remove serial number
        String name = script.getClass().getSimpleName();
        assertEquals(expectedName, name.substring(0, name.lastIndexOf('_')));
    }
}
