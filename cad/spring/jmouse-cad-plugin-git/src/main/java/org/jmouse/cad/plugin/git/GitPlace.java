package org.jmouse.cad.plugin.git;

import java.util.Locale;

/**
 * Whose history Git keeps: a folder of the explorer, or a project — told apart by its kind, as the routes say it
 * ({@code /mounts/{id}}, {@code /projects/{id}}), and named by its identifier there.
 *
 * @param kind what the place is
 * @param id   its identifier among those of its kind: a mount's, a project's address
 */
public record GitPlace(Kind kind, String id) {

    /** What a place whose history is kept is. */
    public enum Kind {
        /** A folder the explorer mounts. */
        MOUNTS,
        /** A project, its documents' own folder in the library folder. */
        PROJECTS;

        /** The kind a route's word names: {@code mounts}, {@code projects}. */
        public static Kind of(String word) {
            try {
                return valueOf(word.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException unknown) {
                throw new IllegalArgumentException("Git keeps the history of mounts and projects, not of '" + word + "'");
            }
        }
    }

    /** The place a route names. */
    public static GitPlace of(String kind, String id) {
        return new GitPlace(Kind.of(kind), id);
    }
}
