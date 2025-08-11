package org.jmouse.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a size in bytes, with utility methods for parsing,
 * converting, and comparing storage units (B, KB, MB, GB, TB, PB).
 * <p>
 * Instances of this class are immutable.
 */
public class Bytes implements Comparable<Bytes> {

    private static final long BYTES_KB = 1024;
    private static final long BYTES_MB = 1024 * BYTES_KB;
    private static final long BYTES_GB = 1024 * BYTES_MB;
    private static final long BYTES_TB = 1024 * BYTES_GB;
    private static final long BYTES_PB = 1024 * BYTES_TB;

    private final long bytes;

    /**
     * Creates a new {@code Bytes} instance representing the given byte count.
     *
     * @param bytes the number of bytes
     */
    public Bytes(long bytes) {
        this.bytes = bytes;
    }

    /**
     * Parses a text representation of a byte size (e.g., {@code "10KB"}, {@code "512MB"}).
     *
     * @param text the size string to parse
     * @return a {@code Bytes} instance, or {@code null} if parsing fails
     */
    public static Bytes parse(CharSequence text) {
        return Parser.parse(text);
    }

    /**
     * Creates a {@code Bytes} instance from the given number of bytes.
     *
     * @param bytes the size in bytes
     * @return a {@code Bytes} instance
     */
    public static Bytes ofBytes(long bytes) {
        return of(bytes, Unit.B);
    }

    /**
     * Creates a {@code Bytes} instance from the given number of kilobytes.
     *
     * @param bytes the size in kilobytes
     * @return a {@code Bytes} instance
     */
    public static Bytes ofKilobyte(long bytes) {
        return of(bytes, Unit.KB);
    }

    /**
     * Creates a {@code Bytes} instance from the given number of megabytes.
     *
     * @param bytes the size in megabytes
     * @return a {@code Bytes} instance
     */
    public static Bytes ofMegabytes(long bytes) {
        return of(bytes, Unit.MB);
    }

    /**
     * Creates a {@code Bytes} instance from the given number of gigabytes.
     *
     * @param bytes the size in gigabytes
     * @return a {@code Bytes} instance
     */
    public static Bytes ofGigabytes(long bytes) {
        return of(bytes, Unit.GB);
    }

    /**
     * Creates a {@code Bytes} instance from the given number of terabytes.
     *
     * @param bytes the size in terabytes
     * @return a {@code Bytes} instance
     */
    public static Bytes ofTerabytes(long bytes) {
        return of(bytes, Unit.TB);
    }

    /**
     * Creates a {@code Bytes} instance from the given number of petabytes.
     *
     * @param bytes the size in petabytes
     * @return a {@code Bytes} instance
     */
    public static Bytes ofPetabytes(long bytes) {
        return of(bytes, Unit.PB);
    }

    /**
     * Creates a {@code Bytes} instance from a specified amount and unit.
     *
     * @param amount the amount in the specified unit
     * @param unit   the unit of the amount (non-null)
     * @return a {@code Bytes} instance
     * @throws IllegalArgumentException if {@code unit} is {@code null}
     */
    public static Bytes of(long amount, Unit unit) {
        if (unit != null) {
            return new Bytes(Math.multiplyExact(amount, unit.getSize()));
        }
        throw new IllegalArgumentException("Unit cannot be null");
    }

    /**
     * Checks if the size is negative.
     *
     * @return {@code true} if negative, otherwise {@code false}
     */
    public boolean isNegative() {
        return bytes < 0;
    }

    /**
     * Checks if the size is exactly zero.
     *
     * @return {@code true} if zero, otherwise {@code false}
     */
    public boolean isZero() {
        return bytes == 0;
    }

    /**
     * Returns the size in bytes.
     *
     * @return size in bytes
     */
    public long toBytes() {
        return bytes;
    }

    /**
     * Returns the size in whole kilobytes.
     *
     * @return size in kilobytes
     */
    public long toKilobytes() {
        return bytes / BYTES_KB;
    }

    /**
     * Returns the size in whole megabytes.
     *
     * @return size in megabytes
     */
    public long toMegabytes() {
        return bytes / BYTES_MB;
    }

    /**
     * Returns the size in whole gigabytes.
     *
     * @return size in gigabytes
     */
    public long getGigabytes() {
        return bytes / BYTES_GB;
    }

    /**
     * Returns the size in whole terabytes.
     *
     * @return size in terabytes
     */
    public long getTerabytes() {
        return bytes / BYTES_TB;
    }

    /**
     * Returns the size in whole petabytes.
     *
     * @return size in petabytes
     */
    public long getPetabytes() {
        return bytes / BYTES_PB;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(Bytes other) {
        return Long.compare(this.bytes, other.bytes);
    }

    /**
     * Returns a string representation, e.g., {@code "1024B"}.
     *
     * @return the string form of this size
     */
    @Override
    public String toString() {
        return "%dB".formatted(bytes);
    }

    /**
     * Enumeration of supported storage units.
     */
    public enum Unit {
        B("B", BYTES_KB),
        KB("KB", BYTES_KB),
        MB("MB", BYTES_MB),
        GB("GB", BYTES_GB),
        TB("TB", BYTES_TB),
        PB("PB", BYTES_PB);

        private final String suffix;
        private final long size;

        Unit(String suffix, long size) {
            this.suffix = suffix;
            this.size = size;
        }

        /**
         * Returns the unit's suffix (e.g., "MB").
         *
         * @return the suffix string
         */
        public String getSuffix() {
            return suffix;
        }

        /**
         * Returns the unit size in bytes.
         *
         * @return the size in bytes
         */
        public long getSize() {
            return size;
        }

        /**
         * Finds a {@code Unit} by its suffix.
         *
         * @param suffix the suffix string
         * @return the matching unit, or {@code null} if not found
         */
        public static Unit forSuffix(String suffix) {
            Unit unit = null;
            for (Unit candidate : values()) {
                if (candidate.getSuffix().equals(suffix)) {
                    unit = candidate;
                    break;
                }
            }
            return unit;
        }
    }

    /**
     * Internal parser for {@link Bytes} text representations.
     */
    public static class Parser {

        /** Pattern matching optional sign, number, and unit suffix. */
        public final static Pattern PATTERN =
                Pattern.compile("^([+\\-]?\\d+)([a-zA-Z]{0,2})$");

        /**
         * Parses a string into a {@link Bytes} object.
         *
         * @param text the string to parse
         * @return the parsed {@code Bytes}, or {@code null} if invalid
         */
        public static Bytes parse(CharSequence text) {
            Matcher matcher = PATTERN.matcher(text);
            if (matcher.matches()) {
                long bytes = Long.parseLong(
                        matcher.group(1),
                        matcher.start(1),
                        matcher.end(1),
                        10
                );
                String suffix = matcher.group(2);
                return Bytes.of(bytes, Unit.forSuffix(suffix));
            }
            return null;
        }
    }
}
