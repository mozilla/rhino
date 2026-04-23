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
            Icode_METHOD_EXPR = Icode_LITERAL_SET - 1,

            // Array literal with skipped index like [1,,2]
            Icode_SPARE_ARRAYLIT = Icode_METHOD_EXPR - 1,

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

            // Call super() in a derived class constructor
            Icode_CONSTRUCT_SUPER = Icode_DELPROP_SUPER - 1,

            // Call super(...arr) in a derived class constructor. Top of stack is the
            // array-like whose elements are spread as the super constructor arguments.
            Icode_CONSTRUCT_SUPER_SPREAD = Icode_CONSTRUCT_SUPER - 1,

            // Class declaration with extends: stack has superclass, creates constructor with
            // homeObject
            Icode_CLASS_STMT = Icode_CONSTRUCT_SUPER_SPREAD - 1,

            // Class expression with extends: stack has superclass, creates constructor with
            // homeObject
            Icode_CLASS_EXPR = Icode_CLASS_STMT - 1,

            // spread
            Icode_SPREAD = Icode_CLASS_EXPR - 1,

            // object rest - create object excluding extracted keys
            Icode_OBJECT_REST = Icode_SPREAD - 1,

            // Wrap top-of-stack in an AwaitMarker (used inside async generator bodies so the
            // driver can distinguish await from yield).
            Icode_WRAP_AWAIT = Icode_OBJECT_REST - 1,

            // Define a method on a class prototype
            Icode_DEFINE_CLASS_METHOD = Icode_WRAP_AWAIT - 1,

            // Define a getter on a class prototype
            Icode_DEFINE_CLASS_GETTER = Icode_DEFINE_CLASS_METHOD - 1,

            // Define a setter on a class prototype
            Icode_DEFINE_CLASS_SETTER = Icode_DEFINE_CLASS_GETTER - 1,

            // Define a static method on a class constructor
            Icode_DEFINE_STATIC_CLASS_METHOD = Icode_DEFINE_CLASS_SETTER - 1,

            // Define a static getter on a class constructor
            Icode_DEFINE_STATIC_CLASS_GETTER = Icode_DEFINE_STATIC_CLASS_METHOD - 1,

            // Define a static setter on a class constructor
            Icode_DEFINE_STATIC_CLASS_SETTER = Icode_DEFINE_STATIC_CLASS_GETTER - 1,

            // Define a static named field on a class constructor (value on stack)
            Icode_DEFINE_STATIC_CLASS_FIELD = Icode_DEFINE_STATIC_CLASS_SETTER - 1,

            // Define a static computed field on a class constructor (key and value on stack)
            Icode_DEFINE_STATIC_CLASS_COMPUTED_FIELD = Icode_DEFINE_STATIC_CLASS_FIELD - 1,

            // Define a class private field: stack has target, symbol key, value -> ...
            Icode_DEFINE_PRIVATE_FIELD = Icode_DEFINE_STATIC_CLASS_COMPUTED_FIELD - 1,

            // Define a class private getter: stack has target, symbol key, fn -> ... fn
            Icode_DEFINE_PRIVATE_GETTER = Icode_DEFINE_PRIVATE_FIELD - 1,

            // Define a class private setter: stack has target, symbol key, fn -> ... fn
            Icode_DEFINE_PRIVATE_SETTER = Icode_DEFINE_PRIVATE_GETTER - 1,

            // Store instance computed field keys on a constructor.
            // Stack has constructor and `count` keys, with the constructor underneath.
            // indexReg holds `count`. After the op: ... constructor.
            Icode_STORE_CLASS_COMPUTED_KEYS = Icode_DEFINE_PRIVATE_SETTER - 1,

            // Push the i-th pre-evaluated instance computed field key of the currently
            // executing function onto the stack. indexReg holds the index.
            Icode_GET_CLASS_COMPUTED_KEY = Icode_STORE_CLASS_COMPUTED_KEYS - 1,

            // Last icode
            MIN_ICODE = Icode_GET_CLASS_COMPUTED_KEY;

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
            case Icode_METHOD_EXPR:
                return "METHOD_EXPR";
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
            case Icode_CONSTRUCT_SUPER:
                return "CONSTRUCT_SUPER";
            case Icode_CONSTRUCT_SUPER_SPREAD:
                return "CONSTRUCT_SUPER_SPREAD";
            case Icode_CLASS_STMT:
                return "CLASS_STMT";
            case Icode_CLASS_EXPR:
                return "CLASS_EXPR";
            case Icode_SPREAD:
                return "SPREAD";
            case Icode_OBJECT_REST:
                return "OBJECT_REST";
            case Icode_WRAP_AWAIT:
                return "WRAP_AWAIT";
            case Icode_DEFINE_CLASS_METHOD:
                return "DEFINE_CLASS_METHOD";
            case Icode_DEFINE_CLASS_GETTER:
                return "DEFINE_CLASS_GETTER";
            case Icode_DEFINE_CLASS_SETTER:
                return "DEFINE_CLASS_SETTER";
            case Icode_DEFINE_STATIC_CLASS_METHOD:
                return "DEFINE_STATIC_CLASS_METHOD";
            case Icode_DEFINE_STATIC_CLASS_GETTER:
                return "DEFINE_STATIC_CLASS_GETTER";
            case Icode_DEFINE_STATIC_CLASS_SETTER:
                return "DEFINE_STATIC_CLASS_SETTER";
            case Icode_DEFINE_STATIC_CLASS_FIELD:
                return "DEFINE_STATIC_CLASS_FIELD";
            case Icode_DEFINE_STATIC_CLASS_COMPUTED_FIELD:
                return "DEFINE_STATIC_CLASS_COMPUTED_FIELD";
            case Icode_DEFINE_PRIVATE_FIELD:
                return "DEFINE_PRIVATE_FIELD";
            case Icode_DEFINE_PRIVATE_GETTER:
                return "DEFINE_PRIVATE_GETTER";
            case Icode_DEFINE_PRIVATE_SETTER:
                return "DEFINE_PRIVATE_SETTER";
            case Icode_STORE_CLASS_COMPUTED_KEYS:
                return "STORE_CLASS_COMPUTED_KEYS";
            case Icode_GET_CLASS_COMPUTED_KEY:
                return "GET_CLASS_COMPUTED_KEY";
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
