package org.jmouse.avatar;

import org.jmouse.storage.jpa.StoredFile;

/**
 * 🪪 The row a face belongs to, as this module needs to see it.
 *
 * <h3>Why an interface rather than an entity</h3>
 *
 * <p>Because the row is the product's: {@code members} in two of them, {@code security_users} in a
 * third, and there is no world in which this library owns a people table. What it does own is the
 * <em>rule</em> — which of three kinds is worn, what a seed may look like, what a picture may be — and
 * that rule was written twice, identically, before it was extracted.</p>
 *
 * <p>⚠️ Implemented by the product's own entity, so a change made here is written back by the product's
 * own repository inside the product's own transaction. This module persists nothing.</p>
 */
public interface AvatarOwner {

    /**
     * 🆔 Which row this is, as the product identifies it.
     *
     * @return the owner's identifier
     */
    String avatarOwnerId();

    /**
     * 🙂 Which of the three kinds is worn.
     *
     * @return the choice
     */
    AvatarChoice avatarChoice();

    /**
     * 🎲 The seed a generated face is drawn from, or {@code null} where none is worn.
     *
     * @return the seed
     */
    String avatarSeed();

    /**
     * 🖼️ The uploaded picture, or {@code null} where none is worn.
     *
     * @return the stored file
     */
    StoredFile avatarFile();

    /**
     * 🎲 Wear a generated face.
     *
     * @param seed what to draw it from
     */
    void wearsPreset(String seed);

    /**
     * 🖼️ Wear a picture.
     *
     * @param picture the stored file
     */
    void wearsPicture(StoredFile picture);

    /**
     * ✍️ Wear drawn initials.
     */
    void wearsInitials();
}
