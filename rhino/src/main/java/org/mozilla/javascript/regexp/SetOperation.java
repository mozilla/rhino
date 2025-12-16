/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

/**
 * Represents a set operation in ES2024 v-flag character classes.
 *
 * <p>In ES2024, the v (unicodeSets) flag enables set operations within character classes. This
 * class represents a single operation (intersection or subtraction) with its right-hand operand.
 *
 * <p>Example: In {@code /[\w--[0-9]]/v}, the {@code --[0-9]} part is represented as a SetOperation
 * with:
 *
 * <ul>
 *   <li>type = SUBTRACT
 *   <li>operand = ClassContents containing [0-9]
 * </ul>
 *
 * <p>The {@code operandCharSet} field is computed during compilation for efficient runtime
 * matching.
 *
 * <p>Extracted from NativeRegExp to improve modularity during refactoring.
 *
 * @see SetOperationType
 * @see ClassContents
 */
class SetOperation {
    /** The type of set operation (SUBTRACT or INTERSECT). */
    SetOperationType type;

    /** The right-hand operand of the operation (parsed character class contents). */
    ClassContents operand;

    /**
     * Pre-built RECharSet for efficient matching at runtime. Computed during compilation by
     * buildOperandCharSets().
     */
    RECharSet operandCharSet;

    /**
     * Creates a set operation with the specified type and operand.
     *
     * @param type The operation type (SUBTRACT or INTERSECT)
     * @param operand The right-hand operand (character class contents)
     */
    SetOperation(SetOperationType type, ClassContents operand) {
        this.type = type;
        this.operand = operand;
    }
}
