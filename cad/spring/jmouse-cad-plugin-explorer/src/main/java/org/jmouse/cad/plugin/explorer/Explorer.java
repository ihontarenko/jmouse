package org.jmouse.cad.plugin.explorer;

import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.source.DocumentLocation.Access;
import org.jmouse.cad.document.source.DocumentLocation;
import org.jmouse.cad.plugin.explorer.ExplorerChange.Kind;
import org.jmouse.cad.plugin.explorer.ExplorerMounts.Held;
import org.jmouse.cad.plugin.explorer.ExplorerMounts.MountRequest;
import org.jmouse.cad.plugin.explorer.ExplorerMounts.Origin;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadPlace;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentDetail;
import org.jmouse.cad.store.catalogue.DocumentReading;
import org.jmouse.cad.store.catalogue.ReadDocument;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.core.io.Resource;
import org.jmouse.plugin.Extensions;
import org.jmouse.storage.Content;
import org.jmouse.storage.FileTree;
import org.jmouse.storage.ObjectDescription;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.exception.ObjectNotFoundException;
import org.jmouse.storage.exception.StorageException;
import org.jmouse.storage.exception.StorageKeyException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 📁 The explorer: every mount the plugins brought, worked as a file manager works a disk — walked, read, written,
 * renamed, moved, tidied —, each change written to the real folder and told on {@link ExplorerChange#TOPIC}.
 *
 * <p>What a reader sees is the folder as it stands: every listing reads it again, so what changed on the disk outside
 * the explorer is there at the next look. Who may do what is the engine's seam — {@link ExplorerCapability#READ} and
 * {@link ExplorerCapability#WRITE}, across the installation for its mounts, at the owner for an owner's —; a mount the
 * reader may not read is not listed, and is answered as not found. A key never leaves its mount: {@link StorageKey} cannot climb.</p>
 */
public class Explorer {

    /** One write of a document at a time: comparing a file's version and writing it are one step. */
    private final Object          writingDocuments = new Object();
    private final MountDocuments  documents;
    private final DocumentReading reading;
    private final CadCatalogue    catalogue;

    /** A mount as the explorer lists it; {@code changeable} says whether this reader may change it. */
    public record MountView(String id, String title, String ownerType, String ownerId, boolean changeable) {
    }

    /** One thing in a folder; {@code key} is where it is from the mount's top. */
    public record EntryView(String key, String name, boolean folder, long sizeBytes, Instant modifiedAt) {
    }

    /**
     * A mount as its mounting is managed: where it comes from and where it is on the server's disk.
     *
     * @param path where it is on the server; null for a store a plugin brings
     */
    public record MountingView(String id, String title, String ownerType, String ownerId, Access access, Origin origin,
                               String path) {
    }

    /** Every mount as its mounting is managed, and whether the reader may mount a folder. */
    public record MountingsView(boolean mayMount, List<MountingView> mounts) {
    }

    /** A file opened for reading: its bytes and what it is. */
    public record Opened(Resource resource, ObjectDescription description) {
    }

    private final Extensions     extensions;
    private final ExplorerMounts mounts;
    private final CadAuthority   authority;
    private final CadPlaces      places;

    public Explorer(Extensions extensions, ExplorerMounts mounts, CadAuthority authority, CadPlaces places,
                    MountDocuments documents, DocumentReading reading, CadCatalogue catalogue) {
        this.documents = documents;
        this.reading = reading;
        this.catalogue = catalogue;
        this.extensions = extensions;
        this.mounts = mounts;
        this.authority = authority;
        this.places = places;
    }

    /** The mounts this reader may open. */
    public List<MountView> mounts() {
        return mounts.all().stream().map(Held::mount).filter(this::mayRead)
                .map(mount -> new MountView(mount.id(), mount.title(), mount.owner() == null ? null : mount.owner().type(),
                                            mount.owner() == null ? null : mount.owner().id(),
                                            mount.writable() && mayWrite(mount)))
                .toList();
    }

    /** @param folder where in the mount, from its top; null or blank for the top itself */
    public List<EntryView> entries(String mountId, String folder) {
        ExplorerMount mount = readable(mountId);
        List<FileTree.Entry> entries = stored(() -> folder == null || folder.isBlank() ? mount.tree().top()
                : mount.tree().list(keyOf(folder)));
        return entries.stream().filter(entry -> !reserved(entry.name())).map(Explorer::viewOf).toList();
    }

    /** A document of a folder, as a list shows it. */
    public record DocumentEntry(String key, String slug, String title, String kind, String source) {
    }

    /**
     * What a folder holds as documents.
     *
     * @param owner whose place sees them; null for a folder that belongs to none, whose documents name nothing of each other
     */
    public record FolderDocuments(DocumentOwner owner, List<DocumentEntry> documents, List<MountDocuments.Unread> unread) {
    }

    public FolderDocuments documents(String mountId) {
        ExplorerMount mount = readable(mountId);
        MountDocuments.FolderReading read = stored(() -> documents.read(mount));
        return new FolderDocuments(mount.owner(), read.documents().stream().map(document -> new DocumentEntry(document.key(),
                document.read().index().slug(), document.read().index().title(), document.read().index().kind(), document.read().source())).toList(),
                read.unread());
    }

    /** A file of a folder as the document it is: what a bench opens; refused, in the language's words, when it does not read. */
    public DocumentDetail document(String mountId, String key) {
        ExplorerMount mount = readable(mountId);
        StorageKey file = keyOf(key);
        if (!MountDocuments.namesDocument(file.value())) {
            throw CadProblem.invalid("'" + key + "' is no document's file.");
        }
        return detailOf(mount, file, reading.read(stored(() -> textOf(mount, file)), file.value()));
    }

    /**
     * A document written into its file, from a bench: the text is read first, so a file is never left holding what does
     * not read. The version says which text the writer was changing: another on the disk by now is refused, not written
     * over; no version says the file is new, and one already there, or an address the folder already holds, is refused.
     */
    public DocumentDetail writeDocument(String mountId, String key, Long version, String source) {
        ExplorerMount mount = changeable(mountId);
        StorageKey file = keyOf(key);
        if (!MountDocuments.namesDocument(file.value())) {
            throw CadProblem.invalid("'" + key + "' is no document's file.");
        }
        ReadDocument read = reading.read(source, file.value());
        synchronized (writingDocuments) {
            if (version == null) {
                requireNew(mount, file, read);
            } else if (version != MountDocuments.versionOf(stored(() -> textOf(mount, file)))) {
                throw CadProblem.staleVersion("'" + file.value() + "'");
            }
            write(mountId, key, Content.of(file.value().substring(file.value().lastIndexOf('/') + 1), "text/plain",
                                           Content.UNKNOWN_SIZE, () -> new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8))));
        }
        return detailOf(mount, file, read);
    }

    private void requireNew(ExplorerMount mount, StorageKey file, ReadDocument read) {
        MountDocuments.FolderReading held = stored(() -> documents.read(mount));
        String address = read.index().slug();
        held.documents().stream().filter(document -> document.key().equals(file.value()) || document.read().index().slug().equals(address))
                .findFirst().ifPresent(document -> {
                    throw CadProblem.conflict("'" + document.key() + "' already holds this document: it is changed, not made again.");
                });
        if (held.unread().stream().anyMatch(unread -> unread.key().equals(file.value()))) {
            throw CadProblem.conflict("'" + file.value() + "' is already there, and does not read: it is not written over.");
        }
    }

    private DocumentDetail detailOf(ExplorerMount mount, StorageKey file, ReadDocument read) {
        return catalogue.heldDetail(read, new DocumentLocation.Mounted(mount.id(), file.value(), mount.access()),
                mount.id() + ":" + file.value(), mount.owner(), entryOf(mount, file).modifiedAt(),
                MountDocuments.versionOf(read.source()), mount.writable() && mayWrite(mount));
    }

    private static String textOf(ExplorerMount mount, StorageKey file) {
        try (InputStream body = mount.store().read(file).getInputStream()) {
            return new String(body.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failed) {
            throw new UncheckedIOException(failed);
        }
    }

    public Opened open(String mountId, String key) {
        ExplorerMount mount = readable(mountId);
        StorageKey file = keyOf(key);
        return stored(() -> new Opened(mount.store().read(file), mount.store().describe(file)));
    }

    /** A file written — made, or replaced — where the reader names it. */
    public EntryView write(String mountId, String key, Content content) {
        ExplorerMount mount = changeable(mountId);
        StorageKey file = keyOf(key);
        stored(() -> mount.store().write(file, content));
        told(mount, Kind.WRITTEN, file, null);
        return entryOf(mount, file);
    }

    /** A folder made — a project's, or one inside it —, with the folders above it. */
    public void createFolder(String mountId, String folder) {
        ExplorerMount mount = changeable(mountId);
        StorageKey key = keyOf(folder);
        stored(() -> {
            mount.tree().createFolder(key);
            return key;
        });
        told(mount, Kind.FOLDER_MADE, key, null);
    }

    /**
     * A file, or a folder with all it holds, copied to another place in the same mount — the folder made, each file
     * written anew and told as written. Refused when something already stands where the copy would go.
     */
    public void copy(String mountId, String from, String to) {
        ExplorerMount mount = changeable(mountId);
        StorageKey source = keyOf(from);
        StorageKey target = keyOf(to);
        if (target.value().equals(source.value()) || target.value().startsWith(source.value() + "/")) {
            throw CadProblem.invalid("A folder cannot be copied into itself.");
        }
        if (entryAt(mount, target).isPresent()) {
            throw CadProblem.conflict("'" + target.value() + "' is already there.");
        }
        FileTree.Entry copied = entryAt(mount, source)
                .orElseThrow(() -> CadProblem.notFound("'" + source.value() + "'"));
        copyEntry(mount, copied, target);
    }

    private void copyEntry(ExplorerMount mount, FileTree.Entry entry, StorageKey target) {
        if (entry.folder()) {
            stored(() -> {
                mount.tree().createFolder(target);
                return target;
            });
            told(mount, Kind.FOLDER_MADE, target, null);
            // What a plugin keeps — a nested repository's workings — is not the explorer's to copy.
            for (FileTree.Entry inner : stored(() -> mount.tree().list(entry.key()))) {
                if (reserved(inner.name())) {
                    continue;
                }
                copyEntry(mount, inner, StorageKey.of(target.value() + "/" + inner.name()));
            }
            return;
        }
        ObjectDescription description = stored(() -> mount.store().describe(entry.key()));
        Content content = Content.of(entry.name(), description.contentType().toString(), description.sizeBytes(),
                                     () -> mount.store().read(entry.key()).getInputStream());
        stored(() -> mount.store().write(target, content));
        told(mount, Kind.WRITTEN, target, null);
    }

    /** What stands at a key of a mount, file or folder; empty where nothing does. */
    private Optional<FileTree.Entry> entryAt(ExplorerMount mount, StorageKey key) {
        String value = key.value();
        int slash = value.lastIndexOf('/');
        try {
            List<FileTree.Entry> siblings = slash < 0 ? mount.tree().top()
                    : mount.tree().list(StorageKey.of(value.substring(0, slash)));
            return siblings.stream().filter(entry -> entry.key().equals(key)).findFirst();
        } catch (ObjectNotFoundException missing) {
            return Optional.empty();
        }
    }

    /**
     * Where a mount stands on the server's disk, for a reader who may read it — what a plugin working a folder as a whole
     * (Git) works in; empty for a mount that is a store a plugin brings, no folder of the disk.
     */
    public Optional<Path> folderForReading(String mountId) {
        return mounts.find(readable(mountId).id()).map(Held::path);
    }

    /** Where a mount stands on the server's disk, for a reader who may change it; refused as every change is refused. */
    public Optional<Path> folderForChanging(String mountId) {
        return mounts.find(changeable(mountId).id()).map(Held::path);
    }

    /** Every mount as its mounting is managed: only for a reader who may mount, since it says where folders are on disk. */
    public MountingsView mountings() {
        authority.requireGlobally(ExplorerCapability.MOUNT);
        return new MountingsView(true, mounts.all().stream().map(Explorer::mountingOf).toList());
    }

    /** Whether the reader may mount a folder: what the interface asks before it offers to. */
    public boolean mayMount() {
        return authority.permitsGlobally(ExplorerCapability.MOUNT);
    }

    /** A folder of the server's disk mounted, opened at once. */
    public MountingView mount(MountRequest request) {
        authority.requireGlobally(ExplorerCapability.MOUNT);
        if (request.owner() != null) {
            places.requireOwner(request.owner());
        }
        return mountingOf(mounts.mount(request));
    }

    /** A folder a curator mounted, unmounted; what it holds on disk is untouched. */
    public void unmount(String mountId) {
        authority.requireGlobally(ExplorerCapability.MOUNT);
        mounts.unmount(mountId);
    }

    private static MountingView mountingOf(Held held) {
        ExplorerMount mount = held.mount();
        return new MountingView(mount.id(), mount.title(), mount.owner() == null ? null : mount.owner().type(),
                                mount.owner() == null ? null : mount.owner().id(), mount.access(), held.origin(),
                                held.path() == null ? null : held.path().toString());
    }

    /** A file or a folder renamed, or filed elsewhere in the same mount. */
    public void move(String mountId, String from, String to) {
        ExplorerMount mount = changeable(mountId);
        StorageKey source = keyOf(from);
        StorageKey target = keyOf(to);
        stored(() -> {
            mount.tree().move(source, target);
            return target;
        });
        told(mount, Kind.MOVED, target, source);
    }

    /** A file removed, or a folder once it is empty. */
    public void delete(String mountId, String key, boolean folder) {
        ExplorerMount mount = changeable(mountId);
        StorageKey target = keyOf(key);
        stored(() -> {
            if (folder) {
                mount.tree().deleteFolder(target);
            } else {
                mount.store().describe(target);
                mount.store().delete(target);
            }
            return target;
        });
        told(mount, Kind.REMOVED, target, null);
    }

    private void told(ExplorerMount mount, Kind kind, StorageKey key, StorageKey previous) {
        extensions.publish(ExplorerChange.TOPIC, new ExplorerChange(mount.id(), kind, key.value(),
                                                                     previous == null ? null : previous.value()));
    }

    private EntryView entryOf(ExplorerMount mount, StorageKey file) {
        String value = file.value();
        int slash = value.lastIndexOf('/');
        List<FileTree.Entry> siblings = stored(() -> slash < 0 ? mount.tree().top()
                : mount.tree().list(StorageKey.of(value.substring(0, slash))));
        return siblings.stream().filter(entry -> entry.key().equals(file)).findFirst().map(Explorer::viewOf)
                .orElseThrow(() -> CadProblem.notFound("The file '" + value + "'"));
    }

    private static EntryView viewOf(FileTree.Entry entry) {
        return new EntryView(entry.key().value(), entry.name(), entry.folder(), entry.sizeBytes(), entry.modifiedAt());
    }

    /** A mount the reader may read; one they may not is not found, as if it were not there. */
    private ExplorerMount readable(String mountId) {
        return mounts.find(mountId).map(Held::mount).filter(this::mayRead)
                .orElseThrow(() -> CadProblem.notFound("The folder '" + mountId + "'"));
    }

    private ExplorerMount changeable(String mountId) {
        ExplorerMount mount = readable(mountId);
        if (!mount.writable()) {
            throw CadProblem.conflict("'" + mount.title() + "' is mounted for reading only.");
        }
        authority.require(ExplorerCapability.WRITE, placeOf(mount));
        return mount;
    }

    private boolean mayRead(ExplorerMount mount) {
        return authority.permits(ExplorerCapability.READ, placeOf(mount));
    }

    private boolean mayWrite(ExplorerMount mount) {
        return authority.permits(ExplorerCapability.WRITE, placeOf(mount));
    }

    /** Where a mount is: the installation's, or its owner's whole chain. */
    private CadPlace placeOf(ExplorerMount mount) {
        return mount.owner() == null ? CadPlace.installation() : places.requireOwner(mount.owner());
    }

    /** A key as written; one that passes through a reserved name is not there, as far as the explorer goes. */
    private StorageKey keyOf(String written) {
        StorageKey key;
        try {
            key = StorageKey.of(written);
        } catch (StorageKeyException refused) {
            throw CadProblem.invalid(refused.getMessage());
        }
        for (String segment : key.value().split("/")) {
            if (reserved(segment)) {
                throw CadProblem.notFound("'" + written + "'");
            }
        }
        return key;
    }

    /** Whether a plugin keeps this name out of the explorer's folders. */
    private boolean reserved(String name) {
        return extensions.contributions(ExplorerReserved.POINT).stream().anyMatch(kept -> kept.name().equals(name));
    }

    /** A storage call, its refusals as the problems every route answers with. */
    private static <T> T stored(Supplier<T> call) {
        try {
            return call.get();
        } catch (ObjectNotFoundException missing) {
            throw new CadProblem.NotFound(missing.getMessage() + ".");
        } catch (StorageKeyException refused) {
            throw CadProblem.invalid(refused.getMessage());
        } catch (StorageException failed) {
            throw CadProblem.conflict(failed.getMessage());
        }
    }
}
