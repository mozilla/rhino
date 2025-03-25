/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mozilla.javascript.ast.ArrayComprehension;
import org.mozilla.javascript.ast.ArrayComprehensionLoop;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.BigIntLiteral;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.BreakStatement;
import org.mozilla.javascript.ast.CatchClause;
import org.mozilla.javascript.ast.Comment;
import org.mozilla.javascript.ast.ComputedPropertyKey;
import org.mozilla.javascript.ast.ConditionalExpression;
import org.mozilla.javascript.ast.ContinueStatement;
import org.mozilla.javascript.ast.DestructuringForm;
import org.mozilla.javascript.ast.DoLoop;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.EmptyExpression;
import org.mozilla.javascript.ast.EmptyStatement;
import org.mozilla.javascript.ast.ErrorNode;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.ForInLoop;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.GeneratorExpression;
import org.mozilla.javascript.ast.GeneratorExpressionLoop;
import org.mozilla.javascript.ast.GeneratorMethodDefinition;
import org.mozilla.javascript.ast.IdeErrorReporter;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Jump;
import org.mozilla.javascript.ast.KeywordLiteral;
import org.mozilla.javascript.ast.Label;
import org.mozilla.javascript.ast.LabeledStatement;
import org.mozilla.javascript.ast.LetNode;
import org.mozilla.javascript.ast.Loop;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NewExpression;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.ParenthesizedExpression;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.RegExpLiteral;
import org.mozilla.javascript.ast.ReturnStatement;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.SwitchCase;
import org.mozilla.javascript.ast.SwitchStatement;
import org.mozilla.javascript.ast.Symbol;
import org.mozilla.javascript.ast.TaggedTemplateLiteral;
import org.mozilla.javascript.ast.TemplateCharacters;
import org.mozilla.javascript.ast.TemplateLiteral;
import org.mozilla.javascript.ast.ThrowStatement;
import org.mozilla.javascript.ast.TryStatement;
import org.mozilla.javascript.ast.UnaryExpression;
import org.mozilla.javascript.ast.UpdateExpression;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;
import org.mozilla.javascript.ast.WhileLoop;
import org.mozilla.javascript.ast.WithStatement;
import org.mozilla.javascript.ast.XmlDotQuery;
import org.mozilla.javascript.ast.XmlElemRef;
import org.mozilla.javascript.ast.XmlExpression;
import org.mozilla.javascript.ast.XmlLiteral;
import org.mozilla.javascript.ast.XmlMemberGet;
import org.mozilla.javascript.ast.XmlPropRef;
import org.mozilla.javascript.ast.XmlRef;
import org.mozilla.javascript.ast.XmlString;
import org.mozilla.javascript.ast.Yield;

/**
 * This class implements the JavaScript parser.
 *
 * <p>It is based on the SpiderMonkey C source files jsparse.c and jsparse.h in the jsref package.
 *
 * <p>The parser generates an {@link AstRoot} parse tree representing the source code. No tree
 * rewriting is permitted at this stage, so that the parse tree is a faithful representation of the
 * source for frontend processing tools and IDEs.
 *
 * <p>This parser implementation is not intended to be reused after a parse finishes, and will throw
 * an IllegalStateException() if invoked again.
 *
 * <p>
 *
 * @see TokenStream
 * @author Mike McCabe
 * @author Brendan Eich
 */
public class Parser {
    /** Maximum number of allowed function or constructor arguments, to follow SpiderMonkey. */
    public static final int ARGC_LIMIT = 1 << 16;

    // TokenInformation flags : currentFlaggedToken stores them together
    // with token type
    static final int CLEAR_TI_MASK = 0xFFFF, // mask to clear token information bits
            TI_AFTER_EOL = 1 << 16, // first token of the source line
            TI_CHECK_LABEL = 1 << 17; // indicates to check for label

    CompilerEnvirons compilerEnv;
    private ErrorReporter errorReporter;
    private IdeErrorReporter errorCollector;
    private String sourceURI;
    private char[] sourceChars;

    boolean calledByCompileFunction; // ugly - set directly by Context
    private boolean parseFinished; // set when finished to prevent reuse

    private TokenStream ts;
    CurrentPositionReporter currentPos;
    private int currentFlaggedToken = Token.EOF;
    private int currentToken;
    private int syntaxErrorCount;

    private List<Comment> scannedComments;
    private Comment currentJsDocComment;

    protected int nestingOfFunction;
    protected int nestingOfFunctionParams;
    private LabeledStatement currentLabel;
    private boolean inDestructuringAssignment;
    protected boolean inUseStrictDirective;

    // The following are per function variables and should be saved/restored
    // during function parsing.  See PerFunctionVariables class below.
    ScriptNode currentScriptOrFn;
    private boolean insideMethod;
    Scope currentScope;
    private int endFlags;
    private boolean inForInit; // bound temporarily during forStatement()
    private Map<String, LabeledStatement> labelSet;
    private List<Loop> loopSet;
    private List<Jump> loopAndSwitchSet;
    // end of per function variables

    // Lacking 2-token lookahead, labels become a problem.
    // These vars store the token info of the last matched name,
    // iff it wasn't the last matched token.
    private int prevNameTokenStart;
    private String prevNameTokenString = "";
    private int prevNameTokenLineno;
    private int prevNameTokenColumn;
    private int lastTokenLineno = -1;
    private int lastTokenColumn = -1;

    private boolean defaultUseStrictDirective;

    // Exception to unwind
    public static class ParserException extends RuntimeException {
        private static final long serialVersionUID = 5882582646773765630L;
    }

    static interface Transformer {
        Node transform(AstNode node);
    }

    public Parser() {
        this(new CompilerEnvirons());
    }

    public Parser(CompilerEnvirons compilerEnv) {
        this(compilerEnv, compilerEnv.getErrorReporter());
    }

    public Parser(CompilerEnvirons compilerEnv, ErrorReporter errorReporter) {
        this.compilerEnv = compilerEnv;
        this.errorReporter = errorReporter;
        if (errorReporter instanceof IdeErrorReporter) {
            errorCollector = (IdeErrorReporter) errorReporter;
        }
    }

    // Add a strict warning on the last matched token.
    void addStrictWarning(String messageId, String messageArg) {
        addStrictWarning(messageId, messageArg, currentPos.getPosition(), currentPos.getLength());
    }

    void addStrictWarning(String messageId, String messageArg, int position, int length) {
        if (compilerEnv.isStrictMode()) addWarning(messageId, messageArg, position, length);
    }

    void addWarning(String messageId, String messageArg) {
        addWarning(messageId, messageArg, currentPos.getPosition(), currentPos.getLength());
    }

    void addWarning(String messageId, int position, int length) {
        addWarning(messageId, null, position, length);
    }

    void addWarning(String messageId, String messageArg, int position, int length) {
        String message = lookupMessage(messageId, messageArg);
        if (compilerEnv.reportWarningAsError()) {
            addError(messageId, messageArg, position, length);
        } else if (errorCollector != null) {
            errorCollector.warning(message, sourceURI, position, length);
        } else {
            errorReporter.warning(
                    message,
                    sourceURI,
                    currentPos.getLineno(),
                    currentPos.getLine(),
                    currentPos.getOffset());
        }
    }

    void addError(String messageId) {
        addError(messageId, currentPos.getPosition(), currentPos.getLength());
    }

    void addError(String messageId, int position, int length) {
        addError(messageId, null, position, length);
    }

    void addError(String messageId, String messageArg) {
        addError(messageId, messageArg, currentPos.getPosition(), currentPos.getLength());
    }

    void addError(String messageId, int c) {
        String messageArg = Character.toString((char) c);
        addError(messageId, messageArg);
    }

    void addError(String messageId, String messageArg, int position, int length) {
        ++syntaxErrorCount;
        String message = lookupMessage(messageId, messageArg);
        if (errorCollector != null) {
            errorCollector.error(message, sourceURI, position, length);
        } else {
            errorReporter.error(
                    message,
                    sourceURI,
                    currentPos.getLineno(),
                    currentPos.getLine(),
                    currentPos.getOffset());
        }
    }

    private void addStrictWarning(
            String messageId,
            String messageArg,
            int position,
            int length,
            int line,
            String lineSource,
            int lineOffset) {
        if (compilerEnv.isStrictMode()) {
            addWarning(messageId, messageArg, position, length, line, lineSource, lineOffset);
        }
    }

    private void addWarning(
            String messageId,
            String messageArg,
            int position,
            int length,
            int line,
            String lineSource,
            int lineOffset) {
        String message = lookupMessage(messageId, messageArg);
        if (compilerEnv.reportWarningAsError()) {
            addError(messageId, messageArg, position, length, line, lineSource, lineOffset);
        } else if (errorCollector != null) {
            errorCollector.warning(message, sourceURI, position, length);
        } else {
            errorReporter.warning(message, sourceURI, line, lineSource, lineOffset);
        }
    }

    private void addError(
            String messageId,
            String messageArg,
            int position,
            int length,
            int line,
            String lineSource,
            int lineOffset) {
        ++syntaxErrorCount;
        String message = lookupMessage(messageId, messageArg);
        if (errorCollector != null) {
            errorCollector.error(message, sourceURI, position, length);
        } else {
            errorReporter.error(message, sourceURI, line, lineSource, lineOffset);
        }
    }

    String lookupMessage(String messageId) {
        return lookupMessage(messageId, null);
    }

    String lookupMessage(String messageId, String messageArg) {
        return messageArg == null
                ? ScriptRuntime.getMessageById(messageId)
                : ScriptRuntime.getMessageById(messageId, messageArg);
    }

    void reportError(String messageId) {
        reportError(messageId, null);
    }

    void reportError(String messageId, String messageArg) {
        reportError(messageId, messageArg, currentPos.getPosition(), currentPos.getLength());
    }

    void reportError(String messageId, int position, int length) {
        reportError(messageId, null, position, length);
    }

    void reportError(String messageId, String messageArg, int position, int length) {
        addError(messageId, messageArg, position, length);

        if (!compilerEnv.recoverFromErrors()) {
            throw new ParserException();
        }
    }

    // Computes the absolute end offset of node N.
    // Use with caution!  Assumes n.getPosition() is -absolute-, which
    // is only true before the node is added to its parent.
    private static int getNodeEnd(AstNode n) {
        return n.getPosition() + n.getLength();
    }

    private void recordComment(int lineno, int column, String comment) {
        if (scannedComments == null) {
            scannedComments = new ArrayList<>();
        }
        Comment commentNode =
                new Comment(ts.tokenBeg, ts.getTokenLength(), ts.commentType, comment);
        if (ts.commentType == Token.CommentType.JSDOC
                && compilerEnv.isRecordingLocalJsDocComments()) {
            currentJsDocComment =
                    new Comment(ts.tokenBeg, ts.getTokenLength(), ts.commentType, comment);
            currentJsDocComment.setLineColumnNumber(lineno, column);
        }
        commentNode.setLineColumnNumber(lineno, column);
        scannedComments.add(commentNode);
    }

    private Comment getAndResetJsDoc() {
        Comment saved = currentJsDocComment;
        currentJsDocComment = null;
        return saved;
    }

    // Returns the next token without consuming it.
    // If previous token was consumed, calls scanner to get new token.
    // If previous token was -not- consumed, returns it (idempotent).
    //
    // This function will not return a newline (Token.EOL - instead, it
    // gobbles newlines until it finds a non-newline token, and flags
    // that token as appearing just after a newline.
    //
    // This function will also not return a Token.COMMENT.  Instead, it
    // records comments in the scannedComments list.  If the token
    // returned by this function immediately follows a jsdoc comment,
    // the token is flagged as such.
    //
    // Note that this function always returned the un-flagged token!
    // The flags, if any, are saved in currentFlaggedToken.
    private int peekToken() throws IOException {
        // By far the most common case:  last token hasn't been consumed,
        // so return already-peeked token.
        if (currentFlaggedToken != Token.EOF) {
            return currentToken;
        }

        int tt = ts.getToken();
        boolean sawEOL = false;

        // process comments and whitespace
        while (tt == Token.EOL || tt == Token.COMMENT) {
            if (tt == Token.EOL) {
                sawEOL = true;
                tt = ts.getToken();
            } else {
                if (compilerEnv.isRecordingComments()) {
                    String comment = ts.getAndResetCurrentComment();
                    recordComment(ts.getTokenStartLineno(), ts.getTokenColumn(), comment);
                    break;
                }
                tt = ts.getToken();
            }
        }

        currentToken = tt;
        currentFlaggedToken = tt | (sawEOL ? TI_AFTER_EOL : 0);
        return currentToken; // return unflagged token
    }

    private int lineNumber() {
        return lastTokenLineno;
    }

    private int columnNumber() {
        return lastTokenColumn;
    }

    private int peekFlaggedToken() throws IOException {
        peekToken();
        return currentFlaggedToken;
    }

    private void consumeToken() {
        currentFlaggedToken = Token.EOF;
        lastTokenLineno = ts.getTokenStartLineno();
        lastTokenColumn = ts.getTokenColumn();
    }

    private int nextToken() throws IOException {
        int tt = peekToken();
        consumeToken();
        return tt;
    }

    private boolean matchToken(int toMatch, boolean ignoreComment) throws IOException {
        int tt = peekToken();
        while (tt == Token.COMMENT && ignoreComment) {
            consumeToken();
            tt = peekToken();
        }
        if (tt != toMatch) {
            return false;
        }
        consumeToken();
        return true;
    }

    // Returns Token.EOL if the current token follows a newline, else returns
    // the current token.  Used in situations where we don't consider certain
    // token types valid if they are preceded by a newline.  One example is the
    // postfix ++ or -- operator, which has to be on the same line as its
    // operand.
    private int peekTokenOrEOL() throws IOException {
        int tt = peekToken();
        // Check for last peeked token flags
        if ((currentFlaggedToken & TI_AFTER_EOL) != 0) {
            tt = Token.EOL;
        }
        return tt;
    }

    private boolean mustMatchToken(int toMatch, String messageId, boolean ignoreComment)
            throws IOException {
        return mustMatchToken(
                toMatch, messageId, ts.tokenBeg, ts.tokenEnd - ts.tokenBeg, ignoreComment);
    }

    private boolean mustMatchToken(
            int toMatch, String msgId, int pos, int len, boolean ignoreComment) throws IOException {
        if (matchToken(toMatch, ignoreComment)) {
            return true;
        }
        reportError(msgId, pos, len);
        return false;
    }

    private void mustHaveXML() {
        if (!compilerEnv.isXmlAvailable()) {
            reportError("msg.XML.not.available");
        }
    }

    public boolean eof() {
        return ts.eof();
    }

    boolean insideFunctionBody() {
        return nestingOfFunction != 0;
    }

    boolean insideFunctionParams() {
        return nestingOfFunctionParams != 0;
    }

    void pushScope(Scope scope) {
        Scope parent = scope.getParentScope();
        // During codegen, parent scope chain may already be initialized,
        // in which case we just need to set currentScope variable.
        if (parent != null) {
            if (parent != currentScope) codeBug();
        } else {
            currentScope.addChildScope(scope);
        }
        currentScope = scope;
    }

    void popScope() {
        currentScope = currentScope.getParentScope();
    }

    private void enterLoop(Loop loop) {
        if (loopSet == null) loopSet = new ArrayList<>();
        loopSet.add(loop);
        if (loopAndSwitchSet == null) loopAndSwitchSet = new ArrayList<>();
        loopAndSwitchSet.add(loop);
        pushScope(loop);
        if (currentLabel != null) {
            currentLabel.setStatement(loop);
            currentLabel.getFirstLabel().setLoop(loop);
            // This is the only time during parsing that we set a node's parent
            // before parsing the children.  In order for the child node offsets
            // to be correct, we adjust the loop's reported position back to an
            // absolute source offset, and restore it when we call
            // restoreRelativeLoopPosition() (invoked just before setBody() is
            // called on the loop).
            loop.setRelative(-currentLabel.getPosition());
        }
    }

    private void exitLoop() {
        loopSet.remove(loopSet.size() - 1);
        loopAndSwitchSet.remove(loopAndSwitchSet.size() - 1);
        popScope();
    }

    private void restoreRelativeLoopPosition(Loop loop) {
        if (loop.getParent() != null) { // see comment in enterLoop
            loop.setRelative(loop.getParent().getPosition());
        }
    }

    private void enterSwitch(SwitchStatement node) {
        if (loopAndSwitchSet == null) loopAndSwitchSet = new ArrayList<>();
        loopAndSwitchSet.add(node);
    }

    private void exitSwitch() {
        loopAndSwitchSet.remove(loopAndSwitchSet.size() - 1);
    }

    /**
     * Builds a parse tree from the given source string.
     *
     * @return an {@link AstRoot} object representing the parsed program. If the parse fails, {@code
     *     null} will be returned. (The parse failure will result in a call to the {@link
     *     ErrorReporter} from {@link CompilerEnvirons}.)
     */
    public AstRoot parse(String sourceString, String sourceURI, int lineno) {
        if (parseFinished) throw new IllegalStateException("parser reused");
        this.sourceURI = sourceURI;
        if (compilerEnv.isIdeMode()) {
            this.sourceChars = sourceString.toCharArray();
        }
        currentPos = ts = new TokenStream(this, null, sourceString, lineno);
        try {
            return parse();
        } catch (IOException iox) {
            // Should never happen
            throw new IllegalStateException();
        } finally {
            parseFinished = true;
        }
    }

    /**
     * Builds a parse tree from the given sourcereader.
     *
     * @see #parse(String,String,int)
     * @throws IOException if the {@link Reader} encounters an error
     * @deprecated use parse(String, String, int) instead
     */
    @Deprecated
    public AstRoot parse(Reader sourceReader, String sourceURI, int lineno) throws IOException {
        if (parseFinished) throw new IllegalStateException("parser reused");
        if (compilerEnv.isIdeMode()) {
            return parse(Kit.readReader(sourceReader), sourceURI, lineno);
        }
        try {
            this.sourceURI = sourceURI;
            currentPos = ts = new TokenStream(this, sourceReader, null, lineno);
            return parse();
        } finally {
            parseFinished = true;
        }
    }

    private AstRoot parse() throws IOException {
        int pos = 0;
        AstRoot root = new AstRoot(pos);
        currentScope = currentScriptOrFn = root;

        int baseLineno = ts.lineno; // line number where source starts
        prevNameTokenLineno = ts.getLineno();
        prevNameTokenColumn = ts.getTokenColumn();
        int end = pos; // in case source is empty

        boolean inDirectivePrologue = true;
        boolean savedStrictMode = inUseStrictDirective;

        inUseStrictDirective = defaultUseStrictDirective;
        if (inUseStrictDirective) {
            root.setInStrictMode(true);
        }

        try {
            for (; ; ) {
                int tt = peekToken();
                if (tt <= Token.EOF) {
                    break;
                }

                AstNode n;
                if (tt == Token.FUNCTION) {
                    consumeToken();
                    try {
                        n =
                                function(
                                        calledByCompileFunction
                                                ? FunctionNode.FUNCTION_EXPRESSION
                                                : FunctionNode.FUNCTION_STATEMENT);
                    } catch (ParserException e) {
                        break;
                    }
                } else if (tt == Token.COMMENT) {
                    n = scannedComments.get(scannedComments.size() - 1);
                    consumeToken();
                } else {
                    n = statement();
                    if (inDirectivePrologue) {
                        String directive = getDirective(n);
                        if (directive == null) {
                            inDirectivePrologue = false;
                        } else if (directive.equals("use strict")) {
                            inUseStrictDirective = true;
                            root.setInStrictMode(true);
                        }
                    }
                }
                end = getNodeEnd(n);
                root.addChildToBack(n);
                n.setParent(root);
            }
        } catch (StackOverflowError ex) {
            String msg = lookupMessage("msg.too.deep.parser.recursion");
            if (!compilerEnv.isIdeMode())
                throw Context.reportRuntimeError(msg, sourceURI, lineNumber(), null, 0);
        } finally {
            inUseStrictDirective = savedStrictMode;
        }

        reportErrorsIfExists(baseLineno);

        // add comments to root in lexical order
        if (scannedComments != null) {
            // If we find a comment beyond end of our last statement or
            // function, extend the root bounds to the end of that comment.
            int last = scannedComments.size() - 1;
            end = Math.max(end, getNodeEnd(scannedComments.get(last)));
            for (Comment c : scannedComments) {
                root.addComment(c);
            }
        }

        root.setLength(end - pos);
        root.setSourceName(sourceURI);
        root.setBaseLineno(baseLineno);
        root.setEndLineno(ts.getLineno());
        return root;
    }

    private AstNode parseFunctionBody(int type, FunctionNode fnNode) throws IOException {
        boolean isExpressionClosure = false;
        if (!matchToken(Token.LC, true)) {
            if (compilerEnv.getLanguageVersion() < Context.VERSION_1_8
                    && type != FunctionNode.ARROW_FUNCTION) {
                reportError("msg.no.brace.body");
            } else {
                isExpressionClosure = true;
            }
        }
        boolean isArrow = type == FunctionNode.ARROW_FUNCTION;
        ++nestingOfFunction;
        int pos = ts.tokenBeg;
        Block pn = new Block(pos); // starts at LC position

        // Function code that is supplied as the arguments to the built-in
        // Function, Generator, and AsyncFunction constructors is strict mode code
        // if the last argument is a String that when processed is a FunctionBody
        // that begins with a Directive Prologue that contains a Use Strict Directive.
        boolean inDirectivePrologue = true;
        boolean savedStrictMode = inUseStrictDirective;

        pn.setLineColumnNumber(lineNumber(), columnNumber());
        try {
            if (isExpressionClosure) {
                AstNode returnValue = assignExpr();
                ReturnStatement n =
                        new ReturnStatement(
                                returnValue.getPosition(), returnValue.getLength(), returnValue);
                // expression closure flag is required on both nodes
                n.putProp(Node.EXPRESSION_CLOSURE_PROP, Boolean.TRUE);
                n.setLineColumnNumber(returnValue.getLineno(), returnValue.getColumn());
                pn.putProp(Node.EXPRESSION_CLOSURE_PROP, Boolean.TRUE);
                if (isArrow) {
                    n.putProp(Node.ARROW_FUNCTION_PROP, Boolean.TRUE);
                }
                pn.addStatement(n);
                pn.setLength(n.getLength());
            } else {
                bodyLoop:
                for (; ; ) {
                    AstNode n;
                    int tt = peekToken();
                    switch (tt) {
                        case Token.ERROR:
                        case Token.EOF:
                        case Token.RC:
                            break bodyLoop;
                        case Token.COMMENT:
                            consumeToken();
                            n = scannedComments.get(scannedComments.size() - 1);
                            break;
                        case Token.FUNCTION:
                            consumeToken();
                            n = function(FunctionNode.FUNCTION_STATEMENT);
                            break;
                        default:
                            n = statement();
                            if (inDirectivePrologue) {
                                String directive = getDirective(n);
                                if (directive == null) {
                                    inDirectivePrologue = false;
                                } else if (directive.equals("use strict")) {
                                    if (fnNode.getDefaultParams() != null) {
                                        reportError("msg.default.args.use.strict");
                                    }
                                    inUseStrictDirective = true;
                                    fnNode.setInStrictMode(true);
                                    if (!savedStrictMode) {
                                        setRequiresActivation();
                                    }
                                }
                            }
                            break;
                    }
                    pn.addStatement(n);
                }
                int end = ts.tokenEnd;
                if (mustMatchToken(Token.RC, "msg.no.brace.after.body", true)) end = ts.tokenEnd;
                pn.setLength(end - pos);
            }
        } catch (ParserException e) {
            // Ignore it
        } finally {
            --nestingOfFunction;
            inUseStrictDirective = savedStrictMode;
        }

        getAndResetJsDoc();
        return pn;
    }

    private static String getDirective(AstNode n) {
        if (n instanceof ExpressionStatement) {
            AstNode e = ((ExpressionStatement) n).getExpression();
            if (e instanceof StringLiteral) {
                return ((StringLiteral) e).getValue();
            }
        }
        return null;
    }

    private void parseFunctionParams(FunctionNode fnNode) throws IOException {
        ++nestingOfFunctionParams;
        try {
            if (matchToken(Token.RP, true)) {
                fnNode.setRp(ts.tokenBeg - fnNode.getPosition());
                return;
            }
            // Would prefer not to call createDestructuringAssignment until codegen,
            // but the symbol definitions have to happen now, before body is parsed.
            Map<String, Node> destructuring = null;
            Map<String, AstNode> destructuringDefault = null;

            Set<String> paramNames = new HashSet<>();
            do {
                int tt = peekToken();
                if (tt == Token.RP) {
                    if (fnNode.hasRestParameter()) {
                        // Error: parameter after rest parameter
                        reportError("msg.parm.after.rest", ts.tokenBeg, ts.tokenEnd - ts.tokenBeg);
                    }

                    fnNode.putIntProp(Node.TRAILING_COMMA, 1);
                    break;
                }
                if (tt == Token.LB || tt == Token.LC) {
                    if (fnNode.hasRestParameter()) {
                        // Error: parameter after rest parameter
                        reportError("msg.parm.after.rest", ts.tokenBeg, ts.tokenEnd - ts.tokenBeg);
                    }

                    AstNode expr = destructuringAssignExpr();
                    if (destructuring == null) {
                        destructuring = new HashMap<>();
                    }

                    if (expr instanceof Assignment) {
                        // We have default arguments inside destructured function parameters
                        // eg: f([x = 1] = [2]) { ... }, transform this into:
                        // f(x) {
                        //      if ($1 == undefined)
                        //          var $1 = [2];
                        //      if (x == undefined)
                        //          if (($1[0]) == undefined)
                        //              var x = 1;
                        //          else
                        //              var x = $1[0];
                        // }
                        // fnNode.addParam(name)
                        AstNode lhs = ((Assignment) expr).getLeft(); // [x = 1]
                        AstNode rhs = ((Assignment) expr).getRight(); // [2]
                        markDestructuring(lhs);
                        fnNode.addParam(lhs);
                        String pname = currentScriptOrFn.getNextTempName();
                        defineSymbol(Token.LP, pname, false);
                        if (destructuringDefault == null) {
                            destructuringDefault = new HashMap<>();
                        }
                        destructuring.put(pname, lhs);
                        destructuringDefault.put(pname, rhs);
                    } else {
                        markDestructuring(expr);
                        fnNode.addParam(expr);
                        // Destructuring assignment for parameters: add a dummy
                        // parameter name, and add a statement to the body to initialize
                        // variables from the destructuring assignment
                        String pname = currentScriptOrFn.getNextTempName();
                        defineSymbol(Token.LP, pname, false);
                        destructuring.put(pname, expr);
                    }
                } else {
                    boolean wasRest = false;
                    int restStartLineno = -1, restStartColumn = -1;
                    if (tt == Token.DOTDOTDOT) {
                        if (fnNode.hasRestParameter()) {
                            // Error: parameter after rest parameter
                            reportError(
                                    "msg.parm.after.rest", ts.tokenBeg, ts.tokenEnd - ts.tokenBeg);
                        }

                        fnNode.setHasRestParameter(true);
                        wasRest = true;
                        consumeToken();
                        restStartLineno = lineNumber();
                        restStartColumn = columnNumber();
                    }

                    if (mustMatchToken(Token.NAME, "msg.no.parm", true)) {
                        if (!wasRest && fnNode.hasRestParameter()) {
                            // Error: parameter after rest parameter
                            reportError(
                                    "msg.parm.after.rest", ts.tokenBeg, ts.tokenEnd - ts.tokenBeg);
                        }

                        Name paramNameNode = createNameNode();
                        if (wasRest) {
                            paramNameNode.setLineColumnNumber(restStartLineno, restStartColumn);
                        }
                        Comment jsdocNodeForName = getAndResetJsDoc();
                        if (jsdocNodeForName != null) {
                            paramNameNode.setJsDocNode(jsdocNodeForName);
                        }
                        fnNode.addParam(paramNameNode);
                        String paramName = ts.getString();
                        defineSymbol(Token.LP, paramName);
                        if (this.inUseStrictDirective) {
                            if ("eval".equals(paramName) || "arguments".equals(paramName)) {
                                reportError("msg.bad.id.strict", paramName);
                            }
                            if (paramNames.contains(paramName))
                                addError("msg.dup.param.strict", paramName);
                            paramNames.add(paramName);
                        }

                        if (matchToken(Token.ASSIGN, true)) {
                            if (compilerEnv.getLanguageVersion() >= Context.VERSION_ES6) {
                                fnNode.putDefaultParams(paramName, assignExpr());
                            } else {
                                reportError("msg.default.args");
                            }
                        }
                    } else {
                        fnNode.addParam(makeErrorNode());
                    }
                }
            } while (matchToken(Token.COMMA, true));

            if (destructuring != null) {
                Node destructuringNode = new Node(Token.COMMA);
                // Add assignment helper for each destructuring parameter
                for (Map.Entry<String, Node> param : destructuring.entrySet()) {
                    AstNode defaultValue = null;
                    if (destructuringDefault != null) {
                        defaultValue = destructuringDefault.get(param.getKey());
                    }
                    Node assign =
                            createDestructuringAssignment(
                                    Token.VAR,
                                    param.getValue(),
                                    createName(param.getKey()),
                                    defaultValue);
                    destructuringNode.addChildToBack(assign);
                }
                fnNode.putProp(Node.DESTRUCTURING_PARAMS, destructuringNode);
            }

            if (mustMatchToken(Token.RP, "msg.no.paren.after.parms", true)) {
                fnNode.setRp(ts.tokenBeg - fnNode.getPosition());
            }
        } finally {
            --nestingOfFunctionParams;
        }
    }

    private FunctionNode function(int type) throws IOException {
        return function(type, false);
    }

    private FunctionNode function(int type, boolean isMethodDefiniton) throws IOException {
        boolean isGenerator = false;
        int syntheticType = type;
        int baseLineno = lineNumber(); // line number where source starts
        int functionSourceStart = ts.tokenBeg; // start of "function" kwd
        int functionStartColumn = columnNumber();
        Name name = null;
        AstNode memberExprNode = null;

        do {
            if (matchToken(Token.NAME, true)) {
                name = createNameNode(true, Token.NAME);
                if (inUseStrictDirective) {
                    String id = name.getIdentifier();
                    if ("eval".equals(id) || "arguments".equals(id)) {
                        reportError("msg.bad.id.strict", id);
                    }
                }
                if (!matchToken(Token.LP, true)) {
                    if (compilerEnv.isAllowMemberExprAsFunctionName()) {
                        AstNode memberExprHead = name;
                        name = null;
                        memberExprNode = memberExprTail(false, memberExprHead);
                    }
                    mustMatchToken(Token.LP, "msg.no.paren.parms", true);
                }
            } else if (matchToken(Token.LP, true)) {
                // Anonymous function:  leave name as null
            } else if (matchToken(Token.MUL, true)
                    && (compilerEnv.getLanguageVersion() >= Context.VERSION_ES6)) {
                // ES6 generator function
                isGenerator = true;
                continue;
            } else {
                if (compilerEnv.isAllowMemberExprAsFunctionName()) {
                    // Note that memberExpr can not start with '(' like
                    // in function (1+2).toString(), because 'function (' already
                    // processed as anonymous function
                    memberExprNode = memberExpr(false);
                }
                mustMatchToken(Token.LP, "msg.no.paren.parms", true);
            }
            break;
        } while (isGenerator);
        int lpPos = currentToken == Token.LP ? ts.tokenBeg : -1;

        if (memberExprNode != null) {
            syntheticType = FunctionNode.FUNCTION_EXPRESSION;
        }

        if (syntheticType != FunctionNode.FUNCTION_EXPRESSION
                && name != null
                && name.length() > 0) {
            // Function statements define a symbol in the enclosing scope
            defineSymbol(Token.FUNCTION, name.getIdentifier());
        }

        FunctionNode fnNode = new FunctionNode(functionSourceStart, name);
        fnNode.setMethodDefinition(isMethodDefiniton);
        fnNode.setFunctionType(type);
        if (isGenerator) {
            fnNode.setIsES6Generator();
        }
        if (lpPos != -1) fnNode.setLp(lpPos - functionSourceStart);

        fnNode.setJsDocNode(getAndResetJsDoc());

        PerFunctionVariables savedVars = new PerFunctionVariables(fnNode);
        boolean wasInsideMethod = insideMethod;
        insideMethod = isMethodDefiniton;
        try {
            parseFunctionParams(fnNode);
            AstNode body = parseFunctionBody(type, fnNode);
            fnNode.setBody(body);
            int end = functionSourceStart + body.getPosition() + body.getLength();
            fnNode.setRawSourceBounds(functionSourceStart, end);
            fnNode.setLength(end - functionSourceStart);

            if (compilerEnv.isStrictMode() && !fnNode.getBody().hasConsistentReturnUsage()) {
                String msg =
                        (name != null && name.length() > 0)
                                ? "msg.no.return.value"
                                : "msg.anon.no.return.value";
                addStrictWarning(msg, name == null ? "" : name.getIdentifier());
            }
        } finally {
            savedVars.restore();
            insideMethod = wasInsideMethod;
        }

        if (memberExprNode != null) {
            // TODO(stevey): fix missing functionality
            Kit.codeBug();
            fnNode.setMemberExprNode(memberExprNode); // rewrite later
            /* old code:
            if (memberExprNode != null) {
                pn = nf.createAssignment(Token.ASSIGN, memberExprNode, pn);
                if (functionType != FunctionNode.FUNCTION_EXPRESSION) {
                    // XXX check JScript behavior: should it be createExprStatement?
                    pn = nf.createExprStatementNoReturn(pn, baseLineno);
                }
            }
            */
        }

        fnNode.setSourceName(sourceURI);
        fnNode.setLineColumnNumber(baseLineno, functionStartColumn);
        fnNode.setEndLineno(lineNumber());

        // Set the parent scope.  Needed for finding undeclared vars.
        // Have to wait until after parsing the function to set its parent
        // scope, since defineSymbol needs the defining-scope check to stop
        // at the function boundary when checking for redeclarations.
        if (compilerEnv.isIdeMode()) {
            fnNode.setParentScope(currentScope);
        }
        return fnNode;
    }

    private AstNode arrowFunction(AstNode params, int startLine, int startColumn)
            throws IOException {
        int baseLineno = lineNumber(); // line number where source starts
        int functionSourceStart =
                params != null ? params.getPosition() : -1; // start of "function" kwd

        FunctionNode fnNode = new FunctionNode(functionSourceStart);
        fnNode.setFunctionType(FunctionNode.ARROW_FUNCTION);
        fnNode.setJsDocNode(getAndResetJsDoc());

        // Would prefer not to call createDestructuringAssignment until codegen,
        // but the symbol definitions have to happen now, before body is parsed.
        Map<String, Node> destructuring = new HashMap<>();
        Map<String, AstNode> destructuringDefault = new HashMap<>();
        Set<String> paramNames = new HashSet<>();

        PerFunctionVariables savedVars = new PerFunctionVariables(fnNode);
        // Intentionally not overwriting "insideMethod" - we want to propagate this from the parent
        // function or scope
        try {
            if (params instanceof ParenthesizedExpression) {
                fnNode.setParens(0, params.getLength());
                if (params.getIntProp(Node.TRAILING_COMMA, 0) == 1) {
                    fnNode.putIntProp(Node.TRAILING_COMMA, 1);
                }
                AstNode p = ((ParenthesizedExpression) params).getExpression();
                if (!(p instanceof EmptyExpression)) {
                    arrowFunctionParams(fnNode, p, destructuring, destructuringDefault, paramNames);
                }
            } else {
                arrowFunctionParams(
                        fnNode, params, destructuring, destructuringDefault, paramNames);
            }

            if (!destructuring.isEmpty()) {
                Node destructuringNode = new Node(Token.COMMA);
                // Add assignment helper for each destructuring parameter
                for (Map.Entry<String, Node> param : destructuring.entrySet()) {
                    AstNode defaultValue = null;
                    if (destructuringDefault != null) {
                        defaultValue = destructuringDefault.get(param.getKey());
                    }
                    Node assign =
                            createDestructuringAssignment(
                                    Token.VAR,
                                    param.getValue(),
                                    createName(param.getKey()),
                                    defaultValue);
                    destructuringNode.addChildToBack(assign);
                }
                fnNode.putProp(Node.DESTRUCTURING_PARAMS, destructuringNode);
            }

            AstNode body = parseFunctionBody(FunctionNode.ARROW_FUNCTION, fnNode);
            fnNode.setBody(body);
            int end = functionSourceStart + body.getPosition() + body.getLength();
            fnNode.setRawSourceBounds(functionSourceStart, end);
            fnNode.setLength(end - functionSourceStart);
        } finally {
            savedVars.restore();
        }

        if (fnNode.isGenerator()) {
            reportError("msg.arrowfunction.generator");
            return makeErrorNode();
        }

        fnNode.setSourceName(sourceURI);
        fnNode.setBaseLineno(baseLineno);
        fnNode.setEndLineno(lineNumber());
        fnNode.setLineColumnNumber(startLine, startColumn);

        return fnNode;
    }

    private void arrowFunctionParams(
            FunctionNode fnNode,
            AstNode params,
            Map<String, Node> destructuring,
            Map<String, AstNode> destructuringDefault,
            Set<String> paramNames)
            throws IOException {
        if (params instanceof ArrayLiteral || params instanceof ObjectLiteral) {
            markDestructuring(params);
            fnNode.addParam(params);
            String pname = currentScriptOrFn.getNextTempName();
            defineSymbol(Token.LP, pname, false);
            destructuring.put(pname, params);
        } else if (params instanceof InfixExpression && params.getType() == Token.COMMA) {
            arrowFunctionParams(
                    fnNode,
                    ((InfixExpression) params).getLeft(),
                    destructuring,
                    destructuringDefault,
                    paramNames);
            arrowFunctionParams(
                    fnNode,
                    ((InfixExpression) params).getRight(),
                    destructuring,
                    destructuringDefault,
                    paramNames);
        } else if (params instanceof Name) {
            fnNode.addParam(params);
            String paramName = ((Name) params).getIdentifier();
            defineSymbol(Token.LP, paramName);

            if (this.inUseStrictDirective) {
                if ("eval".equals(paramName) || "arguments".equals(paramName)) {
                    reportError("msg.bad.id.strict", paramName);
                }
                if (paramNames.contains(paramName)) addError("msg.dup.param.strict", paramName);
                paramNames.add(paramName);
            }
        } else if (params instanceof Assignment) {
            if (compilerEnv.getLanguageVersion() >= Context.VERSION_ES6) {
                AstNode rhs = ((Assignment) params).getRight();
                AstNode lhs = ((Assignment) params).getLeft();
                String paramName;

                /* copy default values for use in IR */
                if (lhs instanceof Name) {
                    paramName = ((Name) lhs).getIdentifier();
                    fnNode.putDefaultParams(paramName, rhs);
                    arrowFunctionParams(
                            fnNode, lhs, destructuring, destructuringDefault, paramNames);
                } else if (lhs instanceof ArrayLiteral || lhs instanceof ObjectLiteral) {
                    markDestructuring(lhs);
                    fnNode.addParam(lhs);
                    String pname = currentScriptOrFn.getNextTempName();
                    defineSymbol(Token.LP, pname, false);
                    destructuring.put(pname, lhs);
                    destructuringDefault.put(pname, rhs);
                } else {
                    reportError("msg.no.parm", params.getPosition(), params.getLength());
                    fnNode.addParam(makeErrorNode());
                }
            } else {
                reportError("msg.default.args");
            }
        } else {
            reportError("msg.no.parm", params.getPosition(), params.getLength());
            fnNode.addParam(makeErrorNode());
        }
    }

    // This function does not match the closing RC: the caller matches
    // the RC so it can provide a suitable error message if not matched.
    // This means it's up to the caller to set the length of the node to
    // include the closing RC.  The node start pos is set to the
    // absolute buffer start position, and the caller should fix it up
    // to be relative to the parent node.  All children of this block
    // node are given relative start positions and correct lengths.

    private AstNode statements(AstNode parent) throws IOException {
        if (currentToken != Token.LC // assertion can be invalid in bad code
                && !compilerEnv.isIdeMode()) codeBug();
        int pos = ts.tokenBeg;
        AstNode block = parent != null ? parent : new Block(pos);
        block.setLineColumnNumber(lineNumber(), columnNumber());

        int tt;
        while ((tt = peekToken()) > Token.EOF && tt != Token.RC) {
            block.addChild(statement());
        }
        block.setLength(ts.tokenBeg - pos);
        return block;
    }

    private AstNode statements() throws IOException {
        return statements(null);
    }

    private static class ConditionData {
        AstNode condition;
        int lp = -1;
        int rp = -1;
    }

    // parse and return a parenthesized expression
    private ConditionData condition() throws IOException {
        ConditionData data = new ConditionData();

        if (mustMatchToken(Token.LP, "msg.no.paren.cond", true)) data.lp = ts.tokenBeg;

        data.condition = expr(false);

        if (mustMatchToken(Token.RP, "msg.no.paren.after.cond", true)) data.rp = ts.tokenBeg;

        // Report strict warning on code like "if (a = 7) ...". Suppress the
        // warning if the condition is parenthesized, like "if ((a = 7)) ...".
        if (data.condition instanceof Assignment) {
            addStrictWarning(
                    "msg.equal.as.assign",
                    "",
                    data.condition.getPosition(),
                    data.condition.getLength());
        }
        return data;
    }

    private AstNode statement() throws IOException {
        int pos = ts.tokenBeg;
        try {
            AstNode pn = statementHelper();
            if (pn != null) {
                if (compilerEnv.isStrictMode() && !pn.hasSideEffects()) {
                    int beg = pn.getPosition();
                    beg = Math.max(beg, lineBeginningFor(beg));
                    addStrictWarning(
                            pn instanceof EmptyStatement
                                    ? "msg.extra.trailing.semi"
                                    : "msg.no.side.effects",
                            "",
                            beg,
                            nodeEnd(pn) - beg);
                }
                int ntt = peekToken();
                if (ntt == Token.COMMENT
                        && pn.getLineno()
                                == scannedComments.get(scannedComments.size() - 1).getLineno()) {
                    pn.setInlineComment(scannedComments.get(scannedComments.size() - 1));
                    consumeToken();
                }
                return pn;
            }
        } catch (ParserException e) {
            // an ErrorNode was added to the ErrorReporter
        }

        // error:  skip ahead to a probable statement boundary
        guessingStatementEnd:
        for (; ; ) {
            int tt = peekTokenOrEOL();
            consumeToken();
            switch (tt) {
                case Token.ERROR:
                case Token.EOF:
                case Token.EOL:
                case Token.SEMI:
                    break guessingStatementEnd;
            }
        }
        // We don't make error nodes explicitly part of the tree;
        // they get added to the ErrorReporter.  May need to do
        // something different here.
        return new EmptyStatement(pos, ts.tokenBeg - pos);
    }

    private AstNode statementHelper() throws IOException {
        // If the statement is set, then it's been told its label by now.
        if (currentLabel != null && currentLabel.getStatement() != null) currentLabel = null;

        AstNode pn = null;
        int tt = peekToken(), pos = ts.tokenBeg;
        int lineno, column;

        switch (tt) {
            case Token.IF:
                return ifStatement();

            case Token.SWITCH:
                return switchStatement();

            case Token.WHILE:
                return whileLoop();

            case Token.DO:
                return doLoop();

            case Token.FOR:
                return forLoop();

            case Token.TRY:
                return tryStatement();

            case Token.THROW:
                pn = throwStatement();
                break;

            case Token.BREAK:
                pn = breakStatement();
                break;

            case Token.CONTINUE:
                pn = continueStatement();
                break;

            case Token.WITH:
                if (this.inUseStrictDirective) {
                    reportError("msg.no.with.strict");
                }
                return withStatement();

            case Token.CONST:
            case Token.VAR:
                consumeToken();
                lineno = lineNumber();
                column = columnNumber();
                pn = variables(currentToken, ts.tokenBeg, true);
                pn.setLineColumnNumber(lineno, column);
                break;

            case Token.LET:
                pn = letStatement();
                if (pn instanceof VariableDeclaration && peekToken() == Token.SEMI) break;
                return pn;

            case Token.RETURN:
            case Token.YIELD:
                pn = returnOrYield(tt, false);
                break;

            case Token.DEBUGGER:
                consumeToken();
                pn = new KeywordLiteral(ts.tokenBeg, ts.tokenEnd - ts.tokenBeg, tt);
                pn.setLineColumnNumber(lineNumber(), columnNumber());
                break;

            case Token.LC:
                return block();

            case Token.ERROR:
                consumeToken();
                return makeErrorNode();

            case Token.SEMI:
                consumeToken();
                pos = ts.tokenBeg;
                pn = new EmptyStatement(pos, ts.tokenEnd - pos);
                pn.setLineColumnNumber(lineNumber(), columnNumber());
                return pn;

            case Token.FUNCTION:
                consumeToken();
                return function(FunctionNode.FUNCTION_EXPRESSION_STATEMENT);

            case Token.DEFAULT:
                pn = defaultXmlNamespace();
                break;

            case Token.NAME:
                pn = nameOrLabel();
                if (pn instanceof ExpressionStatement) break;
                return pn; // LabeledStatement
            case Token.COMMENT:
                // Do not consume token here
                pn = scannedComments.get(scannedComments.size() - 1);
                return pn;
            default:
                // Intentionally not calling lineNumber/columnNumber here!
                // We have not consumed any token yet, so the position would be invalid
                lineno = ts.getLineno();
                column = ts.getTokenColumn();
                pn = new ExpressionStatement(expr(false), !insideFunctionBody());
                pn.setLineColumnNumber(lineno, column);
                break;
        }

        autoInsertSemicolon(pn);
        return pn;
    }

    private void autoInsertSemicolon(AstNode pn) throws IOException {
        int ttFlagged = peekFlaggedToken();
        int pos = pn.getPosition();
        switch (ttFlagged & CLEAR_TI_MASK) {
            case Token.SEMI:
                // Consume ';' as a part of expression
                consumeToken();
                // extend the node bounds to include the semicolon.
                pn.setLength(ts.tokenEnd - pos);
                break;
            case Token.ERROR:
            case Token.EOF:
            case Token.RC:
                // Autoinsert ;
                // Token.EOF can have negative length and negative nodeEnd(pn).
                // So, make the end position at least pos+1.
                warnMissingSemi(pos, Math.max(pos + 1, nodeEnd(pn)));
                break;
            default:
                if ((ttFlagged & TI_AFTER_EOL) == 0) {
                    // Report error if no EOL or autoinsert ; otherwise
                    reportError("msg.no.semi.stmt");
                } else {
                    warnMissingSemi(pos, nodeEnd(pn));
                }
                break;
        }
    }

    private IfStatement ifStatement() throws IOException {
        if (currentToken != Token.IF) codeBug();
        consumeToken();
        int pos = ts.tokenBeg, lineno = lineNumber(), elsePos = -1, column = columnNumber();
        IfStatement pn = new IfStatement(pos);
        ConditionData data = condition();
        AstNode ifTrue = getNextStatementAfterInlineComments(pn), ifFalse = null;
        if (matchToken(Token.ELSE, true)) {
            int tt = peekToken();
            if (tt == Token.COMMENT) {
                pn.setElseKeyWordInlineComment(scannedComments.get(scannedComments.size() - 1));
                consumeToken();
            }
            elsePos = ts.tokenBeg - pos;
            ifFalse = statement();
        }
        int end = getNodeEnd(ifFalse != null ? ifFalse : ifTrue);
        pn.setLength(end - pos);
        pn.setCondition(data.condition);
        pn.setParens(data.lp - pos, data.rp - pos);
        pn.setThenPart(ifTrue);
        pn.setElsePart(ifFalse);
        pn.setElsePosition(elsePos);
        pn.setLineColumnNumber(lineno, column);
        return pn;
    }

    private SwitchStatement switchStatement() throws IOException {
        if (currentToken != Token.SWITCH) codeBug();
        consumeToken();
        int pos = ts.tokenBeg;

        SwitchStatement pn = new SwitchStatement(pos);
        pn.setLineColumnNumber(lineNumber(), columnNumber());
        if (mustMatchToken(Token.LP, "msg.no.paren.switch", true)) pn.setLp(ts.tokenBeg - pos);

        AstNode discriminant = expr(false);
        pn.setExpression(discriminant);
        enterSwitch(pn);

        try {
            if (mustMatchToken(Token.RP, "msg.no.paren.after.switch", true))
                pn.setRp(ts.tokenBeg - pos);

            mustMatchToken(Token.LC, "msg.no.brace.switch", true);

            boolean hasDefault = false;
            int tt;
            switchLoop:
            for (; ; ) {
                tt = nextToken();
                int casePos = ts.tokenBeg;
                int caseLineno = lineNumber(), caseColumn = columnNumber();
                AstNode caseExpression = null;
                switch (tt) {
                    case Token.RC:
                        pn.setLength(ts.tokenEnd - pos);
                        break switchLoop;

                    case Token.CASE:
                        caseExpression = expr(false);
                        mustMatchToken(Token.COLON, "msg.no.colon.case", true);
                        break;

                    case Token.DEFAULT:
                        if (hasDefault) {
                            reportError("msg.double.switch.default");
                        }
                        hasDefault = true;
                        mustMatchToken(Token.COLON, "msg.no.colon.case", true);
                        break;
                    case Token.COMMENT:
                        AstNode n = scannedComments.get(scannedComments.size() - 1);
                        pn.addChild(n);
                        continue switchLoop;
                    default:
                        reportError("msg.bad.switch");
                        break switchLoop;
                }

                SwitchCase caseNode = new SwitchCase(casePos);
                caseNode.setExpression(caseExpression);
                caseNode.setLength(ts.tokenEnd - pos); // include colon
                caseNode.setLineColumnNumber(caseLineno, caseColumn);

                while ((tt = peekToken()) != Token.RC
                        && tt != Token.CASE
                        && tt != Token.DEFAULT
                        && tt != Token.EOF) {
                    if (tt == Token.COMMENT) {
                        Comment inlineComment = scannedComments.get(scannedComments.size() - 1);
                        if (caseNode.getInlineComment() == null
                                && inlineComment.getLineno() == caseNode.getLineno()) {
                            caseNode.setInlineComment(inlineComment);
                        } else {
                            caseNode.addStatement(inlineComment);
                        }
                        consumeToken();
                        continue;
                    }
                    AstNode nextStmt = statement();
                    caseNode.addStatement(nextStmt); // updates length
                }
                pn.addCase(caseNode);
            }
        } finally {
            exitSwitch();
        }
        return pn;
    }

    private WhileLoop whileLoop() throws IOException {
        if (currentToken != Token.WHILE) codeBug();
        consumeToken();
        int pos = ts.tokenBeg;
        WhileLoop pn = new WhileLoop(pos);
        pn.setLineColumnNumber(lineNumber(), columnNumber());
        enterLoop(pn);
        try {
            ConditionData data = condition();
            pn.setCondition(data.condition);
            pn.setParens(data.lp - pos, data.rp - pos);
            AstNode body = getNextStatementAfterInlineComments(pn);
            pn.setLength(getNodeEnd(body) - pos);
            restoreRelativeLoopPosition(pn);
            pn.setBody(body);
        } finally {
            exitLoop();
        }
        return pn;
    }

    private DoLoop doLoop() throws IOException {
        if (currentToken != Token.DO) codeBug();
        consumeToken();
        int pos = ts.tokenBeg, end;
        DoLoop pn = new DoLoop(pos);
        pn.setLineColumnNumber(lineNumber(), columnNumber());
        enterLoop(pn);
        try {
            AstNode body = getNextStatementAfterInlineComments(pn);
            mustMatchToken(Token.WHILE, "msg.no.while.do", true);
            pn.setWhilePosition(ts.tokenBeg - pos);
            ConditionData data = condition();
            pn.setCondition(data.condition);
            pn.setParens(data.lp - pos, data.rp - pos);
            end = getNodeEnd(body);
            restoreRelativeLoopPosition(pn);
            pn.setBody(body);
        } finally {
            exitLoop();
        }
        // Always auto-insert semicolon to follow SpiderMonkey:
        // It is required by ECMAScript but is ignored by the rest of
        // world, see bug 238945
        if (matchToken(Token.SEMI, true)) {
            end = ts.tokenEnd;
        }
        pn.setLength(end - pos);
        return pn;
    }

    private int peekUntilNonComment(int tt) throws IOException {
        while (tt == Token.COMMENT) {
            consumeToken();
            tt = peekToken();
        }
        return tt;
    }

    private AstNode getNextStatementAfterInlineComments(AstNode pn) throws IOException {
        AstNode body = statement();
        if (Token.COMMENT == body.getType()) {
            AstNode commentNode = body;
            body = statement();
            if (pn != null) {
                pn.setInlineComment(commentNode);
            } else {
                body.setInlineComment(commentNode);
            }
        }
        return body;
    }

    private Loop forLoop() throws IOException {
        if (currentToken != Token.FOR) codeBug();
        consumeToken();
        int forPos = ts.tokenBeg, lineno = lineNumber(), column = columnNumber();
        boolean isForEach = false, isForIn = false, isForOf = false;
        int eachPos = -1, inPos = -1, lp = -1, rp = -1;
        AstNode init = null; // init is also foo in 'foo in object'
        AstNode cond = null; // cond is also object in 'foo in object'
        AstNode incr = null;
        Loop pn = null;

        Scope tempScope = new Scope();
        pushScope(tempScope); // decide below what AST class to use
        try {
            // See if this is a for each () instead of just a for ()
            if (matchToken(Token.NAME, true)) {
                if ("each".equals(ts.getString())) {
                    isForEach = true;
                    eachPos = ts.tokenBeg - forPos;
                } else {
                    reportError("msg.no.paren.for");
                }
            }

            if (mustMatchToken(Token.LP, "msg.no.paren.for", true)) lp = ts.tokenBeg - forPos;
            int tt = peekToken();

            init = forLoopInit(tt);
            if (matchToken(Token.IN, true)) {
                isForIn = true;
                inPos = ts.tokenBeg - forPos;
                markDestructuring(init);
                cond = expr(false); // object over which we're iterating
            } else if (compilerEnv.getLanguageVersion() >= Context.VERSION_ES6
                    && matchToken(Token.NAME, true)
                    && "of".equals(ts.getString())) {
                isForOf = true;
                inPos = ts.tokenBeg - forPos;
                markDestructuring(init);
                cond = expr(false); // object over which we're iterating
            } else { // ordinary for-loop
                mustMatchToken(Token.SEMI, "msg.no.semi.for", true);
                if (peekToken() == Token.SEMI) {
                    // no loop condition
                    cond = new EmptyExpression(ts.tokenBeg, 1);
                    // We haven't consumed the token, so we need the CURRENT lexer position
                    cond.setLineColumnNumber(ts.getLineno(), ts.getTokenColumn());
                } else {
                    cond = expr(false);
                }

                mustMatchToken(Token.SEMI, "msg.no.semi.for.cond", true);
                int tmpPos = ts.tokenEnd;
                if (peekToken() == Token.RP) {
                    incr = new EmptyExpression(tmpPos, 1);
                    // We haven't consumed the token, so we need the CURRENT lexer position
                    incr.setLineColumnNumber(ts.getLineno(), ts.getTokenColumn());
                } else {
                    incr = expr(false);
                }
            }

            if (mustMatchToken(Token.RP, "msg.no.paren.for.ctrl", true)) rp = ts.tokenBeg - forPos;

            if (isForIn || isForOf) {
                ForInLoop fis = new ForInLoop(forPos);
                if (init instanceof VariableDeclaration) {
                    // check that there was only one variable given
                    if (((VariableDeclaration) init).getVariables().size() > 1) {
                        reportError("msg.mult.index");
                    }
                }
                if (isForOf && isForEach) {
                    reportError("msg.invalid.for.each");
                }
                fis.setIterator(init);
                fis.setIteratedObject(cond);
                fis.setInPosition(inPos);
                fis.setIsForEach(isForEach);
                fis.setEachPosition(eachPos);
                fis.setIsForOf(isForOf);
                pn = fis;
            } else {
                ForLoop fl = new ForLoop(forPos);
                fl.setInitializer(init);
                fl.setCondition(cond);
                fl.setIncrement(incr);
                pn = fl;
            }

            // replace temp scope with the new loop object
            currentScope.replaceWith(pn);
            popScope();

            // We have to parse the body -after- creating the loop node,
            // so that the loop node appears in the loopSet, allowing
            // break/continue statements to find the enclosing loop.
            enterLoop(pn);
            try {
                AstNode body = getNextStatementAfterInlineComments(pn);
                pn.setLength(getNodeEnd(body) - forPos);
                restoreRelativeLoopPosition(pn);
                pn.setBody(body);
            } finally {
                exitLoop();
            }

        } finally {
            if (currentScope == tempScope) {
                popScope();
            }
        }
        pn.setParens(lp, rp);
        pn.setLineColumnNumber(lineno, column);
        return pn;
    }

    private AstNode forLoopInit(int tt) throws IOException {
        try {
            inForInit = true; // checked by variables() and relExpr()
            AstNode init = null;
            if (tt == Token.SEMI) {
                init = new EmptyExpression(ts.tokenBeg, 1);
                // We haven't consumed the token, so we need the CURRENT lexer position
                init.setLineColumnNumber(ts.getLineno(), ts.getTokenColumn());
            } else if (tt == Token.VAR || tt == Token.LET) {
                consumeToken();
                init = variables(tt, ts.tokenBeg, false);
            } else {
                init = expr(false);
            }
            return init;
        } finally {
            inForInit = false;
        }
    }

    private TryStatement tryStatement() throws IOException {
        if (currentToken != Token.TRY) codeBug();
        consumeToken();

        // Pull out JSDoc info and reset it before recursing.
        Comment jsdocNode = getAndResetJsDoc();

        int tryPos = ts.tokenBeg, lineno = lineNumber(), column = columnNumber(), finallyPos = -1;

        TryStatement pn = new TryStatement(tryPos);
        // Hnadled comment here because there should not be try without LC
        int lctt = peekToken();
        while (lctt == Token.COMMENT) {
            Comment commentNode = scannedComments.get(scannedComments.size() - 1);
            pn.setInlineComment(commentNode);
            consumeToken();
            lctt = peekToken();
        }
        if (lctt != Token.LC) {
            reportError("msg.no.brace.try");
        }
        AstNode tryBlock = getNextStatementAfterInlineComments(pn);
        int tryEnd = getNodeEnd(tryBlock);

        List<CatchClause> clauses = null;

        boolean sawDefaultCatch = false;
        int peek = peekToken();
        while (peek == Token.COMMENT) {
            Comment commentNode = scannedComments.get(scannedComments.size() - 1);
            pn.setInlineComment(commentNode);
            consumeToken();
            peek = peekToken();
        }
        if (peek == Token.CATCH) {
            while (matchToken(Token.CATCH, true)) {
                int catchLineNum = lineNumber();
                if (sawDefaultCatch) {
                    reportError("msg.catch.unreachable");
                }
                int catchPos = ts.tokenBeg,
                        lp = -1,
                        rp = -1,
                        guardPos = -1,
                        catchLine = lineNumber(),
                        catchColumn = columnNumber();
                Name varName = null;
                AstNode catchCond = null;

                switch (peekToken()) {
                    case Token.LP:
                        {
                            matchToken(Token.LP, true);
                            lp = ts.tokenBeg;
                            mustMatchToken(Token.NAME, "msg.bad.catchcond", true);

                            varName = createNameNode();
                            Comment jsdocNodeForName = getAndResetJsDoc();
                            if (jsdocNodeForName != null) {
                                varName.setJsDocNode(jsdocNodeForName);
                            }
                            String varNameString = varName.getIdentifier();
                            if (inUseStrictDirective) {
                                if ("eval".equals(varNameString)
                                        || "arguments".equals(varNameString)) {
                                    reportError("msg.bad.id.strict", varNameString);
                                }
                            }

                            if (matchToken(Token.IF, true)) {
                                guardPos = ts.tokenBeg;
                                catchCond = expr(false);
                            } else {
                                sawDefaultCatch = true;
                            }

                            if (mustMatchToken(Token.RP, "msg.bad.catchcond", true)) {
                                rp = ts.tokenBeg;
                            }
                            mustMatchToken(Token.LC, "msg.no.brace.catchblock", true);
                        }
                        break;
                    case Token.LC:
                        if (compilerEnv.getLanguageVersion() >= Context.VERSION_ES6) {
                            matchToken(Token.LC, true);
                        } else {
                            reportError("msg.no.paren.catch");
                        }
                        break;
                    default:
                        reportError("msg.no.paren.catch");
                        break;
                }

                Scope catchScope = new Scope(catchPos);
                CatchClause catchNode = new CatchClause(catchPos);
                catchNode.setLineColumnNumber(catchLine, catchColumn);
                pushScope(catchScope);
                try {
                    statements(catchScope);
                } finally {
                    popScope();
                }

                tryEnd = getNodeEnd(catchScope);
                catchNode.setVarName(varName);
                catchNode.setCatchCondition(catchCond);
                catchNode.setBody(catchScope);
                if (guardPos != -1) {
                    catchNode.setIfPosition(guardPos - catchPos);
                }
                catchNode.setParens(lp, rp);

                if (mustMatchToken(Token.RC, "msg.no.brace.after.body", true)) tryEnd = ts.tokenEnd;
                catchNode.setLength(tryEnd - catchPos);
                if (clauses == null) clauses = new ArrayList<>();
                clauses.add(catchNode);
            }
        } else if (peek != Token.FINALLY) {
            mustMatchToken(Token.FINALLY, "msg.try.no.catchfinally", true);
        }

        AstNode finallyBlock = null;
        if (matchToken(Token.FINALLY, true)) {
            finallyPos = ts.tokenBeg;
            finallyBlock = statement();
            tryEnd = getNodeEnd(finallyBlock);
        }

        pn.setLength(tryEnd - tryPos);
        pn.setTryBlock(tryBlock);
        pn.setCatchClauses(clauses);
        pn.setFinallyBlock(finallyBlock);
        if (finallyPos != -1) {
            pn.setFinallyPosition(finallyPos - tryPos);
        }
        pn.setLineColumnNumber(lineno, column);

        if (jsdocNode != null) {
            pn.setJsDocNode(jsdocNode);
        }

        return pn;
    }

    private ThrowStatement throwStatement() throws IOException {
        if (currentToken != Token.THROW) codeBug();
        consumeToken();
        int pos = ts.tokenBeg, lineno = lineNumber(), column = columnNumber();
        if (peekTokenOrEOL() == Token.EOL) {
            // ECMAScript does not allow new lines before throw expression,
            // see bug 256617
            reportError("msg.bad.throw.eol");
        }
        AstNode expr = expr(false);
        ThrowStatement pn = new ThrowStatement(pos, expr);
        pn.setLineColumnNumber(lineno, column);
        return pn;
    }

    // If we match a NAME, consume the token and return the statement
    // with that label.  If the name does not match an existing label,
    // reports an error.  Returns the labeled statement node, or null if
    // the peeked token was not a name.  Side effect:  sets scanner token
    // information for the label identifier (tokenBeg, tokenEnd, etc.)

    private LabeledStatement matchJumpLabelName() throws IOException {
        LabeledStatement label = null;

        if (peekTokenOrEOL() == Token.NAME) {
            consumeToken();
            if (labelSet != null) {
                label = labelSet.get(ts.getString());
            }
            if (label == null) {
                reportError("msg.undef.label");
            }
        }

        return label;
    }

    private BreakStatement breakStatement() throws IOException {
        if (currentToken != Token.BREAK) codeBug();
        consumeToken();
        int lineno = lineNumber(), pos = ts.tokenBeg, end = ts.tokenEnd, column = columnNumber();
        Name breakLabel = null;
        if (peekTokenOrEOL() == Token.NAME) {
            breakLabel = createNameNode();
            end = getNodeEnd(breakLabel);
        }

        // matchJumpLabelName only matches if there is one
        LabeledStatement labels = matchJumpLabelName();
        // always use first label as target
        Jump breakTarget = labels == null ? null : labels.getFirstLabel();

        if (breakTarget == null && breakLabel == null) {
            if (loopAndSwitchSet == null || loopAndSwitchSet.size() == 0) {
                reportError("msg.bad.break", pos, end - pos);
            } else {
                breakTarget = loopAndSwitchSet.get(loopAndSwitchSet.size() - 1);
            }
        }

        if (breakLabel != null) {
            breakLabel.setLineColumnNumber(lineNumber(), columnNumber());
        }

        BreakStatement pn = new BreakStatement(pos, end - pos);
        pn.setBreakLabel(breakLabel);
        // can be null if it's a bad break in error-recovery mode
        if (breakTarget != null) pn.setBreakTarget(breakTarget);
        pn.setLineColumnNumber(lineno, column);
        return pn;
    }

    private ContinueStatement continueStatement() throws IOException {
        if (currentToken != Token.CONTINUE) codeBug();
        consumeToken();
        int lineno = lineNumber(), pos = ts.tokenBeg, end = ts.tokenEnd, column = columnNumber();
        Name label = null;
        if (peekTokenOrEOL() == Token.NAME) {
            label = createNameNode();
            end = getNodeEnd(label);
        }

        // matchJumpLabelName only matches if there is one
        LabeledStatement labels = matchJumpLabelName();
        Loop target = null;
        if (labels == null && label == null) {
            if (loopSet == null || loopSet.size() == 0) {
                reportError("msg.continue.outside");
            } else {
                target = loopSet.get(loopSet.size() - 1);
            }
        } else {
            if (labels == null || !(labels.getStatement() instanceof Loop)) {
                reportError("msg.continue.nonloop", pos, end - pos);
            }
            target = labels == null ? null : (Loop) labels.getStatement();
        }

        if (label != null) {
            label.setLineColumnNumber(lineNumber(), columnNumber());
        }

        ContinueStatement pn = new ContinueStatement(pos, end - pos);
        if (target != null) // can be null in error-recovery mode
        pn.setTarget(target);
        pn.setLabel(label);
        pn.setLineColumnNumber(lineno, column);
        return pn;
    }

    private WithStatement withStatement() throws IOException {
        if (currentToken != Token.WITH) codeBug();
        consumeToken();

        Comment withComment = getAndResetJsDoc();

        int lineno = lineNumber(), column = columnNumber(), pos = ts.tokenBeg, lp = -1, rp = -1;
        if (mustMatchToken(Token.LP, "msg.no.paren.with", true)) lp = ts.tokenBeg;

        AstNode obj = expr(false);

        if (mustMatchToken(Token.RP, "msg.no.paren.after.with", true)) rp = ts.tokenBeg;

        WithStatement pn = new WithStatement(pos);
        AstNode body = getNextStatementAfterInlineComments(pn);
        pn.setLength(getNodeEnd(body) - pos);
        pn.setJsDocNode(withComment);
        pn.setExpression(obj);
        pn.setStatement(body);
        pn.setParens(lp, rp);
        pn.setLineColumnNumber(lineno, column);
        return pn;
    }

    private AstNode letStatement() throws IOException {
        if (currentToken != Token.LET) codeBug();
        consumeToken();
        int lineno = lineNumber(), pos = ts.tokenBeg, column = columnNumber();
        AstNode pn;
        if (peekToken() == Token.LP) {
            pn = let(true, pos);
        } else {
            pn = variables(Token.LET, pos, true); // else, e.g.: let x=6, y=7;
        }
        pn.setLineColumnNumber(lineno, column);
        return pn;
    }

    /**
     * Returns whether or not the bits in the mask have changed to all set.
     *
     * @param before bits before change
     * @param after bits after change
     * @param mask mask for bits
     * @return {@code true} if all the bits in the mask are set in "after" but not in "before"
     */
    private static final boolean nowAllSet(int before, int after, int mask) {
        return ((before & mask) != mask) && ((after & mask) == mask);
    }

    private AstNode returnOrYield(int tt, boolean exprContext) throws IOException {
        if (!insideFunctionBody()) {
            reportError(tt == Token.RETURN ? "msg.bad.return" : "msg.bad.yield");
        }
        consumeToken();
        int lineno = lineNumber(), column = columnNumber(), pos = ts.tokenBeg, end = ts.tokenEnd;

        boolean yieldStar = false;
        if ((tt == Token.YIELD)
                && (compilerEnv.getLanguageVersion() >= Context.VERSION_ES6)
                && (peekToken() == Token.MUL)) {
            yieldStar = true;
            consumeToken();
        }

        AstNode e = null;
        // This is ugly, but we don't want to require a semicolon.
        switch (peekTokenOrEOL()) {
            case Token.SEMI:
            case Token.RC:
            case Token.RB:
            case Token.RP:
            case Token.EOF:
            case Token.EOL:
            case Token.ERROR:
                break;
            case Token.YIELD:
                if (compilerEnv.getLanguageVersion() < Context.VERSION_ES6) {
                    // Take extra care to preserve language compatibility
                    break;
                }
            // fallthrough
            default:
                e = expr(false);
                end = getNodeEnd(e);
        }

        int before = endFlags;
        AstNode ret;

        if (tt == Token.RETURN) {
            endFlags |= e == null ? Node.END_RETURNS : Node.END_RETURNS_VALUE;
            ret = new ReturnStatement(pos, end - pos, e);

            // see if we need a strict mode warning
            if (nowAllSet(before, endFlags, Node.END_RETURNS | Node.END_RETURNS_VALUE))
                addStrictWarning("msg.return.inconsistent", "", pos, end - pos);
        } else {
            if (!insideFunctionBody()) reportError("msg.bad.yield");
            endFlags |= Node.END_YIELDS;
            ret = new Yield(pos, end - pos, e, yieldStar);
            setRequiresActivation();
            setIsGenerator();
            if (!exprContext) {
                ret.setLineColumnNumber(lineno, column);
                ret = new ExpressionStatement(ret);
            }
        }

        // see if we are mixing yields and value returns.
        if (insideFunctionBody()
                && nowAllSet(before, endFlags, Node.END_YIELDS | Node.END_RETURNS_VALUE)) {
            FunctionNode fn = (FunctionNode) currentScriptOrFn;
            if (!fn.isES6Generator()) {
                Name name = ((FunctionNode) currentScriptOrFn).getFunctionName();
                if (name == null || name.length() == 0) {
                    addError("msg.anon.generator.returns", "");
                } else {
                    addError("msg.generator.returns", name.getIdentifier());
                }
            }
        }

        ret.setLineColumnNumber(lineno, column);
        return ret;
    }

    private AstNode block() throws IOException {
        if (currentToken != Token.LC) codeBug();
        consumeToken();
        int pos = ts.tokenBeg;
        Scope block = new Scope(pos);
        block.setLineColumnNumber(lineNumber(), columnNumber());
        pushScope(block);
        try {
            statements(block);
            mustMatchToken(Token.RC, "msg.no.brace.block", true);
            block.setLength(ts.tokenEnd - pos);
            return block;
        } finally {
            popScope();
        }
    }

    private AstNode defaultXmlNamespace() throws IOException {
        if (currentToken != Token.DEFAULT) codeBug();
        consumeToken();
        mustHaveXML();
        setRequiresActivation();
        int lineno = lineNumber(), column = columnNumber(), pos = ts.tokenBeg;

        if (!(matchToken(Token.NAME, true) && "xml".equals(ts.getString()))) {
            reportError("msg.bad.namespace");
        }
        if (!(matchToken(Token.NAME, true) && "namespace".equals(ts.getString()))) {
            reportError("msg.bad.namespace");
        }
        if (!matchToken(Token.ASSIGN, true)) {
            reportError("msg.bad.namespace");
        }

        AstNode e = expr(false);
        UnaryExpression dxmln = new UnaryExpression(pos, getNodeEnd(e) - pos);
        dxmln.setOperator(Token.DEFAULTNAMESPACE);
        dxmln.setOperand(e);
        dxmln.setLineColumnNumber(lineno, column);

        ExpressionStatement es = new ExpressionStatement(dxmln, true);
        return es;
    }

    private void recordLabel(Label label, LabeledStatement bundle) throws IOException {
        // current token should be colon that primaryExpr left untouched
        if (peekToken() != Token.COLON) codeBug();
        consumeToken();
        String name = label.getName();
        if (labelSet == null) {
            labelSet = new HashMap<>();
        } else {
            LabeledStatement ls = labelSet.get(name);
            if (ls != null) {
                if (compilerEnv.isIdeMode()) {
                    Label dup = ls.getLabelByName(name);
                    reportError("msg.dup.label", dup.getAbsolutePosition(), dup.getLength());
                }
                reportError("msg.dup.label", label.getPosition(), label.getLength());
            }
        }
        bundle.addLabel(label);
        labelSet.put(name, bundle);
    }

    /**
     * Found a name in a statement context. If it's a label, we gather up any following labels and
     * the next non-label statement into a {@link LabeledStatement} "bundle" and return that.
     * Otherwise we parse an expression and return it wrapped in an {@link ExpressionStatement}.
     */
    private AstNode nameOrLabel() throws IOException {
        if (currentToken != Token.NAME) throw codeBug();
        int pos = ts.tokenBeg;

        // set check for label and call down to primaryExpr
        currentFlaggedToken |= TI_CHECK_LABEL;
        AstNode expr = expr(false);

        if (expr.getType() != Token.LABEL) {
            AstNode n = new ExpressionStatement(expr, !insideFunctionBody());
            n.setLineColumnNumber(expr.getLineno(), expr.getColumn());
            return n;
        }

        LabeledStatement bundle = new LabeledStatement(pos);
        recordLabel((Label) expr, bundle);
        bundle.setLineColumnNumber(expr.getLineno(), expr.getColumn());
        // look for more labels
        AstNode stmt = null;
        while (peekToken() == Token.NAME) {
            currentFlaggedToken |= TI_CHECK_LABEL;
            expr = expr(false);
            if (expr.getType() != Token.LABEL) {
                stmt = new ExpressionStatement(expr, !insideFunctionBody());
                autoInsertSemicolon(stmt);
                break;
            }
            recordLabel((Label) expr, bundle);
        }

        // no more labels; now parse the labeled statement
        try {
            currentLabel = bundle;
            if (stmt == null) {
                stmt = statementHelper();
                int ntt = peekToken();
                if (ntt == Token.COMMENT
                        && stmt.getLineno()
                                == scannedComments.get(scannedComments.size() - 1).getLineno()) {
                    stmt.setInlineComment(scannedComments.get(scannedComments.size() - 1));
                    consumeToken();
                }
            }
        } finally {
            currentLabel = null;
            // remove the labels for this statement from the global set
            for (Label lb : bundle.getLabels()) {
                labelSet.remove(lb.getName());
            }
        }

        // If stmt has parent assigned its position already is relative
        // (See bug #710225)
        bundle.setLength(stmt.getParent() == null ? getNodeEnd(stmt) - pos : getNodeEnd(stmt));
        bundle.setStatement(stmt);
        return bundle;
    }

    /**
     * Parse a 'var' or 'const' statement, or a 'var' init list in a for statement.
     *
     * @param declType A token value: either VAR, CONST, or LET depending on context.
     * @param pos the position where the node should start. It's sometimes the var/const/let
     *     keyword, and other times the beginning of the first token in the first variable
     *     declaration.
     * @return the parsed variable list
     */
    private VariableDeclaration variables(int declType, int pos, boolean isStatement)
            throws IOException {
        int end;
        VariableDeclaration pn = new VariableDeclaration(pos);
        pn.setType(declType);
        pn.setLineColumnNumber(lineNumber(), columnNumber());
        Comment varjsdocNode = getAndResetJsDoc();
        if (varjsdocNode != null) {
            pn.setJsDocNode(varjsdocNode);
        }
        // Example:
        // var foo = {a: 1, b: 2}, bar = [3, 4];
        // var {b: s2, a: s1} = foo, x = 6, y, [s3, s4] = bar;
        for (; ; ) {
            AstNode destructuring = null;
            Name name = null;
            int tt = peekToken(), kidPos = ts.tokenBeg;
            end = ts.tokenEnd;

            if (tt == Token.LB || tt == Token.LC) {
                // Destructuring assignment, e.g., var [a,b] = ...

                // TODO: support default values inside destructured assignment
                // eg: for (let { x = 3 } = {}) ...
                destructuring = destructuringPrimaryExpr();
                end = getNodeEnd(destructuring);

                if (!(destructuring instanceof DestructuringForm))
                    reportError("msg.bad.assign.left", kidPos, end - kidPos);
                markDestructuring(destructuring);
            } else {
                // Simple variable name
                mustMatchToken(Token.NAME, "msg.bad.var", true);
                name = createNameNode();
                name.setLineColumnNumber(lineNumber(), columnNumber());
                if (inUseStrictDirective) {
                    String id = ts.getString();
                    if ("eval".equals(id) || "arguments".equals(ts.getString())) {
                        reportError("msg.bad.id.strict", id);
                    }
                }
                defineSymbol(declType, ts.getString(), inForInit);
            }

            int lineno = lineNumber(), column = columnNumber();

            Comment jsdocNode = getAndResetJsDoc();

            AstNode init = null;
            if (matchToken(Token.ASSIGN, true)) {
                init = assignExpr();
                end = getNodeEnd(init);
            }

            VariableInitializer vi = new VariableInitializer(kidPos, end - kidPos);
            if (destructuring != null) {
                if (init == null && !inForInit) {
                    reportError("msg.destruct.assign.no.init");
                }
                vi.setTarget(destructuring);
            } else {
                vi.setTarget(name);
            }
            vi.setInitializer(init);
            vi.setType(declType);
            vi.setJsDocNode(jsdocNode);
            vi.setLineColumnNumber(lineno, column);
            pn.addVariable(vi);

            if (!matchToken(Token.COMMA, true)) break;
        }
        pn.setLength(end - pos);
        pn.setIsStatement(isStatement);
        return pn;
    }

    // have to pass in 'let' kwd position to compute kid offsets properly
    private AstNode let(boolean isStatement, int pos) throws IOException {
        LetNode pn = new LetNode(pos);
        pn.setLineColumnNumber(lineNumber(), columnNumber());
        if (mustMatchToken(Token.LP, "msg.no.paren.after.let", true)) pn.setLp(ts.tokenBeg - pos);
        pushScope(pn);
        try {
            VariableDeclaration vars = variables(Token.LET, ts.tokenBeg, isStatement);
            pn.setVariables(vars);
            if (mustMatchToken(Token.RP, "msg.no.paren.let", true)) {
                pn.setRp(ts.tokenBeg - pos);
            }
            if (isStatement && peekToken() == Token.LC) {
                // let statement
                consumeToken();
                int beg = ts.tokenBeg; // position stmt at LC
                AstNode stmt = statements();
                mustMatchToken(Token.RC, "msg.no.curly.let", true);
                stmt.setLength(ts.tokenEnd - beg);
                pn.setLength(ts.tokenEnd - pos);
                pn.setBody(stmt);
                pn.setType(Token.LET);
            } else {
                // let expression
                AstNode expr = expr(false);
                pn.setLength(getNodeEnd(expr) - pos);
                pn.setBody(expr);
                if (isStatement) {
                    // let expression in statement context
                    ExpressionStatement es = new ExpressionStatement(pn, !insideFunctionBody());
                    es.setLineColumnNumber(pn.getLineno(), pn.getColumn());
                    return es;
                }
            }
        } finally {
            popScope();
        }
        return pn;
    }

    void defineSymbol(int declType, String name) {
        defineSymbol(declType, name, false);
    }

    void defineSymbol(int declType, String name, boolean ignoreNotInBlock) {
        if (name == null) {
            if (compilerEnv.isIdeMode()) { // be robust in IDE-mode
                return;
            }
            codeBug();
        }
        Scope definingScope = currentScope.getDefiningScope(name);
        Symbol symbol = definingScope != null ? definingScope.getSymbol(name) : null;
        int symDeclType = symbol != null ? symbol.getDeclType() : -1;
        if (symbol != null
                && (symDeclType == Token.CONST
                        || declType == Token.CONST
                        || (definingScope == currentScope && symDeclType == Token.LET))) {
            addError(
                    symDeclType == Token.CONST
                            ? "msg.const.redecl"
                            : symDeclType == Token.LET
                                    ? "msg.let.redecl"
                                    : symDeclType == Token.VAR
                                            ? "msg.var.redecl"
                                            : symDeclType == Token.FUNCTION
                                                    ? "msg.fn.redecl"
                                                    : "msg.parm.redecl",
                    name);
            return;
        }
        switch (declType) {
            case Token.LET:
                if (!ignoreNotInBlock
                        && ((currentScope.getType() == Token.IF) || currentScope instanceof Loop)) {
                    addError("msg.let.decl.not.in.block");
                    return;
                }
                currentScope.putSymbol(new Symbol(declType, name));
                return;

            case Token.VAR:
            case Token.CONST:
            case Token.FUNCTION:
                if (symbol != null) {
                    if (symDeclType == Token.VAR) addStrictWarning("msg.var.redecl", name);
                    else if (symDeclType == Token.LP) {
                        addStrictWarning("msg.var.hides.arg", name);
                    }
                } else {
                    currentScriptOrFn.putSymbol(new Symbol(declType, name));
                }
                return;

            case Token.LP:
                if (symbol != null) {
                    // must be duplicate parameter. Second parameter hides the
                    // first, so go ahead and add the second parameter
                    addWarning("msg.dup.parms", name);
                }
                currentScriptOrFn.putSymbol(new Symbol(declType, name));
                return;

            default:
                throw codeBug();
        }
    }

    private AstNode expr(boolean allowTrailingComma) throws IOException {
        AstNode pn = assignExpr();
        int pos = pn.getPosition();
        while (matchToken(Token.COMMA, true)) {
            int opPos = ts.tokenBeg;
            if (compilerEnv.isStrictMode() && !pn.hasSideEffects())
                addStrictWarning("msg.no.side.effects", "", pos, nodeEnd(pn) - pos);
            if (peekToken() == Token.YIELD) reportError("msg.yield.parenthesized");
            if (allowTrailingComma && peekToken() == Token.RP) {
                pn.putIntProp(Node.TRAILING_COMMA, 1);
                return pn;
            }
            pn = new InfixExpression(Token.COMMA, pn, assignExpr(), opPos);
        }
        return pn;
    }

    private AstNode assignExpr() throws IOException {
        int tt = peekToken();
        if (tt == Token.YIELD) {
            return returnOrYield(tt, true);
        }

        // Intentionally not calling lineNumber/columnNumber here!
        // We have not consumed any token yet, so the position would be invalid
        int startLine = ts.lineno, startColumn = ts.getTokenColumn();

        AstNode pn = condExpr();
        boolean hasEOL = false;
        tt = peekTokenOrEOL();
        if (tt == Token.EOL) {
            hasEOL = true;
            tt = peekToken();
        }
        if (Token.FIRST_ASSIGN <= tt && tt <= Token.LAST_ASSIGN) {
            consumeToken();

            // Pull out JSDoc info and reset it before recursing.
            Comment jsdocNode = getAndResetJsDoc();

            markDestructuring(pn);
            int opPos = ts.tokenBeg;
            if (isNotValidSimpleAssignmentTarget(pn))
                reportError("msg.syntax.invalid.assignment.lhs");

            pn = new Assignment(tt, pn, assignExpr(), opPos);

            if (jsdocNode != null) {
                pn.setJsDocNode(jsdocNode);
            }
        } else if (tt == Token.SEMI) {
            // This may be dead code added intentionally, for JSDoc purposes.
            // For example: /** @type Number */ C.prototype.x;
            if (currentJsDocComment != null) {
                pn.setJsDocNode(getAndResetJsDoc());
            }
        } else if (!hasEOL && tt == Token.ARROW) {
            consumeToken();
            pn = arrowFunction(pn, startLine, startColumn);
        } else if (pn.getIntProp(Node.OBJECT_LITERAL_DESTRUCTURING, 0) == 1
                && !inDestructuringAssignment) {
            reportError("msg.syntax");
        }
        return pn;
    }

    private static boolean isNotValidSimpleAssignmentTarget(AstNode pn) {
        if (pn.getType() == Token.GETPROP)
            return isNotValidSimpleAssignmentTarget(((PropertyGet) pn).getLeft());
        return pn.getType() == Token.QUESTION_DOT;
    }

    private AstNode condExpr() throws IOException {
        AstNode pn = nullishCoalescingExpr();
        if (matchToken(Token.HOOK, true)) {
            int qmarkPos = ts.tokenBeg, colonPos = -1;
            /*
             * Always accept the 'in' operator in the middle clause of a ternary,
             * where it's unambiguous, even if we might be parsing the init of a
             * for statement.
             */
            boolean wasInForInit = inForInit;
            inForInit = false;
            AstNode ifTrue;
            try {
                ifTrue = assignExpr();
            } finally {
                inForInit = wasInForInit;
            }
            if (mustMatchToken(Token.COLON, "msg.no.colon.cond", true)) colonPos = ts.tokenBeg;
            AstNode ifFalse = assignExpr();
            int beg = pn.getPosition(), len = getNodeEnd(ifFalse) - beg;
            ConditionalExpression ce = new ConditionalExpression(beg, len);
            ce.setLineColumnNumber(pn.getLineno(), pn.getColumn());
            ce.setTestExpression(pn);
            ce.setTrueExpression(ifTrue);
            ce.setFalseExpression(ifFalse);
            ce.setQuestionMarkPosition(qmarkPos - beg);
            ce.setColonPosition(colonPos - beg);
            pn = ce;
        }
        return pn;
    }

    private AstNode nullishCoalescingExpr() throws IOException {
        AstNode pn = orExpr();
        if (matchToken(Token.NULLISH_COALESCING, true)) {
            int opPos = ts.tokenBeg;
            AstNode rn = nullishCoalescingExpr();

            // Cannot immediately contain, or be contained within, an && or || operation.
            if (pn.getType() == Token.OR
                    || pn.getType() == Token.AND
                    || rn.getType() == Token.OR
                    || rn.getType() == Token.AND) {
                reportError("msg.nullish.bad.token");
            }

            pn = new InfixExpression(Token.NULLISH_COALESCING, pn, rn, opPos);
        }
        return pn;
    }

    private AstNode orExpr() throws IOException {
        AstNode pn = andExpr();
        if (matchToken(Token.OR, true)) {
            int opPos = ts.tokenBeg;
            pn = new InfixExpression(Token.OR, pn, orExpr(), opPos);
        }
        return pn;
    }

    private AstNode andExpr() throws IOException {
        AstNode pn = bitOrExpr();
        if (matchToken(Token.AND, true)) {
            int opPos = ts.tokenBeg;
            pn = new InfixExpression(Token.AND, pn, andExpr(), opPos);
        }
        return pn;
    }

    private AstNode bitOrExpr() throws IOException {
        AstNode pn = bitXorExpr();
        while (matchToken(Token.BITOR, true)) {
            int opPos = ts.tokenBeg;
            pn = new InfixExpression(Token.BITOR, pn, bitXorExpr(), opPos);
        }
        return pn;
    }

    private AstNode bitXorExpr() throws IOException {
        AstNode pn = bitAndExpr();
        while (matchToken(Token.BITXOR, true)) {
            int opPos = ts.tokenBeg;
            pn = new InfixExpression(Token.BITXOR, pn, bitAndExpr(), opPos);
        }
        return pn;
    }

    private AstNode bitAndExpr() throws IOException {
        AstNode pn = eqExpr();
        while (matchToken(Token.BITAND, true)) {
            int opPos = ts.tokenBeg;
            pn = new InfixExpression(Token.BITAND, pn, eqExpr(), opPos);
        }
        return pn;
    }

    private AstNode eqExpr() throws IOException {
        AstNode pn = relExpr();
        for (; ; ) {
            int tt = peekToken(), opPos = ts.tokenBeg;
            switch (tt) {
                case Token.EQ:
                case Token.NE:
                case Token.SHEQ:
                case Token.SHNE:
                    consumeToken();
                    int parseToken = tt;
                    if (compilerEnv.getLanguageVersion() == Context.VERSION_1_2) {
                        // JavaScript 1.2 uses shallow equality for == and != .
                        if (tt == Token.EQ) parseToken = Token.SHEQ;
                        else if (tt == Token.NE) parseToken = Token.SHNE;
                    }
                    pn = new InfixExpression(parseToken, pn, relExpr(), opPos);
                    continue;
            }
            break;
        }
        return pn;
    }

    private AstNode relExpr() throws IOException {
        AstNode pn = shiftExpr();
        for (; ; ) {
            int tt = peekToken(), opPos = ts.tokenBeg;
            switch (tt) {
                case Token.IN:
                    if (inForInit) break;
                // fall through
                case Token.INSTANCEOF:
                case Token.LE:
                case Token.LT:
                case Token.GE:
                case Token.GT:
                    consumeToken();
                    pn = new InfixExpression(tt, pn, shiftExpr(), opPos);
                    continue;
            }
            break;
        }
        return pn;
    }

    private AstNode shiftExpr() throws IOException {
        AstNode pn = addExpr();
        for (; ; ) {
            int tt = peekToken(), opPos = ts.tokenBeg;
            switch (tt) {
                case Token.LSH:
                case Token.URSH:
                case Token.RSH:
                    consumeToken();
                    pn = new InfixExpression(tt, pn, addExpr(), opPos);
                    continue;
            }
            break;
        }
        return pn;
    }

    private AstNode addExpr() throws IOException {
        AstNode pn = mulExpr();
        for (; ; ) {
            int tt = peekToken(), opPos = ts.tokenBeg;
            if (tt == Token.ADD || tt == Token.SUB) {
                consumeToken();
                pn = new InfixExpression(tt, pn, mulExpr(), opPos);
                continue;
            }
            break;
        }
        return pn;
    }

    private AstNode mulExpr() throws IOException {
        AstNode pn = expExpr();
        for (; ; ) {
            int tt = peekToken(), opPos = ts.tokenBeg;
            switch (tt) {
                case Token.MUL:
                case Token.DIV:
                case Token.MOD:
                    consumeToken();
                    pn = new InfixExpression(tt, pn, expExpr(), opPos);
                    continue;
            }
            break;
        }
        return pn;
    }

    private AstNode expExpr() throws IOException {
        AstNode pn = unaryExpr();
        for (; ; ) {
            int tt = peekToken(), opPos = ts.tokenBeg;
            switch (tt) {
                case Token.EXP:
                    if (pn instanceof UnaryExpression) {
                        reportError(
                                "msg.no.unary.expr.on.left.exp",
                                AstNode.operatorToString(pn.getType()));
                        return makeErrorNode();
                    }
                    consumeToken();
                    pn = new InfixExpression(tt, pn, expExpr(), opPos);
                    continue;
            }
            break;
        }
        return pn;
    }

    private AstNode unaryExpr() throws IOException {
        AstNode node;
        int tt = peekToken();
        if (tt == Token.COMMENT) {
            consumeToken();
            tt = peekUntilNonComment(tt);
        }
        int line, column;

        switch (tt) {
            case Token.VOID:
            case Token.NOT:
            case Token.BITNOT:
            case Token.TYPEOF:
                consumeToken();
                line = lineNumber();
                column = columnNumber();
                node = new UnaryExpression(tt, ts.tokenBeg, unaryExpr());
                node.setLineColumnNumber(line, column);
                return node;

            case Token.ADD:
                consumeToken();
                line = lineNumber();
                column = columnNumber();
                // Convert to special POS token in parse tree
                node = new UnaryExpression(Token.POS, ts.tokenBeg, unaryExpr());
                node.setLineColumnNumber(line, column);
                return node;

            case Token.SUB:
                consumeToken();
                line = lineNumber();
                column = columnNumber();
                // Convert to special NEG token in parse tree
                node = new UnaryExpression(Token.NEG, ts.tokenBeg, unaryExpr());
                node.setLineColumnNumber(line, column);
                return node;

            case Token.INC:
            case Token.DEC:
                consumeToken();
                line = lineNumber();
                column = columnNumber();
                UpdateExpression expr = new UpdateExpression(tt, ts.tokenBeg, memberExpr(true));
                expr.setLineColumnNumber(line, column);
                checkBadIncDec(expr);
                return expr;

            case Token.DELPROP:
                consumeToken();
                line = lineNumber();
                column = columnNumber();
                node = new UnaryExpression(tt, ts.tokenBeg, unaryExpr());
                node.setLineColumnNumber(line, column);
                return node;

            case Token.ERROR:
                consumeToken();
                return makeErrorNode();
            case Token.LT:
                // XML stream encountered in expression.
                if (compilerEnv.isXmlAvailable()) {
                    consumeToken();
                    return memberExprTail(true, xmlInitializer());
                }
            // Fall thru to the default handling of RELOP
            // fall through

            default:
                AstNode pn = memberExpr(true);
                // Don't look across a newline boundary for a postfix incop.
                tt = peekTokenOrEOL();
                if (!(tt == Token.INC || tt == Token.DEC)) {
                    return pn;
                }
                consumeToken();
                UpdateExpression uexpr = new UpdateExpression(tt, ts.tokenBeg, pn, true);
                uexpr.setLineColumnNumber(pn.getLineno(), pn.getColumn());
                checkBadIncDec(uexpr);
                return uexpr;
        }
    }

    private AstNode xmlInitializer() throws IOException {
        if (currentToken != Token.LT) codeBug();
        int pos = ts.tokenBeg, tt = ts.getFirstXMLToken();
        if (tt != Token.XML && tt != Token.XMLEND) {
            reportError("msg.syntax");
            return makeErrorNode();
        }

        XmlLiteral pn = new XmlLiteral(pos);
        pn.setLineColumnNumber(lineNumber(), columnNumber());

        for (; ; tt = ts.getNextXMLToken()) {
            switch (tt) {
                case Token.XML:
                    pn.addFragment(new XmlString(ts.tokenBeg, ts.getString()));
                    mustMatchToken(Token.LC, "msg.syntax", true);
                    int beg = ts.tokenBeg;
                    AstNode expr =
                            (peekToken() == Token.RC)
                                    ? new EmptyExpression(beg, ts.tokenEnd - beg)
                                    : expr(false);
                    mustMatchToken(Token.RC, "msg.syntax", true);
                    XmlExpression xexpr = new XmlExpression(beg, expr);
                    xexpr.setIsXmlAttribute(ts.isXMLAttribute());
                    xexpr.setLength(ts.tokenEnd - beg);
                    pn.addFragment(xexpr);
                    break;

                case Token.XMLEND:
                    pn.addFragment(new XmlString(ts.tokenBeg, ts.getString()));
                    return pn;

                default:
                    reportError("msg.syntax");
                    return makeErrorNode();
            }
        }
    }

    private List<AstNode> argumentList() throws IOException {
        if (matchToken(Token.RP, true)) return null;

        List<AstNode> result = new ArrayList<>();
        boolean wasInForInit = inForInit;
        inForInit = false;
        try {
            do {
                if (peekToken() == Token.RP) {
                    // Quick fix to handle scenario like f1(a,); but not f1(a,b
                    break;
                }
                if (peekToken() == Token.YIELD) {
                    reportError("msg.yield.parenthesized");
                }
                AstNode en = assignExpr();
                if (peekToken() == Token.FOR) {
                    try {
                        result.add(generatorExpression(en, 0, true));
                    } catch (IOException ex) {
                        // #TODO
                    }
                } else {
                    result.add(en);
                }
            } while (matchToken(Token.COMMA, true));
        } finally {
            inForInit = wasInForInit;
        }

        mustMatchToken(Token.RP, "msg.no.paren.arg", true);
        return result;
    }

    /**
     * Parse a new-expression, or if next token isn't {@link Token#NEW}, a primary expression.
     *
     * @param allowCallSyntax passed down to {@link #memberExprTail}
     */
    private AstNode memberExpr(boolean allowCallSyntax) throws IOException {
        int tt = peekToken();
        AstNode pn;

        if (tt != Token.NEW) {
            pn = primaryExpr();
        } else {
            consumeToken();
            int pos = ts.tokenBeg, lineno = lineNumber(), column = columnNumber();
            NewExpression nx = new NewExpression(pos);

            AstNode target = memberExpr(false);
            int end = getNodeEnd(target);
            nx.setTarget(target);
            nx.setLineColumnNumber(lineno, column);

            int lp = -1;
            if (matchToken(Token.LP, true)) {
                lp = ts.tokenBeg;
                List<AstNode> args = argumentList();
                if (args != null && args.size() > ARGC_LIMIT)
                    reportError("msg.too.many.constructor.args");
                int rp = ts.tokenBeg;
                end = ts.tokenEnd;
                if (args != null) nx.setArguments(args);
                nx.setParens(lp - pos, rp - pos);
            }

            // Experimental syntax: allow an object literal to follow a new
            // expression, which will mean a kind of anonymous class built with
            // the JavaAdapter.  the object literal will be passed as an
            // additional argument to the constructor.
            if (matchToken(Token.LC, true)) {
                ObjectLiteral initializer = objectLiteral();
                end = getNodeEnd(initializer);
                nx.setInitializer(initializer);
            }
            nx.setLength(end - pos);
            pn = nx;
        }
        return memberExprTail(allowCallSyntax, pn);
    }

    /**
     * Parse any number of "(expr)", "[expr]" ".expr", "?.expr", "..expr", ".(expr)" or "?.(expr)"
     * constructs trailing the passed expression.
     *
     * @param pn the non-null parent node
     * @return the outermost (lexically last occurring) expression, which will have the passed
     *     parent node as a descendant
     */
    private AstNode memberExprTail(boolean allowCallSyntax, AstNode pn) throws IOException {
        // we no longer return null for errors, so this won't be null
        if (pn == null) codeBug();
        int pos = pn.getPosition();
        int lineno, column;
        boolean isOptionalChain = false;
        tailLoop:
        for (; ; ) {
            lineno = lineNumber();
            column = columnNumber();
            int tt = peekToken();
            switch (tt) {
                case Token.DOT:
                case Token.QUESTION_DOT:
                case Token.DOTDOT:
                    isOptionalChain |= (tt == Token.QUESTION_DOT);
                    pn = propertyAccess(tt, pn, isOptionalChain);
                    break;

                case Token.DOTQUERY:
                    consumeToken();
                    int opPos = ts.tokenBeg, rp = -1;
                    mustHaveXML();
                    setRequiresActivation();
                    AstNode filter = expr(false);
                    int end = getNodeEnd(filter);
                    if (mustMatchToken(Token.RP, "msg.no.paren", true)) {
                        rp = ts.tokenBeg;
                        end = ts.tokenEnd;
                    }
                    XmlDotQuery q = new XmlDotQuery(pos, end - pos);
                    q.setLeft(pn);
                    q.setRight(filter);
                    q.setOperatorPosition(opPos);
                    q.setRp(rp - pos);
                    q.setLineColumnNumber(lineno, column);
                    pn = q;
                    break;

                case Token.LB:
                    consumeToken();
                    pn = makeElemGet(pn, ts.tokenBeg);
                    break;

                case Token.LP:
                    if (!allowCallSyntax) {
                        break tailLoop;
                    }
                    pn = makeFunctionCall(pn, pos, isOptionalChain);
                    break;
                case Token.COMMENT:
                    // Ignoring all the comments, because previous statement may not be terminated
                    // properly.
                    int currentFlagTOken = currentFlaggedToken;
                    peekUntilNonComment(tt);
                    currentFlaggedToken =
                            (currentFlaggedToken & TI_AFTER_EOL) != 0
                                    ? currentFlaggedToken
                                    : currentFlagTOken;
                    break;
                case Token.TEMPLATE_LITERAL:
                    consumeToken();
                    pn = taggedTemplateLiteral(pn);
                    break;
                default:
                    break tailLoop;
            }
        }
        return pn;
    }

    private FunctionCall makeFunctionCall(AstNode pn, int pos, boolean isOptionalChain)
            throws IOException {
        consumeToken();
        checkCallRequiresActivation(pn);
        FunctionCall f = new FunctionCall(pos);
        f.setTarget(pn);
        f.setLp(ts.tokenBeg - pos);
        List<AstNode> args = argumentList();
        if (args != null && args.size() > ARGC_LIMIT) reportError("msg.too.many.function.args");
        f.setArguments(args);
        f.setRp(ts.tokenBeg - pos);
        f.setLength(ts.tokenEnd - pos);
        if (isOptionalChain) {
            f.markIsOptionalCall();
        }
        return f;
    }

    private AstNode taggedTemplateLiteral(AstNode pn) throws IOException {
        AstNode templateLiteral = templateLiteral(true);
        TaggedTemplateLiteral tagged = new TaggedTemplateLiteral();
        tagged.setTarget(pn);
        tagged.setTemplateLiteral(templateLiteral);
        tagged.setLineColumnNumber(pn.getLineno(), pn.getColumn());
        return tagged;
    }

    /**
     * Handles any construct following a "." or ".." operator.
     *
     * @param pn the left-hand side (target) of the operator. Never null.
     * @param isOptionalChain whether we are inside an optional chain, i.e. whether a preceding
     *     property access was done via the {@code ?.} operator
     * @return a PropertyGet, XmlMemberGet, or ErrorNode
     */
    private AstNode propertyAccess(int tt, AstNode pn, boolean isOptionalChain) throws IOException {
        if (pn == null) codeBug();
        if (pn.getType() == Token.SUPER && isOptionalChain) {
            reportError("msg.optional.super");
            return makeErrorNode();
        }

        int memberTypeFlags = 0,
                lineno = lineNumber(),
                dotPos = ts.tokenBeg,
                column = columnNumber();
        consumeToken();

        if (tt == Token.DOTDOT) {
            mustHaveXML();
            memberTypeFlags = Node.DESCENDANTS_FLAG;
        }

        if (!compilerEnv.isXmlAvailable()) {
            int maybeName = nextToken();
            if (maybeName != Token.NAME
                    && !(compilerEnv.isReservedKeywordAsIdentifier()
                            && TokenStream.isKeyword(
                                    ts.getString(),
                                    compilerEnv.getLanguageVersion(),
                                    inUseStrictDirective))) {
                reportError("msg.no.name.after.dot");
            }

            Name name = createNameNode(true, Token.GETPROP);
            PropertyGet pg = new PropertyGet(pn, name, dotPos);
            pg.setLineColumnNumber(lineno, column);
            return pg;
        }

        AstNode ref = null; // right side of . or .. operator
        int token = nextToken();
        switch (token) {
            case Token.THROW:
                // needed for generator.throw();
                saveNameTokenData(ts.tokenBeg, "throw", lineNumber(), columnNumber());
                ref = propertyName(-1, memberTypeFlags);
                break;

            case Token.NAME:
                // handles: name, ns::name, ns::*, ns::[expr]
                ref = propertyName(-1, memberTypeFlags);
                break;

            case Token.MUL:
                // handles: *, *::name, *::*, *::[expr]
                saveNameTokenData(ts.tokenBeg, "*", lineNumber(), columnNumber());
                ref = propertyName(-1, memberTypeFlags);
                break;

            case Token.XMLATTR:
                // handles: '@attr', '@ns::attr', '@ns::*', '@ns::*',
                //          '@::attr', '@::*', '@*', '@*::attr', '@*::*'
                ref = attributeAccess();
                break;

            case Token.RESERVED:
                {
                    String name = ts.getString();
                    saveNameTokenData(ts.tokenBeg, name, lineNumber(), columnNumber());
                    ref = propertyName(-1, memberTypeFlags);
                    break;
                }

            case Token.LB:
                if (tt == Token.QUESTION_DOT) {
                    // a ?.[ expr ]
                    consumeToken();
                    ElementGet g = makeElemGet(pn, ts.tokenBeg);
                    g.setType(Token.QUESTION_DOT);
                    return g;
                } else {
                    reportError("msg.no.name.after.dot");
                    return makeErrorNode();
                }

            case Token.LP:
                if (tt == Token.QUESTION_DOT) {
                    // a function call such as f?.()
                    return makeFunctionCall(pn, pn.getPosition(), isOptionalChain);
                } else {
                    reportError("msg.no.name.after.dot");
                    return makeErrorNode();
                }

            default:
                if (compilerEnv.isReservedKeywordAsIdentifier()) {
                    // allow keywords as property names, e.g. ({if: 1})
                    String name = Token.keywordToName(token);
                    if (name != null) {
                        saveNameTokenData(ts.tokenBeg, name, lineNumber(), columnNumber());
                        ref = propertyName(-1, memberTypeFlags);
                        break;
                    }
                }
                reportError("msg.no.name.after.dot");
                return makeErrorNode();
        }

        boolean xml = ref instanceof XmlRef;
        InfixExpression result = xml ? new XmlMemberGet() : new PropertyGet();
        if (xml && tt == Token.DOT) result.setType(Token.DOT);
        if (isOptionalChain) {
            result.setType(Token.QUESTION_DOT);
        }
        int pos = pn.getPosition();
        result.setPosition(pos);
        result.setLength(getNodeEnd(ref) - pos);
        result.setOperatorPosition(dotPos - pos);
        result.setLineColumnNumber(lineno, column);
        result.setLeft(pn); // do this after setting position
        result.setRight(ref);
        return result;
    }

    private ElementGet makeElemGet(AstNode pn, int lb) throws IOException {
        int pos = pn.getPosition();
        AstNode expr = expr(false);
        int end = getNodeEnd(expr);
        int rb = -1;
        if (mustMatchToken(Token.RB, "msg.no.bracket.index", true)) {
            rb = ts.tokenBeg;
            end = ts.tokenEnd;
        }
        ElementGet g = new ElementGet(pos, end - pos);
        g.setTarget(pn);
        g.setElement(expr);
        g.setParens(lb, rb);
        return g;
    }

    /**
     * Xml attribute expression:
     *
     * <p>{@code @attr}, {@code @ns::attr}, {@code @ns::*}, {@code @ns::*}, {@code @*},
     * {@code @*::attr}, {@code @*::*}, {@code @ns::[expr]}, {@code @*::[expr]}, {@code @[expr]}
     *
     * <p>Called if we peeked an '@' token.
     */
    private AstNode attributeAccess() throws IOException {
        int tt = nextToken(), atPos = ts.tokenBeg;

        switch (tt) {
            // handles: @name, @ns::name, @ns::*, @ns::[expr]
            case Token.NAME:
                return propertyName(atPos, 0);

            // handles: @*, @*::name, @*::*, @*::[expr]
            case Token.MUL:
                saveNameTokenData(ts.tokenBeg, "*", lineNumber(), columnNumber());
                return propertyName(atPos, 0);

            // handles @[expr]
            case Token.LB:
                return xmlElemRef(atPos, null, -1);

            default:
                reportError("msg.no.name.after.xmlAttr");
                return makeErrorNode();
        }
    }

    /**
     * Check if :: follows name in which case it becomes a qualified name.
     *
     * @param atPos a natural number if we just read an '@' token, else -1
     * @param memberTypeFlags flags tracking whether we're a '.' or '..' child
     * @return an XmlRef node if it's an attribute access, a child of a '..' operator, or the name
     *     is followed by ::. For a plain name, returns a Name node. Returns an ErrorNode for
     *     malformed XML expressions. (For now - might change to return a partial XmlRef.)
     */
    private AstNode propertyName(int atPos, int memberTypeFlags) throws IOException {
        int pos = atPos != -1 ? atPos : ts.tokenBeg, lineno = lineNumber(), column = columnNumber();
        int colonPos = -1;
        Name name = createNameNode(true, currentToken);
        Name ns = null;

        if (matchToken(Token.COLONCOLON, true)) {
            ns = name;
            colonPos = ts.tokenBeg;

            switch (nextToken()) {
                // handles name::name
                case Token.NAME:
                    name = createNameNode();
                    break;

                // handles name::*
                case Token.MUL:
                    saveNameTokenData(ts.tokenBeg, "*", lineNumber(), columnNumber());
                    name = createNameNode(false, -1);
                    break;

                // handles name::[expr] or *::[expr]
                case Token.LB:
                    return xmlElemRef(atPos, ns, colonPos);

                default:
                    reportError("msg.no.name.after.coloncolon");
                    return makeErrorNode();
            }
        }

        if (ns == null && memberTypeFlags == 0 && atPos == -1) {
            return name;
        }

        XmlPropRef ref = new XmlPropRef(pos, getNodeEnd(name) - pos);
        ref.setAtPos(atPos);
        ref.setNamespace(ns);
        ref.setColonPos(colonPos);
        ref.setPropName(name);
        ref.setLineColumnNumber(lineno, column);
        return ref;
    }

    /**
     * Parse the [expr] portion of an xml element reference, e.g. @[expr], @*::[expr], or
     * ns::[expr].
     */
    private XmlElemRef xmlElemRef(int atPos, Name namespace, int colonPos) throws IOException {
        int lb = ts.tokenBeg, rb = -1, pos = atPos != -1 ? atPos : lb;
        AstNode expr = expr(false);
        int end = getNodeEnd(expr);
        if (mustMatchToken(Token.RB, "msg.no.bracket.index", true)) {
            rb = ts.tokenBeg;
            end = ts.tokenEnd;
        }
        XmlElemRef ref = new XmlElemRef(pos, end - pos);
        ref.setNamespace(namespace);
        ref.setColonPos(colonPos);
        ref.setAtPos(atPos);
        ref.setExpression(expr);
        ref.setBrackets(lb, rb);
        return ref;
    }

    private AstNode destructuringAssignExpr() throws IOException, ParserException {
        try {
            inDestructuringAssignment = true;
            return assignExpr();
        } finally {
            inDestructuringAssignment = false;
        }
    }

    private AstNode destructuringPrimaryExpr() throws IOException, ParserException {
        try {
            inDestructuringAssignment = true;
            return primaryExpr();
        } finally {
            inDestructuringAssignment = false;
        }
    }

    private AstNode primaryExpr() throws IOException {
        int ttFlagged = peekFlaggedToken();
        int tt = ttFlagged & CLEAR_TI_MASK;

        switch (tt) {
            case Token.FUNCTION:
                consumeToken();
                return function(FunctionNode.FUNCTION_EXPRESSION);

            case Token.LB:
                consumeToken();
                return arrayLiteral();

            case Token.LC:
                consumeToken();
                return objectLiteral();

            case Token.LET:
                consumeToken();
                return let(false, ts.tokenBeg);

            case Token.LP:
                consumeToken();
                return parenExpr();

            case Token.XMLATTR:
                consumeToken();
                mustHaveXML();
                return attributeAccess();

            case Token.NAME:
                consumeToken();
                return name(ttFlagged, tt);

            case Token.NUMBER:
            case Token.BIGINT:
                {
                    consumeToken();
                    return createNumericLiteral(tt, false);
                }

            case Token.STRING:
                consumeToken();
                return createStringLiteral();

            case Token.DIV:
            case Token.ASSIGN_DIV:
                consumeToken();
                // Got / or /= which in this context means a regexp
                ts.readRegExp(tt);
                int pos = ts.tokenBeg, end = ts.tokenEnd;
                RegExpLiteral re = new RegExpLiteral(pos, end - pos);
                re.setValue(ts.getString());
                re.setFlags(ts.readAndClearRegExpFlags());
                re.setLineColumnNumber(lineNumber(), columnNumber());
                return re;

            case Token.NULL:
            case Token.THIS:
            case Token.FALSE:
            case Token.TRUE:
                {
                    consumeToken();
                    pos = ts.tokenBeg;
                    end = ts.tokenEnd;
                    KeywordLiteral keywordLiteral = new KeywordLiteral(pos, end - pos, tt);
                    keywordLiteral.setLineColumnNumber(lineNumber(), columnNumber());
                    return keywordLiteral;
                }

            case Token.SUPER:
                if (((insideFunctionParams() || insideFunctionBody()) && insideMethod)
                        || compilerEnv.isAllowSuper()) {
                    consumeToken();
                    pos = ts.tokenBeg;
                    end = ts.tokenEnd;
                    KeywordLiteral keywordLiteral = new KeywordLiteral(pos, end - pos, tt);
                    keywordLiteral.setLineColumnNumber(lineNumber(), columnNumber());
                    return keywordLiteral;
                } else {
                    reportError("msg.super.shorthand.function");
                }
                break;

            case Token.TEMPLATE_LITERAL:
                consumeToken();
                return templateLiteral(false);

            case Token.RESERVED:
                consumeToken();
                reportError("msg.reserved.id", ts.getString());
                break;

            case Token.ERROR:
                consumeToken();
                // the scanner or one of its subroutines reported the error.
                break;

            case Token.EOF:
                consumeToken();
                reportError("msg.unexpected.eof");
                break;

            default:
                consumeToken();
                reportError("msg.syntax");
                break;
        }
        // should only be reachable in IDE/error-recovery mode
        consumeToken();
        return makeErrorNode();
    }

    private AstNode parenExpr() throws IOException {
        boolean wasInForInit = inForInit;
        inForInit = false;
        try {
            Comment jsdocNode = getAndResetJsDoc();
            int lineno = lineNumber(), column = columnNumber();
            int begin = ts.tokenBeg;
            AstNode e = (peekToken() == Token.RP ? new EmptyExpression(begin) : expr(true));
            if (peekToken() == Token.FOR) {
                return generatorExpression(e, begin);
            }
            mustMatchToken(Token.RP, "msg.no.paren", true);

            int length = ts.tokenEnd - begin;

            boolean hasObjectLiteralDestructuring =
                    e.getIntProp(Node.OBJECT_LITERAL_DESTRUCTURING, 0) == 1;
            boolean hasTrailingComma = e.getIntProp(Node.TRAILING_COMMA, 0) == 1;
            if ((hasTrailingComma || hasObjectLiteralDestructuring || e.getType() == Token.EMPTY)
                    && peekToken() != Token.ARROW) {
                reportError("msg.syntax");
                return makeErrorNode();
            }

            ParenthesizedExpression pn = new ParenthesizedExpression(begin, length, e);
            pn.setLineColumnNumber(lineno, column);
            if (jsdocNode == null) {
                jsdocNode = getAndResetJsDoc();
            }
            if (jsdocNode != null) {
                pn.setJsDocNode(jsdocNode);
            }
            if (hasTrailingComma) {
                pn.putIntProp(Node.TRAILING_COMMA, 1);
            }
            return pn;
        } finally {
            inForInit = wasInForInit;
        }
    }

    private AstNode name(int ttFlagged, int tt) throws IOException {
        String nameString = ts.getString();
        int namePos = ts.tokenBeg, nameLineno = lineNumber(), nameColumn = columnNumber();
        if (0 != (ttFlagged & TI_CHECK_LABEL) && peekToken() == Token.COLON) {
            // Do not consume colon.  It is used as an unwind indicator
            // to return to statementHelper.
            Label label = new Label(namePos, ts.tokenEnd - namePos);
            label.setName(nameString);
            label.setLineColumnNumber(lineNumber(), columnNumber());
            return label;
        }
        // Not a label.  Unfortunately peeking the next token to check for
        // a colon has biffed ts.tokenBeg, ts.tokenEnd.  We store the name's
        // bounds in instance vars and createNameNode uses them.
        saveNameTokenData(namePos, nameString, nameLineno, nameColumn);

        if (compilerEnv.isXmlAvailable()) {
            return propertyName(-1, 0);
        }
        return createNameNode(true, Token.NAME);
    }

    /** May return an {@link ArrayLiteral} or {@link ArrayComprehension}. */
    private AstNode arrayLiteral() throws IOException {
        if (currentToken != Token.LB) codeBug();
        int pos = ts.tokenBeg, end = ts.tokenEnd, lineno = lineNumber(), column = columnNumber();
        List<AstNode> elements = new ArrayList<>();
        ArrayLiteral pn = new ArrayLiteral(pos);
        boolean after_lb_or_comma = true;
        int afterComma = -1;
        int skipCount = 0;
        for (; ; ) {
            int tt = peekToken();
            if (tt == Token.COMMA) {
                consumeToken();
                afterComma = ts.tokenEnd;
                if (!after_lb_or_comma) {
                    after_lb_or_comma = true;
                } else {
                    elements.add(new EmptyExpression(ts.tokenBeg, 1));
                    skipCount++;
                }
            } else if (tt == Token.COMMENT) {
                consumeToken();
            } else if (tt == Token.RB) {
                consumeToken();
                // for ([a,] in obj) is legal, but for ([a] in obj) is
                // not since we have both key and value supplied. The
                // trick is that [a,] and [a] are equivalent in other
                // array literal contexts. So we calculate a special
                // length value just for destructuring assignment.
                end = ts.tokenEnd;
                pn.setDestructuringLength(elements.size() + (after_lb_or_comma ? 1 : 0));
                pn.setSkipCount(skipCount);
                if (afterComma != -1) warnTrailingComma(pos, elements, afterComma);
                break;
            } else if (tt == Token.FOR && !after_lb_or_comma && elements.size() == 1) {
                return arrayComprehension(elements.get(0), pos);
            } else if (tt == Token.EOF) {
                reportError("msg.no.bracket.arg");
                break;
            } else {
                if (!after_lb_or_comma) {
                    reportError("msg.no.bracket.arg");
                }
                elements.add(assignExpr());
                after_lb_or_comma = false;
                afterComma = -1;
            }
        }
        for (AstNode e : elements) {
            pn.addElement(e);
        }
        pn.setLength(end - pos);
        pn.setLineColumnNumber(lineno, column);
        return pn;
    }

    /**
     * Parse a JavaScript 1.7 Array comprehension.
     *
     * @param result the first expression after the opening left-bracket
     * @param pos start of LB token that begins the array comprehension
     * @return the array comprehension or an error node
     */
    private AstNode arrayComprehension(AstNode result, int pos) throws IOException {
        List<ArrayComprehensionLoop> loops = new ArrayList<>();
        while (peekToken() == Token.FOR) {
            loops.add(arrayComprehensionLoop());
        }
        int ifPos = -1;
        ConditionData data = null;
        if (peekToken() == Token.IF) {
            consumeToken();
            ifPos = ts.tokenBeg - pos;
            data = condition();
        }
        mustMatchToken(Token.RB, "msg.no.bracket.arg", true);
        ArrayComprehension pn = new ArrayComprehension(pos, ts.tokenEnd - pos);
        pn.setResult(result);
        pn.setLoops(loops);
        if (data != null) {
            pn.setIfPosition(ifPos);
            pn.setFilter(data.condition);
            pn.setFilterLp(data.lp - pos);
            pn.setFilterRp(data.rp - pos);
        }
        return pn;
    }

    private ArrayComprehensionLoop arrayComprehensionLoop() throws IOException {
        if (nextToken() != Token.FOR) codeBug();
        int pos = ts.tokenBeg;
        int eachPos = -1, lp = -1, rp = -1, inPos = -1;
        boolean isForOf = false;
        ArrayComprehensionLoop pn = new ArrayComprehensionLoop(pos);

        pushScope(pn);
        try {
            if (matchToken(Token.NAME, true)) {
                if (ts.getString().equals("each")) {
                    eachPos = ts.tokenBeg - pos;
                } else {
                    reportError("msg.no.paren.for");
                }
            }
            if (mustMatchToken(Token.LP, "msg.no.paren.for", true)) {
                lp = ts.tokenBeg - pos;
            }

            AstNode iter = null;
            switch (peekToken()) {
                case Token.LB:
                case Token.LC:
                    // handle destructuring assignment
                    iter = destructuringPrimaryExpr();
                    markDestructuring(iter);
                    break;
                case Token.NAME:
                    consumeToken();
                    iter = createNameNode();
                    break;
                default:
                    reportError("msg.bad.var");
            }

            // Define as a let since we want the scope of the variable to
            // be restricted to the array comprehension
            if (iter.getType() == Token.NAME) {
                defineSymbol(Token.LET, ts.getString(), true);
            }

            switch (nextToken()) {
                case Token.IN:
                    inPos = ts.tokenBeg - pos;
                    break;
                case Token.NAME:
                    if ("of".equals(ts.getString())) {
                        if (eachPos != -1) {
                            reportError("msg.invalid.for.each");
                        }
                        inPos = ts.tokenBeg - pos;
                        isForOf = true;
                        break;
                    }
                // fall through
                default:
                    reportError("msg.in.after.for.name");
            }
            AstNode obj = expr(false);
            if (mustMatchToken(Token.RP, "msg.no.paren.for.ctrl", true)) rp = ts.tokenBeg - pos;

            pn.setLength(ts.tokenEnd - pos);
            pn.setIterator(iter);
            pn.setIteratedObject(obj);
            pn.setInPosition(inPos);
            pn.setEachPosition(eachPos);
            pn.setIsForEach(eachPos != -1);
            pn.setParens(lp, rp);
            pn.setIsForOf(isForOf);
            return pn;
        } finally {
            popScope();
        }
    }

    private AstNode generatorExpression(AstNode result, int pos) throws IOException {
        return generatorExpression(result, pos, false);
    }

    private AstNode generatorExpression(AstNode result, int pos, boolean inFunctionParams)
            throws IOException {

        List<GeneratorExpressionLoop> loops = new ArrayList<>();
        while (peekToken() == Token.FOR) {
            loops.add(generatorExpressionLoop());
        }
        int ifPos = -1;
        ConditionData data = null;
        if (peekToken() == Token.IF) {
            consumeToken();
            ifPos = ts.tokenBeg - pos;
            data = condition();
        }
        if (!inFunctionParams) {
            mustMatchToken(Token.RP, "msg.no.paren.let", true);
        }
        GeneratorExpression pn = new GeneratorExpression(pos, ts.tokenEnd - pos);
        pn.setResult(result);
        pn.setLoops(loops);
        if (data != null) {
            pn.setIfPosition(ifPos);
            pn.setFilter(data.condition);
            pn.setFilterLp(data.lp - pos);
            pn.setFilterRp(data.rp - pos);
        }
        return pn;
    }

    private GeneratorExpressionLoop generatorExpressionLoop() throws IOException {
        if (nextToken() != Token.FOR) codeBug();
        int pos = ts.tokenBeg;
        int lp = -1, rp = -1, inPos = -1;
        GeneratorExpressionLoop pn = new GeneratorExpressionLoop(pos);

        pushScope(pn);
        try {
            if (mustMatchToken(Token.LP, "msg.no.paren.for", true)) {
                lp = ts.tokenBeg - pos;
            }

            AstNode iter = null;
            switch (peekToken()) {
                case Token.LB:
                case Token.LC:
                    // handle destructuring assignment
                    iter = destructuringPrimaryExpr();
                    markDestructuring(iter);
                    break;
                case Token.NAME:
                    consumeToken();
                    iter = createNameNode();
                    break;
                default:
                    reportError("msg.bad.var");
            }

            // Define as a let since we want the scope of the variable to
            // be restricted to the array comprehension
            if (iter.getType() == Token.NAME) {
                defineSymbol(Token.LET, ts.getString(), true);
            }

            if (mustMatchToken(Token.IN, "msg.in.after.for.name", true)) inPos = ts.tokenBeg - pos;
            AstNode obj = expr(false);
            if (mustMatchToken(Token.RP, "msg.no.paren.for.ctrl", true)) rp = ts.tokenBeg - pos;

            pn.setLength(ts.tokenEnd - pos);
            pn.setIterator(iter);
            pn.setIteratedObject(obj);
            pn.setInPosition(inPos);
            pn.setParens(lp, rp);
            return pn;
        } finally {
            popScope();
        }
    }

    private static final int PROP_ENTRY = 1;
    private static final int GET_ENTRY = 2;
    private static final int SET_ENTRY = 4;
    private static final int METHOD_ENTRY = 8;

    private ObjectLiteral objectLiteral() throws IOException {
        int pos = ts.tokenBeg, lineno = lineNumber(), column = columnNumber();
        int afterComma = -1;
        List<ObjectProperty> elems = new ArrayList<>();
        Set<String> getterNames = null;
        Set<String> setterNames = null;
        if (this.inUseStrictDirective) {
            getterNames = new HashSet<>();
            setterNames = new HashSet<>();
        }
        Comment objJsdocNode = getAndResetJsDoc();
        boolean objectLiteralDestructuringDefault = false;
        commaLoop:
        for (; ; ) {
            String propertyName = null;
            int entryKind = PROP_ENTRY;
            int tt = peekToken();
            Comment jsdocNode = getAndResetJsDoc();
            if (tt == Token.COMMENT) {
                consumeToken();
                tt = peekUntilNonComment(tt);
            }
            if (tt == Token.RC) {
                if (afterComma != -1) warnTrailingComma(pos, elems, afterComma);
                break commaLoop;
            }
            AstNode pname = objliteralProperty();
            if (pname == null) {
                reportError("msg.bad.prop");
            } else {
                propertyName = ts.getString();
                int ppos = ts.tokenBeg;
                consumeToken();
                if (pname instanceof Name || pname instanceof StringLiteral) {
                    // For complicated reasons, parsing a name does not advance the token
                    pname.setLineColumnNumber(lineNumber(), columnNumber());
                } else if (pname instanceof GeneratorMethodDefinition) {
                    // Same as above
                    ((GeneratorMethodDefinition) pname)
                            .getMethodName()
                            .setLineColumnNumber(lineNumber(), columnNumber());
                }

                // This code path needs to handle both destructuring object
                // literals like:
                // var {get, b} = {get: 1, b: 2};
                // and getters like:
                // var x = {get 1() { return 2; };
                // So we check a whitelist of tokens to check if we're at the
                // first case. (Because of keywords, the second case may be
                // many tokens.)
                int peeked = peekToken();
                if (peeked != Token.COMMA && peeked != Token.COLON && peeked != Token.RC) {
                    if (peeked == Token.ASSIGN) { // we have an object literal with
                        // destructuring assignment and a default value
                        objectLiteralDestructuringDefault = true;
                        if (compilerEnv.getLanguageVersion() >= Context.VERSION_ES6) {
                            elems.add(plainProperty(pname, tt));
                            if (matchToken(Token.COMMA, true)) {
                                continue;
                            } else {
                                break commaLoop;
                            }
                        } else {
                            reportError("msg.default.args");
                        }
                    } else if (peeked == Token.LP) {
                        entryKind = METHOD_ENTRY;
                    } else if (pname.getType() == Token.NAME) {
                        if ("get".equals(propertyName)) {
                            entryKind = GET_ENTRY;
                        } else if ("set".equals(propertyName)) {
                            entryKind = SET_ENTRY;
                        }
                    }
                    if (entryKind == GET_ENTRY || entryKind == SET_ENTRY) {
                        pname = objliteralProperty();
                        if (pname == null) {
                            reportError("msg.bad.prop");
                        }
                        consumeToken();
                    }
                    if (pname == null) {
                        propertyName = null;
                    } else {
                        propertyName = ts.getString();
                        // short-hand method definition
                        ObjectProperty objectProp =
                                methodDefinition(
                                        ppos,
                                        pname,
                                        entryKind,
                                        pname instanceof GeneratorMethodDefinition);
                        pname.setJsDocNode(jsdocNode);
                        elems.add(objectProp);
                    }
                } else {
                    pname.setJsDocNode(jsdocNode);
                    elems.add(plainProperty(pname, tt));
                }
                if (pname instanceof GeneratorMethodDefinition && entryKind != METHOD_ENTRY) {
                    reportError("msg.bad.prop");
                }
            }

            if (this.inUseStrictDirective
                    && propertyName != null
                    && !(pname instanceof ComputedPropertyKey)) {
                switch (entryKind) {
                    case PROP_ENTRY:
                    case METHOD_ENTRY:
                        if (getterNames.contains(propertyName)
                                || setterNames.contains(propertyName)) {
                            addError("msg.dup.obj.lit.prop.strict", propertyName);
                        }
                        getterNames.add(propertyName);
                        setterNames.add(propertyName);
                        break;
                    case GET_ENTRY:
                        if (getterNames.contains(propertyName)) {
                            addError("msg.dup.obj.lit.prop.strict", propertyName);
                        }
                        getterNames.add(propertyName);
                        break;
                    case SET_ENTRY:
                        if (setterNames.contains(propertyName)) {
                            addError("msg.dup.obj.lit.prop.strict", propertyName);
                        }
                        setterNames.add(propertyName);
                        break;
                }
            }

            // Eat any dangling jsdoc in the property.
            getAndResetJsDoc();

            if (matchToken(Token.COMMA, true)) {
                afterComma = ts.tokenEnd;
            } else {
                break commaLoop;
            }
        }

        mustMatchToken(Token.RC, "msg.no.brace.prop", true);
        ObjectLiteral pn = new ObjectLiteral(pos, ts.tokenEnd - pos);
        if (objectLiteralDestructuringDefault) {
            pn.putIntProp(Node.OBJECT_LITERAL_DESTRUCTURING, 1);
        }
        if (objJsdocNode != null) {
            pn.setJsDocNode(objJsdocNode);
        }
        pn.setElements(elems);
        pn.setLineColumnNumber(lineno, column);
        return pn;
    }

    private AstNode objliteralProperty() throws IOException {
        AstNode pname;
        int tt = peekToken();
        switch (tt) {
            case Token.NAME:
                pname = createNameNode();
                break;

            case Token.STRING:
                pname = createStringLiteral();
                break;

            case Token.NUMBER:
            case Token.BIGINT:
                pname = createNumericLiteral(tt, true);
                break;

            case Token.LB:
                if (compilerEnv.getLanguageVersion() >= Context.VERSION_ES6) {
                    int pos = ts.tokenBeg;
                    nextToken();
                    int lineno = lineNumber();
                    int column = columnNumber();
                    AstNode expr = assignExpr();
                    if (peekToken() != Token.RB) {
                        reportError("msg.bad.prop");
                    }
                    nextToken();

                    pname = new ComputedPropertyKey(pos, ts.tokenEnd - pos);
                    pname.setLineColumnNumber(lineno, column);
                    ((ComputedPropertyKey) pname).setExpression(expr);
                } else {
                    reportError("msg.bad.prop");
                    return null;
                }
                break;

            case Token.MUL:
                if (compilerEnv.getLanguageVersion() >= Context.VERSION_ES6) {
                    int pos = ts.tokenBeg;
                    nextToken();
                    int lineno = lineNumber();
                    int column = columnNumber();
                    pname = objliteralProperty();

                    pname = new GeneratorMethodDefinition(pos, ts.tokenEnd - pos, pname);
                    pname.setLineColumnNumber(lineno, column);
                } else {
                    reportError("msg.bad.prop");
                    return null;
                }
                break;

            default:
                if (compilerEnv.isReservedKeywordAsIdentifier()
                        && TokenStream.isKeyword(
                                ts.getString(),
                                compilerEnv.getLanguageVersion(),
                                inUseStrictDirective)) {
                    // convert keyword to property name, e.g. ({if: 1})
                    pname = createNameNode();
                    break;
                }
                return null;
        }

        return pname;
    }

    private ObjectProperty plainProperty(AstNode property, int ptt) throws IOException {
        // Support, e.g., |var {x, y} = o| as destructuring shorthand
        // for |var {x: x, y: y} = o|, as implemented in spidermonkey JS 1.8.
        int tt = peekToken();
        if ((tt == Token.COMMA || tt == Token.RC)
                && ptt == Token.NAME
                && compilerEnv.getLanguageVersion() >= Context.VERSION_1_8) {
            if (!inDestructuringAssignment
                    && compilerEnv.getLanguageVersion() < Context.VERSION_ES6) {
                reportError("msg.bad.object.init");
            }
            AstNode nn = new Name(property.getPosition(), property.getString());
            ObjectProperty pn = new ObjectProperty();
            pn.setIsShorthand(true);
            pn.setLeftAndRight(property, nn);
            return pn;
        } else if (tt == Token.ASSIGN) {
            /* we're in destructuring with defaults in a object literal; treat defaults as values */
            ObjectProperty pn = new ObjectProperty();
            consumeToken(); // consume the `=`
            Assignment defaultValue = new Assignment(property, assignExpr());
            defaultValue.setType(Token.ASSIGN);
            pn.setLeftAndRight(property, defaultValue);
            return pn;
        }
        mustMatchToken(Token.COLON, "msg.no.colon.prop", true);
        ObjectProperty pn = new ObjectProperty();
        pn.setOperatorPosition(ts.tokenBeg);
        pn.setLeftAndRight(property, assignExpr());
        return pn;
    }

    private ObjectProperty methodDefinition(
            int pos, AstNode propName, int entryKind, boolean isGenerator) throws IOException {
        FunctionNode fn = function(FunctionNode.FUNCTION_EXPRESSION, true);
        // We've already parsed the function name, so fn should be anonymous.
        Name name = fn.getFunctionName();
        if (name != null && name.length() != 0) {
            reportError("msg.bad.prop");
        }
        ObjectProperty pn = new ObjectProperty(pos);
        switch (entryKind) {
            case GET_ENTRY:
                pn.setIsGetterMethod();
                fn.setFunctionIsGetterMethod();
                break;
            case SET_ENTRY:
                pn.setIsSetterMethod();
                fn.setFunctionIsSetterMethod();
                break;
            case METHOD_ENTRY:
                pn.setIsNormalMethod();
                fn.setFunctionIsNormalMethod();
                if (isGenerator) {
                    fn.setIsES6Generator();
                }
                break;
        }
        int end = getNodeEnd(fn);
        pn.setLeft(propName);
        pn.setRight(fn);
        pn.setLength(end - pos);
        return pn;
    }

    private Name createNameNode() {
        return createNameNode(false, Token.NAME);
    }

    /**
     * Create a {@code Name} node using the token info from the last scanned name. In some cases we
     * need to either synthesize a name node, or we lost the name token information by peeking. If
     * the {@code token} parameter is not {@link Token#NAME}, then we use token info saved in
     * instance vars.
     */
    private Name createNameNode(boolean checkActivation, int token) {
        int beg = ts.tokenBeg;
        String s = ts.getString();
        int lineno = lineNumber();
        int column = columnNumber();
        if (!"".equals(prevNameTokenString)) {
            beg = prevNameTokenStart;
            s = prevNameTokenString;
            lineno = prevNameTokenLineno;
            column = prevNameTokenColumn;
            prevNameTokenStart = 0;
            prevNameTokenString = "";
            prevNameTokenLineno = 0;
            prevNameTokenColumn = 0;
        }
        if (s == null) {
            if (compilerEnv.isIdeMode()) {
                s = "";
            } else {
                codeBug();
            }
        }
        Name name = new Name(beg, s);
        name.setLineColumnNumber(lineno, column);
        if (checkActivation) {
            checkActivationName(s, token);
        }
        return name;
    }

    private StringLiteral createStringLiteral() {
        int pos = ts.tokenBeg, end = ts.tokenEnd;
        StringLiteral s = new StringLiteral(pos, end - pos);
        s.setLineColumnNumber(lineNumber(), columnNumber());
        s.setValue(ts.getString());
        s.setQuoteCharacter(ts.getQuoteChar());
        return s;
    }

    private AstNode templateLiteral(boolean isTaggedLiteral) throws IOException {
        if (currentToken != Token.TEMPLATE_LITERAL) codeBug();
        int pos = ts.tokenBeg, end = ts.tokenEnd, lineno = lineNumber(), column = columnNumber();
        List<AstNode> elements = new ArrayList<>();
        TemplateLiteral pn = new TemplateLiteral(pos);

        int posChars = ts.tokenBeg + 1;
        int tt = ts.readTemplateLiteral(isTaggedLiteral);
        while (tt == Token.TEMPLATE_LITERAL_SUBST) {
            elements.add(createTemplateLiteralCharacters(posChars));
            elements.add(expr(false));
            mustMatchToken(Token.RC, "msg.syntax", true);
            posChars = ts.tokenBeg + 1;
            tt = ts.readTemplateLiteral(isTaggedLiteral);
        }
        if (tt == Token.ERROR) {
            return makeErrorNode();
        }
        assert tt == Token.TEMPLATE_LITERAL;
        elements.add(createTemplateLiteralCharacters(posChars));
        end = ts.tokenEnd;
        pn.setElements(elements);
        pn.setLength(end - pos);
        pn.setLineColumnNumber(lineno, column);

        return pn;
    }

    private TemplateCharacters createTemplateLiteralCharacters(int pos) {
        TemplateCharacters chars = new TemplateCharacters(pos, ts.tokenEnd - pos - 1);
        chars.setValue(ts.getString());
        chars.setRawValue(ts.getRawString());
        return chars;
    }

    private AstNode createNumericLiteral(int tt, boolean isProperty) {
        String s = ts.getString();
        if (this.inUseStrictDirective && ts.isNumericOldOctal()) {
            if (compilerEnv.getLanguageVersion() >= Context.VERSION_ES6 || !isProperty) {
                if (tt == Token.BIGINT) {
                    reportError("msg.no.old.octal.bigint");
                } else {
                    reportError("msg.no.old.octal.strict");
                }
            }
        }
        if (compilerEnv.getLanguageVersion() >= Context.VERSION_ES6 || !isProperty) {
            if (ts.isNumericBinary()) {
                s = "0b" + s;
            } else if (ts.isNumericOldOctal()) {
                s = "0" + s;
            } else if (ts.isNumericOctal()) {
                s = "0o" + s;
            } else if (ts.isNumericHex()) {
                s = "0x" + s;
            }
        }

        AstNode result;
        if (tt == Token.BIGINT) {
            result = new BigIntLiteral(ts.tokenBeg, s + "n", ts.getBigInt());
        } else {
            result = new NumberLiteral(ts.tokenBeg, s, ts.getNumber());
        }
        result.setLineColumnNumber(lineNumber(), columnNumber());
        return result;
    }

    protected void checkActivationName(String name, int token) {
        if (!insideFunctionBody()) {
            return;
        }
        boolean activation = false;
        if ("arguments".equals(name)
                &&
                // An arrow function not generate arguments. So it not need activation.
                ((FunctionNode) currentScriptOrFn).getFunctionType()
                        != FunctionNode.ARROW_FUNCTION) {
            activation = true;
        } else if (compilerEnv.getActivationNames() != null
                && compilerEnv.getActivationNames().contains(name)) {
            activation = true;
        } else if ("length".equals(name)) {
            if (token == Token.GETPROP && compilerEnv.getLanguageVersion() == Context.VERSION_1_2) {
                // Use of "length" in 1.2 requires an activation object.
                activation = true;
            }
        }
        if (activation) {
            setRequiresActivation();
        }
    }

    protected void setRequiresActivation() {
        if (insideFunctionBody()) {
            ((FunctionNode) currentScriptOrFn).setRequiresActivation();
        }
    }

    private void checkCallRequiresActivation(AstNode pn) {
        if ((pn.getType() == Token.NAME && "eval".equals(((Name) pn).getIdentifier()))
                || (pn.getType() == Token.GETPROP
                        && "eval".equals(((PropertyGet) pn).getProperty().getIdentifier())))
            setRequiresActivation();
    }

    protected void setIsGenerator() {
        if (insideFunctionBody()) {
            ((FunctionNode) currentScriptOrFn).setIsGenerator();
        }
    }

    private void checkBadIncDec(UpdateExpression expr) {
        AstNode op = removeParens(expr.getOperand());
        int tt = op.getType();
        if (!(tt == Token.NAME
                || tt == Token.GETPROP
                || tt == Token.GETELEM
                || tt == Token.GET_REF
                || tt == Token.CALL))
            reportError(expr.getType() == Token.INC ? "msg.bad.incr" : "msg.bad.decr");
    }

    private ErrorNode makeErrorNode() {
        ErrorNode pn = new ErrorNode(ts.tokenBeg, ts.tokenEnd - ts.tokenBeg);
        pn.setLineColumnNumber(lineNumber(), columnNumber());
        return pn;
    }

    // Return end of node.  Assumes node does NOT have a parent yet.
    private static int nodeEnd(AstNode node) {
        return node.getPosition() + node.getLength();
    }

    private void saveNameTokenData(int pos, String name, int lineno, int column) {
        prevNameTokenStart = pos;
        prevNameTokenString = name;
        prevNameTokenLineno = lineno;
        prevNameTokenColumn = column;
    }

    /**
     * Return the file offset of the beginning of the input source line containing the passed
     * position.
     *
     * @param pos an offset into the input source stream. If the offset is negative, it's converted
     *     to 0, and if it's beyond the end of the source buffer, the last source position is used.
     * @return the offset of the beginning of the line containing pos (i.e. 1+ the offset of the
     *     first preceding newline). Returns -1 if the {@link CompilerEnvirons} is not set to
     *     ide-mode, and {@link #parse(Reader,String,int)} was used.
     */
    private int lineBeginningFor(int pos) {
        if (sourceChars == null) {
            return -1;
        }
        if (pos <= 0) {
            return 0;
        }
        char[] buf = sourceChars;
        if (pos >= buf.length) {
            pos = buf.length - 1;
        }
        while (--pos >= 0) {
            char c = buf[pos];
            if (ScriptRuntime.isJSLineTerminator(c)) {
                return pos + 1; // want position after the newline
            }
        }
        return 0;
    }

    private void warnMissingSemi(int pos, int end) {
        // Should probably change this to be a CompilerEnvirons setting,
        // with an enum Never, Always, Permissive, where Permissive means
        // don't warn for 1-line functions like function (s) {return x+2}
        if (compilerEnv.isStrictMode()) {
            int[] linep = new int[2];
            String line = ts.getLine(end, linep);
            // this code originally called lineBeginningFor() and in order to
            // preserve its different line-offset handling, we need to special
            // case ide-mode here
            int beg = compilerEnv.isIdeMode() ? Math.max(pos, end - linep[1]) : pos;
            if (line != null) {
                addStrictWarning("msg.missing.semi", "", beg, end - beg, linep[0], line, linep[1]);
            } else {
                // no line information available, report warning at current line
                addStrictWarning("msg.missing.semi", "", beg, end - beg);
            }
        }
    }

    private void warnTrailingComma(int pos, List<?> elems, int commaPos) {
        if (compilerEnv.getWarnTrailingComma()) {
            // back up from comma to beginning of line or array/objlit
            if (!elems.isEmpty()) {
                pos = ((AstNode) elems.get(0)).getPosition();
            }
            pos = Math.max(pos, lineBeginningFor(commaPos));
            addWarning("msg.extra.trailing.comma", pos, commaPos - pos);
        }
    }

    // helps reduce clutter in the already-large function() method
    protected class PerFunctionVariables {
        private ScriptNode savedCurrentScriptOrFn;
        private Scope savedCurrentScope;
        private int savedEndFlags;
        private boolean savedInForInit;
        private Map<String, LabeledStatement> savedLabelSet;
        private List<Loop> savedLoopSet;
        private List<Jump> savedLoopAndSwitchSet;

        PerFunctionVariables(FunctionNode fnNode) {
            savedCurrentScriptOrFn = Parser.this.currentScriptOrFn;
            Parser.this.currentScriptOrFn = fnNode;

            savedCurrentScope = Parser.this.currentScope;
            Parser.this.currentScope = fnNode;

            savedLabelSet = Parser.this.labelSet;
            Parser.this.labelSet = null;

            savedLoopSet = Parser.this.loopSet;
            Parser.this.loopSet = null;

            savedLoopAndSwitchSet = Parser.this.loopAndSwitchSet;
            Parser.this.loopAndSwitchSet = null;

            savedEndFlags = Parser.this.endFlags;
            Parser.this.endFlags = 0;

            savedInForInit = Parser.this.inForInit;
            Parser.this.inForInit = false;
        }

        void restore() {
            Parser.this.currentScriptOrFn = savedCurrentScriptOrFn;
            Parser.this.currentScope = savedCurrentScope;
            Parser.this.labelSet = savedLabelSet;
            Parser.this.loopSet = savedLoopSet;
            Parser.this.loopAndSwitchSet = savedLoopAndSwitchSet;
            Parser.this.endFlags = savedEndFlags;
            Parser.this.inForInit = savedInForInit;
        }
    }

    PerFunctionVariables createPerFunctionVariables(FunctionNode fnNode) {
        return new PerFunctionVariables(fnNode);
    }

    /**
     * Given a destructuring assignment with a left hand side parsed as an array or object literal
     * and a right hand side expression, rewrite as a series of assignments to the variables defined
     * in left from property accesses to the expression on the right.
     *
     * @param type declaration type: Token.VAR or Token.LET or -1
     * @param left array or object literal containing NAME nodes for variables to assign
     * @param right expression to assign from
     * @return expression that performs a series of assignments to the variables defined in left
     */
    Node createDestructuringAssignment(
            int type, Node left, Node right, AstNode defaultValue, Transformer transformer) {
        String tempName = currentScriptOrFn.getNextTempName();
        Node result =
                destructuringAssignmentHelper(
                        type, left, right, tempName, defaultValue, transformer);
        Node comma = result.getLastChild();
        comma.addChildToBack(createName(tempName));
        return result;
    }

    Node createDestructuringAssignment(int type, Node left, Node right, Transformer transformer) {
        return createDestructuringAssignment(type, left, right, null, transformer);
    }

    Node createDestructuringAssignment(int type, Node left, Node right, AstNode defaultValue) {
        return createDestructuringAssignment(type, left, right, defaultValue, null);
    }

    Node destructuringAssignmentHelper(
            int variableType,
            Node left,
            Node right,
            String tempName,
            AstNode defaultValue,
            Transformer transformer) {
        Scope result = createScopeNode(Token.LETEXPR, left.getLineno(), left.getColumn());
        result.addChildToFront(new Node(Token.LET, createName(Token.NAME, tempName, right)));
        try {
            pushScope(result);
            defineSymbol(Token.LET, tempName, true);
        } finally {
            popScope();
        }
        Node comma = new Node(Token.COMMA);
        result.addChildToBack(comma);
        List<String> destructuringNames = new ArrayList<>();
        boolean empty = true;
        if (left instanceof ArrayLiteral) {
            empty =
                    destructuringArray(
                            (ArrayLiteral) left,
                            variableType,
                            tempName,
                            comma,
                            destructuringNames,
                            defaultValue,
                            transformer);
        } else if (left instanceof ObjectLiteral) {
            empty =
                    destructuringObject(
                            (ObjectLiteral) left,
                            variableType,
                            tempName,
                            comma,
                            destructuringNames,
                            defaultValue,
                            transformer);
        } else if (left.getType() == Token.GETPROP || left.getType() == Token.GETELEM) {
            switch (variableType) {
                case Token.CONST:
                case Token.LET:
                case Token.VAR:
                    reportError("msg.bad.assign.left");
            }
            comma.addChildToBack(simpleAssignment(left, createName(tempName), transformer));
        } else {
            reportError("msg.bad.assign.left");
        }
        if (empty) {
            // Don't want a COMMA node with no children. Just add a zero.
            comma.addChildToBack(createNumber(0));
        }
        result.putProp(Node.DESTRUCTURING_NAMES, destructuringNames);
        return result;
    }

    boolean destructuringArray(
            ArrayLiteral array,
            int variableType,
            String tempName,
            Node parent,
            List<String> destructuringNames,
            AstNode defaultValue, /* defaultValue to use in function param decls */
            Transformer transformer) {
        boolean empty = true;
        int setOp = variableType == Token.CONST ? Token.SETCONST : Token.SETNAME;
        int index = 0;
        boolean defaultValuesSetup = false;
        for (AstNode n : array.getElements()) {
            if (n.getType() == Token.EMPTY) {
                index++;
                continue;
            }
            Node rightElem = new Node(Token.GETELEM, createName(tempName), createNumber(index));

            if (defaultValue != null && !defaultValuesSetup) {
                setupDefaultValues(tempName, parent, defaultValue, setOp, transformer);
                defaultValuesSetup = true;
            }

            if (n.getType() == Token.NAME) {
                /* [x] = [1] */
                String name = n.getString();
                parent.addChildToBack(
                        new Node(setOp, createName(Token.BINDNAME, name, null), rightElem));
                if (variableType != -1) {
                    defineSymbol(variableType, name, true);
                    destructuringNames.add(name);
                }
            } else if (n.getType() == Token.ASSIGN) {
                /* [x = 1] = [2] */
                processDestructuringDefaults(
                        variableType,
                        parent,
                        destructuringNames,
                        (Assignment) n,
                        rightElem,
                        setOp,
                        transformer);
            } else {
                parent.addChildToBack(
                        destructuringAssignmentHelper(
                                variableType,
                                n,
                                rightElem,
                                currentScriptOrFn.getNextTempName(),
                                null,
                                transformer));
            }
            index++;
            empty = false;
        }
        return empty;
    }

    private void processDestructuringDefaults(
            int variableType,
            Node parent,
            List<String> destructuringNames,
            Assignment n,
            Node rightElem,
            int setOp,
            Transformer transformer) {
        Node left = n.getLeft();
        Node right = null;
        if (left.getType() == Token.NAME) {
            String name = left.getString();
            // x = (x == undefined) ?
            //          (($1[0] == undefined) ?
            //              1
            //              : $1[0])
            //          : x

            right = (transformer != null) ? transformer.transform(n.getRight()) : n.getRight();

            Node cond_inner =
                    new Node(
                            Token.HOOK,
                            new Node(Token.SHEQ, createName("undefined"), rightElem),
                            right,
                            rightElem);

            Node cond =
                    new Node(
                            Token.HOOK,
                            new Node(Token.SHEQ, createName("undefined"), createName(name)),
                            cond_inner,
                            left);

            // store it to be transformed later
            if (transformer == null) {
                currentScriptOrFn.putDestructuringRvalues(cond_inner, right);
            }

            parent.addChildToBack(new Node(setOp, createName(Token.BINDNAME, name, null), cond));
            if (variableType != -1) {
                defineSymbol(variableType, name, true);
                destructuringNames.add(name);
            }
        } else {
            // TODO: should handle other nested values on the lhs (ArrayLiteral, ObjectLiteral)
        }
    }

    static Object getPropKey(Node id) {
        Object key;
        if (id instanceof Name) {
            String s = ((Name) id).getIdentifier();
            key = ScriptRuntime.getIndexObject(s);
        } else if (id instanceof StringLiteral) {
            String s = ((StringLiteral) id).getValue();
            key = ScriptRuntime.getIndexObject(s);
        } else if (id instanceof NumberLiteral) {
            double n = ((NumberLiteral) id).getNumber();
            key = ScriptRuntime.getIndexObject(n);
        } else if (id instanceof GeneratorMethodDefinition) {
            key = getPropKey(((GeneratorMethodDefinition) id).getMethodName());
        } else {
            key = null; // Filled later
        }
        return key;
    }

    private void setupDefaultValues(
            String tempName,
            Node parent,
            AstNode defaultValue,
            int setOp,
            Transformer transformer) {
        if (defaultValue != null) {
            // if there's defaultValue it can be substituted for tempName if that's undefined
            // i.e. $1 = ($1 == undefined) ? defaultValue : $1

            Node defaultRvalue =
                    transformer != null ? transformer.transform(defaultValue) : defaultValue;

            Node cond_default =
                    new Node(
                            Token.HOOK,
                            new Node(Token.SHEQ, createName(tempName), createName("undefined")),
                            defaultRvalue,
                            createName(tempName));

            if (transformer == null) {
                currentScriptOrFn.putDestructuringRvalues(cond_default, defaultRvalue);
            }

            Node set_default =
                    new Node(setOp, createName(Token.BINDNAME, tempName, null), cond_default);
            parent.addChildToBack(set_default);
        }
    }

    boolean destructuringObject(
            ObjectLiteral node,
            int variableType,
            String tempName,
            Node parent,
            List<String> destructuringNames,
            AstNode defaultValue, /* defaultValue to use in function param decls */
            Transformer transformer) {
        boolean empty = true;
        int setOp = variableType == Token.CONST ? Token.SETCONST : Token.SETNAME;
        boolean defaultValuesSetup = false;

        for (ObjectProperty prop : node.getElements()) {
            int lineno = 0, column = 0;
            // This function is sometimes called from the IRFactory
            // when executing regression tests, and in those cases the
            // tokenStream isn't set.  Deal with it.
            if (ts != null) {
                lineno = lineNumber();
                column = columnNumber();
            }
            AstNode id = prop.getLeft();

            Node rightElem = null;
            if (id instanceof Name) {
                Node s = Node.newString(((Name) id).getIdentifier());
                rightElem = new Node(Token.GETPROP, createName(tempName), s);
            } else if (id instanceof StringLiteral) {
                Node s = Node.newString(((StringLiteral) id).getValue());
                rightElem = new Node(Token.GETPROP, createName(tempName), s);
            } else if (id instanceof NumberLiteral) {
                Node s = createNumber((int) ((NumberLiteral) id).getNumber());
                rightElem = new Node(Token.GETELEM, createName(tempName), s);
            } else if (id instanceof ComputedPropertyKey) {
                reportError("msg.bad.computed.property.in.destruct");
                return false;
            } else {
                throw codeBug();
            }

            rightElem.setLineColumnNumber(lineno, column);
            if (defaultValue != null && !defaultValuesSetup) {
                setupDefaultValues(tempName, parent, defaultValue, setOp, transformer);
                defaultValuesSetup = true;
            }

            AstNode value = prop.getRight();
            if (value.getType() == Token.NAME) {
                String name = ((Name) value).getIdentifier();
                parent.addChildToBack(
                        new Node(setOp, createName(Token.BINDNAME, name, null), rightElem));
                if (variableType != -1) {
                    defineSymbol(variableType, name, true);
                    destructuringNames.add(name);
                }
            } else if (value.getType() == Token.ASSIGN) {
                processDestructuringDefaults(
                        variableType,
                        parent,
                        destructuringNames,
                        (Assignment) value,
                        rightElem,
                        setOp,
                        transformer);
            } else {
                parent.addChildToBack(
                        destructuringAssignmentHelper(
                                variableType,
                                value,
                                rightElem,
                                currentScriptOrFn.getNextTempName(),
                                null,
                                transformer));
            }
            empty = false;
        }
        return empty;
    }

    protected Node createName(String name) {
        checkActivationName(name, Token.NAME);
        return Node.newString(Token.NAME, name);
    }

    protected Node createName(int type, String name, Node child) {
        Node result = createName(name);
        result.setType(type);
        if (child != null) result.addChildToBack(child);
        return result;
    }

    protected Node createNumber(double number) {
        return Node.newNumber(number);
    }

    /**
     * Create a node that can be used to hold lexically scoped variable definitions (via let
     * declarations).
     *
     * @param token the token of the node to create
     * @param lineno line number of source
     * @return the created node
     */
    protected Scope createScopeNode(int token, int lineno, int column) {
        Scope scope = new Scope();
        scope.setType(token);
        scope.setLineColumnNumber(lineno, column);
        return scope;
    }

    // Quickie tutorial for some of the interpreter bytecodes.
    //
    // GETPROP - for normal foo.bar prop access; right side is a name
    // GETELEM - for normal foo[bar] element access; rhs is an expr
    // SETPROP - for assignment when left side is a GETPROP
    // SETELEM - for assignment when left side is a GETELEM
    // DELPROP - used for delete foo.bar or foo[bar]
    //
    // GET_REF, SET_REF, DEL_REF - in general, these mean you're using
    // get/set/delete on a right-hand side expression (possibly with no
    // explicit left-hand side) that doesn't use the normal JavaScript
    // Object (i.e. ScriptableObject) get/set/delete functions, but wants
    // to provide its own versions instead.  It will ultimately implement
    // Ref, and currently SpecialRef (for __proto__ etc.) and XmlName
    // (for E4X XML objects) are the only implementations.  The runtime
    // notices these bytecodes and delegates get/set/delete to the object.
    //
    // BINDNAME:  used in assignments.  LHS is evaluated first to get a
    // specific object containing the property ("binding" the property
    // to the object) so that it's always the same object, regardless of
    // side effects in the RHS.
    protected Node simpleAssignment(Node left, Node right) {
        return simpleAssignment(left, right, null);
    }

    protected Node simpleAssignment(Node left, Node right, Transformer transformer) {
        int nodeType = left.getType();
        switch (nodeType) {
            case Token.NAME:
                String name = ((Name) left).getIdentifier();
                if (inUseStrictDirective && ("eval".equals(name) || "arguments".equals(name))) {
                    reportError("msg.bad.id.strict", name);
                }
                left.setType(Token.BINDNAME);
                return new Node(Token.SETNAME, left, right);

            case Token.GETPROP:
            case Token.GETELEM:
                {
                    Node obj, id;
                    // If it's a PropertyGet or ElementGet, we're in the parse pass.
                    // We could alternately have PropertyGet and ElementGet
                    // override getFirstChild/getLastChild and return the appropriate
                    // field, but that seems just as ugly as this casting.
                    if (left instanceof PropertyGet) {
                        AstNode target = ((PropertyGet) left).getTarget();
                        obj = transformer != null ? transformer.transform(target) : target;
                        id = ((PropertyGet) left).getProperty();
                    } else if (left instanceof ElementGet) {
                        AstNode target = ((ElementGet) left).getTarget();
                        AstNode elem = ((ElementGet) left).getElement();
                        obj = transformer != null ? transformer.transform(target) : target;
                        id = transformer != null ? transformer.transform(elem) : elem;
                    } else {
                        // This branch is called during IRFactory transform pass.
                        obj = left.getFirstChild();
                        id = left.getLastChild();
                    }
                    int type;
                    if (nodeType == Token.GETPROP) {
                        type = Token.SETPROP;
                        // TODO(stevey) - see https://bugzilla.mozilla.org/show_bug.cgi?id=492036
                        // The new AST code generates NAME tokens for GETPROP ids where the old
                        // parser
                        // generated STRING nodes. If we don't set the type to STRING below, this
                        // will
                        // cause java.lang.VerifyError in codegen for code like
                        // "var obj={p:3};[obj.p]=[9];"
                        id.setType(Token.STRING);
                    } else {
                        type = Token.SETELEM;
                    }
                    return new Node(type, obj, id, right);
                }
            case Token.GET_REF:
                {
                    Node ref = left.getFirstChild();
                    checkMutableReference(ref);
                    return new Node(Token.SET_REF, ref, right);
                }
        }

        throw codeBug();
    }

    protected void checkMutableReference(Node n) {
        int memberTypeFlags = n.getIntProp(Node.MEMBER_TYPE_PROP, 0);
        if ((memberTypeFlags & Node.DESCENDANTS_FLAG) != 0) {
            reportError("msg.bad.assign.left");
        }
    }

    // remove any ParenthesizedExpression wrappers
    protected AstNode removeParens(AstNode node) {
        while (node instanceof ParenthesizedExpression) {
            node = ((ParenthesizedExpression) node).getExpression();
        }
        return node;
    }

    void markDestructuring(AstNode node) {
        if (node instanceof DestructuringForm) {
            ((DestructuringForm) node).setIsDestructuring(true);
        } else if (node instanceof ParenthesizedExpression) {
            markDestructuring(((ParenthesizedExpression) node).getExpression());
        }
    }

    // throw a failed-assertion with some helpful debugging info
    private RuntimeException codeBug() throws RuntimeException {
        throw Kit.codeBug(
                "ts.cursor="
                        + ts.cursor
                        + ", ts.tokenBeg="
                        + ts.tokenBeg
                        + ", currentToken="
                        + currentToken);
    }

    public void setDefaultUseStrictDirective(boolean useStrict) {
        defaultUseStrictDirective = useStrict;
    }

    public boolean inUseStrictDirective() {
        return inUseStrictDirective;
    }

    public void reportErrorsIfExists(int baseLineno) {
        if (this.syntaxErrorCount != 0) {
            String msg = String.valueOf(this.syntaxErrorCount);
            msg = lookupMessage("msg.got.syntax.errors", msg);
            if (!compilerEnv.isIdeMode())
                throw errorReporter.runtimeError(msg, sourceURI, baseLineno, null, 0);
        }
    }

    public void setSourceURI(String sourceURI) {
        this.sourceURI = sourceURI;
    }

    public interface CurrentPositionReporter {
        public int getPosition();

        public int getLength();

        public int getLineno();

        public String getLine();

        public int getOffset();
    }
}
