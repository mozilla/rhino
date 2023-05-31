/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

@RunWith(BlockJUnit4ClassRunner.class)
public class SealedSharedScopeTest {

    private Context ctx;
    private ImporterTopLevel sharedScope;
    private Scriptable scope1;
    private Scriptable scope2;

    @Before
    public void setUp() throws Exception {
        try (Context tmpCtx = Context.enter()) {
            sharedScope = new ImporterTopLevel(tmpCtx, true);
            sharedScope.sealObject();
        }

        ctx = Context.enter();
        scope1 = ctx.newObject(sharedScope);
        scope1.setPrototype(sharedScope);
        scope1.setParentScope(null);
        scope2 = ctx.newObject(sharedScope);
        scope2.setPrototype(sharedScope);
        scope2.setParentScope(null);
    }

    @After
    public void tearDown() throws Exception {
        Context.exit();
    }

    private Object evaluateString(Scriptable scope, String source) {
        Object o = ctx.evaluateString(scope, source, "test", 1, null);
        if (o instanceof Wrapper) {
            o = ((Wrapper) o).unwrap();
        }
        return o;
    }

    /**
     * Test should verify if JavaImporter can import java.util.Date/java.sql.Date without colliding
     * with internal Date function.
     */
    @Test
    public void importClassWithImporter() throws Exception {
        Object o;
        evaluateString(
                scope1, "var imp1 = new JavaImporter();\n" + "imp1.importClass(java.util.Date);");
        evaluateString(
                scope1, "var imp2 = new JavaImporter();\n" + "imp2.importClass(java.sql.Date);");
        o = evaluateString(scope1, "imp1.Date");
        assertEquals(java.util.Date.class, o);

        o = evaluateString(scope1, "imp2.Date");
        assertEquals(java.sql.Date.class, o);

        o = evaluateString(scope1, "Date"); // JavaScript "Statement" function
        assertEquals(IdFunctionObject.class, o.getClass());

        o = evaluateString(scope2, "typeof imp1"); // scope 2 has
        // no imp1
        assertEquals("undefined", o);
    }

    /**
     * Test should verify if JavaImporter can import java.util.Date/java.sql.Date without colliding
     * with internal Date function.
     */
    @Test
    public void importPackageWithImporter() throws Exception {
        Object o;
        evaluateString(
                scope1, "var imp1 = new JavaImporter();\n" + "imp1.importPackage(java.util);");
        evaluateString(
                scope1, "var imp2 = new JavaImporter();\n" + "imp2.importPackage(java.sql);");
        o = evaluateString(scope1, "imp1.Date");
        assertEquals(java.util.Date.class, o);

        o = evaluateString(scope1, "imp2.Date");
        assertEquals(java.sql.Date.class, o);

        o = evaluateString(scope1, "Date"); // JavaScript "Statement" function
        assertEquals(IdFunctionObject.class, o.getClass());

        o = evaluateString(scope2, "typeof imp1 == 'undefined'"); // scope 2 has
        // no imp1
        assertTrue((Boolean) o);
    }

    @Test
    public void globalScope() throws FileNotFoundException, IOException {
        evaluateString(scope1, "importPackage(Packages.java.io);");

        // Loading object via direct class type evaluate and then checking with typeof
        // works
        Object o = evaluateString(scope1, "File");
        assertEquals(java.io.File.class, o);
        o = evaluateString(scope1, "typeof File");
        assertEquals("function", o);

        // Direct checking with typeof fails
        evaluateString(scope2, "importPackage(Packages.java.io);");
        o = evaluateString(scope2, "typeof File");
        assertEquals("function", o);
    }

    @Test
    public void importClassWithScope() throws Exception {
        Object o;
        evaluateString(scope1, "importClass(javax.naming.Name);");
        evaluateString(scope2, "importClass(javax.xml.soap.Name);");
        o = evaluateString(scope1, "Name");
        assertEquals(javax.naming.Name.class, o);

        o = evaluateString(scope2, "Name");
        assertEquals(javax.xml.soap.Name.class, o);

        o = evaluateString(sharedScope, "typeof Name"); // JavaScript "Statement"
        // function
        assertEquals("undefined", o);
    }

    @Test
    public void importPackageWithScope() throws Exception {
        Object o;
        evaluateString(scope1, "importPackage(javax.naming);");
        evaluateString(scope2, "importPackage(javax.xml.soap);");
        o = evaluateString(scope1, "Name");
        assertEquals(javax.naming.Name.class, o);

        o = evaluateString(scope2, "Name");
        assertEquals(javax.xml.soap.Name.class, o);

        o = evaluateString(sharedScope, "typeof Name"); // JavaScript "Statement"
        // function
        assertEquals("undefined", o);
    }

    @Test(expected = EvaluatorException.class)
    public void importClassFailsOnSealedScope() throws Exception {
        evaluateString(sharedScope, "importClass(java.util.Locale);");
    }

    @Test
    public void importClassSucceedsOnScope() throws Exception {
        evaluateString(scope1, "importClass(java.util.Locale);");
        Object o = evaluateString(scope1, "Locale.getDefault()");
        assertEquals(Locale.getDefault(), o);
        try {
            evaluateString(scope2, "Locale.getDefault()");
            fail("EcmaError expected");
        } catch (EcmaError e) {
            assertEquals("ReferenceError: \"Locale\" is not defined. (test#1)", e.getMessage());
        }
    }
}
