/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.optimizer;

import static org.mozilla.classfile.ClassFileWriter.ACC_FINAL;
import static org.mozilla.classfile.ClassFileWriter.ACC_PRIVATE;
import static org.mozilla.classfile.ClassFileWriter.ACC_PUBLIC;
import static org.mozilla.classfile.ClassFileWriter.ACC_STATIC;
import static org.mozilla.classfile.ClassFileWriter.ACC_VOLATILE;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.classfile.DynamicConstant;
import org.mozilla.classfile.DynamicConstantDescriber;
import org.mozilla.javascript.CodeGenUtils;
import org.mozilla.javascript.CompilationResult;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Evaluator;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.InstrumentEmitter;
import org.mozilla.javascript.InstrumentEmitter.Type;
import org.mozilla.javascript.JSDescriptor;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.JSScript;
import org.mozilla.javascript.RegExpProxy;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptOrFn;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.SecurityController;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.config.RhinoConfig;
import org.mozilla.javascript.debug.DebuggableScript;

/**
 * This class generates code for a given IR tree.
 *
 * @author Norris Boyd
 * @author Roger Lawrence
 */
public class Codegen implements Evaluator {
    @Override
    public void captureStackInfo(RhinoException ex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSourcePositionFromStack(Context cx, int[] linep) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPatchedStack(RhinoException ex, String nativeStackTrace) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getScriptStack(RhinoException ex) {
        throw new UnsupportedOperationException();
    }

    /**
     * A single generated class file, and the builder environment used to build the {@link MHJSCode}
     * objects for whichever script/function bodies it contains.
     */
    private static final class CompileUnit {
        final String className;
        final byte[] bytecode;
        final MHJSCode.BuilderEnv env;

        CompileUnit(String className, byte[] bytecode, MHJSCode.BuilderEnv env) {
            this.className = className;
            this.bytecode = bytecode;
            this.env = env;
        }
    }

    private static class CodegenCompilationResult<T extends ScriptOrFn<T>>
            implements CompilationResult<T> {
        final JSDescriptor.Builder<T> builder;

        /** The main class is always {@code units.get(0)}; any others are auxiliary chunks. */
        final List<CompileUnit> units;

        CodegenCompilationResult(JSDescriptor.Builder<T> builder, List<CompileUnit> units) {
            this.builder = builder;
            this.units = units;
        }

        @Override
        public DebuggableScript getDebuggableScript() {
            // Can't debug compiled classes
            return null;
        }
    }

    @Override
    public CompilationResult<JSScript> compileScript(
            CompilerEnvirons compilerEnv, ScriptNode tree, String rawSource) {
        return doCompile(compilerEnv, tree, rawSource, false);
    }

    @Override
    public CompilationResult<JSFunction> compileFunction(
            CompilerEnvirons compilerEnv, ScriptNode tree, String rawSource) {
        return doCompile(compilerEnv, tree, rawSource, true);
    }

    private <T extends ScriptOrFn<T>> CodegenCompilationResult<T> doCompile(
            CompilerEnvirons compilerEnv,
            ScriptNode tree,
            String rawSource,
            boolean returnFunction) {
        var event = InstrumentEmitter.emitter.startEvent(Type.COMPILE_CLASSFILE);
        try {
            return doCompileInt(compilerEnv, tree, rawSource, returnFunction);
        } finally {
            InstrumentEmitter.emitter.endEvent(event, tree.getSourceName());
        }
    }

    private <T extends ScriptOrFn<T>> CodegenCompilationResult<T> doCompileInt(
            CompilerEnvirons compilerEnv,
            ScriptNode tree,
            String rawSource,
            boolean returnFunction) {
        int serial;
        synchronized (globalLock) {
            serial = ++globalSerialClassCounter;
        }

        String baseName = "c";
        if (tree.getSourceName().length() > 0) {
            baseName = tree.getSourceName().replaceAll("\\W", "_");
            if (!Character.isJavaIdentifierStart(baseName.charAt(0))) {
                baseName = "_" + baseName;
            }
        }

        String mainClassName = "org.mozilla.javascript.gen." + baseName + "_" + serial;

        JSDescriptor.Builder<T> builder = new JSDescriptor.Builder<>();
        List<CompileUnit> units =
                compileToClassFileWithSplitting(
                        compilerEnv, builder, mainClassName, tree, rawSource, returnFunction);

        return new CodegenCompilationResult<>(builder, units);
    }

    @Override
    public Script createScriptObject(
            CompilationResult<JSScript> compiled, Object staticSecurityDomain) {
        JSDescriptor<JSScript> desc =
                defineClass((CodegenCompilationResult<JSScript>) compiled, staticSecurityDomain);
        return JSFunction.createScript(desc, null, staticSecurityDomain);
    }

    @Override
    public Function createFunctionObject(
            Context cx,
            VarScope scope,
            CompilationResult<JSFunction> compiled,
            Object staticSecurityDomain) {
        JSDescriptor<JSFunction> desc =
                defineClass((CodegenCompilationResult<JSFunction>) compiled, staticSecurityDomain);
        return JSFunction.createFunction(cx, scope, desc, null, staticSecurityDomain);
    }

    private <T extends ScriptOrFn<T>> JSDescriptor<T> defineClass(
            CodegenCompilationResult<T> compiled, Object staticSecurityDomain) {
        // The generated classes in this case refer only to Rhino classes
        // which must be accessible through this class loader
        ClassLoader rhinoLoader = getClass().getClassLoader();
        GeneratedClassLoader loader;
        loader = SecurityController.createLoader(rhinoLoader, staticSecurityDomain);
        Exception e;
        try {
            // The main class (units.get(0)) must be defined first: any auxiliary chunk classes
            // reference its fields (descriptors, cached constants, template literals), but not
            // the reverse.
            Class<?> mainClass = null;
            for (CompileUnit unit : compiled.units) {
                Class<?> cl = loader.defineClass(unit.className, unit.bytecode);
                loader.linkClass(cl);
                unit.env.compiledClass = cl;
                if (mainClass == null) {
                    mainClass = cl;
                }
            }
            var descs = new ArrayList<JSDescriptor<?>>();
            JSDescriptor<T> desc = compiled.builder.build(d -> descs.add(d));
            mainClass
                    .getField(DESCRIPTORS_FIELD_NAME)
                    .set(null, descs.toArray(new JSDescriptor[0]));
            return desc;
        } catch (SecurityException
                | IllegalArgumentException
                | IllegalAccessException
                | NoSuchFieldException x) {
            e = x;
        }
        throw new RuntimeException(e);
    }

    public byte[] compileToClassFile(
            CompilerEnvirons compilerEnv,
            JSDescriptor.Builder<?> builder,
            MHJSCode.BuilderEnv builderEnv,
            String mainClassName,
            ScriptNode scriptOrFn,
            String rawSource,
            boolean returnFunction) {
        scriptOrFn =
                prepareForCodegen(
                        compilerEnv, builder, mainClassName, scriptOrFn, rawSource, returnFunction);
        initScriptNodesData(scriptOrFn, builder, builderEnv);
        return generateCode(
                rawSource, new MHJSCode.BuilderEnv[] {builderEnv}, new String[] {mainClassName})[0];
    }

    /**
     * Compile a script or function, splitting the generated code across multiple class files (and
     * therefore multiple constant pools) if a single class would exceed a JVM class file limit.
     *
     * <p>Splitting only ever happens across separate script/function bodies: a single body that by
     * itself overflows a class file is not split further, and this method rethrows the {@link
     * ClassFileWriter.ClassSizeException} in that case so that the caller can fall back to the
     * interpreter as before.
     */
    <T extends ScriptOrFn<T>> List<CompileUnit> compileToClassFileWithSplitting(
            CompilerEnvirons compilerEnv,
            JSDescriptor.Builder<T> builder,
            String mainClassName,
            ScriptNode scriptOrFn,
            String rawSource,
            boolean returnFunction) {
        scriptOrFn =
                prepareForCodegen(
                        compilerEnv, builder, mainClassName, scriptOrFn, rawSource, returnFunction);

        MHJSCode.BuilderEnv mainEnv = new MHJSCode.BuilderEnv(mainClassName);
        initScriptNodesData(scriptOrFn, builder, mainEnv);

        int numChunks = 1;
        MHJSCode.BuilderEnv[] chunkEnvs = {mainEnv};
        String[] chunkClassNames = {mainClassName};

        while (true) {
            try {
                byte[][] bytecode = generateCode(rawSource, chunkEnvs, chunkClassNames);
                List<CompileUnit> units = new ArrayList<>(numChunks);
                for (int i = 0; i != numChunks; ++i) {
                    units.add(new CompileUnit(chunkClassNames[i], bytecode[i], chunkEnvs[i]));
                }
                return units;
            } catch (ClassFileWriter.ClassSizeException e) {
                int nextNumChunks = numChunks * 2;
                if (numChunks >= scriptOrFnNodes.length || nextNumChunks > MAX_SPLIT_CHUNKS) {
                    // Either we can't split any further (down to one body per chunk) or we've
                    // hit a sanity limit on how many chunks we'll try: give up and let the
                    // caller fall back to the interpreter, as before this feature existed.
                    throw new ClassFileWriter.ClassSizeException("Class splitting failed", e);
                }
                numChunks = Math.min(nextNumChunks, scriptOrFnNodes.length);
                chunkClassNames = new String[numChunks];
                chunkEnvs = new MHJSCode.BuilderEnv[numChunks];
                chunkClassNames[0] = mainClassName;
                chunkEnvs[0] = mainEnv;
                for (int i = 1; i != numChunks; ++i) {
                    chunkClassNames[i] = mainClassName + "$part" + i;
                    chunkEnvs[i] = new MHJSCode.BuilderEnv(chunkClassNames[i]);
                }
                rebindBuildersToChunks(numChunks, chunkEnvs);
            }
        }
    }

    /** Common setup shared by every codegen entry point: transform the tree and fill in builder. */
    private ScriptNode prepareForCodegen(
            CompilerEnvirons compilerEnv,
            JSDescriptor.Builder<?> builder,
            String mainClassName,
            ScriptNode scriptOrFn,
            String rawSource,
            boolean returnFunction) {
        this.compilerEnv = compilerEnv;

        transform(scriptOrFn);

        if (Token.printTrees) {
            System.out.println(scriptOrFn.toStringTree(scriptOrFn));
        }

        if (returnFunction) {
            CodeGenUtils.fillInForTopLevelFunction(
                    builder, scriptOrFn.getFunctionNode(0), rawSource, compilerEnv);
            scriptOrFn = scriptOrFn.getFunctionNode(0);
        } else {
            CodeGenUtils.fillInForScript(builder, scriptOrFn, rawSource, compilerEnv);
        }

        this.mainClassName = mainClassName;
        this.mainClassSignature = ClassFileWriter.classNameToSignature(mainClassName);
        return scriptOrFn;
    }

    /**
     * Reassign every script/function body's {@link MHJSCode.Builder} to the {@link
     * MHJSCode.BuilderEnv} for the chunk it has been placed in, splitting {@link #scriptOrFnNodes}
     * into {@code numChunks} contiguous groups (the first of which is always the main chunk, so
     * that the top-level script/function - always index 0 - keeps its original class name).
     */
    private void rebindBuildersToChunks(int numChunks, MHJSCode.BuilderEnv[] chunkEnvs) {
        int count = scriptOrFnNodes.length;
        int groupSize = (count + numChunks - 1) / numChunks;
        for (int i = 0; i != count; ++i) {
            ScriptNode n = scriptOrFnNodes[i];
            int chunk = Math.min(i / groupSize, numChunks - 1);
            MHJSCode.BuilderEnv env = chunkEnvs[chunk];

            @SuppressWarnings("unchecked")
            MHJSCode.Builder code =
                    (n instanceof FunctionNode)
                            ? new MHJSFunctionCode.Builder(env)
                            : new MHJSScriptCode.Builder(env);
            code.index = i;
            code.methodName = getBodyMethodName(n, i);
            code.methodType = getNonDirectBodyMethodSIgnature(n);
            if (isGenerator(n)) {
                code.resumeName = code.methodName + "_gen";
                code.resumeType = GENERATOR_METHOD_SIGNATURE;
            }

            JSDescriptor.Builder builder = builders[i];
            builder.setCode(code);
            CodeGenUtils.setConstructor(builder, n);
        }
    }

    /** The class name of whichever chunk {@code n}'s body method has been placed in. */
    String getClassNameForNode(ScriptNode n) {
        return ((MHJSCode.Builder<?>) builders[getIndex(n)].code).env.className;
    }

    private static final int MAX_SPLIT_CHUNKS = 1024;

    private void transform(ScriptNode tree) {
        initOptFunctions_r(tree);

        if (compilerEnv.isInterpretedMode()) {
            // Kit.codeBug("Codegen must not run in interpreted Mode");
            throw new Error();
        }
        Map<String, OptFunctionNode> possibleDirectCalls = null;
        /*
         * Collect all of the contained functions into a hashtable
         * so that the call optimizer can access the class name & parameter
         * count for any call it encounters
         */
        if (tree.getType() == Token.SCRIPT) {
            int functionCount = tree.getFunctionCount();
            for (int i = 0; i != functionCount; ++i) {
                OptFunctionNode ofn = OptFunctionNode.get(tree, i);
                if (ofn.fnode.getFunctionType() == FunctionNode.FUNCTION_STATEMENT) {
                    String name = ofn.fnode.getName();
                    if (name.length() != 0) {
                        if (possibleDirectCalls == null) {
                            possibleDirectCalls = new HashMap<>();
                        }
                        possibleDirectCalls.put(name, ofn);
                    }
                }
            }
        }

        if (possibleDirectCalls != null) {
            directCallTargets = new ArrayList<>();
        }

        OptTransformer ot = new OptTransformer(possibleDirectCalls, directCallTargets);
        ot.transform(tree, compilerEnv);

        new Optimizer().optimize(tree);
    }

    private static void initOptFunctions_r(ScriptNode scriptOrFn) {
        for (int i = 0, N = scriptOrFn.getFunctionCount(); i != N; ++i) {
            FunctionNode fn = scriptOrFn.getFunctionNode(i);
            new OptFunctionNode(fn);
            initOptFunctions_r(fn);
        }
    }

    private <U extends ScriptOrFn<U>> void initScriptNodesData(
            ScriptNode scriptOrFn,
            JSDescriptor.Builder<U> builder,
            MHJSCode.BuilderEnv builderEnv) {
        ArrayList<ScriptNode> x = new ArrayList<>();
        ArrayList<JSDescriptor.Builder<?>> b = new ArrayList<>();
        collectScriptNodes_r(scriptOrFn, builder, builderEnv, x, b);

        int count = x.size();
        scriptOrFnNodes = new ScriptNode[count];
        builders = new JSDescriptor.Builder[count];
        scriptOrFnNodes = x.toArray(scriptOrFnNodes);
        builders = b.toArray(builders);

        scriptOrFnIndexes = new HashMap<>();
        for (int i = 0; i != count; ++i) {
            scriptOrFnIndexes.put(scriptOrFnNodes[i], i);
        }
    }

    private <U extends ScriptOrFn<U>> void collectScriptNodes_r(
            ScriptNode n,
            JSDescriptor.Builder<U> builder,
            MHJSCode.BuilderEnv builderEnv,
            List<ScriptNode> x,
            List<JSDescriptor.Builder<?>> b) {

        @SuppressWarnings("unchecked")
        MHJSCode.Builder<U> code =
                (MHJSCode.Builder<U>)
                        ((n instanceof FunctionNode)
                                ? new MHJSFunctionCode.Builder(builderEnv)
                                : new MHJSScriptCode.Builder(builderEnv));
        code.index = x.size();
        code.methodName = getBodyMethodName(n, x.size());
        code.methodType = getNonDirectBodyMethodSIgnature(n);
        if (isGenerator(n)) {
            code.resumeName = code.methodName + "_gen";
            code.resumeType = GENERATOR_METHOD_SIGNATURE;
        }
        builder.setCode(code);
        builderEnv.hasRegExpLiterals |= (n.getRegexpCount() > 0);
        builderEnv.hasTemplateLiterals |= (n.getTemplateLiteralCount() > 0);
        CodeGenUtils.setConstructor(builder, n);

        x.add(n);
        b.add(builder);
        int nestedCount = n.getFunctionCount();
        for (int i = 0; i != nestedCount; ++i) {
            var f = n.getFunctionNode(i);
            var fb = builder.createChildBuilder();
            CodeGenUtils.fillInForNestedFunction(fb, builder, f);
            collectScriptNodes_r(f, fb, builderEnv, x, b);
        }
    }

    static byte[] generateOptJSCode(
            String mainClass,
            String methodName,
            String methodType,
            String resumeName,
            String resumeType,
            boolean isFunction,
            int index) {
        String sourceFile = "";
        ClassFileWriter cfw =
                new ClassFileWriter(
                        mainClass + "ojsc" + Integer.toString(index),
                        isFunction
                                ? "org.mozilla.javascript.optimizer.OptJSFunctionCode"
                                : "org.mozilla.javascript.optimizer.OptJSScriptCode",
                        sourceFile);
        installConstantDescribers(cfw);
        generateOptJSCodeCtor(cfw, isFunction);
        generateOptJSCodeExecute(cfw, mainClass, methodName, methodType);
        generateOptJSCodeResume(cfw, mainClass, resumeName, GENERATOR_METHOD_SIGNATURE);
        return cfw.toByteArray();
    }

    /** Teach a writer about the runtime types that can be emitted as dynamic constants. */
    private static void installConstantDescribers(ClassFileWriter cfw) {
        cfw.registerDynamicConstantDescriber(new SymbolKeyDescriber());
        cfw.registerDynamicConstantDescriber(new EagerSourceCodeProviderDescriber());
        cfw.registerDynamicConstantDescriber(new UndefinedDescriber());
        cfw.registerDynamicConstantDescriber(new TemplateLiteralCallSiteDescriber());

        cfw.registerStringConcat(
            ConstantDescs.ofConstantBootstrap(
                    ClassDesc.of(StringConcatBootstraps.class.getName()),
                    "concat",
                    ConstantDescs.CD_String,
                    ConstantDescs.CD_String.arrayType()));
    }

    private static void generateOptJSCodeCtor(ClassFileWriter cfw, boolean isFunction) {
        cfw.startMethod("<init>", "()V", ACC_PUBLIC);
        cfw.addALoad(0);
        cfw.addInvoke(
                ByteCode.INVOKESPECIAL,
                isFunction
                        ? "org.mozilla.javascript.optimizer.OptJSFunctionCode"
                        : "org.mozilla.javascript.optimizer.OptJSScriptCode",
                "<init>",
                "()V");
        cfw.add(ByteCode.RETURN);
        cfw.stopMethod(1);
    }

    private static void generateOptJSCodeExecute(
            ClassFileWriter cfw, String mainClass, String methodName, String methodType) {
        cfw.startMethod("execute", methodType, (short) (ACC_PUBLIC | ACC_FINAL));
        cfw.addALoad(1);
        cfw.addALoad(2);
        cfw.addALoad(3);
        cfw.addALoad(4);
        cfw.addALoad(5);
        cfw.addALoad(6);
        cfw.addInvoke(ByteCode.INVOKESTATIC, mainClass, methodName, methodType);
        cfw.add(ByteCode.ARETURN);
        cfw.stopMethod(7);
        // 5: this, cx, js function, new.target, scope, js this, args[]
    }

    private static void generateOptJSCodeResume(
            ClassFileWriter cfw, String mainClass, String methodName, String methodType) {
        cfw.startMethod("resume", methodType, (short) (ACC_PUBLIC | ACC_FINAL));
        if (methodName == null) {
            cfw.add(ByteCode.ACONST_NULL);
        } else {
            cfw.addALoad(1);
            cfw.addALoad(2);
            cfw.addALoad(3);
            cfw.addALoad(4);
            cfw.addILoad(5);
            cfw.addALoad(6);
            cfw.addInvoke(ByteCode.INVOKESTATIC, mainClass, methodName, methodType);
        }
        cfw.add(ByteCode.ARETURN);
        cfw.stopMethod(7);
        // 5: this, cx, js function, new.target, scope, js this, args[]
    }

    /**
     * Generate one class file per entry in {@code chunkEnvs}/{@code chunkClassNames} (in the same
     * order), placing each script/function body in {@link #scriptOrFnNodes} into whichever chunk
     * its {@link MHJSCode.Builder} was bound to (see {@link #rebindBuildersToChunks}). Numeric
     * constants, template literals and the descriptors array are always centralized on chunk 0 (the
     * main class), since those are read via a hardcoded {@link #mainClassName} reference from
     * whichever chunk needs them.
     *
     * @throws ClassFileWriter.ClassSizeException if any single chunk overflows a JVM class file
     *     limit; the caller decides whether to retry with more, smaller chunks.
     */
    private byte[][] generateCode(
            String rawSource, MHJSCode.BuilderEnv[] chunkEnvs, String[] chunkClassNames) {
        String sourceFile = scriptOrFnNodes[0].getSourceName();

        int numChunks = chunkEnvs.length;
        ClassFileWriter[] cfws = new ClassFileWriter[numChunks];
        IdentityHashMap<MHJSCode.BuilderEnv, ClassFileWriter> cfwForEnv = new IdentityHashMap<>();
        for (int c = 0; c != numChunks; ++c) {
            cfws[c] = new ClassFileWriter(chunkClassNames[c], SUPER_CLASS_NAME, sourceFile);
            installConstantDescribers(cfws[c]);
            generateLookupAccessor(cfws[c]);
            cfwForEnv.put(chunkEnvs[c], cfws[c]);
        }
        ClassFileWriter mainCfw = cfws[0];

        prepareRegExpConstants(cfws, chunkEnvs);
        prepareTemplateLiterals(cfws, chunkEnvs);
        mainCfw.addField(ID_FIELD_NAME, "I", ACC_PRIVATE);
        mainCfw.addField(
                DESCRIPTORS_FIELD_NAME,
                "[Lorg/mozilla/javascript/JSDescriptor;",
                (short) (ACC_PUBLIC | ACC_STATIC));

        int count = scriptOrFnNodes.length;
        for (int i = 0; i != count; ++i) {
            ScriptNode n = scriptOrFnNodes[i];
            n.clearAllLabelIds();
            MHJSCode.BuilderEnv env = ((MHJSCode.Builder<?>) builders[i].code).env;
            ClassFileWriter cfw = cfwForEnv.get(env);

            BodyCodegen bodygen = new BodyCodegen();
            bodygen.cfw = cfw;
            bodygen.codegen = this;
            bodygen.compilerEnv = compilerEnv;
            bodygen.scriptOrFn = n;
            bodygen.scriptOrFnIndex = i;
            if (n instanceof FunctionNode) {
                bodygen.scriptOrFnType = "Lorg/mozilla/javascript/JSFunction;";
                bodygen.scriptOrFnClass = "org.mozilla.javascript.JSFunction";
            } else {
                bodygen.scriptOrFnType = "Lorg/mozilla/javascript/JSScript;";
                bodygen.scriptOrFnClass = "org.mozilla.javascript.JSScript";
            }

            bodygen.generateBodyCode();

            if (n.getType() == Token.FUNCTION) {
                OptFunctionNode ofn = OptFunctionNode.get(n);
                if (ofn.isTargetOfDirectCall()) {
                    emitDirectConstructor(cfw, ofn);
                    int pcount = ofn.fnode.getParamCount();
                    if (pcount != 0) {
                        emitNonDirectCall(cfw, ofn);
                    }
                }
            }
        }

        emitConstantDudeInitializers(mainCfw);

        byte[][] result = new byte[numChunks][];
        for (int c = 0; c != numChunks; ++c) {
            result[c] = cfws[c].toByteArray();
        }
        return result;
    }

    private void emitNonDirectCall(ClassFileWriter cfw, OptFunctionNode ofn) {
        // We'll make a method with the same name as the body method but with a non direct
        // signature.

        cfw.startMethod(
                getBodyMethodName(ofn.fnode),
                getNonDirectBodyMethodSIgnature(ofn.fnode),
                (short) (ACC_STATIC | ACC_PUBLIC));

        cfw.addALoad(0);
        cfw.addALoad(1);
        cfw.addALoad(2);
        cfw.addALoad(3);
        cfw.addALoad(4);
        cfw.addALoad(5);
        int pcount = ofn.fnode.getParamCount();
        if (pcount != 0) {
            // loop invariant:
            // stack top == arguments array from addALoad4()
            for (int p = 0; p != pcount; ++p) {
                cfw.add(ByteCode.ARRAYLENGTH);
                cfw.addPush(p);
                int undefArg = cfw.acquireLabel();
                int beyond = cfw.acquireLabel();
                cfw.add(ByteCode.IF_ICMPLE, undefArg);
                // get array[p]
                cfw.addALoad(5);
                cfw.addPush(p);
                cfw.add(ByteCode.AALOAD);
                cfw.add(ByteCode.GOTO, beyond);
                cfw.markLabel(undefArg);
                pushUndefined(cfw);
                cfw.markLabel(beyond);
                // Only one push
                cfw.adjustStackTop(-1);
                cfw.addPush(0.0);
                // restore invariant
                cfw.addALoad(5);
            }
        }

        cfw.addInvoke(
                ByteCode.INVOKESTATIC,
                cfw.getClassName(),
                getBodyMethodName(ofn.fnode),
                getBodyMethodSignature(ofn.fnode));
        cfw.add(ByteCode.ARETURN);

        cfw.stopMethod(6);
        // 5: function, cx, scope, js this, args[]
    }

    private void emitDirectConstructor(ClassFileWriter cfw, OptFunctionNode ofn) {
        /*
            we generate ..
                Scriptable directConstruct(<directCallArgs>) {
                    Scriptable newInstance = createObject(cx, scope);
                    Object val = <body-name>(cx, scope, newInstance, <directCallArgs>);
                    if (val instanceof Scriptable) {
                        return (Scriptable) val;
                    }
                    return newInstance;
                }
        */
        cfw.startMethod(
                getDirectCtorName(ofn.fnode),
                getBodyMethodSignature(ofn.fnode),
                (short) (ACC_STATIC | ACC_PUBLIC));

        int argCount = ofn.fnode.getParamCount();
        int firstLocal = (5 + argCount * 3) + 1;

        emitTrace(cfw, "TRACE emitDirectConstructor body: " + cleanName(ofn.fnode));

        cfw.addALoad(1); // this
        cfw.add(ByteCode.CHECKCAST, Codegen.JSFUNCTION_CLASS_NAME);
        cfw.addALoad(0); // cx
        cfw.addALoad(3); // scope
        cfw.addInvoke(
                ByteCode.INVOKEVIRTUAL,
                "org/mozilla/javascript/BaseFunction",
                "createObject",
                "(Lorg/mozilla/javascript/Context;"
                        + "Lorg/mozilla/javascript/VarScope;"
                        + ")Lorg/mozilla/javascript/Scriptable;");
        cfw.addAStore(firstLocal);

        cfw.addALoad(0); // context
        cfw.addALoad(1); // this function
        cfw.addALoad(2); // new.target will be this function
        cfw.addALoad(3); // Scope
        cfw.addALoad(firstLocal); // thisObj - object created above.
        for (int i = 0; i < argCount; i++) {
            cfw.addALoad(5 + (i * 3));
            cfw.addDLoad(6 + (i * 3));
        }
        cfw.addALoad(5 + argCount * 3);
        cfw.addInvoke(
                ByteCode.INVOKESTATIC,
                cfw.getClassName(),
                getBodyMethodName(ofn.fnode),
                getBodyMethodSignature(ofn.fnode));
        int exitLabel = cfw.acquireLabel();
        cfw.add(ByteCode.DUP); // make a copy of direct call result
        cfw.add(ByteCode.INSTANCEOF, "org/mozilla/javascript/Scriptable");
        cfw.add(ByteCode.IFNE, exitLabel);
        // If the constructor did not return a Scriptable we pass back the `this` we passed in.
        cfw.add(ByteCode.POP);
        cfw.addALoad(firstLocal);
        cfw.markLabel(exitLabel);
        cfw.add(ByteCode.ARETURN);

        cfw.stopMethod((short) (firstLocal + 1));
    }

    static final boolean DEBUG_DIRECT_CALLS = RhinoConfig.get("rhino.debugDirectCalls", false);

    /**
     * Emits code that prints the given message when the surrounding generated code is reached. Only
     * used to check which of the direct call paths a script actually takes.
     */
    static void emitTrace(ClassFileWriter cfw, String message) {
        if (!DEBUG_DIRECT_CALLS) return;
        cfw.add(ByteCode.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
        cfw.addPush(message);
        cfw.addInvoke(
                ByteCode.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
    }

    static boolean isGenerator(ScriptNode node) {
        return (node.getType() == Token.FUNCTION) && ((FunctionNode) node).isGenerator();
    }

    private void generateLookupAccessor(ClassFileWriter cfw) {
        cfw.startMethod(
                "getLookup",
                "()Ljava/lang/invoke/MethodHandles$Lookup;",
                (short) (ACC_STATIC | ACC_PUBLIC));
        cfw.addInvoke(
                ByteCode.INVOKESTATIC,
                "java.lang.invoke.MethodHandles",
                "lookup",
                "()Ljava/lang/invoke/MethodHandles$Lookup;");
        cfw.add(ByteCode.ARETURN);
        cfw.stopMethod(0);
    }

    /**
     * Compile the regexp literals ahead of time so that they can be written to the class file as
     * dynamic constants, and register the describers that will write them.
     *
     * <p>Preparing them here is what lets the constants be resolved later without a context: the
     * regexp implementation reports anything wrong with an expression now, while there is still a
     * script being compiled to report it against.
     */
    private void prepareRegExpConstants(ClassFileWriter[] cfws, MHJSCode.BuilderEnv[] chunkEnvs) {
        boolean anyRegExpLiterals = false;
        for (ScriptNode n : scriptOrFnNodes) {
            if (n.getRegexpCount() > 0) {
                anyRegExpLiterals = true;
                break;
            }
        }
        if (!anyRegExpLiterals) {
            return;
        }
        Context cx = Context.getCurrentContext();
        RegExpProxy proxy = cx == null ? null : ScriptRuntime.getRegExpProxy(cx);
        if (proxy == null) {
            throw new IllegalStateException(
                    "Compilation of regexps literals requires an enclosing context.");
        }

        DynamicConstant[][] constants = new DynamicConstant[scriptOrFnNodes.length][];
        for (int i = 0; i != scriptOrFnNodes.length; ++i) {
            ScriptNode n = scriptOrFnNodes[i];
            int regCount = n.getRegexpCount();
            constants[i] = new DynamicConstant[regCount];
            for (int j = 0; j != regCount; ++j) {
                Object constant =
                        proxy.prepareRegExpConstant(cx, n.getRegexpString(j), n.getRegexpFlags(j));
                if (!(constant instanceof DynamicConstant)) {
                    // This implementation has no constant form, so compile every literal of this
                    // class at run time rather than mixing the two ways of doing it.
                    return;
                }
                constants[i][j] = (DynamicConstant) constant;
            }
        }

        for (DynamicConstantDescriber<?> describer : proxy.getDynamicConstantDescribers()) {
            for (ClassFileWriter cfw : cfws) {
                cfw.registerDynamicConstantDescriber(describer);
            }
        }
        regExpConstants = constants;
        // With the literals in the constant pool there is nothing left to initialize.
        for (MHJSCode.BuilderEnv env : chunkEnvs) {
            env.hasRegExpLiterals = false;
        }
    }

    /**
     * Compile the regexp literals ahead of time so that they can be written to the class file as
     * dynamic constants, and register the describers that will write them.
     *
     * <p>Preparing them here is what lets the constants be resolved later without a context: the
     * regexp implementation reports anything wrong with an expression now, while there is still a
     * script being compiled to report it against.
     */
    private void prepareTemplateLiterals(ClassFileWriter[] cfws, MHJSCode.BuilderEnv[] chunkEnvs) {
        boolean anyTemplateLiterals = false;
        for (ScriptNode n : scriptOrFnNodes) {
            if (n.getTemplateLiteralCount() > 0) {
                anyTemplateLiterals = true;
                break;
            }
        }
        if (!anyTemplateLiterals) {
            return;
        }

        DynamicConstant[][] constants = new DynamicConstant[scriptOrFnNodes.length][];
        for (int i = 0; i != scriptOrFnNodes.length; ++i) {
            ScriptNode n = scriptOrFnNodes[i];
            int tlCount = n.getTemplateLiteralCount();
            constants[i] = new DynamicConstant[tlCount];
            for (int j = 0; j != tlCount; ++j) {
                Object constant = n.getTemplateLiteralObj(j);
                if (!(constant instanceof DynamicConstant)) {
                    // This implementation has no constant form, so compile every literal of this
                    // class at run time rather than mixing the two ways of doing it.
                    throw new IllegalStateException("");
                }
                constants[i][j] = (DynamicConstant) constant;
            }
        }

        templateLiteralConstants = constants;
        // With the literals in the constant pool there is nothing left to initialize.
        for (MHJSCode.BuilderEnv env : chunkEnvs) {
            env.hasTemplateLiterals = false;
        }
    }

    /** The prepared constant for a regexp literal, or null if it is compiled at run time. */
    DynamicConstant getRegExpConstant(ScriptNode n, int regexpIndex) {
        return regExpConstants == null ? null : regExpConstants[getIndex(n)][regexpIndex];
    }

    /** The prepared constant for a regexp literal, or null if it is compiled at run time. */
    DynamicConstant getTemplateLiteralConstant(ScriptNode n, int regexpIndex) {
        return templateLiteralConstants == null ? null : templateLiteralConstants[getIndex(n)][regexpIndex];
    }

    private void emitConstantDudeInitializers(ClassFileWriter cfw) {
        int N = itsConstantListSize;
        if (N == 0) return;

        cfw.startMethod("<clinit>", "()V", (short) (ACC_STATIC | ACC_FINAL));

        double[] array = itsConstantList;
        for (int i = 0; i != N; ++i) {
            double num = array[i];
            String constantName = "_k" + i;
            String constantType = getStaticConstantWrapperType(num);
            // Public: a body method that ends up in a different (split) chunk class needs to
            // read this field via a cross-class GETSTATIC.
            cfw.addField(constantName, constantType, (short) (ACC_STATIC | ACC_PUBLIC));
            int inum = (int) num;
            if (inum == num) {
                cfw.addPush(inum);
                cfw.addInvoke(
                        ByteCode.INVOKESTATIC,
                        "java/lang/Integer",
                        "valueOf",
                        "(I)Ljava/lang/Integer;");
            } else {
                cfw.addPush(num);
                addDoubleWrap(cfw);
            }
            cfw.add(ByteCode.PUTSTATIC, mainClassName, constantName, constantType);
        }

        cfw.add(ByteCode.RETURN);
        cfw.stopMethod(0);
    }

    void pushNumberAsObject(ClassFileWriter cfw, double num) {
        if (num == 0.0) {
            if (1 / num > 0) {
                // +0.0
                cfw.add(
                        ByteCode.GETSTATIC,
                        "org/mozilla/javascript/ScriptRuntime",
                        "zeroObj",
                        "Ljava/lang/Integer;");
            } else {
                cfw.addPush(num);
                addDoubleWrap(cfw);
            }

        } else if (num == 1.0) {
            cfw.add(
                    ByteCode.GETSTATIC,
                    "org/mozilla/javascript/optimizer/OptRuntime",
                    "oneObj",
                    "Ljava/lang/Integer;");
            return;

        } else if (num == -1.0) {
            cfw.add(
                    ByteCode.GETSTATIC,
                    "org/mozilla/javascript/optimizer/OptRuntime",
                    "minusOneObj",
                    "Ljava/lang/Integer;");

        } else if (Double.isNaN(num)) {
            cfw.add(
                    ByteCode.GETSTATIC,
                    "org/mozilla/javascript/ScriptRuntime",
                    "NaNobj",
                    "Ljava/lang/Double;");

        } else if (itsConstantListSize >= 2000) {
            // There appears to be a limit in the JVM on either the number
            // of static fields in a class or the size of the class
            // initializer. Either way, we can't have any more than 2000
            // statically init'd constants.
            cfw.addPush(num);
            addDoubleWrap(cfw);

        } else {
            int N = itsConstantListSize;
            int index = 0;
            if (N == 0) {
                itsConstantList = new double[64];
            } else {
                double[] array = itsConstantList;
                while (index != N && array[index] != num) {
                    ++index;
                }
                if (N == array.length) {
                    array = new double[N * 2];
                    System.arraycopy(itsConstantList, 0, array, 0, N);
                    itsConstantList = array;
                }
            }
            if (index == N) {
                itsConstantList[N] = num;
                itsConstantListSize = N + 1;
            }
            String constantName = "_k" + index;
            String constantType = getStaticConstantWrapperType(num);
            cfw.add(ByteCode.GETSTATIC, mainClassName, constantName, constantType);
        }
    }

    private static void addDoubleWrap(ClassFileWriter cfw) {
        cfw.addInvoke(
                ByteCode.INVOKESTATIC,
                "org/mozilla/javascript/optimizer/OptRuntime",
                "wrapDouble",
                "(D)Ljava/lang/Double;");
    }

    private static String getStaticConstantWrapperType(double num) {
        int inum = (int) num;
        if (inum == num) {
            return "Ljava/lang/Integer;";
        }
        return "Ljava/lang/Double;";
    }

    static void pushUndefined(ClassFileWriter cfw) {
        cfw.addLoadDynamicConstant(Undefined.instance);
    }

    int getIndex(ScriptNode n) {
        return scriptOrFnIndexes.get(n);
    }

    String getDirectCtorName(ScriptNode n) {
        return "_n" + getIndex(n);
    }

    String getBodyMethodName(ScriptNode n) {
        return getBodyMethodName(n, getIndex(n));
    }

    String getBodyMethodName(ScriptNode n, int index) {
        return "_c_" + cleanName(n) + "_" + index;
    }

    /**
     * List of illegal characters in unqualified names as specified in
     * https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-4.html#jvms-4.2.2
     */
    private static Pattern illegalChars = Pattern.compile("[.;\\[/<>]");

    /** Gets a Java-compatible "informative" name for the ScriptOrFnNode */
    String cleanName(final ScriptNode n) {
        String result = "";
        if (n instanceof FunctionNode) {
            Name name = ((FunctionNode) n).getFunctionName();
            if (name == null) {
                result = "anonymous";
            } else {
                result = name.getIdentifier();
            }
        } else {
            result = "script";
        }
        return illegalChars.matcher(result).replaceAll("_");
    }

    String getNonDirectBodyMethodSIgnature(ScriptNode n) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append("Lorg/mozilla/javascript/Context;");
        if (n instanceof FunctionNode) {
            sb.append("Lorg/mozilla/javascript/JSFunction;");
        } else {
            sb.append("Lorg/mozilla/javascript/JSScript;");
        }
        sb.append(
                "Ljava/lang/Object;" + "Lorg/mozilla/javascript/VarScope;" + "Ljava/lang/Object;");
        sb.append("[Ljava/lang/Object;)Ljava/lang/Object;");
        return sb.toString();
    }

    String getBodyMethodSignature(ScriptNode n) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append("Lorg/mozilla/javascript/Context;");
        if (n instanceof FunctionNode) {
            sb.append("Lorg/mozilla/javascript/JSFunction;");
        } else {
            sb.append("Lorg/mozilla/javascript/JSScript;");
        }
        sb.append(
                "Ljava/lang/Object;" + "Lorg/mozilla/javascript/VarScope;" + "Ljava/lang/Object;");
        if (n.getType() == Token.FUNCTION) {
            OptFunctionNode ofn = OptFunctionNode.get(n);
            if (ofn.isTargetOfDirectCall()) {
                int pCount = ofn.fnode.getParamCount();
                for (int i = 0; i != pCount; i++) {
                    sb.append("Ljava/lang/Object;D");
                }
            }
        }
        sb.append("[Ljava/lang/Object;)Ljava/lang/Object;");
        return sb.toString();
    }

    String getCodeInitMethodName(OptFunctionNode ofn) {
        return "_i" + getIndex(ofn.fnode);
    }

    static RuntimeException badTree() {
        throw new RuntimeException("Bad tree in codegen");
    }

    public void setMainMethodClass(String className) {
        mainMethodClass = className;
    }

    static final String DEFAULT_MAIN_METHOD_CLASS = "org.mozilla.javascript.optimizer.OptRuntime";

    private static final String SUPER_CLASS_NAME = "java.lang.Object";

    static final String ID_FIELD_NAME = "_id";

    static final String DESCRIPTOR_CLASS_SIGNATURE = "Lorg/mozilla/javascript/JSDescriptor;";
    static final String DESCRIPTORS_FIELD_NAME = "_descriptors";
    static final String DESCRIPTORS_FIELD_SIGNATURE = "[" + DESCRIPTOR_CLASS_SIGNATURE;

    static final String CODE_INIT_SIGNATURE = "(Lorg/mozilla/javascript/Context;" + ")V";

    static final String CODE_CONSTRUCTOR_SIGNATURE = "(Lorg/mozilla/javascript/Context;I)V";

    static final String JSFUNCTION_CLASS_NAME = "org.mozilla.javascript.JSFunction";
    static final String JSFUNCTION_CLASS_SIGNATURE = "org/mozilla/javascript/JSFunction";
    static final String JSFUNCTION_CONSTRUCTOR_SIGNATURE =
            "("
                    + "Lorg/mozilla/javascript/Context;"
                    + "Lorg/mozilla/javascript/VarScope;"
                    + "Lorg/mozilla/javascript/JSDescriptor;"
                    + "Ljava/lang/Object;"
                    + "Ljava/lang/Object;"
                    + "Lorg/mozilla/javascript/Scriptable;"
                    + ")V";

    static final String GENERATOR_METHOD_SIGNATURE =
            "("
                    + "Lorg/mozilla/javascript/Context;"
                    + "Lorg/mozilla/javascript/JSFunction;"
                    + "Ljava/lang/Object;"
                    + "Lorg/mozilla/javascript/VarScope;"
                    + "I"
                    + "Ljava/lang/Object;)Ljava/lang/Object;";

    private static final Object globalLock = new Object();
    private static int globalSerialClassCounter;

    private CompilerEnvirons compilerEnv;

    private List<OptFunctionNode> directCallTargets;
    ScriptNode[] scriptOrFnNodes;
    JSDescriptor.Builder[] builders;
    private HashMap<ScriptNode, Integer> scriptOrFnIndexes;

    private String mainMethodClass = DEFAULT_MAIN_METHOD_CLASS;

    String mainClassName;
    String mainClassSignature;

    /**
     * The prepared form of every regexp literal, indexed as {@link #scriptOrFnNodes} is and then by
     * the index of the literal, or null when the literals are compiled at run time instead.
     */
    private DynamicConstant[][] regExpConstants;
    private DynamicConstant[][] templateLiteralConstants;

    private double[] itsConstantList;
    private int itsConstantListSize;
}
