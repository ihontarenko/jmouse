package org.jmouse.vault.spring.autoconfigure;

import org.jmouse.vault.Vault;
import org.jmouse.vault.audit.VaultAudit;
import org.jmouse.vault.credential.ConfigurationCredentialSource;
import org.jmouse.vault.credential.CredentialKind;
import org.jmouse.vault.credential.CredentialKinds;
import org.jmouse.vault.credential.CredentialSource;
import org.jmouse.vault.credential.Credentials;
import org.jmouse.vault.credential.WritableCredentialSource;
import org.jmouse.vault.source.ConfigurationVaultSource;
import org.jmouse.vault.source.EnvironmentVaultSource;
import org.jmouse.vault.source.VaultSource;
import org.jmouse.vault.source.WritableVaultSource;
import org.jmouse.vault.spring.SecretReferenceConverter;
import org.jmouse.vault.spring.VaultSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 🗝️ The {@link Vault} and {@link Credentials}, built from {@code jmouse.vault.sources} in that order.
 *
 * <p>The database source joins the chain when {@link VaultJpaAutoConfiguration} produced one; listed but
 * absent, it is skipped with a warning.
 */
@AutoConfiguration
@EnableConfigurationProperties(VaultSettings.class)
public class VaultAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultAutoConfiguration.class);

    private static final String DATABASE      = "database";
    private static final String ENVIRONMENT   = EnvironmentVaultSource.NAME;
    private static final String CONFIGURATION = ConfigurationVaultSource.NAME;

    @Bean
    @ConfigurationPropertiesBinding
    public static SecretReferenceConverter secretReferenceConverter() {
        return new SecretReferenceConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public Vault vault(VaultSettings settings, ObjectProvider<WritableVaultSource> database) {
        List<VaultSource> sources = new ArrayList<>();

        for (String name : settings.sources()) {
            switch (name) {
                case DATABASE -> {
                    WritableVaultSource source = database.getIfAvailable();

                    if (source == null) {
                        LOGGER.warn("🗝️ The vault lists the '{}' source, but it is not configured here "
                                + "(it needs jmouse-vault-jpa and jmouse.vault.active-key); skipped.", DATABASE);
                    } else {
                        sources.add(source);
                    }
                }
                case ENVIRONMENT -> sources.add(EnvironmentVaultSource.system(settings.environmentPrefix(), keysOf(settings)));
                case CONFIGURATION -> sources.add(new ConfigurationVaultSource(settings.secrets(), VaultSettings.SECRETS_PROPERTY));
                default -> throw new IllegalStateException(("jmouse.vault.sources names '%s'; the sources are '%s', "
                        + "'%s' and '%s'.").formatted(name, DATABASE, ENVIRONMENT, CONFIGURATION));
            }
        }

        Vault vault = new Vault(sources);

        LOGGER.info("🗝️ Vault sources, in order: {}", vault.sourceNames());

        return vault;
    }

    /** The vault's own keys, which no source may ever answer with — see {@link EnvironmentVaultSource}. */
    private static Set<String> keysOf(VaultSettings settings) {
        Set<String> keys = new HashSet<>();

        settings.keys().values().stream()
                .filter(key -> key != null && !key.isBlank())
                .map(String::strip)
                .forEach(keys::add);

        return keys;
    }

    @Bean
    @ConditionalOnMissingBean
    public CredentialKinds credentialKinds(ObjectProvider<CredentialKind> declared) {
        List<CredentialKind> kinds = new ArrayList<>(CredentialKinds.standard());

        declared.orderedStream().forEach(kinds::add);

        return new CredentialKinds(kinds);
    }

    @Bean
    @ConditionalOnMissingBean
    public Credentials credentials(VaultSettings settings, Vault vault, CredentialKinds kinds,
                                   ObjectProvider<WritableCredentialSource> database) {
        List<CredentialSource> sources = new ArrayList<>();

        database.ifAvailable(sources::add);
        sources.add(new ConfigurationCredentialSource(settings.credentials()));

        return new Credentials(sources, vault, kinds);
    }

    @Bean
    @ConditionalOnMissingBean
    public VaultAudit discardingVaultAudit() {
        LOGGER.warn("🗝️ No vault audit is configured (it needs the database source); reveals and changes are not recorded.");
        return VaultAudit.discarding();
    }
}
