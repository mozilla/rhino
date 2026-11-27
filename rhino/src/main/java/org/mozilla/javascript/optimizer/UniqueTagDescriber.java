/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.optimizer;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicConstantDesc;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;
import org.mozilla.classfile.DynamicConstantDescriber;
import org.mozilla.classfile.DynamicDescriberRegistry;
import org.mozilla.javascript.UniqueTag;

/**
 * Describes {@link UniqueTag} values as dynamic constants that resolve to the static fields of
 * {@link UniqueTag}, since tags are compared by identity.
 */
public class UniqueTagDescriber extends DynamicConstantDescriber<UniqueTag> {

    private static final ClassDesc CD_UNIQUE_TAG = ClassDesc.of(UniqueTag.class.getName());

    public UniqueTagDescriber(DynamicDescriberRegistry registry) {
        super(registry);
    }

    @Override
    public Class<UniqueTag> describedType() {
        return UniqueTag.class;
    }

    @Override
    public DynamicConstantDesc<UniqueTag> describe(UniqueTag value) {
        String name = TagFields.NAMES.get(value);
        if (name == null) {
            throw new IllegalStateException("UniqueTag descriptor could not be generated");
        }
        return DynamicConstantDesc.ofNamed(
                ConstantDescs.BSM_GET_STATIC_FINAL, name, CD_UNIQUE_TAG, CD_UNIQUE_TAG);
    }

    /** Maps each tag to the name of the UniqueTag field holding it. */
    private static final class TagFields {
        static final Map<UniqueTag, String> NAMES = scan();

        private static Map<UniqueTag, String> scan() {
            var names = new IdentityHashMap<UniqueTag, String>();
            for (Field field : UniqueTag.class.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (field.getType() == UniqueTag.class
                        && Modifier.isPublic(modifiers)
                        && Modifier.isStatic(modifiers)
                        && Modifier.isFinal(modifiers)) {
                    try {
                        names.put((UniqueTag) field.get(null), field.getName());
                    } catch (IllegalAccessException iae) {
                        throw new IllegalStateException(iae);
                    }
                }
            }
            return names;
        }
    }
}
