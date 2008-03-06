package org.mozilla.javascript.tests;

import org.mozilla.javascript.drivers.DoctestBase;
import org.mozilla.javascript.drivers.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * Run doctests in folder testsrc/doctests.
 * 
 * @author Norris Boyd
 */
public class DoctestsTest extends DoctestBase {
    static final String baseDirectory = "testsrc" + File.separator + "doctests";

    static final String doctestsExtension = ".doctest";

    public void runDoctests() throws IOException {
        File[] doctests = FileUtils.recursiveListFiles(new File(baseDirectory),
                new FileFilter() {
                  public boolean accept(File f) {
                      return f.getName().endsWith(doctestsExtension);
                  }
                });
        runDoctests(doctests);
    }
    
    public void testDoctestsInterpreted() throws IOException {
        setOptimizationLevel(-1);
        runDoctests();
    }
    
    public void testDoctestsCompiled() throws IOException {
        setOptimizationLevel(0);
        runDoctests();
    }
    
    public void testDoctestsOptimized() throws IOException {
        setOptimizationLevel(9);
        runDoctests();
    }
}
