# Rhino 1.7.14
## January 6, 2022

# Highlights
## Features
#### ECMAScript features
* #160 Promise support (@gbrail)
* #837 BigInt support (@tuchida)
* #243 Template Literal support (@p-bakker)
* #879 String.raw (@tonygermano)
* #977 JSON superset (@tuchida)
* #932 globalThis (@p-bakker)
* #838 Exponential operator (@tuchida)
* #853 Short-hand property names (@tuchida)
* #902 Object.values / Object.entries / Object.fromEntries (@rPraml)
* #883 Number.EPSILON (@tonygermano)

#### Non-ECMAScript features
* #153 stack property on Error Constructor (@gbrail)
* #888 support for Mozilla-styled Stack formatting (@rbri)

[All features](https://github.com/mozilla/rhino/issues?q=milestone%3A%22Release+1.7.14%22+label%3Afeature+is%3Aclosed)

## Bugs
[All bug fixes](https://github.com/mozilla/rhino/issues?q=milestone%3A%22Release+1.7.14%22+label%3Abug)

## Performance
[All performance enhancements](https://github.com/mozilla/rhino/issues?q=milestone%3A%22Release+1.7.14%22+label%3APerformance)

## Java Interop
* #839 JavaScript for-of loop support for Java Iterables (@tuchida)
* #860 / #857 JSON.stringify support on Java Objects (@tonygermano / @rPraml)
* #1031 delete operator and .length setting support in JavaScript on Java Lists (@rPraml)
* #901 java.util.subList() support on JavaScript Arrays in Java (@rPraml)
* #889 Automatically increase size of Java List instances on .put(...) if required (@rPraml)

[All Java Interop related cases](https://github.com/mozilla/rhino/issues?q=milestone%3A%22Release+1.7.14%22+label%3A%22Java+Interop%22)

## Embedding Rhino
* #864 Context now implements Closable (@gbrail)
* #865 Introduction of LambdaFunction and LambdaConstructor, to be used to represent Java lambda functions as native JavaScript functions and also can be used to construct an entire class out of lambdas (@gbrail)
* #911 Throw InternalError instead of wrapped JavaException if thrown Java Exception class is not visible due to class shutter (@youngj)

[All Rhino embedding related cases](https://github.com/mozilla/rhino/issues?q=milestone%3A%22Release+1.7.14%22+label%3A%22embedding+Rhino%22+)

## Test262 suite
* Running against a much newer version of Test262 suite
* Improved documentation for running the Test262 suite + more options to make running the tests easier & faster
* #930 Support added for automatically regenerating the test262.properties file based on actual passage of tests
* #930 Improved feedback about reason of test failures

## Distribution
* #873 Automatic module names

## Internals
* #878 Removed idSwitch
* #896 SlotMap and Slot refactoring
* #922 Started extracting logic related to Abstract Operations as defined by the ECMAScript specification 

## Misc.
* #661 Rhino now listed in the [kangax ES6 Compatibility table](https://kangax.github.io/compat-table/es6) (must select the `Show obsolete platforms` in the upperleft corner)
* #661 Rhino now available as a compilation target in Babel through @babel/preset-env:
```
{
  "targets": {
    "rhino": "1.7.13"
  }
}
```
* Introduced Java Code Formatting through spotless
* Moved to CircleCI (instead of Travis)
* Enabled Gitlab CI, running tests on multiple Java versions'
  
## Thanks!

This release contains more than 350 commits from 23 contributors. Thanks to everyone who helped!

# Rhino 1.7.13
## September 2, 2020

### Script Engine support

Now that Nashorn has been deprecated, a number of people have asked about using
Rhino with the standard Java "ScriptEngine" interface. This release supports that.

However, in order to avoid breaking existing code, the script engine is
shipped in a separate JAR. Use the "rhino-engine" jar along with the
standard "rhino" jar to include this feature.

### Generator Support

This release supports generators based on the ES6-standard "function *"
syntaxt.

### Other important changes

This release also includes a number of quality and consistency fixes from five contributors.
As always, check out the [compatibility table](http://mozilla.github.io/rhino/compat/engines.html)
to see where Rhino stands today.

Gregory Brail (18):
*     Start on 1.7.13.
*     Add a build config for CircleCi.
*     Upgrade Gradle version to 6.5.
*     Update max workers.
*     Add support for ES6 generators.
*     Make "GeneratorFunction" pattern work in interpreted mode.
*     Complete implementation of GeneratorFunction.
*     Diagnostics to discover test timeouts.
*     Implement standard Java ScriptEngine
*     Change MozillaSuiteBenchmark to not fork threads.
*     Try to improve performance of MozillaSuiteTest
*     Disable some very slow tests
*     Start using JMH for benchmarks.
*     Many small fixes suggested by FindBugs and other linters
*     Turn off all the Mozilla tests that use the "BigO" function.
*     Move "BodyCodegen" into a file with the appropriate name.
*     Add feature flag for changes to Function.__proto__
*     Make __proto__ more closely match the spec

Karl Tauber (2):
*     Debugger fixes for FlatLaf (https://github.com/JFormDesigner/FlatLaf): - make renderer tree row height same as table row height - increase monospaced font size in script source and evaluation view if L&F uses larger font - remove renderer tree border if L&F sets one (built in L&F do not)
*     Debugger: fix NPE in variables view when expanding "CallSite"

Sylvain Jermini (7):
*     improve java.util.{List,Map} interop
*     travis: switch from trusty to xenial + set explicit -Xss in tests
*     try to fix circle, increase Xss
*     Fix failing string.trim.doctest in java11.
*     NativeDate: DateFormat, use explicit pattern, has the default has changed from java8 to 9. See https://stackoverflow.com/q/53317365
*     add java11 to travis test matrix
*     various fixes so the javadoc linter is happy

hjx胡继续 (2):
*     Add String.fromCodePoint()
*     fromCharCode optimize

ian4hu (5):
*     Add String.prototype.trimStart String.prototype.strimEnd
*     style: code style
*     test: string test with hex code instead of literal
*     remove unused StringBuilder
*     fix tests in test262/built-ins/String/fromCodePoint/*

leela52452 (1):
*     fix OWASP Cheat Sheet markdown format

RBRi (48):
*     switch value and done
*     make some method protected to support rhino-external implementations
*     NativeArrayBuffer slice() length is 2
*     fix String.indexOf and String.includes when searching for an empty st… (#747)
*     fix string.split with limit 0
*     fix for issue #665 (maybe we have to adjust the version switch to version 1_6)
*     fix for the recursion detection when converting an array into a string
*     fix #670
*     add testcase for issue #656
*     Symbol.length is 0 fixes #648
*     add testcase for issue #651
*     fix type o the expected value
*     improve seal() and freeze() processing; fixes #174
*     An error should be thrown when defining a property for a read-only variable in strict mode fixes 573
*     code cleanup
*     Do not save/share an instance of NativeArrayBuffer in a static variable. This introduces really strange side effects, because the instance is available (and changeable) from javascript code. These changes are 'persistent' in a way that starting a fresh rhino instance still uses this changed object.
*     various fixes for array calls using this pattern Array.prototype.foo.call(null, ....);
*     fix issue #648
*     fix Object.getOwnPropertyDescriptor for index properties on native strings
*     Function.__proto__ ignores write access
*     improved regexp parser based on commit 2164382abe078ea2024b9dff7fe416a78e3a668f from anba
*     fix handling of undefined parameter value in String.normalize()
*     it should not be possible to change the [[Prototype]]  of a non-extensible object; some cleanup
*     add version guard
*     fix all this-value-not-obj-coercible.js tests for string
*     checkstyle fixes
*     fix test suite setup
*     use the RangeError construction helper
*     improved handling of negative ArrayBuffer size fixes #708
*     in ES6 TypedArray constructors are only callable via new
*     avoid some auto boxing use Double.valueOf instead of new some cleanup try to optimize the code a bit to avoid unnecessary conversations and Double object creation make some methods static
*     regular expressions are not functions in the context of string replace fixes #726
*     improved regex range handling
*     do not inherit strict mode when parsing a function body
*     code style fix
*     fix wrong start object for getter in Object.assign
*     use Undefined.isUndefined()
*     String.prototype[Symbol.iterator].call(undefined) has to throw because undefined is not coercible
*     enable more test cases
*     reduce auto boxing to be able to better control this and avoid boxing if possible
*     make a bunch  of methods static
*     code cleanup
*     make the inner class static (this makes also SpotBugs happy)
*     Object.setPrototypeOf() arg[0] has to be coercible
*     fix one more case
*     match
*     search
*     throw if the lastIndex prop of an regex is readonly

# Rhino 1.7.12
## January 13, 2020

### XML external entities disabled by default

As of this release, Rhino makes "XML external entity injections" more difficult
by disabling fetching of external DTDs and stylesheets by default,
as recommended in the [OWASP Cheat Sheet](https://github.com/OWASP/CheatSheetSeries/blob/master/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.md). 
Even though this may break some existing projects, the fact that this
vulnerability is in the OWASP top 10 makes it important enough to change
the default.

Developers who still need this old capability can re-enable it by setting the
Context feature flag FEATURE_ENABLE_XML_SECURE_PARSING to false. (The default
is true.)

### New JAR for embedding use cases

This release also includes a second JAR artifact, "rhino-runtime.jar". This is
simply the existing Rhino JAR with the "tools" source directory excluded. 
This directory includes the Rhino shell as well as the default "Global" 
object, which includes capabilities to load and process external source
code.

Since some automated source-scanning tools mark these capabilties as insecure,
this new JAR provides a way to only include the parts of Rhino that embedders
typically need without pulling in additional capabilities.

Developers who typically embed "rhino.jar" might consider embedding "rhino-runtime.jar"
instead if they do not need all this.

Thanks to the following developers for the contributions below!

Aditya Pal (1):
*     Fix syntax error for comments in array (#607)

Chris Smith (1):
*     Adding secure configuration for XML parsers (#600)

Gregory Brail (12):
*     Update versions for 1.7.12 release.
*     Fix a code generation bug for generators.
*     Fix "fall through" comment.
*     Fix static analysis around NaN values.
*     More isNaN fixes and one rounding bug.
*     Make XML processor configuration more robust.
*     Enable SpotBugs plugin.
*     Fix minor static analysis findings.
*     Increase Travis timeout.
*     Disable more flaky "BigO" tests.
*     Fix handling of "return" in iterators.
*     Undo setting some members "final".

Ivan Di Francesco (1):
*     Fix warnings (#596)

Roland Praml (2):
*     FIX: NativeJavaObject.getDefaultValue recognizes numbers correctly
*     #511 fixing InterfaceAdapter abstract name lookup.

Stijn Kliemesch (7):
*     Private static method ScriptRuntime.enumInitOrder(Context,IdEnumeration) no longer expects given IdEnumeration's property obj to be of type ScriptableObject specifically, only of type SymbolScriptable.
*     Added testclass IterableTest to test iterable implementations, currently with one testcase for a host object, specifically one that uses Array Iterator.
*     Added more tests to IterableTest.
*     Fix for #616 (#617)
*     Fixes for calling several Object.prototype members.
*     Fixed dynamic scoping for implementations of Object.create and Object.defineProperties
*     Testcase for dynamic scoping and Object.create.

nename0 (2):
*     Fix Array.include return a wrapped Boolean
*     implement Array.includes to align to specs

RBRi (20):
*     fix for Map/Set working with ConsString as key also; closes #583
*     fix propertyIsEnumerable when using an index to access string; closes #582
*     ignore surplus search/match/replace parameters; closes #581
*     add support for setPrototypeOf
*     fixed imports
*     RangeError should be throw if the argument of Number.prototype.toFixed is less than 0 fixes #587
*     fix interpreter fallback when using streams (fixes #592)
*     Parser already always reads the reader into a string. Move this reader handling to the Context to be able to fall back to the interpreter in all cases.
*     fix imports
*     functions declared as var f = function f() {...} within a function should not impact higher scope variable with the same name
*     functions declared as var f = function f() {...} within a function should not impact higher scope variable with the same name
*     fix Boolean(document.all)
*     many more tests are passing already and some cleanup
*     add tests for built-ins/ThrowTypeError and built-ins/TypedArray
*     add tests for built-ins/TypedArrays
*     fix BYTES_PER_ELEMENT property
*     fix BYTES_PER_ELEMENT prototype property
*     fix TypedArray constructor arity
*     Fix issue with parseInt's handling of leading zeroes
*     #529 (#628)

# Rhino 1.7.11
## May 30, 2019

This release includes implementations of a number of missing JavaScript language features,
including:

* Improvement to the accuracy and reliability of the parser and its associated AST.
* The Map, Set, WeakMap, and WeakSet classes.
* More Array functions, including from, of, fill, keys, values, entries, and copyWithin.
* Many more Math methods.
* More Object functions, including seal and freeze.
* Many other bug fixes, as shown below.

In general, Rhino's philosophy for new features and changes is to
follow the ECMAScript spec, but to use the "language version" on the Context class
when backward compatibility would be broken.

For example, the Array.prototype.concat function in older versions of Rhino would treat
any input value as "spreadable" if it has the same constructor as Array. ECMAScript now says
clearly that this should only happen if the "isConcatSpreadable" symbol is present. In
this release, the old behavior is disable when the language level is at least the
"ES6" level (Context.VERSION_ES6, or 200).

Developers working on new code will be happier if they set the language level to
CONTEXT.VERSION_ES6, or use the "-version 200" flag to the command line tool.

A future release will change the default language version of the command line tool.

This release contains contributions from 15 developers. Thanks for all your hard work!

Attila Szegedi (7):
*     Improvements to MemberBox (#438)
*     Labmdify usages of ContextAction
*     Make ContextAction generic.
*     API for comparing continuation implementations
*     Algorithm for structural equality comparison of object graphs
*     Use structural equality as the equality algorithm for Interpreter.CallFrame, which serves as the NativeContinuation implementation.
*     Add workarounds for #437 and #449

Dimitar Nestorov (1):
*     Update README.md

Dirk Hogan (1):
*     431 update expiry of cached commonjs entity if no change on filesystem

Gregory Brail (18):
*     Prepare for next iteration.
*     Support replacing prototype functions of native objects.
*     Fix NullPointerException in __defineGetter__
*     Fix a problem with standard objects that have Symbols in their  prototypes.
*     Implement the built-in Set and Map classes for ES6.
*     Add WeakMap and WeakSet on top of the Map and Set work.
*     Upgrade max heap for Gradle tests to 1 GB.
*     Test cases and a small fix for native arrays.
*     Implement @@isConcatSpreadable and make Java arrays spreadable
*     Code review comments for @@isConcatSpreadable.
*     Make the "Sorting" helper class a proper singleton.
*     Un-do recent addition to the Scriptable interface.
*     Update Gradle wrapper version.
*     Fix flag tests that assume Context is available.
*     Fix a parser bug with missing semicolon warnings.
*     Fix a regression introduced recently to MemberBox.
*     More compatibility fixes for Array.prototype.concat.
*     Work on Array.of, from, and copyWithin.
*     Fix a parsing regression.

Igor Kuzmanenko (2):
*     fixes XmlMemberGet toSource implementation (#483)
*     fixes position for ParenthesizedExpression nodes (#129)

Markus Sunela (1):
*     Add manual OSGi manifest

Mozilla-GitHub-Standards (1):
*     Add Mozilla Code of Conduct file

Nedelcho Delchev (1):
*     Update README.md

RBRi (2):
*     some fixes/enhancements for the typed array support (#436)
*     Array fixes (#467)
*     fix all javadoc errors and all javadoc html warnings
*     method Global#version(xxx) should return the new version identifier if the version was updated
*     add more info to the error message
*     us the right method name if available
*     two minor improvements from HtmlUnit code * window list is sorted * Command 'Go to line' added
*     avoid npe if no file window is available
*     improve the design for flexible subclassing
*     remove duplicated check
*     fix the isSymbol detection to no longer detect the prototype as symbol
*     we have many more 262 tests passing already - i think we have to use as many tests as possible to check our quality
*     and some more; now we are at 51093
*     Use as many test262 tests as possible to check our quality
*     implement missing Math functions
*     disable some slow tests
*     Support for `arguments` object as TypedArray constructor argument this is the same as #297 but includes a simple test.
*     fix issue 437
*     use the system line separator for code generation
*     remove work around for 437
*     fix #449 also and remove the work around from EqualObjectGraphs
*     add @Override and some more cleanup
*     fix ctor called with date arg
*     valueOf has to be called without any args
*     fix remaining utc constructor case
*     minor cleanup
*     fix native array constructor called with null and same for the setter
*     code cleanup based on eclipse Photon suggestions
*     add more delegate methods to MemberBox; name all delegate method using the name of the delegated method
*     avoid star imports across the codebase
*     fall back to the interpreter if the byte code generation fails
*     fix serialization for NativeMap, NativeSet, NativeWeakMap, and NativeWeakSet
*     scope is only undefined in strict mode; fix special null entries for maps
*     more config cleanup - use files for excluding; again enable a bunch of tests already running
*     another VMBridge cleanup step (JDK 1.8 is the minimum at the moment)
*     another map fix
*     more detailed tests hack to dump already passing tests exclude one more class of tests more tests are passing
*     next try to make the travis build pass
*     array.fill
*     array.keys, array.entries, array.values
*     fixes for DataView, including enabling more test cases.
*     fix freeze/preventExtensions/seal/isFrozen/isExtensible/isSealed for ES6
*     add padStart and padEnd
*     make serialVersionUID private
*     make the test pass on my machine (also from inside eclipse)
*     fix null/undefined handling add first array includes impl
*     fix some array length handling border cases
*     NativeArray cleanup more error output for Test262SuiteTest
*     fix for #531 - using a symbol as array index crashes with a class cast exception
*     Update Jacoco version
*     functions are valid keys for WeakMap/WeakSet
*     use valueOf
*     cleanup vm bridge; since we are at java 8 there is no need to check for iterator availability
*     cleanup member; we are using the executable type instead of member
*     fix unused import
*     another jdk check no longer required
*     fix build
*     add (modified) test case from #135
*     first simple version of copyWithin
*     first array.of impl

Raphaël Jakse (1):
*     Test function arity and length properties

Ravi Kishore (1):
*     Retain of comments and their position in the actual code after parsing. (#465)

Stijn Kliemesch (1):
*     Added testcase for #510

Sébastien Doeraene (2):
*     Fix #448: Correctly wrap the result of Math.imul as an Int32.
*     Fix the conversions in typedarrays.Conversions.

Travis Haagen (2):
*     Fix bug that caused modified JavaScript files to never be reloaded
*     Created UrlModuleSourceProviderTest

nabice (2):
*     Fix #533 AstNode.setParent() causes a position error
*     test for #533

raphj (1):
*     Override getArity

stijnkliemesch (1):
*     Fixes Parser.throwStatement()

# Rhino 1.7.10
## April 9, 2018

This release fixes a regression introduced in version 1.7.7.2 that caused the
"propertyIsEnumerable" to throw an exception when used with String and typed array objects,
and possibly with custom user-written objects as well.

[Issue 415](https://github.com/mozilla/rhino/issues/415)

It contains a few other fixes:

Attila Szegedi (2):
*     Make as many CallFrame fields as possible final, initialize them in constructor
*     frame.debuggerFrame != null || frame.idata.itsNeedActivation is identical to frame.useActivation.

Jeremy Whitlock (1):
*     Missing properties are not enumerable when checking enumerability

# Rhino 1.7.9
## March 15, 2018

This release fixes a [potential ArrayIndexOutOfBoundsException](https://github.com/mozilla/rhino/issues/390)
that was introduced in 1.7.8. Since it's potentially pretty serious, projects currently using 1.7.8
should switch to this new release.

[Issue 390](https://github.com/mozilla/rhino/issues/390)

In addition, there is a new flag on Context called "FEATURE_INTEGER_WITHOUT_DECIMAL_PLACE."
If set, Rhino will work harder to display numbers in integer form rather than in floating-point
form. This feature is currently disabled by default, although if it proves popular than we can
consider enabling it in the future.

[PR 398](https://github.com/mozilla/rhino/pull/398)

At language level "ES6" and above, ToNumber conversion is now more compliant to the spec. (This
change is disabled for older language levels to prevent a problem with backward compatibility.)

[PR 383](https://github.com/mozilla/rhino/pull/383)

Finally, there are a number of other fixes.

Thanks to all who contributed, both with issues and with code!

Attila Szegedi:
*     Fix a JavaDoc warning

Ivan Vyshnevskyi:
*     Make ToNumber(String) conversion more spec-compliant
*     Report parsing error for default values in destructuring assignments

Michael[tm] Smith:
*     Add addError(String messageId, int c) method
*     Add “illegal character” test to ParserTest
*     Show word in “identifier is a reserved word” error
*     Add “identifier is a reserved word” test

Oleksandr Maksymenko:
*     changes to process integer object as integer and long as long, not as double

RBRi:
*     cleanup the code an try to make it faster (#373)

jhertel:
*     Correction: Compatability → Compatibility

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
