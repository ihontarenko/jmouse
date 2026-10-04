package org.jmouse.telegram.spring;

import com.fasterxml.jackson.databind.JsonNode;
import org.jmouse.telegram.bot.BotApiUpdates;
import org.jmouse.telegram.update.Update;
import org.jmouse.telegram.update.UpdateDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * Where Telegram posts updates.
 *
 * <h2>⚠️ The secret token is the only thing standing in front of this endpoint</h2>
 *
 * <p>A webhook is a public route. Telegram will not send a bearer token, will not present a client
 * certificate and cannot be allow-listed by address with any confidence. What it does do is echo back
 * the {@code secret_token} given to {@code setWebhook}, in the
 * {@code X-Telegram-Bot-Api-Secret-Token} header.
 *
 * <p>Without checking it, this endpoint accepts anything shaped like an update from anybody on the
 * internet — which means an attacker can make the application believe a message arrived, from a chat
 * id of their choosing. Every handler downstream then behaves as though it came from Telegram.
 *
 * <p>So: the header is compared before the body is looked at, the comparison is constant-time, and a
 * mismatch answers {@code 401} with no detail. ⚠️ An installation with no secret configured is refused
 * at startup rather than served insecurely — see {@code TelegramWebAutoConfiguration}.
 *
 * <h2>⚠️ It answers 200 even when a handler failed</h2>
 *
 * <p>Telegram retries a non-2xx response, with escalating delay, and disables a webhook that keeps
 * failing. A handler defect would therefore turn into Telegram redelivering the same update repeatedly
 * and eventually switching the integration off. The dispatcher already isolates and logs a failing
 * handler; this endpoint's job is to confirm receipt.
 */
@RestController
public class TelegramWebhookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramWebhookController.class);

    /** The header Telegram echoes the configured secret in. */
    public static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final UpdateDispatcher dispatcher;
    private final String           secret;

    public TelegramWebhookController(UpdateDispatcher dispatcher, String secret) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.secret     = Objects.requireNonNull(secret, "webhook secret");
    }

    @PostMapping("${jmouse.telegram.updates.webhook.path:" + TelegramSettings.Webhook.DEFAULT_PATH + "}")
    public ResponseEntity<Void> receive(
            @RequestHeader(value = SECRET_HEADER, required = false) String presented,
            @RequestBody JsonNode body) {

        if (!matchesSecret(presented)) {
            // No detail, and nothing about the body: a probe learns only that it was refused.
            LOGGER.warn("a webhook call arrived with a wrong or missing secret token");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Update update = BotApiUpdates.read(body);

        dispatcher.dispatch(update);

        return ResponseEntity.ok().build();
    }

    /**
     * ⚠️ Constant-time. A short-circuiting {@code equals} leaks the length of the matching prefix
     * through timing, and this is a value an attacker may guess against at whatever rate they like.
     */
    private boolean matchesSecret(String presented) {
        if (presented == null) {
            return false;
        }

        byte[] expected = secret.getBytes(StandardCharsets.UTF_8);
        byte[] actual   = presented.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expected, actual);
    }
}
