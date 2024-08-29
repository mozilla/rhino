package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class OptionalChainingOperatorTests {

    @Test
    public void testOptionalChainingOperator() {

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    String sourceName = "optionalChainingOperator";
                    Scriptable scope = cx.initStandardObjects();

                    String script = " var a = {name: 'val'}; a.outerProp?.innerProp";
                    assertEquals(
                            Undefined.instance,
                            cx.evaluateString(scope, script, sourceName, 1, null));

                    String script2 =
                            " var a = {outerProp: {innerProp: 'val' } }; a.outerProp?.innerProp";
                    assertEquals("val", cx.evaluateString(scope, script2, sourceName, 1, null));

                    String script3 =
                            " var a = {outerProp: {innerProp: { innerInnerProp: {name: 'val' } } } }; a.outerProp?.innerProp?.missingProp?.name";
                    assertEquals(
                            Undefined.instance,
                            cx.evaluateString(scope, script3, sourceName, 1, null));

                    String script4 =
                            " var a = {outerProp: {innerProp: { innerInnerProp: {name: 'val' } } } }; a.outerProp?.innerProp?.innerInnerProp?.name";
                    assertEquals("val", cx.evaluateString(scope, script4, sourceName, 1, null));

                    // NOT WORKING YET
                    //                    String script5 = " var a = {};
                    // a.someNonExistentMethod?.()";
                    //                    assertEquals(
                    //                            Undefined.instance,
                    //                            cx.evaluateString(scope, script5, sourceName, 1,
                    // null));

                    return null;
                });
    }
}
