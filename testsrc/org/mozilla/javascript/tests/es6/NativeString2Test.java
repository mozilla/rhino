package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/**
 * Test for handling const variables.
 */
public class NativeString2Test {

    private Context cx;
    private ScriptableObject scope;

    @Before
    public void setUp() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        scope = cx.initStandardObjects();

        cx.setOptimizationLevel(-1);
    }

    @After
    public void tearDown() {
        Context.exit();
    }

  @Test
  public void testNormalizeNoParam() {
      Object result = cx.evaluateString(
              scope, "'123'.normalize()",
              "test", 1, null
      );
      assertEquals("123", result);
  }

  @Test
  public void testNormalizeNoUndefined() {
      Object result = cx.evaluateString(
              scope, "'123'.normalize(undefined)",
              "test", 1, null
      );
      assertEquals("123", result);
  }

  @Test
  public void testNormalizeNoNull() {
      Object result = cx.evaluateString(
              scope, "try { "
                      + "  '123'.normalize(null);"
                      + "} catch (e) { e.message }",
              "test", 1, null
      );
      assertEquals("The normalization form should be one of 'NFC', 'NFD', 'NFKC', 'NFKD'.", result);
  }
}
