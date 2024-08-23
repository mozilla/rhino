package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.mozilla.javascript.LambdaConstructor.CONSTRUCTOR_FUNCTION;
import static org.mozilla.javascript.LambdaConstructor.CONSTRUCTOR_NEW;

import org.junit.Test;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class InterpreterFunctionPeelingNonRegressionTest {
    @Test
    public void testLambdaFunction() {
        Utils.runWithAllOptimizationLevels(cx -> {
            Scriptable scope = cx.initStandardObjects();

            LambdaFunction makePerson =
                    new LambdaFunction(
                            scope,
                            "makePerson",
                            1,
                            (cx1, scope1, thisObj, args) ->
                                    new Person(ScriptRuntime.toString(args[0])));
            scope.put("makePerson", scope, makePerson);

            String source =
                    "function testLambdaFunction() {\n"
                            + "   return makePerson('Andrea');\n"
                            + "}\n"
                            + "testLambdaFunction().name";
            Object value = cx.evaluateString(scope, source, "test", 1, null);
            assertEquals("Andrea", value);
            
            return null;
        });
    }

    @Test
    public void testLambdaConstructorAsFunction() {
        Utils.runWithAllOptimizationLevels(cx -> {
            Scriptable scope = cx.initStandardObjects();

            LambdaConstructor personCtor =
                    new LambdaConstructor(
                            scope,
                            "Person",
                            1,
                            CONSTRUCTOR_FUNCTION,
                            (cx1, scope1, args) -> new Person(ScriptRuntime.toString(args[0])));
            scope.put("Person", scope, personCtor);

            String source =
                    "function testLambdaConstructor() {\n"
                            + "  return Person('Andrea');\n"
                            + "}\n"
                            + "testLambdaConstructor().name";
            Object value = cx.evaluateString(scope, source, "test", 1, null);
            assertEquals("Andrea", value);
            
            return null;
        });
    }

    @Test
    public void testLambdaConstructorViaNew() {
        Utils.runWithAllOptimizationLevels(cx -> {
            Scriptable scope = cx.initStandardObjects();

            LambdaConstructor personCtor =
                    new LambdaConstructor(
                            scope,
                            "Person",
                            1,
                            CONSTRUCTOR_NEW,
                            (cx1, scope1, args) -> new Person(ScriptRuntime.toString(args[0])));
            scope.put("Person", scope, personCtor);

            String source =
                    "function testLambdaConstructor() {\n"
                            + "  return new Person('Andrea');\n"
                            + "}\n"
                            + "testLambdaConstructor().name";
            Object value = cx.evaluateString(scope, source, "test", 1, null);
            assertEquals("Andrea", value);

            return null;
        });
    }

    static class Person extends ScriptableObject {
        private final String name;

        public Person(String name) {
            this.name = name;
        }

        @Override
        public String getClassName() {
            return "Person";
        }

        @Override
        public Object get(String name, Scriptable start) {
            if (name.equals("name")) {
                return this.name;
            } else {
                return super.get(name, start);
            }
        }
    }
}
