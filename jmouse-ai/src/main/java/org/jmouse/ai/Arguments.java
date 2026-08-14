package org.jmouse.ai;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Arguments as sent, read as what they were meant to be.
 *
 * <p>JSON arrives as whatever the client typed, so every read here converts and — more importantly —
 * refuses in a sentence a model can act on. Reaching into the raw map instead produces a
 * {@link ClassCastException}, which reaches the client as an unexplained failure and teaches it
 * nothing about what would have been accepted. That is the whole difference between a caller that
 * corrects itself on the next call and one that tries the same thing again.
 *
 * <p>Every refusal here follows one shape: <em>what is wrong</em>, then <em>what would have been
 * accepted</em>. An enum refusal lists the permitted values; a number refusal quotes what was sent.
 * A tool author writing their own check should follow it — {@link #refuse(String, String)} is public
 * for exactly that.
 *
 * <p><strong>{@code context} is what makes this reusable inside a nested object.</strong> A refusal
 * about a field buried in {@code fields[2].validation[0]} has to say so; without it, three levels of
 * structure all report a problem with something called {@code min} and the caller has to guess which
 * one. It is why this is a type and not a handful of static helpers.
 *
 * <p>Several readers accept a shape that is not quite right rather than refusing it: a quoted number,
 * a bare string where a one-element list was asked for, a single object where a list of them was.
 * That is not laxity — each is a thing models routinely do, none of them is ambiguous, and refusing
 * costs a round trip to teach a rule about brackets. What is refused is anything whose <em>meaning</em>
 * would have to be guessed.
 *
 * @param values  the object being read
 * @param context how to name a member of it in a refusal, e.g. {@code "fields[2]."} — empty at the top
 */
public record Arguments(Map<String, Object> values, String context) {

    public Arguments {
        values  = values == null ? Map.of() : values;
        context = context == null ? "" : context;
    }

    public static Arguments of(Map<String, Object> values) {
        return new Arguments(values, "");
    }

    /**
     * A reader over an object that sits somewhere inside a larger one, named by where it sits.
     *
     * @param label how to refer to it, e.g. {@code "fields[2]"} — every refusal is prefixed with it
     */
    public static Arguments at(String label, Map<String, Object> values) {
        return new Arguments(values, label + ".");
    }

    /** The same, for an object found inside this one. */
    public Arguments nested(String label, Map<String, Object> inner) {
        return at(context + label, inner);
    }

    public Optional<String> optionalString(String name) {
        Object value = values.get(name);

        if (value == null) {
            return Optional.empty();
        }

        if (!(value instanceof String text)) {
            throw refuse(name, "must be text, but a " + describeType(value) + " was sent");
        }

        return text.isBlank() ? Optional.empty() : Optional.of(text.trim());
    }

    public String requiredString(String name) {
        return optionalString(name).orElseThrow(() -> refuse(name, "is required and was not sent"));
    }

    /**
     * A number, accepting one a model quoted.
     *
     * <p>A model that quotes its numbers is being sloppy rather than wrong; the text is read, and only
     * something that genuinely is not a number is refused.
     */
    public Optional<Double> optionalNumber(String name) {
        Object value = values.get(name);

        switch (value) {
            case null -> {
                return Optional.empty();
            }
            case Number number -> {
                return Optional.of(number.doubleValue());
            }
            case String text when !text.isBlank() -> {
                try {
                    return Optional.of(Double.parseDouble(text.trim()));
                } catch (NumberFormatException notANumber) {
                    throw refuse(name, "must be a number, but '" + text + "' is not one");
                }
            }
            default -> {
            }
        }

        throw refuse(name, "must be a number, but a " + describeType(value) + " was sent");
    }

    public double requiredNumber(String name) {
        return optionalNumber(name).orElseThrow(() -> refuse(name, "is required and was not sent"));
    }

    /**
     * A calendar date, as ISO-8601.
     *
     * <p>Here rather than in each tool because {@link LocalDate#parse} throws a
     * {@link DateTimeParseException} whose message is written for a programmer reading a stack trace,
     * and a date range is one of the few arguments a model reliably gets subtly wrong — a locale's
     * ordering, a two-digit year, a month name. One format, said plainly, once.
     *
     * <p>Only a date. A timestamp, a duration and a money amount each have more than one defensible
     * spelling, and inventing one here would be the library deciding a product's vocabulary.
     */
    public Optional<LocalDate> optionalDate(String name) {
        Optional<String> text = optionalString(name);

        if (text.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(LocalDate.parse(text.get()));
        } catch (DateTimeParseException notADate) {
            throw refuse(name, "must be a date written as YYYY-MM-DD, but '" + text.get()
                             + "' could not be read as one");
        }
    }

    public LocalDate requiredDate(String name) {
        return optionalDate(name).orElseThrow(() -> refuse(name, "is required and was not sent"));
    }

    /**
     * One of a fixed set of named constants, matched without regard to case.
     *
     * <p>Takes the permitted values rather than the enum class, because <em>"which of these may a
     * caller name"</em> is not always the whole enum — a domain can be configured with a subset, and
     * offering the rest would be offering something that then fails further in.
     *
     * <p>The refusal lists every permitted value, so a model that guessed a constant learns the real
     * vocabulary in the same breath rather than guessing a second time.
     */
    public <E extends Enum<E>> Optional<E> optionalEnum(String name, E[] permitted) {
        Optional<String> requested = optionalString(name);

        if (requested.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(Arrays.stream(permitted)
                .filter(constant -> constant.name().equalsIgnoreCase(requested.get()))
                .findFirst()
                .orElseThrow(() -> refuse(name, "must be one of " + namesOf(permitted)
                                              + ", but '" + requested.get() + "' was sent")));
    }

    public <E extends Enum<E>> E requiredEnum(String name, E[] permitted) {
        return optionalEnum(name, permitted)
                .orElseThrow(() -> refuse(name, "is required and must be one of " + namesOf(permitted)));
    }

    /** The permitted values, for a schema description or a refusal that has to list them. */
    public static String namesOf(Enum<?>[] permitted) {
        return Arrays.stream(permitted).map(Enum::name).collect(Collectors.joining(", "));
    }

    public boolean flag(String name) {
        Object value = values.get(name);

        return switch (value) {
            case null                 -> false;
            case Boolean booleanValue -> booleanValue;
            case String text          -> Boolean.parseBoolean(text.trim());
            default -> throw refuse(name, "must be true or false, but a " + describeType(value)
                                        + " was sent");
        };
    }

    /** A list of text values, accepting a bare string as the one-element list a model often sends. */
    public List<String> stringList(String name) {
        Object value = values.get(name);

        if (value == null) {
            return List.of();
        }

        if (value instanceof String text) {
            return text.isBlank() ? List.of() : List.of(text.trim());
        }

        if (!(value instanceof Iterable<?> items)) {
            throw refuse(name, "must be a list of text values, but a " + describeType(value) + " was sent");
        }

        List<String> texts = new ArrayList<>();

        for (Object item : items) {
            if (!(item instanceof String text)) {
                throw refuse(name, "must contain only text values, but it holds a " + describeType(item));
            }
            if (!text.isBlank()) {
                texts.add(text.trim());
            }
        }

        return List.copyOf(texts);
    }

    /**
     * An object of name to value.
     *
     * <p>Numbers and booleans are read as their text rather than refused: what these usually carry is
     * a set of user-named fields whose values are stored as text anyway, and rejecting
     * {@code {"quantity": 4}} in favour of {@code {"quantity": "4"}} would be pedantry that costs a
     * round trip and teaches nothing.
     */
    public Map<String, String> stringMap(String name) {
        Object value = values.get(name);

        if (value == null) {
            return Map.of();
        }

        if (!(value instanceof Map<?, ?> entries)) {
            throw refuse(name, "must be an object of name to value, but a " + describeType(value)
                             + " was sent");
        }

        Map<String, String> mapped = new LinkedHashMap<>();
        entries.forEach((key, entryValue) -> mapped.put(
                String.valueOf(key),
                entryValue == null ? "" : String.valueOf(entryValue)));

        return mapped;
    }

    public Map<String, String> requiredStringMap(String name) {
        Map<String, String> mapped = stringMap(name);

        if (mapped.isEmpty()) {
            throw refuse(name, "is required and was empty");
        }

        return mapped;
    }

    /**
     * A list of objects.
     *
     * <p>A single object is accepted as a one-element list for the same reason {@link #stringList}
     * takes a bare string: a model asked for one of something routinely sends one object, and refusing
     * it costs a round trip to learn a rule about brackets.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> objectList(String name) {
        Object value = values.get(name);

        if (value == null) {
            return List.of();
        }

        if (value instanceof Map<?, ?> singleObject) {
            return List.of((Map<String, Object>) singleObject);
        }

        if (!(value instanceof Iterable<?> items)) {
            throw refuse(name, "must be a list of objects, but a " + describeType(value) + " was sent");
        }

        List<Map<String, Object>> objects = new ArrayList<>();

        for (Object item : items) {
            if (!(item instanceof Map<?, ?> object)) {
                throw refuse(name, "must contain only objects, but it holds a " + describeType(item));
            }
            objects.add((Map<String, Object>) object);
        }

        return List.copyOf(objects);
    }

    /**
     * The same list, each element already knowing where it sits.
     *
     * <p>What {@link #objectList} is almost always followed by, and the loop that follows it is the
     * same four lines in every handler that reads one: index, {@code Arguments.at(name + "[" + index +
     * "]", …)}, read, refuse. Composing that label by hand is not hard, it is <em>skippable</em> — and
     * a handler that skips it refuses the third entry with a sentence about something called
     * {@code quantity}, which is the same sentence all three entries would have produced.
     *
     * <p>Discovered by writing the loop twice in a sandbox and disliking it both times.
     */
    public List<Arguments> each(String name) {
        List<Map<String, Object>> objects = objectList(name);
        List<Arguments>          readers  = new ArrayList<>(objects.size());

        for (int index = 0; index < objects.size(); index++) {
            readers.add(nested(name + "[" + index + "]", objects.get(index)));
        }

        return List.copyOf(readers);
    }

    /**
     * A readable refusal about one named argument, in the shape every reader here uses.
     *
     * <p>Public because a tool's own checks refuse about the same arguments and must sound the same;
     * a hand-written sentence beside these is how one refusal ends up saying something a model cannot
     * act on.
     *
     * @param problem the rest of the sentence, starting with a verb: {@code "must be a positive number"}
     */
    public ToolRefusedException refuse(String name, String problem) {
        return new ToolRefusedException(RefusalReason.INVALID_ARGUMENT,
                "'" + context + name + "' " + problem + ".");
    }

    private String describeType(Object value) {
        return switch (value) {
            case Number ignored      -> "number";
            case Boolean ignored     -> "boolean";
            case Map<?, ?> ignored   -> "object";
            case Iterable<?> ignored -> "list";
            default                  -> value.getClass().getSimpleName().toLowerCase();
        };
    }
}
