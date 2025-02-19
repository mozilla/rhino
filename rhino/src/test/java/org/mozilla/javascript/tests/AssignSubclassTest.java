package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.testutils.Utils;

public class AssignSubclassTest {
    @Test
    public void basicOperations() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    try {
                        ScriptableObject.defineClass(scope, MySubclass.class);
                    } catch (Exception e) {
                        fail(e);
                    }
                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "let o = new MySubclass();"
                                            + "o.foo = 'bar';\n"
                                            + "o.something = 'else';\n"
                                            + "o[0] = 'baz';\n"
                                            + "o[1] = 'frooby';\n"
                                            + "o;\n",
                                    "test",
                                    1,
                                    null);
                    assertInstanceOf(Scriptable.class, result);
                    Scriptable o = (Scriptable) result;
                    assertEquals("bar", o.get("foo", o));
                    assertEquals("else", o.get("something", o));
                    assertEquals("baz", o.get(0, o));
                    assertEquals("frooby", o.get(1, o));
                    return null;
                });
    }

    @Test
    public void assign() {
        Utils.runWithAllModes(
                cx -> {
                    Scriptable scope = cx.initStandardObjects();
                    try {
                        ScriptableObject.defineClass(scope, MySubclass.class);
                    } catch (Exception e) {
                        fail(e);
                    }
                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "\n"
                                            + "let s = {foo: 'bar', something: 'else', 0: 'baz', 1: 'frooby'};\n"
                                            + "let o = new MySubclass();"
                                            + "o.foo = 'x';\n"
                                            + "o.something = 'y';\n"
                                            + "o[0] = 'z';\n"
                                            + "o[1] = false;\n"
                                            + "Object.assign(o, s);\n"
                                            + "o;\n",
                                    "test",
                                    1,
                                    null);
                    assertInstanceOf(Scriptable.class, result);
                    Scriptable o = (Scriptable) result;
                    assertEquals("bar", o.get("foo", o));
                    assertEquals("else", o.get("something", o));
                    assertEquals("baz", o.get(0, o));
                    assertEquals("frooby", o.get(1, o));
                    return null;
                });
    }

    public static class MySubclass extends ScriptableObject {
        private String foo;
        private String bar;

        public MySubclass() {}

        @Override
        public String getClassName() {
            return "MySubclass";
        }

        @Override
        public Object get(String name, Scriptable start) {
            if ("foo".equals(name)) {
                return foo;
            }
            return super.get(name, start);
        }

        @Override
        public boolean has(String name, Scriptable start) {
            return "foo".equals(name) || super.has(name, start);
        }

        @Override
        public boolean putOwnProperty(
                String name, Scriptable start, Object value, boolean isThrow) {
            if ("foo".equals(name)) {
                foo = ScriptRuntime.toString(value);
                return true;
            }
            return super.putOwnProperty(name, start, value, isThrow);
        }

        @Override
        public Object get(int ix, Scriptable start) {
            if (ix == 0) {
                return bar;
            }
            return super.get(ix, start);
        }

        @Override
        public boolean has(int ix, Scriptable start) {
            return ix == 0 || super.has(ix, start);
        }

        @Override
        public boolean putOwnProperty(int ix, Scriptable start, Object value, boolean isThrow) {
            if (ix == 0) {
                bar = ScriptRuntime.toString(value);
                return true;
            }
            return super.putOwnProperty(ix, start, value, isThrow);
        }
    }
}
