package org.jmouse.query.store.exception;

/**
 * 🚫 A saved query nobody kept, or nobody kept any more.
 *
 * <p>Its own type rather than an empty result, because the two callers want opposite things: a screen
 * listing views wants "there are none", and a screen opening one by its identifier wants to say which
 * one is gone. A method that returned empty for both would make every caller invent this sentence.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class SavedQueryNotFoundException extends QueryStoreException {

    public SavedQueryNotFoundException(String identifier) {
        super("there is no saved query '%s'".formatted(identifier));
    }
}
