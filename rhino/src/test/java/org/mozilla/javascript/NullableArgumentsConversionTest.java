package org.mozilla.javascript;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mozilla.javascript.FunctionObject.*;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mozilla.javascript.testutils.Utils;

@RunWith(Parameterized.class)
public class NullableArgumentsConversionTest {

    @Parameterized.Parameters
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

    private final Object arg;
    private final int typeTag;
    private final boolean isNullable;
    private final Object expectedConvertedArg;

    public NullableArgumentsConversionTest(
            Object arg, int typeTag, boolean isNullable, Object expectedConvertedArg) {
        this.arg = arg;
        this.typeTag = typeTag;
        this.isNullable = isNullable;
        this.expectedConvertedArg = expectedConvertedArg;
    }

    @Test
    public void checkArgumentConversion() {
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
