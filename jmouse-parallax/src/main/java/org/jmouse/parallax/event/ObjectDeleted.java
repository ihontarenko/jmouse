package org.jmouse.parallax.event;

/**
 * An object was deleted; published inside the transaction that removes it, so whatever a host keeps about it elsewhere —
 * the categories it was filed in — goes in the same commit.
 *
 * @param slug what the host knows it by: a host files objects by slug, which outlives the object's revisions
 */
public record ObjectDeleted(String objectId, String slug, String actorId) {
}
