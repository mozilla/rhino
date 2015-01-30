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
