package org.jmouse.telegram.user;

import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.MessageDraft;
import org.jmouse.telegram.MessageHandle;
import org.jmouse.telegram.SentMessage;
import org.jmouse.telegram.administration.ChatDetails;
import org.jmouse.telegram.update.Update;

import java.util.List;
import java.util.function.Consumer;

/**
 * One person's Telegram account, signed in over MTProto — ⚠️ <strong>implemented by the product</strong>.
 *
 * <p>Decided 2026-09-29: the MTProto library ({@code tdlight-java}) is not on Maven Central and brings
 * native binaries, so it stays out of the framework. The product that accepts it writes this interface
 * over it; everything above — routing, capabilities, login states, session keys — is here.
 *
 * <p>Failures are thrown as {@code TelegramException} carrying a refusal, as every transport's are: a
 * wrong code is {@code Rejected}, a flood wait is {@code FloodWait} with Telegram's own delay.
 *
 * <p>⚠️ Automating a user account is against Telegram's terms, and accounts doing it get limited. Reading
 * is calmer than mass messaging; it is not risk-free.
 */
public interface UserClient extends AutoCloseable {

    // —— login ——————————————————————————————————————————————————————————————————————————————————

    LoginState state();

    LoginState submitPhoneNumber(String phoneNumber);

    LoginState submitCode(String code);

    LoginState submitPassword(String password);

    /** Signs out and forgets the session — the person has to log in from scratch afterwards. */
    void logOut();

    // —— writing ————————————————————————————————————————————————————————————————————————————————

    /** ⚠️ May write to somebody first — the one thing a bot cannot. */
    SentMessage send(ChatReference chat, MessageDraft draft);

    SentMessage edit(MessageHandle message, MessageDraft draft);

    void delete(MessageHandle message);

    // —— reading ————————————————————————————————————————————————————————————————————————————————

    /** A public {@code @username} to the chat it names, by id. */
    ChatReference resolve(String username);

    /**
     * Joins a channel or a group — by {@code @username}, or by an invite link for a private one.
     *
     * @param target a {@code @username} or a {@code https://t.me/+…} link
     */
    ChatReference join(String target);

    /**
     * @param olderThan a message id from the previous page, or {@code null} for the newest
     */
    HistoryPage history(ChatReference chat, Integer olderThan, int limit);

    /** Hands every update the account receives — including channel posts — to the consumer. */
    void listen(Consumer<Update> consumer);

    // —— creating ———————————————————————————————————————————————————————————————————————————————

    ChatDetails createGroup(String title, List<Long> userIds);

    ChatDetails createChannel(String title, String description);

    @Override
    void close();
}
