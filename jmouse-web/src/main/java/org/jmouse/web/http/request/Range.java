package org.jmouse.web.http.request;

import org.jmouse.util.StringHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 📏 Represents an HTTP byte range as defined in RFC 7233.
 *
 * <p>Supports formats:</p>
 * <ul>
 *   <li>{@code A-B} → range from A to B</li>
 *   <li>{@code A-} → open-ended range from A to end</li>
 *   <li>{@code -N} → suffix range of last N bytes</li>
 * </ul>
 */
public class Range {

    private static final String BYTE_RANGE_PREFIX     = "bytes=";
    private static final String RANGES_SEPARATOR      = ",";
    private static final String RANGE_VALUE_SEPARATOR = "-";

    /**
     * Start position (or -1 for suffix).
     */
    private final long start;

    /**
     * End position (or -1 for open-ended).
     */
    private final long end;

    /**
     * 🏗️ Create a new range.
     *
     * @param start start position (inclusive), or -1
     * @param end   end position (inclusive), or -1
     */
    public Range(long start, long end) {
        this.start = start;
        this.end = end;
    }

    /**
     * 📐 Standard range {@code A-B}.
     */
    public static Range ofRange(long start, long end) {
        return new Range(start, end);
    }

    /**
     * 📐 Open-ended range {@code A-}.
     */
    public static Range ofStart(long start) {
        return new Range(start, -1);
    }

    /**
     * 📐 Suffix range {@code -N} (last N bytes).
     */
    public static Range ofEnd(long end) {
        return new Range(-1, end);
    }

    /**
     * 🔎 Parse a {@code Range} header value into a list of ranges.
     *
     * <p>Example: {@code bytes=0-99,200-299}</p>
     *
     * @param value raw header string
     * @return list of parsed {@link Range}s
     */
    public static List<Range> parseRanges(String value) {
        List<Range> ranges = new ArrayList<>();

        if (value.regionMatches(true, 0, BYTE_RANGE_PREFIX, 0, BYTE_RANGE_PREFIX.length())) {
            value = value.substring(BYTE_RANGE_PREFIX.length());
        }

        String[] tokens = {value};
        if (value.contains(RANGES_SEPARATOR)) {
            tokens = StringHelper.tokenize(value, RANGES_SEPARATOR);
        }

        for (String token : tokens) {
            ranges.add(Range.parseOneRange(token));
        }

        return ranges;
    }

    /**
     * 🔎 Parse a single range expression.
     *
     * @param value raw string (e.g. {@code 0-99}, {@code 500-}, {@code -200})
     * @return parsed {@link Range}
     */
    public static Range parseOneRange(String value) {
        int index = value.indexOf(RANGE_VALUE_SEPARATOR);

        if (index < 0) {
            throw new IllegalArgumentException("INVALID RANGE VALUE: %s".formatted(value));
        } else if (index == 0) {
            long end = Long.parseLong(value.substring(1));
            return Range.ofEnd(end);
        }

        long start = Long.parseLong(value.substring(0, index));

        if (index < value.length() - 1) {
            long end = Long.parseLong(value.substring(index + 1));
            return ofRange(start, end);
        }

        return Range.ofStart(start);
    }

    /**
     * 📍 Resolve the effective start position for a resource of given length.
     *
     * @param length total resource length
     * @return computed start index
     */
    public long getStart(long length) {
        if (start == -1) {
            return (end < length) ? (length - end) : 0;
        } else {
            return start;
        }
    }

    /**
     * 📍 Resolve the effective end position for a resource of given length.
     *
     * @param length total resource length
     * @return computed end index
     */
    public long getEnd(long length) {
        // suffix "-N" or open-ended "A-"
        if (end == -1 || start == -1) {
            return length - 1;
        } else {
            return end;
        }
    }

    /**
     * 🏷️ Render this range as a valid HTTP {@code Range} header value.
     * <p>Examples: {@code bytes=0-99}, {@code bytes=500-}, {@code bytes=-200}</p>
     *
     * @return header value string starting with {@code bytes=}
     */
    public String toHeaderValue() {
        return BYTE_RANGE_PREFIX + toStringRange();
    }

    /**
     * 📃 Render RFC 7233 range-spec (without the {@code bytes=} prefix).
     * <p>Examples: {@code 0-99}, {@code 500-}, {@code -200}</p>
     *
     * @return range spec string
     */
    private String toStringRange() {
        if (start == -1) {
            // suffix: "-N"
            return "-" + end;
        }

        if (end == -1) {
            // open-ended: "A-"
            return start + "-";
        }

        // explicit: "A-B"
        return start + "-" + end;
    }

    @Override
    public String toString() {
        return "Range: %s".formatted(toHeaderValue());
    }
}
