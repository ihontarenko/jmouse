package org.jmouse.vault.credential;

import org.jmouse.vault.SecretName;

import java.util.Map;
import java.util.Set;

/** ✍️ The credential source an application can change — the database. Transactions belong to the caller. */
public interface WritableCredentialSource extends CredentialSource {

    CredentialDefinition save(SecretName name, String kind, String description,
                              Map<String, String> plainFields, Set<String> secretFields, String actor);

    boolean remove(SecretName name);
}
