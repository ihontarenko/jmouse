package org.jmouse.access;

import java.time.Duration;

/**
 * A length of time a policy file writes — {@code 15m}, {@code 3h}, {@code 7d}, {@code 2w}.
 *
 * <h2>Why it exists at all</h2>
 *
 * <p>A condition has <strong>no arithmetic</strong> — the restricted vocabulary admits no {@code +},
 * {@code -}, {@code *} or {@code /} — so <em>"how long ago"</em> cannot be computed inside a rule. It has
 * to arrive as a span somebody wrote and a test that applies it.
 *
 * <h2>⚠️ Deliberately not {@link ConsumptionWindow}</h2>
 *
 * <p>{@code ConsumptionWindow.Rolling} already parses {@code 3h}, and <strong>refuses any length that
 * does not divide a day</strong> — it keys tumbling buckets, and a bucket cut short every midnight is a
 * quota that silently shrinks once a day.
 *
 * <p>That rule is right there and wrong here. {@code 7d} and {@code 45m} are perfectly good ages, and a
 * retention rule has no buckets to keep aligned. Merging the two would mean either losing the divisor
 * check or refusing {@code 30d}, and both are worse than two small parsers that each say what they are
 * for.
 */
public record TimeSpan(Duration length) {

    private static final String SPELLING =
            "write a number and a unit: s (seconds), m (minutes), h (hours), d (days), w (weeks) — "
            + "for example 15m, 3h, 7d, 2w";

    public TimeSpan {
        if (length == null || length.isZero() || length.isNegative()) {
            throw new IllegalArgumentException("a span has to be longer than nothing. " + SPELLING);
        }
    }

    /**
     * The span a policy file wrote, or a refusal naming the spelling that works.
     *
     * <p>⚠️ Called at load as well as at evaluation. A span only checked on the first request would boot
     * clean and then refuse everybody — the failure {@code ConditionCalls} exists to prevent.
     */
    public static TimeSpan parse(String written) {
        if (written == null || written.isBlank()) {
            throw new IllegalArgumentException("a span cannot be empty. " + SPELLING);
        }

        String trimmed = written.trim().toLowerCase();
        char   unit    = trimmed.charAt(trimmed.length() - 1);
        String number  = trimmed.substring(0, trimmed.length() - 1);

        long quantity;

        try {
            quantity = Long.parseLong(number);
        } catch (NumberFormatException unreadable) {
            throw new IllegalArgumentException(
                    ("'%s' is not a span. " + SPELLING).formatted(written), unreadable);
        }

        return new TimeSpan(lengthOf(quantity, unit, written));
    }

    /** How long it is, as somebody would say it — for a refusal, and for an administration screen. */
    public String describe() {
        long seconds = length.toSeconds();

        if (seconds % 604_800L == 0) {
            return plural(seconds / 604_800L, "week");
        }

        if (seconds % 86_400L == 0) {
            return plural(seconds / 86_400L, "day");
        }

        if (seconds % 3_600L == 0) {
            return plural(seconds / 3_600L, "hour");
        }

        if (seconds % 60L == 0) {
            return plural(seconds / 60L, "minute");
        }

        return plural(seconds, "second");
    }

    private static Duration lengthOf(long quantity, char unit, String written) {
        return switch (unit) {
            case 's' -> Duration.ofSeconds(quantity);
            case 'm' -> Duration.ofMinutes(quantity);
            case 'h' -> Duration.ofHours(quantity);
            case 'd' -> Duration.ofDays(quantity);
            case 'w' -> Duration.ofDays(quantity * 7L);
            default  -> throw new IllegalArgumentException(
                    ("'%s' in '%s' is not a unit of time. " + SPELLING).formatted(unit, written));
        };
    }

    private static String plural(long quantity, String unit) {
        return quantity + " " + unit + (quantity == 1 ? "" : "s");
    }
}
