/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class BoundFunctionTest {

    @Test
    public void name() {
        String code = "function foo() {};\n" + " foo.bind({}).name;";

        Utils.assertWithAllModes_ES6("bound foo", code);
    }

    @Test
    public void lenght() {
        Utils.assertWithAllModes_ES6(0, "function foo() {}; foo.bind({}).length;");

        Utils.assertWithAllModes_ES6(1, "function foo(a) {}; foo.bind({}).length;");
        Utils.assertWithAllModes_ES6(2, "function foo(a, b) {}; foo.bind({}).length;");

        Utils.assertWithAllModes_ES6(0, "function foo() {}; foo.bind({}, 'x').length;");
        Utils.assertWithAllModes_ES6(0, "function foo() {}; foo.bind({}, 'x', 'y').length;");

        Utils.assertWithAllModes_ES6(0, "function foo(a) {}; foo.bind({}, 'x').length;");
        Utils.assertWithAllModes_ES6(0, "function foo(a) {}; foo.bind({}, 'x', 'y').length;");

        Utils.assertWithAllModes_ES6(1, "function foo(a, b) {}; foo.bind({}, 'x').length;");
        Utils.assertWithAllModes_ES6(0, "function foo(a, b) {}; foo.bind({}, 'x', 'y').length;");

        Utils.assertWithAllModes_ES6(2, "function foo(a, b, c) {}; foo.bind({}, 'x').length;");
        Utils.assertWithAllModes_ES6(1, "function foo(a, b, c) {}; foo.bind({}, 'x', 'y').length;");
    }

    @Test
    public void fooArgs0_boundArgs0_invokeArgs0() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo();";

        Utils.assertWithAllModes_ES6("boundThis []", code);
    }

    @Test
    public void fooArgs0_boundArgs0_invokeArgs1() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo('x');";

        Utils.assertWithAllModes_ES6("boundThis [x]", code);
    }

    @Test
    public void fooArgs0_boundArgs0_invokeArgs2() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo('x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis [x,y]", code);
    }

    @Test
    public void fooArgs0_boundArgs1_invokeArgs0() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo();";

        Utils.assertWithAllModes_ES6("boundThis [a]", code);
    }

    @Test
    public void fooArgs0_boundArgs1_invokeArgs1() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo('x');";

        Utils.assertWithAllModes_ES6("boundThis [a,x]", code);
    }

    @Test
    public void fooArgs0_boundArgs1_invokeArgs2() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo('x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis [a,x,y]", code);
    }

    @Test
    public void fooArgs0_boundArgs2_invokeArgs0() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo();";

        Utils.assertWithAllModes_ES6("boundThis [a,b]", code);
    }

    @Test
    public void fooArgs0_boundArgs2_invokeArgs1() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo('x');";

        Utils.assertWithAllModes_ES6("boundThis [a,b,x]", code);
    }

    @Test
    public void fooArgs0_boundArgs2_invokeArgs2() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo('x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis [a,b,x,y]", code);
    }

    @Test
    public void fooArgs1_boundArgs0_invokeArgs0() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo();";

        Utils.assertWithAllModes_ES6("boundThis undefined []", code);
    }

    @Test
    public void fooArgs1_boundArgs0_invokeArgs1() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo('x');";

        Utils.assertWithAllModes_ES6("boundThis x []", code);
    }

    @Test
    public void fooArgs1_boundArgs0_invokeArgs2() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo('x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis x [y]", code);
    }

    @Test
    public void fooArgs1_boundArgs1_invokeArgs0() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo();";

        Utils.assertWithAllModes_ES6("boundThis a []", code);
    }

    @Test
    public void fooArgs1_boundArgs1_invokeArgs1() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo('x');";

        Utils.assertWithAllModes_ES6("boundThis a [x]", code);
    }

    @Test
    public void fooArgs1_boundArgs1_invokeArgs2() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo('x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis a [x,y]", code);
    }

    @Test
    public void fooArgs1_boundArgs2_invokeArgs0() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo();";

        Utils.assertWithAllModes_ES6("boundThis a [b]", code);
    }

    @Test
    public void fooArgs1_boundArgs2_invokeArgs1() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo('x');";

        Utils.assertWithAllModes_ES6("boundThis a [b,x]", code);
    }

    @Test
    public void fooArgs1_boundArgs2_invokeArgs2() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo('x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis a [b,x,y]", code);
    }

    @Test
    public void fooArgs2_boundArgs0_invokeArgs0() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo();";

        Utils.assertWithAllModes_ES6("boundThis undefined,undefined []", code);
    }

    @Test
    public void fooArgs2_boundArgs0_invokeArgs1() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo('x');";

        Utils.assertWithAllModes_ES6("boundThis x,undefined []", code);
    }

    @Test
    public void fooArgs2_boundArgs0_invokeArgs2() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo('x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis x,y []", code);
    }

    @Test
    public void fooArgs2_boundArgs1_invokeArgs0() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo();";

        Utils.assertWithAllModes_ES6("boundThis a,undefined []", code);
    }

    @Test
    public void fooArgs2_boundArgs1_invokeArgs1() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo('x');";

        Utils.assertWithAllModes_ES6("boundThis a,x []", code);
    }

    @Test
    public void fooArgs2_boundArgs1_invokeArgs2() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo('x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis a,x [y]", code);
    }

    @Test
    public void fooArgs2_boundArgs2_invokeArgs0() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo();";

        Utils.assertWithAllModes_ES6("boundThis a,b []", code);
    }

    @Test
    public void fooArgs2_boundArgs2_invokeArgs1() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo('x');";

        Utils.assertWithAllModes_ES6("boundThis a,b [x]", code);
    }

    @Test
    public void fooArgs2_boundArgs2_invokeArgs2() {
        String code =
                "function foo(i,j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo('x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis a,b [x,y]", code);
    }


    @Test
    public void fooArgs0_boundArgs0_callArgs0() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.call('callThis');";

        Utils.assertWithAllModes_ES6("boundThis []", code);
    }

    @Test
    public void fooArgs0_boundArgs0_callArgs1() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.call('callThis', 'x');";

        Utils.assertWithAllModes_ES6("boundThis [x]", code);
    }

    @Test
    public void fooArgs0_boundArgs0_callArgs2() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.call('callThis', 'x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis [x,y]", code);
    }

    @Test
    public void fooArgs0_boundArgs1_callArgs0() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.call('callThis');";

        Utils.assertWithAllModes_ES6("boundThis [a]", code);
    }

    @Test
    public void fooArgs0_boundArgs1_callArgs1() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.call('callThis', 'x');";

        Utils.assertWithAllModes_ES6("boundThis [a,x]", code);
    }

    @Test
    public void fooArgs0_boundArgs1_callArgs2() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.call('callThis', 'x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis [a,x,y]", code);
    }

    @Test
    public void fooArgs0_boundArgs2_callArgs0() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.call('callThis');";

        Utils.assertWithAllModes_ES6("boundThis [a,b]", code);
    }

    @Test
    public void fooArgs0_boundArgs2_callArgs1() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.call('callThis', 'x');";

        Utils.assertWithAllModes_ES6("boundThis [a,b,x]", code);
    }

    @Test
    public void fooArgs0_boundArgs2_callArgs2() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.call('callThis', 'x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis [a,b,x,y]", code);
    }

    @Test
    public void fooArgs1_boundArgs0_callArgs0() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.call('callThis');";

        Utils.assertWithAllModes_ES6("boundThis undefined []", code);
    }

    @Test
    public void fooArgs1_boundArgs0_callArgs1() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.call('callThis', 'x');";

        Utils.assertWithAllModes_ES6("boundThis x []", code);
    }

    @Test
    public void fooArgs1_boundArgs0_callArgs2() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.call('callThis', 'x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis x [y]", code);
    }

    @Test
    public void fooArgs1_boundArgs1_callArgs0() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.call('callThis');";

        Utils.assertWithAllModes_ES6("boundThis a []", code);
    }

    @Test
    public void fooArgs1_boundArgs1_callArgs1() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.call('callThis', 'x');";

        Utils.assertWithAllModes_ES6("boundThis a [x]", code);
    }

    @Test
    public void fooArgs1_boundArgs1_callArgs2() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.call('callThis', 'x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis a [x,y]", code);
    }

    @Test
    public void fooArgs1_boundArgs2_callArgs0() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.call('callThis');";

        Utils.assertWithAllModes_ES6("boundThis a [b]", code);
    }

    @Test
    public void fooArgs1_boundArgs2_callArgs1() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.call('callThis', 'x');";

        Utils.assertWithAllModes_ES6("boundThis a [b,x]", code);
    }

    @Test
    public void fooArgs1_boundArgs2_callArgs2() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.call('callThis', 'x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis a [b,x,y]", code);
    }

    @Test
    public void fooArgs2_boundArgs0_callArgs0() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.call('callThis');";

        Utils.assertWithAllModes_ES6("boundThis undefined,undefined []", code);
    }

    @Test
    public void fooArgs2_boundArgs0_callArgs1() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.call('callThis', 'x');";

        Utils.assertWithAllModes_ES6("boundThis x,undefined []", code);
    }

    @Test
    public void fooArgs2_boundArgs0_callArgs2() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.call('callThis', 'x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis x,y []", code);
    }

    @Test
    public void fooArgs2_boundArgs1_callArgs0() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.call('callThis');";

        Utils.assertWithAllModes_ES6("boundThis a,undefined []", code);
    }

    @Test
    public void fooArgs2_boundArgs1_callArgs1() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.call('callThis', 'x');";

        Utils.assertWithAllModes_ES6("boundThis a,x []", code);
    }

    @Test
    public void fooArgs2_boundArgs1_callArgs2() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.call('callThis', 'x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis a,x [y]", code);
    }

    @Test
    public void fooArgs2_boundArgs2_callArgs0() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.call('callThis');";

        Utils.assertWithAllModes_ES6("boundThis a,b []", code);
    }

    @Test
    public void fooArgs2_boundArgs2_callArgs1() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.call('callThis', 'x');";

        Utils.assertWithAllModes_ES6("boundThis a,b [x]", code);
    }

    @Test
    public void fooArgs2_boundArgs2_callArgs2() {
        String code =
                "function foo(i,j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.call('callThis', 'x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis a,b [x,y]", code);
    }


    @Test
    public void fooArgs0_boundArgs0_applyArgs0() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.apply('applyThis');";

        Utils.assertWithAllModes_ES6("boundThis []", code);
    }

    @Test
    public void fooArgs0_boundArgs0_applyArgsNull() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.apply('applyThis', null);";

        Utils.assertWithAllModes_ES6("boundThis []", code);
    }

    @Test
    public void fooArgs0_boundArgs0_applyArgsEmpty() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.apply('applyThis', []);";

        Utils.assertWithAllModes_ES6("boundThis []", code);
    }

    @Test
    public void fooArgs0_boundArgs0_applyArgs1() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.apply('applyThis', ['x']);";

        Utils.assertWithAllModes_ES6("boundThis [x]", code);
    }

    @Test
    public void fooArgs0_boundArgs0_applyArgs2() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.apply('applyThis', ['x', 'y']);";

        Utils.assertWithAllModes_ES6("boundThis [x,y]", code);
    }

    @Test
    public void fooArgs0_boundArgs1_applyArgs0() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.apply('applyThis');";

        Utils.assertWithAllModes_ES6("boundThis [a]", code);
    }

    @Test
    public void fooArgs0_boundArgs1_applyArgsNull() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.apply('applyThis', null);";

        Utils.assertWithAllModes_ES6("boundThis [a]", code);
    }

    @Test
    public void fooArgs0_boundArgs1_applyArgsEmpty() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.apply('applyThis', []);";

        Utils.assertWithAllModes_ES6("boundThis [a]", code);
    }

    @Test
    public void fooArgs0_boundArgs1_applyArgs1() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.apply('applyThis', ['x']);";

        Utils.assertWithAllModes_ES6("boundThis [a,x]", code);
    }

    @Test
    public void fooArgs0_boundArgs1_applyArgs2() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.apply('applyThis', ['x', 'y']);";

        Utils.assertWithAllModes_ES6("boundThis [a,x,y]", code);
    }

    @Test
    public void fooArgs0_boundArgs2_applyArgs0() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.apply('applyThis');";

        Utils.assertWithAllModes_ES6("boundThis [a,b]", code);
    }

    @Test
    public void fooArgs0_boundArgs2_applyArgsNull() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.apply('applyThis', null);";

        Utils.assertWithAllModes_ES6("boundThis [a,b]", code);
    }

    @Test
    public void fooArgs0_boundArgs2_applyArgsEmpty() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.apply('applyThis', []);";

        Utils.assertWithAllModes_ES6("boundThis [a,b]", code);
    }

    @Test
    public void fooArgs0_boundArgs2_applyArgs1() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.apply('applyThis', ['x']);";

        Utils.assertWithAllModes_ES6("boundThis [a,b,x]", code);
    }

    @Test
    public void fooArgs0_boundArgs2_applyArgs2() {
        String code =
                "function foo() {\n"
                        + "  var args = Array.prototype.slice.call(arguments);\n"
                        + "  return this.toString() + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.apply('applyThis', ['x', 'y']);";

        Utils.assertWithAllModes_ES6("boundThis [a,b,x,y]", code);
    }

    @Test
    public void fooArgs1_boundArgs0_applyArgs0() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.apply('applyThis');";

        Utils.assertWithAllModes_ES6("boundThis undefined []", code);
    }

    @Test
    public void fooArgs1_boundArgs0_applyArgsNull() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.apply('applyThis', null);";

        Utils.assertWithAllModes_ES6("boundThis undefined []", code);
    }

    @Test
    public void fooArgs1_boundArgs0_applyArgsEmpty() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.apply('applyThis', []);";

        Utils.assertWithAllModes_ES6("boundThis undefined []", code);
    }

    @Test
    public void fooArgs1_boundArgs0_applyArgs1() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.apply('applyThis', ['x']);";

        Utils.assertWithAllModes_ES6("boundThis x []", code);
    }

    @Test
    public void fooArgs1_boundArgs0_applyArgs2() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.apply('applyThis', 'x', 'y');";

        Utils.assertWithAllModes_ES6("boundThis x [y]", code);
    }

    @Test
    public void fooArgs1_boundArgs1_applyArgs0() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.apply('applyThis');";

        Utils.assertWithAllModes_ES6("boundThis a []", code);
    }

    @Test
    public void fooArgs1_boundArgs1_applyArgsNull() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.apply('applyThis', null);";

        Utils.assertWithAllModes_ES6("boundThis a []", code);
    }

    @Test
    public void fooArgs1_boundArgs1_applyArgsEmpty() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.apply('applyThis', []);";

        Utils.assertWithAllModes_ES6("boundThis a []", code);
    }

    @Test
    public void fooArgs1_boundArgs1_applyArgs1() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.apply('applyThis', ['x']);";

        Utils.assertWithAllModes_ES6("boundThis a [x]", code);
    }

    @Test
    public void fooArgs1_boundArgs1_applyArgs2() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.apply('applyThis', ['x', 'y']);";

        Utils.assertWithAllModes_ES6("boundThis a [x,y]", code);
    }


    @Test
    public void fooArgs1_boundArgs2_applyArgs0() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.apply('applyThis');";

        Utils.assertWithAllModes_ES6("boundThis a [b]", code);
    }

    @Test
    public void fooArgs1_boundArgs2_applyArgsNull() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.apply('applyThis', null);";

        Utils.assertWithAllModes_ES6("boundThis a [b]", code);
    }

    @Test
    public void fooArgs1_boundArgs2_applyArgsEmpty() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.apply('applyThis', []);";

        Utils.assertWithAllModes_ES6("boundThis a [b]", code);
    }

    @Test
    public void fooArgs1_boundArgs2_applyArgs1() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.apply('applyThis', ['x']);";

        Utils.assertWithAllModes_ES6("boundThis a [b,x]", code);
    }

    @Test
    public void fooArgs1_boundArgs2_applyArgs2() {
        String code =
                "function foo(i) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 1);\n"
                        + "  return this.toString() + ' ' + i + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.apply('applyThis', ['x', 'y']);";

        Utils.assertWithAllModes_ES6("boundThis a [b,x,y]", code);
    }

    @Test
    public void fooArgs2_boundArgs0_applyArgs0() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.apply('applyThis');";

        Utils.assertWithAllModes_ES6("boundThis undefined,undefined []", code);
    }

    @Test
    public void fooArgs2_boundArgs0_applyArgsNull() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.apply('applyThis', null);";

        Utils.assertWithAllModes_ES6("boundThis undefined,undefined []", code);
    }

    @Test
    public void fooArgs2_boundArgs0_applyArgsEmpty() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.apply('applyThis', []);";

        Utils.assertWithAllModes_ES6("boundThis undefined,undefined []", code);
    }

    @Test
    public void fooArgs2_boundArgs0_applyArgs1() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.apply('applyThis', ['x']);";

        Utils.assertWithAllModes_ES6("boundThis x,undefined []", code);
    }

    @Test
    public void fooArgs2_boundArgs0_applyArgs2() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis');\n"
                        + "boundFoo.apply('applyThis', ['x', 'y']);";

        Utils.assertWithAllModes_ES6("boundThis x,y []", code);
    }

    @Test
    public void fooArgs2_boundArgs1_applyArgs0() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.apply('applyThis');";

        Utils.assertWithAllModes_ES6("boundThis a,undefined []", code);
    }

    @Test
    public void fooArgs2_boundArgs1_applyArgsNull() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.apply('applyThis', null);";

        Utils.assertWithAllModes_ES6("boundThis a,undefined []", code);
    }

    @Test
    public void fooArgs2_boundArgs1_applyArgsEmpty() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.apply('applyThis', []);";

        Utils.assertWithAllModes_ES6("boundThis a,undefined []", code);
    }

    @Test
    public void fooArgs2_boundArgs1_applyArgs1() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.apply('applyThis', ['x']);";

        Utils.assertWithAllModes_ES6("boundThis a,x []", code);
    }

    @Test
    public void fooArgs2_boundArgs1_applyArgs2() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a');\n"
                        + "boundFoo.apply('applyThis', ['x', 'y']);";

        Utils.assertWithAllModes_ES6("boundThis a,x [y]", code);
    }

    @Test
    public void fooArgs2_boundArgs2_applyArgs0() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.apply('applyThis');";

        Utils.assertWithAllModes_ES6("boundThis a,b []", code);
    }

    @Test
    public void fooArgs2_boundArgs2_applyArgsNull() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.apply('applyThis', null);";

        Utils.assertWithAllModes_ES6("boundThis a,b []", code);
    }

    @Test
    public void fooArgs2_boundArgs2_applyArgsEmpty() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.apply('applyThis', []);";

        Utils.assertWithAllModes_ES6("boundThis a,b []", code);
    }

    @Test
    public void fooArgs2_boundArgs2_applyArgs1() {
        String code =
                "function foo(i, j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.apply('applyThis', ['x']);";

        Utils.assertWithAllModes_ES6("boundThis a,b [x]", code);
    }

    @Test
    public void fooArgs2_boundArgs2_applyArgs2() {
        String code =
                "function foo(i,j) {\n"
                        + "  var args = Array.prototype.slice.call(arguments, 2);\n"
                        + "  return this.toString() + ' ' + i + ',' + j + ' [' + args.join(',') + ']';\n"
                        + "};\n"
                        + "var boundFoo = foo.bind('boundThis', 'a', 'b');\n"
                        + "boundFoo.apply('applyThis', ['x', 'y']);";

        Utils.assertWithAllModes_ES6("boundThis a,b [x,y]", code);
    }

    @Test
    public void invokeBoundCallManyArgs() {
        /* This test is a little fiddly. The call to bind causes the
        max stack size to be high enough that it could mask the
        bug, so we have to make the call to the bound in function
        in another function which has a smaller stack. */
        String code =
                "function f() { return 'Hello!'; };\n"
                        + "var b = f.call.bind(f, 1, 2, 3);\n"
                        + "(function(){ return b(); })();";

        Utils.assertWithAllModes_ES6("Hello!", code);
    }
}
