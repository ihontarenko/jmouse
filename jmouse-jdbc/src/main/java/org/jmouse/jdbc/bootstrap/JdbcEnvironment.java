package org.jmouse.jdbc.bootstrap;

import org.jmouse.core.chain.Chain;
import org.jmouse.jdbc.dialect.DialectRegistry;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;

/**
 * Internal bootstrap state shared across factories.
 *
 * <p>Platform adapters and resolvers will populate this in stage 7.6.0.2+.</p>
 */
public record JdbcEnvironment(
        JdbcConfig config,
        DialectRegistry dialectRegistry,
        Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> chainBuilder
) { }
