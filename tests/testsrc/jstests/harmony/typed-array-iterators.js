load("testsrc/assert.js");


var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
    Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
    Float64Array];

function checkIterator(it, expected) {
    for (var e of expected) {
        var next = it.next();
        assertEquals(next.values, e.values);
        assertEquals(next.done, e.done);
    }
}

for (var t = 0; t < types.length; t++) {
    var type = types[t];
       
    var arr = new type([4, 5, 6]);
    checkIterator(arr.entries(), [
        {value: [0, 4], done: false},
        {value: [1, 5], done: false},
        {value: [2, 6], done: false},
        {value: undefined, done: true}
    ]);
    checkIterator(arr.keys(), [
        {value: 0, done: false},
        {value: 1, done: false},
        {value: 2, done: false},
        {value: undefined, done: true}
    ]);
    checkIterator(arr.values(), [
        {value: 4, done: false},
        {value: 5, done: false},
        {value: 6, done: false},
        {value: undefined, done: true}
    ]);
    checkIterator(arr[Symbol.iterator](), [
        {value: 4, done: false},
        {value: 5, done: false},
        {value: 6, done: false},
        {value: undefined, done: true}
    ]);
}
 
"success";