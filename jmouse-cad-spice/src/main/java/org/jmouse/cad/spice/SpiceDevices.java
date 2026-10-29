package org.jmouse.cad.spice;

import org.jmouse.cad.simulation.EvaluatedParameter;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.spice.SpiceNetlist.ModelType;
import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.sheet.SheetField;
import org.jmouse.sheet.SheetReader;
import org.jmouse.sheet.SheetValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * What the cards of a SPICE netlist are in jMX, both ways, as a table says it — {@code spice-devices.sht} beside this
 * class, or a product's own, whose catalogue carries its devices in components of other names: each element card's
 * component, pins and settings ({@link ElementCard}), each {@code .model}'s device ({@link SpiceDevice}). An entry that
 * names a {@code card} is an element, one that names a {@code model} a device; a device's {@code parameters} and
 * {@code geometry} name entries of their own, which devices of one kind share.
 */
public record SpiceDevices(List<ElementCard> elements, List<SpiceDevice> devices) {

    private static final String RESOURCE = "spice-devices.sht";

    /** The parameter a component of a device of two polarities says which it is by. */
    public static final String POLARITY = "polarity";

    public SpiceDevices {
        elements = List.copyOf(elements);
        devices = List.copyOf(devices);
    }

    /** The table this library ships, for the components its catalogue holds. */
    public static SpiceDevices standard() {
        try (InputStream stream = SpiceDevices.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("'" + RESOURCE + "' is not beside " + SpiceDevices.class.getName());
            }
            return of(SheetReader.read(RESOURCE, new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    public static SpiceDevices of(Sheet table) {
        List<ElementCard> elements = new ArrayList<>();
        List<SpiceDevice> devices = new ArrayList<>();
        for (SheetEntry entry : table.entries()) {
            if (entry.field("card").isPresent()) {
                elements.add(element(entry));
            } else if (entry.field("model").isPresent()) {
                devices.add(device(table, entry));
            }
        }
        return new SpiceDevices(elements, devices);
    }

    /** The element a card is, with the signal it writes — empty for a constant one —; empty when the table has none. */
    public Optional<ElementCard> element(String card, Optional<String> signal) {
        return elements.stream().filter(element -> element.card().equalsIgnoreCase(card) && element.signal().equals(signal))
                .findFirst();
    }

    /**
     * The device a {@code .model} of that type describes.
     *
     * @throws IllegalArgumentException when the table has none
     */
    public SpiceDevice device(ModelType type) {
        return devices.stream().filter(device -> device.modelType() == type).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The SPICE table has no device of model " + type));
    }

    /** Whether an element line of that letter takes a model of that type: {@code Q} an NPN or a PNP. */
    public boolean takes(String element, ModelType type) {
        return devices.stream().anyMatch(device -> device.element().equalsIgnoreCase(element) && device.modelType() == type);
    }

    /**
     * The device a component is: the one of its type, and — for a type of two polarities — of its {@code polarity};
     * empty for a component that is none of them.
     */
    public Optional<SpiceDevice> deviceOf(SimulatedComponent component) {
        Optional<Double> polarity = component.parameter(POLARITY).map(EvaluatedParameter::value);
        return devices.stream().filter(device -> device.componentType().equals(component.type()))
                .filter(device -> device.polarity().isEmpty() || device.polarity().equals(polarity))
                .findFirst();
    }

    private static ElementCard element(SheetEntry entry) {
        Map<String, String> settings = new LinkedHashMap<>();
        entry.value("settings").ifPresent(value -> items(value).forEach(item -> {
            if (!(item instanceof SheetValue.Assignment assignment)) {
                throw refusal(entry, "settings", "each setting is 'argument = setting'");
            }
            settings.put(assignment.name(), assignment.value().written());
        }));
        return new ElementCard(entry.id(), word(entry, "card"), optionalWord(entry, "signal"), word(entry, "component"),
                words(entry, "pins"), optionalWord(entry, "value"), settings);
    }

    private static SpiceDevice device(Sheet table, SheetEntry entry) {
        String model = word(entry, "model");
        ModelType type;
        try {
            type = ModelType.valueOf(model.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw refusal(entry, "model", "'" + model + "' is no .model type SPICE writes");
        }
        Optional<Double> polarity = entry.value(POLARITY).map(value -> number(entry, POLARITY, value));
        List<SpiceDevice.ParameterName> parameters = new ArrayList<>();
        for (SheetField field : referenced(table, entry, "parameters").fields()) {
            parameters.add(parameter(entry, field));
        }
        Optional<SpiceDevice.Geometry> geometry = entry.field("geometry").map(ignored -> {
            SheetEntry written = referenced(table, entry, "geometry");
            return new SpiceDevice.Geometry(number(written, "default", written.value("default").orElseThrow(
                    () -> refusal(written, "default", "a geometry gives the width and length a line writes not"))),
                    words(written, "by-ratio"), words(written, "by-width"));
        });
        return new SpiceDevice(entry.id(), type, word(entry, "element"), word(entry, "type"), polarity, words(entry, "pins"), word(entry, "carrier"),
                parameters, geometry);
    }

    /** {@code I_saturation IS = 1e-14}, {@code R_series RS}, {@code T_nominal TNOM = 27 °C}. */
    private static SpiceDevice.ParameterName parameter(SheetEntry device, SheetField field) {
        return switch (field.value()) {
            case SheetValue.Word spice -> new SpiceDevice.ParameterName(spice.word(), field.name(), Optional.empty(), false);
            case SheetValue.Assignment given when given.value() instanceof SheetValue.Measure measure -> {
                boolean celsius = measure.quantity().kelvin().isPresent() && !measure.quantity().unit().equals("K");
                yield new SpiceDevice.ParameterName(given.name(), field.name(), Optional.of(measure.quantity().value()), celsius);
            }
            default -> throw refusal(device, field.name(), "a parameter is 'jmx_name SPICE_NAME [= SPICE's default]'");
        };
    }

    private static SheetEntry referenced(Sheet table, SheetEntry entry, String field) {
        String id = word(entry, field);
        return table.entry(id).orElseThrow(() -> refusal(entry, field, "the table has no entry '" + id + "'"));
    }

    private static String word(SheetEntry entry, String field) {
        return optionalWord(entry, field).orElseThrow(() -> refusal(entry, field, "it is missing"));
    }

    private static Optional<String> optionalWord(SheetEntry entry, String field) {
        return entry.value(field).map(SheetValue::written);
    }

    private static List<String> words(SheetEntry entry, String field) {
        return entry.value(field).map(value -> items(value).stream().map(SheetValue::written).toList()).orElse(List.of());
    }

    private static List<SheetValue> items(SheetValue value) {
        return value instanceof SheetValue.Sequence sequence ? sequence.items() : List.of(value);
    }

    private static double number(SheetEntry entry, String field, SheetValue value) {
        if (!(value instanceof SheetValue.Measure measure)) {
            throw refusal(entry, field, "it is a number");
        }
        return measure.quantity().value();
    }

    private static IllegalStateException refusal(SheetEntry entry, String field, String why) {
        return new IllegalStateException("The SPICE table's '" + entry.id() + "' " + field + ": " + why);
    }
}
