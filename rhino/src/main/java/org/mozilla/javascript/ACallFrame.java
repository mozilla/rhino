/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import org.mozilla.javascript.debug.DebuggableScript;

/**
 * Interface representing a call frame in the interpreter. This abstraction allows both the original
 * interpreter's CallFrame and InterpreterV2's CallFrameV2 to be used interchangeably for stack
 * traces and debugging purposes.
 */
public abstract class ACallFrame {

    /** Returns the index of this frame in the call stack (0 for the top-level frame). */
    public abstract int getFrameIndex();

    /** Returns the parent frame, or null if this is the top-level frame. */
    public abstract ACallFrame getParentFrame();

    /**
     * Returns the program counter position for source line information. For the original
     * interpreter, this is the PC at the start of the current source line. For InterpreterV2, this
     * may be the current PC.
     */
    public abstract int getPcSourceLineStart();

    /** Returns the debuggable script data associated with this frame. */
    public abstract DebuggableScript getData();
}
