package org.jmouse.git.local;

/**
 * Who a commit is written by, when the application knows the person — a server where several people write one folder —;
 * without one, the repository's own {@code user.name} and {@code user.email} are, as a desktop editor's are.
 */
public record CommitAuthor(String name, String email) {

    public CommitAuthor {
        if (name == null || name.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException("A commit's author needs a name and an e-mail address.");
        }
    }
}
