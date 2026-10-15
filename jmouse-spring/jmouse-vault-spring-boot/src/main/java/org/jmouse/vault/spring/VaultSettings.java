package org.jmouse.vault.spring;

import org.jmouse.vault.credential.ConfigurationCredentialSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;
import java.util.Map;

/**
 * ⚙️ {@code jmouse.vault.*}.
 *
 * <pre>{@code
 * jmouse:
 *   vault:
 *     sources: [database, environment, configuration]   # asked in this order
 *     environment-prefix: RACCOON_SECRET                # RACCOON_SECRET_TELEGRAM__COMMANDS__TOKEN
 *     active-key: primary
 *     keys:
 *       primary: ${RACCOON_VAULT_KEY}                   # base64 of 32 bytes; never stored in the vault
 *     secrets:
 *       "[ai/voyage/api-key]": ...                      # the configuration source
 *     credentials:
 *       "[ai/voyage]": { kind: http-api, fields: { base-url: https://api.voyageai.com } }
 * }</pre>
 *
 * @param sources           the chain, in order: {@code database}, {@code environment}, {@code configuration}
 * @param environmentPrefix ⚠️ per product — one {@code .env} may feed several
 * @param activeKey         the key new seals use; required for the database source
 * @param keys              key identifier → base64 key
 * @param resealOnStart     reseal rows not under the active key at start-up — the second half of a rotation
 */
@ConfigurationProperties(prefix = "jmouse.vault")
public record VaultSettings(
        @DefaultValue({"database", "environment", "configuration"}) List<String> sources,
        @DefaultValue("JMOUSE_SECRET") String environmentPrefix,
        String activeKey,
        Map<String, String> keys,
        Map<String, String> secrets,
        Map<String, ConfigurationCredentialSource.Declaration> credentials,
        @DefaultValue("true") boolean resealOnStart) {

    public static final String SECRETS_PROPERTY = "jmouse.vault.secrets";

    public VaultSettings {
        keys        = keys == null ? Map.of() : keys;
        secrets     = secrets == null ? Map.of() : secrets;
        credentials = credentials == null ? Map.of() : credentials;
    }
}
