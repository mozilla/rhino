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

    private static final Object IMPORTER_TAG = "Importer";

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

    public static void init(Context cx, Scriptable scope, boolean sealed) {
        ImporterTopLevel obj = new ImporterTopLevel();
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    public void initStandardObjects(Context cx, boolean sealed) {
        // Assume that Context.initStandardObjects initialize JavaImporter
        // property lazily so the above init call is not yet called
        cx.initStandardObjects(this, sealed);
        topScopeFlag = true;
        // If seal is true then exportAsJSClass(cx, seal) would seal
        // this obj. Since this is scope as well, it would not allow
        // to add variables.
        IdFunctionObject ctor = exportAsJSClass(MAX_PROTOTYPE_ID, this, false);
        if (sealed) {
            ctor.sealObject();
        }
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
        js_importPackage(this, args);
    }

    private Object js_construct(Scriptable scope, Object[] args) {
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
        // would keep them set to null. It also allow to use
        // JavaImporter without new and still get properly
        // initialized object.
        result.setParentScope(scope);
        result.setPrototype(this);
        return result;
    }

    private static Object js_importClass(Scriptable scope, Object[] args) {
        for (int i = 0; i != args.length; i++) {
            Object arg = args[i];
            if (!(arg instanceof NativeJavaClass)) {
                throw Context.reportRuntimeErrorById("msg.not.class", Context.toString(arg));
            }
            importClass(scope, (NativeJavaClass) arg);
        }
        return Undefined.instance;
    }

    private static Object js_importPackage(ScriptableObject scope, Object[] args) {
        for (int i = 0; i != args.length; i++) {
            Object arg = args[i];
            if (!(arg instanceof NativeJavaPackage)) {
                throw Context.reportRuntimeErrorById("msg.not.pkg", Context.toString(arg));
            }
            importPackage(scope, (NativeJavaPackage) arg);
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

    @Override
    protected void initPrototypeId(int id) {
        String s;
        int arity;
        switch (id) {
            case Id_constructor:
                arity = 0;
                s = "constructor";
                break;
            case Id_importClass:
                arity = 1;
                s = "importClass";
                break;
            case Id_importPackage:
                arity = 1;
                s = "importPackage";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(IMPORTER_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(IMPORTER_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
            case Id_constructor:
                return js_construct(scope, args);

            case Id_importClass:
                return js_importClass(realScope(scope, thisObj, f), args);

            case Id_importPackage:
                return js_importPackage(realScope(scope, thisObj, f), args);
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    private ScriptableObject realScope(Scriptable scope, Scriptable thisObj, IdFunctionObject f) {
        if (topScopeFlag) {
            // when used as top scope importPackage and importClass are global
            // function that ignore thisObj. We use the the top level scope
            // which might not be the same as 'this' when used shared scopes
            thisObj = ScriptableObject.getTopLevelScope(scope);
        }
        return ensureType(thisObj, ScriptableObject.class, f);
    }

    @Override
    protected int findPrototypeId(String s) {
        int id;
        switch (s) {
            case "constructor":
                id = Id_constructor;
                break;
            case "importClass":
                id = Id_importClass;
                break;
            case "importPackage":
                id = Id_importPackage;
                break;
            default:
                id = 0;
                break;
        }
        return id;
    }

    private static final int Id_constructor = 1,
            Id_importClass = 2,
            Id_importPackage = 3,
            MAX_PROTOTYPE_ID = 3;

    private static final String AKEY = "importedPackages";
    private boolean topScopeFlag;
}
