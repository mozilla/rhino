/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.xml.impl.xmlbeans;

import org.mozilla.javascript.*;

/**
 * Class Namespace
 *
 */
class Namespace extends IdScriptableObject
{
    static final long serialVersionUID = -5765755238131301744L;

    private static final Object NAMESPACE_TAG = "Namespace";

    private XMLLibImpl lib;
    private String prefix;
    private String uri;

    public Namespace(XMLLibImpl lib, String uri)
    {
        super(lib.globalScope(), lib.namespacePrototype);

        if (uri == null)
            throw new IllegalArgumentException();

        this.lib = lib;
        this.prefix = (uri.length() == 0) ? "" : null;
        this.uri = uri;
    }


    public Namespace(XMLLibImpl lib, String prefix, String uri)
    {
        super(lib.globalScope(), lib.namespacePrototype);

        if (uri == null)
            throw new IllegalArgumentException();
        if (uri.length() == 0) {
            // prefix should be "" for empty uri
            if (prefix == null)
                throw new IllegalArgumentException();
            if (prefix.length() != 0)
                throw new IllegalArgumentException();
        }

        this.lib = lib;
        this.prefix = prefix;
        this.uri = uri;
    }

    public void exportAsJSClass(boolean sealed)
    {
        exportAsJSClass(MAX_PROTOTYPE_ID, lib.globalScope(), sealed);
    }

    /**
     *
     * @return
     */
    public String uri()
    {
        return uri;
    }

    /**
     *
     * @return
     */
    public String prefix()
    {
        return prefix;
    }

    /**
     *
     * @return
     */
    public String toString ()
    {
        return uri();
    }

    /**
     *
     * @return
     */
    public String toLocaleString ()
    {
        return toString();
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof Namespace)) return false;
        return equals((Namespace)obj);
    }

    public int hashCode()
    {
        return uri().hashCode();
    }

    protected Object equivalentValues(Object value)
    {
        if (!(value instanceof Namespace)) return Scriptable.NOT_FOUND;
        boolean result = equals((Namespace)value);
        return result ? Boolean.TRUE : Boolean.FALSE;
    }

    private boolean equals(Namespace n)
    {
        return uri().equals(n.uri());
    }

    /**
     *
     * @return
     */
    public String getClassName ()
    {
        return "Namespace";
    }

    /**
     *
     * @param hint
     * @return
     */
    public Object getDefaultValue (Class hint)
    {
        return uri();
    }

// #string_id_map#
    private static final int
        Id_prefix               = 1,
        Id_uri                  = 2,
        MAX_INSTANCE_ID         = 2;

    protected int getMaxInstanceId()
    {
        return super.getMaxInstanceId() + MAX_INSTANCE_ID;
    }

    protected int findInstanceIdInfo(String s)
    {
        int id;
// #generated# Last update: 2004-07-20 19:50:50 CEST
        L0: { id = 0; String X = null;
            int s_length = s.length();
            if (s_length==3) { X="uri";id=Id_uri; }
            else if (s_length==6) { X="prefix";id=Id_prefix; }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
        }
// #/generated#

        if (id == 0) return super.findInstanceIdInfo(s);

        int attr;
        switch (id) {
          case Id_prefix:
          case Id_uri:
            attr = PERMANENT | READONLY;
            break;
          default: throw new IllegalStateException();
        }
        return instanceIdInfo(attr, super.getMaxInstanceId() + id);
    }
// #/string_id_map#

    protected String getInstanceIdName(int id)
    {
        switch (id - super.getMaxInstanceId()) {
          case Id_prefix: return "prefix";
          case Id_uri: return "uri";
        }
        return super.getInstanceIdName(id);
    }

    protected Object getInstanceIdValue(int id)
    {
        switch (id - super.getMaxInstanceId()) {
          case Id_prefix:
            if (prefix == null) return Undefined.instance;
            return prefix;
          case Id_uri:
            return uri;
        }
        return super.getInstanceIdValue(id);
    }


// #string_id_map#
    private static final int
        Id_constructor          = 1,
        Id_toString             = 2,
        Id_toSource             = 3,
        MAX_PROTOTYPE_ID        = 3;

    protected int findPrototypeId(String s)
    {
        int id;
// #generated# Last update: 2004-08-21 12:07:01 CEST
        L0: { id = 0; String X = null; int c;
            int s_length = s.length();
            if (s_length==8) {
                c=s.charAt(3);
                if (c=='o') { X="toSource";id=Id_toSource; }
                else if (c=='t') { X="toString";id=Id_toString; }
            }
            else if (s_length==11) { X="constructor";id=Id_constructor; }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
        }
// #/generated#
        return id;
    }
// #/string_id_map#

    protected void initPrototypeId(int id)
    {
        String s;
        int arity;
        switch (id) {
          case Id_constructor: arity=2; s="constructor"; break;
          case Id_toString:    arity=0; s="toString";    break;
          case Id_toSource:    arity=0; s="toSource";    break;
          default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(NAMESPACE_TAG, id, s, arity);
    }

    public Object execIdCall(IdFunctionObject f,
                             Context cx,
                             Scriptable scope,
                             Scriptable thisObj,
                             Object[] args)
    {
        if (!f.hasTag(NAMESPACE_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
          case Id_constructor:
            return jsConstructor(cx, (thisObj == null), args);
          case Id_toString:
            return realThis(thisObj, f).toString();
          case Id_toSource:
            return realThis(thisObj, f).js_toSource();
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    private Namespace realThis(Scriptable thisObj, IdFunctionObject f)
    {
        if(!(thisObj instanceof Namespace))
            throw incompatibleCallError(f);
        return (Namespace)thisObj;
    }

    private Object jsConstructor(Context cx, boolean inNewExpr, Object[] args)
    {
        if (!inNewExpr && args.length == 1) {
            return lib.castToNamespace(cx, args[0]);
        }

        if (args.length == 0) {
            return lib.constructNamespace(cx);
        } else if (args.length == 1) {
            return lib.constructNamespace(cx, args[0]);
        } else {
            return lib.constructNamespace(cx, args[0], args[1]);
        }
    }

    private String js_toSource()
    {
        StringBuffer sb = new StringBuffer();
        sb.append('(');
        toSourceImpl(prefix, uri, sb);
        sb.append(')');
        return sb.toString();
    }

    static void toSourceImpl(String prefix, String uri, StringBuffer sb)
    {
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
