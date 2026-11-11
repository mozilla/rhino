/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

// The script is copied from V8's test/mjsunit/number-tostring.js
// (Copyright 2008 the V8 project authors, BSD-3-Clause), with the
// addition of the assert.js load and the trailing "success" value.
@RhinoTest("testsrc/jstests/number-tostring.js")
public class NumberToStringTest extends ScriptTestsBase {}
