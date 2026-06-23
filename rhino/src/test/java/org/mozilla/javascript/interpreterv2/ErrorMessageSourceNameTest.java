package org.mozilla.javascript.interpreterv2;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.SerializableCallable;
import org.mozilla.javascript.testutils.Utils;

/**
 * Regression test for V2 frame clearing bug.
 *
 * <p>When an exception escapes all JS handlers and is caught by Java code, V2 should clear
 * cx.lastInterpreterFrame before rethrowing (like V1 does). Otherwise, subsequent calls to
 * Context.reportRuntimeError() incorrectly add source info.
 */
class ErrorMessageSourceNameTest {
    @Test
    void sourceInfoShouldNotBePresent() {
        Utils.runWithAllModes(
                cx -> {
                    String script = "throwErrorFromJava();";
                    String sourceName = "Process Automation.SCRIPT1";

                    var scope = cx.initStandardObjects();

                    // Function that throws from Java (like GlideRecord.setTableName does)
                    var throwFunc =
                            new LambdaFunction(
                                    scope,
                                    "throwErrorFromJava",
                                    0,
                                    (SerializableCallable)
                                            (cx1, scope1, thisObj, args) -> {
                                                throw Context.reportRuntimeError(
                                                        "GlideRecord.setTableName - empty table name");
                                            });
                    scope.put("throwErrorFromJava", scope, throwFunc);

                    var e =
                            assertThrows(
                                    RhinoException.class,
                                    () -> cx.evaluateString(scope, script, sourceName, 1, null));

                    // Simulate Java code extracting base message and recreating error
                    // (like flow engine does)
                    String baseMessage = e.details();

                    // Frame should be cleared at this point!
                    Assertions.assertNull(cx.lastInterpreterFrame);

                    // Java code creates new error with base message
                    var e2 =
                            assertThrows(
                                    Exception.class,
                                    () -> {
                                        throw Context.reportRuntimeError(baseMessage);
                                    });

                    // The recreated error should NOT have source info appended
                    // Interpreter V1 behavior: "GlideRecord.setTableName - empty table name"
                    // Interpreter V2 bug: "GlideRecord.setTableName - empty table name
                    // (Process Automation.SCRIPT1; line 1)"
                    Assertions.assertEquals(
                            "GlideRecord.setTableName - empty table name",
                            e2.getMessage(),
                            "Frame should be cleared before rethrowing, so recreated error has no source info");
                    return null;
                });
    }
}
