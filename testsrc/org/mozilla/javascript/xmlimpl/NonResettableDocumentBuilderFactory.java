package org.mozilla.javascript.xmlimpl;

import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class NonResettableDocumentBuilderFactory extends DocumentBuilderFactory {
    private static final String XML_PROPERTY = "javax.xml.parsers.DocumentBuilderFactory";
    private final DocumentBuilderFactory delegateFactory;
    private final DocumentBuilder delegateBuilderAfterReset;

    public NonResettableDocumentBuilderFactory() throws ParserConfigurationException {
        Properties systemProperties = System.getProperties();
        String oldValue = systemProperties.getProperty(XML_PROPERTY);
        systemProperties.remove(XML_PROPERTY);
        System.setProperties(systemProperties);
        delegateFactory = DocumentBuilderFactory.newInstance();
        delegateBuilderAfterReset = delegateFactory.newDocumentBuilder();
        System.setProperty(XML_PROPERTY, oldValue);
    }

    @Override
    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        return new NonResettableDocumentBuilder(
                delegateFactory.newDocumentBuilder(), delegateBuilderAfterReset);
    }

    @Override
    public void setAttribute(String name, Object value) throws IllegalArgumentException {}

    @Override
    public Object getAttribute(String name) throws IllegalArgumentException {
        return null;
    }

    @Override
    public void setFeature(String name, boolean value) {}

    @Override
    public boolean getFeature(String name) {
        return false;
    }

    @Override
    public void setNamespaceAware(boolean awareness) {
        super.setNamespaceAware(awareness);
        delegateFactory.setNamespaceAware(awareness);
    }

    @Override
    public void setValidating(boolean validating) {
        super.setValidating(validating);
        delegateFactory.setValidating(validating);
    }

    @Override
    public void setIgnoringElementContentWhitespace(boolean whitespace) {
        super.setIgnoringElementContentWhitespace(whitespace);
        delegateFactory.setIgnoringElementContentWhitespace(whitespace);
    }

    @Override
    public void setExpandEntityReferences(boolean expandEntityRef) {
        super.setExpandEntityReferences(expandEntityRef);
        delegateFactory.setExpandEntityReferences(expandEntityRef);
    }

    @Override
    public void setIgnoringComments(boolean ignoreComments) {
        super.setIgnoringComments(ignoreComments);
        delegateFactory.setIgnoringComments(ignoreComments);
    }

    @Override
    public void setCoalescing(boolean coalescing) {
        super.setCoalescing(coalescing);
        delegateFactory.setCoalescing(coalescing);
    }
}
