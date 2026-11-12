package org.jmouse.cad.plugin.explorer;

import org.jmouse.cad.store.CadRead;
import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.source.DocumentLocation.Access;
import org.jmouse.cad.jpa.mount.MountedFolder;
import org.jmouse.cad.jpa.mount.MountedFolderRecords;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.plugin.Extensions;
import org.jmouse.storage.local.LocalFileStore;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 📁 Every folder the explorer opens, from where it was mounted: the server's configuration and the plugins bring theirs
 * ({@link ExplorerMount#POINT}), fixed while the server runs; a curator mounts more from the interface, kept in
 * {@code cad_mounted_folders} and opened at once, without a restart.
 *
 * <p>It checks what a folder is, never who may mount it: that is {@link Explorer}'s, through the engine's seam.</p>
 */
public class ExplorerMounts {

    /** Where a mount comes from — what says whether it can be unmounted from the interface. */
    public enum Origin {
        /** The server's configuration or a plugin: fixed while the server runs. */
        CONFIGURED,
        /** A curator, from the interface: kept by the installation, unmounted the same way. */
        MOUNTED
    }

    /**
     * A mount and where it comes from.
     *
     * @param path where it is on the server's disk; null for a store a plugin brings
     */
    public record Held(ExplorerMount mount, Origin origin, Path path) {
    }

    /** A folder a curator asks to mount. */
    public record MountRequest(String id, String title, String path, Access access, DocumentOwner owner) {
    }

    private final Extensions                   extensions;
    private final MountedFolderRecords         records;
    private final Clock                        clock;
    /** The store of each kept folder, made once: a listing must not open its folder anew. */
    private final Map<String, ExplorerMount>   opened = new ConcurrentHashMap<>();

    public ExplorerMounts(Extensions extensions, MountedFolderRecords records, Clock clock) {
        this.extensions = extensions;
        this.records = records;
        this.clock = clock;
    }

    /** Every mount: the configured ones first, then the kept ones by title. */
    @CadRead
    public List<Held> all() {
        List<Held> held = new ArrayList<>();
        extensions.contributions(ExplorerMount.POINT).forEach(mount -> held.add(new Held(mount, Origin.CONFIGURED, mount.folder())));
        records.all().forEach(folder -> held.add(heldOf(folder)));
        return held;
    }

    @CadRead
    public Optional<Held> find(String id) {
        Optional<Held> configured = extensions.contributions(ExplorerMount.POINT).stream()
                .filter(mount -> mount.id().equals(id)).findFirst().map(mount -> new Held(mount, Origin.CONFIGURED, mount.folder()));
        return configured.isPresent() ? configured : records.find(id).map(this::heldOf);
    }

    /** Mounts a folder of the server's disk; refused, saying why, when it is no folder or its id is taken. */
    @Transactional
    public Held mount(MountRequest request) {
        if (request.id() == null || !request.id().matches(ExplorerMount.ID_PATTERN)) {
            throw CadProblem.invalid("A mount's id is one word of lowercase letters, digits and dashes, as datasheets.");
        }
        if (request.access() == null) {
            throw CadProblem.invalid("Say whether the folder is mounted for reading only or for changing.");
        }
        if (find(request.id()).isPresent()) {
            throw CadProblem.conflict("A folder is already mounted as '" + request.id() + "'.");
        }
        Path path = folderOf(request.path());
        String title = request.title() == null || request.title().isBlank() ? request.id() : request.title().strip();
        MountedFolder folder = new MountedFolder(request.id(), title, path.toString(), request.access(), request.owner(),
                                                 clock.instant());
        records.keep(folder);
        return heldOf(folder);
    }

    /** Unmounts a folder a curator mounted; what it holds on disk is untouched. */
    @Transactional
    public void unmount(String id) {
        Held held = find(id).orElseThrow(() -> CadProblem.notFound("The folder '" + id + "'"));
        if (held.origin() == Origin.CONFIGURED) {
            throw CadProblem.conflict("'" + held.mount().title() + "' is mounted by the server's configuration; it is "
                                      + "unmounted there.");
        }
        records.remove(id);
        opened.remove(id);
    }

    private Held heldOf(MountedFolder folder) {
        ExplorerMount mount = opened.computeIfAbsent(folder.getId(), unopened -> ExplorerMount.of(folder.getId(),
                folder.getTitle(), folder.getOwner(), new LocalFileStore(ExplorerPlugin.ID + "-" + folder.getId(),
                                                                        Path.of(folder.getPath())), folder.getAccess()));
        return new Held(mount, Origin.MOUNTED, Path.of(folder.getPath()));
    }

    /** The folder a written path names: absolute, and there. */
    private static Path folderOf(String written) {
        if (written == null || written.isBlank()) {
            throw CadProblem.invalid("Name the folder on the server's disk to mount.");
        }
        Path path;
        try {
            path = Path.of(written.strip()).normalize();
        } catch (InvalidPathException unreadable) {
            throw CadProblem.invalid("'" + written + "' is no path on the server: " + unreadable.getReason() + ".");
        }
        if (!path.isAbsolute()) {
            throw CadProblem.invalid("'" + written + "' is not a whole path: name it from the disk's top, as D:/library is.");
        }
        if (!Files.isDirectory(path)) {
            throw CadProblem.invalid("There is no folder at '" + path + "' on the server.");
        }
        return path;
    }
}
