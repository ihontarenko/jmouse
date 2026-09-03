package org.jmouse.validator.el.runtime;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every check a {@code .jmv} may write, and what each one means. 📚
 *
 * <p>Open, like the constraint registry underneath it: a product registering a constraint of its own
 * registers a signature for it here and its checks read the same as the built-in ones.</p>
 *
 * <h2>⚠️ Why {@code min} and {@code max} are separate words for one constraint</h2>
 *
 * <p>{@code MinMax} takes a {@code mode}, and making an author write {@code minMax(mode: 'min', …)} is
 * making them say twice what they meant once. Three signatures over one constraint is the table doing
 * its job: the vocabulary a person writes and the beans a library binds are allowed to differ, and this
 * is the one place that difference is recorded.</p>
 *
 * <h2>⚠️ {@code optional} maps to nothing, deliberately</h2>
 *
 * <p>Every constraint treats {@code null} as valid, so a check saying "this may be absent" has no
 * constraint to build. It is kept as a word because a line reading only {@code url(…)} looks like an
 * oversight, and a reader cannot otherwise tell a field nobody thought about from one somebody decided
 * about. The runtime skips it; the form-builder draws it as a switch.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class CheckSignatures {

    /** The word that says a field may be absent, and builds no constraint. */
    public static final String OPTIONAL = "optional";

    private final Map<String, CheckSignature> signatures = new LinkedHashMap<>();

    private CheckSignatures() {
    }

    /**
     * The vocabulary the library ships with.
     *
     * @return a fresh catalogue holding every built-in check
     */
    public static CheckSignatures standard() {
        CheckSignatures catalogue = new CheckSignatures();

        catalogue.register(CheckSignature.of("required", "required"));
        catalogue.register(CheckSignature.of("notBlank", "notBlank"));
        catalogue.register(CheckSignature.of("notEmpty", "notBlank"));

        catalogue.register(CheckSignature.of("min", "MinMax", "min").with("mode", "min"));
        catalogue.register(CheckSignature.of("max", "MinMax", "max").with("mode", "max"));
        catalogue.register(CheckSignature.of("range", "MinMax", "min", "max").with("mode", "range"));

        catalogue.register(CheckSignature.of("size", "size", "min", "max"));
        catalogue.register(CheckSignature.of("length", "size", "min", "max"));

        catalogue.register(CheckSignature.of("pattern", "pattern", "regex"));
        catalogue.register(CheckSignature.of("matches", "pattern", "regex"));

        catalogue.register(CheckSignature.of("email", "email", "domain"));
        catalogue.register(CheckSignature.of("url", "webLink", "host"));
        catalogue.register(CheckSignature.of("webLink", "webLink", "host"));

        catalogue.register(CheckSignature.collecting("oneOf", "oneOf", "allowed"));

        return catalogue;
    }

    /**
     * Adds a signature, or replaces the one under that name.
     *
     * @param signature what to register
     * @return this catalogue, so registrations chain
     */
    public CheckSignatures register(CheckSignature signature) {
        signatures.put(key(signature.check()), signature);

        return this;
    }

    /**
     * Finds what a check written in a file means.
     *
     * @param check the word as written, in any case
     * @return its signature, or empty when nothing answers to that word
     */
    public Optional<CheckSignature> resolve(String check) {
        return Optional.ofNullable(signatures.get(key(check)));
    }

    /**
     * Whether a word builds no constraint by design.
     *
     * @param check the word as written
     * @return {@code true} for {@code optional}
     */
    public static boolean isMarker(String check) {
        return OPTIONAL.equalsIgnoreCase(check);
    }

    /** The words a refusal lists, so an author learns the vocabulary from the failure. */
    public String available() {
        return String.join(", ", signatures.keySet());
    }

    /**
     * Every check registered, in the order they were registered.
     *
     * <p>⚠️ For a form to <strong>offer</strong> them — which is why order matters: a builder's list of
     * checks reads in the order this catalogue was written, and an alphabetical one would put
     * {@code email} above {@code required}.</p>
     *
     * @return the signatures
     */
    public Collection<CheckSignature> all() {
        return List.copyOf(signatures.values());
    }

    private static String key(String check) {
        return check == null ? "" : check.toLowerCase();
    }
}
