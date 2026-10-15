package org.jmouse.parallax.store.computation;

import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.time.Duration;
import java.time.Instant;

/**
 * ⏱️ The computation's recurring work, on a scheduler of its own: the worker's poll and its maintenance, and the
 * nightly forgetting of unused datasets.
 *
 * <h3>⚠️ Its own scheduler, not the host's {@code @EnableScheduling}</h3>
 *
 * <p>A library that relied on {@code @Scheduled} would compute nothing in a host that never enabled scheduling, and
 * nobody would be told. The scheduler here is private — not a bean — so it neither replaces the host's own
 * {@code TaskScheduler} nor runs the host's {@code @Scheduled} methods.</p>
 *
 * <p>It starts when the context has refreshed, as {@code @Scheduled} work does, and the schedule is the one the
 * annotations used to carry: the poll every {@code jmouse.parallax.computation.poll-interval} from the start, the
 * maintenance every thirty seconds after fifteen, the forgetting at 04:41 in the server's time zone.</p>
 */
public class ComputationSchedule implements SmartLifecycle {

    private static final String THREAD_NAME_PREFIX = "parallax-schedule-";
    private static final Duration MAINTENANCE_INTERVAL = Duration.ofSeconds(30);
    private static final Duration MAINTENANCE_DELAY = Duration.ofSeconds(15);
    private static final String FORGETTING_TIME = "0 41 4 * * *";

    private final ComputationWorker worker;
    private final Jobs jobs;
    private final ComputationProperties properties;
    private ThreadPoolTaskScheduler scheduler;

    /**
     * 🏗️ The schedule over the worker and the jobs.
     *
     * @param worker     the worker whose poll and maintenance run here
     * @param jobs       the jobs, whose unused datasets are forgotten nightly
     * @param properties the poll interval
     */
    public ComputationSchedule(ComputationWorker worker, Jobs jobs, ComputationProperties properties) {
        this.worker = worker;
        this.jobs = jobs;
        this.properties = properties;
    }

    @Override
    public synchronized void start() {
        if (scheduler != null) {
            return;
        }

        ThreadPoolTaskScheduler started = new ThreadPoolTaskScheduler();
        started.setPoolSize(1);
        started.setThreadNamePrefix(THREAD_NAME_PREFIX);
        started.setDaemon(true);
        started.initialize();

        started.scheduleWithFixedDelay(worker::poll, properties.pollInterval());
        started.scheduleWithFixedDelay(worker::maintain, Instant.now().plus(MAINTENANCE_DELAY), MAINTENANCE_INTERVAL);
        started.schedule(jobs::forgetUnusedDatasets, new CronTrigger(FORGETTING_TIME));

        scheduler = started;
    }

    @Override
    public synchronized void stop() {
        if (scheduler == null) {
            return;
        }

        scheduler.shutdown();
        scheduler = null;
    }

    @Override
    public synchronized boolean isRunning() {
        return scheduler != null;
    }
}
