package org.mozilla.javascript.tests;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.drivers.LanguageVersion;
import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest("testsrc/jstests/const-white-box.js")
@LanguageVersion(Context.VERSION_ES6)
public class ConstWhiteBoxTest extends ScriptTestsBase {}
