package org.mozilla.javascript.xmlimpl;

import org.mozilla.javascript.LazilyLoadedCtor;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.xml.XMLLib;
import org.mozilla.javascript.xml.XMLLoader;

public class XMLLoaderImpl implements XMLLoader {
    @Override
    public void load(ScriptableObject scope, boolean sealed) {
        new LazilyLoadedCtor(scope, "XML", sealed, true, XMLLibImpl::init);
        new LazilyLoadedCtor(scope, "XMLList", sealed, true, XMLLibImpl::init);
        new LazilyLoadedCtor(scope, "Namespace", sealed, true, XMLLibImpl::init);
        new LazilyLoadedCtor(scope, "QName", sealed, true, XMLLibImpl::init);
    }

    @Override
    public XMLLib.Factory getFactory() {
        return XMLLib.Factory.create(XMLLibImpl.class.getName());
    }
}
