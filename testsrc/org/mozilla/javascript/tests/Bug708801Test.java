/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * 
 */
package org.mozilla.javascript.tests;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.IRFactory;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.optimizer.Codegen;
import org.mozilla.javascript.optimizer.OptFunctionNode;

/**
 * @author André Bargull
 * 
 */
public class Bug708801Test {
    private static final ContextFactory factory = new ContextFactory() {
        static final int COMPILER_MODE = 9;

        @Override
        protected Context makeContext() {
            Context cx = super.makeContext();
            cx.setLanguageVersion(Context.VERSION_1_8);
            cx.setOptimizationLevel(COMPILER_MODE);
            return cx;
        }
    };

    private static abstract class Action implements ContextAction {
        protected Context cx;
        protected ScriptableObject scope;

        @SuppressWarnings("unused")
        protected Object evaluate(String s) {
            return cx.evaluateString(scope, s, "<eval>", 1, null);
        }

        /**
         * Compiles {@code source} and returns the transformed and optimized
         * {@link ScriptNode}
         */
        protected ScriptNode compile(CharSequence source) {
            final String mainMethodClassName = "Main";
            final String scriptClassName = "Main";

            CompilerEnvirons compilerEnv = new CompilerEnvirons();
            compilerEnv.initFromContext(cx);
            ErrorReporter compilationErrorReporter = compilerEnv
                    .getErrorReporter();
            Parser p = new Parser(compilerEnv, compilationErrorReporter);
            AstRoot ast = p.parse(source.toString(), "<eval>", 1);
            IRFactory irf = new IRFactory(compilerEnv);
            ScriptNode tree = irf.transformTree(ast);

            Codegen codegen = new Codegen();
            codegen.setMainMethodClass(mainMethodClassName);
            codegen.compileToClassFile(compilerEnv, scriptClassName, tree,
                    tree.getEncodedSource(), false);

            return tree;
        }

        /**
         * Checks every variable {@code v} in {@code source} is marked as a
         * number-variable iff {@code numbers} contains {@code v}
         */
        protected void assertNumberVars(CharSequence source, String... numbers) {
            // wrap source in function
            ScriptNode tree = compile("function f(o, fn){" + source + "}");

            FunctionNode fnode = tree.getFunctionNode(0);
            assertNotNull(fnode);
            OptFunctionNode opt = OptFunctionNode.get(fnode);
            assertNotNull(opt);
            assertSame(fnode, opt.fnode);

            for (int i = 0, c = fnode.getParamCount(); i < c; ++i) {
                assertTrue(opt.isParameter(i));
                assertFalse(opt.isNumberVar(i));
            }

            Set<String> set = new HashSet<String>(asList(numbers));
            for (int i = fnode.getParamCount(), c = fnode.getParamAndVarCount(); i < c; ++i) {
                assertFalse(opt.isParameter(i));
                String name = fnode.getParamOrVarName(i);
                String msg = format("{%s -> number? = %b}", name, opt.isNumberVar(i));
                assertEquals(msg, set.contains(name), opt.isNumberVar(i));
            }
        }

        public final Object run(Context cx) {
            this.cx = cx;
            scope = cx.initStandardObjects(null, true);
            return run();
        }

        protected abstract Object run();
    }

    @Test
    public void testIncDec() {
        factory.call(new Action() {
            @Override
            protected Object run() {
                // pre-inc
                assertNumberVars("var b; ++b");
                assertNumberVars("var b=0; ++b", "b");
                assertNumberVars("var b; ++[1][++b]");
                assertNumberVars("var b=0; ++[1][++b]", "b");
                assertNumberVars("var b; ++[1][b=0,++b]", "b");
                // pre-dec
                assertNumberVars("var b; --b");
                assertNumberVars("var b=0; --b", "b");
                assertNumberVars("var b; --[1][--b]");
                assertNumberVars("var b=0; --[1][--b]", "b");
                assertNumberVars("var b; --[1][b=0,--b]", "b");
                // post-inc
                assertNumberVars("var b; b++");
                assertNumberVars("var b=0; b++", "b");
                assertNumberVars("var b; [1][b++]++");
                assertNumberVars("var b=0; [1][b++]++", "b");
                assertNumberVars("var b; [1][b=0,b++]++", "b");
                // post-dec
                assertNumberVars("var b; b--");
                assertNumberVars("var b=0; b--", "b");
                assertNumberVars("var b; [1][b--]--");
                assertNumberVars("var b=0; [1][b--]--", "b");
                assertNumberVars("var b; [1][b=0,b--]--", "b");
                return null;
            }
        });
    }

    @Test
    public void testTypeofName() {
        factory.call(new Action() {
            @Override
            protected Object run() {
                // Token.TYPEOFNAME
                assertNumberVars("var b; typeof b");
                assertNumberVars("var b=0; typeof b", "b");
                assertNumberVars("var b; if(new Date()<0){b=0} typeof b");
                // Token.TYPEOF
                assertNumberVars("var b; typeof (b,b)");
                assertNumberVars("var b=0; typeof (b,b)", "b");
                assertNumberVars("var b; if(new Date()<0){b=0} typeof (b,b)");

                return null;
            }
        });
    }

    @Test
    public void testEval() {
        factory.call(new Action() {
            @Override
            protected Object run() {
                // direct eval => requires activation
                assertNumberVars("var b; eval('typeof b')");
                assertNumberVars("var b=0; eval('typeof b')");
                assertNumberVars("var b; if(new Date()<0){b=0} eval('typeof b')");

                // indirect eval => requires no activation
                assertNumberVars("var b; (1,eval)('typeof b');");
                assertNumberVars("var b=0; (1,eval)('typeof b')", "b");
                assertNumberVars(
                        "var b; if(new Date()<0){b=0} (1,eval)('typeof b')",
                        "b");

                return null;
            }
        });
    }

    @Test
    public void testRelOp() {
        factory.call(new Action() {
            @Override
            protected Object run() {
                // relational operators: <, <=, >, >=
                assertNumberVars("var b = 1 < 1");
                assertNumberVars("var b = 1 <= 1");
                assertNumberVars("var b = 1 > 1");
                assertNumberVars("var b = 1 >= 1");
                // equality operators: ==, !=, ===, !==
                assertNumberVars("var b = 1 == 1");
                assertNumberVars("var b = 1 != 1");
                assertNumberVars("var b = 1 === 1");
                assertNumberVars("var b = 1 !== 1");

                return null;
            }
        });
    }

    @Test
    public void testMore() {
        factory.call(new Action() {
            @Override
            protected Object run() {
                // simple assignment:
                assertNumberVars("var b");
                assertNumberVars("var b = 1", "b");
                assertNumberVars("var b = 'a'");
                assertNumberVars("var b = true");
                assertNumberVars("var b = /(?:)/");
                assertNumberVars("var b = o");
                assertNumberVars("var b = fn");
                // use before assignment
                assertNumberVars("b; var b = 1");
                assertNumberVars("b || c; var b = 1, c = 2");
                assertNumberVars("if(b) var b = 1");
                assertNumberVars("typeof b; var b=1");
                assertNumberVars("typeof (b,b); var b=1");
                // relational operators: in, instanceof
                assertNumberVars("var b = 1 instanceof 1");
                assertNumberVars("var b = 1 in 1");
                // other operators with nested comma expression:
                assertNumberVars("var b = !(1,1)");
                assertNumberVars("var b = typeof(1,1)");
                assertNumberVars("var b = void(1,1)");
                // nested assignment
                assertNumberVars("var b = 1; var f = (b = 'a')");
                // let expression:
                assertNumberVars("var b = let(x=1) x", "b", "x");
                assertNumberVars("var b = let(x=1,y=1) x,y", "b", "x", "y");
                // conditional operator:
                assertNumberVars("var b = 0 ? 1 : 2", "b");
                assertNumberVars("var b = 'a' ? 1 : 2", "b");
                // comma expression:
                assertNumberVars("var b = (0,1)", "b");
                assertNumberVars("var b = ('a',1)", "b");
                // assignment operations:
                assertNumberVars("var b; var c=0; b=c", "b", "c");
                assertNumberVars("var b; var c=0; b=(c=1)", "b", "c");
                assertNumberVars("var b; var c=0; b=(c='a')");
                assertNumberVars("var b; var c=0; b=(c+=1)", "b", "c");
                assertNumberVars("var b; var c=0; b=(c*=1)", "b", "c");
                assertNumberVars("var b; var c=0; b=(c%=1)", "b", "c");
                assertNumberVars("var b; var c=0; b=(c/=1)", "b", "c");
                // property access:
                assertNumberVars("var b; b=(o.p)");
                assertNumberVars("var b; b=(o.p=1)", "b");
                assertNumberVars("var b; b=(o.p+=1)");
                assertNumberVars("var b; b=(o['p']=1)", "b");
                assertNumberVars("var b; b=(o['p']+=1)");
                assertNumberVars("var b; var o = {p:0}; b=(o.p=1)", "b");
                assertNumberVars("var b; var o = {p:0}; b=(o.p+=1)");
                assertNumberVars("var b; var o = {p:0}; b=(o['p']=1)", "b");
                assertNumberVars("var b; var o = {p:0}; b=(o['p']+=1)");
                assertNumberVars("var b = 1; b.p = true", "b");
                assertNumberVars("var b = 1; b.p", "b");
                assertNumberVars("var b = 1; b.p()", "b");
                assertNumberVars("var b = 1; b[0] = true", "b");
                assertNumberVars("var b = 1; b[0]", "b");
                assertNumberVars("var b = 1; b[0]()", "b");
                // assignment (global)
                assertNumberVars("var b = foo");
                assertNumberVars("var b = foo1=1", "b");
                assertNumberVars("var b = foo2+=1");
                // boolean operators:
                assertNumberVars("var b = 1||2", "b");
                assertNumberVars("var b; var c=1; b=c||2", "b", "c");
                assertNumberVars("var b; var c=1; b=c||c||2", "b", "c");
                assertNumberVars("var b = 1&&2", "b");
                assertNumberVars("var b; var c=1; b=c&&2", "b", "c");
                assertNumberVars("var b; var c=1; b=c&&c&&2", "b", "c");
                // bit not:
                assertNumberVars("var b = ~0", "b");
                assertNumberVars("var b = ~o", "b");
                assertNumberVars("var b; var c=1; b=~c", "b", "c");
                // increment, function call:
                assertNumberVars("var b; var g; b = (g=0,g++)", "b", "g");
                assertNumberVars("var b; var x = fn(b=1)", "b");
                assertNumberVars("var b; var x = fn(b=1).p++", "b", "x");
                assertNumberVars("var b; ({1:{}})[b=1].p++", "b");
                assertNumberVars("var b; o[b=1]++", "b");
                // destructuring
                assertNumberVars("var r,s; [r,s] = [1,1]");
                assertNumberVars("var r=0, s=0; [r,s] = [1,1]");
                assertNumberVars("var r,s; ({a: r, b: s}) = {a:1, b:1}");
                assertNumberVars("var r=0, s=0; ({a: r, b: s}) = {a:1, b:1}");
                // array comprehension
                assertNumberVars("var b=[i*i for each (i in [1,2,3])]");
                assertNumberVars("var b=[j*j for each (j in [1,2,3]) if (j>1)]");

                return null;
            }
        });
    }

}
