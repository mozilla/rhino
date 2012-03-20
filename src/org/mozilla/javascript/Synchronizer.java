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
 * The Original Code is Delegator.java, released
 * Sep 27, 2000.
 *
 * The Initial Developer of the Original Code is
 * Matthias Radestock. <matthias@sorted.org>.
 * Portions created by the Initial Developer are Copyright (C) 2000
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
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

// API class

package org.mozilla.javascript;

/**
 * This class provides support for implementing Java-style synchronized
 * methods in Javascript.
 *
 * Synchronized functions are created from ordinary Javascript
 * functions by the <code>Synchronizer</code> constructor, e.g.
 * <code>new Packages.org.mozilla.javascript.Synchronizer(fun)</code>.
 * The resulting object is a function that establishes an exclusive
 * lock on the <code>this</code> object of its invocation.
 *
 * The Rhino shell provides a short-cut for the creation of
 * synchronized methods: <code>sync(fun)</code> has the same effect as
 * calling the above constructor.
 *
 * @see org.mozilla.javascript.Delegator
 * @author Matthias Radestock
 */

public class Synchronizer extends Delegator {

    private Object syncObject;

    /**
     * Create a new synchronized function from an existing one.
     *
     * @param obj the existing function
     */
    public Synchronizer(Scriptable obj) {
        super(obj);
    }

    /**
     * Create a new synchronized function from an existing one using
     * an explicit object as synchronization object.
     *
     * @param obj the existing function
     * @param syncObject the object to synchronized on
     */
    public Synchronizer(Scriptable obj, Object syncObject) {
        super(obj);
        this.syncObject = syncObject;
    }

    /**
     * @see org.mozilla.javascript.Function#call
     */
    @Override
    public Object call(Context cx, Scriptable scope, Object thisObj,
                       Object[] args)
    {
        Object sync = syncObject != null ? syncObject : thisObj;
        synchronized(sync instanceof Wrapper ? ((Wrapper)sync).unwrap() : sync) {
            return ((Function)obj).call(cx,scope,thisObj,args);
        }
    }
}
