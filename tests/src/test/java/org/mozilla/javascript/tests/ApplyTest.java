package org.mozilla.javascript.tests.es6;

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
        Utils.assertWithAllModes_ES6("Hello!", code);
    }

    @Test
    public void applyToApplyCallsCorrectFunction() {
        String code =
                "Function.prototype.apply.apply(function(x) {return x;}, ['b', ['Hello!', 'Goodbye!']]);";

        Utils.assertWithAllModes_ES6("Hello!", code);
    }

    @Test
    public void applyToApplySetsCorrectFunctionThis() {
        // The `toString` call here converts the `NativeString` that
        // is the bound `this` to a simple `String` that we can
        // easily assert against.
        String code =
                "Function.prototype.apply.apply(function(x) {return this.toString();}, ['b', ['Hello!', 'Goodbye!']]);";

        Utils.assertWithAllModes_ES6("b", code);
    }
}
