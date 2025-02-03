package org.mozilla.javascript.config;

/**
 * Optional Loader interface for loading properties. You can override the default loading mechanism
 * by providing your implementation as service.
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
public interface RhinoPropertiesLoader {

    /** Classes can add their configs to rhinoProperties. */
    void load(RhinoProperties properties);
}
