package org.mozilla.javascript.commonjs.module.provider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.commonjs.module.ModuleScript;

/**
 * A module script provider that uses a module source provider to load modules
 * and caches the loaded modules. It softly references the loaded modules' 
 * Rhino {@link Script} objects, thus a module once loaded can become eligible
 * for garbage collection if it is otherwise unused under memory pressure.
 * Instances of this class are thread safe. 
 * @author Attila Szegedi
 * @version $Id: SoftCachingModuleScriptProvider.java,v 1.1 2010/02/15 19:31:12 szegedia%freemail.hu Exp $
 */
public class SoftCachingModuleScriptProvider extends CachingModuleScriptProviderBase 
{
    private static final long serialVersionUID = 1L;

    private transient ReferenceQueue<Script> scriptRefQueue = 
        new ReferenceQueue<Script>();
    
    private transient ConcurrentMap<String, ScriptReference> scripts = 
        new ConcurrentHashMap<String, ScriptReference>(16, .75f, 
                getConcurrencyLevel()); 

    /**
     * Creates a new module provider with the specified module source provider.
     * @param moduleSourceProvider provider for modules' source code
     */
    public SoftCachingModuleScriptProvider(
            ModuleSourceProvider moduleSourceProvider)
    {
        super(moduleSourceProvider);
    }
    
    @Override
    public ModuleScript getModuleScript(Context cx, String moduleId,
            Scriptable paths) throws FileNotFoundException, IOException
    {
        // Overridden to clear the reference queue before retrieving the 
        // script.
        for(;;) {
            ScriptReference ref = (ScriptReference)scriptRefQueue.poll();
            if(ref == null) {
                break;
            }
            scripts.remove(ref.getModuleId(), ref);
        }
        return super.getModuleScript(cx, moduleId, paths);
    }
    
    @Override
    protected CachedModuleScript getLoadedModule(String moduleId) {
        final ScriptReference scriptRef = scripts.get(moduleId);
        return scriptRef != null ? scriptRef.getCachedModuleScript() : null;
    }
    
    @Override
    protected void putLoadedModule(String moduleId, ModuleScript moduleScript,
            Object validator)
    {
        scripts.put(moduleId, new ScriptReference(moduleScript.getScript(), 
                moduleId, moduleScript.getUri(), validator, scriptRefQueue));
    }
    
    private static class ScriptReference extends SoftReference<Script> {
        private final String moduleId;
        private final String uri;
        private final Object validator;
        
        ScriptReference(Script script, String moduleId, String uri, 
                Object validator, ReferenceQueue<Script> refQueue) {
            super(script, refQueue);
            this.moduleId = moduleId;
            this.uri = uri;
            this.validator = validator;
        }
        
        CachedModuleScript getCachedModuleScript() {
            final Script script = get();
            if(script == null) {
                return null;
            }
            return new CachedModuleScript(new ModuleScript(script, uri), 
                    validator); 
        }
        
        String getModuleId() {
            return moduleId;
        }
    }
    
    private void readObject(ObjectInputStream in) throws IOException, 
    ClassNotFoundException 
    {
        scriptRefQueue = new ReferenceQueue<Script>();
        scripts = new ConcurrentHashMap<String, ScriptReference>();
        final Map<String, CachedModuleScript> serScripts = (Map)in.readObject();
        for(Map.Entry<String, CachedModuleScript> entry: serScripts.entrySet()) {
            final CachedModuleScript cachedModuleScript = entry.getValue();
            putLoadedModule(entry.getKey(), cachedModuleScript.getModule(), 
                    cachedModuleScript.getValidator());
        }
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        final Map<String, CachedModuleScript> serScripts = 
            new HashMap<String, CachedModuleScript>();
        for(Map.Entry<String, ScriptReference> entry: scripts.entrySet()) {
            final CachedModuleScript cachedModuleScript = 
                entry.getValue().getCachedModuleScript();
            if(cachedModuleScript != null) {
                serScripts.put(entry.getKey(), cachedModuleScript);
            }
        }
        out.writeObject(serScripts);
    }
}