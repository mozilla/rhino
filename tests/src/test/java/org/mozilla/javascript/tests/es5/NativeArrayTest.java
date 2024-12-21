package org.mozilla.javascript.tests.es5;

import org.junit.Test;
import org.mozilla.javascript.tests.Utils;

/**
 * @see <a
 *     href="https://github.com/mozilla/rhino/issues/673">https://github.com/mozilla/rhino/issues/673</a>
 */
public class NativeArrayTest {

    @Test
    public void concatNull() {
        final String script =
                "try { "
                        + "  Array.prototype.concat.call(null, [1]);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void copyWithinNull() {
        final String script =
                "try { "
                        + "  Array.prototype.copyWithin.call(null, 1, 3);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void entriesNull() {
        final String script =
                "try { " + "  Array.prototype.entries.call(null);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void everyNull() {
        final String script =
                "try { "
                        + "  Array.prototype.every.call(null, null);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void fillNull() {
        final String script =
                "try { "
                        + "  Array.prototype.fill.call(null, 0, 2, 4);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void filterNull() {
        final String script =
                "try { "
                        + "  Array.prototype.filter.call(null, null);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void findNull() {
        final String script =
                "try { " + "  Array.prototype.find.call(null, null);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void findIndexNull() {
        final String script =
                "try { "
                        + "  Array.prototype.findIndex.call(null, null);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void includesNull() {
        final String script =
                "try { "
                        + "  Array.prototype.includes.call(null, 1);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void indexOfNull() {
        final String script =
                "try { " + "  Array.prototype.indexOf.call(null, 1);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void joinNull() {
        final String script =
                "try { " + "  Array.prototype.join.call(null, 1);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void keysNull() {
        final String script =
                "try { " + "  Array.prototype.keys.call(null);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void lastIndexOfNull() {
        final String script =
                "try { "
                        + "  Array.prototype.lastIndexOf.call(null, 1);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void mapNull() {
        final String script =
                "try { " + "  Array.prototype.map.call(null, null);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void popNull() {
        final String script =
                "try { " + "  Array.prototype.map.call(null);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void pushNull() {
        final String script =
                "try { " + "  Array.prototype.push.call(null, 1);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void reduceNull() {
        final String script =
                "try { "
                        + "  Array.prototype.reduce.call(null, null);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void reduceRightNull() {
        final String script =
                "try { "
                        + "  Array.prototype.reduceRight.call(null, null);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void reverseNull() {
        final String script =
                "try { " + "  Array.prototype.reverse.call(null);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void shiftNull() {
        final String script =
                "try { " + "  Array.prototype.shift.call(null);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void sliceNull() {
        final String script =
                "try { " + "  Array.prototype.slice.call(null, 7);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void someNull() {
        final String script =
                "try { " + "  Array.prototype.some.call(null, null);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void sortNull() {
        final String script =
                "try { " + "  Array.prototype.sort.call(null);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void spliceNull() {
        final String script =
                "try { "
                        + "  Array.prototype.splice.call(null, 1, 0, '#');"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void toLocaleStringNull() {
        final String script =
                "try { "
                        + "  Array.prototype.toLocaleString.call(null);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void toSourceNull() {
        final String script =
                "try { " + "  Array.prototype.toSource.call(null);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void toStringNull() {
        final String script =
                "try { " + "  Array.prototype.toString.call(null);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void unshiftNull() {
        final String script =
                "try { "
                        + "  Array.prototype.unshift.call(null, 4, 5);"
                        + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }

    @Test
    public void valuesNull() {
        final String script =
                "try { " + "  Array.prototype.values.call(null);" + "} catch (e) { e.message }";
        Utils.assertWithAllModes_1_8("Cannot convert null to an object.", script);
    }
}
