# Performance Hints
## var Statements
Use var statements when possible. Not only is it good programming practice, it can speed up your code by allowing the compiler to generate special code to access the variables. For example, you could rewrite

```
function sum(a) {
    result = 0;
    for (i=0; i < a.length; i++)
        result += a[i];
    return result;
}
```
as

```
function sum(a) {
    var result = 0;
    for (var i=0; i < a.length; i++)
        result += a[i];
    return result;
}
```
This is not equivalent code because the second version does not modify global variables result and i. However, if you don't intend for any other function to access these variables, then storing them globally is probably wrong anyway (what if you called another function that had a loop like the one in sum!).

## Arrays
Use the forms of the Array constructor that specify a size or take a list of initial elements. For example, the code

```
var a = new Array();
for (var i=0; i < n; i++)
    a[i] = i;
```
could be sped up by changing the constructor call to new Array(n). A constructor call like that indicates to the runtime that a Java array should be used for the first n entries of the array. Similarly,

    new Array("a", "b", "c")
or
    ["a", "b", "c"]

will cause a 3-element Java array to be allocated to hold the contents of the JavaScript array.

## eval and new Function
Avoid calling eval when possible. Calls to eval are slow because the script being executed must be compiled. Constructing a new function object can be slow for the same reason, while function expressions are more efficient because the function can be compiled. For example, the code

```
function MyObject(a) {
    this.s = a;
    this.toString = new Function("return this.s");
}
```
could be written more efficiently as

```
function MyObject(a) {
    this.s = a;
    this.toString = function () { return this.s }
}
```
Beginning with Rhino 1.4 Release 2, code passed to eval and new Function will be interpreted rather than compiled to class files.

## with
Using the with statement prevents the compiler from generating code for fast access to local variables. You're probably better off explicitly accessing any properties of the object.

### Contributors
 Contributors to this page: Sheppy, williamr, hannesw