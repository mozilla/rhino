/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.classfile;

import java.util.HashMap;

final class ConstantPool {
    ConstantPool(ClassFileWriter cfw) {
        this.cfw = cfw;
        itsTopIndex = 1; // the zero'th entry is reserved
        itsPool = new byte[ConstantPoolSize];
        itsTop = 0;
    }

    private static final int ConstantPoolSize = 256;
    static final byte CONSTANT_Class = 7,
            CONSTANT_Fieldref = 9,
            CONSTANT_Methodref = 10,
            CONSTANT_InterfaceMethodref = 11,
            CONSTANT_String = 8,
            CONSTANT_Integer = 3,
            CONSTANT_Float = 4,
            CONSTANT_Long = 5,
            CONSTANT_Double = 6,
            CONSTANT_NameAndType = 12,
            CONSTANT_Utf8 = 1,
            CONSTANT_MethodType = 16,
            CONSTANT_MethodHandle = 15,
            CONSTANT_InvokeDynamic = 18;

    /**
     * Highest index a constant may occupy. {@code constant_pool_count} is an unsigned 16 bit
     * quantity holding one more than the highest index in use, which leaves 0xfffe as the last
     * usable index.
     */
    private static final int MAX_POOL_INDEX = 0xfffe;

    /**
     * Claim {@code slots} consecutive entries at the top of the pool and return the index of the
     * first of them. Long and double constants occupy two slots; everything else occupies one.
     *
     * @throws ClassFileWriter.ClassSizeException if the entries would not fit
     */
    private int reserveIndex(int slots) {
        int index = itsTopIndex;
        if (index + slots - 1 > MAX_POOL_INDEX) {
            throw new ClassFileWriter.ClassSizeException("Constant pool overflow");
        }
        itsTopIndex = index + slots;
        return index;
    }

    int write(byte[] data, int offset) {
        offset = ClassFileWriter.putInt16(itsTopIndex, data, offset);
        System.arraycopy(itsPool, 0, data, offset, itsTop);
        offset += itsTop;
        return offset;
    }

    int getWriteSize() {
        return 2 + itsTop;
    }

    int addConstant(int k) {
        int index = reserveIndex(1);
        ensure(5);
        itsPool[itsTop++] = CONSTANT_Integer;
        itsTop = ClassFileWriter.putInt32(k, itsPool, itsTop);
        itsPoolTypes.put(index, CONSTANT_Integer);
        return index;
    }

    int addConstant(long k) {
        int index = reserveIndex(2);
        ensure(9);
        itsPool[itsTop++] = CONSTANT_Long;
        itsTop = ClassFileWriter.putInt64(k, itsPool, itsTop);
        itsPoolTypes.put(index, CONSTANT_Long);
        return index;
    }

    int addConstant(float k) {
        int index = reserveIndex(1);
        ensure(5);
        itsPool[itsTop++] = CONSTANT_Float;
        int bits = Float.floatToIntBits(k);
        itsTop = ClassFileWriter.putInt32(bits, itsPool, itsTop);
        itsPoolTypes.put(index, CONSTANT_Float);
        return index;
    }

    int addConstant(double k) {
        int index = reserveIndex(2);
        ensure(9);
        itsPool[itsTop++] = CONSTANT_Double;
        long bits = Double.doubleToLongBits(k);
        itsTop = ClassFileWriter.putInt64(bits, itsPool, itsTop);
        itsPoolTypes.put(index, CONSTANT_Double);
        return index;
    }

    int addConstant(String k) {
        int utf8Index = addUtf8(k);
        int theIndex = itsStringConstHash.getOrDefault(utf8Index, -1);
        if (theIndex == -1) {
            theIndex = reserveIndex(1);
            ensure(3);
            itsPool[itsTop++] = CONSTANT_String;
            itsTop = ClassFileWriter.putInt16(utf8Index, itsPool, itsTop);
            itsStringConstHash.put(utf8Index, theIndex);
            itsPoolTypes.put(theIndex, CONSTANT_String);
        }
        return theIndex;
    }

    int addConstant(Object value) {
        if (value instanceof Integer || value instanceof Byte || value instanceof Short) {
            return addConstant(((Number) value).intValue());
        } else if (value instanceof Character) {
            return addConstant(((Character) value).charValue());
        } else if (value instanceof Boolean) {
            return addConstant(((Boolean) value).booleanValue() ? 1 : 0);
        } else if (value instanceof Float) {
            return addConstant(((Float) value).floatValue());
        } else if (value instanceof Long) {
            return addConstant(((Long) value).longValue());
        } else if (value instanceof Double) {
            return addConstant(((Double) value).doubleValue());
        } else if (value instanceof String) {
            return addConstant((String) value);
            // } else if (value instanceof ClassFileWriter.MethodType) {
            //    return addMethodType((ClassFileWriter.MethodType) value);
        } else if (value instanceof ClassFileWriter.MHandle) {
            return addMethodHandle((ClassFileWriter.MHandle) value);
        } else {
            throw new IllegalArgumentException("value " + value);
        }
    }

    boolean isUnderUtfEncodingLimit(String s) {
        int strLen = s.length();
        if (strLen * 3 <= MAX_UTF_ENCODING_SIZE) {
            return true;
        } else if (strLen > MAX_UTF_ENCODING_SIZE) {
            return false;
        }
        return strLen == getUtfEncodingLimit(s, 0, strLen);
    }

    /**
     * Get maximum i such that {@code start <= i <= end} and {@code s.substring(start, i)} fits JVM
     * UTF string encoding limit.
     */
    int getUtfEncodingLimit(String s, int start, int end) {
        if ((end - start) * 3 <= MAX_UTF_ENCODING_SIZE) {
            return end;
        }
        int limit = MAX_UTF_ENCODING_SIZE;
        for (int i = start; i != end; i++) {
            int c = s.charAt(i);
            if (0 != c && c <= 0x7F) {
                --limit;
            } else if (c < 0x7FF) {
                limit -= 2;
            } else {
                limit -= 3;
            }
            if (limit < 0) {
                return i;
            }
        }
        return end;
    }

    int addUtf8(String k) {
        int theIndex = itsUtf8Hash.getOrDefault(k, -1);
        if (theIndex == -1) {
            int strLen = k.length();
            boolean tooBigString;
            if (strLen > MAX_UTF_ENCODING_SIZE) {
                tooBigString = true;
            } else {
                tooBigString = false;
                // Ask for worst case scenario buffer when each char takes 3
                // bytes
                ensure(1 + 2 + strLen * 3);
                int top = itsTop;

                itsPool[top++] = CONSTANT_Utf8;
                top += 2; // skip length

                char[] chars = cfw.getCharBuffer(strLen);
                k.getChars(0, strLen, chars, 0);

                for (int i = 0; i != strLen; i++) {
                    int c = chars[i];
                    if (c != 0 && c <= 0x7F) {
                        itsPool[top++] = (byte) c;
                    } else if (c > 0x7FF) {
                        itsPool[top++] = (byte) (0xE0 | (c >> 12));
                        itsPool[top++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                        itsPool[top++] = (byte) (0x80 | (c & 0x3F));
                    } else {
                        itsPool[top++] = (byte) (0xC0 | (c >> 6));
                        itsPool[top++] = (byte) (0x80 | (c & 0x3F));
                    }
                }

                int utfLen = top - (itsTop + 1 + 2);
                if (utfLen > MAX_UTF_ENCODING_SIZE) {
                    tooBigString = true;
                } else {
                    // Claim the index before committing the bytes, so that an overflow leaves the
                    // pool untouched
                    theIndex = reserveIndex(1);

                    // Write back length
                    itsPool[itsTop + 1] = (byte) (utfLen >>> 8);
                    itsPool[itsTop + 2] = (byte) utfLen;

                    itsTop = top;
                    itsUtf8Hash.put(k, theIndex);
                }
            }
            if (tooBigString) {
                throw new IllegalArgumentException("Too big string");
            }
            setConstantData(theIndex, k);
            itsPoolTypes.put(theIndex, CONSTANT_Utf8);
        }
        return theIndex;
    }

    private int addNameAndType(String name, String type) {
        int nameIndex = addUtf8(name);
        int typeIndex = addUtf8(type);
        int index = reserveIndex(1);
        ensure(5);
        itsPool[itsTop++] = CONSTANT_NameAndType;
        itsTop = ClassFileWriter.putInt16(nameIndex, itsPool, itsTop);
        itsTop = ClassFileWriter.putInt16(typeIndex, itsPool, itsTop);
        itsPoolTypes.put(index, CONSTANT_NameAndType);
        return index;
    }

    int addClass(String className) {
        int theIndex = itsClassHash.getOrDefault(className, -1);
        if (theIndex == -1) {
            String slashed = className;
            if (className.indexOf('.') > 0) {
                slashed = ClassFileWriter.getSlashedForm(className);
                theIndex = itsClassHash.getOrDefault(slashed, -1);
                if (theIndex != -1) {
                    itsClassHash.put(className, theIndex);
                }
            }
            if (theIndex == -1) {
                int utf8Index = addUtf8(slashed);
                theIndex = reserveIndex(1);
                ensure(3);
                itsPool[itsTop++] = CONSTANT_Class;
                itsTop = ClassFileWriter.putInt16(utf8Index, itsPool, itsTop);
                itsClassHash.put(slashed, theIndex);
                if (!className.equals(slashed)) {
                    itsClassHash.put(className, theIndex);
                }
            }
            setConstantData(theIndex, className);
            itsPoolTypes.put(theIndex, CONSTANT_Class);
        }
        return theIndex;
    }

    int addFieldRef(String className, String fieldName, String fieldType) {
        FieldOrMethodRef ref = new FieldOrMethodRef(className, fieldName, fieldType);

        int theIndex = itsFieldRefHash.getOrDefault(ref, -1);
        if (theIndex == -1) {
            int ntIndex = addNameAndType(fieldName, fieldType);
            int classIndex = addClass(className);
            theIndex = reserveIndex(1);
            ensure(5);
            itsPool[itsTop++] = CONSTANT_Fieldref;
            itsTop = ClassFileWriter.putInt16(classIndex, itsPool, itsTop);
            itsTop = ClassFileWriter.putInt16(ntIndex, itsPool, itsTop);
            itsFieldRefHash.put(ref, theIndex);
            setConstantData(theIndex, ref);
            itsPoolTypes.put(theIndex, CONSTANT_Fieldref);
        }
        return theIndex;
    }

    int addMethodRef(String className, String methodName, String methodType) {
        FieldOrMethodRef ref = new FieldOrMethodRef(className, methodName, methodType);

        int theIndex = itsMethodRefHash.getOrDefault(ref, -1);
        if (theIndex == -1) {
            int ntIndex = addNameAndType(methodName, methodType);
            int classIndex = addClass(className);
            theIndex = reserveIndex(1);
            ensure(5);
            itsPool[itsTop++] = CONSTANT_Methodref;
            itsTop = ClassFileWriter.putInt16(classIndex, itsPool, itsTop);
            itsTop = ClassFileWriter.putInt16(ntIndex, itsPool, itsTop);
            itsMethodRefHash.put(ref, theIndex);
            setConstantData(theIndex, ref);
            itsPoolTypes.put(theIndex, CONSTANT_Methodref);
        }
        return theIndex;
    }

    int addInterfaceMethodRef(String className, String methodName, String methodType) {
        FieldOrMethodRef ref = new FieldOrMethodRef(className, methodName, methodType);

        int theIndex = itsInterfaceMethodRefHash.getOrDefault(ref, -1);
        if (theIndex == -1) {
            int ntIndex = addNameAndType(methodName, methodType);
            int classIndex = addClass(className);
            theIndex = reserveIndex(1);
            ensure(5);
            itsPool[itsTop++] = CONSTANT_InterfaceMethodref;
            itsTop = ClassFileWriter.putInt16(classIndex, itsPool, itsTop);
            itsTop = ClassFileWriter.putInt16(ntIndex, itsPool, itsTop);
            itsInterfaceMethodRefHash.put(ref, theIndex);
            setConstantData(theIndex, ref);
            itsPoolTypes.put(theIndex, CONSTANT_InterfaceMethodref);
        }
        return theIndex;
    }

    int addInvokeDynamic(String methodName, String methodType, int bootstrapIndex) {
        ConstantEntry entry =
                new ConstantEntry(CONSTANT_InvokeDynamic, bootstrapIndex, methodName, methodType);
        int theIndex = itsConstantHash.getOrDefault(entry, -1);

        if (theIndex == -1) {
            int nameTypeIndex = addNameAndType(methodName, methodType);
            theIndex = reserveIndex(1);
            ensure(5);
            itsPool[itsTop++] = CONSTANT_InvokeDynamic;
            itsTop = ClassFileWriter.putInt16(bootstrapIndex, itsPool, itsTop);
            itsTop = ClassFileWriter.putInt16(nameTypeIndex, itsPool, itsTop);
            itsConstantHash.put(entry, theIndex);
            setConstantData(theIndex, methodType);
            itsPoolTypes.put(theIndex, CONSTANT_InvokeDynamic);
        }
        return theIndex;
    }

    int addMethodHandle(ClassFileWriter.MHandle mh) {
        int theIndex = itsConstantHash.getOrDefault(mh, -1);

        if (theIndex == -1) {
            int ref;
            if (mh.tag <= ByteCode.MH_PUTSTATIC) {
                ref = addFieldRef(mh.owner, mh.name, mh.desc);
            } else if (mh.tag == ByteCode.MH_INVOKEINTERFACE) {
                ref = addInterfaceMethodRef(mh.owner, mh.name, mh.desc);
            } else {
                ref = addMethodRef(mh.owner, mh.name, mh.desc);
            }

            theIndex = reserveIndex(1);
            ensure(4);
            itsPool[itsTop++] = CONSTANT_MethodHandle;
            itsPool[itsTop++] = mh.tag;
            itsTop = ClassFileWriter.putInt16(ref, itsPool, itsTop);
            itsConstantHash.put(mh, theIndex);
            itsPoolTypes.put(theIndex, CONSTANT_MethodHandle);
        }
        return theIndex;
    }

    Object getConstantData(int index) {
        return itsConstantData.get(index);
    }

    void setConstantData(int index, Object data) {
        if (index < 1 || index > MAX_POOL_INDEX) {
            throw new ClassFileWriter.ClassSizeException("Constant pool overflow");
        }
        itsConstantData.put(index, data);
    }

    byte getConstantType(int index) {
        return itsPoolTypes.getOrDefault(index, (byte) 0);
    }

    private void ensure(int howMuch) {
        if (itsTop + howMuch > itsPool.length) {
            int newCapacity = itsPool.length * 2;
            if (itsTop + howMuch > newCapacity) {
                newCapacity = itsTop + howMuch;
            }
            byte[] tmp = new byte[newCapacity];
            System.arraycopy(itsPool, 0, tmp, 0, itsTop);
            itsPool = tmp;
        }
    }

    private ClassFileWriter cfw;

    private static final int MAX_UTF_ENCODING_SIZE = 65535;

    private final HashMap<Integer, Integer> itsStringConstHash = new HashMap<>();
    private final HashMap<String, Integer> itsUtf8Hash = new HashMap<>();
    private final HashMap<FieldOrMethodRef, Integer> itsFieldRefHash = new HashMap<>();
    private final HashMap<FieldOrMethodRef, Integer> itsMethodRefHash = new HashMap<>();
    private final HashMap<FieldOrMethodRef, Integer> itsInterfaceMethodRefHash = new HashMap<>();
    private final HashMap<String, Integer> itsClassHash = new HashMap<>();
    private final HashMap<Object, Integer> itsConstantHash = new HashMap<>();

    private int itsTop;
    private int itsTopIndex;
    private final HashMap<Integer, Object> itsConstantData = new HashMap<>();
    private final HashMap<Integer, Byte> itsPoolTypes = new HashMap<>();
    private byte[] itsPool;
}
