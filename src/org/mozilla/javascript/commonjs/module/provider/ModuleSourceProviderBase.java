package org.mozilla.javascript.commonjs.module.provider;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.StringTokenizer;

import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * A base implementation for all module script providers that actually load 
 * module scripts. Performs validation of identifiers, allows loading from
 * preferred locations (attempted before require.paths), from require.paths 
 * itself, and from fallback locations (attempted after require.paths). Note 
 * that while this base class strives to be as generic as possible, it does 
 * have loading from an URI built into its design, for the simple reason that
 * the require.paths is defined in terms of URIs. 
 * @author Attila Szegedi
 * @version $Id: ModuleSourceProviderBase.java,v 1.2 2011/04/01 02:39:19 hannes%helma.at Exp $
 */
public class ModuleSourceProviderBase implements ModuleSourceProvider, Serializable
{
    private static final long serialVersionUID = 1L;

    public ModuleSource getModuleSource(String moduleId, Scriptable paths,
            Object validator) throws IOException 
    {
        if(!isValidModuleIdentifier(moduleId)) {
            throw new IllegalArgumentException("ModuleScript ID '" + moduleId + 
                    "' is not valid");
        }
        if(!entityNeedsRevalidation(validator)) {
            return NOT_MODIFIED;
        }
        
        ModuleSource moduleSource = loadModuleSourceFromPrivilegedLocations(
                moduleId, validator);
        if(moduleSource != null) {
            return moduleSource;
        }
        if(paths != null) {
            moduleSource = loadModuleSourceFromPathArray(moduleId, paths, 
                    validator);
            if(moduleSource != null) {
                return moduleSource;
            }
        }
        return loadModuleSourceFromFallbackLocations(moduleId, validator);
    }

    private ModuleSource loadModuleSourceFromPathArray(String moduleId, 
            Scriptable paths, Object validator) throws IOException
    {
        final long llength = ScriptRuntime.toUint32(
                ScriptableObject.getProperty(paths, "length"));
        // Yeah, I'll ignore entries beyond Integer.MAX_VALUE; so sue me.
        int ilength = llength > Integer.MAX_VALUE ? Integer.MAX_VALUE : 
            (int)llength;
        final String relativeModuleUri = moduleId + ".js";
        for(int i = 0; i < ilength; ++i) {
            final String path = ensureTrailingSlash(
                    ScriptableObject.getTypedProperty(paths, i, String.class));
            try {
                final ModuleSource moduleSource = loadModuleSourceFromUri(
                        new URI(path).resolve(relativeModuleUri), validator);
                if(moduleSource != null) {
                    return moduleSource;
                }
            }
            catch(URISyntaxException e) {
                throw new MalformedURLException(e.getMessage());
            }
        }
        return null;
    }

    private static String ensureTrailingSlash(String path) {
        return path.endsWith("/") ? path : path.concat("/");
    }

    /**
     * Override to determine whether according to the validator, the cached 
     * module script needs revalidation. A validator can carry expiry 
     * information. If the cached representation is not expired, it doesn'
     * t need revalidation, otherwise it does. When no cache revalidation is
     * required, the external resource will not be contacted at all, so some 
     * level of expiry (staleness tolerance) can greatly enhance performance. 
     * The default implementation always returns true so it will always require 
     * revalidation.
     * @param validator the validator
     * @return returns true if the cached module needs revalidation. 
     */
    protected boolean entityNeedsRevalidation(Object validator) {
        return true;
    }
    
    /**
     * Override in a subclass to load a module script from a URI. It is used
     * to load scripts from the path array specified by require.paths property.
     * By default, returns null (which is identical to basically ignoring 
     * require.paths).
     * @param uri the URI of the script
     * @param validator a validator that can be used to revalidate an existing
     * cached source at the URI. Can be null if there is no cached source 
     * available.
     * @return the loaded module script, or null if it can't be found, or 
     * {@link ModuleSourceProvider#NOT_MODIFIED} if it revalidated the existing 
     * cached source against the URI.
     * @throws IOException if the module script was found, but an I/O exception
     * prevented it from being loaded.
     */
    protected ModuleSource loadModuleSourceFromUri(URI uri, Object validator) 
    throws IOException
    {
        return null;
    }

    /**
     * Override to obtain a module source from privileged locations. This will 
     * be called before source is attempted to be obtained from URIs specified 
     * in require.paths.
     * @param moduleId the ID of the module
     * @param validator a validator that can be used to validate an existing
     * cached script. Can be null if there is no cached script available.
     * @return the loaded module script, or null if it can't be found in the
     * privileged locations, or {@link ModuleSourceProvider#NOT_MODIFIED} if
     * the existing cached module script is still valid.
     * @throws IOException if the module script was found, but an I/O exception
     * prevented it from being loaded.
     */
    protected ModuleSource loadModuleSourceFromPrivilegedLocations(
            String moduleId, Object validator) throws IOException
    {
        return null;
    }

    /**
     * Override to obtain a module source from fallback locations. This will 
     * be called after source is attempted to be obtained from URIs specified 
     * in require.paths.
     * @param moduleId the ID of the module
     * @param validator a validator that can be used to validate an existing
     * cached script. Can be null if there is no cached script available.
     * @return the loaded module script, or null if it can't be found in the
     * privileged locations, or {@link ModuleSourceProvider#NOT_MODIFIED} if
     * the existing cached module script is still valid.
     * @throws IOException if the module script was found, but an I/O exception
     * prevented it from being loaded.
     */
    protected ModuleSource loadModuleSourceFromFallbackLocations(
            String moduleId, Object validator) throws IOException
    {
        return null;
    }

    /**
     * Tests whether a module ID is valid.
     * @param moduleId the module ID. It must not be relative (must have 
     * already been resolved into an absolute module ID).
     * @return true if it is a valid module ID, false otherwise.
     */
    public static boolean isValidModuleIdentifier(String moduleId) {
        if(moduleId == null || moduleId.length() == 0 || 
                moduleId.charAt(0) == '/') {
            return false;
        }
        final StringTokenizer tok = new StringTokenizer(moduleId, "/");
        while(tok.hasMoreTokens()) {
            final String term = tok.nextToken();
            if(!isValidTerm(term)) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean isValidTerm(String term) {
        final int l = term.length();
        if(!Character.isJavaIdentifierStart(term.charAt(0))) {
            return false;
        }
        for(int i = 1; i < l; ++i) {
            if(!Character.isJavaIdentifierPart(term.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}