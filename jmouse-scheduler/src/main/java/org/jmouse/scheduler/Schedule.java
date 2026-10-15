package org.jmouse.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * When a trigger fires. Data, not an annotation: a schedule is stored with its trigger, survives a
 * restart and can be changed at run time.
 *
 * <p>Sealed, so every switch over the kinds is checked by the compiler when a fourth one is added.
 * Every computation takes the instant to search from as an argument — none of them reads a clock —
 * so the time a scheduler believes it is comes from exactly one place, the injected {@link java.time.Clock}.
 *
 * <p>Instants are kept to the millisecond — the resolution a store writes — so a schedule read back
 * from a store is equal to the one that was saved, and re-registering it keeps its next firing.
 */
public sealed interface Schedule permits Schedule.Cron, Schedule.Interval, Schedule.Once {

    /**
     * The first firing strictly after {@code after}.
     *
     * @param after exclusive lower bound
     * @return the next firing, or empty when this schedule has no more
     */
    Optional<Instant> nextAfter(Instant after);

    /**
     * The first firing of a trigger registered at {@code registeredAt}.
     *
     * <p>For a recurring schedule that is the next firing after registration. For {@link Once} it is
     * the configured instant <em>even when that is already past</em>, so a one-off registered late is
     * still subject to its misfire policy rather than silently never firing.
     *
     * @param registeredAt the moment of registration
     * @return the first firing, or empty when there is none
     */
    Optional<Instant> firstFiring(Instant registeredAt);

    /**
     * A cron schedule evaluated against the wall clock of {@code zone}.
     *
     * @param expression a five- or six-field expression; see {@link CronExpression}
     * @param zone       the zone, always explicit — a server's default zone is never consulted
     * @return the schedule
     */
    static Cron cron(String expression, ZoneId zone) {
        return new Cron(CronExpression.parse(expression), zone);
    }

    /**
     * A fixed period aligned to the epoch, so {@code every(Duration.ofHours(1))} fires on the hour
     * (UTC) and registering it again yields an equal schedule.
     *
     * @param period at least one second
     * @return the schedule
     */
    static Interval every(Duration period) {
        return new Interval(period, Instant.EPOCH);
    }

    /**
     * A fixed period aligned to {@code anchor}: it fires at {@code anchor + k × period}.
     *
     * @param period at least one second
     * @param anchor any instant on the grid
     * @return the schedule
     */
    static Interval every(Duration period, Instant anchor) {
        return new Interval(period, anchor);
    }

    /**
     * A single firing.
     *
     * @param instant when
     * @return the schedule
     */
    static Once once(Instant instant) {
        return new Once(instant);
    }

    /**
     * Wall-clock cron. Daylight-saving behaviour is described on {@link CronExpression}.
     *
     * @param expression the parsed expression
     * @param zone       the zone whose wall clock the expression describes
     */
    record Cron(CronExpression expression, ZoneId zone) implements Schedule {

        public Cron {
            Objects.requireNonNull(expression, "expression");
            Objects.requireNonNull(zone, "zone");
        }

        @Override
        public Optional<Instant> nextAfter(Instant after) {
            return expression.nextAfter(after, zone);
        }

        @Override
        public Optional<Instant> firstFiring(Instant registeredAt) {
            return nextAfter(registeredAt);
        }
    }

    /**
     * Absolute time: a period of elapsed time, unaffected by daylight saving.
     *
     * @param period at least one second
     * @param anchor an instant on the firing grid
     */
    record Interval(Duration period, Instant anchor) implements Schedule {

        public Interval {
            Objects.requireNonNull(period, "period");
            Objects.requireNonNull(anchor, "anchor");

            if (period.compareTo(Duration.ofSeconds(1)) < 0) {
                throw new IllegalArgumentException("An interval must be at least one second, not " + period + ".");
            }

            if (period.toNanosPart() % 1_000_000 != 0) {
                throw new IllegalArgumentException("An interval is a whole number of milliseconds, not " + period + ".");
            }

            anchor = anchor.truncatedTo(ChronoUnit.MILLIS);
        }

        @Override
        public Optional<Instant> nextAfter(Instant after) {
            if (after.isBefore(anchor)) {
                return Optional.of(anchor);
            }

            long periodNanoseconds = period.toNanos();
            long elapsedPeriods = Duration.between(anchor, after).toNanos() / periodNanoseconds;
            return Optional.of(anchor.plusNanos((elapsedPeriods + 1) * periodNanoseconds));
        }

        @Override
        public Optional<Instant> firstFiring(Instant registeredAt) {
            return nextAfter(registeredAt);
        }
    }

    /**
     * Exactly one firing.
     *
     * @param instant when
     */
    record Once(Instant instant) implements Schedule {

        public Once {
            Objects.requireNonNull(instant, "instant");
            instant = instant.truncatedTo(ChronoUnit.MILLIS);
        }

        @Override
        public Optional<Instant> nextAfter(Instant after) {
            return after.isBefore(instant) ? Optional.of(instant) : Optional.empty();
        }

        @Override
        public Optional<Instant> firstFiring(Instant registeredAt) {
            return Optional.of(instant);
        }
    }
}
