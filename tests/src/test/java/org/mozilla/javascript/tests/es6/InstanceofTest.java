package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/** Test for instanceof operator. */
public class InstanceofTest {

    @Test
    public void rightHandSideNotCallable() {
        String js =
                "try {\n"
                        + "  throw SyntaxError();\n"
                        + "} catch (e) {\n"
                        + "  var value = (e instanceof SyntaxError());\n"
                        + "}";

        Utils.assertEcmaError(
                "TypeError: Target of ``instanceof`` must be callable or have ``[Symbol.hasInstance]`` method.",
                js);
    }
}
