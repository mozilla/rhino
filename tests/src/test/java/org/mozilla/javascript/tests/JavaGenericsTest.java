package org.mozilla.javascript.tests;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.nat.type.TypeInfo;
import org.mozilla.javascript.testutils.Utils;

/**
 * @author ZZZank
 */
public class JavaGenericsTest {

    public static class Exampl extends ArrayList<String> {
        public static final List<Integer> ints = new ArrayList<>();
        public static final Map<Long, String> long2str = new HashMap<>();
        public static final Function<Long, String> objectMapper = l -> l.getClass().getName();

        public static List<Integer> getInts() {
            return ints;
        }

        public static Map<Long, String> getLong2str() {
            return long2str;
        }

        public static Function<Long, String> getObjectMapper() {
            return objectMapper;
        }
    }

    /** small trick to avoid reading class name by human */
    public static final String CLASS_REF = Exampl.class.getName();

    /**
     * in this test, generic info is extracted from inheritance: {@code class Exampl extends
     * ArrayList<String>}
     *
     * @see org.mozilla.javascript.JavaParameters#JavaParameters(Method, Class)
     * @see Exampl
     */
    @Test
    public void instance() {
        Utils.assertWithAllModes(
                "-123",
                Utils.lines(
                        String.format("const exampl = new %s()", JavaGenericsTest.CLASS_REF),
                        "exampl.add(-123)",
                        "exampl[0] + ''")); // force Rhino to convert wrapped java String object to string
    }

    /**
     * in this test, generic info is extracted from method return type: {@code List<Integer>
     * getInts()}
     *
     * @see Exampl#getInts()
     * @see org.mozilla.javascript.NativeJavaList
     */
    @Test
    public void list() {
        Utils.assertWithAllModes(
                123,
                Utils.lines(
                        String.format("const exampl = %s.getInts()", JavaGenericsTest.CLASS_REF),
                        "exampl.add(123.1)",
                        "exampl[0] + 0")); // force Rhino to convert wrapped java object to number
    }

    /**
     * in this test, generic info is extracted from method return type
     *
     * @see Exampl#getLong2str()
     * @see org.mozilla.javascript.NativeJavaMap
     */
    @Test
    public void map$ensureKeyType() {
        Utils.assertWithAllModes(
                Long.class.getName(),
                Utils.lines(
                        String.format(
                                "const exampl = %s.getLong2str()", JavaGenericsTest.CLASS_REF),
                        "exampl.put(0, 123)",
                        "exampl.keySet().iterator().next().getClass().getName() + ''"));
    }

    /**
     * in this test, generic info is extracted from method return type
     *
     * @see Exampl#getLong2str()
     * @see org.mozilla.javascript.NativeJavaMap
     */
    @Test
    public void map$valueType() {
        Utils.runWithAllModes(
                cx -> {
                    var scope = cx.initStandardObjects();
                    var got =
                            cx.evaluateString(
                                    scope,
                                    Utils.lines(
                                            String.format(
                                                    "const exampl = %s.getLong2str()",
                                                    JavaGenericsTest.CLASS_REF),
                                            "exampl.put(0.1, -128.5)",
                                            "exampl"),
                                    "test",
                                    0,
                                    null);
                    var unwrapped = Context.jsToJava(got, TypeInfo.OBJECT);
                    Assert.assertEquals(Map.of(0L, "-128.5"), unwrapped);
                    return null;
                });
    }

    /**
     * in this test, generic info is extracted from method return type
     *
     * <p>Old behaviour: Rhino will pass a {@link Double} to {@link Function#apply(Object)}, causing
     * an {@link IllegalArgumentException}
     *
     * @see Exampl#getObjectMapper()
     * @see org.mozilla.javascript.NativeJavaObject
     */
    @Test
    public void object() {
        Utils.assertWithAllModes(
                Long.class.getName(),
                Utils.lines(
                        String.format(
                                "const exampl = %s.getObjectMapper()", JavaGenericsTest.CLASS_REF),
                        "exampl.apply(0) + ''"));
    }

    /**
     * in this test, generic info is entracted from field type: {@code List<Integer> ints}
     *
     * <p>not yet implemented
     *
     * @see Exampl#ints
     */
    //    @Test
    public void typeFromField() {
        Utils.assertWithAllModes(
                123,
                Utils.lines(
                        String.format("const exampl = %s.ints", JavaGenericsTest.CLASS_REF),
                        "exampl.add(123.1)",
                        "exampl[0] + 0"));
    }
}
