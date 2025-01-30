/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
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
import org.mozilla.javascript.ast.ComputedPropertyKey;
import org.mozilla.javascript.ast.ConditionalExpression;
import org.mozilla.javascript.ast.ContinueStatement;
import org.mozilla.javascript.ast.DestructuringForm;
import org.mozilla.javascript.ast.DoLoop;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.EmptyExpression;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.ForInLoop;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.GeneratorExpression;
import org.mozilla.javascript.ast.GeneratorExpressionLoop;
import org.mozilla.javascript.ast.GeneratorMethodDefinition;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Jump;
import org.mozilla.javascript.ast.KeywordLiteral;
import org.mozilla.javascript.ast.Label;
import org.mozilla.javascript.ast.LabeledStatement;
import org.mozilla.javascript.ast.LetNode;
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
import org.mozilla.javascript.ast.XmlElemRef;
import org.mozilla.javascript.ast.XmlExpression;
import org.mozilla.javascript.ast.XmlFragment;
import org.mozilla.javascript.ast.XmlLiteral;
import org.mozilla.javascript.ast.XmlMemberGet;
import org.mozilla.javascript.ast.XmlPropRef;
import org.mozilla.javascript.ast.XmlRef;
import org.mozilla.javascript.ast.XmlString;
import org.mozilla.javascript.ast.Yield;

/**
 * This class rewrites the parse tree into an IR suitable for codegen.
 *
 * @see Node
 * @author Mike McCabe
 * @author Norris Boyd
 */
public final class IRFactory {
    private static final int LOOP_DO_WHILE = 0;
    private static final int LOOP_WHILE = 1;
    private static final int LOOP_FOR = 2;

    private static final int ALWAYS_TRUE_BOOLEAN = 1;
    private static final int ALWAYS_FALSE_BOOLEAN = -1;

    private Parser parser;
    private AstNodePosition astNodePos;

    public IRFactory(CompilerEnvirons env, String sourceString) {
        this(env, null, sourceString, env.getErrorReporter());
    }

    /** Use {@link #IRFactory(CompilerEnvirons, String, String, ErrorReporter)} */
    @Deprecated
    public IRFactory(CompilerEnvirons env, String sourceString, ErrorReporter errorReporter) {
        this(env, null, sourceString, errorReporter);
    }

    public IRFactory(
            CompilerEnvirons env,
            String sourceName,
            String sourceString,
            ErrorReporter errorReporter) {
        parser = new Parser(env, errorReporter);
        astNodePos = new AstNodePosition(sourceString);
        parser.currentPos = astNodePos;
        parser.setSourceURI(sourceName);
    }

    /** Transforms the tree into a lower-level IR suitable for codegen. */
    public ScriptNode transformTree(AstRoot root) {
        parser.currentScriptOrFn = root;
        parser.inUseStrictDirective = root.isInStrictMode();

        if (Token.printTrees) {
            System.out.println("IRFactory.transformTree");
            System.out.println(root.debugPrint());
        }

        astNodePos.push(root);
        try {
            return (ScriptNode) transform(root);
        } catch (Parser.ParserException e) {
            parser.reportErrorsIfExists(root.getLineno());
            return null;
        } finally {
            astNodePos.pop();
        }
    }

    // Might want to convert this to polymorphism - move transform*
    // functions into the AstNode subclasses.  OTOH that would make
    // IR transformation part of the public AST API - desirable?
    // Another possibility:  create AstTransformer interface and adapter.
    private Node transform(AstNode node) {
        switch (node.getType()) {
            case Token.ARRAYCOMP:
                return transformArrayComp((ArrayComprehension) node);
            case Token.ARRAYLIT:
                return transformArrayLiteral((ArrayLiteral) node);
            case Token.BIGINT:
                return transformBigInt((BigIntLiteral) node);
            case Token.BLOCK:
                return transformBlock(node);
            case Token.BREAK:
                return transformBreak((BreakStatement) node);
            case Token.CALL:
                return transformFunctionCall((FunctionCall) node);
            case Token.CONTINUE:
                return transformContinue((ContinueStatement) node);
            case Token.DO:
                return transformDoLoop((DoLoop) node);
            case Token.EMPTY:
            case Token.COMMENT:
                return node;
            case Token.FOR:
                if (node instanceof ForInLoop) {
                    return transformForInLoop((ForInLoop) node);
                }
                return transformForLoop((ForLoop) node);
            case Token.FUNCTION:
                return transformFunction((FunctionNode) node);
            case Token.GENEXPR:
                return transformGenExpr((GeneratorExpression) node);
            case Token.GETELEM:
                return transformElementGet((ElementGet) node);
            case Token.GETPROP:
                return transformPropertyGet((PropertyGet) node);
            case Token.QUESTION_DOT:
                if (node instanceof ElementGet) {
                    return transformElementGet((ElementGet) node);
                } else {
                    return transformPropertyGet((PropertyGet) node);
                }
            case Token.HOOK:
                return transformCondExpr((ConditionalExpression) node);
            case Token.IF:
                return transformIf((IfStatement) node);

            case Token.TRUE:
            case Token.FALSE:
            case Token.THIS:
            case Token.NULL:
            case Token.DEBUGGER:
                return transformLiteral(node);
            case Token.SUPER:
                parser.setRequiresActivation();
                return transformLiteral(node);
            case Token.NAME:
                return transformName((Name) node);
            case Token.NUMBER:
                return transformNumber((NumberLiteral) node);
            case Token.NEW:
                return transformNewExpr((NewExpression) node);
            case Token.OBJECTLIT:
                return transformObjectLiteral((ObjectLiteral) node);
            case Token.TEMPLATE_LITERAL:
                return transformTemplateLiteral((TemplateLiteral) node);
            case Token.TAGGED_TEMPLATE_LITERAL:
                return transformTemplateLiteralCall((TaggedTemplateLiteral) node);
            case Token.REGEXP:
                return transformRegExp((RegExpLiteral) node);
            case Token.RETURN:
                return transformReturn((ReturnStatement) node);
            case Token.SCRIPT:
                return transformScript((ScriptNode) node);
            case Token.STRING:
                return transformString((StringLiteral) node);
            case Token.SWITCH:
                return transformSwitch((SwitchStatement) node);
            case Token.THROW:
                return transformThrow((ThrowStatement) node);
            case Token.TRY:
                return transformTry((TryStatement) node);
            case Token.WHILE:
                return transformWhileLoop((WhileLoop) node);
            case Token.WITH:
                return transformWith((WithStatement) node);
            case Token.YIELD:
            case Token.YIELD_STAR:
                return transformYield((Yield) node);
            default:
                if (node instanceof ExpressionStatement) {
                    return transformExprStmt((ExpressionStatement) node);
                }
                if (node instanceof Assignment) {
                    return transformAssignment((Assignment) node);
                }
                if (node instanceof UnaryExpression) {
                    return transformUnary((UnaryExpression) node);
                }
                if (node instanceof UpdateExpression) {
                    return transformUpdate((UpdateExpression) node);
                }
                if (node instanceof XmlMemberGet) {
                    return transformXmlMemberGet((XmlMemberGet) node);
                }
                if (node instanceof InfixExpression) {
                    return transformInfix((InfixExpression) node);
                }
                if (node instanceof VariableDeclaration) {
                    return transformVariables((VariableDeclaration) node);
                }
                if (node instanceof ParenthesizedExpression) {
                    return transformParenExpr((ParenthesizedExpression) node);
                }
                if (node instanceof ComputedPropertyKey) {
                    return transformComputedPropertyKey((ComputedPropertyKey) node);
                }
                if (node instanceof LabeledStatement) {
                    return transformLabeledStatement((LabeledStatement) node);
                }
                if (node instanceof LetNode) {
                    return transformLetNode((LetNode) node);
                }
                if (node instanceof XmlRef) {
                    return transformXmlRef((XmlRef) node);
                }
                if (node instanceof XmlLiteral) {
                    return transformXmlLiteral((XmlLiteral) node);
                }
                if (node instanceof GeneratorMethodDefinition) {
                    return transformGeneratorMethodDefinition((GeneratorMethodDefinition) node);
                }
                throw new IllegalArgumentException("Can't transform: " + node);
        }
    }

    private Node transformArrayComp(ArrayComprehension node) {
        // An array comprehension expression such as
        //
        //   [expr for (x in foo) for each ([y, z] in bar) if (cond)]
        //
        // is rewritten approximately as
        //
        // new Scope(ARRAYCOMP) {
        //   new Node(BLOCK) {
        //     let tmp1 = new Array;
        //     for (let x in foo) {
        //       for each (let tmp2 in bar) {
        //         if (cond) {
        //           tmp1.push([y, z] = tmp2, expr);
        //         }
        //       }
        //     }
        //   }
        //   createName(tmp1)
        // }

        int lineno = node.getLineno(), column = node.getColumn();
        Scope scopeNode = parser.createScopeNode(Token.ARRAYCOMP, lineno, column);
        String arrayName = parser.currentScriptOrFn.getNextTempName();
        parser.pushScope(scopeNode);
        try {
            astNodePos.push(node);
            try {
                parser.defineSymbol(Token.LET, arrayName, false);
                Node block = new Node(Token.BLOCK);
                block.setLineColumnNumber(lineno, column);
                Node newArray = createCallOrNew(Token.NEW, parser.createName("Array"));
                Node init =
                        new Node(
                                Token.EXPR_VOID,
                                createAssignment(
                                        Token.ASSIGN, parser.createName(arrayName), newArray),
                                lineno,
                                column);
                block.addChildToBack(init);
                block.addChildToBack(arrayCompTransformHelper(node, arrayName));
                scopeNode.addChildToBack(block);
                scopeNode.addChildToBack(parser.createName(arrayName));
                return scopeNode;
            } finally {
                astNodePos.pop();
            }
        } finally {
            parser.popScope();
        }
    }

    private Node arrayCompTransformHelper(ArrayComprehension node, String arrayName) {
        int lineno = node.getLineno(), column = node.getColumn();
        Node expr = transform(node.getResult());

        List<ArrayComprehensionLoop> loops = node.getLoops();
        int numLoops = loops.size();

        // Walk through loops, collecting and defining their iterator symbols.
        Node[] iterators = new Node[numLoops];
        Node[] iteratedObjs = new Node[numLoops];

        for (int i = 0; i < numLoops; i++) {
            ArrayComprehensionLoop acl = loops.get(i);
            AstNode iter = acl.getIterator();
            astNodePos.push(iter);
            try {
                String name = null;
                if (iter.getType() == Token.NAME) {
                    name = iter.getString();
                } else {
                    // destructuring assignment
                    name = parser.currentScriptOrFn.getNextTempName();
                    parser.defineSymbol(Token.LP, name, false);
                    expr =
                            createBinary(
                                    Token.COMMA,
                                    createAssignment(Token.ASSIGN, iter, parser.createName(name)),
                                    expr);
                }
                Node init = parser.createName(name);
                // Define as a let since we want the scope of the variable to
                // be restricted to the array comprehension
                parser.defineSymbol(Token.LET, name, false);
                iterators[i] = init;
            } finally {
                astNodePos.pop();
            }

            iteratedObjs[i] = transform(acl.getIteratedObject());
        }

        // generate code for tmpArray.push(body)
        Node call =
                createCallOrNew(
                        Token.CALL,
                        createPropertyGet(
                                parser.createName(arrayName), null, "push", 0, node.type));

        Node body = new Node(Token.EXPR_VOID, call);
        body.setLineColumnNumber(lineno, column);

        if (node.getFilter() != null) {
            body = createIf(transform(node.getFilter()), body, null, lineno, column);
        }

        // Now walk loops in reverse to build up the body statement.
        int pushed = 0;
        try {
            for (int i = numLoops - 1; i >= 0; i--) {
                ArrayComprehensionLoop acl = loops.get(i);
                Scope loop =
                        createLoopNode(
                                null, // no label
                                acl.getLineno(),
                                acl.getColumn());
                parser.pushScope(loop);
                pushed++;
                body =
                        createForIn(
                                Token.LET,
                                loop,
                                iterators[i],
                                iteratedObjs[i],
                                body,
                                acl,
                                acl.isForEach(),
                                acl.isForOf());
            }
        } finally {
            for (int i = 0; i < pushed; i++) {
                parser.popScope();
            }
        }

        // Now that we've accumulated any destructuring forms,
        // add expr to the call node; it's pushed on each iteration.
        call.addChildToBack(expr);
        return body;
    }

    private Node transformArrayLiteral(ArrayLiteral node) {
        if (node.isDestructuring()) {
            return node;
        }
        List<AstNode> elems = node.getElements();
        Node array = new Node(Token.ARRAYLIT);
        List<Integer> skipIndexes = null;
        for (int i = 0; i < elems.size(); ++i) {
            AstNode elem = elems.get(i);
            if (elem.getType() != Token.EMPTY) {
                array.addChildToBack(transform(elem));
            } else {
                if (skipIndexes == null) {
                    skipIndexes = new ArrayList<>();
                }
                skipIndexes.add(Integer.valueOf(i));
            }
        }
        array.putIntProp(Node.DESTRUCTURING_ARRAY_LENGTH, node.getDestructuringLength());
        if (skipIndexes != null) {
            int[] skips = new int[skipIndexes.size()];
            for (int i = 0; i < skipIndexes.size(); i++) skips[i] = skipIndexes.get(i).intValue();
            array.putProp(Node.SKIP_INDEXES_PROP, skips);
        }
        return array;
    }

    private Node transformAssignment(Assignment node) {
        AstNode right = node.getRight();
        AstNode left = parser.removeParens(node.getLeft());
        left = transformAssignmentLeft(node, left, right);

        Node target = null;
        if (isDestructuring(left)) {
            target = left;
        } else {
            target = transform(left);
        }

        astNodePos.push(left);
        try {
            return createAssignment(node.getType(), target, transform(right));
        } finally {
            astNodePos.pop();
        }
    }

    private AstNode transformAssignmentLeft(Assignment node, AstNode left, AstNode right) {
        if (right.getType() == Token.NULL
                && node.getType() == Token.ASSIGN
                && left instanceof Name
                && right instanceof KeywordLiteral) {

            String identifier = ((Name) left).getIdentifier();
            for (AstNode p = node.getParent(); p != null; p = p.getParent()) {
                if (p instanceof FunctionNode) {
                    Name functionName = ((FunctionNode) p).getFunctionName();
                    if (functionName != null && functionName.getIdentifier().equals(identifier)) {
                        PropertyGet propertyGet = new PropertyGet();
                        KeywordLiteral thisKeyword = new KeywordLiteral();
                        thisKeyword.setType(Token.THIS);
                        propertyGet.setLeft(thisKeyword);
                        propertyGet.setRight(left);
                        node.setLeft(propertyGet);
                        return propertyGet;
                    }
                }
            }
        }
        return left;
    }

    private Node transformBigInt(BigIntLiteral node) {
        return node;
    }

    private Node transformBlock(AstNode node) {
        if (node instanceof Scope) {
            parser.pushScope((Scope) node);
        }
        try {
            List<Node> kids = new ArrayList<>();
            /*
            Function declarations inside blocks (FUNCTION_EXPRESSION_STATEMENTS) should be
            hoisted at the top of block so that they can be referred by statements above them
             */
            List<Node> functions = new ArrayList<>();

            for (Node kid : node) {
                if (kid instanceof FunctionNode
                        && ((FunctionNode) kid).getFunctionType()
                                == FunctionNode.FUNCTION_EXPRESSION_STATEMENT) {
                    functions.add(transform((AstNode) kid));
                } else {
                    kids.add(transform((AstNode) kid));
                }
            }
            node.removeChildren();

            for (Node function : functions) {
                node.addChildToBack(function);
            }
            for (Node kid : kids) {
                node.addChildToBack(kid);
            }
            return node;
        } finally {
            if (node instanceof Scope) {
                parser.popScope();
            }
        }
    }

    private Node transformBreak(BreakStatement node) {
        return node;
    }

    private Node transformCondExpr(ConditionalExpression node) {
        Node test = transform(node.getTestExpression());
        Node ifTrue = transform(node.getTrueExpression());
        Node ifFalse = transform(node.getFalseExpression());
        return createCondExpr(test, ifTrue, ifFalse);
    }

    private Node transformContinue(ContinueStatement node) {
        return node;
    }

    private Node transformDoLoop(DoLoop loop) {
        loop.setType(Token.LOOP);
        parser.pushScope(loop);
        try {
            Node body = transform(loop.getBody());
            Node cond = transform(loop.getCondition());
            return createLoop(loop, LOOP_DO_WHILE, body, cond, null, null);
        } finally {
            parser.popScope();
        }
    }

    private Node transformElementGet(ElementGet node) {
        // OPT: could optimize to createPropertyGet
        // iff elem is string that can not be number
        Node target = transform(node.getTarget());
        Node element = transform(node.getElement());
        Node getElem = new Node(Token.GETELEM, target, element);
        if (node.type == Token.QUESTION_DOT) {
            getElem.putIntProp(Node.OPTIONAL_CHAINING, 1);
        }
        if (target.getType() == Token.SUPER) {
            getElem.putIntProp(Node.SUPER_PROPERTY_ACCESS, 1);
        }
        return getElem;
    }

    private Node transformExprStmt(ExpressionStatement node) {
        Node expr = transform(node.getExpression());
        return new Node(node.getType(), expr, node.getLineno(), node.getColumn());
    }

    private Node transformForInLoop(ForInLoop loop) {
        loop.setType(Token.LOOP);
        parser.pushScope(loop);
        try {
            int declType = -1;
            AstNode iter = loop.getIterator();
            if (iter instanceof VariableDeclaration) {
                declType = iter.getType();
            }
            Node lhs = transform(iter);
            Node obj = transform(loop.getIteratedObject());
            Node body = transform(loop.getBody());
            return createForIn(
                    declType, loop, lhs, obj, body, loop, loop.isForEach(), loop.isForOf());
        } finally {
            parser.popScope();
        }
    }

    private Node transformForLoop(ForLoop loop) {
        loop.setType(Token.LOOP);
        // XXX: Can't use pushScope/popScope here since 'createFor' may split
        // the scope
        Scope savedScope = parser.currentScope;
        parser.currentScope = loop;
        try {
            Node init = transform(loop.getInitializer());
            Node test = transform(loop.getCondition());
            Node incr = transform(loop.getIncrement());
            Node body = transform(loop.getBody());
            return createFor(loop, init, test, incr, body);
        } finally {
            parser.currentScope = savedScope;
        }
    }

    private Node transformFunction(FunctionNode fn) {
        Node mexpr = decompileFunctionHeader(fn);
        int index = parser.currentScriptOrFn.addFunction(fn);

        Parser.PerFunctionVariables savedVars = parser.createPerFunctionVariables(fn);
        try {
            // If we start needing to record much more codegen metadata during
            // function parsing, we should lump it all into a helper class.
            Node destructuring = (Node) fn.getProp(Node.DESTRUCTURING_PARAMS);
            fn.removeProp(Node.DESTRUCTURING_PARAMS);

            int lineno = fn.getBody().getLineno(), column = fn.getBody().getColumn();
            ++parser.nestingOfFunction; // only for body, not params
            Node body = transform(fn.getBody());

            /* Process simple default parameters */
            List<Object> defaultParams = fn.getDefaultParams();
            if (defaultParams != null) {
                for (int i = defaultParams.size() - 1; i > 0; ) {
                    if (defaultParams.get(i) instanceof AstNode
                            && defaultParams.get(i - 1) instanceof String) {
                        AstNode rhs = (AstNode) defaultParams.get(i);
                        String name = (String) defaultParams.get(i - 1);
                        body.addChildToFront(
                                createIf(
                                        createBinary(
                                                Token.SHEQ,
                                                parser.createName(name),
                                                parser.createName("undefined")),
                                        new Node(
                                                Token.EXPR_VOID,
                                                createAssignment(
                                                        Token.ASSIGN,
                                                        parser.createName(name),
                                                        transform(rhs)),
                                                body.getLineno(),
                                                body.getColumn()),
                                        null,
                                        body.getLineno(),
                                        body.getColumn()));
                    }
                    i -= 2;
                }
            }

            /* transform nodes used as default parameters */
            List<Node[]> dfns = fn.getDestructuringRvalues();
            if (dfns != null) {
                for (var i : dfns) {
                    Node a = i[0];
                    if (i[1] instanceof AstNode) {
                        AstNode b = (AstNode) i[1];
                        a.replaceChild(b, transform(b));
                    }
                }
            }

            if (destructuring != null) {
                body.addChildToFront(new Node(Token.EXPR_VOID, destructuring, lineno, column));
            }

            int syntheticType = fn.getFunctionType();
            Node pn = initFunction(fn, index, body, syntheticType);
            if (mexpr != null) {
                astNodePos.push(fn);
                try {
                    pn = createAssignment(Token.ASSIGN, mexpr, pn);
                } finally {
                    astNodePos.pop();
                }
                if (syntheticType != FunctionNode.FUNCTION_EXPRESSION) {
                    pn = createExprStatementNoReturn(pn, fn.getLineno(), fn.getColumn());
                }
            }
            return pn;

        } finally {
            --parser.nestingOfFunction;
            savedVars.restore();
        }
    }

    private Node transformFunctionCall(FunctionCall node) {
        astNodePos.push(node);
        try {
            Node transformedTarget = transform(node.getTarget());
            Node call = createCallOrNew(Token.CALL, transformedTarget);
            call.setLineColumnNumber(node.getLineno(), node.getColumn());
            List<AstNode> args = node.getArguments();
            for (int i = 0; i < args.size(); i++) {
                AstNode arg = args.get(i);
                call.addChildToBack(transform(arg));
            }
            if (node.isOptionalCall()) {
                call.putIntProp(Node.OPTIONAL_CHAINING, 1);
            }
            if (transformedTarget.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
                call.putIntProp(Node.SUPER_PROPERTY_ACCESS, 1);
            }
            return call;
        } finally {
            astNodePos.pop();
        }
    }

    private Node transformGenExpr(GeneratorExpression node) {
        Node pn;

        FunctionNode fn = new FunctionNode();
        fn.setSourceName(parser.currentScriptOrFn.getNextTempName());
        fn.setIsGenerator();
        fn.setFunctionType(FunctionNode.FUNCTION_EXPRESSION);
        fn.setRequiresActivation();

        Node mexpr = decompileFunctionHeader(fn);
        int index = parser.currentScriptOrFn.addFunction(fn);

        Parser.PerFunctionVariables savedVars = parser.createPerFunctionVariables(fn);
        try {
            // If we start needing to record much more codegen metadata during
            // function parsing, we should lump it all into a helper class.
            Node destructuring = (Node) fn.getProp(Node.DESTRUCTURING_PARAMS);
            fn.removeProp(Node.DESTRUCTURING_PARAMS);

            int lineno = node.getLineno(), column = node.getColumn();
            ++parser.nestingOfFunction; // only for body, not params
            Node body = genExprTransformHelper(node);

            if (destructuring != null) {
                body.addChildToFront(new Node(Token.EXPR_VOID, destructuring, lineno, column));
            }

            int syntheticType = fn.getFunctionType();
            pn = initFunction(fn, index, body, syntheticType);
            if (mexpr != null) {
                astNodePos.push(fn);
                try {
                    pn = createAssignment(Token.ASSIGN, mexpr, pn);
                } finally {
                    astNodePos.pop();
                }
                if (syntheticType != FunctionNode.FUNCTION_EXPRESSION) {
                    pn = createExprStatementNoReturn(pn, fn.getLineno(), fn.getColumn());
                }
            }
        } finally {
            --parser.nestingOfFunction;
            savedVars.restore();
        }

        Node call = createCallOrNew(Token.CALL, pn);
        call.setLineColumnNumber(node.getLineno(), node.getColumn());
        return call;
    }

    private Node genExprTransformHelper(GeneratorExpression node) {
        int lineno = node.getLineno(), column = node.getColumn();
        Node expr = transform(node.getResult());

        List<GeneratorExpressionLoop> loops = node.getLoops();
        int numLoops = loops.size();

        // Walk through loops, collecting and defining their iterator symbols.
        Node[] iterators = new Node[numLoops];
        Node[] iteratedObjs = new Node[numLoops];

        for (int i = 0; i < numLoops; i++) {
            GeneratorExpressionLoop acl = loops.get(i);

            AstNode iter = acl.getIterator();
            astNodePos.push(iter);
            try {
                String name = null;
                if (iter.getType() == Token.NAME) {
                    name = iter.getString();
                } else {
                    // destructuring assignment
                    name = parser.currentScriptOrFn.getNextTempName();
                    parser.defineSymbol(Token.LP, name, false);
                    expr =
                            createBinary(
                                    Token.COMMA,
                                    createAssignment(Token.ASSIGN, iter, parser.createName(name)),
                                    expr);
                }
                Node init = parser.createName(name);
                // Define as a let since we want the scope of the variable to
                // be restricted to the array comprehension
                parser.defineSymbol(Token.LET, name, false);
                iterators[i] = init;
            } finally {
                astNodePos.pop();
            }

            iteratedObjs[i] = transform(acl.getIteratedObject());
        }

        // generate code for tmpArray.push(body)
        Node yield = new Node(Token.YIELD, expr, node.getLineno(), node.getColumn());

        Node body = new Node(Token.EXPR_VOID, yield, lineno, column);

        if (node.getFilter() != null) {
            body = createIf(transform(node.getFilter()), body, null, lineno, column);
        }

        // Now walk loops in reverse to build up the body statement.
        int pushed = 0;
        try {
            for (int i = numLoops - 1; i >= 0; i--) {
                GeneratorExpressionLoop acl = loops.get(i);
                Scope loop =
                        createLoopNode(
                                null, // no label
                                acl.getLineno(),
                                acl.getColumn());
                parser.pushScope(loop);
                pushed++;
                body =
                        createForIn(
                                Token.LET,
                                loop,
                                iterators[i],
                                iteratedObjs[i],
                                body,
                                acl,
                                acl.isForEach(),
                                acl.isForOf());
            }
        } finally {
            for (int i = 0; i < pushed; i++) {
                parser.popScope();
            }
        }

        return body;
    }

    private Node transformIf(IfStatement n) {
        Node cond = transform(n.getCondition());
        Node ifTrue = transform(n.getThenPart());
        Node ifFalse = null;
        if (n.getElsePart() != null) {
            ifFalse = transform(n.getElsePart());
        }
        return createIf(cond, ifTrue, ifFalse, n.getLineno(), n.getColumn());
    }

    private Node transformInfix(InfixExpression node) {
        Node left = transform(node.getLeft());
        Node right = transform(node.getRight());
        Node binaryNode = createBinary(node.getType(), left, right);

        // Since we are transforming InfixExpression -> Node, we need to copy over the column and
        // line, but only for newly created nodes which have no column information set.
        // createBinary() may return a Node reference with the correct line/column.
        // Example: `true && <other_expr>` would return the `<other_expr>` reference from
        // createBinary
        boolean nodeCreated = (binaryNode != left) && (binaryNode != right);
        if (nodeCreated) {
            binaryNode.setLineColumnNumber(node.getLineno(), node.getColumn());
        }

        return binaryNode;
    }

    private Node transformLabeledStatement(LabeledStatement ls) {
        Label label = ls.getFirstLabel();
        Node statement = transform(ls.getStatement());

        // Make a target and put it _after_ the statement node.  Add in the
        // LABEL node, so breaks get the right target.
        Node breakTarget = Node.newTarget();
        Node block = new Node(Token.BLOCK, label, statement, breakTarget);
        label.target = breakTarget;

        return block;
    }

    private Node transformLetNode(LetNode node) {
        parser.pushScope(node);
        try {
            Node vars = transformVariableInitializers(node.getVariables());
            node.addChildToBack(vars);
            boolean letExpr = node.getType() == Token.LETEXPR;
            if (node.getBody() != null) {
                node.addChildToBack(transform(node.getBody()));
            }
            return node;
        } finally {
            parser.popScope();
        }
    }

    private Node transformLiteral(AstNode node) {
        // Trying to call super as a function. See 15.4.2 Static Semantics: HasDirectSuper
        // Note that this will need to change when classes are implemented, because in a class
        // constructor calling "super()" _is_ allowed.
        if (node.getParent() instanceof FunctionCall && node.getType() == Token.SUPER)
            parser.reportError("msg.super.shorthand.function");
        return node;
    }

    private Node transformName(Name node) {
        return node;
    }

    private Node transformNewExpr(NewExpression node) {
        Node nx = createCallOrNew(Token.NEW, transform(node.getTarget()));
        nx.setLineColumnNumber(node.getLineno(), node.getColumn());
        List<AstNode> args = node.getArguments();
        for (int i = 0; i < args.size(); i++) {
            AstNode arg = args.get(i);
            nx.addChildToBack(transform(arg));
        }
        if (node.getInitializer() != null) {
            nx.addChildToBack(transformObjectLiteral(node.getInitializer()));
        }
        return nx;
    }

    private Node transformNumber(NumberLiteral node) {
        return node;
    }

    private Node transformObjectLiteral(ObjectLiteral node) {
        if (node.isDestructuring()) {
            return node;
        }
        // createObjectLiteral rewrites its argument as object
        // creation plus object property entries, so later compiler
        // stages don't need to know about object literals.
        List<ObjectProperty> elems = node.getElements();
        Node object = new Node(Token.OBJECTLIT);
        object.setLineColumnNumber(node.getLineno(), node.getColumn());
        Object[] properties;
        if (elems.isEmpty()) {
            properties = ScriptRuntime.emptyArgs;
        } else {
            int size = elems.size(), i = 0;
            properties = new Object[size];
            for (ObjectProperty prop : elems) {
                Object propKey = Parser.getPropKey(prop.getLeft());
                if (propKey == null) {
                    Node theId = transform(prop.getLeft());
                    properties[i++] = theId;
                } else {
                    properties[i++] = propKey;
                }

                Node right = transform(prop.getRight());
                if (prop.isGetterMethod()) {
                    right = createUnary(Token.GET, right);
                } else if (prop.isSetterMethod()) {
                    right = createUnary(Token.SET, right);
                } else if (prop.isNormalMethod()) {
                    right = createUnary(Token.METHOD, right);
                }
                object.addChildToBack(right);
            }
        }
        object.putProp(Node.OBJECT_IDS_PROP, properties);
        return object;
    }

    private Node transformParenExpr(ParenthesizedExpression node) {
        AstNode expr = node.getExpression();
        while (expr instanceof ParenthesizedExpression) {
            expr = ((ParenthesizedExpression) expr).getExpression();
        }
        Node result = transform(expr);
        result.putProp(Node.PARENTHESIZED_PROP, Boolean.TRUE);
        return result;
    }

    private Node transformComputedPropertyKey(ComputedPropertyKey node) {
        Node transformedExpression = transform(node.getExpression());
        return new Node(node.type, transformedExpression);
    }

    private Node transformPropertyGet(PropertyGet node) {
        Node target = transform(node.getTarget());
        String name = node.getProperty().getIdentifier();
        return createPropertyGet(target, null, name, 0, node.type);
    }

    private Node transformTemplateLiteral(TemplateLiteral node) {
        List<AstNode> elems = node.getElements();
        // start with an empty string to ensure ToString() for each substitution
        Node pn = Node.newString("");
        for (AstNode elem : elems) {
            if (elem.getType() != Token.TEMPLATE_CHARS) {
                pn = createBinary(Token.ADD, pn, transform(elem));
            } else {
                TemplateCharacters chars = (TemplateCharacters) elem;
                // skip empty parts, e.g. `xx${expr}xx` where xx denotes the empty string
                String value = chars.getValue();
                if (value.length() > 0) {
                    pn = createBinary(Token.ADD, pn, Node.newString(value));
                }
            }
        }
        return pn;
    }

    private Node transformTemplateLiteralCall(TaggedTemplateLiteral node) {
        Node transformedTarget = transform(node.getTarget());
        Node call = createCallOrNew(Token.CALL, transformedTarget);
        call.setLineColumnNumber(node.getLineno(), node.getColumn());
        if (transformedTarget.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
            call.putIntProp(Node.SUPER_PROPERTY_ACCESS, 1);
        }
        TemplateLiteral templateLiteral = (TemplateLiteral) node.getTemplateLiteral();
        List<AstNode> elems = templateLiteral.getElements();
        call.addChildToBack(templateLiteral);
        for (AstNode elem : elems) {
            if (elem.getType() != Token.TEMPLATE_CHARS) {
                call.addChildToBack(transform(elem));
            }
        }
        parser.currentScriptOrFn.addTemplateLiteral(templateLiteral);
        return call;
    }

    private Node transformRegExp(RegExpLiteral node) {
        parser.currentScriptOrFn.addRegExp(node);
        return node;
    }

    private Node transformReturn(ReturnStatement node) {
        AstNode rv = node.getReturnValue();
        Node value = rv == null ? null : transform(rv);
        return rv == null
                ? new Node(Token.RETURN, node.getLineno(), node.getColumn())
                : new Node(Token.RETURN, value, node.getLineno(), node.getColumn());
    }

    private Node transformScript(ScriptNode node) {
        if (parser.currentScope != null) Kit.codeBug();
        parser.currentScope = node;
        Node body = new Node(Token.BLOCK);
        for (Node kid : node) {
            body.addChildToBack(transform((AstNode) kid));
        }
        node.removeChildren();
        Node children = body.getFirstChild();
        if (children != null) {
            node.addChildrenToBack(children);
        }
        return node;
    }

    private Node transformString(StringLiteral node) {
        Node stringNode = Node.newString(node.getValue());
        stringNode.setLineColumnNumber(node.getLineno(), node.getColumn());
        return stringNode;
    }

    private Node transformSwitch(SwitchStatement node) {
        // The switch will be rewritten from:
        //
        // switch (expr) {
        //   case test1: statements1;
        //   ...
        //   default: statementsDefault;
        //   ...
        //   case testN: statementsN;
        // }
        //
        // to:
        //
        // {
        //     switch (expr) {
        //       case test1: goto label1;
        //       ...
        //       case testN: goto labelN;
        //     }
        //     goto labelDefault;
        //   label1:
        //     statements1;
        //   ...
        //   labelDefault:
        //     statementsDefault;
        //   ...
        //   labelN:
        //     statementsN;
        //   breakLabel:
        // }
        //
        // where inside switch each "break;" without label will be replaced
        // by "goto breakLabel".
        //
        // If the original switch does not have the default label, then
        // after the switch he transformed code would contain this goto:
        //     goto breakLabel;
        // instead of:
        //     goto labelDefault;

        Node switchExpr = transform(node.getExpression());
        node.addChildToBack(switchExpr);

        Node block = new Node(Token.BLOCK, node, node.getLineno(), node.getColumn());

        for (SwitchCase sc : node.getCases()) {
            AstNode expr = sc.getExpression();
            Node caseExpr = null;

            if (expr != null) {
                caseExpr = transform(expr);
            }

            List<AstNode> stmts = sc.getStatements();
            Node body = new Block();
            if (stmts != null) {
                for (AstNode kid : stmts) {
                    body.addChildToBack(transform(kid));
                }
            }
            addSwitchCase(block, caseExpr, body);
        }
        closeSwitch(block);
        return block;
    }

    private Node transformThrow(ThrowStatement node) {
        Node value = transform(node.getExpression());
        value.setLineColumnNumber(node.getLineno(), node.getColumn());
        Node nx = new Node(Token.THROW, value);
        nx.setLineColumnNumber(node.getLineno(), node.getColumn());
        return nx;
    }

    private Node transformTry(TryStatement node) {
        Node tryBlock = transform(node.getTryBlock());

        Node catchBlocks = new Block();
        for (CatchClause cc : node.getCatchClauses()) {
            Name varName = cc.getVarName();
            Node catchCond = null;
            Node varNameNode = null;

            if (varName != null) {
                varNameNode = parser.createName(varName.getIdentifier());

                AstNode ccc = cc.getCatchCondition();
                if (ccc != null) {
                    catchCond = transform(ccc);
                } else {
                    catchCond = new EmptyExpression();
                }
            }

            Node body = transform(cc.getBody());

            catchBlocks.addChildToBack(
                    createCatch(varNameNode, catchCond, body, cc.getLineno(), cc.getColumn()));
        }
        Node finallyBlock = null;
        if (node.getFinallyBlock() != null) {
            finallyBlock = transform(node.getFinallyBlock());
        }
        return createTryCatchFinally(
                tryBlock, catchBlocks, finallyBlock, node.getLineno(), node.getColumn());
    }

    private Node transformUnary(UnaryExpression node) {
        int type = node.getType();
        if (type == Token.DEFAULTNAMESPACE) {
            return transformDefaultXmlNamespace(node);
        }

        Node child = transform(node.getOperand());
        return createUnary(type, child);
    }

    private Node transformUpdate(UpdateExpression node) {
        int type = node.getType();
        Node child = transform(node.getOperand());
        return createIncDec(type, node.isPostfix(), child);
    }

    private Node transformVariables(VariableDeclaration node) {
        transformVariableInitializers(node);
        return node;
    }

    private Node transformVariableInitializers(VariableDeclaration node) {
        List<VariableInitializer> vars = node.getVariables();
        for (VariableInitializer var : vars) {
            AstNode target = var.getTarget();
            AstNode init = var.getInitializer();

            Node left = null;
            if (var.isDestructuring()) {
                left = target;
            } else {
                left = transform(target);
            }

            Node right = null;
            if (init != null) {
                right = transform(init);
            }

            if (var.isDestructuring()) {
                if (right == null) { // TODO:  should this ever happen?
                    node.addChildToBack(left);
                } else {
                    astNodePos.push(var);
                    try {
                        Node d =
                                parser.createDestructuringAssignment(
                                        node.getType(), left, right, this::transform);
                        node.addChildToBack(d);
                    } finally {
                        astNodePos.pop();
                    }
                }
            } else {
                if (right != null) {
                    left.addChildToBack(right);
                }
                node.addChildToBack(left);
            }
        }
        return node;
    }

    private Node transformWhileLoop(WhileLoop loop) {
        loop.setType(Token.LOOP);
        parser.pushScope(loop);
        try {
            Node cond = transform(loop.getCondition());
            Node body = transform(loop.getBody());
            return createLoop(loop, LOOP_WHILE, body, cond, null, null);
        } finally {
            parser.popScope();
        }
    }

    private Node transformWith(WithStatement node) {
        Node expr = transform(node.getExpression());
        Node stmt = transform(node.getStatement());
        return createWith(expr, stmt, node.getLineno(), node.getColumn());
    }

    private Node transformYield(Yield node) {
        Node kid = node.getValue() == null ? null : transform(node.getValue());
        if (kid != null) return new Node(node.getType(), kid, node.getLineno(), node.getColumn());
        return new Node(node.getType(), node.getLineno(), node.getColumn());
    }

    private Node transformXmlLiteral(XmlLiteral node) {
        // a literal like <foo>{bar}</foo> is rewritten as
        //   new XML("<foo>" + bar + "</foo>");

        Node pnXML = new Node(Token.NEW, node.getLineno(), node.getColumn());
        List<XmlFragment> frags = node.getFragments();

        XmlString first = (XmlString) frags.get(0);
        boolean anon = first.getXml().trim().startsWith("<>");
        pnXML.addChildToBack(parser.createName(anon ? "XMLList" : "XML"));

        Node pn = null;
        for (XmlFragment frag : frags) {
            if (frag instanceof XmlString) {
                String xml = ((XmlString) frag).getXml();
                if (pn == null) {
                    pn = createString(xml);
                } else {
                    pn = createBinary(Token.ADD, pn, createString(xml));
                }
            } else {
                XmlExpression xexpr = (XmlExpression) frag;
                boolean isXmlAttr = xexpr.isXmlAttribute();
                Node expr;
                if (xexpr.getExpression() instanceof EmptyExpression) {
                    expr = createString("");
                } else {
                    expr = transform(xexpr.getExpression());
                }
                if (isXmlAttr) {
                    // Need to put the result in double quotes
                    expr = createUnary(Token.ESCXMLATTR, expr);
                    Node prepend = createBinary(Token.ADD, createString("\""), expr);
                    expr = createBinary(Token.ADD, prepend, createString("\""));
                } else {
                    expr = createUnary(Token.ESCXMLTEXT, expr);
                }
                pn = createBinary(Token.ADD, pn, expr);
            }
        }

        pnXML.addChildToBack(pn);
        return pnXML;
    }

    private Node transformXmlMemberGet(XmlMemberGet node) {
        XmlRef ref = node.getMemberRef();
        Node pn = transform(node.getLeft());
        int flags = ref.isAttributeAccess() ? Node.ATTRIBUTE_FLAG : 0;
        if (node.getType() == Token.DOTDOT) {
            flags |= Node.DESCENDANTS_FLAG;
        }
        return transformXmlRef(pn, ref, flags);
    }

    // We get here if we weren't a child of a . or .. infix node
    private Node transformXmlRef(XmlRef node) {
        int memberTypeFlags = node.isAttributeAccess() ? Node.ATTRIBUTE_FLAG : 0;
        return transformXmlRef(null, node, memberTypeFlags);
    }

    private Node transformXmlRef(Node pn, XmlRef node, int memberTypeFlags) {
        Name namespace = node.getNamespace();
        String ns = namespace != null ? namespace.getIdentifier() : null;
        if (node instanceof XmlPropRef) {
            String name = ((XmlPropRef) node).getPropName().getIdentifier();
            return createPropertyGet(pn, ns, name, memberTypeFlags, node.type);
        }
        Node expr = transform(((XmlElemRef) node).getExpression());
        return createElementGet(pn, ns, expr, memberTypeFlags);
    }

    private Node transformDefaultXmlNamespace(UnaryExpression node) {
        Node child = transform(node.getOperand());
        return createUnary(Token.DEFAULTNAMESPACE, child);
    }

    private Node transformGeneratorMethodDefinition(GeneratorMethodDefinition node) {
        // Unwrap the "temporary" AST node
        return transform(node.getMethodName());
    }

    /** If caseExpression argument is null it indicates a default label. */
    private static void addSwitchCase(Node switchBlock, Node caseExpression, Node statements) {
        if (switchBlock.getType() != Token.BLOCK) throw Kit.codeBug();
        Jump switchNode = (Jump) switchBlock.getFirstChild();
        if (switchNode.getType() != Token.SWITCH) throw Kit.codeBug();

        Node gotoTarget = Node.newTarget();
        if (caseExpression != null) {
            Jump caseNode = new Jump(Token.CASE, caseExpression);
            caseNode.target = gotoTarget;
            switchNode.addChildToBack(caseNode);
        } else {
            switchNode.setDefault(gotoTarget);
        }
        switchBlock.addChildToBack(gotoTarget);
        switchBlock.addChildToBack(statements);
    }

    private static void closeSwitch(Node switchBlock) {
        if (switchBlock.getType() != Token.BLOCK) throw Kit.codeBug();
        Jump switchNode = (Jump) switchBlock.getFirstChild();
        if (switchNode.getType() != Token.SWITCH) throw Kit.codeBug();

        Node switchBreakTarget = Node.newTarget();
        // switchNode.target is only used by NodeTransformer
        // to detect switch end
        switchNode.target = switchBreakTarget;

        Node defaultTarget = switchNode.getDefault();
        if (defaultTarget == null) {
            defaultTarget = switchBreakTarget;
        }

        switchBlock.addChildAfter(makeJump(Token.GOTO, defaultTarget), switchNode);
        switchBlock.addChildToBack(switchBreakTarget);
    }

    private static Node createExprStatementNoReturn(Node expr, int lineno, int column) {
        return new Node(Token.EXPR_VOID, expr, lineno, column);
    }

    private static Node createString(String string) {
        return Node.newString(string);
    }

    /**
     * Catch clause of try/catch/finally
     *
     * @param varName the name of the variable to bind to the exception
     * @param catchCond the condition under which to catch the exception. May be null if no
     *     condition is given.
     * @param stmts the statements in the catch clause
     * @param lineno the starting line number of the catch clause
     * @param column the starting column number of the catch clause
     */
    private Node createCatch(Node varName, Node catchCond, Node stmts, int lineno, int column) {
        if (varName == null) {
            varName = new Node(Token.EMPTY);
        }
        if (catchCond == null) {
            catchCond = new Node(Token.EMPTY);
        }
        return new Node(Token.CATCH, varName, catchCond, stmts, lineno, column);
    }

    private static Node initFunction(
            FunctionNode fnNode, int functionIndex, Node statements, int functionType) {
        fnNode.setFunctionType(functionType);
        fnNode.addChildToBack(statements);

        int functionCount = fnNode.getFunctionCount();
        if (functionCount != 0) {
            // Functions containing other functions require activation objects
            fnNode.setRequiresActivation();
        }

        if (functionType == FunctionNode.FUNCTION_EXPRESSION) {
            Name name = fnNode.getFunctionName();
            if (name != null
                    && name.length() != 0
                    && fnNode.getSymbol(name.getIdentifier()) == null) {
                // A function expression needs to have its name as a
                // variable (if it isn't already allocated as a variable).
                // See ECMA Ch. 13.  We add code to the beginning of the
                // function to initialize a local variable of the
                // function's name to the function value, but only if the
                // function doesn't already define a formal parameter, var,
                // or nested function with the same name.
                fnNode.putSymbol(new Symbol(Token.FUNCTION, name.getIdentifier()));
                Node setFn =
                        new Node(
                                Token.EXPR_VOID,
                                new Node(
                                        Token.SETNAME,
                                        Node.newString(Token.BINDNAME, name.getIdentifier()),
                                        new Node(Token.THISFN)));
                statements.addChildrenToFront(setFn);
            }
        }

        // Add return to end if needed.
        Node lastStmt = statements.getLastChild();
        if (lastStmt == null || lastStmt.getType() != Token.RETURN) {
            statements.addChildToBack(new Node(Token.RETURN));
        }

        Node result = Node.newString(Token.FUNCTION, fnNode.getName());
        result.putIntProp(Node.FUNCTION_PROP, functionIndex);
        return result;
    }

    /**
     * Create loop node. The code generator will later call
     * createWhile|createDoWhile|createFor|createForIn to finish loop generation.
     */
    private Scope createLoopNode(Node loopLabel, int lineno, int column) {
        Scope result = parser.createScopeNode(Token.LOOP, lineno, column);
        if (loopLabel != null) {
            ((Jump) loopLabel).setLoop(result);
        }
        return result;
    }

    private static Node createFor(Scope loop, Node init, Node test, Node incr, Node body) {
        if (init.getType() == Token.LET) {
            // rewrite "for (let i=s; i < N; i++)..." as
            // "let (i=s) { for (; i < N; i++)..." so that "s" is evaluated
            // outside the scope of the for.
            Scope let = Scope.splitScope(loop);
            let.setType(Token.LET);
            let.addChildrenToBack(init);
            let.addChildToBack(createLoop(loop, LOOP_FOR, body, test, new Node(Token.EMPTY), incr));
            return let;
        }
        return createLoop(loop, LOOP_FOR, body, test, init, incr);
    }

    private static Node createLoop(
            Jump loop, int loopType, Node body, Node cond, Node init, Node incr) {
        Node bodyTarget = Node.newTarget();
        Node condTarget = Node.newTarget();
        if (loopType == LOOP_FOR && cond.getType() == Token.EMPTY) {
            cond = new Node(Token.TRUE);
        }
        Jump IFEQ = new Jump(Token.IFEQ, cond);
        IFEQ.target = bodyTarget;
        Node breakTarget = Node.newTarget();

        loop.addChildToBack(bodyTarget);
        loop.addChildrenToBack(body);
        if (loopType == LOOP_WHILE || loopType == LOOP_FOR) {
            // propagate lineno to condition
            loop.addChildrenToBack(new Node(Token.EMPTY, loop.getLineno(), loop.getColumn()));
        }
        loop.addChildToBack(condTarget);
        loop.addChildToBack(IFEQ);
        loop.addChildToBack(breakTarget);

        loop.target = breakTarget;
        Node continueTarget = condTarget;

        if (loopType == LOOP_WHILE || loopType == LOOP_FOR) {
            // Just add a GOTO to the condition in the do..while
            loop.addChildToFront(makeJump(Token.GOTO, condTarget));

            if (loopType == LOOP_FOR) {
                int initType = init.getType();
                if (initType != Token.EMPTY) {
                    if (initType != Token.VAR && initType != Token.LET) {
                        init = new Node(Token.EXPR_VOID, init);
                    }
                    loop.addChildToFront(init);
                }
                Node incrTarget = Node.newTarget();
                loop.addChildAfter(incrTarget, body);
                if (incr.getType() != Token.EMPTY) {
                    incr = new Node(Token.EXPR_VOID, incr);
                    loop.addChildAfter(incr, incrTarget);
                }
                continueTarget = incrTarget;
            }
        }

        loop.setContinue(continueTarget);
        return loop;
    }

    /** Generate IR for a for..in loop. */
    private Node createForIn(
            int declType,
            Node loop,
            Node lhs,
            Node obj,
            Node body,
            AstNode ast,
            boolean isForEach,
            boolean isForOf) {
        astNodePos.push(ast);
        try {
            int destructuring = -1;
            int destructuringLen = 0;
            Node lvalue;
            int type = lhs.getType();
            if (type == Token.VAR || type == Token.LET) {
                Node kid = lhs.getLastChild();
                int kidType = kid.getType();
                if (kidType == Token.ARRAYLIT || kidType == Token.OBJECTLIT) {
                    type = destructuring = kidType;
                    lvalue = kid;
                    destructuringLen = 0;
                    if (kid instanceof ArrayLiteral)
                        destructuringLen = ((ArrayLiteral) kid).getDestructuringLength();
                } else if (kidType == Token.NAME) {
                    lvalue = Node.newString(Token.NAME, kid.getString());
                } else {
                    parser.reportError("msg.bad.for.in.lhs");
                    return null;
                }
            } else if (type == Token.ARRAYLIT || type == Token.OBJECTLIT) {
                destructuring = type;
                lvalue = lhs;
                destructuringLen = 0;
                if (lhs instanceof ArrayLiteral)
                    destructuringLen = ((ArrayLiteral) lhs).getDestructuringLength();
            } else {
                lvalue = makeReference(lhs);
                if (lvalue == null) {
                    parser.reportError("msg.bad.for.in.lhs");
                    return null;
                }
            }

            Node localBlock = new Node(Token.LOCAL_BLOCK);
            int initType =
                    isForEach
                            ? Token.ENUM_INIT_VALUES
                            : isForOf
                                    ? Token.ENUM_INIT_VALUES_IN_ORDER
                                    : (destructuring != -1
                                            ? Token.ENUM_INIT_ARRAY
                                            : Token.ENUM_INIT_KEYS);
            Node init = new Node(initType, obj);
            init.putProp(Node.LOCAL_BLOCK_PROP, localBlock);
            Node cond = new Node(Token.ENUM_NEXT);
            cond.putProp(Node.LOCAL_BLOCK_PROP, localBlock);
            Node id = new Node(Token.ENUM_ID);
            id.putProp(Node.LOCAL_BLOCK_PROP, localBlock);

            Node newBody = new Node(Token.BLOCK);
            Node assign;
            if (destructuring != -1) {
                assign =
                        parser.createDestructuringAssignment(declType, lvalue, id, this::transform);
                if (!isForEach
                        && !isForOf
                        && (destructuring == Token.OBJECTLIT || destructuringLen != 2)) {
                    // destructuring assignment is only allowed in for..each or
                    // with an array type of length 2 (to hold key and value)
                    parser.reportError("msg.bad.for.in.destruct");
                }
            } else {
                assign = parser.simpleAssignment(lvalue, id);
            }
            newBody.addChildToBack(new Node(Token.EXPR_VOID, assign));
            newBody.addChildToBack(body);

            loop = createLoop((Jump) loop, LOOP_WHILE, newBody, cond, null, null);
            loop.addChildToFront(init);
            if (type == Token.VAR || type == Token.LET) loop.addChildToFront(lhs);
            localBlock.addChildToBack(loop);

            return localBlock;
        } finally {
            astNodePos.pop();
        }
    }

    /**
     * Try/Catch/Finally
     *
     * <p>The IRFactory tries to express as much as possible in the tree; the responsibilities
     * remaining for Codegen are to add the Java handlers: (Either (but not both) of TARGET and
     * FINALLY might not be defined)
     *
     * <p>- a catch handler for javascript exceptions that unwraps the exception onto the stack and
     * GOTOes to the catch target
     *
     * <p>- a finally handler
     *
     * <p>... and a goto to GOTO around these handlers.
     */
    private Node createTryCatchFinally(
            Node tryBlock, Node catchBlocks, Node finallyBlock, int lineno, int column) {
        boolean hasFinally =
                (finallyBlock != null)
                        && (finallyBlock.getType() != Token.BLOCK || finallyBlock.hasChildren());

        // short circuit
        if (tryBlock.getType() == Token.BLOCK && !tryBlock.hasChildren() && !hasFinally) {
            return tryBlock;
        }

        boolean hasCatch = catchBlocks.hasChildren();

        // short circuit
        if (!hasFinally && !hasCatch) {
            // bc finally might be an empty block...
            return tryBlock;
        }

        Node handlerBlock = new Node(Token.LOCAL_BLOCK);
        Jump pn = new Jump(Token.TRY, tryBlock);
        pn.setLineColumnNumber(lineno, column);
        pn.putProp(Node.LOCAL_BLOCK_PROP, handlerBlock);

        if (hasCatch) {
            // jump around catch code
            Node endCatch = Node.newTarget();
            pn.addChildToBack(makeJump(Token.GOTO, endCatch));

            // make a TARGET for the catch that the tcf node knows about
            Node catchTarget = Node.newTarget();
            pn.target = catchTarget;
            // mark it
            pn.addChildToBack(catchTarget);

            //
            //  Given
            //
            //   try {
            //       tryBlock;
            //   } catch (e if condition1) {
            //       something1;
            //   ...
            //
            //   } catch (e if conditionN) {
            //       somethingN;
            //   } catch (e) {
            //       somethingDefault;
            //   }
            //
            //  rewrite as
            //
            //   try {
            //       tryBlock;
            //       goto after_catch:
            //   } catch (x) {
            //       with (newCatchScope(e, x)) {
            //           if (condition1) {
            //               something1;
            //               goto after_catch;
            //           }
            //       }
            //   ...
            //       with (newCatchScope(e, x)) {
            //           if (conditionN) {
            //               somethingN;
            //               goto after_catch;
            //           }
            //       }
            //       with (newCatchScope(e, x)) {
            //           somethingDefault;
            //           goto after_catch;
            //       }
            //   }
            // after_catch:
            //
            // If there is no default catch, then the last with block
            // around  "somethingDefault;" is replaced by "rethrow;"

            // It is assumed that catch handler generation will store
            // exception object in handlerBlock register

            // Block with local for exception scope objects
            Node catchScopeBlock = new Node(Token.LOCAL_BLOCK);

            // expects catchblocks children to be (cond block) pairs.
            Node cb = catchBlocks.getFirstChild();
            boolean hasDefault = false;
            int scopeIndex = 0;
            while (cb != null) {
                int catchLineno = cb.getLineno(), catchColumn = cb.getColumn();

                Node name = cb.getFirstChild();
                Node cond = name.getNext();
                Node catchStatement = cond.getNext();
                cb.removeChild(name);
                cb.removeChild(cond);
                cb.removeChild(catchStatement);

                // Add goto to the catch statement to jump out of catch
                // but prefix it with LEAVEWITH since try..catch produces
                // "with"code in order to limit the scope of the exception
                // object.
                catchStatement.addChildToBack(new Node(Token.LEAVEWITH));
                catchStatement.addChildToBack(makeJump(Token.GOTO, endCatch));

                // Create condition "if" when present
                Node condStmt;
                if (cond.getType() == Token.EMPTY) {
                    condStmt = catchStatement;
                    hasDefault = true;
                } else {
                    condStmt = createIf(cond, catchStatement, null, catchLineno, catchColumn);
                }

                // Generate code to create the scope object and store
                // it in catchScopeBlock register
                Node catchScope = new Node(Token.CATCH_SCOPE, name, createUseLocal(handlerBlock));
                catchScope.putProp(Node.LOCAL_BLOCK_PROP, catchScopeBlock);
                catchScope.putIntProp(Node.CATCH_SCOPE_PROP, scopeIndex);
                catchScopeBlock.addChildToBack(catchScope);

                // Add with statement based on catch scope object
                catchScopeBlock.addChildToBack(
                        createWith(
                                createUseLocal(catchScopeBlock),
                                condStmt,
                                catchLineno,
                                catchColumn));

                // move to next cb
                cb = cb.getNext();
                ++scopeIndex;
            }
            pn.addChildToBack(catchScopeBlock);
            if (!hasDefault) {
                // Generate code to rethrow if no catch clause was executed
                Node rethrow = new Node(Token.RETHROW);
                rethrow.putProp(Node.LOCAL_BLOCK_PROP, handlerBlock);
                pn.addChildToBack(rethrow);
            }

            pn.addChildToBack(endCatch);
        }

        if (hasFinally) {
            Node finallyTarget = Node.newTarget();
            pn.setFinally(finallyTarget);

            // add jsr finally to the try block
            pn.addChildToBack(makeJump(Token.JSR, finallyTarget));

            // jump around finally code
            Node finallyEnd = Node.newTarget();
            pn.addChildToBack(makeJump(Token.GOTO, finallyEnd));

            pn.addChildToBack(finallyTarget);
            Node fBlock = new Node(Token.FINALLY, finallyBlock);
            fBlock.putProp(Node.LOCAL_BLOCK_PROP, handlerBlock);
            pn.addChildToBack(fBlock);

            pn.addChildToBack(finallyEnd);
        }
        handlerBlock.addChildToBack(pn);
        return handlerBlock;
    }

    private Node createWith(Node obj, Node body, int lineno, int column) {
        parser.setRequiresActivation();
        Node result = new Node(Token.BLOCK, lineno, column);
        result.addChildToBack(new Node(Token.ENTERWITH, obj));
        Node bodyNode = new Node(Token.WITH, body, lineno, column);
        result.addChildrenToBack(bodyNode);
        result.addChildToBack(new Node(Token.LEAVEWITH));
        return result;
    }

    private static Node createIf(Node cond, Node ifTrue, Node ifFalse, int lineno, int column) {
        int condStatus = isAlwaysDefinedBoolean(cond);
        if (condStatus == ALWAYS_TRUE_BOOLEAN) {
            return ifTrue;
        } else if (condStatus == ALWAYS_FALSE_BOOLEAN) {
            if (ifFalse != null) {
                return ifFalse;
            }
            // Replace if (false) xxx by empty block
            return new Node(Token.BLOCK, lineno, column);
        }

        Node result = new Node(Token.BLOCK, lineno, column);
        Node ifNotTarget = Node.newTarget();
        Jump IFNE = new Jump(Token.IFNE, cond);
        IFNE.target = ifNotTarget;

        result.addChildToBack(IFNE);
        result.addChildrenToBack(ifTrue);

        if (ifFalse != null) {
            Node endTarget = Node.newTarget();
            result.addChildToBack(makeJump(Token.GOTO, endTarget));
            result.addChildToBack(ifNotTarget);
            result.addChildrenToBack(ifFalse);
            result.addChildToBack(endTarget);
        } else {
            result.addChildToBack(ifNotTarget);
        }

        if (cond.getFirstChild() != null) {
            Node conditionalChild = cond.getFirstChild();
            result.setLineColumnNumber(conditionalChild.getLineno(), conditionalChild.getColumn());
        }

        return result;
    }

    private static Node createCondExpr(Node cond, Node ifTrue, Node ifFalse) {
        int condStatus = isAlwaysDefinedBoolean(cond);
        if (condStatus == ALWAYS_TRUE_BOOLEAN) {
            return ifTrue;
        } else if (condStatus == ALWAYS_FALSE_BOOLEAN) {
            return ifFalse;
        }
        return new Node(Token.HOOK, cond, ifTrue, ifFalse);
    }

    private static Node createUnary(int nodeType, Node child) {
        int childType = child.getType();
        switch (nodeType) {
            case Token.DELPROP:
                {
                    Node n;
                    if (childType == Token.NAME) {
                        // Transform Delete(Name "a")
                        //  to Delete(Bind("a"), String("a"))
                        child.setType(Token.BINDNAME);
                        Node right = Node.newString(child.getString());
                        n = new Node(nodeType, child, right);
                    } else if (childType == Token.GETPROP || childType == Token.GETELEM) {
                        Node left = child.getFirstChild();
                        Node right = child.getLastChild();
                        child.removeChild(left);
                        child.removeChild(right);
                        n = new Node(nodeType, left, right);
                    } else if (childType == Token.GET_REF) {
                        Node ref = child.getFirstChild();
                        child.removeChild(ref);
                        n = new Node(Token.DEL_REF, ref);
                    } else {
                        // Always evaluate delete operand, see ES5 11.4.1 & bug #726121
                        n = new Node(nodeType, new Node(Token.TRUE), child);
                    }
                    if (child.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
                        n.putIntProp(Node.SUPER_PROPERTY_ACCESS, 1);
                    }
                    return n;
                }
            case Token.TYPEOF:
                if (childType == Token.NAME) {
                    child.setType(Token.TYPEOFNAME);
                    return child;
                }
                break;
            case Token.BITNOT:
                if (childType == Token.NUMBER) {
                    int value = ScriptRuntime.toInt32(child.getDouble());
                    child.setDouble(~value);
                    return child;
                }
                break;
            case Token.NEG:
                if (childType == Token.NUMBER) {
                    child.setDouble(-child.getDouble());
                    return child;
                }
                break;
            case Token.NOT:
                {
                    int status = isAlwaysDefinedBoolean(child);
                    if (status != 0) {
                        int type;
                        if (status == ALWAYS_TRUE_BOOLEAN) {
                            type = Token.FALSE;
                        } else {
                            type = Token.TRUE;
                        }
                        if (childType == Token.TRUE || childType == Token.FALSE) {
                            child.setType(type);
                            return child;
                        }
                        return new Node(type);
                    }
                    break;
                }
        }
        return new Node(nodeType, child);
    }

    private Node createCallOrNew(int nodeType, Node child) {
        int type = Node.NON_SPECIALCALL;
        if (child.getType() == Token.NAME) {
            String name = child.getString();
            if (name.equals("eval")) {
                type = Node.SPECIALCALL_EVAL;
            } else if (name.equals("With")) {
                type = Node.SPECIALCALL_WITH;
            }
        } else if (child.getType() == Token.GETPROP) {
            String name = child.getLastChild().getString();
            if (name.equals("eval")) {
                type = Node.SPECIALCALL_EVAL;
            }
        }
        Node node = new Node(nodeType, child);
        if (type != Node.NON_SPECIALCALL) {
            // Calls to these functions require activation objects.
            parser.setRequiresActivation();
            node.putIntProp(Node.SPECIALCALL_PROP, type);
        }
        return node;
    }

    private static Node createIncDec(int nodeType, boolean post, Node child) {
        child = makeReference(child);
        int childType = child.getType();

        switch (childType) {
            case Token.NAME:
            case Token.GETPROP:
            case Token.GETELEM:
            case Token.GET_REF:
                {
                    Node n = new Node(nodeType, child);
                    int incrDecrMask = 0;
                    if (nodeType == Token.DEC) {
                        incrDecrMask |= Node.DECR_FLAG;
                    }
                    if (post) {
                        incrDecrMask |= Node.POST_FLAG;
                    }
                    n.putIntProp(Node.INCRDECR_PROP, incrDecrMask);
                    return n;
                }
        }
        throw Kit.codeBug();
    }

    private Node createPropertyGet(
            Node target, String namespace, String name, int memberTypeFlags, int type) {
        if (namespace == null && memberTypeFlags == 0) {
            if (target == null) {
                return parser.createName(name);
            }
            parser.checkActivationName(name, Token.GETPROP);
            if (ScriptRuntime.isSpecialProperty(name)) {
                if (target.getType() == Token.SUPER) {
                    // We have an access to super.__proto__ or super.__parent__.
                    // This needs to behave in the same way as this.__proto__ - it really is not
                    // obvious why, but you can test it in v8 or any other engine. So, we just
                    // replace SUPER with THIS in the AST. It's a bit hacky, but it works - see the
                    // test cases in SuperTest!
                    if (!(target instanceof KeywordLiteral)) {
                        throw Kit.codeBug();
                    }
                    KeywordLiteral oldTarget = (KeywordLiteral) target;
                    target =
                            new KeywordLiteral(
                                    oldTarget.getPosition(), oldTarget.getLength(), Token.THIS);
                    target.setLineColumnNumber(oldTarget.getLineno(), oldTarget.getColumn());
                }

                Node ref = new Node(Token.REF_SPECIAL, target);
                ref.putProp(Node.NAME_PROP, name);
                Node getRef = new Node(Token.GET_REF, ref);
                if (type == Token.QUESTION_DOT) {
                    ref.putIntProp(Node.OPTIONAL_CHAINING, 1);
                    getRef.putIntProp(Node.OPTIONAL_CHAINING, 1);
                }
                return getRef;
            }

            Node node = new Node(Token.GETPROP, target, Node.newString(name));
            if (type == Token.QUESTION_DOT) {
                node.putIntProp(Node.OPTIONAL_CHAINING, 1);
            }
            if (target.getType() == Token.SUPER) {
                node.putIntProp(Node.SUPER_PROPERTY_ACCESS, 1);
            }
            return node;
        }
        Node elem = Node.newString(name);
        memberTypeFlags |= Node.PROPERTY_FLAG;
        return createMemberRefGet(target, namespace, elem, memberTypeFlags);
    }

    /**
     * @param target the node before the LB
     * @param namespace optional namespace
     * @param elem the node in the brackets
     * @param memberTypeFlags E4X flags
     */
    private Node createElementGet(Node target, String namespace, Node elem, int memberTypeFlags) {
        // OPT: could optimize to createPropertyGet
        // iff elem is string that can not be number
        if (namespace == null && memberTypeFlags == 0) {
            // stand-alone [aaa] as primary expression is array literal
            // declaration and should not come here!
            if (target == null) throw Kit.codeBug();
            return new Node(Token.GETELEM, target, elem);
        }
        return createMemberRefGet(target, namespace, elem, memberTypeFlags);
    }

    private Node createMemberRefGet(Node target, String namespace, Node elem, int memberTypeFlags) {
        Node nsNode = null;
        if (namespace != null) {
            // See 11.1.2 in ECMA 357
            if (namespace.equals("*")) {
                nsNode = new Node(Token.NULL);
            } else {
                nsNode = parser.createName(namespace);
            }
        }
        Node ref;
        if (target == null) {
            if (namespace == null) {
                ref = new Node(Token.REF_NAME, elem);
            } else {
                ref = new Node(Token.REF_NS_NAME, nsNode, elem);
            }
        } else {
            if (namespace == null) {
                ref = new Node(Token.REF_MEMBER, target, elem);
            } else {
                ref = new Node(Token.REF_NS_MEMBER, target, nsNode, elem);
            }
        }
        if (memberTypeFlags != 0) {
            ref.putIntProp(Node.MEMBER_TYPE_PROP, memberTypeFlags);
        }
        return new Node(Token.GET_REF, ref);
    }

    private static Node createBinary(int nodeType, Node left, Node right) {
        switch (nodeType) {
            case Token.ADD:
                // numerical addition and string concatenation
                if (left.type == Token.STRING) {
                    String s2;
                    if (right.type == Token.STRING) {
                        s2 = right.getString();
                    } else if (right.type == Token.NUMBER) {
                        s2 = ScriptRuntime.numberToString(right.getDouble(), 10);
                    } else {
                        break;
                    }
                    String s1 = left.getString();
                    left.setString(s1.concat(s2));
                    return left;
                } else if (left.type == Token.NUMBER) {
                    if (right.type == Token.NUMBER) {
                        left.setDouble(left.getDouble() + right.getDouble());
                        return left;
                    } else if (right.type == Token.STRING) {
                        String s1, s2;
                        s1 = ScriptRuntime.numberToString(left.getDouble(), 10);
                        s2 = right.getString();
                        right.setString(s1.concat(s2));
                        return right;
                    }
                }
                // can't do anything if we don't know  both types - since
                // 0 + object is supposed to call toString on the object and do
                // string concatenation rather than addition
                break;

            case Token.SUB:
                // numerical subtraction
                if (left.type == Token.NUMBER) {
                    double ld = left.getDouble();
                    if (right.type == Token.NUMBER) {
                        // both numbers
                        left.setDouble(ld - right.getDouble());
                        return left;
                    } else if (ld == 0.0) {
                        // first 0: 0-x -> -x
                        return new Node(Token.NEG, right);
                    }
                } else if (right.type == Token.NUMBER) {
                    if (right.getDouble() == 0.0) {
                        // second 0: x - 0 -> +x
                        // can not make simply x because x - 0 must be number
                        return new Node(Token.POS, left);
                    }
                }
                break;

            case Token.MUL:
                // numerical multiplication
                if (left.type == Token.NUMBER) {
                    double ld = left.getDouble();
                    if (right.type == Token.NUMBER) {
                        // both numbers
                        left.setDouble(ld * right.getDouble());
                        return left;
                    } else if (ld == 1.0) {
                        // first 1: 1 *  x -> +x
                        return new Node(Token.POS, right);
                    }
                } else if (right.type == Token.NUMBER) {
                    if (right.getDouble() == 1.0) {
                        // second 1: x * 1 -> +x
                        // can not make simply x because x - 0 must be number
                        return new Node(Token.POS, left);
                    }
                }
                // can't do x*0: Infinity * 0 gives NaN, not 0
                break;

            case Token.DIV:
                // number division
                if (right.type == Token.NUMBER) {
                    double rd = right.getDouble();
                    if (left.type == Token.NUMBER) {
                        // both constants -- just divide, trust Java to handle x/0
                        left.setDouble(left.getDouble() / rd);
                        return left;
                    } else if (rd == 1.0) {
                        // second 1: x/1 -> +x
                        // not simply x to force number conversion
                        return new Node(Token.POS, left);
                    }
                }
                break;

            case Token.AND:
                {
                    // Since x && y gives x, not false, when Boolean(x) is false,
                    // and y, not Boolean(y), when Boolean(x) is true, x && y
                    // can only be simplified if x is defined. See bug 309957.

                    int leftStatus = isAlwaysDefinedBoolean(left);
                    if (leftStatus == ALWAYS_FALSE_BOOLEAN) {
                        // if the first one is false, just return it
                        return left;
                    } else if (leftStatus == ALWAYS_TRUE_BOOLEAN) {
                        // if first is true, set to second
                        return right;
                    }
                    break;
                }

            case Token.OR:
                {
                    // Since x || y gives x, not true, when Boolean(x) is true,
                    // and y, not Boolean(y), when Boolean(x) is false, x || y
                    // can only be simplified if x is defined. See bug 309957.

                    int leftStatus = isAlwaysDefinedBoolean(left);
                    if (leftStatus == ALWAYS_TRUE_BOOLEAN) {
                        // if the first one is true, just return it
                        return left;
                    } else if (leftStatus == ALWAYS_FALSE_BOOLEAN) {
                        // if first is false, set to second
                        return right;
                    }
                    break;
                }
        }

        return new Node(nodeType, left, right);
    }

    private Node createAssignment(int assignType, Node left, Node right) {
        Node ref = makeReference(left);
        if (ref == null) {
            if (left.getType() == Token.ARRAYLIT || left.getType() == Token.OBJECTLIT) {
                if (assignType != Token.ASSIGN) {
                    parser.reportError("msg.bad.destruct.op");
                    return right;
                }
                return parser.createDestructuringAssignment(-1, left, right, this::transform);
            }
            parser.reportError("msg.bad.assign.left");
            return right;
        }
        left = ref;

        int assignOp;
        switch (assignType) {
            case Token.ASSIGN:
                {
                    return propagateSuperFromLhs(parser.simpleAssignment(left, right), left);
                }
            case Token.ASSIGN_BITOR:
                assignOp = Token.BITOR;
                break;
            case Token.ASSIGN_LOGICAL_OR:
                assignOp = Token.OR;
                break;
            case Token.ASSIGN_BITXOR:
                assignOp = Token.BITXOR;
                break;
            case Token.ASSIGN_BITAND:
                assignOp = Token.BITAND;
                break;
            case Token.ASSIGN_LOGICAL_AND:
                assignOp = Token.AND;
                break;
            case Token.ASSIGN_LSH:
                assignOp = Token.LSH;
                break;
            case Token.ASSIGN_RSH:
                assignOp = Token.RSH;
                break;
            case Token.ASSIGN_URSH:
                assignOp = Token.URSH;
                break;
            case Token.ASSIGN_ADD:
                assignOp = Token.ADD;
                break;
            case Token.ASSIGN_SUB:
                assignOp = Token.SUB;
                break;
            case Token.ASSIGN_MUL:
                assignOp = Token.MUL;
                break;
            case Token.ASSIGN_DIV:
                assignOp = Token.DIV;
                break;
            case Token.ASSIGN_MOD:
                assignOp = Token.MOD;
                break;
            case Token.ASSIGN_EXP:
                assignOp = Token.EXP;
                break;
            case Token.ASSIGN_NULLISH:
                assignOp = Token.NULLISH_COALESCING;
                break;
            default:
                throw Kit.codeBug();
        }

        int nodeType = left.getType();
        switch (nodeType) {
            case Token.NAME:
                {
                    Node op = new Node(assignOp, left, right);
                    Node lvalueLeft = Node.newString(Token.BINDNAME, left.getString());
                    return propagateSuperFromLhs(new Node(Token.SETNAME, lvalueLeft, op), left);
                }
            case Token.GETPROP:
            case Token.GETELEM:
                {
                    Node obj = left.getFirstChild();
                    Node id = left.getLastChild();

                    int type = nodeType == Token.GETPROP ? Token.SETPROP_OP : Token.SETELEM_OP;

                    Node opLeft = new Node(Token.USE_STACK);
                    Node op = new Node(assignOp, opLeft, right);
                    return propagateSuperFromLhs(new Node(type, obj, id, op), left);
                }
            case Token.GET_REF:
                {
                    ref = left.getFirstChild();
                    parser.checkMutableReference(ref);
                    Node opLeft = new Node(Token.USE_STACK);
                    Node op = new Node(assignOp, opLeft, right);
                    return propagateSuperFromLhs(new Node(Token.SET_REF_OP, ref, op), left);
                }
        }

        throw Kit.codeBug();
    }

    private Node propagateSuperFromLhs(Node result, Node left) {
        if (left.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
            result.putIntProp(Node.SUPER_PROPERTY_ACCESS, 1);
        }
        return result;
    }

    private static Node createUseLocal(Node localBlock) {
        if (Token.LOCAL_BLOCK != localBlock.getType()) throw Kit.codeBug();
        Node result = new Node(Token.LOCAL_LOAD);
        result.putProp(Node.LOCAL_BLOCK_PROP, localBlock);
        return result;
    }

    private static Jump makeJump(int type, Node target) {
        Jump n = new Jump(type);
        n.target = target;
        return n;
    }

    private static Node makeReference(Node node) {
        int type = node.getType();
        switch (type) {
            case Token.NAME:
            case Token.GETPROP:
            case Token.GETELEM:
            case Token.GET_REF:
                return node;
            case Token.CALL:
                node.setType(Token.REF_CALL);
                return new Node(Token.GET_REF, node);
        }
        // Signal caller to report error
        return null;
    }

    // Check if Node always mean true or false in boolean context
    private static int isAlwaysDefinedBoolean(Node node) {
        switch (node.getType()) {
            case Token.FALSE:
            case Token.NULL:
                return ALWAYS_FALSE_BOOLEAN;
            case Token.TRUE:
                return ALWAYS_TRUE_BOOLEAN;
            case Token.NUMBER:
                {
                    double num = node.getDouble();
                    if (!Double.isNaN(num) && num != 0.0) {
                        return ALWAYS_TRUE_BOOLEAN;
                    }
                    return ALWAYS_FALSE_BOOLEAN;
                }
        }
        return 0;
    }

    // Check if node is the target of a destructuring bind.
    boolean isDestructuring(Node n) {
        return n instanceof DestructuringForm && ((DestructuringForm) n).isDestructuring();
    }

    Node decompileFunctionHeader(FunctionNode fn) {
        if (fn.getFunctionName() != null) {
            return null;
        } else if (fn.getMemberExprNode() != null) {
            return transform(fn.getMemberExprNode());
        }
        return null;
    }

    public static class AstNodePosition implements Parser.CurrentPositionReporter {
        private ArrayDeque<AstNode> stack;
        private String sourceString;

        private int savedLineno = -1;
        private String savedLine;
        private int savedLineOffset;

        public AstNodePosition(String sourceString) {
            stack = new ArrayDeque<>();
            this.sourceString = sourceString;
        }

        public void push(AstNode node) {
            stack.push(node);
        }

        public void pop() {
            stack.pop();
        }

        @Override
        public int getPosition() {
            return stack.peek().getAbsolutePosition();
        }

        @Override
        public int getLength() {
            return stack.peek().getLength();
        }

        @Override
        public int getLineno() {
            return stack.peek().getLineno();
        }

        private void cutAndSaveLine() {
            int lineno = getLineno();
            if (savedLineno == lineno) {
                return;
            }

            int l = 1;
            boolean isPrevCR = false;
            int begin = 0;
            for (; begin < sourceString.length(); begin++) {
                char c = sourceString.charAt(begin);
                if (isPrevCR && c == '\n') {
                    continue;
                }
                isPrevCR = (c == '\r');

                if (l == lineno) {
                    break;
                }
                if (ScriptRuntime.isJSLineTerminator(c)) {
                    l++;
                }
            }

            int end = begin;
            for (; end < sourceString.length(); end++) {
                char c = sourceString.charAt(end);
                if (ScriptRuntime.isJSLineTerminator(c)) {
                    break;
                }
            }

            savedLineno = lineno;
            if (end == 0) {
                savedLine = "";
                savedLineOffset = 0;
            } else {
                savedLine = sourceString.substring(begin, end);
                savedLineOffset = getPosition() - begin + 1;
            }
        }

        @Override
        public String getLine() {
            cutAndSaveLine();
            return savedLine;
        }

        @Override
        public int getOffset() {
            cutAndSaveLine();
            return savedLineOffset;
        }
    }
}
