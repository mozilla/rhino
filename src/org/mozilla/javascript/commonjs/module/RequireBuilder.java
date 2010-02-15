package org.mozilla.javascript.commonjs.module;

import java.io.Serializable;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

/**
 * A builder for {@link Require} instances. Useful when you're creating many
 * instances of {@link Require} that are identical except for their top-level
 * scope and current {@link Context}. Also useful if you prefer configuring it
 * using named setters instead of passing many parameters in a constructor.
 * @author Attila Szegedi
 * @version $Id: RequireBuilder.java,v 1.1 2010/02/15 19:31:14 szegedia%freemail.hu Exp $
 */
public class RequireBuilder implements Serializable
{
    private static final long serialVersionUID = 1L;

    private boolean sandboxed = true;
    private ModuleScriptProvider moduleScriptProvider;
    private Script preExec;
    private Script postExec;
    
    /**
     * Sets the {@link ModuleScriptProvider} for the {@link Require} instances
     * that this builder builds.
     * @param moduleScriptProvider the module script provider for the 
     * {@link Require} instances that this builder builds. 
     */
    public void setModuleScriptProvider(
            ModuleScriptProvider moduleScriptProvider)
    {
        this.moduleScriptProvider = moduleScriptProvider;
    }
    
    /**
     * Sets the script that should execute in every module's scope after the
     * module's own script has executed.
     * @param postExec the post-exec script.
     */
    public void setPostExec(Script postExec) {
        this.postExec = postExec;
    }

    /**
     * Sets the script that should execute in every module's scope before the
     * module's own script has executed.
     * @param preExec the pre-exec script.
     */
    public void setPreExec(Script preExec) {
        this.preExec = preExec;
    }
    
    /**
     * Sets whether the created require() instances will be sandboxed. 
     * See {@link Require#Require(Context, Scriptable, ModuleScriptProvider,
     * Script, Script, boolean)} for explanation.
     * @param sandboxed true if the created require() instances will be 
     * sandboxed.
     */
    public void setSandboxed(boolean sandboxed) {
        this.sandboxed = sandboxed;
    }
 
    /**
     * Creates a new require() function. You are still responsible for invoking
     * either {@link Require#install(Scriptable)} or 
     * {@link Require#requireMain(Context, String)} to effectively make it 
     * available to its JavaScript program. 
     * @param cx the current context
     * @param globalScope the global scope containing the JS standard natives.
     * @return a new Require instance.
     */
    public Require createRequire(Context cx, Scriptable globalScope) {
        return new Require(cx, globalScope, moduleScriptProvider, preExec, 
                postExec, sandboxed);
    }
}