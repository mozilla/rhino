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
// Copyright 2021 the V8 project authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.'
var MyUint8Array = /*#__PURE__*/function (_Uint8Array) {
  function MyUint8Array() {
    _classCallCheck(this, MyUint8Array);
    return _callSuper(this, MyUint8Array, arguments);
  }
  _inherits(MyUint8Array, _Uint8Array);
  return _createClass(MyUint8Array);
}(/*#__PURE__*/_wrapNativeSuper(Uint8Array));
;
var MyFloat32Array = /*#__PURE__*/function (_Float32Array) {
  function MyFloat32Array() {
    _classCallCheck(this, MyFloat32Array);
    return _callSuper(this, MyFloat32Array, arguments);
  }
  _inherits(MyFloat32Array, _Float32Array);
  return _createClass(MyFloat32Array);
}(/*#__PURE__*/_wrapNativeSuper(Float32Array));
;
var MyBigInt64Array = /*#__PURE__*/function (_BigInt64Array) {
  function MyBigInt64Array() {
    _classCallCheck(this, MyBigInt64Array);
    return _callSuper(this, MyBigInt64Array, arguments);
  }
  _inherits(MyBigInt64Array, _BigInt64Array);
  return _createClass(MyBigInt64Array);
}(/*#__PURE__*/_wrapNativeSuper(BigInt64Array));
;
var builtinCtors = [Uint8Array, Int8Array, Uint16Array, Int16Array, Uint32Array, Int32Array, Float16Array, Float32Array, Float64Array, Uint8ClampedArray, BigUint64Array, BigInt64Array];
var ctors = [].concat(builtinCtors);
var floatCtors = [Float16Array, Float32Array, Float64Array];
var intCtors = [Uint8Array, Int8Array, Uint16Array, Int16Array, Uint32Array, Int32Array, BigUint64Array, BigInt64Array];

// Each element of the following array is [getter, setter, size, isBigInt].
var dataViewAccessorsAndSizes = [[DataView.prototype.getUint8, DataView.prototype.setUint8, 1, false], [DataView.prototype.getInt8, DataView.prototype.setInt8, 1, false], [DataView.prototype.getUint16, DataView.prototype.setUint16, 2, false], [DataView.prototype.getInt16, DataView.prototype.setInt16, 2, false], [DataView.prototype.getInt32, DataView.prototype.setInt32, 4, false], [DataView.prototype.getFloat16, DataView.prototype.setFloat16, 2, false], [DataView.prototype.getFloat32, DataView.prototype.setFloat32, 4, false], [DataView.prototype.getFloat64, DataView.prototype.setFloat64, 8, false], [DataView.prototype.getBigUint64, DataView.prototype.setBigUint64, 8, true], [DataView.prototype.getBigInt64, DataView.prototype.setBigInt64, 8, true]];
function CreateResizableArrayBuffer(byteLength, maxByteLength) {
  return new ArrayBuffer(byteLength, {
    maxByteLength: maxByteLength
  });
}
function CreateGrowableSharedArrayBuffer(byteLength, maxByteLength) {
  return new SharedArrayBuffer(byteLength, {
    maxByteLength: maxByteLength
  });
}
function CreateResizableArrayBufferViaWasm(initial, maximum) {
  var mem = new WebAssembly.Memory({
    initial,
    maximum
  });
  return mem.toResizableBuffer();
}
function CreateGrowableSharedArrayBufferViaWasm(initial, maximum) {
  var mem = new WebAssembly.Memory({
    initial,
    maximum,
    shared: true
  });
  return mem.toResizableBuffer();
}
function IsBigIntTypedArray(ta) {
  return ta instanceof BigInt64Array || ta instanceof BigUint64Array;
}
function AllBigIntMatchedCtorCombinations(test) {
  for (var targetCtor of ctors) {
    for (var sourceCtor of ctors) {
      if (IsBigIntTypedArray(new targetCtor()) != IsBigIntTypedArray(new sourceCtor())) {
        // Can't mix BigInt and non-BigInt types.
        continue;
      }
      test(targetCtor, sourceCtor);
    }
  }
}
function AllBigIntUnmatchedCtorCombinations(test) {
  for (var targetCtor of ctors) {
    for (var sourceCtor of ctors) {
      if (IsBigIntTypedArray(new targetCtor()) == IsBigIntTypedArray(new sourceCtor())) {
        continue;
      }
      test(targetCtor, sourceCtor);
    }
  }
}
function ReadDataFromBuffer(ab, ctor) {
  var result = [];
  var ta = new ctor(ab, 0, ab.byteLength / ctor.BYTES_PER_ELEMENT);
  for (var item of ta) {
    result.push(Number(item));
  }
  return result;
}
function WriteToTypedArray(array, index, value) {
  if (array instanceof BigInt64Array || array instanceof BigUint64Array) {
    array[index] = BigInt(value);
  } else {
    array[index] = value;
  }
}

// Also preserves undefined.
function Convert(item) {
  if (typeof item == 'bigint') {
    return Number(item);
  }
  return item;
}
function ToNumbers(array) {
  var result = [];
  for (var item of array) {
    result.push(Convert(item));
  }
  return result;
}
function TypedArrayEntriesHelper(ta) {
  return ta.entries();
}
function ArrayEntriesHelper(ta) {
  return Array.prototype.entries.call(ta);
}
function ValuesFromTypedArrayEntries(ta) {
  var result = [];
  var expectedKey = 0;
  for (var [key, value] of ta.entries()) {
    assertEquals(expectedKey, key);
    ++expectedKey;
    result.push(Number(value));
  }
  return result;
}
function ValuesFromArrayEntries(ta) {
  var result = [];
  var expectedKey = 0;
  for (var [key, value] of Array.prototype.entries.call(ta)) {
    assertEquals(expectedKey, key);
    ++expectedKey;
    result.push(Number(value));
  }
  return result;
}
function TypedArrayKeysHelper(ta) {
  return ta.keys();
}
function ArrayKeysHelper(ta) {
  return Array.prototype.keys.call(ta);
}
function TypedArrayValuesHelper(ta) {
  return ta.values();
}
function ArrayValuesHelper(ta) {
  return Array.prototype.values.call(ta);
}
function ValuesFromTypedArrayValues(ta) {
  var result = [];
  for (var value of ta.values()) {
    result.push(Number(value));
  }
  return result;
}
function ValuesFromArrayValues(ta) {
  var result = [];
  for (var value of Array.prototype.values.call(ta)) {
    result.push(Number(value));
  }
  return result;
}
function TypedArrayAtHelper(ta, index) {
  var result = ta.at(index);
  return Convert(result);
}
function ArrayAtHelper(ta, index) {
  var result = Array.prototype.at.call(ta, index);
  return Convert(result);
}
function TypedArrayFillHelper(ta, n, start, end) {
  if (ta instanceof BigInt64Array || ta instanceof BigUint64Array) {
    ta.fill(BigInt(n), start, end);
  } else {
    ta.fill(n, start, end);
  }
}
function ArrayFillHelper(ta, n, start, end) {
  if (ta instanceof BigInt64Array || ta instanceof BigUint64Array) {
    Array.prototype.fill.call(ta, BigInt(n), start, end);
  } else {
    Array.prototype.fill.call(ta, n, start, end);
  }
}
function TypedArrayFindHelper(ta, p) {
  return ta.find(p);
}
function ArrayFindHelper(ta, p) {
  return Array.prototype.find.call(ta, p);
}
function TypedArrayFindIndexHelper(ta, p) {
  return ta.findIndex(p);
}
function ArrayFindIndexHelper(ta, p) {
  return Array.prototype.findIndex.call(ta, p);
}
function TypedArrayFindLastHelper(ta, p) {
  return ta.findLast(p);
}
function ArrayFindLastHelper(ta, p) {
  return Array.prototype.findLast.call(ta, p);
}
function TypedArrayFindLastIndexHelper(ta, p) {
  return ta.findLastIndex(p);
}
function ArrayFindLastIndexHelper(ta, p) {
  return Array.prototype.findLastIndex.call(ta, p);
}
function TypedArrayIncludesHelper(array, n, fromIndex) {
  if (typeof n == 'number' && (array instanceof BigInt64Array || array instanceof BigUint64Array)) {
    return array.includes(BigInt(n), fromIndex);
  }
  return array.includes(n, fromIndex);
}
function ArrayIncludesHelper(array, n, fromIndex) {
  if (typeof n == 'number' && (array instanceof BigInt64Array || array instanceof BigUint64Array)) {
    return Array.prototype.includes.call(array, BigInt(n), fromIndex);
  }
  return Array.prototype.includes.call(array, n, fromIndex);
}
function TypedArrayIndexOfHelper(ta, n, fromIndex) {
  if (typeof n == 'number' && (ta instanceof BigInt64Array || ta instanceof BigUint64Array)) {
    if (fromIndex == undefined) {
      // Technically, passing fromIndex here would still result in the correct
      // behavior, since "undefined" gets converted to 0 which is a good
      // "default" index. This is to test the "only 1 argument passed" code
      // path, too.
      return ta.indexOf(BigInt(n));
    }
    return ta.indexOf(BigInt(n), fromIndex);
  }
  if (fromIndex == undefined) {
    return ta.indexOf(n);
  }
  return ta.indexOf(n, fromIndex);
}
function ArrayIndexOfHelper(ta, n, fromIndex) {
  if (typeof n == 'number' && (ta instanceof BigInt64Array || ta instanceof BigUint64Array)) {
    if (fromIndex == undefined) {
      // Technically, passing fromIndex here would still result in the correct
      // behavior, since "undefined" gets converted to 0 which is a good
      // "default" index. This is to test the "only 1 argument passed" code
      // path, too.
      return Array.prototype.indexOf.call(ta, BigInt(n));
    }
    return Array.prototype.indexOf.call(ta, BigInt(n), fromIndex);
  }
  if (fromIndex == undefined) {
    return Array.prototype.indexOf.call(ta, n);
  }
  return Array.prototype.indexOf.call(ta, n, fromIndex);
}
function TypedArrayLastIndexOfHelper(ta, n, fromIndex) {
  if (typeof n == 'number' && (ta instanceof BigInt64Array || ta instanceof BigUint64Array)) {
    if (fromIndex == undefined) {
      // Shouldn't pass fromIndex here, since passing "undefined" is not the
      // same as not passing the parameter at all. "Undefined" will get
      // converted to 0 which is not a good "default" index, since lastIndexOf
      // iterates from the index downwards.
      return ta.lastIndexOf(BigInt(n));
    }
    return ta.lastIndexOf(BigInt(n), fromIndex);
  }
  if (fromIndex == undefined) {
    return ta.lastIndexOf(n);
  }
  return ta.lastIndexOf(n, fromIndex);
}
function ArrayLastIndexOfHelper(ta, n, fromIndex) {
  if (typeof n == 'number' && (ta instanceof BigInt64Array || ta instanceof BigUint64Array)) {
    if (fromIndex == undefined) {
      // Shouldn't pass fromIndex here, since passing "undefined" is not the
      // same as not passing the parameter at all. "Undefined" will get
      // converted to 0 which is not a good "default" index, since lastIndexOf
      // iterates from the index downwards.
      return Array.prototype.lastIndexOf.call(ta, BigInt(n));
    }
    return Array.prototype.lastIndexOf.call(ta, BigInt(n), fromIndex);
  }
  if (fromIndex == undefined) {
    return Array.prototype.lastIndexOf.call(ta, n);
  }
  return Array.prototype.lastIndexOf.call(ta, n, fromIndex);
}
function SetHelper(target, source, offset) {
  if (target instanceof BigInt64Array || target instanceof BigUint64Array) {
    var bigIntSource = [];
    for (s of source) {
      bigIntSource.push(BigInt(s));
    }
    source = bigIntSource;
  }
  if (offset == undefined) {
    return target.set(source);
  }
  return target.set(source, offset);
}
function testDataViewMethodsUpToSize(view, bufferSize) {
  var _loop = function (getter, setter, size) {
    for (var i = 0; i <= bufferSize - size; ++i) {
      if (isBigInt) {
        setter.call(view, i, 3n);
      } else {
        setter.call(view, i, 3);
      }
      assertEquals(3, Number(getter.call(view, i)));
    }
    if (isBigInt) {
      assertThrows(() => setter.call(view, bufferSize - size + 1, 0n), RangeError);
    } else {
      assertThrows(() => setter.call(view, bufferSize - size + 1, 0), RangeError);
    }
    assertThrows(() => getter.call(view, bufferSize - size + 1), RangeError);
  };
  for (var [getter, setter, size, isBigInt] of dataViewAccessorsAndSizes) {
    _loop(getter, setter, size);
  }
}
function assertAllDataViewMethodsThrow(view, index, errorType) {
  var _loop2 = function (getter, setter) {
    if (isBigInt) {
      assertThrows(() => {
        setter.call(view, index, 3n);
      }, errorType);
    } else {
      assertThrows(() => {
        setter.call(view, index, 3);
      }, errorType);
    }
    assertThrows(() => {
      getter.call(view, index);
    }, errorType);
  };
  for (var [getter, setter, size, isBigInt] of dataViewAccessorsAndSizes) {
    _loop2(getter, setter);
  }
}
function ObjectDefinePropertyHelper(ta, index, value) {
  if (ta instanceof BigInt64Array || ta instanceof BigUint64Array) {
    Object.defineProperty(ta, index, {
      value: BigInt(value)
    });
  } else {
    Object.defineProperty(ta, index, {
      value: value
    });
  }
}
function ObjectDefinePropertiesHelper(ta, index, value) {
  var values = {};
  if (ta instanceof BigInt64Array || ta instanceof BigUint64Array) {
    values[index] = {
      value: BigInt(value)
    };
  } else {
    values[index] = {
      value: value
    };
  }
  Object.defineProperties(ta, values);
}
function TestAtomicsOperations(ta, index) {
  var one = IsBigIntTypedArray(ta) ? 1n : 1;
  var two = IsBigIntTypedArray(ta) ? 2n : 2;
  var three = IsBigIntTypedArray(ta) ? 3n : 3;
  Atomics.store(ta, index, one);
  assertEquals(one, Atomics.load(ta, index));
  assertEquals(one, Atomics.exchange(ta, index, two));
  assertEquals(two, Atomics.load(ta, index));
  assertEquals(two, Atomics.compareExchange(ta, index, two, three));
  assertEquals(three, Atomics.load(ta, index));
  assertEquals(three, Atomics.sub(ta, index, two)); // 3 - 2 = 1
  assertEquals(one, Atomics.load(ta, index));
  assertEquals(one, Atomics.add(ta, index, one)); // 1 + 1 = 2
  assertEquals(two, Atomics.load(ta, index));
  assertEquals(two, Atomics.or(ta, index, one)); // 2 | 1 = 3
  assertEquals(three, Atomics.load(ta, index));
  assertEquals(three, Atomics.xor(ta, index, one)); // 3 ^ 1 = 2
  assertEquals(two, Atomics.load(ta, index));
  assertEquals(two, Atomics.and(ta, index, three)); // 2 & 3 = 2
  assertEquals(two, Atomics.load(ta, index));
}
function AssertAtomicsOperationsThrow(ta, index, error) {
  var one = IsBigIntTypedArray(ta) ? 1n : 1;
  assertThrows(() => {
    Atomics.store(ta, index, one);
  }, error);
  assertThrows(() => {
    Atomics.load(ta, index);
  }, error);
  assertThrows(() => {
    Atomics.exchange(ta, index, one);
  }, error);
  assertThrows(() => {
    Atomics.compareExchange(ta, index, one, one);
  }, error);
  assertThrows(() => {
    Atomics.add(ta, index, one);
  }, error);
  assertThrows(() => {
    Atomics.sub(ta, index, one);
  }, error);
  assertThrows(() => {
    Atomics.and(ta, index, one);
  }, error);
  assertThrows(() => {
    Atomics.or(ta, index, one);
  }, error);
  assertThrows(() => {
    Atomics.xor(ta, index, one);
  }, error);
}
var TypedArrayCopyWithinHelper = function (ta) {
  for (var _len = arguments.length, rest = new Array(_len > 1 ? _len - 1 : 0), _key = 1; _key < _len; _key++) {
    rest[_key - 1] = arguments[_key];
  }
  ta.copyWithin.apply(ta, rest);
};
var ArrayCopyWithinHelper = function (ta) {
  var _Array$prototype$copy;
  for (var _len2 = arguments.length, rest = new Array(_len2 > 1 ? _len2 - 1 : 0), _key2 = 1; _key2 < _len2; _key2++) {
    rest[_key2 - 1] = arguments[_key2];
  }
  (_Array$prototype$copy = Array.prototype.copyWithin).call.apply(_Array$prototype$copy, [ta].concat(rest));
};
var TypedArrayReverseHelper = ta => {
  ta.reverse();
};
var ArrayReverseHelper = ta => {
  Array.prototype.reverse.call(ta);
};
var TypedArraySortHelper = function (ta) {
  for (var _len3 = arguments.length, rest = new Array(_len3 > 1 ? _len3 - 1 : 0), _key3 = 1; _key3 < _len3; _key3++) {
    rest[_key3 - 1] = arguments[_key3];
  }
  ta.sort.apply(ta, rest);
};
var ArraySortHelper = function (ta) {
  var _Array$prototype$sort;
  for (var _len4 = arguments.length, rest = new Array(_len4 > 1 ? _len4 - 1 : 0), _key4 = 1; _key4 < _len4; _key4++) {
    rest[_key4 - 1] = arguments[_key4];
  }
  (_Array$prototype$sort = Array.prototype.sort).call.apply(_Array$prototype$sort, [ta].concat(rest));
};
var TypedArraySliceHelper = function (ta) {
  for (var _len5 = arguments.length, rest = new Array(_len5 > 1 ? _len5 - 1 : 0), _key5 = 1; _key5 < _len5; _key5++) {
    rest[_key5 - 1] = arguments[_key5];
  }
  return ta.slice.apply(ta, rest);
};
var ArraySliceHelper = function (ta) {
  var _Array$prototype$slic;
  for (var _len6 = arguments.length, rest = new Array(_len6 > 1 ? _len6 - 1 : 0), _key6 = 1; _key6 < _len6; _key6++) {
    rest[_key6 - 1] = arguments[_key6];
  }
  return (_Array$prototype$slic = Array.prototype.slice).call.apply(_Array$prototype$slic, [ta].concat(rest));
};
var ArrayFlatHelper = function (ta) {
  var _Array$prototype$flat;
  for (var _len7 = arguments.length, rest = new Array(_len7 > 1 ? _len7 - 1 : 0), _key7 = 1; _key7 < _len7; _key7++) {
    rest[_key7 - 1] = arguments[_key7];
  }
  return (_Array$prototype$flat = Array.prototype.flat).call.apply(_Array$prototype$flat, [ta].concat(rest));
};
var ArrayFlatMapHelper = function (ta) {
  var _Array$prototype$flat2;
  for (var _len8 = arguments.length, rest = new Array(_len8 > 1 ? _len8 - 1 : 0), _key8 = 1; _key8 < _len8; _key8++) {
    rest[_key8 - 1] = arguments[_key8];
  }
  return (_Array$prototype$flat2 = Array.prototype.flatMap).call.apply(_Array$prototype$flat2, [ta].concat(rest));
};
var TypedArrayJoinHelper = function (ta) {
  for (var _len9 = arguments.length, rest = new Array(_len9 > 1 ? _len9 - 1 : 0), _key9 = 1; _key9 < _len9; _key9++) {
    rest[_key9 - 1] = arguments[_key9];
  }
  return ta.join.apply(ta, rest);
};
var ArrayJoinHelper = function (ta) {
  var _Array$prototype$join;
  for (var _len0 = arguments.length, rest = new Array(_len0 > 1 ? _len0 - 1 : 0), _key0 = 1; _key0 < _len0; _key0++) {
    rest[_key0 - 1] = arguments[_key0];
  }
  return (_Array$prototype$join = Array.prototype.join).call.apply(_Array$prototype$join, [ta].concat(rest));
};
var TypedArrayToLocaleStringHelper = function (ta) {
  for (var _len1 = arguments.length, rest = new Array(_len1 > 1 ? _len1 - 1 : 0), _key1 = 1; _key1 < _len1; _key1++) {
    rest[_key1 - 1] = arguments[_key1];
  }
  return ta.toLocaleString.apply(ta, rest);
};
var ArrayToLocaleStringHelper = function (ta) {
  var _Array$prototype$toLo;
  for (var _len10 = arguments.length, rest = new Array(_len10 > 1 ? _len10 - 1 : 0), _key10 = 1; _key10 < _len10; _key10++) {
    rest[_key10 - 1] = arguments[_key10];
  }
  return (_Array$prototype$toLo = Array.prototype.toLocaleString).call.apply(_Array$prototype$toLo, [ta].concat(rest));
};
var TypedArrayForEachHelper = function (ta) {
  for (var _len11 = arguments.length, rest = new Array(_len11 > 1 ? _len11 - 1 : 0), _key11 = 1; _key11 < _len11; _key11++) {
    rest[_key11 - 1] = arguments[_key11];
  }
  return ta.forEach.apply(ta, rest);
};
var ArrayForEachHelper = function (ta) {
  var _Array$prototype$forE;
  for (var _len12 = arguments.length, rest = new Array(_len12 > 1 ? _len12 - 1 : 0), _key12 = 1; _key12 < _len12; _key12++) {
    rest[_key12 - 1] = arguments[_key12];
  }
  return (_Array$prototype$forE = Array.prototype.forEach).call.apply(_Array$prototype$forE, [ta].concat(rest));
};
var TypedArrayReduceHelper = function (ta) {
  for (var _len13 = arguments.length, rest = new Array(_len13 > 1 ? _len13 - 1 : 0), _key13 = 1; _key13 < _len13; _key13++) {
    rest[_key13 - 1] = arguments[_key13];
  }
  return ta.reduce.apply(ta, rest);
};
var ArrayReduceHelper = function (ta) {
  var _Array$prototype$redu;
  for (var _len14 = arguments.length, rest = new Array(_len14 > 1 ? _len14 - 1 : 0), _key14 = 1; _key14 < _len14; _key14++) {
    rest[_key14 - 1] = arguments[_key14];
  }
  return (_Array$prototype$redu = Array.prototype.reduce).call.apply(_Array$prototype$redu, [ta].concat(rest));
};
var TypedArrayReduceRightHelper = function (ta) {
  for (var _len15 = arguments.length, rest = new Array(_len15 > 1 ? _len15 - 1 : 0), _key15 = 1; _key15 < _len15; _key15++) {
    rest[_key15 - 1] = arguments[_key15];
  }
  return ta.reduceRight.apply(ta, rest);
};
var ArrayReduceRightHelper = function (ta) {
  var _Array$prototype$redu2;
  for (var _len16 = arguments.length, rest = new Array(_len16 > 1 ? _len16 - 1 : 0), _key16 = 1; _key16 < _len16; _key16++) {
    rest[_key16 - 1] = arguments[_key16];
  }
  return (_Array$prototype$redu2 = Array.prototype.reduceRight).call.apply(_Array$prototype$redu2, [ta].concat(rest));
};
var TypedArrayFilterHelper = function (ta) {
  for (var _len17 = arguments.length, rest = new Array(_len17 > 1 ? _len17 - 1 : 0), _key17 = 1; _key17 < _len17; _key17++) {
    rest[_key17 - 1] = arguments[_key17];
  }
  return ta.filter.apply(ta, rest);
};
var ArrayFilterHelper = function (ta) {
  var _Array$prototype$filt;
  for (var _len18 = arguments.length, rest = new Array(_len18 > 1 ? _len18 - 1 : 0), _key18 = 1; _key18 < _len18; _key18++) {
    rest[_key18 - 1] = arguments[_key18];
  }
  return (_Array$prototype$filt = Array.prototype.filter).call.apply(_Array$prototype$filt, [ta].concat(rest));
};
var TypedArrayMapHelper = function (ta) {
  for (var _len19 = arguments.length, rest = new Array(_len19 > 1 ? _len19 - 1 : 0), _key19 = 1; _key19 < _len19; _key19++) {
    rest[_key19 - 1] = arguments[_key19];
  }
  return ta.map.apply(ta, rest);
};
var ArrayMapHelper = function (ta) {
  var _Array$prototype$map;
  for (var _len20 = arguments.length, rest = new Array(_len20 > 1 ? _len20 - 1 : 0), _key20 = 1; _key20 < _len20; _key20++) {
    rest[_key20 - 1] = arguments[_key20];
  }
  return (_Array$prototype$map = Array.prototype.map).call.apply(_Array$prototype$map, [ta].concat(rest));
};
var TypedArrayEveryHelper = function (ta) {
  for (var _len21 = arguments.length, rest = new Array(_len21 > 1 ? _len21 - 1 : 0), _key21 = 1; _key21 < _len21; _key21++) {
    rest[_key21 - 1] = arguments[_key21];
  }
  return ta.every.apply(ta, rest);
};
var ArrayEveryHelper = function (ta) {
  var _Array$prototype$ever;
  for (var _len22 = arguments.length, rest = new Array(_len22 > 1 ? _len22 - 1 : 0), _key22 = 1; _key22 < _len22; _key22++) {
    rest[_key22 - 1] = arguments[_key22];
  }
  return (_Array$prototype$ever = Array.prototype.every).call.apply(_Array$prototype$ever, [ta].concat(rest));
};
var TypedArraySomeHelper = function (ta) {
  for (var _len23 = arguments.length, rest = new Array(_len23 > 1 ? _len23 - 1 : 0), _key23 = 1; _key23 < _len23; _key23++) {
    rest[_key23 - 1] = arguments[_key23];
  }
  return ta.some.apply(ta, rest);
};
var ArraySomeHelper = function (ta) {
  var _Array$prototype$some;
  for (var _len24 = arguments.length, rest = new Array(_len24 > 1 ? _len24 - 1 : 0), _key24 = 1; _key24 < _len24; _key24++) {
    rest[_key24 - 1] = arguments[_key24];
  }
  return (_Array$prototype$some = Array.prototype.some).call.apply(_Array$prototype$some, [ta].concat(rest));
};

