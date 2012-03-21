/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Roger Lawrence
 *   Mike McCabe
 *   Igor Bukanov
 *   Bob Jervis
 *   Milen Nankov
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript;

/**
 * This class implements the JavaScript scanner.
 *
 * It is based on the C source files jsscan.c and jsscan.h
 * in the jsref package.
 *
 * @see org.mozilla.javascript.Parser
 *
 * @author Mike McCabe
 * @author Brendan Eich
 */

public class Token
{
    public static enum CommentType {
        LINE, BLOCK_COMMENT, JSDOC, HTML
    }

    // debug flags
    public static final boolean printTrees = false;
    static final boolean printICode = false;
    static final boolean printNames = printTrees || printICode;

    /**
     * Token types.  These values correspond to JSTokenType values in
     * jsscan.c.
     */

    public final static int
    // start enum
        ERROR          = -1, // well-known as the only code < EOF
        EOF            = 0,  // end of file token - (not EOF_CHAR)
        EOL            = 1,  // end of line

        // Interpreter reuses the following as bytecodes
        FIRST_BYTECODE_TOKEN    = 2,

        ENTERWITH      = 2,
        LEAVEWITH      = 3,
        RETURN         = 4,
        GOTO           = 5,
        IFEQ           = 6,
        IFNE           = 7,
        SETNAME        = 8,
        BITOR          = 9,
        BITXOR         = 10,
        BITAND         = 11,
        EQ             = 12,
        NE             = 13,
        LT             = 14,
        LE             = 15,
        GT             = 16,
        GE             = 17,
        LSH            = 18,
        RSH            = 19,
        URSH           = 20,
        ADD            = 21,
        SUB            = 22,
        MUL            = 23,
        DIV            = 24,
        MOD            = 25,
        NOT            = 26,
        BITNOT         = 27,
        POS            = 28,
        NEG            = 29,
        NEW            = 30,
        DELPROP        = 31,
        TYPEOF         = 32,
        GETPROP        = 33,
        GETPROPNOWARN  = 34,
        SETPROP        = 35,
        GETELEM        = 36,
        SETELEM        = 37,
        CALL           = 38,
        NAME           = 39,
        NUMBER         = 40,
        STRING         = 41,
        NULL           = 42,
        THIS           = 43,
        FALSE          = 44,
        TRUE           = 45,
        SHEQ           = 46,   // shallow equality (===)
        SHNE           = 47,   // shallow inequality (!==)
        REGEXP         = 48,
        BINDNAME       = 49,
        THROW          = 50,
        RETHROW        = 51, // rethrow caught exception: catch (e if ) use it
        IN             = 52,
        INSTANCEOF     = 53,
        LOCAL_LOAD     = 54,
        GETVAR         = 55,
        SETVAR         = 56,
        CATCH_SCOPE    = 57,
        ENUM_INIT_KEYS = 58,
        ENUM_INIT_VALUES = 59,
        ENUM_INIT_ARRAY= 60,
        ENUM_NEXT      = 61,
        ENUM_ID        = 62,
        THISFN         = 63,
        RETURN_RESULT  = 64, // to return previously stored return result
        ARRAYLIT       = 65, // array literal
        OBJECTLIT      = 66, // object literal
        GET_REF        = 67, // *reference
        SET_REF        = 68, // *reference    = something
        DEL_REF        = 69, // delete reference
        REF_CALL       = 70, // f(args)    = something or f(args)++
        REF_SPECIAL    = 71, // reference for special properties like __proto
        YIELD          = 72,  // JS 1.7 yield pseudo keyword
        STRICT_SETNAME = 73,
        STRICT_SETPROP = 74,
        STRICT_SETELEM = 75,

        // For XML support:
        DEFAULTNAMESPACE = 76, // default xml namespace =
        ESCXMLATTR     = 77,
        ESCXMLTEXT     = 78,
        REF_MEMBER     = 79, // Reference for x.@y, x..y etc.
        REF_NS_MEMBER  = 80, // Reference for x.ns::y, x..ns::y etc.
        REF_NAME       = 81, // Reference for @y, @[y] etc.
        REF_NS_NAME    = 82; // Reference for ns::y, @ns::y@[y] etc.

        // End of interpreter bytecodes
    public final static int
        LAST_BYTECODE_TOKEN    = REF_NS_NAME,

        TRY            = 83,
        SEMI           = 84,  // semicolon
        LB             = 85,  // left and right brackets
        RB             = 86,
        LC             = 87,  // left and right curlies (braces)
        RC             = 88,
        LP             = 89,  // left and right parentheses
        RP             = 90,
        COMMA          = 91, // comma operator

        ASSIGN         = 92,  // simple assignment  (=)
        ASSIGN_BITOR   = 93,  // |=
        ASSIGN_BITXOR  = 94,  // ^=
        ASSIGN_BITAND  = 95,  // |=
        ASSIGN_LSH     = 96,  // <<=
        ASSIGN_RSH     = 97,  // >>=
        ASSIGN_URSH    = 98,  // >>>=
        ASSIGN_ADD     = 99,  // +=
        ASSIGN_SUB     = 100,  // -=
        ASSIGN_MUL     = 101,  // *=
        ASSIGN_DIV     = 102,  // /=
        ASSIGN_MOD     = 103;  // %=

    public final static int
        FIRST_ASSIGN   = ASSIGN,
        LAST_ASSIGN    = ASSIGN_MOD,

        HOOK           = 104, // conditional (?:)
        COLON          = 105,
        OR             = 106, // logical or (||)
        AND            = 107, // logical and (&&)
        INC            = 108, // increment/decrement (++ --)
        DEC            = 109,
        DOT            = 110, // member operator (.)
        FUNCTION       = 111, // function keyword
        EXPORT         = 112, // export keyword
        IMPORT         = 113, // import keyword
        IF             = 114, // if keyword
        ELSE           = 115, // else keyword
        SWITCH         = 116, // switch keyword
        CASE           = 117, // case keyword
        DEFAULT        = 118, // default keyword
        WHILE          = 119, // while keyword
        DO             = 120, // do keyword
        FOR            = 121, // for keyword
        BREAK          = 122, // break keyword
        CONTINUE       = 123, // continue keyword
        VAR            = 124, // var keyword
        WITH           = 125, // with keyword
        CATCH          = 126, // catch keyword
        FINALLY        = 127, // finally keyword
        VOID           = 128, // void keyword
        RESERVED       = 129, // reserved keywords
        STRICT_RESERVED= 130, // reserved keywords in strict mode

        EMPTY          = 131,

        /* types used for the parse tree - these never get returned
         * by the scanner.
         */

        BLOCK          = 132, // statement block
        LABEL          = 133, // label
        TARGET         = 134,
        LOOP           = 135,
        EXPR_VOID      = 136, // expression statement in functions
        EXPR_RESULT    = 137, // expression statement in scripts
        JSR            = 138,
        SCRIPT         = 139, // top-level node for entire script
        TYPEOFNAME     = 140, // for typeof(simple-name)
        USE_STACK      = 141,
        SETPROP_OP        = 142, // x.y op= something
        STRICT_SETPROP_OP = 143,
        SETELEM_OP        = 144, // x[y] op= something
        STRICT_SETELEM_OP = 145,
        LOCAL_BLOCK    = 146,
        SET_REF_OP     = 147, // *reference op= something

        // For XML support:
        DOTDOT         = 148,  // member operator (..)
        COLONCOLON     = 149,  // namespace::name
        XML            = 150,  // XML type
        DOTQUERY       = 151,  // .() -- e.g., x.emps.emp.(name == "terry")
        XMLATTR        = 152,  // @
        XMLEND         = 153,

        // Optimizer-only-tokens
        TO_OBJECT      = 154,
        TO_DOUBLE      = 155,

        GET            = 156,  // JS 1.5 get pseudo keyword
        SET            = 157,  // JS 1.5 set pseudo keyword
        LET            = 158,  // JS 1.7 let pseudo keyword
        CONST          = 159,
        SETCONST       = 160,
        SETCONSTVAR    = 161,
        ARRAYCOMP      = 162,  // array comprehension
        LETEXPR        = 163,
        WITHEXPR       = 164,
        DEBUGGER       = 165,
        COMMENT        = 166,
        GENEXPR        = 167,
        LAST_TOKEN     = 168;

    /**
     * Returns a name for the token.  If Rhino is compiled with certain
     * hardcoded debugging flags in this file, it calls {@code #typeToName};
     * otherwise it returns a string whose value is the token number.
     */
    public static String name(int token)
    {
        if (!printNames) {
            return String.valueOf(token);
        }
        return typeToName(token);
    }

    /**
     * Always returns a human-readable string for the token name.
     * For instance, {@link #FINALLY} has the name "FINALLY".
     * @param token the token code
     * @return the actual name for the token code
     */
    public static String typeToName(int token) {
        switch (token) {
          case ERROR:           return "ERROR";
          case EOF:             return "EOF";
          case EOL:             return "EOL";
          case ENTERWITH:       return "ENTERWITH";
          case LEAVEWITH:       return "LEAVEWITH";
          case RETURN:          return "RETURN";
          case GOTO:            return "GOTO";
          case IFEQ:            return "IFEQ";
          case IFNE:            return "IFNE";
          case SETNAME:         return "SETNAME";
          case BITOR:           return "BITOR";
          case BITXOR:          return "BITXOR";
          case BITAND:          return "BITAND";
          case EQ:              return "EQ";
          case NE:              return "NE";
          case LT:              return "LT";
          case LE:              return "LE";
          case GT:              return "GT";
          case GE:              return "GE";
          case LSH:             return "LSH";
          case RSH:             return "RSH";
          case URSH:            return "URSH";
          case ADD:             return "ADD";
          case SUB:             return "SUB";
          case MUL:             return "MUL";
          case DIV:             return "DIV";
          case MOD:             return "MOD";
          case NOT:             return "NOT";
          case BITNOT:          return "BITNOT";
          case POS:             return "POS";
          case NEG:             return "NEG";
          case NEW:             return "NEW";
          case DELPROP:         return "DELPROP";
          case TYPEOF:          return "TYPEOF";
          case GETPROP:         return "GETPROP";
          case GETPROPNOWARN:   return "GETPROPNOWARN";
          case SETPROP:         return "SETPROP";
          case GETELEM:         return "GETELEM";
          case SETELEM:         return "SETELEM";
          case CALL:            return "CALL";
          case NAME:            return "NAME";
          case NUMBER:          return "NUMBER";
          case STRING:          return "STRING";
          case NULL:            return "NULL";
          case THIS:            return "THIS";
          case FALSE:           return "FALSE";
          case TRUE:            return "TRUE";
          case SHEQ:            return "SHEQ";
          case SHNE:            return "SHNE";
          case REGEXP:          return "REGEXP";
          case BINDNAME:        return "BINDNAME";
          case THROW:           return "THROW";
          case RETHROW:         return "RETHROW";
          case IN:              return "IN";
          case INSTANCEOF:      return "INSTANCEOF";
          case LOCAL_LOAD:      return "LOCAL_LOAD";
          case GETVAR:          return "GETVAR";
          case SETVAR:          return "SETVAR";
          case CATCH_SCOPE:     return "CATCH_SCOPE";
          case ENUM_INIT_KEYS:  return "ENUM_INIT_KEYS";
          case ENUM_INIT_VALUES:return "ENUM_INIT_VALUES";
          case ENUM_INIT_ARRAY: return "ENUM_INIT_ARRAY";
          case ENUM_NEXT:       return "ENUM_NEXT";
          case ENUM_ID:         return "ENUM_ID";
          case THISFN:          return "THISFN";
          case RETURN_RESULT:   return "RETURN_RESULT";
          case ARRAYLIT:        return "ARRAYLIT";
          case OBJECTLIT:       return "OBJECTLIT";
          case GET_REF:         return "GET_REF";
          case SET_REF:         return "SET_REF";
          case DEL_REF:         return "DEL_REF";
          case REF_CALL:        return "REF_CALL";
          case REF_SPECIAL:     return "REF_SPECIAL";
          case YIELD:           return "YIELD";
          case STRICT_SETNAME:  return "STRICT_SETNAME";
          case STRICT_SETPROP:  return "STRICT_SETPROP";
          case STRICT_SETELEM:  return "STRICT_SETELEM";
          case DEFAULTNAMESPACE:return "DEFAULTNAMESPACE";
          case ESCXMLTEXT:      return "ESCXMLTEXT";
          case ESCXMLATTR:      return "ESCXMLATTR";
          case REF_MEMBER:      return "REF_MEMBER";
          case REF_NS_MEMBER:   return "REF_NS_MEMBER";
          case REF_NAME:        return "REF_NAME";
          case REF_NS_NAME:     return "REF_NS_NAME";
          case TRY:             return "TRY";
          case SEMI:            return "SEMI";
          case LB:              return "LB";
          case RB:              return "RB";
          case LC:              return "LC";
          case RC:              return "RC";
          case LP:              return "LP";
          case RP:              return "RP";
          case COMMA:           return "COMMA";
          case ASSIGN:          return "ASSIGN";
          case ASSIGN_BITOR:    return "ASSIGN_BITOR";
          case ASSIGN_BITXOR:   return "ASSIGN_BITXOR";
          case ASSIGN_BITAND:   return "ASSIGN_BITAND";
          case ASSIGN_LSH:      return "ASSIGN_LSH";
          case ASSIGN_RSH:      return "ASSIGN_RSH";
          case ASSIGN_URSH:     return "ASSIGN_URSH";
          case ASSIGN_ADD:      return "ASSIGN_ADD";
          case ASSIGN_SUB:      return "ASSIGN_SUB";
          case ASSIGN_MUL:      return "ASSIGN_MUL";
          case ASSIGN_DIV:      return "ASSIGN_DIV";
          case ASSIGN_MOD:      return "ASSIGN_MOD";
          case HOOK:            return "HOOK";
          case COLON:           return "COLON";
          case OR:              return "OR";
          case AND:             return "AND";
          case INC:             return "INC";
          case DEC:             return "DEC";
          case DOT:             return "DOT";
          case FUNCTION:        return "FUNCTION";
          case EXPORT:          return "EXPORT";
          case IMPORT:          return "IMPORT";
          case IF:              return "IF";
          case ELSE:            return "ELSE";
          case SWITCH:          return "SWITCH";
          case CASE:            return "CASE";
          case DEFAULT:         return "DEFAULT";
          case WHILE:           return "WHILE";
          case DO:              return "DO";
          case FOR:             return "FOR";
          case BREAK:           return "BREAK";
          case CONTINUE:        return "CONTINUE";
          case VAR:             return "VAR";
          case WITH:            return "WITH";
          case CATCH:           return "CATCH";
          case FINALLY:         return "FINALLY";
          case VOID:            return "VOID";
          case RESERVED:        return "RESERVED";
          case STRICT_RESERVED: return "STRICT_RESERVED";
          case EMPTY:           return "EMPTY";
          case BLOCK:           return "BLOCK";
          case LABEL:           return "LABEL";
          case TARGET:          return "TARGET";
          case LOOP:            return "LOOP";
          case EXPR_VOID:       return "EXPR_VOID";
          case EXPR_RESULT:     return "EXPR_RESULT";
          case JSR:             return "JSR";
          case SCRIPT:          return "SCRIPT";
          case TYPEOFNAME:      return "TYPEOFNAME";
          case USE_STACK:       return "USE_STACK";
          case SETPROP_OP:        return "SETPROP_OP";
          case STRICT_SETPROP_OP: return "STRICT_SETPROP_OP";
          case SETELEM_OP:        return "SETELEM_OP";
          case STRICT_SETELEM_OP: return "SETELEM_OP";
          case LOCAL_BLOCK:     return "LOCAL_BLOCK";
          case SET_REF_OP:      return "SET_REF_OP";
          case DOTDOT:          return "DOTDOT";
          case COLONCOLON:      return "COLONCOLON";
          case XML:             return "XML";
          case DOTQUERY:        return "DOTQUERY";
          case XMLATTR:         return "XMLATTR";
          case XMLEND:          return "XMLEND";
          case TO_OBJECT:       return "TO_OBJECT";
          case TO_DOUBLE:       return "TO_DOUBLE";
          case GET:             return "GET";
          case SET:             return "SET";
          case LET:             return "LET";
          case CONST:           return "CONST";
          case SETCONST:        return "SETCONST";
          case ARRAYCOMP:       return "ARRAYCOMP";
          case WITHEXPR:        return "WITHEXPR";
          case LETEXPR:         return "LETEXPR";
          case DEBUGGER:        return "DEBUGGER";
          case COMMENT:         return "COMMENT";
          case GENEXPR:         return "GENEXPR";
        }

        // Token without name
        throw new IllegalStateException(String.valueOf(token));
    }

    /**
     * Convert a keyword token to a name string for use with the
     * {@link Context.FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER} feature.
     * @param token A token
     * @return the corresponding name string
     */
    public static String keywordToName(int token) {
        switch (token) {
            case Token.BREAK:      return "break";
            case Token.CASE:       return "case";
            case Token.CONTINUE:   return "continue";
            case Token.DEFAULT:    return "default";
            case Token.DELPROP:    return "delete";
            case Token.DO:         return "do";
            case Token.ELSE:       return "else";
            case Token.FALSE:      return "false";
            case Token.FOR:        return "for";
            case Token.FUNCTION:   return "function";
            case Token.IF:         return "if";
            case Token.IN:         return "in";
            case Token.LET:        return "let";
            case Token.NEW:        return "new";
            case Token.NULL:       return "null";
            case Token.RETURN:     return "return";
            case Token.SWITCH:     return "switch";
            case Token.THIS:       return "this";
            case Token.TRUE:       return "true";
            case Token.TYPEOF:     return "typeof";
            case Token.VAR:        return "var";
            case Token.VOID:       return "void";
            case Token.WHILE:      return "while";
            case Token.WITH:       return "with";
            case Token.YIELD:      return "yield";
            case Token.CATCH:      return "catch";
            case Token.CONST:      return "const";
            case Token.DEBUGGER:   return "debugger";
            case Token.FINALLY:    return "finally";
            case Token.INSTANCEOF: return "instanceof";
            case Token.THROW:      return "throw";
            case Token.TRY:        return "try";
            default:               return null;
        }
    }

    /**
     * Return true if the passed code is a valid Token constant.
     * @param code a potential token code
     * @return true if it's a known token
     */
    public static boolean isValidToken(int code) {
        return code >= ERROR
                && code <= LAST_TOKEN;
    }
}
