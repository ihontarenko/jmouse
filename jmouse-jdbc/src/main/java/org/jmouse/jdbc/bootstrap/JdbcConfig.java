package org.jmouse.jdbc.bootstrap;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable JDBC client configuration.
 */
public record JdbcConfig(
        String name,
        String dialectId,
        boolean interceptorsEnabled,
        QueryTuning queryTuning,
        DialectResolution dialectResolution
) {

    public JdbcConfig {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(dialectId, "dialectId");
        Objects.requireNonNull(queryTuning, "queryTuning");
        Objects.requireNonNull(dialectResolution, "dialectResolution");
    }

    public static JdbcConfig defaults() {
        return new JdbcConfig(
                "jdbc",
                "ansi",
                true,
                QueryTuning.defaults(),
                DialectResolution.defaults()
        );
    }

    public JdbcConfig withDialectId(String dialectId) {
        return new JdbcConfig(name, dialectId, interceptorsEnabled, queryTuning, dialectResolution);
    }

    public JdbcConfig withInterceptorsEnabled(boolean enabled) {
        return new JdbcConfig(name, dialectId, enabled, queryTuning, dialectResolution);
    }

    /**
     * Fine-tuning for statements and queries (execution-level, not dialect-level).
     */
    public record QueryTuning(
            Optional<Integer> fetchSize,
            Optional<Integer> maxRows,
            Optional<Duration> queryTimeout
    ) {
        public static QueryTuning defaults() {
            return new QueryTuning(Optional.empty(), Optional.empty(), Optional.empty());
        }
    }

    /**
     * Controls how dialect is resolved (fixed/config vs metadata).
     *
     * <p>Actual resolver implementations appear in stage 7.6.0.4.</p>
     */
    public record DialectResolution(
            Strategy strategy,
            boolean allowMetadataFallback
    ) {
        public DialectResolution {
            if (strategy == null) {
                throw new IllegalArgumentException("Strategy cannot be null");
            }
        }

        public static DialectResolution defaults() {
            return new DialectResolution(Strategy.FIXED, true);
        }

        public enum Strategy {
            FIXED,
            METADATA,
            COMPOSITE
        }
    }
}
