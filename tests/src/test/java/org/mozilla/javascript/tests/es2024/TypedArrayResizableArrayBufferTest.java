package org.mozilla.javascript.tests.es2024;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.drivers.LanguageVersion;
import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest("testsrc/jstests/es2024/typedarray-resizablearraybuffer.js")
@LanguageVersion(Context.VERSION_ES6)
public class TypedArrayResizableArrayBufferTest extends ScriptTestsBase {}
