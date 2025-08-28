package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class ApplyFunctionTest {

    @Test
    public void fooArgs0_invokeArgs0() {
        Utils.assertWithAllModes_ES6("applyThis []", constructCode(0, "foo.apply(", 0, false));
        Utils.assertWithAllModes_ES6("applyThis []", constructCode(0, "foo.apply(", 0, true));
    }

    @Test
    public void fooArgs0_invokeArgsNull() {
        Utils.assertWithAllModes_ES6("applyThis []", constructCode(0, "foo.apply(", -1, false));
        Utils.assertWithAllModes_ES6("applyThis []", constructCode(0, "foo.apply(", -1, true));
    }

    @Test
    public void fooArgs0_invokeArgsEmpty() {
        Utils.assertWithAllModes_ES6("applyThis []", constructCode(0, "foo.apply(", -1, false));
        Utils.assertWithAllModes_ES6("applyThis []", constructCode(0, "foo.apply(", -1, true));
    }

    @Test
    public void fooArgs0_invokeArgs1() {
        Utils.assertWithAllModes_ES6("applyThis [x]", constructCode(0, "foo.apply(", 1, false));
        Utils.assertWithAllModes_ES6("applyThis [x]", constructCode(0, "foo.apply(", 1, true));
    }

    @Test
    public void fooArgs0_invokeArgs2() {
        Utils.assertWithAllModes_ES6("applyThis [x,y]", constructCode(0, "foo.apply(", 2, false));
        Utils.assertWithAllModes_ES6("applyThis [x,y]", constructCode(0, "foo.apply(", 2, true));
    }

    @Test
    public void fooArgs1_invokeArgs0() {
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []", constructCode(1, "foo.apply(", 0, false));
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []", constructCode(1, "foo.apply(", 0, true));
    }

    @Test
    public void fooArgs1_invokeArgsNull() {
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []", constructCode(1, "foo.apply(", -1, false));
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []", constructCode(1, "foo.apply(", -1, true));
    }

    @Test
    public void fooArgs1_invokeArgsEmpty() {
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []", constructCode(1, "foo.apply(", -2, false));
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []", constructCode(1, "foo.apply(", -2, true));
    }

    @Test
    public void fooArgs1_invokeArgs1() {
        Utils.assertWithAllModes_ES6("applyThis x []", constructCode(1, "foo.apply(", 1, false));
        Utils.assertWithAllModes_ES6("applyThis x []", constructCode(1, "foo.apply(", 1, true));
    }

    @Test
    public void fooArgs1_invokeArgs2() {
        Utils.assertWithAllModes_ES6("applyThis x [y]", constructCode(1, "foo.apply(", 2, false));
        Utils.assertWithAllModes_ES6("applyThis x [y]", constructCode(1, "foo.apply(", 2, true));
    }

    @Test
    public void fooArgs2_invokeArgs0() {
        Utils.assertWithAllModes_ES6(
                "applyThis undefined,undefined []", constructCode(2, "foo.apply(", 0, false));
        Utils.assertWithAllModes_ES6(
                "applyThis undefined,undefined []", constructCode(2, "foo.apply(", 0, true));
    }

    @Test
    public void fooArgs2_invokeArgsNull() {
        Utils.assertWithAllModes_ES6(
                "applyThis undefined,undefined []", constructCode(2, "foo.apply(", -1, false));
        Utils.assertWithAllModes_ES6(
                "applyThis undefined,undefined []", constructCode(2, "foo.apply(", -1, true));
    }

    @Test
    public void fooArgs2_invokeArgsEmpty() {
        Utils.assertWithAllModes_ES6(
                "applyThis undefined,undefined []", constructCode(2, "foo.apply(", -2, false));
        Utils.assertWithAllModes_ES6(
                "applyThis undefined,undefined []", constructCode(2, "foo.apply(", -2, true));
    }

    @Test
    public void fooArgs2_invokeArgs1() {
        Utils.assertWithAllModes_ES6(
                "applyThis x,undefined []", constructCode(2, "foo.apply(", 1, false));
        Utils.assertWithAllModes_ES6(
                "applyThis x,undefined []", constructCode(2, "foo.apply(", 1, true));
    }

    @Test
    public void fooArgs2_invokeArgs2() {
        Utils.assertWithAllModes_ES6("applyThis x,y []", constructCode(2, "foo.apply(", 2, false));
        Utils.assertWithAllModes_ES6("applyThis x,y []", constructCode(2, "foo.apply(", 2, true));
    }

    @Test
    public void fooArgs0_callArgs0() {
        Utils.assertWithAllModes_ES6(
                "applyThis []", constructCode(0, "Function.prototype.apply.call(foo, ", 0, false));
        Utils.assertWithAllModes_ES6(
                "applyThis []", constructCode(0, "Function.prototype.apply.call(foo, ", 0, true));
    }

    @Test
    public void fooArgs0_callArgsNull() {
        Utils.assertWithAllModes_ES6(
                "applyThis []", constructCode(0, "Function.prototype.apply.call(foo, ", -1, false));
        Utils.assertWithAllModes_ES6(
                "applyThis []", constructCode(0, "Function.prototype.apply.call(foo, ", -1, true));
    }

    @Test
    public void fooArgs0_callArgsEmpty() {
        Utils.assertWithAllModes_ES6(
                "applyThis []", constructCode(0, "Function.prototype.apply.call(foo, ", -2, false));
        Utils.assertWithAllModes_ES6(
                "applyThis []", constructCode(0, "Function.prototype.apply.call(foo, ", -2, true));
    }

    @Test
    public void fooArgs0_callArgs1() {
        Utils.assertWithAllModes_ES6(
                "applyThis [x]", constructCode(0, "Function.prototype.apply.call(foo, ", 1, false));
        Utils.assertWithAllModes_ES6(
                "applyThis [x]", constructCode(0, "Function.prototype.apply.call(foo, ", 1, true));
    }

    @Test
    public void fooArgs0_callArgs2() {
        Utils.assertWithAllModes_ES6(
                "applyThis [x,y]",
                constructCode(0, "Function.prototype.apply.call(foo, ", 2, false));
        Utils.assertWithAllModes_ES6(
                "applyThis [x,y]",
                constructCode(0, "Function.prototype.apply.call(foo, ", 2, true));
    }

    @Test
    public void fooArgs1_callArgs0() {
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []",
                constructCode(1, "Function.prototype.apply.call(foo, ", 0, false));
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []",
                constructCode(1, "Function.prototype.apply.call(foo, ", 0, true));
    }

    @Test
    public void fooArgs1_callArgsNull() {
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []",
                constructCode(1, "Function.prototype.apply.call(foo, ", -1, false));
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []",
                constructCode(1, "Function.prototype.apply.call(foo, ", -1, true));
    }

    @Test
    public void fooArgs1_callArgsEmpty() {
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []",
                constructCode(1, "Function.prototype.apply.call(foo, ", -2, false));
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []",
                constructCode(1, "Function.prototype.apply.call(foo, ", -2, true));
    }

    @Test
    public void fooArgs1_callArgs1() {
        Utils.assertWithAllModes_ES6(
                "applyThis x []",
                constructCode(1, "Function.prototype.apply.call(foo, ", 1, false));
        Utils.assertWithAllModes_ES6(
                "applyThis x []", constructCode(1, "Function.prototype.apply.call(foo, ", 1, true));
    }

    @Test
    public void fooArgs1_callArgs2() {
        Utils.assertWithAllModes_ES6(
                "applyThis x [y]",
                constructCode(1, "Function.prototype.apply.call(foo, ", 2, false));
        Utils.assertWithAllModes_ES6(
                "applyThis x [y]",
                constructCode(1, "Function.prototype.apply.call(foo, ", 2, true));
    }

    @Test
    public void fooArgs2_callArgs0() {
        Utils.assertWithAllModes_ES6(
                "applyThis undefined,undefined []",
                constructCode(2, "Function.prototype.apply.call(foo, ", 0, false));
        Utils.assertWithAllModes_ES6(
                "applyThis undefined,undefined []",
                constructCode(2, "Function.prototype.apply.call(foo, ", 0, true));
    }

    @Test
    public void fooArgs2_callArgsNull() {
        Utils.assertWithAllModes_ES6(
                "applyThis undefined,undefined []",
                constructCode(2, "Function.prototype.apply.call(foo, ", -1, false));
        Utils.assertWithAllModes_ES6(
                "applyThis undefined,undefined []",
                constructCode(2, "Function.prototype.apply.call(foo, ", -1, true));
    }

    @Test
    public void fooArgs2_callArgsEmpty() {
        Utils.assertWithAllModes_ES6(
                "applyThis undefined,undefined []",
                constructCode(2, "Function.prototype.apply.call(foo, ", -2, false));
        Utils.assertWithAllModes_ES6(
                "applyThis undefined,undefined []",
                constructCode(2, "Function.prototype.apply.call(foo, ", -2, true));
    }

    @Test
    public void fooArgs2_callArgs1() {
        Utils.assertWithAllModes_ES6(
                "applyThis x,undefined []",
                constructCode(2, "Function.prototype.apply.call(foo, ", 1, false));
        Utils.assertWithAllModes_ES6(
                "applyThis x,undefined []",
                constructCode(2, "Function.prototype.apply.call(foo, ", 1, true));
    }

    @Test
    public void fooArgs2_callArgs2() {
        Utils.assertWithAllModes_ES6(
                "applyThis x,y []",
                constructCode(2, "Function.prototype.apply.call(foo, ", 2, false));
        Utils.assertWithAllModes_ES6(
                "applyThis x,y []",
                constructCode(2, "Function.prototype.apply.call(foo, ", 2, true));
    }

    @Test
    public void fooArgs0_applyArgs0() {
        Utils.assertWithAllModes_ES6(
                "applyThis []", constructCode(0, "Function.prototype.apply.apply(foo, ", 0, false));
        Utils.assertWithAllModes_ES6(
                "applyThis []", constructCode(0, "Function.prototype.apply.apply(foo, ", 0, true));
    }

    @Test
    public void fooArgs0_applyArgsNull() {
        Utils.assertWithAllModes_ES6(
                "applyThis []",
                constructCode(0, "Function.prototype.apply.apply(foo, ", -1, false));
        Utils.assertWithAllModes_ES6(
                "applyThis []", constructCode(0, "Function.prototype.apply.apply(foo, ", -1, true));
    }

    @Test
    public void fooArgs0_applyArgsEmpty() {
        Utils.assertWithAllModes_ES6(
                "applyThis []",
                constructCode(0, "Function.prototype.apply.apply(foo, ", -2, false));
        Utils.assertWithAllModes_ES6(
                "applyThis []", constructCode(0, "Function.prototype.apply.apply(foo, ", -2, true));
    }

    @Test
    public void fooArgs0_applyArgs1() {
        Utils.assertWithAllModes_ES6(
                "applyThis [x]",
                constructCode(0, "Function.prototype.apply.apply(foo, ", 1, false));
        Utils.assertWithAllModes_ES6(
                "applyThis [x]", constructCode(0, "Function.prototype.apply.apply(foo, ", 1, true));
    }

    @Test
    public void fooArgs0_applyArgs2() {
        Utils.assertWithAllModes_ES6(
                "applyThis [x,y]",
                constructCode(0, "Function.prototype.apply.apply(foo, ", 2, false));
        Utils.assertWithAllModes_ES6(
                "applyThis [x,y]",
                constructCode(0, "Function.prototype.apply.apply(foo, ", 2, true));
    }

    @Test
    public void fooArgs1_applyArgs0() {
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []",
                constructCode(1, "Function.prototype.apply.apply(foo, ", 0, false));
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []",
                constructCode(1, "Function.prototype.apply.apply(foo, ", 0, true));
    }

    @Test
    public void fooArgs1_applyArgsNull() {
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []",
                constructCode(1, "Function.prototype.apply.apply(foo, ", -1, false));
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []",
                constructCode(1, "Function.prototype.apply.apply(foo, ", -1, true));
    }

    @Test
    public void fooArgs1_applyArgsEmpty() {
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []",
                constructCode(1, "Function.prototype.apply.apply(foo, ", -2, false));
        Utils.assertWithAllModes_ES6(
                "applyThis undefined []",
                constructCode(1, "Function.prototype.apply.apply(foo, ", -2, true));
    }

    @Test
    public void fooArgs1_applyArgs1() {
        Utils.assertWithAllModes_ES6(
                "applyThis x []",
                constructCode(1, "Function.prototype.apply.apply(foo, ", 1, false));
        Utils.assertWithAllModes_ES6(
                "applyThis x []",
                constructCode(1, "Function.prototype.apply.apply(foo, ", 1, true));
    }

    @Test
    public void fooArgs1_applyArgs2() {
        Utils.assertWithAllModes_ES6(
                "applyThis x [y]",
                constructCode(1, "Function.prototype.apply.apply(foo, ", 2, false));
        Utils.assertWithAllModes_ES6(
                "applyThis x [y]",
                constructCode(1, "Function.prototype.apply.apply(foo, ", 2, true));
    }

    public String constructCode(int fooArgs, String invokeFoo, int invokeArgs, boolean activation) {
        StringBuilder code = new StringBuilder();
        code.append("function foo(");
        if (fooArgs == 1) {
            code.append("i");
        } else if (fooArgs == 2) {
            code.append("i,j");
        }
        code.append(") {\n");

        if (activation) {
            code.append("  function inner(s) { return s };\n");
        }
        code.append("  var args = Array.prototype.slice.call(arguments, ")
                .append(Integer.toString(fooArgs))
                .append(");\n");

        code.append("  var res = this.toString()");

        if (fooArgs == 1) {
            code.append(" + ' ' + i");
        } else if (fooArgs == 2) {
            code.append(" + ' ' + i + ',' + j");
        }
        code.append(" + ' [' + args.join(',') + ']';\n");
        if (activation) {
            code.append("  return inner(res);\n");
        } else {
            code.append("  return res;\n");
        }
        code.append("};\n");

        code.append(invokeFoo);
        if (invokeFoo.contains("apply.apply(")) {
            code.append('[');
        }
        code.append("'applyThis'");
        if (invokeArgs == 1) {
            code.append(", ['x']");
        } else if (invokeArgs == 2) {
            code.append(", ['x', 'y']");
        } else if (invokeArgs == -1) {
            code.append(", null");
        } else if (invokeArgs == -2) {
            code.append(", []");
        }
        if (invokeFoo.contains("apply.apply(")) {
            code.append(']');
        }
        code.append(");");

        return code.toString();
    }
}
