package org.jmouse.vault.management;

import org.jmouse.vault.MissingSecretException;
import org.jmouse.vault.SecretName;
import org.jmouse.vault.Vault;
import org.jmouse.vault.audit.VaultAudit;
import org.jmouse.vault.audit.VaultAuditEvent;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Clock;

/**
 * 👁️ Shows a secret's value — the one route that does, alone in its class so a product gates it with its
 * own permission ({@code vault:reveal}) rather than with whatever lets somebody list names.
 *
 * <p>⚠️ A POST, so no prefetch, crawler or history entry triggers it; answered {@code no-store}; and
 * recorded before it is answered — a reveal the audit could not record is not made.
 */
@RestController
@RequestMapping(ManagementRoutes.PREFIX)
public class VaultRevealController {

    private final Vault                 vault;
    private final VaultAudit            audit;
    private final TransactionOperations transactions;
    private final Clock                 clock;
    private final VaultActors           actors;

    public VaultRevealController(Vault vault, VaultAudit audit, TransactionOperations transactions, Clock clock,
                                 VaultActors actors) {
        this.vault        = vault;
        this.audit        = audit;
        this.transactions = transactions;
        this.clock        = clock;
        this.actors       = actors;
    }

    @PostMapping("/secrets/reveal")
    public ResponseEntity<VaultViews.Revealed> reveal(@RequestParam("name") String name, Principal principal) {
        SecretName secretName = SecretName.of(name);

        VaultViews.Revealed revealed = transactions.execute(status -> {
            Vault.Resolved resolved = vault.resolve(secretName)
                    .orElseThrow(() -> new MissingSecretException(secretName, vault.lookedAt(secretName)));

            audit.record(new VaultAuditEvent(clock.instant(), actors.nameOf(principal),
                    VaultAuditEvent.Action.SECRET_REVEALED, secretName.value(), resolved.source()));

            return new VaultViews.Revealed(secretName.value(), resolved.value().reveal(), resolved.source());
        });

        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(revealed);
    }
}
