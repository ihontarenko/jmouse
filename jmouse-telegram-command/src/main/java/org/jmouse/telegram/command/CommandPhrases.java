package org.jmouse.telegram.command;

/**
 * The sentences the router says on its own — replace them to speak the product's language.
 *
 * @param notBound     to a sender with no binding; ⚠️ it must not say whether the command exists
 * @param notPermitted when the guard refuses
 * @param failed       when a handler threw; {@code %s} is the command
 * @param helpHeading  the first line of {@code /help}
 * @param pageExpired  when a ‹ › button outlived its pages
 */
public record CommandPhrases(String notBound, String notPermitted, String failed, String helpHeading, String pageExpired) {

    public static CommandPhrases english() {
        return new CommandPhrases(
                "This chat is not linked to an account yet. Open the product, choose to connect Telegram, and follow the link it gives you.",
                "You may not use this command.",
                "/%s did not work this time. Try again in a moment.",
                "What I can do:",
                "This answer has expired. Run the command again.");
    }
}
