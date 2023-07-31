/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.xml.parsers.ParserConfigurationException;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

/**
 * Test for secure xml parsing
 *
 * @author Chris Smith
 */
public class XMLSecureParserTest {

    private static final String XML_PROPERTY = "javax.xml.parsers.DocumentBuilderFactory";
    private static final String DBF_CLASSNAME = "org.mozilla.javascript.tests.CustomTestDBF";

    public static boolean CALLED_BY_XML_PARSER = false;

    /**
     * Test first that XML can be run directly with the default XML parser for this JRE. Then inject
     * a custom parser to test that the security settings are being applied properly
     */
    @Test
    public void xmlSecureConfiguration() {
        CALLED_BY_XML_PARSER = false;

        // run with defaults for this JRE
        try (Context cx = ContextFactory.getGlobal().enterContext()) {
            executeXML(cx);
        }
        assertFalse(CALLED_BY_XML_PARSER);

        // store the original setting for xml, if any
        String original = System.getProperty(XML_PROPERTY);
        try {
            // inject our own xml parser
            System.setProperty(XML_PROPERTY, DBF_CLASSNAME);
            // run with our injected parser
            try (Context cx = ContextFactory.getGlobal().enterContext()) {
                executeXML(cx);
            }
        } catch (RuntimeException e) {
            // Our parser immediately throws a ParserConfigurationException on creating a
            // documentbuilder,
            // so catch it and make sure it is the correct type of exception with the correct
            // message
            // (in case another PCE is thrown for some reason)
            assertTrue(e.getCause() instanceof ParserConfigurationException);
            ParserConfigurationException pce = (ParserConfigurationException) e.getCause();
            assertEquals(
                    org.mozilla.javascript.tests.CustomTestDBF.INTENTIONAL_CONFIG_EXCEPTION,
                    pce.getMessage());
        } finally {
            // if we found an xml config in the system properties, replace it
            if (original != null) {
                System.setProperty(XML_PROPERTY, original);
            } else {
                System.clearProperty(XML_PROPERTY);
            }
        }
        // Our parser will set a flag on this class when configured properly, check that this
        // happened
        assertTrue(CALLED_BY_XML_PARSER);
    }

    /**
     * Test the same as above, but with the insecure configuration. This means neither the default
     * xml parser nor the custom xml parser should be configured with the secure features.
     */
    @Test
    public void xmlInsecureConfiguration() {
        CALLED_BY_XML_PARSER = false;

        // run with defaults for this JRE
        try (Context cx = new InsecureContextFactory().enterContext()) {
            executeXML(cx);
        }
        assertFalse(CALLED_BY_XML_PARSER);

        // store the original setting for xml, if any
        String original = System.getProperty(XML_PROPERTY);
        try {
            // inject our own xml parser
            System.setProperty(XML_PROPERTY, DBF_CLASSNAME);
            // run with our injected parser
            try (Context cx = new InsecureContextFactory().enterContext()) {
                executeXML(cx);
            }
        } catch (RuntimeException e) {
            // Our parser immediately throws a ParserConfigurationException on creating a
            // documentbuilder,
            // so catch it and make sure it is the correct type of exception with the correct
            // message
            // (in case another PCE is thrown for some reason)
            assertTrue(e.getCause() instanceof ParserConfigurationException);
            ParserConfigurationException pce = (ParserConfigurationException) e.getCause();
            assertEquals(
                    org.mozilla.javascript.tests.CustomTestDBF.INTENTIONAL_CONFIG_EXCEPTION,
                    pce.getMessage());
        } finally {
            // if we found an xml config in the system properties, replace it
            if (original != null) {
                System.setProperty(XML_PROPERTY, original);
            } else {
                System.clearProperty(XML_PROPERTY);
            }
            ContextFactory.initGlobal(new ContextFactory());
        }
        // Our parser will not set this flag as we skipped all security feature settings
        assertFalse(CALLED_BY_XML_PARSER);
    }

    private void executeXML(Context cx) {
        Scriptable scope = cx.initStandardObjects();
        cx.evaluateString(scope, "new XML('<a></a>').toXMLString();", "source", 1, null);
    }

    class InsecureContextFactory extends ContextFactory {

        @Override
        protected boolean hasFeature(Context cx, int featureIndex) {
            switch (featureIndex) {
                case Context.FEATURE_ENABLE_XML_SECURE_PARSING:
                    return false;
            }
            return super.hasFeature(cx, featureIndex);
        }
    }
}
