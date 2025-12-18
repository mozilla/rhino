package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/** Object rest in destructuring throws SyntaxError */
public class ObjectRestErrorTest {

    @Test
    public void objectRestThrowsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "object rest properties in destructuring are not supported",
                "function f() { var { a, ...rest } = { a: 1, b: 2, c: 3 }; }");
    }

    @Test
    public void objectRestInFunctionParamThrowsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "object rest properties in destructuring are not supported",
                "function f({ a, ...rest }) { return rest; }");
    }

    @Test
    public void objectRestInVariableDeclarationThrowsSyntaxError() {
        Utils.assertEvaluatorExceptionES6(
                "object rest properties in destructuring are not supported",
                "var { a, ...rest } = { a: 1, b: 2, c: 3 };");
    }
}
