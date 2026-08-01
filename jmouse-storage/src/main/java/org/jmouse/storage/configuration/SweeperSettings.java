package org.jmouse.storage.configuration;

import org.jmouse.core.binding.BindDefault;

import java.time.Duration;

/**
 * 🧹 When unreferenced objects are reclaimed.
 *
 * <p>{@link #gracePeriod} is not a tuning knob to shorten casually: it is what stops the sweeper
 * from deleting an object written moments ago by a transaction that has not committed yet, and so
 * has no row pointing at it to find.</p>
 *
 * @param enabled     whether the sweeper runs at all
 * @param gracePeriod how long an unreferenced object is left alone before it is a candidate
 * @param schedule    cron expression the sweep runs on
 */
public record SweeperSettings(@BindDefault("false") boolean enabled,
                              @BindDefault(SweeperSettings.DEFAULT_GRACE_PERIOD) Duration gracePeriod,
                              @BindDefault(SweeperSettings.DEFAULT_SCHEDULE) String schedule) {

    /**
     * ⏳ A day, long enough that no in-flight transaction is still uncommitted.
     */
    public static final String DEFAULT_GRACE_PERIOD = "PT24H";

    /**
     * 🕒 Nightly, at 03:00.
     */
    public static final String DEFAULT_SCHEDULE = "0 0 3 * * *";

    /**
     * 🏗️ Fill in whatever configuration omitted.
     */
    public SweeperSettings {
        gracePeriod = (gracePeriod == null) ? Duration.parse(DEFAULT_GRACE_PERIOD) : gracePeriod;
        schedule    = (schedule == null || schedule.isBlank()) ? DEFAULT_SCHEDULE : schedule;
    }

    /**
     * 🏗️ The shipped defaults — off, because reclaiming bytes is a decision an operator makes.
     *
     * @return default sweeper settings
     */
    public static SweeperSettings defaults() {
        return new SweeperSettings(false, null, null);
    }
}
