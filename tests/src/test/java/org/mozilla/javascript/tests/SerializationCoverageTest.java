/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;

public class SerializationCoverageTest {
    private static final Set<String> VALUES =
            new HashSet<>(
                    Arrays.asList(
                            "org.mozilla.javascript.ScriptStackElement",
                            "org.mozilla.javascript.SymbolKey",
                            "org.mozilla.javascript.Undefined",
                            "org.mozilla.javascript.UniqueTag"));

    @Test
    public void serializationRootsHaveAllPolicyHooks() throws Exception {
        Set<String> names = new TreeSet<>();
        Set<String> remainingValues = new HashSet<>(VALUES);
        Set<URI> locations = new HashSet<>();
        locations.add(Context.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        locations.add(
                Class.forName(
                                "org.mozilla.javascript.xmlimpl.XmlProcessor",
                                false,
                                getClass().getClassLoader())
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI());
        for (URI location : locations) collect(Paths.get(location), names);
        int roots = 0;
        boolean consStringFound = false;
        for (String name : names) {
            if (!name.startsWith("org.mozilla.javascript.")
                    || name.startsWith("org.mozilla.javascript.tools.")) continue;
            Class<?> type = Class.forName(name, false, getClass().getClassLoader());
            if (type.isInterface()
                    || type.isEnum()
                    || type.isSynthetic()
                    || !Serializable.class.isAssignableFrom(type)
                    || Throwable.class.isAssignableFrom(type)) continue;
            if (VALUES.contains(name)) {
                remainingValues.remove(name);
                for (Field field : type.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())
                            || Modifier.isTransient(field.getModifiers())) continue;
                    Class<?> valueType = field.getType();
                    if (!(valueType.isPrimitive()
                            || valueType == String.class
                            || valueType.isEnum())) {
                        throw new AssertionError("Value exception has non-value state: " + field);
                    }
                }
                continue;
            }
            if (name.equals("org.mozilla.javascript.ConsString")) {
                consStringFound = true;
                hook(type, "readObject", ObjectInputStream.class);
                hook(type, "readObjectNoData");
                continue;
            }
            if (Serializable.class.isAssignableFrom(type.getSuperclass())) continue;
            roots++;
            hook(type, "readObject", ObjectInputStream.class);
            hook(type, "writeObject", ObjectOutputStream.class);
            hook(type, "readObjectNoData");
        }
        assertEquals(47, roots);
        assertTrue(remainingValues.isEmpty());
        assertTrue(consStringFound);
    }

    private static void hook(Class<?> type, String name, Class<?>... arguments) throws Exception {
        Method method = type.getDeclaredMethod(name, arguments);
        assertEquals(void.class, method.getReturnType());
        assertTrue(Modifier.isPrivate(method.getModifiers()));
        assertFalse(Modifier.isStatic(method.getModifiers()));
    }

    private static void collect(Path root, Set<String> names) throws IOException {
        if (Files.isDirectory(root)) {
            try (Stream<Path> files = Files.walk(root)) {
                files.filter(p -> p.toString().endsWith(".class"))
                        .forEach(
                                p ->
                                        add(
                                                root.relativize(p)
                                                        .toString()
                                                        .replace(File.separatorChar, '/'),
                                                names));
            }
        } else {
            try (JarFile jar = new JarFile(root.toFile())) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) add(entries.nextElement().getName(), names);
            }
        }
    }

    private static void add(String entry, Set<String> names) {
        if (entry.endsWith(".class") && !entry.startsWith("META-INF/")) {
            names.add(entry.substring(0, entry.length() - 6).replace('/', '.'));
        }
    }
}
