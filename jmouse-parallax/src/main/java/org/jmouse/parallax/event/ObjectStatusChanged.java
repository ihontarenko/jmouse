package org.jmouse.parallax.event;

/**
 * A catalogue object was published, archived or returned to draft; published inside the transaction that changes it.
 *
 * @param status the name of the status it now has — {@code PUBLISHED}, {@code ARCHIVED}, {@code DRAFT}; a name rather
 *               than the entity's own enum, because the engine does not know the store's types
 */
public record ObjectStatusChanged(String objectId, String slug, String status, String actorId) {
}
