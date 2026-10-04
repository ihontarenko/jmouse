package org.jmouse.telegram.smoke;

import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.MessageHandle;
import org.jmouse.telegram.update.IncomingMessage;
import org.jmouse.telegram.update.TelegramUser;
import org.jmouse.telegram.update.Update;
import org.jmouse.telegram.update.UpdateDispatcher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.jmouse.telegram.smoke.Checks.check;

/**
 * The inbound half, checked without a network: command parsing, deep-link arguments, fan-out,
 * handler isolation, and an unmodelled update being skipped rather than fatal.
 *
 * <p>Run its {@code main}.
 */
public final class UpdateSmoke {

    public static void main(String[] arguments) {
        parsesACommand();
        stripsTheBotSuffixFromACommand();
        readsADeepLinkArgument();
        routesACommandToItsHandler();
        routesACallbackByItsPrefix();
        deliversToEveryMatchingHandler();
        isolatesAHandlerThatThrows();
        isolatesAFilterThatThrows();
        skipsAnUnknownUpdateWithoutFailing();
        keepsTheForumTopicOnAnIncomingMessage();

        Checks.report();
    }

    private static void parsesACommand() {
        check("a plain command is read",
                message("/start").command().orElse("").equals("start"));

        check("ordinary text is not a command",
                message("start the thing").command().isEmpty());

        check("a lone slash is not a command", message("/").command().isEmpty());
    }

    private static void stripsTheBotSuffixFromACommand() {
        // ⚠️ In a group Telegram appends @botname so several bots can share a command. A naive
        // equals("/start") therefore never matches in exactly the place where bots coexist.
        check("the @botname suffix is stripped",
                message("/start@kitsu_bot").command().orElse("").equals("start"));

        check("the suffix is stripped with an argument too",
                message("/start@kitsu_bot abc123").command().orElse("").equals("start"));
    }

    private static void readsADeepLinkArgument() {
        check("a deep-link token is read",
                message("/start abc123").commandArgument().orElse("").equals("abc123"));

        check("a command with no argument has none",
                message("/start").commandArgument().isEmpty());

        check("trailing space is not an argument",
                message("/start   ").commandArgument().isEmpty());
    }

    private static void routesACommandToItsHandler() {
        List<String>     seen       = new ArrayList<>();
        UpdateDispatcher dispatcher = new UpdateDispatcher();

        dispatcher.onCommand("start", update -> seen.add("start"));
        dispatcher.onCommand("stop", update -> seen.add("stop"));

        dispatcher.dispatch(new Update.MessageReceived(1, message("/start abc")));

        check("only the matching command handler ran", seen.equals(List.of("start")));
    }

    private static void routesACallbackByItsPrefix() {
        List<String>     seen       = new ArrayList<>();
        UpdateDispatcher dispatcher = new UpdateDispatcher();

        dispatcher.onCallback("open:", update -> seen.add("open"));
        dispatcher.onCallback("hide:", update -> seen.add("hide"));

        dispatcher.dispatch(callback("open:4821"));

        check("a callback routes by its prefix", seen.equals(List.of("open")));
    }

    private static void deliversToEveryMatchingHandler() {
        // ⚠️ The architectural point: Telegram gives a bot's updates to ONE consumer, so two products
        // share one ingress. A handler that could consume an update would make which product sees a
        // button press depend on registration order.
        List<String>     seen       = new ArrayList<>();
        UpdateDispatcher dispatcher = new UpdateDispatcher();

        dispatcher.onCallback("open:", update -> seen.add("first"));
        dispatcher.onCallback("open:", update -> seen.add("second"));

        int handled = dispatcher.dispatch(callback("open:1"));

        check("both handlers ran", seen.equals(List.of("first", "second")));
        check("the dispatcher reports how many ran", handled == 2);
    }

    private static void isolatesAHandlerThatThrows() {
        List<String>     seen       = new ArrayList<>();
        UpdateDispatcher dispatcher = new UpdateDispatcher();

        dispatcher.onCallback("open:", update -> {
            throw new IllegalStateException("deliberate");
        });

        dispatcher.onCallback("open:", update -> seen.add("survived"));

        int handled = dispatcher.dispatch(callback("open:1"));

        check("a throwing handler does not stop the next one", seen.equals(List.of("survived")));
        check("it is still counted as having run", handled == 2);
    }

    private static void isolatesAFilterThatThrows() {
        List<String>     seen       = new ArrayList<>();
        UpdateDispatcher dispatcher = new UpdateDispatcher();

        dispatcher.on("a bad filter", update -> {
            throw new IllegalStateException("deliberate");
        }, update -> seen.add("should not run"));

        dispatcher.onCallback("open:", update -> seen.add("survived"));

        dispatcher.dispatch(callback("open:1"));

        check("a throwing filter is treated as no match", seen.equals(List.of("survived")));
    }

    private static void skipsAnUnknownUpdateWithoutFailing() {
        // ⚠️ Telegram adds update types on its own schedule. This is the check that says a release of
        // theirs is not an incident of ours.
        UpdateDispatcher dispatcher = new UpdateDispatcher();

        dispatcher.on(Update.MessageReceived.class, update -> {
            throw new IllegalStateException("must not be reached");
        });

        int handled = dispatcher.dispatch(new Update.Unknown(9, "business_message"));

        check("an unmodelled update is skipped, not fatal", handled == 0);
    }

    private static void keepsTheForumTopicOnAnIncomingMessage() {
        IncomingMessage inTopic = new IncomingMessage(
                ChatReference.of(-100L).inThread(7), 5, somebody(), "hi", null,
                Instant.now(), List.of(), null);

        check("the topic survives on the message", inTopic.chat().threadId() == 7);
        check("but handle() drops it", !inTopic.handle().chat().hasThread());
    }

    private static IncomingMessage message(String text) {
        return new IncomingMessage(
                ChatReference.of(42L), 1, somebody(), text, null, Instant.now(), List.of(), null);
    }

    private static Update callback(String data) {
        return new Update.CallbackPressed(
                1, "query-1", somebody(), MessageHandle.of(42L, 1), data);
    }

    private static TelegramUser somebody() {
        return new TelegramUser(7L, "Ivan", null, "ihontarenko", "uk", false);
    }

    private UpdateSmoke() {
    }
}
