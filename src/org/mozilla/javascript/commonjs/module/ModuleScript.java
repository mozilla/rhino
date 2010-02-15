package org.mozilla.javascript.commonjs.module;

import java.io.Serializable;

import org.mozilla.javascript.Script;
import org.mozilla.javascript.commonjs.module.ModuleScript;

/**
 * Represents a compiled CommonJS module script. The {@link Require} functions 
 * use them and obtain them through a {@link ModuleScriptProvider}. Instances
 * are immutable.
 * @author Attila Szegedi
 * @version $Id: ModuleScript.java,v 1.1 2010/02/15 19:31:14 szegedia%freemail.hu Exp $
 */
public class ModuleScript implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    private final Script script;
    private final String uri;

    /**
     * Creates a new CommonJS module.
     * @param script the script representing the code of the module.
     * @param uri the URI of the module.
     */
    public ModuleScript(Script script, String uri) {
        this.script = script;
        this.uri = uri;
    }
    
    /**
     * Returns the script object representing the code of the module.
     * @return the script object representing the code of the module.
     */
    public Script getScript(){
        return script;
    }

    /**
     * Returns the URI of the module.
     * @return the URI of the module.
     */
    public String getUri() {
        return uri;
    }
}
