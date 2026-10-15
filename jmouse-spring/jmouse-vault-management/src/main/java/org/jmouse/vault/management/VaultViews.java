package org.jmouse.vault.management;

import org.jmouse.vault.SecretEntry;
import org.jmouse.vault.SecretStatus;
import org.jmouse.vault.credential.CredentialDefinition;
import org.jmouse.vault.credential.CredentialStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 📤 What the management API answers with. ⚠️ No view here carries a value, except {@link Revealed}. */
public final class VaultViews {

    private VaultViews() {
    }

    public record Entry(String source, String location, String fingerprint, String description,
                        Instant updatedAt, String updatedBy) {

        static Entry of(SecretEntry entry) {
            return entry == null ? null : new Entry(entry.source(), entry.location(), entry.fingerprint(),
                    entry.description(), entry.updatedAt(), entry.updatedBy());
        }
    }

    /** @param credential the credential this secret is a field of, or null */
    public record Secret(String name, Entry resolved, List<Entry> shadowed, boolean editable, String credential) {

        static Secret of(SecretStatus status, String credential) {
            return new Secret(status.name().value(), Entry.of(status.resolved()),
                    status.shadowed().stream().map(Entry::of).toList(), status.editable(), credential);
        }
    }

    /** @param secrets field → where its secret resolves from, or null when nothing holds it */
    public record Credential(String name, String kind, String description, String source, boolean editable,
                             Map<String, String> fields, Map<String, Entry> secrets, Set<String> secretFields,
                             List<String> missing, Instant updatedAt, String updatedBy) {

        static Credential of(CredentialStatus status) {
            CredentialDefinition definition = status.definition();
            Map<String, Entry>   secrets    = new LinkedHashMap<>();

            status.secrets().forEach((field, secret) -> secrets.put(field, secret == null ? null : Entry.of(secret.resolved())));

            return new Credential(definition.name().value(), definition.kind(), definition.description(),
                    definition.source(), status.editable(), definition.plainFields(), secrets,
                    definition.secretFields(), status.missing(), definition.updatedAt(), definition.updatedBy());
        }
    }

    /** @param writableSource null when nothing can be changed from the application */
    public record Overview(List<String> sources, String writableSource, String environmentExample) {
    }

    public record Revealed(String name, String value, String source) {
    }
}
