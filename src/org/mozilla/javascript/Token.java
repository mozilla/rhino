/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * The contents of this file are subject to the Netscape Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is Netscape
 * Communications Corporation.  Portions created by Netscape are
 * Copyright (C) 1997-1999 Netscape Communications Corporation. All
 * Rights Reserved.
 *
 * Contributor(s):
 * Roger Lawrence
 * Mike McCabe
 * Igor Bukanov
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the GNU Public License (the "GPL"), in which case the
 * provisions of the GPL are applicable instead of those above.
 * If you wish to allow use of your version of this file only
 * under the terms of the GPL and not to allow others to use your
 * version of this file under the NPL, indicate your decision by
 * deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL.  If you do not delete
 * the provisions above, a recipient may use your version of this
 * file under either the NPL or the GPL.
 */

package org.mozilla.javascript;

import java.io.*;

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

    // debug flags
    static final boolean printTrees = false;
    static final boolean printICode = false;
    static final boolean printNames = printTrees || printICode;


    /**
     * Token types.  These values correspond to JSTokenType values in
     * jsscan.c.
     */

    public final static int
    // start enum
        ERROR       = -1, // well-known as the only code < EOF
        EOF         = 0,  // end of file token - (not EOF_CHAR)
        EOL         = 1,  // end of line

        // Interpreter reuses the following as bytecodes
        FIRST_BYTECODE_TOKEN = 2,

        POPV        = 2,
        ENTERWITH   = 3,
        LEAVEWITH   = 4,
        RETURN      = 5,
        GOTO        = 6,
        IFEQ        = 7,
        IFNE        = 8,
        SETNAME     = 9,
        BITOR       = 10,
        BITXOR      = 11,
        BITAND      = 12,
        EQ          = 13,
        NE          = 14,
        LT          = 15,
        LE          = 16,
        GT          = 17,
        GE          = 18,
        LSH         = 19,
        RSH         = 20,
        URSH        = 21,
        ADD         = 22,
        SUB         = 23,
        MUL         = 24,
        DIV         = 25,
        MOD         = 26,
        NOT         = 27,
        BITNOT      = 28,
        POS         = 29,
        NEG         = 30,
        NEW         = 31,
        DELPROP     = 32,
        TYPEOF      = 33,
        GETPROP     = 34,
        SETPROP     = 35,
        GETELEM     = 36,
        SETELEM     = 37,
        CALL        = 38,
        NAME        = 39,
        NUMBER      = 40,
        STRING      = 41,
        ZERO        = 42,
        ONE         = 43,
        NULL        = 44,
        THIS        = 45,
        FALSE       = 46,
        TRUE        = 47,
        SHEQ        = 48,   // shallow equality (===)
        SHNE        = 49,   // shallow inequality (!==)
        REGEXP      = 50,
        POP         = 51,
        BINDNAME    = 52,
        THROW       = 53,
        IN          = 54,
        INSTANCEOF  = 55,
        NEWLOCAL    = 56,
        USELOCAL    = 57,
        GETBASE     = 58,
        GETVAR      = 59,
        SETVAR      = 60,
        UNDEFINED   = 61,
        NEWSCOPE    = 62,
        ENUM_INIT   = 63,
        ENUM_NEXT   = 64,
        ENUM_ID     = 65,
        THISFN      = 66,

        LAST_BYTECODE_TOKEN = 66,
        // End of interpreter bytecodes

        GETTHIS     = 67,
        TRY         = 68,
        SEMI        = 69,  // semicolon
        LB          = 70,  // left and right brackets
        RB          = 71,
        LC          = 72,  // left and right curlies (braces)
        RC          = 73,
        LP          = 74,  // left and right parentheses
        RP          = 75,
        COMMA       = 76,  // comma operator
        ASSIGN      = 77, // simple assignment  (=)
        ASSIGNOP    = 78, // assignment with operation (+= -= etc.)
        HOOK        = 79, // conditional (?:)
        COLON       = 80,
        OR          = 81, // logical or (||)
        AND         = 82, // logical and (&&)
        INC         = 83, // increment/decrement (++ --)
        DEC         = 84,
        DOT         = 85, // member operator (.)
        FUNCTION    = 86, // function keyword
        EXPORT      = 87, // export keyword
        IMPORT      = 88, // import keyword
        IF          = 89, // if keyword
        ELSE        = 90, // else keyword
        SWITCH      = 91, // switch keyword
        CASE        = 92, // case keyword
        DEFAULT     = 93, // default keyword
        WHILE       = 94, // while keyword
        DO          = 95, // do keyword
        FOR         = 96, // for keyword
        BREAK       = 97, // break keyword
        CONTINUE    = 98, // continue keyword
        VAR         = 99, // var keyword
        WITH        = 100, // with keyword
        CATCH       = 101, // catch keyword
        FINALLY     = 102, // finally keyword
        VOID        = 103, // void keyword
        RESERVED    = 104, // reserved keywords

        EMPTY       = 105,

        /* types used for the parse tree - these never get returned
         * by the scanner.
         */

        BLOCK       = 106, // statement block
        ARRAYLIT    = 107, // array literal
        OBJLIT      = 108, // object literal
        LABEL       = 109, // label
        TARGET      = 110,
        LOOP        = 111,
        ENUMDONE    = 112,
        EXPRSTMT    = 113,
        PARENT      = 114,
        JSR         = 115,
        NEWTEMP     = 116,
        USETEMP     = 117,
        SCRIPT      = 118,   // top-level node for entire script
        TYPEOFNAME  = 119,  // for typeof(simple-name)
        USE_STACK   = 120,
        SETPROP_OP  = 121, // x.y op= something
        SETELEM_OP  = 122, // x[y] op= something
        INIT_LIST   = 123,

        LAST_TOKEN  = 123;

    public static String name(int token)
    {
        if (printNames) {
            switch (token) {
                case ERROR:           return "error";
                case EOF:             return "eof";
                case EOL:             return "eol";
                case POPV:            return "popv";
                case ENTERWITH:       return "enterwith";
                case LEAVEWITH:       return "leavewith";
                case RETURN:          return "return";
                case GOTO:            return "goto";
                case IFEQ:            return "ifeq";
                case IFNE:            return "ifne";
                case SETNAME:         return "setname";
                case BITOR:           return "bitor";
                case BITXOR:          return "bitxor";
                case BITAND:          return "bitand";
                case EQ:              return "eq";
                case NE:              return "ne";
                case LT:              return "lt";
                case LE:              return "le";
                case GT:              return "gt";
                case GE:              return "ge";
                case LSH:             return "lsh";
                case RSH:             return "rsh";
                case URSH:            return "ursh";
                case ADD:             return "add";
                case SUB:             return "sub";
                case MUL:             return "mul";
                case DIV:             return "div";
                case MOD:             return "mod";
                case NOT:             return "not";
                case BITNOT:          return "bitnot";
                case POS:             return "pos";
                case NEG:             return "neg";
                case NEW:             return "new";
                case DELPROP:         return "delprop";
                case TYPEOF:          return "typeof";
                case GETPROP:         return "getprop";
                case SETPROP:         return "setprop";
                case GETELEM:         return "getelem";
                case SETELEM:         return "setelem";
                case CALL:            return "call";
                case NAME:            return "name";
                case NUMBER:          return "number";
                case STRING:          return "string";
                case ZERO:            return "zero";
                case ONE:             return "one";
                case NULL:            return "null";
                case THIS:            return "this";
                case FALSE:           return "false";
                case TRUE:            return "true";
                case SHEQ:            return "sheq";
                case SHNE:            return "shne";
                case REGEXP:          return "object";
                case POP:             return "pop";
                case BINDNAME:        return "bindname";
                case THROW:           return "throw";
                case IN:              return "in";
                case INSTANCEOF:      return "instanceof";
                case NEWLOCAL:        return "newlocal";
                case USELOCAL:        return "uselocal";
                case GETBASE:         return "getbase";
                case GETVAR:          return "getvar";
                case SETVAR:          return "setvar";
                case UNDEFINED:       return "undefined";
                case GETTHIS:         return "getthis";
                case TRY:             return "try";
                case NEWSCOPE:        return "newscope";
                case ENUM_INIT:       return "enum_init";
                case ENUM_NEXT:       return "enum_next";
                case ENUM_ID:         return "enum_id";
                case THISFN:          return "thisfn";
                case SEMI:            return "semi";
                case LB:              return "lb";
                case RB:              return "rb";
                case LC:              return "lc";
                case RC:              return "rc";
                case LP:              return "lp";
                case RP:              return "rp";
                case COMMA:           return "comma";
                case ASSIGN:          return "assign";
                case ASSIGNOP:        return "assignop";
                case HOOK:            return "hook";
                case COLON:           return "colon";
                case OR:              return "or";
                case AND:             return "and";
                case INC:             return "inc";
                case DEC:             return "dec";
                case DOT:             return "dot";
                case FUNCTION:        return "function";
                case EXPORT:          return "export";
                case IMPORT:          return "import";
                case IF:              return "if";
                case ELSE:            return "else";
                case SWITCH:          return "switch";
                case CASE:            return "case";
                case DEFAULT:         return "default";
                case WHILE:           return "while";
                case DO:              return "do";
                case FOR:             return "for";
                case BREAK:           return "break";
                case CONTINUE:        return "continue";
                case VAR:             return "var";
                case WITH:            return "with";
                case CATCH:           return "catch";
                case FINALLY:         return "finally";
                case RESERVED:        return "reserved";
                case EMPTY:           return "empty";
                case BLOCK:           return "block";
                case ARRAYLIT:        return "arraylit";
                case OBJLIT:          return "objlit";
                case LABEL:           return "label";
                case TARGET:          return "target";
                case LOOP:            return "loop";
                case ENUMDONE:        return "enumdone";
                case EXPRSTMT:        return "exprstmt";
                case PARENT:          return "parent";
                case JSR:             return "jsr";
                case NEWTEMP:         return "newtemp";
                case USETEMP:         return "usetemp";
                case SCRIPT:          return "script";
                case TYPEOFNAME:      return "typeofname";
                case USE_STACK:       return "use_stack";
                case SETPROP_OP:      return "setprop_op";
                case SETELEM_OP:      return "setelem_op";
                case INIT_LIST:       return "init_list";
            }
            return "<unknown="+token+">";
        }
        return null;
    }
}
