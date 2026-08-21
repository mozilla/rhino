/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Test262UnexpectedlyPassingTest {

    @BeforeEach
    public void setUp() {
        Test262SuiteTest.RESULT_TRACKERS.clear();
    }

    @AfterEach
    public void tearDown() {
        Test262SuiteTest.RESULT_TRACKERS.clear();
    }

    private static Test262SuiteTest.Test262Case testCase(File file) {
        return new Test262SuiteTest.Test262Case(file, null, null, null, false, null, null);
    }

    @Test
    public void reportsOnlyExpectedFailuresThatNowPass() throws Exception {
        File nowPassing = Files.createTempFile("now-passing", ".js").toFile();
        File stillFailing = Files.createTempFile("still-failing", ".js").toFile();
        File missing = new File("/nonexistent/directory/missing.js");

        var nowPassingTracker = new Test262SuiteTest.TestResultTracker("comment");
        nowPassingTracker.setExpectations(
                Test262SuiteTest.TestMode.INTERPRETED, false, false, false, true);
        nowPassingTracker.setExpectations(
                Test262SuiteTest.TestMode.INTERPRETED, true, false, false, true);
        nowPassingTracker.passes(Test262SuiteTest.TestMode.INTERPRETED, false);
        nowPassingTracker.passes(Test262SuiteTest.TestMode.INTERPRETED, true);

        var stillFailingTracker = new Test262SuiteTest.TestResultTracker("comment");
        stillFailingTracker.setExpectations(
                Test262SuiteTest.TestMode.INTERPRETED, false, false, false, true);
        stillFailingTracker.setExpectations(
                Test262SuiteTest.TestMode.INTERPRETED, true, false, false, true);
        stillFailingTracker.passes(Test262SuiteTest.TestMode.INTERPRETED, false);

        var missingTracker = new Test262SuiteTest.TestResultTracker("comment");
        missingTracker.setExpectations(
                Test262SuiteTest.TestMode.INTERPRETED, false, false, false, true);
        missingTracker.passes(Test262SuiteTest.TestMode.INTERPRETED, false);

        Test262SuiteTest.RESULT_TRACKERS.put(testCase(nowPassing), nowPassingTracker);
        Test262SuiteTest.RESULT_TRACKERS.put(testCase(stillFailing), stillFailingTracker);
        Test262SuiteTest.RESULT_TRACKERS.put(testCase(missing), missingTracker);

        assertEquals(List.of(nowPassing.getPath()), Test262SuiteTest.getUnexpectedlyPassingTests());
    }

    @Test
    public void emptyWhenNothingIsTracked() {
        assertTrue(Test262SuiteTest.getUnexpectedlyPassingTests().isEmpty());
    }
}
