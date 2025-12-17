package org.jmouse.jdbc.bootstrap;

import org.jmouse.core.chain.Chain;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;
import org.jmouse.jdbc.intercept.link.SqlLoggingLink;
import org.jmouse.jdbc.intercept.link.TimingLink;

/**
 * Default chain presets (opt-in by config flags).
 *
 * <p>Keep presets small and composable. Do not hardcode platform specifics here.</p>
 */
public final class JdbcPresets {

    private JdbcPresets() {}

    public static Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> defaultChain(JdbcConfig config) {
        Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder = Chain.builder();

        if (config.interceptorsEnabled()) {
            builder.add(new SqlLoggingLink());
            builder.add(new TimingLink(50));
        }

        return builder;
    }
}
