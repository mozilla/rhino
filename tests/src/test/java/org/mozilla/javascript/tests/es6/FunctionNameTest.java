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
        Utils.assertWithAllModes_ES6("f", "let f = () => {}; f.name");
    }

    @Test
    void constEqualsFunction() {
        Utils.assertWithAllModes_ES6("f", "const f = function() {}; f.name");
    }

    @Test
    void constEqualsArrowFunction() {
        Utils.assertWithAllModes_ES6("f", "const f = () => {}; f.name");
    }

    @Test
    void nonAnonymousFunctionsDontGetOverriddenInDeclaration() {
        Utils.assertWithAllModes_ES6("g", "var f = function g() {}; f.name");
    }

    @Test
    void assignmentVarFunction() {
        Utils.assertWithAllModes_ES6("f", "var f; f = function(){}; f.name");
    }

    @Test
    void assignmentVarArrow() {
        Utils.assertWithAllModes_ES6("f", "var f; f = () => {}; f.name");
    }

    @Test
    void assignmentLetFunction() {
        Utils.assertWithAllModes_ES6("f", "let f; f = function(){}; f.name");
    }

    @Test
    void assignmentLetArrow() {
        Utils.assertWithAllModes_ES6("f", "let f; f = () => {}; f.name");
    }

    @Test
    void assignmentShouldNotInferIfParenthesized() {
        Utils.assertWithAllModes_ES6("", "var f; (f) = function(){}; f.name");
    }

    @Test
    void nonAnonymousFunctionsDontGetOverriddenInAssignment() {
        Utils.assertWithAllModes_ES6("g", "var f; f = function g() {}; f.name");
    }

    @Test
    void reassignDoesntOverride() {
        Utils.assertWithAllModes_ES6("f", "var f; f = function() {}; g = f; g.name");
    }
}
