/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.XmlMemberGet;

/**
 * Test resembles issue #483 with {@link XmlMemberGet#toSource()} implementation. {@code toSource()}
 * implementation calls {@link org.mozilla.javascript.ast.AstNode#operatorToString(int)} passing in
 * node's type, which is {@link org.mozilla.javascript.Token#DOT} or {@link
 * org.mozilla.javascript.Token#DOTDOT} by documentation. This causes {@link
 * IllegalArgumentException}, as {@code DOT} and {@code DOTDOT} are not treated as operators in
 * {@code AstNode}.
 */
public class BugXmlMemberGetToSource {
    private CompilerEnvirons environment;

    @Before
    public void setUp() {
        environment = new CompilerEnvirons();
    }

    @Test
    public void xmlMemberGetToSourceDotAt() {
        String script = "a.@b;";
        Parser parser = new Parser(environment);
        AstRoot root = parser.parse(script, null, 1);
        /* up to 1.7.9 following will throw IllegalArgumentException */
        assertEquals("a.@b;", root.toSource().trim());
    }

    @Test
    public void xmlMemberGetToSourceDotDot() {
        String script = "a..b;";
        Parser parser = new Parser(environment);
        AstRoot root = parser.parse(script, null, 1);
        /* up to 1.7.9 following will throw IllegalArgumentException */
        assertEquals("a..b;", root.toSource().trim());
    }
}
