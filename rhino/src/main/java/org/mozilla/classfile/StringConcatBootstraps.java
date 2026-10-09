/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.classfile;

import java.lang.invoke.MethodHandles;

/**
 * Bootstrap methods for string constants that are too long to be written as a single {@code
 * CONSTANT_String}. See {@link ClassFileWriter#describeStringConcatenation}.
 */
public final class StringConcatBootstraps {
    private StringConcatBootstraps() {}

    public static String concat(
            MethodHandles.Lookup lookup, String name, Class<?> type, String... parts) {
        return String.join("", parts);
    }
}
