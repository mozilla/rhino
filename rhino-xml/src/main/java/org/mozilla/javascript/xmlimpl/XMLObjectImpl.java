/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.xmlimpl;

import static org.mozilla.javascript.ClassDescriptor.Destination.PROTO;

import org.mozilla.javascript.ClassDescriptor;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeWith;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Ref;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.xml.XMLObject;

/**
 * This abstract class describes what all XML objects (XML, XMLList) should have in common.
 *
 * @see XML
 */
abstract class XMLObjectImpl extends XMLObject {
    private static final long serialVersionUID = -2553684605738101761L;
    private static final String XMLOBJECT_TAG = "XMLObject";
    private XMLLibImpl lib;
    private boolean prototypeFlag;

    private static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR = new ClassDescriptor.Builder(XMLOBJECT_TAG).build();
    }

    public static ClassDescriptor.Builder populatePrototypeDescriptor(
            ClassDescriptor.Builder builder) {
        return builder.withMethod(PROTO, "addNamespace", 1, XMLObjectImpl::js_addNamespace)
                .withMethod(PROTO, "appendChild", 1, XMLObjectImpl::js_appendChild)
                .withMethod(PROTO, "attribute", 1, XMLObjectImpl::js_attribute)
                .withMethod(PROTO, "attributes", 0, XMLObjectImpl::js_attributes)
                .withMethod(PROTO, "child", 1, XMLObjectImpl::js_child)
                .withMethod(PROTO, "childIndex", 0, XMLObjectImpl::js_childIndex)
                .withMethod(PROTO, "children", 0, XMLObjectImpl::js_children)
                .withMethod(PROTO, "comments", 0, XMLObjectImpl::js_comments)
                .withMethod(PROTO, "contains", 1, XMLObjectImpl::js_contains)
                .withMethod(PROTO, "copy", 0, XMLObjectImpl::js_copy)
                .withMethod(PROTO, "descendants", 1, XMLObjectImpl::js_descendants)
                .withMethod(PROTO, "elements", 1, XMLObjectImpl::js_elements)
                .withMethod(PROTO, "hasComplexContent", 0, XMLObjectImpl::js_hasComplexContent)
                .withMethod(PROTO, "hasOwnProperty", 1, XMLObjectImpl::js_hasOwnProperty)
                .withMethod(PROTO, "hasSimpleContent", 0, XMLObjectImpl::js_hasSimpleContent)
                .withMethod(PROTO, "inScopeNamespaces", 0, XMLObjectImpl::js_inScopeNamespaces)
                .withMethod(PROTO, "insertChildAfter", 2, XMLObjectImpl::js_insertChildAfter)
                .withMethod(PROTO, "insertChildBefore", 2, XMLObjectImpl::js_insertChildBefore)
                .withMethod(PROTO, "length", 0, XMLObjectImpl::js_length)
                .withMethod(PROTO, "localName", 0, XMLObjectImpl::js_localName)
                .withMethod(PROTO, "name", 0, XMLObjectImpl::js_name)
                .withMethod(PROTO, "namespace", 1, XMLObjectImpl::js_namespace)
                .withMethod(
                        PROTO, "namespaceDeclarations", 0, XMLObjectImpl::js_namespaceDeclarations)
                .withMethod(PROTO, "nodeKind", 0, XMLObjectImpl::js_nodeKind)
                .withMethod(PROTO, "normalize", 0, XMLObjectImpl::js_normalize)
                .withMethod(PROTO, "parent", 0, XMLObjectImpl::js_parent)
                .withMethod(PROTO, "prependChild", 1, XMLObjectImpl::js_prependChild)
                .withMethod(
                        PROTO,
                        "processingInstructions",
                        1,
                        XMLObjectImpl::js_processingInstructions)
                .withMethod(
                        PROTO, "propertyIsEnumerable", 1, XMLObjectImpl::js_propertyIsEnumerable)
                .withMethod(PROTO, "removeNamespace", 1, XMLObjectImpl::js_removeNamespace)
                .withMethod(PROTO, "replace", 2, XMLObjectImpl::js_replace)
                .withMethod(PROTO, "setChildren", 1, XMLObjectImpl::js_setChildren)
                .withMethod(PROTO, "setLocalName", 1, XMLObjectImpl::js_setLocalName)
                .withMethod(PROTO, "setName", 1, XMLObjectImpl::js_setName)
                .withMethod(PROTO, "setNamespace", 1, XMLObjectImpl::js_setNamespace)
                .withMethod(PROTO, "text", 0, XMLObjectImpl::js_text)
                .withMethod(PROTO, "toString", 0, XMLObjectImpl::js_toString)
                .withMethod(PROTO, "toSource", 1, XMLObjectImpl::js_toSource)
                .withMethod(PROTO, "toXMLString", 1, XMLObjectImpl::js_toXMLString)
                .withMethod(PROTO, "valueOf", 0, XMLObjectImpl::js_valueOf);
    }

    protected XMLObjectImpl(XMLLibImpl lib, Scriptable scope, XMLObject prototype) {
        initialize(lib, scope, prototype);
    }

    final void initialize(XMLLibImpl lib, Scriptable scope, XMLObject prototype) {
        setParentScope(scope);
        setPrototype(prototype);
        prototypeFlag = (prototype == null);
        this.lib = lib;
    }

    final boolean isPrototype() {
        return prototypeFlag;
    }

    XMLLibImpl getLib() {
        return lib;
    }

    final XML newXML(XmlNode node) {
        return lib.newXML(node);
    }

    XML xmlFromNode(XmlNode node) {
        if (node.getXml() == null) {
            node.setXml(newXML(node));
        }
        return node.getXml();
    }

    final XMLList newXMLList() {
        return lib.newXMLList();
    }

    final XMLList newXMLListFrom(Object o) {
        return lib.newXMLListFrom(o);
    }

    final XmlProcessor getProcessor() {
        return lib.getProcessor();
    }

    final QName newQName(String uri, String localName, String prefix) {
        return lib.newQName(uri, localName, prefix);
    }

    final QName newQName(XmlNode.QName name) {
        return lib.newQName(name);
    }

    final Namespace createNamespace(XmlNode.Namespace declaration) {
        if (declaration == null) return null;
        return lib.createNamespaces(new XmlNode.Namespace[] {declaration})[0];
    }

    final Namespace[] createNamespaces(XmlNode.Namespace[] declarations) {
        return lib.createNamespaces(declarations);
    }

    @Override
    public final Object getDefaultValue(Class<?> hint) {
        return this.toString();
    }

    /**
     * ecmaHas(cx, id) calls this after resolving when id to XMLName and checking it is not Uint32
     * index.
     */
    abstract boolean hasXMLProperty(XMLName name);

    /**
     * ecmaGet(cx, id) calls this after resolving when id to XMLName and checking it is not Uint32
     * index.
     */
    abstract Object getXMLProperty(XMLName name);

    /**
     * ecmaPut(cx, id, value) calls this after resolving when id to XMLName and checking it is not
     * Uint32 index.
     */
    abstract void putXMLProperty(XMLName name, Object value);

    /**
     * ecmaDelete(cx, id) calls this after resolving when id to XMLName and checking it is not
     * Uint32 index.
     */
    abstract void deleteXMLProperty(XMLName name);

    /** Test XML equality with target the target. */
    abstract boolean equivalentXml(Object target);

    abstract void addMatches(XMLList rv, XMLName name);

    private XMLList getMatches(XMLName name) {
        XMLList rv = newXMLList();
        addMatches(rv, name);
        return rv;
    }

    abstract XML getXML();

    // Methods from section 12.4.4 in the spec
    abstract XMLList child(int index);

    abstract XMLList child(XMLName xmlName);

    abstract XMLList children();

    abstract XMLList comments();

    abstract boolean contains(Object xml);

    abstract XMLObjectImpl copy();

    abstract XMLList elements(XMLName xmlName);

    abstract boolean hasOwnProperty(XMLName xmlName);

    abstract boolean hasComplexContent();

    abstract boolean hasSimpleContent();

    abstract int length();

    abstract void normalize();

    abstract Object parent();

    abstract XMLList processingInstructions(XMLName xmlName);

    abstract boolean propertyIsEnumerable(Object member);

    abstract XMLList text();

    @Override
    public abstract String toString();

    abstract String toSource(int indent);

    abstract String toXMLString();

    abstract Object valueOf();

    protected abstract Object jsConstructor(Context cx, boolean inNewExpr, Object[] args);

    //
    //
    // Methods overriding ScriptableObject
    //
    //

    /**
     * XMLObject always compare with any value and equivalentValues never returns {@link
     * Scriptable#NOT_FOUND} for them but rather calls equivalentXml(value) and wrap the result as
     * Boolean.
     */
    @Override
    protected final Object equivalentValues(Object value) {
        boolean result = equivalentXml(value);
        return result ? Boolean.TRUE : Boolean.FALSE;
    }

    //
    //
    // Methods overriding XMLObject
    //
    //

    /** Implementation of ECMAScript [[Has]] */
    @Override
    public final boolean has(Context cx, Object id) {
        if (cx == null) cx = Context.getCurrentContext();
        XMLName xmlName = lib.toXMLNameOrIndex(cx, id);
        if (xmlName == null) {
            long index = ScriptRuntime.lastUint32Result(cx);
            // XXX Fix this cast
            return has((int) index, this);
        }
        return hasXMLProperty(xmlName);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        if (prototypeFlag) {
            if (super.has(name, start)) {
                return true;
            }
        }
        Context cx = Context.getCurrentContext();
        return hasXMLProperty(lib.toXMLNameFromString(cx, name));
    }

    /** Implementation of ECMAScript [[Get]] */
    @Override
    public final Object get(Context cx, Object id) {
        if (cx == null) cx = Context.getCurrentContext();
        XMLName xmlName = lib.toXMLNameOrIndex(cx, id);
        if (xmlName == null) {
            long index = ScriptRuntime.lastUint32Result(cx);
            // XXX Fix this cast
            Object result = get((int) index, this);
            if (result == Scriptable.NOT_FOUND) {
                result = Undefined.instance;
            }
            return result;
        }
        return getXMLProperty(xmlName);
    }

    @Override
    public Object get(String name, Scriptable start) {
        if (prototypeFlag) {
            var res = super.get(name, start);
            if (res != NOT_FOUND) {
                return res;
            }
        }
        Context cx = Context.getCurrentContext();
        return getXMLProperty(lib.toXMLNameFromString(cx, name));
    }

    /** Implementation of ECMAScript [[Put]] */
    @Override
    public final void put(Context cx, Object id, Object value) {
        if (cx == null) cx = Context.getCurrentContext();
        XMLName xmlName = lib.toXMLNameOrIndex(cx, id);
        if (xmlName == null) {
            long index = ScriptRuntime.lastUint32Result(cx);
            // XXX Fix this cast
            put((int) index, this, value);
            return;
        }
        putXMLProperty(xmlName, value);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        if (prototypeFlag) {
            super.put(name, start, value);
        } else {
            Context cx = Context.getCurrentContext();
            putXMLProperty(lib.toXMLNameFromString(cx, name), value);
        }
    }

    /** Implementation of ECMAScript [[Delete]]. */
    @Override
    public final boolean delete(Context cx, Object id) {
        if (cx == null) cx = Context.getCurrentContext();
        XMLName xmlName = lib.toXMLNameOrIndex(cx, id);
        if (xmlName == null) {
            long index = ScriptRuntime.lastUint32Result(cx);
            // XXX Fix this
            delete((int) index);
            return true;
        }
        deleteXMLProperty(xmlName);
        return true;
    }

    @Override
    public void delete(String name) {
        Context cx = Context.getCurrentContext();
        deleteXMLProperty(lib.toXMLNameFromString(cx, name));
    }

    @Override
    public Object getFunctionProperty(Context cx, int id) {
        if (isPrototype()) {
            return super.get(id, this);
        } else {
            Scriptable proto = getPrototype();
            if (proto instanceof XMLObject) {
                return ((XMLObject) proto).getFunctionProperty(cx, id);
            }
        }
        return NOT_FOUND;
    }

    @Override
    public Object getFunctionProperty(Context cx, String name) {
        if (isPrototype()) {
            return super.get(name, this);
        } else {
            Scriptable proto = getPrototype();
            if (proto instanceof XMLObject) {
                return ((XMLObject) proto).getFunctionProperty(cx, name);
            }
        }
        return NOT_FOUND;
    }

    //    TODO    Can this be made more strongly typed?
    @Override
    public Ref memberRef(Context cx, Object elem, int memberTypeFlags) {
        boolean attribute = (memberTypeFlags & Node.ATTRIBUTE_FLAG) != 0;
        boolean descendants = (memberTypeFlags & Node.DESCENDANTS_FLAG) != 0;
        if (!attribute && !descendants) {
            // Code generation would use ecma(Get|Has|Delete|Set) for
            // normal name identifiers so one ATTRIBUTE_FLAG
            // or DESCENDANTS_FLAG has to be set
            throw Kit.codeBug();
        }
        XmlNode.QName qname = lib.toNodeQName(cx, elem, attribute);
        XMLName rv = XMLName.create(qname, attribute, descendants);
        rv.initXMLObject(this);
        return rv;
    }

    /** Generic reference to implement x::ns, x.@ns::y, x..@ns::y etc. */
    @Override
    public Ref memberRef(Context cx, Object namespace, Object elem, int memberTypeFlags) {
        boolean attribute = (memberTypeFlags & Node.ATTRIBUTE_FLAG) != 0;
        boolean descendants = (memberTypeFlags & Node.DESCENDANTS_FLAG) != 0;
        XMLName rv = XMLName.create(lib.toNodeQName(cx, namespace, elem), attribute, descendants);
        rv.initXMLObject(this);
        return rv;
    }

    @Override
    public NativeWith enterWith(Scriptable scope) {
        return new XMLWithScope(lib, scope, this);
    }

    @Override
    public NativeWith enterDotQuery(Scriptable scope) {
        XMLWithScope xws = new XMLWithScope(lib, scope, this);
        xws.initAsDotQuery();
        return xws;
    }

    @Override
    public final Object addValues(Context cx, boolean thisIsLeft, Object value) {
        if (value instanceof XMLObject) {
            XMLObject v1, v2;
            if (thisIsLeft) {
                v1 = this;
                v2 = (XMLObject) value;
            } else {
                v1 = (XMLObject) value;
                v2 = this;
            }
            return lib.addXMLObjects(cx, v1, v2);
        }
        if (value == Undefined.instance) {
            // both "xml + undefined" and "undefined + xml" gives String(xml)
            return ScriptRuntime.toString(this);
        }

        return super.addValues(cx, thisIsLeft, value);
    }

    private static Object[] toObjectArray(Object[] typed) {
        Object[] rv = new Object[typed.length];
        for (int i = 0; i < rv.length; i++) {
            rv[i] = typed[i];
        }
        return rv;
    }

    private static void xmlMethodNotFound(Object object, String name) {
        throw ScriptRuntime.notFunctionError(object, name);
    }

    private static Object js_appendChild(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "appendChild");
        return xml.appendChild(arg(args, 0));
    }

    private static Object js_addNamespace(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "addNamespace");
        Namespace ns = realThis.lib.castToNamespace(cx, arg(args, 0));
        return xml.addNamespace(ns);
    }

    private static Object js_childIndex(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "childIndex");
        return ScriptRuntime.wrapInt(xml.childIndex());
    }

    private static Object js_inScopeNamespaces(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "inScopeNamespaces");
        return cx.newArray(s, toObjectArray(xml.inScopeNamespaces()));
    }

    private static Object js_insertChildAfter(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "insertChildAfter");
        Object arg0 = arg(args, 0);
        if (arg0 == null || arg0 instanceof XML) {
            return xml.insertChildAfter((XML) arg0, arg(args, 1));
        }
        return Undefined.instance;
    }

    private static Object js_insertChildBefore(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "insertChildBefore");
        Object arg0 = arg(args, 0);
        if (arg0 == null || arg0 instanceof XML) {
            return xml.insertChildBefore((XML) arg0, arg(args, 1));
        }
        return Undefined.instance;
    }

    private static Object js_localName(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "localName");
        return xml.localName();
    }

    private static Object js_name(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "name");
        return xml.name();
    }

    private static Object js_namespace(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "namespace");
        String prefix = (args.length > 0) ? ScriptRuntime.toString(args[0]) : null;
        Namespace rv = xml.namespace(prefix);
        if (rv == null) {
            return Undefined.instance;
        } else {
            return rv;
        }
    }

    private static Object js_namespaceDeclarations(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "namespaceDeclarations");
        Namespace[] array = xml.namespaceDeclarations();
        return cx.newArray(s, realThis.toObjectArray(array));
    }

    private static Object js_nodeKind(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "nodeKind");
        return xml.nodeKind();
    }

    private static Object js_prependChild(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "prependChild");
        return xml.prependChild(arg(args, 0));
    }

    private static Object js_removeNamespace(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "removeNamespace");
        Namespace ns = realThis.lib.castToNamespace(cx, arg(args, 0));
        return xml.removeNamespace(ns);
    }

    private static Object js_replace(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "replace");
        XMLName xmlName = realThis.lib.toXMLNameOrIndex(cx, arg(args, 0));
        Object arg1 = arg(args, 1);
        if (xmlName == null) {
            //    I refuse to believe that this number will exceed 2^31
            int index = (int) ScriptRuntime.lastUint32Result(cx);
            return xml.replace(index, arg1);
        } else {
            return xml.replace(xmlName, arg1);
        }
    }

    private static Object js_setChildren(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "setChildren");
        return xml.setChildren(arg(args, 0));
    }

    private static Object js_setLocalName(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "setLocalName");
        String localName;
        Object arg = arg(args, 0);
        if (arg instanceof QName) {
            localName = ((QName) arg).localName();
        } else {
            localName = ScriptRuntime.toString(arg);
        }
        xml.setLocalName(localName);
        return Undefined.instance;
    }

    private static Object js_setName(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "setName");
        Object arg = (args.length != 0) ? args[0] : Undefined.instance;
        QName qname = realThis.lib.constructQName(cx, arg);
        xml.setName(qname);
        return Undefined.instance;
    }

    private static Object js_setNamespace(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        if (xml == null) xmlMethodNotFound(realThis, "setNamespace");
        Namespace ns = realThis.lib.castToNamespace(cx, arg(args, 0));
        xml.setNamespace(ns);
        return Undefined.instance;
    }

    private static Object js_attribute(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XML xml = realThis.getXML();
        XMLName xmlName =
                XMLName.create(realThis.lib.toNodeQName(cx, arg(args, 0), true), true, false);
        return realThis.getMatches(xmlName);
    }

    private static Object js_attributes(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        return realThis.getMatches(XMLName.create(XmlNode.QName.create(null, null), true, false));
    }

    private static Object js_child(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());

        XMLName xmlName = realThis.lib.toXMLNameOrIndex(cx, arg(args, 0));
        if (xmlName == null) {
            //    Two billion or so is a fine upper limit, so we cast to int
            int index = (int) ScriptRuntime.lastUint32Result(cx);
            return realThis.child(index);
        } else {
            return realThis.child(xmlName);
        }
    }

    private static Object js_children(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        return realThis.children();
    }

    private static Object js_comments(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        return realThis.comments();
    }

    private static Object js_contains(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        return ScriptRuntime.wrapBoolean(realThis.contains(arg(args, 0)));
    }

    private static Object js_copy(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        return realThis.copy();
    }

    private static Object js_descendants(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        XmlNode.QName qname =
                (args.length == 0)
                        ? XmlNode.QName.create(null, null)
                        : realThis.lib.toNodeQName(cx, args[0], false);
        return realThis.getMatches(XMLName.create(qname, false, true));
    }

    private static Object js_elements(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        XMLName xmlName =
                (args.length == 0) ? XMLName.formStar() : realThis.lib.toXMLName(cx, args[0]);
        return realThis.elements(xmlName);
    }

    private static Object js_hasOwnProperty(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        if (realThis.prototypeFlag) {
            return realThis.has((String) args[0], realThis);
        }
        XMLName xmlName = realThis.lib.toXMLName(cx, arg(args, 0));
        return ScriptRuntime.wrapBoolean(realThis.hasOwnProperty(xmlName));
    }

    private static Object js_hasComplexContent(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        return ScriptRuntime.wrapBoolean(realThis.hasComplexContent());
    }

    private static Object js_hasSimpleContent(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        return ScriptRuntime.wrapBoolean(realThis.hasSimpleContent());
    }

    private static Object js_length(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        return ScriptRuntime.wrapInt(realThis.length());
    }

    private static Object js_normalize(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        realThis.normalize();
        return Undefined.instance;
    }

    private static Object js_parent(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        return realThis.parent();
    }

    private static Object js_processingInstructions(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        XMLName xmlName =
                (args.length > 0) ? realThis.lib.toXMLName(cx, args[0]) : XMLName.formStar();
        return realThis.processingInstructions(xmlName);
    }

    private static Object js_propertyIsEnumerable(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        return ScriptRuntime.wrapBoolean(realThis.propertyIsEnumerable(arg(args, 0)));
    }

    private static Object js_text(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        return realThis.text();
    }

    private static Object js_toString(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        return realThis.toString();
    }

    private static Object js_toSource(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        int indent = ScriptRuntime.toInt32(args, 0);
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        return realThis.toSource(indent);
    }

    private static Object js_toXMLString(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        return realThis.toXMLString();
    }

    private static Object js_valueOf(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        XMLObjectImpl realThis = realThis(thisObj, f.getFunctionName());
        return realThis.valueOf();
    }

    private static XMLObjectImpl realThis(Object thisObj, String name) {
        return ensureType(thisObj, XMLObjectImpl.class, name);
    }

    private static Object arg(Object[] args, int i) {
        return (i < args.length) ? args[i] : Undefined.instance;
    }

    final XML newTextElementXML(XmlNode reference, XmlNode.QName qname, String value) {
        return lib.newTextElementXML(reference, qname, value);
    }

    /* TODO: Hopefully this can be replaced with ecmaToXml below. */
    final XML newXMLFromJs(Object inputObject) {
        return lib.newXMLFromJs(inputObject);
    }

    final XML ecmaToXml(Object object) {
        return lib.ecmaToXml(object);
    }

    final String ecmaEscapeAttributeValue(String s) {
        //    TODO    Check this
        String quoted = lib.escapeAttributeValue(s);
        return quoted.substring(1, quoted.length() - 1);
    }

    final XML createEmptyXML() {
        return newXML(XmlNode.createEmpty(getProcessor()));
    }
}
