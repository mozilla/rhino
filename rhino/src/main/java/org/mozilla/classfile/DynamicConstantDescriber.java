/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.classfile;

import java.lang.constant.DynamicConstantDesc;
import java.util.Optional;

/**
 * Turns values of a particular type into the {@code CONSTANT_Dynamic} description that {@link
 * ClassFileWriter} writes to the constant pool.
 *
 * <p>Describers are registered with a writer using {@link
 * ClassFileWriter#registerDynamicConstantDescriber}, which keys them on {@link #describedType}.
 *
 * @param <T> the type of value described
 */
public interface DynamicConstantDescriber<T extends DynamicConstant> {

    /** The type of value this describer handles. Used as the registration key. */
    Class<T> describedType();

    /**
     * Describe the given value as a dynamic constant, or return an empty result if this particular
     * value cannot be represented as one.
     */
    Optional<DynamicConstantDesc<T>> describe(T value);
}
