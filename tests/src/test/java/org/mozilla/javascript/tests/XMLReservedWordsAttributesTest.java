package org.mozilla.javascript.tests;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.drivers.LanguageVersion;
import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest("testsrc/jstests/xml-reserved-words-as-attributes.js")
@LanguageVersion(Context.VERSION_ES6)
public class XMLReservedWordsAttributesTest extends ScriptTestsBase {}
