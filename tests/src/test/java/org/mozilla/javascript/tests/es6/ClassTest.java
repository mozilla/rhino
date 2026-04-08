/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for basic ES6 class support. */
public class ClassTest {

    @Test
    public void basicClassDeclaration() {
        Utils.assertWithAllModes_ES6(
                42,
                "class Foo {\n"
                        + "  constructor(x) { this.x = x; }\n"
                        + "}\n"
                        + "var f = new Foo(42);\n"
                        + "f.x\n");
    }

    @Test
    public void classExpression() {
        Utils.assertWithAllModes_ES6(
                "hello",
                "var Foo = class {\n"
                        + "  constructor(msg) { this.msg = msg; }\n"
                        + "};\n"
                        + "new Foo('hello').msg\n");
    }

    @Test
    public void namedClassExpression() {
        Utils.assertWithAllModes_ES6(
                "Bar",
                "var Foo = class Bar {\n"
                        + "  constructor() {}\n"
                        + "};\n"
                        + "Foo.name\n");
    }

    @Test
    public void classInstanceof() {
        Utils.assertWithAllModes_ES6(
                Boolean.TRUE,
                "class Foo {\n"
                        + "  constructor() {}\n"
                        + "}\n"
                        + "new Foo() instanceof Foo\n");
    }

    @Test
    public void classWithoutNewThrows() {
        Utils.assertWithAllModes_ES6(
                "TypeError",
                "class Foo {\n"
                        + "  constructor() {}\n"
                        + "}\n"
                        + "try { Foo(); 'no error'; } catch(e) { e.name; }\n");
    }

    @Test
    public void defaultConstructor() {
        Utils.assertWithAllModes_ES6(
                Boolean.TRUE,
                "class Foo {}\n" + "new Foo() instanceof Foo\n");
    }

    @Test
    public void classExtendsBasic() {
        Utils.assertWithAllModes_ES6(
                "1-2",
                "class Base {\n"
                        + "  constructor(x) { this.x = x; }\n"
                        + "}\n"
                        + "class Child extends Base {\n"
                        + "  constructor(x, y) {\n"
                        + "    super(x);\n"
                        + "    this.y = y;\n"
                        + "  }\n"
                        + "}\n"
                        + "var c = new Child(1, 2);\n"
                        + "c.x + '-' + c.y\n");
    }

    @Test
    public void classExtendsInstanceof() {
        Utils.assertWithAllModes_ES6(
                "true-true",
                "class Base {\n"
                        + "  constructor() {}\n"
                        + "}\n"
                        + "class Child extends Base {\n"
                        + "  constructor() { super(); }\n"
                        + "}\n"
                        + "var c = new Child();\n"
                        + "(c instanceof Child) + '-' + (c instanceof Base)\n");
    }

    @Test
    public void classStrictMode() {
        Utils.assertWithAllModes_ES6(
                "TypeError",
                "class Foo {\n"
                        + "  constructor() {\n"
                        + "    try { arguments.callee; 'no error'; } catch(e) { this.result = e.name; }\n"
                        + "  }\n"
                        + "}\n"
                        + "new Foo().result\n");
    }
}
