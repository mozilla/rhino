package org.mozilla.javascript.interpreterv2;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Test suite for conditional catch blocks functionality in InterpreterV2. Tests the Mozilla/Rhino
 * extension: catch (e if condition) { ... }
 */
class ConditionalCatchTest {

    @Test
    void testBasicConditionalCatch() {
        String script =
                "var result = 'none';\n"
                        + "try {\n"
                        + "    throw new TypeError('test error');\n"
                        + "} catch (e if e instanceof TypeError) {\n"
                        + "    result = 'caught TypeError';\n"
                        + "} catch (e) {\n"
                        + "    result = 'caught other';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("caught TypeError", script);
    }

    @Test
    void testMultipleConditionalCatches() {
        String script =
                "var result = 'none';\n"
                        + "try {\n"
                        + "    throw new ReferenceError('reference error');\n"
                        + "} catch (e if e instanceof TypeError) {\n"
                        + "    result = 'caught TypeError';\n"
                        + "} catch (e if e instanceof ReferenceError) {\n"
                        + "    result = 'caught ReferenceError';\n"
                        + "} catch (e) {\n"
                        + "    result = 'caught other';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("caught ReferenceError", script);
    }

    @Test
    void testComplexCondition() {
        String script =
                "var result = 'none';\n"
                        + "try {\n"
                        + "    throw new TypeError('special error');\n"
                        + "} catch (e if e instanceof TypeError && e.message.indexOf('special') >= 0) {\n"
                        + "    result = 'caught special TypeError';\n"
                        + "} catch (e if e instanceof TypeError) {\n"
                        + "    result = 'caught regular TypeError';\n"
                        + "} catch (e) {\n"
                        + "    result = 'caught other';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("caught special TypeError", script);
    }

    @Test
    void testFallthroughToDefault() {
        String script =
                "var result = 'none';\n"
                        + "try {\n"
                        + "    throw new Error('generic error');\n"
                        + "} catch (e if e instanceof TypeError) {\n"
                        + "    result = 'caught TypeError';\n"
                        + "} catch (e if e instanceof ReferenceError) {\n"
                        + "    result = 'caught ReferenceError';\n"
                        + "} catch (e) {\n"
                        + "    result = 'caught default';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("caught default", script);
    }

    @Test
    void testVariableBinding() {
        String script =
                "var result = 'none';\n"
                        + "var outerVar = 'outer';\n"
                        + "try {\n"
                        + "    throw new TypeError('test');\n"
                        + "} catch (e if outerVar === 'outer' && e instanceof TypeError) {\n"
                        + "    result = 'caught with outer variable: ' + outerVar;\n"
                        + "} catch (e) {\n"
                        + "    result = 'caught other';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("caught with outer variable: outer", script);
    }

    @Test
    void testScopeIsolation() {
        String script =
                "var e = 'outer';\n"
                        + "var result = 'none';\n"
                        + "try {\n"
                        + "    throw new TypeError('test');\n"
                        + "} catch (e if e instanceof TypeError) {\n"
                        + "    result = 'inner e is TypeError: ' + (e instanceof TypeError);\n"
                        + "}\n"
                        + "result + ', outer e: ' + e;";

        Utils.assertWithAllModes_ES6("inner e is TypeError: true, outer e: outer", script);
    }

    @Test
    void testNestedConditionalCatch() {
        String script =
                "var result = 'none';\n"
                        + "try {\n"
                        + "    try {\n"
                        + "        throw new TypeError('inner error');\n"
                        + "    } catch (e if e instanceof ReferenceError) {\n"
                        + "        result = 'caught inner ReferenceError';\n"
                        + "    } catch (e if e instanceof TypeError) {\n"
                        + "        result = 'caught inner TypeError';\n"
                        + "        throw new ReferenceError('outer error');\n"
                        + "    }\n"
                        + "} catch (e if e instanceof ReferenceError) {\n"
                        + "    result = 'caught outer ReferenceError';\n"
                        + "} catch (e) {\n"
                        + "    result = 'caught outer other';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("caught outer ReferenceError", script);
    }

    @Test
    void testConditionalCatchWithFinally() {
        String script =
                "var result = 'none';\n"
                        + "var finallyExecuted = false;\n"
                        + "try {\n"
                        + "    throw new TypeError('test');\n"
                        + "} catch (e if e instanceof TypeError) {\n"
                        + "    result = 'caught TypeError';\n"
                        + "} catch (e) {\n"
                        + "    result = 'caught other';\n"
                        + "} finally {\n"
                        + "    finallyExecuted = true;\n"
                        + "}\n"
                        + "result + ',' + finallyExecuted;";

        Utils.assertWithAllModes_ES6("caught TypeError,true", script);
    }

    @Test
    void testStringException() {
        String script =
                "var result = 'none';\n"
                        + "try {\n"
                        + "    throw 'string exception';\n"
                        + "} catch (e if typeof e === 'string') {\n"
                        + "    result = 'caught string: ' + e;\n"
                        + "} catch (e) {\n"
                        + "    result = 'caught other';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("caught string: string exception", script);
    }

    @Test
    void testNumberException() {
        String script =
                "var result = 'none';\n"
                        + "try {\n"
                        + "    throw 42;\n"
                        + "} catch (e if typeof e === 'number') {\n"
                        + "    result = 'caught number: ' + e;\n"
                        + "} catch (e) {\n"
                        + "    result = 'caught other';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("caught number: 42", script);
    }

    @Test
    void testFunctionCallInCondition() {
        String script =
                "function isSpecialError(e) {\n"
                        + "    return e instanceof TypeError && e.message === 'special';\n"
                        + "}\n"
                        + "\n"
                        + "var result = 'none';\n"
                        + "try {\n"
                        + "    throw new TypeError('special');\n"
                        + "} catch (e if isSpecialError(e)) {\n"
                        + "    result = 'caught special error';\n"
                        + "} catch (e) {\n"
                        + "    result = 'caught other';\n"
                        + "}\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("caught special error", script);
    }

    @Test
    void testConditionEvaluationOrder() {
        String script =
                "var evaluationOrder = [];\n"
                        + "\n"
                        + "function condition1(e) {\n"
                        + "    evaluationOrder.push('condition1');\n"
                        + "    return false;\n"
                        + "}\n"
                        + "\n"
                        + "function condition2(e) {\n"
                        + "    evaluationOrder.push('condition2');\n"
                        + "    return true;\n"
                        + "}\n"
                        + "\n"
                        + "try {\n"
                        + "    throw new Error('test');\n"
                        + "} catch (e if condition1(e)) {\n"
                        + "    // Should not reach here\n"
                        + "} catch (e if condition2(e)) {\n"
                        + "    // Should reach here\n"
                        + "} catch (e) {\n"
                        + "    // Should not reach here\n"
                        + "}\n"
                        + "\n"
                        + "evaluationOrder.join(',');";

        Utils.assertWithAllModes_ES6("condition1,condition2", script);
    }
}
