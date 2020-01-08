/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class exists to provide the prototype for instances of ES6 generator functions
 * defined with the "function *" syntax. This prototype is actually different
 * from the prototype used for garden-variety functions although the behavior
 * is slightly different. 
 */

public class ES6GeneratorFunction extends BaseFunction
{
    private static final long serialVersionUID = 6267977664920485832L;

    private static final String CLASS_NAME = "GeneratorFunction";

    static Object initConstructor(Scriptable scope, boolean sealed)
    {
        ES6GeneratorFunction obj = new ES6GeneratorFunction();
        baseInit(obj, scope, sealed);
        // The "GeneratorFunction" name actually never appears in the global scope.
        // To avoid DRY, just delete what just got set.
        Object cons = ScriptableObject.getProperty(scope, CLASS_NAME);
        ScriptableObject.deleteProperty(scope, CLASS_NAME);
        return cons;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    protected String getFunctionTag() {
        return "function * ";
    }
}
