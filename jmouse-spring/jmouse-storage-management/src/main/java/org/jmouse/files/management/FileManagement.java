package org.jmouse.files.management;

import org.jmouse.files.OwnerReference;
import org.jmouse.files.exception.FileHeldException;
import org.jmouse.files.exception.ManagedFileTooLargeException;
import org.jmouse.files.exception.RemoteFetchException;
import org.jmouse.files.jpa.FileBindings;
import org.jmouse.files.jpa.ManagedFile;
import org.jmouse.files.jpa.ManagedFiles;
import org.jmouse.storage.Content;
import org.jmouse.storage.FileStores;
import org.jmouse.storage.exception.StorageException;
import org.jmouse.storage.exception.UploadRejectedException;
import org.jmouse.storage.delivery.DeliveryIntent;
import org.jmouse.storage.delivery.DeliveryPlan;
import org.jmouse.storage.jpa.StoredFile;
import org.jmouse.storage.jpa.StoredFileDelivery;
import org.jmouse.storage.jpa.StoredFileIngestion;
import org.jmouse.storage.key.StorageKeyRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 📁 The whole of what a file endpoint does, minus the routing and minus the authorization.
 *
 * <p>Four products wrote this: take the multipart apart, ask storage to accept the bytes, record a row,
 * file it somewhere, and answer with something a screen can draw. The parts that differed between them
 * were the permission on the route and the shape of the answer — never this.</p>
 *
 * <h3>⚠️ Nothing here asks who is calling</h3>
 *
 * <p>The same rule every product in this workspace already keeps. Routes are gated before a handler
 * runs, by one engine, against one policy; a second opinion at this level would be a rule somebody
 * edits in one place, and the failure mode is the two disagreeing while only one of them is the one
 * people read.</p>
 *
 * <h3>⚠️ And nothing here decides what may be uploaded</h3>
 *
 * <p>That is {@code jmouse.storage}'s answer, enforced by {@code StoredFileIngestion} before any bytes
 * are written. A second allowlist here would be a rule stated twice, which is a rule somebody edits in
 * one place.</p>
 *
 * <p>Transaction demarcation is the caller's, as everywhere else.</p>
 */
public class FileManagement {

    // OWNS THE TRANSACTION, and this is where it belongs rather than a taste.
    //
    // Every collaborator below is deliberately annotation-free — JpaStoredFileRegistry says so in as many
    // words — on the reading that whoever calls owns the boundary. That reading holds for a product
    // calling from its own service and breaks for one mounting FileController, where the only caller is a
    // controller and a transactional controller is worse than this. An upload is also genuinely one unit:
    // the stored row, the managed row and the binding are three writes describing one act.
    //
    // ⚠️ It is what makes an ordinary @EventListener on FileManagementEvent join the same transaction,
    // so a product’s activity entry and the file it describes commit together or not at all.

    private final StoredFileIngestion  ingestion;
    private final StoredFileDelivery   delivery;
    private final ManagedFiles         files;
    private final FileBindings         bindings;
    private final Supplier<String>     identifiers;
    private final FileStores           fileStores;

    /**
     * Whether an upload may happen at all, or {@code null} where the product meters nothing.
     *
     * <p>Asked before a byte is written — see {@link UploadAllowance} for why this cannot be an event.</p>
     */
    private final UploadAllowance      allowance;

    /** How a file is fetched from a web address, or {@code null} where this installation does not. */
    private final RemoteFileFetcher    fetcher;

    /**
     * Where "this happened" is announced, or {@code null} where nobody is listening.
     *
     * <p>Nullable rather than a no-op publisher: this class is constructed directly in tests and in
     * products that predate the seam, and a required argument there buys nothing.</p>
     */
    private final ApplicationEventPublisher events;

    /**
     * 🏗️ Build the file surface out of its five collaborators.
     *
     * @param ingestion   the write path into storage
     * @param delivery    the read path out of it
     * @param files       the file rows
     * @param bindings    where files are filed
     * @param identifiers where a new file's identifier comes from
     */
    public FileManagement(StoredFileIngestion ingestion, StoredFileDelivery delivery,
                          ManagedFiles files, FileBindings bindings, Supplier<String> identifiers) {
        this(ingestion, delivery, files, bindings, identifiers, null, null);
    }

    /**
     * Build it, announcing what happens to the files it manages.
     *
     * @param ingestion   the write path into storage
     * @param delivery    the read path out of it
     * @param files       the file rows
     * @param bindings    where files are filed
     * @param identifiers where a new file's identifier comes from
     * @param events      where to announce what happened, or {@code null} to announce nothing
     * @param fileStores  the backends, for the callers that need the bytes rather than a response
     */
    public FileManagement(StoredFileIngestion ingestion, StoredFileDelivery delivery,
                          ManagedFiles files, FileBindings bindings, Supplier<String> identifiers,
                          ApplicationEventPublisher events, FileStores fileStores) {
        this(ingestion, delivery, files, bindings, identifiers, events, fileStores, null);
    }

    /**
     * Build it, metering what it is allowed to take.
     *
     * @param ingestion   the write path into storage
     * @param delivery    the read path out of it
     * @param files       the file rows
     * @param bindings    where files are filed
     * @param identifiers where a new file’s identifier comes from
     * @param events      where to announce what happened, or {@code null} to announce nothing
     * @param fileStores  the backends, for the callers that need the bytes rather than a response
     * @param allowance   what may be stored, or {@code null} to meter nothing
     */
    public FileManagement(StoredFileIngestion ingestion, StoredFileDelivery delivery,
                          ManagedFiles files, FileBindings bindings, Supplier<String> identifiers,
                          ApplicationEventPublisher events, FileStores fileStores,
                          UploadAllowance allowance) {
        this(ingestion, delivery, files, bindings, identifiers, events, fileStores, allowance, null);
    }

    /**
     * Build it, able to import from a web address as well.
     *
     * @param ingestion   the write path into storage
     * @param delivery    the read path out of it
     * @param files       the file rows
     * @param bindings    where files are filed
     * @param identifiers where a new file’s identifier comes from
     * @param events      where to announce what happened, or {@code null} to announce nothing
     * @param fileStores  the backends, for the callers that need the bytes rather than a response
     * @param allowance   what may be stored, or {@code null} to meter nothing
     * @param fetcher     how to fetch from a web address, or {@code null} to refuse importing
     */
    public FileManagement(StoredFileIngestion ingestion, StoredFileDelivery delivery,
                          ManagedFiles files, FileBindings bindings, Supplier<String> identifiers,
                          ApplicationEventPublisher events, FileStores fileStores,
                          UploadAllowance allowance, RemoteFileFetcher fetcher) {
        this.ingestion   = ingestion;
        this.delivery    = delivery;
        this.files       = files;
        this.bindings    = bindings;
        this.identifiers = identifiers;
        this.events      = events;
        this.fileStores  = fileStores;
        this.allowance   = allowance;
        this.fetcher     = fetcher;
    }

    /**
     * 📥 Take content in and file it against something.
     *
     * <p>⚠️ <strong>The owner is not part of the storage key beyond its namespace</strong>, because the
     * key is a digest: two people uploading the same document reach the same object and the second
     * write stores nothing at all. What is per-owner is the row and the name on it.</p>
     *
     * @param owner     what should hold it
     * @param namespace the storage namespace, which is a directory root's path
     * @param content   the content itself
     * @param name      what to call it here
     * @param uploadedBy who is putting it there, or {@code null}
     * @return the recorded file
     */
    @Transactional
    public ManagedFile upload(OwnerReference owner, String namespace, Content content, String name,
                              String uploadedBy) {
        // ⚠️ Before anything is opened. A product that meters storage has to refuse a full account
        // rather than fill it and report afterwards — which is all an event could do.
        if (allowance != null) {
            allowance.requireRoomFor(owner, content.declaredSize());
        }

        // ⚠️ The KIND travels with the identifier and does not lay anything out. It is what lets storage
        // resolve the rule of wherever this is going — a folder that admits archives, an issue that does
        // not — instead of applying one installation-wide answer to both.
        StorageKeyRequest request = StorageKeyRequest.forContent(owner.ownerId(), content)
            .ownerType(owner.ownerType())
            .namespace(namespace)
            .build();

        StoredFile stored;

        try {
            stored = ingestion.ingest(request, content);
        } catch (UploadRejectedException refusal) {
            announce(new FileManagementEvent.Rejected(name, content.declaredSize(), owner,
                                                      refusal.getMessage()));
            throw refusal;
        }
        ManagedFile file   = files.record(identifiers.get(), displayName(name, stored), stored, uploadedBy);

        bindings.bind(file.getId(), owner);
        announce(new FileManagementEvent.Uploaded(file, owner));

        return file;
    }

    /**
     * 🌐 Fetch a file from a web address and file it, as if somebody had uploaded it.
     *
     * <p>⚠️ <strong>The size is declared unknown on purpose.</strong> A remote server may lie about
     * {@code Content-Length} or omit it, so the ingestion path measures what actually arrived and removes
     * the object when the real size breaks a limit a false declaration slipped past. The fetcher already
     * refused an oversized <em>declaration</em>; this is the other half.</p>
     *
     * @param owner      what should hold it
     * @param namespace  the storage namespace
     * @param sourceUrl  the address to fetch
     * @param uploadedBy who asked for it, or {@code null}
     * @return the recorded file
     */
    @Transactional
    public ManagedFile importFrom(OwnerReference owner, String namespace, String sourceUrl,
                                  String uploadedBy) {
        if (fetcher == null) {
            throw new RemoteFetchException(
                    "This installation does not import files from web addresses.");
        }

        // ⚠️ The owner goes with it. The fetcher checks a declared size of its own, and one that did not
        // know the destination would let a fetch past a limit an upload of the same bytes would meet.
        RemoteFileFetcher.RemoteFile remote = fetcher.fetch(sourceUrl, owner);

        try (InputStream bytes = remote.inputStream()) {
            Content content = Content.of(remote.originalName(), null, Content.UNKNOWN_SIZE, () -> bytes);

            return upload(owner, namespace, content, remote.originalName(), uploadedBy);
        } catch (IOException unreadable) {
            throw new RemoteFetchException(
                    "Failed to download file from URL: " + unreadable.getMessage());
        }
    }

    /**
     * 📂 Everything filed against one owner.
     *
     * @param owner what holds them
     * @return the files, in the order they are filed
     */
    @Transactional(readOnly = true)
    public List<ManagedFile> listFiledUnder(OwnerReference owner) {
        return files.listFiledUnder(owner);
    }

    /**
     * 🔎 Where one file is filed, among owners of a given kind.
     *
     * <p>Every product listing a file has to render its place beside it, and reaching past this service
     * into the bindings to find out is how a product ends up with half a file manager of its own again.</p>
     *
     * @param fileId    the file
     * @param ownerType the kind of owner to look for
     * @return that owner, or empty
     */
    @Transactional(readOnly = true)
    public Optional<OwnerReference> ownerOf(String fileId, String ownerType) {
        return bindings.ownerOf(fileId, ownerType);
    }

    /**
     * 🔎 One file, where there is one.
     *
     * <p>The other half of {@link #read(String)}. A caller rendering a share card or a search hit is
     * asking whether the file is still there, not asserting that it is — and an exception is a poor way
     * to answer a question you expect to come back empty.</p>
     *
     * @param fileId the file
     * @return the file, or empty
     */
    @Transactional(readOnly = true)
    public Optional<ManagedFile> find(String fileId) {
        return files.find(fileId);
    }

    /**
     * 🔎 One file.
     *
     * @param fileId the file
     * @return the file
     */
    @Transactional(readOnly = true)
    public ManagedFile read(String fileId) {
        return files.require(fileId);
    }

    /**
     * 🏷️ Rename one.
     *
     * @param fileId the file
     * @param name   the new name
     * @return the renamed file
     */
    @Transactional
    public ManagedFile rename(String fileId, String name) {
        String previousName = files.require(fileId).getDisplayName();

        ManagedFile renamed = files.rename(fileId, name);
        announce(new FileManagementEvent.Renamed(renamed, previousName));

        return renamed;
    }

    /**
     * 📦 File it against something else of the same kind.
     *
     * <p>⚠️ <strong>Judged by where it is going.</strong> Uploading into a permissive folder and then
     * moving the file into one that would have refused it was, until this check existed, a way past
     * every acceptance rule in the installation — and the file ended up somewhere its own folder says
     * it may not be.</p>
     *
     * <p>⚠️ This is the one place a rule judges bytes that are <em>already stored</em>, so the refusal
     * has to say so. "Upload rejected" reads as nonsense to somebody who is not uploading anything.</p>
     *
     * @param fileId the file
     * @param owner  where it should now be filed
     * @return the file
     */
    @Transactional
    public ManagedFile refile(String fileId, OwnerReference owner) {
        ManagedFile file = files.require(fileId);

        refuseIfDestinationWouldNotAccept(file, owner);
        // Read before the move, because afterwards there is nothing left saying where it came from —
        // and "moved from the issue you were reading" is the half of the sentence that carries meaning.
        OwnerReference previous = bindings.ownerOf(file.getId(), owner.ownerType()).orElse(null);

        bindings.refile(file.getId(), owner);
        announce(new FileManagementEvent.Refiled(file, previous, owner));

        return file;
    }

    /**
     * 🙈 List and serve it only to whoever may already reach it — or stop doing so.
     *
     * <p>⚠️ A <em>flag</em>, not an authorization decision. What private actually means here — who may
     * still see it, whether a share link overrides it — is the product's answer, asked of its access
     * engine. This only records the intent, and refuses to hide a file something is holding.</p>
     *
     * @param fileId  the file
     * @param private_ whether it should be private
     * @return the file
     */
    @Transactional
    public ManagedFile setPrivate(String fileId, boolean private_) {
        ManagedFile file = files.require(fileId);

        if (private_) {
            refuseIfHeld(file, "made private");
        }

        file.setPrivateFile(private_);
        announce(new FileManagementEvent.PrivacyChanged(file, private_));

        return file;
    }

    /**
     * 🔒 Record that something in the product is holding this file, in that something's own words.
     *
     * <p>⚠️ <strong>Called by the feature that took the dependency and by nothing else.</strong> No
     * endpoint should reach it: a person marking their own file held would be protecting it from
     * themselves. {@link #release} is the other half, and calling it is not optional — a feature that
     * stops depending on a file and never releases it leaves its owner unable to tidy up.</p>
     *
     * @param fileId the file
     * @param reason why it is being held, phrased for whoever will read the refusal
     * @return the file
     */
    @Transactional
    public ManagedFile hold(String fileId, String reason) {
        ManagedFile file = files.require(fileId);

        file.setHeldReason(reason);

        return file;
    }

    /**
     * 🔓 Let go of a file nothing depends on any more. Idempotent — releasing a free file does nothing.
     *
     * @param fileId the file
     * @return the file
     */
    @Transactional
    public ManagedFile release(String fileId) {
        ManagedFile file = files.require(fileId);

        file.setHeldReason(null);

        return file;
    }

    /**
     * 🗑️ Remove it, and everywhere it was filed.
     *
     * <p>⚠️ The bytes stay — another row may share the stored object. The sweeper reclaims what nothing
     * points at.</p>
     *
     * @param fileId the file
     */
    @Transactional
    public void delete(String fileId) {
        // Both read first: after the delete the row and its bindings are gone, and a listener asked to
        // look them up would only ever work for events that are not about removal.
        ManagedFile          file   = files.require(fileId);
        List<OwnerReference> owners = bindings.ownersOf(fileId);
        String               name   = file.getDisplayName();

        refuseIfHeld(file, "deleted");

        files.delete(fileId);
        announce(new FileManagementEvent.Deleted(fileId, name, owners));
    }

    /**
     * 📥 The whole file, in memory, up to a limit the caller sets.
     *
     * <p>For the callers that genuinely need the bytes rather than a response: an agent that has to look
     * at a picture, a preview that has to decode one, a checksum. Everything that serves a file to a
     * browser should go through {@link #planDelivery} instead — it can redirect, stream, resume and
     * answer a conditional request, none of which this does.</p>
     *
     * <p>⚠️ <strong>The limit refuses; it never truncates.</strong> Half an image is not a smaller answer,
     * it is a wrong one — it decodes to a corrupt picture or to nothing, somewhere with no idea a limit
     * was involved. The size is checked against the registry <em>before</em> anything is opened, so an
     * over-large file costs one row read.</p>
     *
     * @param fileId       the file
     * @param maximumBytes how much the caller is prepared to hold
     * @return the bytes
     */
    @Transactional(readOnly = true)
    public byte[] readContent(String fileId, long maximumBytes) {
        ManagedFile file   = files.require(fileId);
        StoredFile  stored = file.getStoredFile();

        if (stored.getSizeBytes() > maximumBytes) {
            throw new ManagedFileTooLargeException(
                    file.getDisplayName(), stored.getSizeBytes(), maximumBytes);
        }

        try (InputStream bytes = fileStores.require(stored.getBackend())
                .read(stored.getStorageKey()).getInputStream()) {
            return bytes.readAllBytes();
        } catch (IOException unreadable) {
            throw new StorageException(
                    "Could not read '%s': %s".formatted(file.getDisplayName(), unreadable.getMessage()),
                    unreadable);
        }
    }

    /**
     * 🚚 How the bytes should reach a client.
     *
     * <p>⚠️ Under <strong>this row's</strong> name, never the registry's: under content-addressed keys
     * the registry's name is whoever uploaded the same bytes first.</p>
     *
     * @param fileId the file
     * @param intent what the client asked for
     * @return the delivery plan
     */
    @Transactional(readOnly = true)
    public DeliveryPlan planDelivery(String fileId, DeliveryIntent intent) {
        ManagedFile file = files.require(fileId);

        return delivery.plan(file.getStoredFile(), file.getDisplayName(), intent);
    }


    /**
     * 🛃 Refuse a move into somewhere that would not have accepted this file in the first place.
     *
     * <p>The stored content type and the name's extension are judged, which is exactly what an upload
     * is judged on — the bytes are not re-read, because acceptance has never been about the bytes.</p>
     */
    private void refuseIfDestinationWouldNotAccept(ManagedFile file, OwnerReference owner) {
        StoredFile stored = file.getStoredFile();
        Content    filed  = Content.of(file.getDisplayName(), stored.getContentType().toString(),
                                       stored.getSizeBytes(), () -> null);

        try {
            ingestion.policyFor(owner.ownerType(), owner.ownerId()).accept(filed);
        } catch (UploadRejectedException refusal) {
            throw new UploadRejectedException(
                    "'%s' cannot be moved there — %s".formatted(file.getDisplayName(),
                                                                lowerCasedFirst(refusal.getMessage())));
        }
    }

    /**
     * 🔤 So a sentence reads as one sentence rather than as two glued together.
     */
    private static String lowerCasedFirst(String sentence) {
        if (sentence == null || sentence.isEmpty()) {
            return "that destination does not accept it.";
        }

        return Character.toLowerCase(sentence.charAt(0)) + sentence.substring(1);
    }

    /**
     * 🔒 Refuse anything that would make a held file unreachable.
     */
    private static void refuseIfHeld(ManagedFile file, String attemptedAction) {
        if (file.isHeld()) {
            throw new FileHeldException(file.getDisplayName(), attemptedAction, file.getHeldReason());
        }
    }
    /**
     * 📣 Say what happened, where anybody asked to hear it.
     */
    private void announce(FileManagementEvent event) {
        if (events != null) {
            events.publishEvent(event);
        }
    }

    /**
     * 🏷️ What to call it here.
     *
     * <p>Taken from this upload rather than from the stored object, and falling back to the registry's
     * name only when the client sent none — a browser posting a blob does exactly that.</p>
     */
    private String displayName(String submitted, StoredFile stored) {
        String candidate = submitted == null ? "" : submitted.trim();

        if (candidate.isEmpty()) {
            candidate = stored.getOriginalName();
        }

        return candidate.length() > ManagedFile.MAXIMUM_NAME_LENGTH
                ? candidate.substring(0, ManagedFile.MAXIMUM_NAME_LENGTH)
                : candidate;
    }
}
