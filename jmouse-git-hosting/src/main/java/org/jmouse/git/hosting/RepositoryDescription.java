package org.jmouse.git.hosting;

import java.util.Optional;

/**
 * What a hosting says about one repository.
 *
 * @param repository    the repository, with the owner and name spelled the way the hosting stores them
 * @param visibility    who may read it
 * @param defaultBranch the branch its {@code HEAD} names, or {@code null} when the hosting reports none — an empty
 *                      GitLab project has no default branch
 */
public record RepositoryDescription(RepositoryReference repository, Visibility visibility, String defaultBranch) {

    public RepositoryDescription {
        if (repository == null || visibility == null) {
            throw new IllegalArgumentException("A repository description needs a repository and a visibility.");
        }
        if (defaultBranch != null && defaultBranch.isBlank()) {
            defaultBranch = null;
        }
    }

    /** Whether reading the repository needs credentials. */
    public boolean isPrivate() {
        return visibility.requiresCredentials();
    }

    /** The default branch, when the hosting reports one. */
    public Optional<String> defaultBranchName() {
        return Optional.ofNullable(defaultBranch);
    }
}
