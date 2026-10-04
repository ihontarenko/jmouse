package org.jmouse.telegram.jpa;

import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;

import java.util.Objects;
import java.util.Optional;

/**
 * Identities out of the {@code telegram_accounts} table.
 *
 * <p>The reason the table exists: an administrator rotates a token, disables an account during an
 * incident, or adds a purpose, and the next send obeys it — with no deploy and no restart.
 *
 * <h2>⚠️ It really does read per call, and that is not an oversight</h2>
 *
 * <p>One query against one indexed row, beside an HTTPS round trip to Telegram that takes a thousand
 * times longer. Caching it would defeat the only thing this class is for: a token rotated at 03:00
 * because it leaked must take effect on the next message, not on the next restart.
 *
 * <p>An installation that measures this and wants a cache puts one behind {@link IdentitySource} with a
 * short time to live — and then owns the decision that a rotation takes that long to land.
 *
 * <h2>Resolution order</h2>
 *
 * <ol>
 *   <li>the row for {@code purpose}, if it is enabled</li>
 *   <li>the row for {@link IdentitySource#GENERAL}, if it is enabled</li>
 *   <li>a refusal naming what to configure</li>
 * </ol>
 *
 * <p>⚠️ A <strong>disabled</strong> row falls through to {@code general} rather than refusing outright.
 * Disabling one purpose during an incident should degrade that purpose to the general bot, not silence
 * the product — and an installation that wants silence instead disables `general` too.
 */
public final class JpaIdentitySource implements IdentitySource {

    private final JpaTelegramAccounts accounts;

    public JpaIdentitySource(JpaTelegramAccounts accounts) {
        this.accounts = Objects.requireNonNull(accounts, "accounts");
    }

    @Override
    public TelegramIdentity identity() {
        return identity(GENERAL);
    }

    @Override
    public TelegramIdentity identity(String purpose) {
        TelegramAccount account = enabled(purpose)
                .or(() -> enabled(GENERAL))
                .orElseThrow(() -> new TelegramException(new TelegramRefusal.Unauthorized(purpose,
                        "no enabled Telegram account answers for this purpose, and none answers for '%s'"
                                .formatted(GENERAL))));

        // ⚠️ Opened here and nowhere else, at the moment of use, so that no read of the table - a
        // listing, an administration response, a log line - can materialise a token.
        return new TelegramIdentity(
                account.getKind(),
                account.getPurpose(),
                accounts.openCredential(account),
                account.getApiBase());
    }

    private Optional<TelegramAccount> enabled(String purpose) {
        return accounts.locate(purpose).filter(TelegramAccount::isEnabled);
    }
}
