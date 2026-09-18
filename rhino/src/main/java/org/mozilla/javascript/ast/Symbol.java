/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

/** Represents a symbol-table entry. */
public class Symbol {

    public enum Type {
        FUNCTION_VAR,
        FUNCTION_LET,
        LP,
        VAR,
        LET,
        CONST;

        public static Type fromToken(int token) {
            return switch (token) {
                case Token.FUNCTION -> FUNCTION_VAR;
                case Token.LP -> LP;
                case Token.VAR -> VAR;
                case Token.LET -> LET;
                case Token.CONST -> CONST;
                default -> {
                    throw new IllegalArgumentException("Invalid declType: " + token);
                }
            };
        }
    }

    private Type declType;
    private int index = -1;
    private String name;
    private Node node;
    private Scope containingTable;
    // For VAR (and pre-ES6 CONST/FUNCTION) symbols, the lexical scope in which
    // the declaration actually appears, before hoisting to containingTable.
    private Scope declaredScope;

    public Symbol() {}

    /**
     * Constructs a new Symbol with a specific name and declaration type
     *
     * @param declType {@link Token#FUNCTION}, {@link Token#LP} (for params), {@link Token#VAR},
     *     {@link Token#LET} or {@link Token#CONST}
     */
    public Symbol(Type declType, String name) {
        setName(name);
        setDeclType(declType);
    }

    /** Returns symbol declaration type */
    public Type getDeclType() {
        return declType;
    }

    /** Sets symbol declaration type */
    public void setDeclType(Type declType) {
        this.declType = declType;
    }

    /** Returns symbol name */
    public String getName() {
        return name;
    }

    /** Sets symbol name */
    public void setName(String name) {
        this.name = name;
    }

    /** Returns the node associated with this identifier */
    public Node getNode() {
        return node;
    }

    /** Returns symbol's index in its scope */
    public int getIndex() {
        return index;
    }

    /** Sets symbol's index in its scope */
    public void setIndex(int index) {
        this.index = index;
    }

    /** Sets the node associated with this identifier */
    public void setNode(Node node) {
        this.node = node;
    }

    /** Returns the Scope in which this symbol is entered */
    public Scope getContainingTable() {
        return containingTable;
    }

    /** Sets this symbol's Scope */
    public void setContainingTable(Scope containingTable) {
        this.containingTable = containingTable;
    }

    /**
     * Returns the lexical scope in which this symbol was declared, or {@code null} if not set. For
     * VAR symbols this is the block scope containing the declaration, which may differ from {@link
     * #getContainingTable()} (the enclosing function/script to which the var is hoisted).
     */
    public Scope getDeclaredScope() {
        return declaredScope;
    }

    /** Sets the lexical scope in which this symbol was declared. */
    public void setDeclaredScope(Scope declaredScope) {
        this.declaredScope = declaredScope;
    }

    public String getDeclTypeName() {
        return declType.name();
    }

    public boolean isDeclTypeLexical() {
        return declType == Type.FUNCTION_LET || declType == Type.LET || declType == Type.CONST;
    }

    public static boolean isDeclTypeLexical(Type declType) {
        return declType == Type.FUNCTION_LET || declType == Type.LET || declType == Type.CONST;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Symbol (");
        result.append(getDeclTypeName());
        result.append(") name=");
        result.append(name);
        if (node != null) {
            result.append(" line=");
            result.append(node.getLineno());
        }
        return result.toString();
    }
}
