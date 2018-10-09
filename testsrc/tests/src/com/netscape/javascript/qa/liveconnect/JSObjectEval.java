/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.netscape.javascript.qa.liveconnect;

import netscape.javascript.JSObject;


/**
 *  Class with one static method that exercises JSObject.eval with
 *  any JSObject and any JS code.  Used by tests in
 *   ns/js/tests/lc3/exceptions
 *
 *
 */
public class JSObjectEval {
    /**
     *  Given a JSObject and some JavaScript code, have the object
     *  evaluate the JavaScript code.
     *
     */
    public static Object eval(JSObject obj, String code) {
        obj.eval(code);
        return null;
    }
}
