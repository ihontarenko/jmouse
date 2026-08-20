package org.jmouse.access;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * The window a consumed quantity is counted over — a calendar period, or a duration.
 *
 * <h2>Why {@link AllowancePeriod} was not simply given a fifth constant</h2>
 *
 * <p>That enum defends being four, and its reasoning is right about what it is for: <em>"a period a
 * product cannot explain in a sentence on an invoice is a period nobody can reconcile against one."</em>
 * Billing rhythms are days, months and years.
 *
 * <p>⚠️ But <em>"100k tokens per 3 hours"</em> is not a billing rhythm. It is a guard on a bill nobody
 * wants, it is never printed on an invoice, and the argument for four does not reach it. Two different
 * jobs, so two shapes — and the enum stays the vocabulary a plan is sold in.
 *
 * <h2>⚠️ Tumbling, not rolling — and everything downstream must say so</h2>
 *
 * <p>A duration window is a bucket derived from the clock, so at a boundary somebody may spend the
 * whole allowance at 11:59 and the whole allowance again at 12:01. That is the honest cost of reusing
 * a counter row, and it buys resetting nothing, surviving a process that was down, and one lookup per
 * decision.
 *
 * <p>A genuinely rolling window is a token bucket, not a counter, and is a different mechanism standing
 * beside this one. If the boundary turns out to matter, that is the answer — <strong>not</strong> a
 * cleverer key.
 *
 * <h2>⚠️ A duration must divide the day</h2>
 *
 * <p>Buckets are numbered within a day so that a key stays readable and sortable —
 * {@code 2026-08-16-04} is the fourth three-hour block. A duration that does not divide 24 hours would
 * leave a short bucket every midnight, which is a quota that silently shrinks once a day, so it is
 * refused when it is written rather than discovered when somebody is throttled early.
 */
public sealed interface ConsumptionWindow {

    /** A window that never rolls over — the whole life of the subject. */
    ConsumptionWindow EVER = new Calendar(AllowancePeriod.EVER);

    /**
     * Reads a window as a policy file spells it: {@code day}, {@code month}, {@code year},
     * {@code ever}, or a duration such as {@code 3h} or {@code 15m}.
     *
     * @throws IllegalArgumentException naming what would have worked
     */
    static ConsumptionWindow parse(String written) {
        if (written == null || written.isBlank()) {
            throw new IllegalArgumentException(
                    "a window has to say what it is: day, month, year, ever, or a duration like '3h'");
        }

        String trimmed = written.trim();

        for (AllowancePeriod period : AllowancePeriod.values()) {
            if (period.name().equalsIgnoreCase(trimmed)) {
                return new Calendar(period);
            }
        }

        return new Rolling(durationOf(trimmed));
    }

    static ConsumptionWindow of(AllowancePeriod period) {
        return new Calendar(period);
    }

    /**
     * Which window a moment falls in, as the string a counter row is keyed by.
     *
     * <p>⚠️ Takes a civil moment rather than an instant: a calendar window is a question about
     * somebody's day, and whose day that is belongs to the caller rather than to this type.
     */
    String keyFor(LocalDateTime moment);

    /** When the current window ends and the allowance returns, or null where it never does. */
    LocalDateTime nextRolloverAfter(LocalDateTime moment);

    /** How to say this to a reader: "per month", "per 3h", "in total". */
    String describe();

    /** A calendar period — the rhythm a plan is actually sold in. */
    record Calendar(AllowancePeriod period) implements ConsumptionWindow {

        @Override
        public String keyFor(LocalDateTime moment) {
            return switch (period) {
                case DAY   -> "%04d-%02d-%02d".formatted(
                        moment.getYear(), moment.getMonthValue(), moment.getDayOfMonth());
                case MONTH -> "%04d-%02d".formatted(moment.getYear(), moment.getMonthValue());
                case YEAR  -> "%04d".formatted(moment.getYear());
                case EVER  -> "ever";
            };
        }

        @Override
        public LocalDateTime nextRolloverAfter(LocalDateTime moment) {
            return switch (period) {
                case DAY   -> moment.truncatedTo(ChronoUnit.DAYS).plusDays(1);
                case MONTH -> moment.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).plusMonths(1);
                case YEAR  -> moment.truncatedTo(ChronoUnit.DAYS).withDayOfYear(1).plusYears(1);
                case EVER  -> null;
            };
        }

        @Override
        public String describe() {
            return period == AllowancePeriod.EVER ? "in total" : "per " + period.name().toLowerCase();
        }
    }

    /** A fixed-length bucket within the day — the protective kind. */
    record Rolling(Duration length) implements ConsumptionWindow {

        private static final long SECONDS_IN_A_DAY = 86_400L;

        public Rolling {
            if (length == null || length.isZero() || length.isNegative()) {
                throw new IllegalArgumentException("a window has to be longer than nothing");
            }

            if (SECONDS_IN_A_DAY % length.toSeconds() != 0) {
                throw new IllegalArgumentException(
                        ("a window of %s does not divide a day, so one bucket every midnight would be "
                         + "short — and a quota that shrinks once a day is worse than one that is "
                         + "refused now. Use a length that divides 24h: 15m, 30m, 1h, 2h, 3h, 4h, 6h, "
                         + "8h, 12h.").formatted(length));
            }
        }

        @Override
        public String keyFor(LocalDateTime moment) {
            return "%04d-%02d-%02d-%02d".formatted(
                    moment.getYear(), moment.getMonthValue(), moment.getDayOfMonth(), bucketIn(moment));
        }

        @Override
        public LocalDateTime nextRolloverAfter(LocalDateTime moment) {
            return moment.truncatedTo(ChronoUnit.DAYS).plusSeconds((bucketIn(moment) + 1) * length.toSeconds());
        }

        @Override
        public String describe() {
            return "per " + length.toString().substring(2).toLowerCase();
        }

        private long bucketIn(LocalDateTime moment) {
            return moment.toLocalTime().toSecondOfDay() / length.toSeconds();
        }
    }

    private static Duration durationOf(String written) {
        try {
            // `PT` prefixed so that a policy file may write the short, readable `3h` rather than the
            // ISO-8601 `PT3H` nobody types by choice.
            return Duration.parse("PT" + written);
        } catch (RuntimeException unreadable) {
            throw new IllegalArgumentException(
                    ("'%s' is not a window. Write a calendar period — day, month, year, ever — or a "
                     + "duration such as 3h or 15m.").formatted(written), unreadable);
        }
    }
}
