/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.rewrite;

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.*;

/**
 * OpenRewrite recipe to migrate Rhino 1.x scope parameters to Rhino 2.0.
 *
 * <p>In Rhino 2.0, several key interfaces (Callable, Constructable, IdFunctionCall) have changed
 * their 'scope' parameter from {@code Scriptable} to the new {@code VarScope} interface.
 * Additionally, 'thisObj' may be migrated from {@code Scriptable} to {@code Object}.
 */
public class MigrateCallableToVarScope extends Recipe {

    @Option(
            displayName = "Migrate scope to VarScope",
            description = "Whether to migrate the 'scope' parameter from Scriptable to VarScope.",
            required = false)
    private final boolean migrateScope;

    @Option(
            displayName = "Migrate thisObj to Object",
            description = "Whether to migrate the 'thisObj' parameter from Scriptable to Object.",
            required = false)
    private final boolean migrateThisObj;

    @Option(
            displayName = "Migrate Script.exec signatures",
            description = "Whether to migrate Script.exec signatures.",
            required = false)
    private final boolean migrateScriptExec;

    public MigrateCallableToVarScope() {
        this.migrateScope = true;
        this.migrateThisObj = true;
        this.migrateScriptExec = true;
    }

    public MigrateCallableToVarScope(
            boolean migrateScope, boolean migrateThisObj, boolean migrateScriptExec) {
        this.migrateScope = migrateScope;
        this.migrateThisObj = migrateThisObj;
        this.migrateScriptExec = migrateScriptExec;
    }

    public boolean isMigrateScope() {
        return migrateScope;
    }

    public boolean isMigrateThisObj() {
        return migrateThisObj;
    }

    @Override
    public String getDisplayName() {
        return "Migrate to Rhino 2.0";
    }

    @Override
    public String getDescription() {
        return "Migrates Rhino interfaces to 2.0 signatures.";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MigrateCallableToVarScope that = (MigrateCallableToVarScope) o;
        return migrateScope == that.migrateScope
                && migrateThisObj == that.migrateThisObj
                && migrateScriptExec == that.migrateScriptExec;
    }

    @Override
    public int hashCode() {
        int result = (migrateScope ? 1 : 0);
        result = 31 * result + (migrateThisObj ? 1 : 0);
        result = 31 * result + (migrateScriptExec ? 1 : 0);
        return result;
    }

    private static final String SCRIPTABLE = "org.mozilla.javascript.Scriptable";
    private static final String VAR_SCOPE = "org.mozilla.javascript.VarScope";
    private static final String OBJECT = "java.lang.Object";

    // Matches the signatures in Rhino interfaces.
    // We match against the OLD signatures (Rhino 1.x) to find candidates,
    // but we also have fallbacks for already partially migrated code.
    private static final MethodMatcher CALL_MATCHER =
            new MethodMatcher(
                    "org.mozilla.javascript.Callable"
                            + " call(org.mozilla.javascript.Context, *, *, java.lang.Object[])",
                    true);

    private static final MethodMatcher CONSTRUCT_MATCHER =
            new MethodMatcher(
                    "org.mozilla.javascript.Constructable"
                            + " construct(org.mozilla.javascript.Context, *, java.lang.Object[])",
                    true);

    private static final MethodMatcher EXEC_ID_CALL_MATCHER =
            new MethodMatcher(
                    "org.mozilla.javascript.IdFunctionCall"
                            + " execIdCall(org.mozilla.javascript.IdFunctionObject, org.mozilla.javascript.Context, *, *, java.lang.Object[])",
                    true);

    private static final MethodMatcher SCRIPT_EXEC_MATCHER =
            new MethodMatcher(
                    "org.mozilla.javascript.Script exec(org.mozilla.javascript.Context, *, *)",
                    true);

    private static final MethodMatcher SCRIPT_EXEC_2_MATCHER =
            new MethodMatcher(
                    "org.mozilla.javascript.Script exec(org.mozilla.javascript.Context, *)", true);

    private static final MethodMatcher INIT_STANDARD_OBJECTS_MATCHER =
            new MethodMatcher("org.mozilla.javascript.Context initStandardObjects(..)", true);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            private J.VariableDeclarations migrateParameterType(
                    J.VariableDeclarations vd, String oldType, String newType) {
                if (vd.getTypeExpression() == null) {
                    return null;
                }
                if (vd.getType() != null && !TypeUtils.isOfClassType(vd.getType(), oldType)) {
                    return null;
                }

                JavaType.FullyQualified newFqType = JavaType.ShallowClass.build(newType);
                J.VariableDeclarations updatedVd = vd.withType(newFqType);

                if (vd.getTypeExpression() != null) {
                    String simpleName =
                            newType.contains(".")
                                    ? newType.substring(newType.lastIndexOf('.') + 1)
                                    : newType;
                    if (vd.getTypeExpression() instanceof J.Identifier) {
                        J.Identifier identifier = (J.Identifier) vd.getTypeExpression();
                        updatedVd =
                                updatedVd.withTypeExpression(
                                        identifier.withSimpleName(simpleName).withType(newFqType));
                        maybeAddImport(newType);
                    } else if (vd.getTypeExpression() instanceof J.FieldAccess) {
                        J.FieldAccess fieldAccess = (J.FieldAccess) vd.getTypeExpression();
                        updatedVd =
                                updatedVd.withTypeExpression(
                                        fieldAccess
                                                .withName(
                                                        fieldAccess
                                                                .getName()
                                                                .withSimpleName(simpleName)
                                                                .withType(newFqType))
                                                .withType(newFqType));
                        maybeAddImport(newType);
                    }
                }

                updatedVd =
                        updatedVd.withVariables(
                                ListUtils.map(
                                        updatedVd.getVariables(),
                                        v ->
                                                v.withName(v.getName().withType(newFqType))
                                                        .withType(newFqType)));
                return updatedVd;
            }

            private java.util.Map<Integer, String> getTargetParameterTypes(
                    JavaType type, int paramCount) {
                java.util.Map<Integer, String> targets = new java.util.HashMap<>();
                if (TypeUtils.isAssignableTo("org.mozilla.javascript.Callable", type)) {
                    if (migrateScope) targets.put(1, VAR_SCOPE);
                    if (migrateThisObj) targets.put(2, OBJECT);
                } else if (TypeUtils.isAssignableTo("org.mozilla.javascript.Constructable", type)) {
                    if (migrateScope) targets.put(1, VAR_SCOPE);
                } else if (TypeUtils.isAssignableTo(
                        "org.mozilla.javascript.IdFunctionCall", type)) {
                    if (migrateScope) targets.put(2, VAR_SCOPE);
                    if (migrateThisObj) targets.put(3, OBJECT);
                } else if (TypeUtils.isAssignableTo("org.mozilla.javascript.Script", type)) {
                    if (paramCount == 3) {
                        if (migrateScriptExec) {
                            if (migrateScope) targets.put(1, VAR_SCOPE);
                            if (migrateThisObj) targets.put(2, OBJECT);
                        }
                    } else if (paramCount == 2) {
                        if (migrateScriptExec) {
                            if (migrateScope) targets.put(1, VAR_SCOPE);
                        }
                    }
                } else if (type == null || type instanceof JavaType.Unknown) {
                    // Fallback heuristics based on parameter counts for common Rhino interfaces
                    if (paramCount == 4) { // Callable.call
                        if (migrateScope) targets.put(1, VAR_SCOPE);
                        if (migrateThisObj) targets.put(2, OBJECT);
                    } else if (paramCount == 3) { // Constructable.construct / Script.exec
                        if (migrateScope) targets.put(1, VAR_SCOPE);
                    } else if (paramCount == 2) { // Script.exec
                        if (migrateScope) targets.put(1, VAR_SCOPE);
                    } else if (paramCount == 5) { // IdFunctionCall.execIdCall
                        if (migrateScope) targets.put(2, VAR_SCOPE);
                        if (migrateThisObj) targets.put(3, OBJECT);
                    }
                }
                return targets;
            }

            @Override
            public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext ctx) {
                java.util.Map<Integer, String> targets =
                        getTargetParameterTypes(
                                lambda.getType(), lambda.getParameters().getParameters().size());

                if (targets.isEmpty()) {
                    return super.visitLambda(lambda, ctx);
                }

                J.Lambda l = super.visitLambda(lambda, ctx);
                boolean changed = false;
                for (java.util.Map.Entry<Integer, String> entry : targets.entrySet()) {
                    int idx = entry.getKey();
                    String targetType = entry.getValue();

                    if (l.getParameters().getParameters().size() <= idx) {
                        continue;
                    }

                    Object raw = l.getParameters().getParameters().get(idx);
                    if (!(raw instanceof J.VariableDeclarations)) {
                        continue;
                    }

                    J.VariableDeclarations updatedVd =
                            migrateParameterType(
                                    (J.VariableDeclarations) raw, SCRIPTABLE, targetType);
                    if (updatedVd != null) {
                        changed = true;
                        final int currentIdx = idx;
                        final J.VariableDeclarations replacement = updatedVd;
                        l =
                                l.withParameters(
                                        l.getParameters()
                                                .withParameters(
                                                        ListUtils.map(
                                                                l.getParameters().getParameters(),
                                                                (i, p) ->
                                                                        i == currentIdx
                                                                                ? replacement
                                                                                : p)));
                    }
                }
                return changed ? l : super.visitLambda(lambda, ctx);
            }

            @Override
            public J.MemberReference visitMemberReference(
                    J.MemberReference memberReference, ExecutionContext ctx) {
                J.MemberReference m = super.visitMemberReference(memberReference, ctx);
                JavaType type = m.getType();
                if (type == null || type instanceof JavaType.Unknown) {
                    return m;
                }

                java.util.Map<Integer, String> targets = getTargetParameterTypes(type, -1);
                if (targets.isEmpty()) {
                    return m;
                }

                // Note: Method references don't have parameters in the reference itself.
                // The target method's declaration must be migrated.
                // visitMethodDeclaration handles this if the target method is an override
                // of a Rhino interface method. If it's a non-override matching signature,
                // we'd need to match its name/signature elsewhere.

                return m;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext ctx) {

                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                J.ClassDeclaration enclosingClass =
                        getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);

                java.util.Map<Integer, String> targets = new java.util.HashMap<>();

                if (CALL_MATCHER.matches(m, enclosingClass)) {
                    if (migrateScope) targets.put(1, VAR_SCOPE);
                    if (migrateThisObj) targets.put(2, OBJECT);
                } else if (CONSTRUCT_MATCHER.matches(m, enclosingClass)) {
                    if (migrateScope) targets.put(1, VAR_SCOPE);
                } else if (EXEC_ID_CALL_MATCHER.matches(m, enclosingClass)) {
                    if (migrateScope) targets.put(2, VAR_SCOPE);
                    if (migrateThisObj) targets.put(3, OBJECT);
                } else if (migrateScriptExec && SCRIPT_EXEC_MATCHER.matches(m, enclosingClass)) {
                    if (migrateScope) targets.put(1, VAR_SCOPE);
                    if (migrateThisObj) targets.put(2, OBJECT);
                } else if (migrateScriptExec && SCRIPT_EXEC_2_MATCHER.matches(m, enclosingClass)) {
                    if (migrateScope) targets.put(1, VAR_SCOPE);
                } else {
                    // Fallback heuristics
                    String name = m.getSimpleName();
                    int paramCount = m.getParameters().size();
                    if ("call".equals(name) && paramCount == 4) {
                        if (migrateScope) targets.put(1, VAR_SCOPE);
                        if (migrateThisObj) targets.put(2, OBJECT);
                    } else if ("construct".equals(name) && paramCount == 3) {
                        if (migrateScope) targets.put(1, VAR_SCOPE);
                    } else if ("execIdCall".equals(name) && paramCount == 5) {
                        if (migrateScope) targets.put(2, VAR_SCOPE);
                        if (migrateThisObj) targets.put(3, OBJECT);
                    } else if ("exec".equals(name)) {
                        if (paramCount == 3) {
                            if (migrateScriptExec) {
                                if (migrateScope) targets.put(1, VAR_SCOPE);
                                if (migrateThisObj) targets.put(2, OBJECT);
                            }
                        } else if (paramCount == 2) {
                            if (migrateScriptExec) {
                                if (migrateScope) targets.put(1, VAR_SCOPE);
                            }
                        }
                    }
                }

                if (targets.isEmpty()) {
                    return m;
                }

                for (java.util.Map.Entry<Integer, String> entry : targets.entrySet()) {
                    int idx = entry.getKey();
                    String targetType = entry.getValue();

                    if (m.getParameters().size() <= idx) {
                        continue;
                    }

                    Object raw = m.getParameters().get(idx);
                    if (!(raw instanceof J.VariableDeclarations)) {
                        continue;
                    }

                    J.VariableDeclarations updatedVd =
                            migrateParameterType(
                                    (J.VariableDeclarations) raw, SCRIPTABLE, targetType);
                    if (updatedVd != null) {
                        m =
                                m.withParameters(
                                        ListUtils.map(
                                                m.getParameters(),
                                                (i, p) -> i == idx ? updatedVd : p));

                        // Documentation polish: Update Javadoc @param tags
                        final String paramName = updatedVd.getVariables().get(0).getSimpleName();
                        final String newTypeSimple =
                                targetType.substring(targetType.lastIndexOf('.') + 1);

                        final J.MethodDeclaration methodForLambda = m;
                        m =
                                m.withComments(
                                        ListUtils.map(
                                                m.getComments(),
                                                c -> {
                                                    String rawText =
                                                            c.printComment(new Cursor(null, c));
                                                    boolean isJavadoc = rawText.startsWith("/**");
                                                    String text = rawText;
                                                    if (isJavadoc) {
                                                        text =
                                                                rawText.substring(
                                                                        3, rawText.length() - 2);
                                                    } else if (rawText.startsWith("/*")) {
                                                        text =
                                                                rawText.substring(
                                                                        2, rawText.length() - 2);
                                                    } else if (rawText.startsWith("//")) {
                                                        text = rawText.substring(2);
                                                    }

                                                    String[] lines = text.split("\n");
                                                    boolean modified = false;
                                                    for (int i = 0; i < lines.length; i++) {
                                                        if (lines[i].contains(
                                                                "@param " + paramName)) {
                                                            String newLine =
                                                                    lines[i].replace(
                                                                            "Scriptable",
                                                                            newTypeSimple);
                                                            if (!newLine.equals(lines[i])) {
                                                                lines[i] = newLine;
                                                                modified = true;
                                                            }
                                                        }
                                                    }

                                                    if (!modified) {
                                                        return c;
                                                    }

                                                    String updatedText = String.join("\n", lines);
                                                    if (isJavadoc) {
                                                        updatedText = "*" + updatedText;
                                                    }
                                                    return new TextComment(
                                                            c.isMultiline(),
                                                            updatedText,
                                                            c.getSuffix(),
                                                            c.getMarkers());
                                                }));
                    }
                }

                maybeRemoveImport(SCRIPTABLE);
                return m;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(
                    J.MethodInvocation invocation, ExecutionContext ctx) {

                J.MethodInvocation inv = super.visitMethodInvocation(invocation, ctx);
                java.util.Map<Integer, String> targets = getTargetArgumentIndices(inv);

                if (targets.isEmpty()) {
                    return inv;
                }

                for (java.util.Map.Entry<Integer, String> entry : targets.entrySet()) {
                    int idx = entry.getKey();
                    String targetType = entry.getValue();

                    if (inv.getArguments().size() <= idx) {
                        continue;
                    }

                    Object rawArg = inv.getArguments().get(idx);
                    if (!(rawArg instanceof J.TypeCast)) {
                        continue;
                    }

                    J.TypeCast cast = (J.TypeCast) rawArg;
                    TypeTree castType = cast.getClazz().getTree();
                    if (!TypeUtils.isOfClassType(castType.getType(), SCRIPTABLE)) {
                        continue;
                    }

                    JavaType.FullyQualified newFqType = JavaType.ShallowClass.build(targetType);
                    TypeTree newTypeTree;

                    String simpleName =
                            targetType.contains(".")
                                    ? targetType.substring(targetType.lastIndexOf('.') + 1)
                                    : targetType;

                    if (castType instanceof J.Identifier) {
                        J.Identifier id = (J.Identifier) castType;
                        newTypeTree = id.withSimpleName(simpleName).withType(newFqType);
                    } else if (castType instanceof J.FieldAccess) {
                        J.FieldAccess fa = (J.FieldAccess) castType;
                        newTypeTree =
                                fa.withName(
                                                fa.getName()
                                                        .withSimpleName(simpleName)
                                                        .withType(newFqType))
                                        .withType(newFqType);
                    } else {
                        continue;
                    }

                    J.TypeCast newCast =
                            cast.withClazz(cast.getClazz().withTree(newTypeTree))
                                    .withType(newFqType);

                    final int currentIdx = idx;
                    final J.TypeCast replacement = newCast;
                    inv =
                            inv.withArguments(
                                    ListUtils.map(
                                            inv.getArguments(),
                                            (i, arg) -> i == currentIdx ? replacement : arg));

                    maybeAddImport(targetType);
                }

                return inv;
            }

            private java.util.Map<Integer, String> getTargetArgumentIndices(
                    J.MethodInvocation inv) {
                java.util.Map<Integer, String> targets = new java.util.HashMap<>();
                if (CALL_MATCHER.matches(inv)) {
                    if (migrateScope) targets.put(1, VAR_SCOPE);
                    if (migrateThisObj) targets.put(2, OBJECT);
                } else if (CONSTRUCT_MATCHER.matches(inv)) {
                    if (migrateScope) targets.put(1, VAR_SCOPE);
                } else if (EXEC_ID_CALL_MATCHER.matches(inv)) {
                    if (migrateScope) targets.put(2, VAR_SCOPE);
                    if (migrateThisObj) targets.put(3, OBJECT);
                } else if (migrateScriptExec && SCRIPT_EXEC_MATCHER.matches(inv)) {
                    if (migrateScope) targets.put(1, VAR_SCOPE);
                    if (migrateThisObj) targets.put(2, OBJECT);
                } else if (migrateScriptExec && SCRIPT_EXEC_2_MATCHER.matches(inv)) {
                    if (migrateScope) targets.put(1, VAR_SCOPE);
                } else {
                    // Fallback heuristics
                    String name = inv.getSimpleName();
                    int argCount = inv.getArguments().size();
                    if ("call".equals(name) && argCount == 4) {
                        if (migrateScope) targets.put(1, VAR_SCOPE);
                        if (migrateThisObj) targets.put(2, OBJECT);
                    } else if ("construct".equals(name) && argCount == 3) {
                        if (migrateScope) targets.put(1, VAR_SCOPE);
                    } else if ("execIdCall".equals(name) && argCount == 5) {
                        if (migrateScope) targets.put(2, VAR_SCOPE);
                        if (migrateThisObj) targets.put(3, OBJECT);
                    } else if ("exec".equals(name)) {
                        if (argCount == 3) {
                            if (migrateScriptExec) {
                                if (migrateScope) targets.put(1, VAR_SCOPE);
                                if (migrateThisObj) targets.put(2, OBJECT);
                            }
                        } else if (argCount == 2) {
                            if (migrateScriptExec) {
                                if (migrateScope) targets.put(1, VAR_SCOPE);
                            }
                        }
                    }
                }
                return targets;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(
                    J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);
                if (mv.getVariables().isEmpty()
                        || mv.getVariables().get(0).getInitializer() == null) {
                    return mv;
                }

                Expression initializer = mv.getVariables().get(0).getInitializer();
                if (initializer instanceof J.MethodInvocation) {
                    J.MethodInvocation inv = (J.MethodInvocation) initializer;
                    if (INIT_STANDARD_OBJECTS_MATCHER.matches(inv)) {
                        J.VariableDeclarations updated =
                                migrateParameterType(mv, SCRIPTABLE, VAR_SCOPE);
                        if (updated != null) {
                            maybeRemoveImport(SCRIPTABLE);
                            return updated;
                        }
                    }
                }
                return mv;
            }

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                J.Assignment asgn = super.visitAssignment(assignment, ctx);
                if (asgn.getAssignment() instanceof J.MethodInvocation) {
                    J.MethodInvocation inv = (J.MethodInvocation) asgn.getAssignment();
                    if (INIT_STANDARD_OBJECTS_MATCHER.matches(inv)) {
                        maybeRemoveImport(SCRIPTABLE);
                    }
                }
                return asgn;
            }
        };
    }
}
