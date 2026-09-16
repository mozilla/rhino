package org.mozilla.javascript.tests;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.drivers.LanguageVersion;
import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest("testsrc/jstests/gh1834-define-property-getter.js")
@LanguageVersion(Context.VERSION_ES6)
public class Gh1834ObjectDefinePropertyGetter extends ScriptTestsBase {}
