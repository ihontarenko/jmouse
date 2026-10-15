package org.jmouse.vault;

import org.jmouse.vault.source.VaultSource;
import org.jmouse.vault.source.WritableVaultSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 🗝️ Where every secret an application uses is asked for.
 *
 * <p>Sources are asked in order and the first that holds a name answers. That is what lets an
 * installation move one secret at a time: an environment variable keeps working until a database value
 * of the same name is set, which then wins without anything else changing.
 *
 * <p>⚠️ Ask <strong>per use</strong>, not once at start-up. A rotated token then needs no restart, and the
 * answer is whatever the sources say now.
 *
 * <p>At most one source is writable — the one {@link #writable()} returns — because "set this secret"
 * has to mean one place. Order still decides who answers, so a writable source placed after the
 * environment can be written to and still be shadowed; {@link SecretStatus#shadowed()} shows that.
 */
public final class Vault {

    /** 🔓 A value together with the source it came from. */
    public record Resolved(SecretName name, SecretValue value, String source) {
    }

    private final List<VaultSource> sources;

    public Vault(List<? extends VaultSource> sources) {
        this.sources = List.copyOf(Objects.requireNonNull(sources, "sources"));

        long writable = this.sources.stream().filter(WritableVaultSource.class::isInstance).count();

        if (writable > 1) {
            throw new IllegalArgumentException("A vault takes at most one writable source; this one was given " + writable + ".");
        }
    }

    public Optional<SecretValue> find(SecretName name) {
        return resolve(name).map(Resolved::value);
    }

    public Optional<SecretValue> find(String name) {
        return find(SecretName.of(name));
    }

    /** @throws MissingSecretException naming every place that was looked at */
    public SecretValue require(SecretName name) {
        return find(name).orElseThrow(() -> new MissingSecretException(name, lookedAt(name)));
    }

    public SecretValue require(String name) {
        return require(SecretName.of(name));
    }

    public Optional<Resolved> resolve(SecretName name) {
        for (VaultSource source : sources) {
            Optional<SecretValue> value = source.find(name);

            if (value.isPresent()) {
                return Optional.of(new Resolved(name, value.get(), source.name()));
            }
        }

        return Optional.empty();
    }

    /** 📋 Every secret any source holds, each once, with the one that answers first. */
    public List<SecretStatus> list() {
        Map<SecretName, List<SecretEntry>> byName = new LinkedHashMap<>();

        for (VaultSource source : sources) {
            for (SecretEntry entry : source.list()) {
                byName.computeIfAbsent(entry.name(), ignored -> new ArrayList<>()).add(entry);
            }
        }

        return byName.values().stream()
                .map(this::statusOf)
                .sorted((left, right) -> left.name().compareTo(right.name()))
                .toList();
    }

    public Optional<SecretStatus> status(SecretName name) {
        List<SecretEntry> entries = new ArrayList<>();

        for (VaultSource source : sources) {
            source.list().stream().filter(entry -> entry.name().equals(name)).forEach(entries::add);
        }

        return entries.isEmpty() ? Optional.empty() : Optional.of(statusOf(entries));
    }

    public Optional<WritableVaultSource> writable() {
        return sources.stream()
                .filter(WritableVaultSource.class::isInstance)
                .map(WritableVaultSource.class::cast)
                .findFirst();
    }

    public List<String> sourceNames() {
        return sources.stream().map(VaultSource::name).toList();
    }

    /** 🧭 Each source's name, with where in it the secret would be read from when it says. */
    public List<String> lookedAt(SecretName name) {
        return sources.stream()
                .map(source -> source.locate(name) == null
                        ? source.name()
                        : source.name() + " (" + source.locate(name) + ")")
                .toList();
    }

    private SecretStatus statusOf(List<SecretEntry> entries) {
        SecretEntry resolved = entries.getFirst();
        boolean     editable = writable().map(source -> source.name().equals(resolved.source())).orElse(false);

        return new SecretStatus(resolved, List.copyOf(entries.subList(1, entries.size())), editable);
    }
}
