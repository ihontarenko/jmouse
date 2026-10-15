package org.jmouse.vault.source;

import org.jmouse.vault.SecretEntry;
import org.jmouse.vault.SecretName;
import org.jmouse.vault.SecretValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 🌍 Secrets from environment variables, named by convention — see {@link SecretName#environmentVariable}.
 *
 * <p>This is also where a {@code .env} file arrives: the workspace's launcher sources it into the
 * environment, so to the application it is environment like any other.
 *
 * <p>⚠️ The prefix is per product ({@code RACCOON_SECRET}, {@code KITSU_SECRET}) because one {@code .env}
 * may feed several products, and two products' {@code telegram/notifications/token} are different bots.
 * It is deliberately not the prefix of the vault's own key ({@code RACCOON_VAULT_KEY}).
 */
public final class EnvironmentVaultSource implements VaultSource {

    public static final String NAME = "environment";

    private final String                        prefix;
    private final Supplier<Map<String, String>> environment;
    private final Set<String>                   withheld;

    /**
     * @param withheld values this source never answers with, whatever variable holds them — the vault's own
     *                 keys. ⚠️ A key placed under the prefix by mistake would otherwise be listed, and revealed,
     *                 as a secret like any other
     */
    public EnvironmentVaultSource(String prefix, Supplier<Map<String, String>> environment, Set<String> withheld) {
        this.prefix      = Objects.requireNonNull(prefix, "prefix");
        this.environment = Objects.requireNonNull(environment, "environment");
        this.withheld    = Set.copyOf(withheld);
    }

    public static EnvironmentVaultSource system(String prefix, Set<String> withheld) {
        return new EnvironmentVaultSource(prefix, System::getenv, withheld);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Optional<SecretValue> find(SecretName name) {
        String value = environment.get().get(name.environmentVariable(prefix));

        if (isAbsentOrWithheld(value)) {
            return Optional.empty();
        }

        return Optional.of(SecretValue.of(value));
    }

    @Override
    public List<SecretEntry> list() {
        List<SecretEntry> entries = new ArrayList<>();

        for (Map.Entry<String, String> variable : environment.get().entrySet()) {
            SecretName name = SecretName.fromEnvironmentVariable(prefix, variable.getKey());

            if (name == null || isAbsentOrWithheld(variable.getValue())) {
                continue;
            }

            entries.add(new SecretEntry(name, NAME, SecretValue.of(variable.getValue()).fingerprint(),
                    null, null, null, variable.getKey()));
        }

        return entries;
    }

    @Override
    public String locate(SecretName name) {
        return name.environmentVariable(prefix);
    }

    private boolean isAbsentOrWithheld(String value) {
        return value == null || value.isBlank() || withheld.contains(value.strip());
    }
}
