# Tutorial: Embedding Rhino

Embedding Rhino can be done simply with good results. With more effort on the part of the embedder, the objects exposed to scripts can be customized further.

This tutorial leads you through the steps from a simple embedding to more customized, complex embeddings. Fully compilable examples are provided along the way.

The examples live in the rhino/examples directory in the distribution and in mozilla/js/rhino/examples in cvs. This document will link to them using lxr.

## RunScript: A simple embedding
About the simplest embedding of Rhino possible is the RunScript example. All it does it read a script from the command line, execute it, and print a result.

Here's an example use of RunScript from a shell command line:

```
$ java RunScript "Math.cos(Math.PI)"
-1
$ java RunScript "function f(x){return x+1} f(7)"
8
```

Note that you'll have to have both the Rhino classes and the RunScript example class file in the classpath. Let's step through the body of main one line at time.

### Entering a Context
The code

    Context cx = Context.enter();
Creates and enters a Context. A Context stores information about the execution environment of a script.

### Initializing standard objects
The code

    Scriptable scope = cx.initStandardObjects();
Initializes the standard objects (Object, Function, etc.) This must be done before scripts can be executed. The null parameter tells initStandardObjects to create and return a scope object that we use in later calls.

### Collecting the arguments
This code is standard Java and not specific to Rhino. It just collects all the arguments and concatenates them together.

```
String s = "";
for (int i=0; i < args.length; i++) {
    s += args[i];
}
```

### Evaluating a script
The code

    Object result = cx.evaluateString(scope, s, "<cmd>", 1, null);
uses the Context cx to evaluate a string. Evaluation of the script looks up variables in scope, and errors will be reported with the filename <cmd> and line number 1.

### Printing the result
The code

    System.out.println(cx.toString(result));
prints the result of evaluating the script (contained in the variable result). result could be a string, JavaScript object, or other values. The toString method converts any JavaScript value to a string.

### Exiting the Context
The code

```
} finally {
    Context.exit();
}
```
exits the Context. This removes the association between the Context and the current thread and is an essential cleanup action. There should be a call to exit for every call to enter. To make sure that it is called even if an exception is thrown, it is put into the finally block corresponding to the try block starting after Context.enter().

## Expose Java APIs
### Using Java APIs
No additional code in the embedding needed! The JavaScript feature called LiveConnect allows JavaScript programs to interact with Java objects:

```
$ java RunScript "java.lang.System.out.println(3)"
3.0
undefined
```

### Implementing interfaces
Using Rhino, JavaScript objects can implement arbitrary Java interfaces. There's no Java code to write -- it's part of Rhino's LiveConnect implementation. For example, we can see how to implement java.lang.Runnable in a Rhino shell session:

```
js> obj = { run: function() { print("hi"); } }
[object Object]
js> obj.run()
hi
js> r = new java.lang.Runnable(obj);
[object Object]
js> t = new java.lang.Thread(r)
Thread[Thread-0,5,main]
js> t.start()
hi
```

### Adding Java objects
The next example is RunScript2. This is the same as RunScript, but with the addition of two extra lines of code:

```
Object wrappedOut = Context.javaToJS(System.out, scope);
ScriptableObject.putProperty(scope, "out", wrappedOut);
```

These lines add a global variable out that is a JavaScript reflection of the System.out variable:

```
$ java RunScript2 "out.println(42)"
42.0
undefined
```

## Using JavaScript objects from Java
After evaluating a script it's possible to query the scope for variables and functions, extracting values and calling JavaScript functions. This is illustrated in the RunScript3 example. This example adds the ability to print the value of variable x and the result of calling function f. Both x and f are expected to be defined by the evaluated script. For example,

```
$ java RunScript3 "x = 7"
x = 7
f is undefined or not a function.
$ java RunScript3 "function f(a) { return a; }"
x is not defined.
f("my args") = my arg
```

### Using JavaScript variables
To print out the value of x, we add the following code:

```
Object x = scope.get("x", scope);
if (x == Scriptable.NOT_FOUND) {
    System.out.println("x is not defined.");
} else {
    System.out.println("x = " + Context.toString(x));
}
```

### Calling JavaScript functions
To get the function f, call it, and print the result, we add this code:

```
Object fObj = scope.get("f", scope);
if (!(fObj instanceof Function)) {
    System.out.println("f is undefined or not a function.");
} else {
    Object functionArgs[] = { "my arg" };
    Function f = (Function)fObj;
    Object result = f.call(cx, scope, scope, functionArgs);
    String report = "f('my args') = " + Context.toString(result);
    System.out.println(report);
}
```

## JavaScript host objects
### Defining Host Objects
Custom host objects can implement special JavaScript features like dynamic properties.

### Counter example
The Counter example is a simple host object. We'll go through it method by method below.

It's easy to try out new host object classes in the shell using its built-in defineClass function. We'll see how to add it to RunScript later. (Note that because the java -jar option preempts the rest of the classpath, we can't use that and access the Counter class.)

```
$ java -cp "js.jar;examples" org.mozilla.javascript.tools.shell.Main
js> defineClass("Counter")
js> c = new Counter(7)
[object Counter]
js> c.count
7
js> c.count
8
js> c.count
9
js> c.resetCount()
js> c.count
0
```

### Counter's constructors
The zero-argument constructor is used by Rhino runtime to create instances. For the counter example, no initialization work is needed, so the implementation is empty.

    public Counter () { }
The method jsConstructor defines the JavaScript constructor that was called with the expression new Counter(7) in the JavaScript code above.

```
public void jsConstructor(int a) { count
= a; }
```

### Class name
The class name is defined by the getClassName method. This is used to determine the name of the constructor.

```
public String getClassName() { return "Counter";
}
```
### Dynamic properties
Dynamic properties are defined by methods beginning with jsGet_ or jsSet_. The method jsGet_count defines the count property.

```
public int jsGet_count() { return count++;
}
```
The expression c.count in the JavaScript code above results in a call to this method.

### Defining JavaScript "methods"
Methods can be defined using the jsFunction_ prefix. Here we define resetCount for JavaScript.

```
public void jsFunction_resetCount() { count
= 0; }
```
The call c.resetCount() above calls this method.

### Adding Counter to RunScript
Now take a look at the RunScript4 example. It's the same as RunScript except for two additions. The method ScriptableObject.defineClass uses a Java class to define the Counter "class" in the top-level scope:

    ScriptableObject.defineClass(scope, Counter.class);
Now we can reference the Counter object from our script:

```
$ java RunScript4 "c = new Counter(3); c.count;
c.count;"
```
It also creates a new instance of the Counter object from within our Java code, constructing it with the value 7, and assigning it to the top-level variable myCounter:

```
Object[] arg = { new Integer(7) };
Scriptable myCounter = cx.newObject(scope, "Counter", arg);
scope.put("myCounter", scope, myCounter);
```
Now we can reference the myCounter object from our script:

```
$ java RunScript3 'RunScript4 'myCounter.count; myCounter.count'
8
```

### Contributors
Contributors to this page: Sheppy, hannesw, kscarfone