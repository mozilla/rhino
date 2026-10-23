/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2025;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

public class ArrayBufferTransferTest {

    @Test
    public void transferIgnoresSpeciesConstructor() {
        final String script =
                "var res = '';"
                        + "['transfer', 'transferToFixedLength'].forEach(function(m) {"
                        + "  var buf = new ArrayBuffer(4);"
                        + "  new Uint8Array(buf)[0] = 42;"
                        + "  buf.constructor = {};"
                        + "  buf.constructor[Symbol.species] = function() { return buf; };"
                        + "  var t = buf[m]();"
                        + "  res += (t !== buf) + ',' + t.byteLength + ',' + new Uint8Array(t)[0]"
                        + "    + ',' + (Object.getPrototypeOf(t) === ArrayBuffer.prototype) + ' ';"
                        + "});"
                        + "res";
        Utils.assertWithAllModes_ES6("true,4,42,true true,4,42,true ", script);
    }

    @Test
    public void transferDoesNotReadConstructor() {
        final String script =
                "var buf = new ArrayBuffer(8, { maxByteLength: 16 });"
                        + "Object.defineProperty(buf, 'constructor', {"
                        + "  get: function() { throw new Error('constructor read'); }"
                        + "});"
                        + "var t = buf.transfer(4);"
                        + "var res = buf.detached + ',' + t.byteLength + ',' + t.maxByteLength;"
                        + "try { t.transfer(17); } catch (e) { res += ',' + (e instanceof RangeError); }"
                        + "res";
        Utils.assertWithAllModes_ES6("true,4,16,true", script);
    }
}
