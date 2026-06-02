package org.mozilla.javascript;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

public class HoistingTest {
    @Test
    public void hoistedFunctionCallTryCatchShouldNotThrowReferenceError() {
        String script =
                """
            var result = test();
            function test() {
               var r;\s
               try {
                   r = _add(2, 3);
                   function _add(a, b) { return a + b; }
               } catch(err) {
                   throw err;
               }
              return r;
            }
            result""";
        Utils.assertWithAllModes_ES6(5, script);
    }

    @Test
    public void functionCallTryCatchExprStmt() {
        String script =
                """
            var result = test();
            function test() {
               var r = 1;\s
               if (r == 1) {
                   function _add(a, b) { return a + b; };
                   return _add(5, 4);
               }
               return _add(2, 3);
            }
            result""";
        Utils.assertWithAllModes_ES6(9, script);
    }

    @Test
    public void hoistedFunctionShouldShadowFunctionWithTheSameName() {
        String script =
                """
            function g() {
                result = '';
                result += f();

                function f() {
                    return 1;
                }

                do {
                    result += f();

                    function f() {
                        return 0;
                    }
                } while (0);

                result += f();
                return result;
            }

            g()""";
        Utils.assertWithAllModes_ES6("100", script);
    }

    @Test
    public void hoistedFunctionCallDoWhileShouldNotThrowReferenceError() {
        String script =
                """
            var result = test();
            function test() {
               var r;\s
               do {
                   r = _add(2, 3);
                   function _add(a, b) { return a + b; }
               } while(0) {
               }
              return r;
            }
            result""";
        Utils.assertWithAllModes_ES6(5, script);
    }

    @Test
    public void hoistedFunctionCallBlockShouldNotThrowReferenceError() {
        String script =
                """
            var result = test();
            function test() {
               var r;\s
               {
                   r = _add(2, 3);
                   function _add(a, b) { return a + b; }
               }
              return r;
            }
            result""";
        Utils.assertWithAllModes_ES6(5, script);
    }

    @Test
    public void hoistedFunctionCallForLoopShouldNotThrowReferenceError() {
        String script =
                """
            var result = test();
            function test() {
               var r;\s
               for (var i = 0; i<1; i++) {
                   r = _add(2, 3);
                   function _add(a, b) { return a + b; }
               }
              return r;
            }
            result""";
        Utils.assertWithAllModes_ES6(5, script);
    }

    @Test
    public void hoistedFunctionCallIfShouldNotThrowReferenceError() {
        String script =
                """
            var result = test();
            function test() {
               var r; var i = 0;\s
               if (i<1) {
                   r = _add(2, 3);
                   function _add(a, b) { return a + b; }
               }
              return r;
            }
            result""";
        Utils.assertWithAllModes_ES6(5, script);
    }

    @Test
    @Disabled("switch-doesnt-open-a-block")
    public void hoistedFunctionCallSwitchShouldNotThrowReferenceError() {
        String script =
                """
            var result = test();
            function test() {
               var r; var i = 0;\s
               switch(i) {
                   case 0:\
                     r = _add(2, 3);
                   default:
                     function _add(a, b) { return a + b; }
                     break;
               }
              return r;
            }
            result""";
        Utils.assertWithAllModes_ES6(5, script);
    }

    @Test
    public void hoistedFunctionCallShouldNotThrowUndefinedErrorNoNestedScope() {
        String script =
                """
            var result = test();
            function test() {
              var r;\s
              r = _add(2, 3);
              function _add(a, b) { return a + b; }
              return r;
            }
            result""";
        Utils.assertWithAllModes_ES6(5, script);
    }

    @Test
    public void hoistedFunctionExpressionCallShouldThrowUndefinedError() {
        String script =
                """
            var result = test();
            function test() {
               var r;\s
               try {
                   r = _add(2, 3);
                   var _add = function(a, b) { return a + b; }
               } catch(err) {
                   throw err;
               }
              return r;
            }
            result""";
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
                        TopLevel scope = cx.initStandardObjects();
                        String script =
                                """
                            var arr_obj = [];
                            test();
                            function test() {
                              try {
                                arr_obj.push({test: \"a\", key: 6});
                                arr_obj.push({test: \"b\", key: 3});
                                arr_obj.push({test: \"c\", key: 5});
                                arr_obj.sort(_sortByKey);
                                function _sortByKey(a, b) {
                                  return a.key - b.key;
                                }
                              } catch(err) {
                                throw err;
                              }
                            }

                            arr_obj""";
                        Object result = cx.evaluateString(scope, script, "hoistedRef", 1, null);
                        Assertions.assertTrue(result instanceof NativeArray);
                        Assertions.assertEquals(3, ((NativeArray) result).size());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Assertions.fail();
                    } finally {
                        cx.setLanguageVersion(languageVersion);
                    }
                    return null;
                });
    }
}
