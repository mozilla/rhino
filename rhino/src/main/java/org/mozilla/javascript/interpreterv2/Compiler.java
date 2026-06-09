package org.mozilla.javascript.interpreterv2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.mozilla.javascript.CodeGenUtils;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.InstructionArray;
import org.mozilla.javascript.JSDescriptor;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.NodeTransformer;
import org.mozilla.javascript.RegExpProxy;
import org.mozilla.javascript.ScriptOrFn;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Jump;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.ast.TemplateCharacters;
import org.mozilla.javascript.interpreterv2.instruction.Add;
import org.mozilla.javascript.interpreterv2.instruction.ArrayLit;
import org.mozilla.javascript.interpreterv2.instruction.ArrayLitWithSpread;
import org.mozilla.javascript.interpreterv2.instruction.BigInt;
import org.mozilla.javascript.interpreterv2.instruction.BindName;
import org.mozilla.javascript.interpreterv2.instruction.BitAnd;
import org.mozilla.javascript.interpreterv2.instruction.BitNot;
import org.mozilla.javascript.interpreterv2.instruction.BitOr;
import org.mozilla.javascript.interpreterv2.instruction.BitXor;
import org.mozilla.javascript.interpreterv2.instruction.Call;
import org.mozilla.javascript.interpreterv2.instruction.CatchScope;
import org.mozilla.javascript.interpreterv2.instruction.Cleanup;
import org.mozilla.javascript.interpreterv2.instruction.ClosureExpression;
import org.mozilla.javascript.interpreterv2.instruction.ClosureStatement;
import org.mozilla.javascript.interpreterv2.instruction.Comparison;
import org.mozilla.javascript.interpreterv2.instruction.DebuggerInstruction;
import org.mozilla.javascript.interpreterv2.instruction.DefaultNamespace;
import org.mozilla.javascript.interpreterv2.instruction.DelName;
import org.mozilla.javascript.interpreterv2.instruction.DelProp;
import org.mozilla.javascript.interpreterv2.instruction.DelPropSuper;
import org.mozilla.javascript.interpreterv2.instruction.DelRef;
import org.mozilla.javascript.interpreterv2.instruction.Divide;
import org.mozilla.javascript.interpreterv2.instruction.Dup;
import org.mozilla.javascript.interpreterv2.instruction.ElemAndThis;
import org.mozilla.javascript.interpreterv2.instruction.ElemAndThisOptional;
import org.mozilla.javascript.interpreterv2.instruction.ElemIncDec;
import org.mozilla.javascript.interpreterv2.instruction.EnterDotQuery;
import org.mozilla.javascript.interpreterv2.instruction.EnterWith;
import org.mozilla.javascript.interpreterv2.instruction.EnumId;
import org.mozilla.javascript.interpreterv2.instruction.EnumInitArray;
import org.mozilla.javascript.interpreterv2.instruction.EnumInitKeys;
import org.mozilla.javascript.interpreterv2.instruction.EnumInitValues;
import org.mozilla.javascript.interpreterv2.instruction.EnumInitValuesInOrder;
import org.mozilla.javascript.interpreterv2.instruction.EnumNext;
import org.mozilla.javascript.interpreterv2.instruction.Equal;
import org.mozilla.javascript.interpreterv2.instruction.EscXmlAttr;
import org.mozilla.javascript.interpreterv2.instruction.EscXmlText;
import org.mozilla.javascript.interpreterv2.instruction.Exponentiate;
import org.mozilla.javascript.interpreterv2.instruction.Generator;
import org.mozilla.javascript.interpreterv2.instruction.GeneratorEnd;
import org.mozilla.javascript.interpreterv2.instruction.GeneratorReturn;
import org.mozilla.javascript.interpreterv2.instruction.GetElem;
import org.mozilla.javascript.interpreterv2.instruction.GetElemSuper;
import org.mozilla.javascript.interpreterv2.instruction.GetProp;
import org.mozilla.javascript.interpreterv2.instruction.GetPropSuper;
import org.mozilla.javascript.interpreterv2.instruction.GetRef;
import org.mozilla.javascript.interpreterv2.instruction.GetVar;
import org.mozilla.javascript.interpreterv2.instruction.GoSubroutine;
import org.mozilla.javascript.interpreterv2.instruction.Goto;
import org.mozilla.javascript.interpreterv2.instruction.IfEq;
import org.mozilla.javascript.interpreterv2.instruction.IfEqPop;
import org.mozilla.javascript.interpreterv2.instruction.IfNe;
import org.mozilla.javascript.interpreterv2.instruction.IfNotNullUndefined;
import org.mozilla.javascript.interpreterv2.instruction.IfNullUndefined;
import org.mozilla.javascript.interpreterv2.instruction.In;
import org.mozilla.javascript.interpreterv2.instruction.Instanceof;
import org.mozilla.javascript.interpreterv2.instruction.Instruction;
import org.mozilla.javascript.interpreterv2.instruction.Int;
import org.mozilla.javascript.interpreterv2.instruction.JumpInstruction;
import org.mozilla.javascript.interpreterv2.instruction.LeaveDotQuery;
import org.mozilla.javascript.interpreterv2.instruction.LeaveWith;
import org.mozilla.javascript.interpreterv2.instruction.LeftShift;
import org.mozilla.javascript.interpreterv2.instruction.LitPush;
import org.mozilla.javascript.interpreterv2.instruction.LitSetAt;
import org.mozilla.javascript.interpreterv2.instruction.LitSpread;
import org.mozilla.javascript.interpreterv2.instruction.LocalClear;
import org.mozilla.javascript.interpreterv2.instruction.LocalLoad;
import org.mozilla.javascript.interpreterv2.instruction.MethodExpression;
import org.mozilla.javascript.interpreterv2.instruction.Mod;
import org.mozilla.javascript.interpreterv2.instruction.Multiply;
import org.mozilla.javascript.interpreterv2.instruction.Name;
import org.mozilla.javascript.interpreterv2.instruction.NameAndThis;
import org.mozilla.javascript.interpreterv2.instruction.NameAndThisOptional;
import org.mozilla.javascript.interpreterv2.instruction.NameIncDec;
import org.mozilla.javascript.interpreterv2.instruction.Neg;
import org.mozilla.javascript.interpreterv2.instruction.New;
import org.mozilla.javascript.interpreterv2.instruction.NewObjectLiteral;
import org.mozilla.javascript.interpreterv2.instruction.NewObjectLiteralWithSpread;
import org.mozilla.javascript.interpreterv2.instruction.Nop;
import org.mozilla.javascript.interpreterv2.instruction.Not;
import org.mozilla.javascript.interpreterv2.instruction.NotEqual;
import org.mozilla.javascript.interpreterv2.instruction.Num;
import org.mozilla.javascript.interpreterv2.instruction.ObjectLit;
import org.mozilla.javascript.interpreterv2.instruction.ObjectRest;
import org.mozilla.javascript.interpreterv2.instruction.Pop;
import org.mozilla.javascript.interpreterv2.instruction.PopResult;
import org.mozilla.javascript.interpreterv2.instruction.Pos;
import org.mozilla.javascript.interpreterv2.instruction.PropAndThis;
import org.mozilla.javascript.interpreterv2.instruction.PropAndThisOptional;
import org.mozilla.javascript.interpreterv2.instruction.PropIncDec;
import org.mozilla.javascript.interpreterv2.instruction.PushConstant;
import org.mozilla.javascript.interpreterv2.instruction.RefIncDec;
import org.mozilla.javascript.interpreterv2.instruction.RefMember;
import org.mozilla.javascript.interpreterv2.instruction.RefName;
import org.mozilla.javascript.interpreterv2.instruction.RefNsMember;
import org.mozilla.javascript.interpreterv2.instruction.RefNsName;
import org.mozilla.javascript.interpreterv2.instruction.RefSpecial;
import org.mozilla.javascript.interpreterv2.instruction.Regexp;
import org.mozilla.javascript.interpreterv2.instruction.Rethrow;
import org.mozilla.javascript.interpreterv2.instruction.Return;
import org.mozilla.javascript.interpreterv2.instruction.ReturnResult;
import org.mozilla.javascript.interpreterv2.instruction.ReturnSubroutine;
import org.mozilla.javascript.interpreterv2.instruction.ReturnUndefined;
import org.mozilla.javascript.interpreterv2.instruction.RightShift;
import org.mozilla.javascript.interpreterv2.instruction.SaveScope;
import org.mozilla.javascript.interpreterv2.instruction.SetConst;
import org.mozilla.javascript.interpreterv2.instruction.SetConstVar;
import org.mozilla.javascript.interpreterv2.instruction.SetElem;
import org.mozilla.javascript.interpreterv2.instruction.SetElemSuper;
import org.mozilla.javascript.interpreterv2.instruction.SetName;
import org.mozilla.javascript.interpreterv2.instruction.SetProp;
import org.mozilla.javascript.interpreterv2.instruction.SetPropSuper;
import org.mozilla.javascript.interpreterv2.instruction.SetRef;
import org.mozilla.javascript.interpreterv2.instruction.SetVar;
import org.mozilla.javascript.interpreterv2.instruction.ShallowEqual;
import org.mozilla.javascript.interpreterv2.instruction.ShallowNotEqual;
import org.mozilla.javascript.interpreterv2.instruction.ShortNumber;
import org.mozilla.javascript.interpreterv2.instruction.SimpleSwitch;
import org.mozilla.javascript.interpreterv2.instruction.SpecialCall;
import org.mozilla.javascript.interpreterv2.instruction.SpecialCallNew;
import org.mozilla.javascript.interpreterv2.instruction.StartSubroutine;
import org.mozilla.javascript.interpreterv2.instruction.StrictSetName;
import org.mozilla.javascript.interpreterv2.instruction.StringConcat;
import org.mozilla.javascript.interpreterv2.instruction.Subtract;
import org.mozilla.javascript.interpreterv2.instruction.Super;
import org.mozilla.javascript.interpreterv2.instruction.TemplateLiteralCallsite;
import org.mozilla.javascript.interpreterv2.instruction.ThawFrame;
import org.mozilla.javascript.interpreterv2.instruction.This;
import org.mozilla.javascript.interpreterv2.instruction.ThisFunction;
import org.mozilla.javascript.interpreterv2.instruction.Throw;
import org.mozilla.javascript.interpreterv2.instruction.ToPropertyKey;
import org.mozilla.javascript.interpreterv2.instruction.Typeof;
import org.mozilla.javascript.interpreterv2.instruction.TypeofName;
import org.mozilla.javascript.interpreterv2.instruction.UnsignedRightShift;
import org.mozilla.javascript.interpreterv2.instruction.ValueAndThis;
import org.mozilla.javascript.interpreterv2.instruction.ValueAndThisOptional;
import org.mozilla.javascript.interpreterv2.instruction.VarIncDec;
import org.mozilla.javascript.interpreterv2.instruction.VoidInstruction;
import org.mozilla.javascript.interpreterv2.instruction.Yield;
import org.mozilla.javascript.interpreterv2.instruction.YieldStar;
import org.mozilla.javascript.interpreterv2.operand.BooleanOperand;
import org.mozilla.javascript.interpreterv2.operand.DoubleOperand;
import org.mozilla.javascript.interpreterv2.operand.GetVarOperand;
import org.mozilla.javascript.interpreterv2.operand.IntOperand;
import org.mozilla.javascript.interpreterv2.operand.NullOperand;
import org.mozilla.javascript.interpreterv2.operand.OneOperand;
import org.mozilla.javascript.interpreterv2.operand.Operand;
import org.mozilla.javascript.interpreterv2.operand.PeekOperand;
import org.mozilla.javascript.interpreterv2.operand.PopOperand;
import org.mozilla.javascript.interpreterv2.operand.StringOperand;
import org.mozilla.javascript.interpreterv2.operand.SuperOperand;
import org.mozilla.javascript.interpreterv2.operand.ThisOperand;
import org.mozilla.javascript.interpreterv2.operand.UndefinedOperand;
import org.mozilla.javascript.sourcemap.Position;
import org.mozilla.javascript.sourcemap.SourceMapper;

class UnknownInstructionException extends RuntimeException {
    private final String instructionName;
    private final int nodeType;

    public UnknownInstructionException(String instructionName, int nodeType) {
        super(
                String.format(
                        "Unknown instruction: %s, Node type: %s",
                        instructionName, Token.typeToName(nodeType)));
        this.instructionName = instructionName;
        this.nodeType = nodeType;
    }

    public String getInstructionName() {
        return instructionName;
    }

    public int getNodeType() {
        return nodeType;
    }
}

public class Compiler<T extends ScriptOrFn<T>> {
    private static final int ECF_TAIL = 1 << 0;
    private CompilerEnvirons compilerEnv;
    private CompilerData.Builder<T> builder;
    private JSDescriptor.Builder<T> descBuilder;
    private boolean treatNumericLiteralsLikeOldRhino = false;
    private int stackDepth = 0;
    private int lineNumber = -1;
    private final List<Instruction> instructions = new ArrayList<>();
    private final LineNumberTable.Builder lineNumberTable = new LineNumberTable.Builder();
    private ScriptNode scriptOrFn;
    private boolean inFunction = false;

    private int localTop;
    private int[] labelTable;
    private int labelTableTop;

    // fixupTable[i] = (label_index << 32) | fixup_site
    private final List<Long> fixupTable = new ArrayList<>();
    private boolean inTryFlag = false;
    private int exceptionTableTop;

    // Track all jump targets (both from label table and direct jumps)
    private final Set<Integer> jumpTargets = new HashSet<>();

    public Compiler() {
        var cx = Context.getCurrentContext();
        // FEATURE_TREAT_NUMERIC_LITERALS_LIKE_OLD_RHINO not present in open-source
    }

    public JSDescriptor<T> compile(
            CompilerEnvirons compilerEnv,
            ScriptNode tree,
            String rawSource,
            boolean returnFunction) {
        this.compilerEnv = compilerEnv;

        // Do side effecting node transformation. This mutates the tree
        new NodeTransformer().transform(tree, compilerEnv);

        if (returnFunction) {
            scriptOrFn = tree.getFunctionNode(0);
        } else {
            scriptOrFn = tree;
        }

        builder =
                new CompilerData.Builder<>(
                        compilerEnv.getLanguageVersion(),
                        scriptOrFn.getSourceName(),
                        scriptOrFn.isInStrictMode(),
                        CompilerData.FunctionType.Script);
        builder.topLevel = true;

        descBuilder = new JSDescriptor.Builder<T>();
        descBuilder.code = builder;
        // Generate instructions from the IR tree
        if (returnFunction) {
            CodeGenUtils.fillInForTopLevelFunction(
                    descBuilder, (FunctionNode) scriptOrFn, rawSource, compilerEnv);
            generateFunctionCode();
        } else {
            CodeGenUtils.fillInForScript(descBuilder, scriptOrFn, rawSource, compilerEnv);
            CodeGenUtils.setConstructor(descBuilder, scriptOrFn);
            generateInstructions(tree);
        }
        return descBuilder.build(x -> {});
    }

    private void addInstruction(Instruction instruction) {
        addInstruction(instructions.size(), instruction);
    }

    private void addInstruction(int index, Instruction instruction) {
        stackChange(instruction.stackChange());
        instructions.add(index, instruction);
    }

    private void generateInstructions(Node node) {
        generateNestedFunctions();
        generateStatement(node, 0);
        fixLabelGotos();

        // add RETURN_RESULT only to scripts as function always ends with RETURN
        if (builder.functionType == CompilerData.FunctionType.Script) {
            lineNumberTable.add(
                    instructions.size(),
                    -1,
                    -1); // This bytecode does not correspond to any real line number
            addInstruction(ReturnResult.instance);
        }

        new InstructionSimplification(instructions, jumpTargets).simplify();

        builder.instructions = instructions.toArray(new Instruction[0]);

        builder.instructionsRef = new InstructionArray(builder.instructions);

        builder.maxVars = scriptOrFn.getParamAndVarCount();
        // maxFrameSize: interpret method needs this amount for its
        // stack and doubleStack arrays
        builder.maxFrameSize = builder.maxVars + builder.maxLocals + builder.maxStack;

        builder.argNames = scriptOrFn.getParamAndVarNames();
        builder.constArgs = scriptOrFn.getParamAndVarConst();
        builder.argCount = scriptOrFn.getParamCount();
        builder.hasRestParams = scriptOrFn.hasRestParameter();
        builder.hasDefaultParams = scriptOrFn.getDefaultParams() != null;

        if (compilerEnv.isGeneratingSource()) {
            builder.sourceStart = scriptOrFn.getRawSourceStart();
            builder.sourceEnd = scriptOrFn.getRawSourceEnd();
        }

        builder.setLineNumberTable(lineNumberTable);

        // Dump instructions for debugging if enabled
        if (CompilerData.shouldDumpInstructions) {
            builder.build().dumpInstructions(System.err);
        }
    }

    private void generateNestedFunctions() {
        int functionCount = scriptOrFn.getFunctionCount();
        if (functionCount == 0) return;

        @SuppressWarnings("unchecked")
        CompilerData<JSFunction>[] array = new CompilerData[functionCount];
        for (int i = 0; i != functionCount; i++) {
            FunctionNode fn = scriptOrFn.getFunctionNode(i);
            var gen = new Compiler<JSFunction>();
            gen.compilerEnv = compilerEnv;
            gen.scriptOrFn = fn;
            gen.builder = new CompilerData.Builder<>(this.builder);
            gen.descBuilder = descBuilder.createChildBuilder();
            gen.descBuilder.code = gen.builder;
            CodeGenUtils.fillInForNestedFunction(gen.descBuilder, descBuilder, fn);
            CodeGenUtils.setConstructor(gen.descBuilder, fn);
            gen.generateFunctionCode();
            array[i] = gen.builder.build();

            // declaredAsFunctionExpression is now set by CodeGenUtils.fillInForNestedFunction
        }
        builder.nestedFunctions = array;
    }

    private void generateFunctionCode() {
        inFunction = true;

        FunctionNode theFunction = (FunctionNode) scriptOrFn;

        CodeGenUtils.setConstructor(descBuilder, theFunction);

        builder.setFunctionTypeFromInt(theFunction.getFunctionType());
        builder.needsActivation = theFunction.requiresActivation();
        builder.requiresArgumentsObject = theFunction.requiresArgumentObject();
        if (theFunction.getFunctionName() != null) {
            builder.name = theFunction.getName();
        }

        if (theFunction.isGenerator()) {
            // For generators with default parameters, generate parameter initialization
            // BEFORE Generator/ThawFrame so defaults are evaluated when f() is called rather
            // than deferred to iter.next(). Ref: ECMA 2026, 10.2.11
            // FunctionDeclarationInstantiation.
            Node paramInitBlock = theFunction.getGeneratorParamInitBlock();
            if (paramInitBlock != null) {
                Node paramInit = paramInitBlock.getFirstChild();
                while (paramInit != null) {
                    generateStatement(paramInit, 0);
                    paramInit = paramInit.getNext();
                }
            }
            // For generators, nested function declarations must be instantiated AFTER
            // parameter initialization to prevent them from shadowing the 'arguments'
            // object during default-parameter evaluation. CallFrameV2 skips its
            // frame-init hoist for generators.
            int functionCount = theFunction.getFunctionCount();
            for (int i = 0; i < functionCount; i++) {
                FunctionNode fn = theFunction.getFunctionNode(i);
                if (fn.getFunctionType() == FunctionNode.FUNCTION_STATEMENT) {
                    addInstruction(new ClosureStatement(i));
                }
            }
            addInstruction(new Generator((short) theFunction.getBaseLineno()));
            addInstruction(new ThawFrame(false, (short) theFunction.getBaseLineno()));
        }
        if (theFunction.isInStrictMode()) {
            builder.isStrict = true;
        }
        if (theFunction.isES6Generator()) {
            builder.isES6Generator = true;
        }
        if (theFunction.isShorthand()) {
            builder.isShorthand = true;
        }

        generateInstructions(theFunction.getLastChild());
    }

    private Object generateRegExpLiteral(int i) {
        Context cx = Context.getCurrentContext();
        RegExpProxy rep = ScriptRuntime.checkRegExpProxy(cx);
        String string = scriptOrFn.getRegexpString(i);
        String flags = scriptOrFn.getRegexpFlags(i);
        return rep.compileRegExp(cx, string, flags);
    }

    private Object generateTemplateLiteral(int i) {
        List<TemplateCharacters> strings = scriptOrFn.getTemplateLiteralStrings(i);
        int j = 0;
        String[] values = new String[strings.size() * 2];
        for (TemplateCharacters s : strings) {
            values[j++] = s.getValue();
            values[j++] = s.getRawValue();
        }
        return values;
    }

    private void generateStatement(Node node, int initialStackDepth) {
        var op = node.getType();
        var child = node.getFirstChild();
        switch (op) {
            case Token.FUNCTION:
                {
                    updateLineNumber(node);
                    int fnIndex = node.getExistingIntProp(Node.FUNCTION_PROP);
                    int fnType = scriptOrFn.getFunctionNode(fnIndex).getFunctionType();
                    // Only function expressions or function expression statements need closure code
                    // creating new function object on stack as function statements are initialized
                    // at script/function start. In addition, function expressions can not be
                    // present here at statement level, they must only be present as expressions.

                    if (fnType == FunctionNode.FUNCTION_EXPRESSION_STATEMENT) {
                        addInstruction(new ClosureStatement(fnIndex));
                    } else {
                        if (fnType != FunctionNode.FUNCTION_STATEMENT) {
                            throw Kit.codeBug();
                        }
                    }
                    // For function statements or function expression statements in scripts, we need
                    // to ensure that the result of the script is the function if it is the last
                    // statement in the script.
                    // For example, eval("function () {}") should return a function, not undefined.

                    // SNC: This breaks the old behavior of
                    // eval("foo(); function foo() { return 1; }");
                    // We'd expect 1, but we get the foo function back from eval. The use case
                    // described in the comment above is non-standard in the first place (JavaScript
                    // should require parens around the anonymous function to turn the definition
                    // into an expression), so we just won't support this use case. In addition,
                    // compiled mode gives us 1 as expected, so this is a deviation from the
                    // behavior in compiled mode. We want consistent results between both modes.
                    /*
                    if (!itsInFunctionFlag) {
                        addIndexOp(Icode_CLOSURE_EXPR, fnIndex);
                        addIcode(Icode_POP_RESULT);
                    }
                    */
                    return;
                }
            case Token.LABEL:
            case Token.BLOCK:
                {
                    updateLineNumber(node);
                    // No Nop for BLOCK/LABEL - child instructions will carry the line number.
                    // When multiple lines are recorded at the same PC, the LineNumberTable
                    // returns the last recorded line (most specific from children).
                    while (child != null) {
                        generateStatement(child, initialStackDepth);
                        child = child.getNext();
                    }
                    return;
                }
            case Token.LOOP:
            case Token.EMPTY:
            case Token.WITH:
                {
                    updateLineNumber(node);
                    instructions.add(Nop.instance);
                }
            // fall through
            case Token.SCRIPT:
                {
                    while (child != null) {
                        generateStatement(child, initialStackDepth);
                        child = child.getNext();
                    }
                    return;
                }
            case Token.ENTERWITH:
                {
                    var obj = getOperand(child, 0);
                    addInstruction(new EnterWith(obj));
                    return;
                }
            case Token.LEAVEWITH:
                {
                    addInstruction(LeaveWith.instance);
                    return;
                }
            case Token.LOCAL_BLOCK:
                {
                    int local = allocLocal();
                    node.putIntProp(Node.LOCAL_PROP, local);
                    updateLineNumber(node);
                    while (child != null) {
                        generateStatement(child, initialStackDepth);
                        child = child.getNext();
                    }
                    addInstruction(new LocalClear(local));
                    releaseLocal(local);
                    return;
                }
            case Token.DEBUGGER:
                {
                    addInstruction(DebuggerInstruction.instance);
                    return;
                }
            case Token.SWITCH:
                {
                    updateLineNumber(node);
                    var testOperands = new ArrayList<Operand>();
                    var targets = new ArrayList<Node>();
                    var shouldUseSimpleSwitch = true;

                    // See comments in IRFactory.createSwitch() for description of SWITCH node
                    var valueOperand = getOperand(child, 0, true);
                    for (Jump caseNode = (Jump) child.getNext();
                            caseNode != null;
                            caseNode = (Jump) caseNode.getNext()) {
                        if (caseNode.getType() != Token.CASE) {
                            throw badTree(caseNode);
                        }

                        updateLineNumber(caseNode);

                        Node test = caseNode.getFirstChild();
                        var lastInstruction = instructions.size();
                        var testOperand = getOperand(test, 0);
                        var target = caseNode.target;
                        if (!shouldUseSimpleSwitch || !testOperand.isValidJumpTableKey()) {
                            shouldUseSimpleSwitch = false;
                            for (int i = 0; i < targets.size(); i++) {
                                addGoto(
                                        targets.get(i),
                                        new IfEqPop(valueOperand, testOperands.get(i)),
                                        lastInstruction);
                                lastInstruction++;
                            }
                            targets.clear();
                            testOperands.clear();

                            addGoto(target, new IfEqPop(valueOperand, testOperand));
                        } else {
                            testOperands.add(testOperand);
                            targets.add(target);
                        }
                    }

                    if (shouldUseSimpleSwitch && !testOperands.isEmpty()) {
                        for (var target : targets) {
                            addGoto(target);
                        }

                        var next = node.getNext();
                        assert next != null && next.getType() == Token.GOTO;

                        var target = ((Jump) next).target;
                        addGoto(target, new SimpleSwitch(valueOperand, testOperands));
                    } else {
                        addInstruction(new Cleanup(valueOperand));
                    }
                    return;
                }
            case Token.TARGET:
                {
                    markTargetLabel(node);
                    return;
                }
            case Token.IFEQ:
                {
                    var target = ((Jump) node).target;
                    var test = getOperand(child, 0);
                    addGoto(target, new IfEq(test));
                    return;
                }
            case Token.IFNE:
                {
                    var target = ((Jump) node).target;
                    var test = getOperand(child, 0);
                    addGoto(target, new IfNe(test));
                    return;
                }
            case Token.GOTO:
                {
                    if (!instructions.isEmpty()
                            && instructions.get(instructions.size() - 1) instanceof SimpleSwitch) {
                        return;
                    }

                    var target = ((Jump) node).target;
                    addGoto(target, new Goto());
                    return;
                }
            case Token.JSR:
                {
                    Node target = ((Jump) node).target;
                    addGoto(target, new GoSubroutine());
                    return;
                }
            case Token.FINALLY:
                {
                    int finallyRegister = getLocalBlockRef(node);
                    addInstruction(new StartSubroutine(finallyRegister, PopOperand.instance));
                    while (child != null) {
                        generateStatement(child, initialStackDepth);
                        child = child.getNext();
                    }
                    addInstruction(new ReturnSubroutine(finallyRegister));
                    return;
                }
            case Token.EXPR_VOID:
                {
                    updateLineNumber(node);
                    var obj = getOperand(child, 0);
                    addInstruction(new VoidInstruction(obj));
                    return;
                }
            case Token.EXPR_RESULT:
                {
                    updateLineNumber(node);
                    visitExpression(child, 0);
                    addInstruction(PopResult.instance);
                    return;
                }
            case Token.TRY:
                {
                    Jump tryNode = (Jump) node;
                    int exceptionObjectLocal = getLocalBlockRef(tryNode);
                    int scopeLocal = allocLocal();

                    addInstruction(new SaveScope(scopeLocal));

                    int tryStart = this.instructions.size();
                    boolean savedFlag = inTryFlag;
                    inTryFlag = true;
                    while (child != null) {
                        generateStatement(child, initialStackDepth);
                        child = child.getNext();
                    }
                    inTryFlag = savedFlag;

                    Node catchTarget = tryNode.target;
                    if (catchTarget != null) {
                        int catchStartPC = labelTable[getTargetLabel(catchTarget)];
                        addExceptionHandler(
                                tryStart,
                                catchStartPC,
                                catchStartPC,
                                false,
                                exceptionObjectLocal,
                                scopeLocal);
                    }
                    Node finallyTarget = tryNode.getFinally();
                    if (finallyTarget != null) {
                        int finallyStartPC = labelTable[getTargetLabel(finallyTarget)];
                        addExceptionHandler(
                                tryStart,
                                finallyStartPC,
                                finallyStartPC,
                                true,
                                exceptionObjectLocal,
                                scopeLocal);
                    }

                    addInstruction(new LocalClear(scopeLocal));
                    releaseLocal(scopeLocal);
                    return;
                }
            case Token.CATCH_SCOPE:
                {
                    int localIndex = getLocalBlockRef(node);
                    int scopeIndex = node.getExistingIntProp(Node.CATCH_SCOPE_PROP);
                    String name = child.getType() == Token.NAME ? child.getString() : "";
                    child = child.getNext();
                    var exception = getOperand(child, 0); // load expression object
                    addInstruction(new CatchScope(exception, name, localIndex, scopeIndex));
                    return;
                }
            case Token.THROW:
                {
                    updateLineNumber(node);
                    var value = getOperand(child, 0);
                    addInstruction(new Throw(value, (short) lineNumber));
                    return;
                }
            case Token.RETHROW:
                {
                    updateLineNumber(node);
                    addInstruction(new Rethrow(getLocalBlockRef(node)));
                    return;
                }
            case Token.RETURN:
                {
                    updateLineNumber(node);
                    if (node.getIntProp(Node.GENERATOR_END_PROP, 0) != 0) {
                        if ((child == null)
                                || (compilerEnv.getLanguageVersion() < Context.VERSION_ES6)) {
                            // End generator function with no result, or old language version
                            // in which generators never return a result.
                            addInstruction(new GeneratorEnd((short) lineNumber));
                        } else {
                            var value = getOperand(child, ECF_TAIL);
                            addInstruction(new GeneratorReturn((short) lineNumber, value));
                        }

                    } else {
                        if (child == null) {
                            addInstruction(ReturnUndefined.instance);
                        } else {
                            visitExpression(child, ECF_TAIL);
                            addInstruction(Return.instance);
                        }
                    }
                    return;
                }
            case Token.RETURN_RESULT:
                {
                    updateLineNumber(node);
                    addInstruction(ReturnResult.instance);
                    return;
                }
            case Token.ENUM_INIT_KEYS:
                {
                    var obj = getOperand(child, 0);
                    addInstruction(new EnumInitKeys(obj, getLocalBlockRef(node)));
                    return;
                }
            case Token.ENUM_INIT_VALUES:
                {
                    var obj = getOperand(child, 0);
                    addInstruction(new EnumInitValues(obj, getLocalBlockRef(node)));
                    return;
                }
            case Token.ENUM_INIT_ARRAY:
                {
                    var obj = getOperand(child, 0);
                    addInstruction(new EnumInitArray(obj, getLocalBlockRef(node)));
                    return;
                }
            case Token.ENUM_INIT_VALUES_IN_ORDER:
                {
                    var obj = getOperand(child, 0);
                    addInstruction(new EnumInitValuesInOrder(obj, getLocalBlockRef(node)));
                    return;
                }

            default:
                throw badTree(node);
        }
    }

    private void addExceptionHandler(
            int tryStart,
            int tryEnd,
            int handlerStart,
            boolean isFinally,
            int exceptionObjectLocal,
            int scopeLocal) {
        int top = exceptionTableTop;
        int[] table = builder.exceptionTable;
        if (table == null) {
            if (top != 0) Kit.codeBug();
            table = new int[CompilerData.EXCEPTION_SLOT_SIZE * 2];
            builder.exceptionTable = table;
        } else if (table.length == top) {
            table = new int[table.length * 2];
            System.arraycopy(builder.exceptionTable, 0, table, 0, top);
            builder.exceptionTable = table;
        }
        table[top + CompilerData.EXCEPTION_TRY_START_SLOT] = tryStart;
        table[top + CompilerData.EXCEPTION_TRY_END_SLOT] = tryEnd;
        table[top + CompilerData.EXCEPTION_HANDLER_SLOT] = handlerStart;
        table[top + CompilerData.EXCEPTION_TYPE_SLOT] = isFinally ? 1 : 0;
        table[top + CompilerData.EXCEPTION_LOCAL_SLOT] = exceptionObjectLocal;
        table[top + CompilerData.EXCEPTION_SCOPE_SLOT] = scopeLocal;

        exceptionTableTop = top + CompilerData.EXCEPTION_SLOT_SIZE;

        // Track exception handler as a jump target
        jumpTargets.add(handlerStart);
    }

    private void visitExpression(Node node, int contextFlags) {
        updateLineNumber(node);
        int op = node.getType();
        var child = node.getFirstChild();
        int savedStackDepth = stackDepth;
        switch (op) {
            case Token.FUNCTION:
                {
                    int fnIndex = node.getExistingIntProp(Node.FUNCTION_PROP);
                    FunctionNode fn = scriptOrFn.getFunctionNode(fnIndex);
                    // See comments in visitStatement for Token.FUNCTION case
                    if (fn.getFunctionType() != FunctionNode.FUNCTION_EXPRESSION
                            && fn.getFunctionType() != FunctionNode.ARROW_FUNCTION) {
                        throw Kit.codeBug();
                    }
                    addInstruction(new ClosureExpression(fnIndex));
                    if (fn.isMethodDefinition()) {
                        throw Kit.codeBug();
                    }
                    return;
                }
            case Token.LOCAL_LOAD:
                {
                    int localIndex = getLocalBlockRef(node);
                    addInstruction(new LocalLoad(localIndex));
                    return;
                }
            case Token.COMMA:
                {
                    var lastChild = node.getLastChild();
                    while (child != lastChild) {
                        var obj = getOperand(child, 0);
                        addInstruction(new VoidInstruction(obj));
                        child = child.getNext();
                    }
                    // Preserve tail context flag if any
                    visitExpression(child, contextFlags & ECF_TAIL);
                    return;
                }
            case Token.USE_STACK:
                {
                    return;
                }
            case Token.OBJECT_REST:
                {
                    Object[] excludedKeys = (Object[]) node.getProp(Node.OBJECT_IDS_PROP);
                    if (excludedKeys == null) {
                        excludedKeys = new Object[0];
                    }
                    int computedCount = 0;
                    int staticCount = 0;
                    for (Object key : excludedKeys) {
                        if (key instanceof Node) {
                            computedCount++;
                        } else {
                            staticCount++;
                        }
                    }
                    Object[] staticKeys = new Object[staticCount];
                    int si = 0;
                    for (Object key : excludedKeys) {
                        if (!(key instanceof Node)) {
                            staticKeys[si++] = key;
                        }
                    }

                    var sourceOperand = getOperand(child, 0);
                    Operand[] computedKeys = new Operand[computedCount];
                    int ci = 0;
                    for (Object key : excludedKeys) {
                        if (key instanceof Node) {
                            computedKeys[ci++] = getOperand((Node) key, 0);
                        }
                    }
                    addInstruction(new ObjectRest(sourceOperand, staticKeys, computedKeys));
                    return;
                }
            case Token.REF_CALL:
            case Token.CALL:
            case Token.NEW:
                {
                    boolean isOptionalChainingCall =
                            node.getIntProp(Node.OPTIONAL_CHAINING, 0) == 1;
                    CompleteOptionalCallJump completeOptionalCallJump = null;
                    // We get a function for news, and lookup result for normal calls
                    Operand lookupResultOrFunction;
                    var lines = new ArrayList<Integer>();
                    if (op == Token.NEW) {
                        lookupResultOrFunction = getOperand(child, 0, false, lines);
                    } else {
                        completeOptionalCallJump =
                                generateCallFunAndThis(child, isOptionalChainingCall);
                        if (completeOptionalCallJump != null) {
                            resolveForwardGoto(completeOptionalCallJump.putArgsAndDoCallLabel);
                        }
                        lookupResultOrFunction = PopOperand.instance;
                    }
                    List<Operand> args = new ArrayList<>();
                    while ((child = child.getNext()) != null) {
                        args.add(getOperand(child, 0, false, lines));
                    }
                    int callType = node.getIntProp(Node.SPECIALCALL_PROP, Node.NON_SPECIALCALL);
                    updateLineNumber(node);
                    updateLineNumbers(lines);
                    if (op != Token.REF_CALL && callType != Node.NON_SPECIALCALL) {
                        if (op == Token.NEW) {
                            addInstruction(
                                    new SpecialCallNew(
                                            lookupResultOrFunction,
                                            args.toArray(Operand.EMPTY_ARRAY),
                                            callType));
                        } else {
                            addInstruction(
                                    new SpecialCall(
                                            lookupResultOrFunction,
                                            args.toArray(Operand.EMPTY_ARRAY),
                                            (short) (lineNumber & 0xFF),
                                            callType));
                        }
                    } else if (node.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
                        addInstruction(
                                new Call(
                                        lookupResultOrFunction,
                                        args.toArray(Operand.EMPTY_ARRAY),
                                        Call.Type.CallOnSuper));
                    } else {
                        // Only use the tail call optimization if we're not in a try
                        // or we're not generating debug info (since the
                        // optimization will confuse the debugger)
                        Call.Type type = Call.Type.Call;
                        if (op == Token.CALL
                                && (contextFlags & ECF_TAIL) != 0
                                && !compilerEnv.isGenerateDebugInfo()
                                && !inTryFlag) {
                            type = Call.Type.TailCall;
                        } else if (op == Token.REF_CALL) {
                            type = Call.Type.RefCall;
                        }

                        if (op == Token.NEW) {
                            addInstruction(
                                    new New(
                                            lookupResultOrFunction,
                                            args.toArray(Operand.EMPTY_ARRAY)));
                        } else {
                            addInstruction(
                                    new Call(
                                            lookupResultOrFunction,
                                            args.toArray(Operand.EMPTY_ARRAY),
                                            type));
                        }
                    }

                    if (completeOptionalCallJump != null) {
                        resolveForwardGoto(completeOptionalCallJump.afterLabel);
                    }
                    return;
                }
            case Token.AND:
            case Token.OR:
                {
                    visitExpression(child, 0);
                    child = child.getNext();

                    int end = instructions.size();
                    JumpInstruction jump =
                            (op == Token.AND)
                                    ? new IfNe(PeekOperand.instance)
                                    : new IfEq(PeekOperand.instance);
                    addInstruction(jump);

                    addInstruction(Pop.instance);

                    // Preserve tail context flag if any
                    visitExpression(child, contextFlags & ECF_TAIL);

                    resolveForwardGoto(end);
                    return;
                }
            case Token.HOOK:
                {
                    Node ifThen = child.getNext();
                    Node ifElse = ifThen.getNext();
                    var test = getOperand(child, 0);
                    int elseJumpStart = instructions.size();
                    addInstruction(new IfNe(test));

                    // Preserve tail context flag if any
                    visitExpression(ifThen, contextFlags & ECF_TAIL);
                    int afterElseJumpStart = instructions.size();
                    addInstruction(new Goto());
                    resolveForwardGoto(elseJumpStart);
                    stackDepth = savedStackDepth;
                    // Preserve tail context flag if any
                    visitExpression(ifElse, contextFlags & ECF_TAIL);
                    resolveForwardGoto(afterElseJumpStart);
                    return;
                }
            case Token.GETPROP:
            case Token.GETPROPNOWARN:
                {
                    var firstChild = child;
                    child = child.getNext();
                    if (node.getIntProp(Node.OPTIONAL_CHAINING, 0) == 1) {
                        var lhs = getOperand(firstChild, 0, true);
                        // Jump if null or undefined
                        int putUndefinedLabel = instructions.size();
                        addInstruction(new IfNullUndefined(lhs));

                        // Access property
                        addInstruction(
                                new GetProp(
                                        lhs.convertToConsume(),
                                        child.getString(),
                                        op == Token.GETPROPNOWARN));
                        int afterLabel = instructions.size();
                        addInstruction(new Goto());

                        // Put undefined
                        resolveForwardGoto(putUndefinedLabel);
                        popIfPeek(lhs);
                        addInstruction(new Name("undefined"));
                        resolveForwardGoto(afterLabel);
                    } else {
                        var lhs = getOperand(firstChild, 0);
                        if (node.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
                            addInstruction(
                                    new GetPropSuper(
                                            lhs, child.getString(), op == Token.GETPROPNOWARN));
                        } else {
                            addInstruction(
                                    new GetProp(lhs, child.getString(), op == Token.GETPROPNOWARN));
                        }
                    }
                    return;
                }
            case Token.DELPROP:
                {
                    boolean isName = child.getType() == Token.BINDNAME;
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    if (node.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
                        addInstruction(new DelPropSuper(lhs, rhs));
                    } else if (isName) {
                        // special handling for delete name
                        addInstruction(new DelName(lhs, rhs));
                    } else {
                        addInstruction(new DelProp(lhs, rhs));
                    }
                    return;
                }
            case Token.GETELEM:
                {
                    var firstChild = child;
                    child = child.getNext();
                    if (node.getIntProp(Node.OPTIONAL_CHAINING, 0) == 1) {
                        var lhs = getOperand(firstChild, 0, true);
                        int putUndefinedLabel = instructions.size();
                        addInstruction(new IfNullUndefined(lhs));

                        // Infix op
                        finishGetElemGeneration(child, lhs.convertToConsume());
                        int afterLabel = instructions.size();
                        addInstruction(new Goto());

                        // Put undefined
                        resolveForwardGoto(putUndefinedLabel);
                        popIfPeek(lhs);
                        addInstruction(new Name("undefined"));
                        resolveForwardGoto(afterLabel);
                    } else {
                        var lhs = getOperand(firstChild, 0);
                        if (node.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
                            var elem = getOperand(child, 0);
                            addInstruction(new GetElemSuper(lhs, elem));
                        } else {
                            finishGetElemGeneration(child, lhs);
                        }
                    }
                    return;
                }
            case Token.BITAND:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new BitAnd(lhs, rhs));
                    return;
                }
            case Token.BITOR:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new BitOr(lhs, rhs));
                    return;
                }
            case Token.BITXOR:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new BitXor(lhs, rhs));
                    return;
                }
            case Token.LSH:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new LeftShift(lhs, rhs));
                    return;
                }
            case Token.RSH:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new RightShift(lhs, rhs));
                    return;
                }
            case Token.URSH:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new UnsignedRightShift(lhs, rhs));
                    return;
                }
            case Token.ADD:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new Add(lhs, rhs));
                    return;
                }
            case Token.STRING_CONCAT:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new StringConcat(lhs, rhs));
                    return;
                }
            case Token.SUB:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new Subtract(lhs, rhs));
                    return;
                }
            case Token.MOD:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new Mod(lhs, rhs));
                    return;
                }
            case Token.DIV:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new Divide(lhs, rhs));
                    return;
                }
            case Token.MUL:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new Multiply(lhs, rhs));
                    return;
                }
            case Token.EXP:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new Exponentiate(lhs, rhs));
                    return;
                }
            case Token.EQ:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new Equal(lhs, rhs));
                    return;
                }
            case Token.NE:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new NotEqual(lhs, rhs));
                    return;
                }
            case Token.SHEQ:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new ShallowEqual(lhs, rhs));
                    return;
                }
            case Token.SHNE:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new ShallowNotEqual(lhs, rhs));
                    return;
                }
            case Token.IN:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new In(lhs, rhs));
                    return;
                }
            case Token.INSTANCEOF:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new Instanceof(lhs, rhs));
                    return;
                }
            case Token.LE:
            case Token.LT:
            case Token.GE:
            case Token.GT:
                {
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new Comparison(lhs, op, rhs));
                    return;
                }
            case Token.POS:
                {
                    var obj = getOperand(child, 0);
                    addInstruction(new Pos(obj));
                    return;
                }
            case Token.NEG:
                {
                    var obj = getOperand(child, 0);
                    addInstruction(new Neg(obj));
                    return;
                }
            case Token.NOT:
                {
                    var obj = getOperand(child, 0);
                    addInstruction(new Not(obj));
                    return;
                }
            case Token.BITNOT:
                {
                    var obj = getOperand(child, 0);
                    addInstruction(new BitNot(obj));
                    return;
                }
            case Token.TYPEOF:
                {
                    var obj = getOperand(child, 0);
                    addInstruction(new Typeof(obj));
                    return;
                }
            case Token.VOID:
                {
                    visitExpression(child, 0);
                    addInstruction(Pop.instance);
                    addInstruction(new PushConstant(Undefined.instance));
                    return;
                }
            case Token.GET_REF:
            case Token.DEL_REF:
                {
                    if (node.getIntProp(Node.OPTIONAL_CHAINING, 0) == 1) {
                        var lhs = getOperand(child, 0, true);
                        // On the stack we'll have either the Ref or undefined

                        // If it's null or undefined, just jump ahead
                        int afterLabel = instructions.size();
                        addInstruction(new IfNullUndefined(lhs));

                        // Otherwise do the GET_REF
                        addInstruction(
                                op == Token.GET_REF
                                        ? new GetRef(lhs.convertToConsume())
                                        : new DelRef(lhs.convertToConsume()));

                        resolveForwardGoto(afterLabel);
                    } else {
                        var lhs = getOperand(child, 0);
                        addInstruction(op == Token.GET_REF ? new GetRef(lhs) : new DelRef(lhs));
                    }
                    return;
                }
            case Token.SETPROP:
            case Token.SETPROP_OP:
                {
                    var lhs = getOperand(child, 0, true);
                    child = child.getNext();
                    String property = child.getString();
                    child = child.getNext();
                    if (op == Token.SETPROP_OP) {
                        addInstruction(new GetProp(lhs, property, false));
                    }
                    var rhs = getOperand(child, 0);
                    addInstruction(
                            node.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1
                                    ? new SetPropSuper(lhs.convertToConsume(), property, rhs)
                                    : new SetProp(lhs.convertToConsume(), property, rhs));
                    return;
                }
            case Token.SETELEM:
            case Token.SETELEM_OP:
                {
                    Operand lhs, elem;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, true);
                        child = child.getNext();
                        elem = getSafeOperand(child, 0, true);
                    } else {
                        lhs = getOperand(child, 0, true);
                        child = child.getNext();
                        elem = getOperand(child, 0, true);
                    }
                    child = child.getNext();
                    if (op == Token.SETELEM_OP) {
                        if (lhs instanceof PeekOperand && elem instanceof PeekOperand) {
                            lhs = new PeekOperand(-1);
                        }

                        addInstruction(new GetElem(lhs, elem));
                    }
                    var rhs = getOperand(child, 0);
                    addInstruction(
                            node.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1
                                    ? new SetElemSuper(
                                            lhs.convertToConsume(), elem.convertToConsume(), rhs)
                                    : new SetElem(
                                            lhs.convertToConsume(), elem.convertToConsume(), rhs));
                    return;
                }
            case Token.SET_REF:
            case Token.SET_REF_OP:
                {
                    var obj = getOperand(child, 0, true);
                    child = child.getNext();
                    if (op == Token.SET_REF_OP) {
                        addInstruction(new GetRef(obj));
                    }
                    var rhs = getOperand(child, 0);
                    addInstruction(new SetRef(obj.convertToConsume(), rhs));
                    return;
                }
            case Token.SETNAME:
                {
                    String name = child.getString();
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new SetName(lhs, name, rhs));
                    return;
                }
            case Token.STRICT_SETNAME:
                {
                    String name = child.getString();
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new StrictSetName(lhs, name, rhs));
                    return;
                }
            case Token.SETCONST:
                {
                    String name = child.getString();
                    Operand lhs, rhs;
                    if (isSafeOperand(child) && isSafeOperand(child.getNext())) {
                        lhs = getSafeOperand(child, 0, false);
                        child = child.getNext();
                        rhs = getSafeOperand(child, 0, false);
                    } else {
                        lhs = getOperand(child, 0);
                        child = child.getNext();
                        rhs = getOperand(child, 0);
                    }
                    addInstruction(new SetConst(lhs, name, rhs));
                    return;
                }
            case Token.TYPEOFNAME:
                {
                    int index = -1;
                    // use typeofname if an activation frame exists
                    // since the vars all exist there instead of in jregs
                    if (inFunction && !builder.needsActivation)
                        index = scriptOrFn.getIndexForNameNode(node);
                    if (index == -1) {
                        addInstruction(new TypeofName(node.getString()));
                    } else {
                        addInstruction(new Typeof(GetVarOperand.createOperand(index)));
                    }
                    return;
                }
            case Token.BINDNAME:
                {
                    addInstruction(new BindName(node.getString()));
                    return;
                }
            case Token.NAME:
                {
                    updateLineNumber(node);
                    addInstruction(new Name(node.getString()));
                    return;
                }
            case Token.STRING:
                {
                    addInstruction(new PushConstant(node.getString()));
                    return;
                }
            case Token.INC:
            case Token.DEC:
                {
                    visitIncDec(node, node.getFirstChild());
                    return;
                }
            case Token.NUMBER:
                {
                    double num = node.getDouble();
                    int inum = (int) num;

                    // SNC Change
                    if (shouldTreatNumberAsInteger(num)) {
                        if (inum == 0) {
                            // Check for negative zero
                            if (1.0 / num < 0.0) {
                                addInstruction(new PushConstant(ScriptRuntime.negativeZeroObj));
                            } else {
                                if (treatNumericLiteralsLikeOldRhino) {
                                    addInstruction(new PushConstant(0));
                                } else {
                                    addInstruction(new PushConstant(Integer.valueOf(0)));
                                }
                            }
                        } else if (inum == 1) {
                            if (treatNumericLiteralsLikeOldRhino) {
                                addInstruction(new PushConstant(1));
                            } else {
                                addInstruction(new PushConstant(Integer.valueOf(1)));
                            }
                        } else if ((short) inum == inum) {
                            addInstruction(new ShortNumber((short) (inum & 0xFFFF)));
                        } else {
                            addInstruction(new Int(inum));
                        }
                    } else {
                        addInstruction(new Num(num));
                    }
                    return;
                }
            case Token.GETVAR:
                {
                    if (builder.needsActivation) {
                        Kit.codeBug();
                    }
                    int index = scriptOrFn.getIndexForNameNode(node);
                    addInstruction(new GetVar(index));
                    return;
                }
            case Token.SETVAR:
                {
                    if (builder.needsActivation) {
                        Kit.codeBug();
                    }
                    int index = scriptOrFn.getIndexForNameNode(child);
                    child = child.getNext();
                    var value = getOperand(child, 0);
                    addInstruction(new SetVar(index, value));
                    return;
                }
            case Token.SETCONSTVAR:
                {
                    if (builder.needsActivation) {
                        Kit.codeBug();
                    }
                    int index = scriptOrFn.getIndexForNameNode(child);
                    child = child.getNext();
                    var value = getOperand(child, 0);
                    addInstruction(new SetConstVar(index, value));
                    return;
                }
            case Token.NULL:
                {
                    addInstruction(new PushConstant(null));
                    return;
                }
            case Token.UNDEFINED:
                {
                    addInstruction(new PushConstant(Undefined.instance));
                    return;
                }
            case Token.THIS:
                {
                    addInstruction(This.instance);
                    return;
                }
            case Token.SUPER:
                {
                    addInstruction(Super.instance);
                    return;
                }
            case Token.THISFN:
                {
                    addInstruction(ThisFunction.instance);
                    return;
                }
            case Token.FALSE:
                {
                    addInstruction(new PushConstant(Boolean.FALSE));
                    return;
                }
            case Token.TRUE:
                {
                    addInstruction(new PushConstant(Boolean.TRUE));
                    return;
                }
            case Token.ENUM_NEXT:
                {
                    addInstruction(new EnumNext(getLocalBlockRef(node)));
                    return;
                }
            case Token.ENUM_ID:
                {
                    addInstruction(new EnumId(getLocalBlockRef(node)));
                    return;
                }
            case Token.BIGINT:
                {
                    addInstruction(new BigInt(node.getBigInt()));
                    return;
                }
            case Token.REGEXP:
                {
                    int index = node.getExistingIntProp(Node.REGEXP_PROP);
                    addInstruction(new Regexp(generateRegExpLiteral(index)));
                    return;
                }
            case Token.ARRAYLIT:
                {
                    updateLineNumber(node);

                    int[] skipIndices = (int[]) node.getProp(Node.SKIP_INDEXES_PROP);
                    int numberOfSpread = node.getIntProp(Node.NUMBER_OF_SPREAD, 0);

                    if (numberOfSpread > 0) {
                        var elements = new ArrayList<Operand>();
                        var spreadFlags = new ArrayList<Boolean>();
                        while (child != null) {
                            updateLineNumber(child);
                            if (child.getType() == Token.DOTDOTDOT) {
                                elements.add(getOperand(child.getFirstChild(), 0));
                                spreadFlags.add(Boolean.TRUE);
                            } else {
                                elements.add(getOperand(child, 0));
                                spreadFlags.add(Boolean.FALSE);
                            }
                            child = child.getNext();
                        }

                        int count = elements.size();
                        boolean[] isSpread = new boolean[count];
                        for (int i = 0; i < count; i++) {
                            isSpread[i] = spreadFlags.get(i);
                        }

                        int[] sourcePositions = null;
                        if (skipIndices != null) {
                            sourcePositions = new int[count];
                            int sourcePos = 0;
                            int skipIdx = 0;
                            for (int i = 0; i < count; i++) {
                                while (skipIdx < skipIndices.length
                                        && skipIndices[skipIdx] == sourcePos) {
                                    sourcePos++;
                                    skipIdx++;
                                }
                                sourcePositions[i] = sourcePos;
                                sourcePos++;
                            }
                        }

                        int nonSpreadCount = count - numberOfSpread;
                        addInstruction(
                                new ArrayLitWithSpread(
                                        elements.toArray(Operand.EMPTY_ARRAY),
                                        isSpread,
                                        skipIndices,
                                        sourcePositions,
                                        nonSpreadCount));
                        return;
                    }

                    var elements = new ArrayList<Operand>();
                    while (child != null) {
                        updateLineNumber(child);
                        elements.add(getOperand(child, 0));
                        child = child.getNext();
                    }

                    addInstruction(
                            new ArrayLit(elements.toArray(Operand.EMPTY_ARRAY), skipIndices));
                    return;
                }
            case Token.OBJECTLIT:
                {
                    Object[] propertyIds = (Object[]) node.getProp(Node.OBJECT_IDS_PROP);
                    int count = propertyIds == null ? 0 : propertyIds.length;
                    int numberOfSpread = node.getIntProp(Node.NUMBER_OF_SPREAD, 0);
                    int nonSpreadCount = count - numberOfSpread;
                    boolean hasSpread = numberOfSpread > 0;
                    boolean hasAnyComputedProperty =
                            propertyIds != null
                                    && Arrays.stream(propertyIds)
                                            .anyMatch(
                                                    id ->
                                                            id instanceof Node
                                                                    && ((Node) id).getType()
                                                                            != Token.DOTDOTDOT);

                    // dedup not supported in open-source

                    updateLineNumber(node);

                    if (hasSpread) {
                        emitObjectLiteralWithSpread(child, propertyIds, count, nonSpreadCount);
                    } else {
                        emitObjectLiteralNoSpread(
                                child, propertyIds, count, hasAnyComputedProperty);
                    }
                    return;
                }
            case Token.ARRAYCOMP:
                {
                    // A bit of a hack: array comprehensions are implemented using
                    // statement nodes for the iteration, yet they appear in an
                    // expression context. So we pass the current stack depth to
                    // visitStatement so it can check that the depth is not altered
                    // by statements.
                    var next = child.getNext();
                    generateStatement(child, stackDepth);
                    visitExpression(next, 0);
                    return;
                }
            case Token.REF_SPECIAL:
                {
                    if (node.getIntProp(Node.OPTIONAL_CHAINING, 0) == 1) {
                        var lhs = getOperand(child, 0, true);
                        // Jump if null or undefined
                        int putUndefinedLabel = instructions.size();
                        addInstruction(new IfNullUndefined(lhs));

                        // Access property
                        addInstruction(
                                new RefSpecial(
                                        lhs.convertToConsume(),
                                        (String) node.getProp(Node.NAME_PROP)));
                        int afterLabel = instructions.size();
                        addInstruction(new Goto());

                        // Put undefined
                        resolveForwardGoto(putUndefinedLabel);
                        popIfPeek(lhs);
                        addInstruction(new Name("undefined"));
                        resolveForwardGoto(afterLabel);
                    } else {
                        var lhs = getOperand(child, 0);
                        addInstruction(new RefSpecial(lhs, (String) node.getProp(Node.NAME_PROP)));
                    }
                    return;
                }
            case Token.WITHEXPR:
                {
                    Node enterWith = node.getFirstChild();
                    Node with = enterWith.getNext();
                    var enterObj = getOperand(enterWith.getFirstChild(), 0);
                    addInstruction(new EnterWith(enterObj));
                    visitExpression(with.getFirstChild(), 0);
                    addInstruction(LeaveWith.instance);
                    return;
                }
            case Token.TEMPLATE_LITERAL:
                {
                    int index = node.getExistingIntProp(Node.TEMPLATE_LITERAL_PROP);
                    addInstruction(new TemplateLiteralCallsite(generateTemplateLiteral(index)));
                    return;
                }
            case Token.NULLISH_COALESCING:
                {
                    visitExpression(child, 0);
                    child = child.getNext();

                    int end = instructions.size();
                    addInstruction(new IfNotNullUndefined(PeekOperand.instance));

                    addInstruction(Pop.instance);
                    visitExpression(child, 0);
                    resolveForwardGoto(end);
                    return;
                }
            case Token.YIELD:
                {
                    Operand valueOperand;
                    if (child != null) {
                        valueOperand = getOperand(child, 0);
                    } else {
                        valueOperand = UndefinedOperand.instance;
                    }

                    addInstruction(new Yield(valueOperand, (short) node.getLineno()));
                    addInstruction(new ThawFrame(true, (short) node.getLineno()));
                    return;
                }
            case Token.YIELD_STAR:
                {
                    Operand valueOperand;
                    if (child != null) {
                        valueOperand = getOperand(child, 0);
                    } else {
                        valueOperand = UndefinedOperand.instance;
                    }

                    addInstruction(new YieldStar(valueOperand, (short) node.getLineno()));
                    addInstruction(new ThawFrame(true, (short) node.getLineno()));
                    return;
                }
            case Token.REF_MEMBER:
            case Token.REF_NS_MEMBER:
            case Token.REF_NAME:
            case Token.REF_NS_NAME:
                {
                    int flags = node.getIntProp(Node.MEMBER_TYPE_PROP, 0);
                    // generate possible target, possible namespace and member
                    List<Operand> operands = new ArrayList<>();
                    do {
                        operands.add(getOperand(child, 0));
                        child = child.getNext();
                    } while (child != null);
                    addInstruction(createRefInstruction(op, operands, flags));
                    return;
                }
            case Token.DOTQUERY:
                {
                    visitExpression(child, 0);
                    addInstruction(EnterDotQuery.instance);
                    int queryPC = instructions.size();
                    visitExpression(child.getNext(), 0);
                    addBackwardGoto(new LeaveDotQuery(), queryPC);
                    return;
                }
            case Token.DEFAULTNAMESPACE:
            case Token.ESCXMLATTR:
            case Token.ESCXMLTEXT:
                {
                    visitExpression(child, 0);
                    addInstruction(getE4xInstruction(op));
                    return;
                }
            default:
                {
                    throw new UnknownInstructionException(
                            "Unknown op: " + Token.typeToName(op), op);
                }
        }
    }

    private Instruction getE4xInstruction(int op) {
        switch (op) {
            case Token.DEFAULTNAMESPACE:
                return DefaultNamespace.instance;
            case Token.ESCXMLATTR:
                return EscXmlAttr.instance;
            case Token.ESCXMLTEXT:
                return EscXmlText.instance;
            default:
                throw new IllegalArgumentException(
                        "Unknown E4X instruction: " + Token.typeToName(op));
        }
    }

    private Instruction createRefInstruction(int op, List<Operand> operands, int flags) {
        switch (op) {
            case Token.REF_MEMBER:
                return new RefMember(operands.get(0), operands.get(1), flags);
            case Token.REF_NS_MEMBER:
                return new RefNsMember(operands.get(0), operands.get(1), operands.get(2), flags);
            case Token.REF_NAME:
                return new RefName(operands.get(0), flags);
            case Token.REF_NS_NAME:
                return new RefNsName(operands.get(0), operands.get(1), flags);
            default:
                throw new IllegalArgumentException(
                        "Unknown ref instruction: " + Token.typeToName(op));
        }
    }

    private void popIfPeek(Operand op) {
        if (op instanceof PeekOperand) {
            addInstruction(Pop.instance);
        }
    }

    private boolean isSafeOperand(Node node) {
        switch (node.getType()) {
            case Token.NUMBER:
            case Token.STRING:
            case Token.NULL:
            case Token.UNDEFINED:
            case Token.TRUE:
            case Token.FALSE:
            case Token.THIS:
            case Token.SUPER:
            case Token.GETVAR:
                return true;
            default:
                return false;
        }
    }

    private Operand getSafeOperand(Node node, int contextFlags, boolean preferPeek) {
        updateLineNumber(node);
        switch (node.getType()) {
            case Token.NUMBER:
                {
                    double num = node.getDouble();
                    int inum = (int) num;

                    return new DoubleOperand(num);
                }
            case Token.STRING:
                return new StringOperand(node.getString());
            case Token.NULL:
                return NullOperand.instance;
            case Token.UNDEFINED:
                return UndefinedOperand.instance;
            case Token.TRUE:
                return BooleanOperand.TRUE;
            case Token.FALSE:
                return BooleanOperand.FALSE;
            case Token.THIS:
                return ThisOperand.instance;
            case Token.SUPER:
                return SuperOperand.instance;
            case Token.GETVAR:
                int index = scriptOrFn.getIndexForNameNode(node);
                return GetVarOperand.createOperand(index);
            default:
                badTree(node);
                return null;
        }
    }

    private Operand getOperand(Node node, int contextFlags) {
        return getOperand(node, contextFlags, false, null);
    }

    /**
     * Returns operands for literals, otherwise returns a peek or pop operand. We limit ourselves to
     * these simple cases because anything more complex requires more careful application to
     * maintain correct evaluation order.
     */
    private Operand getOperand(Node node, int contextFlags, boolean preferPeek) {
        return getOperand(node, contextFlags, preferPeek, null);
    }

    private Operand getOperand(
        Node node, int contextFlags, boolean preferPeek, ArrayList<Integer> lines) {
        addLineNumber(node, lines);
        switch (node.getType()) {
            case Token.NUMBER: {
                double num = node.getDouble();
                int inum = (int) num;

                return new DoubleOperand(num);
            }
            case Token.STRING:
                return new StringOperand(node.getString());
            case Token.NULL:
                return NullOperand.instance;
            case Token.UNDEFINED:
                return UndefinedOperand.instance;
            case Token.TRUE:
                return BooleanOperand.TRUE;
            case Token.FALSE:
                return BooleanOperand.FALSE;
            case Token.THIS:
                return ThisOperand.instance;
            case Token.SUPER:
                return SuperOperand.instance;
            default: {
                visitExpression(node, contextFlags);
                if (preferPeek) {
                    return PeekOperand.instance;
                }
                return PopOperand.instance;
            }
        }
    }

    private void addLineNumber(Node node, ArrayList<Integer> lines) {
        if (lines == null) {
            updateLineNumber(node);
            return;
        }
        int lineno = node.getLineno();
        if (lineno < 0) return;
        SourceMapper mapper = compilerEnv.getSourceMapper();
        if (mapper != null) {
            Position mapped = mapper.mapPosition(lineno, node.getColumn());
            if (mapped == null)
                return;
            lineno = mapped.getLine();
        }
        lines.add(lineno);
    }

    private void updateLineNumber(Node node) {
        int lineno = node.getLineno();
        if (lineno < 0)
            return;
        SourceMapper mapper = compilerEnv.getSourceMapper();
        if (mapper != null) {
            Position mapped = mapper.mapPosition(lineno, node.getColumn());
            if (mapped == null)
                return;
            lineno = mapped.getLine();
        }

        // Token.printColumns and SourceMapper not in open-source

        updateLineNumber(lineno);
    }

    private void updateLineNumbers(ArrayList<Integer> lines) {
        for (int i : lines) {
            updateLineNumber(i);
        }
    }

    private void updateLineNumber(int lineno) {
        if (lineNumber == lineno) return;

        lineNumber = lineno;
        lineNumberTable.add(instructions.size(), lineNumber, lineNumber);
    }

    public void stackChange(int change) {
        if (change <= 0) {
            stackDepth += change;
        } else {
            int newDepth = stackDepth + change;
            if (newDepth > builder.maxStack) {
                builder.maxStack = newDepth;
            }
            stackDepth = newDepth;
        }
    }

    private static int getLocalBlockRef(Node node) {
        Node localBlock = (Node) node.getProp(Node.LOCAL_BLOCK_PROP);
        return localBlock.getExistingIntProp(Node.LOCAL_PROP);
    }

    private int allocLocal() {
        int localSlot = localTop;
        ++localTop;
        if (localTop > builder.maxLocals) {
            builder.maxLocals = localTop;
        }
        return localSlot;
    }

    private void releaseLocal(int localSlot) {
        --localTop;
        if (localSlot != localTop) Kit.codeBug();
    }

    private boolean shouldTreatNumberAsInteger(double num) {
        if (!treatNumericLiteralsLikeOldRhino) {
            return (int) num == num;
        }
        // Old-rhino numeric handling not present in open-source
        return (int) num == num;
    }

    private static RuntimeException badTree(Node node) {
        throw new UnknownInstructionException(
                "Compiler.badTree Unknown op: " + Token.typeToName(node.getType()), node.getType());
    }

    /**
     * No-spread object literal. Pre-populates {@link NewObjectLiteral} with the static keys and
     * with all literal-token property values (anywhere they appear, not just a leading prefix —
     * literals are stack-neutral and side-effect-free, so out-of-order evaluation is safe). The
     * remaining slots are filled in source order by {@link LitSetAt}, which writes to a known slot
     * index without touching the storage's sequential write pointer.
     */
    private void emitObjectLiteralNoSpread(
            Node child, Object[] propertyIds, int count, boolean hasAnyComputedProperty) {
        Object[] keys = new Object[count];
        Operand[] literalValues = new Operand[count];
        boolean[] needsPass2 = new boolean[count];

        // Pass 1: walk children once, capture static keys and literal-value operands.
        // Literal operands are stack-neutral and emit no instructions, so their value slot can
        // be pre-populated regardless of position — including when the key is computed (Pass 2
        // still emits a key-only LitSetAt for those slots). Pass 2 fills the rest in source
        // order.
        Node c = child;
        for (int i = 0; i < count; i++, c = c.getNext()) {
            Object pid = propertyIds[i];
            keys[i] = (pid instanceof Node) ? null : pid;

            int ct = c.getType();
            boolean valueIsLiteral =
                    ct != Token.GET && ct != Token.SET && ct != Token.METHOD && isLiteralToken(ct);
            if (valueIsLiteral) {
                literalValues[i] = getOperand(c, 0);
            }
            // Need Pass 2 for: any computed key (must run the key expression), or any
            // non-literal value (getter/setter/method/expression).
            needsPass2[i] = (pid instanceof Node) || !valueIsLiteral;
        }

        // If we do not have any computed properties, we can simply reuse the keys
        // array as it won't be modified. If we do have computed properties, though,
        // we'll fill some of the `null` holes, so we need to copy it.
        addInstruction(new NewObjectLiteral(keys, literalValues, hasAnyComputedProperty));

        // Pass 2: emit slots needing runtime work in source order with explicit slot indices.
        c = child;
        for (int i = 0; i < count; i++, c = c.getNext()) {
            if (!needsPass2[i]) {
                continue;
            }
            updateLineNumber(c);
            Object pid = propertyIds[i];

            Operand keyOp;
            boolean keyExprEmitted;
            if (pid instanceof Node) {
                visitExpression(((Node) pid).getFirstChild(), 0);
                // ToPropertyKey must happen before the value expression is evaluated
                // (ECMA 13.2.5.5 step 1 before step 6).
                addInstruction(ToPropertyKey.instance);
                keyOp = PopOperand.instance;
                keyExprEmitted = true;
            } else {
                // Static key already at storage.keys[i].
                keyOp = null;
                keyExprEmitted = false;
            }

            Operand valueOp;
            int kind;
            int childType = c.getType();
            if (literalValues[i] != null) {
                // Computed key + literal value: value already pre-populated by NewObjectLiteral;
                // LitSetAt only needs to write the runtime-computed key.
                kind = 0;
                valueOp = null;
            } else if (childType == Token.GET
                    || childType == Token.SET
                    || childType == Token.METHOD) {
                kind = (childType == Token.GET) ? -1 : (childType == Token.SET) ? 1 : 0;
                var func = c.getFirstChild();
                assert (func.getType() == Token.FUNCTION);
                int fnIndex = func.getExistingIntProp(Node.FUNCTION_PROP);
                FunctionNode fn = scriptOrFn.getFunctionNode(fnIndex);
                if (fn.getFunctionType() != FunctionNode.FUNCTION_EXPRESSION
                        && fn.getFunctionType() != FunctionNode.ARROW_FUNCTION) {
                    throw Kit.codeBug();
                }
                if (fn.isMethodDefinition()) {
                    // Stack: [..., obj, storage, (key if emitted)].
                    int objOffset = keyExprEmitted ? -2 : -1;
                    addInstruction(new MethodExpression(fnIndex, new PeekOperand(objOffset)));
                } else {
                    addInstruction(new ClosureExpression(fnIndex));
                }
                valueOp = PopOperand.instance;
            } else {
                kind = 0;
                valueOp = getOperand(c, 0);
            }

            addInstruction(new LitSetAt(i, keyOp, valueOp, kind));
        }

        addInstruction(new ObjectLit(PeekOperand.instance));
    }

    /**
     * With-spread object literal. Pre-populates {@link NewObjectLiteralWithSpread} with the
     * contiguous leading literal-value prefix (slot indices after the first spread aren't
     * compile-time knowable, so we keep the conservative prefix-only optimization here). Subsequent
     * {@link LitPush} / {@link LitSpread} instructions fill the rest sequentially.
     */
    private void emitObjectLiteralWithSpread(
            Node child, Object[] propertyIds, int count, int nonSpreadCount) {
        List<Operand> prefixValues = new ArrayList<>();
        List<Object> prefixKeys = new ArrayList<>();
        int prefixLen = 0;
        Node c = child;
        while (prefixLen < count) {
            Object pid = propertyIds[prefixLen];
            if (pid instanceof Node) {
                break; // computed key or spread
            }
            int ct = c.getType();
            if (ct == Token.GET || ct == Token.SET || ct == Token.METHOD) {
                break;
            }
            if (!isLiteralToken(ct)) {
                break;
            }
            prefixValues.add(getOperand(c, 0));
            prefixKeys.add(pid);
            prefixLen++;
            c = c.getNext();
        }

        addInstruction(
                new NewObjectLiteralWithSpread(
                        prefixKeys.toArray(),
                        prefixValues.toArray(Operand.EMPTY_ARRAY),
                        nonSpreadCount));

        for (int i = prefixLen; i < count; i++, c = c.getNext()) {
            updateLineNumber(c);
            Object pid = propertyIds[i];

            // Spread?
            if (pid instanceof Node && ((Node) pid).getType() == Token.DOTDOTDOT) {
                visitExpression(((Node) pid).getFirstChild(), 0);
                addInstruction(new LitSpread(PopOperand.instance));
                continue;
            }

            Operand keyOp;
            boolean keyExprEmitted;
            if (pid instanceof Node) {
                visitExpression(((Node) pid).getFirstChild(), 0);
                keyOp = PopOperand.instance;
                keyExprEmitted = true;
            } else {
                keyOp = makeStaticKeyOperand(pid);
                keyExprEmitted = false;
            }

            Operand valueOp;
            int kind;
            int childType = c.getType();
            if (childType == Token.GET || childType == Token.SET || childType == Token.METHOD) {
                kind = (childType == Token.GET) ? -1 : (childType == Token.SET) ? 1 : 0;
                var func = c.getFirstChild();
                assert (func.getType() == Token.FUNCTION);
                int fnIndex = func.getExistingIntProp(Node.FUNCTION_PROP);
                FunctionNode fn = scriptOrFn.getFunctionNode(fnIndex);
                if (fn.getFunctionType() != FunctionNode.FUNCTION_EXPRESSION
                        && fn.getFunctionType() != FunctionNode.ARROW_FUNCTION) {
                    throw Kit.codeBug();
                }
                if (fn.isMethodDefinition()) {
                    // Stack: [..., obj, storage, (key if emitted)].
                    int objOffset = keyExprEmitted ? -2 : -1;
                    addInstruction(new MethodExpression(fnIndex, new PeekOperand(objOffset)));
                } else {
                    addInstruction(new ClosureExpression(fnIndex));
                }
                valueOp = PopOperand.instance;
            } else {
                kind = 0;
                valueOp = getOperand(c, 0);
            }

            addInstruction(new LitPush(keyOp, valueOp, kind));
        }

        addInstruction(new ObjectLit(PeekOperand.instance));
    }

    /**
     * Tokens whose {@link #getOperand} branch returns a stack-neutral operand without emitting any
     * instruction. Must stay in sync with {@code getOperand}'s {@code stackChange() == 0} branches
     * — BIGINT is NOT here because {@code getOperand} has no BIGINT branch and falls through to
     * {@code visitExpression}, which emits a {@link
     * org.mozilla.javascript.interpreterv2.instruction.BigInt} instruction.
     */
    private static boolean isLiteralToken(int type) {
        switch (type) {
            case Token.NUMBER:
            case Token.STRING:
            case Token.NULL:
            case Token.UNDEFINED:
            case Token.TRUE:
            case Token.FALSE:
                return true;
            default:
                return false;
        }
    }

    private static Operand makeStaticKeyOperand(Object pid) {
        if (pid instanceof String) {
            return new StringOperand((String) pid);
        }
        if (pid instanceof Integer) {
            return new IntOperand((Integer) pid);
        }
        throw Kit.codeBug();
    }

    private int getTargetLabel(Node target) {
        int label = target.labelId();
        if (label != -1) {
            return label;
        }
        label = labelTableTop;
        if (labelTable == null || label == labelTable.length) {
            if (labelTable == null) {
                labelTable = new int[32];
            } else {
                int[] tmp = new int[labelTable.length * 2];
                System.arraycopy(labelTable, 0, tmp, 0, label);
                labelTable = tmp;
            }
        }
        labelTableTop = label + 1;
        labelTable[label] = -1;

        target.labelId(label);
        return label;
    }

    private void markTargetLabel(Node target) {
        int label = getTargetLabel(target);
        if (labelTable[label] != -1) {
            // Can mark label only once
            Kit.codeBug();
        }
        int targetPC = instructions.size();
        labelTable[label] = targetPC;
        // Track this as a jump target
        jumpTargets.add(targetPC);
    }

    private void addGoto(Node target, JumpInstruction jumpInstruction) {
        int label = getTargetLabel(target);
        if (!(label < labelTableTop)) Kit.codeBug();
        int targetPC = labelTable[label];

        if (targetPC != -1) {
            addBackwardGoto(jumpInstruction, targetPC);
        } else {
            int gotoPC = instructions.size();
            addInstruction(jumpInstruction);
            fixupTable.add(((long) label << 32) | gotoPC);
        }
    }

    private void addGoto(Node target, JumpInstruction jumpInstruction, int gotoPC) {
        int label = getTargetLabel(target);
        if (!(label < labelTableTop)) Kit.codeBug();
        int targetPC = labelTable[label];

        if (targetPC != -1) {
            assert false;
        } else {
            addInstruction(gotoPC, jumpInstruction);
            fixupTable.add(((long) label << 32) | gotoPC);
        }
    }

    private void addGoto(Node target) {
        int label = getTargetLabel(target);
        if (!(label < labelTableTop)) Kit.codeBug();
        int targetPC = labelTable[label];

        if (targetPC != -1) {
            assert false;
        } else {
            int gotoPC = instructions.size();
            fixupTable.add(((long) label << 32) | gotoPC);
        }
    }

    private void fixLabelGotos() {
        for (int i = 0; i < fixupTable.size(); i++) {
            long fixup = fixupTable.get(i);
            int label = (int) (fixup >> 32);
            int jumpSource = (int) fixup;
            int pc = labelTable[label];
            if (pc == -1) {
                // Unlocated label
                throw Kit.codeBug();
            }
            resolveGoto(jumpSource, pc);
        }
        fixupTable.clear();
    }

    private void addBackwardGoto(JumpInstruction jumpInstruction, int jumpPC) {
        int fromPC = instructions.size();
        // Ensure that this is a jump backward
        if (fromPC < jumpPC) throw Kit.codeBug();
        addInstruction(jumpInstruction);
        resolveGoto(fromPC, jumpPC);
    }

    private void resolveForwardGoto(int fromPC) {
        resolveGoto(fromPC, instructions.size());
    }

    private void resolveGoto(int fromPC, int jumpPC) {
        int offset = jumpPC - fromPC;
        var jump = (JumpInstruction) instructions.get(fromPC);

        jump.setOffset(offset);
        jumpTargets.addAll(jump.getTargets(fromPC));
    }

    // Handles super.x++ and variants thereof. We don't want to create new icode in the interpreter
    // for this edge case, so we will transform this into something like super.x = super.x + 1
    private void visitSuperIncDec(Node node, Node child, int childType, int incrDecrMask) {
        Node object = child.getFirstChild();

        var superObject = getOperand(object, 0);
        switch (childType) {
            case Token.GETPROP:
                addInstruction(
                        new GetPropSuper(
                                superObject, object.getNext().getString(), false)); // stack: [p]
                break;

            case Token.GETELEM:
                {
                    Node index = object.getNext();
                    var elem = getOperand(index, 0); // stack: [elem]
                    addInstruction(new GetElemSuper(superObject, elem)); // stack: [p]
                    break;
                }

            default:
                throw badTree(node);
        }

        // If it's a postfix expression, we copy the old value
        // If it's prefix, we only need the _new_ value on the stack
        if ((incrDecrMask & Node.POST_FLAG) != 0) {
            // We can keep this Dup since the previous instruction always pushes to the stack
            addInstruction(Dup.instance); // stack: postfix [p, p], prefix: [p]
        }

        // Increment or decrement the new value

        // stack: prefix [p, 1], postfix: [p, p, 1]
        Operand one = OneOperand.instance;

        if ((incrDecrMask & Node.DECR_FLAG) == 0) {
            addInstruction(new Add(PopOperand.instance, one));
            // stack: prefix [p+1], postfix: [p, p+1]
        } else {
            addInstruction(new Subtract(PopOperand.instance, one));
            // stack: prefix [p-1], postfix: [p, p-1]
        }

        // Assign the new value to the property
        switch (childType) {
            case Token.GETPROP:
                addInstruction(
                        new SetPropSuper(
                                superObject, object.getNext().getString(), PopOperand.instance));
                // stack: prefix [p+-1], postfix: [p, p+-1]
                break;

            case Token.GETELEM:
                {
                    Node index = object.getNext();
                    var indexOperand = getOperand(index, 0);
                    // stack: prefix [p+-1, elem], postfix: [p, p+-1, elem]
                    addInstruction(
                            new SetElemSuper(superObject, indexOperand, PopOperand.instance));
                    // stack: prefix [p+-1], postfix: [p, p+-1]
                    break;
                }
        }

        // If it was a postfix, just drop the new value
        if ((incrDecrMask & Node.POST_FLAG) != 0) {
            addInstruction(Pop.instance); // stack: [p]
        }
    }

    private void visitIncDec(Node node, Node child) {
        int incrDecrMask = node.getExistingIntProp(Node.INCRDECR_PROP);
        int childType = child.getType();

        if (child.getIntProp(Node.SUPER_PROPERTY_ACCESS, 0) == 1) {
            visitSuperIncDec(node, child, childType, incrDecrMask);
            return;
        }

        switch (childType) {
            case Token.GETVAR:
                {
                    if (builder.needsActivation) Kit.codeBug();
                    int i = scriptOrFn.getIndexForNameNode(child);
                    addInstruction(new VarIncDec(i, incrDecrMask));
                    break;
                }
            case Token.NAME:
                {
                    String name = child.getString();
                    addInstruction(new NameIncDec(name, incrDecrMask));
                    break;
                }
            case Token.GETPROP:
                {
                    Node object = child.getFirstChild();
                    var objOperand = getOperand(object, 0);
                    String property = object.getNext().getString();
                    addInstruction(new PropIncDec(objOperand, property, incrDecrMask));
                    break;
                }
            case Token.GETELEM:
                {
                    Node object = child.getFirstChild();
                    var objOperand = getOperand(object, 0);
                    Node index = object.getNext();
                    var elem = getOperand(index, 0);
                    addInstruction(new ElemIncDec(objOperand, elem, incrDecrMask));
                    break;
                }
            case Token.GET_REF:
                {
                    Node ref = child.getFirstChild();
                    var refOperand = getOperand(ref, 0);
                    addInstruction(new RefIncDec(refOperand, incrDecrMask));
                    break;
                }
            default:
                {
                    throw badTree(node);
                }
        }
    }

    private void finishGetElemGeneration(Node child, Operand lhs) {
        var elem = getOperand(child, 0);
        addInstruction(new GetElem(lhs, elem));
    }

    private static final class CompleteOptionalCallJump {
        private final int putArgsAndDoCallLabel;
        private final int afterLabel;

        public CompleteOptionalCallJump(int putArgsAndDoCallLabel, int afterLabel) {
            this.putArgsAndDoCallLabel = putArgsAndDoCallLabel;
            this.afterLabel = afterLabel;
        }
    }

    private CompleteOptionalCallJump generateCallFunAndThis(
            Node left, boolean isOptionalChainingCall) {
        // Generate code to place on stack function and thisObj
        int type = left.getType();
        switch (type) {
            case Token.NAME:
                {
                    String name = left.getString();
                    // stack: ... -> ... function thisObj
                    if (isOptionalChainingCall) {
                        addInstruction(new NameAndThisOptional(name));
                        return completeOptionalCallJump();
                    } else {
                        addInstruction(new NameAndThis(name));
                    }
                    break;
                }
            case Token.GETPROP:
            case Token.GETELEM:
                {
                    Node target = left.getFirstChild();
                    var obj = getOperand(target, 0);
                    Node id = target.getNext();
                    if (type == Token.GETPROP) {
                        String property = id.getString();
                        // stack: ... target -> ... function thisObj
                        if (isOptionalChainingCall) {
                            addInstruction(new PropAndThisOptional(obj, property));
                            return completeOptionalCallJump();
                        } else {
                            addInstruction(new PropAndThis(obj, property));
                        }
                    } else {
                        var idOperand = getOperand(id, 0);
                        // stack: ... target id -> ... function thisObj
                        if (isOptionalChainingCall) {
                            addInstruction(new ElemAndThisOptional(obj, idOperand));
                            return completeOptionalCallJump();
                        } else {
                            addInstruction(new ElemAndThis(obj, idOperand));
                        }
                    }
                    break;
                }
            default:
                // Including Token.GETVAR
                var value = getOperand(left, 0);
                // stack: ... value -> ... function thisObj
                if (isOptionalChainingCall) {
                    addInstruction(new ValueAndThisOptional(value));
                    return completeOptionalCallJump();
                } else {
                    addInstruction(new ValueAndThis(value));
                }
                break;
        }
        return null;
    }

    private CompleteOptionalCallJump completeOptionalCallJump() {
        // If it's null or undefined, pop undefined and skip the arguments and call
        int putArgsAndDoCallLabel = instructions.size();
        addInstruction(new IfNotNullUndefined(PeekOperand.instance));

        // Put undefined
        addInstruction(Pop.instance); // lookupResult
        addInstruction(new Name("undefined"));
        int afterLabel = instructions.size();
        addInstruction(new Goto());

        return new CompleteOptionalCallJump(putArgsAndDoCallLabel, afterLabel);
    }
}
