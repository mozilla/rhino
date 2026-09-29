/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Tests that variable length {@code \\u{...}} code point escapes reject values above U+10FFFF
 * instead of silently wrapping the accumulator.
 */
public class UnicodeCodePointEscapeTest {

    @Test
    public void validCodePointEscapesStillWork() {
        Utils.assertWithAllModes_ES6(Boolean.TRUE, "'\\u{41}' === 'A'");
        Utils.assertWithAllModes_ES6(Boolean.TRUE, "'\\u{10FFFF}'.codePointAt(0) === 0x10FFFF");
        Utils.assertWithAllModes_ES6(Boolean.TRUE, "'\\u{1F600}'.length === 2");
    }

    @Test
    public void overlargeStringEscapeIsRejected() {
        Utils.assertEvaluatorExceptionES6("invalid Unicode escape sequence", "'\\u{100000041}'");
        Utils.assertEvaluatorExceptionES6("invalid Unicode escape sequence", "'\\u{100000000}'");
        Utils.assertEvaluatorExceptionES6("invalid Unicode escape sequence", "'\\u{110000}'");
    }

    @Test
    public void overlargeIdentifierEscapeIsRejected() {
        Utils.assertEvaluatorExceptionES6(
                "invalid Unicode escape sequence", "var \\u{100000041} = 1;");
    }

    @Test
    public void overlargeTemplateEscapeIsRejected() {
        Utils.assertEvaluatorExceptionES6("syntax error", "`\\u{100000041}`");
    }
}
