/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

public class UriNoncharacterTest {
    @Test
    public void decodeUriPreservesNoncharacters() {
        Utils.assertWithAllModes(65534, "decodeURI('%EF%BF%BE').charCodeAt(0)");
        Utils.assertWithAllModes(65535, "decodeURI('%EF%BF%BF').charCodeAt(0)");
    }

    @Test
    public void decodeUriComponentPreservesNoncharacters() {
        Utils.assertWithAllModes(65534, "decodeURIComponent('%EF%BF%BE').charCodeAt(0)");
        Utils.assertWithAllModes(65535, "decodeURIComponent('%EF%BF%BF').charCodeAt(0)");
    }

    @Test
    public void noncharactersRoundTrip() {
        String script =
                "var chars = String.fromCharCode(0xfffe, 0xffff, 0xfdd0, 0xd83f, 0xdffe);"
                        + "decodeURI(encodeURI(chars)) === chars && "
                        + "decodeURIComponent(encodeURIComponent(chars)) === chars";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void malformedUtf8StillThrows() {
        String script =
                "var invalid = ['%C0%80', '%ED%A0%80', '%F4%90%80%80', '%EF%BF'];"
                        + "var decoders = [decodeURI, decodeURIComponent]; var errors = 0;"
                        + "for (var i = 0; i < decoders.length; i++) {"
                        + "  for (var j = 0; j < invalid.length; j++) {"
                        + "    try { decoders[i](invalid[j]); }"
                        + "    catch (e) { if (e instanceof URIError) errors++; }"
                        + "  }"
                        + "} errors";
        Utils.assertWithAllModes(8, script);
    }
}
