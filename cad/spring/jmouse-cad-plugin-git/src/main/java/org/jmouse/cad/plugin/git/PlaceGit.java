package org.jmouse.cad.plugin.git;

import org.jmouse.git.local.CommitAuthors;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.git.local.BranchInfo;
import org.jmouse.git.local.CommitAuthor;
import org.jmouse.git.local.CommitDetail;
import org.jmouse.git.local.CommitInfo;
import org.jmouse.git.local.CommitResult;
import org.jmouse.git.local.CommitSelection.SelectedFile;
import org.jmouse.git.local.CommitSelection;
import org.jmouse.git.local.LocalGit;
import org.jmouse.git.local.LocalGitException;
import org.jmouse.git.local.PushOutcome;
import org.jmouse.git.local.WorkingTreeDiff;
import org.jmouse.git.local.WorkingTreeStatus;
import org.jmouse.settings.ParameterValues;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 🌿 Git over a place whose history it keeps — a folder the explorer mounts, a project ({@link GitPlace}) —, read and changed only
 * where the place's owner lets the reader ({@link GitFolders}): this service decides nothing of its own. Git's refusals are told
 * as the engine's problems.
 */
public final class PlaceGit {

    private final ParameterValues hosts;
    private final ParameterValues history;

    private final GitFolders                 folders;
    private final LocalGit                   git;
    private final CadAuthority               authority;
    private final Optional<CommitAuthors> authors;

    /** The message of a commit, and the files of it with the versions the reader reviewed, against the head shown. */
    public record CommitRequest(String message, String expectedHead, List<SelectedFile> files) {
    }

    /** The tokens' settings, by the one host each is ever sent to. */
    private static final Map<String, String> TOKEN_OF_HOST = Map.of("github.com", GitPlugin.GITHUB_TOKEN, "gitlab.com", GitPlugin.GITLAB_TOKEN);

    private static final String HTTPS = "https://";

    /** Where a mount's repository pushes to; null for nowhere yet. */
    public record RemoteView(String address) {
    }

    public record RemoteRequest(String address) {
    }

    public PlaceGit(GitFolders folders, LocalGit git, CadAuthority authority, Optional<CommitAuthors> authors, ParameterValues hosts, ParameterValues history) {
        this.hosts = hosts;
        this.history = history;
        this.folders = folders;
        this.git = git;
        this.authority = authority;
        this.authors = authors;
    }

    /** The mount's branch, head and changes, and who a commit is by — or that it is not a repository yet. */
    public WorkingTreeStatus status(GitPlace place) {
        Path folder = folders.readable(place);
        return told(() -> git.status(folder, author()));
    }

    public RemoteView remote(GitPlace place) {
        Path folder = folders.readable(place);
        return told(() -> new RemoteView(git.remote(folder).orElse(null)));
    }

    /** Where it pushes to, set by whoever may change it: only a web address, so no token ever leaves as anything else. */
    public RemoteView setRemote(GitPlace place, String address) {
        Path folder = folders.changeable(place);
        if (address == null || !address.strip().startsWith(HTTPS)) {
            throw CadProblem.invalid("A remote is a web address of a repository, as https://github.com/you/project.git.");
        }
        return told(() -> {
            git.setRemote(folder, address.strip());
            return new RemoteView(git.remote(folder).orElse(null));
        });
    }

    /** The current branch sent to the remote, under the token of the host it really goes to. */
    public PushOutcome push(GitPlace place) {
        Path folder = folders.changeable(place);
        return told(() -> git.push(folder, this::tokenOf));
    }

    /** The remote's commits of the current branch taken in, only by moving forward; the mount's new state is what is answered. */
    public WorkingTreeStatus pull(GitPlace place) {
        Path folder = folders.changeable(place);
        return told(() -> {
            git.pull(folder, this::tokenOf);
            return git.status(folder, author());
        });
    }

    private Optional<String> tokenOf(String host) {
        return Optional.ofNullable(TOKEN_OF_HOST.get(host)).map(hosts::secret).filter(token -> !token.isBlank());
    }

    /** The mount made a repository, its files untouched: all of them become changes to commit. */
    public WorkingTreeStatus initialize(GitPlace place) {
        Path folder = folders.changeable(place);
        told(() -> {
            git.initialize(folder, history.text(GitPlugin.FIRST_BRANCH));
            return null;
        });
        return told(() -> git.status(folder, author()));
    }

    /** The latest commits, newest first, at most {@code limit}. */
    public List<CommitInfo> log(GitPlace place, int limit) {
        Path folder = folders.readable(place);
        return told(() -> git.log(folder, limit));
    }

    /** A file as it is against its last commit. */
    public WorkingTreeDiff diff(GitPlace place, String path) {
        Path folder = folders.readable(place);
        return told(() -> git.diff(folder, path, null));
    }

    /** The reviewed versions of the chosen files committed, by the signed-in person where the host names them. */
    public CommitResult commit(GitPlace place, CommitRequest request) {
        Path folder = folders.changeable(place);
        CommitSelection selection = told(() -> new CommitSelection(request.message(), request.expectedHead(), request.files(), author()));
        return told(() -> git.commit(folder, selection));
    }

    /** What a commit holds. */
    public CommitDetail commitOf(GitPlace place, String id) {
        Path folder = folders.readable(place);
        return told(() -> git.commit(folder, id));
    }

    /** A file at a commit against its first parent. */
    public WorkingTreeDiff commitDiff(GitPlace place, String id, String path) {
        Path folder = folders.readable(place);
        return told(() -> git.commitDiff(folder, id, path));
    }

    /** The mount's branches. */
    public List<BranchInfo> branches(GitPlace place) {
        Path folder = folders.readable(place);
        return told(() -> git.branches(folder));
    }

    /** A new branch at the head, switched to; the status it leaves. */
    public WorkingTreeStatus createBranch(GitPlace place, String name) {
        Path folder = folders.changeable(place);
        told(() -> {
            git.createBranch(folder, name);
            return null;
        });
        return told(() -> git.status(folder, author()));
    }

    /** The mount moved to a branch; the status it leaves. */
    public WorkingTreeStatus switchBranch(GitPlace place, String name) {
        Path folder = folders.changeable(place);
        told(() -> {
            git.switchBranch(folder, name);
            return null;
        });
        return told(() -> git.status(folder, author()));
    }

    /** A file put back as its last commit has it, at the version the reader was shown; the status it leaves. */
    public WorkingTreeStatus restore(GitPlace place, String path, String version) {
        Path folder = folders.changeable(place);
        told(() -> {
            git.restore(folder, path, version);
            return null;
        });
        return told(() -> git.status(folder, author()));
    }

    /** The signed-in person as the host names them; {@code null} leaves it to the repository. */
    private CommitAuthor author() {
        return authority.actor().flatMap(actor -> authors.flatMap(known -> known.authorOf(actor))).orElse(null);
    }

    /** What Git refuses, as the engine's problem of the same kind; anything else as it is. */
    private static <T> T told(Supplier<T> work) {
        try {
            return work.get();
        } catch (IllegalArgumentException refusal) {
            throw CadProblem.invalid(refusal.getMessage());
        } catch (LocalGitException refusal) {
            throw switch (refusal.reason()) {
                case CONFLICT -> CadProblem.conflict(refusal.getMessage());
                case NOT_FOUND -> CadProblem.notFound(refusal.getMessage());
                case NOT_A_REPOSITORY, INVALID_PATH, TOO_LARGE, IDENTITY_REQUIRED, UNSUPPORTED_STATE -> CadProblem.invalid(refusal.getMessage());
                case STORAGE -> refusal;
            };
        }
    }
}
