/**
 * 
 */
package org.mozilla.javascript.drivers;

import junit.framework.TestCase;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
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
  
    public void runDoctest(String name, String source) {
        ContextFactory factory = ContextFactory.getGlobal();
        Context cx = factory.enterContext();
        try {
            Global global = new Global(cx);
            // global.runDoctest throws an exception on any failure
            int testsPassed = global.runDoctest(cx, source);
            System.out.println(name + ": " + testsPassed + " passed.");
            assertTrue(testsPassed > 0);
        } finally {
            Context.exit();
        }
    }
    
    public void runDoctests(File[] tests) throws IOException {     
      for (File f: tests) {
          int length = (int)f.length(); // don't worry about very long files
          char[] buf = new char[length];
          new FileReader(f).read(buf, 0, length);
          String session = new String(buf);
          runDoctest(f.getName(), session);
      }
    }  
}
