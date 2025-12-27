package org.jmouse.jdbc.parameters;

public record SQLCompiled(SQLPlan plan) {

    public String compiled() {
        return plan.compiled();
    }

}