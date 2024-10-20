package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

public class ContextFactoryFeatureTest {
    @BeforeAll
    public static void cleanup() {
        // Not all the OTHER tests currently clean up their context, and
        // this test can't work unless they do.
        Context cx = Context.getCurrentContext();
        if (cx != null) {
            Context.exit();
        }
    }

    @Test
    public void defaultContextFactory() {
        ContextFactory factory = new ContextFactory();
        try (Context cx = factory.enterContext()) {
            assertTrue(cx.hasFeature(Context.FEATURE_V8_EXTENSIONS));
            assertTrue(cx.hasFeature(Context.FEATURE_ENABLE_XML_SECURE_PARSING));
            assertFalse(cx.hasFeature(Context.FEATURE_LITTLE_ENDIAN));
            assertFalse(cx.hasFeature(Context.FEATURE_DYNAMIC_SCOPE));
        }
    }

    @Test
    public void contextFactorySubclass() {
        ContextFactory factory =
                new ContextFactory() {
                    @Override
                    protected boolean hasFeature(Context cx, int featureIndex) {
                        switch (featureIndex) {
                            case Context.FEATURE_V8_EXTENSIONS:
                                return false;
                            case Context.FEATURE_DYNAMIC_SCOPE:
                                return true;
                            default:
                                return super.hasFeature(cx, featureIndex);
                        }
                    }
                };
        try (Context cx = factory.enterContext()) {
            assertFalse(cx.hasFeature(Context.FEATURE_V8_EXTENSIONS));
            assertTrue(cx.hasFeature(Context.FEATURE_ENABLE_XML_SECURE_PARSING));
            assertFalse(cx.hasFeature(Context.FEATURE_LITTLE_ENDIAN));
            assertTrue(cx.hasFeature(Context.FEATURE_DYNAMIC_SCOPE));
        }
    }

    @Test
    public void contextFactoryLambda() {
        ContextFactory factory = new ContextFactory();
        factory.setFeatureTester(
                (featureIndex) -> {
                    switch (featureIndex) {
                        case Context.FEATURE_V8_EXTENSIONS:
                            return ContextFactory.Feature.DISABLED;
                        case Context.FEATURE_DYNAMIC_SCOPE:
                            return ContextFactory.Feature.ENABLED;
                        default:
                            return ContextFactory.Feature.DEFAULT;
                    }
                });
        try (Context cx = factory.enterContext()) {
            assertFalse(cx.hasFeature(Context.FEATURE_V8_EXTENSIONS));
            assertTrue(cx.hasFeature(Context.FEATURE_ENABLE_XML_SECURE_PARSING));
            assertFalse(cx.hasFeature(Context.FEATURE_LITTLE_ENDIAN));
            assertTrue(cx.hasFeature(Context.FEATURE_DYNAMIC_SCOPE));
        }
    }
}
