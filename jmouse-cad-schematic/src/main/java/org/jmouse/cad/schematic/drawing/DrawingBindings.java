package org.jmouse.cad.schematic.drawing;

import org.jmouse.cad.Formula;
import org.jmouse.cad.Text;
import org.jmouse.cad.component.Drawing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * What a component's portrait is drawn from: each binding of its {@code uses} line, read as jMF over the
 * component's parameters and handed on as an expression tree — the same trees a living drawing's values are —, so the
 * browser draws a part from that part's own values and never parses; and the words each text input prints, the words
 * on the part's body. A symbol's bindings are no such thing: they are the part's display figures, read while it runs.
 *
 * @param trees each bound input that is a number, by name, and the tree of what it is drawn from
 * @param texts each bound text input, by name, and the words it prints
 */
public record DrawingBindings(Map<String, DrawingExpression> trees, Map<String, String> texts) {

    public DrawingBindings {
        trees = Collections.unmodifiableMap(new LinkedHashMap<>(trees));
        texts = Collections.unmodifiableMap(new LinkedHashMap<>(texts));
    }

    /**
     * The bindings a component writes for the drawing it uses.
     *
     * @param what       how the component is named in a refusal
     * @param drawn      the drawing its {@code uses} line names
     * @param parameters the names a binding may read: the component's parameters
     * @throws IllegalArgumentException when a binding does not read, reads another name, binds no input of the drawing,
     *                                  or binds a text input to anything but words
     */
    public static DrawingBindings of(String what, Drawing use, SymbolDrawing drawn, Set<String> parameters) {
        Set<String> inputs = drawn.inputs().stream().map(SymbolDrawing.Input::name).collect(Collectors.toSet());
        Set<String> textInputs = drawn.inputs().stream().filter(input -> input.text() != null).map(SymbolDrawing.Input::name)
                .collect(Collectors.toSet());
        Map<String, DrawingExpression> trees = new LinkedHashMap<>();
        Map<String, String> texts = new LinkedHashMap<>();
        for (Drawing.Binding binding : use.bindings()) {
            if (binding.value() == null) {
                continue;
            }
            if (!inputs.contains(binding.input())) {
                throw new IllegalArgumentException(what + ": '" + binding.input() + "' is no input of the drawing it uses");
            }
            if (textInputs.contains(binding.input())) {
                if (!(binding.value() instanceof Text words)) {
                    throw new IllegalArgumentException(what + ": '" + binding.input() + "' is a text the part prints on itself: "
                            + "bind it to quoted words, '" + binding.input() + ": \"NE555\"'");
                }
                texts.put(binding.input(), words.text());
            } else if (binding.value() instanceof Formula expression) {
                trees.put(binding.input(), LivingDrawing.binding(what, binding.input(), expression.written(), parameters));
            } else {
                throw new IllegalArgumentException(what + ": '" + binding.input() + "' is a number the drawing reads: bind it to "
                        + "an expression of the part's parameters, not to words");
            }
        }
        return new DrawingBindings(trees, texts);
    }
}
