package org.mozilla.javascript.tests;

import java.lang.reflect.Method;
import junit.framework.TestCase;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSFunction;

public class ConsStringTest extends TestCase {

    public void testAppend() {
        ConsString current = new ConsString("a", "b");
        current = new ConsString(current, "c");
        current = new ConsString(current, "d");

        assertEquals("abcd", current.toString());

        current = new ConsString("x", new ConsString("a", "b"));
        assertEquals("xab", current.toString());

        current = new ConsString(new ConsString("a", "b"), new ConsString("c", "d"));
        assertEquals("abcd", current.toString());
    }

    public void testAppendManyStrings() {
        ConsString current = new ConsString("a", "a");
        for (int i = 0; i < 1000000; i++) {
            current = new ConsString(current, "a");
        }
        assertNotNull(current.toString());
    }

    public void testAppendManyStringsRecursive() {
        recurseAndAppend(4000);
    }

    private void recurseAndAppend(int depth) {
        if (depth == 0) {
            ConsString current = new ConsString("a", "a");
            for (int i = 0; i < 1000000; i++) {
                current = new ConsString(current, "a");
            }
            assertNotNull(current.toString());
        } else {
            recurseAndAppend(depth - 1);
        }
    }

    public void testDoNotLeakConsStringIntoSetter() throws Exception {
        Context cx = Context.enter();
        try {
            final ScriptableObject topScope = cx.initStandardObjects();
            final MyHostObject myHostObject = new MyHostObject();

            // define custom getter method
            final Method getter = MyHostObject.class.getMethod("getFoo");
            final Method setter = MyHostObject.class.getMethod("setFoo", Object.class);
            myHostObject.defineProperty("foo", null, getter, setter, ScriptableObject.EMPTY);
            topScope.put("MyHostObject", topScope, myHostObject);

            final String script =
                    "var a = 'Rhino';\n" + "MyHostObject.foo = '#' + a;\n" + "MyHostObject.foo;";

            final String result = (String) cx.evaluateString(topScope, script, "myScript", 1, null);

            assertEquals("java.lang.String", result);
        } finally {
            Context.exit();
        }
    }

    public void testDoNotLeakConsStringIntoFunction() throws Exception {
        Context cx = Context.enter();
        try {
            final ScriptableObject topScope = cx.initStandardObjects();
            ScriptableObject.defineClass(topScope, MyHostObject.class);

            final String script = "var a = 'Rhino'; new MyHostObject().test('#' + a);";
            final String result = (String) cx.evaluateString(topScope, script, "myScript", 1, null);

            assertEquals("java.lang.String", result);
        } finally {
            Context.exit();
        }
    }

    public static class MyHostObject extends ScriptableObject {

        private String foo;

        @Override
        public String getClassName() {
            return "MyHostObject";
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(Object foo) {
            this.foo = foo.getClass().getName();
        }

        @JSFunction
        public String test(Object obj) {
            return obj.getClass().getName();
        }
    }
}
