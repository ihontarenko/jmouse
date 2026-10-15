package org.jmouse.scheduler.spring;

import org.jmouse.scheduler.SchedulerSettings;

import java.time.Duration;

/**
 * Everything a product configures under {@code jmouse.scheduler}.
 *
 * <pre>{@code
 * jmouse:
 *   scheduler:
 *     enabled: true                # ⚠️ default false: nothing is wired until this is set
 *     polling: true                # run the firing loop in this process
 *     poll-interval: 5s
 *     maximum-jitter: 1s
 *     misfire-threshold: 1m
 *     batch-size: 100
 *     lease-duration: 30s        # how long a claim is in flight before another runner re-dispatches it
 *     store:
 *       type: jdbc                 # jdbc | memory
 *       history-limit: 200         # records kept per trigger
 *       initialize-schema: false   # create the tables at start-up when missing
 * }</pre>
 *
 * <p>⚠️ <strong>These keys are a published contract.</strong> Every adopting product writes them into
 * its own configuration, so renaming one is a break in each of them at once.
 *
 * <p>Bound with Spring's {@code Binder} rather than {@code @ConfigurationProperties}-scanned, so the
 * record stays a plain value a test can construct without a context. Absent values take the defaults
 * in the compact constructor.
 *
 * @param enabled          whether the scheduler is wired at all
 * @param polling          whether this process runs the firing loop; {@code false} for a process that
 *                         only registers and reads triggers
 * @param pollInterval     the pause between passes
 * @param maximumJitter    the random extra pause, at most {@code pollInterval}
 * @param misfireThreshold how late an occurrence may be found and still be on time
 * @param batchSize        the most due triggers one pass takes
 * @param leaseDuration    how long a claim holds occurrences in flight, on the database's clock
 * @param store            where triggers live
 */
public record SchedulerProperties(
        Boolean enabled,
        Boolean polling,
        Duration pollInterval,
        Duration maximumJitter,
        Duration misfireThreshold,
        Integer batchSize,
        Duration leaseDuration,
        Store store
) {

    public SchedulerProperties {
        SchedulerSettings defaults = SchedulerSettings.DEFAULTS;
        enabled = enabled != null && enabled;
        polling = polling == null || polling;
        pollInterval = pollInterval == null ? defaults.pollInterval() : pollInterval;
        maximumJitter = maximumJitter == null ? defaults.maximumJitter() : maximumJitter;
        misfireThreshold = misfireThreshold == null ? defaults.misfireThreshold() : misfireThreshold;
        batchSize = batchSize == null ? defaults.batchSize() : batchSize;
        leaseDuration = leaseDuration == null ? defaults.leaseDuration() : leaseDuration;
        store = store == null ? new Store(null, null, null) : store;
    }

    /** Every value at its default. */
    public static SchedulerProperties defaults() {
        return new SchedulerProperties(null, null, null, null, null, null, null, null);
    }

    /** The runner's settings; validated here, so a bad combination fails the start-up. */
    public SchedulerSettings settings() {
        return new SchedulerSettings(pollInterval, maximumJitter, misfireThreshold, batchSize, leaseDuration);
    }

    /** Which store backs the scheduler. */
    public enum StoreType {

        /** {@code JdbcTriggerStore} over the application's {@code DataSource}: durable, shared by processes. */
        JDBC,

        /** {@code InMemoryTriggerStore}: lost on restart, one process only. */
        MEMORY
    }

    /**
     * @param type             {@code jdbc} (default) or {@code memory}
     * @param historyLimit     records kept per trigger; default 200
     * @param initializeSchema create the tables at start-up when missing; default {@code false}, because
     *                         a product with migrations should own when its tables appear
     */
    public record Store(StoreType type, Integer historyLimit, Boolean initializeSchema) {

        public Store {
            type = type == null ? StoreType.JDBC : type;
            historyLimit = historyLimit == null ? 200 : historyLimit;
            initializeSchema = initializeSchema != null && initializeSchema;
        }
    }
}
