package org.mozilla.javascript.tests;

import junit.framework.TestCase;

public class StrictBindThisTest extends TestCase {
    public void testStrictBindThis() {
        Utils.runWithAllOptimizationLevels(cx -> {
            assertTrue((Boolean)cx.evaluateString(cx.initSafeStandardObjects(), 
                "((function() { 'use strict'; return this == null }).bind(null))()", 
                "testBindScope.js", 1, null));
            return null;
        });
    }
}
