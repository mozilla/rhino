// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

res = "";

function logElement(value, key) {
    res += "set[" + key + "] "; 
}

(function TestForEach() {
  res = "a) ";
  var mySet = new Set(['key1', 17]);
  mySet.forEach(logElement);

  assertEquals("a) set[key1] set[17] ", res);
})();

(function TestForEachNoKey() {
  res = "b) ";
  var mySet = new Set(['', undefined, null, 19]);
  mySet.forEach(logElement);

  assertEquals("b) set[] set[undefined] set[null] set[19] ", res);
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

"success";