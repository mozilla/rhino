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

package org.mozilla.javascript.jdk15;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import org.mozilla.javascript.*;

public class VMBridge_jdk15 extends org.mozilla.javascript.jdk13.VMBridge_jdk13
{
    public VMBridge_jdk15() throws SecurityException, NoSuchMethodException {
        // Just try and see if we can access isVarArgs() on this constructor;
        // want to fail loading if the method doesn't exist so that we can
        // load a bridge to an older JDK
        VMBridge_jdk15.class.getConstructor(new Class[] {}).isVarArgs();
    }

    public boolean isVarArgs(Member member) {
        if (member instanceof Method)
            return ((Method) member).isVarArgs();
        else if (member instanceof Constructor)
            return ((Constructor) member).isVarArgs();
        else 
            return false;
    }

    public Method getIteratorMethod() {
        try {
            Class[] sig = { Context.class, Scriptable.class,
                            ScriptRuntime.emptyArgs.getClass(),
                            Function.class };
            return VMBridge_jdk15.class.getMethod("__iterator__", sig);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static Object __iterator__(Context cx, Scriptable thisObj,
                                      Object[] args, Function funObj)
    {
        if (thisObj instanceof Wrapper) {
            Object obj = ((Wrapper) thisObj).unwrap();
            if (obj instanceof Iterable) {
                Scriptable scope = ScriptableObject.getTopLevelScope(funObj);
                return cx.getWrapFactory().wrap(cx, scope,
                        new WrappedJavaIterator((Iterable) obj, scope),
                        WrappedJavaIterator.class);
            }
        }
        throw ScriptRuntime.typeError1("msg.incompat.call",
                                       NativeIterator.ITERATOR_PROPERTY_NAME);
    }

    static public class WrappedJavaIterator
    {
        WrappedJavaIterator(Iterable iterable, Scriptable scope) {
            this.iterator = iterable.iterator();
            this.scope = scope;
        }

        public Object next() {
            if (!iterator.hasNext()) {
                // Out of values. Throw StopIteration.
                Scriptable top = ScriptableObject.getTopLevelScope(scope);
                Object e = top.get(NativeIterator.STOP_ITERATION, top);
                throw new JavaScriptException(e, null, 0);
            }
            return iterator.next();
        }

        public Object __iterator__() {
            return this;
        }

        private Iterator iterator;
        private Scriptable scope;
    }

}
