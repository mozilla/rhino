/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NativeConsole extends IdScriptableObject {
    private static final long serialVersionUID = 5694613212458273057L;

    private static final Object CONSOLE_TAG = "Console";

    private static final String DEFAULT_LABEL = "default";

    private static final Pattern FMT_REG = Pattern.compile("%[sfdioO%]");

    private final Map<String, Long> timers = new ConcurrentHashMap<>();

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    private final ConsolePrinter printer;

    public enum Level {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    public interface ConsolePrinter extends Serializable {
        void print(Context cx, Scriptable scope, Level level, Object[] args);
    }

    public static void init(Scriptable scope, boolean sealed, ConsolePrinter printer) {
        NativeConsole obj = new NativeConsole(printer);
        obj.activatePrototypeMap(MAX_ID);
        obj.setPrototype(getObjectPrototype(scope));
        obj.setParentScope(scope);
        if (sealed) {
            obj.sealObject();
        }
        ScriptableObject.defineProperty(scope, "console", obj, ScriptableObject.DONTENUM);
    }

    private NativeConsole(ConsolePrinter printer) {
        this.printer = printer;
    }

    @Override
    public String getClassName() {
        return "Console";
    }

    @Override
    protected void initPrototypeId(int id) {
        if (id > LAST_METHOD_ID) {
            throw new IllegalStateException(String.valueOf(id));
        }

        String name;
        int arity;
        switch (id) {
            case Id_toSource:
                arity = 0;
                name = "toSource";
                break;
            case Id_trace:
                arity = 1;
                name = "trace";
                break;
            case Id_debug:
                arity = 1;
                name = "debug";
                break;
            case Id_log:
                arity = 1;
                name = "log";
                break;
            case Id_info:
                arity = 1;
                name = "info";
                break;
            case Id_warn:
                arity = 1;
                name = "warn";
                break;
            case Id_error:
                arity = 1;
                name = "error";
                break;
            case Id_assert:
                arity = 2;
                name = "assert";
                break;
            case Id_count:
                arity = 1;
                name = "count";
                break;
            case Id_countReset:
                arity = 1;
                name = "countReset";
                break;
            case Id_time:
                arity = 1;
                name = "time";
                break;
            case Id_timeEnd:
                arity = 1;
                name = "timeEnd";
                break;
            case Id_timeLog:
                arity = 2;
                name = "timeLog";
                break;
            default:
                throw new IllegalStateException(String.valueOf(id));
        }
        initPrototypeMethod(CONSOLE_TAG, id, name, arity);
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(CONSOLE_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }

        int methodId = f.methodId();
        switch (methodId) {
            case Id_toSource:
                return "Console";

            case Id_trace:
                printer.print(cx, scope, Level.TRACE, args);
                break;

            case Id_debug:
                printer.print(cx, scope, Level.DEBUG, args);
                break;

            case Id_log:
            case Id_info:
                printer.print(cx, scope, Level.INFO, args);
                break;

            case Id_warn:
                printer.print(cx, scope, Level.WARN, args);
                break;

            case Id_error:
                printer.print(cx, scope, Level.ERROR, args);
                break;

            case Id_assert:
                jsAssert(cx, scope, args);
                break;

            case Id_count:
                count(cx, scope, args);
                break;

            case Id_countReset:
                countReset(cx, scope, args);
                break;

            case Id_time:
                time(cx, scope, args);
                break;

            case Id_timeEnd:
                timeEnd(cx, scope, args);
                break;

            case Id_timeLog:
                timeLog(cx, scope, args);
                break;

            default:
                throw new IllegalStateException(String.valueOf(methodId));
        }

        return Undefined.instance;
    }

    private void print(Context cx, Scriptable scope, Level level, String msg) {
        printer.print(cx, scope, level, new String[] {msg});
    }

    public static String format(Context cx, Scriptable scope, Object[] args) {
        String msg = ScriptRuntime.toString(args[0]);
        if (msg == null || msg.length() == 0) {
            return "";
        }

        int argIndex = 1;
        Matcher matcher = FMT_REG.matcher(msg);
        StringBuffer buffer = new StringBuffer(msg.length() * 2);
        while (matcher.find()) {
            String placeHolder = matcher.group();
            String replaceArg;
            switch (placeHolder) {
                case "%%":
                    replaceArg = "%";
                    break;

                case "%s":
                    replaceArg = ScriptRuntime.toString(args, argIndex);
                    break;

                case "%d":
                case "%i":
                    double number = ScriptRuntime.toNumber(args, argIndex);
                    if (Double.isInfinite(number) || Double.isNaN(number)) {
                        replaceArg = "0";
                    } else {
                        replaceArg = String.valueOf(((Double) number).intValue());
                    }
                    break;

                case "%f":
                    replaceArg = String.valueOf(ScriptRuntime.toNumber(args, argIndex));
                    break;

                case "%o":
                case "%O":
                    replaceArg = formatObj(cx, scope, args, argIndex);
                    break;

                default:
                    replaceArg = "";
                    break;
            }

            argIndex++;
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replaceArg));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    private static String formatObj(Context cx, Scriptable scope, Object[] args, int argIndex) {
        if (argIndex >= args.length) {
            return "";
        }

        Object arg = args[argIndex];
        if (arg == null) {
            return "null";
        }
        if (arg == Undefined.instance || arg == Undefined.SCRIPTABLE_UNDEFINED) {
            return "undefined";
        }

        return ScriptRuntime.toString(NativeJSON.stringify(cx, scope, arg, null, null));
    }

    private void jsAssert(Context cx, Scriptable scope, Object[] args) {
        if (args != null && args.length > 0 && ScriptRuntime.toBoolean(args[0])) {
            return;
        }

        StringBuilder msg = new StringBuilder("Assertion failed:");
        if (args != null && args.length > 1) {
            for (int i = 1; i < args.length; ++i) {
                msg.append(" ").append(ScriptRuntime.toString(args[i]));
            }
        } else {
            msg.append(" console.assert");
        }

        print(cx, scope, Level.ERROR, msg.toString());
    }

    private void count(Context cx, Scriptable scope, Object[] args) {
        String label = args.length > 0 ? ScriptRuntime.toString(args[0]) : DEFAULT_LABEL;
        int count = counters.computeIfAbsent(label, l -> new AtomicInteger(0)).incrementAndGet();
        print(cx, scope, Level.INFO, label + ": " + count);
    }

    private void countReset(Context cx, Scriptable scope, Object[] args) {
        String label = args.length > 0 ? ScriptRuntime.toString(args[0]) : DEFAULT_LABEL;
        AtomicInteger counter = counters.remove(label);
        if (counter == null) {
            print(cx, scope, Level.WARN, "Count for '" + label + "' does not exist.");
        }
    }

    private void time(Context cx, Scriptable scope, Object[] args) {
        String label = args.length > 0 ? ScriptRuntime.toString(args[0]) : DEFAULT_LABEL;
        Long start = timers.get(label);
        if (start != null) {
            print(cx, scope, Level.WARN, "Timer '" + label + "' already exists.");
            return;
        }
        timers.put(label, System.nanoTime());
    }

    private void timeEnd(Context cx, Scriptable scope, Object[] args) {
        String label = args.length > 0 ? ScriptRuntime.toString(args[0]) : DEFAULT_LABEL;
        Long start = timers.remove(label);
        if (start == null) {
            print(cx, scope, Level.WARN, "Timer '" + label + "' does not exist.");
            return;
        }
        print(cx, scope, Level.INFO, label + ": " + nano2Milli(System.nanoTime() - start) + "ms");
    }

    private void timeLog(Context cx, Scriptable scope, Object[] args) {
        String label = args.length > 0 ? ScriptRuntime.toString(args[0]) : DEFAULT_LABEL;
        Long start = timers.get(label);
        if (start == null) {
            print(cx, scope, Level.WARN, "Timer '" + label + "' does not exist.");
            return;
        }
        StringBuilder msg =
                new StringBuilder(label + ": " + nano2Milli(System.nanoTime() - start) + "ms");

        if (args.length > 1) {
            for (int i = 1; i < args.length; i++) {
                msg.append(" ").append(ScriptRuntime.toString(args[i]));
            }
        }
        print(cx, scope, Level.INFO, msg.toString());
    }

    private double nano2Milli(Long nano) {
        return nano / 1000000D;
    }

    // #string_id_map#

    @Override
    protected int findPrototypeId(String s) {
        int id;
        // #generated# Last update: 2021-11-10 14:43:23 CST
        L0:
        {
            id = 0;
            String X = null;
            int c;
            L:
            switch (s.length()) {
                case 3:
                    X = "log";
                    id = Id_log;
                    break L;
                case 4:
                    c = s.charAt(0);
                    if (c == 'i') {
                        X = "info";
                        id = Id_info;
                    } else if (c == 't') {
                        X = "time";
                        id = Id_time;
                    } else if (c == 'w') {
                        X = "warn";
                        id = Id_warn;
                    }
                    break L;
                case 5:
                    switch (s.charAt(0)) {
                        case 'c':
                            X = "count";
                            id = Id_count;
                            break L;
                        case 'd':
                            X = "debug";
                            id = Id_debug;
                            break L;
                        case 'e':
                            X = "error";
                            id = Id_error;
                            break L;
                        case 't':
                            X = "trace";
                            id = Id_trace;
                            break L;
                    }
                    break L;
                case 6:
                    X = "assert";
                    id = Id_assert;
                    break L;
                case 7:
                    c = s.charAt(4);
                    if (c == 'E') {
                        X = "timeEnd";
                        id = Id_timeEnd;
                    } else if (c == 'L') {
                        X = "timeLog";
                        id = Id_timeLog;
                    }
                    break L;
                case 8:
                    X = "toSource";
                    id = Id_toSource;
                    break L;
                case 10:
                    X = "countReset";
                    id = Id_countReset;
                    break L;
            }
            if (X != null && X != s && !X.equals(s)) id = 0;
            break L0;
        }
        // #/generated#
        return id;
    }

    private static final int Id_toSource = 1,
            Id_trace = 2,
            Id_debug = 3,
            Id_log = 4,
            Id_info = 5,
            Id_warn = 6,
            Id_error = 7,
            Id_assert = 8,
            Id_count = 9,
            Id_countReset = 10,
            Id_time = 11,
            Id_timeEnd = 12,
            Id_timeLog = 13,
            LAST_METHOD_ID = 13,
            MAX_ID = 13;

    // #/string_id_map#
}
