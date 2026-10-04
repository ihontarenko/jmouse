package org.jmouse.telegram.spring;

import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;

import java.util.Map;
import java.util.Objects;

/**
 * Identities out of {@code jmouse.telegram.identities}.
 *
 * <p>The honest default for an application whose bots do not change — one token in an environment
 * variable, no schema, no administration screen. A product that wants an administrator to rotate a
 * token at runtime declares its own {@link IdentitySource} (that is `JMF-334`'s job) and this one steps
 * aside.
 *
 * <p>⚠️ <strong>It re-reads the map on every call rather than resolving once.</strong> That looks
 * pointless when the source is immutable configuration, and it is the contract: the per-call rule is
 * what lets a database-backed source be substituted without any caller changing, and a source that
 * cached would be the odd one out whose behaviour differs from every other implementation.
 */
public final class PropertiesIdentitySource implements IdentitySource {

    private final Map<String, TelegramSettings.Identity> identities;

    public PropertiesIdentitySource(TelegramSettings settings) {
        this.identities = Objects.requireNonNull(settings, "settings").identities();
    }

    @Override
    public TelegramIdentity identity() {
        return identity(GENERAL);
    }

    @Override
    public TelegramIdentity identity(String purpose) {
        TelegramSettings.Identity configured = identities.get(purpose);

        // ⚠️ The fallback that makes purposes free to adopt: a product asking for a purpose nobody
        // configured gets the general identity rather than a refusal, so naming a purpose in code does
        // not require configuring one first.
        if (configured == null || !configured.enabled()) {
            configured = identities.get(GENERAL);
        }

        if (configured == null) {
            throw new TelegramException(new TelegramRefusal.Unauthorized(purpose,
                    "configure jmouse.telegram.identities.%s.token, or a general identity"
                            .formatted(purpose)));
        }

        if (!configured.enabled()) {
            throw new TelegramException(new TelegramRefusal.Unauthorized(purpose,
                    "every candidate identity is disabled"));
        }

        return new TelegramIdentity(
                configured.kind(), purpose, configured.token(), configured.apiBase());
    }
}
