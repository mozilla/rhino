/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for ES2022 private class fields. */
public class PrivateFieldsTest {

    @Test
    public void declareAndReadFromMethod() {
        Utils.assertWithAllModes_ES6(
                7,
                "class Foo {\n"
                        + "  #x = 7;\n"
                        + "  get() { return this.#x; }\n"
                        + "}\n"
                        + "new Foo().get()\n");
    }

    @Test
    public void defaultUndefinedInitializer() {
        Utils.assertWithAllModes_ES6(
                Boolean.TRUE,
                "class Foo {\n"
                        + "  #x;\n"
                        + "  isUndef() { return this.#x === undefined; }\n"
                        + "}\n"
                        + "new Foo().isUndef()\n");
    }

    @Test
    public void writeFromMethod() {
        Utils.assertWithAllModes_ES6(
                42,
                "class Foo {\n"
                        + "  #x = 1;\n"
                        + "  set(v) { this.#x = v; }\n"
                        + "  get() { return this.#x; }\n"
                        + "}\n"
                        + "var f = new Foo(); f.set(42); f.get()\n");
    }

    @Test
    public void multipleFields() {
        Utils.assertWithAllModes_ES6(
                "1-2",
                "class Foo {\n"
                        + "  #a = 1;\n"
                        + "  #b = 2;\n"
                        + "  render() { return this.#a + '-' + this.#b; }\n"
                        + "}\n"
                        + "new Foo().render()\n");
    }

    @Test
    public void initializerRunsBeforeConstructorBody() {
        // Field initializers must run before the constructor body per ES spec, so the
        // constructor can overwrite them.
        Utils.assertWithAllModes_ES6(
                99,
                "class Foo {\n"
                        + "  #x = 1;\n"
                        + "  constructor(v) { this.#x = v; }\n"
                        + "  get() { return this.#x; }\n"
                        + "}\n"
                        + "new Foo(99).get()\n");
    }

    @Test
    public void privateNameNotInOwnPropertyNames() {
        Utils.assertWithAllModes_ES6(
                "",
                "class Foo { #x = 1; }\n"
                        + "var keys = Object.getOwnPropertyNames(new Foo());\n"
                        + "keys.join(',')\n");
    }

    @Test
    public void privateMethodCallable() {
        Utils.assertWithAllModes_ES6(
                "baz",
                "class B {\n"
                        + "  foo() { return this.#bar(); }\n"
                        + "  #bar() { return 'baz'; }\n"
                        + "}\n"
                        + "new B().foo()\n");
    }

    @Test
    public void privateMethodAccessThis() {
        Utils.assertWithAllModes_ES6(
                3,
                "class C {\n"
                        + "  #x = 3;\n"
                        + "  getVal() { return this.#get(); }\n"
                        + "  #get() { return this.#x; }\n"
                        + "}\n"
                        + "new C().getVal()\n");
    }

    @Test
    public void privateNameNotInOwnPropertySymbols() {
        Utils.assertWithAllModes_ES6(
                0, "class Foo { #x = 1; }\n" + "Object.getOwnPropertySymbols(new Foo()).length\n");
    }

    @Test
    public void distinctClassesHaveDistinctSymbols() {
        // Two classes both declare #x — instances of one shouldn't expose the
        // other's field even if it leaks somehow. Since each SymbolKey is
        // identity-scoped to its class, there's no way for one class's methods
        // to read the other's field.
        Utils.assertWithAllModes_ES6(
                "1-2",
                "class A { #x = 1; getA() { return this.#x; } }\n"
                        + "class B { #x = 2; getB() { return this.#x; } }\n"
                        + "var a = new A(), b = new B();\n"
                        + "a.getA() + '-' + b.getB()\n");
    }

    @Test
    public void duplicatePrivateFieldIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('class F { #x; #x; }'); 'no error'; }\n" + "catch (e) { e.name; }\n");
    }

    @Test
    public void duplicatePrivateMethodIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('class F { #m() {} #m() {} }'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void duplicatePrivateFieldAndMethodIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('class F { #x; #x() {} }'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void deletePrivateFieldIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('class C { #x; del() { delete this.#x; } }'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void deletePrivateMethodIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('class C { #m() {} del() { delete this.#m; } }'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void deleteParenthesizedPrivateFieldIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('class C { #x; del() { delete (this.#x); } }'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void staticPrivateFieldReadWrite() {
        Utils.assertWithAllModes_ES6(
                "bob",
                "class C {\n"
                        + "  static #f;\n"
                        + "  getF() { return C.#f; }\n"
                        + "  setF(f) { C.#f = f; }\n"
                        + "}\n"
                        + "var c = new C();\n"
                        + "c.setF('bob');\n"
                        + "c.getF()\n");
    }

    @Test
    public void staticPrivateFieldWithInitializer() {
        Utils.assertWithAllModes_ES6(
                42,
                "class C {\n"
                        + "  static #f = 42;\n"
                        + "  static getF() { return C.#f; }\n"
                        + "}\n"
                        + "C.getF()\n");
    }

    @Test
    public void staticPrivateFieldHiddenFromGetOwnPropertySymbols() {
        Utils.assertWithAllModes_ES6(
                0, "class C { static #f = 1; }\n" + "Object.getOwnPropertySymbols(C).length\n");
    }

    @Test
    public void staticPrivateMethodCallable() {
        Utils.assertWithAllModes_ES6(
                42,
                "class C {\n"
                        + "  static #helper(x) { return x * 2; }\n"
                        + "  static compute(x) { return C.#helper(x); }\n"
                        + "}\n"
                        + "C.compute(21)\n");
    }

    @Test
    public void staticPrivateMethodCanReadStaticPrivateField() {
        Utils.assertWithAllModes_ES6(
                10,
                "class C {\n"
                        + "  static #f = 7;\n"
                        + "  static #add(x) { return x + C.#f; }\n"
                        + "  static run(x) { return C.#add(x); }\n"
                        + "}\n"
                        + "C.run(3)\n");
    }

    @Test
    public void staticPrivateMethodHiddenFromGetOwnPropertySymbols() {
        Utils.assertWithAllModes_ES6(
                0, "class C { static #m() {} }\n" + "Object.getOwnPropertySymbols(C).length\n");
    }

    @Test
    public void duplicateStaticPrivateMethodIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('class C { static #m() {} static #m() {} }'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void duplicateStaticPrivateFieldAndMethodIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('class C { static #x; static #x() {} }'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void privateNameWithUnicodeEscape4Digit() {
        Utils.assertWithAllModes_ES6(
                7,
                "class C {\n"
                        + "  #\\u0064oor = 7;\n"
                        + "  get() { return this.#door; }\n"
                        + "}\n"
                        + "new C().get()\n");
    }

    @Test
    public void privateNameWithUnicodeEscapeBraced() {
        Utils.assertWithAllModes_ES6(
                7,
                "class C {\n"
                        + "  #\\u{64}oor = 7;\n"
                        + "  get() { return this.#door; }\n"
                        + "}\n"
                        + "new C().get()\n");
    }

    @Test
    public void privateNameReferencedWithUnicodeEscape() {
        Utils.assertWithAllModes_ES6(
                7,
                "class C {\n"
                        + "  #door = 7;\n"
                        + "  get() { return this.#\\u{64}oor; }\n"
                        + "}\n"
                        + "new C().get()\n");
    }

    @Test
    public void privateMethodWithUnicodeEscape() {
        Utils.assertWithAllModes_ES6(
                "hi",
                "class C {\n"
                        + "  #\\u{67}reet() { return 'hi'; }\n"
                        + "  run() { return this.#greet(); }\n"
                        + "}\n"
                        + "new C().run()\n");
    }

    @Test
    public void privateNameInvalidEscapeCodePointIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('class C { #\\\\u{110000} = 1; }'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void privateNameEscapeDecodingToNonIdentifierIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('class C { #a\\\\u0020b = 1; }'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void duplicateStaticAndInstancePrivateFieldIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('class C { #x; static #x; }'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void superPrivateNameAccessIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('class C extends Object { #m() {} test() { return super.#m; } }'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void superPrivateMethodCallIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('class C extends Object { #m() {} test() { return super.#m(); } }'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void superCallInMethodIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('class C extends Object { foo() { super(); } }'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void superCallInConstructorIsValid() {
        // Positive case: super() is valid inside a derived class constructor.
        Utils.assertWithAllModes_ES6(
                "ok",
                "class Base { constructor() { this.tag = 'ok'; } }\n"
                        + "class D extends Base { constructor() { super(); } }\n"
                        + "new D().tag\n");
    }

    @Test
    public void superCallInArrowInsideConstructorParses() {
        // An arrow function inside a constructor inherits super semantics, so the
        // call should parse without a syntax error.
        Utils.assertWithAllModes_ES6(
                "no error",
                "try { eval('class Base {} class D extends Base { constructor() { (() => super())(); } }'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void awaitLabelInPrivateAsyncGeneratorIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('class C { async *#gen() { await: ; } }'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void undeclaredPrivateNameIsSyntaxError() {
        // Accessing a private name that isn't declared in any enclosing class is a
        // syntax error (we report it at IR time). Any evaluation should fail.
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('class F { get() { return this.#missing; } }'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void privateNameInObjectLiteralIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('var o = { #m: 1 };'); 'no error'; }\n" + "catch (e) { e.name; }\n");
    }

    @Test
    public void asyncGeneratorPrivateNameInObjectLiteralIsSyntaxError() {
        // Regression: this form used to NPE rather than reporting a clean syntax error.
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('var o = { async * #m() {} };'); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void generatorPrivateNameInObjectLiteralIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError",
                "try { eval('var o = { *#m() {} };'); 'no error'; }\n" + "catch (e) { e.name; }\n");
    }

    @Test
    public void readPrivateFieldFromForeignObjectThrowsTypeError() {
        Utils.assertWithAllModes_ES6(
                "TypeError",
                "class C { #x = 1; read(o) { return o.#x; } }\n"
                        + "try { new C().read({}); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void readPrivateMethodFromForeignObjectThrowsTypeError() {
        Utils.assertWithAllModes_ES6(
                "TypeError",
                "class C { #m() { return 1; } run(o) { return o.#m(); } }\n"
                        + "try { new C().run({}); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void writePrivateFieldToForeignObjectThrowsTypeError() {
        Utils.assertWithAllModes_ES6(
                "TypeError",
                "class C { #x = 1; write(o) { o.#x = 2; } }\n"
                        + "try { new C().write({}); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void compoundAssignPrivateFieldToForeignObjectThrowsTypeError() {
        Utils.assertWithAllModes_ES6(
                "TypeError",
                "class C { #x = 1; bump(o) { o.#x += 1; } }\n"
                        + "try { new C().bump({}); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void readPrivateFieldFromSiblingClassInstanceThrowsTypeError() {
        // Two classes declare #x, but their SymbolKeys are distinct identities, so
        // an A method reading b.#x should fail even though B has an own #x slot.
        Utils.assertWithAllModes_ES6(
                "TypeError",
                "class A { #x = 1; read(o) { return o.#x; } }\n"
                        + "class B { #x = 2; }\n"
                        + "try { new A().read(new B()); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }

    @Test
    public void staticPrivateFieldReadFromForeignObjectThrowsTypeError() {
        Utils.assertWithAllModes_ES6(
                "TypeError",
                "class C { static #f = 1; static read(o) { return o.#f; } }\n"
                        + "try { C.read({}); 'no error'; }\n"
                        + "catch (e) { e.name; }\n");
    }
}
