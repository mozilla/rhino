package org.mozilla.javascript.drivers;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * Intended to be used as an optional annotation for subclasses
 * of {@link org.mozilla.javascript.drivers.ScriptTestsBase}.
 * Sets the language version of test's script execution context.
 */
@Target(TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LanguageVersion {
    int value();
}
