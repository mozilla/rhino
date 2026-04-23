/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.Symbol;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.Token;

/**
 * AST node for ES6 class declarations and expressions.
 *
 * <pre>
 * <i>ClassDeclaration</i> :
 *        <b>class</b> BindingIdentifier ClassTail
 * <i>ClassExpression</i> :
 *        <b>class</b> BindingIdentifieropt ClassTail
 * <i>ClassTail</i> :
 *        ClassHeritageopt { ClassBody }
 * </pre>
 */
public class ClassNode extends AstNode {

    /** Kind of a class member that is a function: normal method, getter, or setter. */
    public enum MethodKind {
        METHOD,
        GETTER,
        SETTER
    }

    private Name className;
    private AstNode superClass;
    private FunctionNode constructor;
    private boolean isStatement;
    private List<String> methodNames;
    private List<FunctionNode> methods;
    private List<MethodKind> methodKinds;
    private List<String> staticMethodNames;
    private List<FunctionNode> staticMethods;
    private List<MethodKind> staticMethodKinds;
    private List<String> fieldNames;
    private List<AstNode> fieldInitializers;
    private List<AstNode> computedFieldKeys;
    private List<AstNode> computedFieldInitializers;
    private List<String> staticFieldNames;
    private List<AstNode> staticFieldInitializers;
    private List<AstNode> staticComputedFieldKeys;
    private List<AstNode> staticComputedFieldInitializers;
    private List<String> privateFieldNames;
    private List<AstNode> privateFieldInitializers;
    private List<MethodKind> privateFieldKinds;
    private List<String> staticPrivateFieldNames;
    private List<AstNode> staticPrivateFieldInitializers;
    private List<MethodKind> staticPrivateFieldKinds;

    /** Shared SymbolKey for each private name declared in this class. */
    private Map<String, SymbolKey> privateSymbols;

    {
        type = Token.CLASS;
    }

    public ClassNode() {}

    public ClassNode(int pos) {
        super(pos);
    }

    public ClassNode(int pos, int len) {
        super(pos, len);
    }

    public Name getClassName() {
        return className;
    }

    public void setClassName(Name className) {
        this.className = className;
        if (className != null) {
            className.setParent(this);
        }
    }

    public AstNode getSuperClass() {
        return superClass;
    }

    public void setSuperClass(AstNode superClass) {
        this.superClass = superClass;
        if (superClass != null) {
            superClass.setParent(this);
        }
    }

    public FunctionNode getConstructor() {
        return constructor;
    }

    public void setConstructor(FunctionNode constructor) {
        this.constructor = constructor;
        if (constructor != null) {
            constructor.setParent(this);
        }
    }

    public boolean isStatement() {
        return isStatement;
    }

    public void setIsStatement(boolean isStatement) {
        this.isStatement = isStatement;
    }

    public void addMethod(String name, FunctionNode fn) {
        addMethod(name, fn, MethodKind.METHOD);
    }

    public void addMethod(String name, FunctionNode fn, MethodKind kind) {
        if (methodNames == null) {
            methodNames = new ArrayList<>();
            methods = new ArrayList<>();
            methodKinds = new ArrayList<>();
        }
        methodNames.add(name);
        methods.add(fn);
        methodKinds.add(kind);
        fn.setParent(this);
    }

    public int getMethodCount() {
        return methodNames == null ? 0 : methodNames.size();
    }

    public List<String> getMethodNames() {
        return methodNames == null ? Collections.emptyList() : methodNames;
    }

    public List<FunctionNode> getMethods() {
        return methods == null ? Collections.emptyList() : methods;
    }

    public List<MethodKind> getMethodKinds() {
        return methodKinds == null ? Collections.emptyList() : methodKinds;
    }

    public void addStaticMethod(String name, FunctionNode fn) {
        addStaticMethod(name, fn, MethodKind.METHOD);
    }

    public void addStaticMethod(String name, FunctionNode fn, MethodKind kind) {
        if (staticMethodNames == null) {
            staticMethodNames = new ArrayList<>();
            staticMethods = new ArrayList<>();
            staticMethodKinds = new ArrayList<>();
        }
        staticMethodNames.add(name);
        staticMethods.add(fn);
        staticMethodKinds.add(kind);
        fn.setParent(this);
    }

    public int getStaticMethodCount() {
        return staticMethodNames == null ? 0 : staticMethodNames.size();
    }

    public List<String> getStaticMethodNames() {
        return staticMethodNames == null ? Collections.emptyList() : staticMethodNames;
    }

    public List<FunctionNode> getStaticMethods() {
        return staticMethods == null ? Collections.emptyList() : staticMethods;
    }

    public List<MethodKind> getStaticMethodKinds() {
        return staticMethodKinds == null ? Collections.emptyList() : staticMethodKinds;
    }

    public void addField(String name, AstNode initializer) {
        if (fieldNames == null) {
            fieldNames = new ArrayList<>();
            fieldInitializers = new ArrayList<>();
        }
        fieldNames.add(name);
        fieldInitializers.add(initializer);
        if (initializer != null) {
            initializer.setParent(this);
        }
    }

    public int getFieldCount() {
        return fieldNames == null ? 0 : fieldNames.size();
    }

    public List<String> getFieldNames() {
        return fieldNames == null ? Collections.emptyList() : fieldNames;
    }

    public List<AstNode> getFieldInitializers() {
        return fieldInitializers == null ? Collections.emptyList() : fieldInitializers;
    }

    public void addComputedField(AstNode keyExpr, AstNode initializer) {
        if (computedFieldKeys == null) {
            computedFieldKeys = new ArrayList<>();
            computedFieldInitializers = new ArrayList<>();
        }
        computedFieldKeys.add(keyExpr);
        computedFieldInitializers.add(initializer);
        keyExpr.setParent(this);
        if (initializer != null) {
            initializer.setParent(this);
        }
    }

    public int getComputedFieldCount() {
        return computedFieldKeys == null ? 0 : computedFieldKeys.size();
    }

    public List<AstNode> getComputedFieldKeys() {
        return computedFieldKeys == null ? Collections.emptyList() : computedFieldKeys;
    }

    public List<AstNode> getComputedFieldInitializers() {
        return computedFieldInitializers == null
                ? Collections.emptyList()
                : computedFieldInitializers;
    }

    public void addStaticField(String name, AstNode initializer) {
        if (staticFieldNames == null) {
            staticFieldNames = new ArrayList<>();
            staticFieldInitializers = new ArrayList<>();
        }
        staticFieldNames.add(name);
        staticFieldInitializers.add(initializer);
        if (initializer != null) {
            initializer.setParent(this);
        }
    }

    public int getStaticFieldCount() {
        return staticFieldNames == null ? 0 : staticFieldNames.size();
    }

    public List<String> getStaticFieldNames() {
        return staticFieldNames == null ? Collections.emptyList() : staticFieldNames;
    }

    public List<AstNode> getStaticFieldInitializers() {
        return staticFieldInitializers == null ? Collections.emptyList() : staticFieldInitializers;
    }

    public void addStaticComputedField(AstNode keyExpr, AstNode initializer) {
        if (staticComputedFieldKeys == null) {
            staticComputedFieldKeys = new ArrayList<>();
            staticComputedFieldInitializers = new ArrayList<>();
        }
        staticComputedFieldKeys.add(keyExpr);
        staticComputedFieldInitializers.add(initializer);
        keyExpr.setParent(this);
        if (initializer != null) {
            initializer.setParent(this);
        }
    }

    public int getStaticComputedFieldCount() {
        return staticComputedFieldKeys == null ? 0 : staticComputedFieldKeys.size();
    }

    public List<AstNode> getStaticComputedFieldKeys() {
        return staticComputedFieldKeys == null ? Collections.emptyList() : staticComputedFieldKeys;
    }

    public List<AstNode> getStaticComputedFieldInitializers() {
        return staticComputedFieldInitializers == null
                ? Collections.emptyList()
                : staticComputedFieldInitializers;
    }

    public void addPrivateField(String name, AstNode initializer) {
        addPrivateMember(name, initializer, MethodKind.METHOD);
    }

    public void addPrivateMember(String name, AstNode initializer, MethodKind kind) {
        if (privateFieldNames == null) {
            privateFieldNames = new ArrayList<>();
            privateFieldInitializers = new ArrayList<>();
            privateFieldKinds = new ArrayList<>();
        }
        privateFieldNames.add(name);
        privateFieldInitializers.add(initializer);
        privateFieldKinds.add(kind);
        if (initializer != null) {
            initializer.setParent(this);
        }
        getOrCreatePrivateSymbol(name);
    }

    public int getPrivateFieldCount() {
        return privateFieldNames == null ? 0 : privateFieldNames.size();
    }

    public List<String> getPrivateFieldNames() {
        return privateFieldNames == null ? Collections.emptyList() : privateFieldNames;
    }

    public List<AstNode> getPrivateFieldInitializers() {
        return privateFieldInitializers == null
                ? Collections.emptyList()
                : privateFieldInitializers;
    }

    public List<MethodKind> getPrivateFieldKinds() {
        return privateFieldKinds == null ? Collections.emptyList() : privateFieldKinds;
    }

    public void addStaticPrivateField(String name, AstNode initializer) {
        addStaticPrivateMember(name, initializer, MethodKind.METHOD);
    }

    public void addStaticPrivateMember(String name, AstNode initializer, MethodKind kind) {
        if (staticPrivateFieldNames == null) {
            staticPrivateFieldNames = new ArrayList<>();
            staticPrivateFieldInitializers = new ArrayList<>();
            staticPrivateFieldKinds = new ArrayList<>();
        }
        staticPrivateFieldNames.add(name);
        staticPrivateFieldInitializers.add(initializer);
        staticPrivateFieldKinds.add(kind);
        if (initializer != null) {
            initializer.setParent(this);
        }
        getOrCreatePrivateSymbol(name);
    }

    public int getStaticPrivateFieldCount() {
        return staticPrivateFieldNames == null ? 0 : staticPrivateFieldNames.size();
    }

    public List<String> getStaticPrivateFieldNames() {
        return staticPrivateFieldNames == null ? Collections.emptyList() : staticPrivateFieldNames;
    }

    public List<AstNode> getStaticPrivateFieldInitializers() {
        return staticPrivateFieldInitializers == null
                ? Collections.emptyList()
                : staticPrivateFieldInitializers;
    }

    public List<MethodKind> getStaticPrivateFieldKinds() {
        return staticPrivateFieldKinds == null ? Collections.emptyList() : staticPrivateFieldKinds;
    }

    /**
     * Check whether adding a private member of the given kind with the given name would be a
     * duplicate. Private getters and setters may share a name with each other within the same
     * static/instance bucket, but any other combination (including instance vs static overlap) is a
     * duplicate.
     */
    public boolean isDuplicatePrivateMember(String name, MethodKind kind, boolean isStatic) {
        // Any use of this name in the opposite bucket (instance vs static) is a duplicate.
        List<String> otherNames = isStatic ? privateFieldNames : staticPrivateFieldNames;
        if (otherNames != null && otherNames.contains(name)) {
            return true;
        }
        List<String> names = isStatic ? staticPrivateFieldNames : privateFieldNames;
        List<MethodKind> kinds = isStatic ? staticPrivateFieldKinds : privateFieldKinds;
        if (names == null) {
            return false;
        }
        boolean sawGetter = false;
        boolean sawSetter = false;
        for (int i = 0; i < names.size(); i++) {
            if (!name.equals(names.get(i))) {
                continue;
            }
            MethodKind existing = kinds.get(i);
            if (existing == MethodKind.GETTER) {
                sawGetter = true;
            } else if (existing == MethodKind.SETTER) {
                sawSetter = true;
            } else {
                return true;
            }
        }
        if (!sawGetter && !sawSetter) {
            return false;
        }
        if (kind == MethodKind.GETTER) {
            return sawGetter;
        }
        if (kind == MethodKind.SETTER) {
            return sawSetter;
        }
        return true;
    }

    /** Returns the (possibly empty) map of private-name -> shared SymbolKey. */
    public Map<String, SymbolKey> getPrivateSymbols() {
        return privateSymbols == null ? Collections.emptyMap() : privateSymbols;
    }

    /**
     * Returns true if the given private name (including the leading {@code #}) has already been
     * declared as any kind of private member of this class.
     */
    public boolean hasPrivateName(String name) {
        return privateSymbols != null && privateSymbols.containsKey(name);
    }

    /**
     * Returns the shared {@link SymbolKey} for the given private name, creating it on first use.
     * Names include the leading {@code #}.
     */
    public SymbolKey getOrCreatePrivateSymbol(String name) {
        if (privateSymbols == null) {
            privateSymbols = new LinkedHashMap<>();
        }
        return privateSymbols.computeIfAbsent(name, n -> new SymbolKey(n, Symbol.Kind.PRIVATE));
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth));
        sb.append("class");
        if (className != null) {
            sb.append(" ");
            sb.append(className.toSource(0));
        }
        if (superClass != null) {
            sb.append(" extends ");
            sb.append(superClass.toSource(0));
        }
        sb.append(" {\n");
        if (fieldNames != null) {
            for (int i = 0; i < fieldNames.size(); i++) {
                sb.append(makeIndent(depth + 1));
                sb.append(fieldNames.get(i));
                AstNode init = fieldInitializers.get(i);
                if (init != null) {
                    sb.append(" = ");
                    sb.append(init.toSource(0));
                }
                sb.append(";\n");
            }
        }
        if (privateFieldNames != null) {
            for (int i = 0; i < privateFieldNames.size(); i++) {
                sb.append(makeIndent(depth + 1));
                sb.append(privateFieldNames.get(i));
                AstNode init = privateFieldInitializers.get(i);
                if (init != null) {
                    sb.append(" = ");
                    sb.append(init.toSource(0));
                }
                sb.append(";\n");
            }
        }
        if (computedFieldKeys != null) {
            for (int i = 0; i < computedFieldKeys.size(); i++) {
                sb.append(makeIndent(depth + 1));
                sb.append("[");
                sb.append(computedFieldKeys.get(i).toSource(0));
                sb.append("]");
                AstNode init = computedFieldInitializers.get(i);
                if (init != null) {
                    sb.append(" = ");
                    sb.append(init.toSource(0));
                }
                sb.append(";\n");
            }
        }
        if (constructor != null) {
            sb.append(makeIndent(depth + 1));
            sb.append("constructor");
            sb.append(constructor.toSource(0).substring(constructor.toSource(0).indexOf('(')));
            sb.append("\n");
        }
        if (methodNames != null) {
            for (int i = 0; i < methodNames.size(); i++) {
                sb.append(makeIndent(depth + 1));
                MethodKind kind = methodKinds.get(i);
                if (kind == MethodKind.GETTER) {
                    sb.append("get ");
                } else if (kind == MethodKind.SETTER) {
                    sb.append("set ");
                }
                sb.append(methodNames.get(i));
                String fnSrc = methods.get(i).toSource(0);
                sb.append(fnSrc.substring(fnSrc.indexOf('(')));
                sb.append("\n");
            }
        }
        if (staticMethodNames != null) {
            for (int i = 0; i < staticMethodNames.size(); i++) {
                sb.append(makeIndent(depth + 1));
                sb.append("static ");
                MethodKind kind = staticMethodKinds.get(i);
                if (kind == MethodKind.GETTER) {
                    sb.append("get ");
                } else if (kind == MethodKind.SETTER) {
                    sb.append("set ");
                }
                sb.append(staticMethodNames.get(i));
                String fnSrc = staticMethods.get(i).toSource(0);
                sb.append(fnSrc.substring(fnSrc.indexOf('(')));
                sb.append("\n");
            }
        }
        sb.append(makeIndent(depth));
        sb.append("}");
        return sb.toString();
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            if (className != null) {
                className.visit(v);
            }
            if (superClass != null) {
                superClass.visit(v);
            }
            if (fieldInitializers != null) {
                for (AstNode init : fieldInitializers) {
                    if (init != null) {
                        init.visit(v);
                    }
                }
            }
            if (privateFieldInitializers != null) {
                for (AstNode init : privateFieldInitializers) {
                    if (init != null) {
                        init.visit(v);
                    }
                }
            }
            if (computedFieldKeys != null) {
                for (int i = 0; i < computedFieldKeys.size(); i++) {
                    computedFieldKeys.get(i).visit(v);
                    AstNode init = computedFieldInitializers.get(i);
                    if (init != null) {
                        init.visit(v);
                    }
                }
            }
            if (constructor != null) {
                constructor.visit(v);
            }
            if (methods != null) {
                for (FunctionNode method : methods) {
                    method.visit(v);
                }
            }
            if (staticMethods != null) {
                for (FunctionNode method : staticMethods) {
                    method.visit(v);
                }
            }
        }
    }
}
