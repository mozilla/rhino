/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.classfile;

final class ClassFileMethod {
    ClassFileMethod(String name, short nameIndex, String type, short typeIndex, short flags) {
        itsName = name;
        itsNameIndex = nameIndex;
        itsType = type;
        itsTypeIndex = typeIndex;
        itsFlags = flags;
    }

    void setCodeAttribute(byte[] codeAttribute) {
        itsCodeAttribute = codeAttribute;
    }

    int write(byte[] data, int offset) {
        offset = ClassFileWriter.putInt16(itsFlags, data, offset);
        offset = ClassFileWriter.putInt16(itsNameIndex, data, offset);
        offset = ClassFileWriter.putInt16(itsTypeIndex, data, offset);
        // Code attribute only
        offset = ClassFileWriter.putInt16(1, data, offset);
        System.arraycopy(itsCodeAttribute, 0, data, offset, itsCodeAttribute.length);
        offset += itsCodeAttribute.length;
        return offset;
    }

    int getWriteSize() {
        return 2 * 4 + itsCodeAttribute.length;
    }

    String getName() {
        return itsName;
    }

    String getType() {
        return itsType;
    }

    short getFlags() {
        return itsFlags;
    }

    private String itsName;
    private String itsType;
    private short itsNameIndex;
    private short itsTypeIndex;
    private short itsFlags;
    private byte[] itsCodeAttribute;
}
