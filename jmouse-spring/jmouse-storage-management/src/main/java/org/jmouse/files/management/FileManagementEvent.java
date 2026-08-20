package org.jmouse.files.management;

import org.jmouse.files.OwnerReference;
import org.jmouse.files.jpa.ManagedFile;

import java.util.List;

/**
 * 📣 Something happened to a managed file, said once so no product has to watch for it itself.
 *
 * <h3>Why this exists at all</h3>
 *
 * <p>A product adopting these routes gives up the place it used to hang its own behaviour: an audit
 * line, an activity entry, a search re-index. Without a seam the only way back is a product controller
 * wrapping the library's — which is the exact duplication this module was built to delete, arriving by
 * the back door and looking reasonable while it does.</p>
 *
 * <p>So the library says what happened and stays silent about what it means. A tracker turns
 * {@link Uploaded} into <em>"added an attachment"</em> on the issue's history; a knowledge base turns it
 * into a page revision; something else ignores it entirely.</p>
 *
 * <h3>⚠️ Published inside the transaction, deliberately</h3>
 *
 * <p>An ordinary {@code @EventListener} therefore runs before the commit and its writes join the same
 * transaction, which is what an activity log wants: the entry and the file it describes land together or
 * neither does. A listener that must not — sending mail, calling out — asks for
 * {@code @TransactionalEventListener} and gets the other behaviour, which is the way round that makes
 * the safe case the default.</p>
 */
public sealed interface FileManagementEvent {

    /** The file this is about. */
    String fileId();

    /**
     * 📥 A file was taken in and filed.
     *
     * @param fileId the file
     * @param file   the recorded row
     * @param owner  what it was filed against
     */
    record Uploaded(String fileId, ManagedFile file, OwnerReference owner) implements FileManagementEvent {

        public Uploaded(ManagedFile file, OwnerReference owner) {
            this(file.getId(), file, owner);
        }
    }

    /**
     * 🏷️ A file was renamed.
     *
     * @param fileId       the file
     * @param file         the row, carrying the new name
     * @param previousName what it was called before
     */
    record Renamed(String fileId, ManagedFile file, String previousName) implements FileManagementEvent {

        public Renamed(ManagedFile file, String previousName) {
            this(file.getId(), file, previousName);
        }
    }

    /**
     * 📦 A file was filed against something else.
     *
     * @param fileId the file
     * @param file   the row
     * @param from   where it was filed before, or {@code null} where it was filed nowhere
     * @param to     where it is filed now
     */
    record Refiled(String fileId, ManagedFile file, OwnerReference from, OwnerReference to)
            implements FileManagementEvent {

        public Refiled(ManagedFile file, OwnerReference from, OwnerReference to) {
            this(file.getId(), file, from, to);
        }
    }


    /**
     * 🙈 A file was hidden, or stopped being hidden.
     *
     * <p>⚠️ Announced because the flag is only half of what privacy means in most products. This library
     * records the intent; whatever else follows from it — withdrawing a public link, dropping a cached
     * copy, re-indexing — belongs to whoever gave the word its meaning.</p>
     *
     * @param fileId    the file
     * @param file      the row, carrying the new value
     * @param isPrivate what it is now
     */
    record PrivacyChanged(String fileId, ManagedFile file, boolean isPrivate)
            implements FileManagementEvent {

        public PrivacyChanged(ManagedFile file, boolean isPrivate) {
            this(file.getId(), file, isPrivate);
        }
    }

    /**
     * 🚫 Content was offered and the acceptance policy said no.
     *
     * <p>⚠️ Announced because <strong>a refused upload is a decision, not an accident</strong>, and it is
     * worth being able to see how often the policy is what somebody ran into. Nothing was stored, so
     * there is no file and no identifier — what there is, is a name, a size and where it came from.</p>
     *
     * <p>⚠️ Published from inside the failing call, before the exception leaves it. A listener that wants
     * the record to survive the rollback has to say so with its own transaction; the default is that it
     * goes with everything else, which is the safe direction rather than the useful one.</p>
     *
     * @param fileId    always {@code null} — nothing was recorded
     * @param name      what it was called
     * @param sizeBytes what it claimed to weigh, or {@link org.jmouse.storage.Content#UNKNOWN_SIZE}
     * @param owner     what it would have been filed against
     * @param reason    what the policy said
     */
    record Rejected(String fileId, String name, long sizeBytes, OwnerReference owner, String reason)
            implements FileManagementEvent {

        public Rejected(String name, long sizeBytes, OwnerReference owner, String reason) {
            this(null, name, sizeBytes, owner, reason);
        }
    }
    /**
     * 🗑️ A file was removed, and with it every place it was filed.
     *
     * <p>⚠️ Carries the <strong>name and the owners as they were</strong>, because by the time a
     * listener runs there is nothing left to look them up from. An event that made a listener re-read
     * the row it is about would be an event that only works for creation.</p>
     *
     * @param fileId      the file
     * @param displayName what it was called
     * @param owners      everywhere it had been filed
     */
    record Deleted(String fileId, String displayName, List<OwnerReference> owners)
            implements FileManagementEvent {
    }
}
