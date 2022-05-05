package org.mozilla.javascript.tests;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.IRFactory;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.ScriptNode;

public class ReadCommentsTest {

    @Test
    public void readComments() throws IOException {
        CompilerEnvirons compilerEnv = new CompilerEnvirons();
        compilerEnv.setOptimizationLevel(5);
        compilerEnv.setRecordingComments(true);
        compilerEnv.setRecordingLocalJsDocComments(true);
        compilerEnv.setStrictMode(true);

        Parser p = new Parser(compilerEnv);
        String testJs;
        try (BufferedReader scriptIn =
                new BufferedReader(new FileReader("testsrc/jstests/withcomments.js"))) {
            testJs = scriptIn.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        AstRoot ast = p.parse(testJs, "test", 1);
        IRFactory irf = new IRFactory(compilerEnv);
        ScriptNode tree = irf.transformTree(ast);

        Assert.assertEquals(1, tree.getFunctions().size());
        Assert.assertEquals(
                "/** @responseClass HttpAdapter */", tree.getFunctionNode(0).getJsDoc());
    }
}
