/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Norris Boyd
 *   Igor Bukanov
 *   Bob Jervis
 *   Roger Lawrence
 *   Mike McCabe
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript;

import org.mozilla.javascript.debug.DebuggableScript;

/**
 * This class implements the Function native object.
 * See ECMA 15.3.
 * @author Norris Boyd
 */
public abstract class NativeFunction extends BaseFunction
{

    static final long serialVersionUID = 8713897114082216401L;
    
    public final void initScriptFunction(Context cx, Scriptable scope)
    {
        ScriptRuntime.setFunctionProtoAndParent(this, scope);
    }

    /**
     * @param indent How much to indent the decompiled result
     *
     * @param flags Flags specifying format of decompilation output
     */
    @Override
    final String decompile(int indent, int flags)
    {
        String encodedSource = getEncodedSource();
        if (encodedSource == null) {
            return super.decompile(indent, flags);
        } else {
            UintMap properties = new UintMap(1);
            properties.put(Decompiler.INITIAL_INDENT_PROP, indent);
            return Decompiler.decompile(encodedSource, flags, properties);
        }
    }

    @Override
    public int getLength()
    {
        int paramCount = getParamCount();
        if (getLanguageVersion() != Context.VERSION_1_2) {
            return paramCount;
        }
        Context cx = Context.getContext();
        NativeCall activation = ScriptRuntime.findFunctionActivation(cx, this);
        if (activation == null) {
            return paramCount;
        }
        return activation.originalArgs.length;
    }

    @Override
    public int getArity()
    {
        return getParamCount();
    }

    /**
     * @deprecated Use {@link BaseFunction#getFunctionName()} instead.
     * For backwards compatibility keep an old method name used by
     * Batik and possibly others.
     */
    public String jsGet_name()
    {
        return getFunctionName();
    }

    /**
     * Get encoded source string.
     */
    public String getEncodedSource()
    {
        return null;
    }

    public DebuggableScript getDebuggableView()
    {
        return null;
    }

    /**
     * Resume execution of a suspended generator.
     * @param cx The current context
     * @param scope Scope for the parent generator function
     * @param operation The resumption operation (next, send, etc.. )
     * @param state The generator state (has locals, stack, etc.)
     * @param value The return value of yield (if required).
     * @return The next yielded value (if any)
     */
    public Object resumeGenerator(Context cx, Scriptable scope,
                                  int operation, Object state, Object value)
    {
        throw new EvaluatorException("resumeGenerator() not implemented");
    }


    protected abstract int getLanguageVersion();

    /**
     * Get number of declared parameters. It should be 0 for scripts.
     */
    protected abstract int getParamCount();

    /**
     * Get number of declared parameters and variables defined through var
     * statements.
     */
    protected abstract int getParamAndVarCount();

    /**
     * Get parameter or variable name.
     * If <tt>index < {@link #getParamCount()}</tt>, then return the name of the
     * corresponding parameter. Otherwise return the name of variable.
     */
    protected abstract String getParamOrVarName(int index);

    /**
     * Get parameter or variable const-ness.
     * If <tt>index < {@link #getParamCount()}</tt>, then return the const-ness
     * of the corresponding parameter. Otherwise return whether the variable is
     * const.
     */
    protected boolean getParamOrVarConst(int index)
    {
        // By default return false to preserve compatibility with existing
        // classes subclassing this class, which are mostly generated by jsc
        // from earlier Rhino versions. See Bugzilla #396117.
        return false;
    }
}

