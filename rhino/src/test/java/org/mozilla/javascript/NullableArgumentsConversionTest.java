package org.mozilla.javascript;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mozilla.javascript.FunctionObject.*;

import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.testutils.Utils;

public class NullableArgumentsConversionTest {

    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][] {
                    {"string", JAVA_STRING_TYPE, false, "string"},
                    {"string", JAVA_STRING_TYPE, true, "string"},
                    {null, JAVA_STRING_TYPE, false, "null"},
                    {null, JAVA_STRING_TYPE, true, null},
                    {2, JAVA_INT_TYPE, false, 2},
                    {2, JAVA_INT_TYPE, true, 2},
                    {null, JAVA_INT_TYPE, false, 0},
                    {null, JAVA_INT_TYPE, true, null},
                    {2.0, JAVA_DOUBLE_TYPE, false, 2.0},
                    {2.0, JAVA_DOUBLE_TYPE, true, 2.0},
                    {null, JAVA_DOUBLE_TYPE, false, 0.0},
                    {null, JAVA_DOUBLE_TYPE, true, null},
                    {true, JAVA_BOOLEAN_TYPE, false, true},
                    {true, JAVA_BOOLEAN_TYPE, true, true},
                    {null, JAVA_BOOLEAN_TYPE, false, false},
                    {null, JAVA_BOOLEAN_TYPE, true, null}
                });
    }

    private Object arg;
    private int typeTag;
    private boolean isNullable;
    private Object expectedConvertedArg;

    public void initNullableArgumentsConversionTest(
            Object arg, int typeTag, boolean isNullable, Object expectedConvertedArg) {
        this.arg = arg;
        this.typeTag = typeTag;
        this.isNullable = isNullable;
        this.expectedConvertedArg = expectedConvertedArg;
    }

    @MethodSource("data")
    @ParameterizedTest
    public void checkArgumentConversion(
            Object arg, int typeTag, boolean isNullable, Object expectedConvertedArg) {
        initNullableArgumentsConversionTest(arg, typeTag, isNullable, expectedConvertedArg);
        Utils.runWithAllModes(
                context -> {
                    Scriptable scriptable =
                            new ScriptableObject() {
                                @Override
                                public String getClassName() {
                                    return "";
                                }
                            };
                    Object convertedArg =
                            FunctionObject.convertArg(
                                    context, scriptable, arg, typeTag, isNullable);

                    assertThat(convertedArg, is(expectedConvertedArg));
                    return null;
                });
    }
}
