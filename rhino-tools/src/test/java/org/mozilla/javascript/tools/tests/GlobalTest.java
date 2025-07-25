package org.mozilla.javascript.tools.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.testutils.Utils;
import org.mozilla.javascript.tools.shell.Global;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * @author Roland Praml, Foconis Analytics GmbH
 */
public class GlobalTest {
	@Test
	public void runCommand() {
		Utils.runWithAllModes(
				cx -> {
					cx.setLanguageVersion(Context.VERSION_ES6);
					var g = new Global(cx);
					var result =
							cx.evaluateString(
									g,
									"runCommand('/usr/bin/env', 'bash', '-c', 'echo Hello World')",
									"test.js",
									1,
									null);
					assertInstanceOf(Number.class, result);
					assertEquals(0, ((Number) result).intValue());
					return null;
				});
	}

	@Test
	public void runCommandWithOutputStream() {
		Utils.runWithAllModes(
				cx -> {
					cx.setLanguageVersion(Context.VERSION_ES6);
					var g = new Global(cx);
					ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
					g.put("stdOut", g, stdOut);
					var result =
							cx.evaluateString(
									g,
									"runCommand('/usr/bin/env', 'bash', '-c', 'echo Hello World', { output: stdOut})",
									"test.js",
									1,
									null);
					assertInstanceOf(Number.class, result);
					assertEquals(0, ((Number) result).intValue());
					try {
						stdOut.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					assertEquals("Hello World\n", stdOut.toString());
					return null;
				});
	}

	private static final String STDIN_TO_STDOUT =
			"runCommand('/usr/bin/env', 'bash', '-c', 'cat - ', { input: stdIn, output: stdOut, err: stdErr })";
	private static final String STDIN_TO_STDERR =
			"runCommand('/usr/bin/env', 'bash', '-c', 'cat - >&2', { input: stdIn, output: stdOut, err: stdErr })";

	@Test
	public void runCommandWithInAndOutputStream() {

		Utils.runWithAllModes(
				cx -> {
					cx.setLanguageVersion(Context.VERSION_ES6);
					var g = new Global(cx);
					ByteArrayInputStream stdIn = new ByteArrayInputStream("Hello World".getBytes());
					ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
					ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
					g.put("stdIn", g, stdIn);
					g.put("stdOut", g, stdOut);
					g.put("stdErr", g, stdErr);
					cx.evaluateString(g, STDIN_TO_STDOUT, "test.js", 1, null);
					g.put("stdIn", g, "Some error");
					cx.evaluateString(g, STDIN_TO_STDERR, "test.js", 1, null);

					try {
						stdOut.close();
						stdErr.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					assertEquals("Hello World", stdOut.toString());
					assertEquals("Some error", stdErr.toString());
					return null;
				});
	}

	@Test
	public void testStreaming() {

		Utils.runWithAllModes(
				cx -> {
					cx.setLanguageVersion(Context.VERSION_ES6);
					var g = new Global(cx);
					InputStream stdIn = new InputStream() {
						long bytes = 10_000_000_000L;

						@Override
						public int read() throws IOException {
							if (bytes <= 0) {
								return -1;
							}
							bytes--;
							return 42;
						}

						@Override
						public int read(byte[] b, int off, int len) throws IOException {

							if (bytes >= len) {
								bytes -= len;
								return len;
							}
							if (bytes <= 0) {
								return -1;
							}
							return (int) bytes;
						}
					};
					ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
					ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
					g.put("stdIn", g, stdIn);
					g.put("stdOut", g, stdOut);
					g.put("stdErr", g, stdErr);
					cx.evaluateString(g, STDIN_TO_STDOUT, "test.js", 1, null);
					g.put("stdIn", g, "Some error");
					cx.evaluateString(g, STDIN_TO_STDERR, "test.js", 1, null);

					try {
						stdOut.close();
						stdErr.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					assertEquals("Hello World", stdOut.toString());
					assertEquals("Some error", stdErr.toString());
					return null;
				});
	}
}
