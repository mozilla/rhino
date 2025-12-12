# Rhino

Rhino is an open source (MPL 2.0) JavaScript engine, implemented in Java 11. The build system used is gradle, via the
gradle wrapper (`./gradlew`). There are no external dependencies, except for JUnit for unit tests.

## Useful commands

1. Build: `./gradlew build`
2. Run tests: `./gradlew test`
3. Format code: `./gradlew spotlessApply`
4. Checks (tests, formatting): `./gradlew check`

## Rules and code style

- Important: try to follow the existing code style and patterns.
- Be restrained in regard to writing code comments.
- Always add unit tests for any new feature or bug fixes. They should go either in `rhino` or `tests`; search for
  existing tests to make a decision on a case-by-case basis.
- New test classes should be written using JUnit 5. Migrating existing tests from JUnit 4 to JUnit 5 is not a goal
  though, unless explicitly requested.
- Code style is enforced via spotless. After every change, reformat the code.

## Code organization

The code base is organized in multiple modules. Most changes will go into the `rhino` or `tests` modules. Refer to
README.md for the full list.

- `rhino`: The primary codebase necessary and sufficient to run JavaScript code
- `rhino-tools`: Contains the shell, debugger, and the "Global" object, which many tests and other Rhino-based tools use
- `tests`: The tests that depend on all of Rhino and also the external tests, including the Mozilla legacy test scripts
  and the test262 tests

## Architecture

Rhino follows a classical architecture:

- [TokenStream](rhino/src/main/java/org/mozilla/javascript/TokenStream.java) is the lexer
- [Parser](rhino/src/main/java/org/mozilla/javascript/Parser.java) is the parser, which produces an AST modeled by the
  subclasses of [AstNode](rhino/src/main/java/org/mozilla/javascript/ast/AstNode.java)
- the [IRFactory](rhino/src/main/java/org/mozilla/javascript/IRFactory.java) will generate a tree IR, modeled
  by [Node](rhino/src/main/java/org/mozilla/javascript/Node.java)
- there are two backends:
    - one which generates java classes, in [Codegen](rhino/src/main/java/org/mozilla/javascript/optimizer/Codegen.java);
    - and one which generates a bytecode for [Interpreter](rhino/src/main/java/org/mozilla/javascript/Interpreter.java),
      in [CodeGenerator](rhino/src/main/java/org/mozilla/javascript/CodeGenerator.java).

[ScriptRuntime](rhino/src/main/java/org/mozilla/javascript/ScriptRuntime.java) is the main class for all runtime
methods, shared between compiled classes and interpreter.

Builtins such as `Object` or `Array` are implemented in
[NativeObject](rhino/src/main/java/org/mozilla/javascript/NativeObject.java),
[NativeArray](rhino/src/main/java/org/mozilla/javascript/NativeArray.java), etc.
