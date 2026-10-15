package org.jmouse.vault.source;

import org.jmouse.vault.SecretEntry;
import org.jmouse.vault.SecretName;
import org.jmouse.vault.SecretValue;

/**
 * ✍️ A source a running application can change — in practice the database, since an environment
 * variable or a property file cannot be rewritten without a restart and a filesystem.
 *
 * <p>Transactions belong to the caller.
 */
public interface WritableVaultSource extends VaultSource {

    /**
     * @param description null keeps the stored one
     * @param actor       who is changing it, for {@link SecretEntry#updatedBy()}
     */
    SecretEntry put(SecretName name, SecretValue value, String description, String actor);

    /** Changes the description alone; false when the secret is not stored here. */
    boolean describe(SecretName name, String description, String actor);

    /** @return false when it was not stored here */
    boolean remove(SecretName name);
}
