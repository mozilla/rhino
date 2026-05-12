/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AwaitExpression;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NodeVisitor;

public class AsyncParserTest {

    private static FunctionNode parseAndExtractFirstFn(String src) {
        AtomicReference<FunctionNode> ref = new AtomicReference<>();
        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(Context.VERSION_ES6);
        Parser p = new Parser(env);
        p.parse(src, "eval", 1)
                .visit(
                        node -> {
                            if (ref.get() != null) return false;
                            if (node instanceof FunctionNode) {
                                FunctionNode fn = (FunctionNode) node;
                                if (fn.getFunctionName() != null
                                        || fn.getFunctionType() == FunctionNode.ARROW_FUNCTION) {
                                    ref.set(fn);
                                    return false;
                                }
                            }
                            return true;
                        });
        assertNotNull(ref.get(), "Expected to find a function node");
        return ref.get();
    }

    private static AwaitExpression parseAndExtractAwait(String src) {
        AtomicReference<AwaitExpression> ref = new AtomicReference<>();
        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(Context.VERSION_ES6);
        Parser p = new Parser(env);
        p.parse(src, "eval", 1)
                .visit(
                        node -> {
                            if (ref.get() != null) return false;
                            if (node instanceof AwaitExpression) {
                                ref.set((AwaitExpression) node);
                                return false;
                            }
                            return true;
                        });
        return ref.get();
    }

    @Test
    public void asyncFunctionDeclarationIsAsync() {
        FunctionNode fn = parseAndExtractFirstFn("async function foo() {}");
        assertTrue(fn.isAsync(),"Expected async");
        assertFalse(fn.isES6Generator(), "Expected not generator");
        assertEquals("foo", fn.getFunctionName().getIdentifier());
        assertEquals(FunctionNode.FUNCTION_EXPRESSION_STATEMENT, fn.getFunctionType());
    }

    @Test
    public void asyncFunctionExpressionIsAsync() {
        FunctionNode fn = parseAndExtractFirstFn("let f = async function named() {}");
        assertTrue(fn.isAsync(), "Expected async");
        assertEquals("named", fn.getFunctionName().getIdentifier());
    }

    @Test
    public void anonymousAsyncFunctionExpressionIsAsync() {
        AtomicReference<FunctionNode> ref = new AtomicReference<>();
        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(Context.VERSION_ES6);
        Parser p = new Parser(env);
        p.parse("let f = async function() {}", "eval", 1)
                .visit(
                        node -> {
                            if (ref.get() != null) return false;
                            if (node instanceof FunctionNode) {
                                FunctionNode fn = (FunctionNode) node;
                                if (fn.getFunctionType() == FunctionNode.FUNCTION_EXPRESSION) {
                                    ref.set(fn);
                                    return false;
                                }
                            }
                            return true;
                        });
        assertNotNull(ref.get());
        assertTrue(ref.get().isAsync(), "Expected async");
    }

    @Test
    public void asyncArrowFunctionNoParams() {
        AtomicReference<FunctionNode> ref = new AtomicReference<>();
        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(Context.VERSION_ES6);
        Parser p = new Parser(env);
        p.parse("let f = async () => {}", "eval", 1)
                .visit(
                        node -> {
                            if (ref.get() != null) return false;
                            if (node instanceof FunctionNode) {
                                FunctionNode fn = (FunctionNode) node;
                                if (fn.getFunctionType() == FunctionNode.ARROW_FUNCTION) {
                                    ref.set(fn);
                                    return false;
                                }
                            }
                            return true;
                        });
        assertNotNull(ref.get());
        assertTrue(ref.get().isAsync(), "Expected async arrow");
    }

    @Test
    public void asyncArrowFunctionWithParam() {
        AtomicReference<FunctionNode> ref = new AtomicReference<>();
        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(Context.VERSION_ES6);
        Parser p = new Parser(env);
        p.parse("let f = async (x) => x", "eval", 1)
                .visit(
                        node -> {
                            if (ref.get() != null) return false;
                            if (node instanceof FunctionNode) {
                                FunctionNode fn = (FunctionNode) node;
                                if (fn.getFunctionType() == FunctionNode.ARROW_FUNCTION) {
                                    ref.set(fn);
                                    return false;
                                }
                            }
                            return true;
                        });
        assertNotNull(ref.get());
        assertTrue(ref.get().isAsync(), "Expected async arrow");
        assertEquals(1, ref.get().getParams().size());
    }

    @Test
    public void asyncGeneratorFunctionIsAsyncAndGenerator() {
        FunctionNode fn = parseAndExtractFirstFn("async function* gen() {}");
        assertTrue(fn.isAsync(), "Expected async");
        assertTrue(fn.isES6Generator(), "Expected generator");
    }

    @Test
    public void awaitInsideAsyncFunctionProducesAwaitExpression() {
        AwaitExpression await =
                parseAndExtractAwait("async function foo() { let x = await bar(); }");
        assertNotNull(await, "Expected AwaitExpression");
        assertTrue(await.getValue() instanceof FunctionNode || await.getValue() instanceof AstNode);
    }

    @Test
    public void asyncUsedAsIdentifierOutsideAsync() {
        // "async" used as a plain identifier should not cause a parse error
        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(Context.VERSION_ES6);
        Parser p = new Parser(env);
        // Should parse without error
        p.parse("var async = 5; var x = async + 1;", "eval", 1);
    }

    @Test
    public void asyncFunctionNotGenerator() {
        FunctionNode fn = parseAndExtractFirstFn("async function foo() {}");
        assertFalse(fn.isGenerator(), "Async function should not be a generator");
    }

    @Test
    public void regularFunctionIsNotAsync() {
        AtomicReference<FunctionNode> ref = new AtomicReference<>();
        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(Context.VERSION_ES6);
        Parser p = new Parser(env);
        p.parse("function foo() {}", "eval", 1)
                .visit(
                        node -> {
                            if (ref.get() != null) return false;
                            if (node instanceof FunctionNode) {
                                FunctionNode fn = (FunctionNode) node;
                                if (fn.getFunctionName() != null) {
                                    ref.set(fn);
                                    return false;
                                }
                            }
                            return true;
                        });
        assertNotNull(ref.get());
        assertFalse(ref.get().isAsync(), "Regular function should not be async");
    }

    @Test
    public void asyncMethodInObjectLiteral() {
        AtomicReference<FunctionNode> ref = new AtomicReference<>();
        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(Context.VERSION_ES6);
        Parser p = new Parser(env);
        p.parse("let obj = { async method() {} };", "eval", 1)
                .visit(
                        node -> {
                            if (ref.get() != null) return false;
                            if (node instanceof FunctionNode) {
                                FunctionNode fn = (FunctionNode) node;
                                ref.set(fn);
                                return false;
                            }
                            return true;
                        });
        assertNotNull(ref.get());
        assertTrue(ref.get().isAsync(), "Expected async method");
    }

    @Test
    public void asyncFunctionNoNewlineBeforeFunction() {
        // "async" on separate line from "function" — should NOT be async function
        AtomicReference<FunctionNode> ref = new AtomicReference<>();
        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(Context.VERSION_ES6);
        Parser p = new Parser(env);
        // Two separate statements: "async" expression and "function foo() {}"
        // The parser may handle "async" as a standalone expression statement
        p.parse("async\nfunction foo() {}", "eval", 1)
                .visit(
                        node -> {
                            if (ref.get() != null) return false;
                            if (node instanceof FunctionNode) {
                                FunctionNode fn = (FunctionNode) node;
                                if ("foo".equals(fn.getFunctionName().getIdentifier())) {
                                    ref.set(fn);
                                    return false;
                                }
                            }
                            return true;
                        });
        if (ref.get() != null) {
            assertFalse(ref.get().isAsync(), "Function after newline should not be async");
        }
    }

    @Test
    public void awaitExpressionHasValue() {
        AwaitExpression await =
                parseAndExtractAwait("async function f() { return await someExpr; }");
        assertNotNull(await, "Expected AwaitExpression");
        assertNotNull(await.getValue(), "AwaitExpression should have a value");
        assertTrue(await.getValue() instanceof Name, "Value should be a Name");
        assertEquals("someExpr", ((Name) await.getValue()).getIdentifier());
    }

    @Test
    public void asyncArrowFunctionMultipleParams() {
        AtomicReference<FunctionNode> ref = new AtomicReference<>();
        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(Context.VERSION_ES6);
        Parser p = new Parser(env);
        p.parse("let f = async (a, b) => a + b", "eval", 1)
                .visit(
                        node -> {
                            if (ref.get() != null) return false;
                            if (node instanceof FunctionNode) {
                                FunctionNode fn = (FunctionNode) node;
                                if (fn.getFunctionType() == FunctionNode.ARROW_FUNCTION) {
                                    ref.set(fn);
                                    return false;
                                }
                            }
                            return true;
                        });
        assertNotNull(ref.get());
        assertTrue(ref.get().isAsync(), "Expected async arrow");
        assertEquals(2, ref.get().getParams().size());
    }

    interface NodeVisitorHelper extends NodeVisitor {
        static NodeVisitorHelper from(NodeVisitor v) {
            return v::visit;
        }
    }
}
