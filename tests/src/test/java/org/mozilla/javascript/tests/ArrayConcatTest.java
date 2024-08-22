/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.drivers.LanguageVersion;
import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

/**
 * Test for overloaded array concat with non-dense arg. See
 * https://bugzilla.mozilla.org/show_bug.cgi?id=477604
 *
 * @author Marc Guillemot
 */
@RhinoTest("testsrc/jstests/array-concat-pre-es6.js")
@LanguageVersion(Context.VERSION_1_8)
public class ArrayConcatTest extends ScriptTestsBase {
    // Original test case code moved to the JS file above.
}
