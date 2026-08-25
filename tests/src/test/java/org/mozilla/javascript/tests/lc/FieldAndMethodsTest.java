package org.mozilla.javascript.tests.lc;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.testutils.Utils;

public class FieldAndMethodsTest {

    @Test
    public void test() {
        Utils.runWithAllModes(
                cx -> {
                    this.fieldAndMethods = "init";
                    this.methodCallResult = -1;

                    var topLevel = cx.initStandardObjects();

                    topLevel.put("test", topLevel, Context.javaToJS(this, topLevel));

                    return cx.evaluateString(
                            topLevel,
                            """
                let assert = test.asser

                assert(test.fieldAndMethods(42) == -1, "method call");
                assert(test.fieldAndMethods(0) == 42, "method call");

                assert('' + test.fieldAndMethods == 'init', "field get should grab value from field");
                test.fieldAndMethods = 'altered';
                assert('' + test.fieldAndMethods == 'altered', "field set should take effect");

                """,
                            "FieldsAndMethodTest",
                            1,
                            null);
                });
    }

    /**
     * see <a
     * href="https://rhino.github.io/tutorials/scripting_java/#limitations">rhino.github.io</a>
     */
    @Test
    public void lazyField() {
        Utils.runWithAllModes(
                cx -> {
                    var topLevel = cx.initStandardObjects();

                    topLevel.put("test", topLevel, Context.javaToJS(this, topLevel));

                    return cx.evaluateString(
                            topLevel,
                            """
                let assert = test.asser

                test.fieldAndMethods = 'init';

                let fam = test.fieldAndMethods
                let forceConverted = '' + fam
                assert('' + fam == 'init', "field get, obviously");
                assert(forceConverted == 'init', "force conversion now");

                test.fieldAndMethods = 'altered';
                assert('' + fam == 'altered', "value of field is resolved lazily");
                assert(forceConverted == 'init', "but forcefully converted value is not affected");

                """,
                            "FieldsAndMethodTest",
                            1,
                            null);
                });
    }

    public String fieldAndMethods;
    private int methodCallResult;

    public int fieldAndMethods(int i) {
        var last = methodCallResult;
        methodCallResult = i;
        return last;
    }

    public void aMethodThatWillAffectFieldAndMethods(String value) {
        this.fieldAndMethods = value;
    }

    public static void asser(boolean t, String errMessage) {
        if (!t) {
            throw new AssertionError(errMessage);
        }
    }
}
