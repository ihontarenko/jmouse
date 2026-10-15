package org.jmouse.vault.credential;

import org.jmouse.vault.SecretStatus;

import java.util.List;
import java.util.Map;

/**
 * 🧭 A credential as an administration screen shows it.
 *
 * @param secrets field → where its secret resolves from, or null when no source holds it
 * @param missing required fields with no value — a credential that will fail at its first use
 * @param editable whether the definition sits in the writable source
 */
public record CredentialStatus(
        CredentialDefinition definition, Map<String, SecretStatus> secrets, List<String> missing, boolean editable) {
}
