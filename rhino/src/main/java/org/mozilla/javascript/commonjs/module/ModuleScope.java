/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.commonjs.module;

import java.net.URI;
import org.mozilla.javascript.DeclarationScope;
import org.mozilla.javascript.ScopeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.TopLevel;

/**
 * A top-level module scope. This class provides methods to retrieve the module's source and base
 * URIs in order to resolve relative module IDs and check sandbox constraints.
 */
public class ModuleScope extends DeclarationScope {
    private static final long serialVersionUID = 1L;
    private final URI uri;
    private final URI base;

    private ModuleScope(ScopeObject scope, URI uri, URI base) {
        super(scope);
        this.uri = uri;
        this.base = base;
    }

    public static ScopeObject createModuleScope(TopLevel global, URI uri, URI base) {
        var moduleScope = new ModuleScope(global, uri, base);
        return moduleScope;
    }

    public URI getUri() {
        return uri;
    }

    public URI getBase() {
        return base;
    }

    @Override
    public String getClassName() {
        return "module";
    }

    /** Search up the chain of scopes to find a module scope. */
    public static ModuleScope findModuleScope(Scriptable scope) {
        Scriptable current = scope;
        while (current != null) {
            if (current instanceof ModuleScope) {
                return (ModuleScope) current;
            }
            current = current.getParentScope();
        }
        return null;
    }
}
