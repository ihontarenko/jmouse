package org.jmouse.files.management;

import org.jmouse.files.OwnerReference;

/**
 * 🚦 Whether this upload may happen at all — asked <strong>before</strong> a byte is written.
 *
 * <h3>Why not an event</h3>
 *
 * <p>{@link FileManagementEvent} tells a product what happened, which is the right shape for an audit
 * line or an activity entry and the wrong one for a quota: by the time an event is published the bytes
 * are in storage and the row is written, so a listener that objects can only undo. A full account would
 * be <em>filled and then reported</em> rather than refused.</p>
 *
 * <p>So this is asked first, and it refuses by throwing whatever the product's error model uses. The
 * library does not catch it — the product's own exception carries the product's own sentence, and one
 * translated through here would say less.</p>
 *
 * <h3>⚠️ The declared size is a claim, not a measurement</h3>
 *
 * <p>A multipart part reports what the client said, and a remote server importing over HTTP may lie
 * about {@code Content-Length} or omit it entirely — in which case this is asked with
 * {@link org.jmouse.storage.Content#UNKNOWN_SIZE}. So a check here can only refuse what is obviously
 * impossible. <strong>Charging is a second moment</strong>, done from
 * {@link FileManagementEvent.Uploaded} with the size the storage layer actually measured.</p>
 *
 * <p>Declaring no bean at all is the ordinary case: a product without quotas is not asked.</p>
 */
@FunctionalInterface
public interface UploadAllowance {

    /**
     * 🚦 Refuse, by throwing, if this owner may not store this much.
     *
     * @param owner        what the file will be filed against
     * @param declaredSize what the caller claims to be storing, or
     *                     {@link org.jmouse.storage.Content#UNKNOWN_SIZE} where it is not yet known
     */
    void requireRoomFor(OwnerReference owner, long declaredSize);
}
