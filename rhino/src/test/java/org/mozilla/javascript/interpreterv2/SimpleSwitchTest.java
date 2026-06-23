package org.mozilla.javascript.interpreterv2;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Test suite for SimpleSwitch instruction in InterpreterV2. Tests switch statement behavior with
 * various edge cases.
 */
class SimpleSwitchTest {

    @Test
    void testNegativeNumbers() {
        String script =
                "var result = 'none';\n"
                        + "var x = -1;\n"
                        + "switch(x) {\n"
                        + "    case -1:\n"
                        + "        result = 'negative one';\n"
                        + "        break;\n"
                        + "    case 0:\n"
                        + "        result = 'zero';\n"
                        + "        break;\n"
                        + "    case 1:\n"
                        + "        result = 'one';\n"
                        + "        break;\n"
                        + "    default:\n"
                        + "        result = 'default';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("negative one", script);
    }

    @Test
    void testNegativeDoubleValues() {
        String script =
                "var result = 'none';\n"
                        + "var x = -2.5;\n"
                        + "switch(x) {\n"
                        + "    case -2.5:\n"
                        + "        result = 'matched';\n"
                        + "        break;\n"
                        + "    default:\n"
                        + "        result = 'default';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("matched", script);
    }

    @Test
    void testDuplicateCases() {
        // JavaScript spec says duplicate cases are allowed but only first match executes
        String script =
                "var result = 'none';\n"
                        + "var x = 1;\n"
                        + "switch(x) {\n"
                        + "    case 1:\n"
                        + "        result = 'first';\n"
                        + "        break;\n"
                        + "    case 2:\n"
                        + "        result = 'two';\n"
                        + "        break;\n"
                        + "    case 1:\n" // Duplicate case
                        + "        result = 'second';\n"
                        + "        break;\n"
                        + "    default:\n"
                        + "        result = 'default';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("first", script);
    }

    @Test
    void testZeroValue() {
        String script =
                "var result = 'none';\n"
                        + "var x = 0;\n"
                        + "switch(x) {\n"
                        + "    case 0:\n"
                        + "        result = 'zero';\n"
                        + "        break;\n"
                        + "    case 1:\n"
                        + "        result = 'one';\n"
                        + "        break;\n"
                        + "    default:\n"
                        + "        result = 'default';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("zero", script);
    }

    @Test
    void testStringValues() {
        String script =
                "var result = 'none';\n"
                        + "var x = 'hello';\n"
                        + "switch(x) {\n"
                        + "    case 'hi':\n"
                        + "        result = 'hi';\n"
                        + "        break;\n"
                        + "    case 'hello':\n"
                        + "        result = 'hello matched';\n"
                        + "        break;\n"
                        + "    case 'bye':\n"
                        + "        result = 'bye';\n"
                        + "        break;\n"
                        + "    default:\n"
                        + "        result = 'default';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("hello matched", script);
    }

    @Test
    void testBooleanValues() {
        String script =
                "var result = 'none';\n"
                        + "var x = true;\n"
                        + "switch(x) {\n"
                        + "    case true:\n"
                        + "        result = 'true';\n"
                        + "        break;\n"
                        + "    case false:\n"
                        + "        result = 'false';\n"
                        + "        break;\n"
                        + "    default:\n"
                        + "        result = 'default';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("true", script);
    }

    @Test
    void testNullValue() {
        String script =
                "var result = 'none';\n"
                        + "var x = null;\n"
                        + "switch(x) {\n"
                        + "    case null:\n"
                        + "        result = 'null';\n"
                        + "        break;\n"
                        + "    case undefined:\n"
                        + "        result = 'undefined';\n"
                        + "        break;\n"
                        + "    default:\n"
                        + "        result = 'default';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("null", script);
    }

    @Test
    void testUndefinedValue() {
        String script =
                "var result = 'none';\n"
                        + "var x = undefined;\n"
                        + "switch(x) {\n"
                        + "    case null:\n"
                        + "        result = 'null';\n"
                        + "        break;\n"
                        + "    case undefined:\n"
                        + "        result = 'undefined';\n"
                        + "        break;\n"
                        + "    default:\n"
                        + "        result = 'default';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("undefined", script);
    }

    @Test
    void testFallthrough() {
        String script =
                "var result = '';\n"
                        + "var x = 1;\n"
                        + "switch(x) {\n"
                        + "    case 1:\n"
                        + "        result += 'one';\n"
                        + "    case 2:\n"
                        + "        result += 'two';\n"
                        + "    case 3:\n"
                        + "        result += 'three';\n"
                        + "        break;\n"
                        + "    default:\n"
                        + "        result += 'default';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("onetwothree", script);
    }

    @Test
    void testNoMatchNoDefault() {
        String script =
                "var result = 'unchanged';\n"
                        + "var x = 99;\n"
                        + "switch(x) {\n"
                        + "    case 1:\n"
                        + "        result = 'one';\n"
                        + "        break;\n"
                        + "    case 2:\n"
                        + "        result = 'two';\n"
                        + "        break;\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("unchanged", script);
    }

    @Test
    void testExpressionInCase() {
        String script =
                "var result = 'none';\n"
                        + "var x = 6;\n"
                        + "switch(x) {\n"
                        + "    case 2 + 2:\n"
                        + "        result = 'four';\n"
                        + "        break;\n"
                        + "    case 3 * 2:\n"
                        + "        result = 'six';\n"
                        + "        break;\n"
                        + "    default:\n"
                        + "        result = 'default';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("six", script);
    }

    @Test
    void testEmptySwitch() {
        String script =
                "var result = 'before';\n"
                        + "var x = 1;\n"
                        + "switch(x) {\n"
                        + "}\n"
                        + "result = 'after';\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("after", script);
    }

    @Test
    void testOnlyDefault() {
        String script =
                "var result = 'none';\n"
                        + "var x = 99;\n"
                        + "switch(x) {\n"
                        + "    default:\n"
                        + "        result = 'default';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("default", script);
    }

    @Test
    void testMixedPositiveNegativeNumbers() {
        String script =
                "var results = [];\n"
                        + "var values = [-3, -2, -1, 0, 1, 2, 3];\n"
                        + "for (var j = 0; j < values.length; j++) {\n"
                        + "    var i = values[j];\n"
                        + "    switch(i) {\n"
                        + "        case -3:\n"
                        + "            results.push('-3');\n"
                        + "            break;\n"
                        + "        case -2:\n"
                        + "            results.push('-2');\n"
                        + "            break;\n"
                        + "        case -1:\n"
                        + "            results.push('-1');\n"
                        + "            break;\n"
                        + "        case 0:\n"
                        + "            results.push('0');\n"
                        + "            break;\n"
                        + "        case 1:\n"
                        + "            results.push('1');\n"
                        + "            break;\n"
                        + "        case 2:\n"
                        + "            results.push('2');\n"
                        + "            break;\n"
                        + "        case 3:\n"
                        + "            results.push('3');\n"
                        + "            break;\n"
                        + "        default:\n"
                        + "            results.push('default');\n"
                        + "    }\n"
                        + "}\n"
                        + "results.join(',');";

        Utils.assertWithAllModes_ES6("-3,-2,-1,0,1,2,3", script);
    }

    @Test
    void testNegativeZeroEqualsPositiveZero() {
        // Test from regress-444979.js - JavaScript treats -0.0 and 0.0 as equal in switch
        String script =
                "var result = 'none';\n"
                        + "var x = -0.0;\n"
                        + "switch(x) {\n"
                        + "    case 0:\n"
                        + "        result = 'zero';\n"
                        + "        break;\n"
                        + "    default:\n"
                        + "        result = 'default';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("zero", script);
    }

    @Test
    void testPositiveZeroMatchesNegativeZeroCase() {
        // Reverse test - positive zero should match negative zero case
        String script =
                "var result = 'none';\n"
                        + "var x = 0.0;\n"
                        + "switch(x) {\n"
                        + "    case -0:\n"
                        + "        result = 'matched';\n"
                        + "        break;\n"
                        + "    default:\n"
                        + "        result = 'default';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("matched", script);
    }

    @Test
    void testEmptySwitchWithTryCatch() {
        // Test from regress-312260.js - empty switch in try block shouldn't throw
        String script =
                "var result = 'No error';\n"
                        + "try {\n"
                        + "    switch ({}.foo) {}\n"
                        + "} catch(e) {\n"
                        + "    result = 'Error: ' + e;\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("No error", script);
    }

    @Test
    void matchingNullUnmatched() {
        String script =
                ""
                        + "var result;\n"
                        + "switch ('x') {\n"
                        + "  case null: result = 'null'; break;\n"
                        + "  default: result = 'default'; break;\n"
                        + "}\n"
                        + "result";

        Utils.assertWithAllModes_ES6("default", script);
    }

    @Test
    void matchingNullMatched() {
        String script =
                ""
                        + "var result;\n"
                        + "switch (null) {\n"
                        + "  case null: result = 'null'; break;\n"
                        + "  default: result = 'default'; break;\n"
                        + "}\n"
                        + "result";

        Utils.assertWithAllModes_ES6("null", script);
    }

    @Test
    void matchingUndefinedUnmatched() {
        String script =
                ""
                        + "var result;\n"
                        + "switch ('x') {\n"
                        + "  case undefined: result = 'undefined'; break;\n"
                        + "  default: result = 'default'; break;\n"
                        + "}\n"
                        + "result";

        Utils.assertWithAllModes_ES6("default", script);
    }

    @Test
    void matchingUndefinedMatched() {
        String script =
                ""
                        + "var result;\n"
                        + "switch (undefined) {\n"
                        + "  case undefined: result = 'undefined'; break;\n"
                        + "  default: result = 'default'; break;\n"
                        + "}\n"
                        + "result";

        Utils.assertWithAllModes_ES6("undefined", script);
    }

    @Test
    void matchingStrings() {
        String script =
                ""
                        + "var a = 'a';\n"
                        + "var result;\n"
                        + "switch (a) {\n"
                        + "  case 'a': result = 'a'; break;\n"
                        + "  default: result = 'default'; break;\n"
                        + "}\n"
                        + "result";

        Utils.assertWithAllModes_ES6("a", script);
    }

    @Test
    void matchingConsStrings() {
        String script =
                ""
                        + "var a = 'a';\n"
                        + "var result;\n"
                        + "switch (a + 'b') {\n"
                        + "  case 'ab': result = 'ab'; break;\n"
                        + "  default: result = 'default'; break;\n"
                        + "}\n"
                        + "result";

        Utils.assertWithAllModes_ES6("ab", script);
    }
}
