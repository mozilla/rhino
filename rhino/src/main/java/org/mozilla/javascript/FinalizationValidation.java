/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * Validation utilities for FinalizationRegistry operations.
 *
 * <p>Centralizes all validation logic for targets, tokens, and values used in FinalizationRegistry
 * according to ECMAScript 2021 specification requirements.
 */
final class FinalizationValidation {

    private FinalizationValidation() {
        // Utility class - no instances
    }

    /**
     * Validates that a target can be registered for finalization.
     *
     * @param target the target object to validate
     * @throws EcmaError if target is invalid
     */
    static void validateTarget(Object target) {
        if (!isValidTarget(target)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.finalizationregistry.invalid.target", ScriptRuntime.typeof(target));
        }
    }

    /**
     * Validates an unregister token for register() method (allows undefined).
     *
     * @param token the token to validate
     * @throws EcmaError if token is invalid
     */
    static void validateUnregisterToken(Object token) {
        if (!Undefined.isUndefined(token) && !canBeHeldWeakly(token)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.finalizationregistry.invalid.unregister.token",
                    ScriptRuntime.typeof(token));
        }
    }

    /**
     * Validates an unregister token for unregister() method (strict - no undefined).
     *
     * @param token the token to validate
     * @throws EcmaError if token is invalid
     */
    static void validateUnregisterTokenStrict(Object token) {
        if (!canBeHeldWeakly(token)) {
            throw ScriptRuntime.typeError(
                    "FinalizationRegistry unregister token must be an object, got "
                            + ScriptRuntime.typeof(token));
        }
    }

    /**
     * Validates that target and held value are not the same object.
     *
     * @param target the target object
     * @param heldValue the held value
     * @throws EcmaError if they are the same
     */
    static void validateNotSameValue(Object target, Object heldValue) {
        if (isSameValue(target, heldValue)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.finalizationregistry.target.same.as.held");
        }
    }

    /**
     * Check if the given object can be used as a registration target.
     *
     * @param target the target object to validate
     * @return true if target is a valid object that can be registered
     */
    private static boolean isValidTarget(Object target) {
        return ScriptRuntime.isObject(target) || (target instanceof Symbol);
    }

    /**
     * Check if the given value can be held weakly (used for unregister tokens).
     *
     * <p>Per ECMAScript specification, registered symbols (created with Symbol.for()) cannot be
     * held weakly because they persist in the global registry and are not garbage collectable.
     *
     * @param value the value to check
     * @return true if value can be used as an unregister token
     */
    private static boolean canBeHeldWeakly(Object value) {
        if (ScriptRuntime.isObject(value)) {
            return true;
        }
        if (value instanceof Symbol) {
            Symbol symbol = (Symbol) value;
            return symbol.getKind() != Symbol.Kind.REGISTERED;
        }
        return false;
    }

    /**
     * Implements SameValue comparison per ECMAScript specification.
     *
     * @param a first value
     * @param b second value
     * @return true if values are the same per SameValue semantics
     */
    private static boolean isSameValue(Object a, Object b) {
        if (a == b) return true;
        if (a instanceof Number && b instanceof Number) {
            double da = ((Number) a).doubleValue();
            double db = ((Number) b).doubleValue();
            return Double.compare(da, db) == 0;
        }
        return false;
    }
}