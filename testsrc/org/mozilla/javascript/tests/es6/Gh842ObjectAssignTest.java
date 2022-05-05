/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// https://github.com/mozilla/rhino/issues/842

package org.mozilla.javascript.tests.es6;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.drivers.LanguageVersion;
import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest("testsrc/jstests/es6/gh842-Object-assign-null-this.js")
@LanguageVersion(Context.VERSION_ES6)
public class Gh842ObjectAssignTest extends ScriptTestsBase {}
