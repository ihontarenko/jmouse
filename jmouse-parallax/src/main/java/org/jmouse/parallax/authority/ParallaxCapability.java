package org.jmouse.parallax.authority;

/**
 * What Parallax asks to be allowed. The library names capabilities only; the host decides which of its own
 * permissions each one means, and at which place.
 */
public enum ParallaxCapability {

    /** Write the shared catalogue: objects, categories, instruments. */
    CATALOGUE_WRITE,
    /** Change an object's status in the shared catalogue (draft, published, retired). */
    CATALOGUE_PUBLISH,
    /** See what an owner holds — its objects, its jobs. */
    OWNER_READ,
    /** Read scenes, comparisons and an owner's objects. */
    SCENE_READ,
    /** Create and change scenes, comparisons and an owner's objects. */
    SCENE_WRITE,
    SCENE_DELETE,
    /** Hand a scene out by link. */
    SCENE_SHARE,
    /** Run a computation for an owner (inline, queued or live). */
    COMPUTE,
    JOB_READ,
    JOB_CANCEL,
    /** See and steer every job of the installation. */
    JOB_ADMINISTER,
    COLLECTION_CREATE,
    /** Curate collections other people see (publish one, keep the catalogue's shelves). */
    COLLECTION_CURATE,
    EVALUATION_READ,
    EVALUATION_RUN,
    /** Read the machinery's state: warm-up, queue, cache, live runs. */
    MANAGEMENT_READ,
    /** Steer the machinery: start a warm-up. */
    MANAGEMENT_WRITE
}
