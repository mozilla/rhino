package org.mozilla.javascript.tests.es5;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.drivers.LanguageVersion;
import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest("testsrc/jstests/extensions/getset-null.js")
@LanguageVersion(Context.VERSION_1_8)
public class GetSetNullTest extends ScriptTestsBase {}
