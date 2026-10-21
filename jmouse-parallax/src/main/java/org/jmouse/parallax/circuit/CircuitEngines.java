package org.jmouse.parallax.circuit;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * The engines a host offers, by kind. The library's own is always there; SPICE only where the host has a simulator to
 * run it with — a computation that asks for an engine the host has not supplied is refused, never answered by another.
 */
public final class CircuitEngines {

    private final Map<CircuitEngineKind, CircuitEngine> engines;

    private CircuitEngines(Map<CircuitEngineKind, CircuitEngine> engines) {
        this.engines = Map.copyOf(engines);
    }

    /** The library's own engine alone. */
    public static CircuitEngines parallaxOnly() {
        Map<CircuitEngineKind, CircuitEngine> engines = new EnumMap<>(CircuitEngineKind.class);
        engines.put(CircuitEngineKind.PARALLAX, new ParallaxEngine());
        return new CircuitEngines(engines);
    }

    /** These engines and one more — or another in the place of the one of its kind. */
    public CircuitEngines with(CircuitEngine engine) {
        Map<CircuitEngineKind, CircuitEngine> extended = new EnumMap<>(CircuitEngineKind.class);
        extended.putAll(engines);
        extended.put(engine.kind(), engine);
        return new CircuitEngines(extended);
    }

    /** Whether the host offers an engine of this kind. */
    public boolean offers(CircuitEngineKind kind) {
        return engines.containsKey(kind);
    }

    /**
     * The engine of a kind.
     *
     * @throws IllegalArgumentException when the host offers none of that kind
     */
    public CircuitEngine engine(CircuitEngineKind kind) {
        CircuitEngine engine = engines.get(kind);
        if (engine == null) {
            throw new IllegalArgumentException("This server has no " + kind.name().toLowerCase(Locale.ROOT)
                    + " engine to compute circuits with: SPICE runs only where a simulator is configured "
                    + "(jmouse.parallax.spice.command).");
        }
        return engine;
    }
}
