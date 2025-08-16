/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

/** Property of an object literal. */
public abstract class AbstractObjectProperty extends AstNode {
    protected AbstractObjectProperty() {
        super();
    }

    protected AbstractObjectProperty(int pos) {
        super(pos);
    }

    protected AbstractObjectProperty(int pos, int len) {
        super(pos, len);
    }
}
