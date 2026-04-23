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
    public void defaultDerivedConstructorCallsSuper() {
        Utils.assertWithAllModes_ES6(
                "called",
                "var result = '';\n"
                        + "class Base {\n"
                        + "  constructor() { result = 'called'; }\n"
                        + "}\n"
                        + "class Child extends Base {}\n"
                        + "new Child();\n"
                        + "result\n");
    }

    @Test
    public void defaultDerivedConstructorForwardsArguments() {
        Utils.assertWithAllModes_ES6(
                "1-2-3",
                "class Base {\n"
                        + "  constructor(a, b, c) { this.x = a + '-' + b + '-' + c; }\n"
                        + "}\n"
                        + "class Child extends Base {}\n"
                        + "new Child(1, 2, 3).x\n");
    }

    @Test
    public void defaultDerivedConstructorInstanceOf() {
        Utils.assertWithAllModes_ES6(
                "true-true",
                "class Base {\n"
                        + "  constructor() {}\n"
                        + "}\n"
                        + "class Child extends Base {}\n"
                        + "var c = new Child();\n"
                        + "(c instanceof Child) + '-' + (c instanceof Base)\n");
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

    @Test
    public void fieldWithInitializer() {
        Utils.assertWithAllModes_ES6(42, "class Foo {\n" + "  x = 42;\n" + "}\n" + "new Foo().x\n");
    }

    @Test
    public void fieldWithoutInitializer() {
        Utils.assertWithAllModes_ES6(
                "undefined", "class Foo {\n" + "  x;\n" + "}\n" + "typeof new Foo().x\n");
    }

    @Test
    public void multipleFields() {
        Utils.assertWithAllModes_ES6(
                "1-2-3",
                "class Foo {\n"
                        + "  a = 1;\n"
                        + "  b = 2;\n"
                        + "  c = 3;\n"
                        + "}\n"
                        + "var f = new Foo();\n"
                        + "f.a + '-' + f.b + '-' + f.c\n");
    }

    @Test
    public void fieldBeforeConstructorCode() {
        Utils.assertWithAllModes_ES6(
                "10-20",
                "class Foo {\n"
                        + "  x = 10;\n"
                        + "  constructor() {\n"
                        + "    this.y = this.x * 2;\n"
                        + "  }\n"
                        + "}\n"
                        + "var f = new Foo();\n"
                        + "f.x + '-' + f.y\n");
    }

    @Test
    public void fieldWithConstructorArgs() {
        Utils.assertWithAllModes_ES6(
                "default-custom",
                "class Foo {\n"
                        + "  x = 'default';\n"
                        + "  constructor(y) { this.y = y; }\n"
                        + "}\n"
                        + "var f = new Foo('custom');\n"
                        + "f.x + '-' + f.y\n");
    }

    @Test
    public void fieldWithExpressionInitializer() {
        Utils.assertWithAllModes_ES6(
                15, "class Foo {\n" + "  x = 10 + 5;\n" + "}\n" + "new Foo().x\n");
    }

    @Test
    public void fieldsAndMethods() {
        Utils.assertWithAllModes_ES6(
                "42-hello",
                "class Foo {\n"
                        + "  x = 42;\n"
                        + "  greet() { return 'hello'; }\n"
                        + "}\n"
                        + "var f = new Foo();\n"
                        + "f.x + '-' + f.greet()\n");
    }

    @Test
    public void fieldInDerivedClassAfterSuper() {
        Utils.assertWithAllModes_ES6(
                "base-derived",
                "class Base {\n"
                        + "  constructor() { this.x = 'base'; }\n"
                        + "}\n"
                        + "class Child extends Base {\n"
                        + "  y = 'derived';\n"
                        + "  constructor() { super(); }\n"
                        + "}\n"
                        + "var c = new Child();\n"
                        + "c.x + '-' + c.y\n");
    }

    @Test
    public void fieldPerInstance() {
        Utils.assertWithAllModes_ES6(
                "1-2",
                "class Foo {\n"
                        + "  x = 0;\n"
                        + "}\n"
                        + "var a = new Foo(); a.x = 1;\n"
                        + "var b = new Foo(); b.x = 2;\n"
                        + "a.x + '-' + b.x\n");
    }

    @Test
    public void staticPrototypeMethodError() {
        Utils.assertEvaluatorExceptionES6(
                "Unexpected token", "class Foo { static prototype() {} }");
    }

    @Test
    public void staticPrototypeFieldError() {
        Utils.assertEvaluatorExceptionES6(
                "Unexpected token", "class Foo { static prototype = 1; }");
    }

    @Test
    public void instanceConstructorMethodError() {
        Utils.assertEvaluatorExceptionES6(
                "Unexpected token", "class Foo { async constructor() {} }");
    }

    @Test
    public void instanceConstructorFieldError() {
        Utils.assertEvaluatorExceptionES6("Unexpected token", "class Foo { constructor = 1; }");
    }

    @Test
    public void staticConstructorMethodOk() {
        Utils.assertWithAllModes_ES6(
                42, "class Foo { static constructor() { return 42; } } Foo.constructor()");
    }

    @Test
    public void staticConstructorFieldError() {
        Utils.assertEvaluatorExceptionES6(
                "Unexpected token", "class Foo { static constructor; }");
    }

    @Test
    public void staticConstructorFieldWithInitializerError() {
        Utils.assertEvaluatorExceptionES6(
                "Unexpected token", "class Foo { static constructor = 1; }");
    }

    @Test
    public void staticStringConstructorFieldError() {
        Utils.assertEvaluatorExceptionES6(
                "Unexpected token", "class Foo { static 'constructor'; }");
    }

    @Test
    public void staticComputedConstructorFieldOk() {
        Utils.assertWithAllModes_ES6(
                42, "class Foo { static ['constructor'] = 42; } Foo['constructor']");
    }

    @Test
    public void staticPrototypeAsyncMethodError() {
        Utils.assertEvaluatorExceptionES6(
                "Unexpected token", "class Foo { static async prototype() {} }");
    }

    @Test
    public void stringPropertyField() {
        Utils.assertWithAllModes_ES6(1, "class Foo { 'foo' = 1; } new Foo()['foo']");
    }

    @Test
    public void numericPropertyField() {
        Utils.assertWithAllModes_ES6(2, "class Foo { 0 = 2; } new Foo()[0]");
    }

    @Test
    public void computedPropertyField() {
        Utils.assertWithAllModes_ES6(3, "class Foo { [1 + 2] = 3; } new Foo()[3]");
    }

    @Test
    public void stringPropertyMethod() {
        Utils.assertWithAllModes_ES6(
                "hi", "class Foo { 'greet'() { return 'hi'; } } new Foo().greet()");
    }

    @Test
    public void numericPropertyMethod() {
        Utils.assertWithAllModes_ES6(42, "class Foo { 0() { return 42; } } new Foo()[0]()");
    }

    @Test
    public void mixedPropertyNames() {
        Utils.assertWithAllModes_ES6(
                "1-2-3",
                "class Foo {\n"
                        + "  'a' = 1;\n"
                        + "  0 = 2;\n"
                        + "  [1 + 2] = 3;\n"
                        + "}\n"
                        + "var f = new Foo();\n"
                        + "f.a + '-' + f[0] + '-' + f[3]\n");
    }

    @Test
    public void staticStringMethod() {
        Utils.assertWithAllModes_ES6(
                "ok", "class Foo { static 'bar'() { return 'ok'; } } Foo.bar()");
    }

    @Test
    public void staticField() {
        Utils.assertWithAllModes_ES6(42, "class Foo { static x = 42; } Foo.x");
    }

    @Test
    public void staticFieldExpression() {
        Utils.assertWithAllModes_ES6(15, "class Foo { static x = 10 + 5; } Foo.x");
    }

    @Test
    public void staticFieldNoInitializer() {
        Utils.assertWithAllModes_ES6("undefined", "class Foo { static x; } typeof Foo.x");
    }

    @Test
    public void multipleStaticFields() {
        Utils.assertWithAllModes_ES6(
                "1-2-3",
                "class Foo {\n"
                        + "  static a = 1;\n"
                        + "  static b = 2;\n"
                        + "  static c = 3;\n"
                        + "}\n"
                        + "Foo.a + '-' + Foo.b + '-' + Foo.c\n");
    }

    @Test
    public void staticAndInstanceFields() {
        Utils.assertWithAllModes_ES6(
                "static-instance",
                "class Foo {\n"
                        + "  static s = 'static';\n"
                        + "  i = 'instance';\n"
                        + "}\n"
                        + "Foo.s + '-' + new Foo().i\n");
    }

    @Test
    public void staticFieldNotOnInstance() {
        Utils.assertWithAllModes_ES6("undefined", "class Foo { static x = 1; } typeof new Foo().x");
    }

    @Test
    public void staticComputedField() {
        Utils.assertWithAllModes_ES6(99, "class Foo { static [1 + 2] = 99; } Foo[3]");
    }

    @Test
    public void staticFieldNameWithBraceUnicodeEscape() {
        Utils.assertWithAllModes_ES6(1, "class A { static \\u{64} = 1; } A.d");
    }

    @Test
    public void staticFieldNameWith4DigitUnicodeEscape() {
        Utils.assertWithAllModes_ES6(1, "class A { static \\u0064 = 1; } A.d");
    }

    @Test
    public void instanceFieldNameWithBraceUnicodeEscape() {
        Utils.assertWithAllModes_ES6(1, "class A { \\u{64} = 1; } new A().d");
    }

    @Test
    public void methodNameWithBraceUnicodeEscape() {
        Utils.assertWithAllModes_ES6(1, "class A { \\u{64}oor() { return 1; } } new A().door()");
    }

    @Test
    public void staticMethodNameWithBraceUnicodeEscape() {
        Utils.assertWithAllModes_ES6(1, "class A { static \\u{64}oor() { return 1; } } A.door()");
    }

    @Test
    public void staticStringField() {
        Utils.assertWithAllModes_ES6(7, "class Foo { static 'x' = 7; } Foo.x");
    }

    @Test
    public void generatorMethod() {
        Utils.assertWithAllModes_ES6(
                "1,2,3",
                "class Foo {\n"
                        + "  *gen() { yield 1; yield 2; yield 3; }\n"
                        + "}\n"
                        + "Array.from(new Foo().gen()).join(',')\n");
    }

    @Test
    public void staticGeneratorMethod() {
        Utils.assertWithAllModes_ES6(
                "a,b",
                "class Foo {\n"
                        + "  static *gen() { yield 'a'; yield 'b'; }\n"
                        + "}\n"
                        + "Array.from(Foo.gen()).join(',')\n");
    }
}
