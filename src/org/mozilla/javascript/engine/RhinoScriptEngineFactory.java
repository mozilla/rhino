/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.engine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import org.mozilla.javascript.Context;

/**
 * This is an implementation of the standard Java "ScriptEngine" for Rhino. If the Rhino engine
 * (typically in the form of the "rhino-engine" JAR) is in the classpath, then this script engine
 * will be activated.
 *
 * <p>See the list of constants in this class for the list of language names, file extensions, and
 * MIME types that this engine supports. This list is essentially the same as the list supported in
 * the Nashorn script engine that was included in Java 8.
 *
 * <p>Since this engine and Nashorn support the same language and file extensions, then unless you
 * are sure you are running in an environment that has Nashorn, the best way to get this engine is
 * to call ScriptEngine.getEngineByName("rhino") to ask for Rhino directly.
 */
public class RhinoScriptEngineFactory implements ScriptEngineFactory {

    public static final String NAME = "rhino";
    private static final String LANGUAGE = "javascript";
    private static final List<String> NAMES =
            Arrays.asList("rhino", "Rhino", "javascript", "JavaScript");
    private static final List<String> EXTENSIONS = Collections.singletonList("js");
    private static final List<String> MIME_TYPES =
            Arrays.asList(
                    "application/javascript",
                    "application/ecmascript",
                    "text/javascript",
                    "text/ecmascript");
    private static final String LANGUAGE_VERSION =
            String.valueOf(RhinoScriptEngine.DEFAULT_LANGUAGE_VERSION);

    @Override
    public String getEngineName() {
        return NAME;
    }

    @Override
    public String getEngineVersion() {
        try (Context cx = Context.enter()) {
            String v = cx.getImplementationVersion();
            return (v == null ? "unknown" : v);
        }
    }

    @Override
    public List<String> getExtensions() {
        return EXTENSIONS;
    }

    @Override
    public List<String> getMimeTypes() {
        return MIME_TYPES;
    }

    @Override
    public List<String> getNames() {
        return NAMES;
    }

    @Override
    public String getLanguageName() {
        return LANGUAGE;
    }

    @Override
    public String getLanguageVersion() {
        return LANGUAGE_VERSION;
    }

    @Override
    public Object getParameter(String key) {
        switch (key) {
            case ScriptEngine.ENGINE:
                return getEngineName();
            case ScriptEngine.ENGINE_VERSION:
                return getEngineVersion();
            case ScriptEngine.LANGUAGE:
                return getLanguageName();
            case ScriptEngine.LANGUAGE_VERSION:
                return getLanguageVersion();
            case ScriptEngine.NAME:
                return NAME;
            case "THREADING":
                // Engines are explicitly not thread-safe
                return null;
            default:
                return null;
        }
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args) {
        StringBuilder sb = new StringBuilder();
        sb.append(obj).append('.').append(m).append('(');
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(args[i]);
        }
        sb.append(");");
        return sb.toString();
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        return "print('" + toDisplay + "');";
    }

    @Override
    public String getProgram(String... statements) {
        StringBuilder sb = new StringBuilder();
        for (String stmt : statements) {
            sb.append(stmt).append(";\n");
        }
        return sb.toString();
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new RhinoScriptEngine(this);
    }
}
