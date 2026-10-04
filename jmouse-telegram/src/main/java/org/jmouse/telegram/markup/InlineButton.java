package org.jmouse.telegram.markup;

import java.util.Objects;

/**
 * One button under a message.
 *
 * <p>A button is a label plus exactly one action, which is why the action is a sealed type rather
 * than four nullable fields: Telegram refuses a button carrying two, and a record with four
 * {@code null}s cannot express "exactly one" at all.
 *
 * @param text   what the button says
 * @param action what pressing it does
 */
public record InlineButton(String text, Action action) {

    /**
     * ⚠️ Telegram's hard ceiling on callback data, in <strong>bytes</strong> rather than characters.
     * A Cyrillic label packed into callback data reaches it in 32 characters, not 64.
     */
    public static final int CALLBACK_DATA_LIMIT = 64;

    public InlineButton {
        Objects.requireNonNull(text, "button text");
        Objects.requireNonNull(action, "button action");
    }

    /**
     * A button that sends {@code data} back to the bot as a callback query.
     *
     * <p>⚠️ The 64-byte ceiling is the design constraint nobody expects. Callback data is a
     * <em>key</em>, not a payload: put an identifier in it and look the rest up. A serialised object
     * will fit in testing and fail on the first record with a long name.
     */
    public static InlineButton callback(String text, String data) {
        return new InlineButton(text, new Action.Callback(data));
    }

    /** A button that opens a link. It sends nothing back, so the bot never learns it was pressed. */
    public static InlineButton url(String text, String url) {
        return new InlineButton(text, new Action.OpenUrl(url));
    }

    /** What pressing a button does. */
    public sealed interface Action {

        /** Sends {@code data} back to the bot — see {@link #CALLBACK_DATA_LIMIT}. */
        record Callback(String data) implements Action {

            public Callback {
                Objects.requireNonNull(data, "callback data");

                int bytes = data.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;

                if (bytes > CALLBACK_DATA_LIMIT) {
                    throw new IllegalArgumentException(
                            "callback data is %d bytes; Telegram allows %d. Send a key and look the rest up."
                                    .formatted(bytes, CALLBACK_DATA_LIMIT));
                }
            }
        }

        /** Opens a URL in the client. */
        record OpenUrl(String url) implements Action {

            public OpenUrl {
                Objects.requireNonNull(url, "url");
            }
        }
    }
}
