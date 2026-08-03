package org.jmouse.storage.spring;

import org.jmouse.storage.jpa.sweeper.OrphanSweeper;
import org.jmouse.storage.jpa.sweeper.SweepReport;
import org.jmouse.storage.jpa.sweeper.SweepRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ⏰ Runs the orphan sweep on a schedule, one transaction per batch.
 *
 * <p>In the library rather than per product, because every product needs exactly this and the first
 * product to write it by hand is the first product to get the transaction boundary wrong. The
 * second product not to write it at all is worse still: bindings stop deleting bytes the moment the
 * registry arrives, so a product with no sweeper leaks every file it has ever deleted.</p>
 *
 * <p>The library hands back a run to pump rather than sweeping by itself, precisely so this decision
 * lives outside it: sweeping a large registry inside one transaction holds locks for as long as it
 * takes and loses every reclaimed byte to a single failure at the end, while sweeping in no
 * transaction at all is not a library's call to make. One transaction per batch is the answer for
 * an application that has a transaction manager, which is what this class knows and the sweeper
 * does not.</p>
 *
 * <p><strong>Safe to interrupt, safe to run twice.</strong> Stopping between batches loses nothing —
 * what was reclaimed stays reclaimed and what was not is found again next time — which matters
 * because schedulers get restarted.</p>
 *
 * <p>Scheduling needs {@code @EnableScheduling} in the application; without it this bean is built
 * and never fires, and {@link #sweepNow()} still works.</p>
 */
public class ScheduledOrphanSweep {

    /**
     * 🕒 Property carrying the cron expression the sweep runs on.
     */
    public static final String SCHEDULE_PROPERTY = "jmouse.storage.sweeper.schedule";

    /**
     * 🕒 Nightly at 03:00, when nothing else is competing for the bucket.
     */
    public static final String DEFAULT_SCHEDULE = "0 0 3 * * *";

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledOrphanSweep.class);

    private final OrphanSweeper       sweeper;
    private final TransactionTemplate transactionTemplate;

    /**
     * 🏗️ Sweep on the configured schedule, in the application's own transactions.
     *
     * @param sweeper             the sweeper to drive
     * @param transactionTemplate what each batch runs inside
     */
    public ScheduledOrphanSweep(OrphanSweeper sweeper, TransactionTemplate transactionTemplate) {
        this.sweeper             = sweeper;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * ⏰ The scheduled sweep, which does nothing until an operator turns it on.
     *
     * <p>The cron expression is read from one fixed property rather than from the product's own
     * settings prefix. A {@code @Scheduled} expression is resolved by Spring long before the
     * settings record is bound, so it cannot follow a prefix that is itself configuration —
     * pretending otherwise would give a nested placeholder that quietly falls back to the default
     * and a schedule nobody can explain.</p>
     */
    @Scheduled(cron = "${" + SCHEDULE_PROPERTY + ":" + DEFAULT_SCHEDULE + "}")
    public void sweepOnSchedule() {
        if (!sweeper.isEnabled()) {
            return;
        }

        sweepNow();
    }

    /**
     * ▶️ Sweep immediately, whatever the schedule says.
     *
     * <p>Deliberately does not consult whether the sweeper is enabled: asking for a sweep <em>is</em>
     * the decision, and an operator running one by hand should not have to edit configuration first.
     * Expose this from an admin endpoint or an actuator operation to make it reachable.</p>
     *
     * @return what the sweep did — objects reclaimed and bytes freed
     */
    public SweepReport sweepNow() {
        SweepRun run = sweeper.begin();

        while (run.hasMore()) {
            transactionTemplate.executeWithoutResult(status -> run.sweepNextBatch());
        }

        SweepReport report = run.report();
        LOGGER.info("🧹 Orphan sweep finished — {}", report);

        return report;
    }
}
