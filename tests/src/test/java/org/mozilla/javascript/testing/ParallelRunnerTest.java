package org.mozilla.javascript.testing;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

@RunWith(ParallelRunner.class)
public class ParallelRunnerTest {
    private static final AtomicBoolean initialized = new AtomicBoolean();

    private final String testName;

    public ParallelRunnerTest(String name) {
        this.testName = name;
    }

    @BeforeClass
    public static void init() {
        initialized.set(true);
    }

    @AfterClass
    public static void tearDown() {
        initialized.set(false);
    }

    @Test
    public void testTestOne() {
        assertTrue(initialized.get());
        assertNotEquals("shouldFail", testName);
    }

    @Test
    public void testTestTwo() {
        assertTrue(initialized.get());
        assertNotEquals("shouldFail", testName);
    }

    @Parameters
    public static Collection<Object[]> testParameters() {
        ArrayList<Object[]> tests = new ArrayList<>();
        tests.add(new Object[] {"one"});
        tests.add(new Object[] {"two"});
        tests.add(new Object[] {"three"});
        return tests;
    }
}
