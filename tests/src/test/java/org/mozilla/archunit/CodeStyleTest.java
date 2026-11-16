/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.archunit;

import static org.junit.jupiter.api.Assertions.fail;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

/**
 * Source-level code style tests using the Java Compiler Tree API.
 *
 * @author Ronald Brill
 */
public class CodeStyleTest {

    private static final Path SOURCES_ROOT = Paths.get("../rhino/src/main/java");

    /** Parameters of type Context must be named 'cx' or 'topCx'. */
    @Test
    public void contextParameterNames() throws IOException {
        List<String> violations =
                collectViolations(
                        "org.mozilla.javascript.Context",
                        Set.of("cx", "topCx"),
                        "Parameters of type Context must be named 'cx' or 'topCx'");
        assertNoViolations(violations);
    }

    /**
     * Parameters of type VarScope must be named 'scope', 'topScope', 'callScope' or 'parentScope'.
     */
    @Test
    public void varScopeParameterNames() throws IOException {
        List<String> violations =
                collectViolations(
                        "org.mozilla.javascript.VarScope",
                        Set.of("scope", "topScope", "callScope", "parentScope"),
                        "Parameters of type VarScope must be named 'scope', 'topScope', 'callScope' or 'parentScope'");
        assertNoViolations(violations);
    }

    private static void assertNoViolations(List<String> violations) {
        if (!violations.isEmpty()) {
            fail("Code style violations found:\n" + String.join("\n", violations));
        }
    }

    /**
     * Walks all .java source files under {@link #SOURCES_ROOT} and returns a list of violation
     * messages for every method parameter whose simple type name matches {@code
     * fullyQualifiedTypeName} but whose parameter name is not contained in {@code allowedNames}.
     */
    private static List<String> collectViolations(
            String fullyQualifiedTypeName, Set<String> allowedNames, String ruleDescription)
            throws IOException {

        List<File> sourceFiles = findSourceFiles(SOURCES_ROOT);
        if (sourceFiles.isEmpty()) {
            return List.of();
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(diagnostics, null, null)) {

            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjectsFromFiles(sourceFiles);

            // Parse only — no full compilation needed
            JavacTask task =
                    (JavacTask)
                            compiler.getTask(
                                    null,
                                    fileManager,
                                    diagnostics,
                                    List.of("-proc:none"),
                                    null,
                                    compilationUnits);

            Iterable<? extends CompilationUnitTree> units = task.parse();

            List<String> violations = new ArrayList<>();
            String simpleTypeName = simpleNameOf(fullyQualifiedTypeName);

            for (CompilationUnitTree unit : units) {
                String sourcePath = unit.getSourceFile().getName();

                new TreePathScanner<Void, Void>() {
                    @Override
                    public Void visitMethod(MethodTree method, Void unused) {
                        for (VariableTree param : method.getParameters()) {
                            String paramType = param.getType().toString();
                            // Match on simple name to avoid requiring FQN in source
                            if (simpleTypeName.equals(paramType)
                                    || fullyQualifiedTypeName.equals(paramType)) {
                                String paramName = param.getName().toString();
                                if (!allowedNames.contains(paramName)) {
                                    violations.add(
                                            String.format(
                                                    "[%s] %s - method '%s': parameter '%s %s' must be one of %s",
                                                    ruleDescription,
                                                    sourcePath,
                                                    method.getName(),
                                                    paramType,
                                                    paramName,
                                                    allowedNames));
                                }
                            }
                        }
                        return super.visitMethod(method, unused);
                    }
                }.scan(unit, null);
            }

            return violations;
        }
    }

    private static List<File> findSourceFiles(Path root) throws IOException {
        if (!root.toFile().exists()) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(p -> p.toString().endsWith(".java"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }
    }

    private static String simpleNameOf(String fullyQualifiedName) {
        int dot = fullyQualifiedName.lastIndexOf('.');
        return dot >= 0 ? fullyQualifiedName.substring(dot + 1) : fullyQualifiedName;
    }
}
