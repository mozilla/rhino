/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter for optional ICU4J Unicode support via reflection.
 *
 * <p>ICU4J is optional. When present, provides full ES2024 Property of Strings support (emoji
 * sequences), comprehensive script properties, and Script_Extensions. When absent, falls back to
 * Java's built-in Unicode APIs with limited functionality.
 *
 * <p>Uses ConcurrentHashMap to cache script code lookups for performance. Enable debug logging with
 * {@code -Drhino.debug.unicode=true}.
 *
 * @see EmojiSequenceData
 * @see UnicodeProperties
 */
class ICU4JAdapter {
    private static final boolean ICU4J_AVAILABLE;
    private static final Class<?> USCRIPT_CLASS;
    private static final Method GET_CODE_FROM_NAME;
    private static final Method GET_SCRIPT_EXTENSIONS;
    private static final Class<?> UCHARACTER_CLASS;
    private static final Method HAS_BINARY_PROPERTY;
    private static final Class<?> UPROPERTY_CLASS;
    private static final int INVALID_CODE = -1;

    /** Cache for script code lookups to avoid repeated reflection overhead. */
    private static final Map<String, Integer> SCRIPT_CODE_CACHE = new ConcurrentHashMap<>();

    static {
        boolean available = false;
        Class<?> uscriptClass = null;
        Method getCodeFromName = null;
        Method getScriptExtensions = null;
        Class<?> ucharacterClass = null;
        Method hasBinaryProperty = null;
        Class<?> upropertyClass = null;

        try {
            // Try to load ICU4J classes via reflection
            uscriptClass = Class.forName("com.ibm.icu.lang.UScript");
            getCodeFromName = uscriptClass.getMethod("getCodeFromName", String.class);
            getScriptExtensions =
                    uscriptClass.getMethod("getScriptExtensions", int.class, BitSet.class);
            ucharacterClass = Class.forName("com.ibm.icu.lang.UCharacter");
            hasBinaryProperty =
                    ucharacterClass.getMethod("hasBinaryProperty", int.class, int.class);
            upropertyClass = Class.forName("com.ibm.icu.lang.UProperty");
            available = true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // ICU4J not available - will use fallback implementations
        }

        ICU4J_AVAILABLE = available;
        USCRIPT_CLASS = uscriptClass;
        GET_CODE_FROM_NAME = getCodeFromName;
        GET_SCRIPT_EXTENSIONS = getScriptExtensions;
        UCHARACTER_CLASS = ucharacterClass;
        HAS_BINARY_PROPERTY = hasBinaryProperty;
        UPROPERTY_CLASS = upropertyClass;

        // Debug logging for troubleshooting classpath issues
        if (Boolean.getBoolean("rhino.debug.unicode")) {
            String status =
                    available ? "available (full ES2024 support)" : "not found (using fallback)";
            System.err.println("[Rhino] ICU4J: " + status);
        }
    }

    /**
     * Check if ICU4J is available on the classpath.
     *
     * @return {@code true} if ICU4J classes can be loaded, {@code false} otherwise
     */
    static boolean isAvailable() {
        return ICU4J_AVAILABLE;
    }

    /**
     * Get script code from script name. Uses ICU4J if available, otherwise fallback.
     *
     * @param name Script name (e.g., "Latin", "Greek", "Cyrillic")
     * @return Script code (positive integer), or -1 if unknown
     */
    static int getScriptCodeFromName(String name) {
        if (!ICU4J_AVAILABLE) {
            return getScriptCodeFromNameFallback(name);
        }

        // Check cache first
        Integer cached = SCRIPT_CODE_CACHE.get(name);
        if (cached != null) {
            return cached;
        }

        // Perform lookup outside of computeIfAbsent to avoid caching failures
        Integer result = lookupScriptCode(name);

        // Only cache valid results to prevent caching transient errors
        if (result != null && result != INVALID_CODE) {
            SCRIPT_CODE_CACHE.putIfAbsent(name, result);
        }

        return result != null ? result : INVALID_CODE;
    }

    /**
     * Helper method for script code cache lookup via reflection.
     *
     * @param name Script name to look up
     * @return Script code, null if lookup fails, or INVALID_CODE if script not found
     */
    private static Integer lookupScriptCode(String name) {
        try {
            return (Integer) GET_CODE_FROM_NAME.invoke(null, name);
        } catch (Exception e) {
            // Don't cache reflection failures - they may be transient
            return null;
        }
    }

    /**
     * Get script extensions for a code point. Uses ICU4J if available, otherwise fallback.
     *
     * @param codePoint Unicode code point
     * @param result BitSet to populate with script codes (cleared before use)
     */
    static void getScriptExtensions(int codePoint, BitSet result) {
        result.clear();

        if (ICU4J_AVAILABLE) {
            try {
                GET_SCRIPT_EXTENSIONS.invoke(null, codePoint, result);
            } catch (Exception e) {
                getScriptExtensionsFallback(codePoint, result);
            }
        } else {
            getScriptExtensionsFallback(codePoint, result);
        }
    }

    /**
     * Fallback: Minimal script name to code mapping for common scripts.
     *
     * <p>Returns ICU4J UScript integer constants (not ISO 15924 numeric codes). These hardcoded
     * values match ICU4J's UScript enum ordinals exactly.
     *
     * @param name Script name (case-insensitive, with or without underscores)
     * @return ICU4J UScript code, or -1 if unknown
     */
    private static int getScriptCodeFromNameFallback(String name) {
        // Normalize: lowercase, remove underscores
        String normalized = name.replace("_", "").toLowerCase(java.util.Locale.ROOT);

        // Common scripts with their ICU4J UScript codes
        switch (normalized) {
            case "common":
            case "zyyy":
                return 0;
            case "inherited":
            case "zinh":
            case "qaai":
                return 1;
            case "arabic":
            case "arab":
                return 2;
            case "armenian":
            case "armn":
                return 3;
            case "bengali":
            case "beng":
                return 4;
            case "bopomofo":
            case "bopo":
                return 5;
            case "cyrillic":
            case "cyrl":
                return 8;
            case "devanagari":
            case "deva":
                return 10;
            case "greek":
            case "grek":
                return 14;
            case "hebrew":
            case "hebr":
                return 19;
            case "han":
            case "hani":
                return 17;
            case "hangul":
            case "hang":
                return 18;
            case "hiragana":
            case "hira":
                return 20;
            case "katakana":
            case "kana":
                return 22;
            case "latin":
            case "latn":
                return 25;
            case "thai":
                return 38;
            case "gurmukhi":
            case "guru":
                return 16;
            case "gujarati":
            case "gujr":
                return 15;
            case "kannada":
            case "knda":
                return 21;
            case "malayalam":
            case "mlym":
                return 26;
            case "oriya":
            case "orya":
                return 31;
            case "tamil":
            case "taml":
                return 35;
            case "telugu":
            case "telu":
                return 36;
            case "ethiopic":
            case "ethi":
                return 11;
            case "georgian":
            case "geor":
                return 12;
            case "sinhala":
            case "sinh":
                return 33;
            case "tibetan":
            case "tibt":
                return 39;
            case "myanmar":
            case "mymr":
                return 28;
            case "lao":
            case "laoo":
                return 24;
            case "khmer":
            case "khmr":
                return 23;
            default:
                return INVALID_CODE;
        }
    }

    /**
     * Fallback: Returns single script for character using Java's built-in APIs.
     *
     * @param codePoint Unicode code point
     * @param result BitSet to populate with script code
     */
    private static void getScriptExtensionsFallback(int codePoint, BitSet result) {
        // Use Java's built-in UnicodeScript (single script only)
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);

        // Map Java's UnicodeScript enum to ICU4J script codes
        // This is approximate - enum ordinals may not match ICU4J codes exactly
        int scriptCode = getScriptCodeFromJavaEnum(script);

        if (scriptCode != INVALID_CODE) {
            result.set(scriptCode);
        } else {
            // Unknown script, default to Common (0)
            result.set(0);
        }

        // NOTE: This fallback returns single script only. Multi-script characters
        // (e.g., digits 0-9 used across many scripts) are not fully supported without ICU4J.
        // This is acceptable as ICU4J is the recommended configuration for full Unicode support.
    }

    /**
     * Map Java's Character.UnicodeScript enum to ICU4J UScript codes.
     *
     * @param script Java UnicodeScript enum value
     * @return ICU4J UScript code
     */
    private static int getScriptCodeFromJavaEnum(Character.UnicodeScript script) {
        switch (script) {
            case COMMON:
                return 0;
            case LATIN:
                return 25;
            case GREEK:
                return 14;
            case CYRILLIC:
                return 8;
            case ARMENIAN:
                return 3;
            case HEBREW:
                return 19;
            case ARABIC:
                return 2;
            case DEVANAGARI:
                return 10;
            case BENGALI:
                return 4;
            case GURMUKHI:
                return 16;
            case GUJARATI:
                return 15;
            case ORIYA:
                return 31;
            case TAMIL:
                return 35;
            case TELUGU:
                return 36;
            case KANNADA:
                return 21;
            case MALAYALAM:
                return 26;
            case SINHALA:
                return 33;
            case THAI:
                return 38;
            case LAO:
                return 24;
            case TIBETAN:
                return 39;
            case MYANMAR:
                return 28;
            case GEORGIAN:
                return 12;
            case HANGUL:
                return 18;
            case ETHIOPIC:
                return 11;
            case CHEROKEE:
                return 6;
            case CANADIAN_ABORIGINAL:
                return 73;
            case OGHAM:
                return 29;
            case RUNIC:
                return 32;
            case KHMER:
                return 23;
            case MONGOLIAN:
                return 27;
            case HIRAGANA:
                return 20;
            case KATAKANA:
                return 22;
            case BOPOMOFO:
                return 5;
            case HAN:
                return 17;
            case YI:
                return 41;
            default:
                return INVALID_CODE;
        }
    }

    /**
     * Check if a code point has a binary Unicode property. Uses ICU4J if available, otherwise
     * fallback.
     *
     * @param codePoint Unicode code point to check
     * @param propertyName Property name (e.g., "EMOJI", "DEFAULT_IGNORABLE_CODE_POINT")
     * @return true if the code point has the property, false otherwise or if ICU4J unavailable
     */
    static boolean hasBinaryProperty(int codePoint, String propertyName) {
        if (!ICU4J_AVAILABLE) {
            return hasBinaryPropertyFallback(codePoint, propertyName);
        }

        try {
            // Get the UProperty constant via reflection
            java.lang.reflect.Field field = UPROPERTY_CLASS.getField(propertyName);
            int propertyConstant = field.getInt(null);

            // Call UCharacter.hasBinaryProperty(codePoint, property)
            return (Boolean) HAS_BINARY_PROPERTY.invoke(null, codePoint, propertyConstant);
        } catch (Exception e) {
            // Property not found or error - fallback
            return hasBinaryPropertyFallback(codePoint, propertyName);
        }
    }

    /**
     * Fallback: Basic property checking using Java's Character class.
     *
     * @param codePoint Unicode code point to check
     * @param propertyName Property name
     * @return true if the code point has the property (best-effort approximation)
     */
    private static boolean hasBinaryPropertyFallback(int codePoint, String propertyName) {
        // Without ICU4J, we can only provide minimal fallback for some properties
        // Most ES2024 properties (Emoji, Extended_Pictographic, etc.) are not available
        // in Java's standard library
        switch (propertyName) {
            case "ALPHABETIC":
                return Character.isAlphabetic(codePoint);
            case "LOWERCASE":
                return Character.isLowerCase(codePoint);
            case "UPPERCASE":
                return Character.isUpperCase(codePoint);
            case "WHITE_SPACE":
                return Character.isWhitespace(codePoint);
            case "IDEOGRAPHIC":
                return Character.isIdeographic(codePoint);
            case "DIACRITIC":
                // Approximate: combining marks are often diacritics
                int type = Character.getType(codePoint);
                return type == Character.NON_SPACING_MARK
                        || type == Character.COMBINING_SPACING_MARK
                        || type == Character.ENCLOSING_MARK;
            default:
                // For Emoji and other ES2024 properties, we have no fallback
                // Return false (property not matched)
                return false;
        }
    }
}
