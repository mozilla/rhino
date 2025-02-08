package org.mozilla.javascript;

/** This interface is used to load the RegExp implementation from the classpath. */
public interface RegExpLoader {
    // Create a new instance of a RegExpProxy
    RegExpProxy newProxy();
}
