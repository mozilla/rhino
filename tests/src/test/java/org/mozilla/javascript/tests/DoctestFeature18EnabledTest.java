package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.testutils.TestSource;
import org.mozilla.javascript.testutils.Utils;
import org.mozilla.javascript.tools.shell.Global;

public class DoctestFeature18EnabledTest extends DoctestsTest {
    public void initDoctestFeature18EnabledTest(
            String name, String source, boolean interpretedMode) {}

    public static Collection<Object[]> singleDoctest() throws IOException {
        List<Object[]> result = new ArrayList<Object[]>();
        File f = new File(TestSource.resolve("testsrc/doctests/feature18enabled.doctest"));
        String contents = DoctestsTest.loadFile(f);
        result.add(new Object[] {f.getName(), contents, false});
        return result;
    }

    @MethodSource("singleDoctest")
    @Override
    @ParameterizedTest(name = "{0}")
    public void runDoctest(String name, String source, boolean interpretedMode) {
        initDoctestFeature18EnabledTest(name, source, interpretedMode);
        ContextFactory contextFactory =
                Utils.contextFactoryWithFeatures(Context.FEATURE_INTEGER_WITHOUT_DECIMAL_PLACE);

        try (Context context = contextFactory.enterContext()) {
            context.setInterpretedMode(interpretedMode);
            Global global = new Global(context);
            int testsPassed = global.runDoctest(context, global, source, name, 1);
            assertTrue(testsPassed > 0);
        }
    }
}
