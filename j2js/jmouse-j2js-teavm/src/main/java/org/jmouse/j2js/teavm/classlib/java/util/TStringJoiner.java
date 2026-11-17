package org.jmouse.j2js.teavm.classlib.java.util;

import java.util.Objects;

/**
 * {@code java.util.StringJoiner} in the browser: texts joined by a delimiter between a prefix and a suffix, or the empty
 * value while nothing is added — as the JDK's.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class TStringJoiner {

    private final String        prefix;
    private final String        delimiter;
    private final String        suffix;
    private       StringBuilder joined;
    private       String        emptyValue;

    public TStringJoiner(CharSequence delimiter) {
        this(delimiter, "", "");
    }

    public TStringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        Objects.requireNonNull(prefix, "The prefix must not be null");
        Objects.requireNonNull(delimiter, "The delimiter must not be null");
        Objects.requireNonNull(suffix, "The suffix must not be null");
        this.prefix = prefix.toString();
        this.delimiter = delimiter.toString();
        this.suffix = suffix.toString();
    }

    public TStringJoiner setEmptyValue(CharSequence emptyValue) {
        this.emptyValue = Objects.requireNonNull(emptyValue, "The empty value must not be null").toString();
        return this;
    }

    public TStringJoiner add(CharSequence text) {
        if (joined == null) {
            joined = new StringBuilder();
        } else {
            joined.append(delimiter);
        }
        joined.append(text);
        return this;
    }

    /** Adds what {@code other} joined, without its prefix and suffix, as one text — nothing when it joined nothing. */
    public TStringJoiner merge(TStringJoiner other) {
        Objects.requireNonNull(other);
        if (other.joined != null) {
            String added = other.joined.toString();
            if (joined == null) {
                joined = new StringBuilder();
            } else {
                joined.append(delimiter);
            }
            joined.append(added);
        }
        return this;
    }

    public int length() {
        if (joined == null && emptyValue != null) {
            return emptyValue.length();
        }
        return prefix.length() + (joined == null ? 0 : joined.length()) + suffix.length();
    }

    @Override
    public String toString() {
        if (joined == null && emptyValue != null) {
            return emptyValue;
        }
        return prefix + (joined == null ? "" : joined.toString()) + suffix;
    }
}
