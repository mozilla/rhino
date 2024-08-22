/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mozilla.javascript.tests.Utils.runWithAllOptimizationLevels;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/** @author André Bargull */
public class Bug783797Test {

    private interface Action {
        void run(Context cx, ScriptableObject scope1, ScriptableObject scope2);
    }

    private static ContextAction<Void> action(final String fn, final Action a) {
        return cx -> {
            ScriptableObject scope1 = cx.initStandardObjects();
            ScriptableObject scope2 = cx.initStandardObjects();
            scope1.put("scope2", scope1, scope2);

            eval(cx, scope2, fn);
            a.run(cx, scope1, scope2);
            return null;
        };
    }

    private static Object eval(Context cx, Scriptable scope, String source) {
        return cx.evaluateString(scope, source, "<eval>", 1, null);
    }

    private static void assertTRUE(Object actual) {
        assertSame(Boolean.TRUE, actual);
    }

    private static void assertFALSE(Object actual) {
        assertSame(Boolean.FALSE, actual);
    }

    @Test
    public void getElem() {
        String fn = "function test(){ return ''['foo'] }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                eval(cx, scope1, "String.prototype.foo = 'scope1'");
                                eval(cx, scope2, "String.prototype.foo = 'scope2'");

                                assertEquals("scope2", eval(cx, scope2, "test()"));
                                assertEquals("scope2", eval(cx, scope1, "scope2.test()"));
                                assertEquals("scope2", eval(cx, scope1, "scope2.test.call(null)"));
                                assertEquals("scope2", eval(cx, scope1, "var t=scope2.test; t()"));
                                assertEquals(
                                        "scope2",
                                        eval(cx, scope1, "var t=scope2.test; t.call(null)"));
                            }
                        }));
    }

    @Test
    public void getProp() {
        String fn = "function test(){ return ''.foo }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                eval(cx, scope1, "String.prototype.foo = 'scope1'");
                                eval(cx, scope2, "String.prototype.foo = 'scope2'");

                                assertEquals("scope2", eval(cx, scope2, "test()"));
                                assertEquals("scope2", eval(cx, scope1, "scope2.test()"));
                                assertEquals("scope2", eval(cx, scope1, "scope2.test.call(null)"));
                                assertEquals("scope2", eval(cx, scope1, "var t=scope2.test; t()"));
                                assertEquals(
                                        "scope2",
                                        eval(cx, scope1, "var t=scope2.test; t.call(null)"));
                            }
                        }));
    }

    @Test
    public void getPropNoWarn1() {
        String fn = "function test(){ if (''.foo) return true; return false; }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                eval(cx, scope1, "String.prototype.foo = 'scope1'");

                                assertFALSE(eval(cx, scope2, "test()"));
                                assertFALSE(eval(cx, scope1, "scope2.test()"));
                                assertFALSE(eval(cx, scope1, "scope2.test.call(null)"));
                                assertFALSE(eval(cx, scope1, "var t=scope2.test; t()"));
                                assertFALSE(eval(cx, scope1, "var t=scope2.test; t.call(null)"));
                            }
                        }));
    }

    @Test
    public void getPropNoWarn2() {
        String fn = "function test(){ if (''.foo) return true; return false; }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                eval(cx, scope2, "String.prototype.foo = 'scope2'");

                                assertTRUE(eval(cx, scope2, "test()"));
                                assertTRUE(eval(cx, scope1, "scope2.test()"));
                                assertTRUE(eval(cx, scope1, "scope2.test.call(null)"));
                                assertTRUE(eval(cx, scope1, "var t=scope2.test; t()"));
                                assertTRUE(eval(cx, scope1, "var t=scope2.test; t.call(null)"));
                            }
                        }));
    }

    @Test
    public void setElem() {
        String fn = "function test(){ ''['foo'] = '_' }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                String code = "";
                                code += "String.prototype.c = 0;";
                                code +=
                                        "Object.defineProperty(String.prototype, 'foo', {"
                                                + "  set: function(v){ this.__proto__.c++ }})";
                                eval(cx, scope1, code);
                                eval(cx, scope2, code);

                                eval(cx, scope2, "test()");
                                eval(cx, scope1, "scope2.test()");
                                eval(cx, scope1, "scope2.test.call(null)");
                                eval(cx, scope1, "var t=scope2.test; t()");
                                eval(cx, scope1, "var t=scope2.test; t.call(null)");

                                assertTRUE(eval(cx, scope1, "0 == String.prototype.c"));
                                assertTRUE(eval(cx, scope2, "5 == String.prototype.c"));
                            }
                        }));
    }

    @Test
    public void setProp() {
        String fn = "function test(){ ''.foo = '_' }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                String code = "";
                                code += "String.prototype.c = 0;";
                                code +=
                                        "Object.defineProperty(String.prototype, 'foo', {"
                                                + "  set: function(v){ this.__proto__.c++ }})";
                                eval(cx, scope1, code);
                                eval(cx, scope2, code);

                                eval(cx, scope2, "test()");
                                eval(cx, scope1, "scope2.test()");
                                eval(cx, scope1, "scope2.test.call(null)");
                                eval(cx, scope1, "var t=scope2.test; t()");
                                eval(cx, scope1, "var t=scope2.test; t.call(null)");

                                assertTRUE(eval(cx, scope1, "0 == String.prototype.c"));
                                assertTRUE(eval(cx, scope2, "5 == String.prototype.c"));
                            }
                        }));
    }

    @Test
    public void setElemIncDec() {
        String fn = "function test(){ ''['foo']++ }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                String code = "";
                                code += "String.prototype.c = 0;";
                                code +=
                                        "Object.defineProperty(String.prototype, 'foo', {"
                                                + "  set: function(v){ this.__proto__.c++ }})";
                                eval(cx, scope1, code);
                                eval(cx, scope2, code);

                                eval(cx, scope2, "test()");
                                eval(cx, scope1, "scope2.test()");
                                eval(cx, scope1, "scope2.test.call(null)");
                                eval(cx, scope1, "var t=scope2.test; t()");
                                eval(cx, scope1, "var t=scope2.test; t.call(null)");

                                assertTRUE(eval(cx, scope1, "0 == String.prototype.c"));
                                assertTRUE(eval(cx, scope2, "5 == String.prototype.c"));
                            }
                        }));
    }

    @Test
    public void setPropIncDec() {
        String fn = "function test(){ ''.foo++ }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                String code = "";
                                code += "String.prototype.c = 0;";
                                code +=
                                        "Object.defineProperty(String.prototype, 'foo', {"
                                                + "  set: function(v){ this.__proto__.c++ }})";
                                eval(cx, scope1, code);
                                eval(cx, scope2, code);

                                eval(cx, scope2, "test()");
                                eval(cx, scope1, "scope2.test()");
                                eval(cx, scope1, "scope2.test.call(null)");
                                eval(cx, scope1, "var t=scope2.test; t()");
                                eval(cx, scope1, "var t=scope2.test; t.call(null)");

                                assertTRUE(eval(cx, scope1, "0 == String.prototype.c"));
                                assertTRUE(eval(cx, scope2, "5 == String.prototype.c"));
                            }
                        }));
    }

    @Test
    public void setElemOp1() {
        String fn = "function test(){ return ''['foo'] += '_' }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                String code = "";
                                code += "String.prototype.c = 0;";
                                code +=
                                        "Object.defineProperty(String.prototype, 'foo', {"
                                                + "  set: function(v){ this.__proto__.c++ }})";
                                eval(cx, scope1, code);
                                eval(cx, scope2, code);

                                eval(cx, scope2, "test()");
                                eval(cx, scope1, "scope2.test()");
                                eval(cx, scope1, "scope2.test.call(null)");
                                eval(cx, scope1, "var t=scope2.test; t()");
                                eval(cx, scope1, "var t=scope2.test; t.call(null)");

                                assertTRUE(eval(cx, scope1, "0 == String.prototype.c"));
                                assertTRUE(eval(cx, scope2, "5 == String.prototype.c"));
                            }
                        }));
    }

    @Test
    public void setPropOp1() {
        String fn = "function test(){ return ''.foo += '_' }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                String code = "";
                                code += "String.prototype.c = 0;";
                                code += "String.prototype.d = 0;";
                                code +=
                                        "Object.defineProperty(String.prototype, 'foo', {"
                                                + "  set: function(v){ this.__proto__.c++ },"
                                                + "  get: function(v){ this.__proto__.d++ }})";
                                eval(cx, scope1, code);
                                eval(cx, scope2, code);

                                eval(cx, scope2, "test()");
                                eval(cx, scope1, "scope2.test()");
                                eval(cx, scope1, "scope2.test.call(null)");
                                eval(cx, scope1, "var t=scope2.test; t()");
                                eval(cx, scope1, "var t=scope2.test; t.call(null)");

                                assertTRUE(eval(cx, scope1, "0 == String.prototype.c"));
                                assertTRUE(eval(cx, scope2, "5 == String.prototype.c"));
                                assertTRUE(eval(cx, scope1, "0 == String.prototype.d"));
                                assertTRUE(eval(cx, scope2, "5 == String.prototype.d"));
                            }
                        }));
    }

    @Test
    public void setElemOp2() {
        String fn = "function test(){ return ''['foo'] += '_' }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                String code = "";
                                code += "String.prototype.c = 0;";
                                code += "String.prototype.d = 0;";
                                code +=
                                        "Object.defineProperty(String.prototype, 'foo', {"
                                                + "  set: function(v){ this.__proto__.c++ },"
                                                + "  get: function(v){ this.__proto__.d++ }})";
                                eval(cx, scope1, code);
                                eval(cx, scope2, code);

                                eval(cx, scope2, "test()");
                                eval(cx, scope1, "scope2.test()");
                                eval(cx, scope1, "scope2.test.call(null)");
                                eval(cx, scope1, "var t=scope2.test; t()");
                                eval(cx, scope1, "var t=scope2.test; t.call(null)");

                                assertTRUE(eval(cx, scope1, "0 == String.prototype.c"));
                                assertTRUE(eval(cx, scope2, "5 == String.prototype.c"));
                                assertTRUE(eval(cx, scope1, "0 == String.prototype.d"));
                                assertTRUE(eval(cx, scope2, "5 == String.prototype.d"));
                            }
                        }));
    }

    @Test
    public void setPropOp2() {
        String fn = "function test(){ return ''.foo += '_' }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                eval(cx, scope1, "String.prototype.foo = 'scope1'");
                                eval(cx, scope2, "String.prototype.foo = 'scope2'");

                                assertEquals("scope2_", eval(cx, scope2, "test()"));
                                assertEquals("scope2_", eval(cx, scope1, "scope2.test()"));
                                assertEquals("scope2_", eval(cx, scope1, "scope2.test.call(null)"));
                                assertEquals("scope2_", eval(cx, scope1, "var t=scope2.test; t()"));
                                assertEquals(
                                        "scope2_",
                                        eval(cx, scope1, "var t=scope2.test; t.call(null)"));
                            }
                        }));
    }

    @Test
    public void getElemCall() {
        String fn = "function test(){ return ''['foo']() }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                eval(
                                        cx,
                                        scope1,
                                        "String.prototype.foo = function(){ return 'scope1' }");
                                eval(
                                        cx,
                                        scope2,
                                        "String.prototype.foo = function(){ return 'scope2' }");

                                assertEquals("scope2", eval(cx, scope2, "test()"));
                                assertEquals("scope2", eval(cx, scope1, "scope2.test()"));
                                assertEquals("scope2", eval(cx, scope1, "scope2.test.call(null)"));
                                assertEquals("scope2", eval(cx, scope1, "var t=scope2.test; t()"));
                                assertEquals(
                                        "scope2",
                                        eval(cx, scope1, "var t=scope2.test; t.call(null)"));
                            }
                        }));
    }

    @Test
    public void getPropCall() {
        String fn = "function test(){ return ''.foo() }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                eval(
                                        cx,
                                        scope1,
                                        "String.prototype.foo = function(){ return 'scope1' }");
                                eval(
                                        cx,
                                        scope2,
                                        "String.prototype.foo = function(){ return 'scope2' }");

                                assertEquals("scope2", eval(cx, scope2, "test()"));
                                assertEquals("scope2", eval(cx, scope1, "scope2.test()"));
                                assertEquals("scope2", eval(cx, scope1, "scope2.test.call(null)"));
                                assertEquals("scope2", eval(cx, scope1, "var t=scope2.test; t()"));
                                assertEquals(
                                        "scope2",
                                        eval(cx, scope1, "var t=scope2.test; t.call(null)"));
                            }
                        }));
    }

    @Test
    public void enum1() {
        String fn =
                "function test(){ for (var k in '') if (k == 'foo') return true; return false; }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                eval(cx, scope1, "String.prototype.foo = 'scope1'");

                                assertFALSE(eval(cx, scope2, "test()"));
                                assertFALSE(eval(cx, scope1, "scope2.test()"));
                                assertFALSE(eval(cx, scope1, "scope2.test.call(null)"));
                                assertFALSE(eval(cx, scope1, "var t=scope2.test; t()"));
                                assertFALSE(eval(cx, scope1, "var t=scope2.test; t.call(null)"));
                            }
                        }));
    }

    @Test
    public void enum2() {
        String fn =
                "function test(){ for (var k in '') if (k == 'foo') return true; return false; }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                eval(cx, scope2, "String.prototype.foo = 'scope1'");

                                assertTRUE(eval(cx, scope2, "test()"));
                                assertTRUE(eval(cx, scope1, "scope2.test()"));
                                assertTRUE(eval(cx, scope1, "scope2.test.call(null)"));
                                assertTRUE(eval(cx, scope1, "var t=scope2.test; t()"));
                                assertTRUE(eval(cx, scope1, "var t=scope2.test; t.call(null)"));
                            }
                        }));
    }

    @Test
    public void parent() {
        String fn = "function test(){}";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                assertSame(scope2, eval(cx, scope2, "test.__parent__"));
                                assertSame(scope2, eval(cx, scope1, "scope2.test.__parent__"));
                                assertSame(
                                        scope2,
                                        eval(cx, scope1, "var t=scope2.test; t.__parent__"));
                            }
                        }));
    }

    @Test
    public void returnThis() {
        String fn = "function test(){ return this }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                assertSame(scope2, eval(cx, scope2, "test()"));
                                assertSame(scope2, eval(cx, scope2, "test.call(null)"));
                                assertSame(scope2, eval(cx, scope1, "scope2.test()"));
                                assertSame(scope1, eval(cx, scope1, "scope2.test.call(null)"));
                                assertSame(scope1, eval(cx, scope1, "var t=scope2.test; t()"));
                                assertSame(
                                        scope1,
                                        eval(cx, scope1, "var t=scope2.test; t.call(null)"));
                            }
                        }));
    }

    @Test
    public void returnThisNested() {
        String fn = "function test(){ return (function(){ return this })() }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                assertSame(scope2, eval(cx, scope2, "test()"));
                                assertSame(scope2, eval(cx, scope2, "test.call(null)"));
                                assertSame(scope2, eval(cx, scope1, "scope2.test()"));
                                assertSame(scope2, eval(cx, scope1, "scope2.test.call(null)"));
                                assertSame(scope2, eval(cx, scope1, "var t=scope2.test; t()"));
                                assertSame(
                                        scope2,
                                        eval(cx, scope1, "var t=scope2.test; t.call(null)"));
                            }
                        }));
    }

    @Test
    public void returnThisNestedCall() {
        String fn = "function test(o){ return (function(){ return this }).call(o) }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                assertSame(scope2, eval(cx, scope2, "test()"));
                                assertSame(scope2, eval(cx, scope2, "test(null)"));
                                assertSame(scope2, eval(cx, scope2, "test.call(null)"));
                                assertSame(scope2, eval(cx, scope2, "test.call(null, null)"));

                                assertSame(scope1, eval(cx, scope1, "scope2.test()"));
                                assertSame(scope1, eval(cx, scope1, "scope2.test(null)"));
                                assertSame(scope1, eval(cx, scope1, "scope2.test.call(null)"));
                                assertSame(
                                        scope1, eval(cx, scope1, "scope2.test.call(null, null)"));

                                assertSame(scope1, eval(cx, scope1, "var t=scope2.test; t()"));
                                assertSame(scope1, eval(cx, scope1, "var t=scope2.test; t(null)"));
                                assertSame(
                                        scope1,
                                        eval(cx, scope1, "var t=scope2.test; t.call(null)"));
                                assertSame(
                                        scope1,
                                        eval(cx, scope1, "var t=scope2.test; t.call(null, null)"));
                            }
                        }));
    }

    @Test
    public void nameStringPrototype() {
        String fn = "function test(){ return String.prototype }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                assertTRUE(eval(cx, scope2, "String.prototype === test()"));
                                assertTRUE(
                                        eval(cx, scope2, "String.prototype === test.call(null)"));
                                assertFALSE(eval(cx, scope1, "String.prototype === scope2.test()"));
                                assertFALSE(
                                        eval(
                                                cx,
                                                scope1,
                                                "String.prototype === scope2.test.call(null)"));
                                assertFALSE(
                                        eval(
                                                cx,
                                                scope1,
                                                "var t=scope2.test; String.prototype === t()"));
                                assertFALSE(
                                        eval(
                                                cx,
                                                scope1,
                                                "var t=scope2.test; String.prototype === t.call(null)"));
                            }
                        }));
    }

    @Test
    public void nameStringPrototypeNested() {
        String fn = "function test(){ return (function(){ return String.prototype })() }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                assertTRUE(eval(cx, scope2, "String.prototype === test()"));
                                assertTRUE(
                                        eval(cx, scope2, "String.prototype === test.call(null)"));
                                assertFALSE(eval(cx, scope1, "String.prototype === scope2.test()"));
                                assertFALSE(
                                        eval(
                                                cx,
                                                scope1,
                                                "String.prototype === scope2.test.call(null)"));
                                assertFALSE(
                                        eval(
                                                cx,
                                                scope1,
                                                "var t=scope2.test; String.prototype === t()"));
                                assertFALSE(
                                        eval(
                                                cx,
                                                scope1,
                                                "var t=scope2.test; String.prototype === t.call(null)"));
                            }
                        }));
    }

    @Test
    public void thisStringPrototype() {
        String fn = "function test(){ return this.String.prototype }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                assertTRUE(eval(cx, scope2, "String.prototype === test()"));
                                assertTRUE(
                                        eval(cx, scope2, "String.prototype === test.call(null)"));
                                assertFALSE(eval(cx, scope1, "String.prototype === scope2.test()"));
                                assertTRUE(
                                        eval(
                                                cx,
                                                scope1,
                                                "String.prototype === scope2.test.call(null)"));
                                assertTRUE(
                                        eval(
                                                cx,
                                                scope1,
                                                "var t=scope2.test; String.prototype === t()"));
                                assertTRUE(
                                        eval(
                                                cx,
                                                scope1,
                                                "var t=scope2.test; String.prototype === t.call(null)"));
                            }
                        }));
    }

    @Test
    public void thisProto() {
        String fn = "function test(){ return this.__proto__ }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                assertTRUE(eval(cx, scope2, "String.prototype === test.call('')"));
                                assertTRUE(
                                        eval(
                                                cx,
                                                scope1,
                                                "String.prototype === scope2.test.call('')"));
                                assertTRUE(
                                        eval(
                                                cx,
                                                scope1,
                                                "var t=scope2.test; String.prototype === t.call('')"));
                            }
                        }));
    }

    @Test
    public void stringLiteralProto() {
        String fn = "function test(){ return ''.__proto__ }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                assertTRUE(eval(cx, scope2, "String.prototype === test()"));
                                assertTRUE(
                                        eval(cx, scope2, "String.prototype === test.call(null)"));
                                assertFALSE(eval(cx, scope1, "String.prototype === scope2.test()"));
                                assertFALSE(
                                        eval(
                                                cx,
                                                scope1,
                                                "String.prototype === scope2.test.call(null)"));
                                assertFALSE(
                                        eval(
                                                cx,
                                                scope1,
                                                "var t=scope2.test; String.prototype === t()"));
                                assertFALSE(
                                        eval(
                                                cx,
                                                scope1,
                                                "var t=scope2.test; String.prototype === t.call(null)"));
                            }
                        }));
    }

    @Test
    public void thisProtoNested() {
        String fn = "function test(){ return (function(){ return this.__proto__ }).call('') }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                assertTRUE(eval(cx, scope2, "String.prototype === test()"));
                                assertTRUE(
                                        eval(cx, scope2, "String.prototype === test.call(null)"));
                                assertFALSE(eval(cx, scope1, "String.prototype === scope2.test()"));
                                assertFALSE(
                                        eval(
                                                cx,
                                                scope1,
                                                "String.prototype === scope2.test.call(null)"));
                                assertFALSE(
                                        eval(
                                                cx,
                                                scope1,
                                                "var t=scope2.test; String.prototype === t()"));
                                assertFALSE(
                                        eval(
                                                cx,
                                                scope1,
                                                "var t=scope2.test; String.prototype === t.call(null)"));
                            }
                        }));
    }

    @Test
    public void stringLiteralProtoNested() {
        String fn = "function test(){ return (function(){ return ''.__proto__ })() }";
        runWithAllOptimizationLevels(
                action(
                        fn,
                        new Action() {
                            @Override
                            public void run(
                                    Context cx, ScriptableObject scope1, ScriptableObject scope2) {
                                assertTRUE(eval(cx, scope2, "String.prototype === test()"));
                                assertTRUE(
                                        eval(cx, scope2, "String.prototype === test.call(null)"));
                                assertFALSE(eval(cx, scope1, "String.prototype === scope2.test()"));
                                assertFALSE(
                                        eval(
                                                cx,
                                                scope1,
                                                "String.prototype === scope2.test.call(null)"));
                                assertFALSE(
                                        eval(
                                                cx,
                                                scope1,
                                                "var t=scope2.test; String.prototype === t()"));
                                assertFALSE(
                                        eval(
                                                cx,
                                                scope1,
                                                "var t=scope2.test; String.prototype === t.call(null)"));
                            }
                        }));
    }
}
