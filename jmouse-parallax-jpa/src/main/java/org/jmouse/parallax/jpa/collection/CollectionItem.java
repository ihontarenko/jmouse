package org.jmouse.parallax.jpa.collection;

import java.time.Instant;

/**
 * 📌 One entry of a collection, as its row holds it. Exactly one of the three references is set; which one is
 * {@link #kind()}.
 *
 * @param id           the item
 * @param position     its place in the collection, from zero
 * @param kind         what it points at
 * @param objectId     the object, for an {@link Kind#OBJECT} item
 * @param sceneId      the scene, for a {@link Kind#SCENE} item
 * @param comparisonId the comparison, for a {@link Kind#COMPARISON} item
 * @param note         what the curator wrote beside it, or {@code null}
 * @param section      the title of the section it is filed under; {@code null} in an unsectioned collection
 * @param addedBy      who added it
 * @param addedAt      when
 */
public record CollectionItem(String id, int position, Kind kind, String objectId, String sceneId, String comparisonId,
                             String note, String section, String addedBy, Instant addedAt) {

    /** 🧭 What an item points at. */
    public enum Kind { OBJECT, SCENE, COMPARISON }
}
