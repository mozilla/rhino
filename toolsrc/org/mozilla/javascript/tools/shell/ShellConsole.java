/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tools.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.List;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 *
 * @author André Bargull
 */
public abstract class ShellConsole {
    protected ShellConsole() {
    }

    /**
     * Returns the underlying {@link InputStream}
     */
    public abstract InputStream getIn();

    /**
     * Reads a single line from the console
     */
    public abstract String readLine() throws IOException;

    /**
     * Reads a single line from the console and sets the console's prompt to
     * {@code prompt}
     */
    public abstract String readLine(String prompt) throws IOException;

    /**
     * Flushes the console's output
     */
    public abstract void flush() throws IOException;

    /**
     * Prints a single string to the console
     */
    public abstract void print(String s) throws IOException;

    /**
     * Prints the newline character-sequence to the console
     */
    public abstract void println() throws IOException;

    /**
     * Prints a string and the newline character-sequence to the console
     */
    public abstract void println(String s) throws IOException;

    private static interface MethodDecl {
        static final Class<?> UNRESOLVED = new Object(){}.getClass();

        String methodName();

        Class<?>[] parameters();
    }

    private static class Instance<METHOD extends Enum<METHOD> & MethodDecl> {
        private final Object instance;
        private final Class<?> clazz;
        private final EnumMap<METHOD, Method> cache;

        Instance(Object instance, Class<METHOD> methods) {
            this.instance = instance;
            this.clazz = instance.getClass();
            this.cache = new EnumMap<METHOD, Method>(methods);
        }

        private static Method methodOrNull(Class<?> clazz, String name,
                Class<?>... parameterTypes) {
            try {
                return clazz.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException e) {
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private static <T> T tryInvoke(Method m, Object obj, Object... args) {
            try {
                if (m != null) {
                    return (T) m.invoke(obj, args);
                }
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
            return null;
        }

        private Method get(METHOD method) {
            if (!cache.containsKey(method)) {
                String name = method.methodName();
                Class<?>[] parameters = method.parameters();
                synchronized (cache) {
                    cache.put(method, methodOrNull(clazz, name, parameters));
                }
            }
            return cache.get(method);
        }

        void resolve(METHOD method, Class<?>... parameters) {
            Class<?>[] p = method.parameters();
            for (int i = 0, j = 0; i < parameters.length && j < p.length; ++j) {
                if (p[j] == MethodDecl.UNRESOLVED) {
                    p[j] = parameters[i++];
                }
            }
        }

        <T> T invoke(METHOD method, Object... args) {
            return tryInvoke(get(method), instance, args);
        }
    }

    /**
     * JLine ConsoleReader API v1
     */
    private enum ConsoleReaderMethodsV1 implements MethodDecl {
        readLine("readLine"),
        readLine_String("readLine", String.class),
        flushConsole("flushConsole"),
        printString("printString", String.class),
        printNewline("printNewline"),
        setBellEnabled("setBellEnabled", Boolean.TYPE),
        addCompletor("addCompletor", UNRESOLVED);

        private final String name;
        private final Class<?>[] parameters;

        private ConsoleReaderMethodsV1(String name, Class<?>... parameters) {
            this.name = name;
            this.parameters = parameters;
        }

        @Override
        public String methodName() {
            return name;
        }

        @Override
        public Class<?>[] parameters() {
            return parameters;
        }
    }

    /**
     * JLine ConsoleReader API v2
     */
    private enum ConsoleReaderMethodsV2 implements MethodDecl {
        readLine("readLine"),
        readLine_String("readLine", String.class),
        flush("flush"),
        print("print", CharSequence.class),
        println("println"),
        println_CharSequence("println", CharSequence.class),
        getOutput("getOutput"),
        setBellEnabled("setBellEnabled", Boolean.TYPE),
        addCompleter("addCompleter", UNRESOLVED);

        private final String name;
        private final Class<?>[] parameters;

        private ConsoleReaderMethodsV2(String name, Class<?>... parameters) {
            this.name = name;
            this.parameters = parameters;
        }

        @Override
        public String methodName() {
            return name;
        }

        @Override
        public Class<?>[] parameters() {
            return parameters;
        }
    }

    /**
     * {@link ShellConsole} implementation for JLine v1
     */
    private static class JLineShellConsoleV1 extends ShellConsole {
        private final Instance<ConsoleReaderMethodsV1> instance;
        private final InputStream in;

        JLineShellConsoleV1(Instance<ConsoleReaderMethodsV1> instance,
                Charset cs) {
            this.instance = instance;
            this.in = new ConsoleInputStream(this, cs);
        }

        @Override
        public InputStream getIn() {
            return in;
        }

        @Override
        public String readLine() throws IOException {
            return instance.invoke(ConsoleReaderMethodsV1.readLine);
        }

        @Override
        public String readLine(String prompt) throws IOException {
            return instance.invoke(ConsoleReaderMethodsV1.readLine_String,
                    prompt);
        }

        @Override
        public void flush() throws IOException {
            instance.invoke(ConsoleReaderMethodsV1.flushConsole);
        }

        @Override
        public void print(String s) throws IOException {
            instance.invoke(ConsoleReaderMethodsV1.printString, s);
        }

        @Override
        public void println() throws IOException {
            instance.invoke(ConsoleReaderMethodsV1.printNewline);
        }

        @Override
        public void println(String s) throws IOException {
            instance.invoke(ConsoleReaderMethodsV1.printString, s);
            instance.invoke(ConsoleReaderMethodsV1.printNewline);
        }
    }

    /**
     * {@link ShellConsole} implementation for JLine v2
     */
    private static class JLineShellConsoleV2 extends ShellConsole {
        private final Instance<ConsoleReaderMethodsV2> instance;
        private final InputStream in;

        JLineShellConsoleV2(Instance<ConsoleReaderMethodsV2> instance,
                Charset cs) {
            this.instance = instance;
            this.in = new ConsoleInputStream(this, cs);
        }

        @Override
        public InputStream getIn() {
            return in;
        }

        @Override
        public String readLine() throws IOException {
            return instance.invoke(ConsoleReaderMethodsV2.readLine);
        }

        @Override
        public String readLine(String prompt) throws IOException {
            return instance.invoke(ConsoleReaderMethodsV2.readLine_String,
                    prompt);
        }

        @Override
        public void flush() throws IOException {
            instance.invoke(ConsoleReaderMethodsV2.flush);
        }

        @Override
        public void print(String s) throws IOException {
            instance.invoke(ConsoleReaderMethodsV2.print, s);
        }

        @Override
        public void println() throws IOException {
            instance.invoke(ConsoleReaderMethodsV2.println);
        }

        @Override
        public void println(String s) throws IOException {
            instance.invoke(ConsoleReaderMethodsV2.println_CharSequence, s);
        }
    }

    /**
     * JLine's ConsoleReaderInputStream is no longer public, therefore we need
     * to use our own implementation
     */
    private static class ConsoleInputStream extends InputStream {
        private static final byte[] EMPTY = new byte[] {};
        private final ShellConsole console;
        private final Charset cs;
        private byte[] buffer = EMPTY;
        private int cursor = -1;
        private boolean atEOF = false;

        public ConsoleInputStream(ShellConsole console, Charset cs) {
            this.console = console;
            this.cs = cs;
        }

        @Override
        public synchronized int read(byte[] b, int off, int len)
                throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }
            if (!ensureInput()) {
                return -1;
            }
            int n = Math.min(len, buffer.length - cursor);
            for (int i = 0; i < n; ++i) {
                b[off + i] = buffer[cursor + i];
            }
            if (n < len) {
                b[off + n++] = '\n';
            }
            cursor += n;
            return n;
        }

        @Override
        public synchronized int read() throws IOException {
            if (!ensureInput()) {
                return -1;
            }
            if (cursor == buffer.length) {
                cursor++;
                return '\n';
            }
            return buffer[cursor++];
        }

        private boolean ensureInput() throws IOException {
            if (atEOF) {
                return false;
            }
            if (cursor < 0 || cursor > buffer.length) {
                if (readNextLine() == -1) {
                    atEOF = true;
                    return false;
                }
                cursor = 0;
            }
            return true;
        }

        private int readNextLine() throws IOException {
            String line = console.readLine(null);
            if (line != null) {
                buffer = line.getBytes(cs);
                return buffer.length;
            } else {
                buffer = EMPTY;
                return -1;
            }
        }
    }

    private static class SimpleShellConsole extends ShellConsole {
        private final InputStream in;
        private final PrintWriter out;
        private final BufferedReader reader;

        SimpleShellConsole(InputStream in, PrintStream ps, Charset cs) {
            this.in = in;
            this.out = new PrintWriter(ps);
            this.reader = new BufferedReader(new InputStreamReader(in, cs));
        }

        @Override
        public InputStream getIn() {
            return in;
        }

        @Override
        public String readLine() throws IOException {
            return reader.readLine();
        }

        @Override
        public String readLine(String prompt) throws IOException {
            if (prompt != null) {
                out.write(prompt);
                out.flush();
            }
            return reader.readLine();
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void print(String s) throws IOException {
            out.print(s);
        }

        @Override
        public void println() throws IOException {
            out.println();
        }

        @Override
        public void println(String s) throws IOException {
            out.println(s);
        }
    }

    /**
     * Returns a new {@link ShellConsole} which uses the supplied
     * {@link InputStream} and {@link PrintStream} for its input/output
     */
    public static ShellConsole getConsole(InputStream in, PrintStream ps,
            Charset cs) {
        return new SimpleShellConsole(in, ps, cs);
    }

    /**
     * Provides a specialized {@link ShellConsole} to handle line editing,
     * history and completion. Relies on the JLine library (see
     * <http://jline.sourceforge.net>).
     */
    public static ShellConsole getConsole(Scriptable scope, Charset cs) {
        // We don't want a compile-time dependency on the JLine jar, so use
        // reflection to load and reference the JLine classes.
        ClassLoader classLoader = ShellConsole.class.getClassLoader();
        if (classLoader == null) {
            // If the attempt to get a class specific class loader above failed
            // then fallback to the system class loader.
            classLoader = ClassLoader.getSystemClassLoader();
        }
        if (classLoader == null) {
            // If for some reason we still don't have a handle to a class
            // loader then give up (avoid a NullPointerException).
            return null;
        }
        try {
            // first try to load JLine v2...
            Class<?> readerClass = Kit.classOrNull(classLoader,
                    "jline.console.ConsoleReader");
            if (readerClass != null) {
                return getJLineShellConsoleV2(classLoader, readerClass, scope, cs);
            }
            // ...if that fails, try to load JLine v1
            readerClass = Kit.classOrNull(classLoader, "jline.ConsoleReader");
            if (readerClass != null) {
                return getJLineShellConsoleV1(classLoader, readerClass, scope, cs);
            }
        } catch (NoSuchMethodException e) {
        } catch (IllegalAccessException e) {
        } catch (InstantiationException e) {
        } catch (InvocationTargetException e) {
        }
        return null;
    }

    private static JLineShellConsoleV1 getJLineShellConsoleV1(
            ClassLoader classLoader, Class<?> readerClass, Scriptable scope,
            Charset cs) throws NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        // ConsoleReader reader = new ConsoleReader();
        Constructor<?> c = readerClass.getConstructor();
        Object reader = c.newInstance();
        Instance<ConsoleReaderMethodsV1> instance = new Instance<ConsoleReaderMethodsV1>(
                reader, ConsoleReaderMethodsV1.class);

        // reader.setBellEnabled(false);
        instance.invoke(ConsoleReaderMethodsV1.setBellEnabled, false);

        // reader.addCompletor(new FlexibleCompletor(prefixes));
        Class<?> completorClass = Kit.classOrNull(classLoader,
                "jline.Completor");
        Object completor = Proxy.newProxyInstance(classLoader,
                new Class[] { completorClass },
                new FlexibleCompletor(completorClass, scope));
        instance.resolve(ConsoleReaderMethodsV1.addCompletor, completorClass);
        instance.invoke(ConsoleReaderMethodsV1.addCompletor, completor);

        return new JLineShellConsoleV1(instance, cs);
    }

    private static JLineShellConsoleV2 getJLineShellConsoleV2(
            ClassLoader classLoader, Class<?> readerClass, Scriptable scope,
            Charset cs) throws NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        // ConsoleReader reader = new ConsoleReader();
        Constructor<?> c = readerClass.getConstructor();
        Object reader = c.newInstance();
        Instance<ConsoleReaderMethodsV2> instance = new Instance<ConsoleReaderMethodsV2>(
                reader, ConsoleReaderMethodsV2.class);

        // reader.setBellEnabled(false);
        instance.invoke(ConsoleReaderMethodsV2.setBellEnabled, false);

        // reader.addCompleter(new FlexibleCompletor(prefixes));
        Class<?> completorClass = Kit.classOrNull(classLoader,
                "jline.console.completer.Completer");
        Object completor = Proxy.newProxyInstance(classLoader,
                new Class[] { completorClass },
                new FlexibleCompletor(completorClass, scope));
        instance.resolve(ConsoleReaderMethodsV2.addCompleter, completorClass);
        instance.invoke(ConsoleReaderMethodsV2.addCompleter, completor);

        return new JLineShellConsoleV2(instance, cs);
    }
}

/**
* The completors provided with JLine are pretty uptight, they only
* complete on a line that it can fully recognize (only composed of
* completed strings). This one completes whatever came before.
*/
class FlexibleCompletor implements java.lang.reflect.InvocationHandler {
    private Method completeMethod;
    private Scriptable global;

    FlexibleCompletor(Class<?> completorClass, Scriptable global)
        throws NoSuchMethodException
    {
        this.global = global;
        this.completeMethod = completorClass.getMethod("complete", String.class,
                Integer.TYPE, List.class);
    }

    @SuppressWarnings({"unchecked"})
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (method.equals(this.completeMethod)) {
            int result = complete((String)args[0], ((Integer) args[1]).intValue(),
                    (List<String>) args[2]);
            return Integer.valueOf(result);
        }
        throw new NoSuchMethodError(method.toString());
    }

    public int complete(String buffer, int cursor, List<String> candidates) {
        // Starting from "cursor" at the end of the buffer, look backward
        // and collect a list of identifiers separated by (possibly zero)
        // dots. Then look up each identifier in turn until getting to the
        // last, presumably incomplete fragment. Then enumerate all the
        // properties of the last object and find any that have the
        // fragment as a prefix and return those for autocompletion.
        int m = cursor - 1;
        while (m >= 0) {
            char c = buffer.charAt(m);
            if (!Character.isJavaIdentifierPart(c) && c != '.')
                break;
            m--;
        }
        String namesAndDots = buffer.substring(m+1, cursor);
        String[] names = namesAndDots.split("\\.", -1);
        Scriptable obj = this.global;
        for (int i=0; i < names.length - 1; i++) {
            Object val = obj.get(names[i], global);
            if (val instanceof Scriptable)
                obj = (Scriptable) val;
            else {
                return buffer.length(); // no matches
            }
        }
        Object[] ids = (obj instanceof ScriptableObject)
                       ? ((ScriptableObject)obj).getAllIds()
                       : obj.getIds();
        String lastPart = names[names.length-1];
        for (int i=0; i < ids.length; i++) {
            if (!(ids[i] instanceof String))
                continue;
            String id = (String)ids[i];
            if (id.startsWith(lastPart)) {
                if (obj.get(id, obj) instanceof Function)
                    id += "(";
                candidates.add(id);
            }
        }
        return buffer.length() - lastPart.length();
    }
}
