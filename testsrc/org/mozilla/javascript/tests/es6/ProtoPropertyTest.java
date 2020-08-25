package org.mozilla.javascript.tests.es6;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.drivers.LanguageVersion;
import org.mozilla.javascript.drivers.RhinoTest;

@RhinoTest("testsrc/jstests/es6/proto-property.js")
@LanguageVersion(Context.VERSION_ES6)
public class ProtoPropertyTest {}
