package org.jmouse.core;

import java.util.stream.Collectors;

/**
 * ANSI escape codes for terminal styling 🎨
 *
 * <p>Provides color, background, and text style codes for ANSI-compatible consoles.</p>
 *
 * <p>Supports:</p>
 * <ul>
 *     <li>🎨 Basic colors</li>
 *     <li>💪 Bold styles</li>
 *     <li>🖊 Underlined text</li>
 *     <li>🧱 Background colors</li>
 *     <li>✨ Bright variants</li>
 * </ul>
 */
public enum AnsiColors {

    RESET("\u001B[0m"), // 🧼 Reset all styles

    // ─────────────────────────────
    // 🎨 REGULAR COLORS
    // ─────────────────────────────
    BLACK("\u001B[0;30m"),   // ⚫ Black
    RED("\u001B[0;31m"),     // 🔴 Red
    GREEN("\u001B[0;32m"),   // 🟢 Green
    YELLOW("\u001B[0;33m"),  // 🟡 Yellow
    BLUE("\u001B[0;34m"),    // 🔵 Blue
    PURPLE("\u001B[0;35m"),  // 🟣 Purple
    CYAN("\u001B[0;36m"),    // 🩵 Cyan
    WHITE("\u001B[0;37m"),   // ⚪ White

    // ─────────────────────────────
    // 💪 BOLD
    // ─────────────────────────────
    BLACK_BOLD("\u001B[1;30m"),   // ⚫ Bold
    RED_BOLD("\u001B[1;31m"),     // 🔴 Bold
    GREEN_BOLD("\u001B[1;32m"),   // 🟢 Bold
    YELLOW_BOLD("\u001B[1;33m"),  // 🟡 Bold
    BLUE_BOLD("\u001B[1;34m"),    // 🔵 Bold
    PURPLE_BOLD("\u001B[1;35m"),  // 🟣 Bold
    CYAN_BOLD("\u001B[1;36m"),    // 🩵 Bold
    WHITE_BOLD("\u001B[1;37m"),   // ⚪ Bold

    // ─────────────────────────────
    // 🖊 UNDERLINE
    // ─────────────────────────────
    BLACK_UNDERLINED("\u001B[4;30m"),   // ⚫ Underlined
    RED_UNDERLINED("\u001B[4;31m"),     // 🔴 Underlined
    GREEN_UNDERLINED("\u001B[4;32m"),   // 🟢 Underlined
    YELLOW_UNDERLINED("\u001B[4;33m"),  // 🟡 Underlined
    BLUE_UNDERLINED("\u001B[4;34m"),    // 🔵 Underlined
    PURPLE_UNDERLINED("\u001B[4;35m"),  // 🟣 Underlined
    CYAN_UNDERLINED("\u001B[4;36m"),    // 🩵 Underlined
    WHITE_UNDERLINED("\u001B[4;37m"),   // ⚪ Underlined

    // ─────────────────────────────
    // 🧱 BACKGROUND
    // ─────────────────────────────
    BLACK_BG("\u001B[40m"),   // ⚫ BG
    RED_BG("\u001B[41m"),     // 🔴 BG
    GREEN_BG("\u001B[42m"),   // 🟢 BG
    YELLOW_BG("\u001B[43m"),  // 🟡 BG
    BLUE_BG("\u001B[44m"),    // 🔵 BG
    PURPLE_BG("\u001B[45m"),  // 🟣 BG
    CYAN_BG("\u001B[46m"),    // 🩵 BG
    WHITE_BG("\u001B[47m"),   // ⚪ BG

    // ─────────────────────────────
    // ✨ HIGH INTENSITY
    // ─────────────────────────────
    BLACK_BRIGHT("\u001B[0;90m"),   // ⚫ Bright
    RED_BRIGHT("\u001B[0;91m"),     // 🔴 Bright
    GREEN_BRIGHT("\u001B[0;92m"),   // 🟢 Bright
    YELLOW_BRIGHT("\u001B[0;93m"),  // 🟡 Bright
    BLUE_BRIGHT("\u001B[0;94m"),    // 🔵 Bright
    PURPLE_BRIGHT("\u001B[0;95m"),  // 🟣 Bright
    CYAN_BRIGHT("\u001B[0;96m"),    // 🩵 Bright
    WHITE_BRIGHT("\u001B[0;97m"),   // ⚪ Bright

    // ─────────────────────────────
    // 💪✨ BOLD HIGH INTENSITY
    // ─────────────────────────────
    BLACK_BOLD_BRIGHT("\u001B[1;90m"),   // ⚫ Bold bright
    RED_BOLD_BRIGHT("\u001B[1;91m"),     // 🔴 Bold bright
    GREEN_BOLD_BRIGHT("\u001B[1;92m"),   // 🟢 Bold bright
    YELLOW_BOLD_BRIGHT("\u001B[1;93m"),  // 🟡 Bold bright
    BLUE_BOLD_BRIGHT("\u001B[1;94m"),    // 🔵 Bold bright
    PURPLE_BOLD_BRIGHT("\u001B[1;95m"),  // 🟣 Bold bright
    CYAN_BOLD_BRIGHT("\u001B[1;96m"),    // 🩵 Bold bright
    WHITE_BOLD_BRIGHT("\u001B[1;97m"),   // ⚪ Bold bright

    // ─────────────────────────────
    // 🧱✨ HIGH INTENSITY BACKGROUND
    // ─────────────────────────────
    BLACK_BG_BRIGHT("\u001B[100m"),  // ⚫ BG bright
    RED_BG_BRIGHT("\u001B[101m"),    // 🔴 BG bright
    GREEN_BG_BRIGHT("\u001B[102m"),  // 🟢 BG bright
    YELLOW_BG_BRIGHT("\u001B[103m"), // 🟡 BG bright
    BLUE_BG_BRIGHT("\u001B[104m"),   // 🔵 BG bright
    PURPLE_BG_BRIGHT("\u001B[105m"), // 🟣 BG bright
    CYAN_BG_BRIGHT("\u001B[106m"),   // 🩵 BG bright
    WHITE_BG_BRIGHT("\u001B[107m");  // ⚪ BG bright

    private static final PlaceholderReplacer REPLACER = new StandardPlaceholderReplacer();

    private final String value;

    /**
     * Creates ANSI color constant 🎨
     *
     * @param color ANSI escape code
     */
    AnsiColors(String color) {
        this.value = color;
    }

    /** @return ANSI escape sequence */
    public String getValue() {
        return value;
    }

    /** @return enum name */
    public String getName() {
        return name();
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Replaces placeholders with ANSI codes 🎯
     *
     * @param string input template
     * @param arguments format arguments
     * @return formatted colored string
     */
    public static String colorize(String string, Object... arguments) {
        return REPLACER.replace(string.formatted(arguments), placeholderResolver());
    }

    private static PlaceholderResolver placeholderResolver() {
        return AnsiColors::resolve;
    }

    /**
     * Resolves color tokens (supports pipe-separated values) 🔍
     */
    private static String resolve(String string, String defaultValue) {
        return Streamable.of(string.split("\\|"))
                .map(AnsiColors::valueOf)
                .map(AnsiColors::getValue)
                .stream()
                .collect(Collectors.joining());
    }
}