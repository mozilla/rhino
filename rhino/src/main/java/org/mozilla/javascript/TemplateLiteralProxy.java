/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * Proxy to prevent Android JIT from inlining template literal logic. Keeps interpretLoop under
 * Android's 7392KB method size limit.
 */
public interface TemplateLiteralProxy {
    Scriptable getCallSite(Context cx, Scriptable scope, Object[] strings, int index);
}
