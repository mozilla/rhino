/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import java.util.SortedSet;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Comment;

/** Tests position of Comment node in source code. */
public class Issue533Test {
    private static final String SOURCE_URI = "issue533test.js";

    private Parser parser;

    @Before
    public void setUp() {
        CompilerEnvirons compilerEnv = new CompilerEnvirons();
        compilerEnv.setRecordingComments(true);
        parser = new Parser(compilerEnv);
    }

    @Test
    public void getPosition() {
        String script =
                "function a() {\n    //testtest\n   function b() {\n        //password\n    }\n}";
        AstRoot root = parser.parse(script, SOURCE_URI, 0);
        SortedSet<Comment> comments = root.getComments();
        assertEquals(2, comments.size());
        for (Comment comment : comments) {
            assertEquals(comment.getValue(), getFromSource(script, comment));
        }
    }

    private String getFromSource(String source, AstNode node) {
        return source.substring(
                node.getAbsolutePosition(), node.getAbsolutePosition() + node.getLength());
    }
}
