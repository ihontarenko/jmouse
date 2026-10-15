package org.jmouse.telegram.management;

import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentityKind;
import org.jmouse.telegram.MessageDraft;
import org.jmouse.telegram.SentMessage;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramGateway;
import org.jmouse.telegram.jpa.TelegramAccounts;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Telegram accounts — one identity per purpose, kept in {@code telegram_accounts}.
 *
 * <p>⚠️ <strong>The credential is never returned.</strong> Not masked, not partially: an endpoint that
 * echoes a bot token turns read access to a settings screen into control of the bot. A write accepts one;
 * no read ever answers with one — {@link TelegramAccounts.Description} carries no credential field at all.
 *
 * <p>Where identities come from properties there are no rows: every route answers
 * {@link AccountsUnavailableException} rather than an empty list.
 */
@RestController
@RequestMapping(ManagementRoutes.PREFIX + "/accounts")
public class TelegramAccountController {

    private final ObjectProvider<TelegramAccounts> accounts;
    private final TelegramGateway                  gateway;
    private final TransactionOperations            transactions;

    public TelegramAccountController(
            ObjectProvider<TelegramAccounts> accounts, TelegramGateway gateway, TransactionOperations transactions) {
        this.accounts     = accounts;
        this.gateway      = gateway;
        this.transactions = transactions;
    }

    /**
     * What a new or replacing account carries.
     *
     * @param kind       {@code BOT} or {@code USER}
     * @param credential ⚠️ in the clear on the way in, sealed before it is written, never read back
     * @param apiBase    a self-hosted Bot API server, or {@code null} for Telegram's own
     */
    public record AccountDraft(IdentityKind kind, String credential, String apiBase) {
    }

    /** @param credential the new token — ⚠️ never read back */
    public record CredentialChange(String credential) {
    }

    public record EnabledChange(boolean enabled) {
    }

    /**
     * A message to send as the account, to prove its token works.
     *
     * @param chat a chat id or {@code @username} — the person testing must have started the bot
     * @param text what to send, or {@code null} for a standard line
     */
    public record TestMessage(String chat, String text) {
    }

    /**
     * What the test came to — ⚠️ a refusal is an answer here, not an error: turning a misconfigured
     * token from a mystery into a sentence is the whole point of the route.
     *
     * @param delivered whether Telegram accepted it
     * @param messageId the message sent, when delivered
     * @param refusal   the refusal's case name when not
     * @param sentence  the refusal's own sentence when not
     */
    public record TestOutcome(boolean delivered, Integer messageId, String refusal, String sentence) {
    }

    @GetMapping
    public List<TelegramAccounts.Description> accounts() {
        return requireAccounts().list();
    }

    @GetMapping("/{purpose}")
    public ResponseEntity<TelegramAccounts.Description> account(@PathVariable("purpose") String purpose) {
        return ResponseEntity.of(requireAccounts().find(purpose));
    }

    /** Creates the purpose's account, or replaces it whole — kind, credential and API base together. */
    @PutMapping("/{purpose}")
    public TelegramAccounts.Description save(@PathVariable("purpose") String purpose, @RequestBody AccountDraft draft) {
        TelegramAccounts store = requireAccounts();
        IdentityKind     kind  = draft.kind() == null ? IdentityKind.BOT : draft.kind();

        return transactions.execute(status -> store.upsert(purpose, kind, draft.credential(), blankToNull(draft.apiBase())));
    }

    /** Replaces only the token — an account switched off stays off, its API base stays put. */
    @PutMapping("/{purpose}/credential")
    public TelegramAccounts.Description rotate(@PathVariable("purpose") String purpose, @RequestBody CredentialChange change) {
        TelegramAccounts store = requireAccounts();

        return transactions.execute(status -> store.rotate(purpose, change.credential()));
    }

    @PutMapping("/{purpose}/enabled")
    public TelegramAccounts.Description enable(@PathVariable("purpose") String purpose, @RequestBody EnabledChange change) {
        TelegramAccounts store = requireAccounts();

        return transactions.execute(status -> store.setEnabled(purpose, change.enabled()));
    }

    @DeleteMapping("/{purpose}")
    public ResponseEntity<Void> remove(@PathVariable("purpose") String purpose) {
        TelegramAccounts store   = requireAccounts();
        Boolean          removed = transactions.execute(status -> store.remove(purpose));

        return Boolean.TRUE.equals(removed) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Sends one message as the purpose's identity.
     *
     * <p>⚠️ Works for a purpose configured in properties too — it goes through the gateway, not the table.
     */
    @PostMapping("/{purpose}/test-message")
    public TestOutcome test(@PathVariable("purpose") String purpose, @RequestBody TestMessage message) {
        ChatReference chat = ChatReference.parse(message.chat());
        String        text = message.text() == null || message.text().isBlank()
                ? "jMouse: a test message from '%s'".formatted(purpose)
                : message.text();

        try {
            SentMessage sent = gateway.as(purpose).send(chat, MessageDraft.text(text));
            return new TestOutcome(true, sent.messageId(), null, null);
        } catch (TelegramException exception) {
            return new TestOutcome(false, null, exception.refusal().getClass().getSimpleName(), exception.refusal().message());
        }
    }

    private TelegramAccounts requireAccounts() {
        TelegramAccounts store = accounts.getIfAvailable();

        if (store == null) {
            throw new AccountsUnavailableException("Telegram accounts");
        }

        return store;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
