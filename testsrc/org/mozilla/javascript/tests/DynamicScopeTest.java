package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;

/**
 * Models application initialization of a real-world rhino application. This fails against Rhino
 * 1.7.8 RC1
 *
 * @author Raimund Jacob-Bloedorn <jacob@pinuts.de>
 */
public class DynamicScopeTest {

    /** Define the specific behaviour of our application. */
    static class DynamicScopeContextFactory extends ContextFactory {
        @Override
        protected boolean hasFeature(Context cx, int featureIndex) {
            if (featureIndex == Context.FEATURE_DYNAMIC_SCOPE
                    || featureIndex == Context.FEATURE_STRICT_VARS
                    || featureIndex == Context.FEATURE_E4X
                    || featureIndex == Context.FEATURE_STRICT_EVAL
                    || featureIndex == Context.FEATURE_LOCATION_INFORMATION_IN_ERROR
                    || featureIndex == Context.FEATURE_STRICT_MODE) {
                return true;
            }
            return super.hasFeature(cx, featureIndex);
        }

        @Override
        protected Context makeContext() {
            Context cx = super.makeContext();
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(0);
            return cx;
        }
    }

    /** Sometimes a new context ist required. The constructor is protected so this */
    private static class FreshContext extends Context {
        public FreshContext(ContextFactory contextFactory) {
            super(contextFactory);
        }
    }

    @Test
    /** Sealed standard objects vs dynamic scopes. */
    public void initStandardObjectsSealed() {
        ContextFactory contextFactory = new DynamicScopeContextFactory();

        // This is what we do on initialization ...
        final Context cx = contextFactory.enterContext();
        try {
            // Used to fail with org.mozilla.javascript.EvaluatorException: Cannot modify a property
            // of a sealed object: iterator.
            final ScriptableObject scope = cx.initStandardObjects(new TopLevel(), true);

            Object result = cx.evaluateString(scope, "42", "source", 1, null);
            assertEquals(42, result);
        } finally {
            // cx.exit();
        }

        // ... Lots of switches between JS and Java code here ...

        // Almost the same code block as above
        final Context cx2 = new FreshContext(contextFactory);
        try {
            // Used to fail with org.mozilla.javascript.EvaluatorException: Cannot modify a property
            // of a sealed object: iterator.
            final ScriptableObject scope = cx.initStandardObjects(new TopLevel(), true);

            Object result = cx.evaluateString(scope, "23", "source", 1, null);
            assertEquals(23, result);
        } finally {
            // cx.exit
        }
    }

    @Test
    /** Standard method Object.create */
    public void standardMethodObjectCreate() {
        ContextFactory contextFactory = new DynamicScopeContextFactory();

        try (Context cx = contextFactory.enterContext()) {

            // Used to fail with org.mozilla.javascript.EvaluatorException: Cannot modify a property
            // of a sealed object: iterator.
            final ScriptableObject someScope = cx.initStandardObjects();

            Scriptable someObj =
                    (Scriptable)
                            cx.evaluateString(someScope, "var obj = {}; obj;", "source1", 1, null);

            Scriptable subScope =
                    (Scriptable)
                            cx.evaluateString(
                                    someScope,
                                    "Object.create((0,eval)('this'));",
                                    "source2",
                                    1,
                                    null);
            subScope.setParentScope(null);

            Scriptable subObj =
                    (Scriptable)
                            cx.evaluateString(
                                    subScope,
                                    "var subObj = Object.create(obj); subObj;",
                                    "source3",
                                    1,
                                    null);

            assertSame(subObj.getPrototype(), someObj);
            assertSame(subScope.getPrototype(), someScope);
            assertSame(someObj.getParentScope(), someScope);
            assertSame(subObj.getParentScope(), subScope);
        }
    }
}
