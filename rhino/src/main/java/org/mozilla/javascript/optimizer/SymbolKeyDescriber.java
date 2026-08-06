/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.optimizer;

import static org.mozilla.javascript.Symbol.Kind.BUILT_IN;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import org.mozilla.classfile.DynamicConstantDescriber;
import org.mozilla.javascript.SymbolKey;

/**
 * Describes symbols as dynamic constants that resolve to the static fields of {@link SymbolKey}.
 *
 * <p>Because SymbolKey compares by identity, we need to include an additional parameter based on
 * identify hash to avoid different symbols with the same name being accidentally conflated.
 *
 * <p>Symbols of any other kind are not describable: one created by {@code Symbol()} has an identity
 * that cannot be reconstructed, and a registered symbol from {@code Symbol.for} must be looked up
 * in the global registry rather than baked into a class file.
 *
 * <p>This lives alongside the rest of the bytecode back end rather than in SymbolKey itself, so
 * that runtimes without bytecode generation support never have to resolve {@code
 * java.lang.constant}.
 */
public class SymbolKeyDescriber implements DynamicConstantDescriber<SymbolKey> {

    private static final ClassDesc CD_SYMBOL_KEY = ClassDesc.of(SymbolKey.class.getName());

    private static final DirectMethodHandleDesc SYMBOL_REGULAR =
            ConstantDescs.ofConstantBootstrap(
                    CD_SYMBOL_KEY, "symbolConstant", CD_SYMBOL_KEY, ConstantDescs.CD_int);

    @Override
    public Class<SymbolKey> describedType() {
        return SymbolKey.class;
    }

    @Override
    public DynamicConstantDesc<SymbolKey> describe(SymbolKey value) {
        return switch (value.getKind()) {
            case BUILT_IN ->
                    DynamicConstantDesc.ofNamed(
                            ConstantDescs.BSM_GET_STATIC_FINAL,
                            WellKnownFields.NAMES.get(value),
                            CD_SYMBOL_KEY,
                            CD_SYMBOL_KEY);
            case REGULAR ->
                    DynamicConstantDesc.ofNamed(
                            SYMBOL_REGULAR,
                            value.getName(),
                            CD_SYMBOL_KEY,
                            Integer.valueOf(System.identityHashCode(value)));
            default -> {
                throw new IllegalStateException("Symbol descriptor could not be generated");
            }
        };
    }

    /**
     * Maps each well-known symbol to the name of the SymbolKey field holding it. The field names
     * differ from the symbol names, and scanning for them means a newly added well-known symbol
     * becomes describable without anything here having to be updated.
     */
    private static final class WellKnownFields {
        static final Map<SymbolKey, String> NAMES = scan();

        private static Map<SymbolKey, String> scan() {
            var names = new HashMap<SymbolKey, String>();
            for (Field field : SymbolKey.class.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (field.getType() == SymbolKey.class
                        && Modifier.isPublic(modifiers)
                        && Modifier.isStatic(modifiers)
                        && Modifier.isFinal(modifiers)) {
                    try {
                        names.put((SymbolKey) field.get(null), field.getName());
                    } catch (IllegalAccessException iae) {
                        throw new IllegalStateException(iae);
                    }
                }
            }
            return names;
        }
    }
}
