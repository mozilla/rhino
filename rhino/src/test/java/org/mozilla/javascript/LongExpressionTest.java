package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

class LongExpressionTest {
	private static final int ONE_MIB = 1 << 20;
	private static final int BIG_EXPRESSION_LENGTH = 10_000;

	@Test
	void canProcessVeryLongExpressionsWithoutStackOverflow() throws InterruptedException {
		// Run in a separate thread to have a consistent, small-ish stack size
		AtomicReference<Throwable> thrown = new AtomicReference<>(null);
		Thread t = new Thread(null, () -> {
			try {
				checkCanProcessLongAdditionChain();
			} catch (Throwable e) {
				//noinspection CallToPrintStackTrace
				e.printStackTrace();
				thrown.set(e);
			}
		}, "small-stack-thread", ONE_MIB);
		t.start();
		t.join();

		assertNull(thrown.get(), "should not have thrown an exception");
	}

	private void checkCanProcessLongAdditionChain() {
		String script = buildScript();
		Utils.runWithAllModes(cx -> {
			ScriptableObject scope = cx.initStandardObjects();
			Assertions.assertDoesNotThrow(() -> {
				cx.evaluateString(scope, script, "test", 1, null);
			});
			return null;
		});
	}

	// Build a script "v0 + v1 + v2 + ..."
	private static String buildScript() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < BIG_EXPRESSION_LENGTH; ++i) {
			sb.append("v").append(i).append(" = '").append(i).append("';\n");
		}
		sb.append("var res = \n");
		for (int i = 0; i < BIG_EXPRESSION_LENGTH; ++i) {
			sb.append("  v").append(i).append(" + \n");
		}
		sb.append("'';\n");
		return sb.toString();
	}
}
