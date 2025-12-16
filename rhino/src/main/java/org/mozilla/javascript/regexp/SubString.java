/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.regexp;

import java.util.Objects;
import java.util.Optional;

/**
 * Substring view that avoids string copying.
 *
 * <p>Represents a substring by storing a reference to the original string plus offset and length,
 * avoiding the memory overhead of creating new String objects for temporary substrings during regex
 * matching.
 *
 * <p>This class is particularly useful for capture groups in regular expressions, where a pattern
 * might create hundreds of temporary substrings. By storing only references and offsets, we
 * minimize memory allocation and garbage collection overhead.
 *
 * <p><b>Performance Note:</b> SubString instances are reused and mutated in place during regex
 * matching for performance. While not thread-safe during mutation, this is acceptable as each
 * matching operation uses its own RegExpImpl instance.
 *
 * <p><b>Memory:</b> ~24 bytes (8-byte string reference + 2×4-byte integers + padding) regardless
 * of substring length.
 */
public final class SubString {

    // Package-private for performance-critical access within regexp package
    // TODO: Refactor to fully private with proper encapsulation
    String str;
    int index;
    int length;

    /** Creates an empty substring. */
    public SubString() {
        this("", 0, 0);
    }

    /**
     * Creates a substring view of an entire string.
     *
     * @param str the source string (must not be null)
     * @throws NullPointerException if str is null
     */
    public SubString(String str) {
        this(Objects.requireNonNull(str, "str cannot be null"), 0, str.length());
    }

    /**
     * Creates a substring view.
     *
     * @param source the source string (may be null for empty substring)
     * @param start the starting index (must be &gt;= 0)
     * @param len the length (must be &gt;= 0)
     * @throws IllegalArgumentException if indices are invalid
     */
    public SubString(String source, int start, int len) {
        if (start < 0) {
            throw new IllegalArgumentException("start index cannot be negative: " + start);
        }
        if (len < 0) {
            throw new IllegalArgumentException("length cannot be negative: " + len);
        }
        if (source != null && start + len > source.length()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Substring extends beyond source: start=%d, length=%d, source.length=%d",
                            start, len, source.length()));
        }
        this.str = source;
        this.index = start;
        this.length = len;
    }

    /**
     * Gets the substring as an Optional.
     *
     * @return Optional containing the substring, or empty if source is null
     */
    public Optional<String> asString() {
        return Optional.ofNullable(str).map(s -> s.substring(index, index + length));
    }

    /**
     * Gets the substring, or empty string if source is null.
     *
     * @return the substring (never null)
     */
    @Override
    public String toString() {
        return asString().orElse("");
    }

    /**
     * Gets the length of this substring.
     *
     * @return the length (always &gt;= 0)
     */
    public int getLength() {
        return length;
    }

    /**
     * Gets the starting index within the source string.
     *
     * @return the starting index (always &gt;= 0)
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the source string.
     *
     * @return the source string, or null if this is an empty substring
     */
    public String getSource() {
        return str;
    }

    /**
     * Checks if this substring is empty.
     *
     * @return true if length is 0 or source is null
     */
    public boolean isEmpty() {
        return str == null || length == 0;
    }

    /**
     * Appends this substring to a StringBuilder.
     *
     * <p>More efficient than toString() when building strings, as it avoids creating an
     * intermediate String object.
     *
     * @param sb the StringBuilder to append to
     * @return the same StringBuilder for chaining
     * @throws NullPointerException if sb is null
     */
    public StringBuilder appendTo(StringBuilder sb) {
        Objects.requireNonNull(sb, "StringBuilder cannot be null");
        if (str != null && length > 0) {
            sb.append(str, index, index + length);
        }
        return sb;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SubString)) return false;
        SubString other = (SubString) obj;
        return this.toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
