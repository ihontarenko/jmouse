package org.jmouse.cad.simulation;

import org.jmouse.units.Dimension;
import org.jmouse.units.Units;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/**
 * The quantities a simulated document may type a value as, read from {@code quantities.properties} beside this class:
 * each a name and its SI unit, whose dimension the units library reads. A new quantity is a line there.
 */
public final class QuantityTypes {

    private static final String RESOURCE = "quantities.properties";

    private QuantityTypes() {
    }

    public static List<QuantityType> load() {
        Properties written = new Properties();
        try (InputStream stream = QuantityTypes.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("'" + RESOURCE + "' is not beside " + QuantityTypes.class.getName());
            }
            written.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        return written.stringPropertyNames().stream().sorted(Comparator.naturalOrder())
                .map(name -> type(name, written.getProperty(name).strip())).toList();
    }

    private static QuantityType type(String name, String unit) {
        if (unit.isEmpty()) {
            return new QuantityType(name, unit, Dimension.NONE);
        }
        Dimension dimension = Units.read(unit).flatMap(read -> read.dimension())
                .orElseThrow(() -> new IllegalStateException("'" + unit + "' of the quantity '" + name + "' is not a unit"));
        return new QuantityType(name, unit, dimension);
    }
}
