// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

res = "";

function logElement(value, key) {
    res += "set[" + key + "] (" + this + ") ";
}

(function TestForEach() {
  res = "a) ";
  var mySet = new Set(['key1', 17]);
  mySet.forEach(logElement);

  assertEquals("a) set[key1] ([object Object]) set[17] ([object Object]) ", res);
})();

(function TestForEachStrict() {
  'use strict'
  res = "a) ";
  var mySet = new Set(['key1', 17]);
  mySet.forEach(logElement);

  assertEquals("a) set[key1] (undefined) set[17] (undefined) ", res);
})();

(function TestForEachNoKey() {
  res = "b) ";
  var mySet = new Set(['', undefined, null, 19]);
  mySet.forEach(logElement);

  assertEquals("b) set[] ([object Object]) set[undefined] ([object Object]) set[null] ([object Object]) set[19] ([object Object]) ", res);
})();

(function TestForEachNoKeyStrict() {
  'use strict'
  res = "b) ";
  var mySet = new Set(['', undefined, null, 19]);
  mySet.forEach(logElement);

  assertEquals("b) set[] (undefined) set[undefined] (undefined) set[null] (undefined) set[19] (undefined) ", res);
})();

(function TestAddConcatenatedStrings() {
  var mySet = new Set(['key1']);
  var mySet2 = new Set(['key2']);
  for(let i = 1; i <= 1; i++) {
    let key = 'key' + i;

    assertEquals(1, mySet.size);
    mySet.add(key)
    assertEquals(1, mySet.size);

    assertEquals(1, mySet2.size);
    mySet2.add(key)
    assertEquals(2, mySet2.size);
  }
})();

(function TestHasConcatenatedStrings() {
  var mySet = new Set(['key1', 'key2']);
  for(let i = 1; i <= 1; i++) {
    let key = 'key' + i;

    assertTrue(mySet.has(key));
    assertTrue(mySet.has('key1'));
  }
})();

(function TestDeleteConcatenatedStrings() {
  var mySet = new Set(['key1', 'key2']);
  for(let i = 1; i <= 1; i++) {
    let key = 'key' + i;

    assertEquals(2, mySet.size);
    mySet.delete(key)
    assertEquals(1, mySet.size);
  }
})();

(function TestSetAddJavaEnums() {
  var mySet = new Set();
  mySet.add(java.nio.file.AccessMode.READ);
  mySet.add(java.nio.file.AccessMode.READ);
  assertEquals(1, mySet.size);
})();

(function TestSymbolSpecies() {
  var symbolSpeciesValue = Set[Symbol.species];
  assertEquals(Set, symbolSpeciesValue);
})();

"success";