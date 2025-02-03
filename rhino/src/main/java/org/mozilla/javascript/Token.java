/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import org.mozilla.javascript.config.RhinoConfig;

/**
 * This class implements the JavaScript scanner.
 *
 * <p>It is based on the C source files jsscan.c and jsscan.h in the jsref package.
 *
 * @see org.mozilla.javascript.Parser
 * @author Mike McCabe
 * @author Brendan Eich
 */
public class Token {
    public static enum CommentType {
        LINE,
        BLOCK_COMMENT,
        JSDOC,
        HTML
    }

    // debug flags
    public static final boolean printTrees = RhinoConfig.get("rhino.printTrees", false);
    static final boolean printICode = RhinoConfig.get("rhino.printICode", false);
    static final boolean printNames = printTrees || printICode;

    /** Token types. These values correspond to JSTokenType values in jsscan.c. */
    public static final int
            // start enum
            ERROR = -1, // well-known as the only code < EOF
            FIRST_TOKEN = ERROR,
            EOF = ERROR + 1, // end of file token - (not EOF_CHAR)
            EOL = EOF + 1, // end of line

            // Interpreter reuses the following as bytecodes
            FIRST_BYTECODE_TOKEN = EOL + 1,
            ENTERWITH = FIRST_BYTECODE_TOKEN,
            LEAVEWITH = ENTERWITH + 1,
            RETURN = LEAVEWITH + 1,
            GOTO = RETURN + 1,
            IFEQ = GOTO + 1,
            IFNE = IFEQ + 1,
            SETNAME = IFNE + 1,
            BITOR = SETNAME + 1,
            BITXOR = BITOR + 1,
            BITAND = BITXOR + 1,
            EQ = BITAND + 1,
            NE = EQ + 1,
            LT = NE + 1,
            LE = LT + 1,
            GT = LE + 1,
            GE = GT + 1,
            LSH = GE + 1,
            RSH = LSH + 1,
            URSH = RSH + 1,
            ADD = URSH + 1,
            SUB = ADD + 1,
            MUL = SUB + 1,
            DIV = MUL + 1,
            MOD = DIV + 1,
            NOT = MOD + 1,
            BITNOT = NOT + 1,
            POS = BITNOT + 1,
            NEG = POS + 1,
            NEW = NEG + 1,
            DELPROP = NEW + 1,
            TYPEOF = DELPROP + 1,
            GETPROP = TYPEOF + 1,
            GETPROPNOWARN = GETPROP + 1,
            GETPROP_SUPER = GETPROPNOWARN + 1,
            GETPROPNOWARN_SUPER = GETPROP_SUPER + 1,
            SETPROP = GETPROPNOWARN_SUPER + 1,
            SETPROP_SUPER = SETPROP + 1,
            GETELEM = SETPROP_SUPER + 1,
            GETELEM_SUPER = GETELEM + 1,
            SETELEM = GETELEM_SUPER + 1,
            SETELEM_SUPER = SETELEM + 1,
            CALL = SETELEM_SUPER + 1,
            NAME = CALL + 1,
            NUMBER = NAME + 1,
            STRING = NUMBER + 1,
            NULL = STRING + 1,
            THIS = NULL + 1,
            FALSE = THIS + 1,
            TRUE = FALSE + 1,
            SHEQ = TRUE + 1, // shallow equality (===)
            SHNE = SHEQ + 1, // shallow inequality (!==)
            REGEXP = SHNE + 1,
            BINDNAME = REGEXP + 1,
            THROW = BINDNAME + 1,
            RETHROW = THROW + 1, // rethrow caught exception: catch (e if ) use it
            IN = RETHROW + 1,
            INSTANCEOF = IN + 1,
            LOCAL_LOAD = INSTANCEOF + 1,
            GETVAR = LOCAL_LOAD + 1,
            SETVAR = GETVAR + 1,
            CATCH_SCOPE = SETVAR + 1,
            ENUM_INIT_KEYS = CATCH_SCOPE + 1,
            ENUM_INIT_VALUES = ENUM_INIT_KEYS + 1,
            ENUM_INIT_ARRAY = ENUM_INIT_VALUES + 1,
            ENUM_INIT_VALUES_IN_ORDER = ENUM_INIT_ARRAY + 1,
            ENUM_NEXT = ENUM_INIT_VALUES_IN_ORDER + 1,
            ENUM_ID = ENUM_NEXT + 1,
            THISFN = ENUM_ID + 1,
            RETURN_RESULT = THISFN + 1, // to return previously stored return result
            ARRAYLIT = RETURN_RESULT + 1, // array literal
            OBJECTLIT = ARRAYLIT + 1, // object literal
            GET_REF = OBJECTLIT + 1, // *reference
            SET_REF = GET_REF + 1, // *reference    = something
            DEL_REF = SET_REF + 1, // delete reference
            REF_CALL = DEL_REF + 1, // f(args)    = something or f(args)++
            REF_SPECIAL = REF_CALL + 1, // reference for special properties like __proto
            YIELD = REF_SPECIAL + 1, // JS 1.7 yield pseudo keyword
            SUPER = YIELD + 1, // ES6 super keyword
            STRICT_SETNAME = SUPER + 1,
            EXP = STRICT_SETNAME + 1, // Exponentiation Operator

            // For XML support:
            DEFAULTNAMESPACE = EXP + 1, // default xml namespace =
            ESCXMLATTR = DEFAULTNAMESPACE + 1,
            ESCXMLTEXT = ESCXMLATTR + 1,
            REF_MEMBER = ESCXMLTEXT + 1, // Reference for x.@y, x..y etc.
            REF_NS_MEMBER = REF_MEMBER + 1, // Reference for x.ns::y, x..ns::y etc.
            REF_NAME = REF_NS_MEMBER + 1, // Reference for @y, @[y] etc.
            REF_NS_NAME = REF_NAME + 1, // Reference for ns::y, @ns::y@[y] etc.
            BIGINT = REF_NS_NAME + 1; // ES2020 BigInt

    // End of interpreter bytecodes
    public static final int LAST_BYTECODE_TOKEN = BIGINT,
            TRY = LAST_BYTECODE_TOKEN + 1,
            SEMI = TRY + 1, // semicolon
            LB = SEMI + 1, // left and right brackets
            RB = LB + 1,
            LC = RB + 1, // left and right curlies (braces)
            RC = LC + 1,
            LP = RC + 1, // left and right parentheses
            RP = LP + 1,
            COMMA = RP + 1, // comma operator
            ASSIGN = COMMA + 1, // simple assignment  (=)
            ASSIGN_BITOR = ASSIGN + 1, // |=
            ASSIGN_LOGICAL_OR = ASSIGN_BITOR + 1, // ||=
            ASSIGN_BITXOR = ASSIGN_LOGICAL_OR + 1, // ^=
            ASSIGN_BITAND = ASSIGN_BITXOR + 1, // |=
            ASSIGN_LOGICAL_AND = ASSIGN_BITAND + 1, // &&=
            ASSIGN_LSH = ASSIGN_LOGICAL_AND + 1, // <<=
            ASSIGN_RSH = ASSIGN_LSH + 1, // >>=
            ASSIGN_URSH = ASSIGN_RSH + 1, // >>>=
            ASSIGN_ADD = ASSIGN_URSH + 1, // +=
            ASSIGN_SUB = ASSIGN_ADD + 1, // -=
            ASSIGN_MUL = ASSIGN_SUB + 1, // *=
            ASSIGN_DIV = ASSIGN_MUL + 1, // /=
            ASSIGN_MOD = ASSIGN_DIV + 1, // %=
            ASSIGN_EXP = ASSIGN_MOD + 1, // **=
            ASSIGN_NULLISH = ASSIGN_EXP + 1; // ??=
    public static final int FIRST_ASSIGN = ASSIGN,
            LAST_ASSIGN = ASSIGN_NULLISH,
            HOOK = LAST_ASSIGN + 1, // conditional (?:)
            COLON = HOOK + 1,
            OR = COLON + 1, // logical or (||)
            AND = OR + 1, // logical and (&&)
            INC = AND + 1, // increment/decrement (++ --)
            DEC = INC + 1,
            DOT = DEC + 1, // member operator (.)
            FUNCTION = DOT + 1, // function keyword
            EXPORT = FUNCTION + 1, // export keyword
            IMPORT = EXPORT + 1, // import keyword
            IF = IMPORT + 1, // if keyword
            ELSE = IF + 1, // else keyword
            SWITCH = ELSE + 1, // switch keyword
            CASE = SWITCH + 1, // case keyword
            DEFAULT = CASE + 1, // default keyword
            WHILE = DEFAULT + 1, // while keyword
            DO = WHILE + 1, // do keyword
            FOR = DO + 1, // for keyword
            BREAK = FOR + 1, // break keyword
            CONTINUE = BREAK + 1, // continue keyword
            VAR = CONTINUE + 1, // var keyword
            WITH = VAR + 1, // with keyword
            CATCH = WITH + 1, // catch keyword
            FINALLY = CATCH + 1, // finally keyword
            VOID = FINALLY + 1, // void keyword
            RESERVED = VOID + 1, // reserved keywords
            EMPTY = RESERVED + 1,
            COMPUTED_PROPERTY = EMPTY + 1, // computed property in object initializer [x]

            /* types used for the parse tree - these never get returned
             * by the scanner.
             */

            BLOCK = COMPUTED_PROPERTY + 1, // statement block
            LABEL = BLOCK + 1, // label
            TARGET = LABEL + 1,
            LOOP = TARGET + 1,
            EXPR_VOID = LOOP + 1, // expression statement in functions
            EXPR_RESULT = EXPR_VOID + 1, // expression statement in scripts
            JSR = EXPR_RESULT + 1,
            SCRIPT = JSR + 1, // top-level node for entire script
            TYPEOFNAME = SCRIPT + 1, // for typeof(simple-name)
            USE_STACK = TYPEOFNAME + 1,
            SETPROP_OP = USE_STACK + 1, // x.y op= something
            SETELEM_OP = SETPROP_OP + 1, // x[y] op= something
            LOCAL_BLOCK = SETELEM_OP + 1,
            SET_REF_OP = LOCAL_BLOCK + 1, // *reference op= something

            // For XML support:
            DOTDOT = SET_REF_OP + 1, // member operator (..)
            COLONCOLON = DOTDOT + 1, // namespace::name
            XML = COLONCOLON + 1, // XML type
            DOTQUERY = XML + 1, // .() -- e.g., x.emps.emp.(name == "terry")
            XMLATTR = DOTQUERY + 1, // @
            XMLEND = XMLATTR + 1,

            // Optimizer-only-tokens
            TO_OBJECT = XMLEND + 1,
            TO_DOUBLE = TO_OBJECT + 1,
            GET = TO_DOUBLE + 1, // JS 1.5 get pseudo keyword
            SET = GET + 1, // JS 1.5 set pseudo keyword
            LET = SET + 1, // JS 1.7 let pseudo keyword
            CONST = LET + 1,
            SETCONST = CONST + 1,
            SETCONSTVAR = SETCONST + 1,
            ARRAYCOMP = SETCONSTVAR + 1, // array comprehension
            LETEXPR = ARRAYCOMP + 1,
            WITHEXPR = LETEXPR + 1,
            DEBUGGER = WITHEXPR + 1,
            COMMENT = DEBUGGER + 1,
            GENEXPR = COMMENT + 1,
            METHOD = GENEXPR + 1, // ES6 MethodDefinition
            ARROW = METHOD + 1, // ES6 ArrowFunction
            YIELD_STAR = ARROW + 1, // ES6 "yield *", a specialization of yield
            TEMPLATE_LITERAL = YIELD_STAR + 1, // template literal
            TEMPLATE_CHARS = TEMPLATE_LITERAL + 1, // template literal - literal section
            TEMPLATE_LITERAL_SUBST = TEMPLATE_CHARS + 1, // template literal - substitution
            TAGGED_TEMPLATE_LITERAL =
                    TEMPLATE_LITERAL_SUBST + 1, // template literal - tagged/handler
            DOTDOTDOT = TAGGED_TEMPLATE_LITERAL + 1, // spread/rest ...
            NULLISH_COALESCING = DOTDOTDOT + 1, // nullish coalescing (??)
            QUESTION_DOT = NULLISH_COALESCING + 1, // optional chaining operator (?.)
            LAST_TOKEN = QUESTION_DOT + 1;

    /**
     * Returns a name for the token. If Rhino is compiled with certain hardcoded debugging flags in
     * this file, it calls {@code #typeToName}; otherwise it returns a string whose value is the
     * token number.
     */
    public static String name(int token) {
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
    public static String typeToName(int token) {
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
            case GETPROP_SUPER:
                return "GETPROP_SUPER";
            case GETPROPNOWARN_SUPER:
                return "GETPROPNOWARN_SUPER";
            case SETPROP:
                return "SETPROP";
            case SETPROP_SUPER:
                return "SETPROP_SUPER";
            case GETELEM:
                return "GETELEM";
            case GETELEM_SUPER:
                return "GETELEM_SUPER";
            case SETELEM:
                return "SETELEM";
            case SETELEM_SUPER:
                return "SETELEM_SUPER";
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
            case ASSIGN_NULLISH:
                return "ASSIGN_NULLISH";
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
            case SUPER:
                return "SUPER";
            case EXP:
                return "EXP";
            case CONST:
                return "CONST";
            case SETCONST:
                return "SETCONST";
            case SETCONSTVAR:
                return "SETCONSTVAR";
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
            case DOTDOTDOT:
                return "DOTDOTDOT";
            case QUESTION_DOT:
                return "QUESTION_DOT";
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
    public static String keywordToName(int token) {
        switch (token) {
            case Token.BREAK:
                return "break";
            case Token.CASE:
                return "case";
            case Token.CONTINUE:
                return "continue";
            case Token.DEFAULT:
                return "default";
            case Token.DELPROP:
                return "delete";
            case Token.DO:
                return "do";
            case Token.ELSE:
                return "else";
            case Token.FALSE:
                return "false";
            case Token.FOR:
                return "for";
            case Token.FUNCTION:
                return "function";
            case Token.IF:
                return "if";
            case Token.IN:
                return "in";
            case Token.LET:
                return "let";
            case Token.NEW:
                return "new";
            case Token.NULL:
                return "null";
            case Token.RETURN:
                return "return";
            case Token.SWITCH:
                return "switch";
            case Token.THIS:
                return "this";
            case Token.TRUE:
                return "true";
            case Token.TYPEOF:
                return "typeof";
            case Token.VAR:
                return "var";
            case Token.VOID:
                return "void";
            case Token.WHILE:
                return "while";
            case Token.WITH:
                return "with";
            case Token.YIELD:
                return "yield";
            case Token.SUPER:
                return "super";
            case Token.CATCH:
                return "catch";
            case Token.CONST:
                return "const";
            case Token.DEBUGGER:
                return "debugger";
            case Token.FINALLY:
                return "finally";
            case Token.INSTANCEOF:
                return "instanceof";
            case Token.THROW:
                return "throw";
            case Token.TRY:
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
    public static boolean isValidToken(int code) {
        return code >= ERROR && code <= LAST_TOKEN;
    }
}
