package org.mozilla.javascript.interpreterv2;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Test suite for const variable assignment behavior in InterpreterV2. Tests compound assignment
 * operators on const variables to ensure proper stack handling.
 */
class ConstAssignmentTest {

    @Test
    void testConstCompoundAdditionAssignment() {
        // Test from regress-452498-184.js - compound assignment to const should return computed
        // value
        String script =
                "'use strict';\n"
                        + "function f() {\n"
                        + "    const e = 8;\n"
                        + "    return e += 3;\n" // Should return 11 even though e remains 8
                        + "}\n"
                        + "f();";

        Utils.assertWithAllModes_ES6(11.0, script);
    }

    @Test
    void testConstCompoundSubtractionAssignment() {
        String script =
                "'use strict';\n"
                        + "function f() {\n"
                        + "    const x = 10;\n"
                        + "    return x -= 5;\n" // Should return 5 even though x remains 10
                        + "}\n"
                        + "f();";

        Utils.assertWithAllModes_ES6(5.0, script);
    }

    @Test
    void testConstCompoundMultiplicationAssignment() {
        String script =
                "'use strict';\n"
                        + "function f() {\n"
                        + "    const y = 4;\n"
                        + "    return y *= 3;\n" // Should return 12 even though y remains 4
                        + "}\n"
                        + "f();";

        Utils.assertWithAllModes_ES6(12.0, script);
    }

    @Test
    void testConstCompoundDivisionAssignment() {
        String script =
                "'use strict';\n"
                        + "function f() {\n"
                        + "    const z = 20;\n"
                        + "    return z /= 4;\n" // Should return 5 even though z remains 20
                        + "}\n"
                        + "f();";

        Utils.assertWithAllModes_ES6(5.0, script);
    }

    @Test
    void testConstSimpleAssignment() {
        // Simple assignment to const should also return the attempted value
        String script =
                "'use strict';\n"
                        + "function f() {\n"
                        + "    const c = 100;\n"
                        + "    return c = 200;\n" // Should return 200 even though c remains 100
                        + "}\n"
                        + "f();";

        Utils.assertWithAllModes_ES6(200.0, script);
    }

    @Test
    void testConstIncrementOperator() {
        String script =
                "'use strict';\n"
                        + "function f() {\n"
                        + "    const n = 5;\n"
                        + "    return ++n;\n" // Should return 6 even though n remains 5
                        + "}\n"
                        + "f();";

        Utils.assertWithAllModes_ES6(6.0, script);
    }

    @Test
    void testConstDecrementOperator() {
        String script =
                "'use strict';\n"
                        + "function f() {\n"
                        + "    const m = 10;\n"
                        + "    return --m;\n" // Should return 9 even though m remains 10
                        + "}\n"
                        + "f();";

        Utils.assertWithAllModes_ES6(9.0, script);
    }

    @Test
    void testConstPostIncrementOperator() {
        String script =
                "'use strict';\n"
                        + "function f() {\n"
                        + "    const p = 7;\n"
                        + "    return p++;\n" // Should return 7 (original value) even though p
                        // remains 7
                        + "}\n"
                        + "f();";

        Utils.assertWithAllModes_ES6(7.0, script);
    }

    @Test
    void testConstPostDecrementOperator() {
        String script =
                "'use strict';\n"
                        + "function f() {\n"
                        + "    const q = 15;\n"
                        + "    return q--;\n" // Should return 15 (original value) even though q
                        // remains 15
                        + "}\n"
                        + "f();";

        Utils.assertWithAllModes_ES6(15.0, script);
    }

    @Test
    void testMultipleConstAssignments() {
        // Test multiple assignments in sequence to ensure stack doesn't overflow
        String script =
                "'use strict';\n"
                        + "function f() {\n"
                        + "    const a = 1;\n"
                        + "    var results = [];\n"
                        + "    results.push(a += 2);\n" // 3
                        + "    results.push(a *= 3);\n" // 3 (a is still 1, so 1*3)
                        + "    results.push(a -= 1);\n" // 0 (a is still 1, so 1-1)
                        + "    results.push(a);\n" // 1 (original value)
                        + "    return results.join(',');\n"
                        + "}\n"
                        + "f();";

        Utils.assertWithAllModes_ES6("3,3,0,1", script);
    }

    @Test
    void testConstAssignmentWithStrings() {
        String script =
                "'use strict';\n"
                        + "function f() {\n"
                        + "    const s = 'hello';\n"
                        + "    return s += ' world';\n" // Should return 'hello world' even though s
                        // remains 'hello'
                        + "}\n"
                        + "f();";

        Utils.assertWithAllModes_ES6("hello world", script);
    }

    @Test
    void testConstBitwiseAssignment() {
        String script =
                "'use strict';\n"
                        + "function f() {\n"
                        + "    const b = 5;\n" // 0101 in binary
                        + "    return b &= 3;\n" // 0011 in binary, result should be 0001 = 1
                        + "}\n"
                        + "f();";

        Utils.assertWithAllModes_ES6(1.0, script);
    }
}
