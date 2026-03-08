// Copyright 2021 the V8 project authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Flags: --js-staging --allow-natives-syntax

// This file was imported from the V8 project and processed using Babel
// to remove Rhino incompatibilities.

"use strict";

function _defineProperties(e, r) { for (var t = 0; t < r.length; t++) { var o = r[t]; o.enumerable = o.enumerable || !1, o.configurable = !0, "value" in o && (o.writable = !0), Object.defineProperty(e, _toPropertyKey(o.key), o); } }
function _createClass(e, r, t) { return r && _defineProperties(e.prototype, r), t && _defineProperties(e, t), Object.defineProperty(e, "prototype", { writable: !1 }), e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, r) { if ("object" != typeof t || !t) return t; var e = t[Symbol.toPrimitive]; if (void 0 !== e) { var i = e.call(t, r || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === r ? String : Number)(t); }
function _classCallCheck(a, n) { if (!(a instanceof n)) throw new TypeError("Cannot call a class as a function"); }
function _callSuper(t, o, e) { return o = _getPrototypeOf(o), _possibleConstructorReturn(t, _isNativeReflectConstruct() ? Reflect.construct(o, e || [], _getPrototypeOf(t).constructor) : o.apply(t, e)); }
function _possibleConstructorReturn(t, e) { if (e && ("object" == typeof e || "function" == typeof e)) return e; if (void 0 !== e) throw new TypeError("Derived constructors may only return object or undefined"); return _assertThisInitialized(t); }
function _assertThisInitialized(e) { if (void 0 === e) throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); return e; }
function _inherits(t, e) { if ("function" != typeof e && null !== e) throw new TypeError("Super expression must either be null or a function"); t.prototype = Object.create(e && e.prototype, { constructor: { value: t, writable: !0, configurable: !0 } }), Object.defineProperty(t, "prototype", { writable: !1 }), e && _setPrototypeOf(t, e); }
function _wrapNativeSuper(t) { var r = "function" == typeof Map ? new Map() : void 0; return _wrapNativeSuper = function (t) { if (null === t || !_isNativeFunction(t)) return t; if ("function" != typeof t) throw new TypeError("Super expression must either be null or a function"); if (void 0 !== r) { if (r.has(t)) return r.get(t); r.set(t, Wrapper); } function Wrapper() { return _construct(t, arguments, _getPrototypeOf(this).constructor); } return Wrapper.prototype = Object.create(t.prototype, { constructor: { value: Wrapper, enumerable: !1, writable: !0, configurable: !0 } }), _setPrototypeOf(Wrapper, t); }, _wrapNativeSuper(t); }
function _construct(t, e, r) { if (_isNativeReflectConstruct()) return Reflect.construct.apply(null, arguments); var o = [null]; o.push.apply(o, e); var p = new (t.bind.apply(t, o))(); return r && _setPrototypeOf(p, r.prototype), p; }
function _isNativeReflectConstruct() { try { var t = !Boolean.prototype.valueOf.call(Reflect.construct(Boolean, [], function () {})); } catch (t) {} return (_isNativeReflectConstruct = function () { return !!t; })(); }
function _isNativeFunction(t) { try { return -1 !== Function.toString.call(t).indexOf("[native code]"); } catch (n) { return "function" == typeof t; } }
function _setPrototypeOf(t, e) { return _setPrototypeOf = Object.setPrototypeOf ? Object.setPrototypeOf.bind() : function (t, e) { return t.__proto__ = e, t; }, _setPrototypeOf(t, e); }
function _getPrototypeOf(t) { return _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf.bind() : function (t) { return t.__proto__ || Object.getPrototypeOf(t); }, _getPrototypeOf(t); }

load('testsrc/assert.js');
load('testsrc/jstests/typedarray-helpers.js');

(function TypedArrayPrototype() {
  var rab = CreateResizableArrayBuffer(40, 80);
  var ab = new ArrayBuffer(80);
  for (var ctor of ctors) {
    var ta_rab = new ctor(rab, 0, 3);
    var ta_ab = new ctor(ab, 0, 3);
    assertEquals(ta_rab.__proto__, ta_ab.__proto__);
  }
})();
(function TypedArrayLengthAndByteLength() {
  var rab = CreateResizableArrayBuffer(40, 80);
  for (var ctor of ctors) {
    var ta = new ctor(rab, 0, 3);
    assertEquals(rab, ta.buffer);
    assertEquals(3, ta.length);
    assertEquals(3 * ctor.BYTES_PER_ELEMENT, ta.byteLength);
    var empty_ta = new ctor(rab, 0, 0);
    assertEquals(rab, empty_ta.buffer);
    assertEquals(0, empty_ta.length);
    assertEquals(0, empty_ta.byteLength);
    var ta_with_offset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 3);
    assertEquals(rab, ta_with_offset.buffer);
    assertEquals(3, ta_with_offset.length);
    assertEquals(3 * ctor.BYTES_PER_ELEMENT, ta_with_offset.byteLength);
    var empty_ta_with_offset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 0);
    assertEquals(rab, empty_ta_with_offset.buffer);
    assertEquals(0, empty_ta_with_offset.length);
    assertEquals(0, empty_ta_with_offset.byteLength);
    var length_tracking_ta = new ctor(rab);
    assertEquals(rab, length_tracking_ta.buffer);
    assertEquals(40 / ctor.BYTES_PER_ELEMENT, length_tracking_ta.length);
    assertEquals(40, length_tracking_ta.byteLength);
    var offset = 8;
    var length_tracking_ta_with_offset = new ctor(rab, offset);
    assertEquals(rab, length_tracking_ta_with_offset.buffer);
    assertEquals((40 - offset) / ctor.BYTES_PER_ELEMENT, length_tracking_ta_with_offset.length);
    assertEquals(40 - offset, length_tracking_ta_with_offset.byteLength);
    var empty_length_tracking_ta_with_offset = new ctor(rab, 40);
    assertEquals(rab, empty_length_tracking_ta_with_offset.buffer);
    assertEquals(0, empty_length_tracking_ta_with_offset.length);
    assertEquals(0, empty_length_tracking_ta_with_offset.byteLength);
  }
})();
(function ConstructInvalid() {
  var rab = CreateResizableArrayBuffer(40, 80);
  var _loop = function (ctor) {
    // Length too big.
    assertThrows(() => {
      new ctor(rab, 0, 40 / ctor.BYTES_PER_ELEMENT + 1);
    }, RangeError);

    // Offset too close to the end.
    assertThrows(() => {
      new ctor(rab, 40 - ctor.BYTES_PER_ELEMENT, 2);
    }, RangeError);

    // Offset beyond end.
    assertThrows(() => {
      new ctor(rab, 40, 1);
    }, RangeError);
    if (ctor.BYTES_PER_ELEMENT > 1) {
      // Offset not a multiple of the byte size.
      assertThrows(() => {
        new ctor(rab, 1, 1);
      }, RangeError);
      assertThrows(() => {
        new ctor(rab, 1);
      }, RangeError);
    }
  };
  for (var ctor of ctors) {
    _loop(ctor);
  }

  // Verify the error messages.
  assertThrows(() => {
    new Int16Array(rab, 1, 1);
  }, RangeError);
  assertThrows(() => {
    new Int16Array(rab, 38, 2);
  }, RangeError);
})();
(function ConstructFromTypedArray() {
  AllBigIntMatchedCtorCombinations((targetCtor, sourceCtor) => {
    var rab = CreateResizableArrayBuffer(4 * sourceCtor.BYTES_PER_ELEMENT, 8 * sourceCtor.BYTES_PER_ELEMENT);
    var fixedLength = new sourceCtor(rab, 0, 4);
    var fixedLengthWithOffset = new sourceCtor(rab, 2 * sourceCtor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new sourceCtor(rab, 0);
    var lengthTrackingWithOffset = new sourceCtor(rab, 2 * sourceCtor.BYTES_PER_ELEMENT);

    // Write some data into the array.
    var taFull = new sourceCtor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taFull, i, i + 1);
    }

    // Orig. array: [1, 2, 3, 4]
    //              [1, 2, 3, 4] << fixedLength
    //                    [3, 4] << fixedLengthWithOffset
    //              [1, 2, 3, 4, ...] << lengthTracking
    //                    [3, 4, ...] << lengthTrackingWithOffset

    assertEquals([1, 2, 3, 4], ToNumbers(new targetCtor(fixedLength)));
    assertEquals([3, 4], ToNumbers(new targetCtor(fixedLengthWithOffset)));
    assertEquals([1, 2, 3, 4], ToNumbers(new targetCtor(lengthTracking)));
    assertEquals([3, 4], ToNumbers(new targetCtor(lengthTrackingWithOffset)));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * sourceCtor.BYTES_PER_ELEMENT);

    // Orig. array: [1, 2, 3]
    //              [1, 2, 3, ...] << lengthTracking
    //                    [3, ...] << lengthTrackingWithOffset

    assertThrows(() => {
      new targetCtor(fixedLength);
    }, TypeError);
    assertThrows(() => {
      new targetCtor(fixedLengthWithOffset);
    }, TypeError);
    assertEquals([1, 2, 3], ToNumbers(new targetCtor(lengthTracking)));
    assertEquals([3], ToNumbers(new targetCtor(lengthTrackingWithOffset)));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * sourceCtor.BYTES_PER_ELEMENT);
    assertThrows(() => {
      new targetCtor(fixedLength);
    }, TypeError);
    assertThrows(() => {
      new targetCtor(fixedLengthWithOffset);
    }, TypeError);
    assertEquals([1], ToNumbers(new targetCtor(lengthTracking)));
    assertThrows(() => {
      new targetCtor(lengthTrackingWithOffset);
    }, TypeError);

    // Shrink to zero.
    rab.resize(0);
    assertThrows(() => {
      new targetCtor(fixedLength);
    }, TypeError);
    assertThrows(() => {
      new targetCtor(fixedLengthWithOffset);
    }, TypeError);
    assertEquals([], ToNumbers(new targetCtor(lengthTracking)));
    assertThrows(() => {
      new targetCtor(lengthTrackingWithOffset);
    }, TypeError);

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * sourceCtor.BYTES_PER_ELEMENT);
    for (var _i = 0; _i < 6; ++_i) {
      WriteToTypedArray(taFull, _i, _i + 1);
    }

    // Orig. array: [1, 2, 3, 4, 5, 6]
    //              [1, 2, 3, 4] << fixedLength
    //                    [3, 4] << fixedLengthWithOffset
    //              [1, 2, 3, 4, 5, 6, ...] << lengthTracking
    //                    [3, 4, 5, 6, ...] << lengthTrackingWithOffset

    assertEquals([1, 2, 3, 4], ToNumbers(new targetCtor(fixedLength)));
    assertEquals([3, 4], ToNumbers(new targetCtor(fixedLengthWithOffset)));
    assertEquals([1, 2, 3, 4, 5, 6], ToNumbers(new targetCtor(lengthTracking)));
    assertEquals([3, 4, 5, 6], ToNumbers(new targetCtor(lengthTrackingWithOffset)));
  });
  AllBigIntUnmatchedCtorCombinations((targetCtor, sourceCtor) => {
    var rab = CreateResizableArrayBuffer(4 * sourceCtor.BYTES_PER_ELEMENT, 8 * sourceCtor.BYTES_PER_ELEMENT);
    var fixedLength = new sourceCtor(rab, 0, 4);
    var fixedLengthWithOffset = new sourceCtor(rab, 2 * sourceCtor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new sourceCtor(rab, 0);
    var lengthTrackingWithOffset = new sourceCtor(rab, 2 * sourceCtor.BYTES_PER_ELEMENT);
    assertThrows(() => {
      new targetCtor(fixedLength);
    }, TypeError);
    assertThrows(() => {
      new targetCtor(fixedLengthWithOffset);
    }, TypeError);
    assertThrows(() => {
      new targetCtor(lengthTracking);
    }, TypeError);
    assertThrows(() => {
      new targetCtor(lengthTrackingWithOffset);
    }, TypeError);
  });
})();
(function TypedArrayLengthWhenResizedOutOfBounds1() {
  var rab = CreateResizableArrayBuffer(16, 40);

  // Create TAs which cover the bytes 0-7.
  var tas_and_lengths = [];
  for (var ctor of ctors) {
    var length = 8 / ctor.BYTES_PER_ELEMENT;
    tas_and_lengths.push([new ctor(rab, 0, length), length]);
  }
  for (var [ta, _length] of tas_and_lengths) {
    assertEquals(_length, ta.length);
    assertEquals(_length * ta.BYTES_PER_ELEMENT, ta.byteLength);
  }
  rab.resize(2);
  for (var [_ta, _length2] of tas_and_lengths) {
    assertEquals(0, _ta.length);
    assertEquals(0, _ta.byteLength);
  }

  // Resize the rab so that it just barely covers the needed 8 bytes.
  rab.resize(8);
  for (var [_ta2, _length3] of tas_and_lengths) {
    assertEquals(_length3, _ta2.length);
    assertEquals(_length3 * _ta2.BYTES_PER_ELEMENT, _ta2.byteLength);
  }
  rab.resize(40);
  for (var [_ta3, _length4] of tas_and_lengths) {
    assertEquals(_length4, _ta3.length);
    assertEquals(_length4 * _ta3.BYTES_PER_ELEMENT, _ta3.byteLength);
  }
})();

// The previous test with offsets.
(function TypedArrayLengthWhenResizedOutOfBounds2() {
  var rab = CreateResizableArrayBuffer(20, 40);

  // Create TAs which cover the bytes 8-15.
  var tas_and_lengths = [];
  for (var ctor of ctors) {
    var length = 8 / ctor.BYTES_PER_ELEMENT;
    tas_and_lengths.push([new ctor(rab, 8, length), length]);
  }
  for (var [ta, _length5] of tas_and_lengths) {
    assertEquals(_length5, ta.length);
    assertEquals(_length5 * ta.BYTES_PER_ELEMENT, ta.byteLength);
    assertEquals(8, ta.byteOffset);
  }
  rab.resize(10);
  for (var [_ta4, _length6] of tas_and_lengths) {
    assertEquals(0, _ta4.length);
    assertEquals(0, _ta4.byteLength);
    assertEquals(0, _ta4.byteOffset);
  }

  // Resize the rab so that it just barely covers the needed 8 bytes.
  rab.resize(16);
  for (var [_ta5, _length7] of tas_and_lengths) {
    assertEquals(_length7, _ta5.length);
    assertEquals(_length7 * _ta5.BYTES_PER_ELEMENT, _ta5.byteLength);
    assertEquals(8, _ta5.byteOffset);
  }
  rab.resize(40);
  for (var [_ta6, _length8] of tas_and_lengths) {
    assertEquals(_length8, _ta6.length);
    assertEquals(_length8 * _ta6.BYTES_PER_ELEMENT, _ta6.byteLength);
    assertEquals(8, _ta6.byteOffset);
  }
})();
(function LengthTracking1() {
  var rab = CreateResizableArrayBuffer(16, 40);
  var tas = [];
  for (var ctor of ctors) {
    tas.push(new ctor(rab));
  }
  for (var ta of tas) {
    assertEquals(16 / ta.BYTES_PER_ELEMENT, ta.length);
    assertEquals(16, ta.byteLength);
  }
  rab.resize(40);
  for (var _ta7 of tas) {
    assertEquals(40 / _ta7.BYTES_PER_ELEMENT, _ta7.length);
    assertEquals(40, _ta7.byteLength);
  }

  // Resize to a number which is not a multiple of all byte_lengths.
  rab.resize(19);
  for (var _ta8 of tas) {
    var expected_length = Math.floor(19 / _ta8.BYTES_PER_ELEMENT);
    assertEquals(expected_length, _ta8.length);
    assertEquals(expected_length * _ta8.BYTES_PER_ELEMENT, _ta8.byteLength);
  }
  rab.resize(1);
  for (var _ta9 of tas) {
    if (_ta9.BYTES_PER_ELEMENT == 1) {
      assertEquals(1, _ta9.length);
      assertEquals(1, _ta9.byteLength);
    } else {
      assertEquals(0, _ta9.length);
      assertEquals(0, _ta9.byteLength);
    }
  }
  rab.resize(0);
  for (var _ta0 of tas) {
    assertEquals(0, _ta0.length);
    assertEquals(0, _ta0.byteLength);
  }
  rab.resize(8);
  for (var _ta1 of tas) {
    assertEquals(8 / _ta1.BYTES_PER_ELEMENT, _ta1.length);
    assertEquals(8, _ta1.byteLength);
  }
  rab.resize(40);
  for (var _ta10 of tas) {
    assertEquals(40 / _ta10.BYTES_PER_ELEMENT, _ta10.length);
    assertEquals(40, _ta10.byteLength);
  }
})();

// The previous test with offsets.
(function LengthTracking2() {
  var rab = CreateResizableArrayBuffer(16, 40);
  var offset = 8;
  var tas = [];
  for (var ctor of ctors) {
    tas.push(new ctor(rab, offset));
  }
  for (var ta of tas) {
    assertEquals((16 - offset) / ta.BYTES_PER_ELEMENT, ta.length);
    assertEquals(16 - offset, ta.byteLength);
    assertEquals(offset, ta.byteOffset);
  }
  rab.resize(40);
  for (var _ta11 of tas) {
    assertEquals((40 - offset) / _ta11.BYTES_PER_ELEMENT, _ta11.length);
    assertEquals(40 - offset, _ta11.byteLength);
    assertEquals(offset, _ta11.byteOffset);
  }

  // Resize to a number which is not a multiple of all byte_lengths.
  rab.resize(20);
  for (var _ta12 of tas) {
    var expected_length = Math.floor((20 - offset) / _ta12.BYTES_PER_ELEMENT);
    assertEquals(expected_length, _ta12.length);
    assertEquals(expected_length * _ta12.BYTES_PER_ELEMENT, _ta12.byteLength);
    assertEquals(offset, _ta12.byteOffset);
  }

  // Resize so that all TypedArrays go out of bounds (because of the offset).
  rab.resize(7);
  for (var _ta13 of tas) {
    assertEquals(0, _ta13.length);
    assertEquals(0, _ta13.byteLength);
    assertEquals(0, _ta13.byteOffset);
  }
  rab.resize(0);
  for (var _ta14 of tas) {
    assertEquals(0, _ta14.length);
    assertEquals(0, _ta14.byteLength);
    assertEquals(0, _ta14.byteOffset);
  }
  rab.resize(8);
  for (var _ta15 of tas) {
    assertEquals(0, _ta15.length);
    assertEquals(0, _ta15.byteLength);
    assertEquals(offset, _ta15.byteOffset);
  }

  // Resize so that the TypedArrays which have element size > 1 go out of bounds
  // (because less than 1 full element would fit).
  rab.resize(offset + 1);
  for (var _ta16 of tas) {
    if (_ta16.BYTES_PER_ELEMENT == 1) {
      assertEquals(1, _ta16.length);
      assertEquals(1, _ta16.byteLength);
      assertEquals(offset, _ta16.byteOffset);
    } else {
      assertEquals(0, _ta16.length);
      assertEquals(0, _ta16.byteLength);
      assertEquals(offset, _ta16.byteOffset);
    }
  }
  rab.resize(40);
  for (var _ta17 of tas) {
    assertEquals((40 - offset) / _ta17.BYTES_PER_ELEMENT, _ta17.length);
    assertEquals(40 - offset, _ta17.byteLength);
    assertEquals(offset, _ta17.byteOffset);
  }
})();
(function AccessOutOfBoundsTypedArray() {
  for (var ctor of ctors) {
    if (ctor.BYTES_PER_ELEMENT != 1) {
      continue;
    }
    var rab = CreateResizableArrayBuffer(16, 40);
    var array = new ctor(rab, 0, 4);

    // Initial values
    for (var i = 0; i < 4; ++i) {
      assertEquals(0, array[i]);
    }

    // Within-bounds write
    for (var _i2 = 0; _i2 < 4; ++_i2) {
      array[_i2] = _i2;
    }

    // Within-bounds read
    for (var _i3 = 0; _i3 < 4; ++_i3) {
      assertEquals(_i3, array[_i3]);
    }
    rab.resize(2);

    // OOB read. If the RAB isn't large enough to fit the entire TypedArray,
    // the length of the TypedArray is treated as 0.
    for (var _i4 = 0; _i4 < 4; ++_i4) {
      assertEquals(undefined, array[_i4]);
    }

    // OOB write (has no effect)
    for (var _i5 = 0; _i5 < 4; ++_i5) {
      array[_i5] = 10;
    }
    rab.resize(4);

    // Within-bounds read
    for (var _i6 = 0; _i6 < 2; ++_i6) {
      assertEquals(_i6, array[_i6]);
    }
    // The shrunk-and-regrown part got zeroed.
    for (var _i7 = 2; _i7 < 4; ++_i7) {
      assertEquals(0, array[_i7]);
    }
    rab.resize(40);

    // Within-bounds read
    for (var _i8 = 0; _i8 < 2; ++_i8) {
      assertEquals(_i8, array[_i8]);
    }
    for (var _i9 = 2; _i9 < 4; ++_i9) {
      assertEquals(0, array[_i9]);
    }
  }
})();
(function OutOfBoundsTypedArrayAndHas() {
  for (var ctor of ctors) {
    if (ctor.BYTES_PER_ELEMENT != 1) {
      continue;
    }
    var rab = CreateResizableArrayBuffer(16, 40);
    var array = new ctor(rab, 0, 4);

    // Within-bounds read
    for (var i = 0; i < 4; ++i) {
      assertTrue(i in array);
    }
    rab.resize(2);

    // OOB read. If the RAB isn't large enough to fit the entire TypedArray,
    // the length of the TypedArray is treated as 0.
    for (var _i0 = 0; _i0 < 4; ++_i0) {
      assertFalse(_i0 in array);
    }
    rab.resize(4);

    // Within-bounds read
    for (var _i1 = 0; _i1 < 4; ++_i1) {
      assertTrue(_i1 in array);
    }
    rab.resize(40);

    // Within-bounds read
    for (var _i10 = 0; _i10 < 4; ++_i10) {
      assertTrue(_i10 in array);
    }
  }
})();
(function LoadFromOutOfBoundsTypedArrayWithFeedback() {
  function ReadElement2(ta) {
    return ta[2];
  }
  //%EnsureFeedbackVectorForFunction(ReadElement2);

  var rab = CreateResizableArrayBuffer(16, 40);
  var i8a = new Int8Array(rab, 0, 4);
  for (var i = 0; i < 3; ++i) {
    assertEquals(0, ReadElement2(i8a));
  }

  // Within-bounds write
  for (var _i11 = 0; _i11 < 4; ++_i11) {
    i8a[_i11] = _i11;
  }

  // Within-bounds read
  for (var _i12 = 0; _i12 < 3; ++_i12) {
    assertEquals(2, ReadElement2(i8a));
  }
  rab.resize(2);

  // OOB read
  for (var _i13 = 0; _i13 < 3; ++_i13) {
    assertEquals(undefined, ReadElement2(i8a));
  }
  rab.resize(4);

  // Within-bounds read (the memory got zeroed)
  for (var _i14 = 0; _i14 < 3; ++_i14) {
    assertEquals(0, ReadElement2(i8a));
  }
  i8a[2] = 3;
  for (var _i15 = 0; _i15 < 3; ++_i15) {
    assertEquals(3, ReadElement2(i8a));
  }
  rab.resize(40);

  // Within-bounds read
  for (var _i16 = 0; _i16 < 3; ++_i16) {
    assertEquals(3, ReadElement2(i8a));
  }
})();
(function HasAndOutOfBoundsTypedArrayWithFeedback() {
  function HasElement2(ta) {
    return 2 in ta;
  }
  //%EnsureFeedbackVectorForFunction(HasElement2);

  var rab = CreateResizableArrayBuffer(16, 40);
  var i8a = new Int8Array(rab, 0, 4);

  // Within-bounds read
  for (var i = 0; i < 3; ++i) {
    assertTrue(HasElement2(i8a));
  }
  rab.resize(2);

  // OOB read
  for (var _i17 = 0; _i17 < 3; ++_i17) {
    assertFalse(HasElement2(i8a));
  }
  rab.resize(4);

  // Within-bounds read
  for (var _i18 = 0; _i18 < 3; ++_i18) {
    assertTrue(HasElement2(i8a));
  }
  rab.resize(40);

  // Within-bounds read
  for (var _i19 = 0; _i19 < 3; ++_i19) {
    assertTrue(HasElement2(i8a));
  }
})();
(function HasWithOffsetsWithFeedback() {
  function GetElements(ta) {
    var result = '';
    for (var i = 0; i < 8; ++i) {
      result += (i in ta) + ',';
      //           ^ feedback will be here
    }
    return result;
  }
  //%EnsureFeedbackVectorForFunction(GetElements);

  var rab = CreateResizableArrayBuffer(4, 8);
  var fixedLength = new Int8Array(rab, 0, 4);
  var fixedLengthWithOffset = new Int8Array(rab, 1, 3);
  var lengthTracking = new Int8Array(rab, 0);
  var lengthTrackingWithOffset = new Int8Array(rab, 1);
  assertEquals('true,true,true,true,false,false,false,false,', GetElements(fixedLength));
  assertEquals('true,true,true,false,false,false,false,false,', GetElements(fixedLengthWithOffset));
  assertEquals('true,true,true,true,false,false,false,false,', GetElements(lengthTracking));
  assertEquals('true,true,true,false,false,false,false,false,', GetElements(lengthTrackingWithOffset));
  rab.resize(2);
  assertEquals('false,false,false,false,false,false,false,false,', GetElements(fixedLength));
  assertEquals('false,false,false,false,false,false,false,false,', GetElements(fixedLengthWithOffset));
  assertEquals('true,true,false,false,false,false,false,false,', GetElements(lengthTracking));
  assertEquals('true,false,false,false,false,false,false,false,', GetElements(lengthTrackingWithOffset));

  // Resize beyond the offset of the length tracking arrays.
  rab.resize(1);
  assertEquals('false,false,false,false,false,false,false,false,', GetElements(fixedLength));
  assertEquals('false,false,false,false,false,false,false,false,', GetElements(fixedLengthWithOffset));
  assertEquals('true,false,false,false,false,false,false,false,', GetElements(lengthTracking));
  assertEquals('false,false,false,false,false,false,false,false,', GetElements(lengthTrackingWithOffset));
  rab.resize(8);
  assertEquals('true,true,true,true,false,false,false,false,', GetElements(fixedLength));
  assertEquals('true,true,true,false,false,false,false,false,', GetElements(fixedLengthWithOffset));
  assertEquals('true,true,true,true,true,true,true,true,', GetElements(lengthTracking));
  assertEquals('true,true,true,true,true,true,true,false,', GetElements(lengthTrackingWithOffset));
})();
(function StoreToOutOfBoundsTypedArrayWithFeedback() {
  function WriteElement2(ta, i) {
    ta[2] = i;
  }
  //%EnsureFeedbackVectorForFunction(WriteElement2);

  var rab = CreateResizableArrayBuffer(16, 40);
  var i8a = new Int8Array(rab, 0, 4);
  assertEquals(0, i8a[2]);

  // Within-bounds write
  for (var i = 0; i < 3; ++i) {
    WriteElement2(i8a, 3);
  }
  // Within-bounds read
  for (var _i20 = 0; _i20 < 3; ++_i20) {
    assertEquals(3, i8a[2]);
  }
  rab.resize(2);

  // OOB write (has no effect)
  for (var _i21 = 0; _i21 < 3; ++_i21) {
    WriteElement2(i8a, 4);
  }
  rab.resize(4);

  // Within-bounds read (the memory got zeroed)
  for (var _i22 = 0; _i22 < 3; ++_i22) {
    assertEquals(0, i8a[2]);
  }

  // Within-bounds write
  for (var _i23 = 0; _i23 < 3; ++_i23) {
    WriteElement2(i8a, 5);
  }
  rab.resize(40);

  // Within-bounds read
  for (var _i24 = 0; _i24 < 3; ++_i24) {
    assertEquals(5, i8a[2]);
  }
})();
/* Rhino: Need to work on prototype handling in general
(function OOBBehavesLikeDetached() {
  function ReadElement2(ta) {
    return ta[2];
  }
  function HasElement2(ta) {
    return 2 in ta;
  }
  var rab = CreateResizableArrayBuffer(16, 40);
  var i8a = new Int8Array(rab, 0, 4);
  i8a.__proto__ = {
    2: 'wrong value'
  };
  i8a[2] = 10;
  assertEquals(10, ReadElement2(i8a));
  assertTrue(HasElement2(i8a));
  rab.resize(0);
  assertEquals(undefined, ReadElement2(i8a));
  assertFalse(HasElement2(i8a));
})();
(function OOBBehavesLikeDetachedWithFeedback() {
  function ReadElement2(ta) {
    return ta[2];
  }
  function HasElement2(ta) {
    return 2 in ta;
  }
  //%EnsureFeedbackVectorForFunction(ReadElement2);
  //%EnsureFeedbackVectorForFunction(HasElement2);

  var rab = CreateResizableArrayBuffer(16, 40);
  var i8a = new Int8Array(rab, 0, 4);
  i8a.__proto__ = {
    2: 'wrong value'
  };
  i8a[2] = 10;
  for (var i = 0; i < 3; ++i) {
    assertEquals(10, ReadElement2(i8a));
    assertTrue(HasElement2(i8a));
  }
  rab.resize(0);
  for (var _i25 = 0; _i25 < 3; ++_i25) {
    assertEquals(undefined, ReadElement2(i8a));
    assertFalse(HasElement2(i8a));
  }
})();
 */
(function EnumerateElements() {
  var rab = CreateResizableArrayBuffer(100, 200);
  for (var ctor of ctors) {
    var ta = new ctor(rab, 0, 3);
    var keys = '';
    for (var key in ta) {
      keys += key;
    }
    assertEquals('012', keys);
  }
})();
(function IterateTypedArray() {
  var no_elements = 10;
  var offset = 2;
  function TestIteration(ta, expected) {
    var values = [];
    for (var value of ta) {
      values.push(Number(value));
    }
    assertEquals(expected, values);
  }
  for (var ctor of ctors) {
    var buffer_byte_length = no_elements * ctor.BYTES_PER_ELEMENT;
    // We can use the same RAB for all the TAs below, since we won't modify it
    // after writing the initial values.
    var rab = CreateResizableArrayBuffer(buffer_byte_length, 2 * buffer_byte_length);
    var byte_offset = offset * ctor.BYTES_PER_ELEMENT;

    // Write some data into the array.
    var ta_write = new ctor(rab);
    for (var i = 0; i < no_elements; ++i) {
      WriteToTypedArray(ta_write, i, i % 128);
    }

    // Create various different styles of TypedArrays with the RAB as the
    // backing store and iterate them.
    var ta = new ctor(rab, 0, 3);
    TestIteration(ta, [0, 1, 2]);
    var empty_ta = new ctor(rab, 0, 0);
    TestIteration(empty_ta, []);
    var ta_with_offset = new ctor(rab, byte_offset, 3);
    TestIteration(ta_with_offset, [2, 3, 4]);
    var empty_ta_with_offset = new ctor(rab, byte_offset, 0);
    TestIteration(empty_ta_with_offset, []);
    var length_tracking_ta = new ctor(rab);
    {
      var expected = [];
      for (var _i26 = 0; _i26 < no_elements; ++_i26) {
        expected.push(_i26 % 128);
      }
      TestIteration(length_tracking_ta, expected);
    }
    var length_tracking_ta_with_offset = new ctor(rab, byte_offset);
    {
      var _expected = [];
      for (var _i27 = offset; _i27 < no_elements; ++_i27) {
        _expected.push(_i27 % 128);
      }
      TestIteration(length_tracking_ta_with_offset, _expected);
    }
    var empty_length_tracking_ta_with_offset = new ctor(rab, buffer_byte_length);
    TestIteration(empty_length_tracking_ta_with_offset, []);
  }
})();

// Helpers for iteration tests.
function CreateRab(buffer_byte_length, ctor) {
  var rab = CreateResizableArrayBuffer(buffer_byte_length, 2 * buffer_byte_length);
  // Write some data into the array.
  var ta_write = new ctor(rab);
  for (var i = 0; i < buffer_byte_length / ctor.BYTES_PER_ELEMENT; ++i) {
    WriteToTypedArray(ta_write, i, i % 128);
  }
  return rab;
}
function TestIterationAndResize(ta, expected, rab, resize_after, new_byte_length) {
  var values = [];
  var resized = false;
  for (var value of ta) {
    if (value instanceof Array) {
      // When iterating via entries(), the values will be arrays [key, value].
      values.push([value[0], Number(value[1])]);
    } else {
      values.push(Number(value));
    }
    if (!resized && values.length == resize_after) {
      rab.resize(new_byte_length);
      resized = true;
    }
  }
  assertEquals(expected, values);
  assertTrue(resized);
}
(function IterateTypedArrayAndGrowMidIteration() {
  var no_elements = 10;
  var offset = 2;
  for (var ctor of ctors) {
    var buffer_byte_length = no_elements * ctor.BYTES_PER_ELEMENT;
    var byte_offset = offset * ctor.BYTES_PER_ELEMENT;

    // Create various different styles of TypedArrays with the RAB as the
    // backing store and iterate them.

    // Fixed-length TAs aren't affected by resizing.
    var rab = CreateRab(buffer_byte_length, ctor);
    var ta = new ctor(rab, 0, 3);
    TestIterationAndResize(ta, [0, 1, 2], rab, 2, buffer_byte_length * 2);
    rab = CreateRab(buffer_byte_length, ctor);
    var ta_with_offset = new ctor(rab, byte_offset, 3);
    TestIterationAndResize(ta_with_offset, [2, 3, 4], rab, 2, buffer_byte_length * 2);
    rab = CreateRab(buffer_byte_length, ctor);
    var length_tracking_ta = new ctor(rab);
    {
      var expected = [];
      for (var i = 0; i < no_elements; ++i) {
        expected.push(i % 128);
      }
      // After resizing, the new memory contains zeros.
      for (var _i28 = 0; _i28 < no_elements; ++_i28) {
        expected.push(0);
      }
      TestIterationAndResize(length_tracking_ta, expected, rab, 2, buffer_byte_length * 2);
    }
    rab = CreateRab(buffer_byte_length, ctor);
    var length_tracking_ta_with_offset = new ctor(rab, byte_offset);
    {
      var _expected2 = [];
      for (var _i29 = offset; _i29 < no_elements; ++_i29) {
        _expected2.push(_i29 % 128);
      }
      for (var _i30 = 0; _i30 < no_elements; ++_i30) {
        _expected2.push(0);
      }
      TestIterationAndResize(length_tracking_ta_with_offset, _expected2, rab, 2, buffer_byte_length * 2);
    }
  }
})();
(function IterateTypedArrayAndGrowJustBeforeIterationWouldEnd() {
  var no_elements = 10;
  var offset = 2;

  // We need to recreate the RAB between all TA tests, since we grow it.
  for (var ctor of ctors) {
    var buffer_byte_length = no_elements * ctor.BYTES_PER_ELEMENT;
    var byte_offset = offset * ctor.BYTES_PER_ELEMENT;

    // Create various different styles of TypedArrays with the RAB as the
    // backing store and iterate them.

    var rab = CreateRab(buffer_byte_length, ctor);
    var length_tracking_ta = new ctor(rab);
    {
      var expected = [];
      for (var i = 0; i < no_elements; ++i) {
        expected.push(i % 128);
      }
      // After resizing, the new memory contains zeros.
      for (var _i31 = 0; _i31 < no_elements; ++_i31) {
        expected.push(0);
      }
      TestIterationAndResize(length_tracking_ta, expected, rab, no_elements, buffer_byte_length * 2);
    }
    rab = CreateRab(buffer_byte_length, ctor);
    var length_tracking_ta_with_offset = new ctor(rab, byte_offset);
    {
      var _expected3 = [];
      for (var _i32 = offset; _i32 < no_elements; ++_i32) {
        _expected3.push(_i32 % 128);
      }
      for (var _i33 = 0; _i33 < no_elements; ++_i33) {
        _expected3.push(0);
      }
      TestIterationAndResize(length_tracking_ta_with_offset, _expected3, rab, no_elements - offset, buffer_byte_length * 2);
    }
  }
})();
(function IterateTypedArrayAndShrinkMidIteration() {
  var no_elements = 10;
  var offset = 2;
  var _loop2 = function () {
    var buffer_byte_length = no_elements * ctor.BYTES_PER_ELEMENT;
    var byte_offset = offset * ctor.BYTES_PER_ELEMENT;

    // Create various different styles of TypedArrays with the RAB as the
    // backing store and iterate them.

    // Fixed-length TAs aren't affected by shrinking if they stay in-bounds.
    // They appear detached after shrinking out of bounds.
    var rab = CreateRab(buffer_byte_length, ctor);
    var ta1 = new ctor(rab, 0, 3);
    TestIterationAndResize(ta1, [0, 1, 2], rab, 2, buffer_byte_length / 2);
    rab = CreateRab(buffer_byte_length, ctor);
    var ta2 = new ctor(rab, 0, 3);
    assertThrows(() => {
      TestIterationAndResize(ta2, null, rab, 2, 1);
    });
    rab = CreateRab(buffer_byte_length, ctor);
    var ta_with_offset1 = new ctor(rab, byte_offset, 3);
    TestIterationAndResize(ta_with_offset1, [2, 3, 4], rab, 2, buffer_byte_length / 2);
    rab = CreateRab(buffer_byte_length, ctor);
    var ta_with_offset2 = new ctor(rab, byte_offset, 3);
    assertThrows(() => {
      TestIterationAndResize(ta_with_offset2, null, rab, 2, 0);
    });

    // Length-tracking TA with offset 0 doesn't throw, but its length gracefully
    // reduces too.
    rab = CreateRab(buffer_byte_length, ctor);
    var length_tracking_ta = new ctor(rab);
    TestIterationAndResize(length_tracking_ta, [0, 1, 2, 3, 4], rab, 2, buffer_byte_length / 2);

    // Length-tracking TA appears detached when the buffer is resized beyond the
    // offset.
    rab = CreateRab(buffer_byte_length, ctor);
    var length_tracking_ta_with_offset1 = new ctor(rab, byte_offset);
    assertThrows(() => {
      TestIterationAndResize(length_tracking_ta_with_offset1, null, rab, 2, byte_offset / 2);
    });

    // Length-tracking TA reduces its length gracefully when the buffer is
    // resized to barely cover the offset.
    rab = CreateRab(buffer_byte_length, ctor);
    var length_tracking_ta_with_offset2 = new ctor(rab, byte_offset);
    TestIterationAndResize(length_tracking_ta_with_offset2, [2, 3], rab, 2, byte_offset);
  };
  for (var ctor of ctors) {
    _loop2();
  }
})();
(function IterateTypedArrayAndShrinkToZeroMidIteration() {
  var no_elements = 10;
  var offset = 2;
  var _loop3 = function () {
    var buffer_byte_length = no_elements * ctor.BYTES_PER_ELEMENT;
    var byte_offset = offset * ctor.BYTES_PER_ELEMENT;

    // Create various different styles of TypedArrays with the RAB as the
    // backing store and iterate them.

    // Fixed-length TAs appear detached after shrinking out of bounds.
    var rab = CreateRab(buffer_byte_length, ctor);
    var ta = new ctor(rab, 0, 3);
    assertThrows(() => {
      TestIterationAndResize(ta, null, rab, 2, 0);
    });
    rab = CreateRab(buffer_byte_length, ctor);
    var ta_with_offset = new ctor(rab, byte_offset, 3);
    assertThrows(() => {
      TestIterationAndResize(ta_with_offset, null, rab, 2, 0);
    });

    // Length-tracking TA with offset 0 doesn't throw, but its length gracefully
    // goes to zero too.
    rab = CreateRab(buffer_byte_length, ctor);
    var length_tracking_ta = new ctor(rab);
    TestIterationAndResize(length_tracking_ta, [0, 1], rab, 2, 0);

    // Length-tracking TA which is resized beyond the offset appars detached.
    rab = CreateRab(buffer_byte_length, ctor);
    var length_tracking_ta_with_offset = new ctor(rab, byte_offset);
    assertThrows(() => {
      TestIterationAndResize(length_tracking_ta_with_offset, null, rab, 2, 0);
    });
  };
  for (var ctor of ctors) {
    _loop3();
  }
})();
/* Rhino: Destructuring does not seem to be using the iterator
(function Destructuring() {
  var _loop4 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);

    // Write some data into the array.
    var ta_write = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(ta_write, i, i);
    }
    {
      var [a, b, c, d, e] = fixedLength;
      assertEquals([0, 1, 2, 3], ToNumbers([a, b, c, d]));
      assertEquals(undefined, e);
    }
    {
      var [_a, _b, _c] = fixedLengthWithOffset;
      assertEquals([2, 3], ToNumbers([_a, _b]));
      assertEquals(undefined, _c);
    }
    {
      var [_a2, _b2, _c2, _d, _e] = lengthTracking;
      assertEquals([0, 1, 2, 3], ToNumbers([_a2, _b2, _c2, _d]));
      assertEquals(undefined, _e);
    }
    {
      var [_a3, _b3, _c3] = lengthTrackingWithOffset;
      assertEquals([2, 3], ToNumbers([_a3, _b3]));
      assertEquals(undefined, _c3);
    }

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);
    assertThrows(() => {
      var [a, b, c] = fixedLength;
    }, TypeError);
    assertThrows(() => {
      var [a, b, c] = fixedLengthWithOffset;
    }, TypeError);
    {
      var [_a4, _b4, _c4, _d2] = lengthTracking;
      assertEquals([0, 1, 2], ToNumbers([_a4, _b4, _c4]));
      assertEquals(undefined, _d2);
    }
    {
      var [_a5, _b5] = lengthTrackingWithOffset;
      assertEquals([2], ToNumbers([_a5]));
      assertEquals(undefined, _b5);
    }

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    assertThrows(() => {
      var [a, b, c] = fixedLength;
    }, TypeError);
    assertThrows(() => {
      var [a, b, c] = fixedLengthWithOffset;
    }, TypeError);
    assertThrows(() => {
      var [a, b, c] = lengthTrackingWithOffset;
    }, TypeError);
    {
      var [_a6, _b6] = lengthTracking;
      assertEquals([0], ToNumbers([_a6]));
      assertEquals(undefined, _b6);
    }

    // Shrink to 0.
    rab.resize(0);
    assertThrows(() => {
      var [a, b, c] = fixedLength;
    }, TypeError);
    assertThrows(() => {
      var [a, b, c] = fixedLengthWithOffset;
    }, TypeError);
    assertThrows(() => {
      var [a, b, c] = lengthTrackingWithOffset;
    }, TypeError);
    {
      var [_a7] = lengthTracking;
      assertEquals(undefined, _a7);
    }

    // Grow so that all TAs are back in-bounds. The new memory is zeroed.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    {
      var [_a8, _b7, _c5, _d3, _e2] = fixedLength;
      assertEquals([0, 0, 0, 0], ToNumbers([_a8, _b7, _c5, _d3]));
      assertEquals(undefined, _e2);
    }
    {
      var [_a9, _b8, _c6] = fixedLengthWithOffset;
      assertEquals([0, 0], ToNumbers([_a9, _b8]));
      assertEquals(undefined, _c6);
    }
    {
      var [_a0, _b9, _c7, _d4, _e3, f, g] = lengthTracking;
      assertEquals([0, 0, 0, 0, 0, 0], ToNumbers([_a0, _b9, _c7, _d4, _e3, f]));
      assertEquals(undefined, g);
    }
    {
      var [_a1, _b0, _c8, _d5, _e4] = lengthTrackingWithOffset;
      assertEquals([0, 0, 0, 0], ToNumbers([_a1, _b0, _c8, _d5]));
      assertEquals(undefined, _e4);
    }
  };
  for (var ctor of ctors) {
    _loop4();
  }
})();
 */
function TestFill(helper, oobThrows) {
  var _loop5 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);
    assertEquals([0, 0, 0, 0], ReadDataFromBuffer(rab, ctor));
    helper(fixedLength, 1);
    assertEquals([1, 1, 1, 1], ReadDataFromBuffer(rab, ctor));
    helper(fixedLengthWithOffset, 2);
    assertEquals([1, 1, 2, 2], ReadDataFromBuffer(rab, ctor));
    helper(lengthTracking, 3);
    assertEquals([3, 3, 3, 3], ReadDataFromBuffer(rab, ctor));
    helper(lengthTrackingWithOffset, 4);
    assertEquals([3, 3, 4, 4], ReadDataFromBuffer(rab, ctor));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);
    if (oobThrows) {
      assertThrows(() => helper(fixedLength, 5), TypeError);
      assertThrows(() => helper(fixedLengthWithOffset, 6), TypeError);
    } else {
      helper(fixedLength, 5);
      helper(fixedLengthWithOffset, 6);
      // We'll check below that these were no-op.
    }
    assertEquals([3, 3, 4], ReadDataFromBuffer(rab, ctor));
    helper(lengthTracking, 7);
    assertEquals([7, 7, 7], ReadDataFromBuffer(rab, ctor));
    helper(lengthTrackingWithOffset, 8);
    assertEquals([7, 7, 8], ReadDataFromBuffer(rab, ctor));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    if (oobThrows) {
      assertThrows(() => helper(fixedLength, 9), TypeError);
      assertThrows(() => helper(fixedLengthWithOffset, 10), TypeError);
      assertThrows(() => helper(lengthTrackingWithOffset, 11), TypeError);
    } else {
      // We'll check below that these were no-op.
      helper(fixedLength, 9);
      helper(fixedLengthWithOffset, 10);
      helper(lengthTrackingWithOffset, 11);
    }
    assertEquals([7], ReadDataFromBuffer(rab, ctor));
    helper(lengthTracking, 12);
    assertEquals([12], ReadDataFromBuffer(rab, ctor));

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    helper(fixedLength, 13);
    assertEquals([13, 13, 13, 13, 0, 0], ReadDataFromBuffer(rab, ctor));
    helper(fixedLengthWithOffset, 14);
    assertEquals([13, 13, 14, 14, 0, 0], ReadDataFromBuffer(rab, ctor));
    helper(lengthTracking, 15);
    assertEquals([15, 15, 15, 15, 15, 15], ReadDataFromBuffer(rab, ctor));
    helper(lengthTrackingWithOffset, 16);
    assertEquals([15, 15, 16, 16, 16, 16], ReadDataFromBuffer(rab, ctor));

    // Filling with non-undefined start & end.
    helper(fixedLength, 17, 1, 3);
    assertEquals([15, 17, 17, 16, 16, 16], ReadDataFromBuffer(rab, ctor));
    helper(fixedLengthWithOffset, 18, 1, 2);
    assertEquals([15, 17, 17, 18, 16, 16], ReadDataFromBuffer(rab, ctor));
    helper(lengthTracking, 19, 1, 3);
    assertEquals([15, 19, 19, 18, 16, 16], ReadDataFromBuffer(rab, ctor));
    helper(lengthTrackingWithOffset, 20, 1, 2);
    assertEquals([15, 19, 19, 20, 16, 16], ReadDataFromBuffer(rab, ctor));
  };
  for (var ctor of ctors) {
    _loop5();
  }
}
TestFill(TypedArrayFillHelper, true);
TestFill(ArrayFillHelper, false);
(function FillParameterConversionResizes() {
  var _loop6 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * ctor.BYTES_PER_ELEMENT);
        return 3;
      }
    };
    assertThrows(() => {
      TypedArrayFillHelper(fixedLength, evil, 1, 2);
    }, TypeError);
  };
  for (var ctor of ctors) {
    _loop6(ctor);
  }
  var _loop7 = function (_ctor) {
    var rab = CreateResizableArrayBuffer(4 * _ctor.BYTES_PER_ELEMENT, 8 * _ctor.BYTES_PER_ELEMENT);
    var fixedLength = new _ctor(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor.BYTES_PER_ELEMENT);
        return 1;
      }
    };
    assertThrows(() => {
      TypedArrayFillHelper(fixedLength, 3, evil, 2);
    }, TypeError);
  };
  for (var _ctor of ctors) {
    _loop7(_ctor);
  }
  var _loop8 = function (_ctor2) {
    var rab = CreateResizableArrayBuffer(4 * _ctor2.BYTES_PER_ELEMENT, 8 * _ctor2.BYTES_PER_ELEMENT);
    var fixedLength = new _ctor2(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor2.BYTES_PER_ELEMENT);
        return 2;
      }
    };
    assertThrows(() => {
      TypedArrayFillHelper(fixedLength, 3, 1, evil);
    }, TypeError);
  };
  for (var _ctor2 of ctors) {
    _loop8(_ctor2);
  }
  // Resizing + a length-tracking TA -> no OOB, but bounds recomputation needed.
  var _loop9 = function (_ctor3) {
    var rab = CreateResizableArrayBuffer(4 * _ctor3.BYTES_PER_ELEMENT, 8 * _ctor3.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor3(rab);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor3.BYTES_PER_ELEMENT);
        return 1;
      }
    };
    TypedArrayFillHelper(lengthTracking, evil, 0, 4);
  };
  for (var _ctor3 of ctors) {
    _loop9(_ctor3);
  }
  var _loop0 = function (_ctor4) {
    var rab = CreateResizableArrayBuffer(4 * _ctor4.BYTES_PER_ELEMENT, 8 * _ctor4.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor4(rab);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor4.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    TypedArrayFillHelper(lengthTracking, 1, evil, 4);
  };
  for (var _ctor4 of ctors) {
    _loop0(_ctor4);
  }
  var _loop1 = function (_ctor5) {
    var rab = CreateResizableArrayBuffer(4 * _ctor5.BYTES_PER_ELEMENT, 8 * _ctor5.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor5(rab);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor5.BYTES_PER_ELEMENT);
        return 4;
      }
    };
    TypedArrayFillHelper(lengthTracking, 1, 0, evil);
  };
  for (var _ctor5 of ctors) {
    _loop1(_ctor5);
  }
  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  var _loop10 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor6.BYTES_PER_ELEMENT, 8 * _ctor6.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor6(rab);
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return 1;
      }
    };
    TypedArrayFillHelper(lengthTracking, evil, 0, 4);
  };
  for (var _ctor6 of ctors) {
    _loop10();
  }
  var _loop11 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor7.BYTES_PER_ELEMENT, 8 * _ctor7.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor7(rab);
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return 0;
      }
    };
    TypedArrayFillHelper(lengthTracking, 1, evil, 4);
  };
  for (var _ctor7 of ctors) {
    _loop11();
  }
  var _loop12 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor8.BYTES_PER_ELEMENT, 8 * _ctor8.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor8(rab);
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return 4;
      }
    };
    TypedArrayFillHelper(lengthTracking, 1, 0, evil);
  };
  for (var _ctor8 of ctors) {
    _loop12();
  }
})();
(function ArrayFillParameterConversionResizes() {
  var _loop13 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * ctor.BYTES_PER_ELEMENT);
        return 3;
      }
    };
    ArrayFillHelper(fixedLength, evil, 1, 2);
    // The underlying data doesn't change: all writes fail because 'fixedLength'
    // is OOB.
    assertEquals([0, 0], ReadDataFromBuffer(rab, ctor));
  };
  for (var ctor of ctors) {
    _loop13(ctor);
  }
  var _loop14 = function (_ctor9) {
    var rab = CreateResizableArrayBuffer(4 * _ctor9.BYTES_PER_ELEMENT, 8 * _ctor9.BYTES_PER_ELEMENT);
    var fixedLength = new _ctor9(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor9.BYTES_PER_ELEMENT);
        return 1;
      }
    };
    ArrayFillHelper(fixedLength, 3, evil, 2);
    assertEquals([0, 0], ReadDataFromBuffer(rab, _ctor9));
  };
  for (var _ctor9 of ctors) {
    _loop14(_ctor9);
  }
  var _loop15 = function (_ctor0) {
    var rab = CreateResizableArrayBuffer(4 * _ctor0.BYTES_PER_ELEMENT, 8 * _ctor0.BYTES_PER_ELEMENT);
    var fixedLength = new _ctor0(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor0.BYTES_PER_ELEMENT);
        return 2;
      }
    };
    ArrayFillHelper(fixedLength, 3, 1, evil);
    assertEquals([0, 0], ReadDataFromBuffer(rab, _ctor0));
  };
  for (var _ctor0 of ctors) {
    _loop15(_ctor0);
  }
  // Resizing + a length-tracking TA -> no OOB, but bounds recomputation needed.
  var _loop16 = function (_ctor1) {
    var rab = CreateResizableArrayBuffer(4 * _ctor1.BYTES_PER_ELEMENT, 8 * _ctor1.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor1(rab);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor1.BYTES_PER_ELEMENT);
        return 3;
      }
    };
    ArrayFillHelper(lengthTracking, evil, 0, 4);
    assertEquals([3, 3], ReadDataFromBuffer(rab, _ctor1));
  };
  for (var _ctor1 of ctors) {
    _loop16(_ctor1);
  }
  var _loop17 = function (_ctor10) {
    var rab = CreateResizableArrayBuffer(4 * _ctor10.BYTES_PER_ELEMENT, 8 * _ctor10.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor10(rab);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor10.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    ArrayFillHelper(lengthTracking, 3, evil, 4);
    assertEquals([3, 3], ReadDataFromBuffer(rab, _ctor10));
  };
  for (var _ctor10 of ctors) {
    _loop17(_ctor10);
  }
  var _loop18 = function (_ctor11) {
    var rab = CreateResizableArrayBuffer(4 * _ctor11.BYTES_PER_ELEMENT, 8 * _ctor11.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor11(rab);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor11.BYTES_PER_ELEMENT);
        return 4;
      }
    };
    ArrayFillHelper(lengthTracking, 3, 0, evil);
    assertEquals([3, 3], ReadDataFromBuffer(rab, _ctor11));
  };
  for (var _ctor11 of ctors) {
    _loop18(_ctor11);
  }
  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  var _loop19 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor12.BYTES_PER_ELEMENT, 8 * _ctor12.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor12(rab);
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return 3;
      }
    };
    ArrayFillHelper(lengthTracking, evil, 0, 4);
  };
  for (var _ctor12 of ctors) {
    _loop19();
  }
  var _loop20 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor13.BYTES_PER_ELEMENT, 8 * _ctor13.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor13(rab);
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return 0;
      }
    };
    ArrayFillHelper(lengthTracking, 3, evil, 4);
  };
  for (var _ctor13 of ctors) {
    _loop20();
  }
  var _loop21 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor14.BYTES_PER_ELEMENT, 8 * _ctor14.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor14(rab);
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return 4;
      }
    };
    ArrayFillHelper(lengthTracking, 3, 0, evil);
  };
  for (var _ctor14 of ctors) {
    _loop21();
  }
})();
function At(atHelper, oobThrows) {
  var _loop22 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);

    // Write some data into the array.
    var ta_write = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(ta_write, i, i);
    }
    assertEquals(3, atHelper(fixedLength, -1));
    assertEquals(3, atHelper(lengthTracking, -1));
    assertEquals(3, atHelper(fixedLengthWithOffset, -1));
    assertEquals(3, atHelper(lengthTrackingWithOffset, -1));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);
    if (oobThrows) {
      assertThrows(() => {
        atHelper(fixedLength, -1);
      });
      assertThrows(() => {
        atHelper(fixedLengthWithOffset, -1);
      });
    } else {
      assertEquals(undefined, atHelper(fixedLength, -1));
      assertEquals(undefined, atHelper(fixedLengthWithOffset, -1));
    }
    assertEquals(2, atHelper(lengthTracking, -1));
    assertEquals(2, atHelper(lengthTrackingWithOffset, -1));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    if (oobThrows) {
      assertThrows(() => {
        atHelper(fixedLength, -1);
      });
      assertThrows(() => {
        atHelper(fixedLengthWithOffset, -1);
      });
      assertThrows(() => {
        atHelper(lengthTrackingWithOffset, -1);
      });
    } else {
      assertEquals(undefined, atHelper(fixedLength, -1));
      assertEquals(undefined, atHelper(fixedLengthWithOffset, -1));
      assertEquals(undefined, atHelper(lengthTrackingWithOffset, -1));
    }
    assertEquals(0, atHelper(lengthTracking, -1));

    // Grow so that all TAs are back in-bounds. New memory is zeroed.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    assertEquals(0, atHelper(fixedLength, -1));
    assertEquals(0, atHelper(lengthTracking, -1));
    assertEquals(0, atHelper(fixedLengthWithOffset, -1));
    assertEquals(0, atHelper(lengthTrackingWithOffset, -1));
  };
  for (var ctor of ctors) {
    _loop22();
  }
}
At(TypedArrayAtHelper, true);
At(ArrayAtHelper, false);
function AtParameterConversionResizes(atHelper) {
  var _loop23 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * ctor.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertEquals(undefined, atHelper(fixedLength, evil));
  };
  for (var ctor of ctors) {
    _loop23(ctor);
  }
  // Resizing + a length-tracking TA -> no OOB, but bounds recomputation needed.
  var _loop24 = function (_ctor15) {
    var rab = CreateResizableArrayBuffer(4 * _ctor15.BYTES_PER_ELEMENT, 8 * _ctor15.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor15(rab);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor15.BYTES_PER_ELEMENT);
        return -1;
      }
    };
    // The TypedArray is *not* out of bounds since it's length-tracking.
    assertEquals(undefined, atHelper(lengthTracking, evil));
  };
  for (var _ctor15 of ctors) {
    _loop24(_ctor15);
  }
  var _loop25 = function (_ctor16) {
    var rab = CreateResizableArrayBuffer(4 * _ctor16.BYTES_PER_ELEMENT, 8 * _ctor16.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor16(rab);
    WriteToTypedArray(lengthTracking, 0, 25);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor16.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    // The TypedArray is *not* out of bounds since it's length-tracking.
    assertEquals(25, atHelper(lengthTracking, evil));
  };
  for (var _ctor16 of ctors) {
    _loop25(_ctor16);
  }
  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  var _loop26 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor17.BYTES_PER_ELEMENT, 8 * _ctor17.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor17(rab);
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return -1;
      }
    };
    // The TypedArray is *not* out of bounds since it's length-tracking.
    assertEquals(undefined, atHelper(lengthTracking, evil));
  };
  for (var _ctor17 of ctors) {
    _loop26();
  }
  var _loop27 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor18.BYTES_PER_ELEMENT, 8 * _ctor18.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor18(rab);
    WriteToTypedArray(lengthTracking, 0, 25);
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return 0;
      }
    };
    // The TypedArray is *not* out of bounds since it's length-tracking.
    assertEquals(undefined, atHelper(lengthTracking, evil));
  };
  for (var _ctor18 of ctors) {
    _loop27();
  }
}
AtParameterConversionResizes(TypedArrayAtHelper);
AtParameterConversionResizes(ArrayAtHelper);

// The corresponding tests for Array.prototype.slice are in
// typedarray-resizablearraybuffer-array-methods.js.
(function Slice() {
  var _loop28 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);

    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, i);
    }
    var fixedLengthSlice = fixedLength.slice();
    assertEquals([0, 1, 2, 3], ToNumbers(fixedLengthSlice));
    assertFalse(fixedLengthSlice.buffer.resizable);
    var fixedLengthWithOffsetSlice = fixedLengthWithOffset.slice();
    assertEquals([2, 3], ToNumbers(fixedLengthWithOffsetSlice));
    assertFalse(fixedLengthWithOffsetSlice.buffer.resizable);
    var lengthTrackingSlice = lengthTracking.slice();
    assertEquals([0, 1, 2, 3], ToNumbers(lengthTrackingSlice));
    assertFalse(lengthTrackingSlice.buffer.resizable);
    var lengthTrackingWithOffsetSlice = lengthTrackingWithOffset.slice();
    assertEquals([2, 3], ToNumbers(lengthTrackingWithOffsetSlice));
    assertFalse(lengthTrackingWithOffsetSlice.buffer.resizable);

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);
    assertThrows(() => {
      fixedLength.slice();
    });
    assertThrows(() => {
      fixedLengthWithOffset.slice();
    });
    assertEquals([0, 1, 2], ToNumbers(lengthTracking.slice()));
    assertEquals([2], ToNumbers(lengthTrackingWithOffset.slice()));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    assertThrows(() => {
      fixedLength.slice();
    });
    assertThrows(() => {
      fixedLengthWithOffset.slice();
    });
    assertEquals([0], ToNumbers(lengthTracking.slice()));
    assertThrows(() => {
      lengthTrackingWithOffset.slice();
    });

    // Shrink to zero.
    rab.resize(0);
    assertThrows(() => {
      fixedLength.slice();
    });
    assertThrows(() => {
      fixedLengthWithOffset.slice();
    });
    assertEquals([], ToNumbers(lengthTracking.slice()));
    assertThrows(() => {
      lengthTrackingWithOffset.slice();
    });

    // Verify that the previously created slices aren't affected by the
    // shrinking.
    assertEquals([0, 1, 2, 3], ToNumbers(fixedLengthSlice));
    assertEquals([2, 3], ToNumbers(fixedLengthWithOffsetSlice));
    assertEquals([0, 1, 2, 3], ToNumbers(lengthTrackingSlice));
    assertEquals([2, 3], ToNumbers(lengthTrackingWithOffsetSlice));

    // Grow so that all TAs are back in-bounds. New memory is zeroed.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    assertEquals([0, 0, 0, 0], ToNumbers(fixedLength.slice()));
    assertEquals([0, 0], ToNumbers(fixedLengthWithOffset.slice()));
    assertEquals([0, 0, 0, 0, 0, 0], ToNumbers(lengthTracking.slice()));
    assertEquals([0, 0, 0, 0], ToNumbers(lengthTrackingWithOffset.slice()));
  };
  for (var ctor of ctors) {
    _loop28();
  }
})();

// The corresponding tests for Array.prototype.slice are in
// typedarray-resizablearraybuffer-array-methods.js.
(function SliceParameterConversionShrinks() {
  var _loop29 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * ctor.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertThrows(() => {
      fixedLength.slice(evil);
    }, TypeError);
    assertEquals(2 * ctor.BYTES_PER_ELEMENT, rab.byteLength);
  };
  for (var ctor of ctors) {
    _loop29(ctor);
  }
  // Resizing + a length-tracking TA -> no OOB, but bounds recomputation needed.
  var _loop30 = function (_ctor19) {
    var rab = CreateResizableArrayBuffer(4 * _ctor19.BYTES_PER_ELEMENT, 8 * _ctor19.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor19(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(lengthTracking, i, i + 1);
    }
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor19.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertEquals([1, 2, 0, 0], ToNumbers(lengthTracking.slice(evil)));
    assertEquals(2 * _ctor19.BYTES_PER_ELEMENT, rab.byteLength);
  };
  for (var _ctor19 of ctors) {
    _loop30(_ctor19);
  }
  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  var _loop31 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor20.BYTES_PER_ELEMENT, 8 * _ctor20.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor20(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(lengthTracking, i, i + 1);
    }
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return 0;
      }
    };
    assertEquals([0, 0, 0, 0], ToNumbers(lengthTracking.slice(evil)));
    assertEquals(0, rab.byteLength);
  };
  for (var _ctor20 of ctors) {
    _loop31();
  }
})();
function SliceParameterConversionGrows(sliceHelper) {
  var _loop32 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var lengthTracking = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(lengthTracking, i, i + 1);
    }
    var evil = {
      valueOf: () => {
        rab.resize(6 * ctor.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertEquals([1, 2, 3, 4], ToNumbers(sliceHelper(lengthTracking, evil)));
    assertEquals(6 * ctor.BYTES_PER_ELEMENT, rab.byteLength);
  };
  for (var ctor of ctors) {
    _loop32(ctor);
  }
}
SliceParameterConversionGrows(TypedArraySliceHelper);
//SliceParameterConversionGrows(ArraySliceHelper);

// The corresponding test for Array.prototype.slice is not possible, since it
// doesn't call the species constructor if the "original array" is not an Array.
/* Rhino no subclassing yet
(function SliceSpeciesCreateResizes() {
  var _loop33 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var resizeWhenConstructorCalled = false;
    var MyArray = /*#__PURE__*//*function (_ctor23) {
      function MyArray(...params) {
        var _this2;
        _classCallCheck(this, MyArray);
        _this2 = _callSuper(this, MyArray, [...params]);
        if (resizeWhenConstructorCalled) {
          rab.resize(2 * ctor.BYTES_PER_ELEMENT);
        }
        return _this2;
      }
      _inherits(MyArray, _ctor23);
      return _createClass(MyArray);
    }(ctor);
    ;
    var fixedLength = new MyArray(rab, 0, 4);
    resizeWhenConstructorCalled = true;
    assertThrows(() => {
      fixedLength.slice();
    }, TypeError);
    assertEquals(2 * ctor.BYTES_PER_ELEMENT, rab.byteLength);
  };
  for (var ctor of ctors) {
    _loop33(ctor);
  }
  var _loop34 = function (_ctor21) {
    var rab = CreateResizableArrayBuffer(4 * _ctor21.BYTES_PER_ELEMENT, 8 * _ctor21.BYTES_PER_ELEMENT);
    var taWrite = new _ctor21(rab);
    for (var _i34 = 0; _i34 < 4; ++_i34) {
      WriteToTypedArray(taWrite, _i34, 1);
    }
    var resizeWhenConstructorCalled = false;
    var MyArray = /*#__PURE__*//*function (_ctor24) {
      function MyArray(...params) {
        var _this3;
        _classCallCheck(this, MyArray);
        _this3 = _callSuper(this, MyArray, [...params]);
        if (resizeWhenConstructorCalled) {
          rab.resize(2 * _ctor21.BYTES_PER_ELEMENT);
        }
        return _this3;
      }
      _inherits(MyArray, _ctor24);
      return _createClass(MyArray);
    }(_ctor21);
    ;
    var lengthTracking = new MyArray(rab);
    resizeWhenConstructorCalled = true;
    var a = lengthTracking.slice();
    assertEquals(2 * _ctor21.BYTES_PER_ELEMENT, rab.byteLength);
    // The length of the resulting TypedArray is determined before
    // TypedArraySpeciesCreate is called, and it doesn't change.
    assertEquals(4, a.length);
    assertEquals([1, 1, 0, 0], ToNumbers(a));
  };
  for (var _ctor21 of ctors) {
    _loop34(_ctor21);
  }

  // Test that the (start, end) parameters are computed based on the original
  // length.
  var _loop35 = function (_ctor22) {
    var rab = CreateResizableArrayBuffer(4 * _ctor22.BYTES_PER_ELEMENT, 8 * _ctor22.BYTES_PER_ELEMENT);
    var taWrite = new _ctor22(rab);
    for (var _i35 = 0; _i35 < 4; ++_i35) {
      WriteToTypedArray(taWrite, _i35, 1);
    }
    var resizeWhenConstructorCalled = false;
    var MyArray = /*#__PURE__*//*function (_ctor25) {
      function MyArray(...params) {
        var _this4;
        _classCallCheck(this, MyArray);
        _this4 = _callSuper(this, MyArray, [...params]);
        if (resizeWhenConstructorCalled) {
          rab.resize(2 * _ctor22.BYTES_PER_ELEMENT);
        }
        return _this4;
      }
      _inherits(MyArray, _ctor25);
      return _createClass(MyArray);
    }(_ctor22);
    ;
    var lengthTracking = new MyArray(rab);
    resizeWhenConstructorCalled = true;
    var a = lengthTracking.slice(-3, -1);
    assertEquals(2 * _ctor22.BYTES_PER_ELEMENT, rab.byteLength);
    // The length of the resulting TypedArray is determined before
    // TypedArraySpeciesCreate is called, and it doesn't change.
    assertEquals(2, a.length);
    assertEquals([1, 0], ToNumbers(a));
  };
  for (var _ctor22 of ctors) {
    _loop35(_ctor22);
  }

  // Test where the buffer gets resized "between elements".
  {
    var rab = CreateResizableArrayBuffer(8, 16);

    // Fill the buffer with 1-bits.
    var taWrite = new Uint8Array(rab);
    for (var i = 0; i < 8; ++i) {
      WriteToTypedArray(taWrite, i, 255);
    }
    var resizeWhenConstructorCalled = false;
    var MyArray = /*#__PURE__*//*function (_Uint16Array) {
      function MyArray(...params) {
        var _this;
        _classCallCheck(this, MyArray);
        _this = _callSuper(this, MyArray, [...params]);
        if (resizeWhenConstructorCalled) {
          // Resize so that the size is not a multiple of the element size.
          rab.resize(5);
        }
        return _this;
      }
      _inherits(MyArray, _Uint16Array);
      return _createClass(MyArray);
    }(/*#__PURE__*//*_wrapNativeSuper(Uint16Array));
    ;
    var lengthTracking = new MyArray(rab);
    assertEquals([65535, 65535, 65535, 65535], ToNumbers(lengthTracking));
    resizeWhenConstructorCalled = true;
    var a = lengthTracking.slice();
    assertEquals(5, rab.byteLength);
    assertEquals(4, a.length); // The old length is used.
    assertEquals(65535, a[0]);
    assertEquals(65535, a[1]);
    assertEquals(0, a[2]);
    assertEquals(0, a[3]);
  }
})();
*/
function TestCopyWithin(helper, oobThrows) {
  var _loop36 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);

    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, i);
    }

    // Orig. array: [0, 1, 2, 3]
    //              [0, 1, 2, 3] << fixedLength
    //                    [2, 3] << fixedLengthWithOffset
    //              [0, 1, 2, 3, ...] << lengthTracking
    //                    [2, 3, ...] << lengthTrackingWithOffset

    helper(fixedLength, 0, 2);
    assertEquals([2, 3, 2, 3], ToNumbers(fixedLength));
    for (var _i36 = 0; _i36 < 4; ++_i36) {
      WriteToTypedArray(taWrite, _i36, _i36);
    }
    helper(fixedLengthWithOffset, 0, 1);
    assertEquals([3, 3], ToNumbers(fixedLengthWithOffset));
    for (var _i37 = 0; _i37 < 4; ++_i37) {
      WriteToTypedArray(taWrite, _i37, _i37);
    }
    helper(lengthTracking, 0, 2);
    assertEquals([2, 3, 2, 3], ToNumbers(lengthTracking));
    helper(lengthTrackingWithOffset, 0, 1);
    assertEquals([3, 3], ToNumbers(lengthTrackingWithOffset));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);
    for (var _i38 = 0; _i38 < 3; ++_i38) {
      WriteToTypedArray(taWrite, _i38, _i38);
    }

    // Orig. array: [0, 1, 2]
    //              [0, 1, 2, ...] << lengthTracking
    //                    [2, ...] << lengthTrackingWithOffset

    if (oobThrows) {
      assertThrows(() => {
        helper(fixedLength, 0, 1);
      });
      assertThrows(() => {
        helper(fixedLengthWithOffset, 0, 1);
      });
    } else {
      helper(fixedLength, 0, 1);
      helper(fixedLengthWithOffset, 0, 1);
      // We'll check below that these were no-op.
    }
    assertEquals([0, 1, 2], ToNumbers(lengthTracking));
    helper(lengthTracking, 0, 1);
    assertEquals([1, 2, 2], ToNumbers(lengthTracking));
    helper(lengthTrackingWithOffset, 0, 1);
    assertEquals([2], ToNumbers(lengthTrackingWithOffset));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    WriteToTypedArray(taWrite, 0, 0);
    if (oobThrows) {
      assertThrows(() => {
        helper(fixedLength, 0, 1, 1);
      });
      assertThrows(() => {
        helper(fixedLengthWithOffset, 0, 1, 1);
      });
      assertThrows(() => {
        helper(lengthTrackingWithOffset, 0, 1, 1);
      });
    } else {
      helper(fixedLength, 0, 1, 1);
      helper(fixedLengthWithOffset, 0, 1, 1);
      helper(lengthTrackingWithOffset, 0, 1, 1);
    }
    assertEquals([0], ToNumbers(lengthTracking));
    helper(lengthTracking, 0, 0, 1);
    assertEquals([0], ToNumbers(lengthTracking));

    // Shrink to zero.
    rab.resize(0);
    if (oobThrows) {
      assertThrows(() => {
        helper(fixedLength, 0, 1, 1);
      });
      assertThrows(() => {
        helper(fixedLengthWithOffset, 0, 1, 1);
      });
      assertThrows(() => {
        helper(lengthTrackingWithOffset, 0, 1, 1);
      });
    } else {
      helper(fixedLength, 0, 1, 1);
      helper(fixedLengthWithOffset, 0, 1, 1);
      helper(lengthTrackingWithOffset, 0, 1, 1);
    }
    assertEquals([], ToNumbers(lengthTracking));
    helper(lengthTracking, 0, 0, 1);
    assertEquals([], ToNumbers(lengthTracking));

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    for (var _i39 = 0; _i39 < 6; ++_i39) {
      WriteToTypedArray(taWrite, _i39, _i39);
    }

    // Orig. array: [0, 1, 2, 3, 4, 5]
    //              [0, 1, 2, 3] << fixedLength
    //                    [2, 3] << fixedLengthWithOffset
    //              [0, 1, 2, 3, 4, 5, ...] << lengthTracking
    //                    [2, 3, 4, 5, ...] << lengthTrackingWithOffset

    helper(fixedLength, 0, 2);
    assertEquals([2, 3, 2, 3], ToNumbers(fixedLength));
    for (var _i40 = 0; _i40 < 6; ++_i40) {
      WriteToTypedArray(taWrite, _i40, _i40);
    }
    helper(fixedLengthWithOffset, 0, 1);
    assertEquals([3, 3], ToNumbers(fixedLengthWithOffset));
    for (var _i41 = 0; _i41 < 6; ++_i41) {
      WriteToTypedArray(taWrite, _i41, _i41);
    }

    //              [0, 1, 2, 3, 4, 5, ...] << lengthTracking
    //        target ^     ^ start
    helper(lengthTracking, 0, 2);
    assertEquals([2, 3, 4, 5, 4, 5], ToNumbers(lengthTracking));
    for (var _i42 = 0; _i42 < 6; ++_i42) {
      WriteToTypedArray(taWrite, _i42, _i42);
    }

    //                    [2, 3, 4, 5, ...] << lengthTrackingWithOffset
    //              target ^  ^ start
    helper(lengthTrackingWithOffset, 0, 1);
    assertEquals([3, 4, 5, 5], ToNumbers(lengthTrackingWithOffset));
  };
  for (var ctor of ctors) {
    _loop36();
  }
}
TestCopyWithin(TypedArrayCopyWithinHelper, true);
TestCopyWithin(ArrayCopyWithinHelper, false);
(function CopyWithinParameterConversionShrinks() {
  var _loop37 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * ctor.BYTES_PER_ELEMENT);
        return 2;
      }
    };
    assertThrows(() => {
      fixedLength.copyWithin(evil, 0, 1);
    }, TypeError);
    rab.resize(4 * ctor.BYTES_PER_ELEMENT);
    assertThrows(() => {
      fixedLength.copyWithin(0, evil, 3);
    }, TypeError);
    rab.resize(4 * ctor.BYTES_PER_ELEMENT);
    assertThrows(() => {
      fixedLength.copyWithin(0, 1, evil);
    }, TypeError);
  };
  for (var ctor of ctors) {
    _loop37(ctor);
  }
  var _loop38 = function (_ctor26) {
    var rab = CreateResizableArrayBuffer(4 * _ctor26.BYTES_PER_ELEMENT, 8 * _ctor26.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor26(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(lengthTracking, i, i);
    }
    // [0, 1, 2, 3]
    //        ^
    //        target
    // ^
    // start
    var evil = {
      valueOf: () => {
        rab.resize(3 * _ctor26.BYTES_PER_ELEMENT);
        return 2;
      }
    };
    lengthTracking.copyWithin(evil, 0);
    assertEquals([0, 1, 0], ToNumbers(lengthTracking));
  };
  for (var _ctor26 of ctors) {
    _loop38(_ctor26);
  }
  // Resizing + a length-tracking TA -> no OOB, but bounds recomputation needed.
  var _loop39 = function (_ctor27) {
    var rab = CreateResizableArrayBuffer(4 * _ctor27.BYTES_PER_ELEMENT, 8 * _ctor27.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor27(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(lengthTracking, i, i);
    }
    // [0, 1, 2, 3]
    //        ^
    //        start
    // ^
    // target
    var evil = {
      valueOf: () => {
        rab.resize(3 * _ctor27.BYTES_PER_ELEMENT);
        return 2;
      }
    };
    lengthTracking.copyWithin(0, evil);
    assertEquals([2, 1, 2], ToNumbers(lengthTracking));
  };
  for (var _ctor27 of ctors) {
    _loop39(_ctor27);
  }
  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  var _loop40 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor28.BYTES_PER_ELEMENT, 8 * _ctor28.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor28(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(lengthTracking, i, i);
    }
    // [0, 1, 2, 3]
    //        ^
    //        start
    // ^
    // target
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return 2;
      }
    };
    lengthTracking.copyWithin(0, evil);
    assertEquals([], ToNumbers(lengthTracking));
  };
  for (var _ctor28 of ctors) {
    _loop40();
  }
})();
(function CopyWithinParameterConversionGrows() {
  var _loop41 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var lengthTracking = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(lengthTracking, i, i);
    }
    var evil = {
      valueOf: () => {
        rab.resize(6 * ctor.BYTES_PER_ELEMENT);
        WriteToTypedArray(lengthTracking, 4, 4);
        WriteToTypedArray(lengthTracking, 5, 5);
        return 0;
      }
    };
    // Orig. array: [0, 1, 2, 3]  [4, 5]
    //               ^     ^       ^ new elements
    //          target     start
    lengthTracking.copyWithin(evil, 2);
    assertEquals([2, 3, 2, 3, 4, 5], ToNumbers(lengthTracking));
    rab.resize(4 * ctor.BYTES_PER_ELEMENT);
    for (var _i43 = 0; _i43 < 4; ++_i43) {
      WriteToTypedArray(lengthTracking, _i43, _i43);
    }

    // Orig. array: [0, 1, 2, 3]  [4, 5]
    //               ^     ^       ^ new elements
    //           start     target
    lengthTracking.copyWithin(2, evil);
    assertEquals([0, 1, 0, 1, 4, 5], ToNumbers(lengthTracking));
  };
  for (var ctor of ctors) {
    _loop41(ctor);
  }
})();
function EntriesKeysValues(entriesHelper, keysHelper, valuesHelper, valuesFromEntries, valuesFromValues, oobThrows) {
  var _loop42 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);

    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }

    // Orig. array: [0, 2, 4, 6]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, ...] << lengthTracking
    //                    [4, 6, ...] << lengthTrackingWithOffset

    assertEquals([0, 2, 4, 6], valuesFromEntries(fixedLength));
    assertEquals([0, 2, 4, 6], valuesFromValues(fixedLength));
    assertEquals([0, 1, 2, 3], Array.from(keysHelper(fixedLength)));
    assertEquals([4, 6], valuesFromEntries(fixedLengthWithOffset));
    assertEquals([4, 6], valuesFromValues(fixedLengthWithOffset));
    assertEquals([0, 1], Array.from(keysHelper(fixedLengthWithOffset)));
    assertEquals([0, 2, 4, 6], valuesFromEntries(lengthTracking));
    assertEquals([0, 2, 4, 6], valuesFromValues(lengthTracking));
    assertEquals([0, 1, 2, 3], Array.from(keysHelper(lengthTracking)));
    assertEquals([4, 6], valuesFromEntries(lengthTrackingWithOffset));
    assertEquals([4, 6], valuesFromValues(lengthTrackingWithOffset));
    assertEquals([0, 1], Array.from(keysHelper(lengthTrackingWithOffset)));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [0, 2, 4]
    //              [0, 2, 4, ...] << lengthTracking
    //                    [4, ...] << lengthTrackingWithOffset

    // TypedArray.prototype.{entries, keys, values} throw right away when
    // called. Array.prototype.{entries, keys, values} don't throw, but when
    // we try to iterate the returned ArrayIterator, that throws.
    if (oobThrows) {
      assertThrows(() => {
        entriesHelper(fixedLength);
      });
      assertThrows(() => {
        valuesHelper(fixedLength);
      });
      assertThrows(() => {
        keysHelper(fixedLength);
      });
      assertThrows(() => {
        entriesHelper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        valuesHelper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        keysHelper(fixedLengthWithOffset);
      });
    } else {
      entriesHelper(fixedLength);
      valuesHelper(fixedLength);
      keysHelper(fixedLength);
      entriesHelper(fixedLengthWithOffset);
      valuesHelper(fixedLengthWithOffset);
      keysHelper(fixedLengthWithOffset);
    }
    assertThrows(() => {
      Array.from(entriesHelper(fixedLength));
    });
    assertThrows(() => {
      Array.from(valuesHelper(fixedLength));
    });
    assertThrows(() => {
      Array.from(keysHelper(fixedLength));
    });
    assertThrows(() => {
      Array.from(entriesHelper(fixedLengthWithOffset));
    });
    assertThrows(() => {
      Array.from(valuesHelper(fixedLengthWithOffset));
    });
    assertThrows(() => {
      Array.from(keysHelper(fixedLengthWithOffset));
    });
    assertEquals([0, 2, 4], valuesFromEntries(lengthTracking));
    assertEquals([0, 2, 4], valuesFromValues(lengthTracking));
    assertEquals([0, 1, 2], Array.from(keysHelper(lengthTracking)));
    assertEquals([4], valuesFromEntries(lengthTrackingWithOffset));
    assertEquals([4], valuesFromValues(lengthTrackingWithOffset));
    assertEquals([0], Array.from(keysHelper(lengthTrackingWithOffset)));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    if (oobThrows) {
      assertThrows(() => {
        entriesHelper(fixedLength);
      });
      assertThrows(() => {
        valuesHelper(fixedLength);
      });
      assertThrows(() => {
        keysHelper(fixedLength);
      });
      assertThrows(() => {
        entriesHelper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        valuesHelper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        keysHelper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        entriesHelper(lengthTrackingWithOffset);
      });
      assertThrows(() => {
        valuesHelper(lengthTrackingWithOffset);
      });
      assertThrows(() => {
        keysHelper(lengthTrackingWithOffset);
      });
    } else {
      entriesHelper(fixedLength);
      valuesHelper(fixedLength);
      keysHelper(fixedLength);
      entriesHelper(fixedLengthWithOffset);
      valuesHelper(fixedLengthWithOffset);
      keysHelper(fixedLengthWithOffset);
      entriesHelper(lengthTrackingWithOffset);
      valuesHelper(lengthTrackingWithOffset);
      keysHelper(lengthTrackingWithOffset);
    }
    assertThrows(() => {
      Array.from(entriesHelper(fixedLength));
    });
    assertThrows(() => {
      Array.from(valuesHelper(fixedLength));
    });
    assertThrows(() => {
      Array.from(keysHelper(fixedLength));
    });
    assertThrows(() => {
      Array.from(entriesHelper(fixedLengthWithOffset));
    });
    assertThrows(() => {
      Array.from(valuesHelper(fixedLengthWithOffset));
    });
    assertThrows(() => {
      Array.from(keysHelper(fixedLengthWithOffset));
    });
    assertThrows(() => {
      Array.from(entriesHelper(lengthTrackingWithOffset));
    });
    assertThrows(() => {
      Array.from(valuesHelper(lengthTrackingWithOffset));
    });
    assertThrows(() => {
      Array.from(keysHelper(lengthTrackingWithOffset));
    });
    assertEquals([0], valuesFromEntries(lengthTracking));
    assertEquals([0], valuesFromValues(lengthTracking));
    assertEquals([0], Array.from(keysHelper(lengthTracking)));

    // Shrink to zero.
    rab.resize(0);
    if (oobThrows) {
      assertThrows(() => {
        entriesHelper(fixedLength);
      });
      assertThrows(() => {
        valuesHelper(fixedLength);
      });
      assertThrows(() => {
        keysHelper(fixedLength);
      });
      assertThrows(() => {
        entriesHelper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        valuesHelper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        keysHelper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        entriesHelper(lengthTrackingWithOffset);
      });
      assertThrows(() => {
        valuesHelper(lengthTrackingWithOffset);
      });
      assertThrows(() => {
        keysHelper(lengthTrackingWithOffset);
      });
    } else {
      entriesHelper(fixedLength);
      valuesHelper(fixedLength);
      keysHelper(fixedLength);
      entriesHelper(fixedLengthWithOffset);
      valuesHelper(fixedLengthWithOffset);
      keysHelper(fixedLengthWithOffset);
      entriesHelper(lengthTrackingWithOffset);
      valuesHelper(lengthTrackingWithOffset);
      keysHelper(lengthTrackingWithOffset);
    }
    assertThrows(() => {
      Array.from(entriesHelper(fixedLength));
    });
    assertThrows(() => {
      Array.from(valuesHelper(fixedLength));
    });
    assertThrows(() => {
      Array.from(keysHelper(fixedLength));
    });
    assertThrows(() => {
      Array.from(entriesHelper(fixedLengthWithOffset));
    });
    assertThrows(() => {
      Array.from(valuesHelper(fixedLengthWithOffset));
    });
    assertThrows(() => {
      Array.from(keysHelper(fixedLengthWithOffset));
    });
    assertThrows(() => {
      Array.from(entriesHelper(lengthTrackingWithOffset));
    });
    assertThrows(() => {
      Array.from(valuesHelper(lengthTrackingWithOffset));
    });
    assertThrows(() => {
      Array.from(keysHelper(lengthTrackingWithOffset));
    });
    assertEquals([], valuesFromEntries(lengthTracking));
    assertEquals([], valuesFromValues(lengthTracking));
    assertEquals([], Array.from(keysHelper(lengthTracking)));

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    for (var _i44 = 0; _i44 < 6; ++_i44) {
      WriteToTypedArray(taWrite, _i44, 2 * _i44);
    }

    // Orig. array: [0, 2, 4, 6, 8, 10]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, 8, 10, ...] << lengthTracking
    //                    [4, 6, 8, 10, ...] << lengthTrackingWithOffset

    assertEquals([0, 2, 4, 6], valuesFromEntries(fixedLength));
    assertEquals([0, 2, 4, 6], valuesFromValues(fixedLength));
    assertEquals([0, 1, 2, 3], Array.from(keysHelper(fixedLength)));
    assertEquals([4, 6], valuesFromEntries(fixedLengthWithOffset));
    assertEquals([4, 6], valuesFromValues(fixedLengthWithOffset));
    assertEquals([0, 1], Array.from(keysHelper(fixedLengthWithOffset)));
    assertEquals([0, 2, 4, 6, 8, 10], valuesFromEntries(lengthTracking));
    assertEquals([0, 2, 4, 6, 8, 10], valuesFromValues(lengthTracking));
    assertEquals([0, 1, 2, 3, 4, 5], Array.from(keysHelper(lengthTracking)));
    assertEquals([4, 6, 8, 10], valuesFromEntries(lengthTrackingWithOffset));
    assertEquals([4, 6, 8, 10], valuesFromValues(lengthTrackingWithOffset));
    assertEquals([0, 1, 2, 3], Array.from(keysHelper(lengthTrackingWithOffset)));
  };
  for (var ctor of ctors) {
    _loop42();
  }
}
EntriesKeysValues(TypedArrayEntriesHelper, TypedArrayKeysHelper, TypedArrayValuesHelper, ValuesFromTypedArrayEntries, ValuesFromTypedArrayValues, true);
EntriesKeysValues(ArrayEntriesHelper, ArrayKeysHelper, ArrayValuesHelper, ValuesFromArrayEntries, ValuesFromArrayValues, false);
function EntriesKeysValuesGrowMidIteration(entriesHelper, keysHelper, valuesHelper) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }

  // Iterating with entries() (the 4 loops below).
  for (var ctor of ctors) {
    var rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);

    // The fixed length array is not affected by resizing.
    TestIterationAndResize(entriesHelper(fixedLength), [[0, 0], [1, 2], [2, 4], [3, 6]], rab, 2, 6 * ctor.BYTES_PER_ELEMENT);
  }
  for (var _ctor29 of ctors) {
    var _rab = CreateRabForTest(_ctor29);
    var fixedLengthWithOffset = new _ctor29(_rab, 2 * _ctor29.BYTES_PER_ELEMENT, 2);

    // The fixed length array is not affected by resizing.
    TestIterationAndResize(entriesHelper(fixedLengthWithOffset), [[0, 4], [1, 6]], _rab, 2, 6 * _ctor29.BYTES_PER_ELEMENT);
  }
  for (var _ctor30 of ctors) {
    var _rab2 = CreateRabForTest(_ctor30);
    var lengthTracking = new _ctor30(_rab2, 0);
    TestIterationAndResize(entriesHelper(lengthTracking), [[0, 0], [1, 2], [2, 4], [3, 6], [4, 0], [5, 0]], _rab2, 2, 6 * _ctor30.BYTES_PER_ELEMENT);
  }
  for (var _ctor31 of ctors) {
    var _rab3 = CreateRabForTest(_ctor31);
    var lengthTrackingWithOffset = new _ctor31(_rab3, 2 * _ctor31.BYTES_PER_ELEMENT);
    TestIterationAndResize(entriesHelper(lengthTrackingWithOffset), [[0, 4], [1, 6], [2, 0], [3, 0]], _rab3, 2, 6 * _ctor31.BYTES_PER_ELEMENT);
  }

  // Iterating with keys() (the 4 loops below).
  for (var _ctor32 of ctors) {
    var _rab4 = CreateRabForTest(_ctor32);
    var _fixedLength = new _ctor32(_rab4, 0, 4);

    // The fixed length array is not affected by resizing.
    TestIterationAndResize(keysHelper(_fixedLength), [0, 1, 2, 3], _rab4, 2, 6 * _ctor32.BYTES_PER_ELEMENT);
  }
  for (var _ctor33 of ctors) {
    var _rab5 = CreateRabForTest(_ctor33);
    var _fixedLengthWithOffset = new _ctor33(_rab5, 2 * _ctor33.BYTES_PER_ELEMENT, 2);

    // The fixed length array is not affected by resizing.
    TestIterationAndResize(keysHelper(_fixedLengthWithOffset), [0, 1], _rab5, 2, 6 * _ctor33.BYTES_PER_ELEMENT);
  }
  for (var _ctor34 of ctors) {
    var _rab6 = CreateRabForTest(_ctor34);
    var _lengthTracking = new _ctor34(_rab6, 0);
    TestIterationAndResize(keysHelper(_lengthTracking), [0, 1, 2, 3, 4, 5], _rab6, 2, 6 * _ctor34.BYTES_PER_ELEMENT);
  }
  for (var _ctor35 of ctors) {
    var _rab7 = CreateRabForTest(_ctor35);
    var _lengthTrackingWithOffset = new _ctor35(_rab7, 2 * _ctor35.BYTES_PER_ELEMENT);
    TestIterationAndResize(keysHelper(_lengthTrackingWithOffset), [0, 1, 2, 3], _rab7, 2, 6 * _ctor35.BYTES_PER_ELEMENT);
  }

  // Iterating with values() (the 4 loops below).
  for (var _ctor36 of ctors) {
    var _rab8 = CreateRabForTest(_ctor36);
    var _fixedLength2 = new _ctor36(_rab8, 0, 4);

    // The fixed length array is not affected by resizing.
    TestIterationAndResize(valuesHelper(_fixedLength2), [0, 2, 4, 6], _rab8, 2, 6 * _ctor36.BYTES_PER_ELEMENT);
  }
  for (var _ctor37 of ctors) {
    var _rab9 = CreateRabForTest(_ctor37);
    var _fixedLengthWithOffset2 = new _ctor37(_rab9, 2 * _ctor37.BYTES_PER_ELEMENT, 2);

    // The fixed length array is not affected by resizing.
    TestIterationAndResize(valuesHelper(_fixedLengthWithOffset2), [4, 6], _rab9, 2, 6 * _ctor37.BYTES_PER_ELEMENT);
  }
  for (var _ctor38 of ctors) {
    var _rab0 = CreateRabForTest(_ctor38);
    var _lengthTracking2 = new _ctor38(_rab0, 0);
    TestIterationAndResize(valuesHelper(_lengthTracking2), [0, 2, 4, 6, 0, 0], _rab0, 2, 6 * _ctor38.BYTES_PER_ELEMENT);
  }
  for (var _ctor39 of ctors) {
    var _rab1 = CreateRabForTest(_ctor39);
    var _lengthTrackingWithOffset2 = new _ctor39(_rab1, 2 * _ctor39.BYTES_PER_ELEMENT);
    TestIterationAndResize(valuesHelper(_lengthTrackingWithOffset2), [4, 6, 0, 0], _rab1, 2, 6 * _ctor39.BYTES_PER_ELEMENT);
  }
}
EntriesKeysValuesGrowMidIteration(TypedArrayEntriesHelper, TypedArrayKeysHelper, TypedArrayValuesHelper);
EntriesKeysValuesGrowMidIteration(ArrayEntriesHelper, ArrayKeysHelper, ArrayValuesHelper);
function EntriesKeysValuesShrinkMidIteration(entriesHelper, keysHelper, valuesHelper) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }

  // Iterating with entries() (the 5 loops below).
  var _loop43 = function (ctor) {
    var rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);

    // The fixed length array goes out of bounds when the RAB is resized.
    assertThrows(() => {
      TestIterationAndResize(entriesHelper(fixedLength), null, rab, 2, 3 * ctor.BYTES_PER_ELEMENT);
    });
  };
  for (var ctor of ctors) {
    _loop43(ctor);
  }
  var _loop44 = function (_ctor40) {
    var rab = CreateRabForTest(_ctor40);
    var fixedLengthWithOffset = new _ctor40(rab, 2 * _ctor40.BYTES_PER_ELEMENT, 2);

    // The fixed length array goes out of bounds when the RAB is resized.
    assertThrows(() => {
      TestIterationAndResize(entriesHelper(fixedLengthWithOffset), null, rab, 1, 3 * _ctor40.BYTES_PER_ELEMENT);
    });
  };
  for (var _ctor40 of ctors) {
    _loop44(_ctor40);
  }

  // Resizing + a length-tracking TA -> no OOB.
  for (var _ctor41 of ctors) {
    var rab = CreateRabForTest(_ctor41);
    var lengthTracking = new _ctor41(rab, 0);
    TestIterationAndResize(entriesHelper(lengthTracking), [[0, 0], [1, 2], [2, 4]], rab, 2, 3 * _ctor41.BYTES_PER_ELEMENT);
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  for (var _ctor42 of ctors) {
    var _rab10 = CreateRabForTest(_ctor42);
    var _lengthTracking3 = new _ctor42(_rab10, 0);
    TestIterationAndResize(entriesHelper(_lengthTracking3), [[0, 0]], _rab10, 1, 0);
  }
  for (var _ctor43 of ctors) {
    var _rab11 = CreateRabForTest(_ctor43);
    var lengthTrackingWithOffset = new _ctor43(_rab11, 2 * _ctor43.BYTES_PER_ELEMENT);
    TestIterationAndResize(entriesHelper(lengthTrackingWithOffset), [[0, 4], [1, 6]], _rab11, 2, 3 * _ctor43.BYTES_PER_ELEMENT);
  }

  // Iterating with keys() (the 5 loops below).
  var _loop45 = function (_ctor44) {
    var rab = CreateRabForTest(_ctor44);
    var fixedLength = new _ctor44(rab, 0, 4);

    // The fixed length array goes out of bounds when the RAB is resized.
    assertThrows(() => {
      TestIterationAndResize(keysHelper(fixedLength), null, rab, 2, 3 * _ctor44.BYTES_PER_ELEMENT);
    });
  };
  for (var _ctor44 of ctors) {
    _loop45(_ctor44);
  }
  var _loop46 = function (_ctor45) {
    var rab = CreateRabForTest(_ctor45);
    var fixedLengthWithOffset = new _ctor45(rab, 2 * _ctor45.BYTES_PER_ELEMENT, 2);

    // The fixed length array goes out of bounds when the RAB is resized.
    assertThrows(() => {
      TestIterationAndResize(keysHelper(fixedLengthWithOffset), null, rab, 2, 3 * _ctor45.BYTES_PER_ELEMENT);
    });
  };
  for (var _ctor45 of ctors) {
    _loop46(_ctor45);
  }

  // Resizing + a length-tracking TA -> no OOB.
  for (var _ctor46 of ctors) {
    var _rab12 = CreateRabForTest(_ctor46);
    var _lengthTracking4 = new _ctor46(_rab12, 0);
    TestIterationAndResize(keysHelper(_lengthTracking4), [0, 1, 2], _rab12, 2, 3 * _ctor46.BYTES_PER_ELEMENT);
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  for (var _ctor47 of ctors) {
    var _rab13 = CreateRabForTest(_ctor47);
    var _lengthTracking5 = new _ctor47(_rab13, 0);
    TestIterationAndResize(keysHelper(_lengthTracking5), [0], _rab13, 1, 0);
  }
  for (var _ctor48 of ctors) {
    var _rab14 = CreateRabForTest(_ctor48);
    var _lengthTrackingWithOffset3 = new _ctor48(_rab14, 2 * _ctor48.BYTES_PER_ELEMENT);
    TestIterationAndResize(keysHelper(_lengthTrackingWithOffset3), [0, 1], _rab14, 2, 3 * _ctor48.BYTES_PER_ELEMENT);
  }

  // Iterating with values() (the 5 loops below).
  var _loop47 = function (_ctor49) {
    var rab = CreateRabForTest(_ctor49);
    var fixedLength = new _ctor49(rab, 0, 4);

    // The fixed length array goes out of bounds when the RAB is resized.
    assertThrows(() => {
      TestIterationAndResize(valuesHelper(fixedLength), null, rab, 2, 3 * _ctor49.BYTES_PER_ELEMENT);
    });
  };
  for (var _ctor49 of ctors) {
    _loop47(_ctor49);
  }
  var _loop48 = function (_ctor50) {
    var rab = CreateRabForTest(_ctor50);
    var fixedLengthWithOffset = new _ctor50(rab, 2 * _ctor50.BYTES_PER_ELEMENT, 2);

    // The fixed length array goes out of bounds when the RAB is resized.
    assertThrows(() => {
      TestIterationAndResize(valuesHelper(fixedLengthWithOffset), null, rab, 2, 3 * _ctor50.BYTES_PER_ELEMENT);
    });
  };
  for (var _ctor50 of ctors) {
    _loop48(_ctor50);
  }

  // Resizing + a length-tracking TA -> no OOB.
  for (var _ctor51 of ctors) {
    var _rab15 = CreateRabForTest(_ctor51);
    var _lengthTracking6 = new _ctor51(_rab15, 0);
    TestIterationAndResize(valuesHelper(_lengthTracking6), [0, 2, 4], _rab15, 2, 3 * _ctor51.BYTES_PER_ELEMENT);
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  for (var _ctor52 of ctors) {
    var _rab16 = CreateRabForTest(_ctor52);
    var _lengthTracking7 = new _ctor52(_rab16, 0);
    TestIterationAndResize(valuesHelper(_lengthTracking7), [0, 2], _rab16, 2, 0);
  }
  for (var _ctor53 of ctors) {
    var _rab17 = CreateRabForTest(_ctor53);
    var _lengthTrackingWithOffset4 = new _ctor53(_rab17, 2 * _ctor53.BYTES_PER_ELEMENT);
    TestIterationAndResize(valuesHelper(_lengthTrackingWithOffset4), [4, 6], _rab17, 2, 3 * _ctor53.BYTES_PER_ELEMENT);
  }
}
EntriesKeysValuesShrinkMidIteration(TypedArrayEntriesHelper, TypedArrayKeysHelper, TypedArrayValuesHelper);
EntriesKeysValuesShrinkMidIteration(ArrayEntriesHelper, ArrayKeysHelper, ArrayValuesHelper);
function EverySome(everyHelper, someHelper, oobThrows) {
  var _loop49 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);

    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }

    // Orig. array: [0, 2, 4, 6]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, ...] << lengthTracking
    //                    [4, 6, ...] << lengthTrackingWithOffset

    function div3(n) {
      return Number(n) % 3 == 0;
    }
    function even(n) {
      return Number(n) % 2 == 0;
    }
    function over10(n) {
      return Number(n) > 10;
    }
    assertFalse(everyHelper(fixedLength, div3));
    assertTrue(everyHelper(fixedLength, even));
    assertTrue(someHelper(fixedLength, div3));
    assertFalse(someHelper(fixedLength, over10));
    assertFalse(everyHelper(fixedLengthWithOffset, div3));
    assertTrue(everyHelper(fixedLengthWithOffset, even));
    assertTrue(someHelper(fixedLengthWithOffset, div3));
    assertFalse(someHelper(fixedLengthWithOffset, over10));
    assertFalse(everyHelper(lengthTracking, div3));
    assertTrue(everyHelper(lengthTracking, even));
    assertTrue(someHelper(lengthTracking, div3));
    assertFalse(someHelper(lengthTracking, over10));
    assertFalse(everyHelper(lengthTrackingWithOffset, div3));
    assertTrue(everyHelper(lengthTrackingWithOffset, even));
    assertTrue(someHelper(lengthTrackingWithOffset, div3));
    assertFalse(someHelper(lengthTrackingWithOffset, over10));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [0, 2, 4]
    //              [0, 2, 4, ...] << lengthTracking
    //                    [4, ...] << lengthTrackingWithOffset

    if (oobThrows) {
      assertThrows(() => {
        everyHelper(fixedLength, div3);
      });
      assertThrows(() => {
        someHelper(fixedLength, div3);
      });
      assertThrows(() => {
        everyHelper(fixedLengthWithOffset, div3);
      });
      assertThrows(() => {
        someHelper(fixedLengthWithOffset, div3);
      });
    } else {
      assertTrue(everyHelper(fixedLength, div3));
      assertFalse(someHelper(fixedLength, div3));
      assertTrue(everyHelper(fixedLengthWithOffset, div3));
      assertFalse(someHelper(fixedLengthWithOffset, div3));
    }
    assertFalse(everyHelper(lengthTracking, div3));
    assertTrue(everyHelper(lengthTracking, even));
    assertTrue(someHelper(lengthTracking, div3));
    assertFalse(someHelper(lengthTracking, over10));
    assertFalse(everyHelper(lengthTrackingWithOffset, div3));
    assertTrue(everyHelper(lengthTrackingWithOffset, even));
    assertFalse(someHelper(lengthTrackingWithOffset, div3));
    assertFalse(someHelper(lengthTrackingWithOffset, over10));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    if (oobThrows) {
      assertThrows(() => {
        everyHelper(fixedLength, div3);
      });
      assertThrows(() => {
        someHelper(fixedLength, div3);
      });
      assertThrows(() => {
        everyHelper(fixedLengthWithOffset, div3);
      });
      assertThrows(() => {
        someHelper(fixedLengthWithOffset, div3);
      });
      assertThrows(() => {
        everyHelper(lengthTrackingWithOffset, div3);
      });
      assertThrows(() => {
        someHelper(lengthTrackingWithOffset, div3);
      });
    } else {
      assertTrue(everyHelper(fixedLength, div3));
      assertFalse(someHelper(fixedLength, div3));
      assertTrue(everyHelper(fixedLengthWithOffset, div3));
      assertFalse(someHelper(fixedLengthWithOffset, div3));
      assertTrue(everyHelper(lengthTrackingWithOffset, div3));
      assertFalse(someHelper(lengthTrackingWithOffset, div3));
    }
    assertTrue(everyHelper(lengthTracking, div3));
    assertTrue(everyHelper(lengthTracking, even));
    assertTrue(someHelper(lengthTracking, div3));
    assertFalse(someHelper(lengthTracking, over10));

    // Shrink to zero.
    rab.resize(0);
    if (oobThrows) {
      assertThrows(() => {
        everyHelper(fixedLength, div3);
      });
      assertThrows(() => {
        someHelper(fixedLength, div3);
      });
      assertThrows(() => {
        everyHelper(fixedLengthWithOffset, div3);
      });
      assertThrows(() => {
        someHelper(fixedLengthWithOffset, div3);
      });
      assertThrows(() => {
        everyHelper(lengthTrackingWithOffset, div3);
      });
      assertThrows(() => {
        someHelper(lengthTrackingWithOffset, div3);
      });
    } else {
      assertTrue(everyHelper(fixedLength, div3));
      assertFalse(someHelper(fixedLength, div3));
      assertTrue(everyHelper(fixedLengthWithOffset, div3));
      assertFalse(someHelper(fixedLengthWithOffset, div3));
      assertTrue(everyHelper(lengthTrackingWithOffset, div3));
      assertFalse(someHelper(lengthTrackingWithOffset, div3));
    }
    assertTrue(everyHelper(lengthTracking, div3));
    assertTrue(everyHelper(lengthTracking, even));
    assertFalse(someHelper(lengthTracking, div3));
    assertFalse(someHelper(lengthTracking, over10));

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    for (var _i45 = 0; _i45 < 6; ++_i45) {
      WriteToTypedArray(taWrite, _i45, 2 * _i45);
    }

    // Orig. array: [0, 2, 4, 6, 8, 10]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, 8, 10, ...] << lengthTracking
    //                    [4, 6, 8, 10, ...] << lengthTrackingWithOffset

    assertFalse(everyHelper(fixedLength, div3));
    assertTrue(everyHelper(fixedLength, even));
    assertTrue(someHelper(fixedLength, div3));
    assertFalse(someHelper(fixedLength, over10));
    assertFalse(everyHelper(fixedLengthWithOffset, div3));
    assertTrue(everyHelper(fixedLengthWithOffset, even));
    assertTrue(someHelper(fixedLengthWithOffset, div3));
    assertFalse(someHelper(fixedLengthWithOffset, over10));
    assertFalse(everyHelper(lengthTracking, div3));
    assertTrue(everyHelper(lengthTracking, even));
    assertTrue(someHelper(lengthTracking, div3));
    assertFalse(someHelper(lengthTracking, over10));
    assertFalse(everyHelper(lengthTrackingWithOffset, div3));
    assertTrue(everyHelper(lengthTrackingWithOffset, even));
    assertTrue(someHelper(lengthTrackingWithOffset, div3));
    assertFalse(someHelper(lengthTrackingWithOffset, over10));
  };
  for (var ctor of ctors) {
    _loop49();
  }
}
EverySome(TypedArrayEveryHelper, TypedArraySomeHelper, true);
EverySome(ArrayEveryHelper, ArraySomeHelper, false);
function EveryShrinkMidIteration(everyHelper, hasUndefined) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return true;
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    values = [];
    resizeAfter = 2;
    resizeTo = 3 * ctor.BYTES_PER_ELEMENT;
    assertTrue(everyHelper(fixedLength, CollectValuesAndResize));
    if (hasUndefined) {
      assertEquals([0, 2, undefined, undefined], values);
    } else {
      assertEquals([0, 2], values);
    }
  }
  for (var _ctor54 of ctors) {
    rab = CreateRabForTest(_ctor54);
    var fixedLengthWithOffset = new _ctor54(rab, 2 * _ctor54.BYTES_PER_ELEMENT, 2);
    values = [];
    resizeAfter = 1;
    resizeTo = 3 * _ctor54.BYTES_PER_ELEMENT;
    assertTrue(everyHelper(fixedLengthWithOffset, CollectValuesAndResize));
    if (hasUndefined) {
      assertEquals([4, undefined], values);
    } else {
      assertEquals([4], values);
    }
  }

  // Resizing + a length-tracking TA -> no OOB.
  for (var _ctor55 of ctors) {
    rab = CreateRabForTest(_ctor55);
    var lengthTracking = new _ctor55(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 3 * _ctor55.BYTES_PER_ELEMENT;
    assertTrue(everyHelper(lengthTracking, CollectValuesAndResize));
    if (hasUndefined) {
      assertEquals([0, 2, 4, undefined], values);
    } else {
      assertEquals([0, 2, 4], values);
    }
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  for (var _ctor56 of ctors) {
    rab = CreateRabForTest(_ctor56);
    var _lengthTracking8 = new _ctor56(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 0;
    assertTrue(everyHelper(_lengthTracking8, CollectValuesAndResize));
    if (hasUndefined) {
      assertEquals([0, 2, undefined, undefined], values);
    } else {
      assertEquals([0, 2], values);
    }
  }
  for (var _ctor57 of ctors) {
    rab = CreateRabForTest(_ctor57);
    var lengthTrackingWithOffset = new _ctor57(rab, 2 * _ctor57.BYTES_PER_ELEMENT);
    values = [];
    resizeAfter = 1;
    resizeTo = 3 * _ctor57.BYTES_PER_ELEMENT;
    assertTrue(everyHelper(lengthTrackingWithOffset, CollectValuesAndResize));
    if (hasUndefined) {
      assertEquals([4, undefined], values);
    } else {
      assertEquals([4], values);
    }
  }
}
EveryShrinkMidIteration(TypedArrayEveryHelper, true);
// Rhino trying to fix typed arrays here
// // EveryShrinkMidIteration(ArrayEveryHelper, false);
function EveryGrowMidIteration(everyHelper) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return true;
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    values = [];
    resizeAfter = 2;
    resizeTo = 5 * ctor.BYTES_PER_ELEMENT;
    assertTrue(everyHelper(fixedLength, CollectValuesAndResize));
    assertEquals([0, 2, 4, 6], values);
  }
  for (var _ctor58 of ctors) {
    rab = CreateRabForTest(_ctor58);
    var fixedLengthWithOffset = new _ctor58(rab, 2 * _ctor58.BYTES_PER_ELEMENT, 2);
    values = [];
    resizeAfter = 1;
    resizeTo = 5 * _ctor58.BYTES_PER_ELEMENT;
    assertTrue(everyHelper(fixedLengthWithOffset, CollectValuesAndResize));
    assertEquals([4, 6], values);
  }
  for (var _ctor59 of ctors) {
    rab = CreateRabForTest(_ctor59);
    var lengthTracking = new _ctor59(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 5 * _ctor59.BYTES_PER_ELEMENT;
    assertTrue(everyHelper(lengthTracking, CollectValuesAndResize));
    assertEquals([0, 2, 4, 6], values);
  }
  for (var _ctor60 of ctors) {
    rab = CreateRabForTest(_ctor60);
    var lengthTrackingWithOffset = new _ctor60(rab, 2 * _ctor60.BYTES_PER_ELEMENT);
    values = [];
    resizeAfter = 1;
    resizeTo = 5 * _ctor60.BYTES_PER_ELEMENT;
    assertTrue(everyHelper(lengthTrackingWithOffset, CollectValuesAndResize));
    assertEquals([4, 6], values);
  }
}
EveryGrowMidIteration(TypedArrayEveryHelper);
EveryGrowMidIteration(ArrayEveryHelper);
function SomeShrinkMidIteration(someHelper, hasUndefined) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return false;
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    values = [];
    resizeAfter = 2;
    resizeTo = 3 * ctor.BYTES_PER_ELEMENT;
    assertFalse(someHelper(fixedLength, CollectValuesAndResize));
    if (hasUndefined) {
      assertEquals([0, 2, undefined, undefined], values);
    } else {
      assertEquals([0, 2], values);
    }
  }
  for (var _ctor61 of ctors) {
    rab = CreateRabForTest(_ctor61);
    var fixedLengthWithOffset = new _ctor61(rab, 2 * _ctor61.BYTES_PER_ELEMENT, 2);
    values = [];
    resizeAfter = 1;
    resizeTo = 3 * _ctor61.BYTES_PER_ELEMENT;
    assertFalse(someHelper(fixedLengthWithOffset, CollectValuesAndResize));
    if (hasUndefined) {
      assertEquals([4, undefined], values);
    } else {
      assertEquals([4], values);
    }
  }

  // Resizing + a length-tracking TA -> no OOB.
  for (var _ctor62 of ctors) {
    rab = CreateRabForTest(_ctor62);
    var lengthTracking = new _ctor62(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 3 * _ctor62.BYTES_PER_ELEMENT;
    assertFalse(someHelper(lengthTracking, CollectValuesAndResize));
    if (hasUndefined) {
      assertEquals([0, 2, 4, undefined], values);
    } else {
      assertEquals([0, 2, 4], values);
    }
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  for (var _ctor63 of ctors) {
    rab = CreateRabForTest(_ctor63);
    var _lengthTracking9 = new _ctor63(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 0;
    assertFalse(someHelper(_lengthTracking9, CollectValuesAndResize));
    if (hasUndefined) {
      assertEquals([0, 2, undefined, undefined], values);
    } else {
      assertEquals([0, 2], values);
    }
  }
  for (var _ctor64 of ctors) {
    rab = CreateRabForTest(_ctor64);
    var lengthTrackingWithOffset = new _ctor64(rab, 2 * _ctor64.BYTES_PER_ELEMENT);
    values = [];
    resizeAfter = 1;
    resizeTo = 3 * _ctor64.BYTES_PER_ELEMENT;
    assertFalse(someHelper(lengthTrackingWithOffset, CollectValuesAndResize));
    if (hasUndefined) {
      assertEquals([4, undefined], values);
    } else {
      assertEquals([4], values);
    }
  }
}
SomeShrinkMidIteration(TypedArraySomeHelper, true);
// Rhino trying to fix typed arrays here
//SomeShrinkMidIteration(ArraySomeHelper, false);
function SomeGrowMidIteration(someHelper) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values = [];
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return false;
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    values = [];
    resizeAfter = 2;
    resizeTo = 5 * ctor.BYTES_PER_ELEMENT;
    assertFalse(someHelper(fixedLength, CollectValuesAndResize));
    assertEquals([0, 2, 4, 6], values);
  }
  for (var _ctor65 of ctors) {
    rab = CreateRabForTest(_ctor65);
    var fixedLengthWithOffset = new _ctor65(rab, 2 * _ctor65.BYTES_PER_ELEMENT, 2);
    values = [];
    resizeAfter = 1;
    resizeTo = 5 * _ctor65.BYTES_PER_ELEMENT;
    assertFalse(someHelper(fixedLengthWithOffset, CollectValuesAndResize));
    assertEquals([4, 6], values);
  }
  for (var _ctor66 of ctors) {
    rab = CreateRabForTest(_ctor66);
    var lengthTracking = new _ctor66(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 5 * _ctor66.BYTES_PER_ELEMENT;
    assertFalse(someHelper(lengthTracking, CollectValuesAndResize));
    assertEquals([0, 2, 4, 6], values);
  }
  for (var _ctor67 of ctors) {
    rab = CreateRabForTest(_ctor67);
    var lengthTrackingWithOffset = new _ctor67(rab, 2 * _ctor67.BYTES_PER_ELEMENT);
    values = [];
    rab = rab;
    resizeAfter = 1;
    resizeTo = 5 * _ctor67.BYTES_PER_ELEMENT;
    assertFalse(someHelper(lengthTrackingWithOffset, CollectValuesAndResize));
    assertEquals([4, 6], values);
  }
}
SomeGrowMidIteration(TypedArraySomeHelper);
SomeGrowMidIteration(ArraySomeHelper);
function FindFindIndexFindLastFindLastIndex(findHelper, findIndexHelper, findLastHelper, findLastIndexHelper, oobThrows) {
  var _loop50 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);

    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }

    // Orig. array: [0, 2, 4, 6]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, ...] << lengthTracking
    //                    [4, 6, ...] << lengthTrackingWithOffset

    function isTwoOrFour(n) {
      return n == 2 || n == 4;
    }
    assertEquals(2, Number(findHelper(fixedLength, isTwoOrFour)));
    assertEquals(4, Number(findHelper(fixedLengthWithOffset, isTwoOrFour)));
    assertEquals(2, Number(findHelper(lengthTracking, isTwoOrFour)));
    assertEquals(4, Number(findHelper(lengthTrackingWithOffset, isTwoOrFour)));
    assertEquals(1, findIndexHelper(fixedLength, isTwoOrFour));
    assertEquals(0, findIndexHelper(fixedLengthWithOffset, isTwoOrFour));
    assertEquals(1, findIndexHelper(lengthTracking, isTwoOrFour));
    assertEquals(0, findIndexHelper(lengthTrackingWithOffset, isTwoOrFour));
    assertEquals(4, Number(findLastHelper(fixedLength, isTwoOrFour)));
    assertEquals(4, Number(findLastHelper(fixedLengthWithOffset, isTwoOrFour)));
    assertEquals(4, Number(findLastHelper(lengthTracking, isTwoOrFour)));
    assertEquals(4, Number(findLastHelper(lengthTrackingWithOffset, isTwoOrFour)));
    assertEquals(2, findLastIndexHelper(fixedLength, isTwoOrFour));
    assertEquals(0, findLastIndexHelper(fixedLengthWithOffset, isTwoOrFour));
    assertEquals(2, findLastIndexHelper(lengthTracking, isTwoOrFour));
    assertEquals(0, findLastIndexHelper(lengthTrackingWithOffset, isTwoOrFour));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [0, 2, 4]
    //              [0, 2, 4, ...] << lengthTracking
    //                    [4, ...] << lengthTrackingWithOffset

    if (oobThrows) {
      assertThrows(() => {
        findHelper(fixedLength, isTwoOrFour);
      });
      assertThrows(() => {
        findIndexHelper(fixedLength, isTwoOrFour);
      });
      assertThrows(() => {
        findLastHelper(fixedLength, isTwoOrFour);
      });
      assertThrows(() => {
        findLastIndexHelper(fixedLength, isTwoOrFour);
      });
      assertThrows(() => {
        findHelper(fixedLengthWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findIndexHelper(fixedLengthWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findLastHelper(fixedLengthWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findLastIndexHelper(fixedLengthWithOffset, isTwoOrFour);
      });
    } else {
      assertEquals(undefined, findHelper(fixedLength, isTwoOrFour));
      assertEquals(-1, findIndexHelper(fixedLength, isTwoOrFour));
      assertEquals(undefined, findLastHelper(fixedLength, isTwoOrFour));
      assertEquals(-1, findLastIndexHelper(fixedLength, isTwoOrFour));
      assertEquals(undefined, findHelper(fixedLengthWithOffset, isTwoOrFour));
      assertEquals(-1, findIndexHelper(fixedLengthWithOffset, isTwoOrFour));
      assertEquals(undefined, findLastHelper(fixedLengthWithOffset, isTwoOrFour));
      assertEquals(-1, findLastIndexHelper(fixedLengthWithOffset, isTwoOrFour));
    }
    assertEquals(2, Number(findHelper(lengthTracking, isTwoOrFour)));
    assertEquals(4, Number(findHelper(lengthTrackingWithOffset, isTwoOrFour)));
    assertEquals(1, findIndexHelper(lengthTracking, isTwoOrFour));
    assertEquals(0, findIndexHelper(lengthTrackingWithOffset, isTwoOrFour));
    assertEquals(4, Number(findLastHelper(lengthTracking, isTwoOrFour)));
    assertEquals(4, Number(findLastHelper(lengthTrackingWithOffset, isTwoOrFour)));
    assertEquals(2, findLastIndexHelper(lengthTracking, isTwoOrFour));
    assertEquals(0, findLastIndexHelper(lengthTrackingWithOffset, isTwoOrFour));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    if (oobThrows) {
      assertThrows(() => {
        findHelper(fixedLength, isTwoOrFour);
      });
      assertThrows(() => {
        findIndexHelper(fixedLength, isTwoOrFour);
      });
      assertThrows(() => {
        findLastHelper(fixedLength, isTwoOrFour);
      });
      assertThrows(() => {
        findLastIndexHelper(fixedLength, isTwoOrFour);
      });
      assertThrows(() => {
        findHelper(fixedLengthWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findIndexHelper(fixedLengthWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findLastHelper(fixedLengthWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findLastIndexHelper(fixedLengthWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findHelper(lengthTrackingWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findIndexHelper(lengthTrackingWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findLastHelper(lengthTrackingWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findLastIndexHelper(lengthTrackingWithOffset, isTwoOrFour);
      });
    } else {
      assertEquals(undefined, findHelper(fixedLength, isTwoOrFour));
      assertEquals(-1, findIndexHelper(fixedLength, isTwoOrFour));
      assertEquals(undefined, findLastHelper(fixedLength, isTwoOrFour));
      assertEquals(-1, findLastIndexHelper(fixedLength, isTwoOrFour));
      assertEquals(undefined, findHelper(fixedLengthWithOffset, isTwoOrFour));
      assertEquals(-1, findIndexHelper(fixedLengthWithOffset, isTwoOrFour));
      assertEquals(undefined, findLastHelper(fixedLengthWithOffset, isTwoOrFour));
      assertEquals(-1, findLastIndexHelper(fixedLengthWithOffset, isTwoOrFour));
      assertEquals(undefined, findHelper(lengthTrackingWithOffset, isTwoOrFour));
      assertEquals(-1, findIndexHelper(lengthTrackingWithOffset, isTwoOrFour));
      assertEquals(undefined, findLastHelper(lengthTrackingWithOffset, isTwoOrFour));
      assertEquals(-1, findLastIndexHelper(lengthTrackingWithOffset, isTwoOrFour));
    }
    assertEquals(undefined, findHelper(lengthTracking, isTwoOrFour));
    assertEquals(-1, findIndexHelper(lengthTracking, isTwoOrFour));
    assertEquals(undefined, findLastHelper(lengthTracking, isTwoOrFour));
    assertEquals(-1, findLastIndexHelper(lengthTracking, isTwoOrFour));

    // Shrink to zero.
    rab.resize(0);
    if (oobThrows) {
      assertThrows(() => {
        findHelper(fixedLength, isTwoOrFour);
      });
      assertThrows(() => {
        findIndexHelper(fixedLength, isTwoOrFour);
      });
      assertThrows(() => {
        findLastHelper(fixedLength, isTwoOrFour);
      });
      assertThrows(() => {
        findLastIndexHelper(fixedLength, isTwoOrFour);
      });
      assertThrows(() => {
        findHelper(fixedLengthWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findIndexHelper(fixedLengthWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findLastHelper(fixedLengthWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findLastIndexHelper(fixedLengthWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findHelper(lengthTrackingWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findIndexHelper(lengthTrackingWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findLastHelper(lengthTrackingWithOffset, isTwoOrFour);
      });
      assertThrows(() => {
        findLastIndexHelper(lengthTrackingWithOffset, isTwoOrFour);
      });
    } else {
      assertEquals(undefined, findHelper(fixedLength, isTwoOrFour));
      assertEquals(-1, findIndexHelper(fixedLength, isTwoOrFour));
      assertEquals(undefined, findLastHelper(fixedLength, isTwoOrFour));
      assertEquals(-1, findLastIndexHelper(fixedLength, isTwoOrFour));
      assertEquals(undefined, findHelper(fixedLengthWithOffset, isTwoOrFour));
      assertEquals(-1, findIndexHelper(fixedLengthWithOffset, isTwoOrFour));
      assertEquals(undefined, findLastHelper(fixedLengthWithOffset, isTwoOrFour));
      assertEquals(-1, findLastIndexHelper(fixedLengthWithOffset, isTwoOrFour));
      assertEquals(undefined, findHelper(lengthTrackingWithOffset, isTwoOrFour));
      assertEquals(-1, findIndexHelper(lengthTrackingWithOffset, isTwoOrFour));
      assertEquals(undefined, findLastHelper(lengthTrackingWithOffset, isTwoOrFour));
      assertEquals(-1, findLastIndexHelper(lengthTrackingWithOffset, isTwoOrFour));
    }
    assertEquals(undefined, findHelper(lengthTracking, isTwoOrFour));
    assertEquals(-1, findIndexHelper(lengthTracking, isTwoOrFour));
    assertEquals(undefined, findLastHelper(lengthTracking, isTwoOrFour));
    assertEquals(-1, findLastIndexHelper(lengthTracking, isTwoOrFour));

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    for (var _i46 = 0; _i46 < 4; ++_i46) {
      WriteToTypedArray(taWrite, _i46, 0);
    }
    WriteToTypedArray(taWrite, 4, 2);
    WriteToTypedArray(taWrite, 5, 4);

    // Orig. array: [0, 0, 0, 0, 2, 4]
    //              [0, 0, 0, 0] << fixedLength
    //                    [0, 0] << fixedLengthWithOffset
    //              [0, 0, 0, 0, 2, 4, ...] << lengthTracking
    //                    [0, 0, 2, 4, ...] << lengthTrackingWithOffset

    assertEquals(undefined, findHelper(fixedLength, isTwoOrFour));
    assertEquals(undefined, findHelper(fixedLengthWithOffset, isTwoOrFour));
    assertEquals(2, Number(findHelper(lengthTracking, isTwoOrFour)));
    assertEquals(2, Number(findHelper(lengthTrackingWithOffset, isTwoOrFour)));
    assertEquals(-1, findIndexHelper(fixedLength, isTwoOrFour));
    assertEquals(-1, findIndexHelper(fixedLengthWithOffset, isTwoOrFour));
    assertEquals(4, findIndexHelper(lengthTracking, isTwoOrFour));
    assertEquals(2, findIndexHelper(lengthTrackingWithOffset, isTwoOrFour));
    assertEquals(undefined, findLastHelper(fixedLength, isTwoOrFour));
    assertEquals(undefined, findLastHelper(fixedLengthWithOffset, isTwoOrFour));
    assertEquals(4, Number(findLastHelper(lengthTracking, isTwoOrFour)));
    assertEquals(4, Number(findLastHelper(lengthTrackingWithOffset, isTwoOrFour)));
    assertEquals(-1, findLastIndexHelper(fixedLength, isTwoOrFour));
    assertEquals(-1, findLastIndexHelper(fixedLengthWithOffset, isTwoOrFour));
    assertEquals(5, findLastIndexHelper(lengthTracking, isTwoOrFour));
    assertEquals(3, findLastIndexHelper(lengthTrackingWithOffset, isTwoOrFour));
  };
  for (var ctor of ctors) {
    _loop50();
  }
}
FindFindIndexFindLastFindLastIndex(TypedArrayFindHelper, TypedArrayFindIndexHelper, TypedArrayFindLastHelper, TypedArrayFindLastIndexHelper, true);
FindFindIndexFindLastFindLastIndex(ArrayFindHelper, ArrayFindIndexHelper, ArrayFindLastHelper, ArrayFindLastIndexHelper, false);
function FindShrinkMidIteration(findHelper) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return false;
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    values = [];
    resizeAfter = 2;
    resizeTo = 3 * ctor.BYTES_PER_ELEMENT;
    assertEquals(undefined, findHelper(fixedLength, CollectValuesAndResize));
    assertEquals([0, 2, undefined, undefined], values);
  }
  for (var _ctor68 of ctors) {
    rab = CreateRabForTest(_ctor68);
    var fixedLengthWithOffset = new _ctor68(rab, 2 * _ctor68.BYTES_PER_ELEMENT, 2);
    values = [];
    resizeAfter = 1;
    resizeTo = 3 * _ctor68.BYTES_PER_ELEMENT;
    assertEquals(undefined, findHelper(fixedLengthWithOffset, CollectValuesAndResize));
    assertEquals([4, undefined], values);
  }

  // Resizing + a length-tracking TA -> no OOB.
  for (var _ctor69 of ctors) {
    rab = CreateRabForTest(_ctor69);
    var lengthTracking = new _ctor69(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 3 * _ctor69.BYTES_PER_ELEMENT;
    assertEquals(undefined, findHelper(lengthTracking, CollectValuesAndResize));
    assertEquals([0, 2, 4, undefined], values);
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  for (var _ctor70 of ctors) {
    rab = CreateRabForTest(_ctor70);
    var _lengthTracking0 = new _ctor70(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 0;
    assertEquals(undefined, findHelper(_lengthTracking0, CollectValuesAndResize));
    assertEquals([0, 2, undefined, undefined], values);
  }
  for (var _ctor71 of ctors) {
    rab = CreateRabForTest(_ctor71);
    var lengthTrackingWithOffset = new _ctor71(rab, 2 * _ctor71.BYTES_PER_ELEMENT);
    values = [];
    resizeAfter = 1;
    resizeTo = 3 * _ctor71.BYTES_PER_ELEMENT;
    assertEquals(undefined, findHelper(lengthTrackingWithOffset, CollectValuesAndResize));
    assertEquals([4, undefined], values);
  }
}
FindShrinkMidIteration(TypedArrayFindHelper);
FindShrinkMidIteration(ArrayFindHelper);
function FindGrowMidIteration(findHelper) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return false;
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    values = [];
    resizeAfter = 2;
    resizeTo = 5 * ctor.BYTES_PER_ELEMENT;
    assertEquals(undefined, findHelper(fixedLength, CollectValuesAndResize));
    assertEquals([0, 2, 4, 6], values);
  }
  for (var _ctor72 of ctors) {
    rab = CreateRabForTest(_ctor72);
    var fixedLengthWithOffset = new _ctor72(rab, 2 * _ctor72.BYTES_PER_ELEMENT, 2);
    values = [];
    resizeAfter = 1;
    resizeTo = 5 * _ctor72.BYTES_PER_ELEMENT;
    assertEquals(undefined, findHelper(fixedLengthWithOffset, CollectValuesAndResize));
    assertEquals([4, 6], values);
  }
  for (var _ctor73 of ctors) {
    rab = CreateRabForTest(_ctor73);
    var lengthTracking = new _ctor73(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 5 * _ctor73.BYTES_PER_ELEMENT;
    assertEquals(undefined, findHelper(lengthTracking, CollectValuesAndResize));
    assertEquals([0, 2, 4, 6], values);
  }
  for (var _ctor74 of ctors) {
    rab = CreateRabForTest(_ctor74);
    var lengthTrackingWithOffset = new _ctor74(rab, 2 * _ctor74.BYTES_PER_ELEMENT);
    values = [];
    resizeAfter = 1;
    resizeTo = 5 * _ctor74.BYTES_PER_ELEMENT;
    assertEquals(undefined, findHelper(lengthTrackingWithOffset, CollectValuesAndResize));
    assertEquals([4, 6], values);
  }
}
FindGrowMidIteration(TypedArrayFindHelper);
FindGrowMidIteration(ArrayFindHelper);
function FindIndexShrinkMidIteration(findIndexHelper) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return false;
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    values = [];
    resizeAfter = 2;
    resizeTo = 3 * ctor.BYTES_PER_ELEMENT;
    assertEquals(-1, findIndexHelper(fixedLength, CollectValuesAndResize));
    assertEquals([0, 2, undefined, undefined], values);
  }
  for (var _ctor75 of ctors) {
    rab = CreateRabForTest(_ctor75);
    var fixedLengthWithOffset = new _ctor75(rab, 2 * _ctor75.BYTES_PER_ELEMENT, 2);
    values = [];
    resizeAfter = 1;
    resizeTo = 3 * _ctor75.BYTES_PER_ELEMENT;
    assertEquals(-1, findIndexHelper(fixedLengthWithOffset, CollectValuesAndResize));
    assertEquals([4, undefined], values);
  }

  // Resizing + a length-tracking TA -> no OOB.
  for (var _ctor76 of ctors) {
    rab = CreateRabForTest(_ctor76);
    var lengthTracking = new _ctor76(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 3 * _ctor76.BYTES_PER_ELEMENT;
    assertEquals(-1, findIndexHelper(lengthTracking, CollectValuesAndResize));
    assertEquals([0, 2, 4, undefined], values);
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  for (var _ctor77 of ctors) {
    rab = CreateRabForTest(_ctor77);
    var _lengthTracking1 = new _ctor77(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 0;
    assertEquals(-1, findIndexHelper(_lengthTracking1, CollectValuesAndResize));
    assertEquals([0, 2, undefined, undefined], values);
  }
  for (var _ctor78 of ctors) {
    rab = CreateRabForTest(_ctor78);
    var lengthTrackingWithOffset = new _ctor78(rab, 2 * _ctor78.BYTES_PER_ELEMENT);
    values = [];
    resizeAfter = 1;
    resizeTo = 3 * _ctor78.BYTES_PER_ELEMENT;
    assertEquals(-1, findIndexHelper(lengthTrackingWithOffset, CollectValuesAndResize));
    assertEquals([4, undefined], values);
  }
}
FindIndexShrinkMidIteration(TypedArrayFindIndexHelper);
FindIndexShrinkMidIteration(ArrayFindIndexHelper);
function FindIndexGrowMidIteration(findIndexHelper) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return false;
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    values = [];
    resizeAfter = 2;
    resizeTo = 5 * ctor.BYTES_PER_ELEMENT;
    assertEquals(-1, findIndexHelper(fixedLength, CollectValuesAndResize));
    assertEquals([0, 2, 4, 6], values);
  }
  for (var _ctor79 of ctors) {
    rab = CreateRabForTest(_ctor79);
    var fixedLengthWithOffset = new _ctor79(rab, 2 * _ctor79.BYTES_PER_ELEMENT, 2);
    values = [];
    resizeAfter = 1;
    resizeTo = 5 * _ctor79.BYTES_PER_ELEMENT;
    assertEquals(-1, findIndexHelper(fixedLengthWithOffset, CollectValuesAndResize));
    assertEquals([4, 6], values);
  }
  for (var _ctor80 of ctors) {
    rab = CreateRabForTest(_ctor80);
    var lengthTracking = new _ctor80(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 5 * _ctor80.BYTES_PER_ELEMENT;
    assertEquals(-1, findIndexHelper(lengthTracking, CollectValuesAndResize));
    assertEquals([0, 2, 4, 6], values);
  }
  for (var _ctor81 of ctors) {
    rab = CreateRabForTest(_ctor81);
    var lengthTrackingWithOffset = new _ctor81(rab, 2 * _ctor81.BYTES_PER_ELEMENT);
    values = [];
    resizeAfter = 1;
    resizeTo = 5 * _ctor81.BYTES_PER_ELEMENT;
    assertEquals(-1, findIndexHelper(lengthTrackingWithOffset, CollectValuesAndResize));
    assertEquals([4, 6], values);
  }
}
FindIndexGrowMidIteration(TypedArrayFindIndexHelper);
FindIndexGrowMidIteration(ArrayFindIndexHelper);
function FindLastShrinkMidIteration(findLastHelper) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return false;
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    values = [];
    resizeAfter = 2;
    resizeTo = 3 * ctor.BYTES_PER_ELEMENT;
    assertEquals(undefined, findLastHelper(fixedLength, CollectValuesAndResize));
    assertEquals([6, 4, undefined, undefined], values);
  }
  for (var _ctor82 of ctors) {
    rab = CreateRabForTest(_ctor82);
    var fixedLengthWithOffset = new _ctor82(rab, 2 * _ctor82.BYTES_PER_ELEMENT, 2);
    values = [];
    resizeAfter = 1;
    resizeTo = 3 * _ctor82.BYTES_PER_ELEMENT;
    assertEquals(undefined, findLastHelper(fixedLengthWithOffset, CollectValuesAndResize));
    assertEquals([6, undefined], values);
  }

  // Resizing + a length-tracking TA -> no OOB.
  for (var _ctor83 of ctors) {
    rab = CreateRabForTest(_ctor83);
    var lengthTracking = new _ctor83(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 3 * _ctor83.BYTES_PER_ELEMENT;
    assertEquals(undefined, findLastHelper(lengthTracking, CollectValuesAndResize));
    assertEquals([6, 4, 2, 0], values);
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  for (var _ctor84 of ctors) {
    rab = CreateRabForTest(_ctor84);
    var _lengthTracking10 = new _ctor84(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 0;
    assertEquals(undefined, findLastHelper(_lengthTracking10, CollectValuesAndResize));
    assertEquals([6, 4, undefined, undefined], values);
  }
  for (var _ctor85 of ctors) {
    rab = CreateRabForTest(_ctor85);
    var lengthTrackingWithOffset = new _ctor85(rab, 2 * _ctor85.BYTES_PER_ELEMENT);
    values = [];
    resizeAfter = 1;
    resizeTo = 3 * _ctor85.BYTES_PER_ELEMENT;
    assertEquals(undefined, findLastHelper(lengthTrackingWithOffset, CollectValuesAndResize));
    assertEquals([6, 4], values);
  }
}
FindLastShrinkMidIteration(TypedArrayFindLastHelper);
FindLastShrinkMidIteration(ArrayFindLastHelper);
function FindLastGrowMidIteration(findLastHelper) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return false;
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    values = [];
    resizeAfter = 2;
    resizeTo = 5 * ctor.BYTES_PER_ELEMENT;
    assertEquals(undefined, findLastHelper(fixedLength, CollectValuesAndResize));
    assertEquals([6, 4, 2, 0], values);
  }
  for (var _ctor86 of ctors) {
    rab = CreateRabForTest(_ctor86);
    var fixedLengthWithOffset = new _ctor86(rab, 2 * _ctor86.BYTES_PER_ELEMENT, 2);
    values = [];
    resizeAfter = 1;
    resizeTo = 5 * _ctor86.BYTES_PER_ELEMENT;
    assertEquals(undefined, findLastHelper(fixedLengthWithOffset, CollectValuesAndResize));
    assertEquals([6, 4], values);
  }
  for (var _ctor87 of ctors) {
    rab = CreateRabForTest(_ctor87);
    var lengthTracking = new _ctor87(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 5 * _ctor87.BYTES_PER_ELEMENT;
    assertEquals(undefined, findLastHelper(lengthTracking, CollectValuesAndResize));
    assertEquals([6, 4, 2, 0], values);
  }
  for (var _ctor88 of ctors) {
    rab = CreateRabForTest(_ctor88);
    var lengthTrackingWithOffset = new _ctor88(rab, 2 * _ctor88.BYTES_PER_ELEMENT);
    values = [];
    resizeAfter = 1;
    resizeTo = 5 * _ctor88.BYTES_PER_ELEMENT;
    assertEquals(undefined, findLastHelper(lengthTrackingWithOffset, CollectValuesAndResize));
    assertEquals([6, 4], values);
  }
}
FindLastGrowMidIteration(TypedArrayFindLastHelper);
FindLastGrowMidIteration(ArrayFindLastHelper);
function FindLastIndexShrinkMidIteration(findLastIndexHelper) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return false;
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    values = [];
    resizeAfter = 2;
    resizeTo = 3 * ctor.BYTES_PER_ELEMENT;
    assertEquals(-1, findLastIndexHelper(fixedLength, CollectValuesAndResize));
    assertEquals([6, 4, undefined, undefined], values);
  }
  for (var _ctor89 of ctors) {
    rab = CreateRabForTest(_ctor89);
    var fixedLengthWithOffset = new _ctor89(rab, 2 * _ctor89.BYTES_PER_ELEMENT, 2);
    values = [];
    resizeAfter = 1;
    resizeTo = 3 * _ctor89.BYTES_PER_ELEMENT;
    assertEquals(-1, findLastIndexHelper(fixedLengthWithOffset, CollectValuesAndResize));
    assertEquals([6, undefined], values);
  }
  for (var _ctor90 of ctors) {
    rab = CreateRabForTest(_ctor90);
    var lengthTracking = new _ctor90(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 3 * _ctor90.BYTES_PER_ELEMENT;
    assertEquals(-1, findLastIndexHelper(lengthTracking, CollectValuesAndResize));
    assertEquals([6, 4, 2, 0], values);
  }

  // Resizing + a length-tracking TA -> no OOB.
  for (var _ctor91 of ctors) {
    rab = CreateRabForTest(_ctor91);
    var _lengthTracking11 = new _ctor91(rab, 0);
    values = [];
    resizeAfter = 1;
    resizeTo = 2 * _ctor91.BYTES_PER_ELEMENT;
    assertEquals(-1, findLastIndexHelper(_lengthTracking11, CollectValuesAndResize));
    assertEquals([6, undefined, 2, 0], values);
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  for (var _ctor92 of ctors) {
    rab = CreateRabForTest(_ctor92);
    var _lengthTracking12 = new _ctor92(rab, 0);
    values = [];
    resizeAfter = 1;
    resizeTo = 0;
    assertEquals(-1, findLastIndexHelper(_lengthTracking12, CollectValuesAndResize));
    assertEquals([6, undefined, undefined, undefined], values);
  }
  for (var _ctor93 of ctors) {
    rab = CreateRabForTest(_ctor93);
    var lengthTrackingWithOffset = new _ctor93(rab, 2 * _ctor93.BYTES_PER_ELEMENT);
    values = [];
    resizeAfter = 1;
    resizeTo = 3 * _ctor93.BYTES_PER_ELEMENT;
    assertEquals(-1, findLastIndexHelper(lengthTrackingWithOffset, CollectValuesAndResize));
    assertEquals([6, 4], values);
  }
}
FindLastIndexShrinkMidIteration(TypedArrayFindLastIndexHelper);
FindLastIndexShrinkMidIteration(ArrayFindLastIndexHelper);
function FindLastIndexGrowMidIteration(findLastIndexHelper) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return false;
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    values = [];
    resizeAfter = 2;
    resizeTo = 5 * ctor.BYTES_PER_ELEMENT;
    assertEquals(-1, findLastIndexHelper(fixedLength, CollectValuesAndResize));
    assertEquals([6, 4, 2, 0], values);
  }
  for (var _ctor94 of ctors) {
    rab = CreateRabForTest(_ctor94);
    var fixedLengthWithOffset = new _ctor94(rab, 2 * _ctor94.BYTES_PER_ELEMENT, 2);
    values = [];
    resizeAfter = 1;
    resizeTo = 5 * _ctor94.BYTES_PER_ELEMENT;
    assertEquals(-1, findLastIndexHelper(fixedLengthWithOffset, CollectValuesAndResize));
    assertEquals([6, 4], values);
  }
  for (var _ctor95 of ctors) {
    rab = CreateRabForTest(_ctor95);
    var lengthTracking = new _ctor95(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 5 * _ctor95.BYTES_PER_ELEMENT;
    assertEquals(-1, findLastIndexHelper(lengthTracking, CollectValuesAndResize));
    assertEquals([6, 4, 2, 0], values);
  }
  for (var _ctor96 of ctors) {
    rab = CreateRabForTest(_ctor96);
    var lengthTrackingWithOffset = new _ctor96(rab, 2 * _ctor96.BYTES_PER_ELEMENT);
    values = [];
    resizeAfter = 1;
    resizeTo = 5 * _ctor96.BYTES_PER_ELEMENT;
    assertEquals(-1, findLastIndexHelper(lengthTrackingWithOffset, CollectValuesAndResize));
    assertEquals([6, 4], values);
  }
}
FindLastIndexGrowMidIteration(TypedArrayFindLastIndexHelper);
FindLastIndexGrowMidIteration(ArrayFindLastIndexHelper);
function Filter(filterHelper, oobThrows) {
  var _loop51 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);

    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, i);
    }

    // Orig. array: [0, 1, 2, 3]
    //              [0, 1, 2, 3] << fixedLength
    //                    [2, 3] << fixedLengthWithOffset
    //              [0, 1, 2, 3, ...] << lengthTracking
    //                    [2, 3, ...] << lengthTrackingWithOffset

    function isEven(n) {
      return n != undefined && Number(n) % 2 == 0;
    }
    assertEquals([0, 2], ToNumbers(filterHelper(fixedLength, isEven)));
    assertEquals([2], ToNumbers(filterHelper(fixedLengthWithOffset, isEven)));
    assertEquals([0, 2], ToNumbers(filterHelper(lengthTracking, isEven)));
    assertEquals([2], ToNumbers(filterHelper(lengthTrackingWithOffset, isEven)));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [0, 1, 2]
    //              [0, 1, 2, ...] << lengthTracking
    //                    [2, ...] << lengthTrackingWithOffset

    if (oobThrows) {
      assertThrows(() => {
        filterHelper(fixedLength, isEven);
      });
      assertThrows(() => {
        filterHelper(fixedLengthWithOffset, isEven);
      });
    } else {
      assertEquals([], filterHelper(fixedLength, isEven));
      assertEquals([], filterHelper(fixedLengthWithOffset, isEven));
    }
    assertEquals([0, 2], ToNumbers(filterHelper(lengthTracking, isEven)));
    assertEquals([2], ToNumbers(filterHelper(lengthTrackingWithOffset, isEven)));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    if (oobThrows) {
      assertThrows(() => {
        filterHelper(fixedLength, isEven);
      });
      assertThrows(() => {
        filterHelper(fixedLengthWithOffset, isEven);
      });
      assertThrows(() => {
        filterHelper(lengthTrackingWithOffset, isEven);
      });
    } else {
      assertEquals([], filterHelper(fixedLength, isEven));
      assertEquals([], filterHelper(fixedLengthWithOffset, isEven));
      assertEquals([], filterHelper(lengthTrackingWithOffset, isEven));
    }
    assertEquals([0], ToNumbers(filterHelper(lengthTracking, isEven)));

    // Shrink to zero.
    rab.resize(0);
    if (oobThrows) {
      assertThrows(() => {
        filterHelper(fixedLength, isEven);
      });
      assertThrows(() => {
        filterHelper(fixedLengthWithOffset, isEven);
      });
      assertThrows(() => {
        filterHelper(lengthTrackingWithOffset, isEven);
      });
    } else {
      assertEquals([], filterHelper(fixedLength, isEven));
      assertEquals([], filterHelper(fixedLengthWithOffset, isEven));
      assertEquals([], filterHelper(lengthTrackingWithOffset, isEven));
    }
    assertEquals([], ToNumbers(filterHelper(lengthTracking, isEven)));

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    for (var _i47 = 0; _i47 < 6; ++_i47) {
      WriteToTypedArray(taWrite, _i47, _i47);
    }

    // Orig. array: [0, 1, 2, 3, 4, 5]
    //              [0, 1, 2, 3] << fixedLength
    //                    [2, 3] << fixedLengthWithOffset
    //              [0, 1, 2, 3, 4, 5, ...] << lengthTracking
    //                    [2, 3, 4, 5, ...] << lengthTrackingWithOffset

    assertEquals([0, 2], ToNumbers(filterHelper(fixedLength, isEven)));
    assertEquals([2], ToNumbers(filterHelper(fixedLengthWithOffset, isEven)));
    assertEquals([0, 2, 4], ToNumbers(filterHelper(lengthTracking, isEven)));
    assertEquals([2, 4], ToNumbers(filterHelper(lengthTrackingWithOffset, isEven)));
  };
  for (var ctor of ctors) {
    _loop51();
  }
}
Filter(TypedArrayFilterHelper, true);
Filter(ArrayFilterHelper, false);

// The corresponding tests for Array.prototype.filter are in
// typedarray-resizablearraybuffer-array-methods.js.
(function FilterShrinkMidIteration() {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return false;
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    values = [];
    resizeAfter = 2;
    resizeTo = 3 * ctor.BYTES_PER_ELEMENT;
    assertEquals([], ToNumbers(fixedLength.filter(CollectValuesAndResize)));
    assertEquals([0, 2, undefined, undefined], values);
  }
  for (var _ctor97 of ctors) {
    rab = CreateRabForTest(_ctor97);
    var fixedLengthWithOffset = new _ctor97(rab, 2 * _ctor97.BYTES_PER_ELEMENT, 2);
    values = [];
    resizeAfter = 1;
    resizeTo = 3 * _ctor97.BYTES_PER_ELEMENT;
    assertEquals([], ToNumbers(fixedLengthWithOffset.filter(CollectValuesAndResize)));
    assertEquals([4, undefined], values);
  }

  // Resizing + a length-tracking TA -> no OOB.
  for (var _ctor98 of ctors) {
    rab = CreateRabForTest(_ctor98);
    var lengthTracking = new _ctor98(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 3 * _ctor98.BYTES_PER_ELEMENT;
    assertEquals([], ToNumbers(lengthTracking.filter(CollectValuesAndResize)));
    assertEquals([0, 2, 4, undefined], values);
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  for (var _ctor99 of ctors) {
    rab = CreateRabForTest(_ctor99);
    var _lengthTracking13 = new _ctor99(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 0;
    assertEquals([], ToNumbers(_lengthTracking13.filter(CollectValuesAndResize)));
    assertEquals([0, 2, undefined, undefined], values);
  }
  for (var _ctor100 of ctors) {
    rab = CreateRabForTest(_ctor100);
    var lengthTrackingWithOffset = new _ctor100(rab, 2 * _ctor100.BYTES_PER_ELEMENT);
    values = [];
    resizeAfter = 1;
    resizeTo = 3 * _ctor100.BYTES_PER_ELEMENT;
    assertEquals([], ToNumbers(lengthTrackingWithOffset.filter(CollectValuesAndResize)));
    assertEquals([4, undefined], values);
  }
})();
function FilterGrowMidIteration(filterHelper) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return false;
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    values = [];
    resizeAfter = 2;
    resizeTo = 5 * ctor.BYTES_PER_ELEMENT;
    assertEquals([], ToNumbers(filterHelper(fixedLength, CollectValuesAndResize)));
    assertEquals([0, 2, 4, 6], values);
  }
  for (var _ctor101 of ctors) {
    rab = CreateRabForTest(_ctor101);
    var fixedLengthWithOffset = new _ctor101(rab, 2 * _ctor101.BYTES_PER_ELEMENT, 2);
    values = [];
    resizeAfter = 1;
    resizeTo = 5 * _ctor101.BYTES_PER_ELEMENT;
    assertEquals([], ToNumbers(filterHelper(fixedLengthWithOffset, CollectValuesAndResize)));
    assertEquals([4, 6], values);
  }
  for (var _ctor102 of ctors) {
    rab = CreateRabForTest(_ctor102);
    var lengthTracking = new _ctor102(rab, 0);
    values = [];
    resizeAfter = 2;
    resizeTo = 5 * _ctor102.BYTES_PER_ELEMENT;
    assertEquals([], ToNumbers(filterHelper(lengthTracking, CollectValuesAndResize)));
    assertEquals([0, 2, 4, 6], values);
  }
  for (var _ctor103 of ctors) {
    rab = CreateRabForTest(_ctor103);
    var lengthTrackingWithOffset = new _ctor103(rab, 2 * _ctor103.BYTES_PER_ELEMENT);
    values = [];
    resizeAfter = 1;
    resizeTo = 5 * _ctor103.BYTES_PER_ELEMENT;
    assertEquals([], ToNumbers(filterHelper(lengthTrackingWithOffset, CollectValuesAndResize)));
    assertEquals([4, 6], values);
  }
}
FilterGrowMidIteration(TypedArrayFilterHelper);
FilterGrowMidIteration(ArrayFilterHelper);
function ForEachReduceReduceRight(forEachHelper, reduceHelper, reduceRightHelper, oobThrows) {
  var _loop52 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);

    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }

    // Orig. array: [0, 2, 4, 6]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, ...] << lengthTracking
    //                    [4, 6, ...] << lengthTrackingWithOffset

    function Helper(array) {
      var forEachValues = [];
      var reduceValues = [];
      var reduceRightValues = [];
      forEachHelper(array, n => {
        forEachValues.push(n);
      });
      reduceHelper(array, (acc, n) => {
        reduceValues.push(n);
      }, "initial value");
      reduceRightHelper(array, (acc, n) => {
        reduceRightValues.push(n);
      }, "initial value");
      assertEquals(reduceValues, forEachValues);
      reduceRightValues.reverse();
      assertEquals(reduceValues, reduceRightValues);
      return ToNumbers(forEachValues);
    }
    assertEquals([0, 2, 4, 6], Helper(fixedLength));
    assertEquals([4, 6], Helper(fixedLengthWithOffset));
    assertEquals([0, 2, 4, 6], Helper(lengthTracking));
    assertEquals([4, 6], Helper(lengthTrackingWithOffset));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [0, 2, 4]
    //              [0, 2, 4, ...] << lengthTracking
    //                    [4, ...] << lengthTrackingWithOffset

    if (oobThrows) {
      assertThrows(() => {
        Helper(fixedLength);
      });
      assertThrows(() => {
        Helper(fixedLengthWithOffset);
      });
    } else {
      assertEquals([], Helper(fixedLength));
      assertEquals([], Helper(fixedLengthWithOffset));
    }
    assertEquals([0, 2, 4], Helper(lengthTracking));
    assertEquals([4], Helper(lengthTrackingWithOffset));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    if (oobThrows) {
      assertThrows(() => {
        Helper(fixedLength);
      });
      assertThrows(() => {
        Helper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        Helper(lengthTrackingWithOffset);
      });
    } else {
      assertEquals([], Helper(fixedLength));
      assertEquals([], Helper(fixedLengthWithOffset));
    }
    assertEquals([0], Helper(lengthTracking));

    // Shrink to zero.
    rab.resize(0);
    if (oobThrows) {
      assertThrows(() => {
        Helper(fixedLength);
      });
      assertThrows(() => {
        Helper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        Helper(lengthTrackingWithOffset);
      });
    } else {
      assertEquals([], Helper(fixedLength));
      assertEquals([], Helper(fixedLengthWithOffset));
      assertEquals([], Helper(lengthTrackingWithOffset));
    }
    assertEquals([], Helper(lengthTracking));

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    for (var _i48 = 0; _i48 < 6; ++_i48) {
      WriteToTypedArray(taWrite, _i48, 2 * _i48);
    }

    // Orig. array: [0, 2, 4, 6, 8, 10]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, 8, 10, ...] << lengthTracking
    //                    [4, 6, 8, 10, ...] << lengthTrackingWithOffset

    assertEquals([0, 2, 4, 6], Helper(fixedLength));
    assertEquals([4, 6], Helper(fixedLengthWithOffset));
    assertEquals([0, 2, 4, 6, 8, 10], Helper(lengthTracking));
    assertEquals([4, 6, 8, 10], Helper(lengthTrackingWithOffset));
  };
  for (var ctor of ctors) {
    _loop52();
  }
}
ForEachReduceReduceRight(TypedArrayForEachHelper, TypedArrayReduceHelper, TypedArrayReduceRightHelper, true);
ForEachReduceReduceRight(ArrayForEachHelper, ArrayReduceHelper, ArrayReduceRightHelper, false);

// The corresponding tests for Array.prototype.forEach etc are in
// typedarray-resizablearraybuffer-array-methods.js.
(function ForEachReduceReduceRightShrinkMidIteration() {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return true;
  }
  function ForEachHelper(array) {
    values = [];
    array.forEach(CollectValuesAndResize);
    return values;
  }
  function ReduceHelper(array) {
    values = [];
    array.reduce((acc, n) => {
      CollectValuesAndResize(n);
    }, "initial value");
    return values;
  }
  function ReduceRightHelper(array) {
    values = [];
    array.reduceRight((acc, n) => {
      CollectValuesAndResize(n);
    }, "initial value");
    return values;
  }

  // Test for forEach.

  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    resizeAfter = 2;
    resizeTo = 3 * ctor.BYTES_PER_ELEMENT;
    assertEquals([0, 2, undefined, undefined], ForEachHelper(fixedLength));
  }
  for (var _ctor104 of ctors) {
    rab = CreateRabForTest(_ctor104);
    var fixedLengthWithOffset = new _ctor104(rab, 2 * _ctor104.BYTES_PER_ELEMENT, 2);
    resizeAfter = 1;
    resizeTo = 3 * _ctor104.BYTES_PER_ELEMENT;
    assertEquals([4, undefined], ForEachHelper(fixedLengthWithOffset));
  }

  // Resizing + a length-tracking TA -> no OOB.
  for (var _ctor105 of ctors) {
    rab = CreateRabForTest(_ctor105);
    var lengthTracking = new _ctor105(rab, 0);
    resizeAfter = 2;
    resizeTo = 3 * _ctor105.BYTES_PER_ELEMENT;
    assertEquals([0, 2, 4, undefined], ForEachHelper(lengthTracking));
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  for (var _ctor106 of ctors) {
    rab = CreateRabForTest(_ctor106);
    var _lengthTracking14 = new _ctor106(rab, 0);
    resizeAfter = 2;
    resizeTo = 0;
    assertEquals([0, 2, undefined, undefined], ForEachHelper(_lengthTracking14));
  }
  for (var _ctor107 of ctors) {
    rab = CreateRabForTest(_ctor107);
    var lengthTrackingWithOffset = new _ctor107(rab, 2 * _ctor107.BYTES_PER_ELEMENT);
    resizeAfter = 1;
    resizeTo = 3 * _ctor107.BYTES_PER_ELEMENT;
    assertEquals([4, undefined], ForEachHelper(lengthTrackingWithOffset));
  }

  // Tests for reduce.

  for (var _ctor108 of ctors) {
    rab = CreateRabForTest(_ctor108);
    var _fixedLength3 = new _ctor108(rab, 0, 4);
    resizeAfter = 2;
    resizeTo = 3 * _ctor108.BYTES_PER_ELEMENT;
    assertEquals([0, 2, undefined, undefined], ReduceHelper(_fixedLength3));
  }
  for (var _ctor109 of ctors) {
    rab = CreateRabForTest(_ctor109);
    var _fixedLengthWithOffset3 = new _ctor109(rab, 2 * _ctor109.BYTES_PER_ELEMENT, 2);
    resizeAfter = 1;
    resizeTo = 3 * _ctor109.BYTES_PER_ELEMENT;
    assertEquals([4, undefined], ReduceHelper(_fixedLengthWithOffset3));
  }

  // Resizing + a length-tracking TA -> no OOB.
  for (var _ctor110 of ctors) {
    rab = CreateRabForTest(_ctor110);
    var _lengthTracking15 = new _ctor110(rab, 0);
    resizeAfter = 2;
    resizeTo = 3 * _ctor110.BYTES_PER_ELEMENT;
    assertEquals([0, 2, 4, undefined], ReduceHelper(_lengthTracking15));
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  for (var _ctor111 of ctors) {
    rab = CreateRabForTest(_ctor111);
    var _lengthTracking16 = new _ctor111(rab, 0);
    resizeAfter = 2;
    resizeTo = 0;
    assertEquals([0, 2, undefined, undefined], ReduceHelper(_lengthTracking16));
  }
  for (var _ctor112 of ctors) {
    rab = CreateRabForTest(_ctor112);
    var _lengthTrackingWithOffset5 = new _ctor112(rab, 2 * _ctor112.BYTES_PER_ELEMENT);
    resizeAfter = 1;
    resizeTo = 3 * _ctor112.BYTES_PER_ELEMENT;
    assertEquals([4, undefined], ReduceHelper(_lengthTrackingWithOffset5));
  }

  // Tests for reduceRight.

  for (var _ctor113 of ctors) {
    rab = CreateRabForTest(_ctor113);
    var _fixedLength4 = new _ctor113(rab, 0, 4);
    resizeAfter = 2;
    resizeTo = 3 * _ctor113.BYTES_PER_ELEMENT;
    assertEquals([6, 4, undefined, undefined], ReduceRightHelper(_fixedLength4));
  }
  for (var _ctor114 of ctors) {
    rab = CreateRabForTest(_ctor114);
    var _fixedLengthWithOffset4 = new _ctor114(rab, 2 * _ctor114.BYTES_PER_ELEMENT, 2);
    resizeAfter = 1;
    resizeTo = 3 * _ctor114.BYTES_PER_ELEMENT;
    assertEquals([6, undefined], ReduceRightHelper(_fixedLengthWithOffset4));
  }

  // Resizing + a length-tracking TA -> no OOB.
  for (var _ctor115 of ctors) {
    rab = CreateRabForTest(_ctor115);
    var _lengthTracking17 = new _ctor115(rab, 0);
    resizeAfter = 2;
    resizeTo = 3 * _ctor115.BYTES_PER_ELEMENT;
    // Unaffected by the shrinking, since we've already iterated past the point.
    assertEquals([6, 4, 2, 0], ReduceRightHelper(_lengthTracking17));
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  for (var _ctor116 of ctors) {
    rab = CreateRabForTest(_ctor116);
    var _lengthTracking18 = new _ctor116(rab, 0);
    resizeAfter = 2;
    resizeTo = 0;
    assertEquals([6, 4, undefined, undefined], ReduceRightHelper(_lengthTracking18));
  }
  for (var _ctor117 of ctors) {
    rab = CreateRabForTest(_ctor117);
    var _lengthTracking19 = new _ctor117(rab, 0);
    resizeAfter = 1;
    resizeTo = 2 * _ctor117.BYTES_PER_ELEMENT;
    assertEquals([6, undefined, 2, 0], ReduceRightHelper(_lengthTracking19));
  }
  for (var _ctor118 of ctors) {
    rab = CreateRabForTest(_ctor118);
    var _lengthTrackingWithOffset6 = new _ctor118(rab, 2 * _ctor118.BYTES_PER_ELEMENT);
    resizeAfter = 1;
    resizeTo = 3 * _ctor118.BYTES_PER_ELEMENT;
    // Unaffected by the shrinking, since we've already iterated past the point.
    assertEquals([6, 4], ReduceRightHelper(_lengthTrackingWithOffset6));
  }
})();
function ForEachReduceReduceRightGrowMidIteration(forEachHelper, reduceHelper, reduceRightHelper) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return true;
  }
  function ForEachHelper(array) {
    values = [];
    forEachHelper(array, CollectValuesAndResize);
    return values;
  }
  function ReduceHelper(array) {
    values = [];
    reduceHelper(array, (acc, n) => {
      CollectValuesAndResize(n);
    }, "initial value");
    return values;
  }
  function ReduceRightHelper(array) {
    values = [];
    reduceRightHelper(array, (acc, n) => {
      CollectValuesAndResize(n);
    }, "initial value");
    return values;
  }

  // Test for forEach.

  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    resizeAfter = 2;
    resizeTo = 5 * ctor.BYTES_PER_ELEMENT;
    assertEquals([0, 2, 4, 6], ForEachHelper(fixedLength));
  }
  for (var _ctor119 of ctors) {
    rab = CreateRabForTest(_ctor119);
    var fixedLengthWithOffset = new _ctor119(rab, 2 * _ctor119.BYTES_PER_ELEMENT, 2);
    resizeAfter = 1;
    resizeTo = 5 * _ctor119.BYTES_PER_ELEMENT;
    assertEquals([4, 6], ForEachHelper(fixedLengthWithOffset));
  }
  for (var _ctor120 of ctors) {
    rab = CreateRabForTest(_ctor120);
    var lengthTracking = new _ctor120(rab, 0);
    resizeAfter = 2;
    resizeTo = 5 * _ctor120.BYTES_PER_ELEMENT;
    assertEquals([0, 2, 4, 6], ForEachHelper(lengthTracking));
  }
  for (var _ctor121 of ctors) {
    rab = CreateRabForTest(_ctor121);
    var lengthTrackingWithOffset = new _ctor121(rab, 2 * _ctor121.BYTES_PER_ELEMENT);
    resizeAfter = 1;
    resizeTo = 5 * _ctor121.BYTES_PER_ELEMENT;
    assertEquals([4, 6], ForEachHelper(lengthTrackingWithOffset));
  }

  // Test for reduce.

  for (var _ctor122 of ctors) {
    rab = CreateRabForTest(_ctor122);
    var _fixedLength5 = new _ctor122(rab, 0, 4);
    resizeAfter = 2;
    resizeTo = 5 * _ctor122.BYTES_PER_ELEMENT;
    assertEquals([0, 2, 4, 6], ReduceHelper(_fixedLength5));
  }
  for (var _ctor123 of ctors) {
    rab = CreateRabForTest(_ctor123);
    var _fixedLengthWithOffset5 = new _ctor123(rab, 2 * _ctor123.BYTES_PER_ELEMENT, 2);
    resizeAfter = 1;
    resizeTo = 5 * _ctor123.BYTES_PER_ELEMENT;
    assertEquals([4, 6], ReduceHelper(_fixedLengthWithOffset5));
  }
  for (var _ctor124 of ctors) {
    rab = CreateRabForTest(_ctor124);
    var _lengthTracking20 = new _ctor124(rab, 0);
    resizeAfter = 2;
    resizeTo = 5 * _ctor124.BYTES_PER_ELEMENT;
    assertEquals([0, 2, 4, 6], ReduceHelper(_lengthTracking20));
  }
  for (var _ctor125 of ctors) {
    rab = CreateRabForTest(_ctor125);
    var _lengthTrackingWithOffset7 = new _ctor125(rab, 2 * _ctor125.BYTES_PER_ELEMENT);
    resizeAfter = 1;
    resizeTo = 5 * _ctor125.BYTES_PER_ELEMENT;
    assertEquals([4, 6], ReduceHelper(_lengthTrackingWithOffset7));
  }

  // Test for reduceRight.

  for (var _ctor126 of ctors) {
    rab = CreateRabForTest(_ctor126);
    var _fixedLength6 = new _ctor126(rab, 0, 4);
    resizeAfter = 2;
    resizeTo = 5 * _ctor126.BYTES_PER_ELEMENT;
    assertEquals([6, 4, 2, 0], ReduceRightHelper(_fixedLength6));
  }
  for (var _ctor127 of ctors) {
    rab = CreateRabForTest(_ctor127);
    var _fixedLengthWithOffset6 = new _ctor127(rab, 2 * _ctor127.BYTES_PER_ELEMENT, 2);
    resizeAfter = 1;
    resizeTo = 5 * _ctor127.BYTES_PER_ELEMENT;
    assertEquals([6, 4], ReduceRightHelper(_fixedLengthWithOffset6));
  }
  for (var _ctor128 of ctors) {
    rab = CreateRabForTest(_ctor128);
    var _lengthTracking21 = new _ctor128(rab, 0);
    resizeAfter = 2;
    resizeTo = 5 * _ctor128.BYTES_PER_ELEMENT;
    assertEquals([6, 4, 2, 0], ReduceRightHelper(_lengthTracking21));
  }
  for (var _ctor129 of ctors) {
    rab = CreateRabForTest(_ctor129);
    var _lengthTrackingWithOffset8 = new _ctor129(rab, 2 * _ctor129.BYTES_PER_ELEMENT);
    resizeAfter = 1;
    resizeTo = 5 * _ctor129.BYTES_PER_ELEMENT;
    assertEquals([6, 4], ReduceRightHelper(_lengthTrackingWithOffset8));
  }
}
ForEachReduceReduceRightGrowMidIteration(TypedArrayForEachHelper, TypedArrayReduceHelper, TypedArrayReduceRightHelper);
ForEachReduceReduceRightGrowMidIteration(ArrayForEachHelper, ArrayReduceHelper, ArrayReduceRightHelper);
function Includes(helper, oobThrows) {
  var _loop53 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);

    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }

    // Orig. array: [0, 2, 4, 6]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, ...] << lengthTracking
    //                    [4, 6, ...] << lengthTrackingWithOffset

    assertTrue(helper(fixedLength, 2));
    assertFalse(helper(fixedLength, undefined));
    assertTrue(helper(fixedLength, 2, 1));
    assertFalse(helper(fixedLength, 2, 2));
    assertTrue(helper(fixedLength, 2, -3));
    assertFalse(helper(fixedLength, 2, -2));
    assertFalse(helper(fixedLengthWithOffset, 2));
    assertTrue(helper(fixedLengthWithOffset, 4));
    assertFalse(helper(fixedLengthWithOffset, undefined));
    assertTrue(helper(fixedLengthWithOffset, 4, 0));
    assertFalse(helper(fixedLengthWithOffset, 4, 1));
    assertTrue(helper(fixedLengthWithOffset, 4, -2));
    assertFalse(helper(fixedLengthWithOffset, 4, -1));
    assertTrue(helper(lengthTracking, 2));
    assertFalse(helper(lengthTracking, undefined));
    assertTrue(helper(lengthTracking, 2, 1));
    assertFalse(helper(lengthTracking, 2, 2));
    assertTrue(helper(lengthTracking, 2, -3));
    assertFalse(helper(lengthTracking, 2, -2));
    assertFalse(helper(lengthTrackingWithOffset, 2));
    assertTrue(helper(lengthTrackingWithOffset, 4));
    assertFalse(helper(lengthTrackingWithOffset, undefined));
    assertTrue(helper(lengthTrackingWithOffset, 4, 0));
    assertFalse(helper(lengthTrackingWithOffset, 4, 1));
    assertTrue(helper(lengthTrackingWithOffset, 4, -2));
    assertFalse(helper(lengthTrackingWithOffset, 4, -1));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [0, 2, 4]
    //              [0, 2, 4, ...] << lengthTracking
    //                    [4, ...] << lengthTrackingWithOffset

    if (oobThrows) {
      assertThrows(() => {
        helper(fixedLength, 2);
      });
      assertThrows(() => {
        helper(fixedLengthWithOffset, 2);
      });
    } else {
      assertFalse(helper(fixedLength, 2));
      assertFalse(helper(fixedLengthWithOffset, 2));
    }
    assertTrue(helper(lengthTracking, 2));
    assertFalse(helper(lengthTracking, undefined));
    assertFalse(helper(lengthTrackingWithOffset, 2));
    assertTrue(helper(lengthTrackingWithOffset, 4));
    assertFalse(helper(lengthTrackingWithOffset, undefined));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    if (oobThrows) {
      assertThrows(() => {
        helper(fixedLength, 2);
      });
      assertThrows(() => {
        helper(fixedLengthWithOffset, 2);
      });
      assertThrows(() => {
        helper(lengthTrackingWithOffset, 2);
      });
    } else {
      assertFalse(helper(fixedLength, 2));
      assertFalse(helper(fixedLengthWithOffset, 2));
      assertFalse(helper(lengthTrackingWithOffset, 2));
    }

    // Shrink to zero.
    rab.resize(0);
    if (oobThrows) {
      assertThrows(() => {
        helper(fixedLength, 2);
      });
      assertThrows(() => {
        helper(fixedLengthWithOffset, 2);
      });
      assertThrows(() => {
        helper(lengthTrackingWithOffset, 2);
      });
    } else {
      assertFalse(helper(fixedLength, 2));
      assertFalse(helper(fixedLengthWithOffset, 2));
      assertFalse(helper(lengthTrackingWithOffset, 2));
    }
    assertFalse(helper(lengthTracking, 2));
    assertFalse(helper(lengthTracking, undefined));

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    for (var _i49 = 0; _i49 < 6; ++_i49) {
      WriteToTypedArray(taWrite, _i49, 2 * _i49);
    }

    // Orig. array: [0, 2, 4, 6, 8, 10]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, 8, 10, ...] << lengthTracking
    //                    [4, 6, 8, 10, ...] << lengthTrackingWithOffset

    assertTrue(helper(fixedLength, 2));
    assertFalse(helper(fixedLength, undefined));
    assertFalse(helper(fixedLength, 8));
    assertFalse(helper(fixedLengthWithOffset, 2));
    assertTrue(helper(fixedLengthWithOffset, 4));
    assertFalse(helper(fixedLengthWithOffset, undefined));
    assertFalse(helper(fixedLengthWithOffset, 8));
    assertTrue(helper(lengthTracking, 2));
    assertFalse(helper(lengthTracking, undefined));
    assertTrue(helper(lengthTracking, 8));
    assertFalse(helper(lengthTrackingWithOffset, 2));
    assertTrue(helper(lengthTrackingWithOffset, 4));
    assertFalse(helper(lengthTrackingWithOffset, undefined));
    assertTrue(helper(lengthTrackingWithOffset, 8));
  };
  for (var ctor of ctors) {
    _loop53();
  }
}
Includes(TypedArrayIncludesHelper, true);
Includes(ArrayIncludesHelper, false);
function IncludesParameterConversionResizes(helper) {
  var _loop54 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * ctor.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertFalse(helper(fixedLength, undefined));
    // The TA is OOB so it includes only "undefined".
    assertTrue(helper(fixedLength, undefined, evil));
  };
  for (var ctor of ctors) {
    _loop54(ctor);
  }
  var _loop55 = function (_ctor130) {
    var rab = CreateResizableArrayBuffer(4 * _ctor130.BYTES_PER_ELEMENT, 8 * _ctor130.BYTES_PER_ELEMENT);
    var fixedLength = new _ctor130(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor130.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertTrue(helper(fixedLength, 0));
    // The TA is OOB so it includes only "undefined".
    assertFalse(helper(fixedLength, 0, evil));
  };
  for (var _ctor130 of ctors) {
    _loop55(_ctor130);
  }

  // Resizing + a length-tracking TA -> no OOB.
  var _loop56 = function (_ctor131) {
    var rab = CreateResizableArrayBuffer(4 * _ctor131.BYTES_PER_ELEMENT, 8 * _ctor131.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor131(rab);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor131.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertFalse(helper(lengthTracking, undefined));
    // "includes" iterates until the original length and sees "undefined"s.
    assertTrue(helper(lengthTracking, undefined, evil));
  };
  for (var _ctor131 of ctors) {
    _loop56(_ctor131);
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  var _loop57 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor132.BYTES_PER_ELEMENT, 8 * _ctor132.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor132(rab);
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return 0;
      }
    };
    assertFalse(helper(lengthTracking, undefined));
    // "includes" iterates until the original length and sees "undefined"s.
    assertTrue(helper(lengthTracking, undefined, evil));
  };
  for (var _ctor132 of ctors) {
    _loop57();
  }
  var _loop58 = function (_ctor133) {
    var rab = CreateResizableArrayBuffer(4 * _ctor133.BYTES_PER_ELEMENT, 8 * _ctor133.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor133(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(lengthTracking, i, 1);
    }
    var evil = {
      valueOf: () => {
        rab.resize(6 * _ctor133.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertFalse(helper(lengthTracking, 0));
    // The TA grew but we only look at the data until the original length.
    assertFalse(helper(lengthTracking, 0, evil));
  };
  for (var _ctor133 of ctors) {
    _loop58(_ctor133);
  }
  var _loop59 = function (_ctor134) {
    var rab = CreateResizableArrayBuffer(4 * _ctor134.BYTES_PER_ELEMENT, 8 * _ctor134.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor134(rab);
    WriteToTypedArray(lengthTracking, 0, 1);
    var evil = {
      valueOf: () => {
        rab.resize(6 * _ctor134.BYTES_PER_ELEMENT);
        return -4;
      }
    };
    assertTrue(helper(lengthTracking, 1, -4));
    // The TA grew but the start index conversion is done based on the original
    // length.
    assertTrue(helper(lengthTracking, 1, evil));
  };
  for (var _ctor134 of ctors) {
    _loop59(_ctor134);
  }
}
IncludesParameterConversionResizes(TypedArrayIncludesHelper);
IncludesParameterConversionResizes(ArrayIncludesHelper);
(function IncludesSpecialValues() {
  for (var ctor of floatCtors) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var lengthTracking = new ctor(rab);
    lengthTracking[0] = -Infinity;
    lengthTracking[1] = Infinity;
    lengthTracking[2] = NaN;
    assertTrue(lengthTracking.includes(-Infinity));
    assertTrue(lengthTracking.includes(Infinity));
    assertTrue(lengthTracking.includes(NaN));
  }
})();
function IndexOfLastIndexOf(indexOfHelper, lastIndexOfHelper, oobThrows) {
  var _loop60 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);

    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, Math.floor(i / 2));
    }

    // Orig. array: [0, 0, 1, 1]
    //              [0, 0, 1, 1] << fixedLength
    //                    [1, 1] << fixedLengthWithOffset
    //              [0, 0, 1, 1, ...] << lengthTracking
    //                    [1, 1, ...] << lengthTrackingWithOffset

    assertEquals(0, indexOfHelper(fixedLength, 0));
    assertEquals(1, indexOfHelper(fixedLength, 0, 1));
    assertEquals(-1, indexOfHelper(fixedLength, 0, 2));
    assertEquals(-1, indexOfHelper(fixedLength, 0, -2));
    assertEquals(1, indexOfHelper(fixedLength, 0, -3));
    assertEquals(2, indexOfHelper(fixedLength, 1, 1));
    assertEquals(2, indexOfHelper(fixedLength, 1, -3));
    assertEquals(2, indexOfHelper(fixedLength, 1, -2));
    assertEquals(-1, indexOfHelper(fixedLength, undefined));
    assertEquals(1, lastIndexOfHelper(fixedLength, 0));
    assertEquals(1, lastIndexOfHelper(fixedLength, 0, 1));
    assertEquals(1, lastIndexOfHelper(fixedLength, 0, 2));
    assertEquals(1, lastIndexOfHelper(fixedLength, 0, -2));
    assertEquals(1, lastIndexOfHelper(fixedLength, 0, -3));
    assertEquals(-1, lastIndexOfHelper(fixedLength, 1, 1));
    assertEquals(2, lastIndexOfHelper(fixedLength, 1, -2));
    assertEquals(-1, lastIndexOfHelper(fixedLength, 1, -3));
    assertEquals(-1, lastIndexOfHelper(fixedLength, undefined));
    assertEquals(-1, indexOfHelper(fixedLengthWithOffset, 0));
    assertEquals(0, indexOfHelper(fixedLengthWithOffset, 1));
    assertEquals(0, indexOfHelper(fixedLengthWithOffset, 1, -2));
    assertEquals(1, indexOfHelper(fixedLengthWithOffset, 1, -1));
    assertEquals(-1, indexOfHelper(fixedLengthWithOffset, undefined));
    assertEquals(-1, lastIndexOfHelper(fixedLengthWithOffset, 0));
    assertEquals(1, lastIndexOfHelper(fixedLengthWithOffset, 1));
    assertEquals(0, lastIndexOfHelper(fixedLengthWithOffset, 1, -2));
    assertEquals(1, lastIndexOfHelper(fixedLengthWithOffset, 1, -1));
    assertEquals(-1, lastIndexOfHelper(fixedLengthWithOffset, undefined));
    assertEquals(0, indexOfHelper(lengthTracking, 0));
    assertEquals(-1, indexOfHelper(lengthTracking, 0, 2));
    assertEquals(2, indexOfHelper(lengthTracking, 1, -3));
    assertEquals(-1, indexOfHelper(lengthTracking, undefined));
    assertEquals(1, lastIndexOfHelper(lengthTracking, 0));
    assertEquals(1, lastIndexOfHelper(lengthTracking, 0, 2));
    assertEquals(1, lastIndexOfHelper(lengthTracking, 0, -3));
    assertEquals(-1, lastIndexOfHelper(lengthTracking, 1, 1));
    assertEquals(2, lastIndexOfHelper(lengthTracking, 1, 2));
    assertEquals(-1, lastIndexOfHelper(lengthTracking, 1, -3));
    assertEquals(-1, lastIndexOfHelper(lengthTracking, undefined));
    assertEquals(-1, indexOfHelper(lengthTrackingWithOffset, 0));
    assertEquals(0, indexOfHelper(lengthTrackingWithOffset, 1));
    assertEquals(1, indexOfHelper(lengthTrackingWithOffset, 1, 1));
    assertEquals(0, indexOfHelper(lengthTrackingWithOffset, 1, -2));
    assertEquals(-1, indexOfHelper(lengthTrackingWithOffset, undefined));
    assertEquals(-1, lastIndexOfHelper(lengthTrackingWithOffset, 0));
    assertEquals(1, lastIndexOfHelper(lengthTrackingWithOffset, 1));
    assertEquals(1, lastIndexOfHelper(lengthTrackingWithOffset, 1, 1));
    assertEquals(0, lastIndexOfHelper(lengthTrackingWithOffset, 1, -2));
    assertEquals(1, lastIndexOfHelper(lengthTrackingWithOffset, 1, -1));
    assertEquals(-1, lastIndexOfHelper(lengthTrackingWithOffset, undefined));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [0, 0, 1]
    //              [0, 0, 1, ...] << lengthTracking
    //                    [1, ...] << lengthTrackingWithOffset

    if (oobThrows) {
      assertThrows(() => {
        indexOfHelper(fixedLength, 1);
      });
      assertThrows(() => {
        indexOfHelper(fixedLengthWithOffset, 1);
      });
      assertThrows(() => {
        lastIndexOfHelper(fixedLength, 1);
      });
      assertThrows(() => {
        lastIndexOfHelper(fixedLengthWithOffset, 1);
      });
    } else {
      assertEquals(-1, indexOfHelper(fixedLength, 1));
      assertEquals(-1, indexOfHelper(fixedLengthWithOffset, 1));
      assertEquals(-1, lastIndexOfHelper(fixedLength, 1));
      assertEquals(-1, lastIndexOfHelper(fixedLengthWithOffset, 1));
    }
    assertEquals(2, indexOfHelper(lengthTracking, 1));
    assertEquals(-1, indexOfHelper(lengthTracking, undefined));
    assertEquals(1, lastIndexOfHelper(lengthTracking, 0));
    assertEquals(-1, lastIndexOfHelper(lengthTracking, undefined));
    assertEquals(-1, indexOfHelper(lengthTrackingWithOffset, 0));
    assertEquals(0, indexOfHelper(lengthTrackingWithOffset, 1));
    assertEquals(-1, indexOfHelper(lengthTrackingWithOffset, undefined));
    assertEquals(-1, lastIndexOfHelper(lengthTrackingWithOffset, 0));
    assertEquals(0, lastIndexOfHelper(lengthTrackingWithOffset, 1));
    assertEquals(-1, lastIndexOfHelper(lengthTrackingWithOffset, undefined));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    if (oobThrows) {
      assertThrows(() => {
        indexOfHelper(fixedLength, 0);
      });
      assertThrows(() => {
        indexOfHelper(fixedLengthWithOffset, 0);
      });
      assertThrows(() => {
        indexOfHelper(lengthTrackingWithOffset, 0);
      });
      assertThrows(() => {
        lastIndexOfHelper(fixedLength, 0);
      });
      assertThrows(() => {
        lastIndexOfHelper(fixedLengthWithOffset, 0);
      });
      assertThrows(() => {
        lastIndexOfHelper(lengthTrackingWithOffset, 0);
      });
    } else {
      assertEquals(-1, indexOfHelper(fixedLength, 0));
      assertEquals(-1, indexOfHelper(fixedLengthWithOffset, 0));
      assertEquals(-1, indexOfHelper(lengthTrackingWithOffset, 0));
      assertEquals(-1, lastIndexOfHelper(fixedLength, 0));
      assertEquals(-1, lastIndexOfHelper(fixedLengthWithOffset, 0));
      assertEquals(-1, lastIndexOfHelper(lengthTrackingWithOffset, 0));
    }
    assertEquals(0, indexOfHelper(lengthTracking, 0));
    assertEquals(0, lastIndexOfHelper(lengthTracking, 0));

    // Shrink to zero.
    rab.resize(0);
    if (oobThrows) {
      assertThrows(() => {
        indexOfHelper(fixedLength, 0);
      });
      assertThrows(() => {
        indexOfHelper(fixedLengthWithOffset, 0);
      });
      assertThrows(() => {
        indexOfHelper(lengthTrackingWithOffset, 0);
      });
      assertThrows(() => {
        lastIndexOfHelper(fixedLength, 0);
      });
      assertThrows(() => {
        lastIndexOfHelper(fixedLengthWithOffset, 0);
      });
      assertThrows(() => {
        lastIndexOfHelper(lengthTrackingWithOffset, 0);
      });
    } else {
      assertEquals(-1, indexOfHelper(fixedLength, 0));
      assertEquals(-1, indexOfHelper(fixedLengthWithOffset, 0));
      assertEquals(-1, indexOfHelper(lengthTrackingWithOffset, 0));
      assertEquals(-1, lastIndexOfHelper(fixedLength, 0));
      assertEquals(-1, lastIndexOfHelper(fixedLengthWithOffset, 0));
      assertEquals(-1, lastIndexOfHelper(lengthTrackingWithOffset, 0));
    }
    assertEquals(-1, indexOfHelper(lengthTracking, 0));
    assertEquals(-1, indexOfHelper(lengthTracking, undefined));
    assertEquals(-1, lastIndexOfHelper(lengthTracking, 0));
    assertEquals(-1, lastIndexOfHelper(lengthTracking, undefined));

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    for (var _i50 = 0; _i50 < 6; ++_i50) {
      WriteToTypedArray(taWrite, _i50, Math.floor(_i50 / 2));
    }

    // Orig. array: [0, 0, 1, 1, 2, 2]
    //              [0, 0, 1, 1] << fixedLength
    //                    [1, 1] << fixedLengthWithOffset
    //              [0, 0, 1, 1, 2, 2, ...] << lengthTracking
    //                    [1, 1, 2, 2, ...] << lengthTrackingWithOffset

    assertEquals(2, indexOfHelper(fixedLength, 1));
    assertEquals(-1, indexOfHelper(fixedLength, 2));
    assertEquals(-1, indexOfHelper(fixedLength, undefined));
    assertEquals(3, lastIndexOfHelper(fixedLength, 1));
    assertEquals(-1, lastIndexOfHelper(fixedLength, 2));
    assertEquals(-1, lastIndexOfHelper(fixedLength, undefined));
    assertEquals(-1, indexOfHelper(fixedLengthWithOffset, 0));
    assertEquals(0, indexOfHelper(fixedLengthWithOffset, 1));
    assertEquals(-1, indexOfHelper(fixedLengthWithOffset, 2));
    assertEquals(-1, indexOfHelper(fixedLengthWithOffset, undefined));
    assertEquals(-1, lastIndexOfHelper(fixedLengthWithOffset, 0));
    assertEquals(1, lastIndexOfHelper(fixedLengthWithOffset, 1));
    assertEquals(-1, lastIndexOfHelper(fixedLengthWithOffset, 2));
    assertEquals(-1, lastIndexOfHelper(fixedLengthWithOffset, undefined));
    assertEquals(2, indexOfHelper(lengthTracking, 1));
    assertEquals(4, indexOfHelper(lengthTracking, 2));
    assertEquals(-1, indexOfHelper(lengthTracking, undefined));
    assertEquals(3, lastIndexOfHelper(lengthTracking, 1));
    assertEquals(5, lastIndexOfHelper(lengthTracking, 2));
    assertEquals(-1, lastIndexOfHelper(lengthTracking, undefined));
    assertEquals(-1, indexOfHelper(lengthTrackingWithOffset, 0));
    assertEquals(0, indexOfHelper(lengthTrackingWithOffset, 1));
    assertEquals(2, indexOfHelper(lengthTrackingWithOffset, 2));
    assertEquals(-1, indexOfHelper(lengthTrackingWithOffset, undefined));
    assertEquals(-1, lastIndexOfHelper(lengthTrackingWithOffset, 0));
    assertEquals(1, lastIndexOfHelper(lengthTrackingWithOffset, 1));
    assertEquals(3, lastIndexOfHelper(lengthTrackingWithOffset, 2));
    assertEquals(-1, lastIndexOfHelper(lengthTrackingWithOffset, undefined));
  };
  for (var ctor of ctors) {
    _loop60();
  }
}
IndexOfLastIndexOf(TypedArrayIndexOfHelper, TypedArrayLastIndexOfHelper, true);
IndexOfLastIndexOf(ArrayIndexOfHelper, ArrayLastIndexOfHelper, false);
function IndexOfParameterConversionShrinks(indexOfHelper, lastIndexOfHelper) {
  var _loop61 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * ctor.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertEquals(0, indexOfHelper(fixedLength, 0));
    // The TA is OOB so indexOf returns -1.
    assertEquals(-1, indexOfHelper(fixedLength, 0, evil));
  };
  // Shrinking + fixed-length TA.
  for (var ctor of ctors) {
    _loop61(ctor);
  }
  var _loop62 = function (_ctor135) {
    var rab = CreateResizableArrayBuffer(4 * _ctor135.BYTES_PER_ELEMENT, 8 * _ctor135.BYTES_PER_ELEMENT);
    var fixedLength = new _ctor135(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor135.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertEquals(0, indexOfHelper(fixedLength, 0));
    // The TA is OOB so indexOf returns -1, also for undefined).
    assertEquals(-1, indexOfHelper(fixedLength, undefined, evil));
  };
  for (var _ctor135 of ctors) {
    _loop62(_ctor135);
  }

  // Shrinking + length-tracking TA.
  var _loop63 = function (_ctor136) {
    var rab = CreateResizableArrayBuffer(4 * _ctor136.BYTES_PER_ELEMENT, 8 * _ctor136.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor136(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(lengthTracking, i, i);
    }
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor136.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertEquals(2, indexOfHelper(lengthTracking, 2));
    // 2 no longer found.
    assertEquals(-1, indexOfHelper(lengthTracking, 2, evil));
  };
  for (var _ctor136 of ctors) {
    _loop63(_ctor136);
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  var _loop64 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor137.BYTES_PER_ELEMENT, 8 * _ctor137.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor137(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(lengthTracking, i, i);
    }
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return 2;
      }
    };
    assertEquals(2, indexOfHelper(lengthTracking, 2));
    // 2 no longer found.
    assertEquals(-1, indexOfHelper(lengthTracking, evil));
  };
  for (var _ctor137 of ctors) {
    _loop64();
  }
  var _loop65 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor138.BYTES_PER_ELEMENT, 8 * _ctor138.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor138(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(lengthTracking, i, i);
    }
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return 1;
      }
    };
    assertEquals(2, indexOfHelper(lengthTracking, 2));
    // 2 no longer found.
    assertEquals(-1, indexOfHelper(lengthTracking, 2, evil));
  };
  for (var _ctor138 of ctors) {
    _loop65();
  }
}
IndexOfParameterConversionShrinks(TypedArrayIndexOfHelper);
// Rhino just fixing typed arrays
//IndexOfParameterConversionShrinks(ArrayIndexOfHelper);
function LastIndexOfParameterConversionShrinks(lastIndexOfHelper) {
  var _loop66 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * ctor.BYTES_PER_ELEMENT);
        return 2;
      }
    };
    assertEquals(3, lastIndexOfHelper(fixedLength, 0));
    // The TA is OOB so lastIndexOf returns -1.
    assertEquals(-1, lastIndexOfHelper(fixedLength, 0, evil));
  };
  // Shrinking + fixed-length TA.
  for (var ctor of ctors) {
    _loop66(ctor);
  }
  var _loop67 = function (_ctor139) {
    var rab = CreateResizableArrayBuffer(4 * _ctor139.BYTES_PER_ELEMENT, 8 * _ctor139.BYTES_PER_ELEMENT);
    var fixedLength = new _ctor139(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor139.BYTES_PER_ELEMENT);
        return 2;
      }
    };
    assertEquals(3, lastIndexOfHelper(fixedLength, 0));
    // The TA is OOB so lastIndexOf returns -1, also for undefined).
    assertEquals(-1, lastIndexOfHelper(fixedLength, undefined, evil));
  };
  for (var _ctor139 of ctors) {
    _loop67(_ctor139);
  }

  // Shrinking + length-tracking TA.
  var _loop68 = function (_ctor140) {
    var rab = CreateResizableArrayBuffer(4 * _ctor140.BYTES_PER_ELEMENT, 8 * _ctor140.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor140(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(lengthTracking, i, i);
    }
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor140.BYTES_PER_ELEMENT);
        return 2;
      }
    };
    assertEquals(2, lastIndexOfHelper(lengthTracking, 2));
    // 2 no longer found.
    assertEquals(-1, lastIndexOfHelper(lengthTracking, 2, evil));
  };
  for (var _ctor140 of ctors) {
    _loop68(_ctor140);
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  var _loop69 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor141.BYTES_PER_ELEMENT, 8 * _ctor141.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor141(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(lengthTracking, i, i);
    }
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return 2;
      }
    };
    assertEquals(2, lastIndexOfHelper(lengthTracking, 2));
    // 2 no longer found.
    assertEquals(-1, lastIndexOfHelper(lengthTracking, evil));
  };
  for (var _ctor141 of ctors) {
    _loop69();
  }
  var _loop70 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor142.BYTES_PER_ELEMENT, 8 * _ctor142.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor142(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(lengthTracking, i, i);
    }
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return 2;
      }
    };
    assertEquals(2, lastIndexOfHelper(lengthTracking, 2));
    // 2 no longer found.
    assertEquals(-1, lastIndexOfHelper(lengthTracking, 2, evil));
  };
  for (var _ctor142 of ctors) {
    _loop70();
  }
}
LastIndexOfParameterConversionShrinks(TypedArrayLastIndexOfHelper);
// Rhino just typed arrays
//LastIndexOfParameterConversionShrinks(ArrayLastIndexOfHelper);
function IndexOfParameterConversionGrows(indexOfHelper) {
  var _loop71 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var lengthTracking = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(lengthTracking, i, 1);
    }
    var evil = {
      valueOf: () => {
        rab.resize(6 * ctor.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertEquals(-1, indexOfHelper(lengthTracking, 0));
    // The TA grew but we only look at the data until the original length.
    assertEquals(-1, indexOfHelper(lengthTracking, 0, evil));
  };
  // Growing + length-tracking TA.
  for (var ctor of ctors) {
    _loop71(ctor);
  }

  // Growing + length-tracking TA, index conversion.
  var _loop72 = function (_ctor143) {
    var rab = CreateResizableArrayBuffer(4 * _ctor143.BYTES_PER_ELEMENT, 8 * _ctor143.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor143(rab);
    WriteToTypedArray(lengthTracking, 0, 1);
    var evil = {
      valueOf: () => {
        rab.resize(6 * _ctor143.BYTES_PER_ELEMENT);
        return -4;
      }
    };
    assertEquals(0, indexOfHelper(lengthTracking, 1, -4));
    // The TA grew but the start index conversion is done based on the original
    // length.
    assertEquals(0, indexOfHelper(lengthTracking, 1, evil));
  };
  for (var _ctor143 of ctors) {
    _loop72(_ctor143);
  }
}
IndexOfParameterConversionGrows(TypedArrayIndexOfHelper);
IndexOfParameterConversionGrows(ArrayIndexOfHelper);
function LastIndexOfParameterConversionGrows(lastIndexOfHelper) {
  var _loop73 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var lengthTracking = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(lengthTracking, i, 1);
    }
    var evil = {
      valueOf: () => {
        rab.resize(6 * ctor.BYTES_PER_ELEMENT);
        return -1;
      }
    };
    assertEquals(-1, lastIndexOfHelper(lengthTracking, 0));
    // Because lastIndexOf iterates from the given index downwards, it's not
    // possible to test that "we only look at the data until the original
    // length" without also testing that the index conversion happening with the
    // original length.
    assertEquals(-1, lastIndexOfHelper(lengthTracking, 0, evil));
  };
  // Growing + length-tracking TA.
  for (var ctor of ctors) {
    _loop73(ctor);
  }

  // Growing + length-tracking TA, index conversion.
  var _loop74 = function (_ctor144) {
    var rab = CreateResizableArrayBuffer(4 * _ctor144.BYTES_PER_ELEMENT, 8 * _ctor144.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor144(rab);
    var evil = {
      valueOf: () => {
        rab.resize(6 * _ctor144.BYTES_PER_ELEMENT);
        return -4;
      }
    };
    assertEquals(0, lastIndexOfHelper(lengthTracking, 0, -4));
    // The TA grew but the start index conversion is done based on the original
    // length.
    assertEquals(0, lastIndexOfHelper(lengthTracking, 0, evil));
  };
  for (var _ctor144 of ctors) {
    _loop74(_ctor144);
  }
}
LastIndexOfParameterConversionGrows(TypedArrayLastIndexOfHelper);
LastIndexOfParameterConversionGrows(ArrayLastIndexOfHelper);
(function IndexOfLastIndexOfSpecialValues() {
  for (var ctor of floatCtors) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var lengthTracking = new ctor(rab);
    lengthTracking[0] = -Infinity;
    lengthTracking[1] = -Infinity;
    lengthTracking[2] = Infinity;
    lengthTracking[3] = Infinity;
    lengthTracking[4] = NaN;
    lengthTracking[5] = NaN;
    assertEquals(0, lengthTracking.indexOf(-Infinity));
    assertEquals(1, lengthTracking.lastIndexOf(-Infinity));
    assertEquals(2, lengthTracking.indexOf(Infinity));
    assertEquals(3, lengthTracking.lastIndexOf(Infinity));
    // NaN is never found.
    assertEquals(-1, lengthTracking.indexOf(NaN));
    assertEquals(-1, lengthTracking.lastIndexOf(NaN));
  }
})();
function JoinToLocaleString(joinHelper, toLocaleStringHelper, oobThrows) {
  var _loop75 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);

    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }

    // Orig. array: [0, 2, 4, 6]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, ...] << lengthTracking
    //                    [4, 6, ...] << lengthTrackingWithOffset

    assertEquals('0,2,4,6', joinHelper(fixedLength));
    assertEquals('0,2,4,6', toLocaleStringHelper(fixedLength));
    assertEquals('4,6', joinHelper(fixedLengthWithOffset));
    assertEquals('4,6', toLocaleStringHelper(fixedLengthWithOffset));
    assertEquals('0,2,4,6', joinHelper(lengthTracking));
    assertEquals('0,2,4,6', toLocaleStringHelper(lengthTracking));
    assertEquals('4,6', joinHelper(lengthTrackingWithOffset));
    assertEquals('4,6', toLocaleStringHelper(lengthTrackingWithOffset));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [0, 2, 4]
    //              [0, 2, 4, ...] << lengthTracking
    //                    [4, ...] << lengthTrackingWithOffset

    if (oobThrows) {
      assertThrows(() => {
        joinHelper(fixedLength);
      });
      assertThrows(() => {
        toLocaleStringHelper(fixedLength);
      });
      assertThrows(() => {
        joinHelper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        toLocaleStringHelper(fixedLengthWithOffset);
      });
    } else {
      assertEquals('', joinHelper(fixedLength));
      assertEquals('', toLocaleStringHelper(fixedLength));
      assertEquals('', joinHelper(fixedLengthWithOffset));
      assertEquals('', toLocaleStringHelper(fixedLengthWithOffset));
    }
    assertEquals('0,2,4', joinHelper(lengthTracking));
    assertEquals('0,2,4', toLocaleStringHelper(lengthTracking));
    assertEquals('4', joinHelper(lengthTrackingWithOffset));
    assertEquals('4', toLocaleStringHelper(lengthTrackingWithOffset));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    if (oobThrows) {
      assertThrows(() => {
        joinHelper(fixedLength);
      });
      assertThrows(() => {
        toLocaleStringHelper(fixedLength);
      });
      assertThrows(() => {
        joinHelper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        toLocaleStringHelper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        joinHelper(lengthTrackingWithOffset);
      });
      assertThrows(() => {
        toLocaleStringHelper(lengthTrackingWithOffset);
      });
    } else {
      assertEquals('', joinHelper(fixedLength));
      assertEquals('', toLocaleStringHelper(fixedLength));
      assertEquals('', joinHelper(fixedLengthWithOffset));
      assertEquals('', toLocaleStringHelper(fixedLengthWithOffset));
      assertEquals('', joinHelper(lengthTrackingWithOffset));
      assertEquals('', toLocaleStringHelper(lengthTrackingWithOffset));
    }
    assertEquals('0', joinHelper(lengthTracking));
    assertEquals('0', toLocaleStringHelper(lengthTracking));

    // Shrink to zero.
    rab.resize(0);
    if (oobThrows) {
      assertThrows(() => {
        joinHelper(fixedLength);
      });
      assertThrows(() => {
        toLocaleStringHelper(fixedLength);
      });
      assertThrows(() => {
        joinHelper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        toLocaleStringHelper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        joinHelper(lengthTrackingWithOffset);
      });
      assertThrows(() => {
        toLocaleStringHelper(lengthTrackingWithOffset);
      });
    } else {
      assertEquals('', joinHelper(fixedLength));
      assertEquals('', toLocaleStringHelper(fixedLength));
      assertEquals('', joinHelper(fixedLengthWithOffset));
      assertEquals('', toLocaleStringHelper(fixedLengthWithOffset));
      assertEquals('', joinHelper(lengthTrackingWithOffset));
      assertEquals('', toLocaleStringHelper(lengthTrackingWithOffset));
    }
    assertEquals('', joinHelper(lengthTracking));
    assertEquals('', toLocaleStringHelper(lengthTracking));

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    for (var _i51 = 0; _i51 < 6; ++_i51) {
      WriteToTypedArray(taWrite, _i51, 2 * _i51);
    }

    // Orig. array: [0, 2, 4, 6, 8, 10]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, 8, 10, ...] << lengthTracking
    //                    [4, 6, 8, 10, ...] << lengthTrackingWithOffset

    assertEquals('0,2,4,6', joinHelper(fixedLength));
    assertEquals('0,2,4,6', toLocaleStringHelper(fixedLength));
    assertEquals('4,6', joinHelper(fixedLengthWithOffset));
    assertEquals('4,6', toLocaleStringHelper(fixedLengthWithOffset));
    assertEquals('0,2,4,6,8,10', joinHelper(lengthTracking));
    assertEquals('0,2,4,6,8,10', toLocaleStringHelper(lengthTracking));
    assertEquals('4,6,8,10', joinHelper(lengthTrackingWithOffset));
    assertEquals('4,6,8,10', toLocaleStringHelper(lengthTrackingWithOffset));
  };
  for (var ctor of ctors) {
    _loop75();
  }
}
JoinToLocaleString(TypedArrayJoinHelper, TypedArrayToLocaleStringHelper, true);
JoinToLocaleString(ArrayJoinHelper, ArrayToLocaleStringHelper, false);
function JoinParameterConversionShrinks(joinHelper) {
  var _loop76 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var evil = {
      toString: () => {
        rab.resize(2 * ctor.BYTES_PER_ELEMENT);
        return '.';
      }
    };
    // We iterate 4 elements, since it was the starting length, but the TA is
    // OOB right after parameter conversion, so all elements are converted to
    // the empty string.
    assertEquals('...', joinHelper(fixedLength, evil));
  };
  // Shrinking + fixed-length TA.
  for (var ctor of ctors) {
    _loop76(ctor);
  }

  // Shrinking + length-tracking TA.
  var _loop77 = function (_ctor145) {
    var rab = CreateResizableArrayBuffer(4 * _ctor145.BYTES_PER_ELEMENT, 8 * _ctor145.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor145(rab);
    var evil = {
      toString: () => {
        rab.resize(2 * _ctor145.BYTES_PER_ELEMENT);
        return '.';
      }
    };
    // We iterate 4 elements, since it was the starting length. Elements beyond
    // the new length are converted to the empty string.
    assertEquals('0.0..', joinHelper(lengthTracking, evil));
  };
  for (var _ctor145 of ctors) {
    _loop77(_ctor145);
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  var _loop78 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor146.BYTES_PER_ELEMENT, 8 * _ctor146.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor146(rab);
    var evil = {
      toString: () => {
        rab.resize(0);
        return '.';
      }
    };
    // We iterate 4 elements, since it was the starting length. All elements are
    // converted to the empty string.
    assertEquals('...', joinHelper(lengthTracking, evil));
  };
  for (var _ctor146 of ctors) {
    _loop78();
  }
}
JoinParameterConversionShrinks(TypedArrayJoinHelper);
JoinParameterConversionShrinks(ArrayJoinHelper);
function JoinParameterConversionGrows(joinHelper) {
  var _loop79 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var evil = {
      toString: () => {
        rab.resize(6 * ctor.BYTES_PER_ELEMENT);
        return '.';
      }
    };
    assertEquals('0.0.0.0', joinHelper(fixedLength, evil));
  };
  // Growing + fixed-length TA.
  for (var ctor of ctors) {
    _loop79(ctor);
  }

  // Growing + length-tracking TA.
  var _loop80 = function (_ctor147) {
    var rab = CreateResizableArrayBuffer(4 * _ctor147.BYTES_PER_ELEMENT, 8 * _ctor147.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor147(rab);
    var evil = {
      toString: () => {
        rab.resize(6 * _ctor147.BYTES_PER_ELEMENT);
        return '.';
      }
    };
    // We iterate 4 elements, since it was the starting length.
    assertEquals('0.0.0.0', joinHelper(lengthTracking, evil));
  };
  for (var _ctor147 of ctors) {
    _loop80(_ctor147);
  }
}
JoinParameterConversionGrows(TypedArrayJoinHelper);
JoinParameterConversionGrows(ArrayJoinHelper);
function ToLocaleStringNumberPrototypeToLocaleStringShrinks(toLocaleStringHelper) {
  var oldNumberPrototypeToLocaleString = Number.prototype.toLocaleString;
  var oldBigIntPrototypeToLocaleString = BigInt.prototype.toLocaleString;

  // Shrinking + fixed-length TA.
  var _loop81 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var resizeAfter = 2;
    Number.prototype.toLocaleString = function () {
      --resizeAfter;
      if (resizeAfter == 0) {
        rab.resize(2 * ctor.BYTES_PER_ELEMENT);
      }
      return oldNumberPrototypeToLocaleString.call(this);
    };
    BigInt.prototype.toLocaleString = function () {
      --resizeAfter;
      if (resizeAfter == 0) {
        rab.resize(2 * ctor.BYTES_PER_ELEMENT);
      }
      return oldBigIntPrototypeToLocaleString.call(this);
    };

    // We iterate 4 elements, since it was the starting length. The TA goes
    // OOB after 2 elements.
    assertEquals('0,0,,', toLocaleStringHelper(fixedLength));
  };
  for (var ctor of ctors) {
    _loop81(ctor);
  }

  // Shrinking + length-tracking TA.
  var _loop82 = function (_ctor148) {
    var rab = CreateResizableArrayBuffer(4 * _ctor148.BYTES_PER_ELEMENT, 8 * _ctor148.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor148(rab);
    var resizeAfter = 2;
    Number.prototype.toLocaleString = function () {
      --resizeAfter;
      if (resizeAfter == 0) {
        rab.resize(2 * _ctor148.BYTES_PER_ELEMENT);
      }
      return oldNumberPrototypeToLocaleString.call(this);
    };
    BigInt.prototype.toLocaleString = function () {
      --resizeAfter;
      if (resizeAfter == 0) {
        rab.resize(2 * _ctor148.BYTES_PER_ELEMENT);
      }
      return oldBigIntPrototypeToLocaleString.call(this);
    };

    // We iterate 4 elements, since it was the starting length. Elements beyond
    // the new length are converted to the empty string.
    assertEquals('0,0,,', toLocaleStringHelper(lengthTracking));
  };
  for (var _ctor148 of ctors) {
    _loop82(_ctor148);
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  var _loop83 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor149.BYTES_PER_ELEMENT, 8 * _ctor149.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor149(rab);
    var resizeAfter = 1;
    Number.prototype.toLocaleString = function () {
      --resizeAfter;
      if (resizeAfter == 0) {
        rab.resize(0);
      }
      return oldNumberPrototypeToLocaleString.call(this);
    };
    BigInt.prototype.toLocaleString = function () {
      --resizeAfter;
      if (resizeAfter == 0) {
        rab.resize(0);
      }
      return oldBigIntPrototypeToLocaleString.call(this);
    };

    // We iterate 4 elements, since it was the starting length. Elements beyond
    // the new length are converted to the empty string.
    assertEquals('0,,,', toLocaleStringHelper(lengthTracking));
  };
  for (var _ctor149 of ctors) {
    _loop83();
  }
  Number.prototype.toLocaleString = oldNumberPrototypeToLocaleString;
  BigInt.prototype.toLocaleString = oldBigIntPrototypeToLocaleString;
}
// Rhino
//ToLocaleStringNumberPrototypeToLocaleStringShrinks(TypedArrayToLocaleStringHelper);
//ToLocaleStringNumberPrototypeToLocaleStringShrinks(ArrayToLocaleStringHelper);
function ToLocaleStringNumberPrototypeToLocaleStringGrows(toLocaleStringHelper) {
  var oldNumberPrototypeToLocaleString = Number.prototype.toLocaleString;
  var oldBigIntPrototypeToLocaleString = BigInt.prototype.toLocaleString;

  // Growing + fixed-length TA.
  var _loop84 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var resizeAfter = 2;
    Number.prototype.toLocaleString = function () {
      --resizeAfter;
      if (resizeAfter == 0) {
        rab.resize(6 * ctor.BYTES_PER_ELEMENT);
      }
      return oldNumberPrototypeToLocaleString.call(this);
    };
    BigInt.prototype.toLocaleString = function () {
      --resizeAfter;
      if (resizeAfter == 0) {
        rab.resize(6 * ctor.BYTES_PER_ELEMENT);
      }
      return oldBigIntPrototypeToLocaleString.call(this);
    };

    // We iterate 4 elements since it was the starting length. Resizing doesn't
    // affect the TA.
    assertEquals('0,0,0,0', toLocaleStringHelper(fixedLength));
  };
  for (var ctor of ctors) {
    _loop84(ctor);
  }

  // Growing + length-tracking TA.
  var _loop85 = function (_ctor150) {
    var rab = CreateResizableArrayBuffer(4 * _ctor150.BYTES_PER_ELEMENT, 8 * _ctor150.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor150(rab);
    var resizeAfter = 2;
    Number.prototype.toLocaleString = function () {
      --resizeAfter;
      if (resizeAfter == 0) {
        rab.resize(6 * _ctor150.BYTES_PER_ELEMENT);
      }
      return oldNumberPrototypeToLocaleString.call(this);
    };
    BigInt.prototype.toLocaleString = function () {
      --resizeAfter;
      if (resizeAfter == 0) {
        rab.resize(6 * _ctor150.BYTES_PER_ELEMENT);
      }
      return oldBigIntPrototypeToLocaleString.call(this);
    };

    // We iterate 4 elements since it was the starting length.
    assertEquals('0,0,0,0', toLocaleStringHelper(lengthTracking));
  };
  for (var _ctor150 of ctors) {
    _loop85(_ctor150);
  }
  Number.prototype.toLocaleString = oldNumberPrototypeToLocaleString;
  BigInt.prototype.toLocaleString = oldBigIntPrototypeToLocaleString;
}
ToLocaleStringNumberPrototypeToLocaleStringGrows(TypedArrayToLocaleStringHelper);
ToLocaleStringNumberPrototypeToLocaleStringGrows(ArrayToLocaleStringHelper);
function TestMap(mapHelper, oobThrows) {
  var _loop86 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);

    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < taWrite.length; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }

    // Orig. array: [0, 2, 4, 6]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, ...] << lengthTracking
    //                    [4, 6, ...] << lengthTrackingWithOffset

    function Helper(array) {
      var values = [];
      function GatherValues(n, ix) {
        assertEquals(values.length, ix);
        values.push(n);
        if (typeof n == 'bigint') {
          return n + 1n;
        }
        return n + 1;
      }
      var newValues = mapHelper(array, GatherValues);
      for (var _i52 = 0; _i52 < values.length; ++_i52) {
        if (typeof values[_i52] == 'bigint') {
          assertEquals(newValues[_i52], values[_i52] + 1n);
        } else {
          assertEquals(newValues[_i52], values[_i52] + 1);
        }
      }
      return ToNumbers(values);
    }
    assertEquals([0, 2, 4, 6], Helper(fixedLength));
    assertEquals([4, 6], Helper(fixedLengthWithOffset));
    assertEquals([0, 2, 4, 6], Helper(lengthTracking));
    assertEquals([4, 6], Helper(lengthTrackingWithOffset));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [0, 2, 4]
    //              [0, 2, 4, ...] << lengthTracking
    //                    [4, ...] << lengthTrackingWithOffset

    if (oobThrows) {
      assertThrows(() => {
        Helper(fixedLength);
      });
      assertThrows(() => {
        Helper(fixedLengthWithOffset);
      });
    } else {
      assertEquals([], Helper(fixedLength));
      assertEquals([], Helper(fixedLengthWithOffset));
    }
    assertEquals([0, 2, 4], Helper(lengthTracking));
    assertEquals([4], Helper(lengthTrackingWithOffset));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    if (oobThrows) {
      assertThrows(() => {
        Helper(fixedLength);
      });
      assertThrows(() => {
        Helper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        Helper(lengthTrackingWithOffset);
      });
    } else {
      assertEquals([], Helper(fixedLength));
      assertEquals([], Helper(fixedLengthWithOffset));
      assertEquals([], Helper(lengthTrackingWithOffset));
    }
    assertEquals([0], Helper(lengthTracking));

    // Shrink to zero.
    rab.resize(0);
    if (oobThrows) {
      assertThrows(() => {
        Helper(fixedLength);
      });
      assertThrows(() => {
        Helper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        Helper(lengthTrackingWithOffset);
      });
    } else {
      assertEquals([], Helper(fixedLength));
      assertEquals([], Helper(fixedLengthWithOffset));
      assertEquals([], Helper(lengthTrackingWithOffset));
    }
    assertEquals([], Helper(lengthTracking));

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    for (var _i53 = 0; _i53 < 6; ++_i53) {
      WriteToTypedArray(taWrite, _i53, 2 * _i53);
    }

    // Orig. array: [0, 2, 4, 6, 8, 10]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, 8, 10, ...] << lengthTracking
    //                    [4, 6, 8, 10, ...] << lengthTrackingWithOffset

    assertEquals([0, 2, 4, 6], Helper(fixedLength));
    assertEquals([4, 6], Helper(fixedLengthWithOffset));
    assertEquals([0, 2, 4, 6, 8, 10], Helper(lengthTracking));
    assertEquals([4, 6, 8, 10], Helper(lengthTrackingWithOffset));
  };
  for (var ctor of ctors) {
    _loop86();
  }
}
TestMap(TypedArrayMapHelper, true);
TestMap(ArrayMapHelper, false);
function MapShrinkMidIteration(mapHelper, hasUndefined) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n, ix, ta) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    // We still need to return a valid BigInt / non-BigInt, even if
    // n is `undefined`.
    if (IsBigIntTypedArray(ta)) {
      return 0n;
    }
    return 0;
  }
  function Helper(array) {
    values = [];
    mapHelper(array, CollectValuesAndResize);
    return values;
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    resizeAfter = 2;
    resizeTo = 3 * ctor.BYTES_PER_ELEMENT;
    if (hasUndefined) {
      assertEquals([0, 2, undefined, undefined], Helper(fixedLength));
    } else {
      assertEquals([0, 2], Helper(fixedLength));
    }
  }
  for (var _ctor151 of ctors) {
    rab = CreateRabForTest(_ctor151);
    var fixedLengthWithOffset = new _ctor151(rab, 2 * _ctor151.BYTES_PER_ELEMENT, 2);
    resizeAfter = 1;
    resizeTo = 3 * _ctor151.BYTES_PER_ELEMENT;
    if (hasUndefined) {
      assertEquals([4, undefined], Helper(fixedLengthWithOffset));
    } else {
      assertEquals([4], Helper(fixedLengthWithOffset));
    }
  }
  for (var _ctor152 of ctors) {
    rab = CreateRabForTest(_ctor152);
    var lengthTracking = new _ctor152(rab, 0);
    resizeAfter = 2;
    resizeTo = 3 * _ctor152.BYTES_PER_ELEMENT;
    if (hasUndefined) {
      assertEquals([0, 2, 4, undefined], Helper(lengthTracking));
    } else {
      assertEquals([0, 2, 4], Helper(lengthTracking));
    }
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  for (var _ctor153 of ctors) {
    rab = CreateRabForTest(_ctor153);
    var _lengthTracking22 = new _ctor153(rab, 0);
    resizeAfter = 1;
    resizeTo = 0;
    if (hasUndefined) {
      assertEquals([0, undefined, undefined, undefined], Helper(_lengthTracking22));
    } else {
      assertEquals([0], Helper(_lengthTracking22));
    }
  }
  for (var _ctor154 of ctors) {
    rab = CreateRabForTest(_ctor154);
    var lengthTrackingWithOffset = new _ctor154(rab, 2 * _ctor154.BYTES_PER_ELEMENT);
    resizeAfter = 1;
    resizeTo = 3 * _ctor154.BYTES_PER_ELEMENT;
    if (hasUndefined) {
      assertEquals([4, undefined], Helper(lengthTrackingWithOffset));
    } else {
      assertEquals([4], Helper(lengthTrackingWithOffset));
    }
  }
}
MapShrinkMidIteration(TypedArrayMapHelper, true);
// Rhino only typed arrays for now
//MapShrinkMidIteration(ArrayMapHelper, false);
function MapGrowMidIteration(mapHelper) {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var values;
  var rab;
  var resizeAfter;
  var resizeTo;
  function CollectValuesAndResize(n) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    if (values.length == resizeAfter) {
      rab.resize(resizeTo);
    }
    return n;
  }
  function Helper(array) {
    values = [];
    mapHelper(array, CollectValuesAndResize);
    return values;
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    resizeAfter = 2;
    resizeTo = 5 * ctor.BYTES_PER_ELEMENT;
    assertEquals([0, 2, 4, 6], Helper(fixedLength));
  }
  for (var _ctor155 of ctors) {
    rab = CreateRabForTest(_ctor155);
    var fixedLengthWithOffset = new _ctor155(rab, 2 * _ctor155.BYTES_PER_ELEMENT, 2);
    resizeAfter = 1;
    resizeTo = 5 * _ctor155.BYTES_PER_ELEMENT;
    assertEquals([4, 6], Helper(fixedLengthWithOffset));
  }
  for (var _ctor156 of ctors) {
    rab = CreateRabForTest(_ctor156);
    var lengthTracking = new _ctor156(rab, 0);
    resizeAfter = 2;
    resizeTo = 5 * _ctor156.BYTES_PER_ELEMENT;
    assertEquals([0, 2, 4, 6], Helper(lengthTracking));
  }
  for (var _ctor157 of ctors) {
    rab = CreateRabForTest(_ctor157);
    var lengthTrackingWithOffset = new _ctor157(rab, 2 * _ctor157.BYTES_PER_ELEMENT);
    resizeAfter = 1;
    resizeTo = 5 * _ctor157.BYTES_PER_ELEMENT;
    assertEquals([4, 6], Helper(lengthTrackingWithOffset));
  }
}
MapGrowMidIteration(TypedArrayMapHelper);
MapGrowMidIteration(ArrayMapHelper);
(function MapSpeciesCreateShrinks() {
  var values;
  var rab;
  function CollectValues(n, ix, ta) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    // We still need to return a valid BigInt / non-BigInt, even if
    // n is `undefined`.
    if (IsBigIntTypedArray(ta)) {
      return 0n;
    }
    return 0;
  }
  function Helper(array) {
    values = [];
    array.map(CollectValues);
    return values;
  }
  /* Rhino no inheritance yet.
  var _loop87 = function (ctor) {
    rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var resizeWhenConstructorCalled = false;
    var MyArray = /*#__PURE__*//*function (_ctor160) {
      function MyArray(...params) {
        var _this5;
        _classCallCheck(this, MyArray);
        _this5 = _callSuper(this, MyArray, [...params]);
        if (resizeWhenConstructorCalled) {
          rab.resize(2 * ctor.BYTES_PER_ELEMENT);
        }
        return _this5;
      }
      _inherits(MyArray, _ctor160);
      return _createClass(MyArray);
    }(ctor);
    ;
    var fixedLength = new MyArray(rab, 0, 4);
    resizeWhenConstructorCalled = true;
    assertEquals([undefined, undefined, undefined, undefined], Helper(fixedLength));
    assertEquals(2 * ctor.BYTES_PER_ELEMENT, rab.byteLength);
  };
  for (var ctor of ctors) {
    _loop87(ctor);
  }
  */
  /* Rhino no inheritance yet
  var _loop88 = function (_ctor158) {
    rab = CreateResizableArrayBuffer(4 * _ctor158.BYTES_PER_ELEMENT, 8 * _ctor158.BYTES_PER_ELEMENT);
    var taWrite = new _ctor158(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, i);
    }
    var resizeWhenConstructorCalled = false;
    var MyArray = /*#__PURE__*//*function (_ctor161) {
      function MyArray(...params) {
        var _this6;
        _classCallCheck(this, MyArray);
        _this6 = _callSuper(this, MyArray, [...params]);
        if (resizeWhenConstructorCalled) {
          rab.resize(2 * _ctor158.BYTES_PER_ELEMENT);
        }
        return _this6;
      }
      _inherits(MyArray, _ctor161);
      return _createClass(MyArray);
    }(_ctor158);
    ;
    var lengthTracking = new MyArray(rab);
    resizeWhenConstructorCalled = true;
    assertEquals([0, 1, undefined, undefined], Helper(lengthTracking));
    assertEquals(2 * _ctor158.BYTES_PER_ELEMENT, rab.byteLength);
  };
  for (var _ctor158 of ctors) {
    _loop88(_ctor158);
  }
  */
  /* Rhino no inheritance yet
  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  var _loop89 = function (_ctor159) {
    rab = CreateResizableArrayBuffer(4 * _ctor159.BYTES_PER_ELEMENT, 8 * _ctor159.BYTES_PER_ELEMENT);
    var taWrite = new _ctor159(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, i);
    }
    var resizeWhenConstructorCalled = false;
    var MyArray = /*#__PURE__*//*function (_ctor162) {
      function MyArray(...params) {
        var _this7;
        _classCallCheck(this, MyArray);
        _this7 = _callSuper(this, MyArray, [...params]);
        if (resizeWhenConstructorCalled) {
          rab.resize(0);
        }
        return _this7;
      }
      _inherits(MyArray, _ctor162);
      return _createClass(MyArray);
    }(_ctor159);
    ;
    var lengthTracking = new MyArray(rab);
    resizeWhenConstructorCalled = true;
    assertEquals([undefined, undefined, undefined, undefined], Helper(lengthTracking));
    assertEquals(0, rab.byteLength);
  };
  for (var _ctor159 of ctors) {
    _loop89(_ctor159);
  }
  */
})();
(function MapSpeciesCreateGrows() {
  var values;
  var rab;
  function CollectValues(n, ix, ta) {
    if (typeof n == 'bigint') {
      values.push(Number(n));
    } else {
      values.push(n);
    }
    // We still need to return a valid BigInt / non-BigInt, even if
    // n is `undefined`.
    if (IsBigIntTypedArray(ta)) {
      return 0n;
    }
    return 0;
  }
  function Helper(array) {
    values = [];
    array.map(CollectValues);
    return values;
  }
  /* Rhino no inheritance yet
  var _loop90 = function (ctor) {
    rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, i);
    }
    var resizeWhenConstructorCalled = false;
    var MyArray = /*#__PURE__*//*function (_ctor164) {
      function MyArray(...params) {
        var _this8;
        _classCallCheck(this, MyArray);
        _this8 = _callSuper(this, MyArray, [...params]);
        if (resizeWhenConstructorCalled) {
          rab.resize(6 * ctor.BYTES_PER_ELEMENT);
        }
        return _this8;
      }
      _inherits(MyArray, _ctor164);
      return _createClass(MyArray);
    }(ctor);
    ;
    var fixedLength = new MyArray(rab, 0, 4);
    resizeWhenConstructorCalled = true;
    assertEquals([0, 1, 2, 3], Helper(fixedLength));
    assertEquals(6 * ctor.BYTES_PER_ELEMENT, rab.byteLength);
  };
  for (var ctor of ctors) {
    _loop90(ctor);
  }
  */
  /* Rhino no inheritance yet
  var _loop91 = function (_ctor163) {
    rab = CreateResizableArrayBuffer(4 * _ctor163.BYTES_PER_ELEMENT, 8 * _ctor163.BYTES_PER_ELEMENT);
    var taWrite = new _ctor163(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, i);
    }
    var resizeWhenConstructorCalled = false;
    var MyArray = /*#__PURE__*//*function (_ctor165) {
      function MyArray(...params) {
        var _this9;
        _classCallCheck(this, MyArray);
        _this9 = _callSuper(this, MyArray, [...params]);
        if (resizeWhenConstructorCalled) {
          rab.resize(6 * _ctor163.BYTES_PER_ELEMENT);
        }
        return _this9;
      }
      _inherits(MyArray, _ctor165);
      return _createClass(MyArray);
    }(_ctor163);
    ;
    var lengthTracking = new MyArray(rab);
    resizeWhenConstructorCalled = true;
    assertEquals([0, 1, 2, 3], Helper(lengthTracking));
    assertEquals(6 * _ctor163.BYTES_PER_ELEMENT, rab.byteLength);
  };
  for (var _ctor163 of ctors) {
    _loop91(_ctor163);
  }
  */
})();
function Reverse(reverseHelper, oobThrows) {
  var _loop92 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);
    var wholeArrayView = new ctor(rab);
    function WriteData() {
      // Write some data into the array.
      for (var i = 0; i < wholeArrayView.length; ++i) {
        WriteToTypedArray(wholeArrayView, i, 2 * i);
      }
    }
    WriteData();

    // Orig. array: [0, 2, 4, 6]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, ...] << lengthTracking
    //                    [4, 6, ...] << lengthTrackingWithOffset

    reverseHelper(fixedLength);
    assertEquals([6, 4, 2, 0], ToNumbers(wholeArrayView));
    reverseHelper(fixedLengthWithOffset);
    assertEquals([6, 4, 0, 2], ToNumbers(wholeArrayView));
    reverseHelper(lengthTracking);
    assertEquals([2, 0, 4, 6], ToNumbers(wholeArrayView));
    reverseHelper(lengthTrackingWithOffset);
    assertEquals([2, 0, 6, 4], ToNumbers(wholeArrayView));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);
    WriteData();

    // Orig. array: [0, 2, 4]
    //              [0, 2, 4, ...] << lengthTracking
    //                    [4, ...] << lengthTrackingWithOffset

    if (oobThrows) {
      assertThrows(() => {
        reverseHelper(fixedLength);
      });
      assertThrows(() => {
        reverseHelper(fixedLengthWithOffset);
      });
    } else {
      reverseHelper(fixedLength);
      assertEquals([0, 2, 4], ToNumbers(wholeArrayView));
      reverseHelper(fixedLengthWithOffset);
      assertEquals([0, 2, 4], ToNumbers(wholeArrayView));
    }
    reverseHelper(lengthTracking);
    assertEquals([4, 2, 0], ToNumbers(wholeArrayView));
    reverseHelper(lengthTrackingWithOffset);
    assertEquals([4, 2, 0], ToNumbers(wholeArrayView));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    WriteData();
    if (oobThrows) {
      assertThrows(() => {
        reverseHelper(fixedLength);
      });
      assertThrows(() => {
        reverseHelper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        reverseHelper(lengthTrackingWithOffset);
      });
    } else {
      reverseHelper(fixedLength);
      assertEquals([0], ToNumbers(wholeArrayView));
      reverseHelper(fixedLengthWithOffset);
      assertEquals([0], ToNumbers(wholeArrayView));
      reverseHelper(lengthTrackingWithOffset);
      assertEquals([0], ToNumbers(wholeArrayView));
    }
    reverseHelper(lengthTracking);
    assertEquals([0], ToNumbers(wholeArrayView));

    // Shrink to zero.
    rab.resize(0);
    if (oobThrows) {
      assertThrows(() => {
        reverseHelper(fixedLength);
      });
      assertThrows(() => {
        reverseHelper(fixedLengthWithOffset);
      });
      assertThrows(() => {
        reverseHelper(lengthTrackingWithOffset);
      });
    } else {
      reverseHelper(fixedLength);
      assertEquals([], ToNumbers(wholeArrayView));
      reverseHelper(fixedLengthWithOffset);
      assertEquals([], ToNumbers(wholeArrayView));
      reverseHelper(lengthTrackingWithOffset);
      assertEquals([], ToNumbers(wholeArrayView));
    }
    reverseHelper(lengthTracking);
    assertEquals([], ToNumbers(wholeArrayView));

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    WriteData();

    // Orig. array: [0, 2, 4, 6, 8, 10]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, 8, 10, ...] << lengthTracking
    //                    [4, 6, 8, 10, ...] << lengthTrackingWithOffset

    reverseHelper(fixedLength);
    assertEquals([6, 4, 2, 0, 8, 10], ToNumbers(wholeArrayView));
    reverseHelper(fixedLengthWithOffset);
    assertEquals([6, 4, 0, 2, 8, 10], ToNumbers(wholeArrayView));
    reverseHelper(lengthTracking);
    assertEquals([10, 8, 2, 0, 4, 6], ToNumbers(wholeArrayView));
    reverseHelper(lengthTrackingWithOffset);
    assertEquals([10, 8, 6, 4, 0, 2], ToNumbers(wholeArrayView));
  };
  for (var ctor of ctors) {
    _loop92();
  }
}
Reverse(TypedArrayReverseHelper, true);
Reverse(ArrayReverseHelper, false);
(function SetWithResizableTarget() {
  var _loop93 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);
    var taFull = new ctor(rab);

    // Orig. array: [0, 0, 0, 0]
    //              [0, 0, 0, 0] << fixedLength
    //                    [0, 0] << fixedLengthWithOffset
    //              [0, 0, 0, 0, ...] << lengthTracking
    //                    [0, 0, ...] << lengthTrackingWithOffset

    // For making sure we're not calling the source length or element getters
    // if the target is OOB.
    var throwingProxy = new Proxy({}, {
      get(target, prop, receiver) {
        throw new Error('Called getter for ' + prop);
      }
    });
    SetHelper(fixedLength, [1, 2]);
    assertEquals([1, 2, 0, 0], ToNumbers(taFull));
    SetHelper(fixedLength, [3, 4], 1);
    assertEquals([1, 3, 4, 0], ToNumbers(taFull));
    assertThrows(() => {
      SetHelper(fixedLength, [0, 0, 0, 0, 0]);
    }, RangeError);
    assertThrows(() => {
      SetHelper(fixedLength, [0, 0, 0, 0], 1);
    }, RangeError);
    assertEquals([1, 3, 4, 0], ToNumbers(taFull));
    SetHelper(fixedLengthWithOffset, [5, 6]);
    assertEquals([1, 3, 5, 6], ToNumbers(taFull));
    SetHelper(fixedLengthWithOffset, [7], 1);
    assertEquals([1, 3, 5, 7], ToNumbers(taFull));
    assertThrows(() => {
      SetHelper(fixedLengthWithOffset, [0, 0, 0]);
    }, RangeError);
    assertThrows(() => {
      SetHelper(fixedLengthWithOffset, [0, 0], 1);
    }, RangeError);
    assertEquals([1, 3, 5, 7], ToNumbers(taFull));
    SetHelper(lengthTracking, [8, 9]);
    assertEquals([8, 9, 5, 7], ToNumbers(taFull));
    SetHelper(lengthTracking, [10, 11], 1);
    assertEquals([8, 10, 11, 7], ToNumbers(taFull));
    assertThrows(() => {
      SetHelper(lengthTracking, [0, 0, 0, 0, 0]);
    }, RangeError);
    assertThrows(() => {
      SetHelper(lengthTracking, [0, 0, 0, 0], 1);
    }, RangeError);
    assertEquals([8, 10, 11, 7], ToNumbers(taFull));
    SetHelper(lengthTrackingWithOffset, [12, 13]);
    assertEquals([8, 10, 12, 13], ToNumbers(taFull));
    SetHelper(lengthTrackingWithOffset, [14], 1);
    assertEquals([8, 10, 12, 14], ToNumbers(taFull));
    assertThrows(() => {
      SetHelper(lengthTrackingWithOffset, [0, 0, 0]);
    });
    assertThrows(() => {
      SetHelper(lengthTrackingWithOffset, [0, 0], 1);
    });
    assertEquals([8, 10, 12, 14], ToNumbers(taFull));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [8, 10, 12]
    //              [8, 10, 12, ...] << lengthTracking
    //                     [12, ...] << lengthTrackingWithOffset

    assertThrows(() => {
      SetHelper(fixedLength, throwingProxy);
    }, TypeError);
    assertThrows(() => {
      SetHelper(fixedLengthWithOffset, throwingProxy);
    }, TypeError);
    assertEquals([8, 10, 12], ToNumbers(taFull));
    SetHelper(lengthTracking, [15, 16]);
    assertEquals([15, 16, 12], ToNumbers(taFull));
    SetHelper(lengthTracking, [17, 18], 1);
    assertEquals([15, 17, 18], ToNumbers(taFull));
    assertThrows(() => {
      SetHelper(lengthTracking, [0, 0, 0, 0]);
    }, RangeError);
    assertThrows(() => {
      SetHelper(lengthTracking, [0, 0, 0], 1);
    }, RangeError);
    assertEquals([15, 17, 18], ToNumbers(taFull));
    SetHelper(lengthTrackingWithOffset, [19]);
    assertEquals([15, 17, 19], ToNumbers(taFull));
    assertThrows(() => {
      SetHelper(lengthTrackingWithOffset, [0, 0]);
    }, RangeError);
    assertThrows(() => {
      SetHelper(lengthTrackingWithOffset, [0], 1);
    }, RangeError);
    assertEquals([15, 17, 19], ToNumbers(taFull));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    assertThrows(() => {
      SetHelper(fixedLength, throwingProxy);
    }, TypeError);
    assertThrows(() => {
      SetHelper(fixedLengthWithOffset, throwingProxy);
    }, TypeError);
    assertThrows(() => {
      SetHelper(lengthTrackingWithOffset, throwingProxy);
    }, TypeError);
    assertEquals([15], ToNumbers(taFull));
    SetHelper(lengthTracking, [20]);
    assertEquals([20], ToNumbers(taFull));

    // Shrink to zero.
    rab.resize(0);
    assertThrows(() => {
      SetHelper(fixedLength, throwingProxy);
    }, TypeError);
    assertThrows(() => {
      SetHelper(fixedLengthWithOffset, throwingProxy);
    }, TypeError);
    assertThrows(() => {
      SetHelper(lengthTrackingWithOffset, throwingProxy);
    }, TypeError);
    assertThrows(() => {
      SetHelper(lengthTracking, [0]);
    }, RangeError);
    assertEquals([], ToNumbers(taFull));

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [0, 0, 0, 0, 0, 0]
    //              [0, 0, 0, 0] << fixedLength
    //                    [0, 0] << fixedLengthWithOffset
    //              [0, 0, 0, 0, 0, 0, ...] << lengthTracking
    //                    [0, 0, 0, 0, ...] << lengthTrackingWithOffset
    SetHelper(fixedLength, [21, 22]);
    assertEquals([21, 22, 0, 0, 0, 0], ToNumbers(taFull));
    SetHelper(fixedLength, [23, 24], 1);
    assertEquals([21, 23, 24, 0, 0, 0], ToNumbers(taFull));
    assertThrows(() => {
      SetHelper(fixedLength, [0, 0, 0, 0, 0]);
    }, RangeError);
    assertThrows(() => {
      SetHelper(fixedLength, [0, 0, 0, 0], 1);
    }, RangeError);
    assertEquals([21, 23, 24, 0, 0, 0], ToNumbers(taFull));
    SetHelper(fixedLengthWithOffset, [25, 26]);
    assertEquals([21, 23, 25, 26, 0, 0], ToNumbers(taFull));
    SetHelper(fixedLengthWithOffset, [27], 1);
    assertEquals([21, 23, 25, 27, 0, 0], ToNumbers(taFull));
    assertThrows(() => {
      SetHelper(fixedLengthWithOffset, [0, 0, 0]);
    }, RangeError);
    assertThrows(() => {
      SetHelper(fixedLengthWithOffset, [0, 0], 1);
    }, RangeError);
    assertEquals([21, 23, 25, 27, 0, 0], ToNumbers(taFull));
    SetHelper(lengthTracking, [28, 29, 30, 31, 32, 33]);
    assertEquals([28, 29, 30, 31, 32, 33], ToNumbers(taFull));
    SetHelper(lengthTracking, [34, 35, 36, 37, 38], 1);
    assertEquals([28, 34, 35, 36, 37, 38], ToNumbers(taFull));
    assertThrows(() => {
      SetHelper(lengthTracking, [0, 0, 0, 0, 0, 0, 0]);
    }, RangeError);
    assertThrows(() => {
      SetHelper(lengthTracking, [0, 0, 0, 0, 0, 0], 1);
    }, RangeError);
    assertEquals([28, 34, 35, 36, 37, 38], ToNumbers(taFull));
    SetHelper(lengthTrackingWithOffset, [39, 40, 41, 42]);
    assertEquals([28, 34, 39, 40, 41, 42], ToNumbers(taFull));
    SetHelper(lengthTrackingWithOffset, [43, 44, 45], 1);
    assertEquals([28, 34, 39, 43, 44, 45], ToNumbers(taFull));
    assertThrows(() => {
      SetHelper(lengthTrackingWithOffset, [0, 0, 0, 0, 0]);
    }, RangeError);
    assertThrows(() => {
      SetHelper(lengthTrackingWithOffset, [0, 0, 0, 0], 1);
    }, RangeError);
    assertEquals([28, 34, 39, 43, 44, 45], ToNumbers(taFull));
  };
  for (var ctor of ctors) {
    _loop93();
  }
})();
(function SetSourceLengthGetterShrinksTarget() {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var rab;
  var resizeTo;
  function CreateSourceProxy(length) {
    return new Proxy({}, {
      get(target, prop, receiver) {
        if (prop == 'length') {
          rab.resize(resizeTo);
          return length;
        }
        return true; // Can be converted to both BigInt and Number.
      }
    });
  }

  // Tests where the length getter returns a non-zero value -> these are nop if
  // the TA went OOB.
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    resizeTo = 3 * ctor.BYTES_PER_ELEMENT;
    fixedLength.set(CreateSourceProxy(1));
    assertEquals([0, 2, 4], ToNumbers(new ctor(rab)));
  }
  for (var _ctor166 of ctors) {
    rab = CreateRabForTest(_ctor166);
    var fixedLengthWithOffset = new _ctor166(rab, 2 * _ctor166.BYTES_PER_ELEMENT, 2);
    resizeTo = 3 * _ctor166.BYTES_PER_ELEMENT;
    fixedLengthWithOffset.set(CreateSourceProxy(1));
    assertEquals([0, 2, 4], ToNumbers(new _ctor166(rab)));
  }
  for (var _ctor167 of ctors) {
    rab = CreateRabForTest(_ctor167);
    var lengthTracking = new _ctor167(rab, 0);
    resizeTo = 3 * _ctor167.BYTES_PER_ELEMENT;
    lengthTracking.set(CreateSourceProxy(1));
    assertEquals([1, 2, 4], ToNumbers(lengthTracking));
    assertEquals([1, 2, 4], ToNumbers(new _ctor167(rab)));
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  for (var _ctor168 of ctors) {
    rab = CreateRabForTest(_ctor168);
    var _lengthTracking23 = new _ctor168(rab, 0);
    resizeTo = 0;
    _lengthTracking23.set(CreateSourceProxy(1));
    assertEquals([], ToNumbers(_lengthTracking23));
    assertEquals([], ToNumbers(new _ctor168(rab)));
  }
  for (var _ctor169 of ctors) {
    rab = CreateRabForTest(_ctor169);
    var lengthTrackingWithOffset = new _ctor169(rab, 2 * _ctor169.BYTES_PER_ELEMENT);
    resizeTo = 3 * _ctor169.BYTES_PER_ELEMENT;
    lengthTrackingWithOffset.set(CreateSourceProxy(1));
    assertEquals([1], ToNumbers(lengthTrackingWithOffset));
    assertEquals([0, 2, 1], ToNumbers(new _ctor169(rab)));
  }

  // Length-tracking TA goes OOB because of the offset.
  for (var _ctor170 of ctors) {
    rab = CreateRabForTest(_ctor170);
    var _lengthTrackingWithOffset9 = new _ctor170(rab, 2 * _ctor170.BYTES_PER_ELEMENT);
    resizeTo = 1 * _ctor170.BYTES_PER_ELEMENT;
    _lengthTrackingWithOffset9.set(CreateSourceProxy(1));
    assertEquals([0], ToNumbers(new _ctor170(rab)));
  }

  // Tests where the length getter returns a zero -> these don't throw even if
  // the TA went OOB.
  for (var _ctor171 of ctors) {
    rab = CreateRabForTest(_ctor171);
    var _fixedLength7 = new _ctor171(rab, 0, 4);
    resizeTo = 3 * _ctor171.BYTES_PER_ELEMENT;
    _fixedLength7.set(CreateSourceProxy(0));
    assertEquals([0, 2, 4], ToNumbers(new _ctor171(rab)));
  }
  for (var _ctor172 of ctors) {
    rab = CreateRabForTest(_ctor172);
    var _fixedLengthWithOffset7 = new _ctor172(rab, 2 * _ctor172.BYTES_PER_ELEMENT, 2);
    resizeTo = 3 * _ctor172.BYTES_PER_ELEMENT;
    _fixedLengthWithOffset7.set(CreateSourceProxy(0));
    assertEquals([0, 2, 4], ToNumbers(new _ctor172(rab)));
  }
  for (var _ctor173 of ctors) {
    rab = CreateRabForTest(_ctor173);
    var _lengthTracking24 = new _ctor173(rab, 0);
    resizeTo = 3 * _ctor173.BYTES_PER_ELEMENT;
    _lengthTracking24.set(CreateSourceProxy(0));
    assertEquals([0, 2, 4], ToNumbers(new _ctor173(rab)));
  }
  for (var _ctor174 of ctors) {
    rab = CreateRabForTest(_ctor174);
    var _lengthTrackingWithOffset0 = new _ctor174(rab, 2 * _ctor174.BYTES_PER_ELEMENT);
    resizeTo = 3 * _ctor174.BYTES_PER_ELEMENT;
    _lengthTrackingWithOffset0.set(CreateSourceProxy(0));
    assertEquals([0, 2, 4], ToNumbers(new _ctor174(rab)));
  }

  // Length-tracking TA goes OOB because of the offset.
  for (var _ctor175 of ctors) {
    rab = CreateRabForTest(_ctor175);
    var _lengthTrackingWithOffset1 = new _ctor175(rab, 2 * _ctor175.BYTES_PER_ELEMENT);
    resizeTo = 1 * _ctor175.BYTES_PER_ELEMENT;
    _lengthTrackingWithOffset1.set(CreateSourceProxy(0));
    assertEquals([0], ToNumbers(new _ctor175(rab)));
  }
})();
(function SetSourceLengthGetterGrowsTarget() {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var rab;
  var resizeTo;
  function CreateSourceProxy(length) {
    return new Proxy({}, {
      get(target, prop, receiver) {
        if (prop == 'length') {
          rab.resize(resizeTo);
          return length;
        }
        return true; // Can be converted to both BigInt and Number.
      }
    });
  }

  // Test that we still throw for lengthTracking TAs if the source length is
  // too large, even though we resized in the length getter (we check against
  // the original length).
  var _loop94 = function () {
    rab = CreateRabForTest(ctor);
    var lengthTracking = new ctor(rab, 0);
    resizeTo = 6 * ctor.BYTES_PER_ELEMENT;
    assertThrows(() => {
      lengthTracking.set(CreateSourceProxy(6));
    }, RangeError);
    assertEquals([0, 2, 4, 6, 0, 0], ToNumbers(new ctor(rab)));
  };
  for (var ctor of ctors) {
    _loop94();
  }
  var _loop95 = function () {
    rab = CreateRabForTest(_ctor176);
    var lengthTrackingWithOffset = new _ctor176(rab, 2 * _ctor176.BYTES_PER_ELEMENT);
    resizeTo = 6 * _ctor176.BYTES_PER_ELEMENT;
    assertThrows(() => {
      lengthTrackingWithOffset.set(CreateSourceProxy(4));
    }, RangeError);
    assertEquals([0, 2, 4, 6, 0, 0], ToNumbers(new _ctor176(rab)));
  };
  for (var _ctor176 of ctors) {
    _loop95();
  }
})();
(function SetShrinkTargetMidIteration() {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var rab;
  // Resizing will happen when we're calling Get for the `resizeAt`:th data
  // element, but we haven't yet written it to the target.
  var resizeAt;
  var resizeTo;
  function CreateSourceProxy(length) {
    var requestedIndices = [];
    return new Proxy({}, {
      get(target, prop, receiver) {
        if (prop == 'length') {
          return length;
        }
        requestedIndices.push(prop);
        if (requestedIndices.length == resizeAt) {
          rab.resize(resizeTo);
        }
        return true; // Can be converted to both BigInt and Number.
      }
    });
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    resizeAt = 2;
    resizeTo = 3 * ctor.BYTES_PER_ELEMENT;
    fixedLength.set(CreateSourceProxy(4));
    assertEquals([1, 2, 4], ToNumbers(new ctor(rab)));
  }
  for (var _ctor177 of ctors) {
    rab = CreateRabForTest(_ctor177);
    var fixedLengthWithOffset = new _ctor177(rab, 2 * _ctor177.BYTES_PER_ELEMENT, 2);
    resizeAt = 2;
    resizeTo = 3 * _ctor177.BYTES_PER_ELEMENT;
    fixedLengthWithOffset.set(CreateSourceProxy(2));
    assertEquals([0, 2, 1], ToNumbers(new _ctor177(rab)));
  }
  for (var _ctor178 of ctors) {
    rab = CreateRabForTest(_ctor178);
    var lengthTracking = new _ctor178(rab, 0);
    resizeAt = 2;
    resizeTo = 3 * _ctor178.BYTES_PER_ELEMENT;
    lengthTracking.set(CreateSourceProxy(2));
    assertEquals([1, 1, 4], ToNumbers(lengthTracking));
    assertEquals([1, 1, 4], ToNumbers(new _ctor178(rab)));
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  for (var _ctor179 of ctors) {
    rab = CreateRabForTest(_ctor179);
    var _lengthTracking25 = new _ctor179(rab, 0);
    resizeAt = 2;
    resizeTo = 0;
    _lengthTracking25.set(CreateSourceProxy(2));
    assertEquals([], ToNumbers(_lengthTracking25));
    assertEquals([], ToNumbers(new _ctor179(rab)));
  }
  for (var _ctor180 of ctors) {
    rab = CreateRabForTest(_ctor180);
    var lengthTrackingWithOffset = new _ctor180(rab, 2 * _ctor180.BYTES_PER_ELEMENT);
    resizeAt = 2;
    resizeTo = 3 * _ctor180.BYTES_PER_ELEMENT;
    lengthTrackingWithOffset.set(CreateSourceProxy(2));
    assertEquals([1], ToNumbers(lengthTrackingWithOffset));
    assertEquals([0, 2, 1], ToNumbers(new _ctor180(rab)));
  }

  // Length-tracking TA goes OOB because of the offset.
  for (var _ctor181 of ctors) {
    rab = CreateRabForTest(_ctor181);
    var _lengthTrackingWithOffset10 = new _ctor181(rab, 2 * _ctor181.BYTES_PER_ELEMENT);
    resizeAt = 1;
    resizeTo = 1 * _ctor181.BYTES_PER_ELEMENT;
    _lengthTrackingWithOffset10.set(CreateSourceProxy(2));
    assertEquals([0], ToNumbers(new _ctor181(rab)));
  }
})();
(function SetGrowTargetMidIteration() {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //                    [4, 6] << fixedLengthWithOffset
  //              [0, 2, 4, 6, ...] << lengthTracking
  //                    [4, 6, ...] << lengthTrackingWithOffset
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }
  var rab;
  // Resizing will happen when we're calling Get for the `resizeAt`:th data
  // element, but we haven't yet written it to the target.
  var resizeAt;
  var resizeTo;
  function CreateSourceProxy(length) {
    var requestedIndices = [];
    return new Proxy({}, {
      get(target, prop, receiver) {
        if (prop == 'length') {
          return length;
        }
        requestedIndices.push(prop);
        if (requestedIndices.length == resizeAt) {
          rab.resize(resizeTo);
        }
        return true; // Can be converted to both BigInt and Number.
      }
    });
  }
  for (var ctor of ctors) {
    rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    resizeAt = 2;
    resizeTo = 6 * ctor.BYTES_PER_ELEMENT;
    fixedLength.set(CreateSourceProxy(4));
    assertEquals([1, 1, 1, 1], ToNumbers(fixedLength));
    assertEquals([1, 1, 1, 1, 0, 0], ToNumbers(new ctor(rab)));
  }
  for (var _ctor182 of ctors) {
    rab = CreateRabForTest(_ctor182);
    var fixedLengthWithOffset = new _ctor182(rab, 2 * _ctor182.BYTES_PER_ELEMENT, 2);
    resizeAt = 1;
    resizeTo = 6 * _ctor182.BYTES_PER_ELEMENT;
    fixedLengthWithOffset.set(CreateSourceProxy(2));
    assertEquals([1, 1], ToNumbers(fixedLengthWithOffset));
    assertEquals([0, 2, 1, 1, 0, 0], ToNumbers(new _ctor182(rab)));
  }
  for (var _ctor183 of ctors) {
    rab = CreateRabForTest(_ctor183);
    var lengthTracking = new _ctor183(rab, 0);
    resizeAt = 2;
    resizeTo = 6 * _ctor183.BYTES_PER_ELEMENT;
    lengthTracking.set(CreateSourceProxy(2));
    assertEquals([1, 1, 4, 6, 0, 0], ToNumbers(lengthTracking));
    assertEquals([1, 1, 4, 6, 0, 0], ToNumbers(new _ctor183(rab)));
  }
  for (var _ctor184 of ctors) {
    rab = CreateRabForTest(_ctor184);
    var lengthTrackingWithOffset = new _ctor184(rab, 2 * _ctor184.BYTES_PER_ELEMENT);
    resizeAt = 1;
    resizeTo = 6 * _ctor184.BYTES_PER_ELEMENT;
    lengthTrackingWithOffset.set(CreateSourceProxy(2));
    assertEquals([1, 1, 0, 0], ToNumbers(lengthTrackingWithOffset));
    assertEquals([0, 2, 1, 1, 0, 0], ToNumbers(new _ctor184(rab)));
  }
})();
(function SetWithResizableSource() {
  for (var targetIsResizable of [false, true]) {
    for (var targetCtor of ctors) {
      var _loop96 = function () {
        var rab = CreateResizableArrayBuffer(4 * sourceCtor.BYTES_PER_ELEMENT, 8 * sourceCtor.BYTES_PER_ELEMENT);
        var fixedLength = new sourceCtor(rab, 0, 4);
        var fixedLengthWithOffset = new sourceCtor(rab, 2 * sourceCtor.BYTES_PER_ELEMENT, 2);
        var lengthTracking = new sourceCtor(rab, 0);
        var lengthTrackingWithOffset = new sourceCtor(rab, 2 * sourceCtor.BYTES_PER_ELEMENT);

        // Write some data into the array.
        var taFull = new sourceCtor(rab);
        for (var i = 0; i < 4; ++i) {
          WriteToTypedArray(taFull, i, i + 1);
        }

        // Orig. array: [1, 2, 3, 4]
        //              [1, 2, 3, 4] << fixedLength
        //                    [3, 4] << fixedLengthWithOffset
        //              [1, 2, 3, 4, ...] << lengthTracking
        //                    [3, 4, ...] << lengthTrackingWithOffset

        var targetAb = targetIsResizable ? new ArrayBuffer(6 * targetCtor.BYTES_PER_ELEMENT) : new ArrayBuffer(6 * targetCtor.BYTES_PER_ELEMENT, {
          maxByteLength: 8 * targetCtor.BYTES_PER_ELEMENT
        });
        var target = new targetCtor(targetAb);
        if (IsBigIntTypedArray(target) != IsBigIntTypedArray(taFull)) {
          // Can't mix BigInt and non-BigInt types.
          return 1; // continue
        }
        SetHelper(target, fixedLength);
        assertEquals([1, 2, 3, 4, 0, 0], ToNumbers(target));
        SetHelper(target, fixedLengthWithOffset);
        assertEquals([3, 4, 3, 4, 0, 0], ToNumbers(target));
        SetHelper(target, lengthTracking, 1);
        assertEquals([3, 1, 2, 3, 4, 0], ToNumbers(target));
        SetHelper(target, lengthTrackingWithOffset, 1);
        assertEquals([3, 3, 4, 3, 4, 0], ToNumbers(target));

        // Shrink so that fixed length TAs go out of bounds.
        rab.resize(3 * sourceCtor.BYTES_PER_ELEMENT);

        // Orig. array: [1, 2, 3]
        //              [1, 2, 3, ...] << lengthTracking
        //                    [3, ...] << lengthTrackingWithOffset

        assertThrows(() => {
          SetHelper(target, fixedLength);
        }, TypeError);
        assertThrows(() => {
          SetHelper(target, fixedLengthWithOffset);
        }, TypeError);
        assertEquals([3, 3, 4, 3, 4, 0], ToNumbers(target));
        SetHelper(target, lengthTracking);
        assertEquals([1, 2, 3, 3, 4, 0], ToNumbers(target));
        SetHelper(target, lengthTrackingWithOffset);
        assertEquals([3, 2, 3, 3, 4, 0], ToNumbers(target));

        // Shrink so that the TAs with offset go out of bounds.
        rab.resize(1 * sourceCtor.BYTES_PER_ELEMENT);
        assertThrows(() => {
          SetHelper(target, fixedLength);
        }, TypeError);
        assertThrows(() => {
          SetHelper(target, fixedLengthWithOffset);
        }, TypeError);
        assertThrows(() => {
          SetHelper(target, lengthTrackingWithOffset);
        }, TypeError);
        SetHelper(target, lengthTracking, 3);
        assertEquals([3, 2, 3, 1, 4, 0], ToNumbers(target));

        // Shrink to zero.
        rab.resize(0);
        assertThrows(() => {
          SetHelper(target, fixedLength);
        }, TypeError);
        assertThrows(() => {
          SetHelper(target, fixedLengthWithOffset);
        }, TypeError);
        assertThrows(() => {
          SetHelper(target, lengthTrackingWithOffset);
        }, TypeError);
        SetHelper(target, lengthTracking, 4);
        assertEquals([3, 2, 3, 1, 4, 0], ToNumbers(target));

        // Grow so that all TAs are back in-bounds.
        rab.resize(6 * sourceCtor.BYTES_PER_ELEMENT);
        for (var _i54 = 0; _i54 < 6; ++_i54) {
          WriteToTypedArray(taFull, _i54, _i54 + 1);
        }

        // Orig. array: [1, 2, 3, 4, 5, 6]
        //              [1, 2, 3, 4] << fixedLength
        //                    [3, 4] << fixedLengthWithOffset
        //              [1, 2, 3, 4, 5, 6, ...] << lengthTracking
        //                    [3, 4, 5, 6, ...] << lengthTrackingWithOffset

        SetHelper(target, fixedLength);
        assertEquals([1, 2, 3, 4, 4, 0], ToNumbers(target));
        SetHelper(target, fixedLengthWithOffset);
        assertEquals([3, 4, 3, 4, 4, 0], ToNumbers(target));
        SetHelper(target, lengthTracking, 0);
        assertEquals([1, 2, 3, 4, 5, 6], ToNumbers(target));
        SetHelper(target, lengthTrackingWithOffset, 1);
        assertEquals([1, 3, 4, 5, 6, 6], ToNumbers(target));
      };
      for (var sourceCtor of ctors) {
        if (_loop96()) continue;
      }
    }
  }
})();
(function Subarray() {
  var _loop97 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);

    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }

    // Orig. array: [0, 2, 4, 6]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, ...] << lengthTracking
    //                    [4, 6, ...] << lengthTrackingWithOffset

    var fixedLengthSubFull = fixedLength.subarray(0);
    assertEquals([0, 2, 4, 6], ToNumbers(fixedLengthSubFull));
    var fixedLengthWithOffsetSubFull = fixedLengthWithOffset.subarray(0);
    assertEquals([4, 6], ToNumbers(fixedLengthWithOffsetSubFull));
    var lengthTrackingSubFull = lengthTracking.subarray(0);
    assertEquals([0, 2, 4, 6], ToNumbers(lengthTrackingSubFull));
    var lengthTrackingWithOffsetSubFull = lengthTrackingWithOffset.subarray(0);
    assertEquals([4, 6], ToNumbers(lengthTrackingWithOffsetSubFull));

    // Relative offsets
    assertEquals([4, 6], ToNumbers(fixedLength.subarray(-2)));
    assertEquals([6], ToNumbers(fixedLengthWithOffset.subarray(-1)));
    assertEquals([4, 6], ToNumbers(lengthTracking.subarray(-2)));
    assertEquals([6], ToNumbers(lengthTrackingWithOffset.subarray(-1)));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [0, 2, 4]
    //              [0, 2, 4, ...] << lengthTracking
    //                    [4, ...] << lengthTrackingWithOffset

    // We can create subarrays of OOB arrays (which have length 0), as long as
    // the new arrays are not OOB.
    assertEquals([], ToNumbers(fixedLength.subarray(0)));
    assertEquals([], ToNumbers(fixedLengthWithOffset.subarray(0)));
    assertEquals([0, 2, 4], ToNumbers(lengthTracking.subarray(0)));
    assertEquals([4], ToNumbers(lengthTrackingWithOffset.subarray(0)));

    // Also the previously created subarrays are OOB.
    assertEquals(0, fixedLengthSubFull.length);
    assertEquals(0, fixedLengthWithOffsetSubFull.length);

    // Relative offsets
    assertEquals([2, 4], ToNumbers(lengthTracking.subarray(-2)));
    assertEquals([4], ToNumbers(lengthTrackingWithOffset.subarray(-1)));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    assertEquals([], ToNumbers(fixedLength.subarray(0)));
    assertEquals([0], ToNumbers(lengthTracking.subarray(0)));

    // Even the 0-length subarray of fixedLengthWithOffset would be OOB ->
    // this throws.
    assertThrows(() => {
      fixedLengthWithOffset.subarray(0);
    }, RangeError);

    // Also the previously created subarrays are OOB.
    assertEquals(0, fixedLengthSubFull.length);
    assertEquals(0, fixedLengthWithOffsetSubFull.length);
    assertEquals(0, lengthTrackingWithOffsetSubFull.length);

    // Shrink to zero.
    rab.resize(0);
    assertEquals([], ToNumbers(fixedLength.subarray(0)));
    assertEquals([], ToNumbers(lengthTracking.subarray(0)));
    assertThrows(() => {
      fixedLengthWithOffset.subarray(0);
    }, RangeError);
    assertThrows(() => {
      lengthTrackingWithOffset.subarray(0);
    }, RangeError);

    // Also the previously created subarrays are OOB.
    assertEquals(0, fixedLengthSubFull.length);
    assertEquals(0, fixedLengthWithOffsetSubFull.length);
    assertEquals(0, lengthTrackingWithOffsetSubFull.length);

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    for (var _i55 = 0; _i55 < 6; ++_i55) {
      WriteToTypedArray(taWrite, _i55, 2 * _i55);
    }

    // Orig. array: [0, 2, 4, 6, 8, 10]
    //              [0, 2, 4, 6] << fixedLength
    //                    [4, 6] << fixedLengthWithOffset
    //              [0, 2, 4, 6, 8, 10, ...] << lengthTracking
    //                    [4, 6, 8, 10, ...] << lengthTrackingWithOffset

    assertEquals([0, 2, 4, 6], ToNumbers(fixedLength.subarray(0)));
    assertEquals([4, 6], ToNumbers(fixedLengthWithOffset.subarray(0)));
    assertEquals([0, 2, 4, 6, 8, 10], ToNumbers(lengthTracking.subarray(0)));
    assertEquals([4, 6, 8, 10], ToNumbers(lengthTrackingWithOffset.subarray(0)));

    // Also the previously created subarrays are no longer OOB.
    assertEquals(4, fixedLengthSubFull.length);
    assertEquals(2, fixedLengthWithOffsetSubFull.length);

    // Subarrays of length-tracking TAs that don't pass an explicit end argument
    // are also length-tracking.
    assertEquals(lengthTracking.length, lengthTrackingSubFull.length);
    assertEquals(lengthTrackingWithOffset.length, lengthTrackingWithOffsetSubFull.length);
  };
  for (var ctor of ctors) {
    _loop97();
  }
})();
(function SubarrayParameterConversionShrinks() {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //              [0, 2, 4, 6, ...] << lengthTracking
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }

  // Fixed-length TA + first parameter conversion shrinks. The old length is
  // used in the length computation, and the subarray construction fails.
  var _loop98 = function (ctor) {
    var rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * ctor.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertThrows(() => {
      fixedLength.subarray(evil);
    }, RangeError);
  };
  for (var ctor of ctors) {
    _loop98(ctor);
  }

  // Like the previous test, but now we construct a smaller subarray and it
  // succeeds.
  var _loop99 = function (_ctor185) {
    var rab = CreateRabForTest(_ctor185);
    var fixedLength = new _ctor185(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor185.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertEquals([0], ToNumbers(fixedLength.subarray(evil, 1)));
  };
  for (var _ctor185 of ctors) {
    _loop99(_ctor185);
  }

  // Fixed-length TA + second parameter conversion shrinks. The old length is
  // used in the length computation, and the subarray construction fails.
  var _loop100 = function (_ctor186) {
    var rab = CreateRabForTest(_ctor186);
    var fixedLength = new _ctor186(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor186.BYTES_PER_ELEMENT);
        return 3;
      }
    };
    assertThrows(() => {
      fixedLength.subarray(0, evil);
    }, RangeError);
  };
  for (var _ctor186 of ctors) {
    _loop100(_ctor186);
  }

  // Like the previous test, but now we construct a smaller subarray and it
  // succeeds.
  var _loop101 = function (_ctor187) {
    var rab = CreateRabForTest(_ctor187);
    var fixedLength = new _ctor187(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor187.BYTES_PER_ELEMENT);
        return 1;
      }
    };
    assertEquals([0], ToNumbers(fixedLength.subarray(0, evil)));
  };
  for (var _ctor187 of ctors) {
    _loop101(_ctor187);
  }

  // Shrinking + fixed-length TA, subarray construction succeeds even though the
  // TA goes OOB.
  var _loop102 = function (_ctor188) {
    var rab = CreateRabForTest(_ctor188);
    var fixedLength = new _ctor188(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor188.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertEquals([0], ToNumbers(fixedLength.subarray(evil, 1)));
  };
  for (var _ctor188 of ctors) {
    _loop102(_ctor188);
  }

  // Length-tracking TA + first parameter conversion shrinks. The old length is
  // used in the length computation, and the subarray construction fails.
  var _loop103 = function (_ctor189) {
    var rab = CreateRabForTest(_ctor189);
    var lengthTracking = new _ctor189(rab);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor189.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertThrows(() => {
      lengthTracking.subarray(evil, lengthTracking.length);
    });
  };
  for (var _ctor189 of ctors) {
    _loop103(_ctor189);
  }

  // Like the previous test, but now we construct a smaller subarray and it
  // succeeds.
  var _loop104 = function (_ctor190) {
    var rab = CreateRabForTest(_ctor190);
    var lengthTracking = new _ctor190(rab);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor190.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertEquals([0], ToNumbers(lengthTracking.subarray(evil, 1)));
  };
  for (var _ctor190 of ctors) {
    _loop104(_ctor190);
  }

  // Length-tracking TA + first parameter conversion shrinks. The second
  // parameter is negative -> the relative index is not recomputed, and the
  // subarray construction fails.
  var _loop105 = function (_ctor191) {
    var rab = CreateRabForTest(_ctor191);
    var lengthTracking = new _ctor191(rab);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor191.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertThrows(() => {
      lengthTracking.subarray(evil, -1);
    });
  };
  for (var _ctor191 of ctors) {
    _loop105(_ctor191);
  }

  // Length-tracking TA + second parameter conversion shrinks. The second
  // parameter is too large -> the subarray construction fails.
  var _loop106 = function (_ctor192) {
    var rab = CreateRabForTest(_ctor192);
    var lengthTracking = new _ctor192(rab);
    var evil = {
      valueOf: () => {
        rab.resize(2 * _ctor192.BYTES_PER_ELEMENT);
        return 3;
      }
    };
    assertThrows(() => {
      lengthTracking.subarray(0, evil);
    });
  };
  for (var _ctor192 of ctors) {
    _loop106(_ctor192);
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  var _loop107 = function () {
    var rab = CreateRabForTest(_ctor193);
    var lengthTracking = new _ctor193(rab);
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return 1;
      }
    };
    assertThrows(() => {
      lengthTracking.subarray(0, evil);
    });
  };
  for (var _ctor193 of ctors) {
    _loop107();
  }

  // Like the previous test, but now we construct a smaller subarray and it
  // succeeds.
  var _loop108 = function () {
    var rab = CreateRabForTest(_ctor194);
    var lengthTracking = new _ctor194(rab);
    var evil = {
      valueOf: () => {
        rab.resize(0);
        return 0;
      }
    };
    assertEquals([], ToNumbers(lengthTracking.subarray(evil, 0)));
  };
  for (var _ctor194 of ctors) {
    _loop108();
  }
})();
(function SubarrayParameterConversionGrows() {
  // Orig. array: [0, 2, 4, 6]
  //              [0, 2, 4, 6] << fixedLength
  //              [0, 2, 4, 6, ...] << lengthTracking
  function CreateRabForTest(ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    // Write some data into the array.
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, 2 * i);
    }
    return rab;
  }

  // Growing a fixed length TA back in bounds.
  var _loop109 = function (ctor) {
    var rab = CreateRabForTest(ctor);
    var fixedLength = new ctor(rab, 0, 4);

    // Make `fixedLength` OOB.
    rab.resize(2 * ctor.BYTES_PER_ELEMENT);
    var evil = {
      valueOf: () => {
        rab.resize(4 * ctor.BYTES_PER_ELEMENT);
        return 0;
      }
    };

    // The length computation is done before parameter conversion. At that
    // point, the length is 0, since the TA is OOB.
    assertEquals([], ToNumbers(fixedLength.subarray(evil, 0, 1)));
  };
  for (var ctor of ctors) {
    _loop109(ctor);
  }

  // Growing + fixed-length TA. Growing won't affect anything.
  var _loop110 = function (_ctor195) {
    var rab = CreateRabForTest(_ctor195);
    var fixedLength = new _ctor195(rab, 0, 4);
    var evil = {
      valueOf: () => {
        rab.resize(6 * _ctor195.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertEquals([0, 2, 4, 6], ToNumbers(fixedLength.subarray(evil)));
  };
  for (var _ctor195 of ctors) {
    _loop110(_ctor195);
  }

  // Growing + length-tracking TA. The length computation is done with the
  // original length.
  var _loop111 = function (_ctor196) {
    var rab = CreateRabForTest(_ctor196);
    var lengthTracking = new _ctor196(rab, 0);
    var evil = {
      valueOf: () => {
        rab.resize(6 * _ctor196.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertEquals([0, 2, 4, 6], ToNumbers(lengthTracking.subarray(evil, lengthTracking.length)));
  };
  for (var _ctor196 of ctors) {
    _loop111(_ctor196);
  }
})();

// This function cannot be reused between TypedArray.protoype.sort and
// Array.prototype.sort, since the default sorting functions differ.
(function SortWithDefaultComparison() {
  var _loop112 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);
    var taFull = new ctor(rab, 0);
    function WriteUnsortedData() {
      // Write some data into the array.
      for (var i = 0; i < taFull.length; ++i) {
        WriteToTypedArray(taFull, i, 10 - 2 * i);
      }
    }
    // Orig. array: [10, 8, 6, 4]
    //              [10, 8, 6, 4] << fixedLength
    //                     [6, 4] << fixedLengthWithOffset
    //              [10, 8, 6, 4, ...] << lengthTracking
    //                     [6, 4, ...] << lengthTrackingWithOffset

    WriteUnsortedData();
    fixedLength.sort();
    assertEquals([4, 6, 8, 10], ToNumbers(taFull));
    WriteUnsortedData();
    fixedLengthWithOffset.sort();
    assertEquals([10, 8, 4, 6], ToNumbers(taFull));
    WriteUnsortedData();
    lengthTracking.sort();
    assertEquals([4, 6, 8, 10], ToNumbers(taFull));
    WriteUnsortedData();
    lengthTrackingWithOffset.sort();
    assertEquals([10, 8, 4, 6], ToNumbers(taFull));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [10, 8, 6]
    //              [10, 8, 6, ...] << lengthTracking
    //                     [6, ...] << lengthTrackingWithOffset

    WriteUnsortedData();
    assertThrows(() => {
      fixedLength.sort();
    }, TypeError);
    WriteUnsortedData();
    assertThrows(() => {
      fixedLengthWithOffset.sort();
    }, TypeError);
    WriteUnsortedData();
    lengthTracking.sort();
    assertEquals([6, 8, 10], ToNumbers(taFull));
    WriteUnsortedData();
    lengthTrackingWithOffset.sort();
    assertEquals([10, 8, 6], ToNumbers(taFull));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    WriteUnsortedData();
    assertThrows(() => {
      fixedLength.sort();
    }, TypeError);
    WriteUnsortedData();
    assertThrows(() => {
      fixedLengthWithOffset.sort();
    }, TypeError);
    WriteUnsortedData();
    lengthTracking.sort();
    assertEquals([10], ToNumbers(taFull));
    WriteUnsortedData();
    assertThrows(() => {
      lengthTrackingWithOffset.sort();
    }, TypeError);

    // Shrink to zero.
    rab.resize(0);
    WriteUnsortedData();
    assertThrows(() => {
      fixedLength.sort();
    }, TypeError);
    WriteUnsortedData();
    assertThrows(() => {
      fixedLengthWithOffset.sort();
    }, TypeError);
    WriteUnsortedData();
    lengthTracking.sort();
    assertEquals([], ToNumbers(taFull));
    WriteUnsortedData();
    assertThrows(() => {
      lengthTrackingWithOffset.sort();
    }, TypeError);

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [10, 8, 6, 4, 2, 0]
    //              [10, 8, 6, 4] << fixedLength
    //                     [6, 4] << fixedLengthWithOffset
    //              [10, 8, 6, 4, 2, 0, ...] << lengthTracking
    //                     [6, 4, 2, 0, ...] << lengthTrackingWithOffset

    WriteUnsortedData();
    fixedLength.sort();
    assertEquals([4, 6, 8, 10, 2, 0], ToNumbers(taFull));
    WriteUnsortedData();
    fixedLengthWithOffset.sort();
    assertEquals([10, 8, 4, 6, 2, 0], ToNumbers(taFull));
    WriteUnsortedData();
    lengthTracking.sort();
    assertEquals([0, 2, 4, 6, 8, 10], ToNumbers(taFull));
    WriteUnsortedData();
    lengthTrackingWithOffset.sort();
    assertEquals([10, 8, 0, 2, 4, 6], ToNumbers(taFull));
  };
  for (var ctor of ctors) {
    _loop112();
  }
})();

// The default comparison function for Array.prototype.sort is the string sort.
(function ArraySortWithDefaultComparison() {
  var _loop113 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);
    var taFull = new ctor(rab, 0);
    function WriteUnsortedData() {
      // Write some data into the array.
      for (var i = 0; i < taFull.length; ++i) {
        WriteToTypedArray(taFull, i, 10 - 2 * i);
      }
    }
    // Orig. array: [10, 8, 6, 4]
    //              [10, 8, 6, 4] << fixedLength
    //                     [6, 4] << fixedLengthWithOffset
    //              [10, 8, 6, 4, ...] << lengthTracking
    //                     [6, 4, ...] << lengthTrackingWithOffset

    WriteUnsortedData();
    ArraySortHelper(fixedLength);
    assertEquals([10, 4, 6, 8], ToNumbers(taFull));
    WriteUnsortedData();
    ArraySortHelper(fixedLengthWithOffset);
    assertEquals([10, 8, 4, 6], ToNumbers(taFull));
    WriteUnsortedData();
    ArraySortHelper(lengthTracking);
    assertEquals([10, 4, 6, 8], ToNumbers(taFull));
    WriteUnsortedData();
    ArraySortHelper(lengthTrackingWithOffset);
    assertEquals([10, 8, 4, 6], ToNumbers(taFull));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [10, 8, 6]
    //              [10, 8, 6, ...] << lengthTracking
    //                     [6, ...] << lengthTrackingWithOffset

    WriteUnsortedData();
    ArraySortHelper(fixedLength); // OOB -> NOOP
    assertEquals([10, 8, 6], ToNumbers(taFull));
    ArraySortHelper(fixedLengthWithOffset); // OOB -> NOOP
    assertEquals([10, 8, 6], ToNumbers(taFull));
    ArraySortHelper(lengthTracking);
    assertEquals([10, 6, 8], ToNumbers(taFull));
    WriteUnsortedData();
    ArraySortHelper(lengthTrackingWithOffset);
    assertEquals([10, 8, 6], ToNumbers(taFull));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    WriteUnsortedData();
    ArraySortHelper(fixedLength); // OOB -> NOOP
    assertEquals([10], ToNumbers(taFull));
    ArraySortHelper(fixedLengthWithOffset); // OOB -> NOOP
    assertEquals([10], ToNumbers(taFull));
    ArraySortHelper(lengthTrackingWithOffset);
    assertEquals([10], ToNumbers(taFull)); // OOB -> NOOP

    ArraySortHelper(lengthTracking);
    assertEquals([10], ToNumbers(taFull));

    // Shrink to zero.
    rab.resize(0);
    ArraySortHelper(fixedLength); // OOB -> NOOP
    assertEquals([], ToNumbers(taFull));
    ArraySortHelper(fixedLengthWithOffset); // OOB -> NOOP
    assertEquals([], ToNumbers(taFull));
    ArraySortHelper(lengthTrackingWithOffset); // OOB -> NOOP
    assertEquals([], ToNumbers(taFull));
    ArraySortHelper(lengthTracking);
    assertEquals([], ToNumbers(taFull));

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [10, 8, 6, 4, 2, 0]
    //              [10, 8, 6, 4] << fixedLength
    //                     [6, 4] << fixedLengthWithOffset
    //              [10, 8, 6, 4, 2, 0, ...] << lengthTracking
    //                     [6, 4, 2, 0, ...] << lengthTrackingWithOffset

    WriteUnsortedData();
    ArraySortHelper(fixedLength);
    assertEquals([10, 4, 6, 8, 2, 0], ToNumbers(taFull));
    WriteUnsortedData();
    ArraySortHelper(fixedLengthWithOffset);
    assertEquals([10, 8, 4, 6, 2, 0], ToNumbers(taFull));
    WriteUnsortedData();
    ArraySortHelper(lengthTracking);
    assertEquals([0, 10, 2, 4, 6, 8], ToNumbers(taFull));
    WriteUnsortedData();
    ArraySortHelper(lengthTrackingWithOffset);
    assertEquals([10, 8, 0, 2, 4, 6], ToNumbers(taFull));
  };
  for (var ctor of ctors) {
    _loop113();
  }
})();
function SortWithCustomComparison(sortHelper, oobThrows) {
  var _loop114 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);
    var taFull = new ctor(rab, 0);
    function WriteUnsortedData() {
      // Write some data into the array.
      for (var i = 0; i < taFull.length; ++i) {
        WriteToTypedArray(taFull, i, 10 - i);
      }
    }
    function CustomComparison(a, b) {
      // Sort all odd numbers before even numbers.
      a = Number(a);
      b = Number(b);
      if (a % 2 == 1 && b % 2 == 0) {
        return -1;
      }
      if (a % 2 == 0 && b % 2 == 1) {
        return 1;
      }
      if (a < b) {
        return -1;
      }
      if (a > b) {
        return 1;
      }
      return 0;
    }
    // Orig. array: [10, 9, 8, 7]
    //              [10, 9, 8, 7] << fixedLength
    //                     [8, 7] << fixedLengthWithOffset
    //              [10, 9, 8, 7, ...] << lengthTracking
    //                     [8, 7, ...] << lengthTrackingWithOffset

    WriteUnsortedData();
    sortHelper(fixedLength, CustomComparison);
    assertEquals([7, 9, 8, 10], ToNumbers(taFull));
    WriteUnsortedData();
    sortHelper(fixedLengthWithOffset, CustomComparison);
    assertEquals([10, 9, 7, 8], ToNumbers(taFull));
    WriteUnsortedData();
    sortHelper(lengthTracking, CustomComparison);
    assertEquals([7, 9, 8, 10], ToNumbers(taFull));
    WriteUnsortedData();
    sortHelper(lengthTrackingWithOffset, CustomComparison);
    assertEquals([10, 9, 7, 8], ToNumbers(taFull));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [10, 9, 8]
    //              [10, 9, 8, ...] << lengthTracking
    //                     [8, ...] << lengthTrackingWithOffset

    WriteUnsortedData();
    if (oobThrows) {
      assertThrows(() => {
        sortHelper(fixedLength, CustomComparison);
      }, TypeError);
      assertEquals([10, 9, 8], ToNumbers(taFull));
      assertThrows(() => {
        sortHelper(fixedLengthWithOffset, CustomComparison);
      }, TypeError);
      assertEquals([10, 9, 8], ToNumbers(taFull));
    } else {
      sortHelper(fixedLength, CustomComparison);
      assertEquals([10, 9, 8], ToNumbers(taFull));
      sortHelper(fixedLengthWithOffset, CustomComparison);
      assertEquals([10, 9, 8], ToNumbers(taFull));
    }
    WriteUnsortedData();
    sortHelper(lengthTracking, CustomComparison);
    assertEquals([9, 8, 10], ToNumbers(taFull));
    WriteUnsortedData();
    sortHelper(lengthTrackingWithOffset, CustomComparison);
    assertEquals([10, 9, 8], ToNumbers(taFull));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    WriteUnsortedData();
    if (oobThrows) {
      assertThrows(() => {
        sortHelper(fixedLength, CustomComparison);
      }, TypeError);
      assertEquals([10], ToNumbers(taFull));
      assertThrows(() => {
        sortHelper(fixedLengthWithOffset, CustomComparison);
      }, TypeError);
      assertEquals([10], ToNumbers(taFull));
      assertThrows(() => {
        sortHelper(lengthTrackingWithOffset, CustomComparison);
      }, TypeError);
      assertEquals([10], ToNumbers(taFull));
    } else {
      sortHelper(fixedLength, CustomComparison);
      assertEquals([10], ToNumbers(taFull));
      sortHelper(fixedLengthWithOffset, CustomComparison);
      assertEquals([10], ToNumbers(taFull));
      sortHelper(lengthTrackingWithOffset, CustomComparison);
      assertEquals([10], ToNumbers(taFull));
    }
    WriteUnsortedData();
    sortHelper(lengthTracking, CustomComparison);
    assertEquals([10], ToNumbers(taFull));

    // Shrink to zero.
    rab.resize(0);
    if (oobThrows) {
      assertThrows(() => {
        sortHelper(fixedLength, CustomComparison);
      }, TypeError);
      assertThrows(() => {
        sortHelper(fixedLengthWithOffset, CustomComparison);
      }, TypeError);
      assertThrows(() => {
        sortHelper(lengthTrackingWithOffset, CustomComparison);
      }, TypeError);
    } else {
      sortHelper(fixedLength, CustomComparison);
      sortHelper(fixedLengthWithOffset, CustomComparison);
      sortHelper(lengthTrackingWithOffset, CustomComparison);
    }
    sortHelper(lengthTracking, CustomComparison);
    assertEquals([], ToNumbers(taFull));

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);

    // Orig. array: [10, 9, 8, 7, 6, 5]
    //              [10, 9, 8, 7] << fixedLength
    //                     [8, 7] << fixedLengthWithOffset
    //              [10, 9, 8, 7, 6, 5, ...] << lengthTracking
    //                     [8, 7, 6, 5, ...] << lengthTrackingWithOffset

    WriteUnsortedData();
    sortHelper(fixedLength, CustomComparison);
    assertEquals([7, 9, 8, 10, 6, 5], ToNumbers(taFull));
    WriteUnsortedData();
    sortHelper(fixedLengthWithOffset, CustomComparison);
    assertEquals([10, 9, 7, 8, 6, 5], ToNumbers(taFull));
    WriteUnsortedData();
    sortHelper(lengthTracking, CustomComparison);
    assertEquals([5, 7, 9, 6, 8, 10], ToNumbers(taFull));
    WriteUnsortedData();
    sortHelper(lengthTrackingWithOffset, CustomComparison);
    assertEquals([10, 9, 5, 7, 6, 8], ToNumbers(taFull));
  };
  for (var ctor of ctors) {
    _loop114();
  }
}
SortWithCustomComparison(TypedArraySortHelper, true);
SortWithCustomComparison(ArraySortHelper, false);
function SortCallbackShrinks(sortHelper) {
  function WriteUnsortedData(taFull) {
    for (var i = 0; i < taFull.length; ++i) {
      WriteToTypedArray(taFull, i, 10 - i);
    }
  }
  var rab;
  var resizeTo;
  function CustomComparison(a, b) {
    rab.resize(resizeTo);
    if (a < b) {
      return -1;
    }
    if (a > b) {
      return 1;
    }
    return 0;
  }

  // Fixed length TA.
  for (var ctor of ctors) {
    rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    resizeTo = 2 * ctor.BYTES_PER_ELEMENT;
    var fixedLength = new ctor(rab, 0, 4);
    var taFull = new ctor(rab, 0);
    WriteUnsortedData(taFull);
    sortHelper(fixedLength, CustomComparison);

    // The data is unchanged.
    assertEquals([10, 9], ToNumbers(taFull));
  }

  // Length-tracking TA.
  for (var _ctor197 of ctors) {
    rab = CreateResizableArrayBuffer(4 * _ctor197.BYTES_PER_ELEMENT, 8 * _ctor197.BYTES_PER_ELEMENT);
    resizeTo = 2 * _ctor197.BYTES_PER_ELEMENT;
    var lengthTracking = new _ctor197(rab, 0);
    var _taFull = new _ctor197(rab, 0);
    WriteUnsortedData(_taFull);
    sortHelper(lengthTracking, CustomComparison);

    // The sort result is implementation defined, but it contains 2 elements out
    // of the 4 original ones.
    var newData = ToNumbers(_taFull);
    assertEquals(2, newData.length);
    assertTrue([10, 9, 8, 7].includes(newData[0]));
    assertTrue([10, 9, 8, 7].includes(newData[1]));
  }
  for (var _ctor198 of ctors) {
    rab = CreateResizableArrayBuffer(4 * _ctor198.BYTES_PER_ELEMENT, 8 * _ctor198.BYTES_PER_ELEMENT);
    resizeTo = 0;
    var _lengthTracking26 = new _ctor198(rab, 0);
    var _taFull2 = new _ctor198(rab, 0);
    WriteUnsortedData(_taFull2);
    sortHelper(_lengthTracking26, CustomComparison);
  }
}
SortCallbackShrinks(TypedArraySortHelper);
SortCallbackShrinks(ArraySortHelper);
function SortCallbackGrows(sortHelper) {
  function WriteUnsortedData(taFull) {
    for (var i = 0; i < taFull.length; ++i) {
      WriteToTypedArray(taFull, i, 10 - i);
    }
  }
  var rab;
  var resizeTo;
  function CustomComparison(a, b) {
    rab.resize(resizeTo);
    if (a < b) {
      return -1;
    }
    if (a > b) {
      return 1;
    }
    return 0;
  }

  // Fixed length TA.
  for (var ctor of ctors) {
    rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    resizeTo = 6 * ctor.BYTES_PER_ELEMENT;
    var fixedLength = new ctor(rab, 0, 4);
    var taFull = new ctor(rab, 0);
    WriteUnsortedData(taFull);
    sortHelper(fixedLength, CustomComparison);

    // Growing doesn't affect the sorting.
    assertEquals([7, 8, 9, 10, 0, 0], ToNumbers(taFull));
  }

  // Length-tracking TA.
  for (var _ctor199 of ctors) {
    rab = CreateResizableArrayBuffer(4 * _ctor199.BYTES_PER_ELEMENT, 8 * _ctor199.BYTES_PER_ELEMENT);
    resizeTo = 6 * _ctor199.BYTES_PER_ELEMENT;
    var lengthTracking = new _ctor199(rab, 0);
    var _taFull3 = new _ctor199(rab, 0);
    WriteUnsortedData(_taFull3);
    sortHelper(lengthTracking, CustomComparison);

    // Growing doesn't affect the sorting. Only the elements that were part of
    // the original TA are sorted.
    assertEquals([7, 8, 9, 10, 0, 0], ToNumbers(_taFull3));
  }
}
SortCallbackGrows(TypedArraySortHelper);
SortCallbackGrows(ArraySortHelper);
(function ObjectDefinePropertyDefineProperties() {
  var _loop115 = function (helper) {
    var _loop116 = function () {
      var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
      var fixedLength = new ctor(rab, 0, 4);
      var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
      var lengthTracking = new ctor(rab, 0);
      var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);
      var taFull = new ctor(rab, 0);

      // Orig. array: [0, 0, 0, 0]
      //              [0, 0, 0, 0] << fixedLength
      //                    [0, 0] << fixedLengthWithOffset
      //              [0, 0, 0, 0, ...] << lengthTracking
      //                    [0, 0, ...] << lengthTrackingWithOffset

      helper(fixedLength, 0, 1);
      assertEquals([1, 0, 0, 0], ToNumbers(taFull));
      helper(fixedLengthWithOffset, 0, 2);
      assertEquals([1, 0, 2, 0], ToNumbers(taFull));
      helper(lengthTracking, 1, 3);
      assertEquals([1, 3, 2, 0], ToNumbers(taFull));
      helper(lengthTrackingWithOffset, 1, 4);
      assertEquals([1, 3, 2, 4], ToNumbers(taFull));
      assertThrows(() => {
        helper(fixedLength, 4, 8);
      }, TypeError);
      assertThrows(() => {
        helper(fixedLengthWithOffset, 2, 8);
      }, TypeError);
      assertThrows(() => {
        helper(lengthTracking, 4, 8);
      }, TypeError);
      assertThrows(() => {
        helper(lengthTrackingWithOffset, 2, 8);
      }, TypeError);

      // Shrink so that fixed length TAs go out of bounds.
      rab.resize(3 * ctor.BYTES_PER_ELEMENT);

      // Orig. array: [1, 3, 2]
      //              [1, 3, 2, ...] << lengthTracking
      //                    [2, ...] << lengthTrackingWithOffset

      assertThrows(() => {
        helper(fixedLength, 0, 8);
      }, TypeError);
      assertThrows(() => {
        helper(fixedLengthWithOffset, 0, 8);
      }, TypeError);
      assertEquals([1, 3, 2], ToNumbers(taFull));
      helper(lengthTracking, 0, 5);
      assertEquals([5, 3, 2], ToNumbers(taFull));
      helper(lengthTrackingWithOffset, 0, 6);
      assertEquals([5, 3, 6], ToNumbers(taFull));

      // Shrink so that the TAs with offset go out of bounds.
      rab.resize(1 * ctor.BYTES_PER_ELEMENT);
      assertThrows(() => {
        helper(fixedLength, 0, 8);
      }, TypeError);
      assertThrows(() => {
        helper(fixedLengthWithOffset, 0, 8);
      }, TypeError);
      assertThrows(() => {
        helper(lengthTrackingWithOffset, 0, 8);
      }, TypeError);
      assertEquals([5], ToNumbers(taFull));
      helper(lengthTracking, 0, 7);
      assertEquals([7], ToNumbers(taFull));

      // Shrink to zero.
      rab.resize(0);
      assertThrows(() => {
        helper(fixedLength, 0, 8);
      }, TypeError);
      assertThrows(() => {
        helper(fixedLengthWithOffset, 0, 8);
      }, TypeError);
      assertThrows(() => {
        helper(lengthTracking, 0, 8);
      }, TypeError);
      assertThrows(() => {
        helper(lengthTrackingWithOffset, 0, 8);
      }, TypeError);
      assertEquals([], ToNumbers(taFull));

      // Grow so that all TAs are back in-bounds.
      rab.resize(6 * ctor.BYTES_PER_ELEMENT);
      helper(fixedLength, 0, 9);
      assertEquals([9, 0, 0, 0, 0, 0], ToNumbers(taFull));
      helper(fixedLengthWithOffset, 0, 10);
      assertEquals([9, 0, 10, 0, 0, 0], ToNumbers(taFull));
      helper(lengthTracking, 1, 11);
      assertEquals([9, 11, 10, 0, 0, 0], ToNumbers(taFull));
      helper(lengthTrackingWithOffset, 2, 12);
      assertEquals([9, 11, 10, 0, 12, 0], ToNumbers(taFull));

      // Trying to define properties out of the fixed-length bounds throws.
      assertThrows(() => {
        helper(fixedLength, 5, 13);
      }, TypeError);
      assertThrows(() => {
        helper(fixedLengthWithOffset, 3, 13);
      }, TypeError);
      assertEquals([9, 11, 10, 0, 12, 0], ToNumbers(taFull));
      helper(lengthTracking, 4, 14);
      assertEquals([9, 11, 10, 0, 14, 0], ToNumbers(taFull));
      helper(lengthTrackingWithOffset, 3, 15);
      assertEquals([9, 11, 10, 0, 14, 15], ToNumbers(taFull));
    };
    for (var ctor of ctors) {
      _loop116();
    }
  };
  for (var helper of [ObjectDefinePropertyHelper, ObjectDefinePropertiesHelper]) {
    _loop115(helper);
  }
})();
(function ObjectDefinePropertyParameterConversionShrinks() {
  var helper = ObjectDefinePropertyHelper;
  // Fixed length.
  var _loop117 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var evil = {
      toString: () => {
        rab.resize(2 * ctor.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    assertThrows(() => {
      helper(fixedLength, evil, 8);
    }, TypeError);
  };
  for (var ctor of ctors) {
    _loop117(ctor);
  }
  // Length tracking.
  var _loop118 = function (_ctor200) {
    var rab = CreateResizableArrayBuffer(4 * _ctor200.BYTES_PER_ELEMENT, 8 * _ctor200.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor200(rab, 0);
    var evil = {
      toString: () => {
        rab.resize(2 * _ctor200.BYTES_PER_ELEMENT);
        return 3; // Index too large after resize.
      }
    };
    assertThrows(() => {
      helper(lengthTracking, evil, 8);
    }, TypeError);
  };
  for (var _ctor200 of ctors) {
    _loop118(_ctor200);
  }

  // Special case: resizing to 0 -> length-tracking TA still not OOB.
  var _loop119 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor201.BYTES_PER_ELEMENT, 8 * _ctor201.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor201(rab, 0);
    var evil = {
      toString: () => {
        rab.resize(0);
        return 0; // Index too large after resize.
      }
    };
    assertThrows(() => {
      helper(lengthTracking, evil, 8);
    }, TypeError);
  };
  for (var _ctor201 of ctors) {
    _loop119();
  }
})();
(function ObjectDefinePropertyParameterConversionGrows() {
  var helper = ObjectDefinePropertyHelper;
  // Fixed length.
  var _loop120 = function (ctor) {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    // Make fixedLength go OOB.
    rab.resize(2 * ctor.BYTES_PER_ELEMENT);
    var evil = {
      toString: () => {
        rab.resize(6 * ctor.BYTES_PER_ELEMENT);
        return 0;
      }
    };
    helper(fixedLength, evil, 8);
    assertEquals([8, 0, 0, 0], ToNumbers(fixedLength));
  };
  for (var ctor of ctors) {
    _loop120(ctor);
  }
  // Length tracking.
  var _loop121 = function (_ctor202) {
    var rab = CreateResizableArrayBuffer(4 * _ctor202.BYTES_PER_ELEMENT, 8 * _ctor202.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor202(rab, 0);
    var evil = {
      toString: () => {
        rab.resize(6 * _ctor202.BYTES_PER_ELEMENT);
        return 4; // Index valid after resize.
      }
    };
    helper(lengthTracking, evil, 8);
    assertEquals([0, 0, 0, 0, 8, 0], ToNumbers(lengthTracking));
  };
  for (var _ctor202 of ctors) {
    _loop121(_ctor202);
  }
})();
(function ObjectFreeze() {
  var _loop122 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);
    assertThrows(() => {
      Object.freeze(fixedLength);
    }, TypeError);
    assertThrows(() => {
      Object.freeze(fixedLengthWithOffset);
    }, TypeError);
    assertThrows(() => {
      Object.freeze(lengthTracking);
    }, TypeError);
    assertThrows(() => {
      Object.freeze(lengthTrackingWithOffset);
    }, TypeError);
  };
  // Freezing non-OOB non-zero-length TAs throws.
  for (var ctor of ctors) {
    _loop122();
  }
  // Freezing zero-length TAs throws because [[PreventExtensions]] returns false
  // for variable-length TAs.
  var _loop123 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor203.BYTES_PER_ELEMENT, 8 * _ctor203.BYTES_PER_ELEMENT);
    var fixedLength = new _ctor203(rab, 0, 0);
    var fixedLengthWithOffset = new _ctor203(rab, 2 * _ctor203.BYTES_PER_ELEMENT, 0);
    // Zero-length because the offset is at the end:
    var lengthTrackingWithOffset = new _ctor203(rab, 4 * _ctor203.BYTES_PER_ELEMENT);
    assertThrows(() => {
      Object.freeze(fixedLength);
    }, TypeError);
    assertThrows(() => {
      Object.freeze(fixedLengthWithOffset);
    }, TypeError);
    assertThrows(() => {
      Object.freeze(lengthTrackingWithOffset);
    }, TypeError);
  };
  for (var _ctor203 of ctors) {
    _loop123();
  }
  var _loop124 = function () {
    var rab = CreateResizableArrayBuffer(4 * _ctor204.BYTES_PER_ELEMENT, 8 * _ctor204.BYTES_PER_ELEMENT);
    var lengthTracking = new _ctor204(rab);
    var lengthTrackingWithOffset = new _ctor204(rab, 2 * _ctor204.BYTES_PER_ELEMENT);
    rab.resize(2 * _ctor204.BYTES_PER_ELEMENT);
    assertThrows(() => {
      Object.freeze(lengthTrackingWithOffset);
    }, TypeError);
    rab.resize(0 * _ctor204.BYTES_PER_ELEMENT);
    assertThrows(() => {
      Object.freeze(lengthTracking);
    }, TypeError);
  };
  for (var _ctor204 of ctors) {
    _loop124();
  }
})();
/* Rhino: We don't quite have rest parameters working in all cases
(function FunctionApply() {
  var _loop125 = function () {
    var rab = CreateResizableArrayBuffer(4 * ctor.BYTES_PER_ELEMENT, 8 * ctor.BYTES_PER_ELEMENT);
    var fixedLength = new ctor(rab, 0, 4);
    var fixedLengthWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new ctor(rab, 0);
    var lengthTrackingWithOffset = new ctor(rab, 2 * ctor.BYTES_PER_ELEMENT);
    var taWrite = new ctor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taWrite, i, i);
    }
    function func(...args) {
      return [...args];
    }
    assertEquals([0, 1, 2, 3], ToNumbers(func.apply(null, fixedLength)));
    assertEquals([2, 3], ToNumbers(func.apply(null, fixedLengthWithOffset)));
    assertEquals([0, 1, 2, 3], ToNumbers(func.apply(null, lengthTracking)));
    assertEquals([2, 3], ToNumbers(func.apply(null, lengthTrackingWithOffset)));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * ctor.BYTES_PER_ELEMENT);
    assertEquals([], ToNumbers(func.apply(null, fixedLength)));
    assertEquals([], ToNumbers(func.apply(null, fixedLengthWithOffset)));
    assertEquals([0, 1, 2], ToNumbers(func.apply(null, lengthTracking)));
    assertEquals([2], ToNumbers(func.apply(null, lengthTrackingWithOffset)));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * ctor.BYTES_PER_ELEMENT);
    assertEquals([], ToNumbers(func.apply(null, fixedLength)));
    assertEquals([], ToNumbers(func.apply(null, fixedLengthWithOffset)));
    assertEquals([0], ToNumbers(func.apply(null, lengthTracking)));
    assertEquals([], ToNumbers(func.apply(null, lengthTrackingWithOffset)));

    // Shrink to zero.
    rab.resize(0);
    assertEquals([], ToNumbers(func.apply(null, fixedLength)));
    assertEquals([], ToNumbers(func.apply(null, fixedLengthWithOffset)));
    assertEquals([], ToNumbers(func.apply(null, lengthTracking)));
    assertEquals([], ToNumbers(func.apply(null, lengthTrackingWithOffset)));

    // Grow so that all TAs are back in-bounds. New memory is zeroed.
    rab.resize(6 * ctor.BYTES_PER_ELEMENT);
    assertEquals([0, 0, 0, 0], ToNumbers(func.apply(null, fixedLength)));
    assertEquals([0, 0], ToNumbers(func.apply(null, fixedLengthWithOffset)));
    assertEquals([0, 0, 0, 0, 0, 0], ToNumbers(func.apply(null, lengthTracking)));
    assertEquals([0, 0, 0, 0], ToNumbers(func.apply(null, lengthTrackingWithOffset)));
  };
  for (var ctor of ctors) {
    _loop125();
  }
})();
 */
(function TypedArrayFrom() {
  AllBigIntMatchedCtorCombinations((targetCtor, sourceCtor) => {
    var rab = CreateResizableArrayBuffer(4 * sourceCtor.BYTES_PER_ELEMENT, 8 * sourceCtor.BYTES_PER_ELEMENT);
    var fixedLength = new sourceCtor(rab, 0, 4);
    var fixedLengthWithOffset = new sourceCtor(rab, 2 * sourceCtor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new sourceCtor(rab, 0);
    var lengthTrackingWithOffset = new sourceCtor(rab, 2 * sourceCtor.BYTES_PER_ELEMENT);

    // Write some data into the array.
    var taFull = new sourceCtor(rab);
    for (var i = 0; i < 4; ++i) {
      WriteToTypedArray(taFull, i, i + 1);
    }

    // Orig. array: [1, 2, 3, 4]
    //              [1, 2, 3, 4] << fixedLength
    //                    [3, 4] << fixedLengthWithOffset
    //              [1, 2, 3, 4, ...] << lengthTracking
    //                    [3, 4, ...] << lengthTrackingWithOffset

    assertEquals([1, 2, 3, 4], ToNumbers(targetCtor.from(fixedLength)));
    assertEquals([3, 4], ToNumbers(targetCtor.from(fixedLengthWithOffset)));
    assertEquals([1, 2, 3, 4], ToNumbers(targetCtor.from(lengthTracking)));
    assertEquals([3, 4], ToNumbers(targetCtor.from(lengthTrackingWithOffset)));

    // Shrink so that fixed length TAs go out of bounds.
    rab.resize(3 * sourceCtor.BYTES_PER_ELEMENT);

    // Orig. array: [1, 2, 3]
    //              [1, 2, 3, ...] << lengthTracking
    //                    [3, ...] << lengthTrackingWithOffset

    assertThrows(() => {
      targetCtor.from(fixedLength);
    }, TypeError);
    assertThrows(() => {
      targetCtor.from(fixedLengthWithOffset);
    }, TypeError);
    assertEquals([1, 2, 3], ToNumbers(targetCtor.from(lengthTracking)));
    assertEquals([3], ToNumbers(targetCtor.from(lengthTrackingWithOffset)));

    // Shrink so that the TAs with offset go out of bounds.
    rab.resize(1 * sourceCtor.BYTES_PER_ELEMENT);
    assertThrows(() => {
      targetCtor.from(fixedLength);
    }, TypeError);
    assertThrows(() => {
      targetCtor.from(fixedLengthWithOffset);
    }, TypeError);
    assertEquals([1], ToNumbers(targetCtor.from(lengthTracking)));
    assertThrows(() => {
      targetCtor.from(lengthTrackingWithOffset);
    }, TypeError);

    // Shrink to zero.
    rab.resize(0);
    assertThrows(() => {
      targetCtor.from(fixedLength);
    }, TypeError);
    assertThrows(() => {
      targetCtor.from(fixedLengthWithOffset);
    }, TypeError);
    assertEquals([], ToNumbers(targetCtor.from(lengthTracking)));
    assertThrows(() => {
      targetCtor.from(lengthTrackingWithOffset);
    }, TypeError);

    // Grow so that all TAs are back in-bounds.
    rab.resize(6 * sourceCtor.BYTES_PER_ELEMENT);
    for (var _i56 = 0; _i56 < 6; ++_i56) {
      WriteToTypedArray(taFull, _i56, _i56 + 1);
    }

    // Orig. array: [1, 2, 3, 4, 5, 6]
    //              [1, 2, 3, 4] << fixedLength
    //                    [3, 4] << fixedLengthWithOffset
    //              [1, 2, 3, 4, 5, 6, ...] << lengthTracking
    //                    [3, 4, 5, 6, ...] << lengthTrackingWithOffset

    assertEquals([1, 2, 3, 4], ToNumbers(targetCtor.from(fixedLength)));
    assertEquals([3, 4], ToNumbers(targetCtor.from(fixedLengthWithOffset)));
    assertEquals([1, 2, 3, 4, 5, 6], ToNumbers(targetCtor.from(lengthTracking)));
    assertEquals([3, 4, 5, 6], ToNumbers(targetCtor.from(lengthTrackingWithOffset)));
  });
  AllBigIntUnmatchedCtorCombinations((targetCtor, sourceCtor) => {
    var rab = CreateResizableArrayBuffer(4 * sourceCtor.BYTES_PER_ELEMENT, 8 * sourceCtor.BYTES_PER_ELEMENT);
    var fixedLength = new sourceCtor(rab, 0, 4);
    var fixedLengthWithOffset = new sourceCtor(rab, 2 * sourceCtor.BYTES_PER_ELEMENT, 2);
    var lengthTracking = new sourceCtor(rab, 0);
    var lengthTrackingWithOffset = new sourceCtor(rab, 2 * sourceCtor.BYTES_PER_ELEMENT);
    assertThrows(() => {
      targetCtor.from(fixedLength);
    }, TypeError);
    assertThrows(() => {
      targetCtor.from(fixedLengthWithOffset);
    }, TypeError);
    assertThrows(() => {
      targetCtor.from(lengthTracking);
    }, TypeError);
    assertThrows(() => {
      targetCtor.from(lengthTrackingWithOffset);
    }, TypeError);
  });
})();
(function ArrayBufferSizeNotMultipleOfElementSize() {
  // The buffer size is a prime, not multiple of anything.
  var rab = CreateResizableArrayBuffer(11, 20);
  for (var ctor of ctors) {
    if (ctor.BYTES_PER_ELEMENT == 1) continue;

    // This should not throw.
    new ctor(rab);
  }
})();
(function SetValueToNumberResizesToInBounds() {
  var _loop126 = function (ctor) {
    var rab = CreateResizableArrayBuffer(0, 1 * ctor.BYTES_PER_ELEMENT);
    var lengthTracking = new ctor(rab, 0);
    var evil = {
      valueOf: () => {
        // Resize so that `lengthTracking` is no longer OOB.
        rab.resize(1 * ctor.BYTES_PER_ELEMENT);
        if (IsBigIntTypedArray(lengthTracking)) {
          return 2n;
        }
        return 2;
      }
    };
    lengthTracking[0] = evil;
    assertEquals([2], ToNumbers(lengthTracking));
  };
  for (var ctor of ctors) {
    _loop126(ctor);
  }
})();

"success";
