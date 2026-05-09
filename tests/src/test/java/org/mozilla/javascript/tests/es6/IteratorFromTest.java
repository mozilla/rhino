/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

public class IteratorFromTest {

    @Test
    public void iteratorIsGlobal() {
        Utils.assertWithAllModes_ES6("function", "typeof Iterator");
    }

    @Test
    public void fromIsFunction() {
        Utils.assertWithAllModes_ES6("function", "typeof Iterator.from");
    }

    @Test
    public void fromReturnsSameIteratorWhenAlreadyIterator() {
        String code =
                "var arr = [1, 2, 3];\n"
                        + "var it = arr[Symbol.iterator]();\n"
                        + "Iterator.from(it) === it;\n";
        Utils.assertWithAllModes_ES6(Boolean.TRUE, code);
    }

    @Test
    public void fromIterableArrayProducesValues() {
        String code =
                "var it = Iterator.from([10, 20, 30]);\n"
                        + "var a = it.next();\n"
                        + "var b = it.next();\n"
                        + "var c = it.next();\n"
                        + "var d = it.next();\n"
                        + "a.value + ':' + a.done + ',' + b.value + ':' + b.done"
                        + " + ',' + c.value + ':' + c.done"
                        + " + ',' + d.value + ':' + d.done;\n";
        Utils.assertWithAllModes_ES6("10:false,20:false,30:false,undefined:true", code);
    }

    @Test
    public void fromStringFlattensToCharacters() {
        String code =
                "var it = Iterator.from('ab');\n"
                        + "var a = it.next();\n"
                        + "var b = it.next();\n"
                        + "var c = it.next();\n"
                        + "a.value + ',' + b.value + ',' + c.done;\n";
        Utils.assertWithAllModes_ES6("a,b,true", code);
    }

    @Test
    public void fromObjectWithOwnNextIsWrapped() {
        // Object has no @@iterator but has a callable `next`; spec's
        // GetIteratorFlattenable treats the object itself as the iterator.
        String code =
                "var i = 0;\n"
                        + "var raw = { next: function() {\n"
                        + "    return i < 2 ? { value: i++, done: false }\n"
                        + "                 : { value: undefined, done: true };\n"
                        + "}};\n"
                        + "var it = Iterator.from(raw);\n"
                        + "it !== raw && it.next().value === 0 && it.next().value === 1"
                        + " && it.next().done === true;\n";
        Utils.assertWithAllModes_ES6(Boolean.TRUE, code);
    }

    @Test
    public void fromIsIterableItself() {
        String code = "var it = Iterator.from([1, 2]);\n" + "it[Symbol.iterator]() === it;\n";
        Utils.assertWithAllModes_ES6(Boolean.TRUE, code);
    }

    @Test
    public void fromNullThrows() {
        Utils.assertEcmaErrorES6("TypeError: null is not iterable", "Iterator.from(null);");
    }

    @Test
    public void fromUndefinedThrows() {
        Utils.assertEcmaErrorES6("TypeError: undefined is not iterable", "Iterator.from();");
    }

    @Test
    public void fromNumberThrows() {
        Utils.assertEcmaErrorES6("TypeError: 42 is not iterable", "Iterator.from(42);");
    }

    @Test
    public void iteratorCtorCannotBeCalledDirectly() {
        Utils.assertEcmaErrorES6(
                "TypeError: \"Constructor Iterator\" may only be invoked from a \"new\" expression.",
                "Iterator();");
    }

    @Test
    public void iteratorCtorCannotBeConstructedDirectly() {
        Utils.assertEcmaErrorES6(
                "TypeError: Abstract class Iterator not directly constructable.",
                "new Iterator();");
    }

    @Test
    public void arrayIteratorInheritsFromIteratorPrototype() {
        Utils.assertWithAllModes_ES6(
                Boolean.TRUE, "Iterator.prototype.isPrototypeOf([1, 2].values());");
    }

    @Test
    public void stringIteratorInheritsFromIteratorPrototype() {
        Utils.assertWithAllModes_ES6(
                Boolean.TRUE, "Iterator.prototype.isPrototypeOf('ab'[Symbol.iterator]());");
    }

    @Test
    public void mapIteratorInheritsFromIteratorPrototype() {
        Utils.assertWithAllModes_ES6(
                Boolean.TRUE, "Iterator.prototype.isPrototypeOf(new Map().entries());");
    }

    @Test
    public void setIteratorInheritsFromIteratorPrototype() {
        Utils.assertWithAllModes_ES6(
                Boolean.TRUE, "Iterator.prototype.isPrototypeOf(new Set().values());");
    }

    @Test
    public void generatorInheritsFromIteratorPrototype() {
        Utils.assertWithAllModes_ES6(
                Boolean.TRUE,
                "function* gen() { yield 1; }\n" + "Iterator.prototype.isPrototypeOf(gen());");
    }

    @Test
    public void fromResultInheritsFromIteratorPrototype() {
        Utils.assertWithAllModes_ES6(
                Boolean.TRUE,
                "Iterator.prototype.isPrototypeOf(Iterator.from({next: function(){return {done:true}}}));");
    }
}
