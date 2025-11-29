/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2024;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class ResizableArrayBufferTest {

    // Basic resizable buffer creation
    @Test
    public void testResizableBufferCreation() {
        String script =
                "var buffer = new ArrayBuffer(8, { maxByteLength: 16 });"
                        + "buffer.byteLength === 8 && buffer.maxByteLength === 16 && buffer.resizable === true";
        Utils.assertWithAllModes(true, script);
    }

    // Fixed-length buffer (no maxByteLength)
    @Test
    public void testFixedLengthBuffer() {
        String script =
                "var buffer = new ArrayBuffer(8);"
                        + "buffer.byteLength === 8 && buffer.maxByteLength === 8 && buffer.resizable === false";
        Utils.assertWithAllModes(true, script);
    }

    // Resize - grow buffer
    @Test
    public void testResizeGrow() {
        String script =
                "var buffer = new ArrayBuffer(8, { maxByteLength: 16 });"
                        + "buffer.resize(12);"
                        + "buffer.byteLength === 12";
        Utils.assertWithAllModes(true, script);
    }

    // Resize - shrink buffer
    @Test
    public void testResizeShrink() {
        String script =
                "var buffer = new ArrayBuffer(16, { maxByteLength: 32 });"
                        + "buffer.resize(8);"
                        + "buffer.byteLength === 8";
        Utils.assertWithAllModes(true, script);
    }

    // Resize - preserve data when growing
    @Test
    public void testResizePreservesData() {
        String script =
                "var buffer = new ArrayBuffer(4, { maxByteLength: 16 });"
                        + "var view = new Uint8Array(buffer);"
                        + "view[0] = 1; view[1] = 2; view[2] = 3; view[3] = 4;"
                        + "buffer.resize(8);"
                        + "var newView = new Uint8Array(buffer);"
                        + "newView[0] === 1 && newView[1] === 2 && newView[2] === 3 && newView[3] === 4";
        Utils.assertWithAllModes(true, script);
    }

    // Resize - new bytes are zero
    @Test
    public void testResizeNewBytesZero() {
        String script =
                "var buffer = new ArrayBuffer(4, { maxByteLength: 16 });"
                        + "buffer.resize(8);"
                        + "var view = new Uint8Array(buffer);"
                        + "view[4] === 0 && view[5] === 0 && view[6] === 0 && view[7] === 0";
        Utils.assertWithAllModes(true, script);
    }

    // Resize - preserve data when shrinking
    @Test
    public void testResizeShrinkPreservesData() {
        String script =
                "var buffer = new ArrayBuffer(8, { maxByteLength: 16 });"
                        + "var view = new Uint8Array(buffer);"
                        + "view[0] = 1; view[1] = 2; view[2] = 3; view[3] = 4;"
                        + "buffer.resize(4);"
                        + "var newView = new Uint8Array(buffer);"
                        + "newView[0] === 1 && newView[1] === 2 && newView[2] === 3 && newView[3] === 4";
        Utils.assertWithAllModes(true, script);
    }

    // Error: resize beyond maxByteLength
    @Test
    public void testResizeBeyondMax() {
        String script =
                "var buffer = new ArrayBuffer(8, { maxByteLength: 16 });"
                        + "try {"
                        + "  buffer.resize(32);"
                        + "  false;"
                        + "} catch(e) {"
                        + "  e instanceof RangeError;"
                        + "}";
        Utils.assertWithAllModes(true, script);
    }

    // Error: resize fixed-length buffer
    @Test
    public void testResizeFixedLengthError() {
        String script =
                "var buffer = new ArrayBuffer(8);"
                        + "try {"
                        + "  buffer.resize(16);"
                        + "  false;"
                        + "} catch(e) {"
                        + "  e instanceof TypeError;"
                        + "}";
        Utils.assertWithAllModes(true, script);
    }

    // Error: resize detached buffer
    @Test
    public void testResizeDetachedBuffer() {
        String script =
                "var buffer = new ArrayBuffer(8, { maxByteLength: 16 });"
                        + "buffer.transfer();"
                        + "try {"
                        + "  buffer.resize(12);"
                        + "  false;"
                        + "} catch(e) {"
                        + "  e instanceof TypeError;"
                        + "}";
        Utils.assertWithAllModes(true, script);
    }

    // Error: length exceeds maxByteLength in constructor
    @Test
    public void testConstructorLengthExceedsMax() {
        String script =
                "try {"
                        + "  new ArrayBuffer(32, { maxByteLength: 16 });"
                        + "  false;"
                        + "} catch(e) {"
                        + "  e instanceof RangeError;"
                        + "}";
        Utils.assertWithAllModes(true, script);
    }

    // Error: negative maxByteLength
    @Test
    public void testNegativeMaxByteLength() {
        String script =
                "try {"
                        + "  new ArrayBuffer(8, { maxByteLength: -1 });"
                        + "  false;"
                        + "} catch(e) {"
                        + "  e instanceof RangeError;"
                        + "}";
        Utils.assertWithAllModes(true, script);
    }

    // Resize to same size (no-op)
    @Test
    public void testResizeSameSize() {
        String script =
                "var buffer = new ArrayBuffer(8, { maxByteLength: 16 });"
                        + "buffer.resize(8);"
                        + "buffer.byteLength === 8";
        Utils.assertWithAllModes(true, script);
    }

    // Resize to zero
    @Test
    public void testResizeToZero() {
        String script =
                "var buffer = new ArrayBuffer(8, { maxByteLength: 16 });"
                        + "buffer.resize(0);"
                        + "buffer.byteLength === 0";
        Utils.assertWithAllModes(true, script);
    }

    // Resize from zero
    @Test
    public void testResizeFromZero() {
        String script =
                "var buffer = new ArrayBuffer(0, { maxByteLength: 16 });"
                        + "buffer.resize(8);"
                        + "buffer.byteLength === 8";
        Utils.assertWithAllModes(true, script);
    }

    // maxByteLength equals byteLength
    @Test
    public void testMaxByteLengthEqualsLength() {
        String script =
                "var buffer = new ArrayBuffer(16, { maxByteLength: 16 });"
                        + "buffer.byteLength === 16 && buffer.maxByteLength === 16 && buffer.resizable === true";
        Utils.assertWithAllModes(true, script);
    }

    // Resize multiple times
    @Test
    public void testResizeMultipleTimes() {
        String script =
                "var buffer = new ArrayBuffer(8, { maxByteLength: 32 });"
                        + "buffer.resize(16);"
                        + "buffer.resize(24);"
                        + "buffer.resize(12);"
                        + "buffer.byteLength === 12";
        Utils.assertWithAllModes(true, script);
    }

    // NaN maxByteLength converts to 0
    @Test
    public void testNaNMaxByteLength() {
        String script =
                "var buffer = new ArrayBuffer(0, { maxByteLength: NaN });"
                        + "buffer.maxByteLength === 0 && buffer.resizable === true";
        Utils.assertWithAllModes(true, script);
    }

    // Undefined maxByteLength option
    @Test
    public void testUndefinedMaxByteLength() {
        String script =
                "var buffer = new ArrayBuffer(8, { maxByteLength: undefined });"
                        + "buffer.resizable === false && buffer.maxByteLength === 8";
        Utils.assertWithAllModes(true, script);
    }

    // Empty options object
    @Test
    public void testEmptyOptions() {
        String script =
                "var buffer = new ArrayBuffer(8, {});"
                        + "buffer.resizable === false && buffer.maxByteLength === 8";
        Utils.assertWithAllModes(true, script);
    }

    // Error: resize negative size
    @Test
    public void testResizeNegative() {
        String script =
                "var buffer = new ArrayBuffer(8, { maxByteLength: 16 });"
                        + "try {"
                        + "  buffer.resize(-1);"
                        + "  false;"
                        + "} catch(e) {"
                        + "  e instanceof RangeError;"
                        + "}";
        Utils.assertWithAllModes(true, script);
    }

    // Error: resize to Infinity
    @Test
    public void testResizeInfinity() {
        String script =
                "var buffer = new ArrayBuffer(8, { maxByteLength: 16 });"
                        + "try {"
                        + "  buffer.resize(Infinity);"
                        + "  false;"
                        + "} catch(e) {"
                        + "  e instanceof RangeError;"
                        + "}";
        Utils.assertWithAllModes(true, script);
    }
}
