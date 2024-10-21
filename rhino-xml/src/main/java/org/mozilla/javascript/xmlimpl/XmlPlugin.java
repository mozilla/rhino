package org.mozilla.javascript.xmlimpl;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LazilyLoadedCtor;
import org.mozilla.javascript.Plugin;
import org.mozilla.javascript.ScriptableObject;

/**
 * Registers the XML objects in the scope.
 *
 * @author Roland Praml, Foconis Analytics GmbH
 */
public class XmlPlugin implements Plugin {

    @Override
    public void initSafeStandardObjects(Context cx, ScriptableObject scope, boolean sealed) {
        if (cx.hasFeature(Context.FEATURE_E4X)) {
            String xmlImpl = XMLLibImpl.class.getName();
            new LazilyLoadedCtor(scope, "XML", xmlImpl, sealed, true);
            new LazilyLoadedCtor(scope, "XMLList", xmlImpl, sealed, true);
            new LazilyLoadedCtor(scope, "Namespace", xmlImpl, sealed, true);
            new LazilyLoadedCtor(scope, "QName", xmlImpl, sealed, true);
        }
    }

    @Override
    public void initCompilerEnvirons(Context cx, CompilerEnvirons compilerEnvirons) {
        if (cx.hasFeature(Context.FEATURE_E4X)) {
            compilerEnvirons.setXmlAvailable(true);
        }
    }
}
