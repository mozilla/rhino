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

/** Class Namespace */
class Namespace extends ScriptableObject {
    static final long serialVersionUID = -5765755238131301744L;

    private static final String NAMESPACE_TAG = "Namespace";

    private static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(
                                NAMESPACE_TAG,
                                2,
                                Namespace::js_constructorCall,
                                Namespace::js_constructor)
                        .withMethod(PROTO, "toString", 0, Namespace::js_toString)
                        .withMethod(PROTO, "toSource", 0, Namespace::js_toSource)
                        .build();
    }

    public static void init(Context cx, Scriptable scope, ScriptableObject proto, boolean sealed) {
        DESCRIPTOR.buildConstructor(cx, scope, proto, sealed);
    }

    private Namespace prototype;
    private XmlNode.Namespace ns;

    private Namespace() {}

    static Namespace create(Scriptable scope, Namespace prototype, XmlNode.Namespace namespace) {
        Namespace rv = new Namespace();
        rv.setParentScope(scope);
        rv.prototype = prototype;
        rv.setPrototype(prototype);
        rv.ns = namespace;
        rv.createNSProps();
        return rv;
    }

    private void createNSProps() {
        ScriptableObject.defineBuiltInProperty(
                this, "prefix", PERMANENT | READONLY, Namespace::getPrefix);
        ScriptableObject.defineBuiltInProperty(
                this, "uri", PERMANENT | READONLY, Namespace::getURI);
    }

    private static Object getPrefix(Namespace ns, Scriptable start) {
        var res = ns.prefix();
        return res == null ? Undefined.instance : res;
    }

    private static String getURI(Namespace ns, Scriptable start) {
        return ns.uri();
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

    final XmlNode.Namespace getDelegate() {
        return ns;
    }

    public String uri() {
        return ns.getUri();
    }

    public String prefix() {
        return ns.getPrefix();
    }

    @Override
    public String toString() {
        return uri();
    }

    public String toLocaleString() {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Namespace)) {
            return false;
        }
        Namespace n = (Namespace) o;
        return uri().equals(n.uri());
    }

    @Override
    public int hashCode() {
        return uri().hashCode();
    }

    @Override
    protected Object equivalentValues(Object value) {
        if (!(value instanceof Namespace)) return Scriptable.NOT_FOUND;
        boolean result = equals((Namespace) value);
        return result ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public String getClassName() {
        return "Namespace";
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        return uri();
    }

    private static Namespace realThis(Object thisObj, JSFunction f) {
        return ensureType(thisObj, Namespace.class, f.getFunctionName());
    }

    Namespace newNamespace(String uri) {
        Namespace prototype = (this.prototype == null) ? this : this.prototype;
        return create(this.getParentScope(), prototype, XmlNode.Namespace.create(uri));
    }

    Namespace newNamespace(String prefix, String uri) {
        if (prefix == null) return newNamespace(uri);
        Namespace prototype = (this.prototype == null) ? this : this.prototype;
        return create(this.getParentScope(), prototype, XmlNode.Namespace.create(prefix, uri));
    }

    Namespace constructNamespace(Object uriValue) {
        String prefix;
        String uri;

        if (uriValue instanceof Namespace) {
            Namespace ns = (Namespace) uriValue;
            prefix = ns.prefix();
            uri = ns.uri();
        } else if (uriValue instanceof QName) {
            QName qname = (QName) uriValue;
            uri = qname.uri();
            if (uri != null) {
                //    TODO    Is there a way to push this back into QName so that we can make
                // prefix() private?
                prefix = qname.prefix();
            } else {
                uri = qname.toString();
                prefix = null;
            }
        } else {
            uri = ScriptRuntime.toString(uriValue);
            prefix = (uri.length() == 0) ? "" : null;
        }

        return newNamespace(prefix, uri);
    }

    Namespace castToNamespace(Object namespaceObj) {
        if (namespaceObj instanceof Namespace) {
            return (Namespace) namespaceObj;
        }
        return constructNamespace(namespaceObj);
    }

    private Namespace constructNamespace(Object prefixValue, Object uriValue) {
        String prefix;
        String uri;

        if (uriValue instanceof QName) {
            QName qname = (QName) uriValue;
            uri = qname.uri();
            if (uri == null) {
                uri = qname.toString();
            }
        } else {
            uri = ScriptRuntime.toString(uriValue);
        }

        if (uri.length() == 0) {
            if (prefixValue == Undefined.instance) {
                prefix = "";
            } else {
                prefix = ScriptRuntime.toString(prefixValue);
                if (prefix.length() != 0) {
                    throw ScriptRuntime.typeError(
                            "Illegal prefix '" + prefix + "' for 'no namespace'.");
                }
            }
        } else if (prefixValue == Undefined.instance) {
            prefix = "";
        } else if (!XMLName.accept(prefixValue)) {
            prefix = "";
        } else {
            prefix = ScriptRuntime.toString(prefixValue);
        }

        return newNamespace(prefix, uri);
    }

    private Namespace constructNamespace() {
        return newNamespace("", "");
    }

    private Object jsConstructor(Context cx, boolean inNewExpr, Object[] args) {
        if (!inNewExpr && args.length == 1) {
            return castToNamespace(args[0]);
        }

        if (args.length == 0) {
            return constructNamespace();
        } else if (args.length == 1) {
            return constructNamespace(args[0]);
        } else {
            return constructNamespace(args[0], args[1]);
        }
    }

    private String js_toSource() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        toSourceImpl(ns.getPrefix(), ns.getUri(), sb);
        sb.append(')');
        return sb.toString();
    }

    static void toSourceImpl(String prefix, String uri, StringBuilder sb) {
        sb.append("new Namespace(");
        if (uri.length() == 0) {
            if (!"".equals(prefix)) throw new IllegalArgumentException(prefix);
        } else {
            sb.append('\'');
            if (prefix != null) {
                sb.append(ScriptRuntime.escapeString(prefix, '\''));
                sb.append("', '");
            }
            sb.append(ScriptRuntime.escapeString(uri, '\''));
            sb.append('\'');
        }
        sb.append(')');
    }
}
