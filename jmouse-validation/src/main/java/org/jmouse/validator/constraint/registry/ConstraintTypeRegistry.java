package org.jmouse.validator.constraint.registry;

import org.jmouse.helpers.Strings;
import org.jmouse.validator.constraint.api.Constraint;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Constraint types by the name an expression calls them. 🗂️
 *
 * <p>Lookup is case-insensitive, so {@code @NotBlank}, {@code @notBlank} and {@code @NOTBLANK} are one
 * constraint. That is a property of the whole registry rather than of one method, which is why
 * {@link #key(String)} exists and why nothing here touches the map without going through it.</p>
 *
 * <p>⚠️ <strong>It did not used to.</strong> {@link #register} and {@link #resolve} normalised while
 * {@link #alias} stored the alias raw, so every alias carrying an upper-case letter — and all four of
 * the standard ones do — was written under a key {@code resolve} could never look up. The failure was
 * invisible from here: an unresolved name is not ignored, it throws out of the expression adapter, so a
 * field validated with {@code @Min(0)} refused every record submitted to its form.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ConstraintTypeRegistry {

    private static final Function<String, String> NORMALIZER = String::toLowerCase;

    private final Map<String, Class<? extends Constraint>> types = new LinkedHashMap<>();

    /**
     * Registers a constraint under a name expressions may call it by.
     *
     * @param name what an expression writes after the {@code @}
     * @param type the constraint it builds
     * @return this registry, so registrations chain
     */
    public ConstraintTypeRegistry register(String name, Class<? extends Constraint> type) {
        types.put(key(name), type);
        return this;
    }

    /**
     * Gives an already-registered constraint a second name.
     *
     * <p>⚠️ <strong>An alias of something unregistered fails here rather than later.</strong> It used to
     * do nothing at all, which is the same silent hole one layer up: the name would parse, resolve to
     * nothing, and throw at the moment somebody submitted a record. Registration runs at bootstrap,
     * where a mistyped target can still be a startup that refuses to happen.</p>
     *
     * @param name  the constraint that already exists
     * @param alias the second name for it
     * @return this registry, so registrations chain
     * @throws IllegalArgumentException when {@code name} resolves to nothing
     */
    public ConstraintTypeRegistry alias(String name, String alias) {
        Class<? extends Constraint> type = resolve(name).orElseThrow(
                () -> new IllegalArgumentException(
                        "Cannot alias '" + alias + "' to '" + name + "': there is no constraint "
                        + "registered under that name. Registered: " + String.join(", ", types.keySet())
                ));

        types.put(key(alias), type);

        return this;
    }

    /**
     * Finds the constraint an expression named.
     *
     * @param name the name as written, in any case
     * @return the constraint type, or empty when nothing answers to that name
     */
    public Optional<Class<? extends Constraint>> resolve(String name) {
        return Optional.ofNullable(types.get(key(name)));
    }

    /**
     * The one place a name becomes a map key.
     *
     * @param name the name as written
     * @return the key it is stored and looked up under
     */
    private static String key(String name) {
        return Strings.normalize(name, NORMALIZER);
    }
}
