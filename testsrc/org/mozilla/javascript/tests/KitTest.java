/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import org.mozilla.javascript.Kit;

/**
 * Test for {@link Kit}
 *
 * @author Roland Praml, FOCONIS AG
 */
public class KitTest {

    private byte[] contentArr = new byte[123];

    public KitTest() {
        for (int i = 0; i < 123; i++) {
            contentArr[i] = (byte) i;
        }
    }

    private InputStream emptyStream() {
        return new ByteArrayInputStream(new byte[0]);
    }

    private InputStream contentStream() {

        return new ByteArrayInputStream(contentArr);
    }

    /**
     * Test various code paths in Kit.readStream. Especially if bufferCapacity matches stream size.
     */
    @Test
    public void readSteam() throws IOException {

        byte[] buf = Kit.readStream(emptyStream(), 4096); // test, too big
        assertEquals(0, buf.length);

        buf = Kit.readStream(contentStream(), 122); // too small
        assertArrayEquals(contentArr, buf);
        buf = Kit.readStream(contentStream(), 123); // exact match
        assertArrayEquals(contentArr, buf);
        buf = Kit.readStream(contentStream(), 124); // too big
        assertArrayEquals(contentArr, buf);
        buf = Kit.readStream(contentStream(), 60); // much too small
        assertArrayEquals(contentArr, buf);
    }
}
