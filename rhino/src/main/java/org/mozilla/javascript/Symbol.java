/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * A Symbol is a JavaScript object that obeys the special properties of the Symbol prototype. This
 * interface lets us possibly support multiple implementations of Symbol.
 *
 * @since 1.7.8
 */
public interface Symbol {

    /** Represents the kind of the symbol, built in, registered, or normal. */
    enum Kind {
        REGULAR, // A regular symbol is created using the constructor
        BUILT_IN, // A built-in symbol is one of the properties of the "Symbol" constructor
        REGISTERED // A registered symbol was created using "Symbol.for"
    }

    /**
     * Returns the symbol's name. Returns empty string for anonymous symbol (i.e. something created
     * with {@code Symbol()}).
     */
    String getName();

    /** Returns the symbol's kind. */
    Kind getKind();
}
