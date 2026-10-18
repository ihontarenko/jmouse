package org.jmouse.git.local;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Path;

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
