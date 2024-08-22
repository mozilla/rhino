/*
 * This script tests the ability to change prototypes of objects in various ways,
 * including for some native objects, which handle them differently.
 */

load("testsrc/assert.js");

TestObj = function() {
}

TestObj.prototype.func = function() {
  return 'foo';
}

function replacePropertyNormally(obj, name) {
   var to = new obj();
   var origFunc = obj.prototype[name];
   var origValue = to[name]();

   // Re-define the prototype property with one that returns a fixed value
   obj.prototype[name] = function() {
     return 'bar';
   }
   assertEquals('bar', to[name]());

   // Do it again
    obj.prototype[name] = function() {
        return 'baz';
      }
      assertEquals('baz', to[name]());

   // Set the prototype property back to the original value
   // and ensure that it still works.
   obj.prototype[name] = origFunc;
   assertEquals(origValue, to[name]());
}

// Same as above but using a "getter" function.
function replacePropertyWithGetter(obj, name) {
   var to = new obj();
   var origFunc = obj.prototype[name];
   var origValue = to[name]();

   // Re-define the prototype property with one that returns a fixed value
   Object.defineProperty(obj.prototype, name, {
      configurable: true,
      get: function() {
        return function() {
          return 'bar';
        }
      }
   });
   assertEquals('bar', to[name]());

   // Again
   Object.defineProperty(obj.prototype, name, {
       configurable: true,
       get: function() {
         return function() {
           return 'baz';
         }
       }
    });
    assertEquals('baz', to[name]());

   // Set the prototype property back to the original value
   // and ensure that it still works.
   Object.defineProperty(obj.prototype, name, {
     value: origFunc
   });
   assertEquals(origValue, to[name]());
}

// Verify that the above works for regular objects as well as native objects
// that inherit from IdScriptableObject.
replacePropertyNormally(TestObj, 'func');
replacePropertyWithGetter(TestObj, 'func');
replacePropertyNormally(String, 'substring');
replacePropertyWithGetter(String, 'substring');

"success";
