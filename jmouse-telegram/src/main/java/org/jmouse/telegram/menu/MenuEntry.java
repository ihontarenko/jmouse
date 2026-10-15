package org.jmouse.telegram.menu;

import java.util.regex.Pattern;

/**
 * One line of a bot's command menu — what Telegram shows behind the "Menu" button and when somebody types
 * {@code /}.
 *
 * @param command     without the slash — 1–32 lowercase letters, digits or underscores
 * @param description 1–256 characters, one line
 */
public record MenuEntry(String command, String description) {

    private static final Pattern COMMAND = Pattern.compile("[a-z0-9_]{1,32}");

    public MenuEntry {
        if (command == null || !COMMAND.matcher(command).matches()) {
            throw new IllegalArgumentException("A menu command is 1–32 lowercase letters, digits or underscores: " + command);
        }

        if (description == null || description.isBlank() || description.length() > 256) {
            throw new IllegalArgumentException("A menu entry needs a description of 1–256 characters");
        }
    }
}
