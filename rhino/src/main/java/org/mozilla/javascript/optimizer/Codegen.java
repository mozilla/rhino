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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.CodeGenUtils;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Evaluator;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.GeneratedClassLoader;
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
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.RegExpLiteral;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.ast.TemplateCharacters;
import org.mozilla.javascript.ast.TemplateLiteral;

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

    private static class CompilationResult<T extends ScriptOrFn<T>> {
        final JSDescriptor.Builder<T> builder;
        final String className;
        final byte[] bytecode;
        final OptJSCode.BuilderEnv builderEnv;

        CompilationResult(
                JSDescriptor.Builder<T> builder,
                String className,
                byte[] bytecode,
                OptJSCode.BuilderEnv builderEnv) {
            this.builder = builder;
            this.className = className;
            this.bytecode = bytecode;
            this.builderEnv = builderEnv;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object compile(
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

        JSDescriptor.Builder<?> builder = new JSDescriptor.Builder();
        OptJSCode.BuilderEnv builderEnv = new OptJSCode.BuilderEnv(mainClassName);
        byte[] mainClassBytes =
                compileToClassFile(
                        compilerEnv,
                        builder,
                        builderEnv,
                        mainClassName,
                        tree,
                        rawSource,
                        returnFunction);

        return new CompilationResult(builder, mainClassName, mainClassBytes, builderEnv);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Script createScriptObject(Object bytecode, Object staticSecurityDomain) {
        JSDescriptor<JSScript> desc =
                defineClass((CompilationResult<JSScript>) bytecode, staticSecurityDomain);
        return JSFunction.createScript(desc, null, staticSecurityDomain);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Function createFunctionObject(
            Context cx, VarScope scope, Object bytecode, Object staticSecurityDomain) {
        JSDescriptor<JSFunction> desc =
                defineClass((CompilationResult<JSFunction>) bytecode, staticSecurityDomain);
        return JSFunction.createFunction(cx, scope, desc, null, staticSecurityDomain);
    }

    private <T extends ScriptOrFn<T>> JSDescriptor<T> defineClass(
            CompilationResult<T> compiled, Object staticSecurityDomain) {
        // The generated classes in this case refer only to Rhino classes
        // which must be accessible through this class loader
        ClassLoader rhinoLoader = getClass().getClassLoader();
        GeneratedClassLoader loader;
        loader = SecurityController.createLoader(rhinoLoader, staticSecurityDomain);
        Exception e;
        try {
            Class<?> cl = loader.defineClass(compiled.className, compiled.bytecode);
            loader.linkClass(cl);
            compiled.builderEnv.compiledClass = cl;
            var descs = new ArrayList<JSDescriptor<?>>();
            JSDescriptor<T> desc = compiled.builder.build(d -> descs.add(d));
            cl.getField(DESCRIPTORS_FIELD_NAME).set(null, descs.toArray(new JSDescriptor[0]));
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
            OptJSCode.BuilderEnv builderEnv,
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

        initScriptNodesData(scriptOrFn, builder, builderEnv);

        return generateCode(rawSource);
    }

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
            OptJSCode.BuilderEnv builderEnv) {
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

    private static void populateLiterals(ScriptNode n, JSDescriptor.Builder<?> builder) {
        int count = n.getLiteralCount();
        if (count == 0) return;

        Context cx = null;
        RegExpProxy rep = null;
        Object[] array = new Object[count];
        for (int i = 0; i != count; ++i) {
            var literal = n.getLiteral(i);
            if (literal instanceof RegExpLiteral) {
                if (rep == null) {
                    cx = Context.getCurrentContext();
                    rep = ScriptRuntime.checkRegExpProxy(cx);
                }
                var re = (RegExpLiteral) literal;
                array[i] = rep.compileRegExp(cx, re.getValue(), re.getFlags());
            } else if (literal instanceof TemplateLiteral) {
                List<TemplateCharacters> strings = ((TemplateLiteral) literal).getTemplateStrings();
                String[] values = new String[strings.size() * 2];
                int j = 0;
                for (TemplateCharacters s : strings) {
                    values[j++] = s.getValue();
                    values[j++] = s.getRawValue();
                }
                array[i] = values;
            } else {
                array[i] = literal;
            }
        }
        builder.literals = array;
    }

    private <U extends ScriptOrFn<U>> void collectScriptNodes_r(
            ScriptNode n,
            JSDescriptor.Builder<U> builder,
            OptJSCode.BuilderEnv builderEnv,
            List<ScriptNode> x,
            List<JSDescriptor.Builder<?>> b) {

        @SuppressWarnings("unchecked")
        OptJSCode.Builder<U> code =
                (OptJSCode.Builder<U>)
                        ((n instanceof FunctionNode)
                                ? new OptJSFunctionCode.Builder(builderEnv)
                                : new OptJSScriptCode.Builder(builderEnv));
        code.index = x.size();
        code.methodName = getBodyMethodName(n, x.size());
        code.methodType = getNonDirectBodyMethodSIgnature(n);
        if (isGenerator(n)) {
            code.resumeName = code.methodName + "_gen";
            code.resumeType = GENERATOR_METHOD_SIGNATURE;
        }
        builder.setCode(code);
        populateLiterals(n, builder);
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
        generateOptJSCodeCtor(cfw, isFunction);
        generateOptJSCodeExecute(cfw, mainClass, methodName, methodType);
        generateOptJSCodeResume(cfw, mainClass, resumeName, GENERATOR_METHOD_SIGNATURE);
        return cfw.toByteArray();
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

    private byte[] generateCode(String rawSource) {
        boolean hasScript = (scriptOrFnNodes[0].getType() == Token.SCRIPT);
        boolean hasFunctions = (scriptOrFnNodes.length > 1 || !hasScript);
        boolean isStrictMode = scriptOrFnNodes[0].isInStrictMode();

        String sourceFile = scriptOrFnNodes[0].getSourceName();
        ClassFileWriter cfw = new ClassFileWriter(mainClassName, SUPER_CLASS_NAME, sourceFile);
        cfw.addField(ID_FIELD_NAME, "I", ACC_PRIVATE);
        cfw.addField(
                DESCRIPTORS_FIELD_NAME,
                "[Lorg/mozilla/javascript/JSDescriptor;",
                (short) (ACC_PUBLIC | ACC_STATIC));

        generateLookupAccessor(cfw);

        int count = scriptOrFnNodes.length;
        for (int i = 0; i != count; ++i) {
            ScriptNode n = scriptOrFnNodes[i];

            BodyCodegen bodygen = new BodyCodegen(cfw, this, compilerEnv, n, i);

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

        emitConstantDudeInitializers(cfw);

        return cfw.toByteArray();
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
                mainClassName,
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

        cfw.addALoad(0);
        cfw.addALoad(1);
        cfw.add(ByteCode.ACONST_NULL);
        cfw.addALoad(2);
        cfw.addALoad(firstLocal);
        for (int i = 0; i < argCount; i++) {
            cfw.addALoad(5 + (i * 3));
            cfw.addDLoad(6 + (i * 3));
        }
        cfw.addALoad(5 + argCount * 3);
        cfw.addInvoke(
                ByteCode.INVOKESTATIC,
                mainClassName,
                getBodyMethodName(ofn.fnode),
                getBodyMethodSignature(ofn.fnode));
        int exitLabel = cfw.acquireLabel();
        cfw.add(ByteCode.DUP); // make a copy of direct call result
        cfw.add(ByteCode.INSTANCEOF, "org/mozilla/javascript/Scriptable");
        cfw.add(ByteCode.IFEQ, exitLabel);
        // cast direct call result
        cfw.add(ByteCode.CHECKCAST, "org/mozilla/javascript/Scriptable");
        cfw.add(ByteCode.ARETURN);
        cfw.markLabel(exitLabel);

        cfw.addALoad(firstLocal);
        cfw.add(ByteCode.ARETURN);

        cfw.stopMethod((short) (firstLocal + 1));
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

    private void emitConstantDudeInitializers(ClassFileWriter cfw) {
        int N = itsConstantListSize;
        if (N == 0) return;

        cfw.startMethod("<clinit>", "()V", (short) (ACC_STATIC | ACC_FINAL));

        double[] array = itsConstantList;
        for (int i = 0; i != N; ++i) {
            double num = array[i];
            String constantName = "_k" + i;
            String constantType = getStaticConstantWrapperType(num);
            cfw.addField(constantName, constantType, (short) (ACC_STATIC | ACC_PRIVATE));
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
        cfw.add(
                ByteCode.GETSTATIC,
                "org/mozilla/javascript/Undefined",
                "instance",
                "Ljava/lang/Object;");
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

    private double[] itsConstantList;
    private int itsConstantListSize;
}
