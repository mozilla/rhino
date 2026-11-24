/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2025;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.testutils.Utils;

public class IteratorHelpersTest {

    private static void assertES(Object expected, String script) {
        Utils.assertWithAllModes(Context.VERSION_ECMASCRIPT, null, expected, script);
    }

    @Test
    public void prototypeChain() {
        assertES(
                "true,true,true,true",
                "function* g() {}\n"
                        + "var ip = Iterator.prototype;\n"
                        + "[Object.getPrototypeOf(Object.getPrototypeOf([].values())) === ip,\n"
                        + " Object.getPrototypeOf(Object.getPrototypeOf(''[Symbol.iterator]())) === ip,\n"
                        + " new Map().entries() instanceof Iterator,\n"
                        + " g() instanceof Iterator].join()");
    }

    @Test
    public void abstractConstructor() {
        assertES(
                "TypeError,TypeError,TypeError",
                "var r = [];\n"
                        + "try { new Iterator(); } catch (e) { r.push(e.name); }\n"
                        + "try { Iterator(); } catch (e) { r.push(e.name); }\n"
                        + "try { Reflect.construct(Iterator, []); } catch (e) { r.push(e.name); }\n"
                        + "r.join()");
    }

    @Test
    public void lazyHelpers() {
        assertES(
                "2,4,6|1,3|3,4|1,1,2,2|1,2",
                "[[1, 2, 3].values().map(x => x * 2).toArray(),\n"
                        + " [1, 2, 3, 4, 5].values().filter(x => x % 2).take(2).toArray(),\n"
                        + " [1, 2, 3, 4].values().drop(2).toArray(),\n"
                        + " [1, 2].values().flatMap(x => [x, x]).toArray(),\n"
                        + " [1, 2, 3].values().take(Infinity).take(2).toArray()].join('|')");
    }

    @Test
    public void eagerHelpers() {
        assertES(
                "6|10|true|false|2|0a,1b",
                "var log = [];\n"
                        + "['a', 'b'].values().forEach((v, i) => log.push(i + v));\n"
                        + "[[1, 2, 3].values().reduce((a, b) => a + b),\n"
                        + " [1, 2, 3].values().reduce((a, b) => a + b, 4),\n"
                        + " [1, 2, 3].values().some(x => x > 2),\n"
                        + " [1, 2, 3].values().every(x => x > 2),\n"
                        + " [1, 2, 3].values().find(x => x > 1),\n"
                        + " log].join('|')");
    }

    @Test
    public void helperIsLazy() {
        assertES(
                "0|1|2,2",
                "var calls = 0;\n"
                        + "var it = [1, 2, 3].values().map(x => { calls++; return x; });\n"
                        + "var r = [calls];\n"
                        + "it.next();\n"
                        + "r.push(calls);\n"
                        + "r.push([it.next().value, calls]);\n"
                        + "r.join('|')");
    }

    @Test
    public void helperToStringTag() {
        assertES(
                "[object Iterator Helper]",
                "Object.prototype.toString.call([].values().map(x => x))");
    }

    @Test
    public void returnClosesUnderlying() {
        assertES(
                "1|true",
                "var closed = 0;\n"
                        + "var src = { next() { return { value: 1, done: false }; },\n"
                        + "            return() { closed++; return {}; } };\n"
                        + "Object.setPrototypeOf(src, Iterator.prototype);\n"
                        + "var it = src.map(x => x);\n"
                        + "it.next();\n"
                        + "var res = it.return();\n"
                        + "[closed, res.done].join('|')");
    }

    @Test
    public void earlyExitClosesUnderlying() {
        assertES(
                "1,1,1",
                "var closed = 0;\n"
                        + "function src() {\n"
                        + "  var it = { next() { return { value: 1, done: false }; },\n"
                        + "             return() { closed++; return {}; } };\n"
                        + "  return Object.setPrototypeOf(it, Iterator.prototype);\n"
                        + "}\n"
                        + "var r = [];\n"
                        + "src().some(x => true); r.push(closed); closed = 0;\n"
                        + "src().find(x => true); r.push(closed); closed = 0;\n"
                        + "src().take(1).toArray(); r.push(closed);\n"
                        + "r.join()");
    }

    @Test
    public void validationClosesUnderlying() {
        assertES(
                "TypeError:1,RangeError:1",
                "var closed = 0;\n"
                        + "var src = { next() {}, return() { closed++; return {}; } };\n"
                        + "Object.setPrototypeOf(src, Iterator.prototype);\n"
                        + "var r = [];\n"
                        + "try { src.map(1); } catch (e) { r.push(e.name + ':' + closed); }\n"
                        + "closed = 0;\n"
                        + "try { src.take(-1); } catch (e) { r.push(e.name + ':' + closed); }\n"
                        + "r.join()");
    }

    @Test
    public void reentrancyThrows() {
        assertES(
                "TypeError",
                "var it = [1].values().map(x => it.next());\n"
                        + "try { it.next(); 'no error'; } catch (e) { e.name; }");
    }

    @Test
    public void reduceOfEmptyThrows() {
        assertES(
                "TypeError",
                "try { [].values().reduce((a, b) => a); 'no error'; } catch (e) { e.name; }");
    }

    @Test
    public void from() {
        assertES(
                "true|true|0,1,2|a,b",
                "var plain = { i: 0, next() { return { done: this.i > 2, value: this.i++ }; } };\n"
                        + "var wrapped = Iterator.from(plain);\n"
                        + "var ai = [].values();\n"
                        + "[Iterator.from(ai) === ai,\n"
                        + " wrapped instanceof Iterator,\n"
                        + " wrapped.toArray(),\n"
                        + " Iterator.from('ab').toArray()].join('|')");
    }

    @Test
    public void concat() {
        assertES(
                "1,2,3,a",
                "Iterator.concat([1], new Set([2, 3]), ['a'].values()).toArray().join()");
    }

    @Test
    public void constructorAndToStringTagAccessors() {
        assertES(
                "TypeError|true|5|Iterator",
                "var r = [];\n"
                        + "try { Iterator.prototype.constructor = 1; } catch (e) { r.push(e.name); }\n"
                        + "var o = Object.create(Iterator.prototype);\n"
                        + "o.constructor = 5;\n"
                        + "r.push(o.hasOwnProperty('constructor'), o.constructor);\n"
                        + "r.push(Iterator.prototype[Symbol.toStringTag]);\n"
                        + "r.join('|')");
    }

    @Test
    public void legacyIteratorBelowEcmascript() {
        Utils.assertWithAllModes_1_8("undefined", "typeof Iterator.prototype.map");
        Utils.assertWithAllModes_ES6("undefined", "typeof Iterator.prototype.map");
    }
}
