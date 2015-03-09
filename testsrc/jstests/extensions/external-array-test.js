load('testsrc/assert.js');

function checkArray(expectedCount) {
  for (var i = 0; i < testArrayLength; i++) {
    assertEquals(i, testArray[i]);
  }

  var eltCount = 0;
  for (var n in testArray) {
    eltCount++;
  }
  assertEquals(expectedCount, eltCount);
}

assertTrue(testArray != undefined);
assertEquals(testArrayLength, testArray.length);

// Set newly-created array to an array of values
for (var i = 0; i < testArrayLength; i++) {
  testArray[i] = i;
}

checkArray(testArrayLength);

if (!regularArray) {
  // External data means that we can't extend the array
  assertThrows(function() {
    testArray[testArrayLength] = testArrayLength;
  });
  // Also that stuff after the end of the array is "undefined"
  assertEquals(testArray[testArrayLength], undefined);
}

// Should be able to handle non-indexed properties as well, and still be enumerable
testArray.stringField = 'Hello!';
testArray.intField = 42;

for (var i = 0; i < testArrayLength; i++) {
  assertEquals(i, testArray[i]);
}

// Adding non-indexed properties should affect the element count but not "length"
assertEquals('Hello!', testArray.stringField);
assertEquals(42, testArray.intField);
assertEquals(testArrayLength, testArray.length);

var eltCount = 0;
for (var n in testArray) {
  eltCount++;
}
assertEquals(testArrayLength + 2, eltCount);
