package org.jmouse.storage.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jmouse.core.MediaType;
import org.jmouse.storage.ContentTypes;
import org.jmouse.storage.ObjectDescription;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.StoredObject;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 🗃️ One row per object anything has ever written, and the only record of it that survives the row
 * pointing at it going away.
 *
 * <p>Before this table existed, the sole evidence an object was in the bucket was whichever product
 * row happened to reference it. Lose that row by any route the service did not take — a cascade, a
 * hand-written {@code DELETE}, a transaction that rolled back after the bytes had already landed —
 * and the object stayed there forever with nothing able to find it again. This is what makes an
 * orphan sweeper possible, and it is what finally answers "what is actually in this bucket".</p>
 *
 * <h3>What belongs here, and what does not</h3>
 *
 * <p>Everything on this row is a fact about the <em>bytes</em>. Everything about who may see them,
 * which record they hang off, or what to call them in a particular context is a fact about a
 * <em>binding</em> and stays on the product's own table. That split is the whole boundary: products
 * describe bindings, the library describes bytes.</p>
 *
 * <p>{@link #originalName} is the name of the <em>first</em> upload, not an authoritative one. Once
 * deduplication lets several bindings share an object, a binding that must present a different name
 * to a user carries its own — which is why a product's per-file original name stays where it is
 * rather than moving in here.</p>
 *
 * <h3>Mapping it in a product</h3>
 *
 * <p>This entity lives outside a product's own package, so it is not picked up by the default scan.
 * A product maps a {@code @ManyToOne} to it and widens the scan:</p>
 *
 * <pre>{@code
 * @EntityScan({"net.innoventa", "org.jmouse.storage.jpa"})
 * }</pre>
 *
 * <p>A product may <em>reference</em> this entity freely; querying it is
 * {@link StoredFileRegistry}'s job, so that the registry stays the one place that knows how the
 * table is used.</p>
 */
@Entity
@Table(name = StoredFile.TABLE_NAME)
public class StoredFile {

    /**
     * 🏷️ The registry table, named once so migrations, the sweeper and a product's foreign key all
     * spell it the same way.
     */
    public static final String TABLE_NAME = "stored_files";

    /**
     * 🔐 Length of a lower-case hex SHA-256, which is fixed and worth asserting in the schema.
     */
    public static final int SHA256_LENGTH = 64;

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "storage_key", length = 512, nullable = false, unique = true, updatable = false)
    private String storageKey;

    @Column(name = "original_name", length = 512, nullable = false)
    private String originalName;

    @Column(name = "content_type", length = 255, nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "sha256", length = SHA256_LENGTH)
    private String sha256;

    @Column(name = "backend", length = 32, nullable = false, updatable = false)
    private String backend;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 🏗️ For the persistence provider.
     */
    protected StoredFile() {
    }

    /**
     * 🏗️ Build a row for an object that has just been written.
     *
     * @param id           registry identifier
     * @param storageKey   where the object lives
     * @param originalName name the first upload arrived under
     * @param contentType  type the object is served as
     * @param sizeBytes    number of bytes that actually arrived
     * @param sha256       lower-case hex digest computed during the write
     * @param backend      name of the backend that wrote it
     * @param createdAt    when the row was registered
     */
    public StoredFile(String id, String storageKey, String originalName, String contentType,
                      long sizeBytes, String sha256, String backend, LocalDateTime createdAt) {
        this.id           = id;
        this.storageKey   = storageKey;
        this.originalName = originalName;
        this.contentType  = contentType;
        this.sizeBytes    = sizeBytes;
        this.sha256       = sha256;
        this.backend      = backend;
        this.createdAt    = createdAt;
    }

    /**
     * 🆔 Registry identifier, and the value a product's binding holds as its foreign key.
     *
     * @return the identifier
     */
    public String getIdentifier() {
        return id;
    }

    /**
     * 🔑 Where the object lives, as the key its backend was handed.
     *
     * <p>Rebuilt from the stored string rather than stored as a {@link StorageKey}, so a key
     * written under an older layout keeps resolving exactly as it was — the registry stores what a
     * row already has, and a new layout applies only to new writes.</p>
     *
     * @return the storage key
     */
    public StorageKey getStorageKey() {
        return StorageKey.of(storageKey);
    }

    /**
     * 📄 Name the first upload of these bytes arrived under.
     *
     * @return the original filename
     */
    public String getOriginalName() {
        return originalName;
    }

    /**
     * 🎨 Type the object is served as.
     *
     * <p>Authoritative in a way the key is not: a content-addressed key carries no extension, so
     * this row is the only thing that knows what the bytes are.</p>
     *
     * @return the content type, never {@code null}
     */
    public MediaType getContentType() {
        MediaType parsed = ContentTypes.parse(contentType);
        return (parsed != null) ? parsed : ContentTypes.DEFAULT;
    }

    /**
     * 📏 Object length in bytes, as measured during the write.
     *
     * @return the size
     */
    public long getSizeBytes() {
        return sizeBytes;
    }

    /**
     * 🔐 Lower-case hex SHA-256 of the bytes, or {@code null} for an object stored before the
     * registry existed and not yet backfilled.
     *
     * <p>Doubles as a strong entity tag and as the content identity deduplication matches on, so
     * neither costs an extra column or an extra read.</p>
     *
     * @return the digest, or {@code null}
     */
    public String getSha256() {
        return sha256;
    }

    /**
     * 🔐 Record a digest established after the fact, for an object stored before the registry did.
     *
     * @param sha256 lower-case hex digest
     */
    public void backfillSha256(String sha256) {
        this.sha256 = sha256;
    }

    /**
     * ♻️ Bring the row back in line with bytes that were overwritten at the same key.
     *
     * <p>For content whose address is meant to stay fixed while its contents change — a document
     * saved repeatedly under one key. The key and the backend are untouched, because neither
     * moved; everything measured about the bytes is replaced, because all of it did.</p>
     *
     * @param stored receipt from the rewrite
     */
    public void rewrittenAs(StoredObject stored) {
        this.contentType = ContentTypes.baseType(stored.contentType());
        this.sizeBytes   = stored.sizeBytes();
        this.sha256      = stored.sha256();
    }

    /**
     * 🔌 Name of the backend that wrote the object.
     *
     * <p>Recorded although moving objects between backends is out of scope, so that building it
     * later is a feature rather than a schema change to negotiate.</p>
     *
     * @return the backend name
     */
    public String getBackend() {
        return backend;
    }

    /**
     * 🕒 When the object was registered.
     *
     * <p>What the sweeper's grace period is measured against — an object written seconds ago by a
     * transaction that has not committed yet has no references and must not be a candidate.</p>
     *
     * @return the registration timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 📋 This row as the description a delivery decision needs.
     *
     * @return key, size and content type
     */
    public ObjectDescription describe() {
        return new ObjectDescription(getStorageKey(), sizeBytes, getContentType());
    }

    @Override
    public boolean equals(Object other) {
        return (this == other)
                || (other instanceof StoredFile storedFile && Objects.equals(id, storedFile.id));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "StoredFile[%s -> %s, %d bytes, %s]".formatted(id, storageKey, sizeBytes, backend);
    }
}
