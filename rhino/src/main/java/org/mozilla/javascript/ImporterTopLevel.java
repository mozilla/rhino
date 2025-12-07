/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package org.mozilla.javascript;

import java.util.ArrayList;

/**
 * Class ImporterTopLevel
 *
 * <p>This class defines a ScriptableObject that can be instantiated as a top-level ("global")
 * object to provide functionality similar to Java's "import" statement.
 *
 * <p>This class can be used to create a top-level scope using the following code:
 *
 * <pre>
 *  Scriptable scope = new ImporterTopLevel(cx);
 * </pre>
 *
 * Then JavaScript code will have access to the following methods:
 *
 * <ul>
 *   <li>importClass - will "import" a class by making its unqualified name available as a property
 *       of the top-level scope
 *   <li>importPackage - will "import" all the classes of the package by searching for unqualified
 *       names as classes qualified by the given package.
 * </ul>
 *
 * The following code from the shell illustrates this use:
 *
 * <pre>
 * js&gt; importClass(java.io.File)
 * js&gt; f = new File('help.txt')
 * help.txt
 * js&gt; importPackage(java.util)
 * js&gt; v = new Vector()
 * []
 * </pre>
 *
 * @author Norris Boyd
 */
public class ImporterTopLevel extends TopLevel {
    private static final long serialVersionUID = -9095380847465315412L;

    public ImporterTopLevel() {}

    public ImporterTopLevel(Context cx) {
        this(cx, false);
    }

    public ImporterTopLevel(Context cx, boolean sealed) {
        initStandardObjects(cx, sealed);
    }

    @Override
    public String getClassName() {
        return topScopeFlag ? "global" : "JavaImporter";
    }

    public static Object init(Context cx, Scriptable scope, boolean sealed) {
        return init(cx, scope, sealed, false);
    }

    public static Object init(Context cx, Scriptable scope, boolean sealed, boolean isTopScope) {
        LambdaConstructor ctor =
                new LambdaConstructor(scope, "ImporterTopLevel", 0, ImporterTopLevel::js_construct);

        ctor.definePrototypeMethod(scope, "importClass", 1, ImporterTopLevel::js_importClass);
        ctor.definePrototypeMethod(scope, "importPackage", 1, ImporterTopLevel::js_importPackage);

        if (sealed) {
            ctor.sealObject();
        }

        var proto = (Scriptable) ctor.getPrototypeProperty();

        if (isTopScope) {
            scope.put("importClass", scope, proto.get("importClass", proto));
            scope.put("importPackage", scope, proto.get("importPackage", proto));
        }

        return ctor;
    }

    public void initStandardObjects(Context cx, boolean sealed) {
        // Assume that Context.initStandardObjects initialize JavaImporter
        // property lazily so the above init call is not yet called
        cx.initStandardObjects(this, sealed);
        topScopeFlag = true;
        // If seal is true then exportAsJSClass(cx, seal) would seal
        // this obj. Since this is scope as well, it would not allow
        // to add variables.

        var ctor = init(cx, this, sealed, true);
        ScriptableObject.defineProperty(this, "JavaImporter", ctor, DONTENUM);

        // delete "constructor" defined by exportAsJSClass so "constructor"
        // name would refer to Object.constructor
        // and not to JavaImporter.prototype.constructor.
        delete("constructor");
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return super.has(name, start) || getPackageProperty(name, start) != NOT_FOUND;
    }

    @Override
    public Object get(String name, Scriptable start) {
        Object result = super.get(name, start);
        if (result != NOT_FOUND) return result;
        result = getPackageProperty(name, start);
        return result;
    }

    private Object getPackageProperty(String name, Scriptable start) {
        Object result = NOT_FOUND;
        Scriptable scope = start;
        if (topScopeFlag) {
            scope = ScriptableObject.getTopLevelScope(scope);
        }
        Object[] elements = getNativeJavaPackages(scope);
        if (elements == null) {
            return result;
        }
        for (Object element : elements) {
            NativeJavaPackage p = (NativeJavaPackage) element;
            Object v = p.getPkgProperty(name, start, false);
            if (v != null && !(v instanceof NativeJavaPackage)) {
                if (result == NOT_FOUND) {
                    result = v;
                } else {
                    throw Context.reportRuntimeErrorById(
                            "msg.ambig.import", result.toString(), v.toString());
                }
            }
        }

        return result;
    }

    private static Object[] getNativeJavaPackages(Scriptable scope) {
        // retrivee the native java packages stored in top scope.
        synchronized (scope) {
            if (scope instanceof ScriptableObject) {
                ScriptableObject so = (ScriptableObject) scope;
                @SuppressWarnings("unchecked")
                ArrayList<Object> importedPackages =
                        (ArrayList<Object>) so.getAssociatedValue(AKEY);
                if (importedPackages != null) {
                    return importedPackages.toArray();
                }
            }
        }
        return null;
    }

    /**
     * @deprecated Kept only for compatibility.
     */
    @Deprecated
    public void importPackage(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        js_importPackage(cx, funObj.getDeclarationScope(), this, args);
    }

    private static Scriptable js_construct(Context cx, Scriptable scope, Object[] args) {
        ImporterTopLevel result = new ImporterTopLevel();
        for (int i = 0; i != args.length; ++i) {
            Object arg = args[i];
            if (arg instanceof NativeJavaClass) {
                ImporterTopLevel.importClass(result, (NativeJavaClass) arg);
            } else if (arg instanceof NativeJavaPackage) {
                ImporterTopLevel.importPackage(result, (NativeJavaPackage) arg);
            } else {
                throw Context.reportRuntimeErrorById(
                        "msg.not.class.not.pkg", Context.toString(arg));
            }
        }
        // set explicitly prototype and scope
        // as otherwise in top scope mode BaseFunction.construct
        // would keep them set to null. It also allows to use
        // JavaImporter without new and still get properly
        // initialized object.
        result.setParentScope(scope);
        return result;
    }

    private static Object js_importClass(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        for (int i = 0; i != args.length; i++) {
            Object arg = args[i];
            if (!(arg instanceof NativeJavaClass)) {
                throw Context.reportRuntimeErrorById("msg.not.class", Context.toString(arg));
            }
            importClass((ScriptableObject) thisObj, (NativeJavaClass) arg);
        }
        return Undefined.instance;
    }

    private static Object js_importPackage(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        for (int i = 0; i != args.length; i++) {
            Object arg = args[i];
            if (!(arg instanceof NativeJavaPackage)) {
                throw Context.reportRuntimeErrorById("msg.not.pkg", Context.toString(arg));
            }
            importPackage((ScriptableObject) thisObj, (NativeJavaPackage) arg);
        }
        return Undefined.instance;
    }

    private static void importPackage(ScriptableObject scope, NativeJavaPackage pkg) {
        if (pkg == null) {
            return;
        }
        synchronized (scope) {
            @SuppressWarnings("unchecked")
            ArrayList<Object> importedPackages = (ArrayList<Object>) scope.getAssociatedValue(AKEY);
            if (importedPackages == null) {
                importedPackages = new ArrayList<>();
                scope.associateValue(AKEY, importedPackages);
            }
            for (int j = 0; j != importedPackages.size(); j++) {
                if (pkg.equals(importedPackages.get(j))) {
                    return;
                }
            }
            importedPackages.add(pkg);
        }
    }

    private static void importClass(Scriptable scope, NativeJavaClass cl) {
        String s = cl.getClassObject().getName();
        String n = s.substring(s.lastIndexOf('.') + 1);
        Object val = scope.get(n, scope);
        if (val != NOT_FOUND) {
            if (val.equals(cl)) {
                return; // do not redefine same class
            }
            throw Context.reportRuntimeErrorById("msg.prop.defined", n);
        }
        // defineProperty(n, cl, DONTENUM);
        scope.put(n, scope, cl);
    }

    private static final String AKEY = "importedPackages";
    private boolean topScopeFlag;
}
