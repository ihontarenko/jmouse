package org.jmouse.cad.settings;

import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import org.jmouse.schemas.Absence;
import org.jmouse.schemas.SchemaStyle;
import org.jmouse.schemas.TypeSchemas;
import org.jmouse.schemas.WrittenSchema;
import org.jmouse.settings.Bounds;
import org.jmouse.settings.Colour;
import org.jmouse.settings.Label;
import org.jmouse.settings.ParameterTree;
import org.jmouse.settings.RecordChecks;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.io.UncheckedIOException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 🎚️ A section's JSON Schema, written from its record by {@link TypeSchemas} — every component a property, described by its
 * {@code @param} as the record's Javadoc says it, nested records, lists, maps and optionals followed down and written where they
 * are used, an enum by the words its file says it in — and marked for the settings window: each component's {@link Label} its
 * title and unit, its {@link Bounds} and {@link Colour} what the window draws a slider or a picker for. Nothing of it is written by
 * hand, so what a reader is told and what the engine reads cannot drift apart; what cannot be written — a component its Javadoc
 * does not describe or no label names, a type no schema says — is a mismatch, which every module's test asks for.
 */
public final class SettingsSchemas {

    /** The draft every schema is written to. */
    public static final String DRAFT = TypeSchemas.DRAFT;

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    /** How a section is written: inline, as the window reads it top to bottom; an optional value one its file may leave out; its enums in its file's words; marked for the window. */
    private static final SchemaStyle STYLE = SchemaStyle.defined().inline().absent(Absence.LEFT_OUT).spelling(CadSectionReader::written).marking(SettingsSchemas::marks);

    private static final Map<String, Written> WRITTEN = new ConcurrentHashMap<>();

    private SettingsSchemas() {
    }

    /** A section's schema, written once a process. */
    public static ObjectNode of(SettingsSection<?> section) {
        return written(section).schema().deepCopy();
    }

    /**
     * What is wrong with a section, in words: its file not read into its record, its rows (ADR 0007) not folding back into the
     * record its file is, or its schema not able to say what the record reads; none where all three hold.
     */
    public static List<String> mismatches(SettingsSection<?> section) {
        List<String> found = new ArrayList<>();
        try {
            Object shipped = CadSettings.shipped().read(section);
            Object folded = CadSectionReader.JSON.read(ParameterTree.fold(CadSettings.declarationOf(section).seed()), section.type());
            if (!Objects.deepEquals(shipped, folded)) {
                found.add(section.path() + ": its rows fold back into another " + section.type().getSimpleName() + " than its file is");
            }
            // What ships is held to its own marks: a release never ships a value its window would refuse.
            RecordChecks.refusalOf(shipped).ifPresent(refusal -> found.add(section.path() + ": ships " + refusal));
        } catch (IllegalStateException | IllegalArgumentException | UncheckedIOException unread) {
            found.add(section.path() + ": " + unread.getMessage());
        }
        found.addAll(written(section).undescribed());
        return found;
    }

    /** Every mismatch of every section given. */
    public static List<String> mismatches(Collection<? extends SettingsSection<?>> sections) {
        return sections.stream().flatMap(section -> mismatches(section).stream()).toList();
    }

    private static Written written(SettingsSection<?> section) {
        return WRITTEN.computeIfAbsent(section.path(), path -> write(section));
    }

    /** A schema, and what could not be said in it. */
    private record Written(ObjectNode schema, List<String> undescribed) {
    }

    /** A section's schema: its type's, headed by where it is and what it is called. */
    private static Written write(SettingsSection<?> section) {
        WrittenSchema written = TypeSchemas.write(section.type(), STYLE);
        ObjectNode schema = NODES.objectNode();
        schema.put("$schema", DRAFT);
        schema.put("$id", "org/jmouse/cad/settings/" + section.path() + ".schema.json");
        schema.put("title", section.title());
        Optional.ofNullable(written.schema().get("description")).or(() -> Optional.ofNullable(written.schema().path("items").get("description")))
                .ifPresent(description -> schema.set("description", description));
        written.schema().properties().stream()
               .filter(field -> !Map.of("$schema", 0, "title", 0, "description", 0).containsKey(field.getKey()))
               .forEach(field -> schema.set(field.getKey(), field.getValue()));
        return new Written(schema, written.undescribed().stream().map(line -> section.path() + ": " + line).toList());
    }

    /**
     * What the settings window reads beside the type: a component's {@link Label} — its title, and its unit —, required of every
     * component, a record's for what one of a list is called, and {@link Bounds} and {@link Colour} on the value itself or, for a
     * list, on each of its items — the same marks the engine holds a change to ({@link RecordChecks}).
     */
    private static void marks(SchemaGeneratorConfigBuilder configuration, Consumer<String> undescribed) {
        configuration.forFields().withTitleResolver(field -> {
            if (field.isFakeContainerItemScope()) {
                return null;
            }
            Optional<Label> label = componentOf(field).map(component -> component.getAnnotation(Label.class)).filter(said -> !said.value().isBlank());
            if (label.isEmpty()) {
                undescribed.accept(TypeSchemas.nameOf(field.getDeclaringType().getErasedType()) + "." + field.getDeclaredName() + ": has no @Label: a reader is never shown a key");
            }
            return label.map(Label::value).orElse(null);
        });
        configuration.forFields().withInstanceAttributeOverride((property, field, context) -> {
            Optional<RecordComponent> component = componentOf(field);
            if (component.isEmpty()) {
                return;
            }
            Label label = component.get().getAnnotation(Label.class);
            if (!field.isFakeContainerItemScope() && label != null && !label.unit().isBlank()) {
                property.put("unit", label.unit());
            }
            if (field.isFakeContainerItemScope() == field.isContainerType()) {
                return;
            }
            Bounds bounds = component.get().getAnnotation(Bounds.class);
            if (bounds != null) {
                property.put("minimum", bounds.minimum());
                property.put("maximum", bounds.maximum());
                if (bounds.step() > 0) {
                    property.put("multipleOf", bounds.step());
                }
            }
            if (component.get().isAnnotationPresent(Colour.class)) {
                property.put("format", "color");
            }
        });
        // A record that is a list's item says what one is called: «Paper» → «Paper 1».
        configuration.forTypesInGeneral().withTitleResolver(scope -> Optional.ofNullable(scope.getType().getErasedType().getAnnotation(Label.class))
                                                                             .map(Label::value).orElse(null));
    }

    /** The record component a field of a record is. */
    private static Optional<RecordComponent> componentOf(FieldScope field) {
        Class<?> record = field.getDeclaringType().getErasedType();
        if (!record.isRecord()) {
            return Optional.empty();
        }
        return Arrays.stream(record.getRecordComponents()).filter(component -> component.getName().equals(field.getDeclaredName())).findFirst();
    }
}
