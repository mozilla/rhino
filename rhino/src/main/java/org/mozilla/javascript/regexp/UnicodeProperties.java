package org.mozilla.javascript.regexp;

import java.util.Map;
import java.util.regex.Matcher;

/**
 * Unicode properties handler for Java 11 Character class. Handles binary properties from ECMA-262
 * and general category values.
 */
public class UnicodeProperties {
    // Binary Property Names (from ECMA-262 table-binary-unicode-properties)
    public static final byte ALPHABETIC = 1;
    public static final byte ASCII = ALPHABETIC + 1;
    public static final byte CASE_IGNORABLE = ASCII + 1;
    public static final byte ASCII_HEX_DIGIT = CASE_IGNORABLE + 1;
    public static final byte HEX_DIGIT = ASCII_HEX_DIGIT + 1;
    public static final byte ID_CONTINUE = HEX_DIGIT + 1;
    public static final byte ID_START = ID_CONTINUE + 1;
    public static final byte LOWERCASE = ID_START + 1;
    public static final byte UPPERCASE = LOWERCASE + 1;
    public static final byte WHITE_SPACE = UPPERCASE + 1;

    // Non-binary properties
    public static final byte GENERAL_CATEGORY = WHITE_SPACE + 1;
    public static final byte SCRIPT = GENERAL_CATEGORY + 1;

    // Property Values for General Category (from PropertyValueAliases.txt)
    // OTHER
    public static final byte OTHER = 1;
    public static final byte CONTROL = OTHER + 1;
    public static final byte FORMAT = CONTROL + 1;
    public static final byte UNASSIGNED = FORMAT + 1;
    public static final byte PRIVATE_USE = UNASSIGNED + 1;
    public static final byte SURROGATE = PRIVATE_USE + 1;
    public static final byte LETTER = SURROGATE + 1;
    public static final byte LOWERCASE_LETTER = LETTER + 1;
    public static final byte MODIFIER_LETTER = LOWERCASE_LETTER + 1;
    public static final byte OTHER_LETTER = MODIFIER_LETTER + 1;
    public static final byte TITLECASE_LETTER = OTHER_LETTER + 1;
    public static final byte UPPERCASE_LETTER = TITLECASE_LETTER + 1;
    public static final byte MARK = UPPERCASE_LETTER + 1;
    public static final byte SPACING_MARK = MARK + 1;
    public static final byte ENCLOSING_MARK = SPACING_MARK + 1;
    public static final byte NONSPACING_MARK = ENCLOSING_MARK + 1;
    public static final byte NUMBER = NONSPACING_MARK + 1;
    public static final byte DECIMAL_NUMBER = NUMBER + 1;
    public static final byte LETTER_NUMBER = DECIMAL_NUMBER + 1;
    public static final byte OTHER_NUMBER = LETTER_NUMBER + 1;
    public static final byte PUNCTUATION = OTHER_NUMBER + 1;
    public static final byte CONNECTOR_PUNCTUATION = PUNCTUATION + 1;
    public static final byte DASH_PUNCTUATION = CONNECTOR_PUNCTUATION + 1;
    public static final byte CLOSE_PUNCTUATION = DASH_PUNCTUATION + 1;
    public static final byte FINAL_PUNCTUATION = CLOSE_PUNCTUATION + 1;
    public static final byte INITIAL_PUNCTUATION = FINAL_PUNCTUATION + 1;
    public static final byte OTHER_PUNCTUATION = INITIAL_PUNCTUATION + 1;
    public static final byte OPEN_PUNCTUATION = OTHER_PUNCTUATION + 1;
    public static final byte SYMBOL = OPEN_PUNCTUATION + 1;
    public static final byte CURRENCY_SYMBOL = SYMBOL + 1;
    public static final byte MODIFIER_SYMBOL = CURRENCY_SYMBOL + 1;
    public static final byte MATH_SYMBOL = MODIFIER_SYMBOL + 1;
    public static final byte OTHER_SYMBOL = MATH_SYMBOL + 1;
    public static final byte SEPARATOR = OTHER_SYMBOL + 1;
    public static final byte LINE_SEPARATOR = SEPARATOR + 1;
    public static final byte PARAGRAPH_SEPARATOR = LINE_SEPARATOR + 1;
    public static final byte SPACE_SEPARATOR = PARAGRAPH_SEPARATOR + 1;

    // Binary property values
    public static final byte TRUE = SPACE_SEPARATOR + 1;
    public static final byte FALSE = TRUE + 1;

    // Property Name Map (canonical names and aliases)
    public static final Map<String, Byte> PROPERTY_NAMES =
            Map.ofEntries(
                    Map.entry("Alphabetic", ALPHABETIC),
                    Map.entry("Alpha", ALPHABETIC),
                    Map.entry("ASCII", ASCII),
                    Map.entry("Case_Ignorable", CASE_IGNORABLE),
                    Map.entry("CI", CASE_IGNORABLE),
                    Map.entry("General_Category", GENERAL_CATEGORY),
                    Map.entry("gc", GENERAL_CATEGORY),
                    Map.entry("Script", SCRIPT),
                    Map.entry("sc", SCRIPT),
                    Map.entry("ASCII_Hex_Digit", ASCII_HEX_DIGIT),
                    Map.entry("AHex", ASCII_HEX_DIGIT),
                    Map.entry("Hex_Digit", HEX_DIGIT),
                    Map.entry("Hex", HEX_DIGIT),
                    Map.entry("ID_Continue", ID_CONTINUE),
                    Map.entry("IDC", ID_CONTINUE),
                    Map.entry("ID_Start", ID_START),
                    Map.entry("IDS", ID_START),
                    Map.entry("Lowercase", LOWERCASE),
                    Map.entry("Lower", LOWERCASE),
                    Map.entry("Uppercase", UPPERCASE),
                    Map.entry("Upper", UPPERCASE),
                    Map.entry("White_Space", WHITE_SPACE),
                    Map.entry("space", WHITE_SPACE));

    // Property Value Map for General Category (canonical names and aliases)
    public static final Map<String, Byte> PROPERTY_VALUES =
            Map.<String, Byte>ofEntries(
                    Map.entry("Other", OTHER),
                    Map.entry("C", OTHER),
                    Map.entry("Control", CONTROL),
                    Map.entry("Cc", CONTROL),
                    Map.entry("cntrl", CONTROL),
                    Map.entry("Format", FORMAT),
                    Map.entry("Cf", FORMAT),
                    Map.entry("Unassigned", UNASSIGNED),
                    Map.entry("Cn", UNASSIGNED),
                    Map.entry("Private_Use", PRIVATE_USE),
                    Map.entry("Co", PRIVATE_USE),
                    Map.entry("Surrogate", SURROGATE),
                    Map.entry("Cs", SURROGATE),
                    Map.entry("Letter", LETTER),
                    Map.entry("L", LETTER),
                    Map.entry("Lowercase_Letter", LOWERCASE_LETTER),
                    Map.entry("Ll", LOWERCASE_LETTER),
                    Map.entry("Modifier_Letter", MODIFIER_LETTER),
                    Map.entry("Lm", MODIFIER_LETTER),
                    Map.entry("Other_Letter", OTHER_LETTER),
                    Map.entry("Lo", OTHER_LETTER),
                    Map.entry("Titlecase_Letter", TITLECASE_LETTER),
                    Map.entry("Lt", TITLECASE_LETTER),
                    Map.entry("Uppercase_Letter", UPPERCASE_LETTER),
                    Map.entry("Lu", UPPERCASE_LETTER),
                    Map.entry("Mark", MARK),
                    Map.entry("M", MARK),
                    Map.entry("Combining_Mark", MARK),
                    Map.entry("Spacing_Mark", SPACING_MARK),
                    Map.entry("Mc", SPACING_MARK),
                    Map.entry("Enclosing_Mark", ENCLOSING_MARK),
                    Map.entry("Me", ENCLOSING_MARK),
                    Map.entry("Nonspacing_Mark", NONSPACING_MARK),
                    Map.entry("Mn", NONSPACING_MARK),
                    Map.entry("Number", NUMBER),
                    Map.entry("N", NUMBER),
                    Map.entry("Decimal_Number", DECIMAL_NUMBER),
                    Map.entry("Nd", DECIMAL_NUMBER),
                    Map.entry("digit", NUMBER),
                    Map.entry("Letter_Number", LETTER_NUMBER),
                    Map.entry("Nl", LETTER_NUMBER),
                    Map.entry("Other_Number", OTHER_NUMBER),
                    Map.entry("No", OTHER_NUMBER),
                    Map.entry("Punctuation", PUNCTUATION),
                    Map.entry("P", PUNCTUATION),
                    Map.entry("punct", PUNCTUATION),
                    Map.entry("Connector_Punctuation", CONNECTOR_PUNCTUATION),
                    Map.entry("Pc", CONNECTOR_PUNCTUATION),
                    Map.entry("Dash_Punctuation", DASH_PUNCTUATION),
                    Map.entry("Pd", DASH_PUNCTUATION),
                    Map.entry("Close_Punctuation", CLOSE_PUNCTUATION),
                    Map.entry("Pe", CLOSE_PUNCTUATION),
                    Map.entry("Final_Punctuation", FINAL_PUNCTUATION),
                    Map.entry("Pf", FINAL_PUNCTUATION),
                    Map.entry("Initial_Punctuation", INITIAL_PUNCTUATION),
                    Map.entry("Pi", INITIAL_PUNCTUATION),
                    Map.entry("Other_Punctuation", OTHER_PUNCTUATION),
                    Map.entry("Po", OTHER_PUNCTUATION),
                    Map.entry("Open_Punctuation", OPEN_PUNCTUATION),
                    Map.entry("Ps", OPEN_PUNCTUATION),
                    Map.entry("Symbol", SYMBOL),
                    Map.entry("S", SYMBOL),
                    Map.entry("Currency_Symbol", CURRENCY_SYMBOL),
                    Map.entry("Sc", CURRENCY_SYMBOL),
                    Map.entry("Modifier_Symbol", MODIFIER_SYMBOL),
                    Map.entry("Sk", MODIFIER_SYMBOL),
                    Map.entry("Math_Symbol", MATH_SYMBOL),
                    Map.entry("Sm", MATH_SYMBOL),
                    Map.entry("Other_Symbol", OTHER_SYMBOL),
                    Map.entry("So", OTHER_SYMBOL),
                    Map.entry("Separator", SEPARATOR),
                    Map.entry("Z", SEPARATOR),
                    Map.entry("Line_Separator", LINE_SEPARATOR),
                    Map.entry("Zl", LINE_SEPARATOR),
                    Map.entry("Paragraph_Separator", PARAGRAPH_SEPARATOR),
                    Map.entry("Zp", PARAGRAPH_SEPARATOR),
                    Map.entry("Space_Separator", SPACE_SEPARATOR),
                    Map.entry("Zs", SPACE_SEPARATOR));

    /**
     * Looks up a property name and optionally a value and returns an encoded int. For binary
     * properties, combines the property name with TRUE. For General_Category, combines
     * General_Category with the specified value.
     *
     * @param propertyOrValue Property name or property name=value pair
     * @return Encoded int combining property name and value
     */
    @SuppressWarnings("EnumOrdinal") // We don't persist the ordinals; hence this is safe.
    public static int lookup(String propertyOrValue) {
        if (propertyOrValue == null || propertyOrValue.isEmpty()) {
            return -1;
        }

        Matcher m =
                java.util.regex.Pattern.compile(
                                "^(?<propName>[a-zA-Z_]+)(?:=(?<propValue>[a-zA-Z_0-9]+))?$")
                        .matcher(propertyOrValue);
        m.find();
        if (!m.matches() || m.group("propName") == null) {
            return -1;
        }

        if (m.group("propValue") == null) {
            // It's a single property name (binary property)
            String property = m.group("propName");

            Byte propByte = PROPERTY_NAMES.get(property);

            if (propByte == null) {
                // Check if it's a general category value without the gc= prefix
                Byte valueByte = PROPERTY_VALUES.get(property);
                if (valueByte != null) {
                    // It's a GC value, encode it with GC property
                    return encodeProperty(GENERAL_CATEGORY, valueByte);
                }
                return -1;
            }

            if (propByte == GENERAL_CATEGORY || propByte == SCRIPT) {
                return -1;
            }

            // It's a binary property, encode with TRUE
            return encodeProperty(propByte, TRUE);
        } else {
            // It's a property=value format
            String property = m.group("propName");
            String value = m.group("propValue");

            Byte propByte = PROPERTY_NAMES.get(property);
            if (propByte == null) {
                return -1;
            }

            switch (propByte) {
                case GENERAL_CATEGORY:
                    Byte valueByte = PROPERTY_VALUES.get(value);
                    if (valueByte == null) {
                        return -1;
                    }
                    return encodeProperty(GENERAL_CATEGORY, valueByte);
                case SCRIPT:
                    try {
                        return encodeProperty(
                                SCRIPT, (byte) Character.UnicodeScript.forName(value).ordinal());
                    } catch (IllegalArgumentException e) {
                        return -1;
                    }
                default:
                    // Binary properties don't have values
                    return -1;
            }
        }
    }

    /**
     * Encodes a property name and value into a single int. The property name is in the high 16
     * bits, the value in the low 16 bits.
     *
     * @param property Property name constant
     * @param value Property value constant
     * @return Encoded int
     */
    private static int encodeProperty(byte property, byte value) {
        return ((property & 0xFF) << 8) | (value & 0xFF);
    }

    private static final Character.UnicodeScript[] UnicodeScriptValues =
            Character.UnicodeScript.values();

    /**
     * Tests if a code point has a specific Unicode property (direct check without case handling).
     */
    public static boolean hasProperty(int property, int codePoint) {
        byte propByte = (byte) ((property >> 8) & 0xFF);
        int valueByte = (property & 0xFF);
        return hasPropertyDirect(propByte, valueByte, codePoint);
    }

    /** Direct property check without any case handling. */
    private static boolean hasPropertyDirect(byte propByte, int valueByte, int codePoint) {
        switch (propByte) {
            case ALPHABETIC:
                return Character.isAlphabetic(codePoint) == (valueByte == TRUE);

            case ASCII:
                return (codePoint <= 0x7F) == (valueByte == TRUE);

            case CASE_IGNORABLE:
                // Java doesn't have a direct method for this
                // This is an approximation
                return (Character.getType(codePoint) == Character.MODIFIER_SYMBOL
                                || Character.getType(codePoint) == Character.MODIFIER_LETTER
                                || Character.getType(codePoint) == Character.NON_SPACING_MARK)
                        == (valueByte == TRUE);

            case GENERAL_CATEGORY:
                int javaCategory = Character.getType(codePoint);
                return checkGeneralCategory(valueByte, javaCategory);
            case ASCII_HEX_DIGIT:
                return isHexDigit(codePoint) == (valueByte == TRUE);
            case HEX_DIGIT:
                return (Character.digit(codePoint, 16) != -1) == (valueByte == TRUE);
            case ID_CONTINUE:
                return Character.isUnicodeIdentifierPart(codePoint) == (valueByte == TRUE);

            case ID_START:
                return Character.isUnicodeIdentifierStart(codePoint) == (valueByte == TRUE);

            case LOWERCASE:
                return Character.isLowerCase(codePoint) == (valueByte == TRUE);

            case UPPERCASE:
                return Character.isUpperCase(codePoint) == (valueByte == TRUE);

            case WHITE_SPACE:
                {
                    // Note: This only a good approximation of the Unicode white space property
                    return (valueByte == TRUE)
                            == (Character.isSpaceChar(codePoint)
                                    || Character.isWhitespace(codePoint));
                }
            case SCRIPT:
                return Character.UnicodeScript.of(codePoint) == UnicodeScriptValues[valueByte];
            default:
                return false;
        }
    }

    /** Maps our property value bytes to Java's Character.getType() values. */
    private static boolean checkGeneralCategory(int propertyValueByte, int javaCategory) {
        switch (propertyValueByte) {
            case LETTER:
                return javaCategory == Character.UPPERCASE_LETTER
                        || javaCategory == Character.LOWERCASE_LETTER
                        || javaCategory == Character.TITLECASE_LETTER
                        || javaCategory == Character.MODIFIER_LETTER
                        || javaCategory == Character.OTHER_LETTER;
            case UPPERCASE_LETTER:
                return javaCategory == Character.UPPERCASE_LETTER;
            case LOWERCASE_LETTER:
                return javaCategory == Character.LOWERCASE_LETTER;
            case TITLECASE_LETTER:
                return javaCategory == Character.TITLECASE_LETTER;
            case MODIFIER_LETTER:
                return javaCategory == Character.MODIFIER_LETTER;
            case OTHER_LETTER:
                return javaCategory == Character.OTHER_LETTER;
            case MARK:
                return javaCategory == Character.NON_SPACING_MARK
                        || javaCategory == Character.ENCLOSING_MARK
                        || javaCategory == Character.COMBINING_SPACING_MARK;
            case NONSPACING_MARK:
                return javaCategory == Character.NON_SPACING_MARK;
            case ENCLOSING_MARK:
                return javaCategory == Character.ENCLOSING_MARK;
            case SPACING_MARK:
                return javaCategory == Character.COMBINING_SPACING_MARK;
            case NUMBER:
                return javaCategory == Character.DECIMAL_DIGIT_NUMBER
                        || javaCategory == Character.LETTER_NUMBER
                        || javaCategory == Character.OTHER_NUMBER;
            case DECIMAL_NUMBER:
                return javaCategory == Character.DECIMAL_DIGIT_NUMBER;
            case LETTER_NUMBER:
                return javaCategory == Character.LETTER_NUMBER;
            case OTHER_NUMBER:
                return javaCategory == Character.OTHER_NUMBER;

            case SEPARATOR:
                return javaCategory == Character.SPACE_SEPARATOR
                        || javaCategory == Character.LINE_SEPARATOR
                        || javaCategory == Character.PARAGRAPH_SEPARATOR;
            case SPACE_SEPARATOR:
                return javaCategory == Character.SPACE_SEPARATOR;
            case LINE_SEPARATOR:
                return javaCategory == Character.LINE_SEPARATOR;
            case PARAGRAPH_SEPARATOR:
                return javaCategory == Character.PARAGRAPH_SEPARATOR;

            case OTHER:
                return javaCategory == Character.OTHER_LETTER
                        || javaCategory == Character.OTHER_NUMBER
                        || javaCategory == Character.OTHER_PUNCTUATION
                        || javaCategory == Character.OTHER_SYMBOL;
            case CONTROL:
                return javaCategory == Character.CONTROL;
            case FORMAT:
                return javaCategory == Character.FORMAT;
            case SURROGATE:
                return javaCategory == Character.SURROGATE;
            case PRIVATE_USE:
                return javaCategory == Character.PRIVATE_USE;

            case PUNCTUATION:
                return javaCategory == Character.CONNECTOR_PUNCTUATION
                        || javaCategory == Character.DASH_PUNCTUATION
                        || javaCategory == Character.START_PUNCTUATION
                        || javaCategory == Character.END_PUNCTUATION
                        || javaCategory == Character.OTHER_PUNCTUATION
                        || javaCategory == Character.INITIAL_QUOTE_PUNCTUATION
                        || javaCategory == Character.FINAL_QUOTE_PUNCTUATION;
            case DASH_PUNCTUATION:
                return javaCategory == Character.DASH_PUNCTUATION;
            case OPEN_PUNCTUATION:
                return javaCategory == Character.START_PUNCTUATION;
            case CLOSE_PUNCTUATION:
                return javaCategory == Character.END_PUNCTUATION;
            case CONNECTOR_PUNCTUATION:
                return javaCategory == Character.CONNECTOR_PUNCTUATION;
            case OTHER_PUNCTUATION:
                return javaCategory == Character.OTHER_PUNCTUATION;
            case INITIAL_PUNCTUATION:
                return javaCategory == Character.INITIAL_QUOTE_PUNCTUATION;
            case FINAL_PUNCTUATION:
                return javaCategory == Character.FINAL_QUOTE_PUNCTUATION;

            case SYMBOL:
                return javaCategory == Character.MATH_SYMBOL
                        || javaCategory == Character.CURRENCY_SYMBOL
                        || javaCategory == Character.MODIFIER_SYMBOL
                        || javaCategory == Character.OTHER_SYMBOL;
            case MATH_SYMBOL:
                return javaCategory == Character.MATH_SYMBOL;
            case CURRENCY_SYMBOL:
                return javaCategory == Character.CURRENCY_SYMBOL;
            case MODIFIER_SYMBOL:
                return javaCategory == Character.MODIFIER_SYMBOL;
            case OTHER_SYMBOL:
                return javaCategory == Character.OTHER_SYMBOL;
            case UNASSIGNED:
                return javaCategory == Character.UNASSIGNED;

            default:
                return false;
        }
    }

    /** Checks if a code point is a hex digit. */
    private static boolean isHexDigit(int codePoint) {
        return (codePoint >= '0' && codePoint <= '9')
                || (codePoint >= 'a' && codePoint <= 'f')
                || (codePoint >= 'A' && codePoint <= 'F');
    }
}
