package org.jmouse.git.local;

import org.eclipse.jgit.lib.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Working-tree path boundaries and reviewed content identities. Git metadata is never selectable.
 */
final class WorkingTreeFiles {
    static final String DELETED_VERSION = "deleted";
    static final String UNSUPPORTED_VERSION = "unsupported";
    private static final String OVERSIZED_VERSION = "oversized";
    private final Path root;
    private final LocalGitLimits limits;

    WorkingTreeFiles(Path root, LocalGitLimits limits) {
        this.root = root;
        this.limits = limits;
    }

    Path path(String source) throws IOException {
        Path relative = Path.of(source);
        Path target = root.resolve(relative).normalize();
        if (relative.isAbsolute() || !target.startsWith(root) || target.equals(root)) {
            throw new LocalGitException(
                    LocalGitException.Reason.INVALID_PATH,
                    "The selected path is outside the working tree.");
        }
        if (!root.relativize(target).toString().replace('\\', '/').equals(source)) {
            throw new LocalGitException(
                    LocalGitException.Reason.INVALID_PATH,
                    "Use a canonical repository-relative Git path.");
        }
        for (Path component : relative) {
            if (component.toString().equalsIgnoreCase(Constants.DOT_GIT)) {
                throw new LocalGitException(
                        LocalGitException.Reason.INVALID_PATH, "Git metadata cannot be selected.");
            }
        }
        Path existing = target;
        while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
        }
        if (existing == null
                || !existing.toRealPath().startsWith(root)
                || Files.isSymbolicLink(target)) {
            throw new LocalGitException(
                    LocalGitException.Reason.INVALID_PATH,
                    "The selected path escapes the working tree through a symbolic link.");
        }
        return target;
    }

    byte[] read(String source) throws IOException {
        Path file = path(source);
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new LocalGitException(
                    LocalGitException.Reason.INVALID_PATH, "Only regular files can be committed.");
        }
        requireSize(Files.size(file));
        byte[] bytes = Files.readAllBytes(file);
        requireSize(bytes.length);
        return bytes;
    }

    String version(String source) throws IOException {
        Path file = path(source);
        if (Files.isSymbolicLink(file) || (Files.exists(file) && !Files.isRegularFile(file))) {
            return UNSUPPORTED_VERSION;
        }
        if (Files.exists(file) && Files.size(file) > limits.maximumFileBytes()) {
            return OVERSIZED_VERSION;
        }
        return version(read(source));
    }

    static String version(byte[] bytes) {
        if (bytes == null) {
            return DELETED_VERSION;
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by Java.", exception);
        }
    }

    static boolean selectable(String version) {
        return !UNSUPPORTED_VERSION.equals(version) && !OVERSIZED_VERSION.equals(version);
    }

    void requireSize(long bytes) {
        if (bytes > limits.maximumFileBytes()) {
            throw new LocalGitException(
                    LocalGitException.Reason.TOO_LARGE,
                    "The selected file exceeds the interactive Git size limit.");
        }
    }
}
