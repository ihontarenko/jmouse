package org.jmouse.scheduler;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A durable trigger: <em>when</em> ({@link Schedule}), <em>what</em> (a handler key and its
 * {@link TriggerParameters}), <em>after downtime</em> ({@link MisfirePolicy}) and <em>on what
 * condition</em> (an optional gate key).
 *
 * <p>The trigger names its handler and its gate by key rather than holding them, so it is plain data
 * a store can keep and another process can read. Keys resolve against the
 * {@link HandlerRegistry} and {@link GateRegistry} at the moment of firing.
 *
 * @param id            the permanent identifier
 * @param handlerKey    the {@link org.jmouse.scheduler.spi.TriggerHandler#key() handler} this trigger fires
 * @param parameters    what the handler (and the gate) receives
 * @param schedule      when it fires
 * @param misfirePolicy what happens to late occurrences
 * @param gateKey       the {@link org.jmouse.scheduler.spi.TriggerGate#key() gate} evaluated before each
 *                      dispatch, or {@code null} for none
 * @param enabled       a disabled trigger is kept, with its history, and never fires
 */
public record Trigger(
        TriggerId id,
        String handlerKey,
        TriggerParameters parameters,
        Schedule schedule,
        MisfirePolicy misfirePolicy,
        String gateKey,
        boolean enabled
) {

    private static final Pattern KEY_FORMAT = Pattern.compile("[a-z][a-z0-9]*(?:[-.][a-z0-9]+)*");

    public Trigger {
        Objects.requireNonNull(id, "id");
        requireKey(handlerKey, "handler");
        Objects.requireNonNull(schedule, "schedule");
        parameters = parameters == null ? TriggerParameters.empty() : parameters;
        misfirePolicy = misfirePolicy == null ? MisfirePolicy.skip() : misfirePolicy;

        if (gateKey != null) {
            requireKey(gateKey, "gate");
        }
    }

    /**
     * Starts a trigger. The misfire policy defaults to {@link MisfirePolicy#skip()}, there is no gate,
     * and the trigger is enabled.
     *
     * @param id the permanent identifier
     * @return a builder
     */
    public static Builder builder(String id) {
        return new Builder(TriggerId.of(id));
    }

    /** The gate key, when the trigger has one. */
    public Optional<String> gate() {
        return Optional.ofNullable(gateKey);
    }

    /** This trigger with {@code enabled} replaced. */
    public Trigger withEnabled(boolean enabled) {
        return new Trigger(id, handlerKey, parameters, schedule, misfirePolicy, gateKey, enabled);
    }

    /**
     * Validates a handler or gate key: lower-case words joined by {@code -} or {@code .}, such as
     * {@code vectorise-waiting} or {@code budget.preflight}.
     *
     * @param key  the key
     * @param role {@code handler} or {@code gate}, for the message
     * @return the key
     */
    static String requireKey(String key, String role) {
        Objects.requireNonNull(key, role + " key");

        if (key.length() > 200 || !KEY_FORMAT.matcher(key).matches()) {
            throw new IllegalArgumentException("A " + role + " key is lower-case words joined by '-' or '.',"
                    + " at most 200 characters: '" + key + "'.");
        }

        return key;
    }

    /** Assembles a {@link Trigger}. */
    public static final class Builder {

        private final TriggerId id;
        private String handlerKey;
        private TriggerParameters parameters = TriggerParameters.empty();
        private Schedule schedule;
        private MisfirePolicy misfirePolicy = MisfirePolicy.skip();
        private String gateKey;
        private boolean enabled = true;

        private Builder(TriggerId id) {
            this.id = id;
        }

        /** The handler this trigger fires. */
        public Builder handler(String handlerKey) {
            this.handlerKey = handlerKey;
            return this;
        }

        /** The parameters the handler receives. */
        public Builder parameters(TriggerParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        /** The parameters the handler receives, as a JSON-compatible map. */
        public Builder parameters(java.util.Map<String, ?> parameters) {
            this.parameters = TriggerParameters.of(parameters);
            return this;
        }

        /** When the trigger fires. */
        public Builder schedule(Schedule schedule) {
            this.schedule = schedule;
            return this;
        }

        /** What happens to late occurrences. */
        public Builder misfirePolicy(MisfirePolicy misfirePolicy) {
            this.misfirePolicy = misfirePolicy;
            return this;
        }

        /** The gate evaluated before each dispatch. */
        public Builder gate(String gateKey) {
            this.gateKey = gateKey;
            return this;
        }

        /** Whether the trigger fires at all. */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /** @return the trigger */
        public Trigger build() {
            return new Trigger(id, handlerKey, parameters, schedule, misfirePolicy, gateKey, enabled);
        }
    }
}
