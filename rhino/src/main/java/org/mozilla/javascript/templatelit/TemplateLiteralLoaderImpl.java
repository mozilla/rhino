/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.templatelit;

import org.mozilla.javascript.TemplateLiteralLoader;
import org.mozilla.javascript.TemplateLiteralProxy;

/**
 * Default template literal loader implementation.
 *
 * @author Anivar Aravind
 */
public class TemplateLiteralLoaderImpl implements TemplateLiteralLoader {

    @Override
    public TemplateLiteralProxy newProxy() {
        return new DefaultTemplateLiteralProxy();
    }
}
