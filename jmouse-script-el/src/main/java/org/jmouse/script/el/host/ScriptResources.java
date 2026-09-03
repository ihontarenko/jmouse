package org.jmouse.script.el.host;

import java.util.Map;

/**
 * How {@code include} finds a file.
 *
 * <h2>⚠️ The library never touches a file system</h2>
 *
 * <p>A script comes from wherever its host keeps scripts, and for the hosts this dialect is written for
 * that is very often not a disk: a row in a table, a blob in object storage, a level editor's memory.
 * A loader that reached for {@code Files.readString} would work for exactly one of them and be the
 * reason the others each wrote their own.</p>
 *
 * <p>So this is one method, and the host implements it. Path resolution — relative to what, which
 * directory, which workspace — is the host's too, because only the host knows what a path <em>means</em>
 * in its own storage.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@FunctionalInterface
public interface ScriptResources {

    /**
     * Returns the text of an included file.
     *
     * @param path exactly what was written between the quotes of the {@code include}
     * @return the file's text, or {@code null} when there is no such file
     */
    String read(String path);

    /**
     * Resources for a host whose scripts never compose.
     *
     * <p>⚠️ Not an error to configure: a document with no {@code include} never asks. One that does gets
     * a refusal naming the path, which is a better answer than a loader that quietly skipped it.</p>
     *
     * @return resources that hold nothing
     */
    static ScriptResources none() {
        return path -> null;
    }

    /**
     * Resources backed by a map, for a host that already has every script in memory — and for tests.
     *
     * @param files path to text
     * @return resources over that map
     */
    static ScriptResources of(Map<String, String> files) {
        Map<String, String> copy = Map.copyOf(files);

        return copy::get;
    }
}
