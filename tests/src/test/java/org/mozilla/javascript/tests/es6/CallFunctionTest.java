package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class CallFunctionTest {

    @Test
    public void fooArgs0_invokeArgs0() {
        Utils.assertWithAllModes_ES6("callThis []", constructCode(0, "foo.call(", 0, false));
        Utils.assertWithAllModes_ES6("callThis []", constructCode(0, "foo.call(", 0, true));
    }

    @Test
    public void fooArgs0_invokeArgs1() {
        Utils.assertWithAllModes_ES6("callThis [x]", constructCode(0, "foo.call(", 1, false));
        Utils.assertWithAllModes_ES6("callThis [x]", constructCode(0, "foo.call(", 1, true));
    }

    @Test
    public void fooArgs0_invokeArgs2() {
        Utils.assertWithAllModes_ES6("callThis [x,y]", constructCode(0, "foo.call(", 2, false));
        Utils.assertWithAllModes_ES6("callThis [x,y]", constructCode(0, "foo.call(", 2, true));
    }

    @Test
    public void fooArgs1_invokeArgs0() {
        Utils.assertWithAllModes_ES6(
                "callThis undefined []", constructCode(1, "foo.call(", 0, false));
        Utils.assertWithAllModes_ES6(
                "callThis undefined []", constructCode(1, "foo.call(", 0, true));
    }

    @Test
    public void fooArgs1_invokeArgs1() {
        Utils.assertWithAllModes_ES6("callThis x []", constructCode(1, "foo.call(", 1, false));
        Utils.assertWithAllModes_ES6("callThis x []", constructCode(1, "foo.call(", 1, true));
    }

    @Test
    public void fooArgs1_invokeArgs2() {
        Utils.assertWithAllModes_ES6("callThis x [y]", constructCode(1, "foo.call(", 2, false));
        Utils.assertWithAllModes_ES6("callThis x [y]", constructCode(1, "foo.call(", 2, true));
    }

    @Test
    public void fooArgs2_invokeArgs0() {
        Utils.assertWithAllModes_ES6(
                "callThis undefined,undefined []", constructCode(2, "foo.call(", 0, false));
        Utils.assertWithAllModes_ES6(
                "callThis undefined,undefined []", constructCode(2, "foo.call(", 0, true));
    }

    @Test
    public void fooArgs2_invokeArgs1() {
        Utils.assertWithAllModes_ES6(
                "callThis x,undefined []", constructCode(2, "foo.call(", 1, false));
        Utils.assertWithAllModes_ES6(
                "callThis x,undefined []", constructCode(2, "foo.call(", 1, true));
    }

    @Test
    public void fooArgs2_invokeArgs2() {
        Utils.assertWithAllModes_ES6("callThis x,y []", constructCode(2, "foo.call(", 2, false));
        Utils.assertWithAllModes_ES6("callThis x,y []", constructCode(2, "foo.call(", 2, true));
    }

    @Test
    public void fooArgs0_callArgs0() {
        Utils.assertWithAllModes_ES6(
                "callThis []", constructCode(0, "Function.prototype.call.call(foo, ", 0, false));
        Utils.assertWithAllModes_ES6(
                "callThis []", constructCode(0, "Function.prototype.call.call(foo, ", 0, true));
    }

    @Test
    public void fooArgs0_callArgs1() {
        Utils.assertWithAllModes_ES6(
                "callThis [x]", constructCode(0, "Function.prototype.call.call(foo, ", 1, false));
        Utils.assertWithAllModes_ES6(
                "callThis [x]", constructCode(0, "Function.prototype.call.call(foo, ", 1, true));
    }

    @Test
    public void fooArgs0_callArgs2() {
        Utils.assertWithAllModes_ES6(
                "callThis [x,y]", constructCode(0, "Function.prototype.call.call(foo, ", 2, false));
        Utils.assertWithAllModes_ES6(
                "callThis [x,y]", constructCode(0, "Function.prototype.call.call(foo, ", 2, true));
    }

    @Test
    public void fooArgs1_callArgs0() {
        Utils.assertWithAllModes_ES6(
                "callThis undefined []",
                constructCode(1, "Function.prototype.call.call(foo, ", 0, false));
        Utils.assertWithAllModes_ES6(
                "callThis undefined []",
                constructCode(1, "Function.prototype.call.call(foo, ", 0, true));
    }

    @Test
    public void fooArgs1_callArgs1() {
        Utils.assertWithAllModes_ES6(
                "callThis x []", constructCode(1, "Function.prototype.call.call(foo, ", 1, false));
        Utils.assertWithAllModes_ES6(
                "callThis x []", constructCode(1, "Function.prototype.call.call(foo, ", 1, true));
    }

    @Test
    public void fooArgs1_callArgs2() {
        Utils.assertWithAllModes_ES6(
                "callThis x [y]", constructCode(1, "Function.prototype.call.call(foo, ", 2, false));
        Utils.assertWithAllModes_ES6(
                "callThis x [y]", constructCode(1, "Function.prototype.call.call(foo, ", 2, true));
    }

    @Test
    public void fooArgs2_callArgs0() {
        Utils.assertWithAllModes_ES6(
                "callThis undefined,undefined []",
                constructCode(2, "Function.prototype.call.call(foo, ", 0, false));
        Utils.assertWithAllModes_ES6(
                "callThis undefined,undefined []",
                constructCode(2, "Function.prototype.call.call(foo, ", 0, true));
    }

    @Test
    public void fooArgs2_callArgs1() {
        Utils.assertWithAllModes_ES6(
                "callThis x,undefined []",
                constructCode(2, "Function.prototype.call.call(foo, ", 1, false));
        Utils.assertWithAllModes_ES6(
                "callThis x,undefined []",
                constructCode(2, "Function.prototype.call.call(foo, ", 1, true));
    }

    @Test
    public void fooArgs2_callArgs2() {
        Utils.assertWithAllModes_ES6(
                "callThis x,y []",
                constructCode(2, "Function.prototype.call.call(foo, ", 2, false));
        Utils.assertWithAllModes_ES6(
                "callThis x,y []", constructCode(2, "Function.prototype.call.call(foo, ", 2, true));
    }

    @Test
    public void fooArgs0_applyArgs0() {
        Utils.assertWithAllModes_ES6(
                "callThis []", constructCode(0, "Function.prototype.call.apply(foo, ", 0, false));
        Utils.assertWithAllModes_ES6(
                "callThis []", constructCode(0, "Function.prototype.call.apply(foo, ", 0, true));
    }

    @Test
    public void fooArgs0_applyArgs1() {
        Utils.assertWithAllModes_ES6(
                "callThis [x]", constructCode(0, "Function.prototype.call.apply(foo, ", 1, false));
        Utils.assertWithAllModes_ES6(
                "callThis [x]", constructCode(0, "Function.prototype.call.apply(foo, ", 1, true));
    }

    @Test
    public void fooArgs0_applyArgs2() {
        Utils.assertWithAllModes_ES6(
                "callThis [x,y]",
                constructCode(0, "Function.prototype.call.apply(foo, ", 2, false));
        Utils.assertWithAllModes_ES6(
                "callThis [x,y]", constructCode(0, "Function.prototype.call.apply(foo, ", 2, true));
    }

    @Test
    public void fooArgs1_applyArgs0() {
        Utils.assertWithAllModes_ES6(
                "callThis undefined []",
                constructCode(1, "Function.prototype.call.apply(foo, ", 0, false));
        Utils.assertWithAllModes_ES6(
                "callThis undefined []",
                constructCode(1, "Function.prototype.call.apply(foo, ", 0, true));
    }

    @Test
    public void fooArgs1_applyArgs1() {
        Utils.assertWithAllModes_ES6(
                "callThis x []", constructCode(1, "Function.prototype.call.apply(foo, ", 1, false));
        Utils.assertWithAllModes_ES6(
                "callThis x []", constructCode(1, "Function.prototype.call.apply(foo, ", 1, true));
    }

    @Test
    public void fooArgs1_applyArgs2() {
        Utils.assertWithAllModes_ES6(
                "callThis x [y]",
                constructCode(1, "Function.prototype.call.apply(foo, ", 2, false));
        Utils.assertWithAllModes_ES6(
                "callThis x [y]", constructCode(1, "Function.prototype.call.apply(foo, ", 2, true));
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
        if (invokeFoo.contains("call.apply(")) {
            code.append('[');
        }
        code.append("'callThis'");
        if (invokeArgs == 1) {
            code.append(", 'x'");
        } else if (invokeArgs == 2) {
            code.append(", 'x', 'y'");
        }
        if (invokeFoo.contains("call.apply(")) {
            code.append(']');
        }
        code.append(");");

        return code.toString();
    }
}
