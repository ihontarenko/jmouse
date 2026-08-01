package org.jmouse.storage.resource;

import org.jmouse.core.io.AbstractResourceLoader;
import org.jmouse.core.io.Resource;
import org.jmouse.core.io.ResourceLoader;
import org.jmouse.core.io.ResourceLoaderRegistry;
import org.jmouse.helpers.Files;
import org.jmouse.storage.FileStore;
import org.jmouse.storage.StorageKey;

import java.util.List;

/**
 * 🔌 Exposes a {@link FileStore} through the jMouse resource-loader registry, under a
 * {@code storage:} protocol.
 *
 * <pre>{@code
 * registry.addResourceLoader(new FileStoreResourceLoader(fileStore));
 * Resource resource = registry.getRequiredResourceLoader("storage:documents/md/u/42/notes.md")
 *                             .getResource("storage:documents/md/u/42/notes.md");
 * }</pre>
 *
 * <p>Read-only, and deliberately so. {@link ResourceLoader} has nowhere to express writing,
 * describing, deleting or presigning, and inventing a place would mean a second, weaker copy of the
 * storage contract. Those operations stay on {@link FileStore}; this exists purely so jMouse code
 * that already reads a {@link Resource} by location can read from storage without learning a new
 * API.</p>
 *
 * @see ResourceLoaderRegistry
 */
public class FileStoreResourceLoader extends AbstractResourceLoader {

    /**
     * 🏷️ Protocol this loader answers to.
     */
    public static final String STORAGE_PROTOCOL = "storage";

    private final FileStore fileStore;

    /**
     * 🏗️ Read through the given store.
     *
     * @param fileStore the store to expose
     */
    public FileStoreResourceLoader(FileStore fileStore) {
        this.fileStore = fileStore;
    }

    @Override
    public Resource getResource(String location) {
        ensureSupportedProtocol(location);
        return fileStore.read(StorageKey.of(Files.removeProtocol(location)));
    }

    @Override
    public List<String> supportedProtocols() {
        return List.of(STORAGE_PROTOCOL);
    }
}
