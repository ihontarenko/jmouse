package org.jmouse.vault.credential;

import org.jmouse.vault.SecretName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * 📄 Credentials declared in configuration, under {@code jmouse.vault.credentials[<name>]} — the plain
 * fields only. Their secrets resolve through the vault like any other, usually from the environment.
 */
public final class ConfigurationCredentialSource implements CredentialSource {

    public static final String NAME = "configuration";

    /** 📝 One configured credential. */
    public record Declaration(String kind, String description, Map<String, String> fields, Set<String> secrets) {
    }

    private final Map<SecretName, CredentialDefinition> definitions = new TreeMap<>();

    public ConfigurationCredentialSource(Map<String, Declaration> declarations) {
        declarations.forEach((name, declaration) -> {
            SecretName secretName = SecretName.of(name);
            String     kind       = declaration.kind() == null ? CredentialKinds.GENERIC : declaration.kind();

            definitions.put(secretName, new CredentialDefinition(secretName, kind, declaration.description(),
                    declaration.fields() == null ? Map.of() : declaration.fields(),
                    declaration.secrets() == null ? Set.of() : declaration.secrets(),
                    null, null, NAME));
        });
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Optional<CredentialDefinition> find(SecretName name) {
        return Optional.ofNullable(definitions.get(name));
    }

    @Override
    public List<CredentialDefinition> list() {
        return new ArrayList<>(definitions.values());
    }
}
