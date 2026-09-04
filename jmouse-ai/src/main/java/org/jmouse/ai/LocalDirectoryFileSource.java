package org.jmouse.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A file already on the machine running this application, under one named directory.
 *
 * <h2>⚠️ This is the server reading its own filesystem on a caller's word</h2>
 *
 * <p>Which makes it a capability rather than a convenience, and it does not exist until an application
 * constructs one. There is no "unset" state here to get wrong: an installation that has not decided
 * about this simply never adds this source, and the argument is then absent from every schema rather
 * than present and refusing.
 *
 * <p>⚠️ <strong>It reads a LOCAL DIRECTORY and its name says so.</strong> It is not <em>the</em> way
 * files arrive, and an installation whose files live in an object store wants a source of its own
 * rather than a wider version of this one — see {@link ToolFileSource}.
 *
 * <h2>⚠️ Both paths are normalised before they are compared</h2>
 *
 * <p>Comparing the written strings would let {@code root/../../etc} start with {@code root}. Resolving
 * and normalising both sides first is what makes {@code ../} climb out of nothing.
 */
public final class LocalDirectoryFileSource implements ToolFileSource {

    /** The argument name, published so a tool's own wording cannot drift from the schema's. */
    public static final String ARGUMENT = "path";

    private final Path root;

    /**
     * @param root the only directory a path may lie under
     * @throws IllegalArgumentException when no directory is named — an unusable source is refused at
     *                                  construction rather than at the first call, so a misconfigured
     *                                  installation fails where somebody is looking
     */
    public LocalDirectoryFileSource(String root) {
        if (root == null || root.isBlank()) {
            throw new IllegalArgumentException(
                    "A local file source needs the one directory it may read from. Leave the source out "
                    + "altogether to refuse local paths.");
        }
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public String argument() {
        return ARGUMENT;
    }

    @Override
    public String description() {
        return "A file already on this server, given as its path. Only files under the one directory "
             + "this installation reads uploads from are allowed. Use it instead of sending bytes when "
             + "the file is large and you know it is there.";
    }

    @Override
    public boolean carriedBy(ToolInvocation invocation) {
        return invocation.optionalString(ARGUMENT).filter(sent -> !sent.isBlank()).isPresent();
    }

    @Override
    public byte[] read(ToolInvocation invocation) {
        Path target = Path.of(invocation.requiredString(ARGUMENT).trim()).toAbsolutePath().normalize();

        if (!target.startsWith(root)) {
            throw new ToolRefusedException(RefusalReason.MISSING_PERMISSION,
                    "That path is outside the directory this installation reads uploads from (%s)."
                            .formatted(root));
        }

        if (!Files.isRegularFile(target)) {
            throw new ToolRefusedException(RefusalReason.NOTHING_TO_ACT_ON,
                    "There is no file at " + target + ".");
        }

        try {
            return Files.readAllBytes(target);
        } catch (IOException unreadable) {
            throw new ToolRefusedException(RefusalReason.NOTHING_TO_ACT_ON,
                    "That file could not be read: " + unreadable.getMessage());
        }
    }
}
