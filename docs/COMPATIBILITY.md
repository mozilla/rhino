# Rhino Compatibility Policy

January, 2020

## Introduction

Rhino has, for years, offered a "language version" setting that allows the user to 
control approximately which version of the JavaScript language that it supports.
Over the years this has been used in various ways. This document provides guidelines
for maintaining this support.

## Setting the Language Version

The language version is set using the "Context" class. Since all Rhino code
is required to create a Context object before compiling or executing any
code, the language version is always available.

For instance, to set the language version to the "ES6" level (which is the
latest level at the time of writing), initialize Rhino like this:

    Context cx = Context.enter();
    cx.setLanguageVersion(Context.VERSION_ES6);

Well-behaved Rhino code should always set the language version. 
If not set, the language version defaults to 0. This effectively means
that no new features from any relase of JavaScript are enabled.

The command-line launcher for Rhino also supports a "version" parameter, which
takes the version in numeric form, according to the values of the constants
in the Context class. For instance, to launch the shell in ES6 mode:

    java -jar ./buildGradle/libs/rhino-1.7.12.jar -version 200

## Policy

The overall policy is as follows:

1) The "ES6" version, currently the maximum version, will track the ECMAScript
   specification, introducing new capabilities while striving to match
   the current specification at the time.
2) Other versions will continue to be supported, but will not necessarily
   receive new language features unless we are sure that they will not
   break existing code that runs on Rhino.

### ECMAScript Level Support

At the time of writing, the latest language version is "VERSION_ES6", or 200.
When an incompatibility with the then-current ECMAScript language specification
is found, we will fix it in this language version in the following ways:

* New global objects and keywords not previously found in Rhino will only be
  activated at the "ES6" language version and above.
* Any changes that would cause previously-valid code to fail, including 
  syntax changes, new "strict mode" checks, and other checks, will only be
  made at the "ES6" language version and above.

In the future, as we implement more recent JavaScript features, we will continue 
this policy, by adding new global objects, new syntax, and other new capabilities at
the "ES6" level.

If, at some point, supporting a new language feature means breaking existing code
that already works at the "ES6" language level, then we will add an "ES7"
or other appropriately-named level. However, if there is no backward compatibility
issue then we will not.

### Previous Language Versions

The purpose of supporting older language versions is to enable legacy code that uses
Rhino to continue to work without regression while we upgrade the platform. 

We do not intend to "go back in time" to support every nuance of JavaScript with every
version of the specification ever published. The presence of pre-ES6 language versions
is intended to prevent breaking legacy code, and not for any other reason.

### Defaults

The default language version in the library will remain as "0." Doing otherwise would
surely break code that depends on the default behavior of Rhino.

At some point, we may want to consider changing the default language version of the
command-line interpreter to "200," so that it gets "ES6" features by default.

## Recommendations for Rhino Users and Embedders

Rhino users should set the language level to the latest level when they
create their projects. Since we reserve the right to add "higher" language levels
for breaking changes, this will make it less likely that a newer version of Rhino
will break existing code.

At the time of writing, that is "VERSION_ES6," or 200.

Rhino users should also periodically consider increasing the language level
in their projects so that they can take advantage of more modern JavaScript features.

## Recommendations for Rhino Developers

When fixing a bug in Rhino, a developer should ask whether the bug fix would break
existing code. If it would, but the bug will improve compatibility with recent
ECMAScript specs, then the developer should guard the behavior with a version check.

When adding a new feature, a developer should do the same basic thing:

1) If the feature is a new global object (for instance, Symbol, which was released a while
back) it should be guarded by a version check.
2) If the feature involves new syntax, it should also be guarded by a version check.
3) Otherwise, it is not necessary to check the version unless existing code will
be broken by the new feature.

Finally, be reasonable -- it is not our intention to support every combination of
JavaScript features from all of time. If checking versions constantly is resulting
in overly-complex or slow code, take a step back and re-think the solution.

## Checking the Language Version

With access to a Context object, it is straightforward to check the version:

    public void doSomethingInRhino(Context cx) {
      if (cx.getLanguageVersion() >= Context.VERSION_ES6) {
        // Do the thing
      }
    }

Note that it's nearly always a bad idea to use "==" to check the language
version, as it will break things when there is eventually a higher version.
