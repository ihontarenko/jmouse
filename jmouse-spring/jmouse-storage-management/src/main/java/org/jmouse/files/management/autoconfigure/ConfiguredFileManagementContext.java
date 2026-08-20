package org.jmouse.files.management.autoconfigure;

import org.jmouse.files.OwnerReference;
import org.jmouse.files.management.FileManagementContext;

/**
 * 🧭 One namespace for everything, and nobody named as the uploader.
 *
 * <p>What a product gets when it declares no {@link FileManagementContext} of its own. It is the right
 * answer for a product with a single kind of file, and the safe answer for every other: an absent
 * uploader is a file that belongs to nobody, which a screen shows plainly, whereas a guessed one would
 * quietly hand somebody an own-rows permission over it.</p>
 */
public class ConfiguredFileManagementContext implements FileManagementContext {

    private final String namespace;

    /**
     * 🏗️ Build it around the one namespace this installation files everything under.
     *
     * @param namespace the storage namespace
     */
    public ConfiguredFileManagementContext(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String namespaceFor(OwnerReference owner) {
        return namespace;
    }
}
