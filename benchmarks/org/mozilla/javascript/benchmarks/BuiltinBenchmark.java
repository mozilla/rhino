package org.mozilla.javascript.benchmarks;

import java.lang.reflect.InvocationTargetException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.annotations.*;
import org.openjdk.jmh.annotations.*;

public class BuiltinBenchmark {

    @State(Scope.Thread)
    public static class AbstractClassState {

        public void init()
                throws IllegalAccessException, InvocationTargetException, InstantiationException {
            cx = Context.enter();
            cx.setOptimizationLevel(9);
            cx.setLanguageVersion(Context.VERSION_ES6);

            scope = cx.initStandardObjects();
            ScriptableObject.defineClass(scope, AnnotatedClass.class);
            IdClass.init(scope);
            DumbLambdaClass.init(scope);
        }

        void compileScript(String testClassName) {
            testScript =
                    cx.compileString(
                            "o = new "
                                    + testClassName
                                    + "();\n"
                                    + "o.setValue(99);\n"
                                    + "for (var i = 0; i < 1000; i++) {\n"
                                    + "if (o.getValue() !== 99) { throw 'Not working!'; }\n"
                                    + "}\n"
                                    + "o.getValue();",
                            "test.js",
                            1,
                            null);
        }

        @TearDown(Level.Trial)
        public void close() {
            Context.exit();
        }

        Context cx;
        Scriptable scope;
        Script testScript;
    }

    @State(Scope.Thread)
    public static class AnnotatedClassState extends AbstractClassState {

        @Override
        @Setup(Level.Trial)
        public void init()
                throws IllegalAccessException, InvocationTargetException, InstantiationException {
            super.init();
            compileScript("AnnotatedClass");
        }
    }

    @Benchmark
    public Object annotatedClassMethods(AnnotatedClassState state) {
        return state.testScript.exec(state.cx, state.scope);
    }

    public static class AnnotatedClass extends ScriptableObject {

        @Override
        public String getClassName() {
            return "AnnotatedClass";
        }

        @JSFunction
        public void one() {}

        @JSFunction
        public void two() {}

        @JSFunction
        public void three() {}

        @JSFunction
        public void four() {}

        @JSFunction
        public void five() {}

        @JSFunction
        public void six() {}

        @JSFunction
        public void seven() {}

        @JSFunction
        public void eight() {}

        @JSFunction
        public void nine() {}

        @JSFunction
        public void setValue(int value) {
            this.value = value;
        }

        @JSFunction
        public int getValue() {
            return value;
        }

        private int value;
    }

    @State(Scope.Thread)
    public static class IdClassState extends AbstractClassState {

        @Override
        @Setup(Level.Trial)
        public void init()
                throws IllegalAccessException, InvocationTargetException, InstantiationException {
            super.init();
            compileScript("IdClass");
        }
    }

    @Benchmark
    public Object idClassMethods(IdClassState state) {
        return state.testScript.exec(state.cx, state.scope);
    }

    public static class IdClass extends IdScriptableObject {

        private static final String TAG = "IdClass";

        public static void init(Scriptable scope) {
            IdClass idc = new IdClass();
            idc.exportAsJSClass(MAX_ID, scope, false);
        }

        @Override
        public String getClassName() {
            return "IdClass";
        }

        @Override
        protected void initPrototypeId(int id) {
            String s, fnName = null;
            int arity;
            switch (id) {
                case Id_one:
                    arity = 0;
                    s = "one";
                    break;
                case Id_two:
                    arity = 0;
                    s = "two";
                    break;
                case Id_three:
                    arity = 0;
                    s = "three";
                    break;
                case Id_four:
                    arity = 0;
                    s = "four";
                    break;
                case Id_five:
                    arity = 0;
                    s = "five";
                    break;
                case Id_six:
                    arity = 0;
                    s = "six";
                    break;
                case Id_seven:
                    arity = 0;
                    s = "seven";
                    break;
                case Id_eight:
                    arity = 0;
                    s = "eight";
                    break;
                case Id_nine:
                    arity = 0;
                    s = "nine";
                    break;
                case Id_setValue:
                    arity = 1;
                    s = "setValue";
                    break;
                case Id_getValue:
                    arity = 0;
                    s = "getValue";
                    break;
                case Id_constructor:
                    arity = 0;
                    s = "constructor";
                    break;
                default:
                    throw new IllegalArgumentException(String.valueOf(id));
            }

            initPrototypeMethod(TAG, id, s, fnName, arity);
        }

        @Override
        public Object execIdCall(
                IdFunctionObject f,
                Context cx,
                Scriptable scope,
                Scriptable thisObj,
                Object[] args) {
            if (!f.hasTag(TAG)) {
                return super.execIdCall(f, cx, scope, thisObj, args);
            }
            int id = f.methodId();
            IdClass self;
            switch (id) {
                case Id_constructor:
                    return new IdClass();
                case Id_setValue:
                    self = (IdClass) thisObj;
                    if (args.length < 1) {
                        throw ScriptRuntime.throwError(cx, scope, "not enough args");
                    }
                    self.value = ScriptRuntime.toInt32(args[0]);
                    break;
                case Id_getValue:
                    self = (IdClass) thisObj;
                    return self.value;
                default:
                    throw new IllegalArgumentException(
                            "Array.prototype has no method: " + f.getFunctionName());
            }
            return Undefined.instance;
        }

        // #string_id_map#

        @Override
        protected int findPrototypeId(String s) {
            int id;
            // #generated# Last update: 2021-04-13 16:17:26 PDT
            switch (s) {
                case "one":
                    id = Id_one;
                    break;
                case "two":
                    id = Id_two;
                    break;
                case "three":
                    id = Id_three;
                    break;
                case "four":
                    id = Id_four;
                    break;
                case "five":
                    id = Id_five;
                    break;
                case "six":
                    id = Id_six;
                    break;
                case "seven":
                    id = Id_seven;
                    break;
                case "eight":
                    id = Id_eight;
                    break;
                case "nine":
                    id = Id_nine;
                    break;
                case "getValue":
                    id = Id_getValue;
                    break;
                case "setValue":
                    id = Id_setValue;
                    break;
                case "constructor":
                    id = Id_constructor;
                    break;
                default:
                    id = 0;
                    break;
            }
            // #/generated#
            return id;
        }

        private static final int Id_one = 1,
                Id_two = 2,
                Id_three = 3,
                Id_four = 4,
                Id_five = 5,
                Id_six = 6,
                Id_seven = 7,
                Id_eight = 8,
                Id_nine = 9,
                Id_getValue = 10,
                Id_setValue = 11,
                Id_constructor = 12,
                MAX_ID = Id_constructor;

        // #/string_id_map#

        private int value;
    }

    @State(Scope.Thread)
    public static class DumbLambdaState extends AbstractClassState {

        @Override
        @Setup(Level.Trial)
        public void init()
                throws IllegalAccessException, InvocationTargetException, InstantiationException {
            super.init();
            compileScript("DumbLambdaClass");
        }
    }

    @Benchmark
    public Object dumbLambdaClassMethods(DumbLambdaState state) {
        return state.testScript.exec(state.cx, state.scope);
    }

    private static class DumbLambdaClass extends ScriptableObject {

        private static Object noop(
                Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            return Undefined.instance;
        }

        private static Object setValue(
                Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            if (args.length < 1) {
                throw ScriptRuntime.throwError(cx, scope, "Not enough args");
            }
            DumbLambdaClass self =
                    LambdaConstructor.convertThisObject(thisObj, DumbLambdaClass.class);
            self.value = ScriptRuntime.toInt32(args[0]);
            return Undefined.instance;
        }

        private static Object getValue(
                Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            DumbLambdaClass self =
                    LambdaConstructor.convertThisObject(thisObj, DumbLambdaClass.class);
            return self.value;
        }

        public static void init(Scriptable scope) {
            LambdaConstructor cons =
                    new LambdaConstructor(
                            scope,
                            "DumbLambdaClass",
                            0,
                            (Context cx, Scriptable s, Object[] args) -> new DumbLambdaClass());
            cons.definePrototypeMethod(scope, "one", 0, DumbLambdaClass::noop);
            cons.definePrototypeMethod(scope, "two", 0, DumbLambdaClass::noop);
            cons.definePrototypeMethod(scope, "three", 0, DumbLambdaClass::noop);
            cons.definePrototypeMethod(scope, "four", 0, DumbLambdaClass::noop);
            cons.definePrototypeMethod(scope, "five", 0, DumbLambdaClass::noop);
            cons.definePrototypeMethod(scope, "six", 0, DumbLambdaClass::noop);
            cons.definePrototypeMethod(scope, "seven", 0, DumbLambdaClass::noop);
            cons.definePrototypeMethod(scope, "eight", 0, DumbLambdaClass::noop);
            cons.definePrototypeMethod(scope, "nine", 0, DumbLambdaClass::noop);
            cons.definePrototypeMethod(scope, "setValue", 1, DumbLambdaClass::setValue);
            cons.definePrototypeMethod(scope, "getValue", 1, DumbLambdaClass::getValue);
            ScriptableObject.putProperty(scope, "DumbLambdaClass", cons);
        }

        @Override
        public String getClassName() {
            return "DumbLambdaClass";
        }

        private int value;
    }
}
