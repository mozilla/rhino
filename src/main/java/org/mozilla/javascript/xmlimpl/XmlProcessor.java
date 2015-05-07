/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.xmlimpl;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingDeque;

import org.mozilla.javascript.*;

//    Disambiguate from org.mozilla.javascript.Node
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

class XmlProcessor implements Serializable {

    private static final long serialVersionUID = 6903514433204808713L;

    private boolean ignoreComments;
    private boolean ignoreProcessingInstructions;
    private boolean ignoreWhitespace;
    private boolean prettyPrint;
    private int prettyIndent;

    private transient javax.xml.parsers.DocumentBuilderFactory dom;
    private transient javax.xml.transform.TransformerFactory xform;
    private transient LinkedBlockingDeque<DocumentBuilder> documentBuilderPool;
    private RhinoSAXErrorHandler errorHandler = new RhinoSAXErrorHandler();

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.dom = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        this.dom.setNamespaceAware(true);
        this.dom.setIgnoringComments(false);
        this.xform = javax.xml.transform.TransformerFactory.newInstance();
        int poolSize = Runtime.getRuntime().availableProcessors() * 2;
        this.documentBuilderPool = new LinkedBlockingDeque<DocumentBuilder>(poolSize);
    }

    private static class RhinoSAXErrorHandler implements ErrorHandler, Serializable {

        private static final long serialVersionUID = 6918417235413084055L;

        private void throwError(SAXParseException e) {
            throw ScriptRuntime.constructError("TypeError", e.getMessage(),
                e.getLineNumber() - 1);
        }

        public void error(SAXParseException e) {
            throwError(e);
        }

        public void fatalError(SAXParseException e) {
            throwError(e);
        }

        public void warning(SAXParseException e) {
            Context.reportWarning(e.getMessage());
        }
    }

    XmlProcessor() {
        setDefault();
        this.dom = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        this.dom.setNamespaceAware(true);
        this.dom.setIgnoringComments(false);
        this.xform = javax.xml.transform.TransformerFactory.newInstance();
        int poolSize = Runtime.getRuntime().availableProcessors() * 2;
        this.documentBuilderPool = new LinkedBlockingDeque<DocumentBuilder>(poolSize);
    }

    final void setDefault() {
        this.setIgnoreComments(true);
        this.setIgnoreProcessingInstructions(true);
        this.setIgnoreWhitespace(true);
        this.setPrettyPrinting(true);
        this.setPrettyIndent(2);
    }

    final void setIgnoreComments(boolean b) {
        this.ignoreComments = b;
    }

    final void setIgnoreWhitespace(boolean b) {
        this.ignoreWhitespace = b;
    }

    final void setIgnoreProcessingInstructions(boolean b) {
        this.ignoreProcessingInstructions = b;
    }

    final void setPrettyPrinting(boolean b) {
        this.prettyPrint = b;
    }

    final void setPrettyIndent(int i) {
        this.prettyIndent = i;
    }

    final boolean isIgnoreComments() {
        return ignoreComments;
    }

    final boolean isIgnoreProcessingInstructions() {
        return ignoreProcessingInstructions;
    }

    final boolean isIgnoreWhitespace() {
        return ignoreWhitespace;
    }

    final boolean isPrettyPrinting() {
        return prettyPrint;
    }

    final int getPrettyIndent() {
        return prettyIndent;
    }

    private String toXmlNewlines(String rv) {
        StringBuilder nl = new StringBuilder();
        for (int i=0; i<rv.length(); i++) {
            if (rv.charAt(i) == '\r') {
                if (rv.charAt(i+1) == '\n') {
                    //    DOS, do nothing and skip the \r
                } else {
                    //    Macintosh, substitute \n
                    nl.append('\n');
                }
            } else {
                nl.append(rv.charAt(i));
            }
        }
        return nl.toString();
    }

    private javax.xml.parsers.DocumentBuilderFactory getDomFactory() {
        return dom;
    }

    // Get from pool, or create one without locking, if needed.
    private DocumentBuilder getDocumentBuilderFromPool()
            throws ParserConfigurationException {
        DocumentBuilder builder = documentBuilderPool.pollFirst();
        if (builder == null){
            builder = getDomFactory().newDocumentBuilder();
        }
        builder.setErrorHandler(errorHandler);
        return builder;
    }

    // Insert into pool, if resettable. Pool capacity is limited to
    // number of processors * 2.
    private void returnDocumentBuilderToPool(DocumentBuilder db) {
        try {
            db.reset();
            documentBuilderPool.offerFirst(db);
        } catch (UnsupportedOperationException e) {
            // document builders that don't support reset() can't be pooled
        }
    }

    private void addProcessingInstructionsTo(List<Node> list, Node node) {
        if (node instanceof ProcessingInstruction) {
            list.add(node);
        }
        if (node.getChildNodes() != null) {
            for (int i=0; i<node.getChildNodes().getLength(); i++) {
                addProcessingInstructionsTo(list, node.getChildNodes().item(i));
            }
        }
    }

    private void addCommentsTo(List<Node> list, Node node) {
        if (node instanceof Comment) {
            list.add(node);
        }
        if (node.getChildNodes() != null) {
            for (int i=0; i<node.getChildNodes().getLength(); i++) {
                addProcessingInstructionsTo(list, node.getChildNodes().item(i));
            }
        }
    }

    private void addTextNodesToRemoveAndTrim(List<Node> toRemove, Node node) {
        if (node instanceof Text) {
            Text text = (Text)node;
            boolean BUG_369394_IS_VALID = false;
            if (!BUG_369394_IS_VALID) {
                text.setData(text.getData().trim());
            } else {
                if (text.getData().trim().length() == 0) {
                    text.setData("");
                }
            }
            if (text.getData().length() == 0) {
                toRemove.add(node);
            }
        }
        if (node.getChildNodes() != null) {
            for (int i=0; i<node.getChildNodes().getLength(); i++) {
                addTextNodesToRemoveAndTrim(toRemove, node.getChildNodes().item(i));
            }
        }
    }

    final Node toXml(String defaultNamespaceUri, String xml) throws org.xml.sax.SAXException {
        //    See ECMA357 10.3.1
        DocumentBuilder builder = null;
        try {
            String syntheticXml = "<parent xmlns=\"" + defaultNamespaceUri +
                "\">" + xml + "</parent>";
            builder = getDocumentBuilderFromPool();
            Document document = builder.parse( new org.xml.sax.InputSource(new java.io.StringReader(syntheticXml)) );
            if (ignoreProcessingInstructions) {
                List<Node> list = new java.util.ArrayList<Node>();
                addProcessingInstructionsTo(list, document);
                for (Node node: list) {
                    node.getParentNode().removeChild(node);
                }
            }
            if (ignoreComments) {
                List<Node> list = new java.util.ArrayList<Node>();
                addCommentsTo(list, document);
                for (Node node: list) {
                    node.getParentNode().removeChild(node);
                }
            }
            if (ignoreWhitespace) {
                //    Apparently JAXP setIgnoringElementContentWhitespace() has a different meaning, it appears from the Javadoc
                //    Refers to element-only content models, which means we would need to have a validating parser and DTD or schema
                //    so that it would know which whitespace to ignore.

                //    Instead we will try to delete it ourselves.
                List<Node> list = new java.util.ArrayList<Node>();
                addTextNodesToRemoveAndTrim(list, document);
                for (Node node: list) {
                    node.getParentNode().removeChild(node);
                }
            }
            NodeList rv = document.getDocumentElement().getChildNodes();
            if (rv.getLength() > 1) {
                throw ScriptRuntime.constructError("SyntaxError", "XML objects may contain at most one node.");
            } else if (rv.getLength() == 0) {
                Node node = document.createTextNode("");
                return node;
            } else {
                Node node = rv.item(0);
                document.getDocumentElement().removeChild(node);
                return node;
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Unreachable.");
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new RuntimeException(e);
        } finally {
            if (builder != null)
                returnDocumentBuilderToPool(builder);
        }
    }

    Document newDocument() {
        DocumentBuilder builder = null;
        try {
            //    TODO    Should this use XML settings?
            builder = getDocumentBuilderFromPool();
            return builder.newDocument();
        } catch (javax.xml.parsers.ParserConfigurationException ex) {
            //    TODO    How to handle these runtime errors?
            throw new RuntimeException(ex);
        } finally {
            if (builder != null)
                returnDocumentBuilderToPool(builder);
        }
    }

    //    TODO    Cannot remember what this is for, so whether it should use settings or not
    private String toString(Node node) {
        javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(node);
        java.io.StringWriter writer = new java.io.StringWriter();
        javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(writer);
        try {
            javax.xml.transform.Transformer transformer = xform.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "no");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.METHOD, "xml");
            transformer.transform(source, result);
        } catch (javax.xml.transform.TransformerConfigurationException ex) {
            //    TODO    How to handle these runtime errors?
            throw new RuntimeException(ex);
        } catch (javax.xml.transform.TransformerException ex) {
            //    TODO    How to handle these runtime errors?
            throw new RuntimeException(ex);
        }
        return toXmlNewlines(writer.toString());
    }

    String escapeAttributeValue(Object value) {
        String text = ScriptRuntime.toString(value);

        if (text.length() == 0) return "";

        Document dom = newDocument();
        Element e = dom.createElement("a");
        e.setAttribute("b", text);
        String elementText = toString(e);
        int begin = elementText.indexOf('"');
        int end = elementText.lastIndexOf('"');
        return elementText.substring(begin+1,end);
    }

    String escapeTextValue(Object value) {
        if (value instanceof XMLObjectImpl) {
            return ((XMLObjectImpl)value).toXMLString();
        }

        String text = ScriptRuntime.toString(value);

        if (text.length() == 0) return text;

        Document dom = newDocument();
        Element e = dom.createElement("a");
        e.setTextContent(text);
        String elementText = toString(e);

        int begin = elementText.indexOf('>') + 1;
        int end = elementText.lastIndexOf('<');
        return (begin < end) ? elementText.substring(begin, end) : "";
    }

    private String escapeElementValue(String s) {
        //    TODO    Check this
        return escapeTextValue(s);
    }

    private String elementToXmlString(Element element) {
        //    TODO    My goodness ECMA is complicated (see 10.2.1).  We'll try this first.
        Element copy = (Element)element.cloneNode(true);
        if (prettyPrint) {
            beautifyElement(copy, 0);
        }
        return toString(copy);
    }

    final String ecmaToXmlString(Node node) {
        //    See ECMA 357 Section 10.2.1
        StringBuilder s = new StringBuilder();
        int indentLevel = 0;
        if (prettyPrint) {
            for (int i=0; i<indentLevel; i++) {
                s.append(' ');
            }
        }
        if (node instanceof Text) {
            String data = ((Text)node).getData();
            //    TODO Does Java trim() work same as XMLWhitespace?
            String v = (prettyPrint) ? data.trim() : data;
            s.append(escapeElementValue(v));
            return s.toString();
        }
        if (node instanceof Attr) {
            String value = ((Attr)node).getValue();
            s.append(escapeAttributeValue(value));
            return s.toString();
        }
        if (node instanceof Comment) {
            s.append("<!--" + ((Comment)node).getNodeValue() + "-->");
            return s.toString();
        }
        if (node instanceof ProcessingInstruction) {
            ProcessingInstruction pi = (ProcessingInstruction)node;
            s.append("<?" + pi.getTarget() + " " + pi.getData() + "?>");
            return s.toString();
        }
        s.append(elementToXmlString((Element)node));
        return s.toString();
    }

    private void beautifyElement(Element e, int indent) {
        StringBuilder s = new StringBuilder();
        s.append('\n');
        for (int i=0; i<indent; i++) {
            s.append(' ');
        }
        String afterContent = s.toString();
        for (int i=0; i<prettyIndent; i++) {
            s.append(' ');
        }
        String beforeContent = s.toString();

        //    We "mark" all the nodes first; if we tried to do this loop otherwise, it would behave unexpectedly (the inserted nodes
        //    would contribute to the length and it might never terminate).
        ArrayList<Node> toIndent = new ArrayList<Node>();
        boolean indentChildren = false;
        for (int i=0; i<e.getChildNodes().getLength(); i++) {
            if (i == 1) indentChildren = true;
            if (e.getChildNodes().item(i) instanceof Text) {
                toIndent.add(e.getChildNodes().item(i));
            } else {
                indentChildren = true;
                toIndent.add(e.getChildNodes().item(i));
            }
        }
        if (indentChildren) {
            for (int i=0; i<toIndent.size(); i++) {
                e.insertBefore(e.getOwnerDocument().createTextNode(beforeContent),
                        toIndent.get(i));
            }
        }
        NodeList nodes = e.getChildNodes();
        ArrayList<Element> list = new ArrayList<Element>();
        for (int i=0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element) {
                list.add((Element)nodes.item(i));
            }
        }
        for (Element elem: list) {
            beautifyElement(elem, indent + prettyIndent);
        }
        if (indentChildren) {
            e.appendChild(e.getOwnerDocument().createTextNode(afterContent));
        }
    }
}
