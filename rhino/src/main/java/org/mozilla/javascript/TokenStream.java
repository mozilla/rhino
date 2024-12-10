/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.util.HashMap;

/**
 * This class implements the JavaScript scanner.
 *
 * <p>It is based on the C source files jsscan.c and jsscan.h in the jsref package.
 *
 * @see org.mozilla.javascript.Parser
 * @author Mike McCabe
 * @author Brendan Eich
 */
class TokenStream implements Parser.CurrentPositionReporter {
    /*
     * For chars - because we need something out-of-range
     * to check.  (And checking EOF by exception is annoying.)
     * Note distinction from EOF token type!
     */
    private static final int EOF_CHAR = -1;

    /*
     * Return value for readDigits() to signal the caller has
     * to return an number format problem.
     */
    private static final int REPORT_NUMBER_FORMAT_ERROR = -2;

    private static final char BYTE_ORDER_MARK = '\uFEFF';
    private static final char NUMERIC_SEPARATOR = '_';

    TokenStream(Parser parser, Reader sourceReader, String sourceString, int lineno) {
        this.parser = parser;
        this.lineno = lineno;
        if (sourceReader != null) {
            if (sourceString != null) Kit.codeBug();
            this.sourceReader = sourceReader;
            this.sourceBuffer = new char[512];
            this.sourceEnd = 0;
        } else {
            if (sourceString == null) Kit.codeBug();
            this.sourceString = sourceString;
            this.sourceEnd = sourceString.length();
        }
        this.sourceCursor = this.cursor = 0;
    }

    /* This function uses the cached op, string and number fields in
     * TokenStream; if getToken has been called since the passed token
     * was scanned, the op or string printed may be incorrect.
     */
    String tokenToString(int token) {
        if (Token.printTrees) {
            String name = Token.name(token);

            switch (token) {
                case Token.STRING:
                case Token.REGEXP:
                case Token.NAME:
                    return name + " `" + this.string + "'";

                case Token.NUMBER:
                    return "NUMBER " + this.number;

                case Token.BIGINT:
                    return "BIGINT " + this.bigInt.toString();
            }

            return name;
        }
        return "";
    }

    static boolean isKeyword(String s, int version, boolean isStrict) {
        return Token.EOF != stringToKeyword(s, version, isStrict);
    }

    private static int stringToKeyword(String name, int version, boolean isStrict) {
        if (version < Context.VERSION_ES6) {
            return stringToKeywordForJS(name);
        }
        return stringToKeywordForES(name, isStrict);
    }

    /** JavaScript 1.8 and earlier */
    private static int stringToKeywordForJS(String name) {
        // The following assumes that Token.EOF == 0
        final int Id_break = Token.BREAK,
                Id_case = Token.CASE,
                Id_continue = Token.CONTINUE,
                Id_default = Token.DEFAULT,
                Id_delete = Token.DELPROP,
                Id_do = Token.DO,
                Id_else = Token.ELSE,
                Id_export = Token.RESERVED,
                Id_false = Token.FALSE,
                Id_for = Token.FOR,
                Id_function = Token.FUNCTION,
                Id_if = Token.IF,
                Id_in = Token.IN,
                Id_let = Token.LET, // reserved ES5 strict
                Id_new = Token.NEW,
                Id_null = Token.NULL,
                Id_return = Token.RETURN,
                Id_switch = Token.SWITCH,
                Id_this = Token.THIS,
                Id_true = Token.TRUE,
                Id_typeof = Token.TYPEOF,
                Id_var = Token.VAR,
                Id_void = Token.VOID,
                Id_while = Token.WHILE,
                Id_with = Token.WITH,
                Id_yield = Token.YIELD, // reserved ES5 strict

                // the following are #ifdef RESERVE_JAVA_KEYWORDS in jsscan.c
                Id_abstract = Token.RESERVED, // ES3 only
                Id_boolean = Token.RESERVED, // ES3 only
                Id_byte = Token.RESERVED, // ES3 only
                Id_catch = Token.CATCH,
                Id_char = Token.RESERVED, // ES3 only
                Id_class = Token.RESERVED,
                Id_const = Token.CONST, // reserved
                Id_debugger = Token.DEBUGGER,
                Id_double = Token.RESERVED, // ES3 only
                Id_enum = Token.RESERVED,
                Id_extends = Token.RESERVED,
                Id_final = Token.RESERVED, // ES3 only
                Id_finally = Token.FINALLY,
                Id_float = Token.RESERVED, // ES3 only
                Id_goto = Token.RESERVED, // ES3 only
                Id_implements = Token.RESERVED, // ES3, ES5 strict
                Id_import = Token.RESERVED,
                Id_instanceof = Token.INSTANCEOF,
                Id_int = Token.RESERVED, // ES3
                Id_interface = Token.RESERVED, // ES3, ES5 strict
                Id_long = Token.RESERVED, // ES3 only
                Id_native = Token.RESERVED, // ES3 only
                Id_package = Token.RESERVED, // ES3, ES5 strict
                Id_private = Token.RESERVED, // ES3, ES5 strict
                Id_protected = Token.RESERVED, // ES3, ES5 strict
                Id_public = Token.RESERVED, // ES3, ES5 strict
                Id_short = Token.RESERVED, // ES3 only
                Id_static = Token.RESERVED, // ES3, ES5 strict
                Id_super = Token.RESERVED,
                Id_synchronized = Token.RESERVED, // ES3 only
                Id_throw = Token.THROW,
                Id_throws = Token.RESERVED, // ES3 only
                Id_transient = Token.RESERVED, // ES3 only
                Id_try = Token.TRY,
                Id_volatile = Token.RESERVED; // ES3 only

        int id;
        switch (name) {
            case "break":
                id = Id_break;
                break;
            case "case":
                id = Id_case;
                break;
            case "continue":
                id = Id_continue;
                break;
            case "default":
                id = Id_default;
                break;
            case "delete":
                id = Id_delete;
                break;
            case "do":
                id = Id_do;
                break;
            case "else":
                id = Id_else;
                break;
            case "export":
                id = Id_export;
                break;
            case "false":
                id = Id_false;
                break;
            case "for":
                id = Id_for;
                break;
            case "function":
                id = Id_function;
                break;
            case "if":
                id = Id_if;
                break;
            case "in":
                id = Id_in;
                break;
            case "let":
                id = Id_let;
                break;
            case "new":
                id = Id_new;
                break;
            case "null":
                id = Id_null;
                break;
            case "return":
                id = Id_return;
                break;
            case "switch":
                id = Id_switch;
                break;
            case "this":
                id = Id_this;
                break;
            case "true":
                id = Id_true;
                break;
            case "typeof":
                id = Id_typeof;
                break;
            case "var":
                id = Id_var;
                break;
            case "void":
                id = Id_void;
                break;
            case "while":
                id = Id_while;
                break;
            case "with":
                id = Id_with;
                break;
            case "yield":
                id = Id_yield;
                break;
            case "abstract":
                id = Id_abstract;
                break;
            case "boolean":
                id = Id_boolean;
                break;
            case "byte":
                id = Id_byte;
                break;
            case "catch":
                id = Id_catch;
                break;
            case "char":
                id = Id_char;
                break;
            case "class":
                id = Id_class;
                break;
            case "const":
                id = Id_const;
                break;
            case "debugger":
                id = Id_debugger;
                break;
            case "double":
                id = Id_double;
                break;
            case "enum":
                id = Id_enum;
                break;
            case "extends":
                id = Id_extends;
                break;
            case "final":
                id = Id_final;
                break;
            case "finally":
                id = Id_finally;
                break;
            case "float":
                id = Id_float;
                break;
            case "goto":
                id = Id_goto;
                break;
            case "implements":
                id = Id_implements;
                break;
            case "import":
                id = Id_import;
                break;
            case "instanceof":
                id = Id_instanceof;
                break;
            case "int":
                id = Id_int;
                break;
            case "interface":
                id = Id_interface;
                break;
            case "long":
                id = Id_long;
                break;
            case "native":
                id = Id_native;
                break;
            case "package":
                id = Id_package;
                break;
            case "private":
                id = Id_private;
                break;
            case "protected":
                id = Id_protected;
                break;
            case "public":
                id = Id_public;
                break;
            case "short":
                id = Id_short;
                break;
            case "static":
                id = Id_static;
                break;
            case "super":
                id = Id_super;
                break;
            case "synchronized":
                id = Id_synchronized;
                break;
            case "throw":
                id = Id_throw;
                break;
            case "throws":
                id = Id_throws;
                break;
            case "transient":
                id = Id_transient;
                break;
            case "try":
                id = Id_try;
                break;
            case "volatile":
                id = Id_volatile;
                break;
            default:
                id = 0;
                break;
        }
        if (id == 0) {
            return Token.EOF;
        }
        return id & 0xff;
    }

    /** ECMAScript 6. */
    private static int stringToKeywordForES(String name, boolean isStrict) {
        // The following assumes that Token.EOF == 0
        final int
                // 11.6.2.1 Keywords (ECMAScript2015)
                Id_break = Token.BREAK,
                Id_case = Token.CASE,
                Id_catch = Token.CATCH,
                Id_class = Token.RESERVED,
                Id_const = Token.CONST,
                Id_continue = Token.CONTINUE,
                Id_debugger = Token.DEBUGGER,
                Id_default = Token.DEFAULT,
                Id_delete = Token.DELPROP,
                Id_do = Token.DO,
                Id_else = Token.ELSE,
                Id_export = Token.RESERVED,
                Id_extends = Token.RESERVED,
                Id_finally = Token.FINALLY,
                Id_for = Token.FOR,
                Id_function = Token.FUNCTION,
                Id_if = Token.IF,
                Id_import = Token.RESERVED,
                Id_in = Token.IN,
                Id_instanceof = Token.INSTANCEOF,
                Id_new = Token.NEW,
                Id_return = Token.RETURN,
                Id_super = Token.SUPER,
                Id_switch = Token.SWITCH,
                Id_this = Token.THIS,
                Id_throw = Token.THROW,
                Id_try = Token.TRY,
                Id_typeof = Token.TYPEOF,
                Id_var = Token.VAR,
                Id_void = Token.VOID,
                Id_while = Token.WHILE,
                Id_with = Token.WITH,
                Id_yield = Token.YIELD,

                // 11.6.2.2 Future Reserved Words
                Id_await = Token.RESERVED,
                Id_enum = Token.RESERVED,

                // 11.6.2.2 NOTE Strict Future Reserved Words
                Id_implements = Token.RESERVED,
                Id_interface = Token.RESERVED,
                Id_package = Token.RESERVED,
                Id_private = Token.RESERVED,
                Id_protected = Token.RESERVED,
                Id_public = Token.RESERVED,

                // 11.8 Literals
                Id_false = Token.FALSE,
                Id_null = Token.NULL,
                Id_true = Token.TRUE,

                // Non ReservedWord, but Non IdentifierName in strict mode code.
                // 12.1.1 Static Semantics: Early Errors
                Id_let = Token.LET, // TODO : Valid IdentifierName in non-strict mode.
                Id_static = Token.RESERVED;

        int id = 0;
        switch (name) {
            case "break":
                id = Id_break;
                break;
            case "case":
                id = Id_case;
                break;
            case "catch":
                id = Id_catch;
                break;
            case "class":
                id = Id_class;
                break;
            case "const":
                id = Id_const;
                break;
            case "continue":
                id = Id_continue;
                break;
            case "debugger":
                id = Id_debugger;
                break;
            case "default":
                id = Id_default;
                break;
            case "delete":
                id = Id_delete;
                break;
            case "do":
                id = Id_do;
                break;
            case "else":
                id = Id_else;
                break;
            case "export":
                id = Id_export;
                break;
            case "extends":
                id = Id_extends;
                break;
            case "finally":
                id = Id_finally;
                break;
            case "for":
                id = Id_for;
                break;
            case "function":
                id = Id_function;
                break;
            case "if":
                id = Id_if;
                break;
            case "import":
                id = Id_import;
                break;
            case "in":
                id = Id_in;
                break;
            case "instanceof":
                id = Id_instanceof;
                break;
            case "new":
                id = Id_new;
                break;
            case "return":
                id = Id_return;
                break;
            case "super":
                id = Id_super;
                break;
            case "switch":
                id = Id_switch;
                break;
            case "this":
                id = Id_this;
                break;
            case "throw":
                id = Id_throw;
                break;
            case "try":
                id = Id_try;
                break;
            case "typeof":
                id = Id_typeof;
                break;
            case "var":
                id = Id_var;
                break;
            case "void":
                id = Id_void;
                break;
            case "while":
                id = Id_while;
                break;
            case "with":
                id = Id_with;
                break;
            case "yield":
                id = Id_yield;
                break;
            case "await":
                id = Id_await;
                break;
            case "enum":
                id = Id_enum;
                break;
            case "implements":
                if (isStrict) {
                    id = Id_implements;
                }
                break;
            case "interface":
                if (isStrict) {
                    id = Id_interface;
                }
                break;
            case "package":
                if (isStrict) {
                    id = Id_package;
                }
                break;
            case "private":
                if (isStrict) {
                    id = Id_private;
                }
                break;
            case "protected":
                if (isStrict) {
                    id = Id_protected;
                }
                break;
            case "public":
                if (isStrict) {
                    id = Id_public;
                }
                break;
            case "false":
                id = Id_false;
                break;
            case "null":
                id = Id_null;
                break;
            case "true":
                id = Id_true;
                break;
            case "let":
                id = Id_let;
                break;
            case "static":
                if (isStrict) {
                    id = Id_static;
                }
                break;
            default:
                id = 0;
                break;
        }
        if (id == 0) {
            return Token.EOF;
        }
        return id & 0xff;
    }

    @SuppressWarnings("AndroidJdkLibsChecker")
    private static boolean isValidIdentifierName(String str) {
        int i = 0;
        for (int c : str.codePoints().toArray()) {
            if (i++ == 0) {
                if (c != '$' && c != '_' && !Character.isUnicodeIdentifierStart(c)) {
                    return false;
                }
            } else {
                if (c != '$'
                        && c != '\u200c'
                        && c != '\u200d'
                        && !Character.isUnicodeIdentifierPart(c)) {
                    return false;
                }
            }
        }
        return true;
    }

    final String getSourceString() {
        return sourceString;
    }

    @Override
    public int getLineno() {
        return lineno;
    }

    public int getTokenStartLineno() {
        return tokenStartLineno;
    }

    final String getString() {
        return string;
    }

    final char getQuoteChar() {
        return (char) quoteChar;
    }

    final double getNumber() {
        return number;
    }

    final BigInteger getBigInt() {
        return bigInt;
    }

    final boolean isNumericBinary() {
        return isBinary;
    }

    final boolean isNumericOldOctal() {
        return isOldOctal;
    }

    final boolean isNumericOctal() {
        return isOctal;
    }

    final boolean isNumericHex() {
        return isHex;
    }

    final boolean eof() {
        return hitEOF;
    }

    final int getToken() throws IOException {
        int c;

        for (; ; ) {
            // Eat whitespace, possibly sensitive to newlines.
            for (; ; ) {
                c = getChar();
                if (c == EOF_CHAR) {
                    tokenStartLastLineEnd = lastLineEnd;
                    tokenStartLineno = lineno;
                    tokenBeg = cursor - 1;
                    tokenEnd = cursor;
                    return Token.EOF;
                } else if (c == '\n') {
                    dirtyLine = false;
                    tokenStartLastLineEnd = lastLineEnd;
                    tokenStartLineno = lineno;
                    tokenBeg = cursor - 1;
                    tokenEnd = cursor;
                    return Token.EOL;
                } else if (!isJSSpace(c)) {
                    if (c != '-') {
                        dirtyLine = true;
                    }
                    break;
                }
            }

            // Assume the token will be 1 char - fixed up below.
            tokenStartLastLineEnd = lastLineEnd;
            tokenStartLineno = lineno;
            tokenBeg = cursor - 1;
            tokenEnd = cursor;

            if (c == '@') return Token.XMLATTR;

            // identifier/keyword/instanceof?
            // watch out for starting with a <backslash>
            boolean identifierStart;
            boolean isUnicodeEscapeStart = false;
            if (c == '\\') {
                c = getChar();
                if (c == 'u') {
                    identifierStart = true;
                    isUnicodeEscapeStart = true;
                    stringBufferTop = 0;
                } else {
                    identifierStart = false;
                    ungetChar(c);
                    c = '\\';
                }
            } else {
                identifierStart = Character.isUnicodeIdentifierStart(c) || c == '$' || c == '_';
                if (identifierStart) {
                    stringBufferTop = 0;
                    addToString(c);
                }
            }

            if (identifierStart) {
                boolean containsEscape = isUnicodeEscapeStart;
                for (; ; ) {
                    if (isUnicodeEscapeStart) {
                        // strictly speaking we should probably push-back
                        // all the bad characters if the <backslash>uXXXX
                        // sequence is malformed. But since there isn't a
                        // correct context(is there?) for a bad Unicode
                        // escape sequence in an identifier, we can report
                        // an error here.
                        int escapeVal = 0;
                        if (matchTemplateLiteralChar('{')) {
                            for (; ; ) {
                                c = getTemplateLiteralChar();

                                if (c == '}') {
                                    break;
                                }
                                escapeVal = Kit.xDigitToInt(c, escapeVal);
                                if (escapeVal < 0) {
                                    break;
                                }
                            }

                            if (escapeVal < 0 || escapeVal > 0x10FFFF) {
                                parser.reportError("msg.invalid.escape");
                                break;
                            }
                        } else {
                            for (int i = 0; i != 4; ++i) {
                                c = getChar();
                                escapeVal = Kit.xDigitToInt(c, escapeVal);
                                // Next check takes care about c < 0 and bad escape
                                if (escapeVal < 0) {
                                    parser.reportError("msg.invalid.escape");
                                    break;
                                }
                            }
                        }
                        if (escapeVal < 0) {
                            parser.addError("msg.invalid.escape");
                            return Token.ERROR;
                        }
                        addToString(escapeVal);
                        isUnicodeEscapeStart = false;
                    } else {
                        c = getChar();
                        if (c == '\\') {
                            c = getChar();
                            if (c == 'u') {
                                isUnicodeEscapeStart = true;
                                containsEscape = true;
                            } else {
                                parser.addError("msg.illegal.character", c);
                                return Token.ERROR;
                            }
                        } else {
                            if (c == EOF_CHAR
                                    || c == BYTE_ORDER_MARK
                                    || !(Character.isUnicodeIdentifierPart(c) || c == '$')) {
                                break;
                            }
                            addToString(c);
                        }
                    }
                }
                ungetChar(c);

                String str = getStringFromBuffer();
                if (!containsEscape
                        || parser.compilerEnv.getLanguageVersion() >= Context.VERSION_ES6) {
                    // OPT we shouldn't have to make a string (object!) to
                    // check if it's a keyword.

                    // Return the corresponding token if it's a keyword
                    int result =
                            stringToKeyword(
                                    str,
                                    parser.compilerEnv.getLanguageVersion(),
                                    parser.inUseStrictDirective());
                    if (result != Token.EOF) {
                        if ((result == Token.LET || result == Token.YIELD)
                                && parser.compilerEnv.getLanguageVersion() < Context.VERSION_1_7) {
                            // LET and YIELD are tokens only in 1.7 and later
                            string = result == Token.LET ? "let" : "yield";
                            result = Token.NAME;
                        }
                        // Save the string in case we need to use in
                        // object literal definitions.
                        this.string = internString(str);
                        if (result != Token.RESERVED) {
                            return result;
                        } else if (parser.compilerEnv.getLanguageVersion() >= Context.VERSION_ES6) {
                            return result;
                        } else if (!parser.compilerEnv.isReservedKeywordAsIdentifier()) {
                            return result;
                        }
                    }
                } else if (isKeyword(
                        str,
                        parser.compilerEnv.getLanguageVersion(),
                        parser.inUseStrictDirective())) {
                    // If a string contains unicodes, and converted to a keyword,
                    // we convert the last character back to unicode
                    str = convertLastCharToHex(str);
                }

                if (containsEscape
                        && parser.compilerEnv.getLanguageVersion() >= Context.VERSION_ES6
                        && !isValidIdentifierName(str)) {
                    parser.reportError("msg.invalid.escape");
                    return Token.ERROR;
                }

                this.string = internString(str);
                return Token.NAME;
            }

            // is it a number?
            if (isDigit(c) || (c == '.' && isDigit(peekChar()))) {
                stringBufferTop = 0;
                int base = 10;
                isHex = isOldOctal = isOctal = isBinary = false;
                boolean es6 = parser.compilerEnv.getLanguageVersion() >= Context.VERSION_ES6;

                if (c == '0') {
                    c = getChar();
                    if (c == 'x' || c == 'X') {
                        base = 16;
                        isHex = true;
                        c = getChar();
                    } else if (es6 && (c == 'o' || c == 'O')) {
                        base = 8;
                        isOctal = true;
                        c = getChar();
                    } else if (es6 && (c == 'b' || c == 'B')) {
                        base = 2;
                        isBinary = true;
                        c = getChar();
                    } else if (isDigit(c)) {
                        base = 8;
                        isOldOctal = true;
                    } else {
                        addToString('0');
                    }
                }

                int emptyDetector = stringBufferTop;
                if (base == 10 || base == 16 || (base == 8 && !isOldOctal) || base == 2) {
                    c = readDigits(base, c);
                    if (c == REPORT_NUMBER_FORMAT_ERROR) {
                        parser.addError("msg.caught.nfe");
                        return Token.ERROR;
                    }
                } else {
                    while (isDigit(c)) {
                        // finally the oldOctal case
                        if (c >= '8') {
                            /*
                             * We permit 08 and 09 as decimal numbers, which
                             * makes our behavior a superset of the ECMA
                             * numeric grammar.  We might not always be so
                             * permissive, so we warn about it.
                             */
                            parser.addWarning("msg.bad.octal.literal", c == '8' ? "8" : "9");
                            base = 10;

                            c = readDigits(base, c);
                            if (c == REPORT_NUMBER_FORMAT_ERROR) {
                                parser.addError("msg.caught.nfe");
                                return Token.ERROR;
                            }
                            break;
                        }
                        addToString(c);
                        c = getChar();
                    }
                }
                if (stringBufferTop == emptyDetector && (isBinary || isOctal || isHex)) {
                    parser.addError("msg.caught.nfe");
                    return Token.ERROR;
                }

                boolean isInteger = true;
                boolean isBigInt = false;

                if (es6 && c == 'n') {
                    isBigInt = true;
                    c = getChar();
                } else if (base == 10 && (c == '.' || c == 'e' || c == 'E')) {
                    isInteger = false;
                    if (c == '.') {
                        isInteger = false;
                        addToString(c);
                        c = getChar();
                        c = readDigits(base, c);
                        if (c == REPORT_NUMBER_FORMAT_ERROR) {
                            parser.addError("msg.caught.nfe");
                            return Token.ERROR;
                        }
                    }
                    if (c == 'e' || c == 'E') {
                        isInteger = false;
                        addToString(c);
                        c = getChar();
                        if (c == '+' || c == '-') {
                            addToString(c);
                            c = getChar();
                        }
                        if (!isDigit(c)) {
                            parser.addError("msg.missing.exponent");
                            return Token.ERROR;
                        }
                        c = readDigits(base, c);
                        if (c == REPORT_NUMBER_FORMAT_ERROR) {
                            parser.addError("msg.caught.nfe");
                            return Token.ERROR;
                        }
                    }
                }
                ungetChar(c);
                String numString = getStringFromBuffer();
                this.string = numString;

                // try to remove the separator in a fast way
                int pos = numString.indexOf(NUMERIC_SEPARATOR);
                if (pos != -1) {
                    final char[] chars = numString.toCharArray();
                    for (int i = pos + 1; i < chars.length; i++) {
                        if (chars[i] != NUMERIC_SEPARATOR) {
                            chars[pos++] = chars[i];
                        }
                    }
                    numString = new String(chars, 0, pos);
                }

                if (isBigInt) {
                    this.bigInt = new BigInteger(numString, base);
                    return Token.BIGINT;
                }

                double dval;
                if (base == 10 && !isInteger) {
                    try {
                        // Use Java conversion to number from string...
                        dval = Double.parseDouble(numString);
                    } catch (NumberFormatException ex) {
                        parser.addError("msg.caught.nfe");
                        return Token.ERROR;
                    }
                } else {
                    dval = ScriptRuntime.stringPrefixToNumber(numString, 0, base);
                }

                this.number = dval;
                return Token.NUMBER;
            }

            // is it a string?
            if (c == '"' || c == '\'') {
                // We attempt to accumulate a string the fast way, by
                // building it directly out of the reader.  But if there
                // are any escaped characters in the string, we revert to
                // building it out of a StringBuffer.

                quoteChar = c;
                stringBufferTop = 0;

                c = getCharIgnoreLineEnd(false);
                strLoop:
                while (c != quoteChar) {
                    boolean unterminated = false;
                    if (c == EOF_CHAR) {
                        unterminated = true;
                    } else if (c == '\n') {
                        switch (lineEndChar) {
                            case '\n':
                            case '\r':
                                unterminated = true;
                                break;
                            case 0x2028: // <LS>
                            case 0x2029: // <PS>
                                // Line/Paragraph separators need to be included as is
                                c = lineEndChar;
                                break;
                            default:
                                break;
                        }
                    }

                    if (unterminated) {
                        ungetCharIgnoreLineEnd(c);
                        tokenEnd = cursor;
                        parser.addError("msg.unterminated.string.lit");
                        return Token.ERROR;
                    }

                    if (c == '\\') {
                        // We've hit an escaped character
                        int escapeVal;

                        c = getChar();
                        switch (c) {
                            case 'b':
                                c = '\b';
                                break;
                            case 'f':
                                c = '\f';
                                break;
                            case 'n':
                                c = '\n';
                                break;
                            case 'r':
                                c = '\r';
                                break;
                            case 't':
                                c = '\t';
                                break;

                            // \v a late addition to the ECMA spec,
                            // it is not in Java, so use 0xb
                            case 'v':
                                c = 0xb;
                                break;

                            case 'u':
                                // Get 4 hex digits; if the u escape is not
                                // followed by 4 hex digits, use 'u' + the
                                // literal character sequence that follows.
                                int escapeStart = stringBufferTop;
                                addToString('u');
                                escapeVal = 0;
                                if (matchChar('{')) {
                                    for (; ; ) {
                                        c = getChar();

                                        if (c == '}') {
                                            addToString(c);
                                            break;
                                        }
                                        escapeVal = Kit.xDigitToInt(c, escapeVal);
                                        if (escapeVal < 0) {
                                            break;
                                        }
                                        addToString(c);
                                    }

                                    if (escapeVal < 0 || escapeVal > 0x10FFFF) {
                                        parser.reportError("msg.invalid.escape");
                                        continue strLoop;
                                    }
                                } else {
                                    for (int i = 0; i != 4; ++i) {
                                        c = getChar();
                                        escapeVal = Kit.xDigitToInt(c, escapeVal);
                                        if (escapeVal < 0) {
                                            if (parser.compilerEnv.getLanguageVersion()
                                                    >= Context.VERSION_ES6) {
                                                parser.reportError("msg.invalid.escape");
                                            }
                                            continue strLoop;
                                        }
                                        addToString(c);
                                    }
                                }
                                // prepare for replace of stored 'u' sequence
                                // by escape value
                                stringBufferTop = escapeStart;
                                c = escapeVal;
                                break;
                            case 'x':
                                // Get 2 hex digits, defaulting to 'x'+literal
                                // sequence, as above.
                                c = getChar();
                                escapeVal = Kit.xDigitToInt(c, 0);
                                if (escapeVal < 0) {
                                    addToString('x');
                                    continue strLoop;
                                }
                                int c1 = c;
                                c = getChar();
                                escapeVal = Kit.xDigitToInt(c, escapeVal);
                                if (escapeVal < 0) {
                                    addToString('x');
                                    addToString(c1);
                                    continue strLoop;
                                }
                                // got 2 hex digits
                                c = escapeVal;
                                break;

                            case '\n':
                                // Remove line terminator after escape to follow
                                // SpiderMonkey and C/C++
                                c = getChar();
                                continue strLoop;

                            default:
                                if ('0' <= c && c < '8') {
                                    int val = c - '0';
                                    c = getChar();
                                    if ('0' <= c && c < '8') {
                                        val = 8 * val + c - '0';
                                        c = getChar();
                                        if ('0' <= c && c < '8' && val <= 037) {
                                            // c is 3rd char of octal sequence only
                                            // if the resulting val <= 0377
                                            val = 8 * val + c - '0';
                                            c = getChar();
                                        }
                                    }
                                    ungetChar(c);
                                    c = val;
                                }
                        }
                    }
                    addToString(c);
                    c = getChar(false);
                }

                String str = getStringFromBuffer();
                this.string = internString(str);
                cursor = sourceCursor;
                tokenEnd = cursor;
                return Token.STRING;
            }

            if (c == '#'
                    && cursor == 1
                    && peekChar() == '!'
                    && !this.parser.calledByCompileFunction) {
                // #! hashbang: only on the first line of a Script, no leading whitespace
                skipLine();
                return Token.COMMENT;
            }

            switch (c) {
                case ';':
                    return Token.SEMI;
                case '[':
                    return Token.LB;
                case ']':
                    return Token.RB;
                case '{':
                    return Token.LC;
                case '}':
                    return Token.RC;
                case '(':
                    return Token.LP;
                case ')':
                    return Token.RP;
                case ',':
                    return Token.COMMA;
                case '?':
                    if (parser.compilerEnv.getLanguageVersion() >= Context.VERSION_ES6) {
                        if (peekChar() == '.') {
                            // ?.digit is to be treated as ? .num
                            getChar();
                            if (!isDigit(peekChar())) {
                                return Token.QUESTION_DOT;
                            }
                            ungetChar('.');
                        } else if (matchChar('?')) {
                            if (matchChar('=')) {
                                return Token.ASSIGN_NULLISH;
                            }
                            return Token.NULLISH_COALESCING;
                        }
                    }
                    return Token.HOOK;
                case ':':
                    if (matchChar(':')) {
                        return Token.COLONCOLON;
                    }
                    return Token.COLON;
                case '.':
                    if (matchChar('.')) {
                        if (parser.compilerEnv.getLanguageVersion() >= Context.VERSION_1_8
                                && matchChar('.')) {
                            return Token.DOTDOTDOT;
                        }
                        return Token.DOTDOT;
                    } else if (matchChar('(')) {
                        return Token.DOTQUERY;
                    } else {
                        return Token.DOT;
                    }

                case '|':
                    if (matchChar('|')) {
                        if (matchChar('=')) return Token.ASSIGN_LOGICAL_OR;
                        else return Token.OR;
                    } else if (matchChar('=')) {
                        return Token.ASSIGN_BITOR;
                    } else {
                        return Token.BITOR;
                    }

                case '^':
                    if (matchChar('=')) {
                        return Token.ASSIGN_BITXOR;
                    }
                    return Token.BITXOR;

                case '&':
                    if (matchChar('&')) {
                        if (matchChar('=')) return Token.ASSIGN_LOGICAL_AND;
                        else return Token.AND;
                    } else if (matchChar('=')) {
                        return Token.ASSIGN_BITAND;
                    } else {
                        return Token.BITAND;
                    }

                case '=':
                    if (matchChar('=')) {
                        if (matchChar('=')) {
                            return Token.SHEQ;
                        }
                        return Token.EQ;
                    } else if (matchChar('>')) {
                        return Token.ARROW;
                    } else {
                        return Token.ASSIGN;
                    }

                case '!':
                    if (matchChar('=')) {
                        if (matchChar('=')) {
                            return Token.SHNE;
                        }
                        return Token.NE;
                    }
                    return Token.NOT;

                case '<':
                    /* NB:treat HTML begin-comment as comment-till-eol */
                    if (matchChar('!')) {
                        if (matchChar('-')) {
                            if (matchChar('-')) {
                                tokenStartLastLineEnd = lastLineEnd;
                                tokenStartLineno = lineno;
                                tokenBeg = cursor - 4;
                                skipLine();
                                commentType = Token.CommentType.HTML;
                                return Token.COMMENT;
                            }
                            ungetCharIgnoreLineEnd('-');
                        }
                        ungetCharIgnoreLineEnd('!');
                    }
                    if (matchChar('<')) {
                        if (matchChar('=')) {
                            return Token.ASSIGN_LSH;
                        }
                        return Token.LSH;
                    }
                    if (matchChar('=')) {
                        return Token.LE;
                    }
                    return Token.LT;

                case '>':
                    if (matchChar('>')) {
                        if (matchChar('>')) {
                            if (matchChar('=')) {
                                return Token.ASSIGN_URSH;
                            }
                            return Token.URSH;
                        }
                        if (matchChar('=')) {
                            return Token.ASSIGN_RSH;
                        }
                        return Token.RSH;
                    }
                    if (matchChar('=')) {
                        return Token.GE;
                    }
                    return Token.GT;

                case '*':
                    if (parser.compilerEnv.getLanguageVersion() >= Context.VERSION_ES6) {
                        if (matchChar('*')) {
                            if (matchChar('=')) {
                                return Token.ASSIGN_EXP;
                            }
                            return Token.EXP;
                        }
                    }
                    if (matchChar('=')) {
                        return Token.ASSIGN_MUL;
                    }
                    return Token.MUL;

                case '/':
                    markCommentStart();
                    // is it a // comment?
                    if (matchChar('/')) {
                        tokenStartLastLineEnd = lastLineEnd;
                        tokenStartLineno = lineno;
                        tokenBeg = cursor - 2;
                        skipLine();
                        commentType = Token.CommentType.LINE;
                        return Token.COMMENT;
                    }
                    // is it a /* or /** comment?
                    if (matchChar('*')) {
                        boolean lookForSlash = false;
                        tokenStartLastLineEnd = lastLineEnd;
                        tokenStartLineno = lineno;
                        tokenBeg = cursor - 2;
                        if (matchChar('*')) {
                            lookForSlash = true;
                            commentType = Token.CommentType.JSDOC;
                        } else {
                            commentType = Token.CommentType.BLOCK_COMMENT;
                        }
                        for (; ; ) {
                            c = getChar();
                            if (c == EOF_CHAR) {
                                tokenEnd = cursor - 1;
                                parser.addError("msg.unterminated.comment");
                                return Token.COMMENT;
                            } else if (c == '*') {
                                lookForSlash = true;
                            } else if (c == '/') {
                                if (lookForSlash) {
                                    cursor = sourceCursor;
                                    tokenEnd = cursor;
                                    return Token.COMMENT;
                                }
                            } else {
                                lookForSlash = false;
                                tokenEnd = cursor;
                            }
                        }
                    }

                    if (matchChar('=')) {
                        return Token.ASSIGN_DIV;
                    }
                    return Token.DIV;

                case '%':
                    if (matchChar('=')) {
                        return Token.ASSIGN_MOD;
                    }
                    return Token.MOD;

                case '~':
                    return Token.BITNOT;

                case '+':
                    if (matchChar('=')) {
                        return Token.ASSIGN_ADD;
                    } else if (matchChar('+')) {
                        return Token.INC;
                    } else {
                        return Token.ADD;
                    }

                case '-':
                    if (matchChar('=')) {
                        c = Token.ASSIGN_SUB;
                    } else if (matchChar('-')) {
                        if (!dirtyLine) {
                            // treat HTML end-comment after possible whitespace
                            // after line start as comment-until-eol
                            if (matchChar('>')) {
                                markCommentStart("--");
                                skipLine();
                                commentType = Token.CommentType.HTML;
                                return Token.COMMENT;
                            }
                        }
                        c = Token.DEC;
                    } else {
                        c = Token.SUB;
                    }
                    dirtyLine = true;
                    return c;

                case '`':
                    return Token.TEMPLATE_LITERAL;

                default:
                    parser.addError("msg.illegal.character", c);
                    return Token.ERROR;
            }
        }
    }

    /*
     * Helper to read the next digits according to the base
     * and ignore the number separator if there is one.
     */
    private int readDigits(int base, int c) throws IOException {
        if (isDigit(base, c)) {
            addToString(c);

            c = getChar();
            if (c == EOF_CHAR) {
                return EOF_CHAR;
            }

            while (true) {
                if (c == NUMERIC_SEPARATOR) {
                    // we do no peek here, we are optimistic for performance
                    // reasons and because peekChar() only does an getChar/ungetChar.
                    c = getChar();
                    // if the line ends after the separator we have
                    // to report this as an error
                    if (c == '\n' || c == EOF_CHAR) {
                        return REPORT_NUMBER_FORMAT_ERROR;
                    }

                    if (!isDigit(base, c)) {
                        // bad luck we have to roll back
                        ungetChar(c);
                        return NUMERIC_SEPARATOR;
                    }
                    addToString(NUMERIC_SEPARATOR);
                } else if (isDigit(base, c)) {
                    addToString(c);
                    c = getChar();
                    if (c == EOF_CHAR) {
                        return EOF_CHAR;
                    }
                } else {
                    return c;
                }
            }
        }
        return c;
    }

    // Use a HashMap to ensure that we only have one copy -- the original one
    // of any particular string. Yes, the "String.intern" function also does this,
    // but this is how Rhino has worked for years and it's not clear that we
    // want to make the JVM-wide intern pool as big as it might happen if we
    // used that.
    private String internString(String s) {
        String existing = allStrings.putIfAbsent(s, s);
        if (existing == null) {
            // First time we saw it
            return s;
        }
        return existing;
    }

    private static boolean isAlpha(int c) {
        // Use 'Z' < 'a'
        if (c <= 'Z') {
            return 'A' <= c;
        }
        return 'a' <= c && c <= 'z';
    }

    private static boolean isDigit(int base, int c) {
        return (base == 10 && isDigit(c))
                || (base == 16 && isHexDigit(c))
                || (base == 8 && isOctalDigit(c))
                || (base == 2 && isDualDigit(c));
    }

    private static boolean isDualDigit(int c) {
        return '0' == c || c == '1';
    }

    private static boolean isOctalDigit(int c) {
        return '0' <= c && c <= '7';
    }

    private static boolean isDigit(int c) {
        return '0' <= c && c <= '9';
    }

    private static boolean isHexDigit(int c) {
        return ('0' <= c && c <= '9') || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F');
    }

    /* As defined in ECMA.  jsscan.c uses C isspace() (which allows
     * \v, I think.)  note that code in getChar() implicitly accepts
     * '\r' == \u000D as well.
     */
    private static boolean isJSSpace(int c) {
        if (c <= 127) {
            return c == 0x20 || c == 0x9 || c == 0xC || c == 0xB;
        }
        return c == 0xA0
                || c == BYTE_ORDER_MARK
                || Character.getType((char) c) == Character.SPACE_SEPARATOR;
    }

    private static boolean isJSFormatChar(int c) {
        return c > 127 && Character.getType((char) c) == Character.FORMAT;
    }

    /** Parser calls the method when it gets / or /= in literal context. */
    void readRegExp(int startToken) throws IOException {
        int start = tokenBeg;
        stringBufferTop = 0;
        if (startToken == Token.ASSIGN_DIV) {
            // Miss-scanned /=
            addToString('=');
        } else {
            if (startToken != Token.DIV) Kit.codeBug();
            if (peekChar() == '*') {
                tokenEnd = cursor - 1;
                this.string = new String(stringBuffer, 0, stringBufferTop);
                parser.reportError("msg.unterminated.re.lit");
                return;
            }
        }

        boolean inCharSet = false; // true if inside a '['..']' pair
        int c;
        while ((c = getChar()) != '/' || inCharSet) {
            if (c == '\n' || c == EOF_CHAR) {
                ungetChar(c);
                tokenEnd = cursor - 1;
                this.string = new String(stringBuffer, 0, stringBufferTop);
                parser.reportError("msg.unterminated.re.lit");
                return;
            }
            if (c == '\\') {
                addToString(c);
                c = getChar();
                if (c == '\n' || c == EOF_CHAR) {
                    ungetChar(c);
                    tokenEnd = cursor - 1;
                    this.string = new String(stringBuffer, 0, stringBufferTop);
                    parser.reportError("msg.unterminated.re.lit");
                    return;
                }
            } else if (c == '[') {
                inCharSet = true;
            } else if (c == ']') {
                inCharSet = false;
            }
            addToString(c);
        }
        int reEnd = stringBufferTop;

        while (true) {
            if (matchChar('g')) addToString('g');
            else if (matchChar('i')) addToString('i');
            else if (matchChar('m')) addToString('m');
            else if (matchChar('s')) addToString('s');
            else if (matchChar('y')) addToString('y');
            else break;
        }
        tokenEnd = start + stringBufferTop + 2; // include slashes

        if (isAlpha(peekChar())) {
            parser.reportError(
                    "msg.invalid.re.flag", String.valueOf(Character.toChars(peekChar())));
        }

        this.string = new String(stringBuffer, 0, reEnd);
        this.regExpFlags = new String(stringBuffer, reEnd, stringBufferTop - reEnd);
    }

    String readAndClearRegExpFlags() {
        String flags = this.regExpFlags;
        this.regExpFlags = null;
        return flags;
    }

    private StringBuilder rawString = new StringBuilder();

    String getRawString() {
        if (rawString.length() == 0) {
            return "";
        }
        return rawString.toString();
    }

    private int getTemplateLiteralChar() throws IOException {
        /*
         * In Template Literals <CR><LF> and <CR> are normalized to <LF>
         *
         * Line and Paragraph separators (<LS> & <PS>) need to be included in the template strings as is
         */
        int c = getCharIgnoreLineEnd(false);

        if (c == '\n') {
            switch (lineEndChar) {
                case '\r':
                    // check whether dealing with a <CR><LF> sequence
                    if (charAt(cursor) == '\n') {
                        // consume the <LF> that followed the <CR>
                        getCharIgnoreLineEnd(false);
                    }
                    break;
                case 0x2028: // <LS>
                case 0x2029: // <PS>
                    // Line/Paragraph separators need to be included as is
                    c = lineEndChar;
                    break;
                default:
                    break;
            }

            // Adjust numbers: duplicates the logic in getChar thats skipped as getChar is called
            // via getCharIgnoreLineEnd
            lineEndChar = -1;
            lineStart = sourceCursor - 1;
            lineno++;
        }

        rawString.append((char) c);
        return c;
    }

    private void ungetTemplateLiteralChar(int c) {
        ungetCharIgnoreLineEnd(c);
        rawString.setLength(rawString.length() - 1);
    }

    private boolean matchTemplateLiteralChar(int test) throws IOException {
        int c = getTemplateLiteralChar();
        if (c == test) {
            return true;
        }
        ungetTemplateLiteralChar(c);
        return false;
    }

    private int peekTemplateLiteralChar() throws IOException {
        int c = getTemplateLiteralChar();
        ungetTemplateLiteralChar(c);
        return c;
    }

    int readTemplateLiteral(boolean isTaggedLiteral) throws IOException {
        rawString.setLength(0);
        stringBufferTop = 0;
        boolean hasInvalidEscapeSequences = false;

        while (true) {
            int c = getTemplateLiteralChar();
            switch (c) {
                case EOF_CHAR:
                    this.string = hasInvalidEscapeSequences ? null : getStringFromBuffer();
                    tokenEnd = cursor - 1; // restore tokenEnd
                    parser.reportError("msg.unexpected.eof");
                    return Token.ERROR;
                case '`':
                    rawString.setLength(rawString.length() - 1); // don't include "`"
                    this.string = hasInvalidEscapeSequences ? null : getStringFromBuffer();
                    cursor = sourceCursor;
                    tokenEnd = cursor;
                    return Token.TEMPLATE_LITERAL;
                case '$':
                    if (matchTemplateLiteralChar('{')) {
                        rawString.setLength(rawString.length() - 2); // don't include "${"
                        this.string = hasInvalidEscapeSequences ? null : getStringFromBuffer();
                        this.tokenEnd = cursor - 1; // don't include "{"
                        return Token.TEMPLATE_LITERAL_SUBST;
                    } else {
                        addToString(c);
                        break;
                    }
                case '\\':
                    // LineContinuation ::
                    //   \ LineTerminatorSequence
                    // EscapeSequence ::
                    //   CharacterEscapeSequence
                    //   0 [LA not DecimalDigit]
                    //   HexEscapeSequence
                    //   UnicodeEscapeSequence
                    // CharacterEscapeSequence ::
                    //   SingleEscapeCharacter
                    //   NonEscapeCharacter
                    // SingleEscapeCharacter ::
                    //   ' "  \  b f n r t v
                    // NonEscapeCharacter ::
                    //   SourceCharacter but not one of EscapeCharacter or LineTerminator
                    // EscapeCharacter ::
                    //   SingleEscapeCharacter
                    //   DecimalDigit
                    //   x
                    //   u
                    c = getTemplateLiteralChar();
                    switch (c) {
                        case '\n':
                        case '\u2028':
                        case '\u2029':
                            continue;
                        case '\'':
                        case '"':
                        case '\\':
                            // use as-is
                            break;
                        case 'b':
                            c = '\b';
                            break;
                        case 'f':
                            c = '\f';
                            break;
                        case 'n':
                            c = '\n';
                            break;
                        case 'r':
                            c = '\r';
                            break;
                        case 't':
                            c = '\t';
                            break;
                        case 'v':
                            c = 0xb;
                            break;
                        case 'x':
                            {
                                int escapeVal = 0;
                                for (int i = 0; i < 2; i++) {
                                    if (peekTemplateLiteralChar() == '`') {
                                        escapeVal = -1;
                                        break;
                                    }
                                    escapeVal =
                                            Kit.xDigitToInt(getTemplateLiteralChar(), escapeVal);
                                }

                                if (escapeVal < 0) {
                                    if (isTaggedLiteral) {
                                        hasInvalidEscapeSequences = true;
                                        continue;
                                    } else {
                                        parser.reportError("msg.syntax");
                                        return Token.ERROR;
                                    }
                                }
                                c = escapeVal;
                                break;
                            }
                        case 'u':
                            {
                                int escapeVal = 0;

                                if (matchTemplateLiteralChar('{')) {
                                    for (; ; ) {
                                        if (peekTemplateLiteralChar() == '`') {
                                            escapeVal = -1;
                                            break;
                                        }

                                        c = getTemplateLiteralChar();
                                        if (c == EOF_CHAR) {
                                            parser.reportError("msg.syntax");
                                            return Token.ERROR;
                                        }

                                        if (c == '}') {
                                            break;
                                        }
                                        escapeVal = Kit.xDigitToInt(c, escapeVal);
                                    }

                                    if (escapeVal < 0 || escapeVal > 0x10FFFF) {
                                        if (isTaggedLiteral) {
                                            hasInvalidEscapeSequences = true;
                                            continue;
                                        } else {
                                            parser.reportError("msg.syntax");
                                            return Token.ERROR;
                                        }
                                    }

                                    if (escapeVal > 0xFFFF) {
                                        addToString(Character.highSurrogate(escapeVal));
                                        addToString(Character.lowSurrogate(escapeVal));
                                        continue;
                                    }
                                    c = escapeVal;
                                    break;
                                }

                                for (int i = 0; i < 4; i++) {
                                    if (peekTemplateLiteralChar() == '`') {
                                        escapeVal = -1;
                                        break;
                                    }
                                    escapeVal =
                                            Kit.xDigitToInt(getTemplateLiteralChar(), escapeVal);
                                }

                                if (escapeVal < 0) {
                                    if (isTaggedLiteral) {
                                        hasInvalidEscapeSequences = true;
                                        continue;
                                    } else {
                                        parser.reportError("msg.syntax");
                                        return Token.ERROR;
                                    }
                                }
                                c = escapeVal;
                                break;
                            }
                        case '0':
                            {
                                int d = peekTemplateLiteralChar();
                                if (d >= '0' && d <= '9') {
                                    if (isTaggedLiteral) {
                                        hasInvalidEscapeSequences = true;
                                        continue;
                                    } else {
                                        parser.reportError("msg.syntax");
                                        return Token.ERROR;
                                    }
                                }
                                c = 0x00;
                                break;
                            }
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            if (isTaggedLiteral) {
                                hasInvalidEscapeSequences = true;
                                continue;
                            } else {
                                parser.reportError("msg.syntax");
                                return Token.ERROR;
                            }
                        default:
                            // use as-is
                            break;
                    }
                    addToString(c);
                    break;
                default:
                    addToString(c);
                    break;
            }
        }
    }

    boolean isXMLAttribute() {
        return xmlIsAttribute;
    }

    int getFirstXMLToken() throws IOException {
        xmlOpenTagsCount = 0;
        xmlIsAttribute = false;
        xmlIsTagContent = false;
        if (!canUngetChar()) return Token.ERROR;
        ungetChar('<');
        return getNextXMLToken();
    }

    int getNextXMLToken() throws IOException {
        tokenStartLastLineEnd = lastLineEnd;
        tokenStartLineno = lineno;
        tokenBeg = cursor;
        stringBufferTop = 0; // remember the XML

        for (int c = getChar(); c != EOF_CHAR; c = getChar()) {
            if (xmlIsTagContent) {
                switch (c) {
                    case '>':
                        addToString(c);
                        xmlIsTagContent = false;
                        xmlIsAttribute = false;
                        break;
                    case '/':
                        addToString(c);
                        if (peekChar() == '>') {
                            c = getChar();
                            addToString(c);
                            xmlIsTagContent = false;
                            xmlOpenTagsCount--;
                        }
                        break;
                    case '{':
                        ungetChar(c);
                        this.string = getStringFromBuffer();
                        return Token.XML;
                    case '\'':
                    case '"':
                        addToString(c);
                        if (!readQuotedString(c)) return Token.ERROR;
                        break;
                    case '=':
                        addToString(c);
                        xmlIsAttribute = true;
                        break;
                    case ' ':
                    case '\t':
                    case '\r':
                    case '\n':
                        addToString(c);
                        break;
                    default:
                        addToString(c);
                        xmlIsAttribute = false;
                        break;
                }

                if (!xmlIsTagContent && xmlOpenTagsCount == 0) {
                    this.string = getStringFromBuffer();
                    cursor = sourceCursor;
                    tokenEnd = cursor;
                    return Token.XMLEND;
                }
            } else {
                switch (c) {
                    case '<':
                        addToString(c);
                        c = peekChar();
                        switch (c) {
                            case '!':
                                c = getChar(); // Skip !
                                addToString(c);
                                c = peekChar();
                                switch (c) {
                                    case '-':
                                        c = getChar(); // Skip -
                                        addToString(c);
                                        c = getChar();
                                        if (c == '-') {
                                            addToString(c);
                                            if (!readXmlComment()) return Token.ERROR;
                                        } else {
                                            // throw away the string in progress
                                            stringBufferTop = 0;
                                            this.string = null;
                                            parser.addError("msg.XML.bad.form");
                                            return Token.ERROR;
                                        }
                                        break;
                                    case '[':
                                        c = getChar(); // Skip [
                                        addToString(c);
                                        if (getChar() == 'C'
                                                && getChar() == 'D'
                                                && getChar() == 'A'
                                                && getChar() == 'T'
                                                && getChar() == 'A'
                                                && getChar() == '[') {
                                            addToString('C');
                                            addToString('D');
                                            addToString('A');
                                            addToString('T');
                                            addToString('A');
                                            addToString('[');
                                            if (!readCDATA()) return Token.ERROR;

                                        } else {
                                            // throw away the string in progress
                                            stringBufferTop = 0;
                                            this.string = null;
                                            parser.addError("msg.XML.bad.form");
                                            return Token.ERROR;
                                        }
                                        break;
                                    default:
                                        if (!readEntity()) return Token.ERROR;
                                        break;
                                }
                                break;
                            case '?':
                                c = getChar(); // Skip ?
                                addToString(c);
                                if (!readPI()) return Token.ERROR;
                                break;
                            case '/':
                                // End tag
                                c = getChar(); // Skip /
                                addToString(c);
                                if (xmlOpenTagsCount == 0) {
                                    // throw away the string in progress
                                    stringBufferTop = 0;
                                    this.string = null;
                                    parser.addError("msg.XML.bad.form");
                                    return Token.ERROR;
                                }
                                xmlIsTagContent = true;
                                xmlOpenTagsCount--;
                                break;
                            default:
                                // Start tag
                                xmlIsTagContent = true;
                                xmlOpenTagsCount++;
                                break;
                        }
                        break;
                    case '{':
                        ungetChar(c);
                        this.string = getStringFromBuffer();
                        return Token.XML;
                    default:
                        addToString(c);
                        break;
                }
            }
        }

        tokenEnd = cursor;
        stringBufferTop = 0; // throw away the string in progress
        this.string = null;
        parser.addError("msg.XML.bad.form");
        return Token.ERROR;
    }

    /** */
    private boolean readQuotedString(int quote) throws IOException {
        for (int c = getChar(); c != EOF_CHAR; c = getChar()) {
            addToString(c);
            if (c == quote) return true;
        }

        stringBufferTop = 0; // throw away the string in progress
        this.string = null;
        parser.addError("msg.XML.bad.form");
        return false;
    }

    /** */
    private boolean readXmlComment() throws IOException {
        for (int c = getChar(); c != EOF_CHAR; ) {
            addToString(c);
            if (c == '-' && peekChar() == '-') {
                c = getChar();
                addToString(c);
                if (peekChar() == '>') {
                    c = getChar(); // Skip >
                    addToString(c);
                    return true;
                }
                continue;
            }
            c = getChar();
        }

        stringBufferTop = 0; // throw away the string in progress
        this.string = null;
        parser.addError("msg.XML.bad.form");
        return false;
    }

    /** */
    private boolean readCDATA() throws IOException {
        for (int c = getChar(); c != EOF_CHAR; ) {
            addToString(c);
            if (c == ']' && peekChar() == ']') {
                c = getChar();
                addToString(c);
                if (peekChar() == '>') {
                    c = getChar(); // Skip >
                    addToString(c);
                    return true;
                }
                continue;
            }
            c = getChar();
        }

        stringBufferTop = 0; // throw away the string in progress
        this.string = null;
        parser.addError("msg.XML.bad.form");
        return false;
    }

    /** */
    private boolean readEntity() throws IOException {
        int declTags = 1;
        for (int c = getChar(); c != EOF_CHAR; c = getChar()) {
            addToString(c);
            switch (c) {
                case '<':
                    declTags++;
                    break;
                case '>':
                    declTags--;
                    if (declTags == 0) return true;
                    break;
            }
        }

        stringBufferTop = 0; // throw away the string in progress
        this.string = null;
        parser.addError("msg.XML.bad.form");
        return false;
    }

    /** */
    private boolean readPI() throws IOException {
        for (int c = getChar(); c != EOF_CHAR; c = getChar()) {
            addToString(c);
            if (c == '?' && peekChar() == '>') {
                c = getChar(); // Skip >
                addToString(c);
                return true;
            }
        }

        stringBufferTop = 0; // throw away the string in progress
        this.string = null;
        parser.addError("msg.XML.bad.form");
        return false;
    }

    private String getStringFromBuffer() {
        tokenEnd = cursor;
        return new String(stringBuffer, 0, stringBufferTop);
    }

    private void addToString(int c) {
        int N = stringBufferTop;
        int codePointLen = Character.charCount(c);
        if (N + codePointLen >= stringBuffer.length) {
            char[] tmp = new char[stringBuffer.length * 2];
            System.arraycopy(stringBuffer, 0, tmp, 0, N);
            stringBuffer = tmp;
        }
        if (codePointLen == 1) {
            stringBuffer[N] = (char) c;
        } else {
            stringBuffer[N] = Character.highSurrogate(c);
            stringBuffer[N + 1] = Character.lowSurrogate(c);
        }
        stringBufferTop = N + codePointLen;
    }

    private boolean canUngetChar() {
        return ungetCursor == 0 || ungetBuffer[ungetCursor - 1] != '\n';
    }

    private void ungetChar(int c) {
        // can not unread past across line boundary
        if (ungetCursor != 0 && ungetBuffer[ungetCursor - 1] == '\n') Kit.codeBug();
        ungetBuffer[ungetCursor++] = c;
        cursor--;
    }

    private boolean matchChar(int test) throws IOException {
        int c = getCharIgnoreLineEnd();
        if (c == test) {
            tokenEnd = cursor;
            return true;
        }
        ungetCharIgnoreLineEnd(c);
        return false;
    }

    private int peekChar() throws IOException {
        int c = getChar();
        ungetChar(c);
        return c;
    }

    private int getChar() throws IOException {
        return getChar(true, false);
    }

    private int getChar(boolean skipFormattingChars) throws IOException {
        return getChar(skipFormattingChars, false);
    }

    private int getChar(boolean skipFormattingChars, boolean ignoreLineEnd) throws IOException {
        if (ungetCursor != 0) {
            cursor++;
            return ungetBuffer[--ungetCursor];
        }

        for (; ; ) {
            int c;
            if (sourceString != null) {
                if (sourceCursor == sourceEnd) {
                    hitEOF = true;
                    return EOF_CHAR;
                }
                cursor++;
                c = sourceString.codePointAt(sourceCursor);
                sourceCursor += Character.charCount(c);
            } else {
                if (sourceCursor == sourceEnd) {
                    if (!fillSourceBuffer()) {
                        hitEOF = true;
                        return EOF_CHAR;
                    }
                }
                cursor++;
                c = sourceBuffer[sourceCursor++];
            }

            if (!ignoreLineEnd && lineEndChar >= 0) {
                if (lineEndChar == '\r' && c == '\n') {
                    lineEndChar = '\n';
                    continue;
                }
                lineEndChar = -1;
                lineStart = sourceCursor - 1;
                lastLineEnd = tokenEnd;
                lineno++;
            }

            if (c <= 127) {
                if (c == '\n' || c == '\r') {
                    lineEndChar = c;
                    c = '\n';
                }
            } else {
                if (c == BYTE_ORDER_MARK) return c; // BOM is considered whitespace
                if (skipFormattingChars && isJSFormatChar(c)) {
                    continue;
                }
                if (ScriptRuntime.isJSLineTerminator(c)) {
                    lineEndChar = c;
                    c = '\n';
                }
            }
            return c;
        }
    }

    private int getCharIgnoreLineEnd() throws IOException {
        return getChar(true, true);
    }

    private int getCharIgnoreLineEnd(boolean skipFormattingChars) throws IOException {
        return getChar(skipFormattingChars, true);
    }

    private void ungetCharIgnoreLineEnd(int c) {
        ungetBuffer[ungetCursor++] = c;
        cursor--;
    }

    private void skipLine() throws IOException {
        // skip to end of line
        int c;
        while ((c = getChar()) != EOF_CHAR && c != '\n') {}
        ungetChar(c);
        tokenEnd = cursor;
    }

    /** Returns the offset into the current line. */
    @Override
    public int getOffset() {
        int n = sourceCursor - lineStart;
        if (lineEndChar >= 0) {
            --n;
        }
        return n;
    }

    private final int charAt(int index) {
        if (index < 0) {
            return EOF_CHAR;
        }
        if (sourceString != null) {
            if (index >= sourceEnd) {
                return EOF_CHAR;
            }
            return sourceString.charAt(index);
        }
        if (index >= sourceEnd) {
            int oldSourceCursor = sourceCursor;
            try {
                if (!fillSourceBuffer()) {
                    return EOF_CHAR;
                }
            } catch (IOException ioe) {
                // ignore it, we're already displaying an error...
                return EOF_CHAR;
            }
            // index recalculation as fillSourceBuffer can move saved
            // line buffer and change sourceCursor
            index -= (oldSourceCursor - sourceCursor);
        }
        return sourceBuffer[index];
    }

    private final String substring(int beginIndex, int endIndex) {
        if (sourceString != null) {
            return sourceString.substring(beginIndex, endIndex);
        }
        int count = endIndex - beginIndex;
        return new String(sourceBuffer, beginIndex, count);
    }

    @Override
    public String getLine() {
        int lineEnd = sourceCursor;
        if (lineEndChar >= 0) {
            // move cursor before newline sequence
            lineEnd -= 1;
            if (lineEndChar == '\n' && charAt(lineEnd - 1) == '\r') {
                lineEnd -= 1;
            }
        } else {
            // Read until the end of line
            int lineLength = lineEnd - lineStart;
            for (; ; ++lineLength) {
                int c = charAt(lineStart + lineLength);
                if (c == EOF_CHAR || ScriptRuntime.isJSLineTerminator(c)) {
                    break;
                }
            }
            lineEnd = lineStart + lineLength;
        }
        return substring(lineStart, lineEnd);
    }

    final String getLine(int position, int[] linep) {
        assert position >= 0 && position <= cursor;
        assert linep.length == 2;
        int delta = (cursor + ungetCursor) - position;
        int cur = sourceCursor;
        if (delta > cur) {
            // requested line outside of source buffer
            return null;
        }
        // read back until position
        int end = 0, lines = 0;
        for (; delta > 0; --delta, --cur) {
            assert cur > 0;
            int c = charAt(cur - 1);
            if (ScriptRuntime.isJSLineTerminator(c)) {
                if (c == '\n' && charAt(cur - 2) == '\r') {
                    // \r\n sequence
                    delta -= 1;
                    cur -= 1;
                }
                lines += 1;
                end = cur - 1;
            }
        }
        // read back until line start
        int start = 0, offset = 0;
        for (; cur > 0; --cur, ++offset) {
            int c = charAt(cur - 1);
            if (ScriptRuntime.isJSLineTerminator(c)) {
                start = cur;
                break;
            }
        }
        linep[0] = lineno - lines + (lineEndChar >= 0 ? 1 : 0);
        linep[1] = offset;
        if (lines == 0) {
            return getLine();
        }
        return substring(start, end);
    }

    private boolean fillSourceBuffer() throws IOException {
        if (sourceString != null) Kit.codeBug();
        if (sourceEnd == sourceBuffer.length) {
            if (lineStart != 0 && !isMarkingComment()) {
                System.arraycopy(sourceBuffer, lineStart, sourceBuffer, 0, sourceEnd - lineStart);
                sourceEnd -= lineStart;
                sourceCursor -= lineStart;
                lineStart = 0;
            } else {
                char[] tmp = new char[sourceBuffer.length * 2];
                System.arraycopy(sourceBuffer, 0, tmp, 0, sourceEnd);
                sourceBuffer = tmp;
            }
        }
        int n = sourceReader.read(sourceBuffer, sourceEnd, sourceBuffer.length - sourceEnd);
        if (n < 0) {
            return false;
        }
        sourceEnd += n;
        return true;
    }

    /** Return the current position of the scanner cursor. */
    public int getCursor() {
        return cursor;
    }

    /** Return the absolute source offset of the last scanned token. */
    public int getTokenBeg() {
        return tokenBeg;
    }

    /** Return the absolute source end-offset of the last scanned token. */
    public int getTokenEnd() {
        return tokenEnd;
    }

    /** Return tokenEnd - tokenBeg */
    public int getTokenLength() {
        return tokenEnd - tokenBeg;
    }

    /**
     * Return the type of the last scanned comment.
     *
     * @return type of last scanned comment, or 0 if none have been scanned.
     */
    public Token.CommentType getCommentType() {
        return commentType;
    }

    private void markCommentStart() {
        markCommentStart("");
    }

    private void markCommentStart(String prefix) {
        if (parser.compilerEnv.isRecordingComments() && sourceReader != null) {
            commentPrefix = prefix;
            commentCursor = sourceCursor - 1;
        }
    }

    private boolean isMarkingComment() {
        return commentCursor != -1;
    }

    final String getAndResetCurrentComment() {
        if (sourceString != null) {
            if (isMarkingComment()) Kit.codeBug();
            return sourceString.substring(tokenBeg, tokenEnd);
        }
        if (!isMarkingComment()) Kit.codeBug();
        StringBuilder comment = new StringBuilder(commentPrefix);
        comment.append(sourceBuffer, commentCursor, getTokenLength() - commentPrefix.length());
        commentCursor = -1;
        return comment.toString();
    }

    private static String convertLastCharToHex(String str) {
        int lastIndex = str.length() - 1;
        StringBuilder buf = new StringBuilder(str.substring(0, lastIndex));
        buf.append("\\u");
        String hexCode = Integer.toHexString(str.charAt(lastIndex));
        for (int i = 0; i < 4 - hexCode.length(); ++i) {
            buf.append('0');
        }
        buf.append(hexCode);
        return buf.toString();
    }

    @Override
    public int getPosition() {
        return tokenBeg;
    }

    @Override
    public int getLength() {
        return tokenEnd - tokenBeg;
    }

    public int getTokenColumn() {
        return tokenBeg - tokenStartLastLineEnd + 1;
    }

    // stuff other than whitespace since start of line
    private boolean dirtyLine;

    String regExpFlags;

    // Set this to an initial non-null value so that the Parser has
    // something to retrieve even if an error has occurred and no
    // string is found.  Fosters one class of error, but saves lots of
    // code.
    private String string = "";
    private double number;
    private BigInteger bigInt;
    private boolean isBinary;
    private boolean isOldOctal;
    private boolean isOctal;
    private boolean isHex;

    // delimiter for last string literal scanned
    private int quoteChar;

    private char[] stringBuffer = new char[128];
    private int stringBufferTop;
    private final HashMap<String, String> allStrings = new HashMap<>();

    // Room to backtrace from to < on failed match of the last - in <!--
    private final int[] ungetBuffer = new int[3];
    private int ungetCursor;

    private boolean hitEOF = false;

    private int lineStart = 0;
    private int lineEndChar = -1;
    int lineno;

    private String sourceString;
    private Reader sourceReader;
    private char[] sourceBuffer;
    private int sourceEnd;

    // sourceCursor is an index into a small buffer that keeps a
    // sliding window of the source stream.
    int sourceCursor;

    // cursor is a monotonically increasing index into the original
    // source stream, tracking exactly how far scanning has progressed.
    // Its value is the index of the next character to be scanned.
    int cursor;

    // Record start and end positions of last scanned token.
    int tokenBeg;
    int tokenEnd;

    private int lastLineEnd;
    private int tokenStartLastLineEnd;
    private int tokenStartLineno;

    // Type of last comment scanned.
    Token.CommentType commentType;

    // for xml tokenizer
    private boolean xmlIsAttribute;
    private boolean xmlIsTagContent;
    private int xmlOpenTagsCount;

    private Parser parser;

    private String commentPrefix = "";
    private int commentCursor = -1;
}
