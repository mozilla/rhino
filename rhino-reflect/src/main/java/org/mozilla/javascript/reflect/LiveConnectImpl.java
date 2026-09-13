/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.reflect;

import org.mozilla.javascript.LiveConnect;
import org.mozilla.javascript.WrapProcessor;
import org.mozilla.javascript.lc.type.TypeInfo;

public class LiveConnectImpl implements LiveConnect {
    @Override
    public WrapProcessor getWrapProcessor() {
        return new WrapProcessorImpl();
    }

    @Override
    public Object coerceType(TypeInfo type, Object value) {
        return NativeJavaObject.coerceTypeImpl(type, value);
    }
}
