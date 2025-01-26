package org.mozilla.javascript;

import java.io.Serializable;

/**
 * This interface makes it possible to pass a lambda function to the various methods in
 * LambdaConstructor and LambdaFunction that require a Constructable that is also Serializable.
 */
public interface SerializableConstructable extends Constructable, Serializable {}
