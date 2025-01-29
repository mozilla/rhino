package org.mozilla.javascript;

import java.io.Serializable;

/**
 * This interface makes it possible to pass a lambda function to the various methods in
 * LambdaConstructor and LambdaFunction that require a Callable that is also Serializable. Code that
 * works with lambdas will largely "not notice" this interface, but it will make it possible for
 * lambda-based classes to work with serialization like older Rhino native classes.
 */
public interface SerializableCallable extends Callable, Serializable {}
