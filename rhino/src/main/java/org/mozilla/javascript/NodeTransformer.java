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
import java.util.IdentityHashMap;
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
        ancestors = new ArrayDeque<>();
        firstJumpTo = new IdentityHashMap<>();
        initializedAt = new IdentityHashMap<>();
        checkedBindings = Collections.newSetFromMap(new IdentityHashMap<>());
        resets = new ArrayList<>();
        walkOrder = 0;

        // to save against upchecks if no finally blocks are used.
        hasFinally = false;

        // Flatten all only if we are not using scope objects for block scope
        boolean createScopeObjects =
                tree.getType() != Token.FUNCTION || ((FunctionNode) tree).requiresActivation();
        tree.flattenSymbolTable(
                !createScopeObjects, compilerEnv.getLanguageVersion() >= Context.VERSION_ES6);

        // Bindings held in local variable slots have their temporal dead zone checked inline, and
        // the walk works out which accesses need no check.
        trackDeadZones =
                !createScopeObjects && compilerEnv.getLanguageVersion() >= Context.VERSION_ES6;

        // uncomment to print tree before transformation
        if (Token.printTrees) System.out.println(tree.toStringTree(tree));
        transformCompilationUnit_r(tree, tree, tree, createScopeObjects, inStrictMode, false);

        if (trackDeadZones) {
            dropUnneededDeadZones(tree);
        }
    }

    /**
     * A binding none of whose accesses needs a check can never be seen uninitialized, so a let need
     * not start out uninitialized, and re-entering its scope need not reset it.
     */
    private void dropUnneededDeadZones(ScriptNode tree) {
        for (Reset reset : resets) {
            if (hasDeadZone(reset.symbol) && !checkedBindings.contains(reset.symbol)) {
                reset.scope.removeChild(reset.node);
            }
        }
        for (Symbol symbol : tree.getSymbols()) {
            if (symbol.getDeclType() == Symbol.Type.LET && !checkedBindings.contains(symbol)) {
                tree.clearParamOrVarLet(symbol.getIndex());
            }
        }
    }

    /**
     * Notes the jumps and jump targets the walk passes, in source order. A forward jump seen before
     * a binding was initialized may land past that initialization, so on reaching its target the
     * binding is no longer known to be initialized.
     */
    private void beforeChildren(Node node) {
        if (node instanceof Jump) {
            Jump jump = (Jump) node;
            noteJumpTo(jump.target);
            switch (node.getType()) {
                case Token.SWITCH -> noteJumpTo(jump.getDefault());
                case Token.TRY -> noteJumpTo(jump.getFinally());
                case Token.LOOP -> noteJumpTo(jump.getContinue());
                default -> {}
            }
        } else if (node.getType() == Token.TARGET) {
            Integer first = firstJumpTo.get(node);
            if (first != null) {
                initializedAt.values().removeIf(order -> order > first);
            }
        }
    }

    private void noteJumpTo(Node target) {
        if (target != null) {
            firstJumpTo.putIfAbsent(target, ++walkOrder);
        }
    }

    /**
     * Once a store into a binding has run, the binding is initialized, for as long as nothing can
     * jump past the store. Jumps are dealt with by {@link #beforeChildren}; what is left is a store
     * that an expression may skip.
     */
    private void afterChildren(Node node) {
        int type = node.getType();
        if (type != Token.SETVAR && type != Token.SETCONSTVAR && type != Token.INITCONSTVAR) {
            return;
        }
        Node name = node.getFirstChild();
        Scope defining = name.getScope();
        if (defining == null) {
            return;
        }
        Symbol symbol = defining.getSymbol(name.getString());
        if (hasDeadZone(symbol)
                && !initializedAt.containsKey(symbol)
                && alwaysRunsInScope(defining)) {
            initializedAt.put(symbol, ++walkOrder);
        }
    }

    /**
     * Returns true if the node being visited runs whenever the code around it in the scope that
     * defines a binding does.
     */
    private boolean alwaysRunsInScope(Scope defining) {
        for (Node ancestor : ancestors) {
            if (ancestor == defining) {
                return true;
            }
            switch (ancestor.getType()) {
                case Token.HOOK, Token.AND, Token.OR, Token.NULLISH_COALESCING -> {
                    return false;
                }
                default -> {
                    if (ancestor.getIntProp(Node.OPTIONAL_CHAINING, 0) == 1) {
                        return false;
                    }
                }
            }
        }
        // The walk does not count the function or script itself among the ancestors.
        return defining instanceof ScriptNode;
    }

    /**
     * Returns true if an access to the symbol may find it in its temporal dead zone. Records the
     * symbol as needing checks if so.
     */
    private boolean needsDeadZoneCheck(Symbol symbol) {
        if (!hasDeadZone(symbol)) {
            return false;
        }
        if (trackDeadZones) {
            if (initializedAt.containsKey(symbol)) {
                return false;
            }
            checkedBindings.add(symbol);
        }
        return true;
    }

    private void transformCompilationUnit_r(
            final ScriptNode tree,
            final Node parent,
            Scope scope,
            boolean createScopeObjects,
            boolean inStrictMode,
            boolean inLoop) {
        Node node = null;
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

            try {
                node =
                        transform_singleNode(
                                tree,
                                parent,
                                scope,
                                createScopeObjects,
                                inStrictMode,
                                inLoop,
                                node,
                                previous);

                ancestors.push(node);
                transformCompilationUnit_r(
                        tree,
                        node,
                        node instanceof Scope ? (Scope) node : scope,
                        createScopeObjects,
                        inStrictMode,
                        inLoop || node.getType() == Token.LOOP);
                ancestors.pop();
                if (trackDeadZones) {
                    afterChildren(node);
                }
            } catch (ContinueLoop c) {
                node = c.newNode;
            }
        }
    }

    private final class ContinueLoop extends Throwable {
        private final Node newNode;

        ContinueLoop(Node newNode) {
            this.newNode = newNode;
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            // Do nothing, we're just doing control flow here.
            return this;
        }
    }

    private Node transform_singleNode(
            final ScriptNode tree,
            final Node parent,
            Scope scope,
            boolean createScopeObjects,
            boolean inStrictMode,
            boolean inLoop,
            Node node,
            Node previous)
            throws ContinueLoop {
        { // Prevent indenting changes
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
                    List<Boolean> constFlags = new ArrayList<>();
                    List<Boolean> letFlags = new ArrayList<>();
                    for (Symbol symbol : newScope.getSymbolTable().values()) {
                        Node name = Node.newString(Token.NAME, symbol.getName());
                        // Lexical bindings must be created uninitialized, so that the
                        // declaration itself can initialize them. A loop scope belongs to
                        // a for-in/of head, whose bindings are declared afresh in each
                        // iteration's own scope; this one's are never initialized, and
                        // only put the head's names in their temporal dead zone while the
                        // iterated expression is evaluated.
                        boolean lexicalScope = type == Token.BLOCK || type == Token.LOOP;
                        constFlags.add(symbol.getDeclType() == Symbol.Type.CONST && lexicalScope);
                        letFlags.add(isUninitializedLet(symbol) && lexicalScope);
                        innerLet.addChildToBack(name);
                    }
                    putConstFlags(innerLet, constFlags);
                    putFlags(innerLet, Node.LET_IDS_PROP, letFlags);
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

                case Token.CATCH_SCOPE:
                    {
                        // The catch scope is entered before, and left after, its
                        // sibling handler statement.
                        loops.push(node);
                        Node leave = node.getNext().getNext();
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
                                    || elemtype == Token.SCOPE_BLOCK
                                    || elemtype == Token.CATCH_SCOPE) {
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
                            throw new ContinueLoop(node);
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
                                    || elemtype == Token.SCOPE_BLOCK
                                    || elemtype == Token.CATCH_SCOPE) {
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
                        // An ES6 let is created uninitialized, so its declaration initializes it
                        // even without an initializer.
                        boolean initializesLet =
                                type == Token.LET
                                        && compilerEnv.getLanguageVersion() >= Context.VERSION_ES6;
                        for (Node cursor = node.getFirstChild(); cursor != null; ) {
                            // Move cursor to next before createAssignment gets chance
                            // to change n.next
                            Node n = cursor;
                            cursor = cursor.getNext();
                            if (n.getType() == Token.NAME) {
                                Node init;
                                if (n.hasChildren()) {
                                    init = n.getFirstChild();
                                    n.removeChild(init);
                                } else if (initializesLet) {
                                    init = new Node(Token.VOID, Node.newNumber(0.0));
                                } else {
                                    continue;
                                }
                                n.setType(Token.BINDNAME);
                                int setOp;
                                if (type == Token.CONST) {
                                    setOp = Token.SETCONST;
                                } else if (initializesLet) {
                                    setOp = Token.INIT_LET;
                                } else {
                                    setOp = Token.SETNAME;
                                }
                                n = new Node(setOp, n, init);
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

                case Token.ITERATION_SCOPE:
                    {
                        // A for-in/of head binds afresh on each iteration rather than carrying
                        // the previous iteration's bindings over: what that iteration handed to
                        // any closure it created has to stay as it was, and a const has to be
                        // created uninitialized for the assignment of the enumerated value to
                        // initialize it. Leaving the current scope and entering a new one does
                        // both; what the first iteration leaves is the loop's own scope, which
                        // has no further use once the iterated expression has been evaluated.
                        // A flattened scope keeps one slot per binding for the whole call and
                        // nothing can capture it, so INITCONSTVAR storing into that slot again
                        // is all the freshness it needs.
                        Node replacement;
                        if (createScopeObjects
                                && compilerEnv.getLanguageVersion() >= Context.VERSION_ES6) {
                            replacement = new Node(Token.BLOCK);
                            replacement.addChildToBack(new Node(Token.LEAVE_SCOPE));
                            replacement.addChildToBack(enterIterationScope(node));
                        } else {
                            replacement = new Node(Token.EMPTY);
                        }
                        replacement.setLineColumnNumber(node.getLineno(), node.getColumn());
                        node = replaceCurrent(parent, previous, node, replacement);
                        break;
                    }

                case Token.TYPEOFNAME:
                    {
                        Scope defining = scope.getDefiningScope(node.getString());
                        if (defining != null) {
                            node.setScope(defining);
                            if (needsDeadZoneCheck(defining.getSymbol(node.getString()))) {
                                node.putIntProp(Node.TDZ_CHECK_PROP, 1);
                            }
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
                case Token.INIT_LET:
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
                            // The declaration's own store initializes the binding, but any other
                            // access may find it still in its temporal dead zone.
                            boolean deadZone =
                                    type != Token.INIT_LET
                                            && type != Token.SETCONST
                                            && type != Token.DELPROP
                                            && needsDeadZoneCheck(defining.getSymbol(name));
                            if (type == Token.NAME) {
                                node.setType(Token.GETVAR);
                                if (deadZone) {
                                    node.putIntProp(Node.TDZ_CHECK_PROP, 1);
                                }
                            } else if (type == Token.SETNAME
                                    || type == Token.STRICT_SETNAME
                                    || type == Token.INIT_LET) {
                                node.setType(Token.SETVAR);
                                nameSource.setType(Token.STRING);
                                if (deadZone) {
                                    node.putIntProp(Node.TDZ_CHECK_PROP, 1);
                                }
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

            if (trackDeadZones) {
                beforeChildren(node);
            }
            return node;
        }
    }

    /**
     * Prepends to a flattened block scope the stores that re-entering it implies. Because the scope
     * is not reified, each of its bindings keeps one slot for the whole call, so a declaration that
     * ran on an earlier iteration is still in effect: {@code SETCONSTVAR} stores only into a slot
     * still marked uninitialized, and a {@code let} without an initializer stores nothing at all.
     *
     * <p>Only bindings that the scope does not itself initialize on entry need this. A declaration
     * among the scope's leading declarations always runs before anything can read the binding, so
     * it is left to do the initializing; one that a jump can bypass — a {@code case} clause of a
     * switch, or the head of a for-in/for-of loop — or that other code may run before is not, as
     * that code must find the binding in its temporal dead zone.
     */
    private void addScopeReinit(Scope scope) {
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
            resets.add(new Reset(scope, reset, symbol));
            if (previous == null) {
                scope.addChildToFront(reset);
            } else {
                scope.addChildAfter(reset, previous);
            }
            previous = reset;
        }
    }

    /**
     * Collects the names that the leading declarations of a scope initialize. The walk stops at the
     * first statement that is not a declaration, since from there on a jump can land past a
     * declaration, or other code can read a binding before it is declared. It also stops at an
     * initializer that may read one of the scope's bindings not initialized yet, as on a later
     * iteration that read would otherwise find whatever an earlier one left in its slot.
     */
    private static List<String> namesInitializedOnEntry(Scope scope) {
        List<String> names = new ArrayList<>(4);
        Map<String, Symbol> symbolTable = scope.getSymbolTable();
        for (Node child = scope.getFirstChild(); child != null; child = child.getNext()) {
            int type = child.getType();
            if (type != Token.LET && type != Token.CONST && type != Token.VAR) {
                break;
            }
            for (Node decl = child.getFirstChild(); decl != null; decl = decl.getNext()) {
                if (decl.getType() != Token.NAME) {
                    // A destructuring declaration
                    return names;
                }
                if (decl.hasChildren()) {
                    if (readsUninitialized(decl.getFirstChild(), symbolTable, names)) {
                        return names;
                    }
                    names.add(decl.getString());
                } else if (type == Token.LET) {
                    names.add(decl.getString());
                }
            }
        }
        return names;
    }

    private static boolean readsUninitialized(
            Node node, Map<String, Symbol> symbolTable, List<String> initialized) {
        int type = node.getType();
        if ((type == Token.NAME || type == Token.BINDNAME || type == Token.TYPEOFNAME)
                && symbolTable.containsKey(node.getString())
                && !initialized.contains(node.getString())) {
            return true;
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            if (readsUninitialized(child, symbolTable, initialized)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Records on a declaration list which of its children declare consts, for {@link #visitLet} to
     * read. A destructuring child stands for all the names it declares.
     */
    static void putConstFlags(Node declarations, List<Boolean> flags) {
        putFlags(declarations, Node.CONST_IDS_PROP, flags);
    }

    static void putFlags(Node declarations, int prop, List<Boolean> flags) {
        if (!flags.contains(Boolean.TRUE)) {
            return;
        }
        boolean[] array = new boolean[flags.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = flags.get(i);
        }
        declarations.putProp(prop, array);
    }

    /** Returns true if the symbol is a binding that cannot be accessed before it is initialized. */
    private boolean hasDeadZone(Symbol symbol) {
        return symbol != null
                && (symbol.getDeclType() == Symbol.Type.LET
                        || symbol.getDeclType() == Symbol.Type.CONST)
                && compilerEnv.getLanguageVersion() >= Context.VERSION_ES6;
    }

    /** Returns true if the symbol is a let binding that starts out uninitialized. */
    private boolean isUninitializedLet(Symbol symbol) {
        return symbol != null
                && symbol.getDeclType() == Symbol.Type.LET
                && compilerEnv.getLanguageVersion() >= Context.VERSION_ES6;
    }

    private boolean isUninitializedLet(Node scopeNode, String name) {
        return scopeNode instanceof Scope
                && isUninitializedLet(((Scope) scopeNode).getSymbol(name));
    }

    /**
     * Builds the {@code ENTER_SCOPE} that creates a for-in/of head's bindings in a new iteration
     * environment. A const or let is created uninitialized, for its declaration to initialize;
     * anything else starts out undefined. Both back ends walk the names and the children in step,
     * and evaluate no child for a name they create as a const.
     */
    private static Node enterIterationScope(Node iterationScope) {
        Object[] names = (Object[]) iterationScope.getProp(Node.OBJECT_IDS_PROP);
        boolean[] consts = (boolean[]) iterationScope.getProp(Node.CONST_IDS_PROP);
        Node enter = new Node(Token.ENTER_SCOPE);
        enter.putProp(Node.OBJECT_IDS_PROP, names);
        enter.putProp(Node.CONST_IDS_PROP, consts);
        enter.putProp(Node.LET_IDS_PROP, iterationScope.getProp(Node.LET_IDS_PROP));
        for (int i = 0; i < names.length; i++) {
            enter.addChildToBack(new Node(Token.VOID, Node.newNumber(0.0)));
        }
        return enter;
    }

    protected void visitNew(Node node, ScriptNode tree) {}

    protected void visitCall(Node node, ScriptNode tree) {}

    protected Node visitLet(boolean createScope, Node parent, Node previous, Node scopeNode) {
        Node vars = scopeNode.getFirstChild();
        Node realBody = vars.getNext();
        boolean[] varConsts = (boolean[]) vars.getProp(Node.CONST_IDS_PROP);
        // Set when the declarations are in the body of the scope rather than here
        boolean[] varLets = (boolean[]) vars.getProp(Node.LET_IDS_PROP);
        // Destructuring wraps the body, but it is the original body that must be re-parented.
        scopeNode.removeChild(vars);
        scopeNode.removeChild(realBody);
        boolean isExpression = scopeNode.getType() == Token.LETEXPR;
        Node body = isExpression ? new Node(Token.COMMA) : new Node(Token.BLOCK);
        Node originalBody = realBody;
        Node result;
        Node newVars = new Node(Token.ENTER_SCOPE);
        if (createScope) {
            result = new Node(isExpression ? Token.SCOPEEXPR : Token.BLOCK);
            result = replaceCurrent(parent, previous, scopeNode, result);
            ArrayList<Object> list = new ArrayList<>();
            ArrayList<Boolean> consts = new ArrayList<>();
            ArrayList<Boolean> lets = new ArrayList<>();
            int position = -1;
            for (Node v = vars.getFirstChild(); v != null; v = v.getNext()) {
                Node current = v;
                position++;
                boolean declaredConst = varConsts != null && varConsts[position];
                if (current.getType() == Token.LETEXPR) {
                    // destructuring in let expr, e.g. let ([x, y] = [3, 4]) {}
                    List<?> destructuringNames =
                            (List<?>) current.getProp(Node.DESTRUCTURING_NAMES);
                    Node c = current.getFirstChild();
                    if (c.getType() != Token.LET) throw Kit.codeBug();
                    // Add initialization code to front of body
                    if (isExpression) {
                        body.addChildToBack(c.getNext());
                    } else {
                        body.addChildToBack(new Node(Token.EXPR_VOID, c.getNext()));
                    }
                    // Update "list" and "objectLiteral" for the variables
                    // defined in the destructuring assignment
                    if (destructuringNames != null) {
                        list.addAll(destructuringNames);
                        for (Object name : destructuringNames) {
                            consts.add(declaredConst);
                            lets.add(
                                    !declaredConst && isUninitializedLet(scopeNode, (String) name));
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
                        lets.add(Boolean.FALSE);
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
                consts.add(declaredConst);
                boolean declaredLet =
                        varLets != null
                                ? varLets[position]
                                : !declaredConst
                                        && isUninitializedLet(scopeNode, current.getString());
                lets.add(declaredLet);
                Node init = current.getFirstChild();
                if (declaredLet) {
                    if (varLets == null) {
                        // The scope creates the binding uninitialized, and this initializes it.
                        if (init == null) {
                            init = new Node(Token.VOID, Node.newNumber(0.0));
                        } else {
                            current.removeChild(init);
                        }
                        Node bind = Node.newString(Token.BINDNAME, current.getString());
                        Node initLet = new Node(Token.INIT_LET, bind, init);
                        body.addChildToBack(
                                isExpression ? initLet : new Node(Token.EXPR_VOID, initLet));
                    }
                    init = new Node(Token.VOID, Node.newNumber(0.0));
                } else if (init == null) {
                    init = new Node(Token.VOID, Node.newNumber(0.0));
                } else if (declaredConst) {
                    // The scope creates the binding uninitialized, and this initializes it.
                    current.removeChild(init);
                    Node bind = Node.newString(Token.BINDNAME, current.getString());
                    Node setConst = new Node(Token.SETCONST, bind, init);
                    body.addChildToBack(new Node(Token.EXPR_VOID, setConst));
                    init = new Node(Token.VOID, Node.newNumber(0.0));
                }
                newVars.addChildToBack(init);
            }
            newVars.putProp(Node.OBJECT_IDS_PROP, list.toArray());
            putConstFlags(newVars, consts);
            putFlags(newVars, Node.LET_IDS_PROP, lets);
            result.addChildToBack(newVars);
            body.addChildToBack(realBody);
            result.addChildToBack(new Node(Token.SCOPE_BLOCK, body));
            result.addChildToBack(new Node(Token.LEAVE_SCOPE));
        } else {
            result = new Node(isExpression ? Token.COMMA : Token.BLOCK);
            result = replaceCurrent(parent, previous, scopeNode, result);
            newVars = new Node(Token.COMMA);
            int position = -1;
            for (Node v = vars.getFirstChild(); v != null; v = v.getNext()) {
                Node current = v;
                boolean declaredConst = varConsts != null && varConsts[++position];
                if (current.getType() == Token.LETEXPR) {
                    // destructuring in let expr, e.g. let ([x, y] = [3, 4]) {}
                    Node c = current.getFirstChild();
                    if (c.getType() != Token.LET) throw Kit.codeBug();
                    // Add initialization code to front of body
                    if (isExpression) {
                        body.addChildToBack(c.getNext());
                    } else {
                        body.addChildToBack(new Node(Token.EXPR_VOID, c.getNext()));
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
                int setOp = declaredConst ? Token.INITCONSTVAR : Token.SETVAR;
                newVars.addChildToBack(new Node(setOp, stringNode, init));
            }
            if (isExpression) {
                result.addChildToBack(newVars);
                scopeNode.setType(Token.COMMA);
                result.addChildToBack(scopeNode);
                body.addChildToBack(realBody);
                scopeNode.addChildToBack(body);
                if (originalBody instanceof Scope) {
                    Scope scopeParent = ((Scope) originalBody).getParentScope();
                    ((Scope) originalBody).setParentScope((Scope) scopeNode);
                    ((Scope) scopeNode).setParentScope(scopeParent);
                }
            } else {
                scopeNode.setType(Token.BLOCK);
                scopeNode.addChildToFront(new Node(Token.EXPR_VOID, newVars));
                result.addChildToBack(scopeNode);
                body.addChildToBack(realBody);
                scopeNode.addChildrenToBack(body);
                if (originalBody instanceof Scope) {
                    Scope scopeParent = ((Scope) originalBody).getParentScope();
                    ((Scope) originalBody).setParentScope((Scope) scopeNode);
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

    private static final class Reset {
        private final Scope scope;
        private final Node node;
        private final Symbol symbol;

        public Reset(Scope scope, Node node, Symbol symbol) {
            this.scope = scope;
            this.node = node;
            this.symbol = symbol;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((scope == null) ? 0 : scope.hashCode());
            result = prime * result + ((node == null) ? 0 : node.hashCode());
            result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Reset other = (Reset) obj;
            if (scope == null) {
                if (other.scope != null) return false;
            } else if (!scope.equals(other.scope)) return false;
            if (node == null) {
                if (other.node != null) return false;
            } else if (!node.equals(other.node)) return false;
            if (symbol == null) {
                if (other.symbol != null) return false;
            } else if (!symbol.equals(other.symbol)) return false;
            return true;
        }
    }

    private boolean trackDeadZones;
    // The nodes enclosing the one being visited, innermost first
    private Deque<Node> ancestors;
    // For each jump target, when the walk first passed a jump to it
    private Map<Node, Integer> firstJumpTo;
    // For each binding known to be initialized, when the walk passed the store that did so
    private Map<Symbol, Integer> initializedAt;
    private Set<Symbol> checkedBindings;
    private List<Reset> resets;
    private int walkOrder;
    private boolean hasFinally;
    private CompilerEnvirons compilerEnv;
}
