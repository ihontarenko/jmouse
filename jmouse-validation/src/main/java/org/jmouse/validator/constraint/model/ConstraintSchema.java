package org.jmouse.validator.constraint.model;

import java.util.List;

public interface ConstraintSchema {

    String name();

    List<? extends FieldRules> fields();

}
