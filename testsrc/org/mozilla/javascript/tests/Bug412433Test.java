/**
 * 
 */
package org.mozilla.javascript.tests;

import junit.framework.TestCase;

import org.mozilla.javascript.*;

/**
 * See https://bugzilla.mozilla.org/show_bug.cgi?id=412433
 * @author Norris Boyd
 */
public class Bug412433Test extends TestCase {
    public void testMaleformedJavascript2()
    {
        Context context = Context.enter();
        ScriptableObject scope = context.initStandardObjects();
        context.evaluateString(scope, "\"\".split(/[/?,/&]/)", "", 0, null);
        Context.exit();
    }
}
