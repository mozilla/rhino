package org.mozilla.javascript.tests.es2020;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.drivers.LanguageVersion;
import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest("testsrc/jstests/es2020/string-match-all.js")
@LanguageVersion(Context.VERSION_ES6)
public class StringMatchAllTest extends ScriptTestsBase {}
