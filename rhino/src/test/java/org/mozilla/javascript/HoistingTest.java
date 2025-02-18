package org.mozilla.javascript;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class HoistingTest {
    @Test
    public void hoistedFunctionCallTryCatchShouldNotThrowReferenceError() {
        String script =
                ""
                        + "var result = test();\n"
                        + "function test() {\n"
                        + "   var r; \n"
                        + "   try {\n"
                        + "       r = _add(2, 3);\n"
                        + "       function _add(a, b) { return a + b; }\n"
                        + "   } catch(err) {\n"
                        + "       throw err;\n"
                        + "   }\n"
                        + "  return r;\n"
                        + "}\n"
                        + "result";
        Utils.assertWithAllModes_ES6(5, script);
    }

    @Test
    public void functionCallTryCatchExprStmt() {
        String script =
                ""
                        + "var result = test();\n"
                        + "function test() {\n"
                        + "   var r = 1; \n"
                        + "   if (r == 1) {\n"
                        + "       function _add(a, b) { return a + b; };\n"
                        + "       return _add(5, 4);\n"
                        + "   }\n"
                        + "   return _add(2, 3);\n"
                        + "}\n"
                        + "result";
        Utils.assertWithAllModes_ES6(9, script);
    }

    @Test
    public void hoistedFunctionShouldShadowFunctionWithTheSameName() {
        String script =
                ""
                        + "function g() {\n"
                        + "    result = '';\n"
                        + "    result += f();\n"
                        + "\n"
                        + "    function f() {\n"
                        + "        return 1;\n"
                        + "    }\n"
                        + "\n"
                        + "    do {\n"
                        + "        result += f();\n"
                        + "\n"
                        + "        function f() {\n"
                        + "            return 0;\n"
                        + "        }\n"
                        + "    } while (0);\n"
                        + "\n"
                        + "    result += f();\n"
                        + "    return result;\n"
                        + "}\n"
                        + "\n"
                        + "g()";
        Utils.assertWithAllModes_ES6("100", script);
    }

    @Test
    public void hoistedFunctionCallDoWhileShouldNotThrowReferenceError() {
        String script =
                ""
                        + "var result = test();\n"
                        + "function test() {\n"
                        + "   var r; \n"
                        + "   do {\n"
                        + "       r = _add(2, 3);\n"
                        + "       function _add(a, b) { return a + b; }\n"
                        + "   } while(0) {\n"
                        + "   }\n"
                        + "  return r;\n"
                        + "}\n"
                        + "result";
        Utils.assertWithAllModes_ES6(5, script);
    }

    @Test
    public void hoistedFunctionCallBlockShouldNotThrowReferenceError() {
        String script =
                ""
                        + "var result = test();\n"
                        + "function test() {\n"
                        + "   var r; \n"
                        + "   {\n"
                        + "       r = _add(2, 3);\n"
                        + "       function _add(a, b) { return a + b; }\n"
                        + "   }\n"
                        + "  return r;\n"
                        + "}\n"
                        + "result";
        Utils.assertWithAllModes_ES6(5, script);
    }

    @Test
    public void hoistedFunctionCallForLoopShouldNotThrowReferenceError() {
        String script =
                ""
                        + "var result = test();\n"
                        + "function test() {\n"
                        + "   var r; \n"
                        + "   for (var i = 0; i<1; i++) {\n"
                        + "       r = _add(2, 3);\n"
                        + "       function _add(a, b) { return a + b; }\n"
                        + "   }\n"
                        + "  return r;\n"
                        + "}\n"
                        + "result";
        Utils.assertWithAllModes_ES6(5, script);
    }

    @Test
    public void hoistedFunctionCallIfShouldNotThrowReferenceError() {
        String script =
                ""
                        + "var result = test();\n"
                        + "function test() {\n"
                        + "   var r; var i = 0; \n"
                        + "   if (i<1) {\n"
                        + "       r = _add(2, 3);\n"
                        + "       function _add(a, b) { return a + b; }\n"
                        + "   }\n"
                        + "  return r;\n"
                        + "}\n"
                        + "result";
        Utils.assertWithAllModes_ES6(5, script);
    }

    @Test
    @Ignore("switch-doesnt-open-a-block")
    public void hoistedFunctionCallSwitchShouldNotThrowReferenceError() {
        String script =
                ""
                        + "var result = test();\n"
                        + "function test() {\n"
                        + "   var r; var i = 0; \n"
                        + "   switch(i) {\n"
                        + "       case 0:"
                        + "         r = _add(2, 3);\n"
                        + "       default:\n"
                        + "         function _add(a, b) { return a + b; }\n"
                        + "         break;\n"
                        + "   }\n"
                        + "  return r;\n"
                        + "}\n"
                        + "result";
        Utils.assertWithAllModes_ES6(5, script);
    }

    @Test
    public void hoistedFunctionCallShouldNotThrowUndefinedErrorNoNestedScope() {
        String script =
                ""
                        + "var result = test();\n"
                        + "function test() {\n"
                        + "  var r; \n"
                        + "  r = _add(2, 3);\n"
                        + "  function _add(a, b) { return a + b; }\n"
                        + "  return r;\n"
                        + "}\n"
                        + "result";
        Utils.assertWithAllModes_ES6(5, script);
    }

    @Test
    public void hoistedFunctionExpressionCallShouldThrowUndefinedError() {
        String script =
                ""
                        + "var result = test();\n"
                        + "function test() {\n"
                        + "   var r; \n"
                        + "   try {\n"
                        + "       r = _add(2, 3);\n"
                        + "       var _add = function(a, b) { return a + b; }\n"
                        + "   } catch(err) {\n"
                        + "       throw err;\n"
                        + "   }\n"
                        + "  return r;\n"
                        + "}\n"
                        + "result";
        Utils.assertJavaScriptException_ES6(
                "TypeError: _add is not a function, it is undefined. (test#8)", script);
    }

    @Test
    public void hoistedReferenceShouldNotThrowReferenceError() {
        Utils.runWithAllModes(
                (cx) -> {
                    int languageVersion = cx.getLanguageVersion();
                    try {
                        cx.setLanguageVersion(Context.VERSION_ES6);
                        Scriptable scope = cx.initStandardObjects();
                        String script =
                                ""
                                        + "var arr_obj = [];\n"
                                        + "test();\n"
                                        + "function test() {\n"
                                        + "try {\n"
                                        + "    arr_obj.push({test: \"a\", key: 6});\n"
                                        + "    arr_obj.push({test: \"b\", key: 3});\n"
                                        + "    arr_obj.push({test: \"c\", key: 5});\n"
                                        + "    arr_obj.sort(_sortByKey);\n"
                                        + "    function _sortByKey(a, b) {\n"
                                        + "      return a.key - b.key;\n"
                                        + "    }\n"
                                        + "} catch(err) {\n"
                                        + "    throw err;\n"
                                        + "}\n"
                                        + "}\n"
                                        + "\n"
                                        + "arr_obj";
                        Object result = cx.evaluateString(scope, script, "hoistedRef", 1, null);
                        Assert.assertTrue(result instanceof NativeArray);
                        Assert.assertEquals(3, ((NativeArray) result).size());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Assert.fail();
                    } finally {
                        cx.setLanguageVersion(languageVersion);
                    }
                    return null;
                });
    }
}
