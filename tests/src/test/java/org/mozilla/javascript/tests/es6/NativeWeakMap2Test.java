/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.Test;
import org.mozilla.javascript.NativeWeakMap;

public class NativeWeakMap2Test {

    /** Test serialization of an empty object. */
    @Test
    public void serialization() throws IOException, ClassNotFoundException {

        NativeWeakMap weakMap = new NativeWeakMap();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(bos)) {
            oout.writeObject(weakMap);

            try (ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                    ObjectInputStream oin = new ObjectInputStream(bis)) {
                NativeWeakMap result = (NativeWeakMap) oin.readObject();
                assertEquals(0, result.getIds().length);
            }
        }
    }
}
