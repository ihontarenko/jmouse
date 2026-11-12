package org.jmouse.git.local;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A repository a program keeps its own files in: each change committed as it is made, the paths named and nothing else,
 * and each file's history read back — through the moves its commits say —, with the text it had in any commit.
 */
final class PathCommits {

    /** The trailer a move is said by: {@code Moved: passive/resistor.jm -> resistors/resistor.jm}. */
    private static final String MOVED = "Moved";
    private static final Pattern MOVE = Pattern.compile("^" + MOVED + ": (.+) -> (.+)$", Pattern.MULTILINE);

    /** Writes one commit at a time: two changes never interleave in one repository's index. */
    synchronized CommitResult commit(Path directory, PathsCommit change) {
        try (Repository repository = RepositoryAccess.require(directory)) {
            Path root = repository.getWorkTree().toPath().toRealPath();
            Git git = Git.wrap(repository);
            RepositoryAccess.withoutHooks(repository);
            for (String path : change.paths()) {
                if (Files.exists(root.resolve(path))) {
                    git.add().addFilepattern(path).call();
                } else {
                    git.rm().setCached(true).addFilepattern(path).call();
                }
            }
            PersonIdent person = change.when() == null
                    ? new PersonIdent(change.author().name(), change.author().email())
                    : new PersonIdent(change.author().name(), change.author().email(), change.when(), ZoneOffset.UTC);
            RevCommit made = git.commit().setMessage(messageOf(change)).setAuthor(person).setCommitter(person)
                    .setNoVerify(true).setSign(false).call();
            return new CommitResult(made.name(), true);
        } catch (IOException | GitAPIException exception) {
            throw JGitLocalGit.storage("The change could not be committed.");
        }
    }

    /** What a commit says, its moves as trailers after it. */
    private static String messageOf(PathsCommit change) {
        StringBuilder message = new StringBuilder(change.message().strip());
        if (!change.moves().isEmpty()) {
            message.append(message.indexOf("\n\n") < 0 ? "\n\n" : "\n");
            change.moves().forEach(move -> message.append(MOVED).append(": ").append(move.from()).append(" -> ")
                    .append(move.to()).append('\n'));
        }
        return message.toString().strip();
    }

    /**
     * Every commit that changed a file, the oldest first. Where a commit says the file moved to its path, the history goes on
     * at the path it came from, before that commit.
     */
    List<PathRevision> history(Path directory, String path) {
        return histories(directory, path::equals).getOrDefault(path, List.of());
    }

    /**
     * The history of every file at the head that `wanted` takes, by its path there, each the oldest commit first — read in
     * one walk of the history, however many files there are. A move a commit says (`Moved: from -> to`) carries the
     * file's history over to its new path; a file taken away ends its history, and one put back where it stood starts a
     * new one. Each commit's changes are read against its first parent.
     */
    Map<String, List<PathRevision>> histories(Path directory, Predicate<String> wanted) {
        try (Repository repository = RepositoryAccess.require(directory); RevWalk walk = new RevWalk(repository);
             DiffFormatter changes = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            ObjectId head = repository.resolve(Constants.HEAD);
            if (head == null) {
                return Map.of();
            }
            changes.setRepository(repository);
            changes.setDetectRenames(false);
            walk.sort(RevSort.TOPO);
            walk.sort(RevSort.REVERSE, true);
            walk.markStart(walk.parseCommit(head));
            Map<String, List<PathRevision>> living = new HashMap<>();
            for (RevCommit commit : walk) {
                String message = commit.getFullMessage();
                Set<String> movedAway = new HashSet<>();
                Matcher move = MOVE.matcher(message);
                while (move.find()) {
                    String from = move.group(1).strip();
                    String to = move.group(2).strip();
                    List<PathRevision> moved = living.remove(from);
                    if (moved != null) {
                        living.put(to, moved);
                        movedAway.add(from);
                    }
                }
                RevTree before = commit.getParentCount() == 0 ? null : walk.parseCommit(commit.getParent(0)).getTree();
                PersonIdent author = commit.getAuthorIdent();
                for (DiffEntry change : changes.scan(before, commit.getTree())) {
                    if (change.getChangeType() == DiffEntry.ChangeType.DELETE) {
                        if (!movedAway.contains(change.getOldPath())) {
                            living.remove(change.getOldPath());
                        }
                        continue;
                    }
                    String path = change.getNewPath();
                    living.computeIfAbsent(path, ignored -> new ArrayList<>()).add(new PathRevision(commit.name(), path, author.getName(),
                            author.getEmailAddress(), author.getWhenAsInstant(), message, change.getNewId().toObjectId().name()));
                }
            }
            Map<String, List<PathRevision>> histories = new LinkedHashMap<>();
            living.entrySet().stream().filter(entry -> wanted.test(entry.getKey())).sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> histories.put(entry.getKey(), List.copyOf(entry.getValue())));
            return histories;
        } catch (IOException exception) {
            throw JGitLocalGit.storage("The files' histories could not be read.");
        }
    }

    /** The text one version of a file had, by the identifier of its content (`PathRevision.content`). */
    String text(Path directory, String content) {
        try (Repository repository = RepositoryAccess.require(directory)) {
            return new String(repository.open(ObjectId.fromString(content)).getBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw JGitLocalGit.storage("The file's text could not be read.");
        }
    }

    /** Where a commit says it moved the file now at {@code path} from; empty when it moved nothing there. */
    private static Optional<String> movedFrom(String message, String path) {
        Matcher move = MOVE.matcher(message);
        while (move.find()) {
            if (move.group(2).strip().equals(path)) {
                return Optional.of(move.group(1).strip());
            }
        }
        return Optional.empty();
    }

    /** The text a file had in a commit; empty when it was not there. */
    Optional<String> textAt(Path directory, String commit, String path) {
        try (Repository repository = RepositoryAccess.require(directory); RevWalk walk = new RevWalk(repository)) {
            RevCommit found = walk.parseCommit(repository.resolve(commit));
            try (TreeWalk tree = TreeWalk.forPath(repository, path, found.getTree())) {
                if (tree == null) {
                    return Optional.empty();
                }
                return Optional.of(new String(repository.open(tree.getObjectId(0)).getBytes(), StandardCharsets.UTF_8));
            }
        } catch (IOException exception) {
            throw JGitLocalGit.storage("The file could not be read from the commit.");
        }
    }
}
