package org.mozilla.javascript.xmlimpl.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.TopLevel;

public class XmlNonResettableDocumentBuilderTest {
    private static final String XML_PROPERTY = "javax.xml.parsers.DocumentBuilderFactory";
    private final String originalDocumentBuilderFactory = System.getProperty(XML_PROPERTY);

    @BeforeEach
    public void setUp() {
        System.setProperty(XML_PROPERTY, NonResettableDocumentBuilderFactory.class.getName());
    }

    @AfterEach
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
            TopLevel scope = cx.initStandardObjects();
            Object result =
                    cx.evaluateString(
                            scope,
                            "var employees = new XML('<employees><employee><name>John</name></employee></employees>');"
                                    + "employees.employee.name;",
                            "source",
                            1,
                            null);
            Assertions.assertEquals("John", String.valueOf(result));
        }
    }
}
