package org.jmouse.git.local;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A commit of exactly these paths as the working tree holds them: a path that is there is added or changed, one that is
 * gone is removed. What a program that keeps its own files in a repository writes, one change at a time, with no review
 * of a person in between. A file the program moved is said to have moved — never guessed from its content —, so its
 * history goes on through the move.
 *
 * @param message what the commit says; trailers written in it are kept as written
 * @param author  who made the change
 * @param when    when it was made — a history written again keeps its own times —; null for now
 * @param paths   the paths, relative to the repository's root, separated by {@code /}
 * @param moves   the files moved, each from one path to another; both are committed
 */
public record PathsCommit(String message, CommitAuthor author, Instant when, List<String> paths, List<Move> moves) {

    /** One file moved: where it stood, where it stands now. */
    public record Move(String from, String to) {

        public Move {
            Objects.requireNonNull(from, "A move says where the file stood.");
            Objects.requireNonNull(to, "A move says where the file stands.");
        }
    }

    public PathsCommit {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("A commit says what it is.");
        }
        Objects.requireNonNull(author, "A commit has an author.");
        moves = moves == null ? List.of() : List.copyOf(moves);
        List<String> named = new ArrayList<>(paths == null ? List.of() : paths);
        for (Move move : moves) {
            if (!named.contains(move.from())) {
                named.add(move.from());
            }
            if (!named.contains(move.to())) {
                named.add(move.to());
            }
        }
        if (named.isEmpty()) {
            throw new IllegalArgumentException("A commit of paths names at least one.");
        }
        paths = List.copyOf(named);
    }

    /** A commit of paths that moves nothing. */
    public PathsCommit(String message, CommitAuthor author, Instant when, List<String> paths) {
        this(message, author, when, paths, List.of());
    }
}
