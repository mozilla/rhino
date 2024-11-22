/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/** Additional interpreter-specific codes */
abstract class Icode {

    static final int

            // delete operator used on a name
            Icode_DELNAME = 0,

            // Stack: ... value1 -> ... value1 value1
            Icode_DUP = Icode_DELNAME - 1,

            // Stack: ... value2 value1 -> ... value2 value1 value2 value1
            Icode_DUP2 = Icode_DUP - 1,

            // Stack: ... value2 value1 -> ... value1 value2
            Icode_SWAP = Icode_DUP2 - 1,

            // Stack: ... value1 -> ...
            Icode_POP = Icode_SWAP - 1,

            // Store stack top into return register and then pop it
            Icode_POP_RESULT = Icode_POP - 1,

            // To jump conditionally and pop additional stack value
            Icode_IFEQ_POP = Icode_POP_RESULT - 1,

            // various types of ++/--
            Icode_VAR_INC_DEC = Icode_IFEQ_POP - 1,
            Icode_NAME_INC_DEC = Icode_VAR_INC_DEC - 1,
            Icode_PROP_INC_DEC = Icode_NAME_INC_DEC - 1,
            Icode_ELEM_INC_DEC = Icode_PROP_INC_DEC - 1,
            Icode_REF_INC_DEC = Icode_ELEM_INC_DEC - 1,

            // load/save scope from/to local
            Icode_SCOPE_LOAD = Icode_REF_INC_DEC - 1,
            Icode_SCOPE_SAVE = Icode_SCOPE_LOAD - 1,
            Icode_TYPEOFNAME = Icode_SCOPE_SAVE - 1,

            // helper for function calls
            Icode_NAME_AND_THIS = Icode_TYPEOFNAME - 1,
            Icode_PROP_AND_THIS = Icode_NAME_AND_THIS - 1,
            Icode_ELEM_AND_THIS = Icode_PROP_AND_THIS - 1,
            Icode_VALUE_AND_THIS = Icode_ELEM_AND_THIS - 1,
            Icode_NAME_AND_THIS_OPTIONAL = Icode_VALUE_AND_THIS - 1,
            Icode_PROP_AND_THIS_OPTIONAL = Icode_NAME_AND_THIS_OPTIONAL - 1,
            Icode_ELEM_AND_THIS_OPTIONAL = Icode_PROP_AND_THIS_OPTIONAL - 1,
            Icode_VALUE_AND_THIS_OPTIONAL = Icode_ELEM_AND_THIS_OPTIONAL - 1,

            // Create closure object for nested functions
            Icode_CLOSURE_EXPR = Icode_VALUE_AND_THIS_OPTIONAL - 1,
            Icode_CLOSURE_STMT = Icode_CLOSURE_EXPR - 1,

            // Special calls
            Icode_CALLSPECIAL = Icode_CLOSURE_STMT - 1,
            Icode_CALLSPECIAL_OPTIONAL = Icode_CALLSPECIAL - 1,

            // To return undefined value
            Icode_RETUNDEF = Icode_CALLSPECIAL_OPTIONAL - 1,

            // Exception handling implementation
            Icode_GOSUB = Icode_RETUNDEF - 1,
            Icode_STARTSUB = Icode_GOSUB - 1,
            Icode_RETSUB = Icode_STARTSUB - 1,

            // To indicating a line number change in icodes.
            Icode_LINE = Icode_RETSUB - 1,

            // To store shorts and ints inline
            Icode_SHORTNUMBER = Icode_LINE - 1,
            Icode_INTNUMBER = Icode_SHORTNUMBER - 1,

            // To create and populate array to hold values for [] and {} literals
            Icode_LITERAL_NEW_OBJECT = Icode_INTNUMBER - 1,
            Icode_LITERAL_NEW_ARRAY = Icode_LITERAL_NEW_OBJECT - 1,
            Icode_LITERAL_SET = Icode_LITERAL_NEW_ARRAY - 1,
            ICode_FN_STORE_HOME_OBJECT = Icode_LITERAL_SET - 1,

            // Array literal with skipped index like [1,,2]
            Icode_SPARE_ARRAYLIT = ICode_FN_STORE_HOME_OBJECT - 1,

            // Load index register to prepare for the following index operation
            Icode_REG_IND_C0 = Icode_SPARE_ARRAYLIT - 1,
            Icode_REG_IND_C1 = Icode_REG_IND_C0 - 1,
            Icode_REG_IND_C2 = Icode_REG_IND_C1 - 1,
            Icode_REG_IND_C3 = Icode_REG_IND_C2 - 1,
            Icode_REG_IND_C4 = Icode_REG_IND_C3 - 1,
            Icode_REG_IND_C5 = Icode_REG_IND_C4 - 1,
            Icode_REG_IND1 = Icode_REG_IND_C5 - 1,
            Icode_REG_IND2 = Icode_REG_IND1 - 1,
            Icode_REG_IND4 = Icode_REG_IND2 - 1,

            // Load string register to prepare for the following string operation
            Icode_REG_STR_C0 = Icode_REG_IND4 - 1,
            Icode_REG_STR_C1 = Icode_REG_STR_C0 - 1,
            Icode_REG_STR_C2 = Icode_REG_STR_C1 - 1,
            Icode_REG_STR_C3 = Icode_REG_STR_C2 - 1,
            Icode_REG_STR1 = Icode_REG_STR_C3 - 1,
            Icode_REG_STR2 = Icode_REG_STR1 - 1,
            Icode_REG_STR4 = Icode_REG_STR2 - 1,

            // Version of getvar/setvar that read var index directly from bytecode
            Icode_GETVAR1 = Icode_REG_STR4 - 1,
            Icode_SETVAR1 = Icode_GETVAR1 - 1,

            // Load undefined
            Icode_UNDEF = Icode_SETVAR1 - 1,
            Icode_ZERO = Icode_UNDEF - 1,
            Icode_ONE = Icode_ZERO - 1,

            // entrance and exit from .()
            Icode_ENTERDQ = Icode_ONE - 1,
            Icode_LEAVEDQ = Icode_ENTERDQ - 1,
            Icode_TAIL_CALL = Icode_LEAVEDQ - 1,

            // Clear local to allow GC its context
            Icode_LOCAL_CLEAR = Icode_TAIL_CALL - 1,

            // Literal get/set
            Icode_LITERAL_GETTER = Icode_LOCAL_CLEAR - 1,
            Icode_LITERAL_SETTER = Icode_LITERAL_GETTER - 1,

            // const
            Icode_SETCONST = Icode_LITERAL_SETTER - 1,
            Icode_SETCONSTVAR = Icode_SETCONST - 1,
            Icode_SETCONSTVAR1 = Icode_SETCONSTVAR - 1,

            // Generator opcodes (along with Token.YIELD)
            Icode_GENERATOR = Icode_SETCONSTVAR1 - 1,
            Icode_GENERATOR_END = Icode_GENERATOR - 1,
            Icode_DEBUGGER = Icode_GENERATOR_END - 1,
            Icode_GENERATOR_RETURN = Icode_DEBUGGER - 1,
            Icode_YIELD_STAR = Icode_GENERATOR_RETURN - 1,

            // Load BigInt register to prepare for the following BigInt operation
            Icode_REG_BIGINT_C0 = Icode_YIELD_STAR - 1,
            Icode_REG_BIGINT_C1 = Icode_REG_BIGINT_C0 - 1,
            Icode_REG_BIGINT_C2 = Icode_REG_BIGINT_C1 - 1,
            Icode_REG_BIGINT_C3 = Icode_REG_BIGINT_C2 - 1,
            Icode_REG_BIGINT1 = Icode_REG_BIGINT_C3 - 1,
            Icode_REG_BIGINT2 = Icode_REG_BIGINT1 - 1,
            Icode_REG_BIGINT4 = Icode_REG_BIGINT2 - 1,

            // Call to GetTemplateLiteralCallSite
            Icode_TEMPLATE_LITERAL_CALLSITE = Icode_REG_BIGINT4 - 1,
            Icode_LITERAL_KEY_SET = Icode_TEMPLATE_LITERAL_CALLSITE - 1,

            // Jump if stack head is null or undefined
            Icode_IF_NULL_UNDEF = Icode_LITERAL_KEY_SET - 1,
            Icode_IF_NOT_NULL_UNDEF = Icode_IF_NULL_UNDEF - 1,

            // Call a method on the super object, i.e. super.foo()
            Icode_CALL_ON_SUPER = Icode_IF_NOT_NULL_UNDEF - 1,

            // delete super.prop
            Icode_DELPROP_SUPER = Icode_CALL_ON_SUPER - 1,

            // Last icode
            MIN_ICODE = Icode_DELPROP_SUPER;

    static String bytecodeName(int bytecode) {
        if (!validBytecode(bytecode)) {
            throw new IllegalArgumentException(String.valueOf(bytecode));
        }

        if (!Token.printICode) {
            return String.valueOf(bytecode);
        }

        if (validTokenCode(bytecode)) {
            return Token.name(bytecode);
        }

        switch (bytecode) {
            case Icode_DELNAME:
                return "DELNAME";
            case Icode_DUP:
                return "DUP";
            case Icode_DUP2:
                return "DUP2";
            case Icode_SWAP:
                return "SWAP";
            case Icode_POP:
                return "POP";
            case Icode_POP_RESULT:
                return "POP_RESULT";
            case Icode_IFEQ_POP:
                return "IFEQ_POP";
            case Icode_VAR_INC_DEC:
                return "VAR_INC_DEC";
            case Icode_NAME_INC_DEC:
                return "NAME_INC_DEC";
            case Icode_PROP_INC_DEC:
                return "PROP_INC_DEC";
            case Icode_ELEM_INC_DEC:
                return "ELEM_INC_DEC";
            case Icode_REF_INC_DEC:
                return "REF_INC_DEC";
            case Icode_SCOPE_LOAD:
                return "SCOPE_LOAD";
            case Icode_SCOPE_SAVE:
                return "SCOPE_SAVE";
            case Icode_TYPEOFNAME:
                return "TYPEOFNAME";
            case Icode_NAME_AND_THIS:
                return "NAME_AND_THIS";
            case Icode_PROP_AND_THIS:
                return "PROP_AND_THIS";
            case Icode_ELEM_AND_THIS:
                return "ELEM_AND_THIS";
            case Icode_VALUE_AND_THIS:
                return "VALUE_AND_THIS";
            case Icode_NAME_AND_THIS_OPTIONAL:
                return "NAME_AND_THIS_OPTIONAL";
            case Icode_PROP_AND_THIS_OPTIONAL:
                return "PROP_AND_THIS_OPTIONAL";
            case Icode_ELEM_AND_THIS_OPTIONAL:
                return "ELEM_AND_THIS_OPTIONAL";
            case Icode_VALUE_AND_THIS_OPTIONAL:
                return "VALUE_AND_THIS_OPTIONAL";
            case Icode_CLOSURE_EXPR:
                return "CLOSURE_EXPR";
            case Icode_CLOSURE_STMT:
                return "CLOSURE_STMT";
            case Icode_CALLSPECIAL:
                return "CALLSPECIAL";
            case Icode_CALLSPECIAL_OPTIONAL:
                return "CALLSPECIAL_OPTIONAL";
            case Icode_RETUNDEF:
                return "RETUNDEF";
            case Icode_GOSUB:
                return "GOSUB";
            case Icode_STARTSUB:
                return "STARTSUB";
            case Icode_RETSUB:
                return "RETSUB";
            case Icode_LINE:
                return "LINE";
            case Icode_SHORTNUMBER:
                return "SHORTNUMBER";
            case Icode_INTNUMBER:
                return "INTNUMBER";
            case Icode_LITERAL_NEW_OBJECT:
                return "LITERAL_NEW_OBJECT";
            case Icode_LITERAL_NEW_ARRAY:
                return "LITERAL_NEW_ARRAY";
            case Icode_LITERAL_SET:
                return "LITERAL_SET";
            case ICode_FN_STORE_HOME_OBJECT:
                return "FN_STORE_HOME_OBJECT";
            case Icode_SPARE_ARRAYLIT:
                return "SPARE_ARRAYLIT";
            case Icode_REG_IND_C0:
                return "REG_IND_C0";
            case Icode_REG_IND_C1:
                return "REG_IND_C1";
            case Icode_REG_IND_C2:
                return "REG_IND_C2";
            case Icode_REG_IND_C3:
                return "REG_IND_C3";
            case Icode_REG_IND_C4:
                return "REG_IND_C4";
            case Icode_REG_IND_C5:
                return "REG_IND_C5";
            case Icode_REG_IND1:
                return "LOAD_IND1";
            case Icode_REG_IND2:
                return "LOAD_IND2";
            case Icode_REG_IND4:
                return "LOAD_IND4";
            case Icode_REG_STR_C0:
                return "REG_STR_C0";
            case Icode_REG_STR_C1:
                return "REG_STR_C1";
            case Icode_REG_STR_C2:
                return "REG_STR_C2";
            case Icode_REG_STR_C3:
                return "REG_STR_C3";
            case Icode_REG_STR1:
                return "LOAD_STR1";
            case Icode_REG_STR2:
                return "LOAD_STR2";
            case Icode_REG_STR4:
                return "LOAD_STR4";
            case Icode_GETVAR1:
                return "GETVAR1";
            case Icode_SETVAR1:
                return "SETVAR1";
            case Icode_UNDEF:
                return "UNDEF";
            case Icode_ZERO:
                return "ZERO";
            case Icode_ONE:
                return "ONE";
            case Icode_ENTERDQ:
                return "ENTERDQ";
            case Icode_LEAVEDQ:
                return "LEAVEDQ";
            case Icode_TAIL_CALL:
                return "TAIL_CALL";
            case Icode_LOCAL_CLEAR:
                return "LOCAL_CLEAR";
            case Icode_LITERAL_GETTER:
                return "LITERAL_GETTER";
            case Icode_LITERAL_SETTER:
                return "LITERAL_SETTER";
            case Icode_SETCONST:
                return "SETCONST";
            case Icode_SETCONSTVAR:
                return "SETCONSTVAR";
            case Icode_SETCONSTVAR1:
                return "SETCONSTVAR1";
            case Icode_GENERATOR:
                return "GENERATOR";
            case Icode_GENERATOR_END:
                return "GENERATOR_END";
            case Icode_DEBUGGER:
                return "DEBUGGER";
            case Icode_GENERATOR_RETURN:
                return "GENERATOR_RETURN";
            case Icode_YIELD_STAR:
                return "YIELD_STAR";
            case Icode_REG_BIGINT_C0:
                return "REG_BIGINT_C0";
            case Icode_REG_BIGINT_C1:
                return "REG_BIGINT_C1";
            case Icode_REG_BIGINT_C2:
                return "REG_BIGINT_C2";
            case Icode_REG_BIGINT_C3:
                return "REG_BIGINT_C3";
            case Icode_REG_BIGINT1:
                return "LOAD_BIGINT1";
            case Icode_REG_BIGINT2:
                return "LOAD_BIGINT2";
            case Icode_REG_BIGINT4:
                return "LOAD_BIGINT4";
            case Icode_TEMPLATE_LITERAL_CALLSITE:
                return "TEMPLATE_LITERAL_CALLSITE";
            case Icode_LITERAL_KEY_SET:
                return "LITERAL_KEY_SET";
            case Icode_IF_NULL_UNDEF:
                return "IF_NULL_UNDEF";
            case Icode_IF_NOT_NULL_UNDEF:
                return "IF_NOT_NULL_UNDEF";
            case Icode_CALL_ON_SUPER:
                return "CALL_ON_SUPER";
            case Icode_DELPROP_SUPER:
                return "DELPROP_SUPER";
        }

        // icode without name
        throw new IllegalStateException(String.valueOf(bytecode));
    }

    static boolean validIcode(int icode) {
        return MIN_ICODE <= icode && icode <= 0;
    }

    static boolean validTokenCode(int token) {
        return Token.FIRST_BYTECODE_TOKEN <= token && token <= Token.LAST_BYTECODE_TOKEN;
    }

    static boolean validBytecode(int bytecode) {
        return validIcode(bytecode) || validTokenCode(bytecode);
    }
}
