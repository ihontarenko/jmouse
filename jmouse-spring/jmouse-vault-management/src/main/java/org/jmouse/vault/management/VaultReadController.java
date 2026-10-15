package org.jmouse.vault.management;

import org.jmouse.vault.SecretName;
import org.jmouse.vault.Vault;
import org.jmouse.vault.audit.VaultAudit;
import org.jmouse.vault.audit.VaultAuditEvent;
import org.jmouse.vault.credential.CredentialDefinition;
import org.jmouse.vault.credential.CredentialKind;
import org.jmouse.vault.credential.CredentialStatus;
import org.jmouse.vault.credential.Credentials;
import org.jmouse.vault.source.VaultSource;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 👀 Everything an administrator may see without seeing a value: which secrets exist, where each resolves
 * from and what it shadows, the credentials, the kinds, and the audit trail.
 *
 * <p>Every read is in this one class so a product gates them with one rule — {@code vault:read}.
 */
@RestController
@RequestMapping(ManagementRoutes.PREFIX)
public class VaultReadController {

    private static final SecretName EXAMPLE_NAME = SecretName.of("example/api-key");

    private final Vault                 vault;
    private final Credentials           credentials;
    private final VaultAudit            audit;
    private final TransactionOperations transactions;

    public VaultReadController(Vault vault, Credentials credentials, VaultAudit audit, TransactionOperations transactions) {
        this.vault        = vault;
        this.credentials  = credentials;
        this.audit        = audit;
        this.transactions = transactions;
    }

    /** 🧭 The chain, which source takes writes, and how an environment variable is spelled here. */
    @GetMapping
    public VaultViews.Overview overview() {
        String example = vault.lookedAt(EXAMPLE_NAME).stream()
                .filter(location -> location.startsWith("environment ("))
                .findFirst()
                .orElse(null);

        return new VaultViews.Overview(vault.sourceNames(), vault.writable().map(VaultSource::name).orElse(null), example);
    }

    @GetMapping("/secrets")
    public List<VaultViews.Secret> secrets() {
        return transactions.execute(status -> {
            Map<SecretName, String> owners = new HashMap<>();

            for (CredentialStatus credential : credentials.list()) {
                CredentialDefinition definition = credential.definition();

                definition.secretFields().forEach(field -> owners.put(definition.secretName(field), definition.name().value()));
            }

            return vault.list().stream()
                    .map(secret -> VaultViews.Secret.of(secret, owners.get(secret.name())))
                    .toList();
        });
    }

    @GetMapping("/secrets/entry")
    public ResponseEntity<VaultViews.Secret> secret(@RequestParam("name") String name) {
        SecretName secretName = SecretName.of(name);

        return ResponseEntity.of(transactions.execute(status -> vault.status(secretName))
                .map(secret -> VaultViews.Secret.of(secret, null)));
    }

    /** 🧭 Where a secret of this name would be read from, in order — useful for one nobody has set yet. */
    @GetMapping("/secrets/locations")
    public List<String> locations(@RequestParam("name") String name) {
        return vault.lookedAt(SecretName.of(name));
    }

    @GetMapping("/credentials")
    public List<VaultViews.Credential> credentials() {
        return transactions.execute(status -> credentials.list().stream().map(VaultViews.Credential::of).toList());
    }

    @GetMapping("/credentials/entry")
    public ResponseEntity<VaultViews.Credential> credential(@RequestParam("name") String name) {
        SecretName credentialName = SecretName.of(name);

        return ResponseEntity.of(transactions.execute(status -> credentials.status(credentialName))
                .map(VaultViews.Credential::of));
    }

    @GetMapping("/kinds")
    public List<CredentialKind> kinds() {
        return credentials.kinds().list();
    }

    @GetMapping("/audit")
    public List<VaultAuditEvent> audit(@RequestParam(name = "limit", defaultValue = "0") int limit) {
        return transactions.execute(status -> audit.recent(ManagementRoutes.boundedLimit(limit)));
    }
}
