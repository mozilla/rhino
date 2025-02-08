package org.mozilla.javascript.regexp;

import org.mozilla.javascript.RegExpLoader;
import org.mozilla.javascript.RegExpProxy;

/** This class loads the default RegExp implementation. */
public class RegExpLoaderImpl implements RegExpLoader {
    @Override
    public RegExpProxy newProxy() {
        return new RegExpImpl();
    }
}
