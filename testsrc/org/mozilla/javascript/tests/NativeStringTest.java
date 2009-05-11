/**
 * 
 */
package org.mozilla.javascript.tests;

import junit.framework.TestCase;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.Scriptable;

/**
 * @author Marc Guillemot
 */
public class NativeStringTest extends TestCase {
    
	/**
	 * Test for bug #492359
	 * https://bugzilla.mozilla.org/show_bug.cgi?id=492359
	 * Calling generic String or Array functions without arguments was causing ArrayIndexOutOfBoundsException
	 * in 1.7R2
	 */
    public void testtoLowerCaseApply() {
        final String source = "var x = String.toLowerCase; x.apply('HELLO')";
        final ContextAction action = new ContextAction() {
        	public Object run(Context cx) {
        		final Scriptable scope = cx.initStandardObjects();
       			final Object rep = cx.evaluateString(scope, source, "test.js", 0, null);
       			assertEquals("hello", rep);
       			return null;
        	}
        };
        Utils.runWithAllOptimizationLevels(action);
    }
 }
