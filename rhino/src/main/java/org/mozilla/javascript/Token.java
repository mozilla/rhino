/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class implements the JavaScript scanner.
 *
 * <p>It is based on the C source files jsscan.c and jsscan.h in the jsref package.
 *
 * @see org.mozilla.javascript.Parser
 * @author Mike McCabe
 * @author Brendan Eich
 */
public enum Token {
    ERROR, // well-known as the only code < EOF
    EOF, // end of file token - (not EOF_CHAR)
    EOL, // end of line
    // Interpreter reuses the following as bytecodes

    ENTERWITH,
    LEAVEWITH,
    RETURN,
    GOTO,
    IFEQ,
    IFNE,
    SETNAME,
    BITOR,
    BITXOR,
    BITAND,
    EQ,
    NE,
    LT,
    LE,
    GT,
    GE,
    LSH,
    RSH,
    URSH,
    ADD,
    SUB,
    MUL,
    DIV,
    MOD,
    NOT,
    BITNOT,
    POS,
    NEG,
    NEW,
    DELPROP,
    TYPEOF,
    GETPROP,
    GETPROPNOWARN,
    SETPROP,
    GETELEM,
    SETELEM,
    CALL,
    NAME,
    NUMBER,
    STRING,
    NULL,
    THIS,
    FALSE,
    TRUE,
    SHEQ, // shallow equality (===)
    SHNE, // shallow inequality (!==)
    REGEXP,
    BINDNAME,
    THROW,
    RETHROW, // rethrow caught exception: catch (e if ) use it
    IN,
    INSTANCEOF,
    LOCAL_LOAD,
    GETVAR,
    SETVAR,
    CATCH_SCOPE,
    ENUM_INIT_KEYS,
    ENUM_INIT_VALUES,
    ENUM_INIT_ARRAY,
    ENUM_INIT_VALUES_IN_ORDER,
    ENUM_NEXT,
    ENUM_ID,
    THISFN,
    RETURN_RESULT, // to return previously stored return result
    ARRAYLIT, // array literal
    OBJECTLIT, // object literal
    GET_REF, // *reference
    SET_REF, // *reference    = something
    DEL_REF, // delete reference
    REF_CALL, // f(args)    = something or f(args)++
    REF_SPECIAL, // reference for special properties like __proto
    YIELD, // JS 1.7 yield pseudo keyword
    STRICT_SETNAME,
    EXP, // Exponentiation Operator

    // For XML support:
    DEFAULTNAMESPACE, // default xml namespace =
    ESCXMLATTR,
    ESCXMLTEXT,
    REF_MEMBER, // Reference for x.@y, x..y etc.
    REF_NS_MEMBER, // Reference for x.ns::y, x..ns::y etc.
    REF_NAME, // Reference for @y, @[y] etc.
    REF_NS_NAME, // Reference for ns::y, @ns::y@[y] etc.
    BIGINT, // ES2020 BigInt

    // End of interpreter bytecodes
    TRY,
    SEMI, // semicolon
    LB, // left and right brackets
    RB,
    LC, // left and right curlies (braces)
    RC,
    LP, // left and right parentheses
    RP,
    COMMA, // comma operator

    ASSIGN, // simple assignment  (=)
    ASSIGN_BITOR, // |=
    ASSIGN_LOGICAL_OR, // ||=
    ASSIGN_BITXOR, // ^=
    ASSIGN_BITAND, // |=
    ASSIGN_LOGICAL_AND, // &&=
    ASSIGN_LSH, // <<=
    ASSIGN_RSH, // >>=
    ASSIGN_URSH, // >>>=
    ASSIGN_ADD, // +=
    ASSIGN_SUB, // -=
    ASSIGN_MUL, // *=
    ASSIGN_DIV, // /=
    ASSIGN_MOD, // %=
    ASSIGN_EXP, // **=

    HOOK, // conditional (?:)
    COLON,
    OR, // logical or (||)
    AND, // logical and (&&)
    INC, // increment/decrement (++ --)
    DEC,
    DOT, // member operator (.)
    FUNCTION, // function keyword
    EXPORT, // export keyword
    IMPORT, // import keyword
    IF, // if keyword
    ELSE, // else keyword
    SWITCH, // switch keyword
    CASE, // case keyword
    DEFAULT, // default keyword
    WHILE, // while keyword
    DO, // do keyword
    FOR, // for keyword
    BREAK, // break keyword
    CONTINUE, // continue keyword
    VAR, // var keyword
    WITH, // with keyword
    CATCH, // catch keyword
    FINALLY, // finally keyword
    VOID, // void keyword
    RESERVED, // reserved keywords
    EMPTY,
    COMPUTED_PROPERTY, // computed property in object initializer [x]

    /* types used for the parse tree - these never get returned
     * by the scanner.
     */

    BLOCK, // statement block
    LABEL, // label
    TARGET,
    LOOP,
    EXPR_VOID, // expression statement in functions
    EXPR_RESULT, // expression statement in scripts
    JSR,
    SCRIPT, // top-level node for entire script
    TYPEOFNAME, // for typeof(simple-name)
    USE_STACK,
    SETPROP_OP, // x.y op= something
    SETELEM_OP, // x[y] op= something
    LOCAL_BLOCK,
    SET_REF_OP, // *reference op= something

    // For XML support:
    DOTDOT, // member operator (..)
    COLONCOLON, // namespace::name
    XML, // XML type
    DOTQUERY, // .() -- e.g., x.emps.emp.(name == "terry")
    XMLATTR, // @
    XMLEND,

    // Optimizer-only-tokens
    TO_OBJECT,
    TO_DOUBLE,
    GET, // JS 1.5 get pseudo keyword
    SET, // JS 1.5 set pseudo keyword
    LET, // JS 1.7 let pseudo keyword
    CONST,
    SETCONST,
    SETCONSTVAR,
    ARRAYCOMP, // array comprehension
    LETEXPR,
    WITHEXPR,
    DEBUGGER,
    COMMENT,
    GENEXPR,
    METHOD, // ES6 MethodDefinition
    ARROW, // ES6 ArrowFunction
    YIELD_STAR, // ES6 "yield *", a specialization of yield
    TEMPLATE_LITERAL, // template literal
    TEMPLATE_CHARS, // template literal - literal section
    TEMPLATE_LITERAL_SUBST, // template literal - substitution
    TAGGED_TEMPLATE_LITERAL, // template literal - tagged/handler
    DOTDOTDOT, // spread/rest ...
    NULLISH_COALESCING,
    ;

    public static enum CommentType {
        LINE,
        BLOCK_COMMENT,
        JSDOC,
        HTML
    }

    // debug flags
    public static final boolean printTrees = false;
    static final boolean printICode = false;
    static final boolean printNames = printTrees || printICode;

    static Token FIRST_BYTECODE_TOKEN = ENTERWITH;
    static Token LAST_BYTECODE_TOKEN = BIGINT;
    public static final Token FIRST_ASSIGN = ASSIGN;
    public static final Token LAST_ASSIGN = ASSIGN_EXP;

    /**
     * Returns a name for the token. If Rhino is compiled with certain hardcoded debugging flags in
     * this file, it calls {@code #typeToName}; otherwise it returns a string whose value is the
     * token number.
     */
    public static String name(Token token) {
        if (!printNames) {
            return String.valueOf(token);
        }
        return typeToName(token);
    }

    /**
     * Always returns a human-readable string for the token name. For instance, {@link #FINALLY} has
     * the name "FINALLY".
     *
     * @param token the token code
     * @return the actual name for the token code
     */
    public static String typeToName(Token token) {
        switch (token) {
            case ERROR:
                return "ERROR";
            case EOF:
                return "EOF";
            case EOL:
                return "EOL";
            case ENTERWITH:
                return "ENTERWITH";
            case LEAVEWITH:
                return "LEAVEWITH";
            case RETURN:
                return "RETURN";
            case GOTO:
                return "GOTO";
            case IFEQ:
                return "IFEQ";
            case IFNE:
                return "IFNE";
            case SETNAME:
                return "SETNAME";
            case STRICT_SETNAME:
                return "STRICT_SETNAME";
            case BITOR:
                return "BITOR";
            case BITXOR:
                return "BITXOR";
            case BITAND:
                return "BITAND";
            case EQ:
                return "EQ";
            case NE:
                return "NE";
            case LT:
                return "LT";
            case LE:
                return "LE";
            case GT:
                return "GT";
            case GE:
                return "GE";
            case LSH:
                return "LSH";
            case RSH:
                return "RSH";
            case URSH:
                return "URSH";
            case ADD:
                return "ADD";
            case SUB:
                return "SUB";
            case MUL:
                return "MUL";
            case DIV:
                return "DIV";
            case MOD:
                return "MOD";
            case NOT:
                return "NOT";
            case BITNOT:
                return "BITNOT";
            case POS:
                return "POS";
            case NEG:
                return "NEG";
            case NEW:
                return "NEW";
            case DELPROP:
                return "DELPROP";
            case TYPEOF:
                return "TYPEOF";
            case GETPROP:
                return "GETPROP";
            case GETPROPNOWARN:
                return "GETPROPNOWARN";
            case SETPROP:
                return "SETPROP";
            case GETELEM:
                return "GETELEM";
            case SETELEM:
                return "SETELEM";
            case CALL:
                return "CALL";
            case NAME:
                return "NAME";
            case NUMBER:
                return "NUMBER";
            case STRING:
                return "STRING";
            case NULL:
                return "NULL";
            case THIS:
                return "THIS";
            case FALSE:
                return "FALSE";
            case TRUE:
                return "TRUE";
            case SHEQ:
                return "SHEQ";
            case SHNE:
                return "SHNE";
            case REGEXP:
                return "REGEXP";
            case BINDNAME:
                return "BINDNAME";
            case THROW:
                return "THROW";
            case RETHROW:
                return "RETHROW";
            case IN:
                return "IN";
            case INSTANCEOF:
                return "INSTANCEOF";
            case LOCAL_LOAD:
                return "LOCAL_LOAD";
            case GETVAR:
                return "GETVAR";
            case SETVAR:
                return "SETVAR";
            case CATCH_SCOPE:
                return "CATCH_SCOPE";
            case ENUM_INIT_KEYS:
                return "ENUM_INIT_KEYS";
            case ENUM_INIT_VALUES:
                return "ENUM_INIT_VALUES";
            case ENUM_INIT_ARRAY:
                return "ENUM_INIT_ARRAY";
            case ENUM_INIT_VALUES_IN_ORDER:
                return "ENUM_INIT_VALUES_IN_ORDER";
            case ENUM_NEXT:
                return "ENUM_NEXT";
            case ENUM_ID:
                return "ENUM_ID";
            case THISFN:
                return "THISFN";
            case RETURN_RESULT:
                return "RETURN_RESULT";
            case ARRAYLIT:
                return "ARRAYLIT";
            case OBJECTLIT:
                return "OBJECTLIT";
            case GET_REF:
                return "GET_REF";
            case SET_REF:
                return "SET_REF";
            case DEL_REF:
                return "DEL_REF";
            case REF_CALL:
                return "REF_CALL";
            case REF_SPECIAL:
                return "REF_SPECIAL";
            case DEFAULTNAMESPACE:
                return "DEFAULTNAMESPACE";
            case ESCXMLTEXT:
                return "ESCXMLTEXT";
            case ESCXMLATTR:
                return "ESCXMLATTR";
            case REF_MEMBER:
                return "REF_MEMBER";
            case REF_NS_MEMBER:
                return "REF_NS_MEMBER";
            case REF_NAME:
                return "REF_NAME";
            case REF_NS_NAME:
                return "REF_NS_NAME";
            case TRY:
                return "TRY";
            case SEMI:
                return "SEMI";
            case LB:
                return "LB";
            case RB:
                return "RB";
            case LC:
                return "LC";
            case RC:
                return "RC";
            case LP:
                return "LP";
            case RP:
                return "RP";
            case COMMA:
                return "COMMA";
            case ASSIGN:
                return "ASSIGN";
            case ASSIGN_BITOR:
                return "ASSIGN_BITOR";
            case ASSIGN_LOGICAL_OR:
                return "ASSIGN_LOGICAL_OR";
            case ASSIGN_BITXOR:
                return "ASSIGN_BITXOR";
            case ASSIGN_BITAND:
                return "ASSIGN_BITAND";
            case ASSIGN_LOGICAL_AND:
                return "ASSIGN_LOGICAL_AND";
            case ASSIGN_LSH:
                return "ASSIGN_LSH";
            case ASSIGN_RSH:
                return "ASSIGN_RSH";
            case ASSIGN_URSH:
                return "ASSIGN_URSH";
            case ASSIGN_ADD:
                return "ASSIGN_ADD";
            case ASSIGN_SUB:
                return "ASSIGN_SUB";
            case ASSIGN_MUL:
                return "ASSIGN_MUL";
            case ASSIGN_DIV:
                return "ASSIGN_DIV";
            case ASSIGN_MOD:
                return "ASSIGN_MOD";
            case ASSIGN_EXP:
                return "ASSIGN_EXP";
            case HOOK:
                return "HOOK";
            case COLON:
                return "COLON";
            case OR:
                return "OR";
            case NULLISH_COALESCING:
                return "NULLISH_COALESCING";
            case AND:
                return "AND";
            case INC:
                return "INC";
            case DEC:
                return "DEC";
            case DOT:
                return "DOT";
            case FUNCTION:
                return "FUNCTION";
            case EXPORT:
                return "EXPORT";
            case IMPORT:
                return "IMPORT";
            case IF:
                return "IF";
            case ELSE:
                return "ELSE";
            case SWITCH:
                return "SWITCH";
            case CASE:
                return "CASE";
            case DEFAULT:
                return "DEFAULT";
            case WHILE:
                return "WHILE";
            case DO:
                return "DO";
            case FOR:
                return "FOR";
            case BREAK:
                return "BREAK";
            case CONTINUE:
                return "CONTINUE";
            case VAR:
                return "VAR";
            case WITH:
                return "WITH";
            case CATCH:
                return "CATCH";
            case FINALLY:
                return "FINALLY";
            case VOID:
                return "VOID";
            case RESERVED:
                return "RESERVED";
            case EMPTY:
                return "EMPTY";
            case COMPUTED_PROPERTY:
                return "COMPUTED_PROPERTY";
            case BLOCK:
                return "BLOCK";
            case LABEL:
                return "LABEL";
            case TARGET:
                return "TARGET";
            case LOOP:
                return "LOOP";
            case EXPR_VOID:
                return "EXPR_VOID";
            case EXPR_RESULT:
                return "EXPR_RESULT";
            case JSR:
                return "JSR";
            case SCRIPT:
                return "SCRIPT";
            case TYPEOFNAME:
                return "TYPEOFNAME";
            case USE_STACK:
                return "USE_STACK";
            case SETPROP_OP:
                return "SETPROP_OP";
            case SETELEM_OP:
                return "SETELEM_OP";
            case LOCAL_BLOCK:
                return "LOCAL_BLOCK";
            case SET_REF_OP:
                return "SET_REF_OP";
            case DOTDOT:
                return "DOTDOT";
            case COLONCOLON:
                return "COLONCOLON";
            case XML:
                return "XML";
            case DOTQUERY:
                return "DOTQUERY";
            case XMLATTR:
                return "XMLATTR";
            case XMLEND:
                return "XMLEND";
            case TO_OBJECT:
                return "TO_OBJECT";
            case TO_DOUBLE:
                return "TO_DOUBLE";
            case GET:
                return "GET";
            case SET:
                return "SET";
            case LET:
                return "LET";
            case YIELD:
                return "YIELD";
            case EXP:
                return "EXP";
            case CONST:
                return "CONST";
            case SETCONST:
                return "SETCONST";
            case ARRAYCOMP:
                return "ARRAYCOMP";
            case WITHEXPR:
                return "WITHEXPR";
            case LETEXPR:
                return "LETEXPR";
            case DEBUGGER:
                return "DEBUGGER";
            case COMMENT:
                return "COMMENT";
            case GENEXPR:
                return "GENEXPR";
            case METHOD:
                return "METHOD";
            case ARROW:
                return "ARROW";
            case YIELD_STAR:
                return "YIELD_STAR";
            case BIGINT:
                return "BIGINT";
            case TEMPLATE_LITERAL:
                return "TEMPLATE_LITERAL";
            case TEMPLATE_CHARS:
                return "TEMPLATE_CHARS";
            case TEMPLATE_LITERAL_SUBST:
                return "TEMPLATE_LITERAL_SUBST";
            case TAGGED_TEMPLATE_LITERAL:
                return "TAGGED_TEMPLATE_LITERAL";
        }

        // Token without name
        throw new IllegalStateException(String.valueOf(token));
    }

    /**
     * Convert a keyword token to a name string for use with the {@link
     * Context#FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER} feature.
     *
     * @param token A token
     * @return the corresponding name string
     */
    public static String keywordToName(Token token) {
        switch (token) {
            case BREAK:
                return "break";
            case CASE:
                return "case";
            case CONTINUE:
                return "continue";
            case DEFAULT:
                return "default";
            case DELPROP:
                return "delete";
            case DO:
                return "do";
            case ELSE:
                return "else";
            case FALSE:
                return "false";
            case FOR:
                return "for";
            case FUNCTION:
                return "function";
            case IF:
                return "if";
            case IN:
                return "in";
            case LET:
                return "let";
            case NEW:
                return "new";
            case NULL:
                return "null";
            case RETURN:
                return "return";
            case SWITCH:
                return "switch";
            case THIS:
                return "this";
            case TRUE:
                return "true";
            case TYPEOF:
                return "typeof";
            case VAR:
                return "var";
            case VOID:
                return "void";
            case WHILE:
                return "while";
            case WITH:
                return "with";
            case YIELD:
                return "yield";
            case CATCH:
                return "catch";
            case CONST:
                return "const";
            case DEBUGGER:
                return "debugger";
            case FINALLY:
                return "finally";
            case INSTANCEOF:
                return "instanceof";
            case THROW:
                return "throw";
            case TRY:
                return "try";
            default:
                return null;
        }
    }

    /**
     * Return true if the passed code is a valid Token constant.
     *
     * @param code a potential token code
     * @return true if it's a known token
     */
    public static boolean isValidToken(Token code) {
        return code.ordinal() >= ERROR.ordinal();
    }
}
