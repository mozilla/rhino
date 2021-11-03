/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript;

import java.util.Map;

/**
 * Converter for index based java-map access.
 *
 * <p>When accessing java maps with javascript notation like <code>var x = map[42]</code> or <code>
 * var x = map['key']</code>, the key could be either a string or an integer. There are cases where
 * you do not have a <code>Map&lt;String,  ?&gt;></code> or <code>
 * Map&lt;Integer,  ?&gt;></code> but a <code>Map&lt;Long,  ?&gt;></code>. In this case, it is
 * impossible to access the map value with index based access.
 *
 * <p>Note: This conversion takes only place, when <code>FEATURE_ENABLE_JAVA_MAP_ACCESS</code> is
 * set in context.
 *
 * @author Roland Praml, FOCONIS AG
 */
@FunctionalInterface
public interface MapKeyConverter {

    /**
     * Converts the <code>key</code> to the <code>keyType</code>. The returned key should be
     * compatible with the passed <code>map</code>
     *
     * @param key the key (could be either String or Integer)
     * @param keyType the desired type
     * @param map the map.
     */
    Object toKey(Object key, Class<?> keyType, Map<?, ?> map);
}
