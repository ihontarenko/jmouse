package org.jmouse.cad.plugin.explorer;

import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.source.ChangeableResources;
import org.jmouse.cad.document.source.DocumentLocation;
import org.jmouse.cad.document.source.DocumentResources;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.store.authority.CadPlace;
import org.jmouse.cad.store.catalogue.DocumentReading;
import org.jmouse.cad.store.catalogue.PlaceSource;
import org.jmouse.cad.store.catalogue.ReadDocument;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.plugin.Extensions;
import org.jmouse.storage.Content;
import org.jmouse.storage.FileTree;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.exception.StorageException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 📂 The documents a mounted folder holds: every {@code .jm} file in it is a document, and a folder that belongs to a
 * place — a project, a workspace — is seen by that place ({@link PlaceSource}) with nothing imported. The file is the
 * document: what stands on the disk now is what is read, and a file is read again only once it has changed.
 *
 * <p>A file that does not read, or says the same address as another, is told apart and left out: one bad file does not
 * cost a folder its other documents.</p>
 */
public class MountDocuments {

    /** How much of a checksum a version keeps: what a double holds exactly, as a browser reads a number. */
    private static final int VERSION_DIGITS = 13;
    private static final int HEXADECIMAL    = 16;

    /** A folder's file that is no document of it, and why. */
    public record Unread(String key, String problem) {
    }

    /** A document a folder holds, and the file it is. */
    public record FolderDocument(String key, ReadDocument read, DocumentLocation location) {
    }

    /** What a folder holds, as it stands. */
    public record FolderReading(List<FolderDocument> documents, List<Unread> unread) {
    }

    /** One file's reading, kept for as long as the file stands as it did. */
    private record KeptReading(long size, Instant modified, ReadDocument read, String problem) {

        boolean of(FileTree.Entry entry) {
            return size == entry.sizeBytes() && Objects.equals(modified, entry.modifiedAt());
        }
    }

    /** What the explorer brings to the engine's places, taken when it is first asked: the engine is made before it. */
    public record Source(Supplier<MountDocuments> documents) implements PlaceSource {

        @Override
        public String id() {
            return "explorer-mounts";
        }

        @Override
        public Holding holding(CadPlace place) {
            return documents.get().holding(place);
        }
    }

    private final ExplorerMounts           mounts;
    private final Extensions               extensions;
    private final DocumentReading          reading;
    private final IntSupplier              mostFiles;
    private final LongSupplier             largestFile;
    private final Map<String, KeptReading> kept = new ConcurrentHashMap<>();

    /**
     * @param mostFiles   how many entries of one folder are looked at: a mount of a whole disk is not walked to its end
     * @param largestFile how large a file is read as a document
     */
    public MountDocuments(ExplorerMounts mounts, Extensions extensions, DocumentReading reading, IntSupplier mostFiles, LongSupplier largestFile) {
        this.mounts = mounts;
        this.extensions = extensions;
        this.reading = reading;
        this.mostFiles = mostFiles;
        this.largestFile = largestFile;
    }

    /** Whether a file's name is a document's. */
    public static boolean namesDocument(String name) {
        return name.endsWith(DocumentInput.FILE_SUFFIX);
    }

    /** What says which text a file held, as a number a client carries whole: changes whenever the text does. */
    public static long versionOf(String source) {
        return Long.parseLong(DocumentStore.checksumOf(source).substring(0, VERSION_DIGITS), HEXADECIMAL);
    }

    /** Everything a mount holds, whoever asks: who may is the explorer's to decide. */
    public FolderReading read(ExplorerMount mount) {
        List<FolderDocument> documents = new ArrayList<>();
        List<Unread> unread = new ArrayList<>();
        Map<String, String> addressed = new HashMap<>();
        Set<String> reserved = extensions.contributions(ExplorerReserved.POINT).stream().map(ExplorerReserved::name)
                .collect(Collectors.toSet());
        Deque<FileTree.Entry> folders = new ArrayDeque<>();
        List<FileTree.Entry> entries = new ArrayList<>(listed(mount, null, unread));
        int looked = 0;
        int most = mostFiles.getAsInt();
        while (!entries.isEmpty()) {
            for (FileTree.Entry entry : entries) {
                looked++;
                if (reserved.contains(entry.name())) {
                    continue;
                }
                if (entry.folder()) {
                    folders.add(entry);
                } else if (namesDocument(entry.name())) {
                    place(mount, entry, documents, unread, addressed);
                }
            }
            entries = List.of();
            if (looked >= most) {
                unread.add(new Unread("", "The folder holds more than " + most + " entries: only the first were looked at."));
                break;
            }
            if (!folders.isEmpty()) {
                entries = listed(mount, folders.poll().key(), unread);
            }
        }
        return new FolderReading(List.copyOf(documents), List.copyOf(unread));
    }

    /** What the folders of a place hold, the nearest owner's first: a project's own folder before its workspace's. */
    public PlaceSource.Holding holding(CadPlace place) {
        List<ExplorerMount> placed = new ArrayList<>();
        List<ExplorerMounts.Held> all = place.isInstallation() ? List.of() : mounts.all();
        for (int link = place.chain().size() - 1; link >= 0; link--) {
            for (ExplorerMounts.Held held : all) {
                if (place.chain().get(link).equals(held.mount().owner())) {
                    placed.add(held.mount());
                }
            }
        }
        return new PlaceSource.Holding() {
            @Override
            public List<PlaceSource.PlacedDocument> documents() {
                return placed.stream().flatMap(mount -> read(mount).documents().stream())
                        .map(document -> new PlaceSource.PlacedDocument(document.read(), document.location())).toList();
            }

            @Override
            public Optional<DocumentResources> resourcesOf(DocumentLocation location) {
                if (!(location instanceof DocumentLocation.Mounted file)) {
                    return Optional.empty();
                }
                return placed.stream().filter(mount -> mount.id().equals(file.mount())).findFirst()
                        .map(mount -> besideOf(mount, file.key()));
            }
        };
    }

    /**
     * The files beside a document, by name: its 3D model's file, its datasheet. A name is a file's name alone: nothing
     * out of the document's own folder is reached through it.
     */
    private static DocumentResources besideOf(ExplorerMount mount, String key) {
        int slash = key.lastIndexOf('/');
        String folder = slash < 0 ? "" : key.substring(0, slash + 1);
        DocumentResources reading = new DocumentResources() {
            @Override
            public InputStream open(String name) throws IOException {
                try {
                    return mount.store().read(keyBeside(folder, name)).getInputStream();
                } catch (StorageException missing) {
                    throw new FileNotFoundException("Beside " + key + " there is no file " + name);
                }
            }

            @Override
            public List<String> names() {
                List<FileTree.Entry> beside = folder.isEmpty() ? mount.tree().top() : mount.tree().list(StorageKey.of(folder.substring(0, folder.length() - 1)));
                return beside.stream().filter(entry -> !entry.folder()).map(FileTree.Entry::name).toList();
            }
        };
        if (!mount.writable()) {
            return reading;
        }
        return new ChangeableResources() {
            @Override
            public InputStream open(String name) throws IOException {
                return reading.open(name);
            }

            @Override
            public List<String> names() {
                return reading.names();
            }

            @Override
            public void keep(String name, byte[] content) throws IOException {
                if (names().contains(name)) {
                    throw new FileAlreadyExistsException(name);
                }
                mount.store().write(keyBeside(folder, name), Content.ofBytes(name, null, content));
            }

            @Override
            public boolean remove(String name) throws IOException {
                if (!names().contains(name)) {
                    return false;
                }
                mount.store().delete(keyBeside(folder, name));
                return true;
            }
        };
    }

    /** A file named beside a document: a name alone, so nothing out of the document's own folder is reached through it. */
    private static StorageKey keyBeside(String folder, String name) throws FileNotFoundException {
        if (name.isBlank() || name.contains("/") || name.contains("\\")) {
            throw new FileNotFoundException("A document's file is named by its name alone: " + name);
        }
        return StorageKey.of(folder + name);
    }

    private List<FileTree.Entry> listed(ExplorerMount mount, StorageKey folder, List<Unread> unread) {
        try {
            return folder == null ? mount.tree().top() : mount.tree().list(folder);
        } catch (StorageException failed) {
            unread.add(new Unread(folder == null ? "" : folder.value(), failed.getMessage()));
            return List.of();
        }
    }

    private void place(ExplorerMount mount, FileTree.Entry entry, List<FolderDocument> documents, List<Unread> unread,
                       Map<String, String> addressed) {
        String key = entry.key().value();
        KeptReading reading = readingOf(mount, entry);
        if (reading.problem() != null) {
            unread.add(new Unread(key, reading.problem()));
            return;
        }
        String address = reading.read().index().slug();
        String other = addressed.putIfAbsent(address, key);
        if (other != null) {
            unread.add(new Unread(key, "It says the address '" + address + "', which " + other + " says too."));
            return;
        }
        documents.add(new FolderDocument(key, reading.read(), new DocumentLocation.Mounted(mount.id(), key, mount.access())));
    }

    private KeptReading readingOf(ExplorerMount mount, FileTree.Entry entry) {
        String key = mount.id() + ":" + entry.key().value();
        KeptReading known = kept.get(key);
        if (known != null && known.of(entry)) {
            return known;
        }
        KeptReading read = readFile(mount, entry);
        kept.put(key, read);
        return read;
    }

    private KeptReading readFile(ExplorerMount mount, FileTree.Entry entry) {
        long largest = largestFile.getAsLong();
        if (entry.sizeBytes() > largest) {
            return new KeptReading(entry.sizeBytes(), entry.modifiedAt(), null, "It is larger than a document is read at: " + largest + " bytes.");
        }
        try (InputStream body = mount.store().read(entry.key()).getInputStream()) {
            String source = new String(body.readNBytes(Math.toIntExact(largest)), StandardCharsets.UTF_8);
            return new KeptReading(entry.sizeBytes(), entry.modifiedAt(), reading.read(source, entry.key().value()), null);
        } catch (CadProblem refused) {
            return new KeptReading(entry.sizeBytes(), entry.modifiedAt(), null, refused.getMessage());
        } catch (IOException | UncheckedIOException | StorageException failed) {
            return new KeptReading(entry.sizeBytes(), entry.modifiedAt(), null, failed.getMessage());
        }
    }
}
