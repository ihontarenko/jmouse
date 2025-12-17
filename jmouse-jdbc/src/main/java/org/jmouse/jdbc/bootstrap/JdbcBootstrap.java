package org.jmouse.jdbc.bootstrap;

import org.jmouse.core.chain.Chain;
import org.jmouse.jdbc.bind.MissingParameterPolicy;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.connection.TransactionAwareConnectionProvider;
import org.jmouse.jdbc.core.*;
import org.jmouse.jdbc.dialect.*;
import org.jmouse.jdbc.dialect.meta.DatabaseMetaDialectResolver;
import org.jmouse.jdbc.dialect.meta.DialectCatalog;
import org.jmouse.jdbc.intercept.JdbcCall;
import org.jmouse.jdbc.intercept.JdbcChainFactory;
import org.jmouse.jdbc.intercept.JdbcExecutionContext;
import org.jmouse.jdbc.platform.DialectInputs;
import org.jmouse.jdbc.platform.JdbcPlatform;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bootstrap factory for wiring a fully working JDBC client.
 */
public final class JdbcBootstrap {

    private JdbcBootstrap() {}

    public static JdbcClient create(JdbcConfig config, JdbcPlatform platform) {
        return create(config, platform, Dialects.defaults(), JdbcPresets.defaultChain(config));
    }

    public static JdbcClient create(
            JdbcConfig configuration,
            JdbcPlatform platform,
            DialectRegistry dialectRegistry,
            Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> chainBuilder
    ) {
        // 1) Platform connection provider (raw)
        ConnectionProvider rawProvider = platform.connectionProvider();

        // 2) TX-aware wrapper
        ConnectionProvider txAwareProvider = new TransactionAwareConnectionProvider(rawProvider);

        // 3) Resolve dialect id and dialect instance
        DialectResolver resolver = dialectResolver(configuration, platform, rawProvider);
        String dialectId = resolver.resolveDialectId();
        SqlDialect dialect = dialectRegistry.get(dialectId);

        // 4) Core executor
        JdbcExecutor core = new DefaultJdbcExecutor(txAwareProvider);

        // 5) Chain -> with fallback (SQLException -> JdbcAccessException)
        Chain<JdbcExecutionContext, JdbcCall<?>, Object> chain = JdbcChainFactory.withFallback(chainBuilder);

        // 6) Chain executor with context injection (dialect)
        JdbcExecutor executor = new ChainedJdbcExecutor(core, chain) {
            @Override
            protected JdbcExecutionContext newContext() {
                return new JdbcExecutionContext(core, dialect);
            }
        };

        // 7) Templates
        JdbcTemplate jdbc = new JdbcTemplate(executor);
        NamedJdbcTemplate named = new NamedJdbcTemplate(jdbc, MissingParameterPolicy.FAIL_FAST);

        return new JdbcClient(jdbc, named, executor, dialect);
    }

    private static DialectResolver dialectResolver(JdbcConfig config, JdbcPlatform platform, ConnectionProvider rawProvider) {
        DialectInputs inputs = platform.dialectInputs();

        String fallback = config.dialectId(); // always present in config

        JdbcConfig.DialectResolution resolution = config.dialectResolution();

        return switch (resolution.strategy()) {
            case FIXED -> new FixedDialectResolver(
                    inputs.configuredDialectId().orElse(fallback)
            );

            case METADATA -> {
                if (!inputs.allowMetadata() || !inputs.metadataAvailable()) {
                    yield new FixedDialectResolver(inputs.configuredDialectId().orElse(fallback));
                }
                yield new DatabaseMetaDialectResolver(
                        rawProvider,
                        DialectCatalog.defaults(),
                        inputs.configuredDialectId().orElse(fallback)
                );
            }

            case COMPOSITE -> {
                List<DialectResolver> resolvers = new ArrayList<>();

                // config first (if present)
                inputs.configuredDialectId().ifPresent(id -> resolvers.add(new FixedDialectResolver(id)));

                // metadata second (if allowed)
                if (inputs.allowMetadata() && inputs.metadataAvailable() && resolution.allowMetadataFallback()) {
                    resolvers.add(new DatabaseMetaDialectResolver(rawProvider, DialectCatalog.defaults(), fallback));
                }

                yield new CompositeDialectResolver(resolvers, fallback);
            }
        };
    }
}
