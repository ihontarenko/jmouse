package org.jmouse.storage.jpa;

import org.jmouse.storage.Content;
import org.jmouse.storage.FileStore;
import org.jmouse.storage.FileStores;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.StoredObject;
import org.jmouse.storage.key.StorageKeyRequest;
import org.jmouse.storage.key.StorageKeyStrategy;
import org.jmouse.storage.policy.FixedUploadPolicy;
import org.jmouse.storage.policy.UploadPolicy;
import org.jmouse.storage.policy.UploadPolicyResolver;
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
 *
 * <h3>⚠️ The policy is resolved per upload, not held</h3>
 *
 * <p>Because an installation is allowed more than one answer: a destination may carry its own
 * acceptance rule, and this path is where every upload passes. It is resolved once at the top of a
 * call and used throughout it, so one upload is judged by one rule even if the rule changes underneath
 * — and an installation with a single answer pays one virtual call for the privilege.</p>
 */
public class StoredFileIngestion {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoredFileIngestion.class);

    private final FileStores           fileStores;
    private final StoredFileRegistry   registry;
    private final StorageKeyStrategy   keyStrategy;
    private final UploadPolicyResolver uploadPolicies;

    /**
     * 🏗️ Build the write path out of its four collaborators.
     *
     * @param fileStores     every backend the application has
     * @param registry       where written objects are recorded
     * @param keyStrategy    where content is laid out
     * @param uploadPolicies what may enter storage, per destination
     */
    public StoredFileIngestion(FileStores fileStores, StoredFileRegistry registry,
                               StorageKeyStrategy keyStrategy, UploadPolicyResolver uploadPolicies) {
        this.fileStores     = fileStores;
        this.registry       = registry;
        this.keyStrategy    = keyStrategy;
        this.uploadPolicies = uploadPolicies;
    }

    /**
     * 🏗️ Build the write path over one policy that applies everywhere.
     *
     * @param fileStores   every backend the application has
     * @param registry     where written objects are recorded
     * @param keyStrategy  where content is laid out
     * @param uploadPolicy what may enter storage, anywhere in it
     * @deprecated pass a {@link UploadPolicyResolver} instead — a destination may carry its own rule,
     *             and this constructor is the one that cannot express it. Kept because it is what every
     *             caller wrote before there was anything else to pass.
     */
    @Deprecated(since = "1.1")
    public StoredFileIngestion(FileStores fileStores, StoredFileRegistry registry,
                               StorageKeyStrategy keyStrategy, UploadPolicy uploadPolicy) {
        this(fileStores, registry, keyStrategy, new FixedUploadPolicy(uploadPolicy));
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
        // Resolved once, against where the content is going, and used for the whole call — so an upload
        // is judged by one rule rather than by whatever the rule happened to be at each check.
        UploadPolicy uploadPolicy = policyFor(request);

        uploadPolicy.accept(content);

        FileStore fileStore = fileStores.forWriting(requestedBackendName);

        if (!keyStrategy.requiresContentDigest()) {
            return store(fileStore, keyStrategy.compose(request), content, uploadPolicy);
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

            return store(fileStore, keyStrategy.compose(digested), spooled.content(), uploadPolicy);
        }
    }

    /**
     * 🛃 The rule governing content headed for a destination.
     *
     * <p>Exposed because acceptance lives here, and other write paths have to ask the same question
     * without going through {@link #ingest}: re-filing a stored file into another folder is an entry
     * into that folder, and a caller that could not ask this would have no way to judge it. Reaching
     * past this class into the resolver instead is how a second acceptance rule gets written.</p>
     *
     * @param ownerType what kind of thing will hold the content, or {@code null} when unknown
     * @param ownerId   which one, or {@code null} when unknown
     * @return the policy to judge it by
     */
    public UploadPolicy policyFor(String ownerType, String ownerId) {
        return uploadPolicies.policyFor(ownerType, ownerId);
    }

    /**
     * 🛃 The rule governing content headed where this request says.
     *
     * @param request what the caller knows about the content, including where it is going
     * @return the policy to judge it by
     */
    private UploadPolicy policyFor(StorageKeyRequest request) {
        return policyFor(request.ownerType(), request.ownerIdentifier());
    }

    /**
     * ♻️ Overwrite an object in place, keeping its key and its registry row.
     *
     * <p>For content whose address is meant to stay fixed while its contents change: a document
     * saved repeatedly, a generated report refreshed on a schedule. Composing a new key each time
     * would strand the old object and break every link already handed out.</p>
     *
     * <p>Deduplication does not apply here and must not — the caller has said which object to
     * replace, and quietly pointing the binding at somebody else's identical bytes instead would
     * make the next save overwrite <em>their</em> file.</p>
     *
     * <p>⚠️ <strong>Judged by the installation's own rule, not by a destination's.</strong> Nothing is
     * entering anywhere — the object already sits where it sits, under a key that is not being
     * recomposed — and a destination rule governs entry rather than residence. There is also nothing
     * here to resolve one from: an overwrite names an object, never an owner.</p>
     *
     * @param existing the row whose bytes are being replaced
     * @param content  the new content
     * @return the same row, updated to match what was written
     */
    public StoredFile reingest(StoredFile existing, Content content) {
        UploadPolicy uploadPolicy = uploadPolicies.policyFor(null, null);

        uploadPolicy.accept(content);

        FileStore    fileStore = fileStores.require(existing.getBackend());
        StoredObject stored    = fileStore.write(existing.getStorageKey(), content);

        uploadPolicy.ensureNotEmpty(stored.sizeBytes());
        uploadPolicy.ensureWithinSizeLimit(stored.sizeBytes());

        existing.rewrittenAs(stored);

        return existing;
    }

    /**
     * 📝 Write the bytes, verify what actually arrived, and record the object.
     *
     * @param fileStore    backend to write through
     * @param key          where to write
     * @param content      what to write
     * @param uploadPolicy the rule this upload is being judged by
     * @return the registry row
     */
    private StoredFile store(FileStore fileStore, StorageKey key, Content content,
                             UploadPolicy uploadPolicy) {
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
