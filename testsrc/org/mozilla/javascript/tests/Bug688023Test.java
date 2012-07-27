/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;

/**
 * @author André Bargull
 *
 */
public class Bug688023Test {
    private Context cx;

    @Before
    public void setUp() {
        cx = Context.enter();
    }

    @After
    public void tearDown() {
        Context.exit();
    }

    private AstRoot parse(CharSequence cs) {
        CompilerEnvirons compilerEnv = new CompilerEnvirons();
        compilerEnv.initFromContext(cx);
        ErrorReporter compilationErrorReporter = compilerEnv.getErrorReporter();
        Parser p = new Parser(compilerEnv, compilationErrorReporter);
        return p.parse(cs.toString(), "<eval>", 1);
    }

    private String toSource(CharSequence cs) {
        return parse(cs).toSource();
    }

    private static String lines(String... lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    @Test
    public void toSourceForInLoop() {
        assertEquals(lines(
          "for (x in y) ",
          "  b = 1;"
        ), toSource("for(x in y) b=1;"));
        assertEquals(lines(
          "for (x in y) {",
          "  b = 1;",
          "}"
        ), toSource("for(x in y) {b=1;}"));
    }

    @Test
    public void toSourceForLoop() {
        assertEquals(lines(
          "for (; ; ) ",
          "  b = 1;"
        ), toSource("for(;;) b=1;"));
        assertEquals(lines(
          "for (; ; ) {",
          "  b = 1;",
          "}"
        ), toSource("for(;;) {b=1;}"));
    }

    @Test
    public void toSourceWhileLoop() {
        assertEquals(lines(
          "while (a) ",
          "  b = 1;"
        ), toSource("while(a) b=1;"));
        assertEquals(lines(
          "while (a) {",
          "  b = 1;",
          "}"
        ), toSource("while(a) {b=1;}"));
    }

    @Test
    public void toSourceWithStatement() {
        assertEquals(lines(
          "with (a) ",
          "  b = 1;"
        ), toSource("with(a) b=1;"));
        assertEquals(lines(
          "with (a) {",
          "  b = 1;",
          "}"
        ), toSource("with(a) {b=1;}"));
    }

    @Test
    public void toSourceIfStatement() {
        assertEquals(lines(
          "if (a) ",
          "  b = 1;"
        ), toSource("if(a) b=1;"));
        assertEquals(lines(
          "if (a) {",
          "  b = 1;",
          "}"
        ), toSource("if(a) {b=1;}"));
    }

    @Test
    public void toSourceIfElseStatement() {
        assertEquals(lines(
          "if (a) ",
          "  b = 1;",
          "else ",
          "  b = 2;"
        ), toSource("if(a) b=1; else b=2;"));
        assertEquals(lines(
          "if (a) {",
          "  b = 1;",
          "} else ",
          "  b = 2;"
        ), toSource("if(a) { b=1; } else b=2;"));
        assertEquals(lines(
          "if (a) ",
          "  b = 1;",
          "else {",
          "  b = 2;",
          "}"
        ), toSource("if(a) b=1; else { b=2; }"));
        assertEquals(lines(
          "if (a) {",
          "  b = 1;",
          "} else {",
          "  b = 2;",
          "}"
        ), toSource("if(a) { b=1; } else { b=2; }"));
    }

    @Test
    public void toSourceIfElseIfElseStatement() {
        assertEquals(lines(
          "if (a) ",
          "  b = 1;",
          "else if (a) ",
          "  b = 2;",
          "else ",
          "  b = 3;"
        ), toSource("if(a) b=1; else if (a) b=2; else b=3;"));
        assertEquals(lines(
          "if (a) {",
          "  b = 1;",
          "} else if (a) ",
          "  b = 2;",
          "else ",
          "  b = 3;"
        ), toSource("if(a) { b=1; } else if (a) b=2; else b=3;"));
        assertEquals(lines(
          "if (a) ",
          "  b = 1;",
          "else if (a) {",
          "  b = 2;",
          "} else ",
          "  b = 3;"
        ), toSource("if(a) b=1; else if (a) { b=2; } else b=3;"));
        assertEquals(lines(
          "if (a) {",
          "  b = 1;",
          "} else if (a) {",
          "  b = 2;",
          "} else ",
          "  b = 3;"
        ), toSource("if(a) { b=1; } else if (a) { b=2; } else b=3;"));
        assertEquals(lines(
          "if (a) ",
          "  b = 1;",
          "else if (a) ",
          "  b = 2;",
          "else {",
          "  b = 3;",
          "}"
        ), toSource("if(a) b=1; else if (a) b=2; else {b=3;}"));
        assertEquals(lines(
          "if (a) {",
          "  b = 1;",
          "} else if (a) ",
          "  b = 2;",
          "else {",
          "  b = 3;",
          "}"
        ), toSource("if(a) { b=1; } else if (a) b=2; else {b=3;}"));
        assertEquals(lines(
          "if (a) ",
          "  b = 1;",
          "else if (a) {",
          "  b = 2;",
          "} else {",
          "  b = 3;",
          "}"
        ), toSource("if(a) b=1; else if (a) { b=2; } else {b=3;}"));
        assertEquals(lines(
          "if (a) {",
          "  b = 1;",
          "} else if (a) {",
          "  b = 2;",
          "} else {",
          "  b = 3;",
          "}"
        ), toSource("if(a) { b=1; } else if (a) { b=2; } else {b=3;}"));
    }
}
