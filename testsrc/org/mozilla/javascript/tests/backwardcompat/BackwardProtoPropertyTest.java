package org.mozilla.javascript.tests.backwardcompat;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.drivers.LanguageVersion;
import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest("testsrc/jstests/backwardcompat/backward-proto-property.js")
@LanguageVersion(Context.VERSION_1_8)
public class BackwardProtoPropertyTest extends ScriptTestsBase {}
