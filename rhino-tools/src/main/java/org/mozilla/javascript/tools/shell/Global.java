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
//
//        defineProperty(this, "defineClass", 1, Global::defineClass, DONTENUM, DONTENUM | READONLY);
//        defineProperty(this, "deserialize", 1, Global::deserialize, DONTENUM, DONTENUM | READONLY);
//        defineProperty(this, "doctest", 1, Global::doctest, DONTENUM, DONTENUM | READONLY);
//        defineProperty(this, "gc", 0, Global::gc, DONTENUM, DONTENUM | READONLY);
//        defineProperty(this, "help", 0, Global::help, DONTENUM, DONTENUM | READONLY);
//        defineProperty(this, "load", 0, Global::load, DONTENUM, DONTENUM | READONLY);
//        defineProperty(this, "loadClass", 1, Global::loadClass, DONTENUM, DONTENUM | READONLY);
//        defineProperty(this, "print", 0, Global::print, DONTENUM, DONTENUM | READONLY);
//        defineProperty(this, "quit", 0, Global::quit, DONTENUM, DONTENUM | READONLY);
//        defineProperty(this, "readline", 0, Global::readline, DONTENUM, DONTENUM | READONLY);
 //       defineProperty(this, "readFile", 1, Global::readFile, DONTENUM, DONTENUM | READONLY);
        defineProperty(this, "readUrl", 1, Global::readUrl, DONTENUM, DONTENUM | READONLY);
        //defineProperty(this, "runCommand", 1, Global::runCommand, DONTENUM, DONTENUM | READONLY);
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
        Charset charset = Charset.defaultCharset();

        try {
            URL urlObj = new URL(url);
            URLConnection uc = urlObj.openConnection();
            try (InputStream is = uc.getInputStream()) {
                return new String(is.readAllBytes(), charset);
            }
        } catch (IOException e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
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
