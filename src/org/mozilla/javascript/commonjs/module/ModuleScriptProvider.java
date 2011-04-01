package org.mozilla.javascript.commonjs.module;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Should be implemented by Rhino embeddings to allow the require() function to
 * obtain {@link ModuleScript} objects. We provide two default implementations,
 * but you can of course roll your own if they don't suit your needs. 
 * @author Attila Szegedi
 * @version $Id: ModuleScriptProvider.java,v 1.3 2011/04/01 02:39:19 hannes%helma.at Exp $
 */
public interface ModuleScriptProvider
{
    /**
     * Returns a module script. It should attempt to load the module script if 
     * it is not already available to it, or return an already loaded module 
     * script instance if it is available to it. 
     * @param cx current context. Can be used to compile module scripts.
     * @param moduleId the ID of the module. An implementation must only accept
     * an absolute ID, starting with a term. 
     * @param paths the value of the require() function's "paths" attribute. If
     * the require() function is sandboxed, it will be null, otherwise it will
     * be a JavaScript Array object. It is up to the provider implementation
     * whether and how it wants to honor the contents of the array.
     * @return a module script representing the compiled code of the module. 
     * Null should be returned if the script could not found.
     * @throws Exception if there was an unrecoverable problem obtaining the 
     * script
     * @throws IllegalArgumentException if the module ID is syntactically not a
     * valid absolute module identifier.
     */
    public ModuleScript getModuleScript(Context cx, String moduleId, Scriptable paths) 
    throws Exception;
}
