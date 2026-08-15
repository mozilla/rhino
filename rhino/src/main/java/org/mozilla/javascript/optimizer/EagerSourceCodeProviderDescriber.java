/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.optimizer;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import org.mozilla.classfile.DynamicConstantDescriber;
import org.mozilla.javascript.EagerSourceCodeProvider;

/**
 * Describes {@link EagerSourceCodeProvider} as a dynamic constant that is reconstructed from its
 * raw source when the constant is first resolved.
 *
 * <p>The source cannot be carried in the constant's name, because a name may not be empty and may
 * not contain the "[" and ";" characters that ordinary JavaScript source is full of. The source is
 * therefore a bootstrap argument, and the name is a fixed placeholder.
 */
public class EagerSourceCodeProviderDescriber
        implements DynamicConstantDescriber<EagerSourceCodeProvider> {

    private static final ClassDesc CD_EAGER_SOURCE_CODE_PROVIDER =
            ClassDesc.of(EagerSourceCodeProvider.class.getName());

    private static final DirectMethodHandleDesc SOURCE_CONSTANT =
            ConstantDescs.ofConstantBootstrap(
                    CD_EAGER_SOURCE_CODE_PROVIDER,
                    "sourceConstant",
                    CD_EAGER_SOURCE_CODE_PROVIDER,
                    ConstantDescs.CD_String);

    @Override
    public Class<EagerSourceCodeProvider> describedType() {
        return EagerSourceCodeProvider.class;
    }

    @Override
    public DynamicConstantDesc<EagerSourceCodeProvider> describe(EagerSourceCodeProvider value) {
        return DynamicConstantDesc.ofNamed(
                SOURCE_CONSTANT, "source", CD_EAGER_SOURCE_CODE_PROVIDER, value.getRawSource());
    }
}
