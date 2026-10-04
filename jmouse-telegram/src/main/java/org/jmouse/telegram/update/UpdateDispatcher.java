package org.jmouse.telegram.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Where an update goes.
 *
 * <h2>⚠️ This is the fan-out, and that is an architectural requirement rather than a convenience</h2>
 *
 * <p>Telegram delivers a bot's updates to exactly <strong>one</strong> consumer — one webhook, or one
 * {@code getUpdates} caller. A second one does not get a copy; it silently steals from the first.
 *
 * <p>So an installation where two products want the same bot's updates cannot have two listeners. It
 * has one ingress and this dispatcher behind it. That is why every matching handler runs, rather than
 * the first: a handler that could consume an update would make "which product sees this button press"
 * depend on registration order.
 *
 * <h2>⚠️ A handler that throws does not stop the loop</h2>
 *
 * <p>Ingestion is a long-lived loop, and one bad handler must not end it — the failure would be the
 * whole integration going quiet, hours before anybody noticed. Each handler is therefore isolated and
 * logged.
 *
 * <p>Registration is safe at any time: the handler list is copy-on-write, so a product that registers
 * during startup while the loop is already running does not have to synchronise.
 *
 * <pre>{@code
 * dispatcher
 *     .onCommand("start",  update -> bindings.begin(update))
 *     .onCallback("open:", update -> openWhatWasPressed(update))
 *     .on(Update.ChannelPost.class, update -> index(update));
 * }</pre>
 */
public final class UpdateDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDispatcher.class);

    private final List<Registration> registrations = new CopyOnWriteArrayList<>();

    private record Registration(String description, Predicate<Update> when, UpdateHandler handler) {
    }

    /** Anything matching {@code when}. */
    public UpdateDispatcher on(String description, Predicate<Update> when, UpdateHandler handler) {
        Objects.requireNonNull(when, "predicate");
        Objects.requireNonNull(handler, "handler");

        registrations.add(new Registration(description, when, handler));

        return this;
    }

    /** Every update of one kind. */
    public <T extends Update> UpdateDispatcher on(Class<T> kind, UpdateHandler handler) {
        return on(kind.getSimpleName(), kind::isInstance, handler);
    }

    /**
     * A slash command, with or without its {@code @botname} suffix and with or without an argument.
     *
     * @param command the name without its slash — {@code "start"}, not {@code "/start"}
     */
    public UpdateDispatcher onCommand(String command, UpdateHandler handler) {
        String wanted = command.startsWith("/") ? command.substring(1) : command;

        return on("/" + wanted, update -> update instanceof Update.MessageReceived received
                && received.message().command().filter(wanted::equals).isPresent(), handler);
    }

    /**
     * A button press whose callback data begins with {@code prefix}.
     *
     * <p>A prefix rather than an equality test because callback data is capped at 64 bytes and is
     * therefore a key — {@code "open:4821"} — so the routable part is always its head.
     */
    public UpdateDispatcher onCallback(String prefix, UpdateHandler handler) {
        return on("callback " + prefix, update -> update instanceof Update.CallbackPressed pressed
                && pressed.dataStartsWith(prefix), handler);
    }

    /**
     * Hands one update to every handler that wants it.
     *
     * @return how many handlers ran, which is what an ingestion loop logs to notice that nothing is
     *         listening — a bot receiving updates no handler matches looks identical to a bot
     *         receiving nothing at all
     */
    public int dispatch(Update update) {
        if (update instanceof Update.Unknown unknown) {
            // ⚠️ Not an error. Telegram adds update types on its own schedule, and treating one as a
            // failure turns every Telegram release into an incident.
            LOGGER.debug("skipping an update kind this library does not model: {}", unknown.kind());

            return 0;
        }

        int handled = 0;

        for (Registration registration : registrations) {
            if (!matches(registration, update)) {
                continue;
            }

            handled++;

            try {
                registration.handler().handle(update);
            } catch (RuntimeException exception) {
                // Isolated deliberately: one handler must not end the loop. The update is lost, since
                // Telegram considers a delivered update delivered - hence the error rather than a warn.
                LOGGER.error("handler '{}' failed on update {}; the update is lost",
                        registration.description(), update.updateId(), exception);
            }
        }

        if (handled == 0) {
            LOGGER.debug("no handler matched update {} ({})",
                    update.updateId(), update.getClass().getSimpleName());
        }

        return handled;
    }

    /**
     * ⚠️ A predicate that throws is treated as "does not match" rather than being allowed to abort the
     * whole dispatch. A badly written filter otherwise silently stops every <em>later</em> handler from
     * seeing the update, which is a far harder failure to find than one handler misbehaving.
     */
    private boolean matches(Registration registration, Update update) {
        try {
            return registration.when().test(update);
        } catch (RuntimeException exception) {
            LOGGER.error("the filter for '{}' threw; treating it as no match",
                    registration.description(), exception);

            return false;
        }
    }
}
