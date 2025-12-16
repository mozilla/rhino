/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

/**
 * Types of set operations supported in ES2024 v-flag character classes.
 *
 * <p>ES2024 introduced set operations for character classes when using the v (unicodeSets) flag.
 * These operations allow combining character classes using mathematical set operations:
 *
 * <ul>
 *   <li><b>SUBTRACT</b> ({@code --}) - Set difference: all characters in first set but not in
 *       second
 *   <li><b>INTERSECT</b> ({@code &&}) - Set intersection: only characters present in both sets
 * </ul>
 *
 * <p>Example patterns:
 *
 * <pre>
 * /[\w--[0-9]]/v     // Word characters minus digits (letters and underscore only)
 * /[\p{Letter}&&\p{ASCII}]/v  // Letters that are also ASCII
 * </pre>
 *
 * <p>Extracted from NativeRegExp to improve modularity during refactoring.
 *
 * @see SetOperation
 * @see ClassContents
 */
enum SetOperationType {
    /** Subtraction operator (--): characters in left operand but not in right operand. */
    SUBTRACT,

    /** Intersection operator (&&): only characters present in both operands. */
    INTERSECT
}
