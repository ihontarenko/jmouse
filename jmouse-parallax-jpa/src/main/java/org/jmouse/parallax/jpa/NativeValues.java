package org.jmouse.parallax.jpa;

import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 🔢 What a column read by a native query comes back as, turned into the type the store means.
 *
 * <p>⚠️ A native query answers in whatever the driver and the provider's version choose: a count is a {@code Long} on
 * one database and a {@code BigInteger} or an {@code Integer} on another, a boolean may be a number, and a timestamp
 * may arrive as a {@link Timestamp}, a {@link LocalDateTime} or an {@link OffsetDateTime}. Every store reads through
 * here so one place knows all of them.</p>
 *
 * <p>⚠️ A {@link LocalDateTime} is read as UTC, because every timestamp here is written in UTC
 * ({@code hibernate.jdbc.time_zone}, and the drivers' {@code serverTimezone}).</p>
 *
 * <p>The way back is here too: a text written by a native statement is cut to its column's length
 * ({@link #truncated}).</p>
 */
public final class NativeValues {

    private NativeValues() {
    }

    /**
     * 🔢 A whole number, or zero for none.
     *
     * @param value the column
     * @return the number
     */
    public static long longOf(Object value) {
        if (value == null) {
            return 0;
        }

        return ((Number) value).longValue();
    }

    /**
     * 🔢 A whole number that fits an {@code int}.
     *
     * @param value the column
     * @return the number
     */
    public static int intOf(Object value) {
        return Math.toIntExact(longOf(value));
    }

    /**
     * 🔢 A floating-point number, or {@code null} for none.
     *
     * @param value the column
     * @return the number
     */
    public static Double doubleOf(Object value) {
        if (value == null) {
            return null;
        }

        return ((Number) value).doubleValue();
    }

    /**
     * ✅ A flag, whether the driver answers with a boolean or a number.
     *
     * @param value the column
     * @return the flag; {@code null} for none
     */
    public static Boolean booleanOf(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean flag) {
            return flag;
        }

        return ((Number) value).intValue() != 0;
    }

    /**
     * 🕰️ An instant, or {@code null} for none.
     *
     * <p>A {@code DATETIME} column of a native query comes back as a {@link LocalDateTime}: the stored moment, read in the
     * zone Hibernate stores in ({@code hibernate.jdbc.time_zone}), written out as the JVM's wall clock — so it is turned
     * back through the JVM's zone. Read as UTC, every native timestamp on a machine three hours east of it stood three
     * hours in the future: a worker "started in 3 hours".</p>
     *
     * @param value the column
     * @return the instant
     */
    public static Instant instantOf(Object value) {
        return switch (value) {
            case null -> null;
            case Instant instant -> instant;
            case Timestamp timestamp -> timestamp.toInstant();
            case LocalDateTime local -> local.atZone(ZoneId.systemDefault()).toInstant();
            case OffsetDateTime offset -> offset.toInstant();
            default -> throw new IllegalStateException(
                    "A timestamp column came back as " + value.getClass().getName());
        };
    }

    /**
     * 📝 A text column, or {@code null} for none.
     *
     * @param value the column
     * @return the text
     */
    public static String stringOf(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Clob clob) {
            try {
                return clob.getSubString(1, Math.toIntExact(clob.length()));
            } catch (SQLException exception) {
                throw new IllegalStateException("A text column could not be read", exception);
            }
        }

        return value.toString();
    }

    /**
     * ✂️ A text cut to the length its column holds, so a long message is shortened rather than refused by the database.
     *
     * @param text          the text, or {@code null}
     * @param maximumLength the column's length
     * @return the text, at most that long; {@code null} for none
     */
    public static String truncated(String text, int maximumLength) {
        if (text == null || text.length() <= maximumLength) {
            return text;
        }

        return text.substring(0, maximumLength);
    }
}
