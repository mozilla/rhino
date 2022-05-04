/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class CustomTestDBF extends DocumentBuilderFactory {
    public static final String INTENTIONAL_CONFIG_EXCEPTION = "Intentionally thrown";

    @Override
    public Object getAttribute(String arg0) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean getFeature(String name) throws ParserConfigurationException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        throw new ParserConfigurationException(INTENTIONAL_CONFIG_EXCEPTION);
    }

    @Override
    public void setAttribute(String name, Object value) throws IllegalArgumentException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setFeature(String name, boolean value) throws ParserConfigurationException {
        if ("http://apache.org/xml/features/disallow-doctype-decl".equals(name)) {
            org.mozilla.javascript.tests.XMLSecureParserTest.CALLED_BY_XML_PARSER = true;
        }
    }
}
