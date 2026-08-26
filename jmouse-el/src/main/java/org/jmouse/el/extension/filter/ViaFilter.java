package org.jmouse.el.extension.filter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.ConverterNotFound;
import org.jmouse.core.convert.GenericConverter;
import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.util.List;

/**
 * {@code value | via("name")} — the value, through the converter registered under that name.
 *
 * <h2>⚠️ Why a converter is a filter and not a clause of its own</h2>
 *
 * <p>A converter applied to a value <em>is</em> a filter — it takes one value and produces another.
 * Giving it its own syntax would draw a distinction whose only content is where the transformation
 * happens to be implemented, which is not something a reader should have to write down. A mapping
 * declaration says {@code shipping : deliveryAddress | via("shop.address")} and every other filter on
 * that line composes with it exactly as it would with any other.</p>
 *
 * <h2>⚠️ It reaches a REGISTERED converter, never a class name</h2>
 *
 * <p>That is the line between a filter and a hook. A text file naming a fully-qualified class would be
 * a text file calling arbitrary application code, which needs a container to resolve names against and
 * stops the file being data. A name resolves against converters somebody already registered, in
 * Java, deliberately — so what this can reach is exactly what an application chose to expose.</p>
 *
 * <h2>⚠️ The target type comes from the converter, not from the caller</h2>
 *
 * <p>A filter does not know what its result is about to be assigned to — in a mapping declaration the
 * target property's type is decided long after the expression runs. But a named converter knows what
 * it produces, because it was registered for a pair. So the pair is read off the converter. Where a
 * converter supports several and the input matches more than one, that is an ambiguity this filter
 * cannot resolve and it says so rather than picking.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ViaFilter extends AbstractFilter {

    /**
     * Applies the named converter to the input.
     *
     * @param input     the value to convert; {@code null} passes through untouched
     * @param arguments the converter's name, as the single argument
     * @param context   the evaluation context, which carries the conversion registry
     * @param type      the class type inspector (unused)
     * @return the converted value
     * @throws IllegalArgumentException when no name was given
     * @throws ConverterNotFound        when nothing is registered under that name
     */
    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        // ⚠️ A null passes through rather than being handed to a converter. Every converter would
        // otherwise have to defend against it, and "convert nothing" has no useful answer other than
        // nothing — this is the same shape `default` relies on immediately before it in a chain.
        if (input == null) {
            return null;
        }

        Object named = arguments.isEmpty() ? null : arguments.getFirst();

        if (named == null) {
            throw new IllegalArgumentException(
                    "via() needs the name of a converter, as in: value | via(\"shop.address\")");
        }

        String                       name      = context.getConversion().convert(named, String.class);
        GenericConverter<Object, Object> converter = context.getConversion().requireConverter(name);

        return converter.convert(input, targetOf(converter, name, input));
    }

    /**
     * What the named converter produces.
     *
     * @param converter the converter
     * @param name      its name, for a refusal somebody has to read
     * @param input     the value, whose runtime class narrows a converter supporting several pairs
     * @return the target class
     */
    private Class<Object> targetOf(GenericConverter<Object, Object> converter, String name, Object input) {
        List<ClassPair> supported = List.copyOf(converter.getSupportedTypes());

        if (supported.size() == 1) {
            return classOf(supported.getFirst());
        }

        List<ClassPair> matching = supported.stream()
                .filter(pair -> pair.classA().isAssignableFrom(input.getClass()))
                .toList();

        if (matching.size() == 1) {
            return classOf(matching.getFirst());
        }

        // ⚠️ Not ConverterNotFound — one WAS found. This is a registration that cannot answer the
        // question, and saying "not found" would send whoever reads it looking for a missing name.
        throw new IllegalArgumentException(
                ("the converter named '%s' handles %d pairs and %s matches %d of them, so via() cannot "
                 + "tell which one is meant; register it under a name per pair")
                        .formatted(name, supported.size(), input.getClass().getSimpleName(),
                                   matching.size()));
    }

    @SuppressWarnings("unchecked")
    private Class<Object> classOf(ClassPair pair) {
        return (Class<Object>) pair.classB();
    }

    /**
     * Returns the name of this filter.
     *
     * @return the string "via"
     */
    @Override
    public String getName() {
        return "via";
    }
}
