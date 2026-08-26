package org.jmouse.mapper.el;

import org.jmouse.core.convert.GenericConverter;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;
import org.jmouse.el.extension.filter.ViaFilter;

/**
 * {@code via("name")} standing alone — the source, through the converter registered under that name.
 *
 * <h2>⚠️ Why this exists beside the filter, and why it is here rather than in the language</h2>
 *
 * <p>The reference document writes {@code via} two ways. §14 uses it as a filter, which is the general
 * form and what §2 says a converter is: {@code shipping : deliveryAddress | via("shop.address")}. §8
 * writes it with nothing on its left:</p>
 *
 * <pre>{@code
 * target Money {
 *     from BigDecimal : via("money")
 * }
 * }</pre>
 *
 * <p>With no left-hand value, the only thing it can mean is <em>the object being mapped from</em> — so
 * this is exactly {@code source | via("money")}, spelled the way a whole-pair conversion reads.</p>
 *
 * <p>⚠️ <strong>That convention is the mapper's, not the expression language's.</strong> {@code source}
 * is a name {@code JmmBinder} binds into every context it evaluates in; {@code jmouse-el} knows nothing
 * about it and must not start, or the language acquires an opinion about what a value is for. So the
 * filter — which needs no such convention — lives in the language, and this function lives here.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ViaFunction implements Function {

    /** The name a mapping's source is bound under — see {@link JmmBinder}. */
    static final String SOURCE = "source";

    private final ViaFilter filter = new ViaFilter();

    /**
     * Converts the source through the named converter.
     *
     * @param arguments the converter's name, as the single argument
     * @param context   the evaluation context, carrying both the source and the conversion registry
     * @return the converted value
     * @throws IllegalArgumentException when there is no source in scope
     */
    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        Object source = context.getValue(SOURCE);

        if (source == null) {
            // ⚠️ Not "returns null". A bare via() outside a mapping is a line that cannot mean anything,
            // and answering null would let it sit in a file looking like it worked.
            throw new IllegalArgumentException(
                    "via(\"…\") on its own converts the object being mapped from, and there is none in "
                    + "scope here; write it as a filter — value | via(\"…\")");
        }

        // ⚠️ Delegates rather than repeating the lookup. What must not drift is which converter a name
        // resolves to and how its target type is decided; two copies would agree until one was fixed.
        // The classifier is unused by the filter — a converter's target comes from its registration.
        return filter.apply(source, arguments, context, null);
    }

    @Override
    public String getName() {
        return "via";
    }
}
