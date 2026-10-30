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
import java.nio.charset.StandardCharsets;
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
        Map<String, byte[]> classes = new TreeMap<>();
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
        for (URI location : locations) collect(Paths.get(location), classes);
        int roots = 0;
        boolean consStringFound = false;
        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            String name = entry.getKey();
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
            // The JVM invokes the hooks of every class in a serialized object's hierarchy, so a
            // hook that does not consult the policy would bypass the guards of Serializable
            // ancestors. ConsString is exempt: its readObject unconditionally throws, which is
            // stricter than the policy.
            requirePolicyReference(entry.getValue(), type);
            if (Serializable.class.isAssignableFrom(type.getSuperclass())) continue;
            roots++;
            hook(type, "readObject", ObjectInputStream.class);
            hook(type, "writeObject", ObjectOutputStream.class);
            hook(type, "readObjectNoData");
        }
        assertEquals(48, roots);
        assertTrue(remainingValues.isEmpty());
        assertTrue(consStringFound);
    }

    private static void hook(Class<?> type, String name, Class<?>... arguments) throws Exception {
        Method method = type.getDeclaredMethod(name, arguments);
        assertEquals(void.class, method.getReturnType());
        assertTrue(Modifier.isPrivate(method.getModifiers()));
        assertFalse(Modifier.isStatic(method.getModifiers()));
    }

    private static void requirePolicyReference(byte[] data, Class<?> type) throws Exception {
        for (Method method : type.getDeclaredMethods()) {
            if (!Modifier.isPrivate(method.getModifiers())
                    || Modifier.isStatic(method.getModifiers())) continue;
            Class<?>[] parameters = method.getParameterTypes();
            String check;
            if (method.getName().equals("readObject")
                    && parameters.length == 1
                    && parameters[0] == ObjectInputStream.class) {
                check = "checkDeserializationEnabled";
            } else if (method.getName().equals("writeObject")
                    && parameters.length == 1
                    && parameters[0] == ObjectOutputStream.class) {
                check = "checkSerializationEnabled";
            } else {
                continue;
            }
            // The check method reference is present in the class file's constant pool.
            assertTrue(
                    containsAscii(data, check),
                    type.getName()
                            + "."
                            + method.getName()
                            + " does not reference the Java serialization policy");
        }
    }

    private static boolean containsAscii(byte[] data, String text) {
        byte[] needle = text.getBytes(StandardCharsets.US_ASCII);
        outer:
        for (int i = 0; i + needle.length <= data.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private static void collect(Path root, Map<String, byte[]> classes) throws IOException {
        if (Files.isDirectory(root)) {
            try (Stream<Path> files = Files.walk(root)) {
                files.filter(p -> p.toString().endsWith(".class"))
                        .forEach(
                                p -> {
                                    try {
                                        add(
                                                root.relativize(p)
                                                        .toString()
                                                        .replace(File.separatorChar, '/'),
                                                Files.readAllBytes(p),
                                                classes);
                                    } catch (IOException e) {
                                        throw new UncheckedIOException(e);
                                    }
                                });
            }
        } else {
            try (JarFile jar = new JarFile(root.toFile())) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    try (InputStream in = jar.getInputStream(entry)) {
                        add(entry.getName(), in.readAllBytes(), classes);
                    }
                }
            }
        }
    }

    private static void add(String entry, byte[] data, Map<String, byte[]> classes) {
        if (entry.endsWith(".class") && !entry.startsWith("META-INF/")) {
            classes.put(entry.substring(0, entry.length() - 6).replace('/', '.'), data);
        }
    }
}
