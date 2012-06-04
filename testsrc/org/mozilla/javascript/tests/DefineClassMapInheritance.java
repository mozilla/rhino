package org.mozilla.javascript.tests;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

@SuppressWarnings("serial")
public class DefineClassMapInheritance {

    public static class Food extends ScriptableObject {
        @Override
        public String getClassName() {
            return getClass().getSimpleName();
        }
    }

    public static class Fruit extends Food {
    }

    public static class Vegetable extends Food {
    }

    @Test
    public void test() throws IllegalAccessException, InstantiationException,
            InvocationTargetException {
        Context cx = Context.enter();
        try {
            ScriptableObject scope = cx.initStandardObjects();

            // define two classes that share a parent prototype
            ScriptableObject.defineClass(scope, Fruit.class, false, true);
            ScriptableObject.defineClass(scope, Vegetable.class, false, true);

            assertEquals(Boolean.TRUE,
                    evaluate(cx, scope, "(new Fruit instanceof Food)"));
            assertEquals(Boolean.TRUE,
                    evaluate(cx, scope, "(new Vegetable instanceof Food)"));
        } finally {
            Context.exit();
        }
    }

    private static Object evaluate(Context cx, ScriptableObject scope,
            String source) {
        return cx.evaluateString(scope, source, "<eval>", 1, null);
    }
}
