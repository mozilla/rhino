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
                "Bar", "var Foo = class Bar {\n" + "  constructor() {}\n" + "};\n" + "Foo.name\n");
    }

    @Test
    public void classInstanceof() {
        Utils.assertWithAllModes_ES6(
                Boolean.TRUE,
                "class Foo {\n" + "  constructor() {}\n" + "}\n" + "new Foo() instanceof Foo\n");
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
        Utils.assertWithAllModes_ES6(Boolean.TRUE, "class Foo {}\n" + "new Foo() instanceof Foo\n");
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
    public void classToString() {
        Utils.assertWithAllModes_ES6(
                "class Foo {\n  constructor() {}\n}",
                "class Foo {\n" + "  constructor() {}\n" + "}\n" + "Foo.toString()\n");
    }

    @Test
    public void classToStringWithBody() {
        Utils.assertWithAllModes_ES6(
                "class Foo {\n  constructor(x) { this.x = x; }\n}",
                "class Foo {\n"
                        + "  constructor(x) { this.x = x; }\n"
                        + "}\n"
                        + "Foo.toString()\n");
    }

    @Test
    public void classExpressionToString() {
        Utils.assertWithAllModes_ES6(
                "class Foo {\n  constructor() {}\n}",
                "var Foo = class Foo {\n" + "  constructor() {}\n" + "};\n" + "Foo.toString()\n");
    }

    @Test
    public void defaultConstructorToString() {
        Utils.assertWithAllModes_ES6("class Foo {}", "class Foo {}\n" + "Foo.toString()\n");
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

    @Test
    public void instanceMethodBasic() {
        Utils.assertWithAllModes_ES6(
                "hello",
                "class Foo {\n"
                        + "  greet() { return 'hello'; }\n"
                        + "}\n"
                        + "new Foo().greet()\n");
    }

    @Test
    public void instanceMethodMultiple() {
        Utils.assertWithAllModes_ES6(
                "3-6",
                "class Calc {\n"
                        + "  add(a, b) { return a + b; }\n"
                        + "  mul(a, b) { return a * b; }\n"
                        + "}\n"
                        + "var c = new Calc();\n"
                        + "c.add(1, 2) + '-' + c.mul(2, 3)\n");
    }

    @Test
    public void instanceMethodAccessThis() {
        Utils.assertWithAllModes_ES6(
                "hello world",
                "class Greeter {\n"
                        + "  constructor(name) { this.name = name; }\n"
                        + "  greet() { return 'hello ' + this.name; }\n"
                        + "}\n"
                        + "new Greeter('world').greet()\n");
    }

    @Test
    public void instanceMethodOnPrototype() {
        Utils.assertWithAllModes_ES6(
                Boolean.TRUE,
                "class Foo {\n" + "  bar() {}\n" + "}\n" + "Foo.prototype.hasOwnProperty('bar')\n");
    }

    @Test
    public void instanceMethodNonEnumerable() {
        Utils.assertWithAllModes_ES6(
                0,
                "class Foo {\n"
                        + "  bar() {}\n"
                        + "  baz() {}\n"
                        + "}\n"
                        + "Object.keys(Foo.prototype).length\n");
    }

    @Test
    public void instanceMethodInherited() {
        Utils.assertWithAllModes_ES6(
                "hello",
                "class Base {\n"
                        + "  greet() { return 'hello'; }\n"
                        + "}\n"
                        + "class Child extends Base {\n"
                        + "  constructor() { super(); }\n"
                        + "}\n"
                        + "new Child().greet()\n");
    }

    @Test
    public void instanceMethodOverride() {
        Utils.assertWithAllModes_ES6(
                "child",
                "class Base {\n"
                        + "  who() { return 'base'; }\n"
                        + "}\n"
                        + "class Child extends Base {\n"
                        + "  constructor() { super(); }\n"
                        + "  who() { return 'child'; }\n"
                        + "}\n"
                        + "new Child().who()\n");
    }

    @Test
    public void classExpressionWithMethod() {
        Utils.assertWithAllModes_ES6(
                42,
                "var Foo = class {\n"
                        + "  getValue() { return 42; }\n"
                        + "};\n"
                        + "new Foo().getValue()\n");
    }

    @Test
    public void defaultConstructorWithMethods() {
        Utils.assertWithAllModes_ES6(
                "ok",
                "class Foo {\n" + "  check() { return 'ok'; }\n" + "}\n" + "new Foo().check()\n");
    }

    @Test
    public void staticMethodBasic() {
        Utils.assertWithAllModes_ES6(
                42,
                "class Foo {\n" + "  static create() { return 42; }\n" + "}\n" + "Foo.create()\n");
    }

    @Test
    public void staticMethodMultiple() {
        Utils.assertWithAllModes_ES6(
                "a-b",
                "class Foo {\n"
                        + "  static a() { return 'a'; }\n"
                        + "  static b() { return 'b'; }\n"
                        + "}\n"
                        + "Foo.a() + '-' + Foo.b()\n");
    }

    @Test
    public void staticMethodNotOnInstance() {
        Utils.assertWithAllModes_ES6(
                "undefined",
                "class Foo {\n"
                        + "  static bar() { return 1; }\n"
                        + "}\n"
                        + "typeof new Foo().bar\n");
    }

    @Test
    public void staticMethodNonEnumerable() {
        Utils.assertWithAllModes_ES6(
                0, "class Foo {\n" + "  static bar() {}\n" + "}\n" + "Object.keys(Foo).length\n");
    }

    @Test
    public void staticAndInstanceMethods() {
        Utils.assertWithAllModes_ES6(
                "static-instance",
                "class Foo {\n"
                        + "  static s() { return 'static'; }\n"
                        + "  i() { return 'instance'; }\n"
                        + "}\n"
                        + "Foo.s() + '-' + new Foo().i()\n");
    }

    @Test
    public void staticMethodInherited() {
        Utils.assertWithAllModes_ES6(
                "hello",
                "class Base {\n"
                        + "  static greet() { return 'hello'; }\n"
                        + "}\n"
                        + "class Child extends Base {\n"
                        + "  constructor() { super(); }\n"
                        + "}\n"
                        + "Child.greet()\n");
    }

    @Test
    public void staticMethodOnClassExpression() {
        Utils.assertWithAllModes_ES6(
                "ok",
                "var Foo = class {\n"
                        + "  static check() { return 'ok'; }\n"
                        + "};\n"
                        + "Foo.check()\n");
    }
}
