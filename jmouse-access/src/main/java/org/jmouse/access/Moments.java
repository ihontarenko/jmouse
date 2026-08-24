package org.jmouse.access;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Reading a moment out of whatever a product happened to put on the value a rule names.
 *
 * <p>A condition compares {@code resource.createdAt} without knowing, or wanting to know, whether that
 * field is an {@link Instant}, a {@link LocalDateTime}, a legacy {@link Date}, an epoch number or an ISO
 * string. Six products, six answers, one rule.
 *
 * <h2>⚠️ It refuses rather than guessing</h2>
 *
 * <p>A value it cannot read throws. The alternative — answering <em>"not a moment, so no"</em> — reads as
 * {@code false}, and <strong>a {@code false} inside a {@code deny} permits</strong>: a retention rule
 * whose timestamp was null would quietly stop refusing. Throwing sends it through
 * {@code ConditionFunctionFailure} to the axis, which applies the deny and drops the allow.
 */
public final class Moments {

    /**
     * ⚠️ Where epoch seconds stop and epoch milliseconds begin.
     *
     * <p>A bare number is genuinely ambiguous, and no amount of care removes that. This threshold is the
     * usual reading: below it, seconds — which runs to the year 5138; above it, milliseconds — which
     * covers everything after 1973. Anything a real system stores falls on the right side of it.
     *
     * <p>The one case it gets wrong is a moment in 1970 stored in milliseconds, read as seconds and
     * landing in 1973. A rule that cares about the difference should not be reading a bare number.
     */
    private static final long MILLISECONDS_BEGIN_ABOVE = 100_000_000_000L;

    private Moments() {
    }

    /**
     * The moment the value stands for, or a refusal saying what could not be read.
     *
     * @param value what the rule named
     * @param zone  the installation's zone, for the types that carry no offset of their own
     */
    public static Instant read(Object value, ZoneId zone) {
        Instant moment = find(value, zone);

        if (moment == null) {
            throw new IllegalArgumentException(
                    ("%s is not a moment in time, so nothing can be said about how long ago it was. A "
                     + "rule may name an Instant, a LocalDateTime, a ZonedDateTime, an OffsetDateTime, a "
                     + "LocalDate, a java.util.Date, an epoch number, or an ISO-8601 string.")
                            .formatted(describe(value)));
        }

        return moment;
    }

    /**
     * The moment the value stands for, or {@code null} where it is not one.
     *
     * <p>Use this only where <em>absent</em> is a real answer. In a rule it is not — see the class
     * javadoc — so a condition function should be calling {@link #read(Object, ZoneId)}.
     */
    public static Instant find(Object value, ZoneId zone) {
        ZoneId where = zone == null ? ZoneId.systemDefault() : zone;

        return switch (value) {
            case null                     -> null;
            case Instant instant          -> instant;
            case ZonedDateTime zoned      -> zoned.toInstant();
            case OffsetDateTime offset    -> offset.toInstant();
            case LocalDateTime local      -> local.atZone(where).toInstant();
            case LocalDate date           -> date.atStartOfDay(where).toInstant();
            case Date legacy              -> legacy.toInstant();
            case Number epoch             -> fromEpoch(epoch.longValue());
            case CharSequence written     -> fromText(written.toString(), where);
            default                       -> null;
        };
    }

    private static Instant fromEpoch(long epoch) {
        return Math.abs(epoch) < MILLISECONDS_BEGIN_ABOVE
                ? Instant.ofEpochSecond(epoch)
                : Instant.ofEpochMilli(epoch);
    }

    /**
     * ISO-8601, in the three shapes anybody writes: with an offset, without one, and a bare date.
     */
    private static Instant fromText(String written, ZoneId zone) {
        String trimmed = written.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException notAnInstant) {
            // Fall through — the other two shapes are just as ordinary as this one.
        }

        try {
            return LocalDateTime.parse(trimmed).atZone(zone).toInstant();
        } catch (DateTimeParseException notADateTime) {
            // Fall through.
        }

        try {
            return LocalDate.parse(trimmed).atStartOfDay(zone).toInstant();
        } catch (DateTimeParseException notADate) {
            return null;
        }
    }

    private static String describe(Object value) {
        if (value == null) {
            return "nothing";
        }

        return "'%s' (a %s)".formatted(value, value.getClass().getSimpleName());
    }
}
