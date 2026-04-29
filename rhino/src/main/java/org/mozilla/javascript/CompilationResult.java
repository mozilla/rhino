/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import org.mozilla.javascript.debug.DebuggableScript;

/**
 * Opaque result of compiling a {@link org.mozilla.javascript.ast.ScriptNode} via an {@link
 * Evaluator}. Each {@link Evaluator} implementation provides its own concrete subtype; callers must
 * pass the result back to the same evaluator that produced it. The type parameter ties the result
 * to either {@link JSScript} or {@link JSFunction}.
 */
public interface CompilationResult<T extends ScriptOrFn<T>> {
    DebuggableScript getDebuggableScript();
}
