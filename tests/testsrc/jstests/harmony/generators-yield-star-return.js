// Test for yield* with iterator.return() method

load("testsrc/assert.js");

// Test that yield* properly delegates to the iterable's return() method
// and handles the case when return() returns null
function testYieldStarWithNullReturn() {
    var returnGets = 0;
    var iterable = {
        next: function() {
            return {value: 1, done: false};
        },
        get return() {
            returnGets += 1;
            return null;
        },
    };

    iterable[Symbol.iterator] = function() {
        return iterable;
    };

    function* generator() {
        yield* iterable;
    }

    var iterator = generator();
    iterator.next();

    var result = iterator.return(2);
    assertEquals(2, result.value);
    assertEquals(true, result.done);
    assertEquals(1, returnGets);
}

// Test that yield* properly delegates to the iterable's return() method
// and handles the case when return() returns undefined
function testYieldStarWithUndefinedReturn() {
    var returnCalled = false;
    var iterable = {
        next: function() {
            return {value: 1, done: false};
        },
        return: function() {
            returnCalled = true;
            return undefined;
        },
    };

    iterable[Symbol.iterator] = function() {
        return iterable;
    };

    function* generator() {
        yield* iterable;
    }

    var iterator = generator();
    iterator.next();

    var result = iterator.return(42);
    assertEquals(42, result.value);
    assertEquals(true, result.done);
    assertEquals(true, returnCalled);
}

// Test that yield* properly delegates when iterable doesn't have return()
function testYieldStarWithoutReturn() {
    var iterable = {
        next: function() {
            return {value: 1, done: false};
        },
    };

    iterable[Symbol.iterator] = function() {
        return iterable;
    };

    function* generator() {
        yield* iterable;
    }

    var iterator = generator();
    iterator.next();

    var result = iterator.return(99);
    assertEquals(99, result.value);
    assertEquals(true, result.done);
}

// Test that yield* properly delegates when iterable's return() returns a valid iterator result
function testYieldStarWithValidReturn() {
    var returnCalled = false;
    var iterable = {
        next: function() {
            return {value: 1, done: false};
        },
        return: function(value) {
            returnCalled = true;
            return {value: value + 10, done: true};
        },
    };

    iterable[Symbol.iterator] = function() {
        return iterable;
    };

    function* generator() {
        yield* iterable;
    }

    var iterator = generator();
    iterator.next();

    var result = iterator.return(5);
    assertEquals(15, result.value);
    assertEquals(true, result.done);
    assertEquals(true, returnCalled);
}

testYieldStarWithNullReturn();
testYieldStarWithUndefinedReturn();
testYieldStarWithoutReturn();
testYieldStarWithValidReturn();

"success";
