package org.jmouse.scheduler;

/**
 * What happens to occurrences that were due while nothing was polling — downtime, a long pause, a
 * stopped runner.
 *
 * <p>An occurrence is <em>late</em> when it is found more than
 * {@link SchedulerSettings#misfireThreshold()} after its scheduled time; an occurrence found within the
 * threshold is on time and always fires, whatever the policy. The policy decides only about the late
 * ones, and every late occurrence it does not fire is recorded in history as one
 * {@link OccurrenceOutcome#SKIPPED} entry carrying the count.
 *
 * <p>⚠️ <strong>{@link #skip()} is the default</strong>, deliberately: a trigger that spends money
 * unattended must not multiply its spend after an outage unless somebody asked for that in writing.
 */
public sealed interface MisfirePolicy permits MisfirePolicy.Skip, MisfirePolicy.FireOnce, MisfirePolicy.CatchUp {

    /** The largest catch-up a single trigger may ask for in one pass. */
    int MAXIMUM_CATCH_UP = 1000;

    /** Drop every late occurrence and resume at the next future one. */
    static Skip skip() {
        return Skip.INSTANCE;
    }

    /**
     * Fire the most recent late occurrence once — unless an on-time occurrence fires in the same pass,
     * which already covers it — and drop the rest.
     */
    static FireOnce fireOnce() {
        return FireOnce.INSTANCE;
    }

    /**
     * Fire the most recent {@code maximumOccurrences} late occurrences, oldest first, and drop the
     * rest.
     *
     * @param maximumOccurrences between 1 and {@value #MAXIMUM_CATCH_UP}
     * @return the policy
     */
    static CatchUp catchUp(int maximumOccurrences) {
        return new CatchUp(maximumOccurrences);
    }

    /** How many late occurrences this policy keeps for firing; zero for {@link Skip}. */
    int retainedLateOccurrences();

    /** The stable name a store writes: {@code SKIP}, {@code FIRE_ONCE} or {@code CATCH_UP}. */
    String name();

    /** Drop every late occurrence. */
    final class Skip implements MisfirePolicy {

        private static final Skip INSTANCE = new Skip();

        private Skip() {
        }

        @Override
        public int retainedLateOccurrences() {
            return 0;
        }

        @Override
        public String name() {
            return "SKIP";
        }

        @Override
        public String toString() {
            return name();
        }
    }

    /** Fire the most recent late occurrence once. */
    final class FireOnce implements MisfirePolicy {

        private static final FireOnce INSTANCE = new FireOnce();

        private FireOnce() {
        }

        @Override
        public int retainedLateOccurrences() {
            return 1;
        }

        @Override
        public String name() {
            return "FIRE_ONCE";
        }

        @Override
        public String toString() {
            return name();
        }
    }

    /**
     * Fire up to {@code maximumOccurrences} late occurrences.
     *
     * @param maximumOccurrences between 1 and {@value MisfirePolicy#MAXIMUM_CATCH_UP}
     */
    record CatchUp(int maximumOccurrences) implements MisfirePolicy {

        public CatchUp {
            if (maximumOccurrences < 1 || maximumOccurrences > MAXIMUM_CATCH_UP) {
                throw new IllegalArgumentException("A catch-up is bounded between 1 and " + MAXIMUM_CATCH_UP
                        + " occurrences, not " + maximumOccurrences + ".");
            }
        }

        @Override
        public int retainedLateOccurrences() {
            return maximumOccurrences;
        }

        @Override
        public String name() {
            return "CATCH_UP";
        }
    }
}
