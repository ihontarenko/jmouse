package org.jmouse.parallax.limits;

import java.time.Duration;

/**
 * How much Parallax may do, asked at the moment it is about to do it: a host that lets an administrator change a
 * limit at run time answers with the value of now, so nothing here may be cached by the caller.
 */
public interface ParallaxLimits {

    /** Function evaluations a representation may cost to be answered at once; above it, it becomes a queued job. */
    long inlineComputationBudget();

    /** The most a queued job may cost; a heavier request is refused. */
    long maximumJobCost();

    /** The largest dataset, uncompressed, that is kept or answered. */
    long maximumDatasetBytes();

    /** How often a job whose worker vanished is tried again. */
    int jobAttempts();

    /** How long a dataset nobody asks for stays in the cache. */
    Duration datasetRetention();

    /** Whether a reader who is not signed in may compute published catalogue objects. */
    boolean anonymousComputationAllowed();

    /** Heavy catalogue computations queued at once for readers without a workspace; 0 turns the public queue off. */
    long publicQueueLimit();

    /** Whether every published object's representations are computed at their defaults after a start. */
    boolean catalogueWarmupEnabled();

    /** The largest scene document a revision may hold, in bytes. */
    long maximumSceneDocumentBytes();

    /** Objects one scene may hold. */
    long maximumSceneObjects();

    /** Pins one symbol may draw: a 2 × 60 connector, a 144-lead chip. A guard against a drawing that is not a part. */
    int maximumSymbolPins();
}
