/* ***** BEGIN LICENSE BLOCK *****
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
 *   Bob Jervis
 *   Roger Lawrence
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


package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.*;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.ScriptNode;

public final class OptFunctionNode
{
    OptFunctionNode(FunctionNode fnode)
    {
        this.fnode = fnode;
        fnode.setCompilerData(this);
    }

    public static OptFunctionNode get(ScriptNode scriptOrFn, int i)
    {
        FunctionNode fnode = scriptOrFn.getFunctionNode(i);
        return (OptFunctionNode)fnode.getCompilerData();
    }

    public static OptFunctionNode get(ScriptNode scriptOrFn)
    {
        return (OptFunctionNode)scriptOrFn.getCompilerData();
    }

    public boolean isTargetOfDirectCall()
    {
        return directTargetIndex >= 0;
    }

    public int getDirectTargetIndex()
    {
        return directTargetIndex;
    }

    void setDirectTargetIndex(int directTargetIndex)
    {
        // One time action
        if (directTargetIndex < 0 || this.directTargetIndex >= 0)
            Kit.codeBug();
        this.directTargetIndex = directTargetIndex;
    }

    void setParameterNumberContext(boolean b)
    {
        itsParameterNumberContext = b;
    }

    public boolean getParameterNumberContext()
    {
        return itsParameterNumberContext;
    }

    public int getVarCount()
    {
        return fnode.getParamAndVarCount();
    }

    public boolean isParameter(int varIndex)
    {
        return varIndex < fnode.getParamCount();
    }

    public boolean isNumberVar(int varIndex)
    {
        varIndex -= fnode.getParamCount();
        if (varIndex >= 0 && numberVarFlags != null) {
            return numberVarFlags[varIndex];
        }
        return false;
    }

    void setIsNumberVar(int varIndex)
    {
        varIndex -= fnode.getParamCount();
        // Can only be used with non-parameters
        if (varIndex < 0) Kit.codeBug();
        if (numberVarFlags == null) {
            int size = fnode.getParamAndVarCount() - fnode.getParamCount();
            numberVarFlags = new boolean[size];
        }
        numberVarFlags[varIndex] = true;
    }

    public int getVarIndex(Node n)
    {
        int index = n.getIntProp(Node.VARIABLE_PROP, -1);
        if (index == -1) {
            Node node;
            int type = n.getType();
            if (type == Token.GETVAR) {
                node = n;
            } else if (type == Token.SETVAR ||
                       type == Token.SETCONSTVAR) {
                node = n.getFirstChild();
            } else {
                throw Kit.codeBug();
            }
            index = fnode.getIndexForNameNode(node);
            if (index < 0) throw Kit.codeBug();
            n.putIntProp(Node.VARIABLE_PROP, index);
        }
        return index;
    }

    public final FunctionNode fnode;

    private boolean[] numberVarFlags;
    private int directTargetIndex = -1;
    private boolean itsParameterNumberContext;
    boolean itsContainsCalls0;
    boolean itsContainsCalls1;
}
