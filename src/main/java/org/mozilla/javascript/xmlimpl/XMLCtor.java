/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.xmlimpl;

import org.mozilla.javascript.*;

class XMLCtor extends IdFunctionObject
{
    static final long serialVersionUID = -8708195078359817341L;

    private static final Object XMLCTOR_TAG = "XMLCtor";

    private XmlProcessor options;
//    private XMLLibImpl lib;

    XMLCtor(XML xml, Object tag, int id, int arity)
    {
        super(xml, tag, id, arity);
//        this.lib = xml.lib;
        this.options = xml.getProcessor();
        activatePrototypeMap(MAX_FUNCTION_ID);
    }

    private void writeSetting(Scriptable target)
    {
        for (int i = 1; i <= MAX_INSTANCE_ID; ++i) {
            int id = super.getMaxInstanceId() + i;
            String name = getInstanceIdName(id);
            Object value = getInstanceIdValue(id);
            ScriptableObject.putProperty(target, name, value);
        }
    }

    private void readSettings(Scriptable source)
    {
        for (int i = 1; i <= MAX_INSTANCE_ID; ++i) {
            int id = super.getMaxInstanceId() + i;
            String name = getInstanceIdName(id);
            Object value = ScriptableObject.getProperty(source, name);
            if (value == Scriptable.NOT_FOUND) {
                continue;
            }
            switch (i) {
              case Id_ignoreComments:
              case Id_ignoreProcessingInstructions:
              case Id_ignoreWhitespace:
              case Id_prettyPrinting:
                if (!(value instanceof Boolean)) {
                    continue;
                }
                break;
              case Id_prettyIndent:
                if (!(value instanceof Number)) {
                    continue;
                }
                break;
              default:
                throw new IllegalStateException();
            }
            setInstanceIdValue(id, value);
        }
    }

// #string_id_map#

    private static final int
        Id_ignoreComments               = 1,
        Id_ignoreProcessingInstructions = 2,
        Id_ignoreWhitespace             = 3,
        Id_prettyIndent                 = 4,
        Id_prettyPrinting               = 5,

        MAX_INSTANCE_ID                 = 5;

    @Override
    protected int getMaxInstanceId()
    {
        return super.getMaxInstanceId() + MAX_INSTANCE_ID;
    }

    @Override
    protected int findInstanceIdInfo(String s) {
        int id;
// #generated# Last update: 2007-08-20 09:01:10 EDT
        L0: { id = 0; String X = null; int c;
            L: switch (s.length()) {
            case 12: X="prettyIndent";id=Id_prettyIndent; break L;
            case 14: c=s.charAt(0);
                if (c=='i') { X="ignoreComments";id=Id_ignoreComments; }
                else if (c=='p') { X="prettyPrinting";id=Id_prettyPrinting; }
                break L;
            case 16: X="ignoreWhitespace";id=Id_ignoreWhitespace; break L;
            case 28: X="ignoreProcessingInstructions";id=Id_ignoreProcessingInstructions; break L;
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#

        if (id == 0) return super.findInstanceIdInfo(s);

        int attr;
        switch (id) {
          case Id_ignoreComments:
          case Id_ignoreProcessingInstructions:
          case Id_ignoreWhitespace:
          case Id_prettyIndent:
          case Id_prettyPrinting:
            attr = PERMANENT | DONTENUM;
            break;
          default: throw new IllegalStateException();
        }
        return instanceIdInfo(attr, super.getMaxInstanceId() + id);
    }

// #/string_id_map#

    @Override
    protected String getInstanceIdName(int id)
    {
        switch (id - super.getMaxInstanceId()) {
          case Id_ignoreComments:               return "ignoreComments";
          case Id_ignoreProcessingInstructions: return "ignoreProcessingInstructions";
          case Id_ignoreWhitespace:             return "ignoreWhitespace";
          case Id_prettyIndent:                 return "prettyIndent";
          case Id_prettyPrinting:               return "prettyPrinting";
        }
        return super.getInstanceIdName(id);
    }

    @Override
    protected Object getInstanceIdValue(int id)
    {
        switch (id - super.getMaxInstanceId()) {
          case Id_ignoreComments:
            return ScriptRuntime.wrapBoolean(options.isIgnoreComments());
          case Id_ignoreProcessingInstructions:
            return ScriptRuntime.wrapBoolean(options.isIgnoreProcessingInstructions());
          case Id_ignoreWhitespace:
            return ScriptRuntime.wrapBoolean(options.isIgnoreWhitespace());
          case Id_prettyIndent:
            return ScriptRuntime.wrapInt(options.getPrettyIndent());
          case Id_prettyPrinting:
            return ScriptRuntime.wrapBoolean(options.isPrettyPrinting());
        }
        return super.getInstanceIdValue(id);
    }

    @Override
    protected void setInstanceIdValue(int id, Object value) {
        switch (id - super.getMaxInstanceId()) {
            case Id_ignoreComments:
                options.setIgnoreComments(ScriptRuntime.toBoolean(value));
                return;
            case Id_ignoreProcessingInstructions:
                options.setIgnoreProcessingInstructions(ScriptRuntime.toBoolean(value));
                return;
            case Id_ignoreWhitespace:
                options.setIgnoreWhitespace(ScriptRuntime.toBoolean(value));
                return;
            case Id_prettyIndent:
                options.setPrettyIndent(ScriptRuntime.toInt32(value));
                return;
            case Id_prettyPrinting:
                options.setPrettyPrinting(ScriptRuntime.toBoolean(value));
                return;
        }
        super.setInstanceIdValue(id, value);
    }

// #string_id_map#
    private static final int
        Id_defaultSettings              = 1,
        Id_settings                     = 2,
        Id_setSettings                  = 3,
        MAX_FUNCTION_ID                 = 3;

    @Override
    protected int findPrototypeId(String s)
    {
        int id;
// #generated# Last update: 2007-08-20 09:01:10 EDT
        L0: { id = 0; String X = null;
            int s_length = s.length();
            if (s_length==8) { X="settings";id=Id_settings; }
            else if (s_length==11) { X="setSettings";id=Id_setSettings; }
            else if (s_length==15) { X="defaultSettings";id=Id_defaultSettings; }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }
// #/string_id_map#

    @Override
    protected void initPrototypeId(int id)
    {
        String s;
        int arity;
        switch (id) {
          case Id_defaultSettings:  arity=0; s="defaultSettings";  break;
          case Id_settings:         arity=0; s="settings";         break;
          case Id_setSettings:      arity=1; s="setSettings";      break;
          default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(XMLCTOR_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
        if (!f.hasTag(XMLCTOR_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
          case Id_defaultSettings: {
            options.setDefault();
            Scriptable obj = cx.newObject(scope);
            writeSetting(obj);
            return obj;
          }
          case Id_settings: {
            Scriptable obj = cx.newObject(scope);
            writeSetting(obj);
            return obj;
          }
          case Id_setSettings: {
            if (args.length == 0
                || args[0] == null
                || args[0] == Undefined.instance)
            {
                options.setDefault();
            } else if (args[0] instanceof Scriptable) {
                readSettings((Scriptable)args[0]);
            }
            return Undefined.instance;
          }
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    /**
        hasInstance for XML objects works differently than other objects; see ECMA357 13.4.3.10.
     */
    @Override
    public boolean hasInstance(Scriptable instance) {
        return (instance instanceof XML || instance instanceof XMLList);
    }
}
