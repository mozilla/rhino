package org.mozilla.javascript.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.tools.shell.Global;

@RunWith(Parameterized.class)
public class DoctestFeature18EnabledTest extends DoctestsTest {
    public DoctestFeature18EnabledTest(String name, String source, int optimizationLevel) {
        super(name, source, optimizationLevel);
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> singleDoctest() throws IOException {
        List<Object[]> result = new ArrayList<Object[]>();
        File f = new File(DoctestsTest.baseDirectory, "feature18enabled.doctest");
        String contents = DoctestsTest.loadFile(f);
        result.add(new Object[] {f.getName(), contents, -1});
        return result;
    }

    @Override
    @Test
    public void runDoctest() {
        ContextFactory contextFactory =
                new ContextFactory() {
                    @Override
                    protected boolean hasFeature(Context cx, int featureIndex) {
                        if (featureIndex == Context.FEATURE_INTEGER_WITHOUT_DECIMAL_PLACE) {
                            return true;
                        }
                        return super.hasFeature(cx, featureIndex);
                    }
                };

        try (Context context = contextFactory.enterContext()) {
            context.setOptimizationLevel(optimizationLevel);
            Global global = new Global(context);
            int testsPassed = global.runDoctest(context, global, source, name, 1);
            assertTrue(testsPassed > 0);
        }
    }
}
