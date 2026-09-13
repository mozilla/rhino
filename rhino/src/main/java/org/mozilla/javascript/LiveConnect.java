/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import org.mozilla.javascript.lc.type.TypeInfo;

/**
 * An interface for LiveConnect, which optionally uses reflection to allow direct access to Java
 * classes, properties, and methods.
 */
public interface LiveConnect {
    Object coerceType(TypeInfo type, Object value);

    WrapProcessor getWrapProcessor();
}
