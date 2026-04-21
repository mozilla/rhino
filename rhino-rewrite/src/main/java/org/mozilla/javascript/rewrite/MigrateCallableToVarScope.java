/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

/**
 * OpenRewrite recipe that migrates Rhino embedder code from 1.x to 2.0.
 *
 * <p>In Rhino 2.0, the {@code scope} parameter in {@link org.mozilla.javascript.Callable#call},
 * {@link org.mozilla.javascript.Constructable#construct}, and {@link
 * org.mozilla.javascript.IdFunctionCall#execIdCall} changed from {@code Scriptable} to the new
 * {@code VarScope} interface.
 *
 * <p>This recipe finds any method declaration that overrides one of those Rhino interface methods
 * and still carries a {@code Scriptable} scope parameter, then updates the type to {@code VarScope}
 * and adds the import.
 */
public class MigrateCallableToVarScope extends Recipe {

    private static final String OLD_TYPE = "org.mozilla.javascript.Scriptable";
    private static final String NEW_TYPE = "org.mozilla.javascript.VarScope";

    // Matches the OLD (Rhino 1.x) signatures. The "true" flag enables
    // override-aware matching: it will match subtype declarations too.
    private static final MethodMatcher CALL_MATCHER =
            new MethodMatcher(
                    "org.mozilla.javascript.Callable"
                            + " call(org.mozilla.javascript.Context,"
                            + " org.mozilla.javascript.Scriptable,"
                            + " org.mozilla.javascript.Scriptable,"
                            + " java.lang.Object[])",
                    true);

    private static final MethodMatcher CONSTRUCT_MATCHER =
            new MethodMatcher(
                    "org.mozilla.javascript.Constructable"
                            + " construct(org.mozilla.javascript.Context,"
                            + " org.mozilla.javascript.Scriptable,"
                            + " java.lang.Object[])",
                    true);

    private static final MethodMatcher EXEC_ID_CALL_MATCHER =
            new MethodMatcher(
                    "org.mozilla.javascript.IdFunctionCall"
                            + " execIdCall(org.mozilla.javascript.IdFunctionObject,"
                            + " org.mozilla.javascript.Context,"
                            + " org.mozilla.javascript.Scriptable,"
                            + " org.mozilla.javascript.Scriptable,"
                            + " java.lang.Object[])",
                    true);

    @Override
    public String getDisplayName() {
        return "Migrate Rhino scope parameter from Scriptable to VarScope";
    }

    @Override
    public String getDescription() {
        return "Updates overrides of Rhino's Callable.call(), Constructable.construct(), and "
                + "IdFunctionCall.execIdCall() to use VarScope instead of Scriptable for the "
                + "scope parameter, as required by Rhino 2.0.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext ctx) {

                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                // Determine which parameter index holds the `scope` (VarScope target).
                // For call() and construct() it is index 1.
                // For execIdCall() it is index 2 (after IdFunctionObject and Context).
                int scopeParamIndex;
                J.ClassDeclaration enclosingClass =
                        getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);

                if (CALL_MATCHER.matches(m, enclosingClass)) {
                    scopeParamIndex = 1;
                } else if (CONSTRUCT_MATCHER.matches(m, enclosingClass)) {
                    scopeParamIndex = 1;
                } else if (EXEC_ID_CALL_MATCHER.matches(m, enclosingClass)) {
                    scopeParamIndex = 2;
                } else {
                    return m; // Not a Rhino method -- leave untouched
                }

                // Guard: only update if the parameter list is long enough
                if (m.getParameters().size() <= scopeParamIndex) {
                    return m;
                }

                // Parameters in OpenRewrite 8 are J.VariableDeclarations
                Object raw = m.getParameters().get(scopeParamIndex);
                if (!(raw instanceof J.VariableDeclarations)) {
                    return m;
                }

                J.VariableDeclarations vd = (J.VariableDeclarations) raw;
                if (!TypeUtils.isOfClassType(vd.getType(), OLD_TYPE)) {
                    return m; // Already VarScope or something else -- no change needed
                }

                // Build the new fully-qualified type
                JavaType.FullyQualified newFqType = JavaType.ShallowClass.build(NEW_TYPE);

                // Replace the type expression (the "Scriptable" identifier) with "VarScope"
                J.VariableDeclarations updatedVd = vd.withType(newFqType);
                if (vd.getTypeExpression() instanceof J.Identifier) {
                    J.Identifier identifier = (J.Identifier) vd.getTypeExpression();
                    updatedVd =
                            updatedVd.withTypeExpression(
                                    identifier.withSimpleName("VarScope").withType(newFqType));
                } else if (vd.getTypeExpression() instanceof J.FieldAccess) {
                    J.FieldAccess fieldAccess = (J.FieldAccess) vd.getTypeExpression();
                    updatedVd =
                            updatedVd.withTypeExpression(
                                    fieldAccess
                                            .withName(
                                                    fieldAccess
                                                            .getName()
                                                            .withSimpleName("VarScope")
                                                            .withType(newFqType))
                                            .withType(newFqType));
                }

                // Also update the JavaType stored on each named variable inside the declaration
                updatedVd =
                        updatedVd.withVariables(
                                ListUtils.map(
                                        updatedVd.getVariables(),
                                        v -> v.withName(v.getName().withType(newFqType))));

                // Replace the parameter at scopeParamIndex in the method
                final int idx = scopeParamIndex;
                final J.VariableDeclarations finalVd = updatedVd;
                m =
                        m.withParameters(
                                ListUtils.map(m.getParameters(), (i, p) -> i == idx ? finalVd : p));

                // Ensure VarScope is imported.
                // (Scriptable is still used for thisObj, so we do NOT remove it.)
                maybeAddImport(NEW_TYPE);

                return m;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(
                    J.MethodInvocation invocation, ExecutionContext ctx) {

                J.MethodInvocation inv = super.visitMethodInvocation(invocation, ctx);

                int scopeParamIndex = getScopeArgumentIndex(inv);
                if (scopeParamIndex < 0) {
                    return inv;
                }

                if (inv.getArguments().size() <= scopeParamIndex) {
                    return inv;
                }

                Object rawArg = inv.getArguments().get(scopeParamIndex);
                if (!(rawArg instanceof J.TypeCast)) {
                    return inv;
                }

                J.TypeCast cast = (J.TypeCast) rawArg;
                TypeTree castType = cast.getClazz().getTree();
                if (!TypeUtils.isOfClassType(castType.getType(), OLD_TYPE)) {
                    return inv;
                }

                JavaType.FullyQualified newFqType = JavaType.ShallowClass.build(NEW_TYPE);
                TypeTree newTypeTree;

                if (castType instanceof J.Identifier) {
                    J.Identifier id = (J.Identifier) castType;
                    newTypeTree = id.withSimpleName("VarScope").withType(newFqType);
                } else if (castType instanceof J.FieldAccess) {
                    J.FieldAccess fa = (J.FieldAccess) castType;
                    newTypeTree =
                            fa.withName(fa.getName().withSimpleName("VarScope").withType(newFqType))
                                    .withType(newFqType);
                } else {
                    return inv;
                }

                J.TypeCast newCast =
                        cast.withClazz(cast.getClazz().withTree(newTypeTree)).withType(newFqType);

                final int idx = scopeParamIndex;
                final J.TypeCast replacement = newCast;
                inv =
                        inv.withArguments(
                                ListUtils.map(
                                        inv.getArguments(),
                                        (i, arg) -> i == idx ? replacement : arg));

                maybeAddImport(NEW_TYPE);

                return inv;
            }

            private int getScopeArgumentIndex(J.MethodInvocation inv) {
                if (CALL_MATCHER.matches(inv)) {
                    return 1;
                }
                if (CONSTRUCT_MATCHER.matches(inv)) {
                    return 1;
                }
                if (EXEC_ID_CALL_MATCHER.matches(inv)) {
                    return 2;
                }

                // Fallback for parse-error scenarios where invocation method type attribution is
                // missing (common in pre-migration code that no longer compiles against Rhino 2.0).
                String name = inv.getSimpleName();
                int argCount = inv.getArguments().size();
                if ("call".equals(name) && argCount == 4) {
                    return 1;
                }
                if ("construct".equals(name) && argCount == 3) {
                    return 1;
                }
                if ("execIdCall".equals(name) && argCount == 5) {
                    return 2;
                }
                return -1;
            }
        };
    }
}
