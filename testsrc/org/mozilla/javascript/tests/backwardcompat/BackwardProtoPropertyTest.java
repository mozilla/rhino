package org.mozilla.javascript.tests.backwardcompat;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.drivers.LanguageVersion;
import org.mozilla.javascript.drivers.RhinoTest;

@RhinoTest("testsrc/jstests/backwardcompat/backward-proto-property.js")
@LanguageVersion(Context.VERSION_1_8)
public class BackwardProtoPropertyTest {}
