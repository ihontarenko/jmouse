package org.jmouse.git.local;

import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Selected working-tree commits; unrelated staged entries are left to JGit's setOnly
 * implementation.
 */
final class LocalCommitWriter {
    private final LocalGitLimits limits;

    LocalCommitWriter(LocalGitLimits limits) {
        this.limits = limits;
    }

    synchronized CommitResult commit(Path directory, CommitSelection selection) {
        if (selection.files().size() > limits.maximumSelectedFiles()) {
            throw new LocalGitException(
                    LocalGitException.Reason.TOO_LARGE,
                    "Too many files were selected for one interactive commit.");
        }
        try (Repository repository = RepositoryAccess.require(directory)) {
            if (repository.getRepositoryState() != RepositoryState.SAFE) {
                throw new LocalGitException(
                        LocalGitException.Reason.UNSUPPORTED_STATE,
                        "Finish the current merge, rebase or cherry-pick before committing here.");
            }
            if (selection.author() == null && !GitAuthor.read(repository).configured()) {
                throw new LocalGitException(
                        LocalGitException.Reason.IDENTITY_REQUIRED,
                        "Configure Git user.name and user.email before committing.");
            }
            ObjectId head = repository.resolve(Constants.HEAD);
            if (!(head == null ? "" : head.name()).equals(selection.expectedHead())) {
                throw new LocalGitException(
                        LocalGitException.Reason.CONFLICT,
                        "HEAD changed since the commit was reviewed. Refresh the Git view.");
            }
            WorkingTreeFiles files =
                    new WorkingTreeFiles(repository.getWorkTree().toPath().toRealPath(), limits);
            for (CommitSelection.SelectedFile selected : selection.files()) {
                if (!WorkingTreeFiles.version(files.read(selected.path()))
                        .equals(selected.version())) {
                    throw new LocalGitException(
                            LocalGitException.Reason.CONFLICT,
                            "A selected file changed since it was reviewed. Refresh the diff.");
                }
            }
            Git git = Git.wrap(repository);
            Status status = git.status().call();
            if (!status.getConflicting().isEmpty()) {
                throw new LocalGitException(
                        LocalGitException.Reason.CONFLICT,
                        "Resolve Git conflicts before committing.");
            }
            RepositoryAccess.withoutHooks(repository);
            for (CommitSelection.SelectedFile selected : selection.files()) {
                if (status.getUntracked().contains(selected.path())) {
                    git.add().addFilepattern(selected.path()).call();
                }
            }
            CommitCommand command =
                    git.commit()
                            .setMessage(selection.message().trim())
                            .setNoVerify(true)
                            .setSign(false);
            if (selection.author() != null) {
                PersonIdent person = new PersonIdent(selection.author().name(), selection.author().email());
                command.setAuthor(person).setCommitter(person);
            }
            for (CommitSelection.SelectedFile selected : selection.files()) {
                command.setOnly(selected.path());
            }
            return new CommitResult(command.call().name(), true);
        } catch (IOException | GitAPIException exception) {
            throw JGitLocalGit.storage(
                    "The commit could not be created. Newly selected files may now be staged;"
                        + " inspect Git status before retrying.");
        }
    }
}
