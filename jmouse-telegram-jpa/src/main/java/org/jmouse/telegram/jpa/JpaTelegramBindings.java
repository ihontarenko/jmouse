package org.jmouse.telegram.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.update.TelegramUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link TelegramBindings} over Jakarta Persistence.
 *
 * <p>Jakarta Persistence alone, and no transaction demarcation — a product's transaction boundary is its
 * own. Call these inside whatever transaction the product already has; {@link #complete} in particular
 * writes two rows and wants them to land together.
 */
public final class JpaTelegramBindings implements TelegramBindings {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaTelegramBindings.class);

    /** 24 bytes, base64url without padding, giving exactly 32 characters. */
    private static final int TOKEN_ENTROPY_BYTES = 24;

    private final EntityManager entityManager;
    private final SecureRandom  random = new SecureRandom();

    public JpaTelegramBindings(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entity manager");
    }

    @Override
    public Invitation invite(String subject, Duration validFor) {
        Objects.requireNonNull(subject, "subject");

        // ⚠️ Any previous UNUSED invitation for this subject goes. Several live tokens means several
        // links in the wild, any of which binds whoever finds it - and somebody who asks twice because
        // the first link scrolled away should not leave a working one behind.
        entityManager
                .createQuery("""
                        DELETE FROM TelegramBindingToken token
                        WHERE token.subject = :subject AND token.usedAt IS NULL
                        """)
                .setParameter("subject", subject)
                .executeUpdate();

        TelegramBindingToken token = new TelegramBindingToken(
                freshToken(), subject, LocalDateTime.now().plus(validFor));

        entityManager.persist(token);

        return new Invitation(subject, token.getToken(), token.getExpiresAt());
    }

    @Override
    public Optional<Binding> complete(String token, ChatReference chat, TelegramUser from) {
        Objects.requireNonNull(chat, "chat");

        if (token == null || token.isBlank() || chat.chatId() == null) {
            return Optional.empty();
        }

        TelegramBindingToken invitation = entityManager.find(TelegramBindingToken.class, token);

        // ⚠️ One empty answer for unknown, expired and already-used alike. Distinguishing them lets
        // somebody probe for live tokens, and there is nothing a legitimate caller does differently.
        if (invitation == null || !invitation.isOpen()) {
            LOGGER.debug("a binding token was presented and refused");

            return Optional.empty();
        }

        invitation.accept();

        long   chatId       = chat.chatId();
        String languageCode = from == null ? null : from.languageCode();
        long   userId       = from == null ? 0L : from.id();

        TelegramBinding binding = locate(invitation.getSubject(), chatId).orElse(null);

        // Re-accepting from a chat that had blocked the bot revives the binding rather than adding a
        // second row for the same pair.
        if (binding == null) {
            binding = new TelegramBinding(
                    UUID.randomUUID().toString(), invitation.getSubject(), chatId, userId, languageCode);

            entityManager.persist(binding);
        } else {
            binding.revive(userId, languageCode);
        }

        LOGGER.info("telegram binding completed for subject {}", invitation.getSubject());

        return Optional.of(describe(binding));
    }

    @Override
    public List<Binding> deliverableFor(String subject) {
        return entityManager
                .createQuery("""
                        SELECT binding FROM TelegramBinding binding
                        WHERE binding.subject = :subject AND binding.deliverable = TRUE
                        ORDER BY binding.createdAt
                        """, TelegramBinding.class)
                .setParameter("subject", subject)
                .getResultList()
                .stream()
                .map(JpaTelegramBindings::describe)
                .toList();
    }

    @Override
    public List<Binding> allFor(String subject) {
        return entityManager
                .createQuery("""
                        SELECT binding FROM TelegramBinding binding
                        WHERE binding.subject = :subject
                        ORDER BY binding.createdAt
                        """, TelegramBinding.class)
                .setParameter("subject", subject)
                .getResultList()
                .stream()
                .map(JpaTelegramBindings::describe)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>⚠️ By chat rather than by subject, because that is what a {@code BotBlocked} refusal knows: the
     * send that failed named a chat, and the same chat may be bound to several subjects — all of which
     * have stopped being deliverable at once.
     */
    @Override
    public boolean deactivate(ChatReference chat) {
        if (chat.chatId() == null) {
            return false;
        }

        List<TelegramBinding> bindings = entityManager
                .createQuery("""
                        SELECT binding FROM TelegramBinding binding
                        WHERE binding.chatId = :chatId AND binding.deliverable = TRUE
                        """, TelegramBinding.class)
                .setParameter("chatId", chat.chatId())
                .getResultList();

        bindings.forEach(TelegramBinding::stopDelivering);

        if (!bindings.isEmpty()) {
            LOGGER.info("{} telegram binding(s) stopped delivering to chat {}",
                    bindings.size(), chat.chatId());
        }

        return !bindings.isEmpty();
    }

    @Override
    public boolean remove(String subject, ChatReference chat) {
        if (chat.chatId() == null) {
            return false;
        }

        TelegramBinding binding = locate(subject, chat.chatId()).orElse(null);

        if (binding == null) {
            return false;
        }

        entityManager.remove(binding);

        return true;
    }

    private Optional<TelegramBinding> locate(String subject, long chatId) {
        return entityManager
                .createQuery("""
                        SELECT binding FROM TelegramBinding binding
                        WHERE binding.subject = :subject AND binding.chatId = :chatId
                        """, TelegramBinding.class)
                .setParameter("subject", subject)
                .setParameter("chatId", chatId)
                .getResultList()
                .stream()
                .findFirst();
    }

    /**
     * ⚠️ base64<strong>url</strong>, unpadded. A standard-alphabet token would contain {@code +} and
     * {@code /}, which break inside a {@code t.me/…?start=} link unless every caller remembers to encode
     * it — and {@code =} padding is stripped for the same reason.
     */
    private String freshToken() {
        byte[] entropy = new byte[TOKEN_ENTROPY_BYTES];

        random.nextBytes(entropy);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
    }

    private static Binding describe(TelegramBinding binding) {
        return new Binding(
                binding.getSubject(),
                ChatReference.of(binding.getChatId()),
                binding.getTelegramUserId(),
                binding.getLanguageCode(),
                binding.isDeliverable());
    }
}
