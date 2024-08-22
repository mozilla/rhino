package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tests.Utils;

/**
 * Checks if setting "functionName = null" is propagated to thisObject.
 *
 * @author Ahmed Ashour
 */
public class FunctionNullSetTest {

    /** @throws Exception if the test fails */
    @Test
    public void setFunctionToNull() throws Exception {
        final String script = "function onclick() {onclick=null}";

        final ContextAction<Object> action =
                new ContextAction<Object>() {
                    @Override
                    public Object run(final Context cx) {
                        try {
                            final Scriptable scope = cx.initSafeStandardObjects();
                            final MyHostObject prototype = new MyHostObject();
                            ScriptableObject.defineClass(scope, MyHostObject.class);
                            final Method getterMethod =
                                    MyHostObject.class.getMethod("jsxGet_onclick");
                            final Method setterMethod =
                                    MyHostObject.class.getMethod("jsxSet_onclick", Object.class);
                            prototype.defineProperty(
                                    "onclick",
                                    null,
                                    getterMethod,
                                    setterMethod,
                                    ScriptableObject.EMPTY);

                            ScriptableObject.defineProperty(
                                    scope, "o", prototype, ScriptableObject.DONTENUM);

                            final MyHostObject jsObj = new MyHostObject();
                            jsObj.setPrototype(prototype);
                            jsObj.setParentScope(scope);

                            final Function realFunction_ =
                                    cx.compileFunction(jsObj, script, "myevent", 0, null);

                            realFunction_.call(cx, jsObj, jsObj, new Object[0]);

                            assertNull(jsObj.onclick_);
                        } catch (final Exception e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }
                };

        Utils.runWithAllOptimizationLevels(action);
    }

    public static class MyHostObject extends ScriptableObject {
        private Object onclick_ = new Object();

        @Override
        public String getClassName() {
            return getClass().getSimpleName();
        }

        public Object jsxGet_onclick() {
            return onclick_;
        }

        public void jsxSet_onclick(final Object o) {
            onclick_ = o;
        }
    }
}
