/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.mozilla.javascript.testutils;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

/**
 * Enters a Context for tests of trusted legacy serialization compatibility.
 *
 * <p>Enables Java serialization for the contexts created by {@link #factory()} by overriding {@link
 * ContextFactory#hasFeature(Context, int)} for {@link Context#FEATURE_JAVA_SERIALIZATION}.
 */
public final class SerializationTestContext {
    private SerializationTestContext() {}

    private static final class SerializationEnabledFactory extends ContextFactory {
        @Override
        protected boolean hasFeature(Context cx, int featureIndex) {
            if (featureIndex == Context.FEATURE_JAVA_SERIALIZATION) {
                return true;
            }
            return super.hasFeature(cx, featureIndex);
        }
    }

    private static final ContextFactory FACTORY = new SerializationEnabledFactory();

    public static ContextFactory factory() {
        return FACTORY;
    }

    public static Context enter() {
        return FACTORY.enterContext();
    }
}
