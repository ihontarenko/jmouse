package org.jmouse.scheduler;

import org.jmouse.scheduler.spi.TriggerGate;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The gates this process knows, by key. Immutable once built; two gates claiming one key are refused
 * at construction.
 */
public final class GateRegistry {

    private static final GateRegistry EMPTY = new GateRegistry(List.of());

    private final KeyedRegistry<TriggerGate> gates;

    /**
     * @param gates every gate; keys must be unique
     * @throws IllegalStateException when two gates share a key
     */
    public GateRegistry(Collection<? extends TriggerGate> gates) {
        this.gates = new KeyedRegistry<>(gates, TriggerGate::key, "gate");
    }

    /** A registry with no gates — a product that gates nothing writes no adapter. */
    public static GateRegistry empty() {
        return EMPTY;
    }

    /** @return a registry over the given gates */
    public static GateRegistry of(TriggerGate... gates) {
        return new GateRegistry(List.of(gates));
    }

    /** @return the gate registered under {@code key}, or empty */
    public Optional<TriggerGate> find(String key) {
        return gates.find(key);
    }

    /** Every registered key, sorted. */
    public Set<String> keys() {
        return gates.keys();
    }
}
