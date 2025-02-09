package org.jmouse.validator;

public class PropertyConstraint<T> {

    private final String        property;
    private final Constraint<T> constraint;

    public PropertyConstraint(String property, Constraint<T> constraint) {
        this.property = property;
        this.constraint = constraint;
    }

    public String getProperty() {
        return property;
    }

    public Constraint<T> getConstraint() {
        return constraint;
    }

}
