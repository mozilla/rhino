package org.mozilla.javascript.tests;

import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest(value = "testsrc/jstests/undefined-functions.js")
public class UndefinedFunctionsTest extends ScriptTestsBase {}
