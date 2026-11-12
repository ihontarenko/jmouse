package org.jmouse.git.local;

import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

/** Opens only the configured working-tree root, never silently adopts a parent repository. */
final class RepositoryAccess {
    private RepositoryAccess() {}

    static Repository open(Path directory) throws IOException {
        Path root = directory.toRealPath();
        FileRepositoryBuilder builder = new FileRepositoryBuilder().findGitDir(root.toFile());
        if (builder.getGitDir() == null) {
            return null;
        }
        Repository repository = builder.setMustExist(true).build();
        if (repository.isBare() || !repository.getWorkTree().toPath().toRealPath().equals(root)) {
            repository.close();
            return null;
        }
        return repository;
    }

    /**
     * Runs no hook of the repository's for what this program writes: only this Repository object's configuration changes,
     * so no hooksPath is persisted and the user's Git settings are never overwritten.
     */
    static void withoutHooks(Repository repository) {
        repository.getConfig().setString(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_HOOKS_PATH,
                repository.getDirectory().toPath().resolve(UUID.randomUUID().toString()).toString());
    }

    static Repository require(Path directory) throws IOException {
        Repository repository = open(directory);
        if (repository == null) {
            throw new LocalGitException(
                    LocalGitException.Reason.NOT_A_REPOSITORY,
                    "This project root is not a Git working tree.");
        }
        return repository;
    }
}
