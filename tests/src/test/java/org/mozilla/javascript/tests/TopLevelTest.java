package org.mozilla.javascript.tests;

import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest(value = "testsrc/jstests/top-level.js")
public class TopLevelTest extends ScriptTestsBase {}
