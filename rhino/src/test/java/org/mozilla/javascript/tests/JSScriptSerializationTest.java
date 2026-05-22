/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.*;

public class JSScriptSerializationTest {

    private static byte[] serialize(Script script) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(script);
        }
        return baos.toByteArray();
    }

    private static Script deserialize(byte[] bytes) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (Script) ois.readObject();
        }
    }

    @Test
    public void interpretedScriptSurvivesSerializationRoundTrip() throws Exception {
        try (Context cx = Context.enter()) {
            TopLevel scope = cx.initStandardObjects();
            cx.setInterpretedMode(true);
            Script script = cx.compileString("'hello';", "test.js", 1, null);

            assertTrue(script.getDescriptor().isInterpreted());
            assertEquals("hello", script.exec(cx, scope, scope));

            byte[] bytes = serialize(script);
            Script deserialized = deserialize(bytes);

            assertTrue(script.getDescriptor().isInterpreted());
            assertEquals("hello", deserialized.exec(cx, scope, scope));
        }
    }

    @Test
    public void compiledScriptSurvivesSerializationRoundTrip() throws Exception {
        try (Context cx = Context.enter()) {
            TopLevel scope = cx.initStandardObjects();
            cx.setInterpretedMode(false);
            Script script = cx.compileString("'hello';", "test.js", 1, null);

            assertFalse(script.getDescriptor().isInterpreted());
            assertEquals("hello", script.exec(cx, scope, scope));

            byte[] bytes = serialize(script);
            Script deserialized = deserialize(bytes);

            assertFalse(script.getDescriptor().isInterpreted());
            assertEquals("hello", deserialized.exec(cx, scope, scope));
        }
    }
}
