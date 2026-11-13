package org.jmouse.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 🎚️ An application's settings — the one source of every value it is set to: the sections its owners declare, each value a row
 * under its holder (the installation, or a reader over it), and the one way they are changed — checked against the declaration,
 * kept all at once or not at all.
 *
 * <p>The installation holds a value of every parameter: {@link #seed()} keeps each section's seed at the first start, and what a
 * later release declares once it is not kept yet — a list the installation already holds is not seeded again, so an item taken
 * away stays away. Bringing a value back keeps its seed again. A reader holds rows only of what they changed and reads the
 * installation's for every other key. A secret is never seeded: its deployment stands until somebody keeps another, sealed.</p>
 *
 * <p>The declarations are read when a value is first asked for, not when this is made: an owner's own parts read their settings
 * through this, so it cannot wait for the owners to be made. Without a {@link ParameterStore} every value stands at its seed and
 * a change is refused — a change that would be lost at the next start is not offered as kept.</p>
 */
public final class Settings {

    /** A section, and who declares it. */
    public record Owned(String owner, Section section) {
    }

    /** Where a secret parameter's value comes from now: all a reader is ever told of one. */
    public enum SecretState {
        /** It has none. */
        NONE,
        /** The deployment's: nobody kept another. */
        DEPLOYED,
        /** One kept here, sealed. */
        KEPT
    }

    /** Which holder's values of which owner's section. */
    private record Kept(SettingHolder holder, String owner, String section) {
    }

    /** A folded section as the record it is read into. */
    private record Read(Class<?> type, Map<String, String> parameters) {
    }

    private final Supplier<? extends Map<String, ? extends Collection<Section>>> declarations;
    private final Optional<ParameterStore>                                       store;
    private final Optional<SecretSealing>                                        secrets;
    private final Optional<SectionReader>                                        reader;
    private final Map<Kept, Map<String, String>>                                 kept = new ConcurrentHashMap<>();
    private final Map<Read, Object>                                              read = new ConcurrentHashMap<>();
    /** What is told when the installation's values of a section change, by owner and path. */
    private final Map<String, List<Runnable>>                                    listeners = new ConcurrentHashMap<>();
    private volatile Map<String, Map<String, Section>>                           declared;

    /**
     * @param declarations every owner's sections, by the owner's word ({@code cad-engine}, {@code cad-gerber})
     * @param store        where the values are kept; empty for an application that keeps none
     * @param secrets      what seals a secret where it is kept; empty to keep none
     * @param reader       what reads a structured section into its record; empty for an application that declares none
     */
    public Settings(Supplier<? extends Map<String, ? extends Collection<Section>>> declarations, Optional<ParameterStore> store,
                    Optional<SecretSealing> secrets, Optional<SectionReader> reader) {
        this.declarations = declarations;
        this.store = store;
        this.secrets = secrets;
        this.reader = reader;
    }

    /** Every section, by owner in their order, each owner's in the order it declares them. */
    public List<Owned> sections() {
        List<Owned> sections = new ArrayList<>();
        declared().forEach((owner, owned) -> owned.values().forEach(section -> sections.add(new Owned(owner, section))));
        return sections;
    }

    /** An owner's sections, in the order it declares them; none for an owner that declares nothing or is unknown. */
    public List<Section> sectionsOf(String owner) {
        return List.copyOf(declared().getOrDefault(owner, Map.of()).values());
    }

    /** An owner's section at a path; empty for none. */
    public Optional<Section> section(String owner, String path) {
        return Optional.ofNullable(declared().getOrDefault(owner, Map.of()).get(path));
    }

    /** Whether a change can be kept: the application keeps its settings somewhere. */
    public boolean changeable() {
        return store.isPresent();
    }

    /** Whether a secret can be kept here. */
    public boolean keepsSecrets() {
        return secrets.isPresent();
    }

    /**
     * Keeps every value the installation holds none of yet — its seed —, a secret's never: at the first start, every value; after a
     * release that declares more, the new ones; an item of a list the installation already holds, never.
     *
     * @return how many values were kept
     */
    public int seed() {
        if (store.isEmpty()) {
            return 0;
        }
        int seeded = 0;
        for (Owned owned : sections()) {
            Map<String, String> already = keptBy(SettingHolder.INSTALLATION, owned.owner(), owned.section().path());
            Map<String, String> missing = new LinkedHashMap<>();
            owned.section().seed().forEach((key, value) -> {
                if (!already.containsKey(key) && !underHeldList(key, already.keySet())) {
                    missing.put(key, value);
                }
            });
            if (!missing.isEmpty()) {
                store.get().keep(SettingHolder.INSTALLATION, owned.owner(), owned.section().path(), missing);
                forgetRead(SettingHolder.INSTALLATION, owned.owner(), owned.section().path());
                seeded += missing.size();
            }
        }
        return seeded;
    }

    /** Whether a key stands in a list the installation already holds: the list is what the installation made it. */
    private static boolean underHeldList(String key, Set<String> held) {
        List<ParameterTree.Segment> segments = ParameterTree.segmentsOf(key);
        for (int position = 0; position < segments.size(); position++) {
            if (segments.get(position).index()) {
                List<ParameterTree.Segment> list = segments.subList(0, position);
                return held.stream().anyMatch(other -> startsWith(ParameterTree.segmentsOf(other), list));
            }
        }
        return false;
    }

    private static boolean startsWith(List<ParameterTree.Segment> segments, List<ParameterTree.Segment> prefix) {
        return segments.size() > prefix.size() && segments.subList(0, prefix.size()).equals(prefix)
               || segments.equals(prefix);
    }

    /** A section's values as the installation has them, by key; a secret's never. */
    public Map<String, String> valuesOf(String owner, String path) {
        return valuesOf(owner, path, SettingHolder.INSTALLATION);
    }

    /** A section's values as they stand for a holder — a reader's own over the installation's —, by key; a secret's never. */
    public Map<String, String> valuesOf(String owner, String path, SettingHolder holder) {
        Section section = requireSection(owner, path);
        Map<String, String> values = new LinkedHashMap<>(installationOf(owner, section));
        if (holder.isReader() && section.scope() == SettingScope.READER) {
            values.putAll(keptBy(holder, owner, path));
        }
        secretKeys(section).forEach(values::remove);
        return Collections.unmodifiableMap(values);
    }

    /** The values a holder keeps themselves of a section — a reader's own, without the installation's —; a secret's never. */
    public Map<String, String> ownOf(String owner, String path, SettingHolder holder) {
        Section section = requireSection(owner, path);
        Map<String, String> own = new LinkedHashMap<>(keptBy(holder, owner, path));
        secretKeys(section).forEach(own::remove);
        return Collections.unmodifiableMap(own);
    }

    /** A section of fixed parameters as its owner reads them — the installation's —, each value read as it stands then. */
    public ParameterValues parameters(String owner, String path) {
        return parameters(owner, path, SettingHolder.INSTALLATION);
    }

    /** A section of fixed parameters as they stand for a holder. */
    public ParameterValues parameters(String owner, String path, SettingHolder holder) {
        return new ParameterValues(this, owner, path, holder);
    }

    /** A structured section read into its record — the installation's —, read again only once its values change. */
    public <T> T read(String owner, String path, Class<T> type) {
        return read(owner, path, type, SettingHolder.INSTALLATION);
    }

    /** A structured section read into its record as it stands for a holder. */
    public <T> T read(String owner, String path, Class<T> type, SettingHolder holder) {
        Section.Structured structured = requireStructured(owner, path);
        if (!structured.type().equals(type)) {
            throw new IllegalArgumentException("Section '" + path + "' of '" + owner + "' is " + structured.type().getSimpleName()
                                               + ", not " + type.getSimpleName() + ".");
        }
        Map<String, String> values = valuesOf(owner, path, holder);
        return type.cast(read.computeIfAbsent(new Read(type, values), unread -> requireReader(path).read(ParameterTree.fold(values), type)));
    }

    /**
     * Changes values of a holder's section, key by key: every value checked against the declaration — a structured section by
     * reading it whole into its record —, then all kept at once. A null value brings the key back: the installation's to its seed
     * (a key the seed has none of is taken away), a reader's to the installation's.
     *
     * @throws SettingRefused naming what was refused and why; then nothing is kept
     */
    public void change(String owner, String path, SettingHolder holder, Map<String, String> changes) {
        ParameterStore keeping = requireStore();
        Section section = requireChangeable(owner, path, holder);
        Map<String, String> kept = new LinkedHashMap<>();
        Set<String> forgotten = new HashSet<>();
        Map<String, String> seed = section.seed();
        for (Map.Entry<String, String> change : changes.entrySet()) {
            String key = change.getKey();
            String value = change.getValue();
            Optional<Parameter> parameter = parameterOf(section, key);
            if (section.shape() instanceof Section.Fixed && parameter.isEmpty()) {
                throw new SettingRefused(section.label() + " has no parameter '" + key + "'.");
            }
            if (parameter.isPresent() && value != null) {
                Optional<String> refusal = parameter.get().refusalOf(value);
                if (refusal.isPresent()) {
                    throw new SettingRefused(parameter.get().label() + " was not changed: " + refusal.get() + ".");
                }
            }
            if (parameter.isPresent() && parameter.get().kind() == ParameterKind.SECRET) {
                if (value == null || value.isBlank()) {
                    forgotten.add(key);
                } else {
                    kept.put(key, requireSecrets(parameter.get()).seal(value.strip(), contextOf(owner, path, key)));
                }
            } else if (value != null) {
                kept.put(key, value);
            } else if (!holder.isReader() && seed.containsKey(key)) {
                kept.put(key, seed.get(key));
            } else {
                forgotten.add(key);
            }
        }
        if (section.shape() instanceof Section.Structured structured) {
            Map<String, String> next = new LinkedHashMap<>(holder.isReader() ? valuesOf(owner, path, holder) : installationOf(owner, section));
            forgotten.forEach(next::remove);
            if (holder.isReader()) {
                forgotten.forEach(key -> Optional.ofNullable(installationOf(owner, section).get(key)).ifPresent(back -> next.put(key, back)));
            }
            next.putAll(kept);
            requireReadable(section, structured, next);
        }
        if (!kept.isEmpty()) {
            keeping.keep(holder, owner, path, kept);
        }
        if (!forgotten.isEmpty()) {
            keeping.forget(holder, owner, path, forgotten);
        }
        forgetRead(holder, owner, path);
        changed(holder, owner, path);
    }

    /**
     * A holder's section as these values alone — what an editor of a whole record keeps, its lists grown or shrunk —, checked as
     * a change is. A secret is never kept this way.
     *
     * @throws SettingRefused naming what was refused and why; then nothing is kept
     */
    public void replace(String owner, String path, SettingHolder holder, Map<String, String> values) {
        ParameterStore keeping = requireStore();
        Section section = requireChangeable(owner, path, holder);
        for (Map.Entry<String, String> value : values.entrySet()) {
            Optional<Parameter> parameter = parameterOf(section, value.getKey());
            if (section.shape() instanceof Section.Fixed && parameter.isEmpty()) {
                throw new SettingRefused(section.label() + " has no parameter '" + value.getKey() + "'.");
            }
            if (parameter.isPresent() && parameter.get().kind() == ParameterKind.SECRET) {
                throw new SettingRefused(parameter.get().label() + " is a key to a service: it is changed alone, never with a section.");
            }
            if (parameter.isPresent()) {
                Optional<String> refusal = parameter.get().refusalOf(value.getValue());
                if (refusal.isPresent()) {
                    throw new SettingRefused(parameter.get().label() + " was not changed: " + refusal.get() + ".");
                }
            }
        }
        Map<String, String> kept = new LinkedHashMap<>(values);
        if (!holder.isReader()) {
            // A section's keys to services are kept beside its values, never replaced with them.
            Map<String, String> sealed = keptBy(holder, owner, path);
            secretKeys(section).forEach(key -> Optional.ofNullable(sealed.get(key)).ifPresent(value -> kept.put(key, value)));
        }
        if (section.shape() instanceof Section.Structured structured) {
            Map<String, String> next = new LinkedHashMap<>(holder.isReader() ? installationOf(owner, section) : Map.of());
            next.putAll(values);
            requireReadable(section, structured, next);
        }
        keeping.replace(holder, owner, path, kept);
        forgetRead(holder, owner, path);
        changed(holder, owner, path);
    }

    /** Brings a holder's section back: the installation's to its seed — its keys to services kept —, a reader's to the installation's. */
    public void restore(String owner, String path, SettingHolder holder) {
        Section section = requireChangeable(owner, path, holder);
        replace(owner, path, holder, holder.isReader() ? Map.of() : section.seed());
    }

    /**
     * Tells {@code listener} each time the installation's values of a section are changed here — what holds something made from
     * them, a store opened on a folder, makes it again. Never for a reader's own.
     */
    public void whenChanged(String owner, String path, Runnable listener) {
        listeners.computeIfAbsent(owner + "/" + path, unknown -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(listener);
    }

    private void changed(SettingHolder holder, String owner, String path) {
        if (!holder.isReader()) {
            listeners.getOrDefault(owner + "/" + path, List.of()).forEach(Runnable::run);
        }
    }

    /** Where one of a section's secrets stands. */
    public SecretState secretStateOf(String owner, String path, Parameter parameter) {
        if (keptBy(SettingHolder.INSTALLATION, owner, path).containsKey(parameter.key())) {
            return SecretState.KEPT;
        }
        return parameter.defaultValue().isBlank() ? SecretState.NONE : SecretState.DEPLOYED;
    }

    /** A parameter's value as it stands for a holder, asked for as {@code kind} — null to take it as text. */
    String parameterOf(String owner, String path, String key, ParameterKind kind, SettingHolder holder) {
        Section section = requireSection(owner, path);
        Parameter parameter = parameterOf(section, key).orElseThrow(() -> new IllegalArgumentException(
                "Section '" + path + "' of '" + owner + "' declares no parameter '" + key + "'."));
        if (kind != null && parameter.kind() != kind) {
            throw new IllegalArgumentException("Parameter '" + key + "' of '" + path + "' is " + parameter.kind() + ", not " + kind + ".");
        }
        if (parameter.kind() == ParameterKind.SECRET) {
            String sealed = keptBy(SettingHolder.INSTALLATION, owner, path).get(key);
            return sealed == null ? parameter.defaultValue() : requireSecrets(parameter).open(sealed, contextOf(owner, path, key));
        }
        return valuesOf(owner, path, holder).getOrDefault(key, parameter.defaultValue());
    }

    /**
     * The installation's values of a section: a fixed one's defaults with what it keeps over them; a structured one's kept values,
     * its seed while it keeps none.
     */
    private Map<String, String> installationOf(String owner, Section section) {
        Map<String, String> held = keptBy(SettingHolder.INSTALLATION, owner, section.path());
        if (section.shape() instanceof Section.Fixed) {
            Map<String, String> values = new LinkedHashMap<>(section.seed());
            values.putAll(held);
            return values;
        }
        return held.isEmpty() ? section.seed() : held;
    }

    private void requireReadable(Section section, Section.Structured structured, Map<String, String> values) {
        try {
            requireReader(section.path()).read(ParameterTree.fold(values), structured.type());
        } catch (IllegalArgumentException refused) {
            throw new SettingRefused(section.label() + " was not changed: " + refused.getMessage());
        }
    }

    private Section requireChangeable(String owner, String path, SettingHolder holder) {
        Section section = requireSection(owner, path);
        if (holder.isReader() && section.scope() != SettingScope.READER) {
            throw new SettingRefused(section.label() + " is the installation's, not a reader's own.");
        }
        return section;
    }

    private Section requireSection(String owner, String path) {
        return section(owner, path).orElseThrow(() -> new IllegalArgumentException("'" + owner + "' declares no section '" + path + "'."));
    }

    private Section.Structured requireStructured(String owner, String path) {
        if (requireSection(owner, path).shape() instanceof Section.Structured structured) {
            return structured;
        }
        throw new IllegalArgumentException("Section '" + path + "' of '" + owner + "' is of fixed parameters, read through parameters().");
    }

    private static Optional<Parameter> parameterOf(Section section, String key) {
        return section.shape() instanceof Section.Fixed fixed
                ? fixed.parameters().stream().filter(parameter -> parameter.key().equals(key)).findFirst()
                : Optional.empty();
    }

    private static List<String> secretKeys(Section section) {
        return section.shape() instanceof Section.Fixed fixed
                ? fixed.parameters().stream().filter(parameter -> parameter.kind() == ParameterKind.SECRET).map(Parameter::key).toList()
                : List.of();
    }

    private ParameterStore requireStore() {
        return store.orElseThrow(() -> new SettingRefused("This application keeps no settings: they stand at their defaults."));
    }

    private SecretSealing requireSecrets(Parameter parameter) {
        return secrets.orElseThrow(() -> new SettingRefused(parameter.label() + " cannot be kept: this installation has no key to seal it with."));
    }

    private SectionReader requireReader(String path) {
        return reader.orElseThrow(() -> new IllegalStateException(
                "Section '" + path + "' is structured, and this application says nothing of how it is read."));
    }

    private static String contextOf(String owner, String path, String key) {
        return "setting:" + owner + ":" + path + ":" + key;
    }

    private Map<String, String> keptBy(SettingHolder holder, String owner, String path) {
        return kept.computeIfAbsent(new Kept(holder, owner, path), unread -> Collections.unmodifiableMap(
                new LinkedHashMap<>(store.map(keeping -> keeping.kept(holder, owner, path)).orElse(Map.of()))));
    }

    private void forgetRead(SettingHolder holder, String owner, String path) {
        kept.remove(new Kept(holder, owner, path));
    }

    private Map<String, Map<String, Section>> declared() {
        Map<String, Map<String, Section>> known = declared;
        if (known == null) {
            Map<String, Map<String, Section>> read = new LinkedHashMap<>();
            declarations.get().forEach((owner, sections) -> {
                Map<String, Section> byPath = new LinkedHashMap<>();
                for (Section section : sections) {
                    if (byPath.putIfAbsent(section.path(), section) != null) {
                        throw new IllegalStateException("'" + owner + "' declares section '" + section.path() + "' twice.");
                    }
                }
                read.put(owner, Collections.unmodifiableMap(byPath));
            });
            known = Collections.unmodifiableMap(read);
            declared = known;
        }
        return known;
    }
}
