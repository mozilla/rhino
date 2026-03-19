package org.mozilla.javascript.xmlimpl;

import org.mozilla.javascript.LazilyLoadedCtor;
import org.mozilla.javascript.ScopeObject;
import org.mozilla.javascript.xml.XMLLib;
import org.mozilla.javascript.xml.XMLLoader;

public class XMLLoaderImpl implements XMLLoader {
    @Override
    public void load(ScopeObject scope, boolean sealed) {
        String implClass = XMLLibImpl.class.getName();
        new LazilyLoadedCtor<>(scope, "XML", implClass, sealed, true);
        new LazilyLoadedCtor<>(scope, "XMLList", implClass, sealed, true);
        new LazilyLoadedCtor<>(scope, "Namespace", implClass, sealed, true);
        new LazilyLoadedCtor<>(scope, "QName", implClass, sealed, true);
    }

    @Override
    public XMLLib.Factory getFactory() {
        return XMLLib.Factory.create(XMLLibImpl.class.getName());
    }
}
