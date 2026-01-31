/*
Basic test for several LiveConnect features
*/

let l = new java.util.ArrayList()

// method
assertEquals(0, l.size())
// beaning (.isEmpty())
assertTrue(l.empty)

// method overload
l.add("0")
l.add(0, "1")
assertEquals(2, l.size())
assertEquals("1", l.get(0) + '')

let arr = java.lang.reflect.Array.newInstance(java.lang.Byte, 10)

// field
assertEquals(10, arr.length)

"success"