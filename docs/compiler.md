# Rhino JavaScript compiler

## Overview
The JavaScript compiler translates JavaScript source into Java class files. The resulting Java class files can then be loaded and executed at another time, providing a convenient method for transferring JavaScript, and for avoiding translation cost.

Note that the top-level functions available to the shell (such as print) are not available to compiled scripts when they are run outside the shell.

## Compiler command line
    java org.mozilla.javascript.tools.jsc.Main [options] file1.js [file2.js...]

where options are:

### -extends java-class-name

Specifies that a java class extending the Java class java-class-name should be generated from the incoming JavaScript source file. Each global function in the source file is made a method of the generated class, overriding any methods in the base class by the same name.

### -implements java-intf-name

Specifies that a java class implementing the Java interface java-intf-name should be generated from the incoming JavaScript source file. Each global function in the source file is made a method of the generated class, implementing any methods in the interface by the same name.


### -debug or -g

Specifies that debug information should be generated. May not be combined with optimization at an optLevel greater than zero.


### -main-method-class className

Specify the class name used for main method implementation. The class must have a method matching public static void main(Script sc, String[] args).


### -nosource

Does not save the source in the class file. Functions and scripts compiled this way cannot be decompiled. This option can be used to avoid distributing source or simply to save space in the resulting class file.


### -o outputFile

Writes the class file to outputFile, which should end in .class and must be a writable filename.

### -d outputDirectory

Writes the class file to outputDirectory.

### -opt optLevel

Optimizes at level optLevel, which must be an integer between -1 and 9. See Optimization for more details. If optLevel is greater than zero, -debug may not be specified.


### -package packageName

Specifies the package to generate the class into. The string packageName must be composed of valid identifier characters optionally separated by periods.


### -version versionNumber

Specifies the language version to compile with. The string versionNumber must be one of 100, 110, 120, 130, 140, 150, 160, 170, 180, or 200. See JavaScript Language Versions for more information on language versions.

## Examples
```
$ cat test.js
java.lang.System.out.println("hi, mom!");
$ java org.mozilla.javascript.tools.jsc.Main test.js
$ ls *.class
test.class
$ java test
hi, mom!
$ java org.mozilla.javascript.tools.jsc.Main -extends java.applet.Applet
    -implements java.lang.Runnable NervousText.js
```

--Norrisboyd 12:26, 13 June 2007 (PDT)

