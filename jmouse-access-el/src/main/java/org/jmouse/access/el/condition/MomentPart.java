package org.jmouse.access.el.condition;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The parts of a moment a policy condition may ask {@code now()} about.
 *
 * <h2>⚠️ Everything here has to be comparable on its own</h2>
 *
 * <p>{@link ConditionVocabulary} admits no arithmetic — no {@code +}, {@code -}, {@code *}, {@code /} —
 * so a rule can only ever <em>compare</em>. A moment therefore has to arrive already in a form whose
 * natural order is the order a reader means:
 *
 * <ul>
 *   <li>{@link #TIME} is zero-padded {@code HH:mm} precisely so that string order <strong>is</strong>
 *       chronological order — {@code '09:00' < '18:30'} holds as text.</li>
 *   <li>{@link #DATE} is ISO for the same reason.</li>
 *   <li>{@link #MINUTE_OF_DAY} exists so a range that crosses an hour needs no arithmetic either.</li>
 * </ul>
 *
 * <p>Anything wanting subtraction — <em>how long ago</em> — is a different function, not a part here.
 */
public enum MomentPart {

    /** The calendar year, {@code 2026}. */
    YEAR("year"),

    /** The month, {@code 1}–{@code 12}. ⚠️ One-based, unlike {@code java.util.Calendar}. */
    MONTH("month"),

    /** The day of the month, {@code 1}–{@code 31}. */
    DAY("day"),

    /** The day of the week, {@code 1} = Monday … {@code 7} = Sunday — ISO-8601, so a weekend is {@code >= 6}. */
    WEEKDAY("weekday"),

    /** The hour, {@code 0}–{@code 23}. */
    HOUR("hour"),

    /** The minute within the hour, {@code 0}–{@code 59}. Rarely useful alone; see {@link #MINUTE_OF_DAY}. */
    MINUTE("minute"),

    /** Minutes since midnight, {@code 0}–{@code 1439}. {@code 18:00} is {@code 1080}. */
    MINUTE_OF_DAY("minute-of-day"),

    /** The date as {@code 2026-08-21}, which compares as text in calendar order. */
    DATE("date"),

    /** The time of day as {@code 18:30}, which compares as text in clock order. */
    TIME("time"),

    /** Seconds since the epoch. Here for completeness; rarely what a readable rule wants. */
    EPOCH("epoch");

    private final String keyword;

    MomentPart(String keyword) {
        this.keyword = keyword;
    }

    /** How the part is written in a policy file. */
    public String keyword() {
        return keyword;
    }

    /** What this part of the given moment is, as a value a condition can compare. */
    public Object read(ZonedDateTime moment) {
        return switch (this) {
            case YEAR          -> (long) moment.getYear();
            case MONTH         -> (long) moment.getMonthValue();
            case DAY           -> (long) moment.getDayOfMonth();
            case WEEKDAY       -> (long) moment.getDayOfWeek().getValue();
            case HOUR          -> (long) moment.getHour();
            case MINUTE        -> (long) moment.getMinute();
            case MINUTE_OF_DAY -> (long) (moment.getHour() * 60 + moment.getMinute());
            case DATE          -> "%04d-%02d-%02d".formatted(
                    moment.getYear(), moment.getMonthValue(), moment.getDayOfMonth());
            case TIME          -> "%02d:%02d".formatted(moment.getHour(), moment.getMinute());
            case EPOCH         -> moment.toEpochSecond();
        };
    }

    /**
     * The part a policy file names, or a refusal listing the ones that would have worked.
     *
     * <p>⚠️ Called at load as well as at evaluation, which is the point: {@code now('hours')} that only
     * failed on the first request would boot clean and then refuse everybody.
     */
    public static MomentPart parse(String written) {
        if (written == null || written.isBlank()) {
            throw new IllegalArgumentException(
                    ("now(part) needs to say which part of the moment it means — for example "
                     + "now('hour'). Parts: %s.").formatted(keywords()));
        }

        String trimmed = written.trim();

        for (MomentPart part : values()) {
            if (part.keyword.equalsIgnoreCase(trimmed)) {
                return part;
            }
        }

        throw new IllegalArgumentException(
                ("'%s' is not a part of a moment, so this would never answer anything a rule could "
                 + "compare. Parts: %s.").formatted(written, keywords()));
    }

    private static String keywords() {
        return Arrays.stream(values()).map(MomentPart::keyword).collect(Collectors.joining(", "));
    }
}
