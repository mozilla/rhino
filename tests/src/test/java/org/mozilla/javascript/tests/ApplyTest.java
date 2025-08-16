package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class ApplyTest {

    @Test
    public void applyThisFromBoundArgs() {
        // The `toString` call here converts the `NativeString` that
        // is the bound `this` to a simple `String` that we can
        // easily assert against.
        String code =
                "var f = function(x) { return this.toString(); };\n"
                        + "var a = f.apply;\n"
                        + "var b = a.bind(f, 'Hello!');\n"
                        + "b([1,2]);\n";
        Utils.assertWithAllModes("Hello!", code);
    }

    @Test
    public void applyToApplyCallsCorrectFunction() {
        String code =
                "Function.prototype.apply.apply(function(x) {return x;}, ['b', ['Hello!', 'Goodbye!']]);";

        Utils.assertWithAllModes("Hello!", code);
    }

    @Test
    public void applyToApplySetsCorrectFunctionThis() {
        // The `toString` call here converts the `NativeString` that
        // is the bound `this` to a simple `String` that we can
        // easily assert against.
        String code =
                "Function.prototype.apply.apply(function(x) {return this.toString();}, ['b', ['Hello!', 'Goodbye!']]);";

        Utils.assertWithAllModes("b", code);
    }

    @Test
    public void applyToCallCallsCorrectFunction() throws Exception {
        String script =
                "function foo(x) {return x;};\n" + "foo.call.apply(foo, ['b', 'Hello!']);\n";

        Utils.assertWithAllModes("Hello!", script);
    }

    @Test
    public void applyToCallSetsCorrectFunctionThis() throws Exception {
        String script =
                "function foo(x) {return this.toString();};\n"
                        + "foo.call.apply(foo, ['b', 'Hello!']);\n";

        Utils.assertWithAllModes("b", script);
    }
}
