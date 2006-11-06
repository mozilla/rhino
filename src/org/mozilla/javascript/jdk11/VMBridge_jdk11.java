/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
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
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript.jdk11;

import java.util.Hashtable;

import org.mozilla.javascript.*;

public class VMBridge_jdk11 extends VMBridge
{
    private Hashtable threadsWithContext = new Hashtable();

    protected Object getThreadContextHelper()
    {
        return Thread.currentThread();
    }

    protected Context getContext(Object contextHelper)
    {
        Thread t = (Thread)contextHelper;
        return (Context)threadsWithContext.get(t);
    }

    protected void setContext(Object contextHelper, Context cx)
    {
        Thread t = (Thread)contextHelper;
        if (cx == null) {
            // Allow to garbage collect thread reference
            threadsWithContext.remove(t);
        } else {
            threadsWithContext.put(t, cx);
        }
    }

    protected ClassLoader getCurrentThreadClassLoader()
    {
        return null;
    }

    protected boolean tryToMakeAccessible(Object accessibleObject)
    {
        return false;
    }
}
