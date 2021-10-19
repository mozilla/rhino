/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.classfile;

/**
 * Helper class for internal representations of type information. In most cases, type information
 * can be represented by a constant, but in some cases, a payload is included. Despite the payload
 * coming after the type tag in the output, we store it in bits 8-23 for uniformity; the tag is
 * always in bits 0-7.
 */
final class TypeInfo {
    private TypeInfo() {}

    static final int TOP = 0;
    static final int INTEGER = 1;
    static final int FLOAT = 2;
    static final int DOUBLE = 3;
    static final int LONG = 4;
    static final int NULL = 5;
    static final int UNINITIALIZED_THIS = 6;
    static final int OBJECT_TAG = 7;
    static final int UNINITIALIZED_VAR_TAG = 8;

    static final int OBJECT(int constantPoolIndex) {
        return ((constantPoolIndex & 0xFFFF) << 8) | OBJECT_TAG;
    }

    static final int OBJECT(String type, ConstantPool pool) {
        return OBJECT(pool.addClass(type));
    }

    static final int UNINITIALIZED_VARIABLE(int bytecodeOffset) {
        return ((bytecodeOffset & 0xFFFF) << 8) | UNINITIALIZED_VAR_TAG;
    }

    static final int getTag(int typeInfo) {
        return typeInfo & 0xFF;
    }

    static final int getPayload(int typeInfo) {
        return typeInfo >>> 8;
    }

    /**
     * Treat the result of getPayload as a constant pool index and fetch the corresponding String
     * mapped to it.
     *
     * <p>Only works on OBJECT types.
     */
    static final String getPayloadAsType(int typeInfo, ConstantPool pool) {
        if (getTag(typeInfo) == OBJECT_TAG) {
            return (String) pool.getConstantData(getPayload(typeInfo));
        }
        throw new IllegalArgumentException("expecting object type");
    }

    /** Create type information from an internal type. */
    static final int fromType(String type, ConstantPool pool) {
        if (type.length() == 1) {
            switch (type.charAt(0)) {
                case 'B': // sbyte
                case 'C': // unicode char
                case 'S': // short
                case 'Z': // boolean
                case 'I': // all of the above are verified as integers
                    return INTEGER;
                case 'D':
                    return DOUBLE;
                case 'F':
                    return FLOAT;
                case 'J':
                    return LONG;
                default:
                    throw new IllegalArgumentException("bad type");
            }
        }
        return TypeInfo.OBJECT(type, pool);
    }

    static boolean isTwoWords(int type) {
        return type == DOUBLE || type == LONG;
    }

    /**
     * Merge two verification types.
     *
     * <p>In most cases, the verification types must be the same. For example, INTEGER and DOUBLE
     * cannot be merged and an exception will be thrown. The basic rules are:
     *
     * <p>- If the types are equal, simply return one. - If either type is TOP, return TOP. - If
     * either type is NULL, return the other type. - If both types are objects, find the lowest
     * common ancestor in the class hierarchy.
     *
     * <p>This method uses reflection to traverse the class hierarchy. Therefore, it is assumed that
     * the current class being generated is never the target of a full object-object merge, which
     * would need to load the current class reflectively.
     */
    static int merge(int current, int incoming, ConstantPool pool) {
        if (current == incoming) {
            return current;
        }
        int currentTag = getTag(current);
        int incomingTag = getTag(incoming);
        boolean currentIsObject = currentTag == TypeInfo.OBJECT_TAG;
        boolean incomingIsObject = incomingTag == TypeInfo.OBJECT_TAG;

        if (currentIsObject && incoming == NULL) {
            return current;
        } else if (currentTag == TypeInfo.TOP || incomingTag == TypeInfo.TOP) {
            return TypeInfo.TOP;
        } else if (current == NULL && incomingIsObject) {
            return incoming;
        } else if (currentIsObject && incomingIsObject) {
            String currentName = getPayloadAsType(current, pool);
            String incomingName = getPayloadAsType(incoming, pool);
            // The class file always has the class and super names in the same
            // spot. The constant order is: class_data, class_name, super_data,
            // super_name.
            String currentlyGeneratedName = (String) pool.getConstantData(2);
            String currentlyGeneratedSuperName = (String) pool.getConstantData(4);

            // If any of the merged types are the class that's currently being
            // generated, automatically start at the super class instead. At
            // this point, we already know the classes are different, so we
            // don't need to handle that case.
            if (currentName.equals(currentlyGeneratedName)) {
                currentName = currentlyGeneratedSuperName;
            }
            if (incomingName.equals(currentlyGeneratedName)) {
                incomingName = currentlyGeneratedSuperName;
            }

            Class<?> currentClass = getClassFromInternalName(currentName);
            Class<?> incomingClass = getClassFromInternalName(incomingName);

            if (currentClass.isAssignableFrom(incomingClass)) {
                return current;
            } else if (incomingClass.isAssignableFrom(currentClass)) {
                return incoming;
            } else if (incomingClass.isInterface() || currentClass.isInterface()) {
                // For verification purposes, Sun specifies that interfaces are
                // subtypes of Object. Therefore, we know that the merge result
                // involving interfaces where one is not assignable to the
                // other results in Object.
                return OBJECT("java/lang/Object", pool);
            } else {
                Class<?> commonClass = incomingClass.getSuperclass();
                while (commonClass != null) {
                    if (commonClass.isAssignableFrom(currentClass)) {
                        String name = commonClass.getName();
                        name = ClassFileWriter.getSlashedForm(name);
                        return OBJECT(name, pool);
                    }
                    commonClass = commonClass.getSuperclass();
                }
            }
        }
        throw new IllegalArgumentException(
                "bad merge attempt between "
                        + toString(current, pool)
                        + " and "
                        + toString(incoming, pool));
    }

    static String toString(int type, ConstantPool pool) {
        int tag = getTag(type);
        switch (tag) {
            case TypeInfo.TOP:
                return "top";
            case TypeInfo.INTEGER:
                return "int";
            case TypeInfo.FLOAT:
                return "float";
            case TypeInfo.DOUBLE:
                return "double";
            case TypeInfo.LONG:
                return "long";
            case TypeInfo.NULL:
                return "null";
            case TypeInfo.UNINITIALIZED_THIS:
                return "uninitialized_this";
            default:
                if (tag == TypeInfo.OBJECT_TAG) {
                    return getPayloadAsType(type, pool);
                } else if (tag == TypeInfo.UNINITIALIZED_VAR_TAG) {
                    return "uninitialized";
                } else {
                    throw new IllegalArgumentException("bad type");
                }
        }
    }

    /**
     * Take an internal name and return a java.lang.Class instance that represents it.
     *
     * <p>For example, given "java/lang/Object", returns the equivalent of
     * Class.forName("java.lang.Object"), but also handles exceptions.
     */
    private static Class<?> getClassFromInternalName(String internalName) {
        try {
            return Class.forName(internalName.replace('/', '.'));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static String toString(int[] types, int typesTop, ConstantPool pool) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < typesTop; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(toString(types[i], pool));
        }
        sb.append("]");
        return sb.toString();
    }

    static void print(int[] locals, int[] stack, ConstantPool pool) {
        print(locals, locals.length, stack, stack.length, pool);
    }

    static void print(int[] locals, int localsTop, int[] stack, int stackTop, ConstantPool pool) {
        System.out.print("locals: ");
        System.out.println(toString(locals, localsTop, pool));
        System.out.print("stack: ");
        System.out.println(toString(stack, stackTop, pool));
        System.out.println();
    }
}
