package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.List;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.ScriptNode;

/**
 * Common utilities usable by all the compilers for populating {@link JSDescriptor.Builder} entries
 * from IR nodes.
 */
public class CodeGenUtils {

    /** Populates builder data for a nested function. */
    public static void fillInForNestedFunction(
            JSDescriptor.Builder<JSFunction> builder,
            JSDescriptor.Builder<?> parent,
            FunctionNode fn) {
        final AstNode fnParent = fn.getParent();
        if (!(fnParent instanceof AstRoot
                || fnParent instanceof Scope
                || fnParent instanceof Block)) {
            builder.declaredAsFunctionExpression = true;
            boolean isArrow = fn.getFunctionType() == FunctionNode.ARROW_FUNCTION;
            builder.hasLexicalThis = isArrow;
            builder.hasPrototype = !isArrow;
            if (!isArrow) {
                builder.constructor = builder.code;
            } else {
                builder.constructor = new JSCode.NullBuilder<JSFunction>();
            }
        } else {
            builder.hasLexicalThis = false;
            builder.hasPrototype = true;
            builder.constructor = builder.code;
        }

        fillInForFunction(builder, fn);
    }

    private static void fillInForFunction(JSDescriptor.Builder builder, FunctionNode fn) {
        builder.functionType = fn.getFunctionType();
        builder.requiresActivationFrame = fn.requiresActivation();
        builder.requiresArgumentObject = fn.requiresArgumentObject();
        if (fn.getFunctionName() != null) {
            builder.name = fn.getName();
        }
        if (fn.isInStrictMode()) {
            builder.isStrict = true;
        }
        if (fn.isES6Generator()) {
            builder.isES6Generator = true;
        }
        if (fn.isShorthand()) {
            builder.isShorthand = true;
        }
        fillInCommon(builder, fn);
    }

    /** Populate builder data for a top level function. */
    public static void fillInForTopLevelFunction(
            JSDescriptor.Builder builder,
            FunctionNode fn,
            String rawSource,
            CompilerEnvirons compilerEnv) {

        builder.hasPrototype = true;

        fillInTopLevelCommon(builder, fn, rawSource, compilerEnv);
        fillInForFunction(builder, fn);
    }

    /** Populate builder data for a top level script. */
    public static void fillInForScript(
            JSDescriptor.Builder builder,
            ScriptNode scriptOrFn,
            String rawSource,
            CompilerEnvirons compilerEnv) {
        builder.hasPrototype = false;

        fillInTopLevelCommon(builder, scriptOrFn, rawSource, compilerEnv);
        fillInCommon(builder, scriptOrFn);
    }

    private static void fillInTopLevelCommon(
            JSDescriptor.Builder builder,
            ScriptNode scriptOrFn,
            String rawSource,
            CompilerEnvirons compilerEnv) {
        builder.sourceFile = scriptOrFn.getSourceName();
        builder.rawSource = rawSource;
        builder.isTopLevel = true;
        builder.isScript = true;
        builder.isEvalFunction = compilerEnv.isInEval();
        builder.isStrict = scriptOrFn.isInStrictMode();
        builder.hasLexicalThis = false;
        builder.securityController = compilerEnv.securityController();
        builder.securityDomain = compilerEnv.securityDomain();
    }

    private static void fillInCommon(JSDescriptor.Builder builder, ScriptNode scriptOrFn) {
        builder.paramAndVarNames = scriptOrFn.getParamAndVarNames();
        builder.paramCount = scriptOrFn.getParamCount();
        builder.paramIsConst = scriptOrFn.getParamAndVarConst();
        builder.paramAndVarCount = scriptOrFn.getParamAndVarCount();
        builder.hasRestArg = scriptOrFn.hasRestParameter();
        builder.hasDefaultParameters = scriptOrFn.getDefaultParams() != null;

        // For generators, store default param source for runtime evaluation
        // (but not if they contain super - those are compiled as bytecode)
        if (scriptOrFn instanceof FunctionNode) {
            FunctionNode fn = (FunctionNode) scriptOrFn;
            if (fn.isGenerator() && scriptOrFn.getDefaultParams() != null) {
                List<Object> defaultParams = scriptOrFn.getDefaultParams();
                ArrayList<String> sourceList = new ArrayList<>();
                boolean hasSuper = false;
                for (int i = 0; i < defaultParams.size() - 1; i += 2) {
                    if (defaultParams.get(i) instanceof String
                            && defaultParams.get(i + 1) instanceof AstNode) {
                        String paramName = (String) defaultParams.get(i);
                        AstNode expr = (AstNode) defaultParams.get(i + 1);
                        if (containsSuper(expr)) {
                            hasSuper = true;
                            break;
                        }
                        sourceList.add(paramName);
                        sourceList.add(expr.toSource());
                    }
                }
                // Only store if no super (super cases are handled as bytecode)
                if (!hasSuper && !sourceList.isEmpty()) {
                    builder.generatorDefaultParams = sourceList.toArray(new String[0]);
                }
            }
        }

        // Calculate arity (function.length) - count params before first default
        builder.arity = FunctionNode.calculateFunctionArity(scriptOrFn);

        builder.rawSourceStart = scriptOrFn.getRawSourceStart();
        builder.rawSourceEnd = scriptOrFn.getRawSourceEnd();
    }

    /** Configure the constructor appropriately based on a function's type. */
    public static <T extends ScriptOrFn<T>> void setConstructor(
            JSDescriptor.Builder<T> builder, ScriptNode scriptOrFn) {
        if (scriptOrFn instanceof FunctionNode) {
            FunctionNode f = (FunctionNode) scriptOrFn;
            boolean isArrow = f.getFunctionType() == FunctionNode.ARROW_FUNCTION;
            if (isArrow || f.isMethodDefinition() || f.isGenerator()) {
                builder.constructor = new JSCode.NullBuilder<T>();
            } else {
                builder.constructor = builder.code;
            }
        } else {
            builder.constructor = new JSCode.NullBuilder<T>();
        }
    }

    /**
     * Check if an AST node contains any super references.
     *
     * @param node the AST node to check
     * @return true if the node or any of its children contain super references
     */
    private static boolean containsSuper(AstNode node) {
        int type = node.getType();
        // Check if this node is a super-related token
        if (type == Token.SUPER
                || type == Token.GETPROP_SUPER
                || type == Token.GETPROPNOWARN_SUPER
                || type == Token.SETPROP_SUPER
                || type == Token.GETELEM_SUPER
                || type == Token.SETELEM_SUPER) {
            return true;
        }

        // For InfixExpression nodes (like PropertyGet), check left and right fields
        if (node instanceof InfixExpression) {
            InfixExpression infix = (InfixExpression) node;
            if (infix.getLeft() != null && containsSuper(infix.getLeft())) {
                return true;
            }
            if (infix.getRight() != null && containsSuper(infix.getRight())) {
                return true;
            }
        }

        // Recursively check all children
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof AstNode && containsSuper((AstNode) child)) {
                return true;
            }
        }

        return false;
    }
}
