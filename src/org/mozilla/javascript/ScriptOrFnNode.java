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
 *   Igor Bukanov
 *   Bob Jervis
 *   Norris Boyd
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

import java.util.ArrayList;

public class ScriptOrFnNode extends Node.Scope {

    public ScriptOrFnNode(int nodeType) {
        super(nodeType);
        symbols = new ArrayList<Symbol>(4);
        setParent(null);
    }

    public final String getSourceName() { return sourceName; }

    public final void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public final int getEncodedSourceStart() { return encodedSourceStart; }

    public final int getEncodedSourceEnd() { return encodedSourceEnd; }

    public final void setEncodedSourceBounds(int start, int end) {
        this.encodedSourceStart = start;
        this.encodedSourceEnd = end;
    }

    public final int getBaseLineno() { return this.lineno; }

    public final void setBaseLineno(int lineno) {
        // One time action
        if (lineno < 0 || this.lineno >= 0) Kit.codeBug();
        this.lineno = lineno;
    }

    public final int getEndLineno() { return endLineno; }

    public final void setEndLineno(int lineno) {
        // One time action
        if (lineno < 0 || endLineno >= 0) Kit.codeBug();
        endLineno = lineno;
    }

    public final int getFunctionCount() {
        if (functions == null) { return 0; }
        return functions.size();
    }

    public final FunctionNode getFunctionNode(int i) {
        return (FunctionNode)functions.get(i);
    }

    public final int addFunction(FunctionNode fnNode) {
        if (fnNode == null) Kit.codeBug();
        if (functions == null) { functions = new ObjArray(); }
        functions.add(fnNode);
        return functions.size() - 1;
    }

    public final int getRegexpCount() {
        if (regexps == null) { return 0; }
        return regexps.size() / 2;
    }

    public final String getRegexpString(int index) {
        return (String)regexps.get(index * 2);
    }

    public final String getRegexpFlags(int index) {
        return (String)regexps.get(index * 2 + 1);
    }

    public final int addRegexp(String string, String flags) {
        if (string == null) Kit.codeBug();
        if (regexps == null) { regexps = new ObjArray(); }
        regexps.add(string);
        regexps.add(flags);
        return regexps.size() / 2 - 1;
    }

    public int getIndexForNameNode(Node nameNode) {
        if (variableNames == null) throw Kit.codeBug();
        Node.Scope node = nameNode.getScope();
        Symbol symbol = node == null ? null 
                                     : node.getSymbol(nameNode.getString());
        if (symbol == null)
            return -1;
        return symbol.index;
    }

    public final String getParamOrVarName(int index) {
        if (variableNames == null) throw Kit.codeBug();
        return variableNames[index];
    }

    public final int getParamCount() {
        return paramCount;
    }

    public final int getParamAndVarCount() {
        if (variableNames == null) throw Kit.codeBug();
        return symbols.size();
    }

    public final String[] getParamAndVarNames() {
        if (variableNames == null) throw Kit.codeBug();
        return variableNames;
    }

    public final boolean[] getParamAndVarConst() {
        if (variableNames == null) throw Kit.codeBug();
        return isConsts;
    }

    void addSymbol(Symbol symbol) {
        if (variableNames != null) throw Kit.codeBug();
        if (symbol.declType == Token.LP) {
            paramCount++;
        }
        symbols.add(symbol);
    }

    /**
     * Assign every symbol a unique integer index. Generate arrays of variable 
     * names and constness that can be indexed by those indices.
     * 
     * @param flattenAllTables if true, flatten all symbol tables, included
     * nested block scope symbol tables. If false, just flatten the script's
     * or function's symbol table.
     */
    void flattenSymbolTable(boolean flattenAllTables) {
        if (!flattenAllTables) {
            ArrayList<Symbol> newSymbols = new ArrayList<Symbol>();
            if (this.symbolTable != null) {
                // Just replace "symbols" with the symbols in this object's
                // symbol table. Can't just work from symbolTable map since
                // we need to retain duplicate parameters.
                for (int i=0; i < symbols.size(); i++) {
                    Symbol symbol = symbols.get(i);
                    if (symbol.containingTable == this) {
                        newSymbols.add(symbol);
                    }
                }
            }
            symbols = newSymbols;
        }
        variableNames = new String[symbols.size()];
        isConsts = new boolean[symbols.size()];
        for (int i=0; i < symbols.size(); i++) {
            Symbol symbol = symbols.get(i);
            variableNames[i] = symbol.name;
            isConsts[i] = symbol.declType == Token.CONST;
            symbol.index = i;
        }
    }

    public final Object getCompilerData()
    {
        return compilerData;
    }

    public final void setCompilerData(Object data)
    {
        if (data == null) throw new IllegalArgumentException();
        // Can only call once
        if (compilerData != null) throw new IllegalStateException();
        compilerData = data;
    }
    
    public String getNextTempName()
    {
        return "$" + tempNumber++;
    }

    private int encodedSourceStart;
    private int encodedSourceEnd;
    private String sourceName;
    private int endLineno = -1;

    private ObjArray functions;
    private ObjArray regexps;
    
    private ArrayList<Symbol> symbols;
    private int paramCount = 0;
    private String[] variableNames;
    private boolean[] isConsts;

    private Object compilerData;
    private int tempNumber = 0;
}
