package org.jmouse.revisions;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The name a person says for one revision: the day it was written, in its owner's time zone, and its ordinal among that
 * day's revisions of the same thing — {@code 20261020_002}. Written beside the thing's name it reads
 * {@code CLAUDE.md@20261020_002}, {@code lm358@20261023_001}.
 *
 * <p>A projection, never an identity: the store's revision number stays the key. A revision is immutable, so its label
 * never moves once given — which is why a store writes the day and the ordinal once, when the revision is written, rather
 * than working them out on reading: a changed time zone would move every label already handed out.
 *
 * @param day     the day in the owner's time zone
 * @param ordinal the one-based position among that day's revisions of the same thing
 */
public record RevisionLabel(LocalDate day, int ordinal) {

    private static final DateTimeFormatter DAY     = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Pattern           TOKEN   = Pattern.compile("(\\d{8})_(\\d{3,})");
    private static final Pattern           ONE_DAY = Pattern.compile("\\d{8}");

    public RevisionLabel {
        if (day == null || ordinal < 1) {
            throw new IllegalArgumentException("A revision label needs a day and an ordinal of one or more.");
        }
    }

    /**
     * The label of a revision written on {@code day} after {@code writtenThatDay} others of the same thing.
     *
     * @param writtenThatDay how many revisions of it were written that day before this one
     */
    public static RevisionLabel next(LocalDate day, int writtenThatDay) {
        return new RevisionLabel(day, writtenThatDay + 1);
    }

    /** The label alone: {@code 20261020_002}. */
    public String token() {
        return DAY.format(day) + "_" + String.format(Locale.ROOT, "%03d", ordinal);
    }

    /** The label with the name it belongs to: {@code CLAUDE.md@20261020_002}. */
    public String of(String name) {
        return name + "@" + token();
    }

    @Override
    public String toString() {
        return token();
    }

    /**
     * What a revision reference after an {@code @} names: one revision, the last revision of a day, or the latest one.
     */
    public sealed interface Reference permits Exact, LastOfDay, Latest {

        /**
         * Reads {@code 20261020_002}, {@code 20261020} or {@code latest}.
         *
         * @throws IllegalArgumentException for anything else
         */
        static Reference parse(String text) {
            String value = text == null ? "" : text.strip();
            if (value.equalsIgnoreCase("latest")) {
                return new Latest();
            }
            Matcher exact = TOKEN.matcher(value);
            try {
                if (exact.matches()) {
                    return new Exact(new RevisionLabel(LocalDate.parse(exact.group(1), DAY), Integer.parseInt(exact.group(2))));
                }
                if (ONE_DAY.matcher(value).matches()) {
                    return new LastOfDay(LocalDate.parse(value, DAY));
                }
            } catch (DateTimeParseException | NumberFormatException malformed) {
                throw new IllegalArgumentException("«" + value + "» is not a day written as YYYYMMDD.");
            }
            throw new IllegalArgumentException("A revision is written as YYYYMMDD_NNN, YYYYMMDD or latest, not «" + value + "».");
        }

        /** Whether {@code text} is written as a revision reference at all, rather than as a store's own revision number. */
        static boolean written(String text) {
            String value = text == null ? "" : text.strip();
            return value.equalsIgnoreCase("latest") || TOKEN.matcher(value).matches() || ONE_DAY.matcher(value).matches();
        }
    }

    /** One revision by its label. */
    public record Exact(RevisionLabel label) implements Reference {
    }

    /** The last revision written on a day. */
    public record LastOfDay(LocalDate day) implements Reference {
    }

    /** The current revision. */
    public record Latest() implements Reference {
    }
}
