# Scripting Java

This article shows how to use Rhino to reach beyond JavaScript into Java. Scripting Java has many uses. It allows us to write powerful scripts quickly by making use of the many Java libraries available. We can test Java classes by writing scripts. We can also aid our Java development by using scripting for exploratory programming. Exploratory programming is the process of learning about what a library or API can do by writing quick programs that use it. As we will see, scripting makes this process easier.

Note that the ECMA standard doesn't cover communication with Java (or with any external object system for that matter). All the functionality covered in this chapter should thus be considered an extension.

## Accessing Java Packages and Classes
Every piece of Java code is part of a class. Every Java class is part of a package. In JavaScript, however, scripts exist outside of any package hierarchy. How then, do we access classes in Java packages?

Rhino defines a top-level variable named Packages. The properties of the Packages variable are all the top-level Java packages, such as java and com. For example, we can access the value of the java package:

```
js> Packages.java
[JavaPackage java]
```
As a handy shortcut, Rhino defines a top-level variable java that is equivalent to Packages.java. So the previous example could be even shorter:

```
js> java
[JavaPackage java]
```
We can access Java classes simply by stepping down the package hierarchy:

```
js> java.io.File
[JavaClass java.io.File]
```
If your scripts access a lot of different Java classes, it can get awkward to use the full package name of the class every time. Rhino provides a top-level function importPackage that serves the same purpose as Java's import declaration. For example, we could import all of the classes in the java.io package and access class java.io.File using just the name File:

```
js> importPackage(java.io)
js> File
[JavaClass java.io.File]
```
Here importPackage(java.io) makes all the classes in the java.io package (such as File) available at the top level. It's equivalent in effect to the Java declaration import java.io.*;.

It's important to note that Java imports java.lang.* implicitly, while Rhino does not. The reason is that JavaScript has its own top-level objects Boolean, Math, Number, Object, and String that are different from the classes by those names defined in the java.lang package. Because of this conflict, it's a good idea not to use importPackage on the java.lang package.

One thing to be careful of is Rhino's handling of errors in specifying Java package or class names. If java.MyClass is accessed, Rhino attempts to load a class named java.MyClass. If that load fails, it assumes that java.MyClass is a package name, and no error is reported:

```
js> java.MyClass
[JavaPackage java.MyClass]
```
Only if you attempt to use this object as a class will an error be reported.

## External Packages and Classes
External packages and classes can also be used as in Rhino. Make sure your .jar or .class file is on you classpath then you may import them into your JavaScript application. These packages are likely not in the java package, so you'll need to prefix the package name with "Packages." For example, to import the org.mozilla.javascript package you could use importPackage() as follows:

```
$ java org.mozilla.javascript.tools.shell.Main
js> importPackage(Packages.org.mozilla.javascript);
js> Context.currentContext;
org.mozilla.javascript.Context@bb6ab6
```
Occasionally, you will see examples that use the fully qualified name of the package instead of importing using the importPackage() method. This is also acceptable, it just takes more typing. Using a fully qualified name, the above example would look as follows:

```
$ java org.mozilla.javascript.tools.shell.Main
js> jsPackage = Packages.org.mozilla.javascript;
[JavaPackage org.mozilla.javascript]
js> jsPackage.Context.currentContext;
org.mozilla.javascript.Context@bb6ab6
```
Alternatively, if you want to import just one class from a package you can do so using the importClass() method. The above examples could be expressed as follows:

```
$ java org.mozilla.javascript.tools.shell.Main
js> importClass(Packages.org.mozilla.javascript.Context);
js>  Context.currentContext;
org.mozilla.javascript.Context@bb6ab6
```
## Working with Java
Now that we can access Java classes, the next logical step is to create an object. This works just as in Java, with the use of the new operator:

```
js> new java.util.Date()
Thu Jan 24 16:18:17 EST 2002
```
If we store the new object in a JavaScript variable, we can then call methods on it:

```
js> f = new java.io.File("test.txt")
test.txt
js> f.exists()
true
js> f.getName()
test.txt
```
Static methods and fields can be accessed from the class object itself:

```
js> java.lang.Math.PI
3.141592653589793
js> java.lang.Math.cos(0)
1
```
In JavaScript, unlike Java, the method by itself is an object and can be evaluated as well as being called. If we just view the method object by itself we can see the various overloaded forms of the method:

```
js> f.listFiles
function listFiles() {/*
java.io.File[] listFiles()
java.io.File[] listFiles(java.io.FilenameFilter)
java.io.File[] listFiles(java.io.FileFilter)
*/}
````
This output shows that the File class defines three overloaded methods listFiles: one that takes no arguments, another with a FilenameFilter argument, and a third with a FileFilter argument. All the methods return an array of File objects. Being able to view the parameters and return type of Java methods is particularly useful in exploratory programming where we might be investigating a method and are unsure of the parameter or return types.

Another useful feature for exploratory programming is the ability to see all the methods and fields defined for an object. Using the JavaScript for..in construct, we can print out all these values:

```
js> for (i in f) { print(i) }
exists
parentFile
mkdir
toString
wait
[44 others]
```
Note that not only the methods of the File class are listed, but also the methods inherited from the base class java.lang.Object (like wait). This makes it easier to work with objects in deeply nested inheritance hierarchies since you can see all the methods that are available for that object.

Rhino provides another convenience by allowing properties of JavaBeans to be accessed directly by their property names. A JavaBean property foo is defined by the methods getFoo and setFoo. Additionally, a boolean property of the same name can be defined by an isFoo method. For example, the following code actually calls the File object's getName and isDirectory methods.

```
js> f.name
test.txt
js> f.directory
false
```
## Calling Overloaded Methods
The process of choosing a method to call based upon the types of the arguments is called overload resolution. In Java, overload resolution is performed at compile time, while in Rhino it occurs at runtime. This difference is inevitable given JavaScript's use of dynamic typing as was discussed in Chapter 2: since the type of a variable is not known until runtime, only then can overload resolution occur.

As an example, consider the following Java class that defines a number of overloaded methods and calls them.

```
public class Overload {

    public String f(Object o) { return "f(Object)"; }
    public String f(String s) { return "f(String)"; }
    public String f(int i)    { return "f(int)"; }

    public String g(String s, int i) { return "g(String,int)"; }
    public String g(int i, String s) { return "g(int,String)"; }

    public static void main(String[] args) {
        Overload o = new Overload();
        Object[] a = new Object[] { new Integer(3), "hi", Overload.class };
        for (int i = 0; i != a.length; ++i)
            System.out.println(o.f(a[i]));
    }
}
```
When we compile and execute the program, it produces the output

```
f(Object)
f(Object)
f(Object)
```
However, if we write a similar script

```
var o = new Packages.Overload();
var a = [ 3, "hi", Packages.Overload ];
for (var i = 0; i != a.length; ++i)
    print(o.f(a[i]));
```
and execute it, we get the output

```
f(int)
f(String)
f(Object)
```
Because Rhino selects an overloaded method at runtime, it calls the more specific type that matches the argument. Meanwhile Java selects the overloaded method purely on the type of the argument at compile time.

Although this has the benefit of selecting a method that may be a better match for each call, it does have an impact on performance since more work is done at each call. In practice this performance cost hasn't been noticeable in real applications.

Because overload resolution occurs at runtime, it can fail at runtime. For example, if we call Overload's method g with two integers we get an error because neither form of the method is closer to the argument types than the other:

```
js> o.g(3,4)
js:"<stdin>", line 2: The choice of Java method Overload.g
matching JavaScript argument types (number,number) is ambiguous;
candidate methods are: 
class java.lang.String g(java.lang.String,int)
class java.lang.String g(int,java.lang.String)
```
See Java Method Overloading and LiveConnect 3 for a more precise definition of overloading semantics.

## Implementing Java Interfaces
Now that we can access Java classes, create Java objects, and access fields, methods, and properties of those objects, we have a great deal of power at our fingertips. However, there are a few instances where that is not enough: many APIs in Java work by providing interfaces that clients must implement. One example of this is the Thread class: its constructor takes a Runnable that contains a single method run that will be called when the new thread is started.

To address this need, Rhino provides the ability to create new Java objects that implement interfaces. First we must define a JavaScript object with function properties whose names match the methods required by the Java interface. To implement a Runnable, we need only define a single method run with no parameters. If you remember from Chapter 3, it is possible to define a JavaScript object with the {propertyName: value} notation. We can use that syntax here in combination with a function expression to define a JavaScript object with a run method:

```
js> obj = { run: function () { print("\nrunning"); } }
[object Object]
js> obj.run()

running
```
Now we can create an object implementing the Runnable interface by constructing a Runnable:

```
js> r = new java.lang.Runnable(obj);
[object JavaObject]
```
In Java it is not possible to use the new operator on an interface because there is no implementation available. Here Rhino gets the implementation from the JavaScript object obj. Now that we have an object implementing Runnable, we can create a Thread and run it. The function we defined for run will be called on a new thread.

```
js> t = new java.lang.Thread(r)
Thread[Thread-2,5,main]
js> t.start()
js>

running\
```
The final js prompt and the output from the new thread may appear in either order, depending on thread scheduling.

Behind the scenes, Rhino generates the bytecode for a new Java class that implements Runnable and forwards all calls to its run method over to an associated JavaScript object. The object that implements this class is called a Java adapter. Because the forwarding to JavaScript occurs at runtime, it is possible to delay defining the methods implementing an interface until they are called. While omitting a required method is bad practice for programming in the large, it's useful for small scripts and for exploratory programming.

## The JavaAdapter Constructor
In the previous section we created Java adapters using the new operator with Java interfaces. This approach has its limitations: it's not possible to implement multiple interfaces, nor can we extend non-abstract classes. For these reasons there is a JavaAdapter constructor.

The syntax of the JavaAdapter constructor is:

    new JavaAdapter(javaIntfOrClass, [javaIntf, ..., javaIntf,] javascriptObject)
Here javaIntfOrClass is an interface to implement or a class to extend and javaIntf are aditional interfaces to implement. The javascriptObject is the JavaScript object containing the methods that will be called from the Java adapter.

In practice there's little need to call the JavaAdapter constructor directly. Most of the time the previous syntaxes using the new operator will be sufficient.

## JavaScript Functions as Java Interfaces
Often we need to implement an interface with only one method, like in the previous Runnable example or when providing various event listener implementations. To facilitate this Rhino allows to pass JavaScript function when such interface is expected. The function is called as the implementation of interface method.

Here is the simplified Runnable example:

```
js> t = java.lang.Thread(function () { print("\nrunning"); });
Thread[Thread-0,5,main]
js> t.start()
js> 
running
```
Rhino also allows use of JavaScript functions as implementations of Java interfaces with more than one method if all the methods have the same signature. When calling the function, Rhino passes the method's name as the additional argument. Function can use it to distinguish on behalf of which method it was called:

```
js> var frame = new Packages.javax.swing.JFrame();
js> frame.addWindowListener(function(event, methodName) {
	if (methodName == "windowClosing") {     
            print("Calling System.exit()..."); java.lang.System.exit(0);
	}
    });
js> frame.setSize(100, 100);
js> frame.visible = true;
true
js> Calling System.exit()...
```
## Creating Java Arrays
Rhino provides no special syntax for creating Java arrays. You must use the java.lang.reflect.Array class for this purpose. To create an array of five Java strings you would make the following call:

```
js> a = java.lang.reflect.Array.newInstance(java.lang.String, 5);
[Ljava.lang.String;@7ffe01
```
To create an array of primitive types, we must use the special TYPE field defined in the associated object class in the java.lang package. For example, to create an array of bytes, we must use the special field java.lang.Byte.TYPE:

```
js> a = java.lang.reflect.Array.newInstance(java.lang.Character.TYPE, 2);
[C@7a84e4
```
The resulting value can then be used anywhere a Java array of that type is allowed.

```
js> a[0] = 104
104
js> a[1] = 105
105
js> new java.lang.String(a)
hi
```
## Java Strings and JavaScript Strings
It's important to keep in mind that Java strings and JavaScript strings are not the same. Java strings are instances of the type java.lang.String and have all the methods defined by that class. JavaScript strings have methods defined by String.prototype. The most common stumbling block is length, which is a method of Java strings and a dynamic property of JavaScript strings:

```
js> javaString = new java.lang.String("Java")
Java
js> jsString = "JavaScript"
JavaScript
js> javaString.length()
4
js> jsString.length
10
```
Rhino provides some help in reducing the differences between the two types. First, you can pass a JavaScript string to a Java method that requires a Java string and Rhino will perform the conversion. We actually saw this feature in action on the call to the java.lang.String constructor in the preceding example.

Rhino also makes the JavaScript methods available to Java strings if the java.lang.String class doesn't already define them. For example:

```
js> javaString.match(/a.*/)
ava
```
## JavaImporter Constructor
JavaImporter is a new global constructor that allows to omit explicit package names when scripting Java:

```
var SwingGui = JavaImporter(Packages.javax.swing,
                            Packages.javax.swing.event,
                            Packages.javax.swing.border,
                            java.awt.event,
                            java.awt.Point,
                            java.awt.Rectangle,
                            java.awt.Dimension);
...

with (SwingGui) {
    var mybutton = new JButton(test);
    var mypoint = new Point(10, 10);
    var myframe = new JFrame();
...
}
```
Previously such functionality was available only to embeddings that used org.mozilla.javascript.ImporterTopLevel class as the top level scope. The class provides additional importPackage() and importClass() global functions for scripts but their extensive usage has tendency to pollute the global name space with names of Java classes and prevents loaded classes from garbage collection.

See Bugzilla 245882 for details.

## Java Exceptions
Exceptions thrown by Java methods can be caught by JavaScript code using try...catch statement. Rhino wraps Java exceptions into error objects with the following properties:

* javaException: the original exception thrown by the Java method
* rhinoException: the exception wrapped by the Rhino runtime
The instanceof operator can be used to query the type of an exception:

```
try { 
    java.lang.Class.forName("NonExistingClass"); 
} catch (e) {
    if (e.javaException instanceof java.lang.ClassNotFoundException) {
       print("Class not found");
    }
}
```
Rhino also supports an extension to the try...catch statement that allows to define conditional catching of exceptions:

```
function classForName(name) {
    try {
        return java.lang.Class.forName(name);
    } catch (e if e.javaException instanceof java.lang.ClassNotFoundException) {
        print("Class " + name + " not found");
    } catch (e if e.javaException instanceof java.lang.NullPointerException) {
        print("Class name is null");
    }
}

classForName("NonExistingClass");
classForName(null);
```