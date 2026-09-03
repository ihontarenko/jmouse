package org.jmouse.files.directory;

import org.jmouse.files.exception.DirectoryException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 🗂️ Every kind of configuration a folder may carry, and the one place a name is checked against.
 *
 * <h3>⚠️ Not optional, and not merely tidy</h3>
 *
 * <p>A table with a free-text {@code kind} is a junk drawer in six months: rows nobody reads, written
 * by a module that no longer exists, in a shape nothing can bind. So an unknown kind is <strong>refused
 * on write</strong> rather than stored and ignored. The registry is the whole difference between an
 * extensible schema and an untyped one.</p>
 *
 * <p>Contributed rather than enumerated — a module hands over its kinds and this holds them, the same
 * way {@code OwnerReference}'s owner kinds belong to whoever files against them. {@code upload} is
 * registered because this library ships it.</p>
 */
public final class DirectoryConfigurationKinds {

    private final Map<String, DirectoryConfigurationKind<?>> kinds = new LinkedHashMap<>();

    /**
     * 🏗️ Hold these kinds, and the {@code upload} kind this library ships.
     *
     * @param contributed kinds a module contributes
     */
    public DirectoryConfigurationKinds(Collection<DirectoryConfigurationKind<?>> contributed) {
        register(DirectoryUploadConfiguration.KIND);

        if (contributed != null) {
            contributed.forEach(this::register);
        }
    }

    /**
     * 🏗️ Hold only what this library ships.
     */
    public DirectoryConfigurationKinds() {
        this(List.of());
    }

    /**
     * ➕ Take a kind into the registry.
     *
     * <p>⚠️ Two modules claiming one name is refused rather than resolved by order: whichever won would
     * decide how every row of that kind binds, and the loser's rows would still be in the table.</p>
     *
     * @param kind the kind to register
     */
    public void register(DirectoryConfigurationKind<?> kind) {
        DirectoryConfigurationKind<?> existing = kinds.putIfAbsent(kind.name(), kind);

        if (existing != null && !existing.equals(kind)) {
            throw new DirectoryException(
                "Two different configuration kinds are both called '%s' — %s and %s."
                    .formatted(kind.name(), existing.payloadType().getName(),
                               kind.payloadType().getName()));
        }
    }

    /**
     * 🔎 The kind of this name, where there is one.
     *
     * @param name what to look for; case and surrounding space are ignored
     * @return the kind, or empty
     */
    public Optional<DirectoryConfigurationKind<?>> find(String name) {
        if (name == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(kinds.get(name.trim().toLowerCase(Locale.ROOT)));
    }

    /**
     * 🔎 The kind of this name, or a refusal naming the ones that would have worked.
     *
     * @param name what to look for
     * @return the kind
     */
    public DirectoryConfigurationKind<?> require(String name) {
        return find(name).orElseThrow(() -> new DirectoryException(
            "'%s' is not a kind of directory configuration. This installation knows: %s."
                .formatted(name, String.join(", ", names()))));
    }

    /**
     * 📋 What this installation knows about, in registration order.
     *
     * @return the kind names
     */
    public List<String> names() {
        return List.copyOf(kinds.keySet());
    }

    /**
     * 📋 The kinds themselves, in registration order.
     *
     * @return the kinds
     */
    public List<DirectoryConfigurationKind<?>> all() {
        return List.copyOf(kinds.values());
    }
}
