package org.jmouse.cad.exchange.imported;

import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.core.text.Slugifier;
import org.jmouse.el.lexer.Identifiers;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * What a name another product writes becomes in jM: an identifier where a document names a pin, a net or a part
 * ({@link Identifiers}), a slug where it addresses a document ({@link Slugifier}). A sign at either end of the name is a
 * word — {@code +5V} and {@code -5V} are two supplies, {@code IN+} and {@code IN-} two inputs —, as
 * {@code imported-names.properties} spells it.
 */
public final class ImportedNames {

    private static final String SIGNS = "imported-names.properties";

    private static final Properties WORDS = new Properties();

    static {
        try (InputStream input = ImportedNames.class.getResourceAsStream(SIGNS)) {
            if (input == null) {
                throw new IllegalStateException("'" + SIGNS + "' is not beside " + ImportedNames.class.getName());
            }
            WORDS.load(input);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private ImportedNames() {
    }

    /**
     * The identifier a product's name is: {@code GND@1} is {@code GND_1}, {@code +12V} is {@code plus_12V}.
     *
     * @param opening what it opens with when it would open with no letter: {@code pin} makes {@code 1} {@code pin1}
     */
    public static String name(String written, String opening) {
        return Identifiers.of(worded(written), opening);
    }

    /** The slug of a document named by these words: {@code kicad}, {@code power}, {@code +12V} is {@code kicad-power-plus-12v}. */
    public static String slug(String... words) {
        List<String> worded = new ArrayList<>();
        for (String word : words) {
            if (word != null && !word.isBlank()) {
                worded.add(worded(word));
            }
        }
        return Slugifier.standard().slug(String.join(" ", worded));
    }

    /**
     * The technical code a document another product brings is named by, in the namespace of the library it came from:
     * Altium's {@code rcl.PcbLib} footprint {@code R0402} is {@code FP.RCL:R0402}, Eagle's {@code con-jst} package
     * {@code B2B-XH-A} is {@code FP.CON-JST:B2B-XH-A}. Each part in capitals; a character a code does not write — a space, a
     * bracket — is a dash, two of them one, none at either end. A namespace writes no dot: the dot ends the class.
     *
     * @throws IllegalArgumentException when either part has nothing a code can write
     */
    public static String code(TechnicalClass kind, String library, String name) {
        String namespace = codePart(library, NAMESPACE_OUTSIDE);
        String designation = codePart(name, DESIGNATION_OUTSIDE);
        if (namespace.isEmpty() || designation.isEmpty()) {
            throw new IllegalArgumentException("'" + library + "' and '" + name + "' make no code: a code needs letters or digits in each.");
        }
        return kind.code(namespace + NAMESPACE_SEPARATOR + designation);
    }

    /**
     * The name a product's word is in a code, after its namespace — {@code R_0805 (2012)} is {@code R-0805-2012} —: what
     * {@link TechnicalClass#nameIn} reads back. Empty for a word with nothing a code can write.
     */
    public static String codeName(String written) {
        return codePart(written, DESIGNATION_OUTSIDE);
    }

    /** What separates a code's namespace from its designation: {@code CHIP} and {@code R0805} of {@code FP.CHIP:R0805}. */
    private static final String NAMESPACE_SEPARATOR = ":";
    private static final Pattern NAMESPACE_OUTSIDE = Pattern.compile("[^A-Z0-9]+");
    private static final Pattern DESIGNATION_OUTSIDE = Pattern.compile("[^A-Z0-9.]+");
    private static final Pattern DASHES_AT_ENDS = Pattern.compile("^-+|-+$");

    private static String codePart(String written, Pattern outside) {
        String upper = written == null ? "" : written.strip().toUpperCase(Locale.ROOT);
        return DASHES_AT_ENDS.matcher(outside.matcher(upper).replaceAll("-")).replaceAll("");
    }

    /** The letters a part's reference opens with, which its parts are named by: {@code U} of {@code U3}; empty for none. */
    public static Optional<String> designatorOf(String reference) {
        int letters = 0;
        while (letters < reference.length() && isLatinLetter(reference.charAt(letters))) {
            letters++;
        }
        return letters == 0 ? Optional.empty() : Optional.of(reference.substring(0, letters));
    }

    private static boolean isLatinLetter(char character) {
        return (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z');
    }

    /** The name with a sign at its opening and at its end written as its word. */
    private static String worded(String written) {
        String name = written.strip();
        if (name.length() < 2) {
            return name;
        }
        String opening = WORDS.getProperty(name.substring(0, 1));
        if (opening != null) {
            name = opening + " " + name.substring(1);
        }
        String closing = WORDS.getProperty(name.substring(name.length() - 1));
        if (closing != null) {
            name = name.substring(0, name.length() - 1) + " " + closing;
        }
        return name;
    }
}
