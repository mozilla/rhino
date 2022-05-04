package org.mozilla.javascript.tests;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Comment;

/**
 * Tests if comments between try/catch tokens or blocks makes the parsing go wrong. See {@link
 * https://github.com/mozilla/rhino/issues/803}
 *
 * @author Javier Luengo
 */
public class TryCatchWithCommentsParseTest {

    @Test
    public void tryCatchWithCommentsParse() throws IOException {
        CompilerEnvirons compilerEnv = new CompilerEnvirons();
        compilerEnv.setRecordingComments(true);

        Parser p = new Parser(compilerEnv);
        String testJs;
        try (BufferedReader scriptIn =
                new BufferedReader(new FileReader("testsrc/jstests/trycatchwithcomments.js"))) {
            testJs = scriptIn.lines().collect(Collectors.joining(System.lineSeparator()));
        }

        // Parsing of this source should not throw an exception
        AstRoot ast = p.parse(testJs, "test", 1);

        // Check that exactly all comments were registered in tree
        int i = 0;
        for (Comment comment : ast.getComments()) {
            Assert.assertEquals(comment.getValue(), "//" + i);
            i++;
        }
        Assert.assertEquals(i, 15);
    }
}
