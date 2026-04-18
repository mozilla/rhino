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
}
