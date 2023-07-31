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

(function TestForOfNoKey() {
  res = "d) ";
  var myMap = new Map([['', 'value1'], [, 17], [undefined, 18], [null, 19]]);
  for (var entry of myMap) {
    res += "map[" + entry[0] + "] = '" + entry[1] + "' "; 
  }

  assertEquals("d) map[] = 'value1' map[undefined] = '18' map[null] = '19' ", res);
})();

(function TestForOfNoValue() {
  res = "e) ";
  var myMap = new Map([['key1', ''], ['key2',], ['key3', undefined], ['key4', null]]);
  for (var entry of myMap) {
    res += "map[" + entry[0] + "] = '" + entry[1] + "' "; 
  }

  assertEquals("e) map[key1] = '' map[key2] = 'undefined' map[key3] = 'undefined' map[key4] = 'null' ", res);
})();

(function TestEntriesNoKey() {
  res = "f) ";
  var myMap = new Map([['', 'value1'], [, 17], [undefined, 18], [null, 19]]);
  
  var myIter = myMap.entries();
  var entry = myIter.next();
  res += "map[0] = '" + entry.value + "' "; 
  entry = myIter.next();
  res += "map[1] = '" + entry.value + "' "; 
  var entry = myIter.next();
  res += "map[2] = '" + entry.value + "' "; 
  var entry = myIter.next();
  res += "map[3] = '" + entry.value + "' "; 

  assertEquals("f) map[0] = ',value1' map[1] = ',18' map[2] = ',19' map[3] = 'undefined' ", res);
})();

(function TestEntriesNoValue() {
  res = "g) ";
  var myMap = new Map([['key1', ''], ['key2',], ['key3', undefined], ['key4', null]]);

  var myIter = myMap.entries();
  var entry = myIter.next();
  res += "map[0] = '" + entry.value + "' "; 
  entry = myIter.next();
  res += "map[1] = '" + entry.value + "' "; 
  var entry = myIter.next();
  res += "map[2] = '" + entry.value + "' "; 
  var entry = myIter.next();
  res += "map[3] = '" + entry.value + "' "; 

  assertEquals("g) map[0] = 'key1,' map[1] = 'key2,' map[2] = 'key3,' map[3] = 'key4,' ", res);
})();

(function TestGetConcatenatedStrings() {
  var myMap = new Map([['key1', 'value1'], ['key2', 17]]);
  for(let i = 1; i <= 1; i++) {
    let key = 'key' + i;

    assertEquals("value1", myMap.get(key));
    assertEquals("value1", myMap.get('key1'));
  }
})();

(function TestSetConcatenatedStrings() {
  var myMap = new Map([['key1', 'value1'], ['key2', 17]]);
  for(let i = 1; i <= 1; i++) {
    let key = 'key' + i;

    myMap.set(key, 'value2')
    assertEquals("value2", myMap.get(key));
    assertEquals("value2", myMap.get('key1'));
  }
})();

(function TestHasConcatenatedStrings() {
  var myMap = new Map([['key1', 'value1'], ['key2', 17]]);
  for(let i = 1; i <= 1; i++) {
    let key = 'key' + i;

    assertTrue(myMap.has(key));
    assertTrue(myMap.has('key1'));
  }
})();

(function TestDeleteConcatenatedStrings() {
  var myMap = new Map([['key1', 'value1'], ['key2', 17]]);
  for(let i = 1; i <= 1; i++) {
    let key = 'key' + i;

    assertEquals(2, myMap.size);
    myMap.delete(key)
    assertEquals(1, myMap.size);
  }
})();

(function TestSymbolSpecies() {
  var symbolSpeciesValue = Map[Symbol.species];
  assertEquals(Map, symbolSpeciesValue);
})();

"success";