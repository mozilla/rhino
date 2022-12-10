/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.classfile;

final class ExceptionTableEntry {
    ExceptionTableEntry(int startLabel, int endLabel, int handlerLabel, short catchType) {
        itsStartLabel = startLabel;
        itsEndLabel = endLabel;
        itsHandlerLabel = handlerLabel;
        itsCatchType = catchType;
    }

    int itsStartLabel;
    int itsEndLabel;
    int itsHandlerLabel;
    short itsCatchType;
}
