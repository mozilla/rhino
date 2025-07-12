load("testsrc/assert.js");

// These tests deliberately exercise the optimizations in ConstAwareLinker.

// Verify that things that aren't constant aren't constant
var notConstant = 1;
assertEquals(1, notConstant);
notConstant = 2;
assertEquals(2, notConstant);

// Verify that things that are constant stay constant
const constant = 1;
assertEquals(1, constant);
constant = 2;
assertEquals(1, constant);

// Verify that this works in a loop
function checkConstantness() {
  constant++;
  assertEquals(1, constant);
}
const ITERATIONS = 10;
for (let i = 0; i < ITERATIONS; i++) {
  checkConstantness();
}

// Verify that we can set a local constant in a function
function localConstantness() {
  const localConst = 1;
  assertEquals(1, localConst);
  localConst = 2;
  assertEquals(1, localConst);
}
for (let i = 0; i < ITERATIONS; i++) {
   localConstantness();
}

// Set up an object with a const field and try it out
const o = {
  notConst: 1,
};
Object.defineProperty(o, "const", {
  value: 1,
  configurable: false,
  writable: false,
});
assertEquals(1, o.notConst);
assertEquals(1, o.const);
o.notConst = 2;
o.const = 2;
assertEquals(2, o.notConst);
assertEquals(1, o.const);

// Verify that it works in a function
function objectConstness() {
  o.const++;
  assertEquals(1, o.const);
}
for (let i = 0; i < ITERATIONS; i++) {
   objectConstness();
}

/* Test we can actually have a lot of consts in a function. */
function manyConsts() {
    const a0 = 0;
    const a1 = 1;
    const a2 = 2;
    const a3 = 3;
    const a4 = 4;
    const a5 = 5;
    const a6 = 6;
    const a7 = 7;
    const a8 = 8;
    const a9 = 9;
    const a10 = 10;
    const a11 = 11;
    const a12 = 12;
    const a13 = 13;
    const a14 = 14;
    const a15 = 15;
    const a16 = 16;
    const a17 = 17;
    const a18 = 18;
    const a19 = 19;
    const a20 = 20;
    const a21 = 21;
    const a22 = 22;
    const a23 = 23;
    const a24 = 24;
    const a25 = 25;
    const a26 = 26;
    const a27 = 27;
    const a28 = 28;
    const a29 = 29;
    const a30 = 30;
    const a31 = 31;
    const a32 = 32;
    const a33 = 33;
    const a34 = 34;
    const a35 = 35;
    const a36 = 36;
    const a37 = 37;
    const a38 = 38;
    const a39 = 39;
    const a40 = 40;
    const a41 = 41;
    const a42 = 42;
    const a43 = 43;
    const a44 = 44;
    const a45 = 45;
    const a46 = 46;
    const a47 = 47;
    const a48 = 48;
    const a49 = 49;
    const a50 = 50;
    const a51 = 51;
    const a52 = 52;
    const a53 = 53;
    const a54 = 54;
    const a55 = 55;
    const a56 = 56;
    const a57 = 57;
    const a58 = 58;
    const a59 = 59;
    const a60 = 60;
    const a61 = 61;
    const a62 = 62;
    const a63 = 63;
    const a64 = 64;
    const a65 = 65;
    const a66 = 66;
    const a67 = 67;
    const a68 = 68;
    const a69 = 69;
    const a70 = 70;
    const a71 = 71;
    const a72 = 72;
    const a73 = 73;
    const a74 = 74;
    const a75 = 75;
    const a76 = 76;
    const a77 = 77;
    const a78 = 78;
    const a79 = 79;
    const a80 = 80;
    const a81 = 81;
    const a82 = 82;
    const a83 = 83;
    const a84 = 84;
    const a85 = 85;
    const a86 = 86;
    const a87 = 87;
    const a88 = 88;
    const a89 = 89;
    const a90 = 90;
    const a91 = 91;
    const a92 = 92;
    const a93 = 93;
    const a94 = 94;
    const a95 = 95;
    const a96 = 96;
    const a97 = 97;
    const a98 = 98;
    const a99 = 99;
    const a100 = 100;
    const a101 = 101;
    const a102 = 102;
    const a103 = 103;
    const a104 = 104;
    const a105 = 105;
    const a106 = 106;
    const a107 = 107;
    const a108 = 108;
    const a109 = 109;
    const a110 = 110;
    const a111 = 111;
    const a112 = 112;
    const a113 = 113;
    const a114 = 114;
    const a115 = 115;
    const a116 = 116;
    const a117 = 117;
    const a118 = 118;
    const a119 = 119;
    const a120 = 120;
    const a121 = 121;
    const a122 = 122;
    const a123 = 123;
    const a124 = 124;
    const a125 = 125;
    const a126 = 126;
    const a127 = 127;
    const a128 = 128;
    const a129 = 129;

    return a0 + a1 + a2 + a3 + a4 + a5 + a6 + a7 + a8 + a9 +
        a10 + a11 + a12 + a13 + a14 + a15 + a16 + a17 + a18 + a19 +
        a20 + a21 + a22 + a23 + a24 + a25 + a26 + a27 + a28 + a29 +
        a30 + a31 + a32 + a33 + a34 + a35 + a36 + a37 + a38 + a39 +
        a40 + a41 + a42 + a43 + a44 + a45 + a46 + a47 + a48 + a49 +
        a50 + a51 + a52 + a53 + a54 + a55 + a56 + a57 + a58 + a59 +
        a60 + a61 + a62 + a63 + a64 + a65 + a66 + a67 + a68 + a69 +
        a70 + a71 + a72 + a73 + a74 + a75 + a76 + a77 + a78 + a79 +
        a80 + a81 + a82 + a83 + a84 + a85 + a86 + a87 + a88 + a89 +
        a90 + a91 + a92 + a93 + a94 + a95 + a96 + a97 + a98 + a99 +
        a100 + a101 + a102 + a103 + a104 + a105 + a106 + a107 + a108 + a109 +
        a110 + a111 + a112 + a113 + a114 + a115 + a116 + a117 + a118 + a119 +
        a120 + a121 + a122 + a123 + a124 + a125 + a126 + a127 + a128 + a129
}

manyConsts();

'success';
