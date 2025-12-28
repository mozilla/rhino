/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

/**
 * Represents a set operation in ES2024 v-flag character classes (intersection, subtraction).
 *
 * <p>See ECMA 262 §22.2.1.
 *
 * @see ClassContents
 */
final class SetOperation {
    /** Types of set operations: SUBTRACT (--) and INTERSECT (&&). */
    enum Type {
        /** Subtraction operator (--): characters in left operand but not in right operand. */
        SUBTRACT,

        /** Intersection operator (&&): only characters present in both operands. */
        INTERSECT
    }

    /** The type of set operation (SUBTRACT or INTERSECT). */
    private final Type type;

    /** The right-hand operand of the operation (parsed character class contents). */
    private final ClassContents operand;

    /**
     * Pre-built RECharSet for efficient matching at runtime. Computed during compilation by
     * buildOperandCharSets(). Package-private for mutation during compilation.
     */
    RECharSet operandCharSet;

    /**
     * Creates a set operation with the specified type and operand.
     *
     * @param type The operation type (SUBTRACT or INTERSECT)
     * @param operand The right-hand operand (character class contents)
     */
    SetOperation(Type type, ClassContents operand) {
        this.type = type;
        this.operand = operand;
    }

    /**
     * Gets the operation type.
     *
     * @return the operation type (SUBTRACT or INTERSECT)
     */
    Type getType() {
        return type;
    }

    /**
     * Gets the right-hand operand.
     *
     * @return the operand character class contents
     */
    ClassContents getOperand() {
        return operand;
    }

    /**
     * Gets the pre-built character set for runtime matching.
     *
     * @return the compiled character set, or null if not yet compiled
     */
    RECharSet getOperandCharSet() {
        return operandCharSet;
    }
}
