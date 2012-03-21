/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Igor Bukanov, igor@fastmail.fm
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript;

class SpecialRef extends Ref
{
    static final long serialVersionUID = -7521596632456797847L;

    /**
     * Helper class for the __proto__ special property
     */
    private static class ProtoSpecialRef extends SpecialRef
    {
        static final long serialVersionUID = -6410092416752016016L;

        private ProtoSpecialRef(Scriptable target, String name)
        {
            super(target, name);
        }

        @Override
        public Object get(Context cx)
        {
            if (!ScriptRuntime.hasObjectElem(target, name, cx)) {
                // 'null' prototype is reported as 'undefined'
                Object proto = target.getPrototype();
                return (proto != null ? proto : Undefined.instance);
            }
            return super.get(cx);
        }

        @Override
        public Object set(Context cx, Object value)
        {
            if (!ScriptRuntime.hasObjectElem(target, name, cx)) {
                // ignore unless value is 'null' or Scriptable
                if (!(value == null || value instanceof Scriptable)) {
                    return value;
                }
                Scriptable obj = (Scriptable) value;
                if (obj != null) {
                    // Check that obj does not contain on its prototype/scope
                    // chain to prevent cycles
                    Scriptable search = obj;
                    do {
                        if (search == target) {
                            throw Context.reportRuntimeError1(
                                "msg.cyclic.value", name);
                        }
                        search = search.getPrototype();
                    } while (search != null);
                }
                target.setPrototype(obj);
                return value;
            }
            return super.set(cx, value);
        }

        @Override
        public boolean has(Context cx)
        {
            // always return true to follow spidermonkey
            return true;
        }

        @Override
        public boolean delete(Context cx)
        {
            if (!ScriptRuntime.hasObjectElem(target, name, cx)) {
                // always return true to follow spidermonkey
                return true;
            }
            return super.delete(cx);
        }
    }

    protected final Scriptable target;
    protected final String name;

    private SpecialRef(Scriptable target, String name)
    {
        this.target = target;
        this.name = name;
    }

    static Ref createSpecial(Context cx, Scriptable scope, Object object,
                             String name)
    {
        Scriptable target = ScriptRuntime.toObjectOrNull(cx, object, scope);
        if (target == null) {
            throw ScriptRuntime.undefReadError(object, name);
        }

        if ("__proto__".equals(name)) {
            if (cx.hasFeature(Context.FEATURE_PARENT_PROTO_PROPERTIES)) {
                return new ProtoSpecialRef(target, name);
            }
            return new SpecialRef(target, name);
        }

        throw new IllegalArgumentException(name);
    }

    @Override
    public Object get(Context cx)
    {
        return ScriptRuntime.getObjectProp(target, name, cx);
    }

    @Override
    public Object set(Context cx, Object value)
    {
        // TODO: default for 'checked' set to 'false' for now
        return ScriptRuntime.setObjectProp(target, name, value, false, cx);
    }

    @Override
    public boolean has(Context cx)
    {
        return ScriptRuntime.hasObjectElem(target, name, cx);
    }

    @Override
    public boolean delete(Context cx)
    {
        // TODO: default for 'checked' set to 'false' for now
        return ScriptRuntime.deleteObjectElem(target, name, cx, false);
    }
}

