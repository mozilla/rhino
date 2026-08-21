package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link ScriptRuntime#sameZero(Object, Object)}, the SameValueZero comparison used by
 * Array.prototype.includes, TypedArray.prototype.includes, and Map/Set key discrimination. As with
 * {@link ScriptRuntime#same(Object, Object)}, JS numbers may be boxed as different {@link Number}
 * subtypes (or as {@link BigInteger}), so the comparison must be by numeric value rather than by
 * {@link Object#equals(Object)}. The only difference from SameValue is that +0 and -0 compare
 * equal, across all subtypes.
 */
public class ScriptRuntimeSameZeroTest {

    private static final double NEG_DOUBLE_NAN = Double.longBitsToDouble(0xFFF8000000000000L);
    private static final float NEG_FLOAT_NAN = Float.intBitsToFloat(0xFFC00000);

    private static Stream<Arguments> pairs() {
        NativeObject sharedObject = new NativeObject();
        SymbolKey sharedSymbol = new SymbolKey(null, Symbol.Kind.REGULAR);
        return Stream.of(
                // All NaNs are the same value, regardless of sign or subtype.
                Arguments.of(Double.NaN, Double.NaN, true),
                Arguments.of(NEG_DOUBLE_NAN, Double.NaN, true),
                Arguments.of(Float.NaN, Double.NaN, true),
                Arguments.of(Float.NaN, NEG_FLOAT_NAN, true),
                Arguments.of(Double.NaN, 0.0, false),
                Arguments.of(Double.NaN, -0.0, false),
                Arguments.of(Double.NaN, 1.0, false),
                Arguments.of(Double.NaN, Double.POSITIVE_INFINITY, false),
                Arguments.of(Float.NaN, 0.0f, false),
                // Infinities compare by value and sign.
                Arguments.of(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, true),
                Arguments.of(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, true),
                Arguments.of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, false),
                Arguments.of(Float.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, true),
                Arguments.of(Float.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, true),
                Arguments.of(Double.POSITIVE_INFINITY, Double.MAX_VALUE, false),
                Arguments.of(Double.NEGATIVE_INFINITY, Double.MIN_VALUE, false),
                // SameValueZero treats +0 and -0 as equal, across all Number subtypes.
                Arguments.of(0.0, 0.0, true),
                Arguments.of(-0.0, -0.0, true),
                Arguments.of(0.0, -0.0, true),
                Arguments.of(-0.0, 0.0, true),
                Arguments.of(0.0, 0, true),
                Arguments.of(-0.0, 0, true),
                Arguments.of(0.0, 0L, true),
                Arguments.of(-0.0, 0L, true),
                Arguments.of(0.0, (byte) 0, true),
                Arguments.of(-0.0, (byte) 0, true),
                Arguments.of(0.0, (short) 0, true),
                Arguments.of(-0.0, (short) 0, true),
                Arguments.of(0.0, 0.0f, true),
                Arguments.of(-0.0, 0.0f, true),
                Arguments.of(0.0f, -0.0f, true),
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
                Arguments.of(5.5, 5, false),
                Arguments.of(0.5, 0, false),
                Arguments.of(5.0, 6, false),
                Arguments.of(1.1f, 1.1, false),
                // 2^53 + 1 is not representable as a double; it rounds to 2^53. The next
                // representable double after 2^53 is 2^53 + 2.
                Arguments.of(9007199254740993L, 9007199254740992.0, true),
                Arguments.of(9007199254740993L, 9007199254740994.0, false),
                Arguments.of(1e308, Long.MAX_VALUE, false),
                // Big integers compare by value, and never equal regular numbers.
                Arguments.of(BigInteger.ONE, BigInteger.ONE, true),
                Arguments.of(BigInteger.ZERO, BigInteger.ZERO, true),
                Arguments.of(BigInteger.valueOf(-5), new BigInteger("-5"), true),
                Arguments.of(BigInteger.TEN.pow(100), BigInteger.TEN.pow(100), true),
                Arguments.of(BigInteger.ONE, BigInteger.TEN, false),
                Arguments.of(
                        BigInteger.valueOf(2).pow(100),
                        BigInteger.valueOf(2).pow(100).add(BigInteger.ONE),
                        false),
                Arguments.of(BigInteger.ONE, 1.0, false),
                Arguments.of(BigInteger.ONE, 1, false),
                Arguments.of(BigInteger.ZERO, 0.0, false),
                Arguments.of(BigInteger.ZERO, -0.0, false),
                Arguments.of(BigInteger.valueOf(2), 2.0, false),
                Arguments.of(BigInteger.ONE, null, false),
                Arguments.of(BigInteger.ONE, Undefined.instance, false),
                // Other primitive types keep their usual equality rules.
                Arguments.of("abc", "abc", true),
                Arguments.of("abc", "abd", false),
                Arguments.of("ab", "ba", false),
                Arguments.of("1", 1.0, false),
                Arguments.of(new StringBuilder("abc"), "abc", true),
                Arguments.of(new StringBuilder("ab"), "abc", false),
                Arguments.of(Boolean.TRUE, Boolean.TRUE, true),
                Arguments.of(Boolean.FALSE, Boolean.FALSE, true),
                Arguments.of(Boolean.TRUE, Boolean.FALSE, false),
                Arguments.of(Boolean.TRUE, "true", false),
                Arguments.of(Boolean.TRUE, 1.0, false),
                Arguments.of(null, null, true),
                Arguments.of(null, Undefined.instance, false),
                Arguments.of(null, 1.0, false),
                Arguments.of(Undefined.instance, Undefined.instance, true),
                Arguments.of(Undefined.instance, 1.0, false),
                // Values of different types never compare equal.
                Arguments.of(1.0, new NativeObject(), false),
                Arguments.of(BigInteger.ONE, "1", false),
                Arguments.of(BigInteger.ONE, Boolean.TRUE, false),
                // Objects and symbols compare by identity.
                Arguments.of(sharedObject, sharedObject, true),
                Arguments.of(new NativeObject(), new NativeObject(), false),
                Arguments.of(sharedSymbol, sharedSymbol, true),
                Arguments.of(sharedSymbol, new SymbolKey(null, Symbol.Kind.REGULAR), false),
                Arguments.of(sharedSymbol, "a", false),
                Arguments.of(sharedSymbol, null, false));
    }

    @ParameterizedTest(name = "sameZero({0}, {1}) == {2}")
    @MethodSource("pairs")
    void sameZero(Object x, Object y, boolean expected) {
        try (var ignored = Context.enter()) {
            assertEquals(
                    expected,
                    ScriptRuntime.sameZero(x, y),
                    () -> "sameZero(" + describe(x) + ", " + describe(y) + ")");
            assertEquals(
                    expected,
                    ScriptRuntime.sameZero(y, x),
                    () -> "sameZero(" + describe(y) + ", " + describe(x) + ")");
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
