package org.jmouse.validator;

import org.jmouse.core.i18n.LocalizableMessage;

public class ObjectError extends LocalizableMessage.Default {

    /**
     * Constructs a new {@code Default} instance of {@link LocalizableMessage}.
     *
     * @param key            the message key used for localization
     * @param defaultMessage the fallback message if localization is unavailable
     * @param arguments      optional arguments for message formatting
     */
    public ObjectError(String key, String defaultMessage, Object... arguments) {
        super(key, defaultMessage, arguments);
    }

}
