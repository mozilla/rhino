package org.mozilla.javascript;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.FunctionNode;
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
}
