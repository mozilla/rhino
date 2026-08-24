package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.testutils.Utils;

/** Test for new.target. */
public class NewTargetTest {

    @Test
    public void global() {
        Utils.assertException(-1, EvaluatorException.class, "Invalid use of new.", "new.target");
    }

    @Test
    public void globalTypeof() {
        Utils.assertException(
                -1, EvaluatorException.class, "Invalid use of new.", "typeof new.target");
    }

    @Test
    public void eval() {
        Utils.assertException(
                -1, EcmaError.class, "SyntaxError: Invalid use of new.", "eval('new.target;')");
    }

    @Test
    public void evalInsideFunction() {
        Utils.assertWithAllModes("function", "function foo() { eval('new.target;') } typeof foo");
        Utils.assertException(
                -1,
                EcmaError.class,
                "SyntaxError: Invalid use of new.",
                "function foo() { eval('new.target;') } foo()");
    }

    @Test
    public void evalInsideArrowFunction() {
        Utils.assertWithAllModes("function", "f = () => eval('new.target;'); typeof f");
        Utils.assertException(
                -1,
                EcmaError.class,
                "SyntaxError: Invalid use of new.",
                "f = () => eval('new.target;');  f()");
    }

    @Test
    public void typeofFromFunction() {
        Utils.assertWithAllModes("undefined", "function foo() { return typeof new.target } foo();");
    }
}
