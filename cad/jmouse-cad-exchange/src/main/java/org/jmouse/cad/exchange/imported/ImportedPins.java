package org.jmouse.cad.exchange.imported;

import org.jmouse.el.lexer.Identifiers;
import org.jmouse.cad.simulation.SimulationVocabulary;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * What a pin of another product's part is to the engine, by what that product says of it and what it is called: a power
 * pin is a ground or a supply by its name, and a passive one named as either is one too — a chip's extra VSS pins are
 * often marked passive, and a box must still put them at the bottom. The names a ground and a supply go by are data
 * ({@code pin-names.properties}).
 */
public final class ImportedPins {

    /** What the product says a pin is, as far as its nature goes. */
    public enum Role {
        /** A pin the product calls a power one: an input or an output of power. */
        POWER,
        /** A pin it calls passive: a resistor's end, or a chip's pin it would not have fought over. */
        PASSIVE,
        /** Any other: a signal. */
        SIGNAL
    }

    private static final String NAMES = "pin-names.properties";

    /** What a pin named by its package number opens with: {@code pin1}, {@code pinA3}. */
    private static final String PREFIX = "pin";

    private static final Pattern GROUND;
    private static final Pattern SUPPLY;

    static {
        Properties names = new Properties();
        try (InputStream input = ImportedPins.class.getResourceAsStream(NAMES)) {
            if (input == null) {
                throw new IllegalStateException("'" + NAMES + "' is not beside " + ImportedPins.class.getName());
            }
            names.load(input);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        GROUND = Pattern.compile(names.getProperty("ground"));
        SUPPLY = Pattern.compile(names.getProperty("supply"));
    }

    private ImportedPins() {
    }

    /** What a pin is called here: {@code pin} and the package's number for it, as an identifier holds it ({@code pinA_3}). */
    public static String nameOf(String number) {
        return Identifiers.of(PREFIX + number, PREFIX);
    }

    /** Whether a package's name for a pad is a number — in digits —, which a pin's numbers are written in. */
    public static boolean numbered(String pad) {
        return !pad.isEmpty() && pad.chars().allMatch(character -> character >= '0' && character <= '9');
    }

    /** The package number a pin named by {@link #nameOf} stands for: {@code pinA} is {@code A}; empty for another name. */
    public static Optional<String> numberOf(String name) {
        boolean numbered = Identifiers.isIdentifier(name) && name.startsWith(PREFIX) && name.length() > PREFIX.length();
        return numbered ? Optional.of(name.substring(PREFIX.length())) : Optional.empty();
    }

    /** The pin's nature: {@code ground}, {@code power} or {@code analog}. */
    public static String natureOf(String name, Role role) {
        String upper = name.toUpperCase(Locale.ROOT);
        return switch (role) {
            case POWER -> GROUND.matcher(upper).matches() ? SimulationVocabulary.GROUND : SimulationVocabulary.POWER;
            case PASSIVE -> GROUND.matcher(upper).matches() ? SimulationVocabulary.GROUND
                    : SUPPLY.matcher(upper).matches() ? SimulationVocabulary.POWER : SimulationVocabulary.ANALOG;
            case SIGNAL -> SimulationVocabulary.ANALOG;
        };
    }
}
