package org.jmouse.telegram.command;

import java.util.regex.Pattern;

/**
 * One {@code /command} a product offers through its bot.
 *
 * <pre>{@code
 * new BotCommand("search", "Find documents", "<words>", context -> {
 *     List<Hit> hits = search.find(context.subject(), context.arguments());   // AS the bound person
 *     context.replyLong(render(hits));
 * });
 * }</pre>
 *
 * @param name         without the slash — lowercase letters, digits and underscores, at most 32, which is
 *                     what Telegram accepts in a command menu
 * @param description  one line for {@code /help}
 * @param argumentHint what follows the command, for {@code /help} — {@code "<words>"} — or {@code null}
 * @param handler      what it does
 * @param receivesFiles whether a file sent with no command at all comes here — see {@link #receivingFiles()}
 */
public record BotCommand(String name, String description, String argumentHint, CommandHandler handler,
                         boolean receivesFiles) {

    private static final Pattern NAME = Pattern.compile("[a-z0-9_]{1,32}");

    public BotCommand {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "A command name is 1–32 lowercase letters, digits or underscores, without the slash: " + name);
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("A command needs a one-line description for /help");
        }

        if (handler == null) {
            throw new IllegalArgumentException("A command needs a handler");
        }
    }

    public BotCommand(String name, String description, CommandHandler handler) {
        this(name, description, null, handler, false);
    }

    public BotCommand(String name, String description, String argumentHint, CommandHandler handler) {
        this(name, description, argumentHint, handler, false);
    }

    /**
     * The same command, also receiving a file sent with no command: a person drops a PDF into the chat and it
     * arrives here, its caption as the arguments. ⚠️ At most one command of a router may say this — a file with
     * no command has nowhere else to say where it was meant to go.
     */
    public BotCommand receivingFiles() {
        return new BotCommand(name, description, argumentHint, handler, true);
    }

    /** How {@code /help} lists it: {@code /search <words> — Find documents}. */
    public String usage() {
        return "/" + name + (argumentHint == null ? "" : " " + argumentHint) + " — " + description;
    }
}
