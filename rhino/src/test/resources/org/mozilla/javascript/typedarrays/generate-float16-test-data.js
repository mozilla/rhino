// Script to generate Float16 test data using Node.js
//
// REQUIREMENTS: Node.js v21.0.0 or later (Float16 support added in v21)
//
// Run with: node generate-float16-test-data.js > TestData.java
//
// Note: This script is provided for reference and future validation.
// The actual tests use hardcoded IEEE 754 reference values in
// NodeJsFloat16ReferenceTest.java

const testValues = [
    // Basic values
    0.0, -0.0, 1.0, -1.0, 2.0, -2.0,
    0.5, -0.5, 0.25, -0.25,

    // Special values
    Infinity, -Infinity, NaN,

    // Max and overflow
    65504.0,   // Max Float16
    65505.0,   // Just over max
    100000.0,  // Clear overflow
    -65504.0,
    -100000.0,

    // Powers of two
    0.0009765625,  // 2^-10
    0.00048828125, // 2^-11
    0.000244140625, // 2^-12
    0.0001220703125, // 2^-13
    0.00006103515625, // 2^-14 (min normal)
    0.000030517578125, // 2^-15

    // Denormalized range
    0.000000059604644775390625, // 2^-24 (min subnormal)
    0.00000001, // Very small
    0.0000001,
    0.000001,

    // Common values
    Math.PI, -Math.PI,
    Math.E, -Math.E,
    10.0, -10.0,
    100.0, -100.0,
    1000.0, -1000.0,

    // Fractions
    0.1, -0.1,
    0.01, -0.01,
    0.001, -0.001,
    0.0001, -0.0001,

    // Values near boundaries
    65503.0, 65503.5, 65504.0,

    // Rounding test values
    1.5, 2.5, 3.5,
    1.0001, 1.001, 1.01, 1.1,
];

console.log("// Generated Float16 test data from Node.js");
console.log("// Node version:", process.version);
console.log();

function toHex(buffer) {
    return "0x" + Array.from(new Uint8Array(buffer))
        .map(b => b.toString(16).padStart(2, '0'))
        .join('');
}

function float16ToHex(value, littleEndian = true) {
    const buffer = new ArrayBuffer(2);
    const view = new DataView(buffer);
    view.setFloat16(0, value, littleEndian);
    return toHex(buffer);
}

function testFloat16(value, littleEndian = true) {
    const buffer = new ArrayBuffer(2);
    const view = new DataView(buffer);

    try {
        view.setFloat16(0, value, littleEndian);
        const result = view.getFloat16(0, littleEndian);

        const hex = toHex(buffer);

        return {
            input: value,
            output: result,
            hex: hex,
            bytes: Array.from(new Uint8Array(buffer)),
            isNaN: Number.isNaN(result),
            isInfinite: !Number.isFinite(result) && !Number.isNaN(result),
            sign: Math.sign(result)
        };
    } catch (e) {
        return {
            input: value,
            error: e.message
        };
    }
}

console.log("public class NodeJsFloat16TestData {");
console.log("    // Test data generated from Node.js " + process.version);
console.log();
console.log("    public static class TestCase {");
console.log("        public final double input;");
console.log("        public final float expectedOutput;");
console.log("        public final int expectedBits;");
console.log("        public final boolean isNaN;");
console.log("        public final boolean isInfinite;");
console.log();
console.log("        public TestCase(double input, float expectedOutput, int expectedBits, boolean isNaN, boolean isInfinite) {");
console.log("            this.input = input;");
console.log("            this.expectedOutput = expectedOutput;");
console.log("            this.expectedBits = expectedBits;");
console.log("            this.isNaN = isNaN;");
console.log("            this.isInfinite = isInfinite;");
console.log("        }");
console.log("    }");
console.log();
console.log("    public static final TestCase[] LITTLE_ENDIAN_CASES = {");

const results = [];
for (const value of testValues) {
    const result = testFloat16(value, true);
    if (!result.error) {
        results.push(result);

        const inputStr = Number.isNaN(value) ? "Double.NaN" :
                        value === Infinity ? "Double.POSITIVE_INFINITY" :
                        value === -Infinity ? "Double.NEGATIVE_INFINITY" :
                        value.toString();

        const outputStr = Number.isNaN(result.output) ? "Float.NaN" :
                         result.output === Infinity ? "Float.POSITIVE_INFINITY" :
                         result.output === -Infinity ? "Float.NEGATIVE_INFINITY" :
                         result.output + "f";

        const bitsLittle = (result.bytes[0] & 0xff) | ((result.bytes[1] & 0xff) << 8);

        console.log(`        new TestCase(${inputStr}, ${outputStr}, ${result.hex}, ${result.isNaN}, ${result.isInfinite}), // ${value}`);
    }
}

console.log("    };");
console.log();
console.log("    public static final TestCase[] BIG_ENDIAN_CASES = {");

for (const value of testValues) {
    const result = testFloat16(value, false);
    if (!result.error) {
        const inputStr = Number.isNaN(value) ? "Double.NaN" :
                        value === Infinity ? "Double.POSITIVE_INFINITY" :
                        value === -Infinity ? "Double.NEGATIVE_INFINITY" :
                        value.toString();

        const outputStr = Number.isNaN(result.output) ? "Float.NaN" :
                         result.output === Infinity ? "Float.POSITIVE_INFINITY" :
                         result.output === -Infinity ? "Float.NEGATIVE_INFINITY" :
                         result.output + "f";

        console.log(`        new TestCase(${inputStr}, ${outputStr}, ${result.hex}, ${result.isNaN}, ${result.isInfinite}), // ${value}`);
    }
}

console.log("    };");
console.log("}");
