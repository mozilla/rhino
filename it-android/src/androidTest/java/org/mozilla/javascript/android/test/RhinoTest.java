package org.mozilla.javascript.android.test;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mozilla.javascript.android.TestCase;

@RunWith(Parameterized.class)
public class RhinoTest {

    @Parameterized.Parameter(value = 0)
    public TestCase testCase;

    @Test
    public void test() {
        String s = testCase.run();
        Log.i(testCase.toString(), s);
        assertTrue(s.contains("success"));
    }

    @Parameterized.Parameters(name = "{index}, js={0}")
    public static Collection<Object[]> suiteValues() throws IOException {
        List<Object[]> result = new ArrayList<Object[]>();
        Context androidContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        for (TestCase testCase : TestCase.getTestCases(androidContext)) {
            result.add(new Object[] {testCase});
        }
        return result;
    }
}
