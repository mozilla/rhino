package org.mozilla.javascript.xmlimpl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

public class XmlNonResettableDocumentBuilderTest {
    private static final String XML_PROPERTY = "javax.xml.parsers.DocumentBuilderFactory";
    private final String originalDocumentBuilderFactory = System.getProperty(XML_PROPERTY);

    @Before
    public void setUp() {
        System.setProperty(XML_PROPERTY, NonResettableDocumentBuilderFactory.class.getName());
    }

    @After
    public void tearDown() {
        if (originalDocumentBuilderFactory == null) {
            System.clearProperty(XML_PROPERTY);
        } else {
            System.setProperty(XML_PROPERTY, originalDocumentBuilderFactory);
        }
    }

    @Test
    public void nonResettableDocumentBuilder() {
        try (Context cx = new ContextFactory().enterContext()) {
            Scriptable scope = cx.initStandardObjects();
            Object result =
                    cx.evaluateString(
                            scope,
                            "var employees = new XML('<employees><employee><name>John</name></employee></employees>');"
                                    + "employees.employee.name;",
                            "source",
                            1,
                            null);
            Assert.assertEquals("John", String.valueOf(result));
        }
    }
}
