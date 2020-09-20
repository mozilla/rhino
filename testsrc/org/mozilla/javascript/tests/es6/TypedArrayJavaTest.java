package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Test for TypedArrays.
 */
public class TypedArrayJavaTest
{
    /**
     * Test case for {@link https://github.com/mozilla/rhino/issues/768}
     *
     * @throws Exception if test failed
     */
    @Test
    public void subarrayWithoutParams() throws Exception {
        String[] allNativeTypes = {
                "Float32Array",
                "Float64Array",
                "Int8Array",
                "Int16Array",
                "Int32Array",
                "Uint8Array",
                "Uint16Array",
                "Uint32Array",
                "Uint8ClampedArray"
        };

        Context cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        Scriptable global = cx.initStandardObjects();

        for (String type : allNativeTypes) {
            String script = "var ta = new " + type + "([1, 2]);\n"
                            + "'' + ta.subarray();";
            Object obj = cx.evaluateString(global, script, "", 1, null);
            assertEquals("1,2", obj);
        }

        Context.exit();
    }
}
