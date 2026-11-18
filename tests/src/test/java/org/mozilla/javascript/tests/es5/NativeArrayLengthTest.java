/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es5;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * The tests are based on the array exotic object definition of [[DefineOwnProperty]]: an index that
 * is not less than "length" can only be created when the object is extensible and "length" is
 * writable, and "length" is only raised after the element was defined successfully. The methods of
 * Array.prototype that end with Set(O, "length", ..., true) — push, pop, shift, unshift and splice
 * — therefore throw a TypeError for an array with a non-writable "length", while assignments to
 * existing elements keep working.
 *
 * <p>Each assertion reports "errorName:length:elements" for an operation applied to an array with a
 * non-writable "length".
 *
 * <p>See <a href="https://github.com/mozilla/rhino/issues/2591">issue 2591</a>.
 */
public class NativeArrayLengthTest {

    private static final String SETUP =
            "var array = Object.defineProperty([7, 8], 'length', { writable: false });\n";

    private static String outcomeScript(String statement, boolean strict) {
        String body =
                "var name;\n"
                        + "try { "
                        + statement
                        + " name = 'no-error'; } catch (error) { name = error.name; }\n";
        String result = "name + ':' + array.length + ':' + array.join() + ':' + (2 in array)";
        if (strict) {
            return "(function () { 'use strict';\n"
                    + SETUP
                    + body
                    + "return "
                    + result
                    + ";\n"
                    + "})();\n";
        }
        return SETUP + body + result + ";\n";
    }

    private static void assertOutcome(String expected, String statement, boolean strict) {
        Utils.assertWithAllModes_ES6(expected, outcomeScript(statement, strict));
    }

    @Test
    public void pushThrowsAndChangesNothing() {
        // Array.prototype.push creates the element at the current length, which fails, and
        // ends with Set(O, "length", ..., true)
        assertOutcome("TypeError:2:7,8:false", "array.push(1);", false);
        assertOutcome("TypeError:2:7,8:false", "array.push(1);", true);
    }

    @Test
    public void pushWithoutArgumentsThrows() {
        // Even an empty push ends with Set(O, "length", len, true)
        assertOutcome("TypeError:2:7,8:false", "array.push();", false);
        assertOutcome("TypeError:2:7,8:false", "array.push();", true);
    }

    @Test
    public void unshiftThrowsAndChangesNothing() {
        assertOutcome("TypeError:2:7,8:false", "array.unshift(1);", false);
        assertOutcome("TypeError:2:7,8:false", "array.unshift(1);", true);
    }

    @Test
    public void popRemovesElementButThrowsAndKeepsLength() {
        // The element is deleted before the final Set(O, "length", ..., true)
        assertOutcome("TypeError:2:7,:false", "array.pop();", false);
        assertOutcome("TypeError:2:7,:false", "array.pop();", true);
    }

    @Test
    public void popOnEmptyArrayThrows() {
        // Pop on an empty array still performs Set(O, "length", 0, true)
        String script =
                "(function () {\n"
                        + "var array = Object.defineProperty([], 'length', { writable: false });\n"
                        + "try { array.pop(); return 'no-error'; } catch (error) { return error.name; }\n"
                        + "})();";
        Utils.assertWithAllModes_ES6("TypeError", script);
        String strictScript =
                "(function () { 'use strict';\n"
                        + "var array = Object.defineProperty([], 'length', { writable: false });\n"
                        + "try { array.pop(); return 'no-error'; } catch (error) { return error.name; }\n"
                        + "})();";
        Utils.assertWithAllModes_ES6("TypeError", strictScript);
    }

    @Test
    public void shiftRemovesElementButThrowsAndKeepsLength() {
        assertOutcome("TypeError:2:8,:false", "array.shift();", false);
        assertOutcome("TypeError:2:8,:false", "array.shift();", true);
    }

    @Test
    public void spliceShrinkingThrowsAndKeepsLength() {
        // splice(0, 1) moves element 1 to index 0 and deletes index 1, then fails to
        // update "length"
        assertOutcome("TypeError:2:8,:false", "array.splice(0, 1);", false);
        assertOutcome("TypeError:2:8,:false", "array.splice(0, 1);", true);
    }

    @Test
    public void spliceReplacingThrowsAndKeepsLength() {
        // splice(0, 1, 9) replaces element 0, which succeeds, and then fails to update
        // "length"
        assertOutcome("TypeError:2:9,8:false", "array.splice(0, 1, 9);", false);
        assertOutcome("TypeError:2:9,8:false", "array.splice(0, 1, 9);", true);
    }

    @Test
    public void spliceExpandingThrowsAndChangesNothing() {
        // splice(0, 0, 9) creates an element at an index that is not less than "length",
        // which throws before the array is modified
        assertOutcome("TypeError:2:7,8:false", "array.splice(0, 0, 9);", false);
        assertOutcome("TypeError:2:7,8:false", "array.splice(0, 0, 9);", true);
    }

    @Test
    public void assignmentBeyondLengthIsIgnoredInSloppyMode() {
        assertOutcome("no-error:2:7,8:false", "array[2] = 1;", false);
    }

    @Test
    public void assignmentBeyondLengthThrowsInStrictMode() {
        assertOutcome("TypeError:2:7,8:false", "array[2] = 1;", true);
    }

    @Test
    public void stringAssignmentBeyondLengthBehavesLikeIndexedAssignment() {
        assertOutcome("no-error:2:7,8:false", "array['2'] = 1;", false);
        assertOutcome("TypeError:2:7,8:false", "array['2'] = 1;", true);
    }

    @Test
    public void assignmentToExistingElementKeepsWorking() {
        // The elements themselves stay writable; only growing the array is blocked
        String script =
                SETUP + "array[0] = 9; array.join() + ':' + array.length + ':' + array[0];\n";
        Utils.assertWithAllModes_ES6("9,8:2:9", script);
    }

    @Test
    public void definePropertyBeyondLengthThrowsAndKeepsLength() {
        String script =
                SETUP
                        + "var name;\n"
                        + "try { Object.defineProperty(array, 2, { value: 1 }); name = 'no-error'; }\n"
                        + "catch (error) { name = error.name; }\n"
                        + "name + ':' + array.length + ':' + (2 in array);\n";
        Utils.assertWithAllModes_ES6("TypeError:2:false", script);
    }

    @Test
    public void definePropertyBeyondLengthOnFrozenArrayThrowsAndKeepsLength() {
        // The failure must not leave a raised "length" behind, even though the array is
        // not extensible
        String script =
                "var frozen = Object.freeze([7, 8]);\n"
                        + "var name;\n"
                        + "try { Object.defineProperty(frozen, 2, { value: 1 }); name = 'no-error'; }\n"
                        + "catch (error) { name = error.name; }\n"
                        + "name + ':' + frozen.length;\n";
        Utils.assertWithAllModes_ES6("TypeError:2", script);
    }

    @Test
    public void definePropertyWithinLengthKeepsWorking() {
        String script =
                SETUP
                        + "Object.defineProperty(array, 0, { value: 1 });\n"
                        + "array[0] + ':' + array.length;\n";
        Utils.assertWithAllModes_ES6("1:2", script);
    }

    @Test
    public void definePropertyBeyondLengthOnWritableArrayRaisesLength() {
        String script =
                "var array = [7, 8];\n"
                        + "Object.defineProperty(array, 5, { value: 1 });\n"
                        + "array.length + ':' + array[5];\n";
        Utils.assertWithAllModes_ES6("6:1", script);
    }

    @Test
    public void lengthValueChangeOnNonWritableLengthThrows() {
        String script =
                SETUP
                        + "var name;\n"
                        + "try { Object.defineProperty(array, 'length', { value: 5 }); name = 'no-error'; }\n"
                        + "catch (error) { name = error.name; }\n"
                        + "name + ':' + array.length;\n";
        Utils.assertWithAllModes_ES6("TypeError:2", script);
    }

    @Test
    public void sparseArrayAssignmentBeyondNonWritableLengthIsIgnored() {
        String sloppy =
                "var array = [7, 8];\n"
                        + "array[100] = 1; // move the array out of dense mode\n"
                        + "Object.defineProperty(array, 'length', { writable: false });\n"
                        + "var name;\n"
                        + "try { array[101] = 1; name = 'no-error'; } catch (error) { name = error.name; }\n"
                        + "name + ':' + array.length + ':' + (101 in array);\n";
        Utils.assertWithAllModes_ES6("no-error:101:false", sloppy);
        String strict =
                "(function () { 'use strict';\n"
                        + "var array = [7, 8];\n"
                        + "array[100] = 1;\n"
                        + "Object.defineProperty(array, 'length', { writable: false });\n"
                        + "var name;\n"
                        + "try { array[101] = 1; name = 'no-error'; } catch (error) { name = error.name; }\n"
                        + "return name + ':' + array.length + ':' + (101 in array);\n"
                        + "})();";
        Utils.assertWithAllModes_ES6("TypeError:101:false", strict);
    }

    @Test
    public void issue2591Repro() {
        // The exact reproduction from the issue: none of the operations may change "length"
        String script =
                "function lengthAfter(operation) {\n"
                        + "  var array = Object.defineProperty([7, 8], 'length', { writable: false });\n"
                        + "  try { operation(array); } catch (error) { /* empty */ }\n"
                        + "  return array.length;\n"
                        + "}\n"
                        + "var result = '';\n"
                        + "result += lengthAfter(function (array) { array.push(1); }) + ',';\n"
                        + "result += lengthAfter(function (array) { array.unshift(1); }) + ',';\n"
                        + "result += lengthAfter(function (array) { array.pop(); }) + ',';\n"
                        + "result += lengthAfter(function (array) { array.shift(); }) + ',';\n"
                        + "result += lengthAfter(function (array) { array.splice(0, 1); }) + ',';\n"
                        + "result += lengthAfter(function (array) { array[2] = 1; }) + ',';\n"
                        + "result += lengthAfter(function (array) { Object.defineProperty(array, 2, { value: 1 }); }) + ',';\n"
                        + "var frozen = Object.freeze([7, 8]);\n"
                        + "try { Object.defineProperty(frozen, 2, { value: 1 }); } catch (error) { /* empty */ }\n"
                        + "result + frozen.length;\n";
        Utils.assertWithAllModes_ES6("2,2,2,2,2,2,2,2", script);
    }

    @Test
    public void writableArraysKeepWorking() {
        // Guard against regressions of the fast paths
        String script =
                "var array = [7, 8];\n"
                        + "var result = '';\n"
                        + "result += array.push(1) + ':' + array.join() + ' ';\n"
                        + "result += array.unshift(0) + ':' + array.join() + ' ';\n"
                        + "result += array.pop() + ':' + array.join() + ' ';\n"
                        + "result += array.shift() + ':' + array.join() + ' ';\n"
                        + "result += array.splice(1, 1, 5) + ':' + array.join() + ' ';\n"
                        + "result += (array[3] = 6);\n"
                        + "result;\n";
        Utils.assertWithAllModes_ES6("3:7,8,1 4:0,7,8,1 1:0,7,8 0:7,8 8:7,5 6", script);
    }
}
