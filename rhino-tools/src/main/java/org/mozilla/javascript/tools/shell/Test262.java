/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tools.shell;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScopeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.typedarrays.NativeArrayBuffer;

/**
 * Implements the $262 object required by the Test262 ECMAScript conformance test suite.
 *
 * <p>This class provides the host-defined functions specified by Test262 for testing ECMAScript
 * implementations. It supports creating new realms, evaluating scripts, and accessing global
 * objects.
 *
 * @see <a href="https://github.com/tc39/test262/blob/main/INTERPRETING.md#host-defined-functions">
 *     Test262 Host-Defined Functions</a>
 */
public class Test262 extends ScriptableObject {

    /** Enum to control how realms are initialized. */
    public enum RealmMode {
        /** Uses initSafeStandardObjects - restricts Java access for sandboxing (used in tests) */
        SAFE,
        /** Uses initStandardObjects - full access to Java integration (used in shell) */
        STANDARD
    }

    private RealmMode realmMode;

    public Test262() {
        super();
    }

    Test262(VarScope scope, Scriptable prototype, RealmMode mode) {
        super(scope, prototype);
        this.realmMode = mode;
    }

    /**
     * Initialize the $262 prototype object with all Test262 host-defined functions.
     *
     * @param cx the current Context
     * @param scope the scope to install the prototype in
     * @param mode the realm mode (SAFE for tests, STANDARD for shell)
     * @return the initialized $262 prototype
     */
    public static Test262 init(Context cx, VarScope scope, RealmMode mode) {
        Test262 proto = new Test262();
        proto.realmMode = mode;
        proto.setPrototype(getObjectPrototype(scope));
        proto.setParentScope(scope);

        proto.defineProperty(scope, "gc", 0, Test262::gc);
        proto.defineProperty(scope, "createRealm", 0, Test262::createRealm);
        proto.defineProperty(scope, "evalScript", 1, Test262::evalScript);
        proto.defineProperty(scope, "detachArrayBuffer", 0, Test262::detachArrayBuffer);

        proto.defineProperty(cx, scope, "global", Test262::getGlobal, null, DONTENUM | READONLY);
        proto.defineProperty(cx, scope, "agent", Test262::getAgent, null, DONTENUM | READONLY);

        proto.defineProperty(SymbolKey.TO_STRING_TAG, "__262__", DONTENUM | READONLY);

        ScriptableObject.defineProperty(scope, "__262__", proto, DONTENUM);
        return proto;
    }

    /**
     * Install a $262 instance into a scope.
     *
     * @param scope the scope to install into
     * @param parentScope the parent scope for the $262 instance
     * @param mode the realm mode
     * @return the installed $262 instance
     */
    public static Test262 install(ScopeObject scope, Scriptable parentScope, RealmMode mode) {
        Test262 instance = new Test262(scope, parentScope, mode);

        scope.put("$262", scope, instance);
        scope.setAttributes("$262", ScriptableObject.DONTENUM);

        return instance;
    }

    private static Object gc(Context cx, VarScope scope, Object thisObj, Object[] args) {
        System.gc();
        return Undefined.instance;
    }

    public static Object evalScript(Context cx, VarScope scope, Object thisObj, Object[] args) {
        if (args.length == 0) {
            throw ScriptRuntime.throwError(cx, scope, "not enough args");
        }
        String source = Context.toString(args[0]);
        return cx.evaluateString(scope, source, "<evalScript>", 1, null);
    }

    public static Object getGlobal(Scriptable scriptable) {
        return ((TopLevel) scriptable.getParentScope()).getGlobalThis();
    }

    public static Test262 createRealm(Context cx, VarScope scope, Object thisObj, Object[] args) {
        // Get the realm mode from the parent $262 instance
        Test262 parent = (Test262) ScriptRuntime.toObject(scope, thisObj);
        RealmMode mode = parent.realmMode;

        // Create realm based on mode
        TopLevel realm;
        if (mode == RealmMode.SAFE) {
            realm = cx.initSafeStandardObjects(new TopLevel());
        } else {
            realm = cx.initStandardObjects(new TopLevel());
        }

        return install(realm, ScriptRuntime.toObject(realm, thisObj).getPrototype(), mode);
    }

    public static Object detachArrayBuffer(
            Context cx, VarScope scope, Object thisObj, Object[] args) {
        Scriptable buf = ScriptRuntime.toObject(scope, args[0]);
        if (buf instanceof NativeArrayBuffer) {
            ((NativeArrayBuffer) buf).detach();
        }
        return Undefined.instance;
    }

    public static Object getAgent(Scriptable scriptable) {
        throw new UnsupportedOperationException("$262.agent property not yet implemented");
    }

    @Override
    public String getClassName() {
        return "__262__";
    }
}
