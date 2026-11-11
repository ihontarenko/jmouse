package org.jmouse.cad.plugin.explorer;

import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadPlace;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.core.io.Resource;
import org.jmouse.cad.plugin.explorer.ExplorerChange.Kind;
import org.jmouse.plugin.Extensions;
import org.jmouse.storage.Content;
import org.jmouse.storage.FileTree;
import org.jmouse.storage.ObjectDescription;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.exception.ObjectNotFoundException;
import org.jmouse.storage.exception.StorageException;
import org.jmouse.storage.exception.StorageKeyException;

import java.time.Instant;
import java.util.List;
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

    /** A mount as the explorer lists it; {@code changeable} says whether this reader may change it. */
    public record MountView(String id, String title, String ownerType, String ownerId, boolean changeable) {
    }

    /** One thing in a folder; {@code key} is where it is from the mount's top. */
    public record EntryView(String key, String name, boolean folder, long sizeBytes, Instant modifiedAt) {
    }

    /** A file opened for reading: its bytes and what it is. */
    public record Opened(Resource resource, ObjectDescription description) {
    }

    private final Extensions   extensions;
    private final CadAuthority authority;
    private final CadPlaces    places;

    public Explorer(Extensions extensions, CadAuthority authority, CadPlaces places) {
        this.extensions = extensions;
        this.authority = authority;
        this.places = places;
    }

    /** The mounts this reader may open. */
    public List<MountView> mounts() {
        return extensions.contributions(ExplorerMount.POINT).stream().filter(this::mayRead)
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
        return entries.stream().map(Explorer::viewOf).toList();
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
        return extensions.contributions(ExplorerMount.POINT).stream()
                .filter(mount -> mount.id().equals(mountId) && mayRead(mount)).findFirst()
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

    private static StorageKey keyOf(String written) {
        try {
            return StorageKey.of(written);
        } catch (StorageKeyException refused) {
            throw CadProblem.invalid(refused.getMessage());
        }
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
