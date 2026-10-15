package org.jmouse.vault.management;

import org.jmouse.vault.SecretName;
import org.jmouse.vault.SecretValue;
import org.jmouse.vault.Vault;
import org.jmouse.vault.audit.VaultAudit;
import org.jmouse.vault.audit.VaultAuditEvent;
import org.jmouse.vault.credential.Credentials;
import org.jmouse.vault.source.WritableVaultSource;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Clock;
import java.util.Map;
import java.util.Set;

/**
 * ✍️ Changes to the writable source — {@code vault:write}. Every change goes into the audit trail in the
 * same transaction, so a change and its record land together or not at all.
 *
 * <p>Answers carry no value: setting a secret answers with its status, fingerprint included, so a screen
 * can show that the new value landed without echoing it.
 */
@RestController
@RequestMapping(ManagementRoutes.PREFIX)
public class VaultWriteController {

    /** @param description null keeps the stored one */
    public record SecretDraft(String value, String description) {
    }

    public record DescriptionChange(String description) {
    }

    /**
     * @param fields       plain values
     * @param secrets      secret values; a blank one keeps what is stored
     * @param secretFields for an open-ended kind, every field that is secret
     */
    public record CredentialDraft(String kind, String description, Map<String, String> fields,
                                  Map<String, String> secrets, Set<String> secretFields) {
    }

    private final Vault                 vault;
    private final Credentials           credentials;
    private final VaultAudit            audit;
    private final TransactionOperations transactions;
    private final Clock                 clock;
    private final VaultActors           actors;

    public VaultWriteController(Vault vault, Credentials credentials, VaultAudit audit,
                                TransactionOperations transactions, Clock clock, VaultActors actors) {
        this.vault        = vault;
        this.credentials  = credentials;
        this.audit        = audit;
        this.transactions = transactions;
        this.clock        = clock;
        this.actors       = actors;
    }

    @PutMapping("/secrets")
    public VaultViews.Secret setSecret(@RequestParam("name") String name, @RequestBody SecretDraft draft, Principal principal) {
        SecretName          secretName = SecretName.of(name);
        SecretValue         value      = SecretValue.of(draft.value() == null ? "" : draft.value().strip());
        WritableVaultSource source     = requireWritable();

        return transactions.execute(status -> {
            source.put(secretName, value, blankToNull(draft.description()), actorOf(principal));
            record(principal, VaultAuditEvent.Action.SECRET_SET, secretName.value(), source.name());

            return VaultViews.Secret.of(vault.status(secretName).orElseThrow(), null);
        });
    }

    @PatchMapping("/secrets/description")
    public ResponseEntity<Void> describeSecret(@RequestParam("name") String name, @RequestBody DescriptionChange change,
                                               Principal principal) {
        SecretName          secretName = SecretName.of(name);
        WritableVaultSource source     = requireWritable();

        Boolean described = transactions.execute(status -> {
            boolean changed = source.describe(secretName, blankToNull(change.description()), actorOf(principal));

            if (changed) {
                record(principal, VaultAuditEvent.Action.SECRET_DESCRIBED, secretName.value(), source.name());
            }

            return changed;
        });

        return Boolean.TRUE.equals(described) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /** 🗑️ Removes it from the writable source only; a value of the same name elsewhere then answers. */
    @DeleteMapping("/secrets")
    public ResponseEntity<Void> removeSecret(@RequestParam("name") String name, Principal principal) {
        SecretName          secretName = SecretName.of(name);
        WritableVaultSource source     = requireWritable();

        Boolean removed = transactions.execute(status -> {
            boolean gone = source.remove(secretName);

            if (gone) {
                record(principal, VaultAuditEvent.Action.SECRET_REMOVED, secretName.value(), source.name());
            }

            return gone;
        });

        return Boolean.TRUE.equals(removed) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/credentials")
    public VaultViews.Credential saveCredential(@RequestParam("name") String name, @RequestBody CredentialDraft draft,
                                                Principal principal) {
        SecretName credentialName = SecretName.of(name);

        return transactions.execute(status -> {
            var saved = credentials.save(credentialName, draft.kind(), blankToNull(draft.description()),
                    draft.fields(), draft.secrets(), draft.secretFields(), actorOf(principal));

            record(principal, VaultAuditEvent.Action.CREDENTIAL_SAVED, credentialName.value(), saved.definition().source());

            return VaultViews.Credential.of(saved);
        });
    }

    @DeleteMapping("/credentials")
    public ResponseEntity<Void> removeCredential(@RequestParam("name") String name, Principal principal) {
        SecretName credentialName = SecretName.of(name);

        Boolean removed = transactions.execute(status -> {
            boolean gone = credentials.remove(credentialName);

            if (gone) {
                record(principal, VaultAuditEvent.Action.CREDENTIAL_REMOVED, credentialName.value(), null);
            }

            return gone;
        });

        return Boolean.TRUE.equals(removed) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private WritableVaultSource requireWritable() {
        return vault.writable().orElseThrow(() -> new IllegalStateException(
                "Secrets are not kept in the database here (sources: %s), so they cannot be changed from the application."
                        .formatted(vault.sourceNames())));
    }

    private void record(Principal principal, VaultAuditEvent.Action action, String subject, String source) {
        audit.record(new VaultAuditEvent(clock.instant(), actorOf(principal), action, subject, source));
    }

    private String actorOf(Principal principal) {
        return actors.nameOf(principal);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
