/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.xml.impl.xmlbeans;

import org.mozilla.javascript.*;

/**
 * Class QName
 *
 */
final class QName extends IdScriptableObject
{
    static final long serialVersionUID = 416745167693026750L;

    private static final Object QNAME_TAG = "QName";

    XMLLibImpl lib;
    private String prefix;
    private String localName;
    private String uri;

    QName(XMLLibImpl lib, String uri, String localName, String prefix)
    {
        super(lib.globalScope(), lib.qnamePrototype);
        if (localName == null) throw new IllegalArgumentException();
        this.lib = lib;
        this.uri = uri;
        this.prefix = prefix;
        this.localName = localName;
    }

    void exportAsJSClass(boolean sealed)
    {
        exportAsJSClass(MAX_PROTOTYPE_ID, lib.globalScope(), sealed);
    }

    /**
     *
     * @return
     */
    public String toString()
    {
        String result;

        if (uri == null)
        {
            result = "*::".concat(localName);
        }
        else if(uri.length() == 0)
        {
            result = localName;
        }
        else
        {
            result = uri + "::" + localName;
        }

        return result;
    }

    public String localName()
    {
        return localName;
    }

    String prefix()
    {
        return (prefix == null) ? prefix : "";
    }

    String uri()
    {
        return uri;
    }

    public boolean equals(Object obj)
    {
        if(!(obj instanceof QName)) return false;
        return equals((QName)obj);
    }

    public int hashCode()
    {
        return localName.hashCode() ^ (uri == null ? 0 : uri.hashCode());
    }

    protected Object equivalentValues(Object value)
    {
        if(!(value instanceof QName)) return Scriptable.NOT_FOUND;
        boolean result = equals((QName)value);
        return result ? Boolean.TRUE : Boolean.FALSE;
    }

    private boolean equals(QName q)
    {
        boolean result;

        if (uri == null) {
            result = q.uri == null && localName.equals(q.localName);
        } else {
            result = uri.equals(q.uri) && localName.equals(q.localName);
        }

        return result;
    }

    /**
     *
     * @return
     */
    public String getClassName ()
    {
        return "QName";
    }

    /**
     *
     * @param hint
     * @return
     */
    public Object getDefaultValue (Class hint)
    {
        return toString();
    }

// #string_id_map#
    private static final int
        Id_localName            = 1,
        Id_uri                  = 2,
        MAX_INSTANCE_ID         = 2;

    protected int getMaxInstanceId()
    {
        return super.getMaxInstanceId() + MAX_INSTANCE_ID;
    }

    protected int findInstanceIdInfo(String s)
    {
        int id;
// #generated# Last update: 2004-07-18 12:32:51 CEST
        L0: { id = 0; String X = null;
            int s_length = s.length();
            if (s_length==3) { X="uri";id=Id_uri; }
            else if (s_length==9) { X="localName";id=Id_localName; }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
        }
// #/generated#

        if (id == 0) return super.findInstanceIdInfo(s);

        int attr;
        switch (id) {
          case Id_localName:
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
          case Id_localName: return "localName";
          case Id_uri: return "uri";
        }
        return super.getInstanceIdName(id);
    }

    protected Object getInstanceIdValue(int id)
    {
        switch (id - super.getMaxInstanceId()) {
          case Id_localName: return localName;
          case Id_uri: return uri;
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
// #generated# Last update: 2004-08-21 12:45:13 CEST
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
        initPrototypeMethod(QNAME_TAG, id, s, arity);
    }

    public Object execIdCall(IdFunctionObject f,
                             Context cx,
                             Scriptable scope,
                             Scriptable thisObj,
                             Object[] args)
    {
        if (!f.hasTag(QNAME_TAG)) {
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

    private QName realThis(Scriptable thisObj, IdFunctionObject f)
    {
        if(!(thisObj instanceof QName))
            throw incompatibleCallError(f);
        return (QName)thisObj;
    }

    private Object jsConstructor(Context cx, boolean inNewExpr, Object[] args)
    {
        if (!inNewExpr && args.length == 1) {
            return lib.castToQName(cx, args[0]);
        }
        if (args.length == 0) {
            return lib.constructQName(cx, Undefined.instance);
        } else if (args.length == 1) {
            return lib.constructQName(cx, args[0]);
        } else {
            return lib.constructQName(cx, args[0], args[1]);
        }
    }

    private String js_toSource()
    {
        StringBuffer sb = new StringBuffer();
        sb.append('(');
        toSourceImpl(uri, localName, prefix, sb);
        sb.append(')');
        return sb.toString();
    }

    private static void toSourceImpl(String uri, String localName,
                                     String prefix, StringBuffer sb)
    {
        sb.append("new QName(");
        if (uri == null && prefix == null) {
            if (!"*".equals(localName)) {
                sb.append("null, ");
            }
        } else {
            Namespace.toSourceImpl(prefix, uri, sb);
            sb.append(", ");
        }
        sb.append('\'');
        sb.append(ScriptRuntime.escapeString(localName, '\''));
        sb.append("')");
    }

}
