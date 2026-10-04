package org.jmouse.telegram.markup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * What appears under or instead of the keyboard when a message arrives.
 *
 * <p>⚠️ Every member of this hierarchy needs {@link org.jmouse.telegram.Capability#REPLY_MARKUP},
 * which a user-account transport does not have — buttons are a bot concept and the user API has no
 * equivalent. A gateway asked to attach one on behalf of a user identity refuses with
 * {@link org.jmouse.telegram.TelegramRefusal.CapabilityUnavailable} rather than dropping it silently,
 * because a notification whose buttons quietly vanished looks like a product defect.
 */
public sealed interface ReplyMarkup {

    /**
     * Buttons attached to the message itself.
     *
     * <p>The kind worth reaching for: it belongs to one message, it can be edited or removed later
     * without touching the person's keyboard, and it is how a notification carries an action.
     */
    record InlineKeyboard(List<List<InlineButton>> rows) implements ReplyMarkup {

        public InlineKeyboard {
            Objects.requireNonNull(rows, "rows");
            rows = rows.stream().map(List::copyOf).toList();
        }

        /** One row of buttons. */
        public static InlineKeyboard row(InlineButton... buttons) {
            return new InlineKeyboard(List.of(List.of(buttons)));
        }

        /** One button per row, stacked — what a list of choices usually wants. */
        public static InlineKeyboard stacked(InlineButton... buttons) {
            return new InlineKeyboard(Arrays.stream(buttons).map(List::of).toList());
        }

        /** Rows of at most {@code perRow} buttons each. */
        public static InlineKeyboard grid(int perRow, InlineButton... buttons) {
            if (perRow < 1) {
                throw new IllegalArgumentException("a row holds at least one button");
            }

            List<List<InlineButton>> rows    = new ArrayList<>();
            List<InlineButton>       current = new ArrayList<>();

            for (InlineButton button : buttons) {
                current.add(button);

                if (current.size() == perRow) {
                    rows.add(List.copyOf(current));
                    current.clear();
                }
            }

            if (!current.isEmpty()) {
                rows.add(List.copyOf(current));
            }

            return new InlineKeyboard(rows);
        }

        public boolean isEmpty() {
            return rows.isEmpty();
        }
    }

    /**
     * Buttons that replace the person's keyboard.
     *
     * <p>⚠️ Persistent and account-wide for that chat rather than attached to one message, which is
     * why it is the wrong tool for a notification: it changes the interface until something changes it
     * back. Pressing one sends its label as an ordinary message, so the bot receives text and not a
     * callback — meaning the label is part of the protocol and renaming it breaks the handler.
     */
    record ReplyKeyboard(List<List<String>> rows, boolean oneTime, boolean resize) implements ReplyMarkup {

        public ReplyKeyboard {
            Objects.requireNonNull(rows, "rows");
            rows = rows.stream().map(List::copyOf).toList();
        }

        public static ReplyKeyboard of(List<List<String>> rows) {
            return new ReplyKeyboard(rows, false, true);
        }
    }

    /** Takes a {@link ReplyKeyboard} away again. */
    record RemoveKeyboard() implements ReplyMarkup {
    }

    /** Opens the compose field as a reply to this message. */
    record ForceReply(String placeholder) implements ReplyMarkup {

        public static ForceReply plain() {
            return new ForceReply(null);
        }
    }
}
