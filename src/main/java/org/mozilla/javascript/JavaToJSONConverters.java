/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Collections;
import org.mozilla.javascript.function.UnaryOperator;

/**
 * This class provides implementations of converters for Java objects to be used by the
 * JSON.stringify method.
 *
 * <p>JSON.stringify will automatically convert instances of java.util.Map to javascript objects.
 * Instances of java.util.Collection and java Arrays will be converted to javascript arrays.
 *
 * <p>This is a final effort at conversion for other java objects that appear as values, and may be
 * preempted by objects which define a toJSON() method or by a replacer function passed to
 * JSON.stringify. The return value will, in turn, be converted according to {@link
 * Context#javaToJS} and stringified.
 *
 * @author Tony Germano
 */
public class JavaToJSONConverters {

    private JavaToJSONConverters() {
    }

    /**
     * Convert Object to its toString() value.
     */
    public static final UnaryOperator<Object> STRING = Object::toString;

    /**
     * Always return undefined
     */
    public static final UnaryOperator<Object> UNDEFINED = o -> Undefined.instance;

    /**
     * Always return an empty object
     */
    public static final UnaryOperator<Object> EMPTY_OBJECT = o -> Collections.EMPTY_MAP;

    /**
     * Throw a TypeError naming the class that could not be converted
     */
    public static final UnaryOperator<Object> THROW_TYPE_ERROR =
            o -> {
                throw ScriptRuntime.typeErrorById(
                        "msg.json.cant.serialize", o.getClass().getName());
            };
}
