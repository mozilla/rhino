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
import org.mozilla.javascript.Undefined;

/**
 * Describes {@link Undefined} as a dynamic constant that resolves to the canonical {@link
 * Undefined#instance}.
 */
public class UndefinedDescriber implements DynamicConstantDescriber<Undefined> {

    private static final ClassDesc CD_UNDEFINED = ClassDesc.of(Undefined.class.getName());

    private static final DirectMethodHandleDesc UNDEFINED_CONSTANT =
            ConstantDescs.ofConstantBootstrap(CD_UNDEFINED, "undefinedConstant", CD_UNDEFINED);

    @Override
    public Class<Undefined> describedType() {
        return Undefined.class;
    }

    @Override
    public DynamicConstantDesc<Undefined> describe(Undefined value) {
        return DynamicConstantDesc.ofNamed(UNDEFINED_CONSTANT, "undefined", CD_UNDEFINED);
    }
}
