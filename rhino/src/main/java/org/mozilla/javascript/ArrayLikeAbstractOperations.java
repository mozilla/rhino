package org.mozilla.javascript;

import static org.mozilla.javascript.NativeArray.getLengthProperty;
import static org.mozilla.javascript.ScriptRuntimeES6.requireObjectCoercible;
import static org.mozilla.javascript.Scriptable.NOT_FOUND;

import java.io.Serializable;
import java.util.Comparator;

/** Contains implementation of shared methods useful for arrays and typed arrays. */
public class ArrayLikeAbstractOperations {
    public enum IterativeOperation {
        EVERY,
        FILTER,
        FOR_EACH,
        MAP,
        SOME,
        FIND,
        FIND_INDEX,
        FIND_LAST,
        FIND_LAST_INDEX,
    }

    public enum ReduceOperation {
        REDUCE,
        REDUCE_RIGHT,
    }

    /**
     * Implements the methods "every", "filter", "forEach", "map", and "some" without using an
     * IdFunctionObject.
     */
    public static Object iterativeMethod(
            Context cx,
            IterativeOperation operation,
            Scriptable scope,
            Scriptable thisObj,
            Object[] args) {
        return iterativeMethod(cx, null, operation, scope, thisObj, args, true);
    }

    /**
     * Implements the methods "every", "filter", "forEach", "map", and "some" using an
     * IdFunctionObject.
     */
    public static Object iterativeMethod(
            Context cx,
            IdFunctionObject fun,
            IterativeOperation operation,
            Scriptable scope,
            Scriptable thisObj,
            Object[] args) {
        return iterativeMethod(cx, fun, operation, scope, thisObj, args, false);
    }

    private static Object iterativeMethod(
            Context cx,
            IdFunctionObject fun,
            IterativeOperation operation,
            Scriptable scope,
            Scriptable thisObj,
            Object[] args,
            boolean skipCoercibleCheck) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

        if (!skipCoercibleCheck) {
            if (IterativeOperation.FIND == operation
                    || IterativeOperation.FIND_INDEX == operation
                    || IterativeOperation.FIND_LAST == operation
                    || IterativeOperation.FIND_LAST_INDEX == operation) {
                requireObjectCoercible(cx, o, fun);
            }
        }

        return coercibleIterativeMethod(cx, operation, scope, args, o);
    }

    public static Object iterativeMethod(
            Context cx,
            Object tag,
            String name,
            IterativeOperation operation,
            Scriptable scope,
            Scriptable thisObj,
            Object[] args) {
        return iterativeMethod(cx, tag, name, operation, scope, thisObj, args, false);
    }

    private static Object iterativeMethod(
            Context cx,
            Object tag,
            String name,
            IterativeOperation operation,
            Scriptable scope,
            Scriptable thisObj,
            Object[] args,
            boolean skipCoercibleCheck) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

        if (!skipCoercibleCheck) {
            if (IterativeOperation.FIND == operation
                    || IterativeOperation.FIND_INDEX == operation
                    || IterativeOperation.FIND_LAST == operation
                    || IterativeOperation.FIND_LAST_INDEX == operation) {
                requireObjectCoercible(cx, o, tag, name);
            }
        }

        return coercibleIterativeMethod(cx, operation, scope, args, o);
    }

    private static Object coercibleIterativeMethod(
            Context cx,
            IterativeOperation operation,
            Scriptable scope,
            Object[] args,
            Scriptable o) {
        long length = getLengthProperty(cx, o);
        if (operation == IterativeOperation.MAP && length > Integer.MAX_VALUE) {
            String msg = ScriptRuntime.getMessageById("msg.arraylength.bad");
            throw ScriptRuntime.rangeError(msg);
        }

        Object callbackArg = args.length > 0 ? args[0] : Undefined.instance;

        Function f = getCallbackArg(cx, callbackArg);
        Scriptable parent = ScriptableObject.getTopLevelScope(f);
        Scriptable thisArg;
        if (args.length < 2 || args[1] == null || args[1] == Undefined.instance) {
            thisArg = parent;
        } else {
            thisArg = ScriptRuntime.toObject(cx, scope, args[1]);
        }

        Scriptable array = null;
        if (operation == IterativeOperation.FILTER || operation == IterativeOperation.MAP) {
            int resultLength = operation == IterativeOperation.MAP ? (int) length : 0;
            array = arraySpeciesCreate(cx, scope, o, resultLength);
        }
        long j = 0;
        long start =
                (operation == IterativeOperation.FIND_LAST
                                || operation == IterativeOperation.FIND_LAST_INDEX)
                        ? length - 1
                        : 0;
        long end =
                (operation == IterativeOperation.FIND_LAST
                                || operation == IterativeOperation.FIND_LAST_INDEX)
                        ? -1
                        : length;
        long increment =
                (operation == IterativeOperation.FIND_LAST
                                || operation == IterativeOperation.FIND_LAST_INDEX)
                        ? -1
                        : +1;
        for (long i = start; i != end; i += increment) {
            Object[] innerArgs = new Object[3];
            Object elem = getRawElem(o, i);
            if (elem == NOT_FOUND) {
                if (operation == IterativeOperation.FIND
                        || operation == IterativeOperation.FIND_INDEX
                        || operation == IterativeOperation.FIND_LAST
                        || operation == IterativeOperation.FIND_LAST_INDEX) {
                    elem = Undefined.instance;
                } else {
                    continue;
                }
            }
            innerArgs[0] = elem;
            innerArgs[1] = Long.valueOf(i);
            innerArgs[2] = o;
            Object result = f.call(cx, parent, thisArg, innerArgs);
            switch (operation) {
                case EVERY:
                    if (!ScriptRuntime.toBoolean(result)) return Boolean.FALSE;
                    break;
                case FILTER:
                    if (ScriptRuntime.toBoolean(result)) defineElem(cx, array, j++, innerArgs[0]);
                    break;
                case FOR_EACH:
                    break;
                case MAP:
                    defineElem(cx, array, i, result);
                    break;
                case SOME:
                    if (ScriptRuntime.toBoolean(result)) return Boolean.TRUE;
                    break;
                case FIND:
                case FIND_LAST:
                    if (ScriptRuntime.toBoolean(result)) return elem;
                    break;
                case FIND_INDEX:
                case FIND_LAST_INDEX:
                    if (ScriptRuntime.toBoolean(result))
                        return ScriptRuntime.wrapNumber((double) i);
                    break;
            }
        }
        switch (operation) {
            case EVERY:
                return Boolean.TRUE;
            case FILTER:
            case MAP:
                return array;
            case SOME:
                return Boolean.FALSE;
            case FIND_INDEX:
            case FIND_LAST_INDEX:
                return ScriptRuntime.wrapNumber(-1);
            case FOR_EACH:
            default:
                return Undefined.instance;
        }
    }

    static Scriptable arraySpeciesCreate(Context cx, Scriptable scope, Scriptable o, int length) {
        if (o instanceof NativeArray) {
            Object c = ScriptableObject.getProperty(o, "constructor");
            if (c instanceof Scriptable) {
                c = ScriptableObject.getProperty((Scriptable) c, SymbolKey.SPECIES);
                if (c == null || c == NOT_FOUND) {
                    c = Undefined.instance;
                }
            }

            if (!Undefined.isUndefined(c)) {
                if (c instanceof Constructable) {
                    return ((Constructable) c)
                            .construct(cx, scope, new Object[] {Double.valueOf(length)});
                } else {
                    throw ScriptRuntime.typeErrorById("msg.ctor.not.found", o);
                }
            }
        }
        return cx.newArray(scope, length);
    }

    static Function getCallbackArg(Context cx, Object callbackArg) {
        if (!(callbackArg instanceof Function)) {
            throw ScriptRuntime.notFunctionError(callbackArg);
        }

        Function f = (Function) callbackArg;

        if (cx.getLanguageVersion() >= Context.VERSION_ES6) {
            RegExpProxy reProxy = ScriptRuntime.getRegExpProxy(cx);
            if (reProxy != null && reProxy.isRegExp(f))
                // Previously, it was allowed to pass RegExp instance as a callback (it implements
                // Function)
                // But according to ES2015 21.2.6 Properties of RegExp Instances:
                // > RegExp instances are ordinary objects that inherit properties from the RegExp
                // prototype object.
                // > RegExp instances have internal slots [[RegExpMatcher]], [[OriginalSource]], and
                // [[OriginalFlags]].
                // so, no [[Call]] for RegExp-s
                throw ScriptRuntime.notFunctionError(callbackArg);
        }

        return f;
    }

    static void defineElem(Context cx, Scriptable target, long index, Object value) {
        if (!(target instanceof NativeArray && ((NativeArray) target).getDenseOnly())
                && target instanceof ScriptableObject) {
            var so = (ScriptableObject) target;
            ScriptableObject desc = new NativeObject();
            desc.defineProperty("value", value, 0);
            desc.defineProperty("writable", Boolean.TRUE, 0);
            desc.defineProperty("enumerable", Boolean.TRUE, 0);
            desc.defineProperty("configurable", Boolean.TRUE, 0);
            so.defineOwnProperty(cx, index, desc);
            return;
        }
        if (index > Integer.MAX_VALUE) {
            String id = Long.toString(index);
            target.put(id, target, value);
        } else {
            target.put((int) index, target, value);
        }
    }

    // same as NativeArray::getElem, but without converting NOT_FOUND to undefined
    static Object getRawElem(Scriptable target, long index) {
        if (index > Integer.MAX_VALUE) {
            return ScriptableObject.getProperty(target, Long.toString(index));
        }
        return ScriptableObject.getProperty(target, (int) index);
    }

    public static long toSliceIndex(double value, long length) {
        long result;
        if (value < 0.0) {
            if (value + length < 0.0) {
                result = 0;
            } else {
                result = (long) (value + length);
            }
        } else if (value > length) {
            result = length;
        } else {
            result = (long) value;
        }
        return result;
    }

    /** Implements the methods "reduce" and "reduceRight". */
    public static Object reduceMethod(
            Context cx,
            ReduceOperation operation,
            Scriptable scope,
            Scriptable thisObj,
            Object[] args) {
        Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

        long length = getLengthProperty(cx, o);
        Object callbackArg = args.length > 0 ? args[0] : Undefined.instance;
        if (callbackArg == null || !(callbackArg instanceof Function)) {
            throw ScriptRuntime.notFunctionError(callbackArg);
        }
        Function f = (Function) callbackArg;
        Scriptable parent = ScriptableObject.getTopLevelScope(f);
        // hack to serve both reduce and reduceRight with the same loop
        boolean movingLeft = operation == ReduceOperation.REDUCE;
        Object value = args.length > 1 ? args[1] : NOT_FOUND;
        for (long i = 0; i < length; i++) {
            long index = movingLeft ? i : (length - 1 - i);
            Object elem = getRawElem(o, index);
            if (elem == NOT_FOUND) {
                continue;
            }
            if (value == NOT_FOUND) {
                // no initial value passed, use first element found as initial value
                value = elem;
            } else {
                Object[] innerArgs = {value, elem, index, o};
                value = f.call(cx, parent, parent, innerArgs);
            }
        }
        if (value == NOT_FOUND) {
            // reproduce spidermonkey error message
            throw ScriptRuntime.typeErrorById("msg.empty.array.reduce");
        }
        return value;
    }

    public static Comparator<Object> getSortComparator(
            final Context cx, final Scriptable scope, final Object[] args) {
        if (args.length > 0 && Undefined.instance != args[0]) {
            return getSortComparatorFromArguments(cx, scope, args);
        } else {
            return DEFAULT_COMPARATOR;
        }
    }

    public static ElementComparator getSortComparatorFromArguments(
            Context cx, Scriptable scope, Object[] args) {
        final Callable jsCompareFunction = ScriptRuntime.getValueFunctionAndThis(args[0], cx);
        final Scriptable funThis = ScriptRuntime.lastStoredScriptable(cx);
        final Object[] cmpBuf = new Object[2]; // Buffer for cmp arguments
        return new ElementComparator(
                new Comparator<Object>() {
                    @Override
                    public int compare(final Object x, final Object y) {
                        // This comparator is invoked only for non-undefined objects
                        cmpBuf[0] = x;
                        cmpBuf[1] = y;
                        Object ret = jsCompareFunction.call(cx, scope, funThis, cmpBuf);
                        double d = ScriptRuntime.toNumber(ret);
                        int cmp = Double.compare(d, 0);
                        if (cmp < 0) {
                            return -1;
                        } else if (cmp > 0) {
                            return +1;
                        }
                        return 0;
                    }
                });
    }

    // Comparators for the js_sort method. Putting them here lets us unit-test them better.

    private static final Comparator<Object> STRING_COMPARATOR = new StringLikeComparator();
    private static final Comparator<Object> DEFAULT_COMPARATOR = new ElementComparator();

    public static final class StringLikeComparator implements Comparator<Object>, Serializable {

        private static final long serialVersionUID = 5299017659728190979L;

        @Override
        public int compare(final Object x, final Object y) {
            final String a = ScriptRuntime.toString(x);
            final String b = ScriptRuntime.toString(y);
            return a.compareTo(b);
        }
    }

    public static final class ElementComparator implements Comparator<Object>, Serializable {

        private static final long serialVersionUID = -1189948017688708858L;

        private final Comparator<Object> child;

        public ElementComparator() {
            child = STRING_COMPARATOR;
        }

        public ElementComparator(Comparator<Object> c) {
            child = c;
        }

        @Override
        public int compare(final Object x, final Object y) {
            // Sort NOT_FOUND to very end, Undefined before that, exclusively, as per
            // ECMA 22.1.3.25.1.
            if (x == Undefined.instance) {
                if (y == Undefined.instance) {
                    return 0;
                }
                if (y == NOT_FOUND) {
                    return -1;
                }
                return 1;
            } else if (x == NOT_FOUND) {
                return y == NOT_FOUND ? 0 : 1;
            }

            if (y == NOT_FOUND) {
                return -1;
            }
            if (y == Undefined.instance) {
                return -1;
            }

            return child.compare(x, y);
        }
    }
}
