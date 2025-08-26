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
        Utils.assertWithAllModes_ES6("x", "o = { x: () => {} }; o.x.name");
    }

    @Test
    void method() {
        Utils.assertWithAllModes_ES6("x", "o = { x() {} }; o.x.name");
    }

    @Test
    void methodNumericLiteral() {
        Utils.assertWithAllModes_ES6("1", "o = { 1() {} }; o['1'].name");
    }

    @Test
    void methodBooleanLiteral() {
        Utils.assertWithAllModes_ES6("false", "o = { false() {} }; o['false'].name");
    }

    @Test
    void methodComputedProperty() {
        Utils.assertWithAllModes_ES6("3", "o = { [1 + 2]() {} }; o['3'].name");
    }

    @Test
    void methodComputedPropertyNamedSymbol() {
        Utils.assertWithAllModes_ES6("[foo]", "s = Symbol('foo'); o = { [s]() {} }; o[s].name");
    }

    @Test
    void methodComputedPropertyAnonymousSymbol() {
        Utils.assertWithAllModes_ES6("", "s = Symbol(); o = { [s]() {} }; o[s].name");
    }

    @Test
    void methodComputedPropertyBuiltInSymbol() {
        Utils.assertWithAllModes_ES6(
                "[Symbol.iterator]", "s = Symbol.iterator; o = { [s]() {} }; o[s].name");
    }

    @Test
    void methodGenerators() {
        Utils.assertWithAllModes_ES6("x", "o = { *x() {} }; o.x.name");
    }

    @Test
    void getter() {
        Utils.assertWithAllModes_ES6(
                "get x",
                "var o = { get x(){} }; var desc = Object.getOwnPropertyDescriptor(o, \"x\"); desc.get.name");
    }

    @Test
    void getterComputedName() {
        Utils.assertWithAllModes_ES6(
                "get [foo]",
                "var s = Symbol('foo'); var o = { get [s](){} }; var desc = Object.getOwnPropertyDescriptor(o, s); desc.get.name");
    }

    @Test
    void getterComputedNameAnonymousSymbol() {
        Utils.assertWithAllModes_ES6(
                "get ",
                "var s = Symbol(); var o = { get [s](){} }; var desc = Object.getOwnPropertyDescriptor(o, s); desc.get.name");
    }

    @Test
    void setter() {
        Utils.assertWithAllModes_ES6(
                "set x",
                "var o = { set x(v){} }; var desc = Object.getOwnPropertyDescriptor(o, \"x\"); desc.set.name");
    }

    @Test
    void setterComputedName() {
        Utils.assertWithAllModes_ES6(
                "set [foo]",
                "var s = Symbol('foo'); var o = { set [s](v){} }; var desc = Object.getOwnPropertyDescriptor(o, s); desc.set.name");
    }

    @Test
    void setterComputedNameAnonymousSymbol() {
        Utils.assertWithAllModes_ES6(
                "set ",
                "var s = Symbol(); var o = { set [s](v){} }; var desc = Object.getOwnPropertyDescriptor(o, s); desc.set.name");
    }

    @Test
    void arrowInObject() {
        Utils.assertWithAllModes_ES6(
                "id",
                "var o = { id: () => {} }; var desc = Object.getOwnPropertyDescriptor(o, 'id'); desc.value.name");
    }

    @Test
    void computedNameArrow() {
        Utils.assertWithAllModes_ES6(
                "Id",
                "var id = 'Id'; var o = { [id]: () => {} }; var desc = Object.getOwnPropertyDescriptor(o, 'Id'); desc.value.name");
    }

    @Test
    void computedNameArrowSymbol() {
        Utils.assertWithAllModes_ES6(
                "[foo]",
                "var s = Symbol('foo'); var o = { [s]: () => {} }; var desc = Object.getOwnPropertyDescriptor(o, s); desc.value.name");
    }

    @Test
    void computedNameArrowAnonymousSymbol() {
        Utils.assertWithAllModes_ES6(
                "",
                "var s = Symbol(); var o = { [s]: () => {} }; var desc = Object.getOwnPropertyDescriptor(o, s); desc.value.name");
    }

    @Test
    void protoIsNotASpecialNameForMethods() {
        Utils.assertWithAllModes_ES6("__proto__", "var o = { __proto__() {} }; o.__proto__.name");
    }

    @Test
    void protoIsASpecialNameForNormalPropFunctionValue() {
        Utils.assertWithAllModes_ES6("", "var o = { __proto__: function() {} }; o.__proto__.name");
    }

    @Test
    void inferenceIsNotUsedInEs5() {
        Utils.assertWithAllModes_1_8("", "var f = function() {}; f.name");
    }
}
