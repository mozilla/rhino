package org.mozilla.javascript.commonjs.module;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.UniqueTag;

/**
 * Implements the require() function as defined by 
 * <a href="http://wiki.commonjs.org/wiki/Modules/1.1">Common JS modules</a>.
 * <h1>Thread safety</h1>
 * You will ordinarily create one instance of require() for every top-level
 * scope. This ordinarily means one instance per program execution, except if 
 * you use shared top-level scopes and installing most objects into them. 
 * Module loading is thread safe, so using a single require() in a shared 
 * top-level scope is also safe.
 * <h1>Creation</h1>
 * If you need to create many otherwise identical require() functions for 
 * different scopes, you might want to use {@link RequireBuilder} for 
 * convenience.
 * <h1>Making it available</h1>
 * In order to make the require() function available to your JavaScript 
 * program, you need to invoke either {@link #install(Scriptable)} or 
 * {@link #requireMain(Context, String)}.
 * @author Attila Szegedi
 * @version $Id: Require.java,v 1.3 2011/04/01 02:39:19 hannes%helma.at Exp $
 */
public class Require extends ScriptableObject implements Function
{
    private static final long serialVersionUID = 1L;

    private final ModuleScriptProvider moduleScriptProvider;
    private final Scriptable nativeScope;
    private final Scriptable paths;
    private final boolean sandboxed;
    private final Script preExec;
    private final Script postExec;
    private String mainModuleId = null;

    // Modules that completed loading; visible to all threads
    private final Map<String, Scriptable> exportedModuleInterfaces = 
        new ConcurrentHashMap<String, Scriptable>();
    private final Object loadLock = new Object();
    // Modules currently being loaded on the thread. Used to resolve circular
    // dependencies while loading.
    private static final ThreadLocal<Map<String, Scriptable>> 
        loadingModuleInterfaces = new ThreadLocal<Map<String,Scriptable>>();

    /**
     * Creates a new instance of the require() function. Upon constructing it,
     * you will either want to install it in the global (or some other) scope 
     * using {@link #install(Scriptable)}, or alternatively, you can load the
     * program's main module using {@link #requireMain(Context, String)} and 
     * then act on the main module's exports.
     * @param cx the current context
     * @param nativeScope a scope that provides the standard native JavaScript 
     * objects.
     * @param moduleScriptProvider a provider for module scripts
     * @param preExec an optional script that is executed in every module's 
     * scope before its module script is run.
     * @param postExec an optional script that is executed in every module's 
     * scope after its module script is run.
     * @param sandboxed if set to true, the require function will be sandboxed. 
     * This means that it doesn't have the "paths" property, and also that the 
     * modules it loads don't export the "module.uri" property.  
     */
    public Require(Context cx, Scriptable nativeScope, 
            ModuleScriptProvider moduleScriptProvider, Script preExec, 
            Script postExec, boolean sandboxed) {
        this.moduleScriptProvider = moduleScriptProvider;
        this.nativeScope = nativeScope;
        this.sandboxed = sandboxed;
        this.preExec = preExec;
        this.postExec = postExec;
        setPrototype(ScriptableObject.getFunctionPrototype(nativeScope));
        if(!sandboxed) {
            paths = cx.newArray(nativeScope, 0);
            defineReadOnlyProperty(this, "paths", paths);
        }
        else {
            paths = null;
        }
    }
    
    /**
     * Calling this method establishes a module as being the main module of the
     * program to which this require() instance belongs. The module will be
     * loaded as if require()'d and its "module" property will be set as the
     * "main" property of this require() instance. You have to call this method
     * before the module has been loaded (that is, the call to this method must
     * be the first to require the module and thus trigger its loading). Note
     * that the main module will execute in its own scope and not in the global
     * scope. Since all other modules see the global scope, executing the main
     * module in the global scope would open it for tampering by other modules.
     * @param cx the current context 
     * @param mainModuleId the ID of the main module
     * @return the "exports" property of the main module
     * @throws IllegalStateException if the main module is already loaded when
     * required, or if this require() instance already has a different main 
     * module set. 
     */
    public Scriptable requireMain(Context cx, String mainModuleId) {
        if(this.mainModuleId != null) {
            if(this.mainModuleId.equals(mainModuleId)) {
                return getExportedModuleInterface(cx, mainModuleId, false);
            }
            throw new IllegalStateException("main module already set to " + 
                    this.mainModuleId);
        }
        final Scriptable mainExports = getExportedModuleInterface(cx, 
                mainModuleId, true);
        this.mainModuleId = mainModuleId;
        return mainExports;
    }

    /**
     * Binds this instance of require() into the specified scope under the 
     * property name "require".
     * @param scope the scope where the require() function is to be installed.
     */
    public void install(Scriptable scope) {
        ScriptableObject.putProperty(scope, "require", this);
    }

    public Object call(Context cx, Scriptable scope, Scriptable thisObj,
            Object[] args)
    {
        if(args == null || args.length < 1) {
            throw ScriptRuntime.throwError(cx, scope, 
                    "require() needs one argument");
        }
        final String id = (String)Context.jsToJava(args[0], String.class);
        final String absoluteId = getAbsoluteId(cx, thisObj, id);
        return getExportedModuleInterface(cx, absoluteId, false);
    }

    private String getAbsoluteId(Context cx, Scriptable scope, String id)
    {
        if(id.startsWith("./") || id.startsWith("../")) {
            final String moduleId = getModuleId(scope);
            if(moduleId == null) {
                throw ScriptRuntime.throwError(cx, scope, 
                        "Can't resolve relative module ID " + id + 
                        " when require() is used outside of a module");
            }
            return resolveRelativeId(getParentDirectory(moduleId), id);
        }
        return id;
    }

    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        throw ScriptRuntime.throwError(cx, scope, 
                "require() can not be invoked as a constructor");
    }

    private static String resolveRelativeId(String directory, String id) {
        if(id.startsWith("./")) {
            return resolveRelativeId(directory, id.substring(2));
        }
        else if(id.startsWith("../")) {
            return resolveRelativeId(getParentDirectory(directory), 
                    id.substring(3));
        }
        else {
            return "".equals(directory) ? id : (directory + "/" + id);
        }
    }
    
    private static String getModuleId(Scriptable scope) {
        final Object module = ScriptableObject.getProperty(scope, "module");
        if(!(module instanceof Scriptable)) {
            return null;
        }
        final Object id = ScriptableObject.getProperty((Scriptable)module, 
                "id");
        if(id == UniqueTag.NOT_FOUND || id == null) {
            return null;
        }
        return String.valueOf(Context.jsToJava(id, String.class));
    }
    
    private static String getParentDirectory(String path) {
        final int i = path.lastIndexOf('/');
        return i == -1 ? "" : path.substring(0, i);
    }
    
    private Scriptable getExportedModuleInterface(Context cx, String id, 
            boolean isMain) 
    {
        // Check if the requested module is already completely loaded
        Scriptable exports = exportedModuleInterfaces.get(id);
        if(exports != null) {
            if(isMain) {
                throw new IllegalStateException(
                        "Attempt to set main module after it was loaded");
            }
            return exports;
        }
        // Check if it is currently being loaded on the current thread 
        // (supporting circular dependencies).
        Map<String, Scriptable> threadLoadingModules = 
            loadingModuleInterfaces.get();
        if(threadLoadingModules != null) {
            exports = threadLoadingModules.get(id);
            if(exports != null) {
                return exports;
            }
        }
        // The requested module is neither already loaded, nor is it being 
        // loaded on the current thread. End of fast path. We must synchronize 
        // now, as we have to guarantee that at most one thread can load 
        // modules at any one time. Otherwise, two threads could end up 
        // attempting to load two circularly dependent modules in opposite 
        // order, which would lead to either unacceptable non-determinism or 
        // deadlock, depending on whether we underprotected or overprotected it
        // with locks.
        synchronized(loadLock) {
            // Recheck if it is already loaded - other thread might've 
            // completed loading it just as we entered the synchronized block.
            exports = exportedModuleInterfaces.get(id);
            if(exports != null) {
                return exports;
            }
            // Nope, still not loaded; we're loading it then.
            final ModuleScript moduleScript = getModule(cx, id);
            exports = cx.newObject(nativeScope);
            // Are we the outermost locked invocation on this thread?
            final boolean outermostLocked = threadLoadingModules == null;
            if(outermostLocked) {
                threadLoadingModules = new HashMap<String, Scriptable>();
                loadingModuleInterfaces.set(threadLoadingModules);
            }
            // Must make the module exports available immediately on the 
            // current thread, to satisfy the CommonJS Modules/1.1 requirement 
            // that "If there is a dependency cycle, the foreign module may not
            // have finished executing at the time it is required by one of its
            // transitive dependencies; in this case, the object returned by 
            // "require" must contain at least the exports that the foreign 
            // module has prepared before the call to require that led to the 
            // current module's execution."
            threadLoadingModules.put(id, exports);
            try {
                executeModuleScript(cx, id, exports, moduleScript, isMain);
            }
            catch(RuntimeException e) {
                // Throw loaded module away if there was an exception
                threadLoadingModules.remove(id);
                throw e;
            }
            finally {
                if(outermostLocked) {
                    // Make loaded modules visible to other threads only after 
                    // the topmost triggering load has completed. This strategy
                    // (compared to the one where we'd make each module 
                    // globally available as soon as it loads) prevents other
                    // threads from observing a partially loaded circular
                    // dependency of a module that completed loading.
                    exportedModuleInterfaces.putAll(threadLoadingModules);
                    loadingModuleInterfaces.set(null);
                }
            }
        }
        return exports;
    }

    private void executeModuleScript(Context cx, String id, 
            Scriptable exports, ModuleScript moduleScript, boolean isMain)
    {
        final ScriptableObject moduleObject = (ScriptableObject)cx.newObject(
                nativeScope);
        defineReadOnlyProperty(moduleObject, "id", id);
        if(!sandboxed) {
            final String uri = moduleScript.getUri();
            if(uri != null) {
                defineReadOnlyProperty(moduleObject, "uri", uri);
            }
        }
        final Scriptable executionScope = cx.newObject(nativeScope);
        // Set this so it can access the global JS environment objects. 
        // This means we're currently using the "MGN" approach (ModuleScript 
        // with Global Natives) as specified here: 
        // <http://wiki.commonjs.org/wiki/Modules/ProposalForNativeExtension>
        ScriptableObject.putProperty(executionScope, "exports", exports);
        ScriptableObject.putProperty(executionScope, "module", moduleObject);
        install(executionScope);
        executionScope.setPrototype(nativeScope);
        if(isMain) {
            defineReadOnlyProperty(this, "main", moduleObject);
        }
        executeOptionalScript(preExec, cx, executionScope);
        moduleScript.getScript().exec(cx, executionScope);
        executeOptionalScript(postExec, cx, executionScope);
    }
    
    private static void executeOptionalScript(Script script, Context cx, 
            Scriptable executionScope)
    {
        if(script != null) {
            script.exec(cx, executionScope);
        }
    }

    private static void defineReadOnlyProperty(ScriptableObject obj, 
            String name, Object value) {
        ScriptableObject.putProperty(obj, name, value);
        obj.setAttributes(name, ScriptableObject.READONLY | 
                ScriptableObject.PERMANENT);
    }
    
    private ModuleScript getModule(Context cx, String id) {
        try {
            final ModuleScript moduleScript = 
                moduleScriptProvider.getModuleScript(cx, id, paths);
            if(moduleScript == null) {
                throw ScriptRuntime.throwError(cx, nativeScope, "Module " + id 
                        + " not found."); 
            }
            return moduleScript;
        }
        catch(RuntimeException e) {
            throw e;
        }
        catch(Exception e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
    }
    
    @Override
    public String getClassName() {
        return "Function";
    }
}