/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2019;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Holes of an array returned by the {@code flatMap} callback are skipped. See
 * https://github.com/mozilla/rhino/issues/2588
 */
public class ArrayFlatMapHolesTest {

    private static final String SHOW =
            "function show(a) {\n"
                    + "  var parts = [];\n"
                    + "  for (var i = 0; i < a.length; i++) parts.push(i in a ? String(a[i]) : '<hole>');\n"
                    + "  return '[' + parts.join(', ') + '] ' + a.length;\n"
                    + "}\n";

    @Test
    public void middleHole() {
        Utils.assertWithAllModes_ES6(
                "[1, 1, 2, 2] 4", SHOW + "show([1, 2].flatMap(function (x) { return [x, , x]; }))");
    }

    @Test
    public void onlyHoles() {
        Utils.assertWithAllModes_ES6(
                "[] 0", SHOW + "show([1, 2].flatMap(function () { return new Array(3); }))");
    }

    @Test
    public void leadingAndTrailingHoles() {
        Utils.assertWithAllModes_ES6(
                "[1, 2] 2", SHOW + "show([1, 2].flatMap(function (x) { return [, x, ,]; }))");
    }

    @Test
    public void deletedElement() {
        Utils.assertWithAllModes_ES6(
                "[1, 3] 2",
                SHOW
                        + "show([1].flatMap(function () {\n"
                        + "  var r = [1, 2, 3]; delete r[1]; return r;\n"
                        + "}))");
    }

    @Test
    public void explicitUndefinedIsKept() {
        Utils.assertWithAllModes_ES6(
                "[undefined, 1] 2",
                SHOW + "show([1].flatMap(function (x) { return [undefined, x]; }))");
    }

    @Test
    public void holesOfReceiverAreSkipped() {
        Utils.assertWithAllModes_ES6(
                "[1, 1, 2, 2] 4", SHOW + "show([1, , 2].flatMap(function (x) { return [x, x]; }))");
    }
}
