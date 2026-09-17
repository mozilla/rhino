/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.Context.reportError;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Jump;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.ast.Symbol;

/**
 * This class transforms a tree to a lower-level representation for codegen.
 *
 * @see Node
 * @author Norris Boyd
 */
public class NodeTransformer {

    public NodeTransformer() {}

    public final void transform(ScriptNode tree, CompilerEnvirons env) {
        transform(tree, false, env);
    }

    public final void transform(ScriptNode tree, boolean inStrictMode, CompilerEnvirons env) {
        compilerEnv = env;
        boolean useStrictMode = inStrictMode;
        // Support strict mode inside a function only for "ES6" language level
        // and above. Otherwise, we will end up breaking backward compatibility for
        // many existing scripts.
        if ((env.getLanguageVersion() >= Context.VERSION_ES6) && tree.isInStrictMode()) {
            useStrictMode = true;
        }
        transformCompilationUnit(tree, useStrictMode);
        for (int i = 0; i != tree.getFunctionCount(); ++i) {
            FunctionNode fn = tree.getFunctionNode(i);
            transform(fn, useStrictMode, env);
        }
    }

    private void transformCompilationUnit(ScriptNode tree, boolean inStrictMode) {
        loops = new ArrayDeque<>();
        loopEnds = new ArrayDeque<>();

        // to save against upchecks if no finally blocks are used.
        hasFinally = false;

        // Flatten all only if we are not using scope objects for block scope
        boolean createScopeObjects =
                tree.getType() != Token.FUNCTION || ((FunctionNode) tree).requiresActivation();
        tree.flattenSymbolTable(!createScopeObjects);

        // uncomment to print tree before transformation
        if (Token.printTrees) System.out.println(tree.toStringTree(tree));
        transformCompilationUnit_r(tree, tree, tree, createScopeObjects, inStrictMode, false);
    }

    private void transformCompilationUnit_r(
            final ScriptNode tree,
            final Node parent,
            Scope scope,
            boolean createScopeObjects,
            boolean inStrictMode,
            boolean inLoop) {
        Node node = null;
        siblingLoop:
        for (; ; ) {
            Node previous = null;
            if (node == null) {
                node = parent.getFirstChild();
            } else {
                previous = node;
                node = node.getNext();
            }
            if (node == null) {
                break;
            }

            int type = node.getType();
            if ((type == Token.BLOCK || type == Token.LOOP || type == Token.ARRAYCOMP)
                    && (node instanceof Scope)
                    && !createScopeObjects
                    && inLoop
                    && compilerEnv.getLanguageVersion() >= Context.VERSION_ES6) {
                // The scope is not reified, so its bindings keep the same slots for the whole
                // call. A loop can enter the scope again, and the bindings must be fresh when
                // it does.
                addScopeReinit((Scope) node);
            }
            if (createScopeObjects
                    && (type == Token.BLOCK || type == Token.LOOP || type == Token.ARRAYCOMP)
                    && (node instanceof Scope)) {
                Scope newScope = (Scope) node;
                if (newScope.getSymbolTable() != null) {
                    // transform to let statement so we get a with statement
                    // created to contain scoped let variables
                    Node let = new Node(type == Token.ARRAYCOMP ? Token.LETEXPR : Token.LET);
                    Node innerLet = new Node(Token.LET);
                    let.addChildToBack(innerLet);
                    for (Symbol symbol : newScope.getSymbolTable().values()) {
                        Node name = Node.newString(Token.NAME, symbol.getName());
                        // Const bindings must be created uninitialized, so that the
                        // declaration itself can initialize them. Loop scopes are
                        // excluded because for-in and for-of assign to their variable
                        // on every iteration.
                        if (symbol.getDeclType() == Symbol.Type.CONST && type == Token.BLOCK) {
                            name.putIntProp(Node.IS_CONST_PROP, 1);
                        }
                        innerLet.addChildToBack(name);
                    }
                    newScope.setSymbolTable(null); // so we don't transform again
                    Node oldNode = node;
                    node = replaceCurrent(parent, previous, node, let);
                    type = node.getType();
                    let.addChildToBack(oldNode);
                }
            }

            switch (type) {
                case Token.LABEL:
                case Token.SWITCH:
                case Token.LOOP:
                    loops.push(node);
                    loopEnds.push(((Jump) node).target);
                    break;

                case Token.WITH:
                    {
                        loops.push(node);
                        Node leave = node.getNext();
                        if (leave.getType() != Token.LEAVE_SCOPE) {
                            Kit.codeBug();
                        }
                        loopEnds.push(leave);
                        break;
                    }

                case Token.SCOPE_BLOCK:
                    {
                        loops.push(node);
                        Node leave = node.getNext();
                        if (leave.getType() != Token.LEAVE_SCOPE) {
                            Kit.codeBug();
                        }
                        loopEnds.push(leave);
                        break;
                    }

                case Token.TRY:
                    {
                        Jump jump = (Jump) node;
                        Node finallytarget = jump.getFinally();
                        if (finallytarget != null) {
                            hasFinally = true;
                            loops.push(node);
                            loopEnds.push(finallytarget);
                        }
                        break;
                    }

                case Token.TARGET:
                case Token.LEAVE_SCOPE:
                    if (!loopEnds.isEmpty() && loopEnds.peek() == node) {
                        loopEnds.pop();
                        loops.pop();
                    }
                    break;

                case Token.YIELD:
                case Token.YIELD_STAR:
                    ((FunctionNode) tree).addResumptionPoint(node);
                    break;

                case Token.RETURN:
                    {
                        boolean isGenerator =
                                tree.getType() == Token.FUNCTION
                                        && ((FunctionNode) tree).isGenerator();
                        if (isGenerator) {
                            node.putIntProp(Node.GENERATOR_END_PROP, 1);
                        }
                        /* If we didn't support try/finally, it wouldn't be
                         * necessary to put LEAVEWITH nodes here... but as
                         * we do need a series of JSR FINALLY nodes before
                         * each RETURN, we need to ensure that each finally
                         * block gets the correct scope... which could mean
                         * that some LEAVEWITH nodes are necessary.
                         */
                        if (!hasFinally) break; // skip the whole mess.
                        Node unwindBlock = null;
                        // Iterate from the top of the stack (most recently inserted) and down
                        for (Node n : loops) {
                            int elemtype = n.getType();
                            if (elemtype == Token.TRY
                                    || elemtype == Token.WITH
                                    || elemtype == Token.SCOPE_BLOCK) {
                                Node unwind;
                                if (elemtype == Token.TRY) {
                                    Jump jsrnode = new Jump(Token.JSR);
                                    jsrnode.target = ((Jump) n).getFinally();
                                    unwind = jsrnode;
                                } else {
                                    unwind = new Node(Token.LEAVE_SCOPE);
                                }
                                if (unwindBlock == null) {
                                    unwindBlock = new Node(Token.BLOCK);
                                    unwind.setLineColumnNumber(node.getLineno(), node.getColumn());
                                }
                                unwindBlock.addChildToBack(unwind);
                            }
                        }
                        if (unwindBlock != null) {
                            Node returnNode = node;
                            Node returnExpr = returnNode.getFirstChild();
                            node = replaceCurrent(parent, previous, node, unwindBlock);
                            if (returnExpr == null || isGenerator) {
                                unwindBlock.addChildToBack(returnNode);
                            } else {
                                Node store = new Node(Token.EXPR_RESULT, returnExpr);
                                unwindBlock.addChildToFront(store);
                                returnNode = new Node(Token.RETURN_RESULT);
                                unwindBlock.addChildToBack(returnNode);
                                // transform return expression
                                transformCompilationUnit_r(
                                        tree,
                                        store,
                                        scope,
                                        createScopeObjects,
                                        inStrictMode,
                                        inLoop);
                            }
                            // skip transformCompilationUnit_r to avoid infinite loop
                            continue siblingLoop;
                        }
                        break;
                    }

                case Token.BREAK:
                case Token.CONTINUE:
                    {
                        Jump jump = (Jump) node;
                        Jump jumpStatement = jump.getJumpStatement();
                        if (jumpStatement == null) Kit.codeBug();

                        if (loops.isEmpty()) {
                            // Parser/IRFactory ensure that break/continue
                            // always has a jump statement associated with it
                            // which should be found
                            throw Kit.codeBug();
                        }
                        // Iterate from the top of the stack (most recently inserted) and down
                        for (Node n : loops) {
                            if (n == jumpStatement) {
                                break;
                            }

                            int elemtype = n.getType();
                            if (elemtype == Token.WITH
                                    || elemtype == Token.SCOPEEXPR
                                    || elemtype == Token.SCOPE_BLOCK) {
                                Node leave = new Node(Token.LEAVE_SCOPE);
                                previous = addBeforeCurrent(parent, previous, node, leave);
                            } else if (elemtype == Token.TRY) {
                                Jump tryNode = (Jump) n;
                                Jump jsrFinally = new Jump(Token.JSR);
                                jsrFinally.target = tryNode.getFinally();
                                previous = addBeforeCurrent(parent, previous, node, jsrFinally);
                            }
                        }

                        if (type == Token.BREAK) {
                            jump.target = jumpStatement.target;
                        } else {
                            jump.target = jumpStatement.getContinue();
                        }
                        jump.setType(Token.GOTO);

                        break;
                    }

                case Token.CALL:
                    visitCall(node, tree);
                    break;

                case Token.NEW:
                    visitNew(node, tree);
                    break;

                case Token.LETEXPR:
                case Token.LET:
                    {
                        Node child = node.getFirstChild();
                        if (child.getType() == Token.LET || child.getType() == Token.CONST) {
                            // We have a let statement or expression rather than a
                            // let declaration. A CONST child means a "for (const ...; ...)"
                            // head that createFor split into a scope of its own.
                            boolean createWith =
                                    tree.getType() != Token.FUNCTION
                                            || ((FunctionNode) tree).requiresActivation();
                            node = visitLet(createWith, parent, previous, node);
                            break;
                        }
                        // fall through to process let declaration...
                    }
                /* fall through */
                case Token.CONST:
                case Token.VAR:
                    {
                        Node result = new Node(Token.BLOCK);
                        for (Node cursor = node.getFirstChild(); cursor != null; ) {
                            // Move cursor to next before createAssignment gets chance
                            // to change n.next
                            Node n = cursor;
                            cursor = cursor.getNext();
                            if (n.getType() == Token.NAME) {
                                if (!n.hasChildren()) continue;
                                Node init = n.getFirstChild();
                                n.removeChild(init);
                                n.setType(Token.BINDNAME);
                                n =
                                        new Node(
                                                type == Token.CONST
                                                        ? Token.SETCONST
                                                        : Token.SETNAME,
                                                n,
                                                init);
                            } else {
                                // May be a destructuring assignment already transformed
                                // to a LETEXPR
                                if (n.getType() != Token.LETEXPR) throw Kit.codeBug();
                            }
                            Node pop = new Node(Token.EXPR_VOID, n);
                            pop.setLineColumnNumber(node.getLineno(), node.getColumn());
                            result.addChildToBack(pop);
                        }
                        node = replaceCurrent(parent, previous, node, result);
                        break;
                    }

                case Token.ITERATION:
                    {
                        // A reified iteration environment is copied, so that the bindings the
                        // iteration just finished handed to any closure it created stay as they
                        // were. A flattened scope keeps one slot per binding for the whole call
                        // and nothing can capture it, so the RESETVAR stores that addScopeReinit
                        // prepends are all the freshness it needs.
                        boolean copyScope =
                                createScopeObjects
                                        && compilerEnv.getLanguageVersion() >= Context.VERSION_ES6;
                        Node replacement = new Node(copyScope ? Token.SCOPE_REPLACE : Token.EMPTY);
                        replacement.setLineColumnNumber(node.getLineno(), node.getColumn());
                        node = replaceCurrent(parent, previous, node, replacement);
                        break;
                    }

                case Token.TYPEOFNAME:
                    {
                        Scope defining = scope.getDefiningScope(node.getString());
                        if (defining != null) {
                            node.setScope(defining);
                        }
                    }
                    break;

                case Token.TYPEOF:
                case Token.IFNE:
                    {
                        /* We want to suppress warnings for undefined property o.p
                         * for the following constructs: typeof o.p, if (o.p),
                         * if (!o.p), if (o.p == undefined), if (undefined == o.p)
                         */
                        Node child = node.getFirstChild();
                        if (type == Token.IFNE) {
                            while (child.getType() == Token.NOT) {
                                child = child.getFirstChild();
                            }
                            if (child.getType() == Token.EQ || child.getType() == Token.NE) {
                                Node first = child.getFirstChild();
                                Node last = child.getLastChild();
                                if (first.getType() == Token.UNDEFINED) {
                                    child = last;
                                } else if (last.getType() == Token.UNDEFINED) {
                                    child = first;
                                }
                            }
                        }
                        if (child.getType() == Token.GETPROP) {
                            child.setType(Token.GETPROPNOWARN);
                        }
                        break;
                    }

                case Token.SETNAME:
                    if (inStrictMode) {
                        node.setType(Token.STRICT_SETNAME);
                        if (node.getFirstChild().getType() == Token.BINDNAME) {
                            Node name = node.getFirstChild();
                            if (name instanceof Name
                                    && "eval".equals(((Name) name).getIdentifier())) {
                                // Don't allow set of `eval` in strict mode
                                reportError("syntax error");
                            }
                        }
                    }
                /* fall through */
                case Token.NAME:
                case Token.SETCONST:
                case Token.DELPROP:
                    {
                        // Turn name to var for faster access if possible
                        if (createScopeObjects) {
                            break;
                        }
                        Node nameSource;
                        if (type == Token.NAME) {
                            nameSource = node;
                        } else {
                            nameSource = node.getFirstChild();
                            if (nameSource.getType() != Token.BINDNAME) {
                                if (type == Token.DELPROP) {
                                    break;
                                }
                                throw Kit.codeBug();
                            }
                        }
                        if (nameSource.getScope() != null) {
                            break; // already have a scope set
                        }
                        String name = nameSource.getString();
                        Scope defining = scope.getDefiningScope(name);
                        if (defining != null) {
                            nameSource.setScope(defining);
                            if (type == Token.NAME) {
                                node.setType(Token.GETVAR);
                            } else if (type == Token.SETNAME || type == Token.STRICT_SETNAME) {
                                node.setType(Token.SETVAR);
                                nameSource.setType(Token.STRING);
                            } else if (type == Token.SETCONST) {
                                // A loop can run a block scoped declaration more than once, and
                                // each run re-binds. Pre-ES6 const is hoisted to the function
                                // scope instead, and keeps its "assign once" behaviour.
                                boolean reinitialized =
                                        compilerEnv.getLanguageVersion() >= Context.VERSION_ES6
                                                && inLoop
                                                && defining != tree;
                                node.setType(
                                        reinitialized ? Token.INITCONSTVAR : Token.SETCONSTVAR);
                                nameSource.setType(Token.STRING);
                            } else if (type == Token.DELPROP) {
                                // Local variables are by definition permanent
                                Node n = new Node(Token.FALSE);
                                node = replaceCurrent(parent, previous, node, n);
                            } else {
                                throw Kit.codeBug();
                            }
                        }
                        break;
                    }

                case Token.OBJECTLIT:
                    {
                        Object[] propertyIds = (Object[]) node.getProp(Node.OBJECT_IDS_PROP);
                        if (propertyIds != null) {
                            for (Object propertyId : propertyIds) {
                                if (!(propertyId instanceof Node)) continue;
                                transformCompilationUnit_r(
                                        tree,
                                        (Node) propertyId,
                                        node instanceof Scope ? (Scope) node : scope,
                                        createScopeObjects,
                                        inStrictMode,
                                        inLoop);
                            }
                        }
                    }
            }

            transformCompilationUnit_r(
                    tree,
                    node,
                    node instanceof Scope ? (Scope) node : scope,
                    createScopeObjects,
                    inStrictMode,
                    inLoop || node.getType() == Token.LOOP);
        }
    }

    /**
     * Prepends to a flattened block scope the stores that re-entering it implies. Because the scope
     * is not reified, each of its bindings keeps one slot for the whole call, so a declaration that
     * ran on an earlier iteration is still in effect: {@code SETCONSTVAR} stores only into a slot
     * still marked uninitialized, and a {@code let} without an initializer stores nothing at all.
     *
     * <p>Only bindings that the scope does not itself initialize on entry need this. A declaration
     * among the scope's leading statements always runs before anything can read the binding, so it
     * is left to do the initializing; one that a jump can bypass — a {@code case} clause of a
     * switch, or the head of a for-in/for-of loop — is not.
     */
    private static void addScopeReinit(Scope scope) {
        Map<String, Symbol> symbolTable = scope.getSymbolTable();
        if (symbolTable == null || symbolTable.isEmpty()) {
            return;
        }
        List<String> initializedOnEntry = namesInitializedOnEntry(scope);
        Node previous = null;
        for (Symbol symbol : symbolTable.values()) {
            if (!symbol.isDeclTypeLexical() || initializedOnEntry.contains(symbol.getName())) {
                continue;
            }
            Node name = Node.newString(symbol.getName());
            name.setScope(scope);
            Node reset = new Node(Token.RESETVAR, name);
            reset.setLineColumnNumber(scope.getLineno(), scope.getColumn());
            if (previous == null) {
                scope.addChildToFront(reset);
            } else {
                scope.addChildAfter(reset, previous);
            }
            previous = reset;
        }
    }

    /**
     * Collects the names that the leading statements of a scope declare with an initializer. The
     * walk stops at the first {@link Token#TARGET}, since from there on a jump can land past a
     * declaration and leave the binding holding whatever an earlier iteration left in its slot.
     */
    private static List<String> namesInitializedOnEntry(Scope scope) {
        List<String> names = new ArrayList<>(4);
        for (Node child = scope.getFirstChild(); child != null; child = child.getNext()) {
            int type = child.getType();
            if (type == Token.TARGET) {
                break;
            }
            if (type != Token.LET && type != Token.CONST && type != Token.VAR) {
                continue;
            }
            for (Node decl = child.getFirstChild(); decl != null; decl = decl.getNext()) {
                if (decl.getType() == Token.NAME && decl.hasChildren()) {
                    names.add(decl.getString());
                }
            }
        }
        return names;
    }

    /**
     * Collects the names a let wrapper scope declares as {@code const}. Only a {@code for (const
     * ...; ...; ...)} head, which {@link IRFactory} splits into a scope of its own, still carries a
     * symbol table by the time it gets here; the wrappers synthesized for ordinary blocks have
     * already given their symbols away.
     */
    private static Set<String> constNames(Node scopeNode) {
        if (!(scopeNode instanceof Scope)) {
            return Collections.emptySet();
        }
        Map<String, Symbol> symbolTable = ((Scope) scopeNode).getSymbolTable();
        if (symbolTable == null) {
            return Collections.emptySet();
        }
        Set<String> names = new HashSet<>(4);
        for (Symbol symbol : symbolTable.values()) {
            if (symbol.getDeclType() == Symbol.Type.CONST) {
                names.add(symbol.getName());
            }
        }
        return names;
    }

    protected void visitNew(Node node, ScriptNode tree) {}

    protected void visitCall(Node node, ScriptNode tree) {}

    protected Node visitLet(boolean createScope, Node parent, Node previous, Node scopeNode) {
        Node vars = scopeNode.getFirstChild();
        Node body = vars.getNext();
        Set<String> constNames = constNames(scopeNode);
        scopeNode.removeChild(vars);
        scopeNode.removeChild(body);
        boolean isExpression = scopeNode.getType() == Token.LETEXPR;
        Node result;
        Node newVars = new Node(Token.ENTER_SCOPE);
        if (createScope) {
            result = new Node(isExpression ? Token.SCOPEEXPR : Token.BLOCK);
            result = replaceCurrent(parent, previous, scopeNode, result);
            ArrayList<Object> list = new ArrayList<>();
            ArrayList<Boolean> consts = new ArrayList<>();
            for (Node v = vars.getFirstChild(); v != null; v = v.getNext()) {
                Node current = v;
                if (current.getType() == Token.LETEXPR) {
                    // destructuring in let expr, e.g. let ([x, y] = [3, 4]) {}
                    List<?> destructuringNames =
                            (List<?>) current.getProp(Node.DESTRUCTURING_NAMES);
                    Node c = current.getFirstChild();
                    if (c.getType() != Token.LET) throw Kit.codeBug();
                    // Add initialization code to front of body
                    if (isExpression) {
                        body = new Node(Token.COMMA, c.getNext(), body);
                    } else {
                        body = new Node(Token.BLOCK, new Node(Token.EXPR_VOID, c.getNext()), body);
                    }
                    // Update "list" and "objectLiteral" for the variables
                    // defined in the destructuring assignment
                    if (destructuringNames != null) {
                        list.addAll(destructuringNames);
                        for (int i = 0; i < destructuringNames.size(); i++) {
                            consts.add(Boolean.FALSE);
                            newVars.addChildToBack(new Node(Token.VOID, Node.newNumber(0.0)));
                        }
                    }
                    // Process all NAME children of the inner LET node (not just the first)
                    // This includes the main temp variable ($0) and any computed property temps
                    // ($1, $2, etc.)
                    for (Node child = c.getFirstChild(); child != null; child = child.getNext()) {
                        if (child.getType() != Token.NAME) throw Kit.codeBug();
                        list.add(ScriptRuntime.getIndexObject(child.getString()));
                        consts.add(Boolean.FALSE);
                        Node init = child.getFirstChild();
                        if (init == null) {
                            init = new Node(Token.VOID, Node.newNumber(0.0));
                        }
                        newVars.addChildToBack(init);
                    }
                    continue; // Already processed all children, move to next sibling of LETEXPR
                }
                if (current.getType() != Token.NAME) throw Kit.codeBug();
                list.add(ScriptRuntime.getIndexObject(current.getString()));
                boolean isConst = current.getIntProp(Node.IS_CONST_PROP, 0) != 0;
                consts.add(isConst);
                Node init = current.getFirstChild();
                if (init == null) {
                    init = new Node(Token.VOID, Node.newNumber(0.0));
                } else if (isConst) {
                    // A const binding is only ever created here, never initialized:
                    // the declaration in the body of the scope does that.
                    throw Kit.codeBug();
                }
                newVars.addChildToBack(init);
            }
            newVars.putProp(Node.OBJECT_IDS_PROP, list.toArray());
            if (consts.contains(Boolean.TRUE)) {
                boolean[] constFlags = new boolean[consts.size()];
                for (int i = 0; i < constFlags.length; i++) {
                    constFlags[i] = consts.get(i);
                }
                newVars.putProp(Node.CONST_IDS_PROP, constFlags);
            }
            result.addChildToBack(newVars);
            result.addChildToBack(new Node(Token.SCOPE_BLOCK, body));
            result.addChildToBack(new Node(Token.LEAVE_SCOPE));
        } else {
            result = new Node(isExpression ? Token.COMMA : Token.BLOCK);
            result = replaceCurrent(parent, previous, scopeNode, result);
            newVars = new Node(Token.COMMA);
            for (Node v = vars.getFirstChild(); v != null; v = v.getNext()) {
                Node current = v;
                if (current.getType() == Token.LETEXPR) {
                    // destructuring in let expr, e.g. let ([x, y] = [3, 4]) {}
                    Node c = current.getFirstChild();
                    if (c.getType() != Token.LET) throw Kit.codeBug();
                    // Add initialization code to front of body
                    if (isExpression) {
                        body = new Node(Token.COMMA, c.getNext(), body);
                    } else {
                        body = new Node(Token.BLOCK, new Node(Token.EXPR_VOID, c.getNext()), body);
                    }
                    // We're removing the LETEXPR, so move the symbols
                    Scope.joinScopes((Scope) current, (Scope) scopeNode);
                    // Process all NAME children of the inner LET node (not just the first)
                    // This includes the main temp variable ($0) and any computed property temps
                    // ($1, $2, etc.)
                    for (Node child = c.getFirstChild(); child != null; child = child.getNext()) {
                        if (child.getType() != Token.NAME) throw Kit.codeBug();
                        Node stringNode = Node.newString(child.getString());
                        stringNode.setScope((Scope) scopeNode);
                        Node init = child.getFirstChild();
                        if (init == null) {
                            init = new Node(Token.VOID, Node.newNumber(0.0));
                        }
                        newVars.addChildToBack(new Node(Token.SETVAR, stringNode, init));
                    }
                    continue; // Already processed all children, move to next sibling of LETEXPR
                }
                if (current.getType() != Token.NAME) throw Kit.codeBug();
                Node stringNode = Node.newString(current.getString());
                stringNode.setScope((Scope) scopeNode);
                Node init = current.getFirstChild();
                if (init == null) {
                    init = new Node(Token.VOID, Node.newNumber(0.0));
                }
                // A const slot rejects SETVAR, and the declaration may run again if an outer
                // loop re-enters, so initialize it unconditionally.
                int setOp =
                        constNames.contains(current.getString())
                                ? Token.INITCONSTVAR
                                : Token.SETVAR;
                newVars.addChildToBack(new Node(setOp, stringNode, init));
            }
            if (isExpression) {
                result.addChildToBack(newVars);
                scopeNode.setType(Token.COMMA);
                result.addChildToBack(scopeNode);
                scopeNode.addChildToBack(body);
                if (body instanceof Scope) {
                    Scope scopeParent = ((Scope) body).getParentScope();
                    ((Scope) body).setParentScope((Scope) scopeNode);
                    ((Scope) scopeNode).setParentScope(scopeParent);
                }
            } else {
                scopeNode.setType(Token.BLOCK);
                scopeNode.addChildToFront(new Node(Token.EXPR_VOID, newVars));
                result.addChildToBack(scopeNode);
                scopeNode.addChildrenToBack(body);
                if (body instanceof Scope) {
                    Scope scopeParent = ((Scope) body).getParentScope();
                    ((Scope) body).setParentScope((Scope) scopeNode);
                    ((Scope) scopeNode).setParentScope(scopeParent);
                }
            }
        }
        return result;
    }

    private static Node addBeforeCurrent(Node parent, Node previous, Node current, Node toAdd) {
        if (previous == null) {
            if (!(current == parent.getFirstChild())) Kit.codeBug();
            parent.addChildToFront(toAdd);
        } else {
            if (!(current == previous.getNext())) Kit.codeBug();
            parent.addChildAfter(toAdd, previous);
        }
        return toAdd;
    }

    private static Node replaceCurrent(Node parent, Node previous, Node current, Node replacement) {
        if (previous == null) {
            if (!(current == parent.getFirstChild())) Kit.codeBug();
            parent.replaceChild(current, replacement);
        } else if (previous.next == current) {
            // Check cachedPrev.next == current is necessary due to possible
            // tree mutations
            parent.replaceChildAfter(previous, replacement);
        } else {
            parent.replaceChild(current, replacement);
        }
        return replacement;
    }

    private Deque<Node> loops;
    private Deque<Node> loopEnds;
    private boolean hasFinally;
    private CompilerEnvirons compilerEnv;
}
