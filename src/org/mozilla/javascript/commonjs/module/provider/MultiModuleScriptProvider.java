package org.mozilla.javascript.commonjs.module.provider;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.commonjs.module.ModuleScript;
import org.mozilla.javascript.commonjs.module.ModuleScriptProvider;

/**
 * A multiplexer for module script providers.
 * @author Attila Szegedi
 * @version $Id: MultiModuleScriptProvider.java,v 1.2 2010/02/19 09:47:19 szegedia%freemail.hu Exp $
 */
public class MultiModuleScriptProvider implements ModuleScriptProvider
{
    private final ModuleScriptProvider[] providers;
    
    /**
     * Creates a new multiplexing module script provider tht gathers the 
     * specified providers
     * @param providers the providers to multiplex.
     */
    public MultiModuleScriptProvider(Iterable<? extends ModuleScriptProvider> providers) {
        final List<ModuleScriptProvider> l = new LinkedList<ModuleScriptProvider>();
        for (ModuleScriptProvider provider : providers) {
            l.add(provider);
        }
        this.providers = l.toArray(new ModuleScriptProvider[l.size()]);
    }
    
    public ModuleScript getModuleScript(Context cx, String moduleId, Scriptable paths) throws Exception {
        for (ModuleScriptProvider provider : providers) {
            final ModuleScript script = provider.getModuleScript(cx, moduleId, paths);
            if(script != null) {
                return script;
            }
        }
        return null;
    }
}
