package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

class FunctionNameTest {
    @Test
    void varEqualsFunction() {
        Utils.assertWithAllModes_ES6("f", "var f = function() {}; f.name");
    }

    @Test
    void varEqualsArrowFunction() {
        Utils.assertWithAllModes_ES6("f", "var f = () => {}; f.name");
    }

    @Test
    void letEqualsFunction() {
        Utils.assertWithAllModes_ES6("f", "let f = function() {}; f.name");
    }

    @Test
    void letEqualsArrowFunction() {
        Utils.assertWithAllModes_ES6("f", "var f = () => {}; f.name");
    }

    @Test
    void constEqualsFunction() {
        Utils.assertWithAllModes_ES6("f", "const f = function() {}; f.name");
    }

    @Test
    void constEqualsArrowFunction() {
        Utils.assertWithAllModes_ES6("f", "var f = () => {}; f.name");
    }
}
