/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.typedarrays.NativeArrayBuffer;
import org.mozilla.javascript.typedarrays.NativeDataView;
import org.mozilla.javascript.typedarrays.NativeFloat32Array;
import org.mozilla.javascript.typedarrays.NativeFloat64Array;
import org.mozilla.javascript.typedarrays.NativeInt16Array;
import org.mozilla.javascript.typedarrays.NativeInt32Array;
import org.mozilla.javascript.typedarrays.NativeInt8Array;
import org.mozilla.javascript.typedarrays.NativeUint16Array;
import org.mozilla.javascript.typedarrays.NativeUint32Array;
import org.mozilla.javascript.typedarrays.NativeUint8Array;
import org.mozilla.javascript.typedarrays.NativeUint8ClampedArray;
import org.mozilla.javascript.v8dtoa.DoubleConversion;
import org.mozilla.javascript.v8dtoa.FastDtoa;
import org.mozilla.javascript.xml.XMLLib;
import org.mozilla.javascript.xml.XMLLoader;
import org.mozilla.javascript.xml.XMLObject;

/**
 * This is the class that implements the runtime.
 *
 * @author Norris Boyd
 */
public class ScriptRuntime {

    /** No instances should be created. */
    protected ScriptRuntime() {}

    /**
     * Returns representation of the [[ThrowTypeError]] object. See ECMA 5 spec, 13.2.3
     *
     * @deprecated {@link #typeErrorThrower(Context)}
     */
    @Deprecated
    public static BaseFunction typeErrorThrower() {
        return typeErrorThrower(Context.getCurrentContext());
    }

    /** Returns representation of the [[ThrowTypeError]] object. See ECMA 5 spec, 13.2.3 */
    public static BaseFunction typeErrorThrower(Context cx) {
        if (cx.typeErrorThrower == null) {
            BaseFunction thrower =
                    new BaseFunction() {
                        private static final long serialVersionUID = -5891740962154902286L;

                        @Override
                        public Object call(
                                Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                            throw typeErrorById("msg.op.not.allowed");
                        }

                        @Override
                        public int getLength() {
                            return 0;
                        }
                    };
            ScriptRuntime.setFunctionProtoAndParent(thrower, cx, cx.topCallScope, false);
            thrower.preventExtensions();
            cx.typeErrorThrower = thrower;
        }
        return cx.typeErrorThrower;
    }

    static class NoSuchMethodShim implements Callable {
        String methodName;
        Callable noSuchMethodMethod;

        NoSuchMethodShim(Callable noSuchMethodMethod, String methodName) {
            this.noSuchMethodMethod = noSuchMethodMethod;
            this.methodName = methodName;
        }

        /**
         * Perform the call.
         *
         * @param cx the current Context for this thread
         * @param scope the scope to use to resolve properties.
         * @param thisObj the JavaScript <code>this</code> object
         * @param args the array of arguments
         * @return the result of the call
         */
        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            Object[] nestedArgs = new Object[2];

            nestedArgs[0] = methodName;
            nestedArgs[1] = newArrayLiteral(args, null, cx, scope);
            return noSuchMethodMethod.call(cx, scope, thisObj, nestedArgs);
        }
    }

    /*
     * There's such a huge space (and some time) waste for the Foo.class
     * syntax: the compiler sticks in a test of a static field in the
     * enclosing class for null and the code for creating the class value.
     * It has to do this since the reference has to get pushed off until
     * execution time (i.e. can't force an early load), but for the
     * 'standard' classes - especially those in java.lang, we can trust
     * that they won't cause problems by being loaded early.
     */

    public static final Class<?> BooleanClass = Kit.classOrNull("java.lang.Boolean"),
            ByteClass = Kit.classOrNull("java.lang.Byte"),
            CharacterClass = Kit.classOrNull("java.lang.Character"),
            ClassClass = Kit.classOrNull("java.lang.Class"),
            DoubleClass = Kit.classOrNull("java.lang.Double"),
            FloatClass = Kit.classOrNull("java.lang.Float"),
            IntegerClass = Kit.classOrNull("java.lang.Integer"),
            LongClass = Kit.classOrNull("java.lang.Long"),
            NumberClass = Kit.classOrNull("java.lang.Number"),
            ObjectClass = Kit.classOrNull("java.lang.Object"),
            ShortClass = Kit.classOrNull("java.lang.Short"),
            StringClass = Kit.classOrNull("java.lang.String"),
            DateClass = Kit.classOrNull("java.util.Date"),
            BigIntegerClass = Kit.classOrNull("java.math.BigInteger");
    public static final Class<?> ContextClass = Kit.classOrNull("org.mozilla.javascript.Context"),
            ContextFactoryClass = Kit.classOrNull("org.mozilla.javascript.ContextFactory"),
            FunctionClass = Kit.classOrNull("org.mozilla.javascript.Function"),
            ScriptableObjectClass = Kit.classOrNull("org.mozilla.javascript.ScriptableObject");
    public static final Class<Scriptable> ScriptableClass = Scriptable.class;

    private static final Object LIBRARY_SCOPE_KEY = "LIBRARY_SCOPE";

    public static boolean isRhinoRuntimeType(Class<?> cl) {
        if (cl.isPrimitive()) {
            return (cl != Character.TYPE);
        }
        return (cl == StringClass
                || cl == BooleanClass
                || NumberClass.isAssignableFrom(cl)
                || ScriptableClass.isAssignableFrom(cl));
    }

    public static ScriptableObject initSafeStandardObjects(
            Context cx, ScriptableObject scope, boolean sealed) {
        if (scope == null) {
            scope = new NativeObject();
        } else if (scope instanceof TopLevel) {
            ((TopLevel) scope).clearCache();
        }

        scope.associateValue(LIBRARY_SCOPE_KEY, scope);
        new ClassCache().associate(scope);

        BaseFunction.init(cx, scope, sealed);
        NativeObject.init(scope, sealed);

        Scriptable objectProto = ScriptableObject.getObjectPrototype(scope);

        // Function.prototype.__proto__ should be Object.prototype
        Scriptable functionProto = ScriptableObject.getClassPrototype(scope, "Function");
        functionProto.setPrototype(objectProto);

        // Set the prototype of the object passed in if need be
        if (scope.getPrototype() == null) scope.setPrototype(objectProto);

        // must precede NativeGlobal since it's needed therein
        NativeError.init(scope, sealed);
        NativeGlobal.init(cx, scope, sealed);

        NativeArray.init(cx, scope, sealed);
        if (!cx.isInterpretedMode()) {
            // When optimizing, attempt to fulfill all requests for new Array(N)
            // with a higher threshold before switching to a sparse
            // representation
            NativeArray.setMaximumInitialCapacity(200000);
        }
        NativeString.init(scope, sealed);
        NativeBoolean.init(scope, sealed);
        NativeNumber.init(scope, sealed);
        NativeDate.init(scope, sealed);
        new LazilyLoadedCtor(scope, "Math", sealed, true, NativeMath::init);
        new LazilyLoadedCtor(scope, "JSON", sealed, true, NativeJSON::init);

        NativeWith.init(scope, sealed);
        NativeCall.init(scope, sealed);
        NativeScript.init(cx, scope, sealed);

        NativeIterator.init(cx, scope, sealed); // Also initializes NativeGenerator & ES6Generator

        NativeArrayIterator.init(scope, sealed);
        NativeStringIterator.init(scope, sealed);
        registerRegExp(cx, scope, sealed);

        NativeJavaObject.init(scope, sealed);
        NativeJavaMap.init(scope, sealed);

        // define lazy-loaded properties using their class name
        // Depends on the old reflection-based lazy loading mechanism
        // to property initialize the prototype.
        new LazilyLoadedCtor(
                scope, "Continuation", "org.mozilla.javascript.NativeContinuation", sealed, true);

        if (cx.hasFeature(Context.FEATURE_E4X)) {
            if (xmlLoaderImpl != null) {
                xmlLoaderImpl.load(scope, sealed);
            }
        }

        if (((cx.getLanguageVersion() >= Context.VERSION_1_8)
                        && cx.hasFeature(Context.FEATURE_V8_EXTENSIONS))
                || (cx.getLanguageVersion() >= Context.VERSION_ES6)) {
            new LazilyLoadedCtor(scope, "ArrayBuffer", sealed, true, NativeArrayBuffer::init);
            new LazilyLoadedCtor(scope, "Int8Array", sealed, true, NativeInt8Array::init);
            new LazilyLoadedCtor(scope, "Uint8Array", sealed, true, NativeUint8Array::init);
            new LazilyLoadedCtor(
                    scope, "Uint8ClampedArray", sealed, true, NativeUint8ClampedArray::init);
            new LazilyLoadedCtor(scope, "Int16Array", sealed, true, NativeInt16Array::init);
            new LazilyLoadedCtor(scope, "Uint16Array", sealed, true, NativeUint16Array::init);
            new LazilyLoadedCtor(scope, "Int32Array", sealed, true, NativeInt32Array::init);
            new LazilyLoadedCtor(scope, "Uint32Array", sealed, true, NativeUint32Array::init);
            new LazilyLoadedCtor(scope, "Float32Array", sealed, true, NativeFloat32Array::init);
            new LazilyLoadedCtor(scope, "Float64Array", sealed, true, NativeFloat64Array::init);
            new LazilyLoadedCtor(scope, "DataView", sealed, true, NativeDataView::init);
        }

        if (cx.getLanguageVersion() >= Context.VERSION_ES6) {
            NativeSymbol.init(cx, scope, sealed);
            NativeCollectionIterator.init(scope, NativeSet.ITERATOR_TAG, sealed);
            NativeCollectionIterator.init(scope, NativeMap.ITERATOR_TAG, sealed);
            new LazilyLoadedCtor(scope, "Map", sealed, true, NativeMap::init);
            new LazilyLoadedCtor(scope, "Promise", sealed, true, NativePromise::init);
            new LazilyLoadedCtor(scope, "Set", sealed, true, NativeSet::init);
            new LazilyLoadedCtor(scope, "WeakMap", sealed, true, NativeWeakMap::init);
            new LazilyLoadedCtor(scope, "WeakSet", sealed, true, NativeWeakSet::init);
            new LazilyLoadedCtor(scope, "BigInt", sealed, true, NativeBigInt::init);
            new LazilyLoadedCtor(scope, "Proxy", sealed, true, NativeProxy::init);
            new LazilyLoadedCtor(scope, "Reflect", sealed, true, NativeReflect::init);
        }

        if (scope instanceof TopLevel) {
            ((TopLevel) scope).cacheBuiltins(scope, sealed);
        }

        return scope;
    }

    private static void registerRegExp(Context cx, ScriptableObject scope, boolean sealed) {
        RegExpProxy regExpProxy = getRegExpProxy(cx);
        if (regExpProxy != null) {
            regExpProxy.register(scope, sealed);
        }
    }

    public static ScriptableObject initStandardObjects(
            Context cx, ScriptableObject scope, boolean sealed) {
        ScriptableObject s = initSafeStandardObjects(cx, scope, sealed);

        // These depend on the legacy initialization behavior of the lazy loading mechanism
        new LazilyLoadedCtor(
                s, "Packages", "org.mozilla.javascript.NativeJavaTopPackage", sealed, true);
        new LazilyLoadedCtor(
                s, "getClass", "org.mozilla.javascript.NativeJavaTopPackage", sealed, true);
        new LazilyLoadedCtor(s, "JavaAdapter", "org.mozilla.javascript.JavaAdapter", sealed, true);
        new LazilyLoadedCtor(
                s, "JavaImporter", "org.mozilla.javascript.ImporterTopLevel", sealed, true);

        for (String packageName : getTopPackageNames()) {
            new LazilyLoadedCtor(
                    s, packageName, "org.mozilla.javascript.NativeJavaTopPackage", sealed, true);
        }

        return s;
    }

    static String[] getTopPackageNames() {
        // Include "android" top package if running on Android
        return "Dalvik".equals(System.getProperty("java.vm.name"))
                ? new String[] {"java", "javax", "org", "com", "edu", "net", "android"}
                : new String[] {"java", "javax", "org", "com", "edu", "net"};
    }

    public static ScriptableObject getLibraryScopeOrNull(Scriptable scope) {
        ScriptableObject libScope;
        libScope = (ScriptableObject) ScriptableObject.getTopScopeValue(scope, LIBRARY_SCOPE_KEY);
        return libScope;
    }

    // It is public so NativeRegExp can access it.
    public static boolean isJSLineTerminator(int c) {
        // Optimization for faster check for eol character:
        // they do not have 0xDFD0 bits set
        if ((c & 0xDFD0) != 0) {
            return false;
        }
        return c == '\n' || c == '\r' || c == 0x2028 || c == 0x2029;
    }

    public static boolean isJSWhitespaceOrLineTerminator(int c) {
        return (isStrWhiteSpaceChar(c) || isJSLineTerminator(c));
    }

    /**
     * Indicates if the character is a Str whitespace char according to ECMA spec: StrWhiteSpaceChar
     * ::: <TAB> <SP> <NBSP> <FF> <VT> <CR> <LF> <LS> <PS> <USP> <BOM>
     */
    static boolean isStrWhiteSpaceChar(int c) {
        switch (c) {
            case ' ': // <SP>
            case '\n': // <LF>
            case '\r': // <CR>
            case '\t': // <TAB>
            case '\u00A0': // <NBSP>
            case '\u000C': // <FF>
            case '\u000B': // <VT>
            case '\u2028': // <LS>
            case '\u2029': // <PS>
            case '\uFEFF': // <BOM>
                return true;
            default:
                return Character.getType(c) == Character.SPACE_SEPARATOR;
        }
    }

    public static Boolean wrapBoolean(boolean b) {
        return Boolean.valueOf(b);
    }

    public static Integer wrapInt(int i) {
        return Integer.valueOf(i);
    }

    public static Number wrapNumber(double x) {
        if (Double.isNaN(x)) {
            return ScriptRuntime.NaNobj;
        }
        return Double.valueOf(x);
    }

    /**
     * Convert the value to a boolean.
     *
     * <p>See ECMA 9.2.
     */
    public static boolean toBoolean(Object val) {
        for (; ; ) {
            if (val instanceof Boolean) return ((Boolean) val).booleanValue();
            if (val == null || Undefined.isUndefined(val)) return false;
            if (val instanceof CharSequence) return ((CharSequence) val).length() != 0;
            if (val instanceof BigInteger) {
                return !BigInteger.ZERO.equals(val);
            }
            if (val instanceof Number) {
                double d = ((Number) val).doubleValue();
                return (!Double.isNaN(d) && d != 0.0);
            }
            if (val instanceof Scriptable) {
                if (val instanceof ScriptableObject
                        && ((ScriptableObject) val).avoidObjectDetection()) {
                    return false;
                }
                if (Context.getContext().isVersionECMA1()) {
                    // pure ECMA
                    return true;
                }
                // ECMA extension
                val = ((Scriptable) val).getDefaultValue(BooleanClass);
                if ((val instanceof Scriptable) && !isSymbol(val))
                    throw errorWithClassName("msg.primitive.expected", val);
                continue;
            }
            warnAboutNonJSObject(val);
            return true;
        }
    }

    /**
     * Convert the value to a number.
     *
     * <p>See ECMA 9.3.
     */
    public static double toNumber(Object val) {
        for (; ; ) {
            if (val instanceof BigInteger) {
                throw typeErrorById("msg.cant.convert.to.number", "BigInt");
            }
            if (val instanceof Number) return ((Number) val).doubleValue();
            if (val == null) return +0.0;
            if (Undefined.isUndefined(val)) return NaN;
            if (val instanceof String) return toNumber((String) val);
            if (val instanceof CharSequence) return toNumber(val.toString());
            if (val instanceof Boolean) return ((Boolean) val).booleanValue() ? 1 : +0.0;
            if (isSymbol(val)) throw typeErrorById("msg.not.a.number");

            if (val instanceof Scriptable) {
                // Assert: val is an Object
                val = toPrimitive(val, NumberClass);
                // Assert: val is a primitive
            } else {
                warnAboutNonJSObject(val);
                return Double.NaN;
            }
        }
    }

    public static double toNumber(Object[] args, int index) {
        return (index < args.length) ? toNumber(args[index]) : NaN;
    }

    public static final double NaN = Double.NaN;
    public static final Double NaNobj = Double.valueOf(NaN);

    // Preserve backward-compatibility with historical value of this.
    public static final double negativeZero = Double.longBitsToDouble(0x8000000000000000L);

    public static final Integer zeroObj = Integer.valueOf(0);
    public static final Double negativeZeroObj = Double.valueOf(-0.0);

    static double stringPrefixToNumber(String s, int start, int radix) {
        return stringToNumber(s, start, s.length() - 1, radix, true);
    }

    static double stringToNumber(String s, int start, int end, int radix) {
        return stringToNumber(s, start, end, radix, false);
    }

    /*
     * Helper function for toNumber, parseInt, and TokenStream.getToken.
     */
    private static double stringToNumber(
            String source, int sourceStart, int sourceEnd, int radix, boolean isPrefix) {
        char digitMax = '9';
        char lowerCaseBound = 'a';
        char upperCaseBound = 'A';
        if (radix < 10) {
            digitMax = (char) ('0' + radix - 1);
        }
        if (radix > 10) {
            lowerCaseBound = (char) ('a' + radix - 10);
            upperCaseBound = (char) ('A' + radix - 10);
        }
        int end;
        double sum = 0.0;
        for (end = sourceStart; end <= sourceEnd; end++) {
            char c = source.charAt(end);
            int newDigit;
            if ('0' <= c && c <= digitMax) newDigit = c - '0';
            else if ('a' <= c && c < lowerCaseBound) newDigit = c - 'a' + 10;
            else if ('A' <= c && c < upperCaseBound) newDigit = c - 'A' + 10;
            else if (!isPrefix) return NaN; // isn't a prefix but found unexpected char
            else break; // unexpected char
            sum = sum * radix + newDigit;
        }
        if (sourceStart == end) { // stopped right at the beginning
            return NaN;
        }
        if (sum > NativeNumber.MAX_SAFE_INTEGER) {
            if (radix == 10) {
                /* If we're accumulating a decimal number and the number
                 * is >= 2^53, then the result from the repeated multiply-add
                 * above may be inaccurate.  Call Java to get the correct
                 * answer.
                 */
                try {
                    return Double.parseDouble(source.substring(sourceStart, end));
                } catch (NumberFormatException nfe) {
                    return NaN;
                }
            } else if (radix == 2 || radix == 4 || radix == 8 || radix == 16 || radix == 32) {
                /* The number may also be inaccurate for one of these bases.
                 * This happens if the addition in value*radix + digit causes
                 * a round-down to an even least significant mantissa bit
                 * when the first dropped bit is a one.  If any of the
                 * following digits in the number (which haven't been added
                 * in yet) are nonzero then the correct action would have
                 * been to round up instead of down.  An example of this
                 * occurs when reading the number 0x1000000000000081, which
                 * rounds to 0x1000000000000000 instead of 0x1000000000000100.
                 */
                int bitShiftInChar = 1;
                int digit = 0;

                final int SKIP_LEADING_ZEROS = 0;
                final int FIRST_EXACT_53_BITS = 1;
                final int AFTER_BIT_53 = 2;
                final int ZEROS_AFTER_54 = 3;
                final int MIXED_AFTER_54 = 4;

                int state = SKIP_LEADING_ZEROS;
                int exactBitsLimit = 53;
                double factor = 0.0;
                boolean bit53 = false;
                // bit54 is the 54th bit (the first dropped from the mantissa)
                boolean bit54 = false;
                int pos = sourceStart;

                for (; ; ) {
                    if (bitShiftInChar == 1) {
                        if (pos == end) break;
                        digit = source.charAt(pos++);
                        if ('0' <= digit && digit <= '9') digit -= '0';
                        else if ('a' <= digit && digit <= 'z') digit -= 'a' - 10;
                        else digit -= 'A' - 10;
                        bitShiftInChar = radix;
                    }
                    bitShiftInChar >>= 1;
                    boolean bit = (digit & bitShiftInChar) != 0;

                    switch (state) {
                        case SKIP_LEADING_ZEROS:
                            if (bit) {
                                --exactBitsLimit;
                                sum = 1.0;
                                state = FIRST_EXACT_53_BITS;
                            }
                            break;
                        case FIRST_EXACT_53_BITS:
                            sum *= 2.0;
                            if (bit) sum += 1.0;
                            --exactBitsLimit;
                            if (exactBitsLimit == 0) {
                                bit53 = bit;
                                state = AFTER_BIT_53;
                            }
                            break;
                        case AFTER_BIT_53:
                            bit54 = bit;
                            factor = 2.0;
                            state = ZEROS_AFTER_54;
                            break;
                        case ZEROS_AFTER_54:
                            if (bit) {
                                state = MIXED_AFTER_54;
                            }
                        // fallthrough
                        case MIXED_AFTER_54:
                            factor *= 2;
                            break;
                    }
                }
                switch (state) {
                    case SKIP_LEADING_ZEROS:
                        sum = 0.0;
                        break;
                    case FIRST_EXACT_53_BITS:
                    case AFTER_BIT_53:
                        // do nothing
                        break;
                    case ZEROS_AFTER_54:
                        // x1.1 -> x1 + 1 (round up)
                        // x0.1 -> x0 (round down)
                        if (bit54 && bit53) sum += 1.0;
                        sum *= factor;
                        break;
                    case MIXED_AFTER_54:
                        // x.100...1.. -> x + 1 (round up)
                        // x.0anything -> x (round down)
                        if (bit54) sum += 1.0;
                        sum *= factor;
                        break;
                }
            }
            /* We don't worry about inaccurate numbers for any other base. */
        }
        return sum;
    }

    /**
     * ToNumber applied to the String type
     *
     * <p>See the #sec-tonumber-applied-to-the-string-type section of ECMA
     */
    public static double toNumber(String s) {
        final int len = s.length();

        // Skip whitespace at the start
        int start = 0;
        char startChar;
        for (; ; ) {
            if (start == len) {
                // empty or contains only whitespace
                return +0.0;
            }
            startChar = s.charAt(start);
            if (!ScriptRuntime.isStrWhiteSpaceChar(startChar)) {
                // found first non-whitespace character
                break;
            }
            start++;
        }

        // Skip whitespace at the end
        int end = len - 1;
        char endChar;
        while (ScriptRuntime.isStrWhiteSpaceChar(endChar = s.charAt(end))) {
            end--;
        }

        // Do not break scripts relying on old non-compliant conversion
        // (see bug #368)
        // 1. makes ToNumber parse only a valid prefix in hex literals (similar to 'parseInt()')
        //    ToNumber('0x10 something') => 16
        // 2. allows plus and minus signs for hexadecimal numbers
        //    ToNumber('-0x10') => -16
        // 3. disables support for binary ('0b10') and octal ('0o13') literals
        //    ToNumber('0b1') => NaN
        //    ToNumber('0o5') => NaN
        final Context cx = Context.getCurrentContext();
        final boolean oldParsingMode = cx == null || cx.getLanguageVersion() < Context.VERSION_ES6;

        // Handle non-base10 numbers
        if (startChar == '0') {
            if (start + 2 <= end) {
                final char radixC = s.charAt(start + 1);
                int radix = -1;
                if (radixC == 'x' || radixC == 'X') {
                    radix = 16;
                } else if (!oldParsingMode && (radixC == 'o' || radixC == 'O')) {
                    radix = 8;
                } else if (!oldParsingMode && (radixC == 'b' || radixC == 'B')) {
                    radix = 2;
                }
                if (radix != -1) {
                    if (oldParsingMode) {
                        return stringPrefixToNumber(s, start + 2, radix);
                    }
                    return stringToNumber(s, start + 2, end, radix);
                }
            }
        } else if (oldParsingMode && (startChar == '+' || startChar == '-')) {
            // If in old parsing mode, check for a signed hexadecimal number
            if (start + 3 <= end && s.charAt(start + 1) == '0') {
                final char radixC = s.charAt(start + 2);
                if (radixC == 'x' || radixC == 'X') {
                    double val = stringPrefixToNumber(s, start + 3, 16);
                    return startChar == '-' ? -val : val;
                }
            }
        }

        if (endChar == 'y') {
            // check for "Infinity"
            if (startChar == '+' || startChar == '-') {
                start++;
            }
            if (start + 7 == end && s.regionMatches(start, "Infinity", 0, 8)) {
                return startChar == '-' ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            }
            return NaN;
        }
        // A base10, non-infinity number:
        // just try a normal floating point conversion
        String sub = s.substring(start, end + 1);
        // Quick test to check string contains only valid characters because
        // Double.parseDouble() can be slow and accept input we want to reject
        for (int i = sub.length() - 1; i >= 0; i--) {
            char c = sub.charAt(i);
            if (('0' <= c && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-')
                continue;
            return NaN;
        }
        try {
            return Double.parseDouble(sub);
        } catch (NumberFormatException ex) {
            return NaN;
        }
    }

    /** Convert the value to a BigInt. */
    public static BigInteger toBigInt(Object val) {
        val = toPrimitive(val, NumberClass);
        if (val instanceof BigInteger) {
            return (BigInteger) val;
        }
        if (val instanceof BigDecimal) {
            return ((BigDecimal) val).toBigInteger();
        }
        if (val instanceof Number) {
            if (val instanceof Long) {
                return BigInteger.valueOf(((Long) val));
            } else {
                double d = ((Number) val).doubleValue();
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    throw rangeErrorById("msg.cant.convert.to.bigint.isnt.integer", toString(val));
                }
                BigDecimal bd = new BigDecimal(d, MathContext.UNLIMITED);
                try {
                    return bd.toBigIntegerExact();
                } catch (ArithmeticException e) {
                    throw rangeErrorById("msg.cant.convert.to.bigint.isnt.integer", toString(val));
                }
            }
        }
        if (val == null || Undefined.isUndefined(val)) {
            throw typeErrorById("msg.cant.convert.to.bigint", toString(val));
        }
        if (val instanceof String) {
            return toBigInt((String) val);
        }
        if (val instanceof CharSequence) {
            return toBigInt(val.toString());
        }
        if (val instanceof Boolean) {
            return ((Boolean) val).booleanValue() ? BigInteger.ONE : BigInteger.ZERO;
        }
        if (isSymbol(val)) {
            throw typeErrorById("msg.cant.convert.to.bigint", toString(val));
        }
        throw errorWithClassName("msg.primitive.expected", val);
    }

    /** ToBigInt applied to the String type */
    public static BigInteger toBigInt(String s) {
        final int len = s.length();

        // Skip whitespace at the start
        int start = 0;
        char startChar;
        for (; ; ) {
            if (start == len) {
                // empty or contains only whitespace
                return BigInteger.ZERO;
            }
            startChar = s.charAt(start);
            if (!ScriptRuntime.isStrWhiteSpaceChar(startChar)) {
                // found first non-whitespace character
                break;
            }
            start++;
        }

        // Skip whitespace at the end
        int end = len - 1;
        while (ScriptRuntime.isStrWhiteSpaceChar(s.charAt(end))) {
            end--;
        }

        // Handle non-base10 numbers
        if (startChar == '0') {
            if (start + 2 <= end) {
                final char radixC = s.charAt(start + 1);
                int radix = -1;
                if (radixC == 'x' || radixC == 'X') {
                    radix = 16;
                } else if (radixC == 'o' || radixC == 'O') {
                    radix = 8;
                } else if (radixC == 'b' || radixC == 'B') {
                    radix = 2;
                }
                if (radix != -1) {
                    try {
                        return new BigInteger(s.substring(start + 2, end + 1), radix);
                    } catch (NumberFormatException ex) {
                        throw syntaxErrorById("msg.bigint.bad.form");
                    }
                }
            }
        }

        // A base10, non-infinity bigint:
        // just try a normal biginteger conversion
        String sub = s.substring(start, end + 1);
        for (int i = sub.length() - 1; i >= 0; i--) {
            char c = sub.charAt(i);
            if (i == 0 && (c == '+' || c == '-')) {
                continue;
            }
            if ('0' <= c && c <= '9') {
                continue;
            }
            throw syntaxErrorById("msg.bigint.bad.form");
        }
        try {
            return new BigInteger(sub);
        } catch (NumberFormatException ex) {
            throw syntaxErrorById("msg.bigint.bad.form");
        }
    }

    /**
     * Convert the value to a Numeric (Number or BigInt).
     *
     * <p>toNumber does not allow java.math.BigInteger. toNumeric allows java.math.BigInteger.
     *
     * <p>See ECMA 7.1.3 (v11.0).
     */
    public static Number toNumeric(Object val) {
        val = toPrimitive(val, NumberClass);
        if (val instanceof Number) {
            return (Number) val;
        }
        return toNumber(val);
    }

    public static int toIndex(Object val) {
        if (Undefined.isUndefined(val)) {
            return 0;
        }
        double integerIndex = toInteger(val);
        if (integerIndex < 0) {
            throw rangeError("index out of range");
        }
        // ToLength
        double index = Math.min(integerIndex, NativeNumber.MAX_SAFE_INTEGER);
        if (integerIndex != index) {
            throw rangeError("index out of range");
        }
        return (int) index;
    }

    /**
     * Helper function for builtin objects that use the varargs form. ECMA function formal arguments
     * are undefined if not supplied; this function pads the argument array out to the expected
     * length, if necessary.
     */
    public static Object[] padArguments(Object[] args, int count) {
        if (count < args.length) return args;

        Object[] result = new Object[count];
        System.arraycopy(args, 0, result, 0, args.length);
        if (args.length < count) {
            Arrays.fill(result, args.length, count, Undefined.instance);
        }
        return result;
    }

    /**
     * Helper function for builtin objects that use the varargs form. ECMA function formal arguments
     * are undefined if not supplied; this function pads the argument array out to the expected
     * length, if necessary. Also the rest parameter array construction is done here.
     */
    public static Object[] padAndRestArguments(
            Context cx, Scriptable scope, Object[] args, int argCount) {
        Object[] result = new Object[argCount];
        int paramCount = argCount - 1;
        if (args.length < paramCount) {
            System.arraycopy(args, 0, result, 0, args.length);
            Arrays.fill(result, args.length, paramCount, Undefined.instance);
        } else {
            System.arraycopy(args, 0, result, 0, paramCount);
        }

        Object[] restValues;
        if (args.length > paramCount) {
            restValues = new Object[args.length - paramCount];
            System.arraycopy(args, paramCount, restValues, 0, restValues.length);
        } else {
            restValues = ScriptRuntime.emptyArgs;
        }
        result[paramCount] = cx.newArray(scope, restValues);
        return result;
    }

    public static String escapeString(String s) {
        return escapeString(s, '"');
    }

    /**
     * For escaping strings printed by object and array literals; not quite the same as 'escape.'
     */
    public static String escapeString(String s, char escapeQuote) {
        if (!(escapeQuote == '"' || escapeQuote == '\'')) Kit.codeBug();
        StringBuilder sb = null;

        for (int i = 0, L = s.length(); i != L; ++i) {
            int c = s.charAt(i);

            if (' ' <= c && c <= '~' && c != escapeQuote && c != '\\') {
                // an ordinary print character (like C isprint()) and not "
                // or \ .
                if (sb != null) {
                    sb.append((char) c);
                }
                continue;
            }
            if (sb == null) {
                sb = new StringBuilder(L + 3);
                sb.append(s);
                sb.setLength(i);
            }

            int escape = -1;
            switch (c) {
                case '\b':
                    escape = 'b';
                    break;
                case '\f':
                    escape = 'f';
                    break;
                case '\n':
                    escape = 'n';
                    break;
                case '\r':
                    escape = 'r';
                    break;
                case '\t':
                    escape = 't';
                    break;
                case 0xb:
                    escape = 'v';
                    break; // Java lacks \v.
                case ' ':
                    escape = ' ';
                    break;
                case '\\':
                    escape = '\\';
                    break;
            }
            if (escape >= 0) {
                // an \escaped sort of character
                sb.append('\\');
                sb.append((char) escape);
            } else if (c == escapeQuote) {
                sb.append('\\');
                sb.append(escapeQuote);
            } else {
                int hexSize;
                if (c < 256) {
                    // 2-digit hex
                    sb.append("\\x");
                    hexSize = 2;
                } else {
                    // Unicode.
                    sb.append("\\u");
                    hexSize = 4;
                }
                // append hexadecimal form of c left-padded with 0
                for (int shift = (hexSize - 1) * 4; shift >= 0; shift -= 4) {
                    int digit = 0xf & (c >> shift);
                    int hc = (digit < 10) ? '0' + digit : 'a' - 10 + digit;
                    sb.append((char) hc);
                }
            }
        }
        return (sb == null) ? s : sb.toString();
    }

    static boolean isValidIdentifierName(String s, Context cx, boolean isStrict) {
        int L = s.length();
        if (L == 0) return false;
        if (!Character.isJavaIdentifierStart(s.charAt(0))) return false;
        for (int i = 1; i != L; ++i) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) return false;
        }
        return !TokenStream.isKeyword(s, cx.getLanguageVersion(), isStrict);
    }

    public static CharSequence toCharSequence(Object val) {
        if (val instanceof NativeString) {
            return ((NativeString) val).toCharSequence();
        }
        return val instanceof CharSequence ? (CharSequence) val : toString(val);
    }

    /**
     * Convert the value to a string.
     *
     * <p>See ECMA 9.8.
     */
    public static String toString(Object val) {
        for (; ; ) {
            if (val == null) {
                return "null";
            }
            if (Undefined.isUndefined(val)) {
                return "undefined";
            }
            if (val instanceof String) {
                return (String) val;
            }
            if (val instanceof CharSequence) {
                return val.toString();
            }
            if (val instanceof BigInteger) {
                return ((BigInteger) val).toString(10);
            }
            if (val instanceof Number) {
                // XXX should we just teach NativeNumber.stringValue()
                // about Numbers?
                return numberToString(((Number) val).doubleValue(), 10);
            }
            if (val instanceof Boolean) {
                return val.toString();
            }
            if (isSymbol(val)) {
                throw typeErrorById("msg.not.a.string");
            }
            if (val instanceof Scriptable) {
                // Assert: val is an Object
                val = toPrimitive(val, StringClass);
                // Assert: val is a primitive
            } else {
                warnAboutNonJSObject(val);
                return val.toString();
            }
        }
    }

    static String defaultObjectToString(Scriptable obj) {
        if (obj == null) return "[object Null]";
        if (Undefined.isUndefined(obj)) return "[object Undefined]";

        Object tagValue = ScriptableObject.getProperty(obj, SymbolKey.TO_STRING_TAG);
        // Note: Scriptable.NOT_FOUND is not a CharSequence, so we don't need to explicitly check
        // for it
        if (tagValue instanceof CharSequence) {
            return "[object " + tagValue + "]";
        }

        return "[object " + obj.getClassName() + "]";
    }

    public static String toString(Object[] args, int index) {
        return (index < args.length) ? toString(args[index]) : "undefined";
    }

    /** Optimized version of toString(Object) for numbers. */
    public static String toString(double val) {
        return numberToString(val, 10);
    }

    public static String numberToString(double d, int base) {
        if ((base < 2) || (base > 36)) {
            throw ScriptRuntime.rangeErrorById("msg.bad.radix", Integer.toString(base));
        }

        if (Double.isNaN(d)) return "NaN";
        if (d == Double.POSITIVE_INFINITY) return "Infinity";
        if (d == Double.NEGATIVE_INFINITY) return "-Infinity";
        if (d == 0.0) return "0";

        if (base != 10) {
            return DToA.JS_dtobasestr(base, d);
        }
        // V8 FastDtoa can't convert all numbers, so try it first but
        // fall back to old DToA in case it fails
        String result = FastDtoa.numberToString(d);
        if (result != null) {
            return result;
        }
        StringBuilder buffer = new StringBuilder();
        DToA.JS_dtostr(buffer, DToA.DTOSTR_STANDARD, 0, d);
        return buffer.toString();
    }

    public static String bigIntToString(BigInteger n, int base) {
        if ((base < 2) || (base > 36)) {
            throw rangeErrorById("msg.bad.radix", Integer.toString(base));
        }

        return n.toString(base);
    }

    static String uneval(Context cx, Scriptable scope, Object value) {
        if (value == null) {
            return "null";
        }
        if (Undefined.isUndefined(value)) {
            return "undefined";
        }
        if (value instanceof CharSequence) {
            String escaped = escapeString(value.toString());
            StringBuilder sb = new StringBuilder(escaped.length() + 2);
            sb.append('\"');
            sb.append(escaped);
            sb.append('\"');
            return sb.toString();
        }
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (d == 0 && 1 / d < 0) {
                return "-0";
            }
            return toString(d);
        }
        if (value instanceof Boolean) {
            return toString(value);
        }
        if (value instanceof Scriptable) {
            Scriptable obj = (Scriptable) value;
            // Wrapped Java objects won't have "toSource" and will report
            // errors for get()s of nonexistent name, so use has() first
            if (ScriptableObject.hasProperty(obj, "toSource")) {
                Object v = ScriptableObject.getProperty(obj, "toSource");
                if (v instanceof Function) {
                    Function f = (Function) v;
                    return toString(f.call(cx, scope, obj, emptyArgs));
                }
            }
            return toString(value);
        }
        warnAboutNonJSObject(value);
        return value.toString();
    }

    static String defaultObjectToSource(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        boolean toplevel, iterating;
        if (cx.iterating == null) {
            toplevel = true;
            iterating = false;
            cx.iterating = new HashSet<>();
        } else {
            toplevel = false;
            iterating = cx.iterating.contains(thisObj);
        }

        StringBuilder result = new StringBuilder(128);
        if (toplevel) {
            result.append("(");
        }
        result.append('{');

        // Make sure cx.iterating is set to null when done
        // so we don't leak memory
        try {
            if (!iterating) {
                cx.iterating.add(thisObj); // stop recursion.
                Object[] ids = thisObj.getIds();
                for (int i = 0; i < ids.length; i++) {
                    Object id = ids[i];
                    Object value;
                    if (id instanceof Integer) {
                        int intId = ((Integer) id).intValue();
                        value = thisObj.get(intId, thisObj);
                        if (value == Scriptable.NOT_FOUND) continue; // a property has been removed
                        if (i > 0) result.append(", ");
                        result.append(intId);
                    } else {
                        String strId = (String) id;
                        value = thisObj.get(strId, thisObj);
                        if (value == Scriptable.NOT_FOUND) continue; // a property has been removed
                        if (i > 0) result.append(", ");
                        if (ScriptRuntime.isValidIdentifierName(strId, cx, cx.isStrictMode())) {
                            result.append(strId);
                        } else {
                            result.append('\'');
                            result.append(ScriptRuntime.escapeString(strId, '\''));
                            result.append('\'');
                        }
                    }
                    result.append(':');
                    result.append(ScriptRuntime.uneval(cx, scope, value));
                }
            }
        } finally {
            if (toplevel) {
                cx.iterating = null;
            }
        }

        result.append('}');
        if (toplevel) {
            result.append(')');
        }
        return result.toString();
    }

    public static Scriptable toObject(Scriptable scope, Object val) {
        if (val instanceof Scriptable) {
            return (Scriptable) val;
        }
        return toObject(Context.getContext(), scope, val);
    }

    /**
     * <strong>Warning</strong>: This doesn't allow to resolve primitive prototype properly when
     * many top scopes are involved
     *
     * @deprecated Use {@link #toObjectOrNull(Context, Object, Scriptable)} instead
     */
    @Deprecated
    public static Scriptable toObjectOrNull(Context cx, Object obj) {
        if (obj instanceof Scriptable) {
            return (Scriptable) obj;
        } else if (obj != null && !Undefined.isUndefined(obj)) {
            return toObject(cx, getTopCallScope(cx), obj);
        }
        return null;
    }

    /**
     * @param scope the scope that should be used to resolve primitive prototype
     */
    public static Scriptable toObjectOrNull(Context cx, Object obj, Scriptable scope) {
        if (obj instanceof Scriptable) {
            return (Scriptable) obj;
        } else if (obj != null && !Undefined.isUndefined(obj)) {
            return toObject(cx, scope, obj);
        }
        return null;
    }

    /**
     * @deprecated Use {@link #toObject(Scriptable, Object)} instead.
     */
    @Deprecated
    public static Scriptable toObject(Scriptable scope, Object val, Class<?> staticClass) {
        if (val instanceof Scriptable) {
            return (Scriptable) val;
        }
        return toObject(Context.getContext(), scope, val);
    }

    /**
     * Convert the value to an object.
     *
     * <p>See ECMA 9.9.
     */
    public static Scriptable toObject(Context cx, Scriptable scope, Object val) {
        if (val == null) {
            throw typeErrorById("msg.null.to.object");
        }
        if (Undefined.isUndefined(val)) {
            throw typeErrorById("msg.undef.to.object");
        }

        if (isSymbol(val)) {
            if (val instanceof SymbolKey) {
                NativeSymbol result =
                        new NativeSymbol((SymbolKey) val, NativeSymbol.SymbolKind.REGULAR);
                setBuiltinProtoAndParent(result, scope, TopLevel.Builtins.Symbol);
                return result;
            }

            NativeSymbol result = new NativeSymbol((NativeSymbol) val);
            setBuiltinProtoAndParent(result, scope, TopLevel.Builtins.Symbol);
            return result;
        }
        if (val instanceof Scriptable) {
            return (Scriptable) val;
        }
        if (val instanceof CharSequence) {
            // FIXME we want to avoid toString() here, especially for concat()
            NativeString result = new NativeString((CharSequence) val);
            setBuiltinProtoAndParent(result, scope, TopLevel.Builtins.String);
            return result;
        }
        if (cx.getLanguageVersion() >= Context.VERSION_ES6 && val instanceof BigInteger) {
            NativeBigInt result = new NativeBigInt(((BigInteger) val));
            setBuiltinProtoAndParent(result, scope, TopLevel.Builtins.BigInt);
            return result;
        }
        if (val instanceof Number) {
            NativeNumber result = new NativeNumber(((Number) val).doubleValue());
            setBuiltinProtoAndParent(result, scope, TopLevel.Builtins.Number);
            return result;
        }
        if (val instanceof Boolean) {
            NativeBoolean result = new NativeBoolean(((Boolean) val).booleanValue());
            setBuiltinProtoAndParent(result, scope, TopLevel.Builtins.Boolean);
            return result;
        }

        // Extension: Wrap as a LiveConnect object.
        Object wrapped = cx.getWrapFactory().wrap(cx, scope, val, null);
        if (wrapped instanceof Scriptable) return (Scriptable) wrapped;
        throw errorWithClassName("msg.invalid.type", val);
    }

    /**
     * @deprecated Use {@link #toObject(Context, Scriptable, Object)} instead.
     */
    @Deprecated
    public static Scriptable toObject(
            Context cx, Scriptable scope, Object val, Class<?> staticClass) {
        return toObject(cx, scope, val);
    }

    /**
     * @deprecated The method is only present for compatibility.
     */
    @Deprecated
    public static Object call(
            Context cx, Object fun, Object thisArg, Object[] args, Scriptable scope) {
        if (!(fun instanceof Function)) {
            throw notFunctionError(toString(fun));
        }
        Function function = (Function) fun;
        Scriptable thisObj = toObjectOrNull(cx, thisArg, scope);
        if (thisObj == null) {
            throw undefCallError(null, "function");
        }
        return function.call(cx, scope, thisObj, args);
    }

    public static Scriptable newObject(
            Context cx, Scriptable scope, String constructorName, Object[] args) {
        scope = ScriptableObject.getTopLevelScope(scope);
        Constructable ctor = getExistingCtor(cx, scope, constructorName);
        if (args == null) {
            args = ScriptRuntime.emptyArgs;
        }
        return ctor.construct(cx, scope, args);
    }

    public static Scriptable newBuiltinObject(
            Context cx, Scriptable scope, TopLevel.Builtins type, Object[] args) {
        scope = ScriptableObject.getTopLevelScope(scope);
        Constructable ctor = TopLevel.getBuiltinCtor(cx, scope, type);
        if (args == null) {
            args = ScriptRuntime.emptyArgs;
        }
        return ctor.construct(cx, scope, args);
    }

    static Scriptable newNativeError(
            Context cx, Scriptable scope, TopLevel.NativeErrors type, Object[] args) {
        scope = ScriptableObject.getTopLevelScope(scope);
        Constructable ctor = TopLevel.getNativeErrorCtor(cx, scope, type);
        if (args == null) {
            args = ScriptRuntime.emptyArgs;
        }
        return ctor.construct(cx, scope, args);
    }

    /** See ECMA 9.4. */
    public static double toInteger(Object val) {
        return toInteger(toNumber(val));
    }

    // convenience method
    public static double toInteger(double d) {
        // if it's NaN
        if (Double.isNaN(d)) return +0.0;

        if ((d == 0.0) || Double.isInfinite(d)) return d;

        if (d > 0.0) return Math.floor(d);

        return Math.ceil(d);
    }

    public static double toInteger(Object[] args, int index) {
        return (index < args.length) ? toInteger(args[index]) : +0.0;
    }

    public static long toLength(Object[] args, int index) {
        double len = toInteger(args, index);
        if (len <= 0.0) {
            return 0;
        }
        return (long) Math.min(len, NativeNumber.MAX_SAFE_INTEGER);
    }

    public static long toLength(Object value) {
        double len = toInteger(value);
        if (len <= 0.0) {
            return 0;
        }
        return (long) Math.min(len, NativeNumber.MAX_SAFE_INTEGER);
    }

    /** See ECMA 9.5. */
    public static int toInt32(Object val) {
        // short circuit for common integer values
        if (val instanceof Integer) return ((Integer) val).intValue();

        return toInt32(toNumber(val));
    }

    public static int toInt32(Object[] args, int index) {
        return (index < args.length) ? toInt32(args[index]) : 0;
    }

    public static int toInt32(double d) {
        return DoubleConversion.doubleToInt32(d);
    }

    /**
     * See ECMA 9.6.
     *
     * @return long value representing 32 bits unsigned integer
     */
    public static long toUint32(double d) {
        return DoubleConversion.doubleToInt32(d) & 0xffffffffL;
    }

    public static long toUint32(Object val) {
        return toUint32(toNumber(val));
    }

    /** See ECMA 9.7. */
    public static char toUint16(Object val) {
        double d = toNumber(val);
        return (char) DoubleConversion.doubleToInt32(d);
    }

    /**
     * If "arg" is a "canonical numeric index," which means any number constructed from a string
     * that doesn't have extra whitespace or non-standard formatting, return it -- otherwise return
     * an empty option. Defined in ECMA 7.1.21.
     */
    public static Optional<Double> canonicalNumericIndexString(String arg) {
        if ("-0".equals(arg)) {
            return Optional.of(Double.NEGATIVE_INFINITY);
        }
        double num = toNumber(arg);
        // According to tests, "NaN" is not a number ;-)
        if (Double.isNaN(num)) {
            return Optional.empty();
        }
        String numStr = toString(num);
        if (numStr.equals(arg)) {
            return Optional.of(num);
        }
        return Optional.empty();
    }

    /** Implements the abstract operation AdvanceStringIndex. See ECMAScript spec 22.2.7.3 */
    public static long advanceStringIndex(String string, long index, boolean unicode) {
        if (index >= NativeNumber.MAX_SAFE_INTEGER) Kit.codeBug();
        if (!unicode) {
            return index + 1;
        }
        int length = string.length();
        if (index + 1 > length) {
            return index + 1;
        }
        int cp = string.codePointAt((int) index);
        return index + Character.charCount(cp);
    }

    // XXX: this is until setDefaultNamespace will learn how to store NS
    // properly and separates namespace form Scriptable.get etc.
    private static final String DEFAULT_NS_TAG = "__default_namespace__";

    public static Object setDefaultNamespace(Object namespace, Context cx) {
        Scriptable scope = cx.currentActivationCall;
        if (scope == null) {
            scope = getTopCallScope(cx);
        }

        XMLLib xmlLib = currentXMLLib(cx);
        Object ns = xmlLib.toDefaultXmlNamespace(cx, namespace);

        // XXX : this should be in separated namesapce from Scriptable.get/put
        if (!scope.has(DEFAULT_NS_TAG, scope)) {
            // XXX: this is racy of cause
            ScriptableObject.defineProperty(
                    scope,
                    DEFAULT_NS_TAG,
                    ns,
                    ScriptableObject.PERMANENT | ScriptableObject.DONTENUM);
        } else {
            scope.put(DEFAULT_NS_TAG, scope, ns);
        }

        return Undefined.instance;
    }

    public static Object searchDefaultNamespace(Context cx) {
        Scriptable scope = cx.currentActivationCall;
        if (scope == null) {
            scope = getTopCallScope(cx);
        }
        Object nsObject;
        for (; ; ) {
            Scriptable parent = scope.getParentScope();
            if (parent == null) {
                nsObject = ScriptableObject.getProperty(scope, DEFAULT_NS_TAG);
                if (nsObject == Scriptable.NOT_FOUND) {
                    return null;
                }
                break;
            }
            nsObject = scope.get(DEFAULT_NS_TAG, scope);
            if (nsObject != Scriptable.NOT_FOUND) {
                break;
            }
            scope = parent;
        }
        return nsObject;
    }

    public static Object getTopLevelProp(Scriptable scope, String id) {
        scope = ScriptableObject.getTopLevelScope(scope);
        return ScriptableObject.getProperty(scope, id);
    }

    public static Function getExistingCtor(Context cx, Scriptable scope, String constructorName) {
        Object ctorVal = ScriptableObject.getProperty(scope, constructorName);
        if (ctorVal instanceof Function) {
            return (Function) ctorVal;
        }
        if (ctorVal == Scriptable.NOT_FOUND) {
            throw Context.reportRuntimeErrorById("msg.ctor.not.found", constructorName);
        }
        throw Context.reportRuntimeErrorById("msg.not.ctor", constructorName);
    }

    /**
     * Return -1L if str is not an index, or the index value as lower 32 bits of the result. Note
     * that the result needs to be cast to an int in order to produce the actual index, which may be
     * negative.
     *
     * <p>Note that this method on its own does not actually produce an index that is useful for an
     * actual Object or Array, because it may be larger than Integer.MAX_VALUE. Most callers should
     * instead call toStringOrIndex, which calls this under the covers.
     */
    public static long indexFromString(String str) {
        // The length of the decimal string representation of
        //  Integer.MAX_VALUE, 2147483647
        final int MAX_VALUE_LENGTH = 10;

        int len = str.length();
        if (len > 0) {
            int i = 0;
            boolean negate = false;
            int c = str.charAt(0);
            if (c == '-') {
                if (len > 1) {
                    c = str.charAt(1);
                    if (c == '0') return -1L; // "-0" is not an index
                    i = 1;
                    negate = true;
                }
            }
            c -= '0';
            if (0 <= c && c <= 9 && len <= (negate ? MAX_VALUE_LENGTH + 1 : MAX_VALUE_LENGTH)) {
                // Use negative numbers to accumulate index to handle
                // Integer.MIN_VALUE that is greater by 1 in absolute value
                // then Integer.MAX_VALUE
                int index = -c;
                int oldIndex = 0;
                i++;
                if (index != 0) {
                    // Note that 00, 01, 000 etc. are not indexes
                    while (i != len && 0 <= (c = str.charAt(i) - '0') && c <= 9) {
                        oldIndex = index;
                        index = 10 * index - c;
                        i++;
                    }
                }
                // Make sure all characters were consumed and that it couldn't
                // have overflowed.
                if (i == len
                        && (oldIndex > (Integer.MIN_VALUE / 10)
                                || (oldIndex == (Integer.MIN_VALUE / 10)
                                        && c
                                                <= (negate
                                                        ? -(Integer.MIN_VALUE % 10)
                                                        : (Integer.MAX_VALUE % 10))))) {
                    return 0xFFFFFFFFL & (negate ? index : -index);
                }
            }
        }
        return -1L;
    }

    /** If str is a decimal presentation of Uint32 value, return it as long. Othewise return -1L; */
    public static long testUint32String(String str) {
        // The length of the decimal string representation of
        //  UINT32_MAX_VALUE, 4294967296
        final int MAX_VALUE_LENGTH = 10;

        int len = str.length();
        if (1 <= len && len <= MAX_VALUE_LENGTH) {
            int c = str.charAt(0);
            c -= '0';
            if (c == 0) {
                // Note that 00,01 etc. are not valid Uint32 presentations
                return (len == 1) ? 0L : -1L;
            }
            if (1 <= c && c <= 9) {
                long v = c;
                for (int i = 1; i != len; ++i) {
                    c = str.charAt(i) - '0';
                    if (!(0 <= c && c <= 9)) {
                        return -1;
                    }
                    v = 10 * v + c;
                }
                // Check for overflow
                if ((v >>> 32) == 0) {
                    return v;
                }
            }
        }
        return -1;
    }

    /** If s represents index, then return index value wrapped as Integer and othewise return s. */
    static Object getIndexObject(String s) {
        long indexTest = indexFromString(s);
        if (indexTest >= 0 && indexTest <= Integer.MAX_VALUE) {
            return Integer.valueOf((int) indexTest);
        }
        return s;
    }

    /**
     * If d is exact int value, return its value wrapped as Integer and othewise return d converted
     * to String.
     */
    static Object getIndexObject(double d) {
        int i = (int) d;
        if (i == d) {
            return Integer.valueOf(i);
        }
        return toString(d);
    }

    /**
     * Helper to return a string or an integer. Always use a null check on s.stringId to determine
     * if the result is string or integer.
     *
     * @see ScriptRuntime#toStringIdOrIndex(Object)
     */
    public static final class StringIdOrIndex {
        final String stringId;
        final int index;

        StringIdOrIndex(String stringId) {
            this.stringId = stringId;
            this.index = -1;
        }

        StringIdOrIndex(int index) {
            this.stringId = null;
            this.index = index;
        }

        public String getStringId() {
            return stringId;
        }

        public int getIndex() {
            return index;
        }
    }

    /**
     * If id is a number or a string presentation of an int32 value, then id the returning
     * StringIdOrIndex has the index set, otherwise the stringId is set.
     */
    public static StringIdOrIndex toStringIdOrIndex(Object id) {
        if (id instanceof Number) {
            double d = ((Number) id).doubleValue();
            if (d < 0.0) {
                return new StringIdOrIndex(toString(id));
            }
            int index = (int) d;
            if (index == d) {
                return new StringIdOrIndex(index);
            }
            return new StringIdOrIndex(toString(id));
        }
        String s;
        if (id instanceof String) {
            s = (String) id;
        } else {
            s = toString(id);
        }
        long indexTest = indexFromString(s);
        if (indexTest >= 0 && indexTest <= Integer.MAX_VALUE) {
            return new StringIdOrIndex((int) indexTest);
        }
        return new StringIdOrIndex(s);
    }

    /**
     * Call obj.[[Get]](id)
     *
     * @deprecated Use {@link #getObjectElem(Object, Object, Context, Scriptable)} instead
     */
    @Deprecated
    public static Object getObjectElem(Object obj, Object elem, Context cx) {
        return getObjectElem(obj, elem, cx, getTopCallScope(cx));
    }

    /** Call obj.[[Get]](id) */
    public static Object getObjectElem(Object obj, Object elem, Context cx, Scriptable scope) {
        Scriptable sobj = asScriptableOrThrowUndefReadError(cx, scope, obj, elem);
        return getObjectElem(sobj, elem, cx);
    }

    public static Object getObjectElem(Scriptable obj, Object elem, Context cx) {
        Object result;

        if (obj instanceof XMLObject) {
            result = ((XMLObject) obj).get(cx, elem);
        } else if (isSymbol(elem)) {
            result = ScriptableObject.getProperty(obj, (Symbol) elem);
        } else {
            StringIdOrIndex s = toStringIdOrIndex(elem);
            if (s.stringId == null) {
                int index = s.index;
                result = ScriptableObject.getProperty(obj, index);
            } else {
                result = ScriptableObject.getProperty(obj, s.stringId);
            }
        }

        if (result == Scriptable.NOT_FOUND) {
            result = Undefined.instance;
        }
        return result;
    }

    public static Object getSuperElem(
            Object superObject, Object elem, Context cx, Scriptable scope, Object thisObject) {
        Scriptable superScriptable =
                asScriptableOrThrowUndefReadError(cx, scope, superObject, elem);
        Scriptable thisScriptable = asScriptableOrThrowUndefReadError(cx, scope, thisObject, elem);
        return getSuperElem(elem, superScriptable, thisScriptable);
    }

    public static Object getSuperElem(
            Object elem, Scriptable superScriptable, Scriptable thisScriptable) {
        Object result;
        // No XML support for super
        if (isSymbol(elem)) {
            result =
                    ScriptableObject.getSuperProperty(
                            superScriptable, thisScriptable, (Symbol) elem);
        } else {
            StringIdOrIndex s = toStringIdOrIndex(elem);
            if (s.stringId == null) {
                int index = s.index;
                result = ScriptableObject.getSuperProperty(superScriptable, thisScriptable, index);
            } else {
                result =
                        ScriptableObject.getSuperProperty(
                                superScriptable, thisScriptable, s.stringId);
            }
        }

        if (result == Scriptable.NOT_FOUND) {
            result = Undefined.instance;
        }
        return result;
    }

    /**
     * Version of getObjectElem when elem is a valid JS identifier name.
     *
     * @deprecated Use {@link #getObjectProp(Object, String, Context, Scriptable)} instead
     */
    @Deprecated
    public static Object getObjectProp(Object obj, String property, Context cx) {
        return getObjectProp(obj, property, cx, getTopCallScope(cx));
    }

    /**
     * Version of getObjectElem when elem is a valid JS identifier name.
     *
     * @param scope the scope that should be used to resolve primitive prototype
     */
    public static Object getObjectProp(Object obj, String property, Context cx, Scriptable scope) {
        Scriptable sobj = asScriptableOrThrowUndefReadError(cx, scope, obj, property);
        return getObjectProp(sobj, property, cx);
    }

    public static Object getObjectProp(Scriptable obj, String property, Context cx) {
        Object result = ScriptableObject.getProperty(obj, property);
        if (result == Scriptable.NOT_FOUND) {
            if (cx.hasFeature(Context.FEATURE_STRICT_MODE)) {
                Context.reportWarning(
                        ScriptRuntime.getMessageById("msg.ref.undefined.prop", property));
            }
            result = Undefined.instance;
        }

        return result;
    }

    /**
     * @deprecated Use {@link #getObjectPropNoWarn(Object, String, Context, Scriptable)} instead
     */
    @Deprecated
    public static Object getObjectPropNoWarn(Object obj, String property, Context cx) {
        return getObjectPropNoWarn(obj, property, cx, getTopCallScope(cx));
    }

    public static Object getObjectPropNoWarn(
            Object obj, String property, Context cx, Scriptable scope) {
        Scriptable sobj = asScriptableOrThrowUndefReadError(cx, scope, obj, property);
        Object result = ScriptableObject.getProperty(sobj, property);
        if (result == Scriptable.NOT_FOUND) {
            return Undefined.instance;
        }
        return result;
    }

    public static Object getSuperProp(
            Object superObject,
            String property,
            Context cx,
            Scriptable scope,
            Object thisObject,
            boolean noWarn) {
        Scriptable superScriptable =
                asScriptableOrThrowUndefReadError(cx, scope, superObject, property);
        Scriptable thisScriptable =
                asScriptableOrThrowUndefReadError(cx, scope, thisObject, property);
        return getSuperProp(superScriptable, thisScriptable, property, cx, noWarn);
    }

    private static Object getSuperProp(
            Scriptable superScriptable,
            Scriptable thisScriptable,
            String property,
            Context cx,
            boolean noWarn) {
        Object result =
                ScriptableObject.getSuperProperty(superScriptable, thisScriptable, property);

        if (result == Scriptable.NOT_FOUND) {
            if (noWarn) {
                return Undefined.instance;
            }
            if (cx.hasFeature(Context.FEATURE_STRICT_MODE)) {
                Context.reportWarning(
                        ScriptRuntime.getMessageById("msg.ref.undefined.prop", property));
            }
            return Undefined.instance;
        }
        return result;
    }

    /**
     * A cheaper and less general version of the above for well-known argument types.
     *
     * @deprecated Use {@link #getObjectIndex(Object, double, Context, Scriptable)} instead
     */
    @Deprecated
    public static Object getObjectIndex(Object obj, double dblIndex, Context cx) {
        return getObjectIndex(obj, dblIndex, cx, getTopCallScope(cx));
    }

    /** A cheaper and less general version of the above for well-known argument types. */
    public static Object getObjectIndex(Object obj, double dblIndex, Context cx, Scriptable scope) {
        Scriptable sobj = asScriptableOrThrowUndefReadError(cx, scope, obj, dblIndex);

        int index = (int) dblIndex;
        if (index == dblIndex && index >= 0) {
            return getObjectIndex(sobj, index, cx);
        }
        String s = toString(dblIndex);
        return getObjectProp(sobj, s, cx);
    }

    public static Object getObjectIndex(Scriptable obj, int index, Context cx) {
        Object result = ScriptableObject.getProperty(obj, index);
        if (result == Scriptable.NOT_FOUND) {
            result = Undefined.instance;
        }
        return result;
    }

    public static Object getSuperIndex(
            Object superObject, double dblIndex, Context cx, Scriptable scope, Object thisObject) {
        Scriptable superScriptable =
                asScriptableOrThrowUndefReadError(cx, scope, superObject, dblIndex);
        Scriptable thisScriptable =
                asScriptableOrThrowUndefReadError(cx, scope, thisObject, dblIndex);

        int index = (int) dblIndex;
        if (index == dblIndex && index >= 0) {
            return getSuperIndex(superScriptable, thisScriptable, index);
        }

        String s = toString(dblIndex);
        return getSuperProp(superScriptable, thisScriptable, s, cx, false);
    }

    private static Object getSuperIndex(
            Scriptable superScriptable, Scriptable thisScriptable, int index) {
        Object result = ScriptableObject.getSuperProperty(superScriptable, thisScriptable, index);
        if (result == Scriptable.NOT_FOUND) {
            return Undefined.instance;
        }
        return result;
    }

    /**
     * Call obj.[[Put]](id, value)
     *
     * @deprecated Use {@link #setObjectElem(Object, Object, Object, Context, Scriptable)} instead
     */
    @Deprecated
    public static Object setObjectElem(Object obj, Object elem, Object value, Context cx) {
        return setObjectElem(obj, elem, value, cx, getTopCallScope(cx));
    }

    /** Call obj.[[Put]](id, value) */
    public static Object setObjectElem(
            Object obj, Object elem, Object value, Context cx, Scriptable scope) {
        Scriptable sobj = asScriptableOrThrowUndefWriteError(cx, scope, obj, elem, value);
        return setObjectElem(sobj, elem, value, cx);
    }

    public static Object setObjectElem(Scriptable obj, Object elem, Object value, Context cx) {
        if (obj instanceof XMLObject) {
            ((XMLObject) obj).put(cx, elem, value);
        } else if (isSymbol(elem)) {
            ScriptableObject.putProperty(obj, (Symbol) elem, value);
        } else {
            StringIdOrIndex s = toStringIdOrIndex(elem);
            if (s.stringId == null) {
                ScriptableObject.putProperty(obj, s.index, value);
            } else {
                ScriptableObject.putProperty(obj, s.stringId, value);
            }
        }

        return value;
    }

    /** Call super.[[Put]](id, value) */
    public static Object setSuperElem(
            Object superObject,
            Object elem,
            Object value,
            Context cx,
            Scriptable scope,
            Object thisObject) {
        Scriptable superScriptable =
                asScriptableOrThrowUndefWriteError(cx, scope, superObject, elem, value);
        Scriptable thisScriptable =
                asScriptableOrThrowUndefWriteError(cx, scope, thisObject, elem, value);
        return setSuperElem(superScriptable, thisScriptable, elem, value, cx);
    }

    public static Object setSuperElem(
            Scriptable superScriptable,
            Scriptable thisScriptable,
            Object elem,
            Object value,
            Context cx) {
        // No XML support for super
        if (isSymbol(elem)) {
            ScriptableObject.putSuperProperty(
                    superScriptable, thisScriptable, (Symbol) elem, value);
        } else {
            StringIdOrIndex s = toStringIdOrIndex(elem);
            if (s.stringId == null) {
                ScriptableObject.putSuperProperty(superScriptable, thisScriptable, s.index, value);
            } else {
                ScriptableObject.putSuperProperty(
                        superScriptable, thisScriptable, s.stringId, value);
            }
        }
        return value;
    }

    /**
     * Version of setObjectElem when elem is a valid JS identifier name.
     *
     * @deprecated Use {@link #setObjectProp(Object, String, Object, Context, Scriptable)} instead
     */
    @Deprecated
    public static Object setObjectProp(Object obj, String property, Object value, Context cx) {
        return setObjectProp(obj, property, value, cx, getTopCallScope(cx));
    }

    /** Version of setObjectElem when elem is a valid JS identifier name. */
    public static Object setObjectProp(
            Object obj, String property, Object value, Context cx, Scriptable scope) {
        verifyIsScriptableOrComplainWriteErrorInEs5Strict(obj, property, value, cx);
        Scriptable sobj = asScriptableOrThrowUndefWriteError(cx, scope, obj, property, value);
        return setObjectProp(sobj, property, value, cx);
    }

    public static Object setObjectProp(Scriptable obj, String property, Object value, Context cx) {
        ScriptableObject.putProperty(obj, property, value);
        return value;
    }

    /** Version of setSuperElem when elem is a valid JS identifier name. */
    public static Object setSuperProp(
            Object superObject,
            String property,
            Object value,
            Context cx,
            Scriptable scope,
            Object thisObject) {
        verifyIsScriptableOrComplainWriteErrorInEs5Strict(superObject, property, value, cx);
        verifyIsScriptableOrComplainWriteErrorInEs5Strict(thisObject, property, value, cx);

        Scriptable superScriptable =
                asScriptableOrThrowUndefWriteError(cx, scope, superObject, property, value);
        Scriptable thisScriptable =
                asScriptableOrThrowUndefWriteError(cx, scope, thisObject, property, value);

        return setSuperProp(superScriptable, thisScriptable, property, value, cx);
    }

    public static Object setSuperProp(
            Scriptable superScriptable,
            Scriptable thisScriptable,
            String property,
            Object value,
            Context cx) {
        ScriptableObject.putSuperProperty(superScriptable, thisScriptable, property, value);
        return value;
    }

    /**
     * A cheaper and less general version of the above for well-known argument types.
     *
     * @deprecated Use {@link #setObjectIndex(Object, double, Object, Context, Scriptable)} instead
     */
    @Deprecated
    public static Object setObjectIndex(Object obj, double dblIndex, Object value, Context cx) {
        return setObjectIndex(obj, dblIndex, value, cx, getTopCallScope(cx));
    }

    /** A cheaper and less general version of the above for well-known argument types. */
    public static Object setObjectIndex(
            Object obj, double dblIndex, Object value, Context cx, Scriptable scope) {
        Scriptable sobj = asScriptableOrThrowUndefWriteError(cx, scope, obj, dblIndex, value);
        int index = (int) dblIndex;
        if (index == dblIndex && index >= 0) {
            return setObjectIndex(sobj, index, value, cx);
        }
        String s = toString(dblIndex);
        return setObjectProp(sobj, s, value, cx);
    }

    public static Object setObjectIndex(Scriptable obj, int index, Object value, Context cx) {
        ScriptableObject.putProperty(obj, index, value);
        return value;
    }

    /** A cheaper and less general version of the above for well-known argument types. */
    public static Object setSuperIndex(
            Object superObject,
            double dblIndex,
            Object value,
            Context cx,
            Scriptable scope,
            Object thisObject) {
        Scriptable superScriptable =
                asScriptableOrThrowUndefWriteError(cx, scope, superObject, dblIndex, value);
        Scriptable thisScriptable =
                asScriptableOrThrowUndefWriteError(cx, scope, thisObject, dblIndex, value);

        int index = (int) dblIndex;
        if (index == dblIndex && index >= 0) {
            return setSuperIndex(superScriptable, thisScriptable, index, value, cx);
        }
        String s = toString(dblIndex);
        return setSuperProp(superScriptable, thisScriptable, s, value, cx);
    }

    public static Object setSuperIndex(
            Scriptable superScriptable,
            Scriptable thisScriptable,
            int index,
            Object value,
            Context cx) {
        ScriptableObject.putSuperProperty(superScriptable, thisScriptable, index, value);
        return value;
    }

    public static boolean deleteObjectElem(Scriptable target, Object elem, Context cx) {
        if (isSymbol(elem)) {
            SymbolScriptable so = ScriptableObject.ensureSymbolScriptable(target);
            Symbol s = (Symbol) elem;
            so.delete(s);
            return !so.has(s, target);
        }
        StringIdOrIndex s = toStringIdOrIndex(elem);
        if (s.stringId == null) {
            target.delete(s.index);
            return !target.has(s.index, target);
        }
        target.delete(s.stringId);
        return !target.has(s.stringId, target);
    }

    public static boolean hasObjectElem(Scriptable target, Object elem, Context cx) {
        boolean result;

        if (isSymbol(elem)) {
            result = ScriptableObject.hasProperty(target, (Symbol) elem);
        } else {
            StringIdOrIndex s = toStringIdOrIndex(elem);
            if (s.stringId == null) {
                result = ScriptableObject.hasProperty(target, s.index);
            } else {
                result = ScriptableObject.hasProperty(target, s.stringId);
            }
        }

        return result;
    }

    public static Object refGet(Ref ref, Context cx) {
        return ref.get(cx);
    }

    /**
     * @deprecated Use {@link #refSet(Ref, Object, Context, Scriptable)} instead
     */
    @Deprecated
    public static Object refSet(Ref ref, Object value, Context cx) {
        return refSet(ref, value, cx, getTopCallScope(cx));
    }

    public static Object refSet(Ref ref, Object value, Context cx, Scriptable scope) {
        return ref.set(cx, scope, value);
    }

    public static Object refDel(Ref ref, Context cx) {
        return wrapBoolean(ref.delete(cx));
    }

    static boolean isSpecialProperty(String s) {
        return s.equals("__proto__") || s.equals("__parent__");
    }

    /**
     * @deprecated Use {@link #specialRef(Object, String, Context, Scriptable)} instead
     */
    @Deprecated
    public static Ref specialRef(Object obj, String specialProperty, Context cx) {
        return specialRef(obj, specialProperty, cx, getTopCallScope(cx));
    }

    public static Ref specialRef(Object obj, String specialProperty, Context cx, Scriptable scope) {
        return SpecialRef.createSpecial(cx, scope, obj, specialProperty);
    }

    /**
     * @deprecated Use {@link #delete(Object, Object, Context, Scriptable, boolean)} instead
     */
    @Deprecated
    public static Object delete(Object obj, Object id, Context cx) {
        return delete(obj, id, cx, false);
    }

    /**
     * The delete operator
     *
     * <p>See ECMA 11.4.1
     *
     * <p>In ECMA 0.19, the description of the delete operator (11.4.1) assumes that the [[Delete]]
     * method returns a value. However, the definition of the [[Delete]] operator (8.6.2.5) does not
     * define a return value. Here we assume that the [[Delete]] method doesn't return a value.
     *
     * @deprecated Use {@link #delete(Object, Object, Context, Scriptable, boolean)} instead
     */
    @Deprecated
    public static Object delete(Object obj, Object id, Context cx, boolean isName) {
        return delete(obj, id, cx, getTopCallScope(cx), isName);
    }

    /**
     * The delete operator
     *
     * <p>See ECMA 11.4.1
     *
     * <p>In ECMA 0.19, the description of the delete operator (11.4.1) assumes that the [[Delete]]
     * method returns a value. However, the definition of the [[Delete]] operator (8.6.2.5) does not
     * define a return value. Here we assume that the [[Delete]] method doesn't return a value.
     */
    public static Object delete(
            Object obj, Object id, Context cx, Scriptable scope, boolean isName) {
        Scriptable sobj = toObjectOrNull(cx, obj, scope);
        if (sobj == null) {
            if (isName) {
                return Boolean.TRUE;
            }
            throw undefDeleteError(obj, id);
        }
        boolean result = deleteObjectElem(sobj, id, cx);
        return wrapBoolean(result);
    }

    /** Looks up a name in the scope chain and returns its value. */
    public static Object name(Context cx, Scriptable scope, String name) {
        Scriptable parent = scope.getParentScope();
        if (parent == null) {
            Object result = topScopeName(cx, scope, name);
            if (result == Scriptable.NOT_FOUND) {
                throw notFoundError(scope, name);
            }
            return result;
        }

        return nameOrFunction(cx, scope, parent, name, false, false);
    }

    private static Object nameOrFunction(
            Context cx,
            Scriptable scope,
            Scriptable parentScope,
            String name,
            boolean asFunctionCall,
            boolean isOptionalChainingCall) {
        Object result;
        Scriptable thisObj = scope; // It is used only if asFunctionCall==true.

        XMLObject firstXMLObject = null;
        for (; ; ) {
            if (scope instanceof NativeWith) {
                Scriptable withObj = scope.getPrototype();
                if (withObj instanceof XMLObject) {
                    XMLObject xmlObj = (XMLObject) withObj;
                    if (xmlObj.has(name, xmlObj)) {
                        // function this should be the target object of with
                        thisObj = xmlObj;
                        result = xmlObj.get(name, xmlObj);
                        break;
                    }
                    if (firstXMLObject == null) {
                        firstXMLObject = xmlObj;
                    }
                } else {
                    result = ScriptableObject.getProperty(withObj, name);
                    if (result != Scriptable.NOT_FOUND) {
                        // function this should be the target object of with
                        thisObj = withObj;
                        break;
                    }
                }
            } else if (scope instanceof NativeCall) {
                // NativeCall does not prototype chain and Scriptable.get
                // can be called directly.
                result = scope.get(name, scope);
                if (result != Scriptable.NOT_FOUND) {
                    if (asFunctionCall) {
                        // ECMA 262 requires that this for nested funtions
                        // should be top scope
                        thisObj = ScriptableObject.getTopLevelScope(parentScope);
                    }
                    break;
                }
            } else {
                // Can happen if Rhino embedding decided that nested
                // scopes are useful for what ever reasons.
                result = ScriptableObject.getProperty(scope, name);
                if (result != Scriptable.NOT_FOUND) {
                    thisObj = scope;
                    break;
                }
            }
            scope = parentScope;
            parentScope = parentScope.getParentScope();
            if (parentScope == null) {
                result = topScopeName(cx, scope, name);
                if (result == Scriptable.NOT_FOUND) {
                    if (firstXMLObject == null || asFunctionCall) {
                        throw notFoundError(scope, name);
                    }
                    // The name was not found, but we did find an XML
                    // object in the scope chain and we are looking for name,
                    // not function. The result should be an empty XMLList
                    // in name context.
                    result = firstXMLObject.get(name, firstXMLObject);
                }
                // For top scope thisObj for functions is always scope itself.
                thisObj = scope;
                break;
            }
        }

        if (asFunctionCall) {
            if (!(result instanceof Callable)) {
                if (isOptionalChainingCall
                        && (result == Scriptable.NOT_FOUND
                                || result == null
                                || Undefined.isUndefined(result))) {
                    storeScriptable(cx, null);
                    return null;
                }
                throw notFunctionError(result, name);
            }
            storeScriptable(cx, thisObj);
        }

        return result;
    }

    private static Object topScopeName(Context cx, Scriptable scope, String name) {
        if (cx.useDynamicScope) {
            scope = checkDynamicScope(cx.topCallScope, scope);
        }
        return ScriptableObject.getProperty(scope, name);
    }

    /**
     * Returns the object in the scope chain that has a given property.
     *
     * <p>The order of evaluation of an assignment expression involves evaluating the lhs to a
     * reference, evaluating the rhs, and then modifying the reference with the rhs value. This
     * method is used to 'bind' the given name to an object containing that property so that the
     * side effects of evaluating the rhs do not affect which property is modified. Typically used
     * in conjunction with setName.
     *
     * <p>See ECMA 10.1.4
     */
    public static Scriptable bind(Context cx, Scriptable scope, String id) {
        Scriptable firstXMLObject = null;
        Scriptable parent = scope.getParentScope();
        childScopesChecks:
        if (parent != null) {
            // Check for possibly nested "with" scopes first
            while (scope instanceof NativeWith) {
                Scriptable withObj = scope.getPrototype();
                if (withObj instanceof XMLObject) {
                    XMLObject xmlObject = (XMLObject) withObj;
                    if (xmlObject.has(cx, id)) {
                        return xmlObject;
                    }
                    if (firstXMLObject == null) {
                        firstXMLObject = xmlObject;
                    }
                } else {
                    if (ScriptableObject.hasProperty(withObj, id)) {
                        return withObj;
                    }
                }
                scope = parent;
                parent = parent.getParentScope();
                if (parent == null) {
                    break childScopesChecks;
                }
            }
            for (; ; ) {
                if (ScriptableObject.hasProperty(scope, id)) {
                    return scope;
                }
                scope = parent;
                parent = parent.getParentScope();
                if (parent == null) {
                    break childScopesChecks;
                }
            }
        }
        // scope here is top scope
        if (cx.useDynamicScope) {
            scope = checkDynamicScope(cx.topCallScope, scope);
        }
        if (ScriptableObject.hasProperty(scope, id)) {
            return scope;
        }
        // Nothing was found, but since XML objects always bind
        // return one if found
        return firstXMLObject;
    }

    public static Object setName(
            Scriptable bound, Object value, Context cx, Scriptable scope, String id) {
        if (bound != null) {
            // TODO: we used to special-case XMLObject here, but putProperty
            // seems to work for E4X and it's better to optimize  the common case
            ScriptableObject.putProperty(bound, id, value);
        } else {
            // "newname = 7;", where 'newname' has not yet
            // been defined, creates a new property in the
            // top scope unless strict mode is specified.
            if (cx.hasFeature(Context.FEATURE_STRICT_MODE)
                    || cx.hasFeature(Context.FEATURE_STRICT_VARS)) {
                Context.reportWarning(ScriptRuntime.getMessageById("msg.assn.create.strict", id));
            }
            // Find the top scope by walking up the scope chain.
            bound = ScriptableObject.getTopLevelScope(scope);
            if (cx.useDynamicScope) {
                bound = checkDynamicScope(cx.topCallScope, bound);
            }
            bound.put(id, bound, value);
        }
        return value;
    }

    public static Object strictSetName(
            Scriptable bound, Object value, Context cx, Scriptable scope, String id) {
        if (bound != null) {
            // TODO: The LeftHandSide also may not be a reference to a
            // data property with the attribute value {[[Writable]]:false},
            // to an accessor property with the attribute value
            // {[[Put]]:undefined}, nor to a non-existent property of an
            // object whose [[Extensible]] internal property has the value
            // false. In these cases a TypeError exception is thrown (11.13.1).
            // TODO: we used to special-case XMLObject here, but putProperty
            // seems to work for E4X and we should optimize  the common case
            ScriptableObject.putProperty(bound, id, value);
            return value;
        }
        // See ES5 8.7.2
        String msg = "Assignment to undefined \"" + id + "\" in strict mode";
        throw constructError("ReferenceError", msg);
    }

    public static Object setConst(Scriptable bound, Object value, Context cx, String id) {
        if (bound instanceof XMLObject) {
            bound.put(id, bound, value);
        } else {
            ScriptableObject.putConstProperty(bound, id, value);
        }
        return value;
    }

    /**
     * This is the enumeration needed by the for..in statement.
     *
     * <p>See ECMA 12.6.3.
     *
     * <p>IdEnumeration maintains a ObjToIntMap to make sure a given id is enumerated only once
     * across multiple objects in a prototype chain.
     *
     * <p>XXX - ECMA delete doesn't hide properties in the prototype, but js/ref does. This means
     * that the js/ref for..in can avoid maintaining a hash table and instead perform lookups to see
     * if a given property has already been enumerated.
     */
    private static class IdEnumeration implements Serializable {
        private static final long serialVersionUID = 1L;
        Scriptable obj;
        Object[] ids;
        HashSet<Object> used;
        Object currentId;
        int index;
        int enumType; /* one of ENUM_INIT_KEYS, ENUM_INIT_VALUES,
                         ENUM_INIT_ARRAY, ENUMERATE_VALUES_IN_ORDER */

        // if true, integer ids will be returned as numbers rather than strings
        boolean enumNumbers;

        Scriptable iterator;
    }

    public static Scriptable toIterator(
            Context cx, Scriptable scope, Scriptable obj, boolean keyOnly) {
        if (ScriptableObject.hasProperty(obj, NativeIterator.ITERATOR_PROPERTY_NAME)) {
            Object v = ScriptableObject.getProperty(obj, NativeIterator.ITERATOR_PROPERTY_NAME);
            if (!(v instanceof Callable)) {
                throw typeErrorById("msg.invalid.iterator");
            }
            Callable f = (Callable) v;
            Object[] args = new Object[] {keyOnly ? Boolean.TRUE : Boolean.FALSE};
            v = f.call(cx, scope, obj, args);
            if (!(v instanceof Scriptable)) {
                throw typeErrorById("msg.iterator.primitive");
            }
            return (Scriptable) v;
        }
        return null;
    }

    /**
     * For backwards compatibility with generated class files
     *
     * @deprecated Use {@link #enumInit(Object, Context, Scriptable, int)} instead
     */
    @Deprecated
    public static Object enumInit(Object value, Context cx, boolean enumValues) {
        return enumInit(value, cx, enumValues ? ENUMERATE_VALUES : ENUMERATE_KEYS);
    }

    public static final int ENUMERATE_KEYS = 0;
    public static final int ENUMERATE_VALUES = 1;
    public static final int ENUMERATE_ARRAY = 2;
    public static final int ENUMERATE_KEYS_NO_ITERATOR = 3;
    public static final int ENUMERATE_VALUES_NO_ITERATOR = 4;
    public static final int ENUMERATE_ARRAY_NO_ITERATOR = 5;
    public static final int ENUMERATE_VALUES_IN_ORDER = 6;

    /**
     * @deprecated Use {@link #enumInit(Object, Context, Scriptable, int)} instead
     */
    @Deprecated
    public static Object enumInit(Object value, Context cx, int enumType) {
        return enumInit(value, cx, getTopCallScope(cx), enumType);
    }

    public static Object enumInit(Object value, Context cx, Scriptable scope, int enumType) {
        IdEnumeration x = new IdEnumeration();
        x.obj = toObjectOrNull(cx, value, scope);
        // "for of" loop
        if (enumType == ENUMERATE_VALUES_IN_ORDER) {
            x.enumType = enumType;
            x.iterator = null;
            return enumInitInOrder(cx, x);
        }
        if (x.obj == null) {
            // null or undefined do not cause errors but rather lead to empty
            // "for in" loop
            return x;
        }
        x.enumType = enumType;
        x.iterator = null;
        if (enumType != ENUMERATE_KEYS_NO_ITERATOR
                && enumType != ENUMERATE_VALUES_NO_ITERATOR
                && enumType != ENUMERATE_ARRAY_NO_ITERATOR) {
            x.iterator =
                    toIterator(
                            cx,
                            x.obj.getParentScope(),
                            x.obj,
                            enumType == ScriptRuntime.ENUMERATE_KEYS);
        }
        if (x.iterator == null) {
            // enumInit should read all initial ids before returning
            // or "for (a.i in a)" would wrongly enumerate i in a as well
            enumChangeObject(x);
        }

        return x;
    }

    private static Object enumInitInOrder(Context cx, IdEnumeration x) {
        if (!(x.obj instanceof SymbolScriptable)
                || !ScriptableObject.hasProperty(x.obj, SymbolKey.ITERATOR)) {
            throw typeErrorById("msg.not.iterable", toString(x.obj));
        }

        Object iterator = ScriptableObject.getProperty(x.obj, SymbolKey.ITERATOR);
        if (!(iterator instanceof Callable)) {
            throw typeErrorById("msg.not.iterable", toString(x.obj));
        }
        Callable f = (Callable) iterator;
        Scriptable scope = x.obj.getParentScope();
        Object[] args = new Object[] {};
        Object v = f.call(cx, scope, x.obj, args);
        if (!(v instanceof Scriptable)) {
            throw typeErrorById("msg.not.iterable", toString(x.obj));
        }
        x.iterator = (Scriptable) v;
        return x;
    }

    public static void setEnumNumbers(Object enumObj, boolean enumNumbers) {
        ((IdEnumeration) enumObj).enumNumbers = enumNumbers;
    }

    /**
     * @deprecated since 1.7.15. Use {@link #enumNext(Object, Context)} instead
     */
    @Deprecated
    public static Boolean enumNext(Object enumObj) {
        return enumNext(enumObj, Context.getContext());
    }

    public static Boolean enumNext(Object enumObj, Context cx) {
        IdEnumeration x = (IdEnumeration) enumObj;
        if (x.iterator != null) {
            if (x.enumType == ENUMERATE_VALUES_IN_ORDER) {
                return enumNextInOrder(x, cx);
            }
            Object v = ScriptableObject.getProperty(x.iterator, "next");
            if (!(v instanceof Callable)) return Boolean.FALSE;
            Callable f = (Callable) v;
            try {
                x.currentId = f.call(cx, x.iterator.getParentScope(), x.iterator, emptyArgs);
                return Boolean.TRUE;
            } catch (JavaScriptException e) {
                if (e.getValue() instanceof NativeIterator.StopIteration) {
                    return Boolean.FALSE;
                }
                throw e;
            }
        }
        for (; ; ) {
            if (x.obj == null) {
                return Boolean.FALSE;
            }
            if (x.index == x.ids.length) {
                x.obj = x.obj.getPrototype();
                enumChangeObject(x);
                continue;
            }
            Object id = x.ids[x.index++];
            if (x.used != null && x.used.contains(id)) {
                continue;
            }
            if (id instanceof Symbol) {
                continue;
            } else if (id instanceof String) {
                String strId = (String) id;
                if (!x.obj.has(strId, x.obj)) continue; // must have been deleted
                x.currentId = strId;
            } else {
                int intId = ((Number) id).intValue();
                if (!x.obj.has(intId, x.obj)) continue; // must have been deleted
                x.currentId = x.enumNumbers ? Integer.valueOf(intId) : String.valueOf(intId);
            }
            return Boolean.TRUE;
        }
    }

    private static Boolean enumNextInOrder(IdEnumeration enumObj, Context cx) {
        Object v = ScriptableObject.getProperty(enumObj.iterator, ES6Iterator.NEXT_METHOD);
        if (!(v instanceof Callable)) {
            throw notFunctionError(enumObj.iterator, ES6Iterator.NEXT_METHOD);
        }
        Callable f = (Callable) v;
        Scriptable scope = enumObj.iterator.getParentScope();
        Object r = f.call(cx, scope, enumObj.iterator, emptyArgs);
        Scriptable iteratorResult = toObject(cx, scope, r);
        Object done = ScriptableObject.getProperty(iteratorResult, ES6Iterator.DONE_PROPERTY);
        if (done != Scriptable.NOT_FOUND && toBoolean(done)) {
            return Boolean.FALSE;
        }
        enumObj.currentId =
                ScriptableObject.getProperty(iteratorResult, ES6Iterator.VALUE_PROPERTY);
        return Boolean.TRUE;
    }

    public static Object enumId(Object enumObj, Context cx) {
        IdEnumeration x = (IdEnumeration) enumObj;
        if (x.iterator != null) {
            return x.currentId;
        }
        switch (x.enumType) {
            case ENUMERATE_KEYS:
            case ENUMERATE_KEYS_NO_ITERATOR:
                return x.currentId;
            case ENUMERATE_VALUES:
            case ENUMERATE_VALUES_NO_ITERATOR:
                return enumValue(enumObj, cx);
            case ENUMERATE_ARRAY:
            case ENUMERATE_ARRAY_NO_ITERATOR:
                Object[] elements = {x.currentId, enumValue(enumObj, cx)};
                return cx.newArray(ScriptableObject.getTopLevelScope(x.obj), elements);
            default:
                throw Kit.codeBug();
        }
    }

    public static Object enumValue(Object enumObj, Context cx) {
        IdEnumeration x = (IdEnumeration) enumObj;

        Object result;

        if (isSymbol(x.currentId)) {
            SymbolScriptable so = ScriptableObject.ensureSymbolScriptable(x.obj);
            result = so.get((Symbol) x.currentId, x.obj);
        } else {
            StringIdOrIndex s = toStringIdOrIndex(x.currentId);
            if (s.stringId == null) {
                result = x.obj.get(s.index, x.obj);
            } else {
                result = x.obj.get(s.stringId, x.obj);
            }
        }

        return result;
    }

    private static void enumChangeObject(IdEnumeration x) {
        Object[] ids = null;
        while (x.obj != null) {
            ids = x.obj.getIds();
            if (ids.length != 0) {
                break;
            }
            x.obj = x.obj.getPrototype();
        }
        if (x.obj != null && x.ids != null) {
            Object[] previous = x.ids;
            int L = previous.length;
            if (x.used == null) {
                x.used = new HashSet<>();
            }
            for (int i = 0; i != L; ++i) {
                x.used.add(previous[i]);
            }
        }
        x.ids = ids;
        x.index = 0;
    }

    /**
     * This is used to handle all the special cases that are required when invoking
     * Object.fromEntries or constructing a NativeMap or NativeWeakMap from an iterable.
     *
     * @param cx the current context
     * @param scope the current scope
     * @param arg1 the iterable object.
     * @param setter the setter to set the value
     * @return true, if arg1 was iterable.
     */
    public static boolean loadFromIterable(
            Context cx, Scriptable scope, Object arg1, BiConsumer<Object, Object> setter) {
        if ((arg1 == null) || Undefined.isUndefined(arg1)) return false;

        // Call the "[Symbol.iterator]" property as a function.
        final Object ito = ScriptRuntime.callIterator(arg1, cx, scope);
        if (Undefined.isUndefined(ito)) {
            // Per spec, ignore if the iterator is undefined
            return false;
        }

        // Finally, run through all the iterated values and add them!
        try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, ito)) {
            for (Object val : it) {
                Scriptable sVal = ScriptableObject.ensureScriptable(val);
                if (sVal instanceof Symbol) {
                    throw ScriptRuntime.typeErrorById(
                            "msg.arg.not.object", ScriptRuntime.typeof(sVal));
                }
                Object finalKey = sVal.get(0, sVal);
                if (finalKey == Scriptable.NOT_FOUND) {
                    finalKey = Undefined.instance;
                }
                Object finalVal = sVal.get(1, sVal);
                if (finalVal == Scriptable.NOT_FOUND) {
                    finalVal = Undefined.instance;
                }
                setter.accept(finalKey, finalVal);
            }
        }
        return true;
    }

    /**
     * Prepare for calling name(...): return function corresponding to name and make current top
     * scope available as ScriptRuntime.lastStoredScriptable() for consumption as thisObj. The
     * caller must call ScriptRuntime.lastStoredScriptable() immediately after calling this method.
     */
    public static Callable getNameFunctionAndThis(String name, Context cx, Scriptable scope) {
        return getNameFunctionAndThisInner(name, cx, scope, false);
    }

    public static Callable getNameFunctionAndThisOptional(
            String name, Context cx, Scriptable scope) {
        return getNameFunctionAndThisInner(name, cx, scope, true);
    }

    private static Callable getNameFunctionAndThisInner(
            String name, Context cx, Scriptable scope, boolean isOptionalChainingCall) {
        Scriptable parent = scope.getParentScope();
        if (parent == null) {
            Object result = topScopeName(cx, scope, name);
            if (!(result instanceof Callable)) {
                if (isOptionalChainingCall
                        && (result == Scriptable.NOT_FOUND
                                || result == null
                                || Undefined.isUndefined(result))) {
                    storeScriptable(cx, null);
                    return null;
                }

                if (result == Scriptable.NOT_FOUND) {
                    throw notFoundError(scope, name);
                }
                throw notFunctionError(result, name);
            }
            // Top scope is not NativeWith or NativeCall => thisObj == scope
            storeScriptable(cx, scope);
            return (Callable) result;
        }

        // name will call storeScriptable(cx, thisObj);
        return (Callable) nameOrFunction(cx, scope, parent, name, true, isOptionalChainingCall);
    }

    /**
     * Prepare for calling obj[id](...): return function corresponding to obj[id] and make obj
     * properly converted to Scriptable available as ScriptRuntime.lastStoredScriptable() for
     * consumption as thisObj. The caller must call ScriptRuntime.lastStoredScriptable() immediately
     * after calling this method.
     *
     * @deprecated Use {@link #getElemFunctionAndThis(Object, Object, Context, Scriptable)} instead
     */
    @Deprecated
    public static Callable getElemFunctionAndThis(Object obj, Object elem, Context cx) {
        return getElemFunctionAndThis(obj, elem, cx, getTopCallScope(cx));
    }

    /**
     * Prepare for calling obj[id](...): return function corresponding to obj[id] and make obj
     * properly converted to Scriptable available as ScriptRuntime.lastStoredScriptable() for
     * consumption as thisObj. The caller must call ScriptRuntime.lastStoredScriptable() immediately
     * after calling this method.
     */
    public static Callable getElemFunctionAndThis(
            Object obj, Object elem, Context cx, Scriptable scope) {
        return getElemFunctionAndThisInner(obj, elem, cx, scope, false);
    }

    public static Callable getElemFunctionAndThisOptional(
            Object obj, Object elem, Context cx, Scriptable scope) {
        return getElemFunctionAndThisInner(obj, elem, cx, scope, true);
    }

    private static Callable getElemFunctionAndThisInner(
            Object obj, Object elem, Context cx, Scriptable scope, boolean isOptionalChainingCall) {
        Scriptable thisObj;
        Object value;

        if (isSymbol(elem)) {
            thisObj = toObjectOrNull(cx, obj, scope);
            if (thisObj == null) {
                throw undefCallError(obj, String.valueOf(elem));
            }
            value = ScriptableObject.getProperty(thisObj, (Symbol) elem);

        } else {
            StringIdOrIndex s = toStringIdOrIndex(elem);
            if (s.stringId != null) {
                return getPropFunctionAndThis(obj, s.stringId, cx, scope);
            }

            thisObj = toObjectOrNull(cx, obj, scope);
            if (thisObj == null) {
                throw undefCallError(obj, String.valueOf(elem));
            }

            value = ScriptableObject.getProperty(thisObj, s.index);
        }

        if (!(value instanceof Callable)) {
            if (isOptionalChainingCall
                    && (value == Scriptable.NOT_FOUND
                            || value == null
                            || Undefined.isUndefined(value))) {
                storeScriptable(cx, null);
                return null;
            }
            throw notFunctionError(value, elem);
        }

        storeScriptable(cx, thisObj);
        return (Callable) value;
    }

    /**
     * Prepare for calling obj.property(...): return function corresponding to obj.property and make
     * obj properly converted to Scriptable available as ScriptRuntime.lastStoredScriptable() for
     * consumption as thisObj. The caller must call ScriptRuntime.lastStoredScriptable() immediately
     * after calling this method. Warning: this doesn't allow to resolve primitive prototype
     * properly when many top scopes are involved.
     *
     * @deprecated Use {@link #getPropFunctionAndThis(Object, String, Context, Scriptable)} instead
     */
    @Deprecated
    public static Callable getPropFunctionAndThis(Object obj, String property, Context cx) {
        return getPropFunctionAndThis(obj, property, cx, getTopCallScope(cx));
    }

    /**
     * Prepare for calling obj.property(...): return function corresponding to obj.property and make
     * obj properly converted to Scriptable available as ScriptRuntime.lastStoredScriptable() for
     * consumption as thisObj. The caller must call ScriptRuntime.lastStoredScriptable() immediately
     * after calling this method.
     */
    public static Callable getPropFunctionAndThis(
            Object obj, String property, Context cx, Scriptable scope) {
        return getPropFunctionAndThisInner(obj, property, cx, scope, false);
    }

    public static Callable getPropFunctionAndThisOptional(
            Object obj, String property, Context cx, Scriptable scope) {
        return getPropFunctionAndThisInner(obj, property, cx, scope, true);
    }

    private static Callable getPropFunctionAndThisInner(
            Object obj,
            String property,
            Context cx,
            Scriptable scope,
            boolean isOptionalChainingCall) {
        Scriptable thisObj = toObjectOrNull(cx, obj, scope);
        return getPropFunctionAndThisHelper(obj, property, cx, thisObj, isOptionalChainingCall);
    }

    private static Callable getPropFunctionAndThisHelper(
            Object obj,
            String property,
            Context cx,
            Scriptable thisObj,
            boolean isOptionalChainingCall) {
        if (thisObj == null) {
            if (isOptionalChainingCall) {
                storeScriptable(cx, null);
                return null;
            }
            throw undefCallError(obj, property);
        }

        Object value = ScriptableObject.getProperty(thisObj, property);
        if (!(value instanceof Callable)) {
            Object noSuchMethod = ScriptableObject.getProperty(thisObj, "__noSuchMethod__");
            if (noSuchMethod instanceof Callable)
                value = new NoSuchMethodShim((Callable) noSuchMethod, property);
        }

        if (!(value instanceof Callable)) {
            if (isOptionalChainingCall
                    && (value == Scriptable.NOT_FOUND
                            || value == null
                            || Undefined.isUndefined(value))) {
                storeScriptable(cx, null);
                return null;
            }
            throw notFunctionError(thisObj, value, property);
        }

        storeScriptable(cx, thisObj);
        return (Callable) value;
    }

    /**
     * Prepare for calling &lt;expression&gt;(...): return function corresponding to
     * &lt;expression&gt; and make parent scope of the function available as
     * ScriptRuntime.lastStoredScriptable() for consumption as thisObj. The caller must call
     * ScriptRuntime.lastStoredScriptable() immediately after calling this method.
     */
    public static Callable getValueFunctionAndThis(Object value, Context cx) {
        return getValueFunctionAndThisInner(value, cx, false);
    }

    public static Callable getValueFunctionAndThisOptional(Object value, Context cx) {
        return getValueFunctionAndThisInner(value, cx, true);
    }

    private static Callable getValueFunctionAndThisInner(
            Object value, Context cx, boolean isOptionalChainingCall) {
        if (!(value instanceof Callable)) {
            if (isOptionalChainingCall
                    && (value == Scriptable.NOT_FOUND
                            || value == null
                            || Undefined.isUndefined(value))) {
                storeScriptable(cx, null);
                return null;
            }
            throw notFunctionError(value);
        }

        Callable f = (Callable) value;
        Scriptable thisObj = null;
        if (f instanceof Scriptable) {
            thisObj = ((Scriptable) f).getParentScope();
        }
        if (thisObj == null) {
            if (cx.topCallScope == null) throw new IllegalStateException();
            thisObj = cx.topCallScope;
        }
        if (thisObj.getParentScope() != null) {
            if (thisObj instanceof NativeWith) {
                // functions defined inside with should have with target
                // as their thisObj
            } else if (thisObj instanceof NativeCall) {
                // nested functions should have top scope as their thisObj
                thisObj = ScriptableObject.getTopLevelScope(thisObj);
            }
        }
        storeScriptable(cx, thisObj);
        return f;
    }

    /**
     * Given an object, get the "Symbol.iterator" element, throw a TypeError if it is not present,
     * then call the result, (throwing a TypeError if the result is not a function), and return that
     * result, whatever it is.
     */
    public static Object callIterator(Object obj, Context cx, Scriptable scope) {
        final Callable getIterator =
                ScriptRuntime.getElemFunctionAndThis(obj, SymbolKey.ITERATOR, cx, scope);
        final Scriptable iterable = ScriptRuntime.lastStoredScriptable(cx);
        return getIterator.call(cx, scope, iterable, ScriptRuntime.emptyArgs);
    }

    /**
     * Given an iterator result, return true if and only if there is a "done" property that's true.
     */
    public static boolean isIteratorDone(Context cx, Object result) {
        if (!(result instanceof Scriptable)) {
            return false;
        }
        final Object prop = getObjectProp((Scriptable) result, ES6Iterator.DONE_PROPERTY, cx);
        return toBoolean(prop);
    }

    /**
     * Perform function call in reference context. Should always return value that can be passed to
     * {@link #refGet(Ref, Context)} or {@link #refSet(Ref, Object, Context)} arbitrary number of
     * times. The args array reference should not be stored in any object that is can be
     * GC-reachable after this method returns. If this is necessary, store args.clone(), not args
     * array itself.
     */
    public static Ref callRef(Callable function, Scriptable thisObj, Object[] args, Context cx) {
        if (function instanceof RefCallable) {
            RefCallable rfunction = (RefCallable) function;
            Ref ref = rfunction.refCall(cx, thisObj, args);
            if (ref == null) {
                throw new IllegalStateException(
                        rfunction.getClass().getName() + ".refCall() returned null");
            }
            return ref;
        }
        // No runtime support for now
        String msg = getMessageById("msg.no.ref.from.function", toString(function));
        throw constructError("ReferenceError", msg);
    }

    /**
     * Operator new.
     *
     * <p>See ECMA 11.2.2
     */
    public static Scriptable newObject(Object ctor, Context cx, Scriptable scope, Object[] args) {
        if (!(ctor instanceof Constructable)) {
            throw notFunctionError(ctor);
        }
        return ((Constructable) ctor).construct(cx, scope, args);
    }

    public static Object callSpecial(
            Context cx,
            Callable fun,
            Scriptable thisObj,
            Object[] args,
            Scriptable scope,
            Scriptable callerThis,
            int callType,
            String filename,
            int lineNumber,
            boolean isOptionalChainingCall) {
        if (fun == null && isOptionalChainingCall) {
            return Undefined.instance;
        }

        if (callType == Node.SPECIALCALL_EVAL) {
            if (thisObj.getParentScope() == null && NativeGlobal.isEvalFunction(fun)) {
                return evalSpecial(cx, scope, callerThis, args, filename, lineNumber);
            }
        } else if (callType == Node.SPECIALCALL_WITH) {
            if (NativeWith.isWithFunction(fun)) {
                throw Context.reportRuntimeErrorById("msg.only.from.new", "With");
            }
        } else {
            throw Kit.codeBug();
        }

        return fun.call(cx, scope, thisObj, args);
    }

    public static Object newSpecial(
            Context cx, Object fun, Object[] args, Scriptable scope, int callType) {
        if (callType == Node.SPECIALCALL_EVAL) {
            if (NativeGlobal.isEvalFunction(fun)) {
                throw typeErrorById("msg.not.ctor", "eval");
            }
        } else if (callType == Node.SPECIALCALL_WITH) {
            if (NativeWith.isWithFunction(fun)) {
                return NativeWith.newWithSpecial(cx, scope, args);
            }
        } else {
            throw Kit.codeBug();
        }

        return newObject(fun, cx, scope, args);
    }

    /**
     * Function.prototype.apply and Function.prototype.call
     *
     * <p>See Ecma 15.3.4.[34]
     */
    public static Object applyOrCall(
            boolean isApply, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        int L = args.length;
        Callable function = getCallable(thisObj);

        Scriptable callThis = getApplyOrCallThis(cx, scope, L == 0 ? null : args[0], L, function);

        Object[] callArgs;
        if (isApply) {
            // Follow Ecma 15.3.4.3
            callArgs = L <= 1 ? ScriptRuntime.emptyArgs : getApplyArguments(cx, args[1]);
        } else {
            // Follow Ecma 15.3.4.4
            if (L <= 1) {
                callArgs = ScriptRuntime.emptyArgs;
            } else {
                callArgs = new Object[L - 1];
                System.arraycopy(args, 1, callArgs, 0, L - 1);
            }
        }

        return function.call(cx, scope, callThis, callArgs);
    }

    static Scriptable getApplyOrCallThis(
            Context cx, Scriptable scope, Object arg0, int l, Callable target) {
        Scriptable callThis;
        if (cx.hasFeature(Context.FEATURE_OLD_UNDEF_NULL_THIS)) {
            // Legacy behavior
            if (l != 0) {
                callThis = toObjectOrNull(cx, arg0, scope);
            } else {
                callThis = null;
            }
            if (callThis == null) {
                // This covers the case of args[0] == (null|undefined) as well.
                callThis = getTopCallScope(cx);
            }
        } else {
            // Spec-compliant behavior
            if (l != 0) {
                callThis =
                        arg0 == Undefined.instance
                                ? Undefined.SCRIPTABLE_UNDEFINED
                                : toObjectOrNull(cx, arg0, scope);
            } else {
                callThis = Undefined.SCRIPTABLE_UNDEFINED;
            }

            // Replace missing this with global object only for non-strict functions
            boolean missingCallThis =
                    callThis == null || callThis == Undefined.SCRIPTABLE_UNDEFINED;
            boolean isFunctionStrict =
                    !(target instanceof NativeFunction) || ((NativeFunction) target).isStrict();
            if (missingCallThis && !isFunctionStrict) {
                callThis = getTopCallScope(cx);
            }
        }

        return callThis;
    }

    /**
     * @return true if the passed in Scriptable looks like an array
     */
    static boolean isArrayLike(Scriptable obj) {
        return obj != null
                && (obj instanceof NativeArray
                        || obj instanceof Arguments
                        || ScriptableObject.hasProperty(obj, "length"));
    }

    static Object[] getApplyArguments(Context cx, Object arg1) {
        if (arg1 == null || Undefined.isUndefined(arg1)) {
            return ScriptRuntime.emptyArgs;
        } else if (arg1 instanceof Scriptable && isArrayLike((Scriptable) arg1)) {
            return cx.getElements((Scriptable) arg1);
        } else if (arg1 instanceof ScriptableObject) {
            return ScriptRuntime.emptyArgs;
        } else {
            throw ScriptRuntime.typeErrorById("msg.arg.isnt.array");
        }
    }

    static Callable getCallable(Scriptable thisObj) {
        Callable function;
        if (thisObj instanceof Callable) {
            function = (Callable) thisObj;
        } else if (thisObj == null) {
            throw ScriptRuntime.notFunctionError(null, null);
        } else {
            Object value = thisObj.getDefaultValue(ScriptRuntime.FunctionClass);
            if (!(value instanceof Callable)) {
                throw ScriptRuntime.notFunctionError(value, thisObj);
            }
            function = (Callable) value;
        }
        return function;
    }

    /**
     * The eval function property of the global object.
     *
     * <p>See ECMA 15.1.2.1
     */
    public static Object evalSpecial(
            Context cx,
            Scriptable scope,
            Object thisArg,
            Object[] args,
            String filename,
            int lineNumber) {
        if (args.length < 1) return Undefined.instance;
        Object x = args[0];
        if (!(x instanceof CharSequence)) {
            if (cx.hasFeature(Context.FEATURE_STRICT_MODE)
                    || cx.hasFeature(Context.FEATURE_STRICT_EVAL)) {
                throw Context.reportRuntimeErrorById("msg.eval.nonstring.strict");
            }
            String message = ScriptRuntime.getMessageById("msg.eval.nonstring");
            Context.reportWarning(message);
            return x;
        }
        if (filename == null) {
            int[] linep = new int[1];
            filename = Context.getSourcePositionFromStack(linep);
            if (filename != null) {
                lineNumber = linep[0];
            } else {
                filename = "";
            }
        }
        String sourceName = ScriptRuntime.makeUrlForGeneratedScript(true, filename, lineNumber);

        ErrorReporter reporter;
        reporter = DefaultErrorReporter.forEval(cx.getErrorReporter());

        Evaluator evaluator = Context.createInterpreter();
        if (evaluator == null) {
            throw new JavaScriptException("Interpreter not present", filename, lineNumber);
        }

        // Compile with explicit interpreter instance to force interpreter
        // mode.
        Consumer<CompilerEnvirons> compilerEnvironsProcessor =
                compilerEnvs -> {
                    // If we are inside a method, we need to allow super. Methods have the home
                    // object set and propagated via the activation (i.e. the NativeCall),
                    // but non-methods will have the home object set to null.
                    boolean isInsideMethod =
                            scope instanceof NativeCall
                                    && ((NativeCall) scope).getHomeObject() != null;
                    compilerEnvs.setAllowSuper(isInsideMethod);
                };
        Script script =
                cx.compileString(
                        x.toString(),
                        evaluator,
                        reporter,
                        sourceName,
                        1,
                        null,
                        compilerEnvironsProcessor);
        evaluator.setEvalScriptFlag(script);
        Callable c = (Callable) script;
        Scriptable thisObject =
                thisArg == Undefined.instance
                        ? Undefined.SCRIPTABLE_UNDEFINED
                        : (Scriptable) thisArg;
        return c.call(cx, scope, thisObject, ScriptRuntime.emptyArgs);
    }

    /** The typeof operator */
    public static String typeof(Object value) {
        if (value == null) return "object";
        if (value == Undefined.instance) return "undefined";
        if (value instanceof Delegator) return typeof(((Delegator) value).getDelegee());
        if (value instanceof ScriptableObject) return ((ScriptableObject) value).getTypeOf();
        if (value instanceof Scriptable) return (value instanceof Callable) ? "function" : "object";
        if (value instanceof CharSequence) return "string";
        if (value instanceof BigInteger) return "bigint";
        if (value instanceof Number) return "number";
        if (value instanceof Boolean) return "boolean";
        if (isSymbol(value)) return "symbol";
        throw errorWithClassName("msg.invalid.type", value);
    }

    /** The typeof operator that correctly handles the undefined case */
    public static String typeofName(Scriptable scope, String id) {
        Context cx = Context.getContext();
        Scriptable val = bind(cx, scope, id);
        if (val == null) return "undefined";
        return typeof(getObjectProp(val, id, cx));
    }

    public static boolean isObject(Object value) {
        if (value == null) {
            return false;
        }
        if (Undefined.isUndefined(value)) {
            return false;
        }
        if (value instanceof ScriptableObject) {
            String type = ((ScriptableObject) value).getTypeOf();
            return "object".equals(type) || "function".equals(type);
        }
        if (value instanceof Scriptable) {
            return !(value instanceof Callable);
        }
        return false;
    }

    // neg:
    // implement the '-' operator inline in the caller
    // as "-toNumber(val)"

    // not:
    // implement the '!' operator inline in the caller
    // as "!toBoolean(val)"

    // bitnot:
    // implement the '~' operator inline in the caller
    // as "~toInt32(val)"

    public static Object add(Object lval, Object rval, Context cx) {
        // if lval and rval are primitive numerics of the same type, give them priority
        if (lval instanceof Integer && rval instanceof Integer) {
            return add((Integer) lval, (Integer) rval);
        }
        if (lval instanceof BigInteger && rval instanceof BigInteger) {
            return ((BigInteger) lval).add((BigInteger) rval);
        }
        if (lval instanceof Number
                && !(lval instanceof BigInteger)
                && rval instanceof Number
                && !(rval instanceof BigInteger)) {
            return wrapNumber(((Number) lval).doubleValue() + ((Number) rval).doubleValue());
        }

        // e4x extension start
        if (lval instanceof XMLObject) {
            Object test = ((XMLObject) lval).addValues(cx, true, rval);
            if (test != Scriptable.NOT_FOUND) {
                return test;
            }
        }
        if (rval instanceof XMLObject) {
            Object test = ((XMLObject) rval).addValues(cx, false, lval);
            if (test != Scriptable.NOT_FOUND) {
                return test;
            }
        }
        // e4x extension end

        // spec starts here for abstract operation ApplyStringOrNumericBinaryOperator
        // where opText is "+".
        final Object lprim = toPrimitive(lval);
        final Object rprim = toPrimitive(rval);
        if (lprim instanceof CharSequence || rprim instanceof CharSequence) {
            final CharSequence lstr =
                    (lprim instanceof CharSequence) ? (CharSequence) lprim : toString(lprim);
            final CharSequence rstr =
                    (rprim instanceof CharSequence) ? (CharSequence) rprim : toString(rprim);
            return new ConsString(lstr, rstr);
        }

        // Skipping (lval = lprim, rval = rprim) and using xprim values directly.
        final Number lnum = toNumeric(lprim);
        final Number rnum = toNumeric(rprim);
        if (lnum instanceof BigInteger && rnum instanceof BigInteger) {
            return ((BigInteger) lnum).add((BigInteger) rnum);
        }
        if (lnum instanceof BigInteger || rnum instanceof BigInteger) {
            throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
        }
        return lnum.doubleValue() + rnum.doubleValue();
    }

    /**
     * https://262.ecma-international.org/11.0/#sec-addition-operator-plus 5. Let lprim be ?
     * ToPrimitive(lval). 7. If Type(lprim) is String or Type(rprim) is String, then a. Let lstr be
     * ? ToString(lprim).
     *
     * <p>Should call toPrimitive before toCharSequence
     *
     * @deprecated Use {@link #add(Object, Object, Context)} instead
     */
    @Deprecated
    public static CharSequence add(CharSequence val1, Object val2) {
        return new ConsString(val1, toCharSequence(val2));
    }

    /**
     * https://262.ecma-international.org/11.0/#sec-addition-operator-plus 6. Let rprim be ?
     * ToPrimitive(rval). 7. If Type(lprim) is String or Type(rprim) is String, then b. Let rstr be
     * ? ToString(rprim).
     *
     * <p>Should call toPrimitive before toCharSequence
     *
     * @deprecated Use {@link #add(Object, Object, Context)} instead
     */
    @Deprecated
    public static CharSequence add(Object val1, CharSequence val2) {
        return new ConsString(toCharSequence(val1), val2);
    }

    public static Number subtract(Number val1, Number val2) {
        if (val1 instanceof BigInteger && val2 instanceof BigInteger) {
            return ((BigInteger) val1).subtract((BigInteger) val2);
        } else if (val1 instanceof BigInteger || val2 instanceof BigInteger) {
            throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
        } else if (val1 instanceof Integer && val2 instanceof Integer) {
            return subtract((Integer) val1, (Integer) val2);
        } else {
            return val1.doubleValue() - val2.doubleValue();
        }
    }

    public static Number multiply(Number val1, Number val2) {
        if (val1 instanceof BigInteger && val2 instanceof BigInteger) {
            return ((BigInteger) val1).multiply((BigInteger) val2);
        } else if (val1 instanceof BigInteger || val2 instanceof BigInteger) {
            throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
        } else if (val1 instanceof Integer && val2 instanceof Integer) {
            return multiply((Integer) val1, (Integer) val2);
        } else {
            return val1.doubleValue() * val2.doubleValue();
        }
    }

    public static Number divide(Number val1, Number val2) {
        if (val1 instanceof BigInteger && val2 instanceof BigInteger) {
            if (val2.equals(BigInteger.ZERO)) {
                throw ScriptRuntime.rangeErrorById("msg.division.zero");
            }
            return ((BigInteger) val1).divide((BigInteger) val2);
        } else if (val1 instanceof BigInteger || val2 instanceof BigInteger) {
            throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
        } else {
            // Do not try to optimize for the integer case because JS doesn't
            // have an integer type.
            return val1.doubleValue() / val2.doubleValue();
        }
    }

    public static Number remainder(Number val1, Number val2) {
        if (val1 instanceof BigInteger && val2 instanceof BigInteger) {
            if (val2.equals(BigInteger.ZERO)) {
                throw ScriptRuntime.rangeErrorById("msg.division.zero");
            }
            return ((BigInteger) val1).remainder((BigInteger) val2);
        } else if (val1 instanceof BigInteger || val2 instanceof BigInteger) {
            throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
        } else {
            // Do not try an integer-specific optimization because we need to get
            // both +0 and -0 right.
            return val1.doubleValue() % val2.doubleValue();
        }
    }

    // Integer-optimized methods.

    public static Object add(Integer i1, Integer i2) {
        // Do 64-bit addition to account for overflow
        long r = i1.longValue() + i2.longValue();
        if ((r >= Integer.MIN_VALUE) && (r <= Integer.MAX_VALUE)) {
            return (int) r;
        }
        return (double) r;
    }

    public static Number subtract(Integer i1, Integer i2) {
        // Account for overflow
        long r = i1.longValue() - i2.longValue();
        if ((r >= Integer.MIN_VALUE) && (r <= Integer.MAX_VALUE)) {
            return (int) r;
        }
        return (double) r;
    }

    public static Number multiply(Integer i1, Integer i2) {
        // Account for overflow
        long r = i1.longValue() * i2.longValue();
        if ((r >= Integer.MIN_VALUE) && (r <= Integer.MAX_VALUE)) {
            return (int) r;
        }
        return (double) r;
    }

    @SuppressWarnings("AndroidJdkLibsChecker")
    public static Number exponentiate(Number val1, Number val2) {
        if (val1 instanceof BigInteger && val2 instanceof BigInteger) {
            if (((BigInteger) val2).signum() == -1) {
                throw ScriptRuntime.rangeErrorById("msg.bigint.negative.exponent");
            }

            try {
                int intVal2 = ((BigInteger) val2).intValueExact();
                return ((BigInteger) val1).pow(intVal2);
            } catch (ArithmeticException e) {
                // This is outside the scope of the ECMA262 specification.
                throw ScriptRuntime.rangeErrorById("msg.bigint.out.of.range.arithmetic");
            }
        } else if (val1 instanceof BigInteger || val2 instanceof BigInteger) {
            throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
        } else {
            return Math.pow(val1.doubleValue(), val2.doubleValue());
        }
    }

    public static Number bitwiseAND(Number val1, Number val2) {
        if (val1 instanceof BigInteger && val2 instanceof BigInteger) {
            return ((BigInteger) val1).and((BigInteger) val2);
        } else if (val1 instanceof BigInteger || val2 instanceof BigInteger) {
            throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
        } else if (val1 instanceof Integer && val2 instanceof Integer) {
            return Integer.valueOf(((Integer) val1).intValue() & ((Integer) val2).intValue());
        } else {
            int result = toInt32(val1.doubleValue()) & toInt32(val2.doubleValue());
            return Double.valueOf(result);
        }
    }

    public static Number bitwiseOR(Number val1, Number val2) {
        if (val1 instanceof BigInteger && val2 instanceof BigInteger) {
            return ((BigInteger) val1).or((BigInteger) val2);
        } else if (val1 instanceof BigInteger || val2 instanceof BigInteger) {
            throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
        } else if (val1 instanceof Integer && val2 instanceof Integer) {
            return Integer.valueOf(((Integer) val1).intValue() | ((Integer) val2).intValue());
        } else {
            int result = toInt32(val1.doubleValue()) | toInt32(val2.doubleValue());
            return Double.valueOf(result);
        }
    }

    public static Number bitwiseXOR(Number val1, Number val2) {
        if (val1 instanceof BigInteger && val2 instanceof BigInteger) {
            return ((BigInteger) val1).xor((BigInteger) val2);
        } else if (val1 instanceof BigInteger || val2 instanceof BigInteger) {
            throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
        } else if (val1 instanceof Integer && val2 instanceof Integer) {
            return Integer.valueOf(((Integer) val1).intValue() ^ ((Integer) val2).intValue());
        } else {
            int result = toInt32(val1.doubleValue()) ^ toInt32(val2.doubleValue());
            return Double.valueOf(result);
        }
    }

    @SuppressWarnings("AndroidJdkLibsChecker")
    public static Number leftShift(Number val1, Number val2) {
        if (val1 instanceof BigInteger && val2 instanceof BigInteger) {
            try {
                int intVal2 = ((BigInteger) val2).intValueExact();
                return ((BigInteger) val1).shiftLeft(intVal2);
            } catch (ArithmeticException e) {
                // This is outside the scope of the ECMA262 specification.
                throw ScriptRuntime.rangeErrorById("msg.bigint.out.of.range.arithmetic");
            }
        } else if (val1 instanceof BigInteger || val2 instanceof BigInteger) {
            throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
        } else if (val1 instanceof Integer && val2 instanceof Integer) {
            return Integer.valueOf(((Integer) val1).intValue() << ((Integer) val2).intValue());
        } else {
            int result = toInt32(val1.doubleValue()) << toInt32(val2.doubleValue());
            return Double.valueOf(result);
        }
    }

    @SuppressWarnings("AndroidJdkLibsChecker")
    public static Number signedRightShift(Number val1, Number val2) {
        if (val1 instanceof BigInteger && val2 instanceof BigInteger) {
            try {
                int intVal2 = ((BigInteger) val2).intValueExact();
                return ((BigInteger) val1).shiftRight(intVal2);
            } catch (ArithmeticException e) {
                // This is outside the scope of the ECMA262 specification.
                throw ScriptRuntime.rangeErrorById("msg.bigint.out.of.range.arithmetic");
            }
        } else if (val1 instanceof BigInteger || val2 instanceof BigInteger) {
            throw ScriptRuntime.typeErrorById("msg.cant.convert.to.number", "BigInt");
        } else if (val1 instanceof Integer && val2 instanceof Integer) {
            return Integer.valueOf(((Integer) val1).intValue() >> ((Integer) val2).intValue());
        } else {
            int result = toInt32(val1.doubleValue()) >> toInt32(val2.doubleValue());
            return Double.valueOf(result);
        }
    }

    public static Number bitwiseNOT(Number val) {
        if (val instanceof BigInteger) {
            return ((BigInteger) val).not();
        } else if (val instanceof Integer) {
            return Integer.valueOf(~((Integer) val).intValue());
        } else {
            int result = ~toInt32(val.doubleValue());
            return Double.valueOf(result);
        }
    }

    /**
     * The method is only present for compatibility.
     *
     * @deprecated Use {@link #nameIncrDecr(Scriptable, String, Context, int)} instead
     */
    @Deprecated
    public static Object nameIncrDecr(Scriptable scopeChain, String id, int incrDecrMask) {
        return nameIncrDecr(scopeChain, id, Context.getContext(), incrDecrMask);
    }

    public static Object nameIncrDecr(
            Scriptable scopeChain, String id, Context cx, int incrDecrMask) {
        Scriptable target;
        Object value;
        search:
        {
            do {
                if (cx.useDynamicScope && scopeChain.getParentScope() == null) {
                    scopeChain = checkDynamicScope(cx.topCallScope, scopeChain);
                }
                target = scopeChain;
                do {
                    if (target instanceof NativeWith
                            && target.getPrototype() instanceof XMLObject) {
                        break;
                    }
                    value = target.get(id, scopeChain);
                    if (value != Scriptable.NOT_FOUND) {
                        break search;
                    }
                    target = target.getPrototype();
                } while (target != null);
                scopeChain = scopeChain.getParentScope();
            } while (scopeChain != null);
            throw notFoundError(null, id);
        }
        return doScriptableIncrDecr(target, id, scopeChain, value, incrDecrMask);
    }

    /**
     * @deprecated Use {@link #propIncrDecr(Object, String, Context, Scriptable, int)} instead
     */
    @Deprecated
    public static Object propIncrDecr(Object obj, String id, Context cx, int incrDecrMask) {
        return propIncrDecr(obj, id, cx, getTopCallScope(cx), incrDecrMask);
    }

    public static Object propIncrDecr(
            Object obj, String id, Context cx, Scriptable scope, int incrDecrMask) {
        Scriptable start = asScriptableOrThrowUndefReadError(cx, scope, obj, id);

        Scriptable target = start;
        Object value;
        search:
        {
            do {
                value = target.get(id, start);
                if (value != Scriptable.NOT_FOUND) {
                    break search;
                }
                target = target.getPrototype();
            } while (target != null);
            start.put(id, start, NaNobj);
            return NaNobj;
        }
        return doScriptableIncrDecr(target, id, start, value, incrDecrMask);
    }

    private static Object doScriptableIncrDecr(
            Scriptable target,
            String id,
            Scriptable protoChainStart,
            Object value,
            int incrDecrMask) {
        final boolean post = (incrDecrMask & Node.POST_FLAG) != 0;

        Number number;
        if (value instanceof Number) {
            number = (Number) value;
        } else {
            number = toNumeric(value);
        }

        Number result;
        if (number instanceof BigInteger) {
            if ((incrDecrMask & Node.DECR_FLAG) == 0) {
                result = ((BigInteger) number).add(BigInteger.ONE);
            } else {
                result = ((BigInteger) number).subtract(BigInteger.ONE);
            }
        } else if (number instanceof Integer) {
            if ((incrDecrMask & Node.DECR_FLAG) == 0) {
                result = ((Integer) number).intValue() + 1;
            } else {
                result = ((Integer) number).intValue() - 1;
            }
        } else {
            if ((incrDecrMask & Node.DECR_FLAG) == 0) {
                result = number.doubleValue() + 1.0;
            } else {
                result = number.doubleValue() - 1.0;
            }
        }

        target.put(id, protoChainStart, result);
        if (post) {
            return number;
        }
        return result;
    }

    /**
     * @deprecated Use {@link #elemIncrDecr(Object, Object, Context, Scriptable, int)} instead
     */
    @Deprecated
    public static Object elemIncrDecr(Object obj, Object index, Context cx, int incrDecrMask) {
        return elemIncrDecr(obj, index, cx, getTopCallScope(cx), incrDecrMask);
    }

    public static Object elemIncrDecr(
            Object obj, Object index, Context cx, Scriptable scope, int incrDecrMask) {
        Object value = getObjectElem(obj, index, cx, scope);
        final boolean post = (incrDecrMask & Node.POST_FLAG) != 0;

        Number number;
        if (value instanceof Number) {
            number = (Number) value;
        } else {
            number = toNumeric(value);
        }

        Number result;
        if (number instanceof BigInteger) {
            if ((incrDecrMask & Node.DECR_FLAG) == 0) {
                result = ((BigInteger) number).add(BigInteger.ONE);
            } else {
                result = ((BigInteger) number).subtract(BigInteger.ONE);
            }
        } else if (number instanceof Integer) {
            if ((incrDecrMask & Node.DECR_FLAG) == 0) {
                result = ((Integer) number).intValue() + 1;
            } else {
                result = ((Integer) number).intValue() - 1;
            }
        } else {
            if ((incrDecrMask & Node.DECR_FLAG) == 0) {
                result = number.doubleValue() + 1.0;
            } else {
                result = number.doubleValue() - 1.0;
            }
        }

        setObjectElem(obj, index, result, cx, scope);
        if (post) {
            return number;
        }
        return result;
    }

    /**
     * @deprecated Use {@link #refIncrDecr(Ref, Context, Scriptable, int)} instead
     */
    @Deprecated
    public static Object refIncrDecr(Ref ref, Context cx, int incrDecrMask) {
        return refIncrDecr(ref, cx, getTopCallScope(cx), incrDecrMask);
    }

    public static Object refIncrDecr(Ref ref, Context cx, Scriptable scope, int incrDecrMask) {
        Object value = ref.get(cx);
        boolean post = ((incrDecrMask & Node.POST_FLAG) != 0);

        Number number;
        if (value instanceof Number) {
            number = (Number) value;
        } else {
            number = toNumeric(value);
        }

        Number result;
        if (number instanceof BigInteger) {
            if ((incrDecrMask & Node.DECR_FLAG) == 0) {
                result = ((BigInteger) number).add(BigInteger.ONE);
            } else {
                result = ((BigInteger) number).subtract(BigInteger.ONE);
            }
        } else if (number instanceof Integer) {
            if ((incrDecrMask & Node.DECR_FLAG) == 0) {
                result = ((Integer) number).intValue() + 1;
            } else {
                result = ((Integer) number).intValue() - 1;
            }
        } else {
            if ((incrDecrMask & Node.DECR_FLAG) == 0) {
                result = number.doubleValue() + 1.0;
            } else {
                result = number.doubleValue() - 1.0;
            }
        }

        ref.set(cx, scope, result);
        if (post) {
            return number;
        }
        return result;
    }

    public static Number negate(Number val) {
        if (val instanceof BigInteger) {
            return ((BigInteger) val).negate();
        }
        if (val instanceof Integer) {
            int iv = (Integer) val;
            if (iv == 0) {
                return negativeZeroObj;
            }
            if (iv > Integer.MIN_VALUE && iv < Integer.MAX_VALUE) {
                // Account for twos-complement representation by not trying
                // to negate values at the extremes
                return Integer.valueOf(-((Integer) val).intValue());
            }
        }
        return -val.doubleValue();
    }

    public static Object toPrimitive(Object input) {
        return toPrimitive(input, null);
    }

    /**
     * The abstract operation ToPrimitive takes argument input (an ECMAScript language value) and
     * optional argument preferredType (string or number) and returns either a normal completion
     * containing an ECMAScript language value or a throw completion. It converts its input argument
     * to a non-Object type. If an object is capable of converting to more than one primitive type,
     * it may use the optional hint preferredType to favour that type.
     *
     * @param input
     * @param preferredType
     * @return
     * @see <a href="https://262.ecma-international.org/15.0/index.html#sec-toprimitive"></a>
     */
    public static Object toPrimitive(Object input, Class<?> preferredType) {
        // 1. If input is an Object, then
        //    a. Let exoticToPrim be ? GetMethod(input, @@toPrimitive).
        //    b. If exoticToPrim is not undefined, then
        //        i. If preferredType is not present, then
        //            1. Let hint be "default".
        //        ii. Else if preferredType is string, then
        //            1. Let hint be "string".
        //        iii. Else,
        //            1. Assert: preferredType is number.
        //            2. Let hint be "number".
        //        iv. Let result be ? Call(exoticToPrim, input, « hint »).
        //        v.  If result is not an Object, return result.
        //        vi. Throw a TypeError exception.
        //    c. If preferredType is not present, let preferredType be number.
        //    d. Return ? OrdinaryToPrimitive(input, preferredType).
        // 2. Return input.

        // do not return on Scriptable's here; we like to fall back to our
        // default impl getDefaultValue() for them
        if (!(input instanceof Scriptable) && !isObject(input)) {
            return input;
        }

        final Scriptable s = (Scriptable) input;
        // to be backward compatible: getProperty(Scriptable obj, Symbol key)
        // throws if obj is not a SymbolScriptable
        Object exoticToPrim = null;
        if (s instanceof SymbolScriptable) {
            exoticToPrim = ScriptableObject.getProperty(s, SymbolKey.TO_PRIMITIVE);
        }
        if (exoticToPrim instanceof Function) {
            final Function func = (Function) exoticToPrim;
            final Context cx = Context.getCurrentContext();
            final Scriptable scope = func.getParentScope();
            final String hint;
            if (preferredType == null) {
                hint = "default";
            } else if (StringClass == preferredType) {
                hint = "string";
            } else {
                hint = "number";
            }
            final Object result = func.call(cx, scope, s, new Object[] {hint});
            if (isObject(result)) {
                throw typeErrorById("msg.cant.convert.to.primitive");
            }
            return result;
        }
        if (exoticToPrim != null
                && exoticToPrim != Scriptable.NOT_FOUND
                && !Undefined.isUndefined(exoticToPrim)) {
            throw notFunctionError(exoticToPrim);
        }
        final Object result = s.getDefaultValue(preferredType);
        if ((result instanceof Scriptable) && !isSymbol(result))
            throw typeErrorById("msg.bad.default.value");
        return result;
    }

    /**
     * Equality
     *
     * <p>See ECMA 11.9
     */
    public static boolean eq(Object x, Object y) {
        if (x == null || Undefined.isUndefined(x)) {
            if (y == null || Undefined.isUndefined(y)) {
                return true;
            }
            if (y instanceof ScriptableObject) {
                Object test = ((ScriptableObject) y).equivalentValues(x);
                if (test != Scriptable.NOT_FOUND) {
                    return ((Boolean) test).booleanValue();
                }
            }
            return false;
        } else if (x instanceof BigInteger) {
            return eqBigInt((BigInteger) x, y);
        } else if (x instanceof Number) {
            return eqNumber(((Number) x).doubleValue(), y);
        } else if (x == y) {
            return true;
        } else if (x instanceof CharSequence) {
            return eqString((CharSequence) x, y);
        } else if (x instanceof Boolean) {
            boolean b = ((Boolean) x).booleanValue();
            if (y instanceof Boolean) {
                return b == ((Boolean) y).booleanValue();
            }
            if (y instanceof ScriptableObject) {
                Object test = ((ScriptableObject) y).equivalentValues(x);
                if (test != Scriptable.NOT_FOUND) {
                    return ((Boolean) test).booleanValue();
                }
            }
            return eqNumber(b ? 1.0 : 0.0, y);
        } else if (isSymbol(x) && isObject(y)) {
            return eq(x, toPrimitive(y));
        } else if (x instanceof Scriptable) {
            if (x instanceof Delegator) {
                x = ((Delegator) x).getDelegee();
                if (y instanceof Delegator) {
                    return eq(x, ((Delegator) y).getDelegee());
                }
                if (x == y) {
                    return true;
                }
            }
            if (y instanceof Delegator && ((Delegator) y).getDelegee() == x) {
                return true;
            }
            if (isSymbol(y) && isObject(x)) {
                return eq(toPrimitive(x), y);
            }
            if (y == null || Undefined.isUndefined(y)) {
                if (x instanceof ScriptableObject) {
                    Object test = ((ScriptableObject) x).equivalentValues(y);
                    if (test != Scriptable.NOT_FOUND) {
                        return ((Boolean) test).booleanValue();
                    }
                }
                return false;
            } else if (y instanceof Scriptable) {
                if (x instanceof ScriptableObject) {
                    Object test = ((ScriptableObject) x).equivalentValues(y);
                    if (test != Scriptable.NOT_FOUND) {
                        return ((Boolean) test).booleanValue();
                    }
                }
                if (y instanceof ScriptableObject) {
                    Object test = ((ScriptableObject) y).equivalentValues(x);
                    if (test != Scriptable.NOT_FOUND) {
                        return ((Boolean) test).booleanValue();
                    }
                }
                if (x instanceof Wrapper && y instanceof Wrapper) {
                    // See bug 413838. Effectively an extension to ECMA for
                    // the LiveConnect case.
                    Object unwrappedX = ((Wrapper) x).unwrap();
                    Object unwrappedY = ((Wrapper) y).unwrap();
                    return unwrappedX == unwrappedY
                            || (isPrimitive(unwrappedX)
                                    && isPrimitive(unwrappedY)
                                    && eq(unwrappedX, unwrappedY));
                }
                return false;
            } else if (y instanceof Boolean) {
                if (x instanceof ScriptableObject) {
                    Object test = ((ScriptableObject) x).equivalentValues(y);
                    if (test != Scriptable.NOT_FOUND) {
                        return ((Boolean) test).booleanValue();
                    }
                }
                double d = ((Boolean) y).booleanValue() ? 1.0 : 0.0;
                return eqNumber(d, x);
            } else if (y instanceof BigInteger) {
                return eqBigInt((BigInteger) y, x);
            } else if (y instanceof Number) {
                return eqNumber(((Number) y).doubleValue(), x);
            } else if (y instanceof CharSequence) {
                return eqString((CharSequence) y, x);
            }
            // covers the case when y == Undefined.instance as well
            return false;
        } else {
            warnAboutNonJSObject(x);
            return x == y;
        }
    }

    /*
     * Implement "SameValue" as in ECMA 7.2.9. This is not the same as "eq" because it handles
     * signed zeroes and NaNs differently.
     */
    public static boolean same(Object x, Object y) {
        if (!typeof(x).equals(typeof(y))) {
            return false;
        }
        if (x instanceof Number) {
            if (isNaN(x) && isNaN(y)) {
                return true;
            }
            return x.equals(y);
        }
        return eq(x, y);
    }

    /** Implement "SameValueZero" from ECMA 7.2.9 */
    public static boolean sameZero(Object x, Object y) {
        if (!typeof(x).equals(typeof(y))) {
            return false;
        }
        if (x instanceof BigInteger) {
            return x.equals(y);
        }
        if (x instanceof Number) {
            if (isNaN(x) && isNaN(y)) {
                return true;
            }
            final double dx = ((Number) x).doubleValue();
            if (y instanceof Number) {
                final double dy = ((Number) y).doubleValue();
                if (((dx == negativeZero) && (dy == 0.0)) || ((dx == 0.0) && dy == negativeZero)) {
                    return true;
                }
            }
            return eqNumber(dx, y);
        }
        return eq(x, y);
    }

    public static boolean isNaN(Object n) {
        if (n instanceof Double) {
            return ((Double) n).isNaN();
        }
        if (n instanceof Float) {
            return ((Float) n).isNaN();
        }
        return false;
    }

    public static boolean isPrimitive(Object obj) {
        return obj == null
                || Undefined.isUndefined(obj)
                || (obj instanceof Number)
                || (obj instanceof String)
                || (obj instanceof Boolean);
    }

    static boolean eqNumber(double x, Object y) {
        for (; ; ) {
            if (y == null || Undefined.isUndefined(y)) {
                return false;
            } else if (y instanceof BigInteger) {
                return eqBigInt((BigInteger) y, x);
            } else if (y instanceof Number) {
                return x == ((Number) y).doubleValue();
            } else if (y instanceof CharSequence) {
                return x == toNumber(y);
            } else if (y instanceof Boolean) {
                return x == (((Boolean) y).booleanValue() ? 1.0 : +0.0);
            } else if (isSymbol(y)) {
                return false;
            } else if (y instanceof Scriptable) {
                if (y instanceof ScriptableObject) {
                    Object xval = wrapNumber(x);
                    Object test = ((ScriptableObject) y).equivalentValues(xval);
                    if (test != Scriptable.NOT_FOUND) {
                        return ((Boolean) test).booleanValue();
                    }
                }
                y = toPrimitive(y);
            } else {
                warnAboutNonJSObject(y);
                return false;
            }
        }
    }

    static boolean eqBigInt(BigInteger x, Object y) {
        for (; ; ) {
            if (y == null || Undefined.isUndefined(y)) {
                return false;
            } else if (y instanceof BigInteger) {
                return x.equals(y);
            } else if (y instanceof Number) {
                return eqBigInt(x, ((Number) y).doubleValue());
            } else if (y instanceof CharSequence) {
                BigInteger biy;
                try {
                    biy = toBigInt(y);
                } catch (EcmaError e) {
                    return false;
                }
                return x.equals(biy);
            } else if (y instanceof Boolean) {
                BigInteger biy = ((Boolean) y).booleanValue() ? BigInteger.ONE : BigInteger.ZERO;
                return x.equals(biy);
            } else if (isSymbol(y)) {
                return false;
            } else if (y instanceof Scriptable) {
                if (y instanceof ScriptableObject) {
                    Object test = ((ScriptableObject) y).equivalentValues(x);
                    if (test != Scriptable.NOT_FOUND) {
                        return ((Boolean) test).booleanValue();
                    }
                }
                y = toPrimitive(y);
            } else {
                warnAboutNonJSObject(y);
                return false;
            }
        }
    }

    private static boolean eqBigInt(BigInteger x, double y) {
        if (Double.isNaN(y) || Double.isInfinite(y)) {
            return false;
        }

        double d = Math.ceil(y);
        if (d != y) {
            return false;
        }

        BigDecimal bdx = new BigDecimal(x);
        BigDecimal bdy = new BigDecimal(d, MathContext.UNLIMITED);
        return bdx.compareTo(bdy) == 0;
    }

    private static boolean eqString(CharSequence x, Object y) {
        for (; ; ) {
            if (y == null || Undefined.isUndefined(y)) {
                return false;
            } else if (y instanceof CharSequence) {
                CharSequence c = (CharSequence) y;
                return x.length() == c.length() && x.toString().equals(c.toString());
            } else if (y instanceof BigInteger) {
                BigInteger bix;
                try {
                    bix = toBigInt(x);
                } catch (EcmaError e) {
                    return false;
                }
                return bix.equals(y);
            } else if (y instanceof Number) {
                return toNumber(x.toString()) == ((Number) y).doubleValue();
            } else if (y instanceof Boolean) {
                return toNumber(x.toString()) == (((Boolean) y).booleanValue() ? 1.0 : 0.0);
            } else if (isSymbol(y)) {
                return false;
            } else if (y instanceof Scriptable) {
                if (y instanceof ScriptableObject) {
                    Object test = ((ScriptableObject) y).equivalentValues(x.toString());
                    if (test != Scriptable.NOT_FOUND) {
                        return ((Boolean) test).booleanValue();
                    }
                }
                y = toPrimitive(y);
                continue;
            } else {
                warnAboutNonJSObject(y);
                return false;
            }
        }
    }

    public static boolean shallowEq(Object x, Object y) {
        if (x == y) {
            if (!(x instanceof Number)) {
                return true;
            }
            // NaN check
            double d = ((Number) x).doubleValue();
            return !Double.isNaN(d);
        }
        if (x == null || x == Undefined.instance || x == Undefined.SCRIPTABLE_UNDEFINED) {
            if ((x == Undefined.instance && y == Undefined.SCRIPTABLE_UNDEFINED)
                    || (x == Undefined.SCRIPTABLE_UNDEFINED && y == Undefined.instance))
                return true;
            return false;
        } else if (x instanceof BigInteger) {
            if (y instanceof BigInteger) {
                return x.equals(y);
            }
        } else if (x instanceof Number && !(x instanceof BigInteger)) {
            if (y instanceof Number && !(y instanceof BigInteger)) {
                return ((Number) x).doubleValue() == ((Number) y).doubleValue();
            }
        } else if (x instanceof CharSequence) {
            if (y instanceof CharSequence) {
                return x.toString().equals(y.toString());
            }
        } else if (x instanceof Boolean) {
            if (y instanceof Boolean) {
                return x.equals(y);
            }
        } else if (x instanceof Scriptable) {
            if (x instanceof Wrapper && y instanceof Wrapper) {
                return ((Wrapper) x).unwrap() == ((Wrapper) y).unwrap();
            }
            if (x instanceof Delegator) {
                x = ((Delegator) x).getDelegee();
                if (y instanceof Delegator) {
                    return shallowEq(x, ((Delegator) y).getDelegee());
                }
                if (x == y) {
                    return true;
                }
            }
            if (y instanceof Delegator && ((Delegator) y).getDelegee() == x) {
                return true;
            }
        } else {
            warnAboutNonJSObject(x);
            return x == y;
        }
        return false;
    }

    /**
     * The instanceof operator.
     *
     * @return a instanceof b
     */
    public static boolean instanceOf(Object a, Object b, Context cx) {
        // Check RHS is an object
        if (!(b instanceof Scriptable)) {
            throw typeErrorById("msg.instanceof.not.object");
        }

        // for primitive values on LHS, return false
        if (!(a instanceof Scriptable)) return false;

        return ((Scriptable) b).hasInstance((Scriptable) a);
    }

    /**
     * Delegates to
     *
     * @return true iff rhs appears in lhs' proto chain
     */
    public static boolean jsDelegatesTo(Scriptable lhs, Scriptable rhs) {
        Scriptable proto = lhs.getPrototype();

        while (proto != null) {
            if (proto.equals(rhs)) return true;
            proto = proto.getPrototype();
        }

        return false;
    }

    /**
     * The in operator.
     *
     * <p>This is a new JS 1.3 language feature. The in operator mirrors the operation of the for ..
     * in construct, and tests whether the rhs has the property given by the lhs. It is different
     * from the for .. in construct in that: <br>
     * - it doesn't perform ToObject on the right hand side <br>
     * - it returns true for DontEnum properties.
     *
     * @param a the left hand operand
     * @param b the right hand operand
     * @return true if property name or element number a is a property of b
     */
    public static boolean in(Object a, Object b, Context cx) {
        if (!(b instanceof Scriptable)) {
            throw typeErrorById("msg.in.not.object");
        }

        return hasObjectElem((Scriptable) b, a, cx);
    }

    public static boolean compare(Object val1, Object val2, int op) {
        assert op == Token.GE || op == Token.LE || op == Token.GT || op == Token.LT;

        if (val1 instanceof Number && val2 instanceof Number) {
            return compare((Number) val1, (Number) val2, op);
        } else {
            if (isSymbol(val1) || isSymbol(val2)) {
                throw typeErrorById("msg.compare.symbol");
            }
            val1 = toPrimitive(val1, NumberClass);
            val2 = toPrimitive(val2, NumberClass);

            if (val1 instanceof CharSequence) {
                if (val2 instanceof CharSequence) {
                    return compareTo(val1.toString(), val2.toString(), op);
                }

                if (val2 instanceof BigInteger) {
                    try {
                        return compareTo(toBigInt(val1.toString()), (BigInteger) val2, op);
                    } catch (EcmaError e) {
                        return false;
                    }
                }
            }
            if (val1 instanceof BigInteger && val2 instanceof CharSequence) {
                try {
                    return compareTo((BigInteger) val1, toBigInt(val2.toString()), op);
                } catch (EcmaError e) {
                    return false;
                }
            }

            return compare(toNumeric(val1), toNumeric(val2), op);
        }
    }

    public static boolean compare(Number val1, Number val2, int op) {
        assert op == Token.GE || op == Token.LE || op == Token.GT || op == Token.LT;

        if (val1 instanceof BigInteger && val2 instanceof BigInteger) {
            return compareTo((BigInteger) val1, (BigInteger) val2, op);
        } else if (val1 instanceof BigInteger || val2 instanceof BigInteger) {
            BigDecimal bd1;
            if (val1 instanceof BigInteger) {
                bd1 = new BigDecimal((BigInteger) val1);
            } else {
                double d = val1.doubleValue();
                if (Double.isNaN(d)) {
                    return false;
                } else if (d == Double.POSITIVE_INFINITY) {
                    return op == Token.GE || op == Token.GT;
                } else if (d == Double.NEGATIVE_INFINITY) {
                    return op == Token.LE || op == Token.LT;
                }
                bd1 = new BigDecimal(d, MathContext.UNLIMITED);
            }

            BigDecimal bd2;
            if (val2 instanceof BigInteger) {
                bd2 = new BigDecimal((BigInteger) val2);
            } else {
                double d = val2.doubleValue();
                if (Double.isNaN(d)) {
                    return false;
                } else if (d == Double.POSITIVE_INFINITY) {
                    return op == Token.LE || op == Token.LT;
                } else if (d == Double.NEGATIVE_INFINITY) {
                    return op == Token.GE || op == Token.GT;
                }
                bd2 = new BigDecimal(d, MathContext.UNLIMITED);
            }

            return compareTo(bd1, bd2, op);
        }
        return compareTo(val1.doubleValue(), val2.doubleValue(), op);
    }

    private static <T> boolean compareTo(Comparable<T> val1, T val2, int op) {
        switch (op) {
            case Token.GE:
                return val1.compareTo(val2) >= 0;
            case Token.LE:
                return val1.compareTo(val2) <= 0;
            case Token.GT:
                return val1.compareTo(val2) > 0;
            case Token.LT:
                return val1.compareTo(val2) < 0;
            default:
                throw Kit.codeBug();
        }
    }

    private static boolean compareTo(double d1, double d2, int op) {
        switch (op) {
            case Token.GE:
                return d1 >= d2;
            case Token.LE:
                return d1 <= d2;
            case Token.GT:
                return d1 > d2;
            case Token.LT:
                return d1 < d2;
            default:
                throw Kit.codeBug();
        }
    }

    // ------------------
    // Statements
    // ------------------

    public static ScriptableObject getGlobal(Context cx) {
        final String GLOBAL_CLASS = "org.mozilla.javascript.tools.shell.Global";
        Class<?> globalClass = Kit.classOrNull(GLOBAL_CLASS);
        if (globalClass != null) {
            try {
                Class<?>[] parm = {ScriptRuntime.ContextClass};
                Constructor<?> globalClassCtor = globalClass.getConstructor(parm);
                Object[] arg = {cx};
                return (ScriptableObject) globalClassCtor.newInstance(arg);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                // fall through...
            }
        }
        return new ImporterTopLevel(cx);
    }

    public static boolean hasTopCall(Context cx) {
        return (cx.topCallScope != null);
    }

    public static Scriptable getTopCallScope(Context cx) {
        Scriptable scope = cx.topCallScope;
        if (scope == null) {
            throw new IllegalStateException();
        }
        return scope;
    }

    /**
     * @deprecated Use {@link #doTopCall(Callable, Context, Scriptable, Scriptable, Object[],
     *     boolean)} instead
     */
    @Deprecated
    public static Object doTopCall(
            Callable callable, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return doTopCall(callable, cx, scope, thisObj, args, cx.isTopLevelStrict);
    }

    public static Object doTopCall(
            Callable callable,
            Context cx,
            Scriptable scope,
            Scriptable thisObj,
            Object[] args,
            boolean isTopLevelStrict) {
        if (scope == null) throw new IllegalArgumentException();
        if (cx.topCallScope != null) throw new IllegalStateException();

        Object result;
        cx.topCallScope = ScriptableObject.getTopLevelScope(scope);
        cx.useDynamicScope = cx.hasFeature(Context.FEATURE_DYNAMIC_SCOPE);
        boolean previousTopLevelStrict = cx.isTopLevelStrict;
        cx.isTopLevelStrict = isTopLevelStrict;
        ContextFactory f = cx.getFactory();
        try {
            result = f.doTopCall(callable, cx, scope, thisObj, args);
        } finally {
            cx.topCallScope = null;
            // Cleanup cached references
            cx.cachedXMLLib = null;
            cx.isTopLevelStrict = previousTopLevelStrict;
            // Function should always call exitActivationFunction
            // if it creates activation record
            assert (cx.currentActivationCall == null);
        }
        return result;
    }

    /**
     * Return <code>possibleDynamicScope</code> if <code>staticTopScope</code> is present on its
     * prototype chain and return <code>staticTopScope</code> otherwise. Should only be called when
     * <code>staticTopScope</code> is top scope.
     */
    static Scriptable checkDynamicScope(
            Scriptable possibleDynamicScope, Scriptable staticTopScope) {
        // Return cx.topCallScope if scope
        if (possibleDynamicScope == staticTopScope) {
            return possibleDynamicScope;
        }
        Scriptable proto = possibleDynamicScope;
        for (; ; ) {
            proto = proto.getPrototype();
            if (proto == staticTopScope) {
                return possibleDynamicScope;
            }
            if (proto == null) {
                return staticTopScope;
            }
        }
    }

    public static void addInstructionCount(Context cx, int instructionsToAdd) {
        cx.instructionCount += instructionsToAdd;
        if (cx.instructionCount > cx.instructionThreshold) {
            cx.observeInstructionCount(cx.instructionCount);
            cx.instructionCount = 0;
        }
    }

    public static void initScript(
            NativeFunction funObj,
            Scriptable thisObj,
            Context cx,
            Scriptable scope,
            boolean evalScript) {
        if (cx.topCallScope == null) throw new IllegalStateException();

        int varCount = funObj.getParamAndVarCount();
        if (varCount != 0) {

            Scriptable varScope = scope;
            // Never define any variables from var statements inside with
            // object. See bug 38590.
            while (varScope instanceof NativeWith) {
                varScope = varScope.getParentScope();
            }

            for (int i = varCount; i-- != 0; ) {
                String name = funObj.getParamOrVarName(i);
                boolean isConst = funObj.getParamOrVarConst(i);
                // Don't overwrite existing def if already defined in object
                // or prototypes of object.
                if (!ScriptableObject.hasProperty(scope, name)) {
                    if (isConst) {
                        ScriptableObject.defineConstProperty(varScope, name);
                    } else if (!evalScript) {
                        if (!(funObj instanceof InterpretedFunction)
                                || ((InterpretedFunction) funObj).hasFunctionNamed(name)) {
                            // Global var definitions are supposed to be DONTDELETE
                            ScriptableObject.defineProperty(
                                    varScope, name, Undefined.instance, ScriptableObject.PERMANENT);
                        }
                    } else {
                        varScope.put(name, varScope, Undefined.instance);
                    }
                } else {
                    ScriptableObject.redefineProperty(scope, name, isConst);
                }
            }
        }
    }

    /**
     * @deprecated Use {@link #createFunctionActivation(NativeFunction, Context, Scriptable,
     *     Object[], boolean, boolean, Scriptable)} instead
     */
    @Deprecated
    public static Scriptable createFunctionActivation(
            NativeFunction funObj, Scriptable scope, Object[] args) {
        return createFunctionActivation(
                funObj, Context.getCurrentContext(), scope, args, false, false, null);
    }

    /**
     * @deprecated Use {@link #createFunctionActivation(NativeFunction, Context, Scriptable,
     *     Object[], boolean, boolean, Scriptable)} instead
     */
    @Deprecated
    public static Scriptable createFunctionActivation(
            NativeFunction funObj, Scriptable scope, Object[] args, boolean isStrict) {
        return new NativeCall(
                funObj, Context.getCurrentContext(), scope, args, false, isStrict, false, null);
    }

    public static Scriptable createFunctionActivation(
            NativeFunction funObj,
            Context cx,
            Scriptable scope,
            Object[] args,
            boolean isStrict,
            boolean argsHasRest,
            Scriptable homeObject) {
        return new NativeCall(funObj, cx, scope, args, false, isStrict, argsHasRest, homeObject);
    }

    /**
     * @deprecated Use {@link #createArrowFunctionActivation(NativeFunction, Context, Scriptable,
     *     Object[], boolean, boolean, Scriptable)} instead
     */
    @Deprecated
    public static Scriptable createArrowFunctionActivation(
            NativeFunction funObj, Scriptable scope, Object[] args, boolean isStrict) {
        return new NativeCall(
                funObj, Context.getCurrentContext(), scope, args, true, isStrict, false, null);
    }

    public static Scriptable createArrowFunctionActivation(
            NativeFunction funObj,
            Context cx,
            Scriptable scope,
            Object[] args,
            boolean isStrict,
            boolean argsHasRest,
            Scriptable homeObject) {
        return new NativeCall(funObj, cx, scope, args, true, isStrict, argsHasRest, homeObject);
    }

    public static void enterActivationFunction(Context cx, Scriptable scope) {
        if (cx.topCallScope == null) throw new IllegalStateException();
        NativeCall call = (NativeCall) scope;
        call.parentActivationCall = cx.currentActivationCall;
        cx.currentActivationCall = call;
        call.defineAttributesForArguments();
    }

    public static void exitActivationFunction(Context cx) {
        NativeCall call = cx.currentActivationCall;
        cx.currentActivationCall = call.parentActivationCall;
        call.parentActivationCall = null;
    }

    static NativeCall findFunctionActivation(Context cx, Function f) {
        NativeCall call = cx.currentActivationCall;
        while (call != null) {
            if (call.function == f) return call;
            call = call.parentActivationCall;
        }
        return null;
    }

    public static Scriptable newCatchScope(
            Throwable t,
            Scriptable lastCatchScope,
            String exceptionName,
            Context cx,
            Scriptable scope) {
        Object obj;
        boolean cacheObj;

        getObj:
        if (t instanceof JavaScriptException) {
            cacheObj = false;
            obj = ((JavaScriptException) t).getValue();
        } else {
            cacheObj = true;

            // Create wrapper object unless it was associated with
            // the previous scope object

            if (lastCatchScope != null) {
                NativeObject last = (NativeObject) lastCatchScope;
                obj = last.getAssociatedValue(t);
                if (obj == null) Kit.codeBug();
                break getObj;
            }

            RhinoException re;
            TopLevel.NativeErrors type;
            String errorMsg;
            Throwable javaException = null;

            if (t instanceof EcmaError) {
                EcmaError ee = (EcmaError) t;
                re = ee;
                type = TopLevel.NativeErrors.valueOf(ee.getName());
                errorMsg = ee.getErrorMessage();
            } else if (t instanceof WrappedException) {
                WrappedException we = (WrappedException) t;
                re = we;
                javaException = we.getWrappedException();

                if (!isVisible(cx, javaException)) {
                    type = TopLevel.NativeErrors.InternalError;
                    errorMsg = javaException.getMessage();
                } else {
                    type = TopLevel.NativeErrors.JavaException;
                    errorMsg =
                            javaException.getClass().getName() + ": " + javaException.getMessage();
                }
            } else if (t instanceof EvaluatorException) {
                // Pure evaluator exception, nor WrappedException instance
                EvaluatorException ee = (EvaluatorException) t;
                re = ee;
                type = TopLevel.NativeErrors.InternalError;
                errorMsg = ee.getMessage();
            } else if (cx.hasFeature(Context.FEATURE_ENHANCED_JAVA_ACCESS)) {
                // With FEATURE_ENHANCED_JAVA_ACCESS, scripts can catch
                // all exception types
                re = new WrappedException(t);
                type = TopLevel.NativeErrors.JavaException;
                errorMsg = t.toString();
            } else {
                // Script can catch only instances of JavaScriptException,
                // EcmaError and EvaluatorException
                throw Kit.codeBug();
            }

            String sourceUri = re.sourceName();
            if (sourceUri == null) {
                sourceUri = "";
            }
            int line = re.lineNumber();
            Object[] args;
            if (line > 0) {
                args = new Object[] {errorMsg, sourceUri, Integer.valueOf(line)};
            } else {
                args = new Object[] {errorMsg, sourceUri};
            }

            Scriptable errorObject = newNativeError(cx, scope, type, args);
            // set exception in Error objects to enable non-ECMA "stack" property
            if (errorObject instanceof NativeError) {
                ((NativeError) errorObject).setStackProvider(re);
            }

            if (javaException != null && isVisible(cx, javaException)) {
                Object wrap = cx.getWrapFactory().wrap(cx, scope, javaException, null);
                ScriptableObject.defineProperty(
                        errorObject,
                        "javaException",
                        wrap,
                        ScriptableObject.PERMANENT
                                | ScriptableObject.READONLY
                                | ScriptableObject.DONTENUM);
            }
            if (isVisible(cx, re)) {
                Object wrap = cx.getWrapFactory().wrap(cx, scope, re, null);
                ScriptableObject.defineProperty(
                        errorObject,
                        "rhinoException",
                        wrap,
                        ScriptableObject.PERMANENT
                                | ScriptableObject.READONLY
                                | ScriptableObject.DONTENUM);
            }
            obj = errorObject;
        }

        NativeObject catchScopeObject = new NativeObject();
        // See ECMA 12.4
        if (exceptionName != null) {
            catchScopeObject.defineProperty(exceptionName, obj, ScriptableObject.PERMANENT);
        }

        if (cx.hasFeature(Context.FEATURE_ENHANCED_JAVA_ACCESS) && isVisible(cx, t)) {
            // Add special Rhino object __exception__ defined in the catch
            // scope that can be used to retrieve the Java exception associated
            // with the JavaScript exception (to get stack trace info, etc.)
            catchScopeObject.defineProperty(
                    "__exception__",
                    Context.javaToJS(t, scope),
                    ScriptableObject.PERMANENT | ScriptableObject.DONTENUM);
        }

        if (cacheObj) {
            catchScopeObject.associateValue(t, obj);
        }
        return catchScopeObject;
    }

    public static Scriptable wrapException(Throwable t, Scriptable scope, Context cx) {
        RhinoException re;
        String errorName;
        String errorMsg;
        Throwable javaException = null;

        if (t instanceof EcmaError) {
            EcmaError ee = (EcmaError) t;
            re = ee;
            errorName = ee.getName();
            errorMsg = ee.getErrorMessage();
        } else if (t instanceof WrappedException) {
            WrappedException we = (WrappedException) t;
            re = we;
            javaException = we.getWrappedException();
            errorName = "JavaException";
            errorMsg = javaException.getClass().getName() + ": " + javaException.getMessage();
        } else if (t instanceof EvaluatorException) {
            // Pure evaluator exception, nor WrappedException instance
            EvaluatorException ee = (EvaluatorException) t;
            re = ee;
            errorName = "InternalError";
            errorMsg = ee.getMessage();
        } else if (cx.hasFeature(Context.FEATURE_ENHANCED_JAVA_ACCESS)) {
            // With FEATURE_ENHANCED_JAVA_ACCESS, scripts can catch
            // all exception types
            re = new WrappedException(t);
            errorName = "JavaException";
            errorMsg = t.toString();
        } else {
            // Script can catch only instances of JavaScriptException,
            // EcmaError and EvaluatorException
            throw Kit.codeBug();
        }

        String sourceUri = re.sourceName();
        if (sourceUri == null) {
            sourceUri = "";
        }
        int line = re.lineNumber();
        Object[] args;
        if (line > 0) {
            args = new Object[] {errorMsg, sourceUri, Integer.valueOf(line)};
        } else {
            args = new Object[] {errorMsg, sourceUri};
        }

        Scriptable errorObject = cx.newObject(scope, errorName, args);
        ScriptableObject.putProperty(errorObject, "name", errorName);
        // set exception in Error objects to enable non-ECMA "stack" property
        if (errorObject instanceof NativeError) {
            ((NativeError) errorObject).setStackProvider(re);
        }

        if (javaException != null && isVisible(cx, javaException)) {
            Object wrap = cx.getWrapFactory().wrap(cx, scope, javaException, null);
            ScriptableObject.defineProperty(
                    errorObject,
                    "javaException",
                    wrap,
                    ScriptableObject.PERMANENT
                            | ScriptableObject.READONLY
                            | ScriptableObject.DONTENUM);
        }
        if (isVisible(cx, re)) {
            Object wrap = cx.getWrapFactory().wrap(cx, scope, re, null);
            ScriptableObject.defineProperty(
                    errorObject,
                    "rhinoException",
                    wrap,
                    ScriptableObject.PERMANENT
                            | ScriptableObject.READONLY
                            | ScriptableObject.DONTENUM);
        }
        return errorObject;
    }

    private static boolean isVisible(Context cx, Object obj) {
        ClassShutter shutter = cx.getClassShutter();
        return shutter == null || shutter.visibleToScripts(obj.getClass().getName());
    }

    public static Scriptable enterWith(Object obj, Context cx, Scriptable scope) {
        Scriptable sobj = toObjectOrNull(cx, obj, scope);
        if (sobj == null) {
            throw typeErrorById("msg.undef.with", toString(obj));
        }
        if (sobj instanceof XMLObject) {
            XMLObject xmlObject = (XMLObject) sobj;
            return xmlObject.enterWith(scope);
        }
        return new NativeWith(scope, sobj);
    }

    public static Scriptable leaveWith(Scriptable scope) {
        NativeWith nw = (NativeWith) scope;
        return nw.getParentScope();
    }

    public static Scriptable enterDotQuery(Object value, Scriptable scope) {
        if (!(value instanceof XMLObject)) {
            throw notXmlError(value);
        }
        XMLObject object = (XMLObject) value;
        return object.enterDotQuery(scope);
    }

    public static Object updateDotQuery(boolean value, Scriptable scope) {
        // Return null to continue looping
        NativeWith nw = (NativeWith) scope;
        return nw.updateDotQuery(value);
    }

    public static Scriptable leaveDotQuery(Scriptable scope) {
        NativeWith nw = (NativeWith) scope;
        return nw.getParentScope();
    }

    /**
     * @deprecated Use {@link #setFunctionProtoAndParent(BaseFunction, Context, Scriptable)} instead
     */
    @Deprecated
    public static void setFunctionProtoAndParent(BaseFunction fn, Scriptable scope) {
        setFunctionProtoAndParent(fn, Context.getCurrentContext(), scope, false);
    }

    public static void setFunctionProtoAndParent(BaseFunction fn, Context cx, Scriptable scope) {
        setFunctionProtoAndParent(fn, cx, scope, false);
    }

    /**
     * @deprecated Use {@link #setFunctionProtoAndParent(BaseFunction, Context, Scriptable,
     *     boolean)} instead
     */
    @Deprecated
    public static void setFunctionProtoAndParent(
            BaseFunction fn, Scriptable scope, boolean es6GeneratorFunction) {
        setFunctionProtoAndParent(fn, Context.getCurrentContext(), scope, es6GeneratorFunction);
    }

    public static void setFunctionProtoAndParent(
            BaseFunction fn, Context cx, Scriptable scope, boolean es6GeneratorFunction) {
        fn.setParentScope(scope);
        if (es6GeneratorFunction) {
            fn.setPrototype(ScriptableObject.getGeneratorFunctionPrototype(scope));
        } else {
            fn.setPrototype(ScriptableObject.getFunctionPrototype(scope));
        }

        if (cx != null && cx.getLanguageVersion() >= Context.VERSION_ES6) {
            fn.setStandardPropertyAttributes(ScriptableObject.READONLY | ScriptableObject.DONTENUM);
        }
    }

    public static void setObjectProtoAndParent(ScriptableObject object, Scriptable scope) {
        // Compared with function it always sets the scope to top scope
        scope = ScriptableObject.getTopLevelScope(scope);
        object.setParentScope(scope);
        Scriptable proto = ScriptableObject.getClassPrototype(scope, object.getClassName());
        object.setPrototype(proto);
    }

    public static void setBuiltinProtoAndParent(
            ScriptableObject object, Scriptable scope, TopLevel.Builtins type) {
        scope = ScriptableObject.getTopLevelScope(scope);
        object.setParentScope(scope);
        object.setPrototype(TopLevel.getBuiltinPrototype(scope, type));
    }

    public static void initFunction(
            Context cx, Scriptable scope, NativeFunction function, int type, boolean fromEvalCode) {
        if (type == FunctionNode.FUNCTION_STATEMENT) {
            String name = function.getFunctionName();
            if (name != null && name.length() != 0) {
                if (!fromEvalCode) {
                    // ECMA specifies that functions defined in global and
                    // function scope outside eval should have DONTDELETE set.
                    ScriptableObject.defineProperty(
                            scope, name, function, ScriptableObject.PERMANENT);
                } else {
                    scope.put(name, scope, function);
                }
            }
        } else if (type == FunctionNode.FUNCTION_EXPRESSION_STATEMENT) {
            String name = function.getFunctionName();
            if (name != null && name.length() != 0) {
                // Always put function expression statements into initial
                // activation object ignoring the with statement to follow
                // SpiderMonkey
                while (scope instanceof NativeWith) {
                    scope = scope.getParentScope();
                }
                scope.put(name, scope, function);
            }
        } else {
            throw Kit.codeBug();
        }
    }

    public static Scriptable newArrayLiteral(
            Object[] objects, int[] skipIndices, Context cx, Scriptable scope) {
        final int SKIP_DENSITY = 2;
        int count = objects.length;
        int skipCount = 0;
        if (skipIndices != null) {
            skipCount = skipIndices.length;
        }
        int length = count + skipCount;
        if (length > 1 && skipCount * SKIP_DENSITY < length) {
            // If not too sparse, create whole array for constructor
            Object[] sparse;
            if (skipCount == 0) {
                sparse = objects;
            } else {
                sparse = new Object[length];
                int skip = 0;
                for (int i = 0, j = 0; i != length; ++i) {
                    if (skip != skipCount && skipIndices[skip] == i) {
                        sparse[i] = Scriptable.NOT_FOUND;
                        ++skip;
                        continue;
                    }
                    sparse[i] = objects[j];
                    ++j;
                }
            }
            return cx.newArray(scope, sparse);
        }

        Scriptable array = cx.newArray(scope, length);

        int skip = 0;
        for (int i = 0, j = 0; i != length; ++i) {
            if (skip != skipCount && skipIndices[skip] == i) {
                ++skip;
                continue;
            }
            array.put(i, array, objects[j]);
            ++j;
        }
        return array;
    }

    /**
     * This method is here for backward compat with existing compiled code. It is called when an
     * object literal is compiled. The next instance will be the version called from new code.
     * <strong>This method only present for compatibility.</strong>
     *
     * @deprecated Use {@link #fillObjectLiteral(Scriptable, Object[], Object[], int[], Context,
     *     Scriptable)} instead
     */
    @Deprecated
    public static Scriptable newObjectLiteral(
            Object[] propertyIds, Object[] propertyValues, Context cx, Scriptable scope) {
        // Passing null for getterSetters means no getters or setters
        Scriptable object = cx.newObject(scope);
        fillObjectLiteral(object, propertyIds, propertyValues, null, cx, scope);
        return object;
    }

    /**
     * This method is here for backward compat with existing compiled code. <strong>This method only
     * present for compatibility.</strong>
     *
     * @deprecated Use {@link #fillObjectLiteral(Scriptable, Object[], Object[], int[], Context,
     *     Scriptable)}
     */
    @Deprecated
    public static Scriptable newObjectLiteral(
            Object[] propertyIds,
            Object[] propertyValues,
            int[] getterSetters,
            Context cx,
            Scriptable scope) {
        Scriptable object = cx.newObject(scope);
        fillObjectLiteral(object, propertyIds, propertyValues, getterSetters, cx, scope);
        return object;
    }

    public static void fillObjectLiteral(
            Scriptable object,
            Object[] propertyIds,
            Object[] propertyValues,
            int[] getterSetters,
            Context cx,
            Scriptable scope) {
        int end = propertyIds == null ? 0 : propertyIds.length;
        for (int i = 0; i != end; ++i) {
            Object id = propertyIds[i];

            // -1 for property getter, 1 for property setter, 0 for a regular value property
            int getterSetter = getterSetters == null ? 0 : getterSetters[i];
            Object value = propertyValues[i];

            if (getterSetter == 0) {
                if (id instanceof Symbol) {
                    Symbol sym = (Symbol) id;
                    SymbolScriptable so = (SymbolScriptable) object;
                    so.put(sym, object, value);
                } else if (id instanceof Integer && ((Integer) id) >= 0) {
                    int index = (Integer) id;
                    object.put(index, object, value);
                } else {
                    StringIdOrIndex s = toStringIdOrIndex(id);
                    if (s.stringId == null) {
                        object.put(s.index, object, value);
                    } else if (isSpecialProperty(s.stringId)) {
                        Ref ref = specialRef(object, s.stringId, cx, scope);
                        ref.set(cx, scope, value);
                    } else {
                        object.put(s.stringId, object, value);
                    }
                }
            } else {
                ScriptableObject so = (ScriptableObject) object;
                Callable getterOrSetter = (Callable) value;
                boolean isSetter = getterSetter == 1;
                Integer index = id instanceof Integer ? (Integer) id : null;
                Object key =
                        index != null
                                ? null
                                : (id instanceof Symbol ? id : ScriptRuntime.toString(id));
                so.setGetterOrSetter(key, index == null ? 0 : index, getterOrSetter, isSetter);
            }
        }
    }

    public static boolean isArrayObject(Object obj) {
        return obj instanceof NativeArray || obj instanceof Arguments;
    }

    public static Object[] getArrayElements(Scriptable object) {
        Context cx = Context.getContext();
        long longLen = NativeArray.getLengthProperty(cx, object);
        if (longLen > Integer.MAX_VALUE) {
            // arrays beyond  MAX_INT is not in Java in any case
            throw new IllegalArgumentException();
        }
        int len = (int) longLen;
        if (len == 0) {
            return ScriptRuntime.emptyArgs;
        }
        Object[] result = new Object[len];
        for (int i = 0; i < len; i++) {
            Object elem = ScriptableObject.getProperty(object, i);
            result[i] = (elem == Scriptable.NOT_FOUND) ? Undefined.instance : elem;
        }
        return result;
    }

    static void checkDeprecated(Context cx, String name) {
        int version = cx.getLanguageVersion();
        if (version >= Context.VERSION_1_4 || version == Context.VERSION_DEFAULT) {
            String msg = getMessageById("msg.deprec.ctor", name);
            if (version == Context.VERSION_DEFAULT) Context.reportWarning(msg);
            else throw Context.reportRuntimeError(msg);
        }
    }

    /**
     * @deprecated Use {@link #getMessageById(String messageId, Object... args)} instead
     */
    @Deprecated
    public static String getMessage0(String messageId) {
        return getMessage(messageId, null);
    }

    /**
     * @deprecated Use {@link #getMessageById(String messageId, Object... args)} instead
     */
    @Deprecated
    public static String getMessage1(String messageId, Object arg1) {
        Object[] arguments = {arg1};
        return getMessage(messageId, arguments);
    }

    /**
     * @deprecated Use {@link #getMessageById(String messageId, Object... args)} instead
     */
    @Deprecated
    public static String getMessage2(String messageId, Object arg1, Object arg2) {
        Object[] arguments = {arg1, arg2};
        return getMessage(messageId, arguments);
    }

    /**
     * @deprecated Use {@link #getMessageById(String messageId, Object... args)} instead
     */
    @Deprecated
    public static String getMessage3(String messageId, Object arg1, Object arg2, Object arg3) {
        Object[] arguments = {arg1, arg2, arg3};
        return getMessage(messageId, arguments);
    }

    /**
     * @deprecated Use {@link #getMessageById(String messageId, Object... args)} instead
     */
    @Deprecated
    public static String getMessage4(
            String messageId, Object arg1, Object arg2, Object arg3, Object arg4) {
        Object[] arguments = {arg1, arg2, arg3, arg4};
        return getMessage(messageId, arguments);
    }

    /**
     * This is an interface defining a message provider. Create your own implementation to override
     * the default error message provider.
     *
     * @author Mike Harm
     */
    public interface MessageProvider {

        /**
         * Returns a textual message identified by the given messageId, parameterized by the given
         * arguments.
         *
         * @param messageId the identifier of the message
         * @param arguments the arguments to fill into the message
         */
        String getMessage(String messageId, Object[] arguments);
    }

    public static final MessageProvider messageProvider = new DefaultMessageProvider();

    /**
     * @deprecated Use {@link #getMessageById(String messageId, Object... args)} instead
     */
    @Deprecated
    public static String getMessage(String messageId, Object[] arguments) {
        return messageProvider.getMessage(messageId, arguments);
    }

    public static String getMessageById(String messageId, Object... args) {
        return messageProvider.getMessage(messageId, args);
    }

    /* OPT there's a noticable delay for the first error!  Maybe it'd
     * make sense to use a ListResourceBundle instead of a properties
     * file to avoid (synchronized) text parsing.
     */
    private static class DefaultMessageProvider implements MessageProvider {
        @Override
        public String getMessage(String messageId, Object[] arguments) {
            final String defaultResource = "org.mozilla.javascript.resources.Messages";

            Context cx = Context.getCurrentContext();
            Locale locale = cx != null ? cx.getLocale() : Locale.getDefault();

            // ResourceBundle does caching.
            ResourceBundle rb = ResourceBundle.getBundle(defaultResource, locale);

            String formatString;
            try {
                formatString = rb.getString(messageId);
            } catch (java.util.MissingResourceException mre) {
                throw new RuntimeException(
                        "no message resource found for message property " + messageId);
            }

            /*
             * It's OK to format the string, even if 'arguments' is null;
             * we need to format it anyway, to make double ''s collapse to
             * single 's.
             */
            MessageFormat formatter = new MessageFormat(formatString);
            return formatter.format(arguments);
        }
    }

    public static EcmaError constructError(String error, String message) {
        int[] linep = new int[1];
        String filename = Context.getSourcePositionFromStack(linep);
        return constructError(error, message, filename, linep[0], null, 0);
    }

    public static EcmaError constructError(String error, String message, int lineNumberDelta) {
        int[] linep = new int[1];
        String filename = Context.getSourcePositionFromStack(linep);
        if (linep[0] != 0) {
            linep[0] += lineNumberDelta;
        }
        return constructError(error, message, filename, linep[0], null, 0);
    }

    public static EcmaError constructError(
            String error,
            String message,
            String sourceName,
            int lineNumber,
            String lineSource,
            int columnNumber) {
        return new EcmaError(error, message, sourceName, lineNumber, lineSource, columnNumber);
    }

    public static EcmaError rangeError(String message) {
        return constructError("RangeError", message);
    }

    public static EcmaError rangeErrorById(String messageId, Object... args) {
        String msg = getMessageById(messageId, args);
        return rangeError(msg);
    }

    public static EcmaError typeError(String message) {
        return constructError("TypeError", message);
    }

    public static EcmaError typeErrorById(String messageId, Object... args) {
        String msg = getMessageById(messageId, args);
        return typeError(msg);
    }

    /**
     * @deprecated Use {@link #typeErrorById(String messageId, Object... args)} instead
     */
    @Deprecated
    public static EcmaError typeError0(String messageId) {
        String msg = getMessage0(messageId);
        return typeError(msg);
    }

    /**
     * @deprecated Use {@link #typeErrorById(String messageId, Object... args)} instead
     */
    @Deprecated
    public static EcmaError typeError1(String messageId, Object arg1) {
        String msg = getMessage1(messageId, arg1);
        return typeError(msg);
    }

    /**
     * @deprecated Use {@link #typeErrorById(String messageId, Object... args)} instead
     */
    @Deprecated
    public static EcmaError typeError2(String messageId, Object arg1, Object arg2) {
        String msg = getMessage2(messageId, arg1, arg2);
        return typeError(msg);
    }

    /**
     * @deprecated Use {@link #typeErrorById(String messageId, Object... args)} instead
     */
    @Deprecated
    public static EcmaError typeError3(String messageId, String arg1, String arg2, String arg3) {
        String msg = getMessage3(messageId, arg1, arg2, arg3);
        return typeError(msg);
    }

    private static Scriptable asScriptableOrThrowUndefReadError(
            Context cx, Scriptable scope, Object obj, Object elem) {
        Scriptable scriptable = toObjectOrNull(cx, obj, scope);
        if (scriptable == null) {
            throw undefReadError(obj, elem);
        }
        return scriptable;
    }

    private static Scriptable asScriptableOrThrowUndefWriteError(
            Context cx, Scriptable scope, Object obj, Object elem, Object value) {
        Scriptable scriptable = toObjectOrNull(cx, obj, scope);
        if (scriptable == null) {
            throw undefWriteError(obj, elem, value);
        }
        return scriptable;
    }

    private static void verifyIsScriptableOrComplainWriteErrorInEs5Strict(
            Object obj, String property, Object value, Context cx) {
        if (!(obj instanceof Scriptable)
                && cx.isStrictMode()
                && cx.getLanguageVersion() >= Context.VERSION_1_8) {
            throw undefWriteError(obj, property, value);
        }
    }

    public static RuntimeException undefReadError(Object object, Object id) {
        return typeErrorById("msg.undef.prop.read", toString(object), toString(id));
    }

    public static RuntimeException undefCallError(Object object, Object id) {
        return typeErrorById("msg.undef.method.call", toString(object), toString(id));
    }

    public static RuntimeException undefWriteError(Object object, Object id, Object value) {
        return typeErrorById(
                "msg.undef.prop.write", toString(object), toString(id), toString(value));
    }

    private static RuntimeException undefDeleteError(Object object, Object id) {
        throw typeErrorById("msg.undef.prop.delete", toString(object), toString(id));
    }

    public static RuntimeException notFoundError(Scriptable object, String property) {
        // XXX: use object to improve the error message
        String msg = getMessageById("msg.is.not.defined", property);
        throw constructError("ReferenceError", msg);
    }

    public static RuntimeException notFunctionError(Object value) {
        return notFunctionError(value, value);
    }

    public static RuntimeException notFunctionError(Object value, Object messageHelper) {
        // Use value for better error reporting
        String msg = (messageHelper == null) ? "null" : messageHelper.toString();
        if (value == Scriptable.NOT_FOUND) {
            return typeErrorById("msg.function.not.found", msg);
        }
        return typeErrorById("msg.isnt.function", msg, typeof(value));
    }

    public static RuntimeException notFunctionError(Object obj, Object value, String propertyName) {
        // Use obj and value for better error reporting
        String objString = toString(obj);
        if (obj instanceof NativeFunction) {
            // Omit function body in string representations of functions
            int paren = objString.indexOf(')');
            int curly = objString.indexOf('{', paren);
            if (curly > -1) {
                objString = objString.substring(0, curly + 1) + "...}";
            }
        }
        if (value == Scriptable.NOT_FOUND) {
            return typeErrorById("msg.function.not.found.in", propertyName, objString);
        }
        return typeErrorById("msg.isnt.function.in", propertyName, objString, typeof(value));
    }

    private static RuntimeException notXmlError(Object value) {
        throw typeErrorById("msg.isnt.xml.object", toString(value));
    }

    public static EcmaError syntaxError(String message) {
        return constructError("SyntaxError", message);
    }

    public static EcmaError syntaxErrorById(String messageId, Object... args) {
        String msg = getMessageById(messageId, args);
        return syntaxError(msg);
    }

    public static EcmaError referenceError(String message) {
        return constructError("ReferenceError", message);
    }

    private static void warnAboutNonJSObject(Object nonJSObject) {
        final String omitParam = ScriptRuntime.getMessageById("params.omit.non.js.object.warning");
        if (!"true".equals(omitParam)) {
            String message =
                    ScriptRuntime.getMessageById(
                            "msg.non.js.object.warning",
                            nonJSObject,
                            nonJSObject.getClass().getName());
            Context.reportWarning(message);
            // Just to be sure that it would be noticed
            System.err.println(message);
        }
    }

    public static RegExpProxy getRegExpProxy(Context cx) {
        return cx.getRegExpProxy();
    }

    public static void setRegExpProxy(Context cx, RegExpProxy proxy) {
        if (proxy == null) throw new IllegalArgumentException();
        cx.regExpProxy = proxy;
    }

    public static RegExpProxy checkRegExpProxy(Context cx) {
        RegExpProxy result = getRegExpProxy(cx);
        if (result == null) {
            throw Context.reportRuntimeErrorById("msg.no.regexp");
        }
        return result;
    }

    public static Scriptable wrapRegExp(Context cx, Scriptable scope, Object compiled) {
        return cx.getRegExpProxy().wrapRegExp(cx, scope, compiled);
    }

    public static Scriptable getTemplateLiteralCallSite(
            Context cx, Scriptable scope, Object[] strings, int index) {
        Object callsite = strings[index];

        if (callsite instanceof Scriptable) return (Scriptable) callsite;

        assert callsite instanceof String[];
        String[] vals = (String[]) callsite;
        assert (vals.length & 1) == 0;

        ScriptableObject siteObj = (ScriptableObject) cx.newArray(scope, vals.length >>> 1);
        ScriptableObject rawObj = (ScriptableObject) cx.newArray(scope, vals.length >>> 1);

        siteObj.put("raw", siteObj, rawObj);
        siteObj.setAttributes("raw", ScriptableObject.DONTENUM);

        for (int i = 0, n = vals.length; i < n; i += 2) {
            int idx = i >>> 1;
            siteObj.put(idx, siteObj, (vals[i] == null ? Undefined.instance : vals[i]));

            rawObj.put(idx, rawObj, vals[i + 1]);
        }

        AbstractEcmaObjectOperations.setIntegrityLevel(
                cx, rawObj, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.FROZEN);
        AbstractEcmaObjectOperations.setIntegrityLevel(
                cx, siteObj, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.FROZEN);

        strings[index] = siteObj;

        return siteObj;
    }

    private static XMLLib currentXMLLib(Context cx) {
        // Scripts should be running to access this
        if (cx.topCallScope == null) throw new IllegalStateException();

        XMLLib xmlLib = cx.cachedXMLLib;
        if (xmlLib == null) {
            xmlLib = XMLLib.extractFromScope(cx.topCallScope);
            if (xmlLib == null) throw new IllegalStateException();
            cx.cachedXMLLib = xmlLib;
        }

        return xmlLib;
    }

    /**
     * Escapes the reserved characters in a value of an attribute
     *
     * @param value Unescaped text
     * @return The escaped text
     */
    public static String escapeAttributeValue(Object value, Context cx) {
        XMLLib xmlLib = currentXMLLib(cx);
        return xmlLib.escapeAttributeValue(value);
    }

    /**
     * Escapes the reserved characters in a value of a text node
     *
     * @param value Unescaped text
     * @return The escaped text
     */
    public static String escapeTextValue(Object value, Context cx) {
        XMLLib xmlLib = currentXMLLib(cx);
        return xmlLib.escapeTextValue(value);
    }

    public static Ref memberRef(Object obj, Object elem, Context cx, int memberTypeFlags) {
        if (!(obj instanceof XMLObject)) {
            throw notXmlError(obj);
        }
        XMLObject xmlObject = (XMLObject) obj;
        return xmlObject.memberRef(cx, elem, memberTypeFlags);
    }

    public static Ref memberRef(
            Object obj, Object namespace, Object elem, Context cx, int memberTypeFlags) {
        if (!(obj instanceof XMLObject)) {
            throw notXmlError(obj);
        }
        XMLObject xmlObject = (XMLObject) obj;
        return xmlObject.memberRef(cx, namespace, elem, memberTypeFlags);
    }

    public static Ref nameRef(Object name, Context cx, Scriptable scope, int memberTypeFlags) {
        XMLLib xmlLib = currentXMLLib(cx);
        return xmlLib.nameRef(cx, name, scope, memberTypeFlags);
    }

    public static Ref nameRef(
            Object namespace, Object name, Context cx, Scriptable scope, int memberTypeFlags) {
        XMLLib xmlLib = currentXMLLib(cx);
        return xmlLib.nameRef(cx, namespace, name, scope, memberTypeFlags);
    }

    public static void storeUint32Result(Context cx, long value) {
        if ((value >>> 32) != 0) throw new IllegalArgumentException();
        cx.scratchUint32 = value;
    }

    public static long lastUint32Result(Context cx) {
        long value = cx.scratchUint32;
        if ((value >>> 32) != 0) throw new IllegalStateException();
        return value;
    }

    private static void storeScriptable(Context cx, Scriptable value) {
        // The previously stored scratchScriptable should be consumed
        if (cx.scratchScriptable != null) throw new IllegalStateException();
        cx.scratchScriptable = value;
    }

    public static Scriptable lastStoredScriptable(Context cx) {
        Scriptable result = cx.scratchScriptable;
        cx.scratchScriptable = null;
        return result;
    }

    public static void discardLastStoredScriptable(Context cx) {
        if (cx.scratchScriptable == null) throw new IllegalStateException();
        cx.scratchScriptable = null;
    }

    static String makeUrlForGeneratedScript(
            boolean isEval, String masterScriptUrl, int masterScriptLine) {
        if (isEval) {
            return masterScriptUrl + '#' + masterScriptLine + "(eval)";
        }
        return masterScriptUrl + '#' + masterScriptLine + "(Function)";
    }

    static boolean isGeneratedScript(String sourceUrl) {
        // ALERT: this may clash with a valid URL containing (eval) or
        // (Function)
        return sourceUrl.indexOf("(eval)") >= 0 || sourceUrl.indexOf("(Function)") >= 0;
    }

    /**
     * Not all "NativeSymbol" instances are actually symbols. So account for that here rather than
     * just by using an "instanceof" check.
     */
    static boolean isSymbol(Object obj) {
        return ((obj instanceof NativeSymbol) && ((NativeSymbol) obj).isSymbol())
                || (obj instanceof SymbolKey);
    }

    /**
     * Return that the symbol was created by the constructor, or is a built-in Symbol, and was not
     * put in the registry using "for".
     */
    static boolean isUnregisteredSymbol(Object obj) {
        if (obj instanceof NativeSymbol) {
            NativeSymbol ns = (NativeSymbol) obj;
            return ns.isSymbol() && ns.getKind() != NativeSymbol.SymbolKind.REGISTERED;
        }
        return (obj instanceof SymbolKey);
    }

    private static RuntimeException errorWithClassName(String msg, Object val) {
        return Context.reportRuntimeErrorById(msg, val.getClass().getName());
    }

    /**
     * Equivalent to executing "new Error(message, sourceFileName, sourceLineNo)" from JavaScript.
     *
     * @param cx the current context
     * @param scope the current scope
     * @param message the message
     * @return a JavaScriptException you should throw
     */
    public static JavaScriptException throwError(Context cx, Scriptable scope, String message) {
        int[] linep = {0};
        String filename = Context.getSourcePositionFromStack(linep);
        final Scriptable error =
                newBuiltinObject(
                        cx,
                        scope,
                        TopLevel.Builtins.Error,
                        new Object[] {message, filename, Integer.valueOf(linep[0])});
        return new JavaScriptException(error, filename, linep[0]);
    }

    /**
     * Equivalent to executing "new $constructorName(message, sourceFileName, sourceLineNo)" from
     * JavaScript.
     *
     * @param cx the current context
     * @param scope the current scope
     * @param message the message
     * @return a JavaScriptException you should throw
     */
    public static JavaScriptException throwCustomError(
            Context cx, Scriptable scope, String constructorName, String message) {
        int[] linep = {0};
        String filename = Context.getSourcePositionFromStack(linep);
        final Scriptable error =
                cx.newObject(
                        scope,
                        constructorName,
                        new Object[] {message, filename, Integer.valueOf(linep[0])});
        return new JavaScriptException(error, filename, linep[0]);
    }

    /** Throws a ReferenceError "cannot delete a super property". See ECMAScript spec 13.5.1.2 */
    public static void throwDeleteOnSuperPropertyNotAllowed() {
        throw referenceError("msg.delete.super");
    }

    /**
     * Load a single implementation of "serviceClass" using the ServiceLoader. If there are no
     * implementations, return null. If there is more than one implementation, throw a fatal
     * exception, since this indicates that the classpath was configured incorrectly.
     */
    static <T> T loadOneServiceImplementation(Class<T> serviceClass) {
        Iterator<T> it = ServiceLoader.load(serviceClass).iterator();
        if (it.hasNext()) {
            T result = it.next();
            if (it.hasNext()) {
                throw Kit.codeBug(
                        "Invalid configuration: more than one implementation of " + serviceClass);
            }
            return result;
        }
        return null;
    }

    public static final Object[] emptyArgs = new Object[0];
    public static final String[] emptyStrings = new String[0];

    static final XMLLoader xmlLoaderImpl =
            ScriptRuntime.loadOneServiceImplementation(XMLLoader.class);
}
