/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * A proxy for template literal operations, allowing the template literal implementation to be
 * loaded optionally and reducing the complexity of the interpreter loop.
 *
 * @author Anivar Aravind (following the pattern established by Norris Boyd for RegExpProxy)
 */
public interface TemplateLiteralProxy {
    /**
     * Get or create a template literal call site object.
     *
     * @param cx the current context
     * @param scope the current scope
     * @param strings the template literal strings array
     * @param index the index of the template literal
     * @return the call site object
     */
    public Scriptable getCallSite(Context cx, Scriptable scope, Object[] strings, int index);
}
