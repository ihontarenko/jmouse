package org.jmouse.schemas;

import org.jmouse.cad.pcb.drawing.Track;
import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.TypeScope;
import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;
import org.jmouse.descriptions.Description;
import org.jmouse.descriptions.Descriptions;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * 📐 A Java type said as a JSON Schema (draft 2020-12), as Jackson writes a value of it: a record an object of its components in the
 * order it declares them, with any getter Jackson writes beside them; a value that may be empty — an {@link Optional}, a type
 * marked {@link Nullable} — nullable, and left out only where the style leaves empty values out; a sealed type one of the types it
 * permits; an enum its words; a list an array; a map an object of its values. Each type and component is described by what its
 * Javadoc says ({@code jmouse-descriptions}); what it does not describe, and any type no schema can say, is noted beside the
 * schema rather than filled with a placeholder.
 */
public final class TypeSchemas {

    /** The draft every schema is written to. */
    public static final String DRAFT = "https://json-schema.org/draft/2020-12/schema";

    /** What a description keeps when an inline tag was not turned into words. */
    private static final Pattern UNWRITTEN_TAG = Pattern.compile("\\{@");

    /** Types written as a text in JSON beside those the generator knows. */
    private static final Set<Class<?>> WRITTEN_AS_TEXT = Set.of(Path.class, Duration.class, URI.class);

    /** Optionals of a number, which Jackson writes as the number or as null: the JSON type each is. */
    private static final Map<Class<?>, String> OPTIONAL_NUMBERS = Map.of(OptionalDouble.class, "number", OptionalInt.class, "integer",
                                                                         OptionalLong.class, "integer");

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private TypeSchemas() {
    }

    /** The schema of a type, written in a style. */
    public static WrittenSchema write(Type type, SchemaStyle style) {
        return new Writer(style).write(type);
    }

    /** Writes one schema, noting each thing it cannot say. */
    private static final class Writer {

        private final SchemaStyle style;
        private final Set<String> undescribed = new TreeSet<>();

        private Writer(SchemaStyle style) {
            this.style = style;
        }

        WrittenSchema write(Type type) {
            SchemaGeneratorConfigBuilder configuration = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                    .with(new JacksonSchemaModule())
                    .with(Option.SCHEMA_VERSION_INDICATOR, Option.FLATTENED_OPTIONALS, Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES,
                          Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT, Option.NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS,
                          Option.PLAIN_DEFINITION_KEYS)
                    .with(Option.NONSTATIC_NONVOID_NONGETTER_METHODS, Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS)
                    .without(Option.PUBLIC_STATIC_FIELDS,
                             Option.NONPUBLIC_STATIC_FIELDS, Option.VALUES_FROM_CONSTANT_FIELDS);
            if (style.nesting() == Nesting.INLINE) {
                configuration.with(Option.INLINE_ALL_SCHEMAS);
            } else {
                configuration.with(Option.DEFINITIONS_FOR_ALL_OBJECTS);
                configuration.forTypesInGeneral().withDefinitionNamingStrategy(new TypeNames());
            }
            configuration.forTypesInGeneral()
                         .withSubtypeResolver(Writer::permitted)
                         .withCustomDefinitionProvider(this::plainOrEnum)
                         .withDescriptionResolver(this::typeDescription)
                         .withPropertySorter(TypeSchemas::inOrder);
            configuration.forFields()
                         .withRequiredCheck(field -> !field.isFakeContainerItemScope()
                                                     && isRequired(field.getRawMember().getType(), field.getRawMember().getAnnotatedType()))
                         .withNullableCheck(field -> isEmptiable(field.getRawMember().getType(), field.getRawMember().getAnnotatedType()) ? Boolean.TRUE : null)
                         .withDescriptionResolver(this::componentDescription);
            // A getter beside a record's components — isUnplated() — is a property Jackson writes too: unplated. It is the model's to
            // say, never read back: read-only.
            configuration.forMethods()
                         .withIgnoreCheck(method -> !isGetter(method.getRawMember()))
                         .withRequiredCheck(method -> !method.isFakeContainerItemScope()
                                                      && isRequired(method.getRawMember().getReturnType(), method.getRawMember().getAnnotatedReturnType()))
                         .withNullableCheck(method -> isEmptiable(method.getRawMember().getReturnType(), method.getRawMember().getAnnotatedReturnType())
                                                      ? Boolean.TRUE : null)
                         .withReadOnlyCheck(method -> !method.isFakeContainerItemScope())
                         .withDescriptionResolver(this::getterDescription);
            style.marks().mark(configuration, undescribed::add);
            ObjectNode schema;
            try {
                schema = new SchemaGenerator(configuration.build()).generateSchema(type);
            } catch (IllegalArgumentException | UnsupportedOperationException unsaid) {
                undescribed.add(type.getTypeName() + ": " + unsaid.getMessage());
                schema = NODES.objectNode().put("$schema", DRAFT);
            }
            merged(schema);
            inlined(schema);
            emptyObjects(schema, "");
            return new WrittenSchema(schema, List.copyOf(undescribed));
        }

        /** A sealed type is one of the types it permits, each as its own object: what Jackson writes, deducing which by its fields. */
        private static List<ResolvedType> permitted(ResolvedType declared, SchemaGenerationContext context) {
            Class<?> erased = declared.getErasedType();
            if (!erased.isSealed()) {
                return null;
            }
            return Arrays.stream(erased.getPermittedSubclasses())
                         .filter(subclass -> !subclass.isInterface() || subclass.isSealed())
                         .map(subclass -> context.getTypeContext().resolveSubtype(declared, subclass))
                         .toList();
        }

        /** A value written as a text, and an enum as the words its style spells it in; any other type the generator's own. */
        private CustomDefinition plainOrEnum(ResolvedType type, SchemaGenerationContext context) {
            Class<?> erased = type.getErasedType();
            if (WRITTEN_AS_TEXT.contains(erased)) {
                return new CustomDefinition(NODES.objectNode().put("type", "string"), CustomDefinition.INLINE_DEFINITION,
                                            CustomDefinition.INCLUDING_ATTRIBUTES);
            }
            if (OPTIONAL_NUMBERS.containsKey(erased)) {
                ObjectNode number = NODES.objectNode();
                number.putArray("type").add(OPTIONAL_NUMBERS.get(erased)).add("null");
                return new CustomDefinition(number, CustomDefinition.INLINE_DEFINITION, CustomDefinition.INCLUDING_ATTRIBUTES);
            }
            if (!erased.isEnum()) {
                if (isUnsaid(erased)) {
                    undescribed.add(nameOf(erased) + ": a model's type is a record, an enum or a sealed type; no schema says what a "
                                    + erased.getSimpleName() + " is written as");
                }
                return null;
            }
            ObjectNode enumeration = NODES.objectNode().put("type", "string");
            ArrayNode words = enumeration.putArray("enum");
            for (Object constant : erased.getEnumConstants()) {
                words.add(style.enumWords().apply((Enum<?>) constant));
            }
            return new CustomDefinition(enumeration, CustomDefinition.INLINE_DEFINITION, CustomDefinition.INCLUDING_ATTRIBUTES);
        }

        /** What a record, an enum or a sealed type is, as its Javadoc says; noted where it says nothing. */
        private String typeDescription(TypeScope scope) {
            Class<?> erased = scope.getType().getErasedType();
            if (!isDescribed(erased)) {
                return null;
            }
            Optional<String> summary = Descriptions.of(erased).map(Description::summary).filter(words -> !words.isBlank());
            if (summary.isEmpty()) {
                undescribed.add(nameOf(erased) + ": its Javadoc says nothing of what it is");
                return null;
            }
            return said(nameOf(erased), summary.get());
        }

        /** What a record's component is, as the record's {@code @param} says; noted where it says nothing. */
        private String componentDescription(FieldScope field) {
            Class<?> record = field.getDeclaringType().getErasedType();
            if (field.isFakeContainerItemScope() || !record.isRecord() || Modifier.isStatic(field.getRawMember().getModifiers())) {
                return null;
            }
            String where = nameOf(record) + "." + field.getDeclaredName();
            Optional<String> words = Descriptions.of(record).flatMap(said -> said.member(field.getDeclaredName())).filter(said -> !said.isBlank());
            if (words.isEmpty()) {
                undescribed.add(where + ": its record's Javadoc has no @param " + field.getDeclaredName());
                return null;
            }
            return said(where, words.get());
        }

        /** What a getter of a record is, as its own Javadoc says; noted where it says nothing. */
        private String getterDescription(MethodScope method) {
            Class<?> record = method.getDeclaringType().getErasedType();
            if (method.isFakeContainerItemScope() || !record.isRecord()) {
                return null;
            }
            String property = Descriptions.propertyOf(method.getRawMember().getName());
            String where = nameOf(record) + "." + property;
            Optional<String> words = Descriptions.of(record).flatMap(said -> said.member(property)).filter(said -> !said.isBlank());
            if (words.isEmpty()) {
                undescribed.add(where + ": its getter " + method.getRawMember().getName() + "() has no Javadoc");
                return null;
            }
            return said(where, words.get());
        }

        /** Whether a property must be there: always where empty values are written as null, else where it cannot be empty. */
        private boolean isRequired(Class<?> raw, AnnotatedType annotated) {
            return style.absence() == Absence.WRITTEN_AS_NULL || !isEmptiable(raw, annotated);
        }

        /**
         * An object said with nothing in it — no properties, nothing for its values — is a type the generator could not see into:
         * a class whose JSON is whatever its getters happen to be. Noted, never shipped as an empty promise.
         */
        private void emptyObjects(JsonNode node, String where) {
            if (node instanceof ObjectNode object && "object".equals(object.path("type").asString(""))
                && !object.has("properties") && !(object.get("additionalProperties") instanceof ObjectNode)) {
                undescribed.add((where.isEmpty() ? "the schema" : where) + ": is said as an object with nothing in it");
            }
            node.properties().forEach(field -> emptyObjects(field.getValue(), "$defs".equals(field.getKey()) || "properties".equals(field.getKey())
                                                                             ? where : where.isEmpty() ? field.getKey() : where + "." + field.getKey()));
            if (node.isArray()) {
                node.values().forEach(item -> emptyObjects(item, where));
            }
        }

        /** Words a reader is shown must be words: an inline tag the processor left as written is noted. */
        private String said(String where, String words) {
            if (UNWRITTEN_TAG.matcher(words).find()) {
                undescribed.add(where + ": its description keeps a Javadoc tag instead of words");
            }
            return words;
        }
    }

    /**
     * A value said in parts — its type's schema, then what its component says of it, which the generator joins with {@code allOf}
     * where both describe it — said as one: the component's words over its type's, as a reader of the property means them.
     */
    private static void merged(JsonNode node) {
        if (node instanceof ObjectNode object && object.get("allOf") instanceof ArrayNode parts
            && parts.valueStream().allMatch(JsonNode::isObject)) {
            object.remove("allOf");
            ObjectNode whole = NODES.objectNode();
            parts.valueStream().forEach(part -> whole.setAll((ObjectNode) part));
            whole.setAll(object.deepCopy());
            object.removeAll();
            object.setAll(whole);
        }
        node.values().forEach(TypeSchemas::merged);
    }

    /**
     * Only a type of the model is defined once and named: a map, and a value that may be null, are said where they are used —
     * {@code "additionalProperties": …}, {@code "anyOf": [null, …]} —, not as definitions a reader must look up.
     */
    private static void inlined(ObjectNode schema) {
        if (!(schema.get("$defs") instanceof ObjectNode defined)) {
            return;
        }
        Map<String, ObjectNode> said = new LinkedHashMap<>();
        defined.properties().forEach(entry -> {
            if (entry.getValue() instanceof ObjectNode definition && (entry.getKey().endsWith("-nullable") || isMap(definition))) {
                said.put("#/$defs/" + entry.getKey(), definition);
            }
        });
        said.keySet().forEach(reference -> defined.remove(reference.substring("#/$defs/".length())));
        // A definition said inline may itself use another said inline: until nothing is left to say.
        boolean changed = true;
        while (changed) {
            changed = sayInline(schema, said);
        }
        if (defined.isEmpty()) {
            schema.remove("$defs");
        }
    }

    private static boolean isMap(ObjectNode definition) {
        return "object".equals(definition.path("type").asString("")) && !definition.has("properties")
               && definition.get("additionalProperties") instanceof ObjectNode;
    }

    private static boolean sayInline(JsonNode node, Map<String, ObjectNode> said) {
        boolean changed = false;
        if (node instanceof ObjectNode object && object.get("$ref") != null && said.containsKey(object.get("$ref").asString())) {
            ObjectNode whole = said.get(object.get("$ref").asString()).deepCopy();
            object.remove("$ref");
            whole.setAll(object);
            object.removeAll();
            object.setAll(whole);
            changed = true;
        }
        for (JsonNode inside : node.values()) {
            changed |= sayInline(inside, said);
        }
        return changed;
    }

    /** Whether a type is one of the model's own — a record, an enum, a sealed type — and so described by its Javadoc. */
    public static boolean isDescribed(Class<?> type) {
        return type.isRecord() || type.isEnum() || type.isSealed();
    }

    /** A public instance method taking nothing and named as a getter — {@code getSurface()}, {@code isUnplated()} —, as Jackson writes one. */
    private static boolean isGetter(Method method) {
        String name = method.getName();
        return method.getParameterCount() == 0 && Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())
               && method.getReturnType() != void.class && isDescribed(method.getDeclaringClass())
               && (name.matches("get[A-Z].*") || name.matches("is[A-Z].*") && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class));
    }

    /**
     * The order properties are written in, the same every time: a record's components as it declares them, then its getters by
     * name — reflection hands methods over in no order it promises.
     */
    private static int inOrder(MemberScope<?, ?> one, MemberScope<?, ?> other) {
        boolean oneIsGetter = one instanceof MethodScope;
        boolean otherIsGetter = other instanceof MethodScope;
        if (oneIsGetter != otherIsGetter) {
            return oneIsGetter ? 1 : -1;
        }
        return oneIsGetter ? one.getSchemaPropertyName().compareTo(other.getSchemaPropertyName()) : 0;
    }

    /** Whether a value may be empty: an {@link Optional}, or a type marked {@link Nullable}. */
    private static boolean isEmptiable(Class<?> raw, AnnotatedType annotated) {
        return Optional.class.equals(raw) || OPTIONAL_NUMBERS.containsKey(raw) || annotated.isAnnotationPresent(Nullable.class);
    }

    /** A class of the model's own that is none of the types a schema can say: its JSON is whatever its getters happen to be. */
    private static boolean isUnsaid(Class<?> type) {
        return !type.isPrimitive() && !type.isArray() && !type.isInterface() && !isDescribed(type) && !type.getName().startsWith("java.")
               && !WRITTEN_AS_TEXT.contains(type);
    }

    /** A type as its schema names it: its name inside its package — {@code Track}. */
    public static String nameOf(Class<?> type) {
        String name = type.getName();
        String inPackage = type.getPackageName().isEmpty() ? name : name.substring(type.getPackageName().length() + 1);
        return inPackage.replace('$', '.');
    }
}
