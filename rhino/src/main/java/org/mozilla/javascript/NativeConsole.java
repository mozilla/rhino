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

public class NativeConsole extends ScriptableObject {
    private static final long serialVersionUID = 5694613212458273057L;

    private static final String CLASS_NAME = "Console";

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
        obj.setPrototype(getObjectPrototype(scope));
        obj.setParentScope(scope);

        obj.defineProperty(
                scope, "toSource", 0, NativeConsole::js_toSource, 0, DONTENUM | READONLY);
        obj.defineBuiltinProperty(
                scope,
                "trace",
                1,
                (cx, callScope, ignoredThis, args) -> obj.js_trace(cx, callScope, args),
                0,
                DONTENUM | READONLY);
        obj.defineBuiltinProperty(
                scope,
                "debug",
                1,
                (cx, callScope, ignoredThis, args) -> obj.js_debug(cx, callScope, args),
                0,
                DONTENUM | READONLY);
        obj.defineBuiltinProperty(
                scope,
                "log",
                1,
                (cx, callScope, ignoredThis, args) -> obj.js_log(cx, callScope, args),
                0,
                DONTENUM | READONLY);
        obj.defineBuiltinProperty(
                scope,
                "info",
                1,
                (cx, callScope, ignoredThis, args) -> obj.js_info(cx, callScope, args),
                0,
                DONTENUM | READONLY);
        obj.defineBuiltinProperty(
                scope,
                "warn",
                1,
                (cx, callScope, ignoredThis, args) -> obj.js_warn(cx, callScope, args),
                0,
                DONTENUM | READONLY);
        obj.defineBuiltinProperty(
                scope,
                "error",
                1,
                (cx, callScope, ignoredThis, args) -> obj.js_error(cx, callScope, args),
                0,
                DONTENUM | READONLY);
        obj.defineBuiltinProperty(
                scope,
                "assert",
                2,
                (cx, callScope, ignoredThis, args) -> obj.js_assert(cx, callScope, args),
                0,
                DONTENUM | READONLY);
        obj.defineBuiltinProperty(
                scope,
                "count",
                1,
                (cx, callScope, ignoredThis, args) -> obj.js_count(cx, callScope, args),
                0,
                DONTENUM | READONLY);
        obj.defineBuiltinProperty(
                scope,
                "countReset",
                1,
                (cx, callScope, ignoredThis, args) -> obj.js_countReset(cx, callScope, args),
                0,
                DONTENUM | READONLY);
        obj.defineBuiltinProperty(
                scope,
                "time",
                1,
                (cx, callScope, ignoredThis, args) -> obj.js_time(cx, callScope, args),
                0,
                DONTENUM | READONLY);
        obj.defineBuiltinProperty(
                scope,
                "timeEnd",
                1,
                (cx, callScope, ignoredThis, args) -> obj.js_timeEnd(cx, callScope, args),
                0,
                DONTENUM | READONLY);
        obj.defineBuiltinProperty(
                scope,
                "timeLog",
                2,
                (cx, callScope, ignoredThis, args) -> obj.js_timeLog(cx, callScope, args),
                0,
                DONTENUM | READONLY);
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
        return CLASS_NAME;
    }

    private static Object js_toSource(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return CLASS_NAME;
    }

    private Object js_trace(Context cx, Scriptable scope, Object[] args) {
        ScriptStackElement[] stack = new EvaluatorException("[object Object]").getScriptStack();
        printer.print(cx, scope, Level.TRACE, args, stack);
        return Undefined.instance;
    }

    private Object js_debug(Context cx, Scriptable scope, Object[] args) {
        printer.print(cx, scope, Level.DEBUG, args, null);
        return Undefined.instance;
    }

    private Object js_log(Context cx, Scriptable scope, Object[] args) {
        printer.print(cx, scope, Level.INFO, args, null);
        return Undefined.instance;
    }

    private Object js_info(Context cx, Scriptable scope, Object[] args) {
        printer.print(cx, scope, Level.INFO, args, null);
        return Undefined.instance;
    }

    private Object js_warn(Context cx, Scriptable scope, Object[] args) {
        printer.print(cx, scope, Level.WARN, args, null);
        return Undefined.instance;
    }

    private Object js_error(Context cx, Scriptable scope, Object[] args) {
        printer.print(cx, scope, Level.ERROR, args, null);
        return Undefined.instance;
    }

    private Object js_assert(Context cx, Scriptable scope, Object[] args) {
        jsAssert(cx, scope, args);
        return Undefined.instance;
    }

    private Object js_count(Context cx, Scriptable scope, Object[] args) {
        count(cx, scope, args);
        return Undefined.instance;
    }

    private Object js_countReset(Context cx, Scriptable scope, Object[] args) {
        countReset(cx, scope, args);
        return Undefined.instance;
    }

    private Object js_time(Context cx, Scriptable scope, Object[] args) {
        time(cx, scope, args);
        return Undefined.instance;
    }

    private Object js_timeEnd(Context cx, Scriptable scope, Object[] args) {
        timeEnd(cx, scope, args);
        return Undefined.instance;
    }

    private Object js_timeLog(Context cx, Scriptable scope, Object[] args) {
        timeLog(cx, scope, args);
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

                if ("%%".equals(placeHolder)) {
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
}
