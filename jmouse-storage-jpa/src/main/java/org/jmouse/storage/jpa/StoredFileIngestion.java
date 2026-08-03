package org.jmouse.storage.jpa;

import org.jmouse.storage.Content;
import org.jmouse.storage.FileStore;
import org.jmouse.storage.FileStores;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.StoredObject;
import org.jmouse.storage.key.StorageKeyRequest;
import org.jmouse.storage.key.StorageKeyStrategy;
import org.jmouse.storage.policy.UploadPolicy;
import org.jmouse.storage.support.SpooledContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * 📥 The whole write path, in one place: judge it, place it, store it, record it.
 *
 * <p>Every product was doing these four things in its own order with its own omissions. Doing them
 * here means a product's ingestion service is left with the parts that are genuinely its own —
 * who may upload, what row to hang the result off, what to audit.</p>
 *
 * <h3>What it guarantees</h3>
 *
 * <p><strong>A rejected upload leaves nothing behind.</strong> The policy runs before anything is
 * opened, and the size limit is re-checked against the bytes that <em>actually</em> arrived — a
 * client's declared length is a claim, and a lying one would otherwise smuggle an oversized file
 * past a check that trusted it. An object that fails the second check is removed before the caller
 * hears about it.</p>
 *
 * <p><strong>Identical bytes cost one object.</strong> When the layout is content-addressed, the
 * digest is known before the key is composed, so the registry can be asked whether these exact
 * bytes are already stored. If they are, nothing is written and the existing row is returned — and
 * because deletion of a binding never deletes bytes, two bindings sharing one object stay
 * independent.</p>
 *
 * <p><strong>Bytes are read once.</strong> Spooling only happens when the layout needs the digest
 * up front; an owner-namespaced layout streams straight through as before.</p>
 *
 * <p>Transaction demarcation is the caller's, as everywhere else in this module. Note the ordering
 * consequence: bytes are written before the row is registered, so a transaction that rolls back
 * afterwards leaves an object nothing points at — which is exactly the case the orphan sweeper
 * exists to clean up, and why its grace period is not optional.</p>
 */
public class StoredFileIngestion {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoredFileIngestion.class);

    private final FileStores         fileStores;
    private final StoredFileRegistry registry;
    private final StorageKeyStrategy keyStrategy;
    private final UploadPolicy       uploadPolicy;

    /**
     * 🏗️ Build the write path out of its four collaborators.
     *
     * @param fileStores   every backend the application has
     * @param registry     where written objects are recorded
     * @param keyStrategy  where content is laid out
     * @param uploadPolicy what may enter storage
     */
    public StoredFileIngestion(FileStores fileStores, StoredFileRegistry registry,
                               StorageKeyStrategy keyStrategy, UploadPolicy uploadPolicy) {
        this.fileStores   = fileStores;
        this.registry     = registry;
        this.keyStrategy  = keyStrategy;
        this.uploadPolicy = uploadPolicy;
    }

    /**
     * 📥 Take content into storage and record it, on the default backend.
     *
     * @param request where the content should be laid out
     * @param content the content itself
     * @return the registry row backing it
     */
    public StoredFile ingest(StorageKeyRequest request, Content content) {
        return ingest(request, content, null);
    }

    /**
     * 📥 Take content into storage and record it, on a caller-chosen backend.
     *
     * @param request              where the content should be laid out
     * @param content              the content itself
     * @param requestedBackendName backend to write to, or {@code null} for the default; ignored
     *                             when the application does not expose the choice
     * @return the registry row backing it
     */
    public StoredFile ingest(StorageKeyRequest request, Content content, String requestedBackendName) {
        uploadPolicy.accept(content);

        FileStore fileStore = fileStores.forWriting(requestedBackendName);

        if (!keyStrategy.requiresContentDigest()) {
            return store(fileStore, keyStrategy.compose(request), content);
        }

        try (SpooledContent spooled = SpooledContent.of(content)) {
            uploadPolicy.ensureNotEmpty(spooled.sizeBytes());
            uploadPolicy.ensureWithinSizeLimit(spooled.sizeBytes());

            Optional<StoredFile> alreadyStored = registry.findBySha256(spooled.sha256());

            if (alreadyStored.isPresent()) {
                LOGGER.debug("📥 '{}' matches stored object '{}' — reusing it rather than writing again",
                             content.originalFilename(), alreadyStored.get().getStorageKey());
                return alreadyStored.get();
            }

            StorageKeyRequest digested = request.withContentDigest(spooled.sha256());

            return store(fileStore, keyStrategy.compose(digested), spooled.content());
        }
    }

    /**
     * 📝 Write the bytes, verify what actually arrived, and record the object.
     *
     * @param fileStore backend to write through
     * @param key       where to write
     * @param content   what to write
     * @return the registry row
     */
    private StoredFile store(FileStore fileStore, StorageKey key, Content content) {
        StoredObject stored = fileStore.write(key, content);

        try {
            uploadPolicy.ensureNotEmpty(stored.sizeBytes());
            uploadPolicy.ensureWithinSizeLimit(stored.sizeBytes());
        } catch (RuntimeException rejection) {
            // The declared size was a claim and the claim was false. Remove what was written before
            // the caller ever hears about it, so a refused upload is not a leaked object.
            fileStore.delete(key);
            throw rejection;
        }

        return registry.register(stored, content.originalFilename());
    }
}
