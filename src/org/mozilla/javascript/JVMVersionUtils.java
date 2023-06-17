/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

public enum JVMVersionUtils {
    JAVA_1_8_UNDER,
    JAVA_1_8,
    JAVA_1_8_ABOVE;

    public static JVMVersionUtils getJavaVersion() {
        if (ScriptRuntime.IS_ANDROID) return JAVA_1_8;

        String specVersion = System.getProperty("java.specification.version");

        if (specVersion.startsWith("1.")) {
            int version = Integer.parseInt(specVersion.substring(2));
            if (version < 8) {
                return JAVA_1_8_UNDER;
            } else {
                return JAVA_1_8;
            }
        } else {
            return JAVA_1_8_ABOVE;
        }
    }
}
