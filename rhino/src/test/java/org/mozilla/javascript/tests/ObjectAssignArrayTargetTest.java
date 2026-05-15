package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Regression test for Object.assign with array target.
 *
 * <p>Issue: {@code Object.assign([], src)} returned an empty array instead of a copy of src's
 * enumerable own properties. The integer-keyed property writes went through {@code
 * AbstractEcmaObjectOperations.put} -&gt; {@code ScriptableObject.putOwnProperty} -&gt; {@code
 * putImpl}, which bypasses {@code NativeArray.put(int)} and so the array's length was never
 * updated. The slots got stored but stayed invisible to iteration and serialization.
 *
 * <p>Upstream issue: <a href="https://github.com/mozilla/rhino/issues/2126">mozilla/rhino#2126</a>.
 *
 * <p>The fix special-cases NativeArray targets in {@code NativeObject.js_assign} to route writes
 * through {@code targetObj.put(...)} so length is maintained.
 */
public class ObjectAssignArrayTargetTest {

    // ---------------------------------------------------------------------
    // The original bug report
    // ---------------------------------------------------------------------

    @Test
    public void emptyArrayTarget_stringSource_jsonStringify() {
        Utils.assertWithAllModes_ES6(
                "[\"bla\",\"blubb\"]",
                "const arr = ['bla','blubb'];"
                        + "const clone = Object.assign([], arr);"
                        + "JSON.stringify(clone);");
    }

    @Test
    public void emptyArrayTarget_lengthIsUpdated() {
        Utils.assertWithAllModes_ES6(
                "3", "const clone = Object.assign([], ['a','b','c']);" + "'' + clone.length;");
    }

    @Test
    public void emptyArrayTarget_valuesAreAccessibleByIndex() {
        Utils.assertWithAllModes_ES6(
                "a,b,c",
                "const clone = Object.assign([], ['a','b','c']);"
                        + "clone[0] + ',' + clone[1] + ',' + clone[2];");
    }

    // ---------------------------------------------------------------------
    // Variants
    // ---------------------------------------------------------------------

    @Test
    public void issueReproducer_sameLengthPreFilledTarget() {
        // The exact reproducer from issue #2126:
        //   let src = [1,2,4]; let tgt = [4,5,7]; Object.assign(tgt, src);
        // Before the fix, tgt stayed [4,5,7] - values were not overwritten,
        // because the indexed writes never made it past the slot map and
        // through NativeArray.put(int).
        Utils.assertWithAllModes_ES6(
                "[1,2,4]",
                "const src = [1,2,4];"
                        + "const tgt = [4,5,7];"
                        + "Object.assign(tgt, src);"
                        + "JSON.stringify(tgt);");
    }

    @Test
    public void preFilledArrayTarget_overwriteByIndex() {
        Utils.assertWithAllModes_ES6(
                "[\"a\",\"b\",\"z\"]",
                "const t = ['x','y','z'];" + "Object.assign(t, ['a','b']);" + "JSON.stringify(t);");
    }

    @Test
    public void preFilledArrayTarget_lengthGrowsWhenSourceLonger() {
        Utils.assertWithAllModes_ES6(
                "3", "const t = ['x'];" + "Object.assign(t, ['a','b','c']);" + "'' + t.length;");
    }

    @Test
    public void arrayTarget_objectSourceWithStringIndices() {
        Utils.assertWithAllModes_ES6(
                "[\"a\",\"b\"]",
                "const t = [];" + "Object.assign(t, {0:'a', 1:'b'});" + "JSON.stringify(t);");
    }

    @Test
    public void arrayTarget_multipleSources() {
        Utils.assertWithAllModes_ES6(
                "[\"x\",\"y\",\"z\"]",
                "const t = [];"
                        + "Object.assign(t, ['a','b'], ['x','y','z']);"
                        + "JSON.stringify(t);");
    }

    @Test
    public void arrayTarget_sparseSourceSkipsHoles() {
        Utils.assertWithAllModes_ES6(
                "[\"a\",\"y\",\"c\",\"w\"]",
                "const src = ['a',,'c'];"
                        + "const t = ['x','y','z','w'];"
                        + "Object.assign(t, src);"
                        + "JSON.stringify(t);");
    }

    @Test
    public void arrayTarget_nestedValuesAreShallowCopied() {
        Utils.assertWithAllModes_ES6(
                Boolean.TRUE,
                "const inner = {x:1};"
                        + "const src = [inner];"
                        + "const clone = Object.assign([], src);"
                        + "clone[0] === inner;");
    }

    @Test
    public void arrayTarget_arrayJoinWorksAfterAssign() {
        Utils.assertWithAllModes_ES6(
                "a|b|c", "const clone = Object.assign([], ['a','b','c']);" + "clone.join('|');");
    }

    @Test
    public void arrayTarget_forEachCallbackInvokedForEachElement() {
        Utils.assertWithAllModes_ES6(
                "3",
                "const clone = Object.assign([], ['a','b','c']);"
                        + "let count = 0;"
                        + "clone.forEach(() => count++);"
                        + "'' + count;");
    }

    // ---------------------------------------------------------------------
    // Object target (not array) - must remain unchanged by the fix
    // ---------------------------------------------------------------------

    @Test
    public void objectTarget_arraySource_keepsExistingBehavior() {
        Utils.assertWithAllModes_ES6(
                "a,b,true",
                "const o = Object.assign({}, ['a','b']);"
                        + "o[0] + ',' + o[1] + ',' + (o.length === undefined);");
    }

    @Test
    public void objectTarget_objectSource_keepsExistingBehavior() {
        Utils.assertWithAllModes_ES6(
                "3", "const o = Object.assign({}, {a:1, b:2});" + "'' + (o.a + o.b);");
    }

    // ---------------------------------------------------------------------
    // Deeper / compound scenarios
    // ---------------------------------------------------------------------

    @Test
    public void arrayOfArrays_sourceContainsNestedArrays_outerLengthCorrect() {
        Utils.assertWithAllModes_ES6(
                "[[1,2],[3,4],[5,6]]",
                "const src = [[1,2],[3,4],[5,6]];"
                        + "const clone = Object.assign([], src);"
                        + "JSON.stringify(clone);");
    }

    @Test
    public void recursiveDeepClone_viaMapAndAssign() {
        Utils.assertWithAllModes_ES6(
                "[[1,2],[3,4]]|true",
                "const src = [[1,2],[3,4]];"
                        + "const deepish = src.map(a => Object.assign([], a));"
                        + "JSON.stringify(deepish) + '|' + (deepish[0] !== src[0]);");
    }

    @Test
    public void sequentialAssigns_lengthAccumulates() {
        Utils.assertWithAllModes_ES6(
                "[\"a\",\"b\",\"c\",\"d\"]|4",
                "const t = [];"
                        + "Object.assign(t, ['a','b']);"
                        + "Object.assign(t, [, , 'c', 'd']);"
                        + "JSON.stringify(t) + '|' + t.length;");
    }

    @Test
    public void assignThenPushThenAssign_arrayProtocolIntact() {
        Utils.assertWithAllModes_ES6(
                "[\"a\",\"b\",\"c\",\"d\"]|4",
                "const t = Object.assign([], ['a','b']);"
                        + "t.push('c');"
                        + "Object.assign(t, [, , , 'd']);"
                        + "JSON.stringify(t) + '|' + t.length;");
    }

    @Test
    public void chainedAssignReturnsTarget_usableAsArray() {
        Utils.assertWithAllModes_ES6(
                "A-C",
                "Object.assign([], ['a','b','c'])"
                        + "  .map(s => s.toUpperCase())"
                        + "  .filter(s => s !== 'B')"
                        + "  .join('-');");
    }

    @Test
    public void preSizedArrayViaConstructor_overwriteAndExtend() {
        // new Array(5) sets length to 5 with holes; assign writes indices 0,1.
        // length must stay 5 (assign never shrinks).
        Utils.assertWithAllModes_ES6(
                "[\"a\",\"b\",null,null,null]|5",
                "const t = new Array(5);"
                        + "Object.assign(t, ['a','b']);"
                        + "JSON.stringify(t) + '|' + t.length;");
    }

    @Test
    public void hundredEntriesViaAssign_lengthStaysAccurate() {
        // sum 0..99 = 4950
        Utils.assertWithAllModes_ES6(
                "100|0|99|4950",
                "const big = [];"
                        + "for (let i = 0; i < 100; i++) big.push(i);"
                        + "const clone = Object.assign([], big);"
                        + "clone.length + '|' + clone[0] + '|' + clone[99]"
                        + "  + '|' + clone.reduce((a,b)=>a+b, 0);");
    }

    @Test
    public void assignFromArrayIntoSpliceResult_chainedArrayOperations() {
        Utils.assertWithAllModes_ES6(
                "[\"a\",\"b\",\"c\"]|3",
                "const src = ['x','a','b','c','y'];"
                        + "const removed = src.splice(1, 3);"
                        + "const clone = Object.assign([], removed);"
                        + "JSON.stringify(clone) + '|' + clone.length;");
    }

    @Test
    public void nestedObjectAssign_arraysAtMultipleLevels() {
        Utils.assertWithAllModes_ES6(
                "{\"a\":[\"1\",\"2\"],\"b\":[[\"x\",\"y\"],\"z\"]}",
                "const obj = {"
                        + "  a: Object.assign([], ['1','2']),"
                        + "  b: Object.assign([], [Object.assign([], ['x','y']), 'z'])"
                        + "};"
                        + "JSON.stringify(obj);");
    }

    @Test
    public void spreadOfAssignedArray_intoAnotherArray() {
        Utils.assertWithAllModes_ES6(
                "[0,\"a\",\"b\",\"c\",4]",
                "const clone = Object.assign([], ['a','b','c']);"
                        + "const spread = [0, ...clone, 4];"
                        + "JSON.stringify(spread);");
    }

    // ---------------------------------------------------------------------
    // Destructuring & spread - related code paths
    // (Array-rest and object-rest in destructuring are NOT supported by
    // the Rhino parser yet, so they cannot be exercised.)
    // ---------------------------------------------------------------------

    @Test
    public void arraySpread_intoArrayLiteral_lengthCorrect() {
        Utils.assertWithAllModes_ES6(
                "[\"a\",\"b\",\"c\"]|3",
                "const src = ['a','b','c'];"
                        + "const out = [...src];"
                        + "JSON.stringify(out) + '|' + out.length;");
    }

    @Test
    public void arraySpread_withPrefixAndSuffix() {
        Utils.assertWithAllModes_ES6(
                "[\"a\",\"b\",\"c\",\"d\"]|4",
                "const src = ['b','c'];"
                        + "const out = ['a', ...src, 'd'];"
                        + "JSON.stringify(out) + '|' + out.length;");
    }

    @Test
    public void arrayDestructuring_basicPositional() {
        Utils.assertWithAllModes_ES6(
                "x|y|z", "const [a, b, c] = ['x','y','z'];" + "a + '|' + b + '|' + c;");
    }

    @Test
    public void arrayDestructuring_withSkipsAndDefaults() {
        Utils.assertWithAllModes_ES6(
                "b|def", "const [, b, , d = 'def'] = ['a','b','c'];" + "b + '|' + d;");
    }

    @Test
    public void objectDestructuring_basicNoRest() {
        Utils.assertWithAllModes_ES6("1|2", "const {a, b} = {a:1, b:2, c:3};" + "a + '|' + b;");
    }

    @Test
    public void objectSpread_intoObjectLiteral_keepsExistingBehavior() {
        Utils.assertWithAllModes_ES6(
                "6",
                "const src = {a:1, b:2};"
                        + "const out = {...src, c:3};"
                        + "'' + (out.a + out.b + out.c);");
    }

    @Test
    public void objectSpread_arraySourceAsObject() {
        Utils.assertWithAllModes_ES6(
                "a,b", "const out = {...['a','b']};" + "out[0] + ',' + out[1];");
    }

    @Test
    public void arrayFrom_iterableSource() {
        Utils.assertWithAllModes_ES6(
                "[\"a\",\"b\",\"c\"]|3",
                "const out = Array.from(['a','b','c']);"
                        + "JSON.stringify(out) + '|' + out.length;");
    }

    @Test
    public void arrayOf_variadicArgs() {
        Utils.assertWithAllModes_ES6(
                "[\"a\",\"b\",\"c\"]|3",
                "const out = Array.of('a','b','c');" + "JSON.stringify(out) + '|' + out.length;");
    }
}
