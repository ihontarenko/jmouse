package org.jmouse.vault;

import java.util.List;

/**
 * 🧭 Where a secret resolves from, and every other source that also holds it but loses.
 *
 * @param resolved the entry that answers
 * @param shadowed entries of the same name in later sources — present, never used; a stale {@code .env}
 *                 line under a database value is exactly what this is for
 * @param editable whether the answering source can be changed from the application
 */
public record SecretStatus(SecretEntry resolved, List<SecretEntry> shadowed, boolean editable) {

    public SecretName name() {
        return resolved.name();
    }
}
