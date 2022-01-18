package org.mozilla.javascript.tests.es5;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/**
 * @see <a
 *     href="https://github.com/mozilla/rhino/issues/673">https://github.com/mozilla/rhino/issues/673</a>
 */
public class NativeArrayTest {

    private Context cx;
    private ScriptableObject scope;

    @Before
    public void setUp() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_1_8);
        scope = cx.initStandardObjects();
    }

    @After
    public void tearDown() {
        Context.exit();
    }

    @Test
    public void testConcatNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.concat.call(null, [1]);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testCopyWithinNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.copyWithin.call(null, 1, 3);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testEntriesNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.entries.call(null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testEveryNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.every.call(null, null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testFillNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.fill.call(null, 0, 2, 4);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testFilterNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.filter.call(null, null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testFindNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.find.call(null, null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testFindIndexNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.findIndex.call(null, null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testIncludesNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.includes.call(null, 1);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testIndexOfNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.indexOf.call(null, 1);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testJoinNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.join.call(null, 1);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testKeysNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.keys.call(null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testLastIndexOfNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.lastIndexOf.call(null, 1);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testMapNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.map.call(null, null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testPopNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.map.call(null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testPushNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.push.call(null, 1);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testReduceNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.reduce.call(null, null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testReduceRightNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.reduceRight.call(null, null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testReverseNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.reverse.call(null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testShiftNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.shift.call(null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testSliceNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.slice.call(null, 7);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testSomeNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.some.call(null, null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testSortNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.sort.call(null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testSpliceNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.splice.call(null, 1, 0, '#');"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testToLocaleStringNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.toLocaleString.call(null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testToSourceNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.toSource.call(null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testToStringNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.toString.call(null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testUnshiftNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.unshift.call(null, 4, 5);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }

    @Test
    public void testValuesNull() {
        Object result =
                cx.evaluateString(
                        scope,
                        "try { "
                                + "  Array.prototype.values.call(null);"
                                + "} catch (e) { e.message }",
                        "test",
                        1,
                        null);
        assertEquals("Cannot convert null to an object.", result);
    }
}
