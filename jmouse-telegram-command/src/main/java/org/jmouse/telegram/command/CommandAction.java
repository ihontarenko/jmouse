package org.jmouse.telegram.command;

import org.jmouse.telegram.markup.InlineButton;

import java.nio.charset.StandardCharsets;

/**
 * A button under an answer that runs a command — "Open" beside a document runs {@code /read <id>}.
 *
 * <p>⚠️ <strong>Pressing it is exactly typing it.</strong> The press goes through the same sender
 * resolution and the same {@link CommandGuard} as a typed command, so a forwarded message's buttons do
 * nothing for somebody who could not have typed the command themselves.
 *
 * @param label    what the button says
 * @param command  the command it runs, without the slash
 * @param argument what follows the command, or empty — ⚠️ the whole callback must fit Telegram's 64 bytes,
 *                 so pass an identifier, never a sentence
 */
public record CommandAction(String label, String command, String argument) {

    public CommandAction {
        if (label == null || label.isBlank() || command == null || command.isBlank()) {
            throw new IllegalArgumentException("An action needs a label and the command it runs");
        }

        argument = argument == null ? "" : argument.strip();

        if (callbackData(command, argument).getBytes(StandardCharsets.UTF_8).length > InlineButton.CALLBACK_DATA_LIMIT) {
            throw new IllegalArgumentException(
                    "/%s %s does not fit in a button's %d bytes — pass an identifier".formatted(
                            command, argument, InlineButton.CALLBACK_DATA_LIMIT));
        }
    }

    public static CommandAction of(String label, String command) {
        return new CommandAction(label, command, "");
    }

    InlineButton button() {
        return InlineButton.callback(label, callbackData(command, argument));
    }

    static String callbackData(String command, String argument) {
        return CommandRouter.ACTION_PREFIX + command + ":" + argument;
    }
}
