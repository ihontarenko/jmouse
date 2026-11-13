package org.jmouse.plugin;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 📁 The server's folders, as whoever sets a {@code FOLDERS} parameter is shown them to choose from instead of
 * typing a path: only folders, never a file's name. Who may ask is the application's to decide.
 */
public final class ServerFolders {

    /** A folder lists at most this many: a person chooses among them, and a larger one is typed through. */
    private static final int MOST_LISTED = 1000;

    private ServerFolders() {
    }

    /**
     * A folder as it is listed.
     *
     * @param name  its last part; its whole path for a drive
     * @param path  its whole path
     */
    public record Folder(String name, String path) {
    }

    /**
     * What a folder holds.
     *
     * @param path      its whole path
     * @param parent    the folder it is in; null at the top of a drive
     * @param folders   the folders in it, by name
     * @param truncated whether it holds more than are listed
     */
    public record Listing(String path, String parent, List<Folder> folders, boolean truncated) {
    }

    /** Where browsing starts: the drives of the server, those it can read. */
    public static List<Folder> roots() {
        return Arrays.stream(File.listRoots()).filter(File::isDirectory)
                .map(root -> new Folder(root.getPath(), root.getPath())).toList();
    }

    /**
     * What a folder holds.
     *
     * @throws IllegalArgumentException when the path is not whole, or names nothing that is a folder
     * @throws UncheckedIOException     when the server may not open it
     */
    public static Listing listing(String wholePath) {
        Path folder = Path.of(wholePath.strip()).normalize();
        if (!folder.isAbsolute() || !Files.isDirectory(folder)) {
            throw new IllegalArgumentException("'" + wholePath + "' is no folder of the server.");
        }
        try (Stream<Path> entries = Files.list(folder)) {
            List<Folder> found = entries.filter(Files::isDirectory)
                    .map(entry -> new Folder(entry.getFileName().toString(), entry.toString()))
                    .sorted(Comparator.comparing(Folder::name, String.CASE_INSENSITIVE_ORDER))
                    .limit(MOST_LISTED + 1L).toList();
            Path parent = folder.getParent();
            return new Listing(folder.toString(), parent == null ? null : parent.toString(),
                               found.subList(0, Math.min(found.size(), MOST_LISTED)), found.size() > MOST_LISTED);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The folder " + folder + " cannot be read", unreadable);
        }
    }
}
