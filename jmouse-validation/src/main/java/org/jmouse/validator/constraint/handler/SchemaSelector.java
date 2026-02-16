package org.jmouse.validator.constraint.handler;

import org.jmouse.validator.ValidationHints;

public interface SchemaSelector {

    /**
     * @param objectName binding object name ("userForm", "dynamicForm", ...)
     * @param hints optional hints (create/update/admin-defined/etc.)
     * @return schema name to use, or null if none
     */
    String select(String objectName, ValidationHints hints);

}
