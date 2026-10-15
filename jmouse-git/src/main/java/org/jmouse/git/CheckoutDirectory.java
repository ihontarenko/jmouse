package org.jmouse.git;

import org.eclipse.jgit.util.FileUtils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/** File-system rules for the directory a caller hands over: emptiness, size on disk, and clearing it again. */
final class CheckoutDirectory {

    private CheckoutDirectory() {
    }

    /** Whether the path does not exist yet, or is a directory with nothing in it. */
    static boolean isAbsentOrEmpty(Path directory) {
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            return true;
        }
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        try (Stream<Path> children = Files.list(directory)) {
            return children.findAny().isEmpty();
        } catch (IOException failure) {
            throw new GitException(GitException.Reason.TRANSPORT);
        }
    }

    /** Creates the directory when it is absent; refuses one that already holds anything. */
    static void requireEmpty(Path directory) {
        if (!isAbsentOrEmpty(directory)) {
            throw new GitException(GitException.Reason.DIRECTORY_NOT_EMPTY);
        }
        try {
            Files.createDirectories(directory);
        } catch (IOException failure) {
            throw new GitException(GitException.Reason.TRANSPORT);
        }
    }

    /**
     * The bytes regular files under the directory occupy, symbolic links not followed. Files that vanish while
     * being counted — JGit renames its temporary pack files as it works — are skipped rather than failing the count.
     */
    static long bytesOf(Path directory) {
        AtomicLong total = new AtomicLong();
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    if (attributes.isRegularFile()) {
                        total.addAndGet(attributes.size());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException failure) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException failure) {
            throw new GitException(GitException.Reason.TRANSPORT);
        }
        return total.get();
    }

    /**
     * Removes everything inside the directory and keeps the directory itself, which belongs to the caller.
     * Best effort: this runs while another failure is already being reported, so it never replaces that failure.
     */
    static void deleteContents(Path directory) {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        List<Path> children;
        try (Stream<Path> listing = Files.list(directory)) {
            children = listing.toList();
        } catch (IOException failure) {
            return;
        }
        for (Path child : children) {
            try {
                // JGit's own deletion clears the read-only flag Windows leaves on pack files.
                FileUtils.delete(child.toFile(), FileUtils.RECURSIVE | FileUtils.RETRY | FileUtils.SKIP_MISSING);
            } catch (IOException failure) {
                // Leave what cannot be removed; the original refusal is what the caller needs to see.
            }
        }
    }
}
