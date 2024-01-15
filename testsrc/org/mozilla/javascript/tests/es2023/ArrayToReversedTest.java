package org.mozilla.javascript.tests.es2023;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.drivers.LanguageVersion;
import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest("testsrc/jstests/es2023/array-toReversed.js")
@LanguageVersion(Context.VERSION_ES6)
public class ArrayToReversedTest extends ScriptTestsBase {}
