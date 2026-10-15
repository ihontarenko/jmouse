package org.jmouse.telegram.management;

import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.jpa.TelegramBindings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Who is bound to which chat — the deep-link flow's results — and the invitations that start one.
 */
@RestController
@RequestMapping(ManagementRoutes.PREFIX + "/bindings")
public class TelegramBindingController {

    private final ObjectProvider<TelegramBindings> bindings;
    private final TransactionOperations            transactions;

    public TelegramBindingController(ObjectProvider<TelegramBindings> bindings, TransactionOperations transactions) {
        this.bindings     = bindings;
        this.transactions = transactions;
    }

    /**
     * One row of the listing.
     *
     * @param chat        the chat's wire value
     * @param deliverable false once the person blocked the bot or the binding was switched off — kept so a
     *                    table can say so, rather than showing nothing
     */
    public record BindingView(String subject, String chat, long telegramUserId, String languageCode, boolean deliverable) {

        static BindingView of(TelegramBindings.Binding binding) {
            return new BindingView(binding.subject(), binding.chat().wireValue(), binding.telegramUserId(),
                                   binding.languageCode(), binding.deliverable());
        }
    }

    /**
     * @param subject     the product's identifier for whoever will be bound
     * @param validFor    how long the link works, or {@code null} for a day
     * @param botUsername the bot to link to, for the ready-made deep link; {@code null} leaves it out
     */
    public record InvitationRequest(String subject, Duration validFor, String botUsername) {
    }

    /**
     * @param token     ⚠️ binds whoever presents it first — hand it only to the person it is for
     * @param deepLink  {@code https://t.me/<bot>?start=<token>}, when a bot was named
     */
    public record InvitationView(String subject, String token, String deepLink, LocalDateTime expiresAt) {
    }

    /**
     * @param deliverable {@code true} only deliverable, {@code false} only switched off, absent for both
     * @param subject     when given, every binding of that subject instead of a page of everybody's
     */
    @GetMapping
    public ManagementPage<BindingView> bindings(
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "deliverable", required = false) Boolean deliverable,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        TelegramBindings store = requireBindings();

        if (subject != null && !subject.isBlank()) {
            List<BindingView> all = store.allFor(subject).stream().map(BindingView::of).toList();
            return new ManagementPage<>(all, all.size(), 0, all.size());
        }

        int                           bounded = ManagementRoutes.boundedLimit(limit);
        int                           from    = Math.max(0, offset);
        TelegramBindings.BindingPage  page    = store.page(deliverable, from, bounded);

        return new ManagementPage<>(page.bindings().stream().map(BindingView::of).toList(), page.total(), from, bounded);
    }

    /** Starts a binding for a subject; ⚠️ replaces that subject's previous unused invitation. */
    @PostMapping("/invitations")
    public InvitationView invite(@RequestBody InvitationRequest request) {
        if (request.subject() == null || request.subject().isBlank()) {
            throw new IllegalArgumentException("An invitation needs the subject it will bind");
        }

        TelegramBindings store    = requireBindings();
        Duration         validFor = request.validFor() == null ? TelegramBindings.DEFAULT_VALIDITY : request.validFor();

        TelegramBindings.Invitation invitation = transactions.execute(status -> store.invite(request.subject(), validFor));

        String deepLink = request.botUsername() == null || request.botUsername().isBlank()
                ? null
                : invitation.deepLink(request.botUsername());

        return new InvitationView(invitation.subject(), invitation.token(), deepLink, invitation.expiresAt());
    }

    /** Forgets one binding outright — for a person who asks to be forgotten. */
    @DeleteMapping
    public ResponseEntity<Void> remove(@RequestParam("subject") String subject, @RequestParam("chat") String chat) {
        TelegramBindings store   = requireBindings();
        ChatReference    target  = ChatReference.parse(chat);
        Boolean          removed = transactions.execute(status -> store.remove(subject, target));

        return Boolean.TRUE.equals(removed) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private TelegramBindings requireBindings() {
        TelegramBindings store = bindings.getIfAvailable();

        if (store == null) {
            throw new AccountsUnavailableException("Telegram bindings");
        }

        return store;
    }
}
