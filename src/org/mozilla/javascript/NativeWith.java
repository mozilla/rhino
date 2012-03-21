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
 *   Norris Boyd
 *   Igor Bukanov
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

import java.io.Serializable;

/**
 * This class implements the object lookup required for the
 * <code>with</code> statement.
 * It simply delegates every action to its prototype except
 * for operations on its parent.
 */
public class NativeWith implements Scriptable, IdFunctionCall, Serializable {

    private static final long serialVersionUID = 1L;

    static void init(Scriptable scope, boolean sealed)
    {
        NativeWith obj = new NativeWith();

        obj.setParentScope(scope);
        obj.setPrototype(ScriptableObject.getObjectPrototype(scope));

        IdFunctionObject ctor = new IdFunctionObject(obj, FTAG, Id_constructor,
                                         "With", 0, scope);
        ctor.markAsConstructor(obj);
        if (sealed) {
            ctor.sealObject();
        }
        ctor.exportAsScopeProperty();
    }

    private NativeWith() {
    }

    protected NativeWith(Scriptable parent, Scriptable prototype) {
        this.parent = parent;
        this.prototype = prototype;
    }

    public String getClassName() {
        return "With";
    }

    public boolean has(String id, Scriptable start)
    {
        return prototype.has(id, prototype);
    }

    public boolean has(int index, Scriptable start)
    {
        return prototype.has(index, prototype);
    }

    public Object get(String id, Scriptable start)
    {
        if (start == this)
            start = prototype;
        return prototype.get(id, start);
    }

    public Object get(int index, Scriptable start)
    {
        if (start == this)
            start = prototype;
        return prototype.get(index, start);
    }

    public void put(String id, Scriptable start, Object value, boolean checked)
    {
        if (start == this)
            start = prototype;
        prototype.put(id, start, value, checked);
    }

    public void put(int index, Scriptable start, Object value, boolean checked)
    {
        if (start == this)
            start = prototype;
        prototype.put(index, start, value, checked);
    }

    public void delete(String id, boolean checked)
    {
        prototype.delete(id, checked);
    }

    public void delete(int index, boolean checked)
    {
        prototype.delete(index, checked);
    }

    public Scriptable getPrototype() {
        return prototype;
    }

    public void setPrototype(Scriptable prototype) {
        this.prototype = prototype;
    }

    public Scriptable getParentScope() {
        return parent;
    }

    public void setParentScope(Scriptable parent) {
        this.parent = parent;
    }

    public Object[] getIds() {
        return prototype.getIds();
    }

    public Object getDefaultValue(Class<?> typeHint) {
        return prototype.getDefaultValue(typeHint);
    }

    public boolean hasInstance(Object value) {
        return prototype.hasInstance(value);
    }

    /**
     * Must return null to continue looping or the final collection result.
     */
    protected Object updateDotQuery(boolean value)
    {
        // NativeWith itself does not support it
        throw new IllegalStateException();
    }

    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Object thisObj, Object[] args)
    {
        if (f.hasTag(FTAG)) {
            if (f.methodId() == Id_constructor) {
                throw Context.reportRuntimeError1("msg.cant.call.indirect", "With");
            }
        }
        throw f.unknown();
    }

    static boolean isWithFunction(Object functionObj)
    {
        if (functionObj instanceof IdFunctionObject) {
            IdFunctionObject f = (IdFunctionObject)functionObj;
            return f.hasTag(FTAG) && f.methodId() == Id_constructor;
        }
        return false;
    }

    static Object newWithSpecial(Context cx, Scriptable scope, Object[] args)
    {
        ScriptRuntime.checkDeprecated(cx, "With");
        scope = ScriptableObject.getTopLevelScope(scope);
        NativeWith thisObj = new NativeWith();
        thisObj.setPrototype(args.length == 0
                             ? ScriptableObject.getObjectPrototype(scope)
                             : ScriptRuntime.toObject(cx, scope, args[0]));
        thisObj.setParentScope(scope);
        return thisObj;
    }

    private static final Object FTAG = "With";

    private static final int
        Id_constructor = 1;

    protected Scriptable prototype;
    protected Scriptable parent;
}
