package org.mozilla.javascript.tests;

import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest(value = "testsrc/jstests/call-undefined-properties.js")
public class CallUndefinedPropertiesTest extends ScriptTestsBase {}
