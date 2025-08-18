/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2025;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class ArrayBufferTransferTest {

    @Test
    public void transferBasic() {
        final String script = "typeof ArrayBuffer.prototype.transfer === 'function'";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void transferToFixedLengthBasic() {
        final String script = "typeof ArrayBuffer.prototype.transferToFixedLength === 'function'";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void transferSameSize() {
        final String script =
                "var source = new ArrayBuffer(4);"
                        + "var sourceArray = new Uint8Array(source);"
                        + "sourceArray[0] = 1; sourceArray[1] = 2; sourceArray[2] = 3; sourceArray[3] = 4;"
                        + "var dest = source.transfer();"
                        + "source.byteLength === 0 && dest.byteLength === 4";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void transferDataCopied() {
        final String script =
                "var source = new ArrayBuffer(4);"
                        + "var sourceArray = new Uint8Array(source);"
                        + "sourceArray[0] = 1; sourceArray[1] = 2; sourceArray[2] = 3; sourceArray[3] = 4;"
                        + "var dest = source.transfer();"
                        + "var destArray = new Uint8Array(dest);"
                        + "destArray[0] === 1 && destArray[1] === 2 && destArray[2] === 3 && destArray[3] === 4";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void transferDetached() {
        final String script =
                "var source = new ArrayBuffer(4);"
                        + "var dest = source.transfer();"
                        + "source.detached === true";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void transferToLarger() {
        final String script =
                "var source = new ArrayBuffer(4);"
                        + "var sourceArray = new Uint8Array(source);"
                        + "sourceArray[0] = 1; sourceArray[1] = 2;"
                        + "var dest = source.transfer(8);"
                        + "dest.byteLength === 8";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void transferToSmaller() {
        final String script =
                "var source = new ArrayBuffer(4);"
                        + "var sourceArray = new Uint8Array(source);"
                        + "sourceArray[0] = 1; sourceArray[1] = 2; sourceArray[2] = 3; sourceArray[3] = 4;"
                        + "var dest = source.transfer(2);"
                        + "var destArray = new Uint8Array(dest);"
                        + "dest.byteLength === 2 && destArray[0] === 1 && destArray[1] === 2";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void transferToFixedLengthSameSize() {
        final String script =
                "var source = new ArrayBuffer(4);"
                        + "var dest = source.transferToFixedLength();"
                        + "source.byteLength === 0 && dest.byteLength === 4";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void transferDetachedBuffer() {
        final String script =
                "var source = new ArrayBuffer(4);"
                        + "source.transfer();"
                        + "try {"
                        + "  source.transfer();"
                        + "  false;"
                        + "} catch(e) {"
                        + "  e instanceof TypeError;"
                        + "}";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void transferNegativeLength() {
        final String script =
                "var source = new ArrayBuffer(4);"
                        + "try {"
                        + "  source.transfer(-1);"
                        + "  false;"
                        + "} catch(e) {"
                        + "  e instanceof RangeError;"
                        + "}";
        Utils.assertWithAllModes_ES6(true, script);
    }
}
