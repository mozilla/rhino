/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

class CatchDestructuringTest {
    @Test
    void cannotUseObjectDestructuringInEs5() {
        Utils.assertEvaluatorException_1_8(
                "Destructuring in catch blocks requires ES6 or later (test#3)",
                "try {\n"
                        + "  throw new Error('test');\n"
                        + "} catch ({message}) {\n"
                        + "  message;\n"
                        + "}\n");
    }

    @Test
    void canUseObjectDestructuring() {
        Utils.assertWithAllModes_ES6(
                "hey",
                "try {\n"
                        + "  throw new Error('hey');\n"
                        + "} catch ({message}) {\n"
                        + "  message;\n"
                        + "}\n");
    }

    @Test
    void canUseObjectDestructuringWithRenaming() {
        Utils.assertWithAllModes_ES6(
                "hey",
                "try {\n"
                        + "  throw new Error('hey');\n"
                        + "} catch ({message: m}) {\n"
                        + "  m;\n"
                        + "}\n");
    }

    @Test
    void canUseNestedObjectDestructuring() {
        Utils.assertWithAllModes_ES6(
                "nested",
                "try {\n"
                        + "  throw {error: {code: 'nested'}};\n"
                        + "} catch ({error: {code}}) {\n"
                        + "  code;\n"
                        + "}\n");
    }

    @Test
    void canUseDefaultValuesInDestructuring() {
        Utils.assertWithAllModes_ES6(
                "default",
                "try {\n"
                        + "  throw {};\n"
                        + "} catch ({message = 'default'}) {\n"
                        + "  message;\n"
                        + "}\n");
    }

    @Test
    void cannotUseObjectDestructuringAndConditions() {
        Utils.assertEvaluatorExceptionES6(
                "invalid catch block condition (test#3)",
                "try {\n"
                        + "  throw new Error('hey');\n"
                        + "} catch ( {e} if e.message ) {\n"
                        + "  e.message;\n"
                        + "}\n");
    }

    @Test
    void cannotUseArrayDestructuringInEs5() {
        Utils.assertEvaluatorException_1_8(
                "Destructuring in catch blocks requires ES6 or later (test#3)",
                "try {\n"
                        + "  throw ['h', 'e', 'y']\n"
                        + "} catch ([h, e, y]) {\n"
                        + "  h + e + y;\n"
                        + "}\n");
    }

    @Test
    void canUseArrayDestructuring() {
        Utils.assertWithAllModes_ES6(
                "hey",
                "try {\n"
                        + "  throw ['h', 'e', 'y']\n"
                        + "} catch ([h, e, y]) {\n"
                        + "  h + e + y;\n"
                        + "}\n");
    }

    @Test
    void canSkipArrayElements() {
        Utils.assertWithAllModes_ES6(
                "c",
                "try {\n"
                        + "  throw ['a', 'b', 'c'];\n"
                        + "} catch ([, , third]) {\n"
                        + "  third;\n"
                        + "}\n");
    }

    @Test
    void canUseNestedArrayDestructuring() {
        Utils.assertWithAllModes_ES6(
                "b",
                "try {\n"
                        + "  throw [['a', 'b'], ['c', 'd']];\n"
                        + "} catch ([[x, y], [z, w]]) {\n"
                        + "  y;\n"
                        + "}\n");
    }

    @Test
    void canCombineObjectAndArrayDestructuring() {
        Utils.assertWithAllModes_ES6(
                "value",
                "try {\n"
                        + "  throw {data: ['value', 'other']};\n"
                        + "} catch ({data: [first]}) {\n"
                        + "  first;\n"
                        + "}\n");
    }

    @Test
    void canCombineArrayAndObjectDestructuring() {
        Utils.assertWithAllModes_ES6(
                "test",
                "try {\n"
                        + "  throw [{message: 'test'}];\n"
                        + "} catch ([{message}]) {\n"
                        + "  message;\n"
                        + "}\n");
    }
}
