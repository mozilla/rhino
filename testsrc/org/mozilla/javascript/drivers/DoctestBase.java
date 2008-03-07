/**
 * 
 */
package org.mozilla.javascript.drivers;

import junit.framework.TestCase;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * A doctest is a test in the form of an interactive shell session; Rhino
 * collects and runs the inputs to the shell prompt and compares them to the
 * expected outputs.
 * 
 * @author Norris Boyd
 */
public class DoctestBase extends TestCase {
    private int optimizationLevel;
    
    public void setOptimizationLevel(int level) {
        this.optimizationLevel = level;
    }
  
    public void runDoctest(Context cx, Global global, String name,
                           String source)
    {
        // create a lightweight top-level scope
        Scriptable scope = cx.newObject(global);
        scope.setPrototype(global);
        // global.runDoctest throws an exception on any failure
        int testsPassed = global.runDoctest(cx, scope, source, name, 1);
        System.out.println(name + ": " + testsPassed + " passed.");
        assertTrue(testsPassed > 0);
    }
    
    public void runDoctests(File[] tests) throws IOException {
        ContextFactory factory = ContextFactory.getGlobal();
        Context cx = factory.enterContext();
        try {
            cx.setOptimizationLevel(this.optimizationLevel);
            Global global = new Global(cx);
            for (File f : tests) {
                int length = (int) f.length(); // don't worry about very long
                                               // files
                char[] buf = new char[length];
                new FileReader(f).read(buf, 0, length);
                runDoctest(cx, global, f.getName(), new String(buf));
            }
        } finally {
            Context.exit();
        }
    }  
}
