// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

res = "";

function logElement(value, key, m) {
    res += "map[" + key + "] = '" + value + "' "; 
}

(function TestForEach() {
  res = "a) ";
  var myMap = new Map([['key1', 'value1'], ['key2', 17]]);
  myMap.forEach(logElement);

  assertEquals("a) map[key1] = 'value1' map[key2] = '17' ", res);
})();

(function TestForEachNoKey() {
  res = "b) ";
  var myMap = new Map([['', 'value1'], [, 17], [undefined, 18], [null, 19]]);
  myMap.forEach(logElement);

  assertEquals("b) map[] = 'value1' map[undefined] = '18' map[null] = '19' ", res);
})();

(function TestForEachNoValue() {
  res = "c) ";
  var myMap = new Map([['key1', ''], ['key2',], ['key3', undefined], ['key4', null]]);
  myMap.forEach(logElement);

  assertEquals("c) map[key1] = '' map[key2] = 'undefined' map[key3] = 'undefined' map[key4] = 'null' ", res);
})();


function key() {
}

(function TestUseFunctAsKey() {
  var myMap = new Map();
  myMap.set(key, 'foo');

  assertEquals(myMap.get(key), 'foo');
})();

"success";