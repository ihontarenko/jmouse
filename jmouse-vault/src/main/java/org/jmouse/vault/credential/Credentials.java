package org.jmouse.vault.credential;

import org.jmouse.vault.SecretName;
import org.jmouse.vault.SecretStatus;
import org.jmouse.vault.SecretValue;
import org.jmouse.vault.Vault;
import org.jmouse.vault.source.WritableVaultSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 🗂️ Structured credentials: a provider's URL and its key, a client's ID and its secret — a named set of
 * fields of a declared {@link CredentialKind}, where the secret fields live in the {@link Vault} and the
 * plain ones beside the definition.
 *
 * <p>Whatever consumes a credential keeps only its name ({@code ai/openai}) and asks here at use.
 */
public final class Credentials {

    private final List<CredentialSource> sources;
    private final Vault                  vault;
    private final CredentialKinds        kinds;

    public Credentials(List<? extends CredentialSource> sources, Vault vault, CredentialKinds kinds) {
        this.sources = List.copyOf(sources);
        this.vault   = Objects.requireNonNull(vault, "vault");
        this.kinds   = Objects.requireNonNull(kinds, "kinds");
    }

    public Optional<ResolvedCredential> find(SecretName name) {
        return definition(name).map(definition -> new ResolvedCredential(definition, vault));
    }

    public ResolvedCredential require(SecretName name) {
        return find(name).orElseThrow(() -> new IllegalArgumentException(
                "No credential '%s' is configured (looked at %s).".formatted(name, sources.stream().map(CredentialSource::name).toList())));
    }

    public Optional<CredentialDefinition> definition(SecretName name) {
        for (CredentialSource source : sources) {
            Optional<CredentialDefinition> definition = source.find(name);

            if (definition.isPresent()) {
                return definition;
            }
        }

        return Optional.empty();
    }

    public List<CredentialStatus> list() {
        Map<SecretName, CredentialDefinition> first = new LinkedHashMap<>();

        for (CredentialSource source : sources) {
            source.list().forEach(definition -> first.putIfAbsent(definition.name(), definition));
        }

        Map<SecretName, SecretStatus> secrets = new LinkedHashMap<>();

        vault.list().forEach(status -> secrets.put(status.name(), status));

        return first.values().stream()
                .sorted((left, right) -> left.name().compareTo(right.name()))
                .map(definition -> statusOf(definition, secrets))
                .toList();
    }

    public Optional<CredentialStatus> status(SecretName name) {
        return definition(name).map(definition -> {
            Map<SecretName, SecretStatus> secrets = new LinkedHashMap<>();

            for (String field : definition.secretFields()) {
                vault.status(definition.secretName(field)).ifPresent(status -> secrets.put(status.name(), status));
            }

            return statusOf(definition, secrets);
        });
    }

    /**
     * ✍️ Creates or replaces a credential in the writable sources.
     *
     * @param plainFields  plain values, replacing the stored set
     * @param secretFields secret values; a blank or absent one keeps what is stored
     * @param secretNames  for an open-ended kind, every field that is secret — including ones kept as they
     *                     are; ignored for a closed kind, whose fields say it themselves
     */
    public CredentialStatus save(SecretName name, String kindName, String description,
                                 Map<String, String> plainFields, Map<String, String> secretFields,
                                 Set<String> secretNames, String actor) {
        CredentialKind kind = kinds.require(kindName);

        WritableCredentialSource definitions = writableDefinitions();
        WritableVaultSource      secrets     = writableSecrets();

        Map<String, String> plain        = clean(plainFields);
        Map<String, String> secretValues = clean(secretFields);
        Set<String>         secretSet    = new LinkedHashSet<>();

        for (CredentialKind.Field field : kind.fields()) {
            if (field.secret()) {
                secretSet.add(field.name());
            }
        }

        if (kind.openEnded() && secretNames != null) {
            secretSet.addAll(secretNames);
        }

        secretSet.addAll(secretValues.keySet());

        for (String field : plain.keySet()) {
            requireDeclared(kind, field, false);

            if (secretSet.contains(field)) {
                throw new IllegalArgumentException("The field '%s' is secret and cannot be stored as plain text.".formatted(field));
            }
        }

        for (String field : secretValues.keySet()) {
            requireDeclared(kind, field, true);
        }

        for (CredentialKind.Field field : kind.fields()) {
            if (!field.required()) {
                continue;
            }

            boolean present = field.secret()
                    ? secretValues.containsKey(field.name()) || vault.find(name.child(field.name())).isPresent()
                    : plain.containsKey(field.name());

            if (!present) {
                throw new IllegalArgumentException("A %s credential needs '%s'.".formatted(kind.title(), field.name()));
            }
        }

        Optional<CredentialDefinition> previous = definitions.find(name);

        definitions.save(name, kind.name(), description, plain, secretSet, actor);

        secretValues.forEach((field, value) -> secrets.put(name.child(field), SecretValue.of(value),
                "%s of credential %s".formatted(field, name), actor));

        previous.ifPresent(old -> {
            Set<String> dropped = new HashSet<>(old.secretFields());

            dropped.removeAll(secretSet);
            dropped.forEach(field -> secrets.remove(name.child(field)));
        });

        return status(name).orElseThrow();
    }

    /** 🗑️ Removes the definition and every secret of it the writable source holds. */
    public boolean remove(SecretName name) {
        WritableCredentialSource       definitions = writableDefinitions();
        Optional<CredentialDefinition> definition  = definitions.find(name);

        if (definition.isEmpty()) {
            return false;
        }

        writableSecretsIfAny().ifPresent(secrets -> definition.get().secretFields()
                .forEach(field -> secrets.remove(name.child(field))));

        return definitions.remove(name);
    }

    public CredentialKinds kinds() {
        return kinds;
    }

    private CredentialStatus statusOf(CredentialDefinition definition, Map<SecretName, SecretStatus> secrets) {
        Map<String, SecretStatus> fields  = new LinkedHashMap<>();
        List<String>              missing = new ArrayList<>();

        for (String field : definition.secretFields()) {
            fields.put(field, secrets.get(definition.secretName(field)));
        }

        kinds.find(definition.kind()).ifPresent(kind -> {
            for (CredentialKind.Field field : kind.fields()) {
                if (!field.required()) {
                    continue;
                }

                boolean present = field.secret()
                        ? fields.get(field.name()) != null
                        : definition.plainFields().containsKey(field.name());

                if (!present) {
                    missing.add(field.name());
                }
            }
        });

        boolean editable = sources.stream()
                .filter(WritableCredentialSource.class::isInstance)
                .anyMatch(source -> source.name().equals(definition.source()));

        return new CredentialStatus(definition, fields, missing, editable);
    }

    private static void requireDeclared(CredentialKind kind, String field, boolean secret) {
        Optional<CredentialKind.Field> declared = kind.field(field);

        if (declared.isEmpty()) {
            if (!kind.openEnded()) {
                throw new IllegalArgumentException("A %s credential has no field '%s'.".formatted(kind.title(), field));
            }

            SecretName.of(field);
            return;
        }

        if (declared.get().secret() != secret) {
            throw new IllegalArgumentException("The field '%s' is %s.".formatted(field, declared.get().secret() ? "secret" : "plain"));
        }
    }

    private static Map<String, String> clean(Map<String, String> values) {
        Map<String, String> cleaned = new LinkedHashMap<>();

        if (values != null) {
            values.forEach((field, value) -> {
                if (value != null && !value.isBlank()) {
                    cleaned.put(field, value.strip());
                }
            });
        }

        return cleaned;
    }

    private WritableCredentialSource writableDefinitions() {
        return sources.stream()
                .filter(WritableCredentialSource.class::isInstance)
                .map(WritableCredentialSource.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Credentials are not kept in the database here, so they cannot be changed from the application."));
    }

    private WritableVaultSource writableSecrets() {
        return writableSecretsIfAny().orElseThrow(() -> new IllegalStateException(
                "Secrets are not kept in the database here, so they cannot be changed from the application."));
    }

    private Optional<WritableVaultSource> writableSecretsIfAny() {
        return vault.writable();
    }
}
