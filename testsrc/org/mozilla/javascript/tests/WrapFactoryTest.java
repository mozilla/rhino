package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/** @author hatanaka */
public class WrapFactoryTest {
    /** javascript code */
    private static String script =
            "var result = typeof test;" //
                    + "var mapResult = typeof map.get('test');" //
                    + "var getResult = typeof object.get();";

    /** for your reference default setting (javaPrimitiveWrap = true) */
    @Test
    public void primitiveWrapTrue() {
        test(true, "text", "string", "object", "object");
        test(true, Boolean.FALSE, "boolean", "object", "object");
        test(true, new Integer(1), "number", "object", "object");
        test(true, new Long(2L), "number", "object", "object");
        test(true, new BigInteger("3"), "bigint", "object", "object"); // TODO: compatibility issue
        test(true, new BigDecimal("4.0"), "number", "object", "object");
    }

    /** javaPrimitiveWrap = false */
    @Test
    public void primitiveWrapFalse() {
        test(false, "text", "string", "string", "string"); // Great! I want to do this.
        test(false, Boolean.FALSE, "boolean", "boolean", "boolean");
        test(false, new Integer(1), "number", "number", "number");
        test(false, new Long(2L), "number", "number", "number");

        // I want to treat BigInteger / BigDecimal as BigInteger / BigDecimal. But fails.
        test(
                false,
                new BigInteger("30"),
                "bigint",
                "object",
                "object"); // TODO: compatibility issue
        test(false, new BigDecimal("4.0"), "number", "object", "object");

        // This is the best. I want not to convert to number.
        // test(false, new BigInteger("30"), "object", "object", "object");
        // test(false, new BigDecimal("4.0"), "object", "object", "object");
    }

    /**
     * @param javaPrimitiveWrap
     * @param object
     * @param result typeof value
     * @param mapResult typeof map value
     * @param getResult typeof getter value
     */
    private void test(
            boolean javaPrimitiveWrap,
            Object object,
            String result,
            String mapResult,
            String getResult) {
        Context cx = Context.enter();
        try {
            cx.getWrapFactory().setJavaPrimitiveWrap(javaPrimitiveWrap);
            Scriptable scope = cx.initStandardObjects(new ImporterTopLevel(cx));

            // register object
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("test", object);
            ScriptableObject.putProperty(scope, "map", map);
            ScriptableObject.putProperty(scope, "object", Optional.of(object));
            ScriptableObject.putProperty(scope, "test", object);

            // execute script
            cx.evaluateString(scope, script, "", 1, null);

            // evaluate result
            assertEquals(result, ScriptableObject.getProperty(scope, "result"));
            assertEquals(mapResult, ScriptableObject.getProperty(scope, "mapResult"));
            assertEquals(getResult, ScriptableObject.getProperty(scope, "getResult"));
        } finally {
            Context.exit();
        }
    }
}
