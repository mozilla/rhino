/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.classfile;

final class ClassFileField {
    ClassFileField(short nameIndex, short typeIndex, short flags) {
        itsNameIndex = nameIndex;
        itsTypeIndex = typeIndex;
        itsFlags = flags;
        itsHasAttributes = false;
    }

    void setAttributes(short attr1, short attr2, short attr3, int index) {
        itsHasAttributes = true;
        itsAttr1 = attr1;
        itsAttr2 = attr2;
        itsAttr3 = attr3;
        itsIndex = index;
    }

    int write(byte[] data, int offset) {
        offset = ClassFileWriter.putInt16(itsFlags, data, offset);
        offset = ClassFileWriter.putInt16(itsNameIndex, data, offset);
        offset = ClassFileWriter.putInt16(itsTypeIndex, data, offset);
        if (!itsHasAttributes) {
            // write 0 attributes
            offset = ClassFileWriter.putInt16(0, data, offset);
        } else {
            offset = ClassFileWriter.putInt16(1, data, offset);
            offset = ClassFileWriter.putInt16(itsAttr1, data, offset);
            offset = ClassFileWriter.putInt16(itsAttr2, data, offset);
            offset = ClassFileWriter.putInt16(itsAttr3, data, offset);
            offset = ClassFileWriter.putInt16(itsIndex, data, offset);
        }
        return offset;
    }

    int getWriteSize() {
        int size = 2 * 3;
        if (!itsHasAttributes) {
            size += 2;
        } else {
            size += 2 + 2 * 4;
        }
        return size;
    }

    private short itsNameIndex;
    private short itsTypeIndex;
    private short itsFlags;
    private boolean itsHasAttributes;
    private short itsAttr1, itsAttr2, itsAttr3;
    private int itsIndex;
}
