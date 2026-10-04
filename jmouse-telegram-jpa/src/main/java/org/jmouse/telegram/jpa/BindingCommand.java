package org.jmouse.telegram.jpa;

import org.jmouse.telegram.MessageDraft;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramGateway;
import org.jmouse.telegram.update.IncomingMessage;
import org.jmouse.telegram.update.Update;
import org.jmouse.telegram.update.UpdateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * The half of the binding flow that runs inside the bot: {@code /start <token>}.
 *
 * <p>Register it on the dispatcher and the loop closes — the product issues an invitation, the person
 * opens the link, and this records the chat and says so.
 *
 * <pre>{@code
 * dispatcher.onCommand("start", new BindingCommand(bindings, gateway));
 * }</pre>
 *
 * <p>⚠️ It lives here rather than in the Spring module because it is the binding store's own behaviour,
 * not wiring. A product that wants different wording passes it; one that wants different behaviour
 * writes its own handler and this class is a worked example of what that has to do.
 *
 * <h2>⚠️ It always answers, including when it refuses</h2>
 *
 * <p>Somebody who followed a stale link and gets silence concludes the product is broken and tries again.
 * One sentence — deliberately the <em>same</em> one for an unknown, expired and already-used token, since
 * distinguishing them lets a token be probed for — ends that.
 *
 * <h2>⚠️ A bare {@code /start} is not an error either</h2>
 *
 * <p>People open a bot from search and press Start with no payload at all. That is a greeting, not a
 * failed binding, and treating it as one is how a first impression becomes an error message.
 */
public final class BindingCommand implements UpdateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BindingCommand.class);

    /** What is said when a binding is recorded. */
    public static final String DEFAULT_ACCEPTED = "Done — notifications will arrive here.";

    /** What is said when the token is no good, whichever way it is no good. */
    public static final String DEFAULT_REFUSED =
            "That link is no longer valid. Ask for a new one and open it again.";

    /** What is said to a bare /start, from somebody who found the bot rather than a link. */
    public static final String DEFAULT_GREETING =
            "Open the link the application gave you and this chat will be connected.";

    private final TelegramBindings bindings;
    private final TelegramGateway  gateway;
    private final String           accepted;
    private final String           refused;
    private final String           greeting;

    public BindingCommand(TelegramBindings bindings, TelegramGateway gateway) {
        this(bindings, gateway, DEFAULT_ACCEPTED, DEFAULT_REFUSED, DEFAULT_GREETING);
    }

    public BindingCommand(
            TelegramBindings bindings,
            TelegramGateway  gateway,
            String           accepted,
            String           refused,
            String           greeting) {

        this.bindings = Objects.requireNonNull(bindings, "bindings");
        this.gateway  = Objects.requireNonNull(gateway, "gateway");
        this.accepted = accepted;
        this.refused  = refused;
        this.greeting = greeting;
    }

    @Override
    public void handle(Update update) {
        if (!(update instanceof Update.MessageReceived received)) {
            return;
        }

        IncomingMessage  message  = received.message();
        Optional<String> argument = message.commandArgument();

        if (argument.isEmpty()) {
            reply(message, greeting);

            return;
        }

        Optional<TelegramBindings.Binding> binding =
                bindings.complete(argument.get(), message.chat(), message.from());

        reply(message, binding.isPresent() ? accepted : refused);
    }

    /**
     * ⚠️ A failure to reply must not fail the binding.
     *
     * <p>The row is already written by the time this runs, and the transaction is the caller's. Letting a
     * send failure propagate would roll the binding back over a message that does not matter — and the
     * person would then follow the link again and be told it is no longer valid, which it now would be.
     */
    private void reply(IncomingMessage message, String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        try {
            gateway.send(message.chat(), MessageDraft.text(text));
        } catch (TelegramException exception) {
            LOGGER.warn("the binding was recorded but its confirmation could not be sent: {}",
                    exception.refusal().message());
        }
    }
}
