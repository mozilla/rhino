package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

/**
 * Regression test for Object.assign with array target.
 *
 * <p>Bug: In Rhino 1.7.15+ (including 1.9.1), {@code Object.assign([], src)} returns an empty
 * array instead of a copy of src's enumerable own properties. The integer-keyed property writes
 * go through {@code AbstractEcmaObjectOperations.put} → {@code ScriptableObject.putOwnProperty} →
 * {@code putImpl}, which bypasses {@code NativeArray.put(int)} and so the array's length is never
 * updated. The slots are stored but invisible to iteration and serialization.
 *
 * <p>Upstream issue: mozilla/rhino#2126
 *
 * <p>The patch special-cases array targets in {@code NativeObject.js_assign} to route writes
 * through {@code targetObj.put(int, ...)} so length is maintained.
 */
public class ObjectAssignArrayTargetTest {

    /** Helper: run a script in interpreter mode (rhinoOptimizationLevel=-1, as used in AppServer). */
    private static Object eval(final String script) {
        ContextFactory factory = new ContextFactory();
        return factory.call(cx -> {
            cx.setOptimizationLevel(-1);
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();
            return cx.evaluateString(scope, script, "test.js", 1, null);
        });
    }

    // ---------------------------------------------------------------------
    // The original bug report (QUAL-3295 Major Bug 4)
    // ---------------------------------------------------------------------

    @Test
    public void emptyArrayTarget_stringSource_jsonStringify() {
        // The exact reproducer from QUAL-3295:
        //   const arr = ["bla","blubb"]; const clone = Object.assign([], arr);
        //   JSON.stringify(clone) -> "["bla","blubb"]"  (was: "[]")
        Object result = eval(
                "const arr = ['bla','blubb'];"
                + "const clone = Object.assign([], arr);"
                + "JSON.stringify(clone);");
        assertEquals("[\"bla\",\"blubb\"]", result);
    }

    @Test
    public void emptyArrayTarget_lengthIsUpdated() {
        Object result = eval(
                "const clone = Object.assign([], ['a','b','c']);"
                + "clone.length;");
        assertEquals(3.0, ((Number) result).doubleValue(), 0.0);
    }

    @Test
    public void emptyArrayTarget_valuesAreAccessibleByIndex() {
        Object result = eval(
                "const clone = Object.assign([], ['a','b','c']);"
                + "clone[0] + ',' + clone[1] + ',' + clone[2];");
        assertEquals("a,b,c", result);
    }

    // ---------------------------------------------------------------------
    // Variants
    // ---------------------------------------------------------------------

    @Test
    public void preFilledArrayTarget_overwriteByIndex() {
        // Target ["x","y","z"], source ["a","b"] -> ["a","b","z"]
        Object result = eval(
                "const t = ['x','y','z'];"
                + "Object.assign(t, ['a','b']);"
                + "JSON.stringify(t);");
        assertEquals("[\"a\",\"b\",\"z\"]", result);
    }

    @Test
    public void preFilledArrayTarget_lengthGrowsWhenSourceLonger() {
        Object result = eval(
                "const t = ['x'];"
                + "Object.assign(t, ['a','b','c']);"
                + "t.length;");
        assertEquals(3.0, ((Number) result).doubleValue(), 0.0);
    }

    @Test
    public void arrayTarget_objectSourceWithStringIndices() {
        // {0:"a", 1:"b"} -> array indices via String key path
        Object result = eval(
                "const t = [];"
                + "Object.assign(t, {0:'a', 1:'b'});"
                + "JSON.stringify(t);");
        assertEquals("[\"a\",\"b\"]", result);
    }

    @Test
    public void arrayTarget_multipleSources() {
        Object result = eval(
                "const t = [];"
                + "Object.assign(t, ['a','b'], ['x','y','z']);"
                + "JSON.stringify(t);");
        // Second source overwrites the first by index
        assertEquals("[\"x\",\"y\",\"z\"]", result);
    }

    @Test
    public void arrayTarget_sparseSourceSkipsHoles() {
        // ["a", <hole>, "c"] - hole is NOT enumerable, should be skipped
        Object result = eval(
                "const src = ['a',,'c'];"
                + "const t = ['x','y','z','w'];"
                + "Object.assign(t, src);"
                + "JSON.stringify(t);");
        // src[1] is a hole -> t[1] stays 'y'
        assertEquals("[\"a\",\"y\",\"c\",\"w\"]", result);
    }

    @Test
    public void arrayTarget_nestedValuesAreShallowCopied() {
        Object result = eval(
                "const inner = {x:1};"
                + "const src = [inner];"
                + "const clone = Object.assign([], src);"
                + "clone[0] === inner;"); // same reference (shallow)
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void arrayTarget_arrayJoinWorksAfterAssign() {
        // join() walks 0..length-1 - direct regression test for length update
        Object result = eval(
                "const clone = Object.assign([], ['a','b','c']);"
                + "clone.join('|');");
        assertEquals("a|b|c", result);
    }

    @Test
    public void arrayTarget_forEachCallbackInvokedForEachElement() {
        // Array.prototype.forEach uses length - regression test
        Object result = eval(
                "const clone = Object.assign([], ['a','b','c']);"
                + "let count = 0;"
                + "clone.forEach(() => count++);"
                + "count;");
        assertEquals(3.0, ((Number) result).doubleValue(), 0.0);
    }

    // ---------------------------------------------------------------------
    // Object target (NOT array) - must remain unchanged by patch
    // ---------------------------------------------------------------------

    @Test
    public void objectTarget_arraySource_keepsExistingBehavior() {
        // {} <- ["a","b"]  => {0:"a", 1:"b"}, no length
        Object result = eval(
                "const o = Object.assign({}, ['a','b']);"
                + "o[0] + ',' + o[1] + ',' + (o.length === undefined);");
        assertEquals("a,b,true", result);
    }

    @Test
    public void objectTarget_objectSource_keepsExistingBehavior() {
        Object result = eval(
                "const o = Object.assign({}, {a:1, b:2});"
                + "o.a + o.b;");
        assertEquals(3.0, ((Number) result).doubleValue(), 0.0);
    }

    // ---------------------------------------------------------------------
    // Deeper / compound scenarios
    // ---------------------------------------------------------------------

    @Test
    public void arrayOfArrays_sourceContainsNestedArrays_outerLengthCorrect() {
        // Source has arrays as values - inner arrays are just refs, but
        // we want to verify the outer container is correctly built.
        Object result = eval(
                "const src = [[1,2],[3,4],[5,6]];"
                + "const clone = Object.assign([], src);"
                + "JSON.stringify(clone);");
        assertEquals("[[1,2],[3,4],[5,6]]", result);
    }

    @Test
    public void recursiveDeepClone_viaMapAndAssign() {
        // A common pattern: shallow-clone every nested array via map+assign.
        // Doubly exercises the patch path (one assign per nested array).
        Object result = eval(
                "const src = [[1,2],[3,4]];"
                + "const deepish = src.map(a => Object.assign([], a));"
                + "JSON.stringify(deepish) + '|' + (deepish[0] !== src[0]);");
        assertEquals("[[1,2],[3,4]]|true", result);
    }

    @Test
    public void sequentialAssigns_lengthAccumulates() {
        // Each assign on the same target should leave length consistent.
        Object result = eval(
                "const t = [];"
                + "Object.assign(t, ['a','b']);"
                + "Object.assign(t, [, , 'c', 'd']);" // holes preserve a,b
                + "JSON.stringify(t) + '|' + t.length;");
        assertEquals("[\"a\",\"b\",\"c\",\"d\"]|4", result);
    }

    @Test
    public void assignThenPushThenAssign_arrayProtocolIntact() {
        // Mix Object.assign with normal Array.prototype.push to ensure
        // length is not just "set" but really maintained as a writable property.
        Object result = eval(
                "const t = Object.assign([], ['a','b']);"
                + "t.push('c');"
                + "Object.assign(t, [, , , 'd']);"
                + "JSON.stringify(t) + '|' + t.length;");
        assertEquals("[\"a\",\"b\",\"c\",\"d\"]|4", result);
    }

    @Test
    public void chainedAssignReturnsTarget_usableAsArray() {
        // Object.assign returns the target. Chain it through array methods.
        Object result = eval(
                "Object.assign([], ['a','b','c'])"
                + "  .map(s => s.toUpperCase())"
                + "  .filter(s => s !== 'B')"
                + "  .join('-');");
        assertEquals("A-C", result);
    }

    @Test
    public void preSizedArrayViaConstructor_overwriteAndExtend() {
        // new Array(n) sets length without filling slots. Assign should
        // overwrite the placeholder length and report real new length.
        Object result = eval(
                "const t = new Array(5);"
                + "Object.assign(t, ['a','b']);"
                + "JSON.stringify(t) + '|' + t.length;");
        // Original length was 5 (with holes); assign writes indices 0,1.
        // length must stay 5 (assign never shrinks).
        assertEquals("[\"a\",\"b\",null,null,null]|5", result);
    }

    @Test
    public void hundredEntriesViaAssign_lengthStaysAccurate() {
        // Volume test: many indices in one assign call. Catches off-by-one
        // and intermediate-state bugs that small arrays would hide.
        Object result = eval(
                "const big = [];"
                + "for (let i = 0; i < 100; i++) big.push(i);"
                + "const clone = Object.assign([], big);"
                + "clone.length + '|' + clone[0] + '|' + clone[99] + '|' + clone.reduce((a,b)=>a+b, 0);");
        // sum 0..99 = 4950
        assertEquals("100|0|99|4950", result);
    }

    @Test
    public void assignFromArrayIntoSpliceResult_chainedArrayOperations() {
        // splice returns an array - feed it as source. Compound test:
        // source itself is dynamically produced by an array op.
        Object result = eval(
                "const src = ['x','a','b','c','y'];"
                + "const removed = src.splice(1, 3);" // ['a','b','c']
                + "const clone = Object.assign([], removed);"
                + "JSON.stringify(clone) + '|' + clone.length;");
        assertEquals("[\"a\",\"b\",\"c\"]|3", result);
    }

    @Test
    public void nestedObjectAssign_arraysAtMultipleLevels() {
        // Build an object whose properties are arrays-via-Object.assign,
        // then JSON-stringify the whole thing. If any layer's length is
        // wrong, the JSON output goes sideways.
        Object result = eval(
                "const obj = {"
                + "  a: Object.assign([], ['1','2']),"
                + "  b: Object.assign([], [Object.assign([], ['x','y']), 'z'])"
                + "};"
                + "JSON.stringify(obj);");
        assertEquals("{\"a\":[\"1\",\"2\"],\"b\":[[\"x\",\"y\"],\"z\"]}", result);
    }

    // ---------------------------------------------------------------------
    // Destructuring & spread — formally not Object.assign, but they share
    // the indexed-property write path in Rhino's internals. If the same
    // length-update bug applies there, these tests will catch it.
    // ---------------------------------------------------------------------

    @Test
    public void arraySpread_intoArrayLiteral_lengthCorrect() {
        Object result = eval(
                "const src = ['a','b','c'];"
                + "const out = [...src];"
                + "JSON.stringify(out) + '|' + out.length;");
        assertEquals("[\"a\",\"b\",\"c\"]|3", result);
    }

    @Test
    public void arraySpread_withPrefixAndSuffix() {
        Object result = eval(
                "const src = ['b','c'];"
                + "const out = ['a', ...src, 'd'];"
                + "JSON.stringify(out) + '|' + out.length;");
        assertEquals("[\"a\",\"b\",\"c\",\"d\"]|4", result);
    }

    @Test
    public void arrayDestructuring_basicPositional() {
        // Plain positional destructuring - no rest. Should work and not
        // share the buggy assign-path.
        Object result = eval(
                "const [a, b, c] = ['x','y','z'];"
                + "a + '|' + b + '|' + c;");
        assertEquals("x|y|z", result);
    }

    @Test
    public void arrayDestructuring_withSkipsAndDefaults() {
        Object result = eval(
                "const [, b, , d = 'def'] = ['a','b','c'];"
                + "b + '|' + d;");
        assertEquals("b|def", result);
    }

    // NOTE: Array rest pattern (const [a, ...rest] = ...) and
    // object rest pattern (const {a, ...rest} = ...) are not supported
    // by the Rhino 1.9.1 parser - "Invalid assignment left-hand side"
    // resp. "object rest properties in destructuring are not supported".
    // Since they can't be written in user code, they can't trigger any
    // bug either. No test needed.

    @Test
    public void objectSpread_intoObjectLiteral_keepsExistingBehavior() {
        // {...src} uses CopyDataProperties — target is Object, not Array,
        // so our patch path is not triggered. Should still work.
        Object result = eval(
                "const src = {a:1, b:2};"
                + "const out = {...src, c:3};"
                + "out.a + out.b + out.c;");
        assertEquals(6.0, ((Number) result).doubleValue(), 0.0);
    }

    @Test
    public void objectSpread_arraySourceAsObject() {
        // {...arr} — array as source, object as target. Indices become keys.
        Object result = eval(
                "const out = {...['a','b']};"
                + "out[0] + ',' + out[1];");
        assertEquals("a,b", result);
    }

    @Test
    public void objectDestructuring_basicNoRest() {
        Object result = eval(
                "const {a, b} = {a:1, b:2, c:3};"
                + "a + '|' + b;");
        assertEquals("1|2", result);
    }

    @Test
    public void arrayFrom_iterableSource() {
        // Array.from also relies on integer-indexed property writes
        Object result = eval(
                "const out = Array.from(['a','b','c']);"
                + "JSON.stringify(out) + '|' + out.length;");
        assertEquals("[\"a\",\"b\",\"c\"]|3", result);
    }

    @Test
    public void arrayOf_variadicArgs() {
        Object result = eval(
                "const out = Array.of('a','b','c');"
                + "JSON.stringify(out) + '|' + out.length;");
        assertEquals("[\"a\",\"b\",\"c\"]|3", result);
    }

    @Test
    public void spreadOfAssignedArray_intoAnotherArray() {
        // The assigned array must behave like a normal array when spread.
        // Length-bug would cause [...clone] to be empty.
        Object result = eval(
                "const clone = Object.assign([], ['a','b','c']);"
                + "const spread = [0, ...clone, 4];"
                + "JSON.stringify(spread);");
        assertEquals("[0,\"a\",\"b\",\"c\",4]", result);
    }
}
