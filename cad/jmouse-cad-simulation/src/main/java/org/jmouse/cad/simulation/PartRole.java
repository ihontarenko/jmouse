package org.jmouse.cad.simulation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 🎛️ What a part does in a circuit, by what it is — never by the sign of one moment's power, which flips with the phase: a
 * source feeds it, a store gives back what it took, an instrument reads or drives it, and every other part dissipates.
 * Which kinds are which is data ({@code part-roles.properties} beside this class), the one table the engine, the
 * interface and every export read.
 */
public enum PartRole {

    SOURCE,
    STORING,
    INSTRUMENT,
    DISSIPATING;

    private static final String RESOURCE = "part-roles.properties";

    private static final Map<String, PartRole> BY_KIND = read();

    /**
     * The role of a part of a component of this kind ({@code type}), and whether it reads the circuit — a {@code measure} of
     * its own or one of its bases': one that does is an instrument, whatever its kind.
     */
    public static PartRole of(String kind, boolean reads) {
        if (reads) {
            return INSTRUMENT;
        }
        return kind == null ? DISSIPATING : BY_KIND.getOrDefault(kind, DISSIPATING);
    }

    /** Whether it is the bench's rather than the circuit's: the simulator drives and reads through it, and no pcb carries it. */
    public boolean ofTheBench() {
        return this == SOURCE || this == INSTRUMENT;
    }

    /** As the table and a wire write it: {@code source}. */
    public String word() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static Map<String, PartRole> read() {
        Properties written = new Properties();
        try (InputStream stream = PartRole.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("'" + RESOURCE + "' is not beside " + PartRole.class.getName());
            }
            written.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        Map<String, PartRole> roles = Arrays.stream(values()).collect(Collectors.toMap(PartRole::word, Function.identity()));
        return written.stringPropertyNames().stream()
                .flatMap(role -> Arrays.stream(written.getProperty(role).split(",")).map(String::strip).filter(kind -> !kind.isEmpty())
                        .map(kind -> Map.entry(kind, requireRole(roles, role))))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static PartRole requireRole(Map<String, PartRole> roles, String role) {
        PartRole found = roles.get(role.strip());
        if (found == null) {
            throw new IllegalStateException("'" + RESOURCE + "' names the role '" + role + "', which is none of " + roles.keySet());
        }
        return found;
    }
}
