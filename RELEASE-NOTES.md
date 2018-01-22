# Rhino 1.7.8
## January 22, 2018

Most important changes in this release:

* JavaScript objects are no longer (somewhat) thread-safe by default
* Rhino is resistant to "hash flooding" attacks
* Rhino is only supported for Java 8 and up
* Rhino only builds with Gradle.

The primary change in this release is that the object storage format has changed
for objects derived from ScriptableObject (which is nearly all objects).

First, objects are no longer thread-safe by default. (They were thread-safe previously, but
not in a way that we could prove was 100 percent correct in all cases.) We do not believe
that the vast majority of Rhino code depended on this capability. 

The feature flag Context.FEATURE_THREAD_SAFE_OBJECTS may be used to enable locking on all
objects by default in case it is needed. Furthermore, the built-in "sync" function is still
supported, which can be used to wrap a function in a similar way to the "synchronized" keyword
in Java.

Second, when an object grows to a large number of properties, the native hash table implementation
is replaced with java.util.HashMap. This more complex (but slightly slower) hash implementation
is resistant to hash collisions. This makes Rhino in general resistant to "hash flooding" attacks.

Rhino now depends on Java 8. It also works on Java 9, although a few tests are currently breaking
around Date parsing and UTF-8 encoding.

Additional changes:

[Issue 290](https://github.com/mozilla/rhino/issues/290) Resist hash flooding attacks
[Issue 303](https://github.com/mozilla/rhino/issues/303) Arrow function position set error
[Issue 323](https://github.com/mozilla/rhino/issues/323) Possible OutOfMemory due to
infinite loop on parsing
[Issue 341](https://github.com/mozilla/rhino/issues/341) Objects are only thread-safe when
feature is enabled
[Issue 351](https://github.com/mozilla/rhino/issues/351) Function-level use-strict breaks
backward compatibility
[Issue 357](https://github.com/mozilla/rhino/issues/357) Array.sort() can throw
ArrayIndexOutOfBoundsException
[Issue 295](https://github.com/mozilla/rhino/issues/295) Change WrapFactory to only wrap
"true" primitive types and not subclasses.
[Issue 377](https://github.com/mozilla/rhino/issues/377) Context initialization in 
"sealed" mode failed for ES6 language level.
[PR102](https://github.com/mozilla/rhino/pull/102) Fix regexp parsing for "/0{0/"
[PR108](https://github.com/mozilla/rhino/pull/108) Attach jsdoc nodes to function params.
[PR 169](https://github.com/mozilla/rhino/pull/169) Enable calling default method
on Java 8.
[PR322](https://github.com/mozilla/rhino/pull/322) Fix static array functions
[PR353](https://github.com/mozilla/rhino/pull/353) Member box call error.
[PR355](https://github.com/mozilla/rhino/pull/358) Support array-like parameters to 
Function.prototype.apply().
[PR372](https://github.com/mozilla/rhino/pull/372) Improve test262 integration and enable
many more tests.

# Rhino 1.7.7.2
## August 24, 2017

This release contains fixes for a few important bugs that have caught Rhino users out in the
field.

* Do not throw a Java exception from array.prototype.sort() no matter how weird the user-supplied
comparator function is. This is a major difference between JavaScript and Java and has caused
us to avoid using "Arrays.sort" on JavaScript arrays.
* Fix incorrect offsets in the "DataView" class.

It also includes several other fixes:

* Always append a column number to V8-style stack traces. (Unfortunately it is always "0".)
* Support Object.is and Object.assign.
* Make the Symbol implementation match the spec (for VERSION_ES6 and up only).
* Avoid throwing internal Java exceptions for certain native objects in "toJSON".
* Allow subclassing of ContinuationPending.
* For VERSION_ES6 and up, sort properties in the spec-defined order (int property names
first).
* Fix stack overflow in string concatenation.
* Improve performance of ConsString.toString

The next release is likely to be 1.7.8.

# Rhino 1.7.7.1
## February 2, 2016

This release fixes a few critical bugs that were affecting code in the field:

* Improve String.prototype.repeat to work more efficiently and to not overflow
* Fix CallSite.isNative() and isTopLevel() so that they do not throw fatal errors
* Replace the implementation of the "YearFromTime" internal method for the Date class to avoid large CPU loops

Specific Changes:

*     Formatting issue with SourceReader.
*     Fix CallSite.isNative() and isTopLevel() to not throw.
*     Make String.prototype.repeat not overflow for large values, and change code style a bit.
*     Add tests from 1.7.7.
*     Add Gradle code from 1.7.7.
*     Replace YearFromTime with code from jsdate.cpp to avoid long CPU loops.

# Rhino 1.7.7
## June 17, 2015

Major changes in this release:

[Release 1.7.7](https://github.com/mozilla/rhino/issues?q=milestone%3A%22Release+1.7.7%22+is%3Aclosed)

Specific changes:

* [Issue 202](https://github.com/mozilla/rhino/issues/202) Initial support for ECMA Script 6 "method definitions".
* [Issue 201](https://github.com/mozilla/rhino/issues/201) Make sure that all native Error instances can be converted
to JSON.
* [Issue 184](https://github.com/mozilla/rhino/issues/184) Fix compile encoding errors.
* [Issue 178](https://github.com/mozilla/rhino/issues/178) Support build using Gradle (build using Ant is still
supported but will be removed in a future release.)
* [Issue 176](https://github.com/mozilla/rhino/issues/176) Add ScriptRuntime.throwCustomError to make it easier
to re-throw Java exceptions
* [Issue 166](https://github.com/mozilla/rhino/issues/166) Support many ES6 additions to the Math class.
* [Issue 165](https://github.com/mozilla/rhino/issues/165) Support many ES6 additions to the Number class.
* [Issue 163](https://github.com/mozilla/rhino/issues/163) Support ES6 additions to the String class.

Thanks to everyone who contributed!

Ahmed Ashour (3):
*     Add .gitattributes for End-Of-Line automatic handling
*     Remove extra space
*     Update .gitignore for eclipse generated files.

Evgeny Shepelyuk (45):
*     Implementing possibility of writing  JS tests code inside JUnit Test.
*     + gradle wrapper     + sources and test compilation     + jar building
*     + More JUnit style for Gradle compatibility     - disabled test removed
*     + running tests
*     + benchmark test changes for Gradle compaibility     + run benchmark from Gradle     + publishing artifacts from Gradle
*     + benchmark test changes for Gradle compaibility     + run benchmark from Gradle     + publishing artifacts from Gradle
*     + publishing artifacts to maven compatible repositories
*     + releasing with Gradle
*     + releasing with Gradle
*     + releasing with Gradle
*     + releasing with Gradle
*     [Gradle Release Plugin] - new version commit:  '1.7.8'.
*     + releasing with Gradle
*     + releasing with Gradle
*     + releasing with Gradle
*     [Gradle Release Plugin] - pre tag commit:  '1.7.8'.
*     Revert "+ publishing artifacts to maven compatible repositories"
*     rollback gradle release
*     + properly populating manifest     + exclude unnecesary files from jar
*     + adding license to jar
*     + build will not fail when maven credentials are not defined
*     + getting rhino display version from MANIFEST
*     * modifying README with Gradle details
*     Update README.md
*     Improving reporting of MozillaSuiteTest tests
*     Improving reporting of MozillaSuiteTest tests
*     + initial implementation of String.prototype.includes
*     + initial implementation of String.prototype.includes
*     + initial implementation of String.prototype.includes
*     + initial implementation of String.prototype.startsWith
*     + initial implementation of String.prototype.startsWith
*     + initial implementation of String.prototype.endsWith
*     + initial implementation of String.prototype.endsWith
*     + initial implementation of String.prototype.endsWith
*     + implementation of String.prototype.normalize
*     + implementation of String.prototype.normalize
*     + implementation of String.prototype.normalize
*     + implementation of String.prototype.repeat
*     + implementation of String.prototype.repeat
*     + implementation of String.prototype.codePointAt
*     + implementation of String.prototype.codePointAt
*     * fixing tests after implementation of ES6 string methods     + implementing RequireObjectCoercible from ECMA spec
*     * fixing tests after implementation of ES6 string methods     + implementing RequireObjectCoercible from ECMA spec
*     * fixing tests after implementation of ES6 string methods     + implementing RequireObjectCoercible from ECMA spec
*     Better exception reporting

Gregory Brail (16):
*     Update for new development iteration.
*     Read manifest URLs in a loop until we find the right one when trying     to determine the implementation version.
*     Permissions fix.
*     Fix potential NPE in ComplianceTest.
*     Re-run IDMap on NativeString.
*     Merge branch 'issue_176_2' of https://github.com/raimi/rhino into raimi-issue_176_2
*     Merge branch 'master' of https://github.com/asashour/rhino into asashour-master
*     Add .gitattributes.
*     Re-arrange "global variables" regression tests for fewer spurious failures     under Gradle.
*     Don't make "javaException" and "rhinoException" on the NativeError     object enumerable, because they cannot be converted to JSON.
*     Fix file name for V8 benchmark results.
*     Add some ES6 methods to Math and Number.
*     "BigO" regression test exhibits different and inconsistent behavior     on Java 8. So fix it.
*     Fix a bug in Math.hypot().
*     Added a constant for ECMAScript 6 language level:     Context.VERSION_ES6.
*     Add "readline" and "write" to console.

Raimund Jacob (5):
*     Allow throwing arbitrary objects from java world
*     176: Adjust javadoc to reality.
*     Emacs, sorry
*     Issue176: Test infrastructure
*     176: Actually Working Tests

sainaen (1):
*     Print exception message in case of JavaScriptException in ScriptTestsBase

tntim96 (1):
*     Fix Test compile encoding error 'unmappable character for encoding ASCII' - https://github.com/mozilla/rhino/issues/184

uchida_t (5):
*     set capacity for StringBuilder in String#repeat
*     Implement ES6 MethodDefinition
*     `set` and `get` is valid method name.
*     NumericLiteral/StringLiteral is valid method name.

# Rhino 1.7.6
## April 15, 2015

Merged many of the outstanding pull requests in the GitHub repo.

High-level changes include the following:

* Many compatibility fixes for Date, Array, String, and others (André Bargull)
* Array.find() and findIndex() (Evgeny Shepelyuk)
* String.trimLeft() and trimRight() (Travis Ennis)
* AST and "toSource" fixes (tntim96)
* Support for V8 Error extensions, including Error.captureStackTrace (Greg Brail)
* Support for typed arrays (Greg Brail)
* Support for attaching "external data" as the indexed properties of any object (Greg Brail)

André Bargull (60):
*     NativeDate: Date.length and Date.UTC.length is 7
*     NativeDate: Fix bug 732779 (Date.prototype.setXXX functions don't evaluate all parameters)
*     NativeDate: Date.prototype.toJSON uses [[GET]] to obtain the "toISOString" property
*     add js_toISOString method to format date values in ISO-8601 Extended Format with expanded year representation if necessary
*     NativeDate: Update Date.parse to support simplified ISO 8601 Extended Format [15.9.1.15]
*     Fix indentation in NativeDate.java
*     NativeError: Error.prototype.name and Error.prototype.message are not enumerable
*     NativeError: 15.11.2.1 and 15.11.4.4 updates
*     Arguments: arguments object should not have its own 'constructor' property, instead it inherits 'constructor' through its prototype
*     Arguments: 'callee', 'caller' and 'length' properties can be redefined for the arguments object
*     BaseFunction: Function.prototype.toString arity is 0
*     BaseFunction: The 'prototype' property on function instances can be redefined
*     BaseFunction: The 'arguments' property can be redefined for function instances
*     NativeArray: Check [[Extensible]] flag for dense-array case in [[Put]]
*     NativeArray: Remove (invalid) round-trips to ScriptRuntime when getting/setting elements
*     NativeArray: Follow spec more closely for Array.isArray and Array.prototype.concat
*     NativeArray: Array.prototype.{indexOf, lastIndexOf} bug fixes
*     NativeArray: Array.prototype.sort bug fixes (bug 728286)
*     NativeArray: Multiple changes to ensure specification algorithms are followed more closely
*     TopLevel,NativeGlobal,ScriptRuntime: Add cache for native error objects
*     NativeNumber: Handle case when precision is Infinity for Number.prototype.{toFixed,toExponential,toPrecision}
*     NativeObject: Object.prototype.toLocaleString uses [[Get]] to retrieve 'toString' property
*     NativeObject: Handle undefined arguments in Object.prototype.{hasOwnProperty,propertyIsEnumerable}
*     NativeString: String.prototype.replace arity is 2 instead of 1
*     NativeString: Handle undefined arguments in String.prototype.slice
*     ScriptRuntime: Fix range check to follow spec in numberToString()
*     ScriptRuntime: Set-up proto and parent-scope for TypeErrorThrower function
*     ScriptableObject: Object.defineProperties needs to make sure to call [[Get]] exactly once for each property entry
*     NativeRegExp: Handle undefined arguments in compile and exec
*     NativeRegExp: Report error if a RegExp flag is used more than once
*     NativeRegExp: RegExp.prototype.compile arity is 2
*     NativeRegExp: RegExp.prototype.lastIndex is lazily evaluated and may be set to non-writable as well
*     NativeRegExpCtor: arity of RegExp constructor is 2
*     NativeRegExpCtor: RegExp.prototype.{multiline,star,input,underscore} properties can be re-defined
*     RegExpImpl: Multiple changes for String.prototype.{match,search,replace,split}
*     Remove obsolete test case js1_2/function/regexparg-2-n.js
*     update test case doctests/arguments.doctest now that the arguments object inherits the 'constructor' property through its prototype
*     NativeRegExp: Make octal escape sequences match web reality
*     RegExpImpl: String.prototype.split with separator=undefined no longer treated as separator='undefined'
*     Fix indentation
*     Context: remove duplicate code in Context#newObject()
*     Context: Use StackTraceElement API to traverse stack-trace
*     NativeArray: address review comment from hns
*     Updated tests files per instructions in o.m.j.tests.MozillaSuiteTest
*     Patch for Bug 783797 ("Function calls across multiple global scopes are not handled properly")
*     Silence warnings in ClassFileWriter
*     Add missing @Deprecated annotations
*     Add missing @Override annotations
*     Add missing generic type info to deprecatedsrc/
*     Add missing generic type info to toolsrc/
*     Add missing generic type info to testsrc/
*     Add missing generic type info to src/
*     Fix invalid JavaDoc links
*     Replace StringBuffer with StringBuilder if possible
*     Address review comments from hns
*     Generators save and later restore the current stack when processing the 'yield' operation. Our current implementation for restoring the stack unfortunately confuses the Java classfile verifier, so at class load time a VerifierError is thrown. This happens because the verifier can no longer ensure that the proper types are placed on the stack, since the stack-state is saved in a simple Object[]. Re-ordering a few operations is only necessary so the verifier will again accept the generated class. But this is only done for generators because it creates slightly less efficient code compared to the standard case.
*     Add doctest and update comments with proper bug number
*     Patch for Bug 782363 ("Increment/Decrement alters const variables")
*     Patch for Bug 780458 ("Math.IEEEremainder makes ToInt32 slow for non-integer values") (V8):
*     Patch for Bug 789277 ("JSC: "missing ; after statement" message prints out for the line after the problem one")

C. Scott Ananian (1):
*     Don't swallow empty lines in doctest; split lines on Mac/Windows/Unix.

Edison (2):
*     Add working directory support to "runCommand"
*     Add working directory support to "runCommand"

Elliott Baron (1):
*     Add manpage for Rhino shell.

Evgeny Shepelyuk (2):
*     find and findIndex initial impl
*     Improving test framework     + one JUnit class = one JS suite     + reporting JS stacktrace on error     + load function is available in JS     + separate file for JS assertions

Gregory Brail (31):
*     Update versions for next iteration.
*     Update README for release notes.
*     Change benchmark output so we can "plot" it in Maven.
*     Fix code cleanup fix that broke the Java 6 build.
*     Fix benchmark output file format again.
*     Re-run ID map on NativeString.
*     Manually add .gitignore additions from @sghill.
*     Added a bit more to the README including content from @shirishp
*     Add a NOTICE with the V8 copyright message.
*     Move anba's new DoubleConversion code into the package with the     rest of the code derived from V8.
*     Remove retrotranslator code to generate 1.4-compatible bytecode.     Switch bytecode generation to Java 6.
*     Remove code and build artifacts pointing to the "old E4X" implementation,     based on XML Beans.
*     Remove unused XML beans-based E4X implementation.
*     One last vestige of XML Beans.
*     Re-generate ID map on NativeArray.
*     Initial checkin of typed arrays and tests from V8.     Fix bad capitalization.
*     Fix some integer encoding and add more test cases.
*     Switch typed array tests to use Evgeny's framework for running them.     Make them work only with version 1.8.
*     Make typed arrays only appear in 1.8.
*     Add List implementation for all native arrays.
*     Add "Error" to the set of standard Error constructors     that could go down the new code path to create an error.
*     Complete List implementation for typed arrays.     Write typed array unit tests for the List implementation.
*     Do not double-initialize Error.
*     Make loading of typed array classes lazy.     Rename Java classes so that the names are more consistent.
*     Support for V8-style stack trace support:       Error.prepareStackTrace       Error.captureStackTrace       Error.stackTraceLimit     And "V8" format stack traces.
*     Improve efficiency of NativeError via pre-cached Method objects     and reduced number of default fields.
*     Make "stack" non-enumerable until generated.
*     Add "setExternalArrayData" to ScriptableObject to allow array data     to be stored outside the core object.
*     Set default version in shell to "180".
*     Add method to both get and set external array data.
*     Add "initSafeStandardObjects" to create standard objects with     no Java class access whatsoever.

Ievgenii.Shepeliuk (2):
*     `findIndex' implementation
*     more V8 compatibility

Raymond Auge (1):
*     rhino exits the JVM even when run as a subshell of another java shell - bug-835147

Travis Ennis (2):
*     Added the Javascript 1.8 String methods trimLeft and trim Right.
*     Added the Javascript 1.8 String methods trimLeft and trimRight.

sainaen (1):
*     Add 'LanguageVersion' annotation. Make 1.8 default version for 'ScriptsTestsBase'

sghill (1):
*     removing old .cvsignore files

tntim96 (5):
*     'undefined' pattern should be treated as empty string in RegExp constructor     http://www.ecma-international.org/ecma-262/5.1/#sec-15.10.4.1     https://sourceforge.net/p/htmlunit/bugs/1599/
*     Bug 798642. AST 'toSource' on getter/setter mistakenly adding 'function' keyword     https://bugzilla.mozilla.org/show_bug.cgi?id=798642
*     Bug 800616. Fix AST 'toSource' for Octal and Hexadecimal literals     https://bugzilla.mozilla.org/show_bug.cgi?id=800616
*     Fix AST empty switch to source
*     Fix compile encoding error 'unmappable character for encoding ASCII'

# Rhino 1.7R5
## January 29, 2015

André Bargull (24):

*     Add missing license header to DefineClassMapInheritance.java
*     Remove invalid UTF-8 encoded unicode replacement characters (EF BF BD)
*     Add missing entries
*     Bug 772011: Decompiler does not add curly brackets for labelled statements
*     Add fix for bug-772833 (comment copied over from Parser::condExpr1 in frontend/Parser.cpp)
*     Fix bug 686806:     - trailing commas aren't allowed in object/array literals per JSON spec     - avoid using Integer.parseInt() to parse unicode escape sequences since parseInt() also accepts non-ASCII digits     - also avoid using Character.isDigit() in readNumber() for the very same reason     - readString() always created a StringBuilder instance to collect the input data, obviously this is actually only necessary when the input contains escaped characters. Therefore I've changed readString() to take the same approach as used in jsonparser.cpp     - the JSON number specification is stricter than Double.parseDouble(), for example Double.parseDouble() accepts the input string '10.'. To ensure only valid JSON number literals are passed to Double.parseDouble(), readNumber() was refactored to perform the necessary input validation.
*     Patch for bug 774083.
*     Patch for Bug 688023:
*     Fix broken test cases which relied on the old (and erroneous) toSource() output
*     Patch bug 685403
*     Patch for bug 637811:
*     Fix bug 773573: Search for first curly bracket after closing parentheses to take account of object destructuring parameters
*     Simple doctest for bug 773573
*     Array.prototype.sort performed an unchecked cast from long to int without any overflow checks, this may result in a negative length which then throws a NegativeArraySizeException in Java, cf. js1_5/Array/regress-157652.js . A similar problem was found in NativeJSON, so I've handled that as well
*     Add explicit cast to int to ensure previous behaviour is retained
*     Calls must not be special-calls and reference-calls at the same time, cf. js1_5/Regress/regress-319391.js for a test case
*     Enable js1_5/Regress/regress-319391.js for MozillaSuiteTest
*     Patch for Bug 728286
*     Add test case
*     Patch for Bug 778549
*     Add missing overflow detection when processing RegExp character class pattern
*     Patch for Bug 780147:
*     Patch for Bug 608235 ("Incorrect error message for undefined[undefined]")
*     Patch for Bug 784358 ("Defining const variable within eval() throws redeclaration error")

Evgeny Shepelyuk (1):

*     fix xmlbeans url

Gregory Brail (10):

*     Add JUnit-based benchmarks that we can automate in Jenkins.
*     Extract zipped-up tests into a directory and check them in that way.
*     Extracted the stuff that was formerly in testsrc/tests.tar.gz.
*     Add XML output for EMMA coverage reports.
*     Fix character encoding tests to work on Mac.
*     Add output to benchmarks that can work with the Jenkins     "Measurement Plots" plugin. This replaces the former output from the     "SunSpider" and "V8" benchmarks.
*     Add files for Maven deployment.
*     README update.
*     Update README for other tests.
*     Fix E4X test 13.4.4.24 which was failing on Java 8 due to different     HashMap iteration ordering.

Hannes Wallnoefer (8):

*     Unwrap Synchronizer in BaseFunction.toSource().
*     Override ScriptableObject.isEmpty in NativeArray
*     Reduce concurrency level / memory footprint in concurrent class cache hash maps
*     Return null for unhandled JavaAdapter methods
*     Make JavaAdapter work with abstract base classes and protected constructors
*     Change build version to 1_7R5pre
*     Extract code to create JS error from Java exception into separate ScriptRuntime method
*     Reduce invocation magic in ShellConsole JLine support classes

Kyle Cronin (2):

*     Patch for Bug 827538
*     Patch for Bug 738388

<!--- Start with " git shortlog --no-merges Rhino1_7R4_RELEASE.. | sed 's/^ /*/'" -->
