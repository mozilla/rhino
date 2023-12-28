/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import java.math.BigInteger;
import org.mozilla.javascript.Token;

/** AST node for a BigInt literal. Node type is {@link Token#BIGINT}. */
public class BigIntLiteral extends AstNode {

    private String value;
    private BigInteger bigInt;

    {
        type = Token.BIGINT;
    }

    public BigIntLiteral() {}

    public BigIntLiteral(int pos) {
        super(pos);
    }

    public BigIntLiteral(int pos, int len) {
        super(pos, len);
    }

    /** Constructor. Sets the length to the length of the {@code value} string. */
    public BigIntLiteral(int pos, String value) {
        super(pos);
        setValue(value);
        setLength(value.length());
    }

    /** Constructor. Sets the length to the length of the {@code value} string. */
    public BigIntLiteral(int pos, String value, BigInteger bigInt) {
        this(pos, value);
        setBigInt(bigInt);
    }

    /** Returns the node's string value (the original source token) */
    public String getValue() {
        return value;
    }

    /**
     * Sets the node's value
     *
     * @throws IllegalArgumentException} if value is {@code null}
     */
    public void setValue(String value) {
        assertNotNull(value);
        this.value = value;
    }

    /** Gets the {@code BigInteger} value. */
    @Override
    public BigInteger getBigInt() {
        return bigInt;
    }

    /** Sets the node's {@code BigInteger} value. */
    @Override
    public void setBigInt(BigInteger value) {
        bigInt = value;
    }

    @Override
    public String toSource(int depth) {
        return makeIndent(depth) + (bigInt == null ? "<null>" : bigInt.toString() + "n");
    }

    /** Visits this node. There are no children to visit. */
    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }
}
