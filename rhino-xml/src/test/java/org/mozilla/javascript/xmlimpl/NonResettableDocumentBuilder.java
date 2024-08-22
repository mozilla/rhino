package org.mozilla.javascript.xmlimpl;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class NonResettableDocumentBuilder extends DocumentBuilder {
    private DocumentBuilder delegateBuilder;
    private final DocumentBuilder delegateBuilderAfterReset;

    public NonResettableDocumentBuilder(
            DocumentBuilder delegateBuilderBeforeReset, DocumentBuilder delegateBuilderAfterReset) {
        this.delegateBuilder = delegateBuilderBeforeReset;
        this.delegateBuilderAfterReset = delegateBuilderAfterReset;
    }

    @Override
    public Document parse(InputSource is) throws SAXException, IOException {
        return delegateBuilder.parse(is);
    }

    @Override
    public boolean isNamespaceAware() {
        return delegateBuilder.isNamespaceAware();
    }

    @Override
    public boolean isValidating() {
        return delegateBuilder.isNamespaceAware();
    }

    @Override
    public void setEntityResolver(EntityResolver er) {
        delegateBuilder.setEntityResolver(er);
    }

    @Override
    public void setErrorHandler(ErrorHandler eh) {
        delegateBuilder.setErrorHandler(eh);
    }

    @Override
    public Document newDocument() {
        return delegateBuilder.newDocument();
    }

    @Override
    public DOMImplementation getDOMImplementation() {
        return delegateBuilder.getDOMImplementation();
    }

    @Override
    public void reset() {
        delegateBuilder = delegateBuilderAfterReset;
    }
}
