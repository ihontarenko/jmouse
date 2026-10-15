package org.jmouse.telegram.user;

import org.jmouse.telegram.update.TelegramUser;

/**
 * Where a person's login stands — explicit states rather than a method that blocks, because two of the
 * steps wait for somebody to type something.
 *
 * <pre>
 *   WaitingForPhoneNumber ─phone→ WaitingForCode ─code→ (WaitingForPassword ─password→) Ready
 * </pre>
 *
 * <p>⚠️ The code is delivered <em>inside Telegram itself</em>, to the person's other devices — not by SMS
 * unless Telegram falls back to it. A screen asking for it should say so.
 */
public sealed interface LoginState {

    /** One sentence for a screen: what the person has to do next. */
    String prompt();

    record WaitingForPhoneNumber() implements LoginState {

        @Override
        public String prompt() {
            return "Enter the phone number of the Telegram account, in international format.";
        }
    }

    /**
     * @param phoneNumber where the code went, as the person typed it
     * @param codeLength  how many digits to expect, or 0 when Telegram did not say
     */
    record WaitingForCode(String phoneNumber, int codeLength) implements LoginState {

        @Override
        public String prompt() {
            return "Enter the code Telegram sent — it arrives in Telegram on your other devices.";
        }
    }

    /** @param hint the two-step password's hint, or {@code null} */
    record WaitingForPassword(String hint) implements LoginState {

        @Override
        public String prompt() {
            return hint == null || hint.isBlank()
                    ? "Enter the account's two-step verification password."
                    : "Enter the account's two-step verification password (hint: " + hint + ").";
        }
    }

    /** @param self the account now signed in */
    record Ready(TelegramUser self) implements LoginState {

        @Override
        public String prompt() {
            return "Signed in as " + (self == null ? "the account" : self.displayName()) + ".";
        }
    }

    /** Logged out, or the session was revoked from another device — a new login starts over. */
    record Closed() implements LoginState {

        @Override
        public String prompt() {
            return "Signed out. Start again with the phone number.";
        }
    }

    default boolean isReady() {
        return this instanceof Ready;
    }
}
