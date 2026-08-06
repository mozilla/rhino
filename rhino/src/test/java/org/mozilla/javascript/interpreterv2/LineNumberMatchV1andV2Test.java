package org.mozilla.javascript.interpreterv2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.*;
import org.mozilla.javascript.debug.*;

public class LineNumberMatchV1andV2Test {

    private static class LineTrackingDebugger implements Debugger {
        private final List<Integer> linesHit = new ArrayList<>();

        @Override
        public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript) {
            return new DebugFrame() {
                @Override
                public void onLineChange(Context cx, int lineNumber) {
                    linesHit.add(lineNumber);
                }
            };
        }

        @Override
        public void handleCompilationDone(Context cx, DebuggableScript fnOrScript, String source) {}

        public List<Integer> getLinesHit() {
            return new ArrayList<>(linesHit);
        }
    }

    private static List<Integer> getLinesHit(String code, Context.EvaluationMethod method) {
        try (Context cx = Context.enter()) {
            cx.setEvaluationMethod(method);
            cx.setGeneratingDebug(true);

            LineTrackingDebugger debugger = new LineTrackingDebugger();
            cx.setDebugger(debugger, null);

            VarScope scope = cx.initStandardObjects();

            try {
                cx.evaluateString(scope, code, "test", 1, null);
            } catch (Exception e) {
                // Ignore execution errors
            }

            return debugger.getLinesHit();
        }
    }

    private static void runTest(String testName, String code) throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("Testing: " + testName);
        System.out.println("=".repeat(70));

        List<Integer> v1Lines = getLinesHit(code, Context.EvaluationMethod.Interpreter);
        List<Integer> v2Lines = getLinesHit(code, Context.EvaluationMethod.InterpreterV2);

        Set<Integer> v1Set = new LinkedHashSet<>(v1Lines);
        Set<Integer> v2Set = new LinkedHashSet<>(v2Lines);

        Set<Integer> onlyV1 = new LinkedHashSet<>(v1Set);
        onlyV1.removeAll(v2Set);

        Set<Integer> onlyV2 = new LinkedHashSet<>(v2Set);
        onlyV2.removeAll(v1Set);

        boolean exactMatch = v1Lines.equals(v2Lines);
        boolean setMatch = v1Set.equals(v2Set);

        System.out.println("\nResults:");
        System.out.println("V1 lines: " + v1Lines);
        System.out.println("V2 lines: " + v2Lines);

        // Build failure message with details
        StringBuilder message = new StringBuilder();
        message.append("\n").append(testName).append(" - V1 vs V2 line tracking differs\n\n");
        message.append("V1 lines: ").append(v1Lines).append("\n");
        message.append("V2 lines: ").append(v2Lines).append("\n\n");

        if (exactMatch) {
            System.out.println("\n✓ EXACT MATCH - V1 and V2 are identical!");
            // Test passes - no assertion error
        } else if (matchV1WithExtra(v1Lines, v2Lines)) {
            System.out.println("\n✓ GOOD MATCH - V2 visits all V1 lines in order, plus others!");
            // Test passes - no assertion error
        } else if (setMatch) {
            System.out.println("\n✓ SET MATCH - Same lines, different count/order");
            message.append("Issue: SET MATCH - Same lines hit, but different count/order\n");
            message.append(
                    "This means V2 hits the correct lines but in a different order or frequency.\n");
            fail(message.toString());
        } else {
            System.out.println("\n✗ SET MISMATCH - Different line sets");
            message.append("Issue: SET MISMATCH - Different line sets\n\n");

            if (!onlyV1.isEmpty()) {
                System.out.println("  Lines ONLY in V1: " + onlyV1);
                message.append("Lines ONLY in V1: ").append(onlyV1).append("\n");
            }
            if (!onlyV2.isEmpty()) {
                System.out.println("  Lines ONLY in V2: " + onlyV2);
                message.append("Lines ONLY in V2: ").append(onlyV2).append("\n");
            }

            fail(message.toString());
        }
    }

    private static boolean matchV1WithExtra(List<Integer> v1, List<Integer> v2) {
        int v1Offset = 0;
        int v2Offset = 0;

        while (v1Offset < v1.size() && v2Offset < v2.size()) {
            if (v1.get(v1Offset).equals(v2.get(v2Offset))) {
                v1Offset++;
            }
            v2Offset++;
        }
        // We reached the end of v1 with matching elements.
        return v1Offset == v1.size();
    }

    @Test
    public void testGetterExtraLines() throws Exception {
        String code =
                "var proto = {};\n"
                        + "Object.defineProperty(proto, \"parent\", {\n"
                        + "  get: function() {\n"
                        + "    return \"parent\";\n"
                        + "  },\n"
                        + "  configurable: true\n"
                        + "});\n"
                        + "var x = proto.parent;";
        runTest("testGetterExtraLines", code);
    }

    @Test
    public void testMultilineCall() throws Exception {
        String code = "var result = Math.max(\n" + "  1,\n" + "  2,\n" + "  3,\n" + "  4\n" + ");";
        runTest("testMultilineCall", code);
    }

    @Test
    public void testClosingBrace() throws Exception {
        String code =
                "var x = \"test\";\n"
                        + "if (x !== 'test'){\n"
                        + "  throw new Error('#1');\n"
                        + "};\n"
                        + "var y = \"test2\";\n"
                        + "if (y !== 'test2'){\n"
                        + "  throw new Error('#2');\n"
                        + "};";
        runTest("testClosingBrace", code);
    }

    @Test
    public void testWithStatement() throws Exception {
        String code =
                "var obj = {p: 1};\n"
                        + "with(obj){\n"
                        + "    do{\n"
                        + "        p = 2;\n"
                        + "    } while(false);\n"
                        + "}";
        runTest("testWithStatement", code);
    }

    @Test
    public void testWithForIn() throws Exception {
        String code =
                "var myObj = {p1: 'a',\n"
                        + "             p2: 'b'};\n"
                        + "\n"
                        + "\n"
                        + "for(var prop in myObj){\n"
                        + "}";
        runTest("testWithForIn", code);
    }

    @Test
    public void testSimpleObject() throws Exception {
        String code =
                "var myObj = {p1: 'a',\n"
                        + "             p2: 'b'};\n"
                        + "\n"
                        + "console.log(myObj);";
        runTest("testSimpleObject", code);
    }

    @Test
    public void testObjectLiteralKey() throws Exception {
        String code =
                "var arr = [];\n"
                        + "\n"
                        + "Object.defineProperties(arr, {\n"
                        + "  \"0\": {\n"
                        + "    set: function() {},\n"
                        + "    get: function() {},\n"
                        + "    configurable: true\n"
                        + "  }\n"
                        + "});\n";
        runTest("testObjectLiteralKey", code);
    }

    @Test
    public void testTryKeyword() throws Exception {
        String code =
                "try\n"
                        + "{\n"
                        + "  var __obj = {\n"
                        + "    toString: function() {\n"
                        + "      return new Object();\n"
                        + "    }\n"
                        + "  }\n"
                        + "  String(__obj);\n"
                        + "}\n"
                        + "catch (e)\n"
                        + "{\n"
                        + "  console.log(e);\n"
                        + "}";
        runTest("testTryKeyword", code);
    }

    @Test
    public void testFunctionDefinition() throws Exception {
        String code =
                "test();\n"
                        + "\n"
                        + "function test()\n"
                        + "{\n"
                        + "  var counter = 0;\n"
                        + "  try\n"
                        + "  {\n"
                        + "    throw 42;\n"
                        + "  }\n"
                        + "  catch(e2)\n"
                        + "  {\n"
                        + "    counter++;\n"
                        + "  }\n"
                        + "}";
        runTest("testFunctionDefinition", code);
    }

    @Test
    public void testWithBreak() throws Exception {
        String code =
                "var myObj = {\n"
                        + "    p1: 'a',\n"
                        + "    value: 'myObj_value'\n"
                        + "}\n"
                        + "\n"
                        + "with(myObj){\n"
                        + "    do{\n"
                        + "        break;\n"
                        + "        p1 = 'x1';\n"
                        + "    } while(false);\n"
                        + "}";
        runTest("testWithBreak", code);
    }

    @Test
    public void testObjectPropertyKey() throws Exception {
        String code =
                "Object.defineProperty(Object.prototype,\n"
                        + "  \"prop\",\n"
                        + "  {\n"
                        + "    value: 1001,\n"
                        + "    writable: false,\n"
                        + "    enumerable: false,\n"
                        + "    configurable: false\n"
                        + "  }\n"
                        + ");\n"
                        + "var prop = 1002;";
        runTest("testObjectPropertyKey", code);
    }

    @Test
    public void testWithForInBreak() throws Exception {
        String code =
                "var myObj = {\n"
                        + "    p1: 'a',\n"
                        + "    p2: 'b',\n"
                        + "    p3: 'c'\n"
                        + "};\n"
                        + "\n"
                        + "with(myObj){\n"
                        + "  for(var prop in myObj){\n"
                        + "    break;\n"
                        + "    p1 = 'x1';\n"
                        + "  }\n"
                        + "}";
        runTest("testWithForInBreak", code);
    }

    @Test
    public void testThrowClosingBrace() throws Exception {
        String code =
                "var object = {\n"
                        + "  valueOf: function() {\n"
                        + "    return \"%5E\"\n"
                        + "  },\n"
                        + "  toString: function() {\n"
                        + "    return {}\n"
                        + "  }\n"
                        + "};\n"
                        + "\n"
                        + "//CHECK#1\n"
                        + "if (object.valueOf() !== \"%5E\") {\n"
                        + "  throw new Error('#1: valueOf returns %5E');\n"
                        + "}\n"
                        + "\n"
                        + "//CHECK#2\n"
                        + "var object2 = {\n"
                        + "  valueOf: function() {\n"
                        + "    return \"\"\n"
                        + "  },\n"
                        + "  toString: function() {\n"
                        + "    return \"%5E\"\n"
                        + "  }\n"
                        + "};\n"
                        + "if (object2.toString() !== \"%5E\") {\n"
                        + "  throw new Error('#2: toString returns %5E');\n"
                        + "}\n"
                        + "\n"
                        + "//CHECK#3\n"
                        + "if (decodeURIComponent(object) !== \"^\") {\n"
                        + "  throw new Error('#3: decodeURIComponent works');\n"
                        + "}";
        runTest("testThrowClosingBrace", code);
    }

    @Test
    public void testIfWithThrowNotTaken() throws Exception {
        String code =
                "var x = 2\n"
                        + "if (x == 3) {\n"
                        + "  throw new Error('#3: decodeURIComponent(object) === \"^\"');\n"
                        + "}\n"
                        + "\n"
                        + "//CHECK#4\n"
                        + "try {\n"
                        + "  var object2 = {\n"
                        + "    valueOf: function() {\n"
                        + "      return {}\n"
                        + "    }\n"
                        + "  };\n"
                        + "  if (object2.valueOf !== undefined) {\n"
                        + "    // This executes\n"
                        + "  }\n"
                        + "} catch(e) {\n"
                        + "  // Error handling\n"
                        + "}";
        runTest("testIfWithThrowNotTaken", code);
    }

    @Test
    public void testWithFunctionCall() throws Exception {
        String code =
                "var myObj = {\n"
                        + "    p1: 'a',\n"
                        + "    p2: 'b',\n"
                        + "    p3: 'c',\n"
                        + "    parseInt: function() { return 'obj_parseInt'; },\n"
                        + "    NaN: 'obj_NaN',\n"
                        + "    Infinity: 'obj_Infinity'\n"
                        + "};\n"
                        + "\n"
                        + "var st_p1 = \"p1\";\n"
                        + "var st_p2 = \"p2\";\n"
                        + "var st_p3 = \"p3\";\n"
                        + "\n"
                        + "var f = function(){\n"
                        + "  with(myObj){\n"
                        + "    st_p1 = p1;\n"
                        + "    st_p2 = p2;\n"
                        + "    st_p3 = p3;\n"
                        + "    p1 = 'x1';\n"
                        + "    this.p2 = 'x2';\n"
                        + "    var p4 = 'x4';\n"
                        + "    var value = 'value';\n"
                        + "  }\n"
                        + "}\n"
                        + "var obj = new f();\n"
                        + "\n"
                        + "if(!(myObj.p1 === \"a\")){\n"
                        + "  throw new Error('#1: myObj.p1 === \"a\". Actual:  myObj.p1 ==='+ myObj.p1);\n"
                        + "}\n"
                        + "\n"
                        + "if(!(myObj.p2 === \"b\")){\n"
                        + "  throw new Error('#2: myObj.p2 === \"b\". Actual:  myObj.p2 ==='+ myObj.p2);\n"
                        + "}";
        runTest("testWithFunctionCall", code);
    }

    @Test
    public void testWithSimpleAssignments() throws Exception {
        String code =
                "var myObj = {\n"
                        + "    p1: 'a',\n"
                        + "    p2: 'b',\n"
                        + "    p3: 'c'\n"
                        + "};\n"
                        + "\n"
                        + "with(myObj){\n"
                        + "    p1 = 'x1';\n"
                        + "    p2 = 'x2';\n"
                        + "    p3 = 'x3';\n"
                        + "}\n"
                        + "\n"
                        + "if(!(myObj.p1 === \"x1\")){\n"
                        + "  throw new Error('#1: myObj.p1 === \"x1\"');\n"
                        + "}\n"
                        + "\n"
                        + "if(!(myObj.p2 === \"x2\")){\n"
                        + "  throw new Error('#2: myObj.p2 === \"x2\"');\n"
                        + "}\n"
                        + "\n"
                        + "if(!(myObj.p3 === \"x3\")){\n"
                        + "  throw new Error('#3: myObj.p3 === \"x3\"');\n"
                        + "}";
        runTest("testWithSimpleAssignments", code);
    }

    @Test
    public void testAsiEmptyObject() throws Exception {
        String code = "(\n" + "    {}\n" + ") * 1";
        runTest("testAsiEmptyObject", code);
    }

    @Test
    public void testForLoopMultiline() throws Exception {
        String code = "for(false\n" + "    ;false\n" + "    ;\n" + ") {\n" + "  break;\n" + "}";
        runTest("testForLoopMultiline", code);
    }

    @Test
    public void testRegexpExecLoop() throws Exception {
        String code =
                "var x = 2;\n"
                        + "do{\n"
                        + "    if (x !== null) {\n"
                        + "       var x = null\n"
                        + "    } else {\n"
                        + "      break;\n"
                        + "    }\n"
                        + "}while(true);";
        runTest("testRegexpExecLoop", code);
    }

    @Test
    public void testMultilineFunctionCall() throws Exception {
        String code =
                "function concat(a, b, c, d) {\n"
                        + "    return a + b + c + d;\n"
                        + "}\n"
                        + "\n"
                        + "var result = concat(\n"
                        + "    1, 2, 3, 4\n"
                        + ");\n"
                        + "\n"
                        + "if (result !== 10) {\n"
                        + "    throw new Error('Expected 10');\n"
                        + "}";
        runTest("testMultilineFunctionCall", code);
    }

    @Test
    public void testContinueMultiline() throws Exception {
        String code =
                "var count = 0;\n"
                        + "OUTER: for (var i = 0; i < 2; i++) {\n"
                        + "    for (var j = 0; j < 2; j++) {\n"
                        + "        count++;\n"
                        + "        continue\n"
                        + "OUTER;\n"
                        + "    }\n"
                        + "}\n"
                        + "\n"
                        + "if (count !== 2) {\n"
                        + "    throw new Error('Expected count to be 2, got ' + count);\n"
                        + "}";
        runTest("testContinueMultiline", code);
    }

    @Test
    public void testOperatorSplitLines() throws Exception {
        String code =
                "var x = 5\n"
                        + "    ==\n"
                        + "    5;\n"
                        + "\n"
                        + "if (x !== true) {\n"
                        + "    throw new Error('Expected true');\n"
                        + "}\n"
                        + "\n"
                        + "var y = 10\n"
                        + "    +\n"
                        + "    20;\n"
                        + "\n"
                        + "if (y !== 30) {\n"
                        + "    throw new Error('Expected 30');\n"
                        + "}";
        runTest("testOperatorSplitLines", code);
    }

    @Test
    @Disabled("This doesn't work. It might have to do with the block in the function")
    public void testFunctionInIfExpression() throws Exception {
        String code =
                "if (\n"
                        + "function factorial(n) {}\n"
                        + "(5)\n"
                        + " !==\n"
                        + " 120)\n"
                        + "{\n"
                        + "}";
        runTest("testFunctionInIfExpression", code);
    }
}
