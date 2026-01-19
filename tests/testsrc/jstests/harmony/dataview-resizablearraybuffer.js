// Copyright 2021 the V8 project authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Flags: --allow-natives-syntax --js-staging

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

(function DataViewPrototype() {
  var rab = CreateResizableArrayBuffer(40, 80);
  var ab = new ArrayBuffer(80);
  var dvRab = new DataView(rab, 0, 3);
  var dvAb = new DataView(ab, 0, 3);
  assertEquals(dvRab.__proto__, dvAb.__proto__);
})();
(function DataViewByteLength() {
  var rab = CreateResizableArrayBuffer(40, 80);
  var dv = new DataView(rab, 0, 3);
  assertEquals(rab, dv.buffer);
  assertEquals(3, dv.byteLength);
  var emptyDv = new DataView(rab, 0, 0);
  assertEquals(rab, emptyDv.buffer);
  assertEquals(0, emptyDv.byteLength);
  var dvWithOffset = new DataView(rab, 2, 3);
  assertEquals(rab, dvWithOffset.buffer);
  assertEquals(3, dvWithOffset.byteLength);
  var emptyDvWithOffset = new DataView(rab, 2, 0);
  assertEquals(rab, emptyDvWithOffset.buffer);
  assertEquals(0, emptyDvWithOffset.byteLength);
  var lengthTracking = new DataView(rab);
  assertEquals(rab, lengthTracking.buffer);
  assertEquals(40, lengthTracking.byteLength);
  var offset = 8;
  var lengthTrackingWithOffset = new DataView(rab, offset);
  assertEquals(rab, lengthTrackingWithOffset.buffer);
  assertEquals(40 - offset, lengthTrackingWithOffset.byteLength);
  var emptyLengthTrackingWithOffset = new DataView(rab, 40);
  assertEquals(rab, emptyLengthTrackingWithOffset.buffer);
  assertEquals(0, emptyLengthTrackingWithOffset.byteLength);
})();
(function ConstructInvalid() {
  var rab = CreateResizableArrayBuffer(40, 80);

  // Length too big.
  assertThrows(() => {
    new DataView(rab, 0, 41);
  }, RangeError);

  // Offset too close to the end.
  assertThrows(() => {
    new DataView(rab, 39, 2);
  }, RangeError);

  // Offset beyond end.
  assertThrows(() => {
    new DataView(rab, 40, 1);
  }, RangeError);
})();
(function ConstructorParameterConversionShrinks() {
  var rab = CreateResizableArrayBuffer(40, 80);
  var evil = {
    valueOf: () => {
      rab.resize(10);
      return 0;
    }
  };
  assertThrows(() => {
    new DataView(rab, evil, 20);
  }, RangeError);
})();
(function ConstructorParameterConversionGrows() {
  var gsab = CreateResizableArrayBuffer(40, 80);
  var evil = {
    valueOf: () => {
      gsab.resize(50);
      return 0;
    }
  };

  // Constructing will fail unless we take the new size into account.
  var dv = new DataView(gsab, evil, 50);
  assertEquals(50, dv.byteLength);
})();
/* Rhino: This is a test of our prototype and Reflect handling and not array buffers
(function OrdinaryCreateFromConstructorShrinks() {
  {
    var rab = CreateResizableArrayBuffer(16, 40);
    var newTarget = function () {}.bind(null);
    Object.defineProperty(newTarget, "prototype", {
      get() {
        rab.resize(8);
        return DataView.prototype;
      }
    });
    assertThrows(() => {
      Reflect.construct(DataView, [rab, 0, 16], newTarget);
    }, RangeError);
  }
  {
    var _rab = CreateResizableArrayBuffer(16, 40);
    var _newTarget = function () {}.bind(null);
    Object.defineProperty(_newTarget, "prototype", {
      get() {
        _rab.resize(6);
        return DataView.prototype;
      }
    });
    assertThrows(() => {
      Reflect.construct(DataView, [_rab, 8, 2], _newTarget);
    }, RangeError);
  }
})();
 */
(function DataViewByteLengthWhenResizedOutOfBounds1() {
  var rab = CreateResizableArrayBuffer(16, 40);
  var fixedLength = new DataView(rab, 0, 8);
  var lengthTracking = new DataView(rab);
  assertEquals(8, fixedLength.byteLength);
  assertEquals(16, lengthTracking.byteLength);
  assertEquals(0, fixedLength.byteOffset);
  assertEquals(0, lengthTracking.byteOffset);
  rab.resize(2);
  assertThrows(() => {
    fixedLength.byteLength;
  }, TypeError);
  assertEquals(2, lengthTracking.byteLength);
  assertThrows(() => {
    fixedLength.byteOffset;
  }, TypeError);
  assertEquals(0, lengthTracking.byteOffset);
  rab.resize(8);
  assertEquals(8, fixedLength.byteLength);
  assertEquals(8, lengthTracking.byteLength);
  assertEquals(0, fixedLength.byteOffset);
  assertEquals(0, lengthTracking.byteOffset);
  rab.resize(40);
  assertEquals(8, fixedLength.byteLength);
  assertEquals(40, lengthTracking.byteLength);
  assertEquals(0, fixedLength.byteOffset);
  assertEquals(0, lengthTracking.byteOffset);
  rab.resize(0);
  assertThrows(() => {
    fixedLength.byteLength;
  }, TypeError);
  assertEquals(0, lengthTracking.byteLength);
  assertThrows(() => {
    fixedLength.byteOffset;
  }, TypeError);
  assertEquals(0, lengthTracking.byteOffset);
})();

// The previous test with offsets.
(function DataViewByteLengthWhenResizedOutOfBounds2() {
  var rab = CreateResizableArrayBuffer(20, 40);
  var fixedLengthWithOffset = new DataView(rab, 8, 8);
  var lengthTrackingWithOffset = new DataView(rab, 8);
  assertEquals(8, fixedLengthWithOffset.byteLength);
  assertEquals(12, lengthTrackingWithOffset.byteLength);
  assertEquals(8, fixedLengthWithOffset.byteOffset);
  assertEquals(8, lengthTrackingWithOffset.byteOffset);
  rab.resize(10);
  assertThrows(() => {
    fixedLengthWithOffset.byteLength;
  }, TypeError);
  assertEquals(2, lengthTrackingWithOffset.byteLength);
  assertThrows(() => {
    fixedLengthWithOffset.byteOffset;
  }, TypeError);
  assertEquals(8, lengthTrackingWithOffset.byteOffset);
  rab.resize(16);
  assertEquals(8, fixedLengthWithOffset.byteLength);
  assertEquals(8, lengthTrackingWithOffset.byteLength);
  assertEquals(8, fixedLengthWithOffset.byteOffset);
  assertEquals(8, lengthTrackingWithOffset.byteOffset);
  rab.resize(40);
  assertEquals(8, fixedLengthWithOffset.byteLength);
  assertEquals(32, lengthTrackingWithOffset.byteLength);
  assertEquals(8, fixedLengthWithOffset.byteOffset);
  assertEquals(8, lengthTrackingWithOffset.byteOffset);
  rab.resize(6);
  assertThrows(() => {
    fixedLengthWithOffset.byteLength;
  }, TypeError);
  assertThrows(() => {
    lengthTrackingWithOffset.byteLength;
  }, TypeError);
  assertThrows(() => {
    fixedLengthWithOffset.byteOffset;
  }, TypeError);
  assertThrows(() => {
    lengthTrackingWithOffset.byteOffset;
  }, TypeError);
})();
(function GetAndSet() {
  var rab = CreateResizableArrayBuffer(64, 128);
  var fixedLength = new DataView(rab, 0, 32);
  var fixedLengthWithOffset = new DataView(rab, 2, 32);
  var lengthTracking = new DataView(rab, 0);
  var lengthTrackingWithOffset = new DataView(rab, 2);
  testDataViewMethodsUpToSize(fixedLength, 32);
  assertAllDataViewMethodsThrow(fixedLength, 33, RangeError);
  testDataViewMethodsUpToSize(fixedLengthWithOffset, 32);
  assertAllDataViewMethodsThrow(fixedLengthWithOffset, 33, RangeError);
  testDataViewMethodsUpToSize(lengthTracking, 64);
  assertAllDataViewMethodsThrow(lengthTracking, 65, RangeError);
  testDataViewMethodsUpToSize(lengthTrackingWithOffset, 64 - 2);
  assertAllDataViewMethodsThrow(lengthTrackingWithOffset, 64 - 2 + 1, RangeError);

  // Shrink so that fixed length TAs go out of bounds.
  rab.resize(30);
  assertAllDataViewMethodsThrow(fixedLength, 0, TypeError);
  assertAllDataViewMethodsThrow(fixedLengthWithOffset, 0, TypeError);
  testDataViewMethodsUpToSize(lengthTracking, 30);
  testDataViewMethodsUpToSize(lengthTrackingWithOffset, 30 - 2);

  // Shrink so that the TAs with offset go out of bounds.
  rab.resize(1);
  assertAllDataViewMethodsThrow(fixedLength, 0, TypeError);
  assertAllDataViewMethodsThrow(fixedLengthWithOffset, 0, TypeError);
  assertAllDataViewMethodsThrow(lengthTrackingWithOffset, 0, TypeError);
  testDataViewMethodsUpToSize(lengthTracking, 1);
  assertAllDataViewMethodsThrow(lengthTracking, 2, RangeError);

  // Shrink to zero.
  rab.resize(0);
  assertAllDataViewMethodsThrow(fixedLength, 0, TypeError);
  assertAllDataViewMethodsThrow(fixedLengthWithOffset, 0, TypeError);
  assertAllDataViewMethodsThrow(lengthTrackingWithOffset, 0, TypeError);
  testDataViewMethodsUpToSize(lengthTracking, 0);
  assertAllDataViewMethodsThrow(lengthTracking, 1, RangeError);

  // Grow so that all views are back in-bounds.
  rab.resize(34);
  testDataViewMethodsUpToSize(fixedLength, 32);
  assertAllDataViewMethodsThrow(fixedLength, 33, RangeError);
  testDataViewMethodsUpToSize(fixedLengthWithOffset, 32);
  assertAllDataViewMethodsThrow(fixedLengthWithOffset, 33, RangeError);
  testDataViewMethodsUpToSize(lengthTracking, 34);
  assertAllDataViewMethodsThrow(lengthTracking, 35, RangeError);
  testDataViewMethodsUpToSize(lengthTrackingWithOffset, 34 - 2);
  assertAllDataViewMethodsThrow(lengthTrackingWithOffset, 34 - 2 + 1, RangeError);
})();
(function GetParameterConversionShrinks() {
  {
    var rab = CreateResizableArrayBuffer(640, 1280);
    var fixedLength = new DataView(rab, 0, 64);
    var evil = {
      valueOf: () => {
        rab.resize(10);
        return 0;
      }
    };
    assertThrows(() => {
      fixedLength.getUint8(evil);
    }, TypeError);
  }
  {
    var _rab2 = CreateResizableArrayBuffer(640, 1280);
    var fixedLengthWithOffset = new DataView(_rab2, 2, 64);
    var _evil = {
      valueOf: () => {
        _rab2.resize(10);
        return 0;
      }
    };
    assertThrows(() => {
      fixedLengthWithOffset.getUint8(_evil);
    }, TypeError);
  }
  {
    var _rab3 = CreateResizableArrayBuffer(640, 1280);
    var lengthTracking = new DataView(_rab3);
    var _evil2 = {
      valueOf: () => {
        _rab3.resize(10);
        return 12;
      }
    };
    // The DataView is not out of bounds but the index is.
    assertThrows(() => {
      lengthTracking.getUint8(_evil2);
    }, RangeError);
    assertEquals(0, lengthTracking.getUint8(2));
  }
  {
    var _rab4 = CreateResizableArrayBuffer(640, 1280);
    var lengthTrackingWithOffset = new DataView(_rab4, 2);
    var _evil3 = {
      valueOf: () => {
        _rab4.resize(10);
        return 12;
      }
    };
    // The DataView is not out of bounds but the index is.
    assertThrows(() => {
      lengthTrackingWithOffset.getUint8(_evil3);
    }, RangeError);
    _evil3 = {
      valueOf: () => {
        _rab4.resize(0);
        return 0;
      }
    };
    // Now the DataView is out of bounds.
    assertThrows(() => {
      lengthTrackingWithOffset.getUint8(_evil3);
    }, TypeError);
  }
})();
(function SetParameterConversionShrinks() {
  {
    var rab = CreateResizableArrayBuffer(640, 1280);
    var fixedLength = new DataView(rab, 0, 64);
    var evil = {
      valueOf: () => {
        rab.resize(10);
        return 0;
      }
    };
    assertThrows(() => {
      fixedLength.setUint8(evil, 0);
    }, TypeError);
  }
  {
    var _rab5 = CreateResizableArrayBuffer(640, 1280);
    var fixedLengthWithOffset = new DataView(_rab5, 2, 64);
    var _evil4 = {
      valueOf: () => {
        _rab5.resize(10);
        return 0;
      }
    };
    assertThrows(() => {
      fixedLengthWithOffset.setUint8(_evil4, 0);
    }, TypeError);
  }
  {
    var _rab6 = CreateResizableArrayBuffer(640, 1280);
    var lengthTracking = new DataView(_rab6);
    var _evil5 = {
      valueOf: () => {
        _rab6.resize(10);
        return 12;
      }
    };
    lengthTracking.setUint8(12, 0); // Does not throw.
    // The DataView is not out of bounds but the index is.
    assertThrows(() => {
      lengthTracking.setUint8(_evil5, 0);
    }, RangeError);
    lengthTracking.setUint8(2, 0); // Does not throw.
  }
  {
    var _rab7 = CreateResizableArrayBuffer(640, 1280);
    var lengthTrackingWithOffset = new DataView(_rab7, 2);
    var _evil6 = {
      valueOf: () => {
        _rab7.resize(10);
        return 12;
      }
    };
    lengthTrackingWithOffset.setUint8(12, 0); // Does not throw.
    // The DataView is not out of bounds but the index is.
    assertThrows(() => {
      lengthTrackingWithOffset.setUint8(_evil6, 0);
    }, RangeError);
    _evil6 = {
      valueOf: () => {
        _rab7.resize(0);
        return 0;
      }
    };
    // Now the DataView is out of bounds.
    assertThrows(() => {
      lengthTrackingWithOffset.setUint8(_evil6, 0);
    }, TypeError);
  }

  // The same tests as before, except now the "resizing" parameter is the second
  // one, not the first one.
  {
    var _rab8 = CreateResizableArrayBuffer(640, 1280);
    var _fixedLength = new DataView(_rab8, 0, 64);
    var _evil7 = {
      valueOf: () => {
        _rab8.resize(10);
        return 0;
      }
    };
    assertThrows(() => {
      _fixedLength.setUint8(0, _evil7);
    }, TypeError);
  }
  {
    var _rab9 = CreateResizableArrayBuffer(640, 1280);
    var _fixedLengthWithOffset = new DataView(_rab9, 2, 64);
    var _evil8 = {
      valueOf: () => {
        _rab9.resize(10);
        return 0;
      }
    };
    assertThrows(() => {
      _fixedLengthWithOffset.setUint8(0, _evil8);
    }, TypeError);
  }
  {
    var _rab0 = CreateResizableArrayBuffer(640, 1280);
    var _lengthTracking = new DataView(_rab0);
    var _evil9 = {
      valueOf: () => {
        _rab0.resize(10);
        return 0;
      }
    };
    _lengthTracking.setUint8(12, 0); // Does not throw.
    // The DataView is not out of bounds but the index is.
    assertThrows(() => {
      _lengthTracking.setUint8(12, _evil9);
    }, RangeError);
    _lengthTracking.setUint8(2, 0); // Does not throw.
  }
  {
    var _rab1 = CreateResizableArrayBuffer(640, 1280);
    var _lengthTrackingWithOffset = new DataView(_rab1, 2);
    var _evil0 = {
      valueOf: () => {
        _rab1.resize(10);
        return 0;
      }
    };
    // The DataView is not out of bounds but the index is.
    assertThrows(() => {
      _lengthTrackingWithOffset.setUint8(12, _evil0);
    }, RangeError);
    _evil0 = {
      valueOf: () => {
        _rab1.resize(0);
        return 0;
      }
    };
    // Now the DataView is out of bounds.
    assertThrows(() => {
      _lengthTrackingWithOffset.setUint8(0, _evil0);
    }, TypeError);
  }
})();
(function DataViewsAndRabGsabDataViews() {
  // Internally we differentiate between JSDataView and JSRabGsabDataView. Test
  // that they're indistinguishable externally.
  var ab = new ArrayBuffer(10);
  var rab = new ArrayBuffer(10, {
    maxByteLength: 20
  });
  var dv1 = new DataView(ab);
  var dv2 = new DataView(rab);
  assertEquals(DataView.prototype, dv1.__proto__);
  assertEquals(DataView.prototype, dv2.__proto__);
  assertEquals(DataView, dv1.constructor);
  assertEquals(DataView, dv2.constructor);
  var MyDataView = /*#__PURE__*/function (_DataView) {
    function MyDataView(buffer) {
      _classCallCheck(this, MyDataView);
      return _callSuper(this, MyDataView, [buffer]);
    }
    _inherits(MyDataView, _DataView);
    return _createClass(MyDataView);
  }(/*#__PURE__*/_wrapNativeSuper(DataView));
  var dv3 = new MyDataView(ab);
  var dv4 = new MyDataView(rab);
  assertEquals(MyDataView.prototype, dv3.__proto__);
  assertEquals(MyDataView.prototype, dv4.__proto__);
  assertEquals(MyDataView, dv3.constructor);
  assertEquals(MyDataView, dv4.constructor);
})();

"success";
