/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeJavaPackage;

public class NativeJavaPackageSerializationTest {
    private static final class SerializationEnabledFactory extends ContextFactory {
        @Override
        protected boolean hasFeature(Context cx, int featureIndex) {
            if (featureIndex == Context.FEATURE_JAVA_SERIALIZATION) {
                return true;
            }
            return super.hasFeature(cx, featureIndex);
        }
    }

    @SuppressWarnings("deprecation")
    private static byte[] serialize(NativeJavaPackage pkg) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(pkg);
        }
        return bos.toByteArray();
    }

    private static NativeJavaPackage deserialize(byte[] bytes) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (NativeJavaPackage) ois.readObject();
        }
    }

    @Test
    public void deserializationBlockedWhenPolicyDisabled() throws Exception {
        byte[] bytes;
        try (Context cx = new SerializationEnabledFactory().enterContext()) {
            cx.initStandardObjects(null);
            bytes = serialize(new NativeJavaPackage("java.lang", cx.getApplicationClassLoader()));
        }
        assertThrows(InvalidObjectException.class, () -> deserialize(bytes));
    }

    @Test
    public void roundTripWithEnabledContextRestoresClassLoader() throws Exception {
        try (Context cx = new SerializationEnabledFactory().enterContext()) {
            cx.initStandardObjects(null);
            @SuppressWarnings("deprecation")
            NativeJavaPackage original =
                    new NativeJavaPackage("java.lang", cx.getApplicationClassLoader());

            NativeJavaPackage restored = deserialize(serialize(original));

            assertEquals("[JavaPackage java.lang]", restored.toString());
            Field field = NativeJavaPackage.class.getDeclaredField("classLoader");
            field.setAccessible(true);
            assertSame(cx.getApplicationClassLoader(), field.get(restored));
        }
    }
}
