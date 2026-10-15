package org.jmouse.parallax.event;

/** A catalogue object was created; published inside the transaction that creates it. */
public record ObjectCreated(String objectId, String slug, String actorId) {
}
