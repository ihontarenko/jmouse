package org.jmouse.jdbc.intercept.guard;

public record SQLGuardPolicy(
        boolean multiStatement,
        boolean ddl,
        boolean udWithoutWhere
) {
    public static SQLGuardPolicy strict() {
        return new SQLGuardPolicy(true, true, true);
    }

    public static SQLGuardPolicy permissive() {
        return new SQLGuardPolicy(true, true, false);
    }
}