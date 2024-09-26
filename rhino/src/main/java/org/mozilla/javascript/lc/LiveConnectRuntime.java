package org.mozilla.javascript.lc;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.lang.reflect.Constructor;

/**
 * @author Roland Praml, Foconis Analytics GmbH
 */
public class LiveConnectRuntime {
	public static ScriptableObject getGlobal(Context cx) {
		final String GLOBAL_CLASS = "org.mozilla.javascript.tools.shell.Global";
		Class<?> globalClass = Kit.classOrNull(GLOBAL_CLASS);
		if (globalClass != null) {
			try {
				Class<?>[] parm = {ScriptRuntime.ContextClass};
				Constructor<?> globalClassCtor = globalClass.getConstructor(parm);
				Object[] arg = {cx};
				return (ScriptableObject) globalClassCtor.newInstance(arg);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				// fall through...
			}
		}
		return new ImporterTopLevel(cx);
	}
	public static void main(final Script script, final String[] args) {
		ContextFactory.getGlobal()
				.call(
						cx -> {
							ScriptableObject global = getGlobal(cx);

							// get the command line arguments and define "arguments"
							// array in the top-level object
							Object[] argsCopy = new Object[args.length];
							System.arraycopy(args, 0, argsCopy, 0, args.length);
							Scriptable argsObj = cx.newArray(global, argsCopy);
							global.defineProperty("arguments", argsObj, ScriptableObject.DONTENUM);
							script.exec(cx, global);
							return null;
						});
	}
}
