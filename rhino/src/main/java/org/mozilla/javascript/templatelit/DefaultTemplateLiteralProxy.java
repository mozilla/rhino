/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.templatelit;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.TemplateLiteralProxy;

/**
 * Default implementation of template literal proxy. This implementation delegates to ScriptRuntime
 * to reduce the complexity of the interpreter loop and fix Android JIT compilation issues.
 *
 * @author Anivar Aravind
 */
public class DefaultTemplateLiteralProxy implements TemplateLiteralProxy {

    @Override
    public Scriptable getCallSite(Context cx, Scriptable scope, Object[] strings, int index) {
        // Delegate to the static implementation in ScriptRuntime
        return ScriptRuntime.getTemplateLiteralCallSiteImpl(cx, scope, strings, index);
    }
}
