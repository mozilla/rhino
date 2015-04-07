/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.jdk18;

import java.lang.reflect.Method;

public class VMBridge_jdk18 extends org.mozilla.javascript.jdk15.VMBridge_jdk15
{
    public VMBridge_jdk18() throws SecurityException, InstantiationException {
        super();
    }

    @Override
    public boolean isDefaultMethod(Method method) {
        return method.isDefault();
    }
}
