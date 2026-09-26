/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.mozilla.javascript;

import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import org.mozilla.javascript.config.RhinoConfig;

/**
 * Java serialization policy for guarded Rhino runtime objects.
 *
 * <p>Serialization and deserialization are disabled unless {@code rhino.serialization.enabled} is
 * {@code true} in {@link RhinoConfig}. The value is resolved once per process, on first use, and
 * later configuration changes, including system property changes, have no effect.
 */
public final class JavaSerializationPolicy {
    private static final boolean ENABLED = RhinoConfig.get("rhino.serialization.enabled", false);

    private JavaSerializationPolicy() {}

    /** Throws unless Java serialization is enabled for this process. */
    public static void checkSerializationEnabled() throws NotSerializableException {
        if (!ENABLED) {
            throw new NotSerializableException("Java serialization is disabled in Rhino");
        }
    }

    /** Throws unless Java deserialization is enabled for this process. */
    public static void checkDeserializationEnabled() throws InvalidObjectException {
        if (!ENABLED) {
            throw new InvalidObjectException("Java deserialization is disabled in Rhino");
        }
    }

    static boolean isEnabled() {
        return ENABLED;
    }
}
