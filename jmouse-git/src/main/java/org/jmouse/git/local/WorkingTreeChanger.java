package org.jmouse.git.local;

import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * What rewrites a working tree: the branches, and a file taken back to its last commit. Nothing here loses work without
 * being told: a switch is refused over any change, a file is taken back only at the version the reader saw.
 *
 * <p>⚠️ <strong>A folder's own {@code .git/config} may name a program to run</strong>: JGit runs a filter's {@code smudge}
 * command as it writes a file out, and a mounted folder is not something the server wrote. So a repository whose own
 * configuration declares a filter is refused here. The server's user's configuration is the operator's own and is not
 * asked about: {@code git lfs install} writes a filter there.</p>
 */
final class WorkingTreeChanger {

    private final LocalGitLimits limits;
    private final CommitReader   history;

    WorkingTreeChanger(LocalGitLimits limits, CommitReader history) {
        this.limits = limits;
        this.history = history;
    }

    List<BranchInfo> branches(Path directory) {
        try (Repository repository = RepositoryAccess.require(directory)) {
            String current = repository.getFullBranch();
            List<BranchInfo> branches = new ArrayList<>();
            for (Ref branch : Git.wrap(repository).branchList().call()) {
                ObjectId head = branch.getObjectId();
                branches.add(new BranchInfo(Repository.shortenRefName(branch.getName()), head == null ? "" : head.name(),
                        branch.getName().equals(current)));
            }
            return List.copyOf(branches);
        } catch (IOException | GitAPIException exception) {
            throw JGitLocalGit.storage("The branches could not be read.");
        }
    }

    /** A new branch at the head, switched to: the working tree stays exactly as it is, its changes with it. */
    synchronized void createBranch(Path directory, String name) {
        try (Repository repository = RepositoryAccess.require(directory)) {
            requireSafe(repository);
            requireNoOwnFilter(repository);
            String reference = Constants.R_HEADS + (name == null ? "" : name);
            if (name == null || !Repository.isValidRefName(reference)) {
                throw new LocalGitException(LocalGitException.Reason.INVALID_PATH, "'" + name + "' cannot be the name of a branch.");
            }
            if (repository.resolve(Constants.HEAD) == null) {
                throw new LocalGitException(LocalGitException.Reason.UNSUPPORTED_STATE, "Commit something first: a branch stands at a commit.");
            }
            if (repository.exactRef(reference) != null) {
                throw new LocalGitException(LocalGitException.Reason.CONFLICT, "There is a branch '" + name + "' already.");
            }
            Git.wrap(repository).checkout().setCreateBranch(true).setName(name).call();
        } catch (IOException | GitAPIException exception) {
            throw JGitLocalGit.storage("The branch could not be made.");
        }
    }

    /** The working tree moved to a branch; refused while any tracked file differs, so a switch never carries or loses work. */
    synchronized void switchBranch(Path directory, String name) {
        try (Repository repository = RepositoryAccess.require(directory)) {
            requireSafe(repository);
            requireNoOwnFilter(repository);
            String reference = Constants.R_HEADS + name;
            if (name == null || !Repository.isValidRefName(reference) || repository.exactRef(reference) == null) {
                throw new LocalGitException(LocalGitException.Reason.NOT_FOUND, "There is no branch '" + name + "'.");
            }
            if (reference.equals(repository.getFullBranch())) {
                return;
            }
            int changed = trackedChanges(Git.wrap(repository).status().call());
            if (changed > 0) {
                throw new LocalGitException(LocalGitException.Reason.CONFLICT,
                        "Commit or take back the " + changed + " changed file" + (changed == 1 ? "" : "s") + " first.");
            }
            Git.wrap(repository).checkout().setName(name).call();
        } catch (CheckoutConflictException exception) {
            throw new LocalGitException(LocalGitException.Reason.CONFLICT,
                    "New files here would be overwritten by that branch: " + String.join(", ", exception.getConflictingPaths()));
        } catch (IOException | GitAPIException exception) {
            throw JGitLocalGit.storage("The branch could not be switched to.");
        }
    }

    /**
     * Another branch brought into the one in hand: moved up to it where the one in hand has nothing of its own, else joined by a merge
     * commit by `author` — the repository's own identity where none is given. Refused over changes in hand; where the two changed the
     * same lines, refused with the files named, and nothing is changed: the repository is left as it was.
     */
    synchronized MergeOutcome mergeBranch(Path directory, String name, CommitAuthor author) {
        try (Repository repository = RepositoryAccess.require(directory)) {
            requireSafe(repository);
            requireNoOwnFilter(repository);
            String reference = Constants.R_HEADS + name;
            Ref other = name == null || !Repository.isValidRefName(reference) ? null : repository.exactRef(reference);
            if (other == null) {
                throw new LocalGitException(LocalGitException.Reason.NOT_FOUND, "There is no branch '" + name + "'.");
            }
            String into = repository.getBranch();
            if (reference.equals(repository.getFullBranch())) {
                throw new LocalGitException(LocalGitException.Reason.CONFLICT, "A branch is not brought into itself.");
            }
            Git git = Git.wrap(repository);
            int changed = trackedChanges(git.status().call());
            if (changed > 0) {
                throw new LocalGitException(LocalGitException.Reason.CONFLICT,
                        "Commit or take back the " + changed + " changed file" + (changed == 1 ? "" : "s") + " first.");
            }
            if (author == null && !GitAuthor.read(repository).configured()) {
                throw new LocalGitException(LocalGitException.Reason.IDENTITY_REQUIRED, "Configure Git user.name and user.email before merging.");
            }
            MergeResult merged = git.merge().include(other).setCommit(false).setFastForward(MergeCommand.FastForwardMode.FF).call();
            return switch (merged.getMergeStatus()) {
                case ALREADY_UP_TO_DATE -> new MergeOutcome(name, into, MergeOutcome.State.ALREADY_HERE, headOf(repository));
                case FAST_FORWARD -> new MergeOutcome(name, into, MergeOutcome.State.FAST_FORWARD, headOf(repository));
                case MERGED_NOT_COMMITTED -> {
                    CommitCommand commit = git.commit().setMessage("Merge branch '" + name + "' into '" + into + "'").setNoVerify(true).setSign(false);
                    if (author != null) {
                        PersonIdent person = new PersonIdent(author.name(), author.email());
                        commit.setAuthor(person).setCommitter(person);
                    }
                    yield new MergeOutcome(name, into, MergeOutcome.State.MERGED, commit.call().name());
                }
                case CONFLICTING -> {
                    List<String> files = merged.getConflicts() == null ? List.of() : List.copyOf(merged.getConflicts().keySet());
                    abandonMerge(git, repository);
                    throw new LocalGitException(LocalGitException.Reason.CONFLICT, "'" + name + "' and '" + into + "' changed the same lines of "
                            + String.join(", ", files) + ". Nothing was changed.");
                }
                default -> {
                    abandonMerge(git, repository);
                    throw new LocalGitException(LocalGitException.Reason.CONFLICT, "It could not be brought in: " + merged.getMergeStatus() + ". Nothing was changed.");
                }
            };
        } catch (CheckoutConflictException exception) {
            throw new LocalGitException(LocalGitException.Reason.CONFLICT,
                    "New files here would be overwritten by that branch: " + String.join(", ", exception.getConflictingPaths()));
        } catch (IOException | GitAPIException exception) {
            throw JGitLocalGit.storage("The branch could not be brought in.");
        }
    }

    /** A merge left half done put away: the working tree and the index back at the branch's head, no merge in hand. */
    private static void abandonMerge(Git git, Repository repository) throws GitAPIException, IOException {
        repository.writeMergeCommitMsg(null);
        repository.writeMergeHeads(null);
        git.reset().setMode(ResetCommand.ResetType.HARD).setRef(Constants.HEAD).call();
    }

    private static String headOf(Repository repository) throws IOException {
        ObjectId head = repository.resolve(Constants.HEAD);
        return head == null ? "" : head.name();
    }

    /**
     * A file put back as its last commit has it: its change dropped from the index too.
     *
     * @param expectedVersion the version the reader was shown: another means somebody changed it since, and it is refused
     */
    synchronized void restore(Path directory, String source, String expectedVersion) {
        try (Repository repository = RepositoryAccess.require(directory)) {
            requireSafe(repository);
            requireNoOwnFilter(repository);
            WorkingTreeFiles files = new WorkingTreeFiles(repository.getWorkTree().toPath().toRealPath(), limits);
            Path target = files.path(source);
            if (!history.inHead(repository, source)) {
                throw new LocalGitException(LocalGitException.Reason.UNSUPPORTED_STATE,
                        "'" + source + "' is not in the last commit: there is nothing to take it back to.");
            }
            if (!files.version(source).equals(expectedVersion)) {
                throw new LocalGitException(LocalGitException.Reason.CONFLICT, "'" + source + "' changed since it was shown.");
            }
            byte[] committed = history.atHead(repository, source);
            Git.wrap(repository).reset().addPath(source).call();
            Files.createDirectories(target.getParent());
            Files.write(target, committed);
        } catch (LargeObjectException exception) {
            throw new LocalGitException(LocalGitException.Reason.TOO_LARGE, "The file exceeds the interactive Git size limit.");
        } catch (IOException | GitAPIException exception) {
            throw JGitLocalGit.storage("The file could not be taken back.");
        }
    }

    private static int trackedChanges(Status status) {
        Set<String> changed = new java.util.TreeSet<>();
        List.of(status.getAdded(), status.getChanged(), status.getRemoved(), status.getModified(), status.getMissing(),
                status.getConflicting()).forEach(changed::addAll);
        return changed.size();
    }

    private static void requireSafe(Repository repository) {
        if (repository.getRepositoryState() != RepositoryState.SAFE) {
            throw new LocalGitException(LocalGitException.Reason.UNSUPPORTED_STATE, "Finish the current merge, rebase or cherry-pick first.");
        }
    }

    static void requireNoOwnFilter(Repository repository) throws IOException {
        FileBasedConfig own = new FileBasedConfig(new File(repository.getDirectory(), Constants.CONFIG), FS.DETECTED);
        try {
            own.load();
        } catch (ConfigInvalidException unreadable) {
            throw new LocalGitException(LocalGitException.Reason.UNSUPPORTED_STATE, "The repository's own configuration cannot be read.");
        }
        if (!own.getSubsections(ConfigConstants.CONFIG_FILTER_SECTION).isEmpty()) {
            throw new LocalGitException(LocalGitException.Reason.UNSUPPORTED_STATE,
                    "This repository's own configuration names a filter, a program Git would run here: use Git itself in this folder.");
        }
    }
}
