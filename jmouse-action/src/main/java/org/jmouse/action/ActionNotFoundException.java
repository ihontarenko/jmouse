package org.jmouse.action;

/**
 * Thrown when action handler cannot be resolved. 🔎
 */
public class ActionNotFoundException extends ActionException {

    public ActionNotFoundException(String actionName) {
        super("No action handler registered for action '%s'.".formatted(actionName));
    }

}
