load("testsrc/assert.js");

// Test for some corner cases in the EmbeddedSlotMap code

var obj = {};

// We should be able to define and redefine a property with an accessor
Object.defineProperty(obj, 'a',
  {
    configurable:true,
    get:function() {
      return 1;
    },
    set:function(arg){ }});
assertEquals(1, obj.a);

Object.defineProperty(obj, 'a', { value:42 });
assertEquals(42, obj.a);

Object.defineProperty(obj, 'b', {
  configurable: true,
  value:43 });
assertEquals(43, obj.b);

Object.defineProperty(obj, 'b',
  {
    get:function() {
      return 1;
    },
    set:function(arg){ }});
assertEquals(1, obj.b);

obj.c = 44;
assertEquals(44, obj.c);

Object.defineProperty(obj, 'c',
  {
    get:function() {
      return 1;
    },
    set:function(arg){ }});
assertEquals(1, obj.c);

Object.defineProperty(obj, 'd',
  {
    configurable:true,
    get:function() {
      return 1;
    },
    set:function(arg){ }});
assertEquals(1, obj.d);

Object.defineProperty(obj, 'd',
  {
    get:function() {
      return 11;
    },
    set:function(arg){ }});
assertEquals(11, obj.d);

"success";
