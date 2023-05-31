package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

/**
 * This is a set of tests for the case in which functions of the built-in Array type are used with a
 * native Java class.
 */
public class NativeWrappedArrayTest {

    private Context cx;
    private Scriptable global;

    @Before
    public void init() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        cx.getWrapFactory().setJavaPrimitiveWrap(false);
        global = new Global(cx);
    }

    @After
    public void terminate() {
        Context.exit();
    }

    @Test
    public void nativeArray() throws IOException {
        final String setFunc = "function makeTestArray() { return makeNativeArray(); }";
        cx.evaluateString(global, setFunc, "setfunc.js", 1, null);

        try (InputStreamReader rdr =
                new InputStreamReader(new FileInputStream("testsrc/jstests/wrapped-arrays.js"))) {
            Object ret = cx.evaluateReader(global, rdr, "wrapped-arrays.js", 1, null);
            assertEquals("success", ret);
        } catch (RhinoException re) {
            fail(re.getMessage() + '\n' + re.getScriptStackTrace());
        }
    }

    @Test
    public void arrayLikeArray() throws IOException {
        final String setFunc = "function makeTestArray() { return makeArrayLikeArray(); }";
        cx.evaluateString(global, setFunc, "setfunc.js", 1, null);

        try (InputStreamReader rdr =
                new InputStreamReader(new FileInputStream("testsrc/jstests/wrapped-arrays.js"))) {
            Object ret = cx.evaluateReader(global, rdr, "wrapped-arrays.js", 1, null);
            assertEquals("success", ret);
        } catch (RhinoException re) {
            fail(re.getMessage() + '\n' + re.getScriptStackTrace());
        }
    }

    @Test
    public void javaArray() throws IOException {
        final String setFunc = "function makeTestArray() { return makeJavaArray(); }";
        cx.evaluateString(global, setFunc, "setfunc.js", 1, null);

        try (InputStreamReader rdr =
                new InputStreamReader(new FileInputStream("testsrc/jstests/wrapped-arrays.js"))) {
            Object ret = cx.evaluateReader(global, rdr, "wrapped-arrays.js", 1, null);
            assertEquals("success", ret);
        } catch (RhinoException re) {
            fail(re.getMessage() + '\n' + re.getScriptStackTrace());
        }
    }

    @Test
    public void customArray() throws IOException {
        ((ScriptableObject) global)
                .defineFunctionProperties(
                        new String[] {"makeCustomArray"}, NativeWrappedArrayTest.class, 0);

        final String setFunc = "function makeTestArray() { return makeCustomArray(); }";
        cx.evaluateString(global, setFunc, "setfunc.js", 1, null);

        try (InputStreamReader rdr =
                new InputStreamReader(new FileInputStream("testsrc/jstests/wrapped-arrays.js"))) {
            Object ret = cx.evaluateReader(global, rdr, "wrapped-arrays.js", 1, null);
            assertEquals("success", ret);
        } catch (RhinoException re) {
            fail(re.getMessage() + '\n' + re.getScriptStackTrace());
        }
    }

    @SuppressWarnings("unused")
    public static Object makeCustomArray(
            Context cx, Scriptable thisObj, Object[] args, Function fn) {
        ArrayList<String> a = new ArrayList<>(4);
        a.add("one");
        a.add("two");
        a.add("three");
        a.add("four");
        return new WrappedArray(thisObj, a);
    }

    static class WrappedArray extends ScriptableObject {

        private final ArrayList<String> list;
        private int length;

        WrappedArray(Scriptable scope, ArrayList<String> l) {
            super(scope, ScriptableObject.getArrayPrototype(scope));
            this.list = l;
            this.length = l.size();
        }

        @Override
        public String getClassName() {
            return null;
        }

        @Override
        public Object get(int ix, Scriptable start) {
            return list.get(ix);
        }

        @Override
        public Object get(String n, Scriptable start) {
            if ("length".equals(n)) {
                return length;
            }
            return Scriptable.NOT_FOUND;
        }

        @Override
        public void put(int ix, Scriptable start, Object val) {
            // Ensure enough capacity for array expansion
            // by automatically expanding the array like JavaScript sort of does.
            for (int ax = list.size(); ax <= ix; ax++) {
                list.add(null);
            }
            list.set(ix, val.toString());
        }

        @Override
        public void put(String n, Scriptable start, Object val) {
            if ("length".equals(n)) {
                length = ScriptRuntime.toInt32(val);
            }
        }

        @Override
        public void delete(int ix) {
            list.set(ix, null);
        }

        @Override
        public Object[] getIds() {
            Object[] ids = new Object[list.size()];
            for (int i = 0; i < list.size(); i++) {
                ids[i] = i;
            }
            return ids;
        }
    }
}
