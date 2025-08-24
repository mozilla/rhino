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
        Utils.assertWithAllModes_ES6("boundThis []", constructCode(0, 0, "(", 0, false));
        Utils.assertWithAllModes_ES6("boundThis []", constructCode(0, 0, "(", 0, true));
    }

    @Test
    public void fooArgs0_boundArgs0_invokeArgs1() {
        Utils.assertWithAllModes_ES6("boundThis [x]", constructCode(0, 0, "(", 1, false));
        Utils.assertWithAllModes_ES6("boundThis [x]", constructCode(0, 0, "(", 1, true));
    }

    @Test
    public void fooArgs0_boundArgs0_invokeArgs2() {
        Utils.assertWithAllModes_ES6("boundThis [x,y]", constructCode(0, 0, "(", 2, false));
        Utils.assertWithAllModes_ES6("boundThis [x,y]", constructCode(0, 0, "(", 2, true));
    }

    @Test
    public void fooArgs0_boundArgs1_invokeArgs0() {
        Utils.assertWithAllModes_ES6("boundThis [a]", constructCode(0, 1, "(", 0, false));
        Utils.assertWithAllModes_ES6("boundThis [a]", constructCode(0, 1, "(", 0, true));
    }

    @Test
    public void fooArgs0_boundArgs1_invokeArgs1() {
        Utils.assertWithAllModes_ES6("boundThis [a,x]", constructCode(0, 1, "(", 1, false));
        Utils.assertWithAllModes_ES6("boundThis [a,x]", constructCode(0, 1, "(", 1, true));
    }

    @Test
    public void fooArgs0_boundArgs1_invokeArgs2() {
        Utils.assertWithAllModes_ES6("boundThis [a,x,y]", constructCode(0, 1, "(", 2, false));
        Utils.assertWithAllModes_ES6("boundThis [a,x,y]", constructCode(0, 1, "(", 2, true));
    }

    @Test
    public void fooArgs0_boundArgs2_invokeArgs0() {
        Utils.assertWithAllModes_ES6("boundThis [a,b]", constructCode(0, 2, "(", 0, false));
        Utils.assertWithAllModes_ES6("boundThis [a,b]", constructCode(0, 2, "(", 0, true));
    }

    @Test
    public void fooArgs0_boundArgs2_invokeArgs1() {
        Utils.assertWithAllModes_ES6("boundThis [a,b,x]", constructCode(0, 2, "(", 1, false));
        Utils.assertWithAllModes_ES6("boundThis [a,b,x]", constructCode(0, 2, "(", 1, true));
    }

    @Test
    public void fooArgs0_boundArgs2_invokeArgs2() {
        Utils.assertWithAllModes_ES6("boundThis [a,b,x,y]", constructCode(0, 2, "(", 2, false));
        Utils.assertWithAllModes_ES6("boundThis [a,b,x,y]", constructCode(0, 2, "(", 2, true));
    }

    @Test
    public void fooArgs1_boundArgs0_invokeArgs0() {
        Utils.assertWithAllModes_ES6("boundThis undefined []", constructCode(1, 0, "(", 0, false));
        Utils.assertWithAllModes_ES6("boundThis undefined []", constructCode(1, 0, "(", 0, true));
    }

    @Test
    public void fooArgs1_boundArgs0_invokeArgs1() {
        Utils.assertWithAllModes_ES6("boundThis x []", constructCode(1, 0, "(", 1, false));
        Utils.assertWithAllModes_ES6("boundThis x []", constructCode(1, 0, "(", 1, true));
    }

    @Test
    public void fooArgs1_boundArgs0_invokeArgs2() {
        Utils.assertWithAllModes_ES6("boundThis x [y]", constructCode(1, 0, "(", 2, false));
        Utils.assertWithAllModes_ES6("boundThis x [y]", constructCode(1, 0, "(", 2, true));
    }

    @Test
    public void fooArgs1_boundArgs1_invokeArgs0() {
        Utils.assertWithAllModes_ES6("boundThis a []", constructCode(1, 1, "(", 0, false));
        Utils.assertWithAllModes_ES6("boundThis a []", constructCode(1, 1, "(", 0, true));
    }

    @Test
    public void fooArgs1_boundArgs1_invokeArgs1() {
        Utils.assertWithAllModes_ES6("boundThis a [x]", constructCode(1, 1, "(", 1, false));
        Utils.assertWithAllModes_ES6("boundThis a [x]", constructCode(1, 1, "(", 1, true));
    }

    @Test
    public void fooArgs1_boundArgs1_invokeArgs2() {
        Utils.assertWithAllModes_ES6("boundThis a [x,y]", constructCode(1, 1, "(", 2, false));
        Utils.assertWithAllModes_ES6("boundThis a [x,y]", constructCode(1, 1, "(", 2, true));
    }

    @Test
    public void fooArgs1_boundArgs2_invokeArgs0() {
        Utils.assertWithAllModes_ES6("boundThis a [b]", constructCode(1, 2, "(", 0, false));
        Utils.assertWithAllModes_ES6("boundThis a [b]", constructCode(1, 2, "(", 0, true));
    }

    @Test
    public void fooArgs1_boundArgs2_invokeArgs1() {
        Utils.assertWithAllModes_ES6("boundThis a [b,x]", constructCode(1, 2, "(", 1, false));
        Utils.assertWithAllModes_ES6("boundThis a [b,x]", constructCode(1, 2, "(", 1, true));
    }

    @Test
    public void fooArgs1_boundArgs2_invokeArgs2() {
        Utils.assertWithAllModes_ES6("boundThis a [b,x,y]", constructCode(1, 2, "(", 2, false));
        Utils.assertWithAllModes_ES6("boundThis a [b,x,y]", constructCode(1, 2, "(", 2, true));
    }

    @Test
    public void fooArgs2_boundArgs0_invokeArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis undefined,undefined []", constructCode(2, 0, "(", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis undefined,undefined []", constructCode(2, 0, "(", 0, true));
    }

    @Test
    public void fooArgs2_boundArgs0_invokeArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis x,undefined []", constructCode(2, 0, "(", 1, false));
        Utils.assertWithAllModes_ES6("boundThis x,undefined []", constructCode(2, 0, "(", 1, true));
    }

    @Test
    public void fooArgs2_boundArgs0_invokeArgs2() {
        Utils.assertWithAllModes_ES6("boundThis x,y []", constructCode(2, 0, "(", 2, false));
        Utils.assertWithAllModes_ES6("boundThis x,y []", constructCode(2, 0, "(", 2, true));
    }

    @Test
    public void fooArgs2_boundArgs1_invokeArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,undefined []", constructCode(2, 1, "(", 0, false));
        Utils.assertWithAllModes_ES6("boundThis a,undefined []", constructCode(2, 1, "(", 0, true));
    }

    @Test
    public void fooArgs2_boundArgs1_invokeArgs1() {
        Utils.assertWithAllModes_ES6("boundThis a,x []", constructCode(2, 1, "(", 1, false));
        Utils.assertWithAllModes_ES6("boundThis a,x []", constructCode(2, 1, "(", 1, true));
    }

    @Test
    public void fooArgs2_boundArgs1_invokeArgs2() {
        Utils.assertWithAllModes_ES6("boundThis a,x [y]", constructCode(2, 1, "(", 2, false));
        Utils.assertWithAllModes_ES6("boundThis a,x [y]", constructCode(2, 1, "(", 2, true));
    }

    @Test
    public void fooArgs2_boundArgs2_invokeArgs0() {
        Utils.assertWithAllModes_ES6("boundThis a,b []", constructCode(2, 2, "(", 0, false));
        Utils.assertWithAllModes_ES6("boundThis a,b []", constructCode(2, 2, "(", 0, true));
    }

    @Test
    public void fooArgs2_boundArgs2_invokeArgs1() {
        Utils.assertWithAllModes_ES6("boundThis a,b [x]", constructCode(2, 2, "(", 1, false));
        Utils.assertWithAllModes_ES6("boundThis a,b [x]", constructCode(2, 2, "(", 1, true));
    }

    @Test
    public void fooArgs2_boundArgs2_invokeArgs2() {
        Utils.assertWithAllModes_ES6("boundThis a,b [x,y]", constructCode(2, 2, "(", 2, false));
        Utils.assertWithAllModes_ES6("boundThis a,b [x,y]", constructCode(2, 2, "(", 2, true));
    }

    @Test
    public void fooArgs0_boundArgs0_callArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis []", constructCode(0, 0, ".call('callThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis []", constructCode(0, 0, ".call('callThis'", 0, true));
    }

    @Test
    public void fooArgs0_boundArgs0_callArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis [x]", constructCode(0, 0, ".call('callThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [x]", constructCode(0, 0, ".call('callThis'", 1, true));
    }

    @Test
    public void fooArgs0_boundArgs0_callArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis [x,y]", constructCode(0, 0, ".call('callThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [x,y]", constructCode(0, 0, ".call('callThis'", 2, true));
    }

    @Test
    public void fooArgs0_boundArgs1_callArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis [a]", constructCode(0, 1, ".call('callThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [a]", constructCode(0, 1, ".call('callThis'", 0, true));
    }

    @Test
    public void fooArgs0_boundArgs1_callArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis [a,x]", constructCode(0, 1, ".call('callThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [a,x]", constructCode(0, 1, ".call('callThis'", 1, true));
    }

    @Test
    public void fooArgs0_boundArgs1_callArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis [a,x,y]", constructCode(0, 1, ".call('callThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [a,x,y]", constructCode(0, 1, ".call('callThis'", 2, true));
    }

    @Test
    public void fooArgs0_boundArgs2_callArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis [a,b]", constructCode(0, 2, ".call('callThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [a,b]", constructCode(0, 2, ".call('callThis'", 0, true));
    }

    @Test
    public void fooArgs0_boundArgs2_callArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis [a,b,x]", constructCode(0, 2, ".call('callThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [a,b,x]", constructCode(0, 2, ".call('callThis'", 1, true));
    }

    @Test
    public void fooArgs0_boundArgs2_callArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis [a,b,x,y]", constructCode(0, 2, ".call('callThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [a,b,x,y]", constructCode(0, 2, ".call('callThis'", 2, true));
    }

    @Test
    public void fooArgs1_boundArgs0_callArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis undefined []", constructCode(1, 0, ".call('callThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis undefined []", constructCode(1, 0, ".call('callThis'", 0, true));
    }

    @Test
    public void fooArgs1_boundArgs0_callArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis x []", constructCode(1, 0, ".call('callThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis x []", constructCode(1, 0, ".call('callThis'", 1, true));
    }

    @Test
    public void fooArgs1_boundArgs0_callArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis x [y]", constructCode(1, 0, ".call('callThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis x [y]", constructCode(1, 0, ".call('callThis'", 2, true));
    }

    @Test
    public void fooArgs1_boundArgs1_callArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis a []", constructCode(1, 1, ".call('callThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a []", constructCode(1, 1, ".call('callThis'", 0, true));
    }

    @Test
    public void fooArgs1_boundArgs1_callArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis a [x]", constructCode(1, 1, ".call('callThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a [x]", constructCode(1, 1, ".call('callThis'", 1, true));
    }

    @Test
    public void fooArgs1_boundArgs1_callArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis a [x,y]", constructCode(1, 1, ".call('callThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a [x,y]", constructCode(1, 1, ".call('callThis'", 2, true));
    }

    @Test
    public void fooArgs1_boundArgs2_callArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis a [b]", constructCode(1, 2, ".call('callThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a [b]", constructCode(1, 2, ".call('callThis'", 0, true));
    }

    @Test
    public void fooArgs1_boundArgs2_callArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis a [b,x]", constructCode(1, 2, ".call('callThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a [b,x]", constructCode(1, 2, ".call('callThis'", 1, true));
    }

    @Test
    public void fooArgs1_boundArgs2_callArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis a [b,x,y]", constructCode(1, 2, ".call('callThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a [b,x,y]", constructCode(1, 2, ".call('callThis'", 2, true));
    }

    @Test
    public void fooArgs2_boundArgs0_callArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis undefined,undefined []",
                constructCode(2, 0, ".call('callThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis undefined,undefined []",
                constructCode(2, 0, ".call('callThis'", 0, true));
    }

    @Test
    public void fooArgs2_boundArgs0_callArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis x,undefined []", constructCode(2, 0, ".call('callThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis x,undefined []", constructCode(2, 0, ".call('callThis'", 1, true));
    }

    @Test
    public void fooArgs2_boundArgs0_callArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis x,y []", constructCode(2, 0, ".call('callThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis x,y []", constructCode(2, 0, ".call('callThis'", 2, true));
    }

    @Test
    public void fooArgs2_boundArgs1_callArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,undefined []", constructCode(2, 1, ".call('callThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a,undefined []", constructCode(2, 1, ".call('callThis'", 0, true));
    }

    @Test
    public void fooArgs2_boundArgs1_callArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,x []", constructCode(2, 1, ".call('callThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a,x []", constructCode(2, 1, ".call('callThis'", 1, true));
    }

    @Test
    public void fooArgs2_boundArgs1_callArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,x [y]", constructCode(2, 1, ".call('callThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a,x [y]", constructCode(2, 1, ".call('callThis'", 2, true));
    }

    @Test
    public void fooArgs2_boundArgs2_callArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,b []", constructCode(2, 2, ".call('callThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a,b []", constructCode(2, 2, ".call('callThis'", 0, true));
    }

    @Test
    public void fooArgs2_boundArgs2_callArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,b [x]", constructCode(2, 2, ".call('callThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a,b [x]", constructCode(2, 2, ".call('callThis'", 1, true));
    }

    @Test
    public void fooArgs2_boundArgs2_callArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,b [x,y]", constructCode(2, 2, ".call('callThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a,b [x,y]", constructCode(2, 2, ".call('callThis'", 2, true));
    }

    @Test
    public void fooArgs0_boundArgs0_applyArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis []", constructCode(0, 0, ".apply('applyThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis []", constructCode(0, 0, ".apply('applyThis'", 0, true));
    }

    @Test
    public void fooArgs0_boundArgs0_applyArgsNull() {
        Utils.assertWithAllModes_ES6(
                "boundThis []", constructCode(0, 0, ".apply('applyThis'", -1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis []", constructCode(0, 0, ".apply('applyThis'", -1, true));
    }

    @Test
    public void fooArgs0_boundArgs0_applyArgsEmpty() {
        Utils.assertWithAllModes_ES6(
                "boundThis []", constructCode(0, 0, ".apply('applyThis'", -2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis []", constructCode(0, 0, ".apply('applyThis'", -2, true));
    }

    @Test
    public void fooArgs0_boundArgs0_applyArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis [x]", constructCode(0, 0, ".apply('applyThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [x]", constructCode(0, 0, ".apply('applyThis'", 1, true));
    }

    @Test
    public void fooArgs0_boundArgs0_applyArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis [x,y]", constructCode(0, 0, ".apply('applyThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [x,y]", constructCode(0, 0, ".apply('applyThis'", 2, true));
    }

    @Test
    public void fooArgs0_boundArgs1_applyArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis [a]", constructCode(0, 1, ".apply('applyThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [a]", constructCode(0, 1, ".apply('applyThis'", 0, true));
    }

    @Test
    public void fooArgs0_boundArgs1_applyArgsNull() {
        Utils.assertWithAllModes_ES6(
                "boundThis [a]", constructCode(0, 1, ".apply('applyThis'", -1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [a]", constructCode(0, 1, ".apply('applyThis'", -1, true));
    }

    @Test
    public void fooArgs0_boundArgs1_applyArgsEmpty() {
        Utils.assertWithAllModes_ES6(
                "boundThis [a]", constructCode(0, 1, ".apply('applyThis'", -2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [a]", constructCode(0, 1, ".apply('applyThis'", -2, true));
    }

    @Test
    public void fooArgs0_boundArgs1_applyArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis [a,x]", constructCode(0, 1, ".apply('applyThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [a,x]", constructCode(0, 1, ".apply('applyThis'", 1, true));
    }

    @Test
    public void fooArgs0_boundArgs1_applyArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis [a,x,y]", constructCode(0, 1, ".apply('applyThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [a,x,y]", constructCode(0, 1, ".apply('applyThis'", 2, true));
    }

    @Test
    public void fooArgs0_boundArgs2_applyArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis [a,b]", constructCode(0, 2, ".apply('applyThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [a,b]", constructCode(0, 2, ".apply('applyThis'", 0, true));
    }

    @Test
    public void fooArgs0_boundArgs2_applyArgsNull() {
        Utils.assertWithAllModes_ES6(
                "boundThis [a,b]", constructCode(0, 2, ".apply('applyThis'", -1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [a,b]", constructCode(0, 2, ".apply('applyThis'", -1, true));
    }

    @Test
    public void fooArgs0_boundArgs2_applyArgsEmpty() {
        Utils.assertWithAllModes_ES6(
                "boundThis [a,b]", constructCode(0, 2, ".apply('applyThis'", -2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [a,b]", constructCode(0, 2, ".apply('applyThis'", -2, true));
    }

    @Test
    public void fooArgs0_boundArgs2_applyArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis [a,b,x]", constructCode(0, 2, ".apply('applyThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [a,b,x]", constructCode(0, 2, ".apply('applyThis'", 1, true));
    }

    @Test
    public void fooArgs0_boundArgs2_applyArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis [a,b,x,y]", constructCode(0, 2, ".apply('applyThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis [a,b,x,y]", constructCode(0, 2, ".apply('applyThis'", 2, true));
    }

    @Test
    public void fooArgs1_boundArgs0_applyArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis undefined []", constructCode(1, 0, ".apply('applyThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis undefined []", constructCode(1, 0, ".apply('applyThis'", 0, true));
    }

    @Test
    public void fooArgs1_boundArgs0_applyArgsNull() {
        Utils.assertWithAllModes_ES6(
                "boundThis undefined []", constructCode(1, 0, ".apply('applyThis'", -1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis undefined []", constructCode(1, 0, ".apply('applyThis'", -1, true));
    }

    @Test
    public void fooArgs1_boundArgs0_applyArgsEmpty() {
        Utils.assertWithAllModes_ES6(
                "boundThis undefined []", constructCode(1, 0, ".apply('applyThis'", -2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis undefined []", constructCode(1, 0, ".apply('applyThis'", -2, true));
    }

    @Test
    public void fooArgs1_boundArgs0_applyArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis x []", constructCode(1, 0, ".apply('applyThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis x []", constructCode(1, 0, ".apply('applyThis'", 1, true));
    }

    @Test
    public void fooArgs1_boundArgs0_applyArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis x [y]", constructCode(1, 0, ".apply('applyThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis x [y]", constructCode(1, 0, ".apply('applyThis'", 2, true));
    }

    @Test
    public void fooArgs1_boundArgs1_applyArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis a []", constructCode(1, 1, ".apply('applyThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a []", constructCode(1, 1, ".apply('applyThis'", 0, true));
    }

    @Test
    public void fooArgs1_boundArgs1_applyArgsNull() {
        Utils.assertWithAllModes_ES6(
                "boundThis a []", constructCode(1, 1, ".apply('applyThis'", -1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a []", constructCode(1, 1, ".apply('applyThis'", -1, true));
    }

    @Test
    public void fooArgs1_boundArgs1_applyArgsEmpty() {
        Utils.assertWithAllModes_ES6(
                "boundThis a []", constructCode(1, 1, ".apply('applyThis'", -2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a []", constructCode(1, 1, ".apply('applyThis'", -2, true));
    }

    @Test
    public void fooArgs1_boundArgs1_applyArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis a [x]", constructCode(1, 1, ".apply('applyThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a [x]", constructCode(1, 1, ".apply('applyThis'", 1, true));
    }

    @Test
    public void fooArgs1_boundArgs1_applyArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis a [x,y]", constructCode(1, 1, ".apply('applyThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a [x,y]", constructCode(1, 1, ".apply('applyThis'", 2, true));
    }

    @Test
    public void fooArgs1_boundArgs2_applyArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis a [b]", constructCode(1, 2, ".apply('applyThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a [b]", constructCode(1, 2, ".apply('applyThis'", 0, true));
    }

    @Test
    public void fooArgs1_boundArgs2_applyArgsNull() {
        Utils.assertWithAllModes_ES6(
                "boundThis a [b]", constructCode(1, 2, ".apply('applyThis'", -1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a [b]", constructCode(1, 2, ".apply('applyThis'", -1, true));
    }

    @Test
    public void fooArgs1_boundArgs2_applyArgsEmpty() {
        Utils.assertWithAllModes_ES6(
                "boundThis a [b]", constructCode(1, 2, ".apply('applyThis'", -2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a [b]", constructCode(1, 2, ".apply('applyThis'", -2, true));
    }

    @Test
    public void fooArgs1_boundArgs2_applyArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis a [b,x]", constructCode(1, 2, ".apply('applyThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a [b,x]", constructCode(1, 2, ".apply('applyThis'", 1, true));
    }

    @Test
    public void fooArgs1_boundArgs2_applyArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis a [b,x,y]", constructCode(1, 2, ".apply('applyThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a [b,x,y]", constructCode(1, 2, ".apply('applyThis'", 2, true));
    }

    @Test
    public void fooArgs2_boundArgs0_applyArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis undefined,undefined []",
                constructCode(2, 0, ".apply('applyThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis undefined,undefined []",
                constructCode(2, 0, ".apply('applyThis'", 0, true));
    }

    @Test
    public void fooArgs2_boundArgs0_applyArgsNull() {
        Utils.assertWithAllModes_ES6(
                "boundThis undefined,undefined []",
                constructCode(2, 0, ".apply('applyThis'", -1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis undefined,undefined []",
                constructCode(2, 0, ".apply('applyThis'", -1, true));
    }

    @Test
    public void fooArgs2_boundArgs0_applyArgsEmpty() {
        Utils.assertWithAllModes_ES6(
                "boundThis undefined,undefined []",
                constructCode(2, 0, ".apply('applyThis'", -2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis undefined,undefined []",
                constructCode(2, 0, ".apply('applyThis'", -2, true));
    }

    @Test
    public void fooArgs2_boundArgs0_applyArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis x,undefined []", constructCode(2, 0, ".apply('applyThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis x,undefined []", constructCode(2, 0, ".apply('applyThis'", 1, true));
    }

    @Test
    public void fooArgs2_boundArgs0_applyArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis x,y []", constructCode(2, 0, ".apply('applyThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis x,y []", constructCode(2, 0, ".apply('applyThis'", 2, true));
    }

    @Test
    public void fooArgs2_boundArgs1_applyArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,undefined []", constructCode(2, 1, ".apply('applyThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a,undefined []", constructCode(2, 1, ".apply('applyThis'", 0, true));
    }

    @Test
    public void fooArgs2_boundArgs1_applyArgsNull() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,undefined []", constructCode(2, 1, ".apply('applyThis'", -1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a,undefined []", constructCode(2, 1, ".apply('applyThis'", -1, true));
    }

    @Test
    public void fooArgs2_boundArgs1_applyArgsEmpty() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,undefined []", constructCode(2, 1, ".apply('applyThis'", -2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a,undefined []", constructCode(2, 1, ".apply('applyThis'", -2, true));
    }

    @Test
    public void fooArgs2_boundArgs1_applyArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,x []", constructCode(2, 1, ".apply('applyThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a,x []", constructCode(2, 1, ".apply('applyThis'", 1, true));
    }

    @Test
    public void fooArgs2_boundArgs1_applyArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,x [y]", constructCode(2, 1, ".apply('applyThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a,x [y]", constructCode(2, 1, ".apply('applyThis'", 2, true));
    }

    @Test
    public void fooArgs2_boundArgs2_applyArgs0() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,b []", constructCode(2, 2, ".apply('applyThis'", 0, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a,b []", constructCode(2, 2, ".apply('applyThis'", 0, true));
    }

    @Test
    public void fooArgs2_boundArgs2_applyArgsNull() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,b []", constructCode(2, 2, ".apply('applyThis'", -1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a,b []", constructCode(2, 2, ".apply('applyThis'", -1, true));
    }

    @Test
    public void fooArgs2_boundArgs2_applyArgsEmpty() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,b []", constructCode(2, 2, ".apply('applyThis'", -2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a,b []", constructCode(2, 2, ".apply('applyThis'", -2, true));
    }

    @Test
    public void fooArgs2_boundArgs2_applyArgs1() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,b [x]", constructCode(2, 2, ".apply('applyThis'", 1, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a,b [x]", constructCode(2, 2, ".apply('applyThis'", 1, true));
    }

    @Test
    public void fooArgs2_boundArgs2_applyArgs2() {
        Utils.assertWithAllModes_ES6(
                "boundThis a,b [x,y]", constructCode(2, 2, ".apply('applyThis'", 2, false));
        Utils.assertWithAllModes_ES6(
                "boundThis a,b [x,y]", constructCode(2, 2, ".apply('applyThis'", 2, true));
    }

    public String constructCode(
            int fooArgs, int boundArgs, String invokeFoo, int invokeArgs, boolean activation) {
        StringBuilder code = new StringBuilder();
        code.append("function foo(");
        if (fooArgs == 1) {
            code.append("i");
        } else if (fooArgs == 2) {
            code.append("i,j");
        }
        code.append(") {\n");

        if (activation) {
            code.append("  function inner(s) { return s };\n");
        }
        code.append("  var args = Array.prototype.slice.call(arguments, ")
                .append(Integer.toString(fooArgs))
                .append(");\n");

        code.append("  var res = this.toString()");

        if (fooArgs == 1) {
            code.append(" + ' ' + i");
        } else if (fooArgs == 2) {
            code.append(" + ' ' + i + ',' + j");
        }
        code.append(" + ' [' + args.join(',') + ']';\n");
        if (activation) {
            code.append("  return inner(res);\n");
        } else {
            code.append("  return res;\n");
        }
        code.append("};\n");

        code.append("var boundFoo = foo.bind('boundThis'");
        if (boundArgs == 1) {
            code.append(", 'a'");
        } else if (boundArgs == 2) {
            code.append(", 'a', 'b'");
        }
        code.append(");\n");

        code.append("boundFoo").append(invokeFoo).append("");
        if (invokeFoo.length() > 1) {
            code.append(", ");
        }
        if (invokeFoo.contains("apply(")) {
            if (invokeArgs == 1) {
                code.append("['x']");
            } else if (invokeArgs == 2) {
                code.append("['x', 'y']");
            } else if (invokeArgs == -1) {
                code.append("null");
            } else if (invokeArgs == -2) {
                code.append("[]");
            }
        } else {
            if (invokeArgs == 1) {
                code.append("'x'");
            } else if (invokeArgs == 2) {
                code.append("'x', 'y'");
            }
        }
        code.append(");");

        return code.toString();
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
