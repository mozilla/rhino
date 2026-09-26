/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;

public class JavaSerializationPolicyTest {
    private static final class PolicyFactory extends ContextFactory {
        private final boolean enabled;

        PolicyFactory(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        protected boolean hasFeature(Context cx, int featureIndex) {
            if (featureIndex == Context.FEATURE_JAVA_SERIALIZATION) {
                return enabled;
            }
            return super.hasFeature(cx, featureIndex);
        }
    }

    @Test
    public void disabledByDefault() {
        assertThrows(NotSerializableException.class, Context::checkSerializationEnabled);
        assertThrows(InvalidObjectException.class, Context::checkDeserializationEnabled);
        try (Context cx = new ContextFactory().enterContext()) {
            assertFalse(cx.hasFeature(Context.FEATURE_JAVA_SERIALIZATION));
            assertThrows(NotSerializableException.class, Context::checkSerializationEnabled);
            assertThrows(InvalidObjectException.class, Context::checkDeserializationEnabled);
        }
    }

    @Test
    public void enabledFactoryAllowsSerialization() throws Exception {
        try (Context cx = new PolicyFactory(true).enterContext()) {
            assertTrue(cx.hasFeature(Context.FEATURE_JAVA_SERIALIZATION));
            Context.checkSerializationEnabled();
            Context.checkDeserializationEnabled();
        }
    }

    @Test
    public void disabledFactoryBlocksSerialization() {
        try (Context cx = new PolicyFactory(false).enterContext()) {
            assertFalse(cx.hasFeature(Context.FEATURE_JAVA_SERIALIZATION));
            assertThrows(NotSerializableException.class, Context::checkSerializationEnabled);
            assertThrows(InvalidObjectException.class, Context::checkDeserializationEnabled);
        }
    }

    @Test
    public void roundTripWithEnabledFactory() throws Exception {
        try (Context cx = new PolicyFactory(true).enterContext()) {
            Hashtable original = new Hashtable();
            original.put("key", "value");
            byte[] serialized = serialize(original);
            Hashtable restored = (Hashtable) deserialize(serialized);
            assertEquals("value", restored.iterator().next().value());

            assertEquals("ab", deserialize(serialize(new ConsString("a", "b"))));
            assertSame(Undefined.instance, deserialize(serialize(Undefined.instance)));
        }
    }

    private static byte[] serialize(Object value) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ObjectOutputStream stream = new ObjectOutputStream(output)) {
            stream.writeObject(value);
        }
        return output.toByteArray();
    }

    private static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return stream.readObject();
        }
    }
}
