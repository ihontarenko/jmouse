package org.jmouse.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The plugins of one application, registered, resolved and started — and what their owners read of the points.
 *
 * <p>A plugin is refused, with the reason kept in {@link #plugins()}, when its registration fails, when another took its
 * id, when a plugin it requires is missing or refused, when it brings something to a point nobody started declares or
 * declares a point another already declared, when a contribution's identity is taken in its point, when it stands on
 * itself, or when it fails to start. Whatever stands on a refused plugin is refused with it; nothing else is.</p>
 *
 * <p>Plugins start in an order where each comes after the plugins it requires and after the plugins that declared the
 * points it reaches; contributions are read in that order. They stop in the reverse one.</p>
 */
public final class Plugins implements Extensions, AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(Plugins.class.getName());

    /** By the plugin itself, not its id: two offered under one id are two plugins, the second refused. */
    private final Map<Plugin, PluginState>     states        = new LinkedHashMap<>();
    private final List<Registered>             started       = new ArrayList<>();
    private final Map<String, ExtensionPoint<?>> declared    = new LinkedHashMap<>();
    private final Map<String, List<Object>>    contributions = new HashMap<>();
    private final Map<String, List<Decorator<?>>> decorators = new HashMap<>();
    private final Map<String, List<Consumer<?>>>  listeners  = new HashMap<>();

    private Plugins() {
    }

    /** Registers, resolves and starts the plugins offered; none offered is an application with no plugin. */
    public static Plugins start(Collection<? extends Plugin> offered) {
        Plugins plugins = new Plugins();
        plugins.startAll(offered);
        return plugins;
    }

    // ── Reading the points ───────────────────────────────────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> contributions(Contributions<T> point) {
        return (List<T>) List.copyOf(contributions.getOrDefault(point.name(), List.of()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T decorated(Decorations<T> point, T service) {
        T current = service;
        for (Decorator<?> decorator : decorators.getOrDefault(point.name(), List.of())) {
            current = ((Decorator<T>) decorator).decorate(current);
        }
        return current;
    }

    /** Hands the event to every listener; one that fails is reported and the rest still hear it. */
    @Override
    @SuppressWarnings("unchecked")
    public <T> void publish(Topic<T> topic, T event) {
        for (Consumer<?> listener : listeners.getOrDefault(topic.name(), List.of())) {
            try {
                ((Consumer<T>) listener).accept(event);
            } catch (RuntimeException failure) {
                LOGGER.log(System.Logger.Level.WARNING, "A listener of '" + topic.name() + "' failed", failure);
            }
        }
    }

    @Override
    public List<PluginState> plugins() {
        return List.copyOf(states.values());
    }

    @Override
    public List<ExtensionPoint<?>> points() {
        return List.copyOf(declared.values());
    }

    /** Stops the started plugins, the last started first; one that fails to stop does not keep the others running. */
    @Override
    public void close() {
        for (Registered registered : started.reversed()) {
            try {
                registered.plugin.stop();
            } catch (RuntimeException failure) {
                LOGGER.log(System.Logger.Level.WARNING, "Plugin '" + registered.id() + "' failed to stop", failure);
            }
        }
        started.clear();
    }

    // ── Starting ─────────────────────────────────────────────────────────────────────────────────────────────────

    private void startAll(Collection<? extends Plugin> offered) {
        List<Registered> accepted = registered(offered);
        resolve(accepted);
        List<Registered> ordered = ordered(accepted);
        index(ordered);
        for (Registered registered : ordered) {
            if (isRefused(registered)) {
                continue;
            }
            try {
                registered.plugin.start(this);
                started.add(registered);
                states.put(registered.plugin, PluginState.started(registered.descriptor()));
            } catch (RuntimeException failure) {
                LOGGER.log(System.Logger.Level.WARNING, "Plugin '" + registered.id() + "' failed to start", failure);
                refuseWithDependents(ordered, registered, "failed to start: " + messageOf(failure));
                index(ordered);
            }
        }
    }

    /** Every plugin offered, its registration taken down; a duplicate id or a failing registration is refused here. */
    private List<Registered> registered(Collection<? extends Plugin> offered) {
        List<Registered> registered = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (Plugin plugin : offered) {
            PluginDescriptor descriptor = plugin.descriptor();
            if (!ids.add(descriptor.id())) {
                states.put(plugin, PluginState.refused(descriptor, "another plugin has the id '" + descriptor.id() + "'"));
                continue;
            }
            Registered taken = new Registered(plugin, descriptor);
            try {
                plugin.register(taken);
                registered.add(taken);
            } catch (RuntimeException failure) {
                refuse(taken, "its registration failed: " + messageOf(failure));
            }
        }
        return registered;
    }

    /** Refuses whatever cannot stand, again and again, until what is left stands together. */
    private void resolve(List<Registered> accepted) {
        boolean changed = true;
        while (changed) {
            changed = false;
            Map<String, Registered> byId = new HashMap<>();
            accepted.forEach(registered -> byId.put(registered.id(), registered));
            Map<String, Declaration> points = new HashMap<>();
            for (Registered registered : List.copyOf(accepted)) {
                String reason = null;
                for (ExtensionPoint<?> point : registered.declared) {
                    Declaration earlier = points.putIfAbsent(point.name(), new Declaration(point, registered));
                    if (earlier != null && earlier.owner != registered) {
                        reason = "declares '" + point.name() + "', which '" + earlier.owner.id() + "' declared";
                    }
                }
                if (reason == null) {
                    reason = missingRequirement(registered, byId);
                }
                if (reason != null) {
                    changed |= refuseFrom(accepted, registered, reason);
                }
            }
            for (Registered registered : List.copyOf(accepted)) {
                String reason = unreachedPoint(registered, points);
                if (reason == null) {
                    reason = takenIdentity(registered, accepted);
                }
                if (reason != null) {
                    changed |= refuseFrom(accepted, registered, reason);
                }
            }
        }
    }

    private static String missingRequirement(Registered registered, Map<String, Registered> byId) {
        for (String required : registered.descriptor().requires()) {
            if (!byId.containsKey(required)) {
                return "requires '" + required + "', which is not started";
            }
        }
        return null;
    }

    /** The first point the plugin reaches that is not declared as it reaches it. */
    private static String unreachedPoint(Registered registered, Map<String, Declaration> points) {
        for (ExtensionPoint<?> point : registered.reached()) {
            Declaration declaration = points.get(point.name());
            if (declaration == null) {
                return "reaches '" + point.name() + "', which no started plugin declares";
            }
            if (!declaration.point.equals(point)) {
                return "reaches '" + point.name() + "' as " + describe(point) + ", but it is " + describe(declaration.point);
            }
        }
        return null;
    }

    /** An identity this plugin contributes that a plugin before it already holds in the same point. */
    private static String takenIdentity(Registered registered, List<Registered> accepted) {
        for (Contribution contribution : registered.contributions) {
            if (!(contribution.value instanceof Identified identified)) {
                continue;
            }
            for (Registered earlier : accepted) {
                if (earlier == registered) {
                    break;
                }
                boolean taken = earlier.contributions.stream().anyMatch(other -> other.point.equals(contribution.point)
                        && other.value instanceof Identified held && held.id().equals(identified.id()));
                if (taken) {
                    return "contributes '" + identified.id() + "' to '" + contribution.point.name() + "', which '"
                            + earlier.id() + "' already did";
                }
            }
        }
        return null;
    }

    /**
     * The accepted plugins in an order where each follows what it stands on — the plugins it requires and the declarers
     * of the points it reaches —, the offered order kept where nothing decides. A plugin standing on itself is refused.
     */
    private List<Registered> ordered(List<Registered> accepted) {
        Map<Registered, Set<Registered>> standsOn = new LinkedHashMap<>();
        Map<String, Registered> declarers = new HashMap<>();
        accepted.forEach(registered -> registered.declared.forEach(point -> declarers.put(point.name(), registered)));
        Map<String, Registered> byId = new HashMap<>();
        accepted.forEach(registered -> byId.put(registered.id(), registered));
        for (Registered registered : accepted) {
            Set<Registered> below = new LinkedHashSet<>();
            registered.descriptor().requires().forEach(required -> below.add(byId.get(required)));
            registered.reached().forEach(point -> below.add(declarers.get(point.name())));
            below.remove(registered);
            standsOn.put(registered, below);
        }
        List<Registered> ordered = new ArrayList<>();
        Set<Registered> placed = new HashSet<>();
        // Each time the first plugin, in the offered order, whose ground is placed: a later one never jumps an earlier one
        // that is ready too.
        boolean progress = true;
        while (progress) {
            progress = false;
            for (Registered registered : accepted) {
                if (!placed.contains(registered) && placed.containsAll(standsOn.get(registered))) {
                    ordered.add(registered);
                    placed.add(registered);
                    progress = true;
                    break;
                }
            }
        }
        for (Registered registered : accepted) {
            if (!placed.contains(registered)) {
                refuse(registered, "stands on itself through the plugins it requires or the points it reaches");
            }
        }
        return ordered;
    }

    /** The points, the contributions, the decorators and the listeners of the plugins in order, read anew. */
    private void index(List<Registered> ordered) {
        declared.clear();
        contributions.clear();
        decorators.clear();
        listeners.clear();
        for (Registered registered : ordered) {
            if (isRefused(registered)) {
                continue;
            }
            registered.declared.forEach(point -> declared.put(point.name(), point));
            registered.contributions.forEach(contribution -> contributions
                    .computeIfAbsent(contribution.point.name(), name -> new ArrayList<>()).add(contribution.value));
            registered.decorations.forEach(decoration -> decorators
                    .computeIfAbsent(decoration.point.name(), name -> new ArrayList<>()).add(decoration.decorator));
            registered.subscriptions.forEach(subscription -> listeners
                    .computeIfAbsent(subscription.topic.name(), name -> new ArrayList<>()).add(subscription.listener));
        }
        decorators.values().forEach(chain -> chain.sort(Comparator.comparingInt(Decorator::order)));
    }

    /** Refuses a plugin that failed to start, and every plugin not yet started that stands on it. */
    private void refuseWithDependents(List<Registered> ordered, Registered failed, String reason) {
        refuse(failed, reason);
        Set<String> fallen = new HashSet<>(Set.of(failed.id()));
        Set<String> fallenPoints = new HashSet<>();
        failed.declared.forEach(point -> fallenPoints.add(point.name()));
        for (Registered registered : ordered) {
            if (isRefused(registered)) {
                continue;
            }
            boolean standsOnFallen = registered.descriptor().requires().stream().anyMatch(fallen::contains)
                    || registered.reached().stream().anyMatch(point -> fallenPoints.contains(point.name()));
            if (standsOnFallen) {
                refuse(registered, "stands on '" + failed.id() + "', which " + reason);
                fallen.add(registered.id());
                registered.declared.forEach(point -> fallenPoints.add(point.name()));
            }
        }
    }

    private boolean refuseFrom(List<Registered> accepted, Registered registered, String reason) {
        refuse(registered, reason);
        return accepted.remove(registered);
    }

    private void refuse(Registered registered, String reason) {
        states.put(registered.plugin, PluginState.refused(registered.descriptor(), reason));
    }

    private boolean isRefused(Registered registered) {
        PluginState state = states.get(registered.plugin);
        return state != null && state.status() == PluginState.Status.REFUSED;
    }

    private static String describe(ExtensionPoint<?> point) {
        String kind = switch (point) {
            case Contributions<?> ignored -> "contributions";
            case Decorations<?> ignored -> "decorations";
            case Topic<?> ignored -> "a topic";
        };
        return kind + " of " + point.type().getSimpleName();
    }

    private static String messageOf(RuntimeException failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }

    // ── What one plugin said ─────────────────────────────────────────────────────────────────────────────────────

    private record Declaration(ExtensionPoint<?> point, Registered owner) {
    }

    private record Contribution(Contributions<?> point, Object value) {
    }

    private record Decoration(Decorations<?> point, Decorator<?> decorator) {
    }

    private record Subscription(Topic<?> topic, Consumer<?> listener) {
    }

    /** One plugin's registration, taken down as it is said; a contribution of the wrong type is refused at once. */
    private static final class Registered implements PluginRegistration {

        private final Plugin                  plugin;
        private final PluginDescriptor        descriptor;
        private final List<ExtensionPoint<?>> declared      = new ArrayList<>();
        private final List<Contribution>      contributions = new ArrayList<>();
        private final List<Decoration>        decorations   = new ArrayList<>();
        private final List<Subscription>      subscriptions = new ArrayList<>();

        private Registered(Plugin plugin, PluginDescriptor descriptor) {
            this.plugin = plugin;
            this.descriptor = descriptor;
        }

        private String id() {
            return descriptor.id();
        }

        private PluginDescriptor descriptor() {
            return descriptor;
        }

        /** Every point it brings something to — the points it declared itself left out. */
        private List<ExtensionPoint<?>> reached() {
            List<ExtensionPoint<?>> reached = new ArrayList<>();
            contributions.forEach(contribution -> reached.add(contribution.point));
            decorations.forEach(decoration -> reached.add(decoration.point));
            subscriptions.forEach(subscription -> reached.add(subscription.topic));
            reached.removeIf(point -> declared.stream().anyMatch(own -> own.name().equals(point.name())));
            return reached;
        }

        @Override
        public <T> void declare(ExtensionPoint<T> point) {
            declared.add(point);
        }

        @Override
        public <T> void contribute(Contributions<T> point, T contribution) {
            if (!point.type().isInstance(contribution)) {
                throw new IllegalArgumentException("'" + point.name() + "' holds " + point.type().getSimpleName()
                        + ", not " + (contribution == null ? "nothing" : contribution.getClass().getSimpleName()));
            }
            contributions.add(new Contribution(point, contribution));
        }

        @Override
        public <T> void decorate(Decorations<T> point, Decorator<T> decorator) {
            if (decorator == null) {
                throw new IllegalArgumentException("A decoration of '" + point.name() + "' needs a decorator.");
            }
            decorations.add(new Decoration(point, decorator));
        }

        @Override
        public <T> void subscribe(Topic<T> topic, Consumer<? super T> listener) {
            if (listener == null) {
                throw new IllegalArgumentException("A subscription to '" + topic.name() + "' needs a listener.");
            }
            subscriptions.add(new Subscription(topic, listener));
        }
    }
}
