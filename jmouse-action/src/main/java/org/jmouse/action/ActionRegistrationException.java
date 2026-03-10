package org.jmouse.action;

/**
 * Thrown when action registration fails. 🚫
 */
public class ActionRegistrationException extends ActionException {

    public ActionRegistrationException(String message) {
        super(message);
    }

}
