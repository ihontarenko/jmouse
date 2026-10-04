package org.jmouse.telegram.update;

import java.util.Objects;

/**
 * Who an update came from.
 *
 * @param id           Telegram's own id for this person. ⚠️ The only stable handle there is — a
 *                     username can be changed or dropped, and names change freely
 * @param firstName    as they have it set
 * @param lastName     or {@code null}; Telegram does not require one
 * @param username     the {@code @name} without its {@code @}, or {@code null}. ⚠️ Optional, and
 *                     not an identifier — treating it as one is how a binding follows the wrong person
 *                     after a rename
 * @param languageCode an IETF tag such as {@code uk} or {@code en}, or {@code null}
 * @param bot          whether this is another bot
 */
public record TelegramUser(
        long    id,
        String  firstName,
        String  lastName,
        String  username,
        String  languageCode,
        boolean bot
) {

    public TelegramUser {
        Objects.requireNonNull(firstName, "first name");
    }

    /**
     * ⚠️ This is where a person's language is learned, and there is no other opportunity.
     *
     * <p>Telegram reports it on the update and nowhere else — there is no endpoint to ask later. A
     * binding that does not capture it at the moment somebody first writes to the bot has lost it, and
     * every notification to that person afterwards is in whatever language the product guessed.
     */
    public boolean hasLanguage() {
        return languageCode != null && !languageCode.isBlank();
    }

    /** First and last name joined, for a log line or an administration listing. */
    public String displayName() {
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }

        return firstName + " " + lastName;
    }
}
