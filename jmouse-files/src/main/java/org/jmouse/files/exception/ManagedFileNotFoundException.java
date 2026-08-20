package org.jmouse.files.exception;

/**
 * 🔍 No managed file under that identifier.
 *
 * <p>⚠️ Distinct from the storage layer's {@code ObjectNotFoundException}, and the difference is
 * worth keeping: this one means the <em>row</em> is missing, that one means the row exists and the
 * bytes it points at do not. They arrive at a caller as the same 404, but they mean opposite things
 * to whoever is asked to fix it.</p>
 */
public class ManagedFileNotFoundException extends RuntimeException {

    /**
     * 🏗️ Report a missing file.
     *
     * @param identifier the identifier that resolved to nothing
     */
    public ManagedFileNotFoundException(String identifier) {
        super("No such file: %s".formatted(identifier));
    }
}
