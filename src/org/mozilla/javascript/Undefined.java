/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * This class implements the Undefined value in JavaScript.
 */
public class Undefined implements Serializable
{
    static final long serialVersionUID = 9195680630202616767L;

    public static final Object instance = new Undefined();

    private Undefined()
    {
    }

    public Object readResolve()
    {
        return instance;
    }

    @Override
    public boolean equals(Object obj) {
        return isUndefined(obj) || super.equals(obj);
    }

    @Override
    public int hashCode() {
        // All instances of Undefined are equivalent!
        return 0;
    }

    public static final Scriptable SCRIPTABLE_UNDEFINED;

    static {
        SCRIPTABLE_UNDEFINED = (Scriptable) Proxy.newProxyInstance(Undefined.class.getClassLoader(), new Class[]{Scriptable.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("toString")) return "undefined";
                if (method.getName().equals("equals")) {
                    return args.length > 0 && isUndefined(args[0]);
                }
                throw new UnsupportedOperationException("undefined doesn't support " + method.getName());
            }
        });
    }

    public static boolean isUndefined(Object obj)
    {
        return Undefined.instance == obj || Undefined.SCRIPTABLE_UNDEFINED == obj;
    }
}
