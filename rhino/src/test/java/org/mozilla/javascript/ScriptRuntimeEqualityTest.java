package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link ScriptRuntime#eq(Object, Object)} (the {@code ==} operator, IsLooselyEqual) and
 * {@link ScriptRuntime#shallowEq(Object, Object)} (the {@code ===} operator, IsStrictlyEqual). JS
 * numbers may be boxed as different {@link Number} subtypes (or as {@link BigInteger}), so both
 * comparisons must be by numeric value rather than by {@link Object#equals(Object)}. They differ
 * only in cross-type behavior: eq applies abstract-equality conversions (e.g. {@code 1 == "1"},
 * {@code null == undefined}, finite BigInt/Number mathematical-value equality) while shallowEq
 * requires the same type.
 */
public class ScriptRuntimeEqualityTest {

    private static final double NEG_DOUBLE_NAN = Double.longBitsToDouble(0xFFF8000000000000L);
    private static final float NEG_FLOAT_NAN = Float.intBitsToFloat(0xFFC00000);

    private static Stream<Arguments> eqPairs() {
        NativeObject sharedObject = new NativeObject();
        SymbolKey sharedSymbol = new SymbolKey(null, Symbol.Kind.REGULAR);
        return Stream.of(
                // Numbers of different subtypes compare by numeric value.
                Arguments.of(5.0, 5, true),
                Arguments.of(5.0, 5L, true),
                Arguments.of(5.0, 5.0f, true),
                Arguments.of(5.0, (short) 5, true),
                Arguments.of(5.0, (byte) 5, true),
                Arguments.of(5, 5L, true),
                Arguments.of(5, (short) 5, true),
                Arguments.of((short) 5, (byte) 5, true),
                Arguments.of(Integer.MAX_VALUE, (double) Integer.MAX_VALUE, true),
                Arguments.of(Integer.MIN_VALUE, (double) Integer.MIN_VALUE, true),
                Arguments.of(1L << 60, (double) (1L << 60), true),
                Arguments.of(1L << 53, (double) (1L << 53), true),
                // Long.MAX_VALUE is not exactly representable as a double, but both sides round
                // to the same Number value.
                Arguments.of(Long.MAX_VALUE, (double) Long.MAX_VALUE, true),
                // 2^53 + 1 is not representable as a double; it rounds to 2^53.
                Arguments.of(9007199254740993L, 9007199254740992.0, true),
                Arguments.of(9007199254740993L, 9007199254740994.0, false),
                Arguments.of(5.5, 5, false),
                Arguments.of(0.5, 0, false),
                Arguments.of(5.0, 6, false),
                Arguments.of(1.1f, 1.1, false),
                Arguments.of(1e308, Long.MAX_VALUE, false),
                // NaN is never loosely equal to anything, regardless of sign or subtype.
                Arguments.of(Double.NaN, Double.NaN, false),
                Arguments.of(NEG_DOUBLE_NAN, Double.NaN, false),
                Arguments.of(Float.NaN, Double.NaN, false),
                Arguments.of(Float.NaN, NEG_FLOAT_NAN, false),
                Arguments.of(Double.NaN, 0.0, false),
                Arguments.of(Double.NaN, 1.0, false),
                // +0 and -0 are equal, across all Number subtypes.
                Arguments.of(0.0, 0.0, true),
                Arguments.of(-0.0, -0.0, true),
                Arguments.of(0.0, -0.0, true),
                Arguments.of(-0.0, 0, true),
                Arguments.of(-0.0, 0L, true),
                Arguments.of(-0.0, (byte) 0, true),
                Arguments.of(-0.0, (short) 0, true),
                Arguments.of(-0.0, 0.0f, true),
                Arguments.of(0.0f, -0.0f, true),
                // Infinities compare by value and sign.
                Arguments.of(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, true),
                Arguments.of(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, true),
                Arguments.of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, false),
                Arguments.of(Float.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, true),
                Arguments.of(Double.POSITIVE_INFINITY, Double.MAX_VALUE, false),
                // Big integers compare by value against each other.
                Arguments.of(BigInteger.ONE, BigInteger.ONE, true),
                Arguments.of(BigInteger.ZERO, BigInteger.ZERO, true),
                Arguments.of(BigInteger.valueOf(-5), new BigInteger("-5"), true),
                Arguments.of(BigInteger.TEN.pow(100), BigInteger.TEN.pow(100), true),
                Arguments.of(BigInteger.ONE, BigInteger.TEN, false),
                // Finite BigInt/Number pairs compare by mathematical value.
                Arguments.of(BigInteger.ONE, 1.0, true),
                Arguments.of(BigInteger.ONE, 1, true),
                Arguments.of(BigInteger.ZERO, -0.0, true),
                Arguments.of(BigInteger.valueOf(2), 2.0f, true),
                Arguments.of(BigInteger.valueOf(9007199254740992L), 9007199254740992.0, true),
                Arguments.of(BigInteger.valueOf(9007199254740993L), 9007199254740992.0, false),
                Arguments.of(BigInteger.ONE, 0.5, false),
                Arguments.of(BigInteger.ONE, Double.POSITIVE_INFINITY, false),
                Arguments.of(BigInteger.ONE, Double.NEGATIVE_INFINITY, false),
                Arguments.of(BigInteger.ONE, Double.NaN, false),
                // Abstract equality conversions between other types.
                Arguments.of("1", 1.0, true),
                Arguments.of("abc", "abc", true),
                Arguments.of("abc", "abd", false),
                Arguments.of("ab", "ba", false),
                Arguments.of(new StringBuilder("abc"), "abc", true),
                Arguments.of(Boolean.TRUE, 1.0, true),
                Arguments.of(Boolean.FALSE, 0.0, true),
                Arguments.of(Boolean.TRUE, Boolean.TRUE, true),
                Arguments.of(Boolean.FALSE, Boolean.FALSE, true),
                Arguments.of(Boolean.TRUE, Boolean.FALSE, false),
                Arguments.of(Boolean.TRUE, "true", false),
                Arguments.of(null, null, true),
                Arguments.of(Undefined.instance, Undefined.instance, true),
                Arguments.of(null, Undefined.instance, true),
                Arguments.of(null, 0.0, false),
                Arguments.of(Undefined.instance, 1.0, false),
                Arguments.of(BigInteger.ONE, "1", true),
                Arguments.of(BigInteger.ONE, Boolean.TRUE, true),
                Arguments.of(BigInteger.ONE, null, false),
                Arguments.of(BigInteger.ONE, Undefined.instance, false),
                // Objects and symbols compare by identity.
                Arguments.of(sharedObject, sharedObject, true),
                Arguments.of(new NativeObject(), new NativeObject(), false),
                Arguments.of(sharedSymbol, sharedSymbol, true),
                Arguments.of(sharedSymbol, new SymbolKey(null, Symbol.Kind.REGULAR), false),
                Arguments.of(sharedSymbol, "a", false),
                Arguments.of(sharedSymbol, null, false));
    }

    @ParameterizedTest(name = "eq({0}, {1}) == {2}")
    @MethodSource("eqPairs")
    void eq(Object x, Object y, boolean expected) {
        try (var ignored = Context.enter()) {
            assertEquals(
                    expected,
                    ScriptRuntime.eq(x, y),
                    () -> "eq(" + describe(x) + ", " + describe(y) + ")");
            assertEquals(
                    expected,
                    ScriptRuntime.eq(y, x),
                    () -> "eq(" + describe(y) + ", " + describe(x) + ")");
        }
    }

    private static Stream<Arguments> shallowEqPairs() {
        NativeObject sharedObject = new NativeObject();
        SymbolKey sharedSymbol = new SymbolKey(null, Symbol.Kind.REGULAR);
        return Stream.of(
                // Numbers of different subtypes compare by numeric value.
                Arguments.of(5.0, 5, true),
                Arguments.of(5.0, 5L, true),
                Arguments.of(5.0, 5.0f, true),
                Arguments.of(5.0, (short) 5, true),
                Arguments.of(5.0, (byte) 5, true),
                Arguments.of(5, 5L, true),
                Arguments.of(5, (short) 5, true),
                Arguments.of((short) 5, (byte) 5, true),
                Arguments.of(Integer.MAX_VALUE, (double) Integer.MAX_VALUE, true),
                Arguments.of(Integer.MIN_VALUE, (double) Integer.MIN_VALUE, true),
                Arguments.of(1L << 60, (double) (1L << 60), true),
                Arguments.of(1L << 53, (double) (1L << 53), true),
                Arguments.of(Long.MAX_VALUE, (double) Long.MAX_VALUE, true),
                Arguments.of(9007199254740993L, 9007199254740992.0, true),
                Arguments.of(9007199254740993L, 9007199254740994.0, false),
                Arguments.of(5.5, 5, false),
                Arguments.of(0.5, 0, false),
                Arguments.of(5.0, 6, false),
                Arguments.of(1.1f, 1.1, false),
                Arguments.of(1e308, Long.MAX_VALUE, false),
                // NaN is never strictly equal to anything, regardless of sign or subtype.
                Arguments.of(Double.NaN, Double.NaN, false),
                Arguments.of(NEG_DOUBLE_NAN, Double.NaN, false),
                Arguments.of(Float.NaN, Double.NaN, false),
                Arguments.of(Float.NaN, NEG_FLOAT_NAN, false),
                Arguments.of(Double.NaN, 0.0, false),
                // +0 and -0 are equal, across all Number subtypes.
                Arguments.of(0.0, 0.0, true),
                Arguments.of(-0.0, -0.0, true),
                Arguments.of(0.0, -0.0, true),
                Arguments.of(-0.0, 0, true),
                Arguments.of(-0.0, 0L, true),
                Arguments.of(-0.0, (byte) 0, true),
                Arguments.of(-0.0, (short) 0, true),
                Arguments.of(-0.0, 0.0f, true),
                Arguments.of(0.0f, -0.0f, true),
                // Infinities compare by value and sign.
                Arguments.of(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, true),
                Arguments.of(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, true),
                Arguments.of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, false),
                Arguments.of(Float.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, true),
                Arguments.of(Double.POSITIVE_INFINITY, Double.MAX_VALUE, false),
                // Big integers compare by value against each other only.
                Arguments.of(BigInteger.ONE, BigInteger.ONE, true),
                Arguments.of(BigInteger.ZERO, BigInteger.ZERO, true),
                Arguments.of(BigInteger.valueOf(-5), new BigInteger("-5"), true),
                Arguments.of(BigInteger.TEN.pow(100), BigInteger.TEN.pow(100), true),
                Arguments.of(BigInteger.ONE, BigInteger.TEN, false),
                Arguments.of(BigInteger.ONE, 1.0, false),
                Arguments.of(BigInteger.ONE, 1, false),
                Arguments.of(BigInteger.ZERO, -0.0, false),
                Arguments.of(BigInteger.ONE, "1", false),
                Arguments.of(BigInteger.ONE, Boolean.TRUE, false),
                Arguments.of(BigInteger.ONE, null, false),
                Arguments.of(BigInteger.ONE, Undefined.instance, false),
                // Other primitive types keep their strict equality rules.
                Arguments.of("abc", "abc", true),
                Arguments.of("abc", "abd", false),
                Arguments.of("ab", "ba", false),
                Arguments.of("1", 1.0, false),
                Arguments.of(new StringBuilder("abc"), "abc", true),
                Arguments.of(Boolean.TRUE, Boolean.TRUE, true),
                Arguments.of(Boolean.FALSE, Boolean.FALSE, true),
                Arguments.of(Boolean.TRUE, Boolean.FALSE, false),
                Arguments.of(Boolean.TRUE, "true", false),
                Arguments.of(Boolean.TRUE, 1.0, false),
                Arguments.of(null, null, true),
                Arguments.of(Undefined.instance, Undefined.instance, true),
                Arguments.of(null, Undefined.instance, false),
                Arguments.of(null, 0.0, false),
                Arguments.of(Undefined.instance, 1.0, false),
                // Objects and symbols compare by identity.
                Arguments.of(sharedObject, sharedObject, true),
                Arguments.of(new NativeObject(), new NativeObject(), false),
                Arguments.of(sharedSymbol, sharedSymbol, true),
                Arguments.of(sharedSymbol, new SymbolKey(null, Symbol.Kind.REGULAR), false),
                Arguments.of(sharedSymbol, "a", false),
                Arguments.of(sharedSymbol, null, false));
    }

    /**
     * Loose equality converts objects via ToPrimitive; {@code {} == 0} is false because
     * ToPrimitive({}) is "[object Object]", whose numeric value is NaN. Requires a real object with
     * a prototype chain, hence the standard objects.
     */
    @Test
    void eqObjectCoercion() {
        try (var ignored = Context.enter()) {
            Context cx = Context.getCurrentContext();
            TopLevel scope = cx.initSafeStandardObjects();
            Scriptable obj = cx.newObject(scope);
            assertFalse(ScriptRuntime.eq(obj, 0.0));
            assertFalse(ScriptRuntime.eq(0.0, obj));
            assertTrue(ScriptRuntime.eq(obj, obj));
            assertFalse(ScriptRuntime.shallowEq(obj, 0.0));
            assertFalse(ScriptRuntime.shallowEq(0.0, obj));
        }
    }

    @ParameterizedTest(name = "shallowEq({0}, {1}) == {2}")
    @MethodSource("shallowEqPairs")
    void shallowEq(Object x, Object y, boolean expected) {
        try (var ignored = Context.enter()) {
            assertEquals(
                    expected,
                    ScriptRuntime.shallowEq(x, y),
                    () -> "shallowEq(" + describe(x) + ", " + describe(y) + ")");
            assertEquals(
                    expected,
                    ScriptRuntime.shallowEq(y, x),
                    () -> "shallowEq(" + describe(y) + ", " + describe(x) + ")");
        }
    }

    private static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        if (value == Undefined.instance) {
            return "undefined";
        }
        return value.getClass().getSimpleName() + "(" + value + ")";
    }
}
