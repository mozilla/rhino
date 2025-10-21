package org.mozilla.javascript.tests.es5;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/** Test for let. */
public class LetTest {

    @Test
    public void simple() throws Exception {
        String script =
                "function f(a,b,c) {\n"
                        + " let sum = a + b + c;\n"
                        + " return sum;\n"
                        + "}\n"
                        + "f(1, 2, 3);";
        Utils.assertWithAllModes_1_8(6, script);
    }

    @Test
    public void switchLet() throws Exception {
        String script =
                "var sum = 0;\n"
                        + "var t = 1;\n"
                        + "switch(t) {\n"
                        + "  case 1:\n"
                        + "    let s = t + 2;\n"
                        + "    sum += s + 3;\n"
                        + "    break;\n"
                        + "  default:\n"
                        + "    sum = 7;\n"
                        + "}\n"
                        + "sum;";
        Utils.assertWithAllModes_1_8(6, script);
    }

    @Test
    public void forSwitchLet() throws Exception {
        String script =
                "  var sum = 0;\n"
                        + "for (let i = 0; i < 1; i++)\n"
                        + "  switch (i) {\n"
                        + "    case 0:\n"
                        + "      let test = 7;\n"
                        + "      sum += 4;\n"
                        + "      break;\n"
                        + "    }"
                        + "sum;";
        Utils.assertWithAllModes_1_8(4, script);
    }

    @Test
    public void ifSwitchLet() throws Exception {
        String script =
                "var sum = 0;\n"
                        + "if (sum == 0)\n"
                        + "  switch (sum) {\n"
                        + "    case 0:\n"
                        + "      let test = 7;\n"
                        + "      sum += 4;\n"
                        + "      break;\n"
                        + "    }"
                        + "sum;";
        Utils.assertWithAllModes_1_8(4, script);
    }

    @Test
    public void letInsideBodyOfSwitch() {
        String script =
                "switch (0) {\n"
                        + "  default:\n"
                        + "    let f;\n"
                        + "  {\n"
                        + "  }\n"
                        + "}\n"
                        + "\n"
                        + "typeof f;\n";
        Utils.assertWithAllModes_1_8("undefined", script);
    }

	@Test
	public void letInsideSwitchShadowsOuterVariable() {
		String script =
				"var sum = 0;\n" +
						"  let test = 0;\n" +
						"  switch (test) {\n" +
						"    case 0:\n" +
						"      let test = 7;\n" +
						"      sum += test;\n" +
						"      break;\n" +
						"    }\n" +
						"sum";
		Utils.assertWithAllModes_1_8(7, script);
	}
}
