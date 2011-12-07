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
 * Portions created by the Initial Developer are Copyright (C) 1997-2000
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Igor Bukanov
 *   Ethan Hugg
 *   Terry Lucas
 *   Milen Nankov
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

package org.mozilla.javascript.xml;

import org.mozilla.javascript.*;

/**
 *  This Interface describes what all XML objects (XML, XMLList) should have in common.
 *
 */
public abstract class XMLObject extends IdScriptableObject
{
    
    static final long serialVersionUID = 8455156490438576500L;
    
    public XMLObject()
    {
    }

    public XMLObject(Scriptable scope, Scriptable prototype)
    {
        super(scope, prototype);
    }

    /**
     * Implementation of ECMAScript [[Has]].
     */
    public abstract boolean has(Context cx, Object id);

    /**
     * Implementation of ECMAScript [[Get]].
     */
    public abstract Object get(Context cx, Object id);

    /**
     * Implementation of ECMAScript [[Put]].
     */
    public abstract void put(Context cx, Object id, Object value);

    /**
     * Implementation of ECMAScript [[Delete]].
     */
    public abstract boolean delete(Context cx, Object id);


    public abstract Object getFunctionProperty(Context cx, String name);

    public abstract Object getFunctionProperty(Context cx, int id);

    /**
     * Return an additional object to look for methods that runtime should
     * consider during method search. Return null if no such object available.
     */
    public abstract Scriptable getExtraMethodSource(Context cx);

    /**
     * Generic reference to implement x.@y, x..y etc.
     */
    public abstract Ref memberRef(Context cx, Object elem,
                                  int memberTypeFlags);

    /**
     * Generic reference to implement x::ns, x.@ns::y, x..@ns::y etc.
     */
    public abstract Ref memberRef(Context cx, Object namespace, Object elem,
                                  int memberTypeFlags);

    /**
     * Wrap this object into NativeWith to implement the with statement.
     */
    public abstract NativeWith enterWith(Scriptable scope);

    /**
     * Wrap this object into NativeWith to implement the .() query.
     */
    public abstract NativeWith enterDotQuery(Scriptable scope);

    /**
     * Custom <tt>+</tt> operator.
     * Should return {@link Scriptable#NOT_FOUND} if this object does not have
     * custom addition operator for the given value,
     * or the result of the addition operation.
     * <p>
     * The default implementation returns {@link Scriptable#NOT_FOUND}
     * to indicate no custom addition operation.
     *
     * @param cx the Context object associated with the current thread.
     * @param thisIsLeft if true, the object should calculate this + value
     *                   if false, the object should calculate value + this.
     * @param value the second argument for addition operation.
     */
    public Object addValues(Context cx, boolean thisIsLeft, Object value)
    {
        return Scriptable.NOT_FOUND;
    }

    /**
     * Gets the value returned by calling the typeof operator on this object.
     * @see org.mozilla.javascript.ScriptableObject#getTypeOf()
     * @return "xml" or "undefined" if {@link #avoidObjectDetection()} returns <code>true</code>
     */
    @Override
    public String getTypeOf()
    {
    	return avoidObjectDetection() ? "undefined" : "xml";
    }
}
