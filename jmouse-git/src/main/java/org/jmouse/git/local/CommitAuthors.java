package org.jmouse.git.local;

import java.util.Optional;

/**
 * Who a commit is by — the host's seam: an application whose people have names and e-mail addresses says them, so a
 * repository several people write keeps who wrote what. Without it, or for a person with no address, a commit is by the
 * repository's own identity, as a desktop editor's is.
 */
@FunctionalInterface
public interface CommitAuthors {

    /** A host that knows nobody's name: every commit is by the repository's own identity. */
    CommitAuthors NONE = actor -> Optional.empty();

    /** The author of a commit made by this actor; empty to leave it to the repository. */
    Optional<CommitAuthor> authorOf(String actor);
}
