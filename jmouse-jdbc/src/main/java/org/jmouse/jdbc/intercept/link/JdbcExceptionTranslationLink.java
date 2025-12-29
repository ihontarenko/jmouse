package org.jmouse.jdbc.intercept.link;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.core.proxy.Bubble;
import org.jmouse.jdbc.exception.DataAccessException;
import org.jmouse.jdbc.exception.SQLExceptionTranslator;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;

import java.sql.SQLException;

/**
 * Interceptor link responsible for translating {@link SQLException SQLExceptions}
 * into jMouse {@link DataAccessException} hierarchy.
 * <p>
 * {@code JdbcExceptionTranslationLink} acts as a <b>boundary adapter</b> between
 * low-level JDBC exceptions and higher-level, framework-specific data access
 * exceptions.
 *
 * <h3>Exception handling strategy</h3>
 * <ul>
 *     <li>Delegates execution to the next link in the chain</li>
 *     <li>Intercepts thrown exceptions</li>
 *     <li>Extracts {@link SQLException} directly or from a {@link Bubble}</li>
 *     <li>Delegates translation to a {@link SQLExceptionTranslator}</li>
 * </ul>
 *
 * <h3>Translation inputs</h3>
 * <p>
 * The translator receives:
 * <ul>
 *     <li>operation name ({@link JdbcCall#operation()})</li>
 *     <li>SQL string ({@link JdbcCall#sql()})</li>
 *     <li>original {@link SQLException}</li>
 * </ul>
 *
 * <h3>Fallback behavior</h3>
 * <p>
 * If the exception is not (and does not wrap) an {@link SQLException},
 * it is wrapped into a generic {@link DataAccessException}.
 *
 * <h3>Typical chain position</h3>
 * <pre>{@code
 * builder
 *   .add(new JdbcExceptionTranslationLink(translator))
 *   .add(new JdbcCallExecutorLink());
 * }</pre>
 *
 * @author jMouse
 */
public final class JdbcExceptionTranslationLink
        implements Link<JdbcExecutionContext, JdbcCall<?>, Object> {

    /**
     * Strategy for translating {@link SQLException} into {@link DataAccessException}.
     */
    private final SQLExceptionTranslator translator;

    /**
     * Creates a new exception translation link.
     *
     * @param translator SQL exception translator
     */
    public JdbcExceptionTranslationLink(SQLExceptionTranslator translator) {
        this.translator = translator;
    }

    /**
     * Executes the next chain segment and translates any encountered SQL exceptions.
     *
     * @param context execution context
     * @param call    JDBC call descriptor
     * @param next    next chain segment
     * @return outcome produced by downstream execution
     * @throws DataAccessException translated data access exception
     */
    @Override
    public Outcome<Object> handle(
            JdbcExecutionContext context,
            JdbcCall<?> call,
            Chain<JdbcExecutionContext, JdbcCall<?>, Object> next
    ) {
        try {
            return next.proceed(context, call);
        } catch (Throwable exception) {
            SQLException sqlException = null;

            if (exception instanceof SQLException) {
                sqlException = (SQLException) exception;
            }

            if (exception instanceof Bubble bubble && bubble.getCause() instanceof SQLException) {
                sqlException = (SQLException) bubble.getCause();
            }

            if (sqlException != null) {
                throw translator.translate(
                        call.operation().name(),
                        call.sql(),
                        sqlException
                );
            }

            throw new DataAccessException("SQL Exception", exception);
        }
    }
}
