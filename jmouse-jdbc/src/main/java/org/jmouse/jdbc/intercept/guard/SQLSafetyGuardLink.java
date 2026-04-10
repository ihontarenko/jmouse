package org.jmouse.jdbc.intercept.guard;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;
import org.jmouse.jdbc.intercept.JdbcOperation;

import java.util.Objects;

/**
 * Guard link that validates SQL before execution.
 *
 * <p>Applies safety rules such as multi-statement blocking,
 * DDL restriction, and UPDATE/DELETE without WHERE detection.</p>
 */
public final class SQLSafetyGuardLink implements Link<JdbcExecutionContext, JdbcCall<?>, Object> {

    private final SQLScanner     scanner;
    private final SQLGuardPolicy policy;

    /**
     * Creates a guard link with default {@link SQLScanner}.
     *
     * @param policy guard policy
     */
    public SQLSafetyGuardLink(SQLGuardPolicy policy) {
        this(policy, new SQLScanner());
    }

    /**
     * Creates a guard link with custom scanner.
     *
     * @param policy guard policy
     * @param scanner SQL scanner
     */
    public SQLSafetyGuardLink(SQLGuardPolicy policy, SQLScanner scanner) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.scanner = Objects.requireNonNull(scanner, "scanner");
    }

    /**
     * Applies SQL safety checks and delegates to the next link if valid.
     *
     * @param context execution context
     * @param call JDBC call
     * @param next next chain link
     * @return execution outcome
     * @throws SQLGuardViolationException if SQL violates configured policy
     */
    @Override
    public Outcome<Object> handle(JdbcExecutionContext context, JdbcCall<?> call, Chain<JdbcExecutionContext, JdbcCall<?>, Object> next) {
        // Optional: you may decide to guard only SQL-carrying calls
        String        sql       = call.sql();
        JdbcOperation operation = call.operation();

        // Rule 1: multi-statement (applies to everything)
        if (policy.multiStatement()) {
            SQLScan scan = scanner.scan(sql);
            if (scan.hasStatementSeparator()) {
                throw new SQLGuardViolationException("Blocked SQL (multi-statement is not allowed): " + sql);
            }
        }

        // Rule 2: DDL (applies to everything)
        if (policy.ddl()) {
            SQLScan scan = scanner.scan(sql);
            if (scan.isDDL()) {
                throw new SQLGuardViolationException("Blocked SQL (DDL is not allowed): " + sql);
            }
        }

        // Rule 3: UPDATE/DELETE without WHERE
        if (policy.udWithoutWhere()) {
            SQLScan scan = scanner.scan(sql);
            boolean isUD = (operation == JdbcOperation.UPDATE) || scan.isUD();
            if (isUD && !scan.hasWhere()) {
                throw new SQLGuardViolationException("Blocked SQL (UPDATE/DELETE without WHERE): " + sql);
            }
        }

        return next.proceed(context, call);
    }
}