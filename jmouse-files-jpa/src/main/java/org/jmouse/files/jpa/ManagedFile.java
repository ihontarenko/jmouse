package org.jmouse.files.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.jmouse.storage.jpa.StoredFile;

import java.time.LocalDateTime;

/**
 * 📄 A file as a person meets it: a name, an uploader, and the bytes behind it.
 *
 * <h3>What it is not</h3>
 *
 * <p>It is not the bytes — {@link StoredFile} is, and one of those may back several of these. It is
 * not a place either: where a file sits lives in {@link FileBinding}, so that a product filing into
 * a directory tree and a product attaching to an issue use one table rather than two shapes.</p>
 *
 * <h3>⚠️ The name is here and not on the registry row, and that is the point</h3>
 *
 * <p>Keys are content-addressed, so two people uploading the same document reach one
 * {@link StoredFile} whose {@code originalName} is whoever got there first. Serving that to everyone
 * shows one person another person's filename. The name a reader sees belongs to <em>this</em> row,
 * and every delivery goes through the overload that says so.</p>
 *
 * <h3>⚠️ Deleting one of these never deletes bytes</h3>
 *
 * <p>Same reason: another row may point at the same object. Reclaiming what nothing points at is the
 * sweeper's job, and this table is discovered as a reference source automatically because
 * {@link #storedFile} is a mapped association to the registry.</p>
 */
@Entity
@Table(name = "managed_files")
public class ManagedFile {

    /** Longest a display name may be, matching the column. */
    public static final int MAXIMUM_NAME_LENGTH = 512;

    /** How long a held-reason sentence may be. */
    public static final int MAXIMUM_HELD_REASON_LENGTH = 255;

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "display_name", length = MAXIMUM_NAME_LENGTH, nullable = false)
    private String displayName;

    /**
     * The bytes.
     *
     * <p>⚠️ {@code optional = false} on purpose: a file row with nothing behind it is not a file,
     * and allowing one would put a hole in every listing that renders a size or a type.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "stored_file_id", nullable = false)
    private StoredFile storedFile;

    /**
     * Who put it there, as the product's own identifier for a person.
     *
     * <p>⚠️ A string rather than an association, because this library has no people table and must
     * not acquire one. Every product already has its own — {@code members}, {@code security_users} —
     * and a foreign key into one of them from here would make adopting this library a schema
     * negotiation instead of a dependency.</p>
     */
    @Column(name = "uploaded_by", length = 64)
    private String uploadedBy;

    /**
     * Whether this file is listed and served only to whoever may already reach it.
     *
     * <p>⚠️ <strong>Here rather than in a product table beside this one</strong>, which is what the
     * extraction is for: a product that had to keep a row of its own alongside every file row would have
     * centralised nothing. Products with no notion of a private file leave it false and never read it.</p>
     *
     * <p>⚠️ It is a <em>flag</em>, not an authorization decision. What a private file actually means —
     * who may still see it, whether a share link overrides it — is the product's answer, asked of the
     * access engine like everything else. This column only records the intent.</p>
     */
    @Column(name = "is_private", nullable = false)
    private boolean privateFile;

    /**
     * Why something in the product is holding this file — said by whoever is holding it.
     *
     * <p>An avatar, a page block, a form entry: something depends on these bytes, so its owner may
     * still read and list the file but may not delete it, hide it or revoke its link. Each of those
     * silently breaks whatever is displaying it.</p>
     *
     * <p>⚠️ <strong>A sentence, not a flag, and this library never composes one.</strong> It has no
     * opinion about avatars or page blocks; it knows only that something is holding the file and can
     * repeat what that something said. A boolean would force it to translate {@code true} into words in
     * somebody else's vocabulary — which is exactly how a generic module ends up carrying a product's.</p>
     *
     * <p>⚠️ {@code null} means nothing is holding it, and there is deliberately no second column: a flag
     * and a reason can disagree, one field cannot.</p>
     *
     * <p>⚠️ Not a permission and not a privacy setting. A held file is listed, browsable and downloadable
     * exactly like any other — its owner can see it, which is the point of filing an avatar here rather
     * than in a bucket nobody can look into.</p>
     */
    @Column(name = "held_reason", length = MAXIMUM_HELD_REASON_LENGTH)
    private String heldReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ManagedFile() {
    }

    /**
     * 🏗️ Record a file.
     *
     * @param id          identifier the product minted
     * @param displayName what to call it here
     * @param storedFile  the bytes
     * @param uploadedBy  who put it there, or {@code null} where nobody is signed in
     */
    public ManagedFile(String id, String displayName, StoredFile storedFile, String uploadedBy) {
        this.id          = id;
        this.displayName = displayName;
        this.storedFile  = storedFile;
        this.uploadedBy  = uploadedBy;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 🔒 Whether this file is private.
     *
     * @return {@code true} when it is
     */
    public boolean isPrivateFile() {
        return privateFile;
    }

    /**
     * 🔒 Say whether this file is private.
     *
     * @param privateFile whether it is
     */
    public void setPrivateFile(boolean privateFile) {
        this.privateFile = privateFile;
    }

    /**
     * 🏷️ Call it something else.
     *
     * @param displayName the new name
     */
    public void renameTo(String displayName) {
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public StoredFile getStoredFile() {
        return storedFile;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ManagedFile file && id != null && id.equals(file.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "ManagedFile[%s '%s']".formatted(id, displayName);
    }

    /**
     * Why something is holding this file, or {@code null} where nothing is.
     *
     * @return the reason
     */
    public String getHeldReason() {
        return heldReason;
    }

    /**
     * Record that something is holding it, in that something's own words — or let go with {@code null}.
     *
     * <p>⚠️ Written by the feature that took the dependency and by nothing else. No endpoint should
     * reach this: a person marking their own file held would be protecting it from themselves.</p>
     *
     * @param heldReason why it is held, phrased for whoever will read the refusal
     */
    public void setHeldReason(String heldReason) {
        this.heldReason = heldReason;
    }

    /**
     * Whether anything is holding it — derived, so it can never contradict the reason.
     *
     * @return whether it is held
     */
    public boolean isHeld() {
        return heldReason != null;
    }
}
