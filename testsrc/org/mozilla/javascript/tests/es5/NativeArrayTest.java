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
    public void concatNull() {
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
    public void copyWithinNull() {
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
    public void entriesNull() {
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
    public void everyNull() {
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
    public void fillNull() {
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
    public void filterNull() {
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
    public void findNull() {
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
    public void findIndexNull() {
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
    public void includesNull() {
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
    public void indexOfNull() {
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
    public void joinNull() {
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
    public void keysNull() {
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
    public void lastIndexOfNull() {
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
    public void mapNull() {
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
    public void popNull() {
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
    public void pushNull() {
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
    public void reduceNull() {
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
    public void reduceRightNull() {
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
    public void reverseNull() {
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
    public void shiftNull() {
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
    public void sliceNull() {
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
    public void someNull() {
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
    public void sortNull() {
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
    public void spliceNull() {
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
    public void toLocaleStringNull() {
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
    public void toSourceNull() {
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
    public void toStringNull() {
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
    public void unshiftNull() {
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
    public void valuesNull() {
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
