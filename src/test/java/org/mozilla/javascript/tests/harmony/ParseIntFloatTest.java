package org.mozilla.javascript.tests.harmony;

import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest(
    value = "src/test/resources/jstests/harmony/parse-int-float.js"
)
public class ParseIntFloatTest
    extends ScriptTestsBase
{
}
