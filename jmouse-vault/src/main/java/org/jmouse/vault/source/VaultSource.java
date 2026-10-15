package org.jmouse.vault.source;

import org.jmouse.vault.SecretEntry;
import org.jmouse.vault.SecretName;
import org.jmouse.vault.SecretValue;

import java.util.List;
import java.util.Optional;

/**
 * 📦 One place secrets come from. A {@link org.jmouse.vault.Vault} asks several, in order.
 *
 * <p>Every source answers for itself and knows nothing of the others; precedence is the chain's business.
 */
public interface VaultSource {

    /** 🏷️ The name the chain is ordered by, and the one an administrator sees beside each secret. */
    String name();

    Optional<SecretValue> find(SecretName name);

    /** 📋 Every secret this source holds, without values. */
    List<SecretEntry> list();

    /** Where this source would read a name from, e.g. the environment variable — even when it is absent. */
    default String locate(SecretName name) {
        return null;
    }
}
