package org.jmouse.telegram.command;

import org.jmouse.telegram.CallbackAnswer;
import org.jmouse.telegram.MessageDraft;
import org.jmouse.telegram.SentMessage;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramGateway;
import org.jmouse.telegram.markup.InlineButton;
import org.jmouse.telegram.markup.ReplyMarkup;
import org.jmouse.telegram.menu.MenuEntry;
import org.jmouse.telegram.update.IncomingMessage;
import org.jmouse.telegram.update.Update;
import org.jmouse.telegram.update.UpdateDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.time.Clock;
import java.time.Instant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Bot commands that act as the bound person.
 *
 * <pre>{@code
 * CommandRouter router = new CommandRouter(gateway.as("raccoon-commands"),
 *         new BindingsSenderResolver(bindings), guard, List.of(search, read));
 * router.install(dispatcher);
 * }</pre>
 *
 * <h2>What happens to a command</h2>
 *
 * <ol>
 *   <li>{@code /start} is left alone — the binding flow owns it.</li>
 *   <li>⚠️ The sender is resolved to a product subject. Unbound → one sentence, no handler, and no hint of
 *       which commands exist.</li>
 *   <li>Unknown command, or {@code /help} → the list of commands.</li>
 *   <li>The {@link CommandGuard} is asked; refused → one sentence.</li>
 *   <li>The handler runs <strong>on the executor</strong>, never on the polling thread — a five-second
 *       search must not stop every other update.</li>
 * </ol>
 *
 * <p>⚠️ The gateway passed in is the one replies go out on — normally the product's own bot, the same one
 * whose updates this router reads.
 */
public final class CommandRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRouter.class);

    /** Callback data prefix for page turns: {@code jmc:p:<token>:<index>}. */
    static final String PAGE_PREFIX = "jmc:p:";

    /** Callback data prefix for a {@link CommandAction}: {@code jmc:c:<command>:<argument>}. */
    static final String ACTION_PREFIX = "jmc:c:";

    private static final String HELP  = "help";
    private static final String START = "start";

    private final TelegramGateway         gateway;
    private final SenderResolver          senders;
    private final CommandGuard            guard;
    private final Map<String, BotCommand> commands;
    private final Executor                executor;
    private final CommandPhrases          phrases;
    private final ReplyPages              pages;
    private final BotCommand              fileReceiver;

    public CommandRouter(TelegramGateway gateway, SenderResolver senders, CommandGuard guard, List<BotCommand> commands) {
        this(gateway, senders, guard, commands, Executors.newVirtualThreadPerTaskExecutor(), CommandPhrases.english());
    }

    public CommandRouter(
            TelegramGateway gateway, SenderResolver senders, CommandGuard guard, List<BotCommand> commands,
            Executor executor, CommandPhrases phrases) {
        if (gateway == null || senders == null || guard == null || executor == null || phrases == null) {
            throw new IllegalArgumentException("A command router needs a gateway, a sender resolver, a guard, an executor and its phrases");
        }

        this.gateway  = gateway;
        this.senders  = senders;
        this.guard    = guard;
        this.commands = index(commands);
        this.fileReceiver = fileReceiver(this.commands.values());
        this.executor = executor;
        this.phrases  = phrases;
        this.pages    = new ReplyPages(Clock.systemUTC());
    }

    private static Map<String, BotCommand> index(List<BotCommand> commands) {
        Map<String, BotCommand> indexed = new LinkedHashMap<>();

        for (BotCommand command : commands) {
            if (START.equals(command.name()) || HELP.equals(command.name())) {
                throw new IllegalArgumentException("/" + command.name() + " belongs to the library: /start binds, /help lists");
            }

            if (indexed.putIfAbsent(command.name(), command) != null) {
                throw new IllegalArgumentException("Two commands are named /" + command.name());
            }
        }

        // ⚠️ Declaration order kept: it is the order /help and the menu show.
        return Collections.unmodifiableMap(indexed);
    }

    /** The one command a file with no command goes to, or {@code null} when none receives files. */
    private static BotCommand fileReceiver(Collection<BotCommand> commands) {
        List<BotCommand> receiving = commands.stream().filter(BotCommand::receivesFiles).toList();

        if (receiving.size() > 1) {
            throw new IllegalArgumentException("Only one command may receive files, and "
                    + receiving.stream().map(command -> "/" + command.name()).toList() + " all say they do");
        }

        return receiving.isEmpty() ? null : receiving.getFirst();
    }

    /** Registers on the dispatcher: every command message, and the page-turn buttons. */
    public void install(UpdateDispatcher dispatcher) {
        dispatcher.on("bot commands", CommandRouter::isCommand, this::onMessage);

        if (fileReceiver != null) {
            dispatcher.on("bot files", CommandRouter::isFileWithoutCommand, this::onFile);
        }
        dispatcher.onCallback(PAGE_PREFIX, this::onPageTurn);
        dispatcher.onCallback(ACTION_PREFIX, this::onAction);
    }

    /** The commands, in declaration order — for a product publishing Telegram's command menu. */
    public List<BotCommand> commands() {
        return List.copyOf(commands.values());
    }

    /** The commands as Telegram's menu lists them, {@code /help} first. */
    public List<MenuEntry> menu() {
        List<MenuEntry> entries = new ArrayList<>();

        for (BotCommand command : commands.values()) {
            entries.add(new MenuEntry(command.name(), command.description()));
        }

        entries.add(new MenuEntry(HELP, phrases.helpHeading()));

        return entries;
    }

    private static boolean isCommand(Update update) {
        return update instanceof Update.MessageReceived received && received.message().command().isPresent();
    }

    /** A file dropped into the chat with no command: an attachment, and no slash in its caption. */
    private static boolean isFileWithoutCommand(Update update) {
        return update instanceof Update.MessageReceived received && received.message().hasAttachments()
                && received.message().command().isEmpty();
    }

    /** The file goes to the command that receives files, its caption — if any — as the arguments. */
    private void onFile(Update update) {
        IncomingMessage message = ((Update.MessageReceived) update).message();
        String          caption = message.caption() == null ? "" : message.caption().strip();

        invoke(message, fileReceiver.name(), caption);
    }

    private void onMessage(Update update) {
        IncomingMessage message = ((Update.MessageReceived) update).message();
        String          name    = message.command().orElseThrow().toLowerCase();

        if (START.equals(name)) {
            return;
        }

        invoke(message, name, message.commandArgument().orElse(""));
    }

    /**
     * A {@link CommandAction} pressed: runs its command exactly as if it had been typed in that chat.
     *
     * <p>⚠️ The callback is answered first and at once — the person's client shows a spinner until it is —
     * and the command's own reply arrives as a new message.
     */
    private void onAction(Update update) {
        Update.CallbackPressed pressed = (Update.CallbackPressed) update;
        String                 data    = pressed.data().substring(ACTION_PREFIX.length());
        int                    colon   = data.indexOf(':');

        try {
            gateway.answerCallback(new CallbackAnswer(pressed.queryId(), null, false));
        } catch (TelegramException exception) {
            LOGGER.debug("a button press could not be acknowledged", exception);
        }

        if (colon < 0 || pressed.message() == null) {
            return;
        }

        IncomingMessage typed = new IncomingMessage(
                pressed.message().chat(), pressed.message().messageId(), pressed.from(),
                "/" + data.substring(0, colon), null, Instant.now(), List.of(), null);

        invoke(typed, data.substring(0, colon), data.substring(colon + 1));
    }

    /** One way into a command, typed or pressed: who, what, may they — then run it off this thread. */
    private void invoke(IncomingMessage message, String name, String arguments) {
        Optional<String> subject = senders.subjectOf(message.from());

        if (subject.isEmpty()) {
            say(message, phrases.notBound());
            return;
        }

        BotCommand command = commands.get(name);

        if (command == null || HELP.equals(name)) {
            sayHelp(message);
            return;
        }

        if (!guard.permits(subject.get(), command.name())) {
            say(message, phrases.notPermitted());
            return;
        }

        CommandContext context = new CommandContext(message, subject.get(), command.name(), arguments, gateway, this);

        executor.execute(() -> run(command, context));
    }

    private void run(BotCommand command, CommandContext context) {
        try {
            command.handler().handle(context);
        } catch (Exception exception) {
            LOGGER.warn("/{} failed for subject {}", command.name(), context.subject(), exception);
            say(context.message(), phrases.failed().formatted(command.name()));
        }
    }

    SentMessage replyInPages(CommandContext context, String text) {
        List<String> split = ReplyPages.split(text);

        if (split.size() == 1) {
            return context.reply(split.getFirst());
        }

        String token = pages.keep(context.subject(), context.command(), split);

        return gateway.send(context.chat(), page(split, 0, token));
    }

    private void onPageTurn(Update update) {
        Update.CallbackPressed pressed = (Update.CallbackPressed) update;
        String[]               parts   = pressed.data().substring(PAGE_PREFIX.length()).split(":");

        try {
            Optional<ReplyPages.Stored> stored  = parts.length == 2 ? pages.find(parts[0]) : Optional.empty();
            Optional<String>            subject = senders.subjectOf(pressed.from());

            // ⚠️ Whose pages they are, and is that person still allowed: a forwarded button pressed by
            // somebody else, or a right withdrawn since, shows nothing.
            boolean allowed = stored.isPresent() && subject.isPresent()
                    && stored.get().subject().equals(subject.get())
                    && guard.permits(subject.get(), stored.get().command());

            if (!allowed || pressed.message() == null) {
                gateway.answerCallback(new CallbackAnswer(pressed.queryId(), phrases.pageExpired(), false));
                return;
            }

            List<String> split = stored.get().pages();
            int          index = Math.clamp(Integer.parseInt(parts[1]), 0, split.size() - 1);

            gateway.edit(pressed.message(), page(split, index, parts[0]));
            gateway.answerCallback(new CallbackAnswer(pressed.queryId(), null, false));
        } catch (TelegramException | NumberFormatException exception) {
            LOGGER.debug("a page turn could not be shown", exception);
        }
    }

    private static MessageDraft page(List<String> split, int index, String token) {
        List<InlineButton> buttons = new ArrayList<>();

        if (index > 0) {
            buttons.add(InlineButton.callback("‹", PAGE_PREFIX + token + ":" + (index - 1)));
        }

        if (index < split.size() - 1) {
            buttons.add(InlineButton.callback("›", PAGE_PREFIX + token + ":" + (index + 1)));
        }

        String text = split.get(index) + "\n\n" + (index + 1) + " / " + split.size();

        return MessageDraft.builder()
                           .text(text)
                           .markup(ReplyMarkup.InlineKeyboard.row(buttons.toArray(InlineButton[]::new)))
                           .withoutPreview()
                           .build();
    }

    /**
     * The list of commands, and a button for each one that needs no argument — so /help is a menu one tap
     * away from every command, not only a list to retype from.
     */
    private void sayHelp(IncomingMessage message) {
        StringBuilder      text    = new StringBuilder(phrases.helpHeading());
        List<InlineButton> buttons = new ArrayList<>();

        for (BotCommand command : commands.values()) {
            text.append('\n').append(command.usage());

            if (command.argumentHint() == null) {
                buttons.add(CommandAction.of("/" + command.name(), command.name()).button());
            }
        }

        MessageDraft.Builder draft = MessageDraft.builder().text(text.toString()).withoutPreview();

        if (!buttons.isEmpty()) {
            draft.markup(ReplyMarkup.InlineKeyboard.grid(2, buttons.toArray(InlineButton[]::new)));
        }

        try {
            gateway.send(message.chat(), draft.build());
        } catch (TelegramException exception) {
            LOGGER.debug("help could not be sent to {}", message.chat(), exception);
        }
    }

    private void say(IncomingMessage message, String text) {
        try {
            gateway.send(message.chat(), MessageDraft.text(text));
        } catch (TelegramException exception) {
            LOGGER.debug("a command reply could not be sent to {}", message.chat(), exception);
        }
    }
}
