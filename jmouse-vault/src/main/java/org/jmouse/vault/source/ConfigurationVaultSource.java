package org.jmouse.vault.source;

import org.jmouse.vault.SecretEntry;
import org.jmouse.vault.SecretName;
import org.jmouse.vault.SecretValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 📄 Secrets written into the application's configuration — in this workspace, the git-ignored
 * {@code application-sensitive.yml}, under {@code jmouse.vault.secrets}.
 */
public final class ConfigurationVaultSource implements VaultSource {

    public static final String NAME = "configuration";

    private final Map<SecretName, SecretValue> secrets = new TreeMap<>();
    private final String                       propertyPrefix;

    /**
     * @param secrets        name → value, as configured; blank values are ignored
     * @param propertyPrefix the key the map sits under, for {@link SecretEntry#location()}
     */
    public ConfigurationVaultSource(Map<String, String> secrets, String propertyPrefix) {
        this.propertyPrefix = propertyPrefix;

        secrets.forEach((name, value) -> {
            if (value != null && !value.isBlank()) {
                this.secrets.put(SecretName.of(name), SecretValue.of(value));
            }
        });
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Optional<SecretValue> find(SecretName name) {
        return Optional.ofNullable(secrets.get(name));
    }

    @Override
    public List<SecretEntry> list() {
        List<SecretEntry> entries = new ArrayList<>();

        secrets.forEach((name, value) -> entries.add(
                new SecretEntry(name, NAME, value.fingerprint(), null, null, null, locate(name))));

        return entries;
    }

    @Override
    public String locate(SecretName name) {
        return propertyPrefix + "[" + name.value() + "]";
    }
}
