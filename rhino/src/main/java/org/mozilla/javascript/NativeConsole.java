/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NativeConsole extends IdScriptableObject {
    private static final long serialVersionUID = 5694613212458273057L;

    private static final Object CONSOLE_TAG = "Console";

    private static final String DEFAULT_LABEL = "default";

    private static final Pattern FMT_REG = Pattern.compile("%[sfdioOc%]");

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
        void print(
                Context cx,
                Scriptable scope,
                Level level,
                Object[] args,
                ScriptStackElement[] stack);
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
                {
                    ScriptStackElement[] stack =
                            new EvaluatorException("[object Object]").getScriptStack();
                    printer.print(cx, scope, Level.TRACE, args, stack);
                    break;
                }

            case Id_debug:
                printer.print(cx, scope, Level.DEBUG, args, null);
                break;

            case Id_log:
            case Id_info:
                printer.print(cx, scope, Level.INFO, args, null);
                break;

            case Id_warn:
                printer.print(cx, scope, Level.WARN, args, null);
                break;

            case Id_error:
                printer.print(cx, scope, Level.ERROR, args, null);
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
        printer.print(cx, scope, level, new String[] {msg}, null);
    }

    public static String format(Context cx, Scriptable scope, Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }

        StringBuffer buffer = new StringBuffer();
        int argIndex = 0;

        Object first = args[0];
        if (first instanceof String || first instanceof ConsString) {
            String msg = first.toString();
            Matcher matcher = FMT_REG.matcher(msg);

            argIndex = 1;
            while (matcher.find()) {
                String placeHolder = matcher.group();
                String replaceArg;

                if (placeHolder.equals("%%")) {
                    replaceArg = "%";
                } else if (argIndex >= args.length) {
                    replaceArg = placeHolder;
                    argIndex++;
                } else {
                    Object val = args[argIndex];
                    switch (placeHolder) {
                        case "%s":
                            replaceArg = formatString(val);
                            break;

                        case "%d":
                        case "%i":
                            replaceArg = formatInt(val);
                            break;

                        case "%f":
                            replaceArg = formatFloat(val);
                            break;

                        case "%o":
                        case "%O":
                            replaceArg = formatObj(cx, scope, val);
                            break;

                        // %c is not supported,
                        // simply removed from the output

                        default:
                            replaceArg = "";
                            break;
                    }
                    argIndex++;
                }

                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replaceArg));
            }
            matcher.appendTail(buffer);
        }

        for (int i = argIndex; i < args.length; i++) {
            if (buffer.length() > 0) {
                buffer.append(' ');
            }

            final Object val = args[i];
            if (val instanceof String) {
                buffer.append(formatString(val));
            } else {
                buffer.append(formatObj(cx, scope, val));
            }
        }

        return buffer.toString();
    }

    private static String formatString(Object val) {
        if (val instanceof BigInteger) {
            return ScriptRuntime.toString(val) + "n";
        }

        if (ScriptRuntime.isSymbol(val)) {
            return val.toString();
        }

        return ScriptRuntime.toString(val);
    }

    private static String formatInt(Object val) {
        if (val instanceof BigInteger) {
            return ScriptRuntime.bigIntToString((BigInteger) val, 10) + "n";
        }

        if (ScriptRuntime.isSymbol(val)) {
            return ScriptRuntime.NaNobj.toString();
        }

        double number = ScriptRuntime.toNumber(val);

        if (Double.isInfinite(number) || Double.isNaN(number)) {
            return ScriptRuntime.toString(number);
        }

        return String.valueOf((long) number);
    }

    private static String formatFloat(Object val) {
        if (val instanceof BigInteger || ScriptRuntime.isSymbol(val)) {
            return ScriptRuntime.NaNobj.toString();
        }

        return ScriptRuntime.numberToString(ScriptRuntime.toNumber(val), 10);
    }

    private static String formatObj(Context cx, Scriptable scope, Object arg) {
        if (arg == null) {
            return "null";
        }

        if (Undefined.isUndefined(arg)) {
            return Undefined.SCRIPTABLE_UNDEFINED.toString();
        }

        if (arg instanceof NativeError) {
            NativeError err = (NativeError) arg;
            String msg = err.toString();
            msg += "\n";
            msg += err.get("stack");
            return msg;
        }

        try {
            // NativeJSON.stringify outputs Callable's as null, convert to string
            // to make the output less confusing
            final Callable replacer =
                    new Callable() {
                        @Override
                        public Object call(
                                Context callCx,
                                Scriptable callScope,
                                Scriptable callThisObj,
                                Object[] callArgs) {
                            Object value = callArgs[1];
                            while (value instanceof Delegator) {
                                value = ((Delegator) value).getDelegee();
                            }
                            if (value instanceof BaseFunction) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("function ")
                                        .append(((BaseFunction) value).getFunctionName())
                                        .append("() {...}");
                                return sb.toString();
                            }
                            if (value instanceof Callable) {
                                return ScriptRuntime.toString(value);
                            }
                            if (arg instanceof NativeError) {
                                return ((NativeError) arg).toString();
                            }
                            return value;
                        }
                    };
            Object stringify = NativeJSON.stringify(cx, scope, arg, replacer, null);
            return ScriptRuntime.toString(stringify);
        } catch (EcmaError e) {
            if ("TypeError".equals(e.getName())) {
                // Fall back to use ScriptRuntime.toString() in some case such as
                // NativeJSON.stringify not support BigInt yet.
                return ScriptRuntime.toString(arg);
            }
            throw e;
        }
    }

    private void jsAssert(Context cx, Scriptable scope, Object[] args) {
        if (args != null && args.length > 0 && ScriptRuntime.toBoolean(args[0])) {
            return;
        }

        if (args == null || args.length < 2) {
            printer.print(
                    cx,
                    scope,
                    Level.ERROR,
                    new String[] {"Assertion failed: console.assert"},
                    null);
            return;
        }

        Object first = args[1];
        if (first instanceof String) {
            args[1] = "Assertion failed: " + first;
            Object[] newArgs = new Object[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, newArgs.length);
            args = newArgs;
        } else {
            args[0] = "Assertion failed:";
        }

        printer.print(cx, scope, Level.ERROR, args, null);
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

    @Override
    protected int findPrototypeId(String s) {
        int id;
        switch (s) {
            case "log":
                id = Id_log;
                break;
            case "info":
                id = Id_info;
                break;
            case "time":
                id = Id_time;
                break;
            case "warn":
                id = Id_warn;
                break;
            case "count":
                id = Id_count;
                break;
            case "debug":
                id = Id_debug;
                break;
            case "error":
                id = Id_error;
                break;
            case "trace":
                id = Id_trace;
                break;
            case "assert":
                id = Id_assert;
                break;
            case "timeEnd":
                id = Id_timeEnd;
                break;
            case "timeLog":
                id = Id_timeLog;
                break;
            case "toSource":
                id = Id_toSource;
                break;
            case "countReset":
                id = Id_countReset;
                break;
            default:
                id = 0;
                break;
        }
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
}
