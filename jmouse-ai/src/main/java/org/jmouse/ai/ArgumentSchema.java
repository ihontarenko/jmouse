package org.jmouse.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The builder an action's JSON Schema is written with.
 *
 * <p>Exists so a tool definition reads as a list of arguments rather than as nested map literals.
 * <strong>A hand-written schema map fails silently</strong>, which is the entire argument for this
 * class: a misspelled {@code "propertyies"} key is still a valid map, the schema still serialises, the
 * client still connects, and the argument merely never appears to the model. Nothing rejects it,
 * nothing logs it, and the symptom is a model that "keeps forgetting" to send something.
 *
 * <p>⚠️ <strong>There is deliberately no way to hand {@link ToolAction} a map you assembled
 * yourself.</strong> {@link ToolAction.Builder#inputSchema(ArgumentSchema)} takes this type, and this
 * type is the only thing that produces the shape the catalogue accepts. A builder that also took a
 * {@code Map} would be a door left open next to a wall.
 *
 * <p>Covers only what actions actually use, and should not grow into a general JSON Schema library:
 * add a method when an action needs it, not before. Every method that has a "required" twin is
 * spelled out rather than taking a boolean, because {@code requiredString(name, text)} reads as what
 * it means at the call site and {@code string(name, text, true)} does not.
 */
public final class ArgumentSchema {

    private final Map<String, Object> properties = new LinkedHashMap<>();
    private final List<String>        required   = new ArrayList<>();

    private ArgumentSchema() {
    }

    public static ArgumentSchema builder() {
        return new ArgumentSchema();
    }

    /** An action taking no arguments at all. */
    public static ArgumentSchema none() {
        return builder();
    }

    /**
     * The scope argument, described the same way everywhere.
     *
     * <p>Written once because a model reads these descriptions as instructions, and five hand-copied
     * variants of "name the place to act in" are five chances for one of them to say something subtly
     * different about when it may be omitted.
     *
     * @param kind       the product's own word for the place: {@code workspace}, {@code persona}
     * @param listAction the action that reports them, so the description can point at it
     */
    public ArgumentSchema scope(String kind, String listAction) {
        return optionalString(ToolInvocation.SCOPE_ARGUMENT,
                "Name of the " + kind + " to act in, exactly as " + listAction + " reports it. Omit it "
              + "only when there is a single " + kind + " to act in.");
    }

    /**
     * The confirmation argument, described the same way everywhere.
     *
     * <p>Written once for the same reason as {@link #scope}: one domain's copy quietly saying
     * something different about how many times a token works is worse than no description at all.
     */
    public ArgumentSchema confirm() {
        return optionalString(ToolInvocation.CONFIRM_ARGUMENT,
                "The token a preview returned. Send the same arguments plus this to carry out exactly "
              + "the previewed operation. Works once.");
    }

    /** The escape hatch from deduplication, for a caller that really does want a second one. */
    public ArgumentSchema allowDuplicate() {
        return optionalBoolean(ToolInvocation.ALLOW_DUPLICATE_ARGUMENT,
                "Set true only when a second, genuinely separate record is wanted. An identical call "
              + "repeated within the deduplication window otherwise does nothing.");
    }

    /** The result-count argument, clamped to the same ceiling everywhere. */
    public ArgumentSchema limit(int fallback) {
        return optionalNumber(ToolInvocation.LIMIT_ARGUMENT,
                "How many results to return, up to " + ToolInvocation.MAXIMUM_LIMIT
              + ". Default " + fallback + ".");
    }

    public ArgumentSchema optionalString(String name, String description) {
        properties.put(name, Map.of("type", "string", "description", description));
        return this;
    }

    public ArgumentSchema requiredString(String name, String description) {
        required.add(name);
        return optionalString(name, description);
    }

    /**
     * Text restricted to a fixed set of values.
     *
     * <p>The values go into the schema so a client's own check catches a wrong one before the call is
     * made; {@link Arguments#optionalEnum} refuses the same thing again on the way in, because a
     * client is not obliged to check anything.
     */
    public ArgumentSchema optionalEnum(String name, String description, Enum<?>[] permitted) {
        properties.put(name, Map.of(
                "type",        "string",
                "enum",        java.util.Arrays.stream(permitted).map(Enum::name).toList(),
                "description", description));
        return this;
    }

    public ArgumentSchema requiredEnum(String name, String description, Enum<?>[] permitted) {
        required.add(name);
        return optionalEnum(name, description, permitted);
    }

    public ArgumentSchema optionalNumber(String name, String description) {
        properties.put(name, Map.of("type", "number", "description", description));
        return this;
    }

    public ArgumentSchema requiredNumber(String name, String description) {
        required.add(name);
        return optionalNumber(name, description);
    }

    public ArgumentSchema optionalBoolean(String name, String description) {
        properties.put(name, Map.of("type", "boolean", "description", description));
        return this;
    }

    /** A calendar date, spelled the one way {@link Arguments#optionalDate} reads. */
    public ArgumentSchema optionalDate(String name, String description) {
        properties.put(name, Map.of(
                "type",        "string",
                "format",      "date",
                "description", description + " ISO-8601, as YYYY-MM-DD."));
        return this;
    }

    public ArgumentSchema optionalStringList(String name, String description) {
        properties.put(name, Map.of(
                "type",        "array",
                "items",       Map.of("type", "string"),
                "description", description));
        return this;
    }

    public ArgumentSchema requiredStringList(String name, String description) {
        required.add(name);
        return optionalStringList(name, description);
    }

    /**
     * A free-form object of text values.
     *
     * <p>Untyped on purpose: the keys are a user's own field names, learned at runtime through a
     * describing action. Enumerating them here would mean carrying every definition in the published
     * schema, which is precisely what a describing action exists to avoid.
     */
    public ArgumentSchema optionalStringMap(String name, String description) {
        properties.put(name, Map.of(
                "type",                 "object",
                "additionalProperties", Map.of("type", "string"),
                "description",          description));
        return this;
    }

    public ArgumentSchema requiredStringMap(String name, String description) {
        required.add(name);
        return optionalStringMap(name, description);
    }

    /**
     * A list of objects, each shaped by a schema of its own.
     *
     * <p>The item schema is another {@link ArgumentSchema}, so a nested object is described in the
     * same words as a top-level one and — more usefully — a required member of it is genuinely
     * required. Describing a nested shape as "a list of objects" and leaving the members to prose is
     * how a model learns the shape by trying it.
     */
    public ArgumentSchema optionalObjectList(String name, String description, ArgumentSchema item) {
        properties.put(name, Map.of(
                "type",        "array",
                "items",       item.build(),
                "description", description));
        return this;
    }

    public ArgumentSchema requiredObjectList(String name, String description, ArgumentSchema item) {
        required.add(name);
        return optionalObjectList(name, description, item);
    }

    /** A single nested object, shaped by a schema of its own. */
    public ArgumentSchema optionalObject(String name, String description, ArgumentSchema shape) {
        Map<String, Object> nested = new LinkedHashMap<>(shape.build());
        nested.put("description", description);
        properties.put(name, Map.copyOf(nested));
        return this;
    }

    /** Which arguments this schema declares, for anything that has to reason about them. */
    public List<String> declaredArguments() {
        return List.copyOf(properties.keySet());
    }

    public Map<String, Object> build() {
        Map<String, Object> schema = new LinkedHashMap<>();

        schema.put("type", "object");
        schema.put("properties", Map.copyOf(properties));

        if (!required.isEmpty()) {
            schema.put("required", List.copyOf(required));
        }

        // Named explicitly: a model that invents an argument should be told so by the client's own
        // schema check, rather than have it silently dropped on the way in and wonder why nothing
        // happened.
        schema.put("additionalProperties", false);

        return schema;
    }
}
