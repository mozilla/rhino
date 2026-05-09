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
import java.util.stream.Collectors;
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
    public enum ElementKind {
        METHOD,
        GETTER,
        SETTER
    }

    public static class ClassField {
        public final String name;
        public final AstNode computedName;
        public final AstNode initializer;
        public final ElementKind kind;

        private ClassField(
                String name, AstNode computedName, AstNode initializer, ElementKind kind) {
            this.name = name;
            this.computedName = computedName;
            this.initializer = initializer;
            this.kind = kind;
        }

        public static ClassField makeFIeld(String name, AstNode initializer) {
            return new ClassField(name, null, initializer, null);
        }
    }

    private List<ClassField> instanceFields = new ArrayList<>();
    // private List<ClassField> instanceMethods;
    // private List<ClassField> privateInstanceFields;
    // private List<ClassField> privateInstanceMethods;
    // private List<ClassField> staticFields;
    // private List<ClassField> staticMethods;
    // private List<ClassField> privateStaticFields;
    // private List<ClassField> privateStaticMethods;

    private Name className;
    private AstNode superClass;
    private FunctionNode constructor;
    private boolean isStatement;
    private List<String> methodNames;
    private List<FunctionNode> methods;
    private List<ElementKind> methodKinds;
    private List<AstNode> methodComputedKeys;
    private List<String> staticMethodNames;
    private List<FunctionNode> staticMethods;
    private List<ElementKind> staticMethodKinds;
    private List<AstNode> staticMethodComputedKeys;
    private List<AstNode> computedFieldKeys;
    private List<AstNode> computedFieldInitializers;
    private List<String> staticFieldNames;
    private List<AstNode> staticFieldInitializers;
    private List<AstNode> staticComputedFieldKeys;
    private List<AstNode> staticComputedFieldInitializers;
    private List<String> privateFieldNames;
    private List<AstNode> privateFieldInitializers;
    private List<ElementKind> privateFieldKinds;
    private List<String> staticPrivateFieldNames;
    private List<AstNode> staticPrivateFieldInitializers;
    private List<ElementKind> staticPrivateFieldKinds;

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
        addMethod(name, fn, ElementKind.METHOD);
    }

    public void addMethod(String name, FunctionNode fn, ElementKind kind) {
        addMethodInternal(name, null, fn, kind);
    }

    /**
     * Add an instance method whose property key is computed at runtime (e.g. {@code [expr]()}). The
     * {@code name} is null; {@code keyExpr} holds the expression to evaluate.
     */
    public void addComputedMethod(AstNode keyExpr, FunctionNode fn, ElementKind kind) {
        addMethodInternal(null, keyExpr, fn, kind);
    }

    private void addMethodInternal(
            String name, AstNode keyExpr, FunctionNode fn, ElementKind kind) {
        if (methodNames == null) {
            methodNames = new ArrayList<>();
            methods = new ArrayList<>();
            methodKinds = new ArrayList<>();
            methodComputedKeys = new ArrayList<>();
        }
        methodNames.add(name);
        methods.add(fn);
        methodKinds.add(kind);
        methodComputedKeys.add(keyExpr);
        fn.setParent(this);
        if (keyExpr != null) {
            keyExpr.setParent(this);
        }
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

    public List<ElementKind> getMethodKinds() {
        return methodKinds == null ? Collections.emptyList() : methodKinds;
    }

    public List<AstNode> getMethodComputedKeys() {
        return methodComputedKeys == null ? Collections.emptyList() : methodComputedKeys;
    }

    public void addStaticMethod(String name, FunctionNode fn) {
        addStaticMethod(name, fn, ElementKind.METHOD);
    }

    public void addStaticMethod(String name, FunctionNode fn, ElementKind kind) {
        addStaticMethodInternal(name, null, fn, kind);
    }

    /**
     * Add a static method whose property key is computed at runtime (e.g. {@code static [expr]()}).
     * The {@code name} is null; {@code keyExpr} holds the expression to evaluate.
     */
    public void addStaticComputedMethod(AstNode keyExpr, FunctionNode fn, ElementKind kind) {
        addStaticMethodInternal(null, keyExpr, fn, kind);
    }

    private void addStaticMethodInternal(
            String name, AstNode keyExpr, FunctionNode fn, ElementKind kind) {
        if (staticMethodNames == null) {
            staticMethodNames = new ArrayList<>();
            staticMethods = new ArrayList<>();
            staticMethodKinds = new ArrayList<>();
            staticMethodComputedKeys = new ArrayList<>();
        }
        staticMethodNames.add(name);
        staticMethods.add(fn);
        staticMethodKinds.add(kind);
        staticMethodComputedKeys.add(keyExpr);
        fn.setParent(this);
        if (keyExpr != null) {
            keyExpr.setParent(this);
        }
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

    public List<ElementKind> getStaticMethodKinds() {
        return staticMethodKinds == null ? Collections.emptyList() : staticMethodKinds;
    }

    public List<AstNode> getStaticMethodComputedKeys() {
        return staticMethodComputedKeys == null
                ? Collections.emptyList()
                : staticMethodComputedKeys;
    }

    public void addField(String name, AstNode initializer) {
        var elem = ClassField.makeFIeld(name, initializer);
        if (initializer != null) {
            initializer.setParent(this);
        }
        instanceFields.add(elem);
    }

    public int getFieldCount() {
        return instanceFields.size();
    }

    public List<ClassField> getInstanceFields() {
        return List.copyOf(instanceFields);
    }

    public List<String> getFieldNames() {
        return instanceFields.stream().map(e -> e.name).collect(Collectors.toList());
    }

    public List<AstNode> getFieldInitializers() {
        return instanceFields.stream().map(e -> e.initializer).collect(Collectors.toList());
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
        addPrivateMember(name, initializer, ElementKind.METHOD);
    }

    public void addPrivateMember(String name, AstNode initializer, ElementKind kind) {
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

    public List<ElementKind> getPrivateFieldKinds() {
        return privateFieldKinds == null ? Collections.emptyList() : privateFieldKinds;
    }

    public void addStaticPrivateField(String name, AstNode initializer) {
        addStaticPrivateMember(name, initializer, ElementKind.METHOD);
    }

    public void addStaticPrivateMember(String name, AstNode initializer, ElementKind kind) {
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

    public List<ElementKind> getStaticPrivateFieldKinds() {
        return staticPrivateFieldKinds == null ? Collections.emptyList() : staticPrivateFieldKinds;
    }

    /**
     * Check whether adding a private member of the given kind with the given name would be a
     * duplicate. Private getters and setters may share a name with each other within the same
     * static/instance bucket, but any other combination (including instance vs static overlap) is a
     * duplicate.
     */
    public boolean isDuplicatePrivateMember(String name, ElementKind kind, boolean isStatic) {
        // Any use of this name in the opposite bucket (instance vs static) is a duplicate.
        List<String> otherNames = isStatic ? privateFieldNames : staticPrivateFieldNames;
        if (otherNames != null && otherNames.contains(name)) {
            return true;
        }
        List<String> names = isStatic ? staticPrivateFieldNames : privateFieldNames;
        List<ElementKind> kinds = isStatic ? staticPrivateFieldKinds : privateFieldKinds;
        if (names == null) {
            return false;
        }
        boolean sawGetter = false;
        boolean sawSetter = false;
        for (int i = 0; i < names.size(); i++) {
            if (!name.equals(names.get(i))) {
                continue;
            }
            ElementKind existing = kinds.get(i);
            if (existing == ElementKind.GETTER) {
                sawGetter = true;
            } else if (existing == ElementKind.SETTER) {
                sawSetter = true;
            } else {
                return true;
            }
        }
        if (!sawGetter && !sawSetter) {
            return false;
        }
        if (kind == ElementKind.GETTER) {
            return sawGetter;
        }
        if (kind == ElementKind.SETTER) {
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
        for (var e : instanceFields) {
            sb.append(makeIndent(depth + 1));
            sb.append(e.name);
            AstNode init = e.initializer;
            if (init != null) {
                sb.append(" = ");
                sb.append(init.toSource(0));
            }
            sb.append(";\n");
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
                ElementKind kind = methodKinds.get(i);
                if (kind == ElementKind.GETTER) {
                    sb.append("get ");
                } else if (kind == ElementKind.SETTER) {
                    sb.append("set ");
                }
                String name = methodNames.get(i);
                if (name == null) {
                    sb.append("[");
                    sb.append(methodComputedKeys.get(i).toSource(0));
                    sb.append("]");
                } else {
                    sb.append(name);
                }
                String fnSrc = methods.get(i).toSource(0);
                sb.append(fnSrc.substring(fnSrc.indexOf('(')));
                sb.append("\n");
            }
        }
        if (staticMethodNames != null) {
            for (int i = 0; i < staticMethodNames.size(); i++) {
                sb.append(makeIndent(depth + 1));
                sb.append("static ");
                ElementKind kind = staticMethodKinds.get(i);
                if (kind == ElementKind.GETTER) {
                    sb.append("get ");
                } else if (kind == ElementKind.SETTER) {
                    sb.append("set ");
                }
                String name = staticMethodNames.get(i);
                if (name == null) {
                    sb.append("[");
                    sb.append(staticMethodComputedKeys.get(i).toSource(0));
                    sb.append("]");
                } else {
                    sb.append(name);
                }
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
            for (var e : instanceFields) {
                if (e.initializer != null) {
                    e.initializer.visit(v);
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
                for (int i = 0; i < methods.size(); i++) {
                    if (methodComputedKeys != null && methodComputedKeys.get(i) != null) {
                        methodComputedKeys.get(i).visit(v);
                    }
                    methods.get(i).visit(v);
                }
            }
            if (staticMethods != null) {
                for (int i = 0; i < staticMethods.size(); i++) {
                    if (staticMethodComputedKeys != null
                            && staticMethodComputedKeys.get(i) != null) {
                        staticMethodComputedKeys.get(i).visit(v);
                    }
                    staticMethods.get(i).visit(v);
                }
            }
        }
    }
}
