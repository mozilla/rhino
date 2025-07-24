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

    @Test
    void declarationMixedWithAssignment() {
        Utils.assertWithAllModes_ES6("g", "var f = g = () => {}; f.name");
    }

    @Test
    void propertyFunction() {
        Utils.assertWithAllModes_ES6("x", "o = { x: function(){} }; o.x.name");
    }

    @Test
    void propertyArrow() {
        Utils.assertWithAllModes_ES6("x", "({x: () => {}}).x.name");
    }

    @Test
    void method() {
        Utils.assertWithAllModes_ES6("x", "({x() {}}).x.name");
    }

    @Test
    void methodNumericLiteral() {
        Utils.assertWithAllModes_ES6("1", "({1() {}})['1'].name");
    }

    @Test
    void methodBooleanLiteral() {
        Utils.assertWithAllModes_ES6("false", "({false() {}})['false'].name");
    }

    @Test
    void methodComputedProperty() {
        // TODO: this is not working at the moment, because it cannot be done statically
        //  but needs to be done at runtime
        Utils.assertWithAllModes_ES6("", "({ [1 + 2]() {}})['3'].name");
    }

    @Test
    void methodGenerators() {
        Utils.assertWithAllModes_ES6("x", "({*x() {}}).x.name");
    }

    @Test
    void getter() {
        Utils.assertWithAllModes_ES6(
                "get x",
                "var o = {get x(){}}; var desc = Object.getOwnPropertyDescriptor(o, \"x\"); desc.get.name");
    }

    @Test
    void setter() {
        Utils.assertWithAllModes_ES6(
                "set x",
                "var o = {set x(v){}}; var desc = Object.getOwnPropertyDescriptor(o, \"x\"); desc.set.name");
    }
}
