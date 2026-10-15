package org.jmouse.scheduler.jdbc;

import org.jmouse.scheduler.MisfirePolicy;
import org.jmouse.scheduler.OccurrenceOutcome;
import org.jmouse.scheduler.OccurrenceRecord;
import org.jmouse.scheduler.Schedule;
import org.jmouse.scheduler.Trigger;
import org.jmouse.scheduler.TriggerId;
import org.jmouse.scheduler.spi.ScheduledTrigger;
import org.jmouse.scheduler.spi.TriggerLease;
import org.jmouse.scheduler.spi.TriggerStore;
import org.jmouse.scheduler.spi.TriggerStoreException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@link TriggerStore} over plain JDBC — PostgreSQL, MySQL/MariaDB, and H2 for tests.
 *
 * <h2>The claim and the confirmation are one statement each</h2>
 *
 * <pre>{@code
 * -- claim: take the lease
 * UPDATE jmouse_scheduler_triggers SET planned_at = ?, lease_expires_at = <database now + lease>, version = version + 1
 *  WHERE trigger_id = ? AND version = ? AND (lease_expires_at IS NULL OR lease_expires_at <= <database now>)
 * -- confirm: advance and release
 * UPDATE jmouse_scheduler_triggers SET next_fire_at = ?, planned_at = NULL, lease_expires_at = NULL, version = version + 1
 *  WHERE trigger_id = ? AND version = <the lease's version>
 * }</pre>
 *
 * <p>Every database serialises writes to one row, so exactly one of any number of processes
 * presenting the same version sees an update count of one. No lock is held between polls.
 *
 * <p>⚠️ <strong>Lease time is the database's time.</strong> {@code <database now>} is read from the
 * database itself ({@link JdbcTriggerSchema.Dialect#currentTimeQuery()}) immediately before each claim
 * and due-query, so a runner whose machine clock is wrong can neither steal a live lease nor hold an
 * expired one. Only {@code next_fire_at} and {@code planned_at} — schedule time — come from the
 * runners' clock.
 *
 * <h2>Plain JDBC, not jmouse-jdbc</h2>
 *
 * <p>jmouse-jdbc brings its own context, expression language and transaction manager. A scheduler
 * embedded in a Spring application must use the application's {@link DataSource} and nothing else,
 * and every statement here is a single auto-committed statement, so there is nothing for a
 * transaction manager to do. When the {@code DataSource} is transaction-aware, a call made inside a
 * product's transaction joins it.
 *
 * <p>The tables are described by {@link JdbcTriggerSchema}. Every instant is stored as epoch
 * milliseconds.
 */
public final class JdbcTriggerStore implements TriggerStore {

    private static final String TRIGGERS = "jmouse_scheduler_triggers";
    private static final String OCCURRENCES = "jmouse_scheduler_occurrences";

    private static final String TRIGGER_COLUMNS = "trigger_id, handler_key, parameters, schedule_kind, "
            + "schedule_expression, schedule_zone, schedule_period_milliseconds, schedule_instant, misfire_policy, "
            + "catch_up_limit, gate_key, enabled, next_fire_at, version, planned_at";

    private static final String UPDATE_DEFINITION = "UPDATE " + TRIGGERS + " SET handler_key = ?, parameters = ?, "
            + "schedule_kind = ?, schedule_expression = ?, schedule_zone = ?, schedule_period_milliseconds = ?, "
            + "schedule_instant = ?, misfire_policy = ?, catch_up_limit = ?, gate_key = ?, enabled = ?, "
            + "next_fire_at = ?, planned_at = NULL, lease_expires_at = NULL, version = version + 1, updated_at = ? "
            + "WHERE trigger_id = ?";

    private static final String INSERT_DEFINITION = "INSERT INTO " + TRIGGERS + " (handler_key, parameters, "
            + "schedule_kind, schedule_expression, schedule_zone, schedule_period_milliseconds, schedule_instant, "
            + "misfire_policy, catch_up_limit, gate_key, enabled, next_fire_at, version, updated_at, trigger_id) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)";

    private static final String SELECT_ONE = "SELECT " + TRIGGER_COLUMNS + " FROM " + TRIGGERS + " WHERE trigger_id = ?";

    private static final String SELECT_ALL = "SELECT " + TRIGGER_COLUMNS + " FROM " + TRIGGERS + " ORDER BY trigger_id";

    private static final String SELECT_DUE = "SELECT " + TRIGGER_COLUMNS + " FROM " + TRIGGERS
            + " WHERE enabled = ? AND next_fire_at IS NOT NULL AND next_fire_at <= ?"
            + " AND (lease_expires_at IS NULL OR lease_expires_at <= ?) ORDER BY next_fire_at";

    private static final String CLAIM = "UPDATE " + TRIGGERS + " SET planned_at = ?, lease_expires_at = ?, "
            + "version = version + 1, updated_at = ? WHERE trigger_id = ? AND version = ?"
            + " AND (lease_expires_at IS NULL OR lease_expires_at <= ?)";

    private static final String CONFIRM = "UPDATE " + TRIGGERS + " SET next_fire_at = ?, planned_at = NULL, "
            + "lease_expires_at = NULL, version = version + 1, "
            + "updated_at = ? WHERE trigger_id = ? AND version = ?";

    private static final String DELETE_TRIGGER = "DELETE FROM " + TRIGGERS + " WHERE trigger_id = ?";

    private static final String DELETE_HISTORY = "DELETE FROM " + OCCURRENCES + " WHERE trigger_id = ?";

    private static final String INSERT_OCCURRENCE = "INSERT INTO " + OCCURRENCES
            + " (trigger_id, scheduled_at, recorded_at, outcome, occurrence_count, reason) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SELECT_HISTORY_IDENTIFIERS = "SELECT occurrence_id FROM " + OCCURRENCES
            + " WHERE trigger_id = ? ORDER BY occurrence_id DESC";

    private static final String TRIM_HISTORY = "DELETE FROM " + OCCURRENCES
            + " WHERE trigger_id = ? AND occurrence_id <= ?";

    private static final String SELECT_HISTORY = "SELECT trigger_id, scheduled_at, recorded_at, outcome, "
            + "occurrence_count, reason FROM " + OCCURRENCES + " WHERE trigger_id = ? ORDER BY occurrence_id DESC";

    private final DataSource dataSource;
    private final int historyLimit;
    private final Clock clock;
    private volatile JdbcTriggerSchema.Dialect dialect;

    /**
     * @param dataSource   the database holding the {@link JdbcTriggerSchema} tables
     * @param historyLimit the most history records kept per trigger, at least one
     */
    public JdbcTriggerStore(DataSource dataSource, int historyLimit) {
        this(dataSource, historyLimit, Clock.systemUTC());
    }

    /**
     * @param dataSource   the database holding the {@link JdbcTriggerSchema} tables
     * @param historyLimit the most history records kept per trigger, at least one
     * @param clock        stamps {@code updated_at}, a column for people reading the table; leases never
     *                     use it — they are measured on the database's clock
     */
    public JdbcTriggerStore(DataSource dataSource, int historyLimit, Clock clock) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.clock = Objects.requireNonNull(clock, "clock");

        if (historyLimit < 1) {
            throw new IllegalArgumentException("The history limit must be at least one, not " + historyLimit + ".");
        }

        this.historyLimit = historyLimit;
    }

    @Override
    public ScheduledTrigger save(Trigger trigger, Instant nextFireAt) {
        Objects.requireNonNull(trigger, "trigger");

        try (Connection connection = dataSource.getConnection()) {
            if (writeDefinition(connection, UPDATE_DEFINITION, trigger, nextFireAt) == 0) {
                try {
                    writeDefinition(connection, INSERT_DEFINITION, trigger, nextFireAt);
                } catch (SQLException insertFailure) {
                    if (!isConstraintViolation(insertFailure)) {
                        throw insertFailure;
                    }

                    // Another process inserted the same trigger between our update and our insert.
                    writeDefinition(connection, UPDATE_DEFINITION, trigger, nextFireAt);
                }
            }

            return selectOne(connection, trigger.id())
                    .orElseThrow(() -> new TriggerStoreException("Trigger '" + trigger.id()
                            + "' vanished while it was being saved", null));
        } catch (SQLException exception) {
            throw failure("save trigger '" + trigger.id() + "'", exception);
        }
    }

    @Override
    public Optional<ScheduledTrigger> find(TriggerId id) {
        try (Connection connection = dataSource.getConnection()) {
            return selectOne(connection, id);
        } catch (SQLException exception) {
            throw failure("read trigger '" + id + "'", exception);
        }
    }

    @Override
    public List<ScheduledTrigger> findAll() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL)) {
            return readTriggers(statement);
        } catch (SQLException exception) {
            throw failure("list triggers", exception);
        }
    }

    @Override
    public boolean remove(TriggerId id) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(DELETE_HISTORY)) {
                statement.setString(1, id.value());
                statement.executeUpdate();
            }

            try (PreparedStatement statement = connection.prepareStatement(DELETE_TRIGGER)) {
                statement.setString(1, id.value());
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException exception) {
            throw failure("remove trigger '" + id + "'", exception);
        }
    }

    @Override
    public List<ScheduledTrigger> findDue(Instant now, int limit) {
        try (Connection connection = dataSource.getConnection()) {
            long databaseNow = databaseTime(connection);

            try (PreparedStatement statement = connection.prepareStatement(SELECT_DUE)) {
                statement.setBoolean(1, true);
                statement.setLong(2, now.toEpochMilli());
                statement.setLong(3, databaseNow);
                statement.setMaxRows(limit);
                return readTriggers(statement);
            }
        } catch (SQLException exception) {
            throw failure("find due triggers", exception);
        }
    }

    @Override
    public Optional<TriggerLease> claim(TriggerId id, long expectedVersion, Instant plannedAt, Duration leaseDuration) {
        Objects.requireNonNull(plannedAt, "plannedAt");

        try (Connection connection = dataSource.getConnection()) {
            long databaseNow = databaseTime(connection);

            try (PreparedStatement statement = connection.prepareStatement(CLAIM)) {
                statement.setLong(1, plannedAt.toEpochMilli());
                statement.setLong(2, databaseNow + leaseDuration.toMillis());
                statement.setLong(3, clock.millis());
                statement.setString(4, id.value());
                statement.setLong(5, expectedVersion);
                statement.setLong(6, databaseNow);

                if (statement.executeUpdate() != 1) {
                    return Optional.empty();
                }

                return Optional.of(new TriggerLease(id, expectedVersion + 1, plannedAt));
            }
        } catch (SQLException exception) {
            throw failure("claim trigger '" + id + "'", exception);
        }
    }

    @Override
    public boolean confirm(TriggerLease lease, Instant nextFireAt) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(CONFIRM)) {
            setInstant(statement, 1, nextFireAt);
            statement.setLong(2, clock.millis());
            statement.setString(3, lease.triggerId().value());
            statement.setLong(4, lease.version());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw failure("confirm trigger '" + lease.triggerId() + "'", exception);
        }
    }

    /** The database's current time in epoch milliseconds — the clock every lease is measured on. */
    private long databaseTime(Connection connection) throws SQLException {
        JdbcTriggerSchema.Dialect current = dialect;

        if (current == null) {
            current = JdbcTriggerSchema.detect(connection.getMetaData().getDatabaseProductName());
            dialect = current;
        }

        try (PreparedStatement statement = connection.prepareStatement(current.currentTimeQuery());
             ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getLong(1);
        }
    }

    @Override
    public void record(OccurrenceRecord occurrenceRecord) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(INSERT_OCCURRENCE)) {
                statement.setString(1, occurrenceRecord.triggerId().value());
                statement.setLong(2, occurrenceRecord.scheduledAt().toEpochMilli());
                statement.setLong(3, occurrenceRecord.recordedAt().toEpochMilli());
                statement.setString(4, occurrenceRecord.outcome().name());
                statement.setLong(5, occurrenceRecord.count());
                statement.setString(6, occurrenceRecord.reason());
                statement.executeUpdate();
            }

            trimHistory(connection, occurrenceRecord.triggerId());
        } catch (SQLException exception) {
            throw failure("record an occurrence of trigger '" + occurrenceRecord.triggerId() + "'", exception);
        }
    }

    @Override
    public List<OccurrenceRecord> history(TriggerId id, int limit) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_HISTORY)) {
            statement.setString(1, id.value());
            statement.setMaxRows(limit);

            List<OccurrenceRecord> records = new ArrayList<>();

            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    records.add(new OccurrenceRecord(
                            TriggerId.of(rows.getString("trigger_id")),
                            Instant.ofEpochMilli(rows.getLong("scheduled_at")),
                            Instant.ofEpochMilli(rows.getLong("recorded_at")),
                            OccurrenceOutcome.valueOf(rows.getString("outcome")),
                            rows.getLong("occurrence_count"),
                            rows.getString("reason")));
                }
            }

            return records;
        } catch (SQLException exception) {
            throw failure("read the history of trigger '" + id + "'", exception);
        }
    }

    /**
     * Deletes everything older than the {@code historyLimit}-th newest record. Portable: the
     * {@code N+1}-th identifier is found with {@link PreparedStatement#setMaxRows}, not with a
     * dialect's {@code LIMIT}/{@code OFFSET}, and MySQL refuses a subquery on the table being deleted from.
     */
    private void trimHistory(Connection connection, TriggerId id) throws SQLException {
        long oldestToDelete = -1;

        try (PreparedStatement statement = connection.prepareStatement(SELECT_HISTORY_IDENTIFIERS)) {
            statement.setString(1, id.value());
            statement.setMaxRows(historyLimit + 1);

            try (ResultSet rows = statement.executeQuery()) {
                int seen = 0;

                while (rows.next()) {
                    seen++;

                    if (seen == historyLimit + 1) {
                        oldestToDelete = rows.getLong(1);
                    }
                }
            }
        }

        if (oldestToDelete < 0) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(TRIM_HISTORY)) {
            statement.setString(1, id.value());
            statement.setLong(2, oldestToDelete);
            statement.executeUpdate();
        }
    }

    private int writeDefinition(Connection connection, String sql, Trigger trigger, Instant nextFireAt)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            statement.setString(index++, trigger.handlerKey());
            statement.setString(index++, TriggerParametersJson.write(trigger.parameters()));
            index = bindSchedule(statement, index, trigger.schedule());
            statement.setString(index++, trigger.misfirePolicy().name());

            if (trigger.misfirePolicy() instanceof MisfirePolicy.CatchUp catchUp) {
                statement.setInt(index++, catchUp.maximumOccurrences());
            } else {
                statement.setNull(index++, Types.INTEGER);
            }

            statement.setString(index++, trigger.gateKey());
            statement.setBoolean(index++, trigger.enabled());
            setInstant(statement, index++, nextFireAt);
            statement.setLong(index++, clock.millis());
            statement.setString(index, trigger.id().value());
            return statement.executeUpdate();
        }
    }

    /** Binds kind, expression, zone, period and instant; returns the next parameter index. */
    private static int bindSchedule(PreparedStatement statement, int start, Schedule schedule) throws SQLException {
        String kind;
        String expression = null;
        String zone = null;
        Long periodMilliseconds = null;
        Instant instant = null;

        switch (schedule) {
            case Schedule.Cron cron -> {
                kind = "CRON";
                expression = cron.expression().source();
                zone = cron.zone().getId();
            }
            case Schedule.Interval interval -> {
                kind = "INTERVAL";
                periodMilliseconds = interval.period().toMillis();
                instant = interval.anchor();
            }
            case Schedule.Once once -> {
                kind = "ONCE";
                instant = once.instant();
            }
        }

        statement.setString(start, kind);
        statement.setString(start + 1, expression);
        statement.setString(start + 2, zone);

        if (periodMilliseconds == null) {
            statement.setNull(start + 3, Types.BIGINT);
        } else {
            statement.setLong(start + 3, periodMilliseconds);
        }

        setInstant(statement, start + 4, instant);
        return start + 5;
    }

    private static Optional<ScheduledTrigger> selectOne(Connection connection, TriggerId id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ONE)) {
            statement.setString(1, id.value());
            List<ScheduledTrigger> triggers = readTriggers(statement);
            return triggers.stream().findFirst();
        }
    }

    private static List<ScheduledTrigger> readTriggers(PreparedStatement statement) throws SQLException {
        List<ScheduledTrigger> triggers = new ArrayList<>();

        try (ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                triggers.add(readTrigger(rows));
            }
        }

        return triggers;
    }

    private static ScheduledTrigger readTrigger(ResultSet row) throws SQLException {
        String id = row.getString("trigger_id");

        try {
            Trigger trigger = new Trigger(
                    TriggerId.of(id),
                    row.getString("handler_key"),
                    TriggerParametersJson.read(row.getString("parameters")),
                    readSchedule(row),
                    readMisfirePolicy(row),
                    row.getString("gate_key"),
                    row.getBoolean("enabled"));
            return new ScheduledTrigger(trigger, readInstant(row, "next_fire_at"), row.getLong("version"),
                    readInstant(row, "planned_at"));
        } catch (RuntimeException exception) {
            throw new TriggerStoreException("The stored trigger '" + id + "' is malformed", exception);
        }
    }

    private static Schedule readSchedule(ResultSet row) throws SQLException {
        String kind = row.getString("schedule_kind");

        return switch (kind) {
            case "CRON" -> Schedule.cron(row.getString("schedule_expression"), ZoneId.of(row.getString("schedule_zone")));
            case "INTERVAL" -> Schedule.every(Duration.ofMillis(row.getLong("schedule_period_milliseconds")),
                    readInstant(row, "schedule_instant"));
            case "ONCE" -> Schedule.once(readInstant(row, "schedule_instant"));
            default -> throw new IllegalArgumentException("Unknown schedule kind '" + kind + "'.");
        };
    }

    private static MisfirePolicy readMisfirePolicy(ResultSet row) throws SQLException {
        String name = row.getString("misfire_policy");

        return switch (name) {
            case "SKIP" -> MisfirePolicy.skip();
            case "FIRE_ONCE" -> MisfirePolicy.fireOnce();
            case "CATCH_UP" -> MisfirePolicy.catchUp(row.getInt("catch_up_limit"));
            default -> throw new IllegalArgumentException("Unknown misfire policy '" + name + "'.");
        };
    }

    private static Instant readInstant(ResultSet row, String column) throws SQLException {
        long epochMilliseconds = row.getLong(column);
        return row.wasNull() ? null : Instant.ofEpochMilli(epochMilliseconds);
    }

    private static void setInstant(PreparedStatement statement, int index, Instant instant) throws SQLException {
        if (instant == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, instant.toEpochMilli());
        }
    }

    /** SQLState class 23 is an integrity constraint violation on every database this store supports. */
    private static boolean isConstraintViolation(SQLException exception) {
        String state = exception.getSQLState();
        return state != null && state.startsWith("23");
    }

    private static TriggerStoreException failure(String action, SQLException exception) {
        return new TriggerStoreException("jmouse-scheduler could not " + action, exception);
    }
}
