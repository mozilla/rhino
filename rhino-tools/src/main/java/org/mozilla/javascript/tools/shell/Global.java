/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tools.shell;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeConsole;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Synchronizer;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.RequireBuilder;
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;
import org.mozilla.javascript.serialize.ScriptableInputStream;
import org.mozilla.javascript.serialize.ScriptableOutputStream;
import org.mozilla.javascript.tools.ToolErrorReporter;

/**
 * This class provides for sharing functions across multiple threads. This is of particular interest
 * to server applications.
 *
 * @author Norris Boyd
 */
public class Global extends ImporterTopLevel {
    static final long serialVersionUID = 4029130780977538005L;

    NativeArray history;
    boolean attemptedJLineLoad;
    private ShellConsole console;
    private InputStream inStream;
    private PrintStream outStream;
    private PrintStream errStream;
    private boolean sealedStdLib = false;
    boolean initialized;
    private QuitAction quitAction;
    private String[] prompts = {"js> ", "  > "};
    private HashMap<String, String> doctestCanonicalizations;

    public Global() {}

    public Global(Context cx) {
        init(cx);
    }

    public boolean isInitialized() {
        return initialized;
    }

    /** Set the action to call from quit(). */
    public void initQuitAction(QuitAction quitAction) {
        if (quitAction == null) throw new IllegalArgumentException("quitAction is null");
        if (this.quitAction != null) throw new IllegalArgumentException("The method is once-call.");

        this.quitAction = quitAction;
    }

    public void init(ContextFactory factory) {
        factory.call(
                cx -> {
                    init(cx);
                    return null;
                });
    }

    public void init(Context cx) {
        // Define some global functions particular to the shell. Note
        // that these functions are not part of ECMA.
        initStandardObjects(cx, sealedStdLib);
        NativeConsole.init(this, sealedStdLib, new ShellConsolePrinter());

        defineProperty(this, "defineClass", 1, Global::defineClass, DONTENUM, DONTENUM | READONLY);
        defineProperty(this, "deserialize", 1, Global::deserialize, DONTENUM, DONTENUM | READONLY);
        defineProperty(this, "doctest", 1, Global::doctest, DONTENUM, DONTENUM | READONLY);
        defineProperty(this, "gc", 0, Global::gc, DONTENUM, DONTENUM | READONLY);
        defineProperty(this, "help", 0, Global::help, DONTENUM, DONTENUM | READONLY);
        defineProperty(this, "load", 0, Global::load, DONTENUM, DONTENUM | READONLY);
        defineProperty(this, "loadClass", 1, Global::loadClass, DONTENUM, DONTENUM | READONLY);
        defineProperty(this, "print", 0, Global::print, DONTENUM, DONTENUM | READONLY);
        defineProperty(this, "quit", 0, Global::quit, DONTENUM, DONTENUM | READONLY);
        defineProperty(this, "readline", 0, Global::readline, DONTENUM, DONTENUM | READONLY);
        defineProperty(this, "readFile", 1, Global::readFile, DONTENUM, DONTENUM | READONLY);
        defineProperty(this, "readUrl", 1, Global::readUrl, DONTENUM, DONTENUM | READONLY);
        defineProperty(this, "runCommand", 1, Global::runCommand, DONTENUM, DONTENUM | READONLY);
        //        defineProperty(this, "seal", 0, Global::seal, DONTENUM, DONTENUM | READONLY);
        //        defineProperty(this, "serialize", 2, Global::serialize, DONTENUM, DONTENUM |
        // READONLY);
        //        defineProperty(this, "spawn", 1, Global::spawn, DONTENUM, DONTENUM | READONLY);
        //        defineProperty(this, "sync", 1, Global::sync, DONTENUM, DONTENUM | READONLY);
        //        defineProperty(this, "toint32", 0, Global::toint32, DONTENUM, DONTENUM |
        // READONLY);
        //        defineProperty(this, "version", 0, Global::version, DONTENUM, DONTENUM |
        // READONLY);
        //        defineProperty(this, "write", 0, Global::write, DONTENUM, DONTENUM | READONLY);

        // Set up "environment" in the global scope to provide access to the
        // System environment variables.
        Environment.defineClass(this);
        Environment environment = new Environment(this);
        defineProperty("environment", environment, DONTENUM);

        history = (NativeArray) cx.newArray(this, 0);
        defineProperty("history", history, DONTENUM);

        initialized = true;
    }

    public Require installRequire(Context cx, List<String> modulePath, boolean sandboxed) {
        RequireBuilder rb = new RequireBuilder();
        rb.setSandboxed(sandboxed);
        List<URI> uris = new ArrayList<URI>();
        if (modulePath != null) {
            for (String path : modulePath) {
                try {
                    URI uri = new URI(path);
                    if (!uri.isAbsolute()) {
                        // call resolve("") to canonify the path
                        uri = new File(path).toURI().resolve("");
                    }
                    if (!uri.toString().endsWith("/")) {
                        // make sure URI always terminates with slash to
                        // avoid loading from unintended locations
                        uri = new URI(uri + "/");
                    }
                    uris.add(uri);
                } catch (URISyntaxException usx) {
                    throw new RuntimeException(usx);
                }
            }
        }
        rb.setModuleScriptProvider(
                new SoftCachingModuleScriptProvider(new UrlModuleSourceProvider(uris, null)));
        Require require = rb.createRequire(cx, this);
        require.install(this);
        return require;
    }

    /**
     * Print a help message.
     *
     * <p>This method is defined as a JavaScript function.
     */
    private static Object help(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        PrintStream out = getInstance(thisObj).getOut();
        out.println(ToolErrorReporter.getMessage("msg.help"));
        return Undefined.instance;
    }

    private static Object gc(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        System.gc();
        return Undefined.instance;
    }

    /**
     * Print the string values of its arguments.
     *
     * <p>This method is defined as a JavaScript function. Note that its arguments are of the
     * "varargs" form, which allows it to handle an arbitrary number of arguments supplied to the
     * JavaScript function.
     */
    private static Object print(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return doPrint(args, thisObj, true);
    }

    /** Print just as in "print," but without the trailing newline. */
    private static Object write(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return doPrint(args, thisObj, false);
    }

    private static Object doPrint(Object[] args, Scriptable thisObj, boolean newline) {
        PrintStream out = getInstance(thisObj).getOut();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) out.print(" ");

            // Convert the arbitrary JavaScript value into a string form.
            String s = Context.toString(args[i]);

            out.print(s);
        }
        if (newline) {
            out.println();
        }
        return Context.getUndefinedValue();
    }

    /**
     * Call embedding-specific quit action passing its argument as int32 exit code.
     *
     * <p>This method is defined as a JavaScript function.
     */
    private static Object quit(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Global global = getInstance(thisObj);
        if (global.quitAction != null) {
            int exitCode = (args.length == 0 ? 0 : ScriptRuntime.toInt32(args[0]));
            global.quitAction.quit(cx, exitCode);
        }
        return Undefined.instance;
    }

    /**
     * Get and set the language version.
     *
     * <p>This method is defined as a JavaScript function.
     */
    private static double version(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length > 0) {
            double d = Context.toNumber(args[0]);
            cx.setLanguageVersion((int) d);
        }
        return cx.getLanguageVersion();
    }

    /**
     * Load and execute a set of JavaScript source files.
     *
     * <p>This method is defined as a JavaScript function.
     */
    private static Object load(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        for (Object arg : args) {
            String file = Context.toString(arg);
            try {
                Main.processFile(cx, thisObj, file);
            } catch (IOException ioex) {
                String msg =
                        ToolErrorReporter.getMessage(
                                "msg.couldnt.read.source", file, ioex.getMessage());
                throw Context.reportRuntimeError(msg);
            } catch (VirtualMachineError ex) {
                // Treat StackOverflow and OutOfMemory as runtime errors
                ex.printStackTrace();
                String msg = ToolErrorReporter.getMessage("msg.uncaughtJSException", ex.toString());
                throw Context.reportRuntimeError(msg);
            }
        }
        return Undefined.instance;
    }

    /**
     * Load a Java class that defines a JavaScript object using the conventions outlined in
     * ScriptableObject.defineClass.
     *
     * <p>This method is defined as a JavaScript function.
     *
     * @throws WrappedException (cause: IllegalAccessException) if access is not available to a
     *     reflected class member
     * @throws WrappedException (cause: InstantiationException) if unable to instantiate the named
     *     class
     * @throws WrappedException (cause: InvocationTargetException) if an exception is thrown during
     *     execution of methods of the named class
     * @see org.mozilla.javascript.ScriptableObject#defineClass(Scriptable, Class)
     */
    @SuppressWarnings({"unchecked"})
    private static Object defineClass(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Class<?> clazz = getClass(args);
        if (!Scriptable.class.isAssignableFrom(clazz)) {
            throw reportRuntimeError("msg.must.implement.Scriptable");
        }
        try {
            ScriptableObject.defineClass(thisObj, (Class<? extends Scriptable>) clazz);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
        return Undefined.instance;
    }

    /**
     * Load and execute a script compiled to a class file.
     *
     * <p>This method is defined as a JavaScript function. When called as a JavaScript function, a
     * single argument is expected. This argument should be the name of a class that implements the
     * Script interface, as will any script compiled by jsc.
     *
     * @throws WrappedException (cause: IllegalAccessException) if access is not available to the
     *     class
     * @throws WrappedException (cause: InstantiationException) if unable to instantiate the named
     *     class
     */
    private static Object loadClass(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Class<?> clazz = getClass(args);
        if (!Script.class.isAssignableFrom(clazz)) {
            throw reportRuntimeError("msg.must.implement.Script");
        }
        Script script = null;
        try {
            script = (Script) clazz.getDeclaredConstructor().newInstance();
        } catch (IllegalAccessException
                | InstantiationException
                | NoSuchMethodException
                | InvocationTargetException e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
        script.exec(cx, thisObj, thisObj);
        return Undefined.instance;
    }

    private static Class<?> getClass(Object[] args) {
        if (args.length == 0) {
            throw reportRuntimeError("msg.expected.string.arg");
        }
        Object arg0 = args[0];
        if (arg0 instanceof Wrapper) {
            Object wrapped = ((Wrapper) arg0).unwrap();
            if (wrapped instanceof Class) return (Class<?>) wrapped;
        }
        String className = Context.toString(arg0);
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException cnfe) {
            throw reportRuntimeError("msg.class.not.found", className);
        }
    }

    private static Object serialize(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length < 2) {
            throw Context.reportRuntimeError(
                    "Expected an object to serialize and a filename to write "
                            + "the serialization to");
        }
        Object obj = args[0];
        String filename = Context.toString(args[1]);
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            Scriptable topLevelScope = ScriptableObject.getTopLevelScope(thisObj);
            try (ScriptableOutputStream out = new ScriptableOutputStream(fos, topLevelScope)) {
                out.writeObject(obj);
            }
        } catch (IOException e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
        return Undefined.instance;
    }

    private static Object deserialize(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length < 1) {
            throw Context.reportRuntimeError("Expected a filename to read the serialization from");
        }
        String filename = Context.toString(args[0]);
        try (FileInputStream fis = new FileInputStream(filename)) {
            Scriptable topLevelScope = ScriptableObject.getTopLevelScope(thisObj);
            try (ObjectInputStream in = new ScriptableInputStream(fis, topLevelScope)) {
                Object deserialized = in.readObject();
                return Context.toObject(deserialized, topLevelScope);
            }
        } catch (ClassNotFoundException | IOException e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
    }

    public String[] getPrompts(Context cx) {
        if (ScriptableObject.hasProperty(this, "prompts")) {
            Object promptsJS = ScriptableObject.getProperty(this, "prompts");
            if (promptsJS instanceof Scriptable) {
                Scriptable s = (Scriptable) promptsJS;
                if (ScriptableObject.hasProperty(s, 0) && ScriptableObject.hasProperty(s, 1)) {
                    Object elem0 = ScriptableObject.getProperty(s, 0);
                    if (elem0 instanceof Function) {
                        elem0 = ((Function) elem0).call(cx, this, s, new Object[0]);
                    }
                    prompts[0] = Context.toString(elem0);
                    Object elem1 = ScriptableObject.getProperty(s, 1);
                    if (elem1 instanceof Function) {
                        elem1 = ((Function) elem1).call(cx, this, s, new Object[0]);
                    }
                    prompts[1] = Context.toString(elem1);
                }
            }
        }
        return prompts;
    }

    /**
     * Example: doctest("js&gt; function f() {\n &gt; return 3;\n &gt; }\njs&gt; f();\n3\n");
     * returns 2 (since 2 tests were executed).
     */
    private static Object doctest(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length == 0) {
            return Boolean.FALSE;
        }
        String session = Context.toString(args[0]);
        Global global = getInstance(thisObj);
        return global.runDoctest(cx, global, session, null, 0);
    }

    @SuppressWarnings("AndroidJdkLibsChecker")
    public int runDoctest(
            Context cx, Scriptable scope, String session, String sourceName, int lineNumber) {
        doctestCanonicalizations = new HashMap<String, String>();
        String[] lines = session.split("\r\n?|\n", -1);
        String prompt0 = this.prompts[0].trim();
        String prompt1 = this.prompts[1].trim();
        int testCount = 0;
        int i = 0;
        while (i < lines.length && !lines[i].trim().startsWith(prompt0)) {
            i++; // skip lines that don't look like shell sessions
        }
        while (i < lines.length) {
            StringBuilder inputString =
                    new StringBuilder(lines[i].trim().substring(prompt0.length()));
            inputString.append('\n');
            i++;
            while (i < lines.length && lines[i].trim().startsWith(prompt1)) {
                inputString.append(lines[i].trim().substring(prompt1.length()));
                inputString.append('\n');
                i++;
            }
            StringBuilder expectedString = new StringBuilder();
            while (i < lines.length && !lines[i].trim().startsWith(prompt0)) {
                expectedString.append(lines[i]).append('\n');
                i++;
            }
            PrintStream savedOut = this.getOut();
            PrintStream savedErr = this.getErr();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            this.setOut(new PrintStream(out));
            this.setErr(new PrintStream(err));
            String resultString = "";
            ErrorReporter savedErrorReporter = cx.getErrorReporter();

            cx.setErrorReporter(new ToolErrorReporter(false, this.getErr()));
            try {
                testCount++;
                String finalInputString = inputString.toString();
                Object result =
                        cx.evaluateString(scope, finalInputString, "doctest input", 1, null);
                if (result != Context.getUndefinedValue()
                        && !(result instanceof Function
                                && finalInputString.trim().startsWith("function"))) {
                    resultString = Context.toString(result);
                }
            } catch (RhinoException e) {
                ToolErrorReporter.reportException(cx.getErrorReporter(), e);
            } finally {
                this.setOut(savedOut);
                this.setErr(savedErr);
                cx.setErrorReporter(savedErrorReporter);
                resultString +=
                        err.toString(StandardCharsets.UTF_8) + out.toString(StandardCharsets.UTF_8);
            }

            if (!doctestOutputMatches(expectedString.toString(), resultString)) {
                String message =
                        "doctest failure running:\n"
                                + inputString
                                + "expected: "
                                + expectedString
                                + "actual: "
                                + resultString
                                + "\n";
                if (sourceName != null) {
                    throw Context.reportRuntimeError(
                            message, sourceName, lineNumber + i - 1, null, 0);
                } else {
                    throw Context.reportRuntimeError(message);
                }
            }
        }
        return testCount;
    }

    /**
     * Compare actual result of doctest to expected, modulo some acceptable differences. Currently
     * just trims the strings before comparing, but should ignore differences in line numbers for
     * error messages for example.
     *
     * @param expected the expected string
     * @param actual the actual string
     * @return true iff actual matches expected modulo some acceptable differences
     */
    private boolean doctestOutputMatches(String expected, String actual) {
        expected = expected.trim();
        actual = actual.trim().replace("\r\n", "\n");
        if (expected.equals(actual)) return true;
        for (Map.Entry<String, String> entry : doctestCanonicalizations.entrySet()) {
            expected = expected.replace(entry.getKey(), entry.getValue());
        }
        if (expected.equals(actual)) return true;
        // java.lang.Object.toString() prints out a unique hex number associated
        // with each object. This number changes from run to run, so we want to
        // ignore differences between these numbers in the output. We search for a
        // regexp that matches the hex number preceded by '@', then enter mappings into
        // "doctestCanonicalizations" so that we ensure that the mappings are
        // consistent within a session.
        Pattern p = Pattern.compile("@[0-9a-fA-F]+");
        Matcher expectedMatcher = p.matcher(expected);
        Matcher actualMatcher = p.matcher(actual);
        for (; ; ) {
            if (!expectedMatcher.find()) return false;
            if (!actualMatcher.find()) return false;
            if (actualMatcher.start() != expectedMatcher.start()) return false;
            int start = expectedMatcher.start();
            if (!expected.substring(0, start).equals(actual.substring(0, start))) return false;
            String expectedGroup = expectedMatcher.group();
            String actualGroup = actualMatcher.group();
            String mapping = doctestCanonicalizations.get(expectedGroup);
            if (mapping == null) {
                doctestCanonicalizations.put(expectedGroup, actualGroup);
                expected = expected.replace(expectedGroup, actualGroup);
            } else if (!actualGroup.equals(mapping)) {
                return false; // wrong object!
            }
            if (expected.equals(actual)) return true;
        }
    }

    /**
     * The spawn function runs a given function or script in a different thread.
     *
     * <p>js&gt; function g() { a = 7; } js&gt; a = 3; 3 js&gt; spawn(g) Thread[Thread-1,5,main]
     * js&gt; a 3
     */
    private static Object spawn(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Runner runner;
        if (args.length != 0 && args[0] instanceof Function) {
            Object[] newArgs = null;
            if (args.length > 1 && args[1] instanceof Scriptable) {
                newArgs = cx.getElements((Scriptable) args[1]);
            }
            if (newArgs == null) {
                newArgs = ScriptRuntime.emptyArgs;
            }
            runner = new Runner(scope, (Function) args[0], newArgs);
        } else if (args.length != 0 && args[0] instanceof Script) {
            runner = new Runner(scope, (Script) args[0]);
        } else {
            throw reportRuntimeError("msg.spawn.args");
        }
        runner.factory = cx.getFactory();
        Thread thread = new Thread(runner);
        thread.start();
        return thread;
    }

    /**
     * The sync function creates a synchronized function (in the sense of a Java synchronized
     * method) from an existing function. The new function synchronizes on the the second argument
     * if it is defined, or otherwise the <code>this</code> object of its invocation. js&gt; var o =
     * { f : sync(function(x) { print("entry"); Packages.java.lang.Thread.sleep(x*1000);
     * print("exit"); })}; js&gt; spawn(function() {o.f(5);}); Thread[Thread-0,5,main] entry js&gt;
     * spawn(function() {o.f(5);}); Thread[Thread-1,5,main] js&gt; exit entry exit
     */
    private static Object sync(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length >= 1 && args.length <= 2 && args[0] instanceof Function) {
            Object syncObject = null;
            if (args.length == 2 && args[1] != Undefined.instance) {
                syncObject = args[1];
            }
            return new Synchronizer((Function) args[0], syncObject);
        } else {
            throw reportRuntimeError("msg.sync.args");
        }
    }

    /**
     * Execute the specified command with the given argument and options as a separate process and
     * return the exit status of the process.
     *
     * <p>Usage:
     *
     * <pre>
     * runCommand(command)
     * runCommand(command, arg1, ..., argN)
     * runCommand(command, arg1, ..., argN, options)
     * </pre>
     *
     * <p>All except the last arguments to runCommand are converted to strings and denote command
     * name and its arguments. If the last argument is a JavaScript object, it is an option object.
     * Otherwise it is converted to string denoting the last argument and options objects assumed to
     * be empty. The following properties of the option object are processed:
     *
     * <ul>
     *   <li><code>args</code> - provides an array of additional command arguments
     *   <li><code>env</code> - explicit environment object. All its enumerable properties define
     *       the corresponding environment variable names.
     *   <li><code>input</code> - the process input. If it is not java.io.InputStream, it is
     *       converted to string and sent to the process as its input. If not specified, no input is
     *       provided to the process.
     *   <li><code>output</code> - the process output instead of java.lang.System.out. If it is not
     *       instance of java.io.OutputStream, the process output is read, converted to a string,
     *       appended to the output property value converted to string and put as the new value of
     *       the output property.
     *   <li><code>err</code> - the process error output instead of java.lang.System.err. If it is
     *       not instance of java.io.OutputStream, the process error output is read, converted to a
     *       string, appended to the err property value converted to string and put as the new value
     *       of the err property.
     *   <li><code>dir</code> - the working direcotry to run the commands.
     * </ul>
     */
    @SuppressWarnings("AndroidJdkLibsChecker")
    private static Object runCommand(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        int L = args.length;
        if (L == 0 || (L == 1 && args[0] instanceof Scriptable)) {
            throw reportRuntimeError("msg.runCommand.bad.args");
        }
        File wd = null;
        InputStream in = null;
        OutputStream out = null, err = null;
        ByteArrayOutputStream outBytes = null, errBytes = null;
        Object outObj = null, errObj = null;
        String[] environment = null;
        Scriptable params = null;
        Object[] addArgs = null;
        try {
            if (args[L - 1] instanceof Scriptable) {
                params = (Scriptable) args[L - 1];
                --L;
                Object envObj = ScriptableObject.getProperty(params, "env");
                if (envObj != Scriptable.NOT_FOUND) {
                    if (envObj == null) {
                        environment = new String[0];
                    } else {
                        if (!(envObj instanceof Scriptable)) {
                            throw reportRuntimeError("msg.runCommand.bad.env");
                        }
                        Scriptable envHash = (Scriptable) envObj;
                        Object[] ids = ScriptableObject.getPropertyIds(envHash);
                        environment = new String[ids.length];
                        for (int i = 0; i != ids.length; ++i) {
                            Object keyObj = ids[i], val;
                            String key;
                            if (keyObj instanceof String) {
                                key = (String) keyObj;
                                val = ScriptableObject.getProperty(envHash, key);
                            } else {
                                int ikey = ((Number) keyObj).intValue();
                                key = Integer.toString(ikey);
                                val = ScriptableObject.getProperty(envHash, ikey);
                            }
                            if (val == ScriptableObject.NOT_FOUND) {
                                val = Undefined.instance;
                            }
                            environment[i] = key + '=' + ScriptRuntime.toString(val);
                        }
                    }
                }
                Object wdObj = ScriptableObject.getProperty(params, "dir");
                if (wdObj != Scriptable.NOT_FOUND) {
                    wd = new File(ScriptRuntime.toString(wdObj));
                }

                Object inObj = ScriptableObject.getProperty(params, "input");
                if (inObj != Scriptable.NOT_FOUND) {
                    in = toInputStream(inObj);
                }
                outObj = ScriptableObject.getProperty(params, "output");
                if (outObj != Scriptable.NOT_FOUND) {
                    out = toOutputStream(outObj);
                    if (out == null) {
                        outBytes = new ByteArrayOutputStream();
                        out = outBytes;
                    }
                }
                errObj = ScriptableObject.getProperty(params, "err");
                if (errObj != Scriptable.NOT_FOUND) {
                    err = toOutputStream(errObj);
                    if (err == null) {
                        errBytes = new ByteArrayOutputStream();
                        err = errBytes;
                    }
                }
                Object addArgsObj = ScriptableObject.getProperty(params, "args");
                if (addArgsObj != Scriptable.NOT_FOUND) {
                    Scriptable s = Context.toObject(addArgsObj, getTopLevelScope(thisObj));
                    addArgs = cx.getElements(s);
                }
            }
            Global global = getInstance(thisObj);
            if (out == null) {
                out = global.getOut();
            }
            if (err == null) {
                err = global.getErr();
            }
            // If no explicit input stream, do not send any input to process,
            // in particular, do not use System.in to avoid deadlocks
            // when waiting for user input to send to process which is already
            // terminated as it is not always possible to interrupt read method.

            String[] cmd = new String[(addArgs == null) ? L : L + addArgs.length];
            for (int i = 0; i != L; ++i) {
                cmd[i] = ScriptRuntime.toString(args[i]);
            }
            if (addArgs != null) {
                for (int i = 0; i != addArgs.length; ++i) {
                    cmd[L + i] = ScriptRuntime.toString(addArgs[i]);
                }
            }

            int exitCode = runProcess(cmd, environment, wd, in, out, err);
            if (outBytes != null) {
                String s =
                        ScriptRuntime.toString(outObj) + outBytes.toString(StandardCharsets.UTF_8);
                ScriptableObject.putProperty(params, "output", s);
            }
            if (errBytes != null) {
                String s =
                        ScriptRuntime.toString(errObj) + errBytes.toString(StandardCharsets.UTF_8);
                ScriptableObject.putProperty(params, "err", s);
            }

            return exitCode;
        } catch (IOException e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
    }

    /** The seal function seals all supplied arguments. */
    private static Object seal(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        for (int i = 0; i != args.length; ++i) {
            Object arg = args[i];
            if (!(arg instanceof ScriptableObject) || arg == Undefined.instance) {
                if (!(arg instanceof Scriptable) || arg == Undefined.instance) {
                    throw reportRuntimeError("msg.shell.seal.not.object");
                } else {
                    throw reportRuntimeError("msg.shell.seal.not.scriptable");
                }
            }
        }

        for (int i = 0; i != args.length; ++i) {
            Object arg = args[i];
            ((ScriptableObject) arg).sealObject();
        }
        return Undefined.instance;
    }

    /**
     * The readFile reads the given file content and convert it to a string using the specified
     * character coding or default character coding if explicit coding argument is not given.
     *
     * <p>Usage:
     *
     * <pre>
     * readFile(filePath)
     * readFile(filePath, charCoding)
     * </pre>
     *
     * <p>The first form converts file's context to string using the default character coding.
     */
    private static Object readFile(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length == 0) {
            throw reportRuntimeError("msg.shell.readFile.bad.args");
        }
        String path = ScriptRuntime.toString(args[0]);
        Charset charset =
                args.length < 2
                        ? Charset.defaultCharset()
                        : Charset.forName(ScriptRuntime.toString(args[1]));

        try {
            // the file path is not sanitized here.And even if it did, it would hardly increase
            // security, since we can access Packages.java.io.File directly
            @SuppressWarnings("codeql")
            File f = new File(path);
            if (!f.exists()) {
                throw new FileNotFoundException("File not found: " + path);
            } else if (!f.canRead()) {
                throw new IOException("Cannot read file: " + path);
            }
            try (InputStream is = new FileInputStream(f)) {
                return new String(is.readAllBytes(), charset);
            }
        } catch (IOException e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
    }

    /**
     * The readUrl opens connection to the given URL, read all its data and converts them to a
     * string using the specified character coding or default character coding if explicit coding
     * argument is not given.
     *
     * <p>Usage:
     *
     * <pre>
     * readUrl(url)
     * readUrl(url, charCoding)
     * </pre>
     *
     * <p>The first form converts file's context to string using the default charCoding.
     */
    private static Object readUrl(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length == 0) {
            throw reportRuntimeError("msg.shell.readUrl.bad.args");
        }
        String url = ScriptRuntime.toString(args[0]);
        Charset charset = args.length < 2 ? null : Charset.forName(ScriptRuntime.toString(args[1]));

        try {
            URL urlObj = new URL(url);
            URLConnection uc = urlObj.openConnection();
            try (InputStream is = uc.getInputStream()) {

                if (charset == null) {
                    String type = uc.getContentType();
                    if (type != null) {
                        charset = getCharsetFromType(type);
                    }
                }
                if (charset == null) {
                    charset = Charset.defaultCharset();
                }
                return new String(is.readAllBytes(), charset);
            }
        } catch (IOException e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
    }

    /** Convert the argument to int32 number. */
    private static Object toint32(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object arg = (args.length != 0 ? args[0] : Undefined.instance);
        if (arg instanceof Integer) return arg;
        return ScriptRuntime.wrapInt(ScriptRuntime.toInt32(arg));
    }

    private boolean loadJLine(Charset cs) {
        if (!attemptedJLineLoad) {
            // Check if we can use JLine for better command line handling
            attemptedJLineLoad = true;
            console = ShellConsole.getConsole(this, cs);
        }
        return console != null;
    }

    public ShellConsole getConsole(Charset cs) {
        if (!loadJLine(cs)) {
            console = ShellConsole.getConsole(getIn(), getErr(), cs);
        }
        return console;
    }

    public InputStream getIn() {
        if (inStream == null && !attemptedJLineLoad) {
            if (loadJLine(Charset.defaultCharset())) {
                inStream = console.getIn();
            }
        }
        return inStream == null ? System.in : inStream;
    }

    public void setIn(InputStream in) {
        inStream = in;
    }

    public PrintStream getOut() {
        return outStream == null ? System.out : outStream;
    }

    public void setOut(PrintStream out) {
        outStream = out;
    }

    public PrintStream getErr() {
        return errStream == null ? System.err : errStream;
    }

    public void setErr(PrintStream err) {
        errStream = err;
    }

    public void setSealedStdLib(boolean value) {
        sealedStdLib = value;
    }

    private static Global getInstance(Scriptable thisObj) {
        if (!(thisObj instanceof Global))
            throw reportRuntimeError("msg.bad.shell.function.scope", String.valueOf(thisObj));
        return (Global) thisObj;
    }

    /**
     * Runs the given process using Runtime.exec(). If any of in, out, err is null, the
     * corresponding process stream will be closed immediately, otherwise it will be closed as soon
     * as all data will be read from/written to process
     *
     * @return Exit value of process.
     * @throws IOException If there was an error executing the process.
     */
    private static int runProcess(
            String[] cmd,
            String[] environment,
            File wd,
            InputStream in,
            OutputStream out,
            OutputStream err)
            throws IOException {
        Process p;
        if (environment == null) {
            p = Runtime.getRuntime().exec(cmd, null, wd);
        } else {
            p = Runtime.getRuntime().exec(cmd, environment, wd);
        }

        try {
            PipeThread inThread = null;
            if (in != null) {
                inThread = new PipeThread(false, in, p.getOutputStream());
                inThread.start();
            } else {
                p.getOutputStream().close();
            }

            PipeThread outThread = null;
            if (out != null) {
                outThread = new PipeThread(true, p.getInputStream(), out);
                outThread.start();
            } else {
                p.getInputStream().close();
            }

            PipeThread errThread = null;
            if (err != null) {
                errThread = new PipeThread(true, p.getErrorStream(), err);
                errThread.start();
            } else {
                p.getErrorStream().close();
            }

            // wait for process completion
            for (; ; ) {
                try {
                    p.waitFor();
                    if (outThread != null) {
                        outThread.join();
                    }
                    if (inThread != null) {
                        inThread.join();
                    }
                    if (errThread != null) {
                        errThread.join();
                    }
                    break;
                } catch (InterruptedException ignore) {
                }
            }

            return p.exitValue();
        } finally {
            p.destroy();
        }
    }

    static void pipe(boolean fromProcess, InputStream from, OutputStream to) throws IOException {
        try {
            final int SIZE = 4096;
            byte[] buffer = new byte[SIZE];
            for (; ; ) {
                int n;
                if (!fromProcess) {
                    n = from.read(buffer, 0, SIZE);
                } else {
                    try {
                        n = from.read(buffer, 0, SIZE);
                    } catch (IOException ex) {
                        // Ignore exception as it can be cause by closed pipe
                        break;
                    }
                }
                if (n < 0) {
                    break;
                }
                if (fromProcess) {
                    to.write(buffer, 0, n);
                    to.flush();
                } else {
                    try {
                        to.write(buffer, 0, n);
                        to.flush();
                    } catch (IOException ex) {
                        // Ignore exception as it can be cause by closed pipe
                        break;
                    }
                }
            }
        } finally {
            try {
                if (fromProcess) {
                    from.close();
                } else {
                    to.close();
                }
            } catch (IOException ex) {
                // Ignore errors on close. On Windows JVM may throw invalid
                // refrence exception if process terminates too fast.
            }
        }
    }

    private static InputStream toInputStream(Object value) throws IOException {
        InputStream is = null;
        String s = null;
        if (value instanceof Wrapper) {
            Object unwrapped = ((Wrapper) value).unwrap();
            if (unwrapped instanceof InputStream) {
                is = (InputStream) unwrapped;
            } else if (unwrapped instanceof byte[]) {
                is = new ByteArrayInputStream((byte[]) unwrapped);
            } else if (unwrapped instanceof Reader) {
                s = readReader((Reader) unwrapped);
            } else if (unwrapped instanceof char[]) {
                s = new String((char[]) unwrapped);
            }
        }
        if (is == null) {
            if (s == null) {
                s = ScriptRuntime.toString(value);
            }
            is = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
        }
        return is;
    }

    private static OutputStream toOutputStream(Object value) {
        OutputStream os = null;
        if (value instanceof Wrapper) {
            Object unwrapped = ((Wrapper) value).unwrap();
            if (unwrapped instanceof OutputStream) {
                os = (OutputStream) unwrapped;
            }
        }
        return os;
    }

    /**
     * The readline reads one line from the standard input. "Prompt" is optional.
     *
     * <p>Usage:
     *
     * <pre>
     * readline(prompt)
     * </pre>
     */
    private static Object readline(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Global self = getInstance(thisObj);
        try {
            if (args.length > 0) {
                return self.console.readLine(Context.toString(args[0]));
            }
            return self.console.readLine();
        } catch (IOException e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
    }

    private static Charset getCharsetFromType(String type) {
        int i = type.indexOf(';');
        if (i >= 0) {
            int end = type.length();
            ++i;
            while (i != end && type.charAt(i) <= ' ') {
                ++i;
            }
            String charset = "charset";
            if (charset.regionMatches(true, 0, type, i, charset.length())) {
                i += charset.length();
                while (i != end && type.charAt(i) <= ' ') {
                    ++i;
                }
                if (i != end && type.charAt(i) == '=') {
                    ++i;
                    while (i != end && type.charAt(i) <= ' ') {
                        ++i;
                    }
                    if (i != end) {
                        // i is at the start of non-empty
                        // charCoding spec
                        while (type.charAt(end - 1) <= ' ') {
                            --end;
                        }
                        return Charset.forName(type.substring(i, end));
                    }
                }
            }
        }
        return null;
    }

    private static String readReader(Reader reader) throws IOException {
        return readReader(reader, 4096);
    }

    private static String readReader(Reader reader, int initialBufferSize) throws IOException {
        char[] buffer = new char[initialBufferSize];
        int offset = 0;
        for (; ; ) {
            int n = reader.read(buffer, offset, buffer.length - offset);
            if (n < 0) {
                break;
            }
            offset += n;
            if (offset == buffer.length) {
                char[] tmp = new char[buffer.length * 2];
                System.arraycopy(buffer, 0, tmp, 0, offset);
                buffer = tmp;
            }
        }
        return new String(buffer, 0, offset);
    }

    static RuntimeException reportRuntimeError(String msgId) {
        String message = ToolErrorReporter.getMessage(msgId);
        return Context.reportRuntimeError(message);
    }

    static RuntimeException reportRuntimeError(String msgId, String msgArg) {
        String message = ToolErrorReporter.getMessage(msgId, msgArg);
        return Context.reportRuntimeError(message);
    }
}

class Runner implements Runnable, ContextAction<Object> {

    Runner(Scriptable scope, Function func, Object[] args) {
        this.scope = scope;
        f = func;
        this.args = args;
    }

    Runner(Scriptable scope, Script script) {
        this.scope = scope;
        s = script;
    }

    @Override
    public void run() {
        factory.call(this);
    }

    @Override
    public Object run(Context cx) {
        if (f != null) return f.call(cx, scope, scope, args);
        else return s.exec(cx, scope, scope);
    }

    ContextFactory factory;
    private Scriptable scope;
    private Function f;
    private Script s;
    private Object[] args;
}

class PipeThread extends Thread {

    PipeThread(boolean fromProcess, InputStream from, OutputStream to) {
        setDaemon(true);
        this.fromProcess = fromProcess;
        this.from = from;
        this.to = to;
    }

    @Override
    public void run() {
        try {
            Global.pipe(fromProcess, from, to);
        } catch (IOException ex) {
            throw Context.throwAsScriptRuntimeEx(ex);
        }
    }

    private boolean fromProcess;
    private InputStream from;
    private OutputStream to;
}
