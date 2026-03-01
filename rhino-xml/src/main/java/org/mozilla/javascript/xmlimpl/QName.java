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
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

/** Class QName */
final class QName extends ScriptableObject {
    static final long serialVersionUID = 416745167693026750L;

    private static final String QNAME_TAG = "QName";

    private static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(
                                QNAME_TAG, 0, QName::js_constructorCall, QName::js_constructor)
                        .withMethod(PROTO, "toString", 0, QName::js_toString)
                        .withMethod(PROTO, "toSource", 0, QName::js_toSource)
                        .build();
    }

    static void init(Context cx, Scriptable scope, ScriptableObject proto, boolean sealed) {
        DESCRIPTOR.buildConstructor(cx, scope, proto, sealed);
    }

    private static Object js_constructor(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        var realThis = realThis(f.getPrototypeProperty(), f);
        return realThis.jsConstructor(cx, true, args);
    }

    private static Object js_constructorCall(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        var realThis = realThis(f.getPrototypeProperty(), f);
        return realThis.jsConstructor(cx, false, args);
    }

    private static Object js_toString(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        return realThis(thisObj, f).toString();
    }

    private static Object js_toSource(
            Context cx, JSFunction f, Object nt, Scriptable s, Object thisObj, Object[] args) {
        return realThis(thisObj, f).js_toSource();
    }

    private XMLLibImpl lib;

    private QName prototype;

    private XmlNode.QName delegate;

    private QName() {}

    static QName create(XMLLibImpl lib, Scriptable scope, QName prototype, XmlNode.QName delegate) {
        QName rv = new QName();
        rv.lib = lib;
        rv.setParentScope(scope);
        rv.prototype = prototype;
        rv.setPrototype(prototype);
        rv.delegate = delegate;
        rv.createNSProps();
        return rv;
    }

    private void createNSProps() {
        ScriptableObject.defineBuiltInProperty(
                this, "localName", PERMANENT | READONLY, QName::getLocalName);
        ScriptableObject.defineBuiltInProperty(this, "uri", PERMANENT | READONLY, QName::getURI);
    }

    private static Object getLocalName(QName qn, Scriptable start) {
        return qn.localName();
    }

    private static String getURI(QName qn, Scriptable start) {
        return qn.uri();
    }

    @Override
    public String toString() {
        //    ECMA357 13.3.4.2
        if (delegate.getNamespace() == null) {
            return "*::" + localName();
        } else if (delegate.getNamespace().isGlobal()) {
            //    leave as empty
            return localName();
        } else {
            return uri() + "::" + localName();
        }
    }

    public String localName() {
        if (delegate.getLocalName() == null) return "*";
        return delegate.getLocalName();
    }

    /*
     * TODO This property is supposed to be invisible and I think we can
     *  make it private at some point, though Namespace might need it
     */
    String prefix() {
        if (delegate.getNamespace() == null) return null;
        return delegate.getNamespace().getPrefix();
    }

    String uri() {
        if (delegate.getNamespace() == null) return null;
        return delegate.getNamespace().getUri();
    }

    /**
     * @deprecated
     */
    @Deprecated
    final XmlNode.QName toNodeQname() {
        return delegate;
    }

    final XmlNode.QName getDelegate() {
        return delegate;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof QName)) return false;
        return equalsInternal((QName) obj);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    protected Object equivalentValues(Object value) {
        if (!(value instanceof QName)) return Scriptable.NOT_FOUND;
        boolean result = equals((QName) value);
        return result ? Boolean.TRUE : Boolean.FALSE;
    }

    private boolean equalsInternal(QName q) {
        return this.delegate.equals(q.delegate);
    }

    @Override
    public String getClassName() {
        return "QName";
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        return toString();
    }

    private static QName realThis(Object thisObj, JSFunction f) {
        return ensureType(thisObj, QName.class, f.getFunctionName());
    }

    QName newQName(XMLLibImpl lib, String q_uri, String q_localName, String q_prefix) {
        QName prototype = this.prototype;
        if (prototype == null) {
            prototype = this;
        }
        XmlNode.Namespace ns = null;
        if (q_prefix != null) {
            ns = XmlNode.Namespace.create(q_prefix, q_uri);
        } else if (q_uri != null) {
            ns = XmlNode.Namespace.create(q_uri);
        }
        if (q_localName != null && q_localName.equals("*")) q_localName = null;
        return create(lib, this.getParentScope(), prototype, XmlNode.QName.create(ns, q_localName));
    }

    //    See ECMA357 13.3.2
    QName constructQName(XMLLibImpl lib, Context cx, Object namespace, Object name) {
        String nameString = null;
        if (name instanceof QName) {
            if (namespace == Undefined.instance) {
                return (QName) name;
            } else {
                nameString = ((QName) name).localName();
            }
        }
        if (name == Undefined.instance) {
            nameString = "";
        } else {
            nameString = ScriptRuntime.toString(name);
        }

        if (namespace == Undefined.instance) {
            if ("*".equals(nameString)) {
                namespace = null;
            } else {
                namespace = lib.getDefaultNamespace(cx);
            }
        }
        Namespace namespaceNamespace = null;
        if (namespace == null) {
            //    leave as null
        } else if (namespace instanceof Namespace) {
            namespaceNamespace = (Namespace) namespace;
        } else {
            namespaceNamespace = lib.newNamespace(ScriptRuntime.toString(namespace));
        }
        String q_localName = nameString;
        String q_uri;
        String q_prefix;
        if (namespace == null) {
            q_uri = null;
            q_prefix = null; //    corresponds to undefined; see QName class
        } else {
            q_uri = namespaceNamespace.uri();
            q_prefix = namespaceNamespace.prefix();
        }
        return newQName(lib, q_uri, q_localName, q_prefix);
    }

    QName constructQName(XMLLibImpl lib, Context cx, Object nameValue) {
        return constructQName(lib, cx, Undefined.instance, nameValue);
    }

    QName castToQName(XMLLibImpl lib, Context cx, Object qnameValue) {
        if (qnameValue instanceof QName) {
            return (QName) qnameValue;
        }
        return constructQName(lib, cx, qnameValue);
    }

    private Object jsConstructor(Context cx, boolean inNewExpr, Object[] args) {
        //    See ECMA357 13.3.2
        if (!inNewExpr && args.length == 1) {
            return castToQName(lib, cx, args[0]);
        }
        if (args.length == 0) {
            return constructQName(lib, cx, Undefined.instance);
        } else if (args.length == 1) {
            return constructQName(lib, cx, args[0]);
        } else {
            return constructQName(lib, cx, args[0], args[1]);
        }
    }

    private String js_toSource() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        toSourceImpl(uri(), localName(), prefix(), sb);
        sb.append(')');
        return sb.toString();
    }

    private static void toSourceImpl(
            String uri, String localName, String prefix, StringBuilder sb) {
        sb.append("new QName(");
        if (uri == null && prefix == null) {
            if (!"*".equals(localName)) {
                sb.append("null, ");
            }
        } else if (uri != null) {
            Namespace.toSourceImpl(prefix, uri, sb);
            sb.append(", ");
        }
        sb.append('\'');
        sb.append(ScriptRuntime.escapeString(localName, '\''));
        sb.append("')");
    }
}
