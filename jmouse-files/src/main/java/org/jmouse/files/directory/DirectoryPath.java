package org.jmouse.files.directory;

import org.jmouse.files.exception.DirectoryException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * 🧭 A place in the tree, written the way a person writes one: {@code innoventa/files}.
 *
 * <h3>⚠️ The first segment is the application, and it is load-bearing</h3>
 *
 * <p>Roots are named {@code <application>/<purpose>} — {@code innoventa/files},
 * {@code innoventa/avatars}, {@code tessera/attachments} — so that one installation's tree says out
 * loud whose files are in which part of it. That is a naming convention this type enforces the shape
 * of, not a permission boundary: what somebody may read is the authorization engine's answer, never
 * a prefix.</p>
 *
 * <h3>⚠️ Why a root's path is also the storage namespace, and a deeper directory's is not</h3>
 *
 * <p>The root path is handed to {@code StorageKeyRequest.namespace(...)}, so the bytes of everything
 * filed under {@code innoventa/files} sit under that prefix in the bucket. It follows that a root
 * cannot be renamed or moved — the keys already written would no longer agree with the tree.</p>
 *
 * <p>It also follows that directories <em>below</em> a root must NOT contribute to the key.
 * Otherwise moving a file between two folders would mean rewriting its bytes, and a move is meant to
 * be a row changing rather than an object being copied. So a deep path is organisation for people;
 * only its first two segments are addressing.</p>
 */
public record DirectoryPath(List<String> segments) {

    /** What separates one segment from the next, in configuration and in a log line alike. */
    public static final String SEPARATOR = "/";

    /** How many segments a root has: the application, then the purpose. */
    public static final int ROOT_DEPTH = 2;

    /** Longest one segment may be, matching the column that stores a directory's slug. */
    public static final int MAXIMUM_SEGMENT_LENGTH = 128;

    /**
     * 🏗️ Refuse a path that could not name a place.
     */
    public DirectoryPath {
        if (segments == null || segments.isEmpty()) {
            throw new DirectoryException("A directory path needs at least one segment.");
        }

        segments = List.copyOf(segments);

        for (String segment : segments) {
            if (segment.isBlank()) {
                throw new DirectoryException(
                    "A directory path may not contain an empty segment — '%s' does."
                        .formatted(String.join(SEPARATOR, segments)));
            }

            if (segment.length() > MAXIMUM_SEGMENT_LENGTH) {
                throw new DirectoryException(
                    "A directory name may be up to %d characters — '%s' is %d."
                        .formatted(MAXIMUM_SEGMENT_LENGTH, segment, segment.length()));
            }
        }
    }

    /**
     * 🧭 Read a written path.
     *
     * <p>Leading, trailing and repeated separators are dropped rather than refused: {@code /a//b/} and
     * {@code a/b} are the same place, and a configuration file that gained a trailing slash is not
     * worth failing a boot over.</p>
     *
     * @param path the path as written, e.g. {@code innoventa/files}
     * @return the parsed path
     */
    public static DirectoryPath of(String path) {
        if (path == null || path.isBlank()) {
            throw new DirectoryException("A directory path was expected, and none was given.");
        }

        return new DirectoryPath(
            Arrays.stream(path.split(SEPARATOR))
                .map(String::trim)
                .filter(segment -> !segment.isEmpty())
                .toList());
    }

    /**
     * 🌱 Read a root path, and refuse anything that is not one.
     *
     * <p>⚠️ Refused rather than accepted-and-truncated. A caller asking for the root
     * {@code innoventa/files/manuals} means something, and quietly giving them
     * {@code innoventa/files} would file everything one level up from where they said.</p>
     *
     * @param path the path as written
     * @return the parsed root path
     */
    public static DirectoryPath ofRoot(String path) {
        DirectoryPath parsed = of(path);

        if (parsed.segments().size() != ROOT_DEPTH) {
            throw new DirectoryException(
                "A root is named <application>/<purpose> — '%s' has %d segment(s), not %d."
                    .formatted(parsed, parsed.segments().size(), ROOT_DEPTH));
        }

        return parsed;
    }

    /**
     * 🏢 The application half of the path.
     *
     * @return the first segment
     */
    public String application() {
        return segments.getFirst();
    }

    /**
     * 🌱 The root this path sits under, which is its first two segments.
     *
     * @return the root path
     */
    public DirectoryPath root() {
        if (segments.size() < ROOT_DEPTH) {
            throw new DirectoryException(
                "'%s' is shallower than a root, so it has none.".formatted(this));
        }

        return new DirectoryPath(segments.subList(0, ROOT_DEPTH));
    }

    /**
     * ➕ This path with one more segment on the end.
     *
     * @param segment the segment to append
     * @return the longer path
     */
    public DirectoryPath resolve(String segment) {
        return new DirectoryPath(
            Stream.concat(segments.stream(), Stream.of(segment))
                .toList());
    }

    /**
     * 🗄️ The storage namespace this path addresses — its root, and only its root.
     *
     * @return the namespace, e.g. {@code innoventa/files}
     */
    public String namespace() {
        return root().toString();
    }

    @Override
    public String toString() {
        return String.join(SEPARATOR, segments);
    }
}
