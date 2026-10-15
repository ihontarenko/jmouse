package org.jmouse.git.hosting;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Which repository an address names on a hosting.
 *
 * @param host  the hosting's web host, lowercase, with its port when that is not the default
 * @param owner the owner on GitHub; the full namespace on GitLab, which may hold subgroups ({@code group/sub})
 * @param name  the repository's own name, without {@code .git}
 */
public record RepositoryReference(String host, String owner, String name) {

    private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9._-]{1,255}");

    public RepositoryReference {
        if (host == null || host.isBlank() || !host.equals(host.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("A repository reference needs a lowercase host.");
        }
        if (owner == null || owner.isEmpty()) {
            throw new IllegalArgumentException("A repository reference needs an owner.");
        }
        for (String segment : owner.split("/", -1)) {
            requireSegment(segment);
        }
        requireSegment(name);
    }

    /** {@code owner/name}, the way both hostings print a repository. */
    public String path() {
        return owner + "/" + name;
    }

    /** Whether a single path segment is one a hosting can name: letters, digits, dot, underscore and dash. */
    static boolean isSegment(String segment) {
        return segment != null && SEGMENT.matcher(segment).matches() && !segment.equals(".") && !segment.equals("..");
    }

    private static void requireSegment(String segment) {
        if (!isSegment(segment)) {
            throw new IllegalArgumentException("A repository owner or name holds only letters, digits, '.', '_' and '-'.");
        }
    }
}
