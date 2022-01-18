package org.mozilla.javascript.tests;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.drivers.LanguageVersion;
import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest("testsrc/jstests/extensions/obsolete-generators.js")
@LanguageVersion(Context.VERSION_1_8)
public class ObsoleteGeneratorTest extends ScriptTestsBase {}
