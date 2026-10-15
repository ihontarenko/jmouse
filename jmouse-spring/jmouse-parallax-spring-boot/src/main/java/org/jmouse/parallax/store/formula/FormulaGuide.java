package org.jmouse.parallax.store.formula;

import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.FormulaPrinter;
import org.jmouse.formula.MathematicalConstant;
import org.jmouse.formula.MathematicalFunction;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.parallax.Computations;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.file.ObjectFiles;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.store.support.ParallaxJson;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The jMF language as the interface teaches it: the reference (the built-in functions and constants come from the
 * formula library's own tables, only their sentences from `jmouse/parallax/formula/reference.json`), and any typed formula
 * interpreted — typeset, classified and given its parameters — without computing anything.
 */
public class FormulaGuide {

    public record FunctionEntry(String name, int arity, boolean complex, String description) {
    }

    public record ConstantEntry(String name, String latex, double value, String description) {
    }

    public record Interpretation(String source, String text, String latex, String kind, ObjectDefinition definition,
                                 List<ParameterDefinition> parameters, List<RepresentationKind> representations,
                                 String refusal, Integer position) {
    }

    private static final int MAXIMUM_SOURCE = 4000;

    private final Map<String, Object> reference;
    private final Computations computations;

    public FormulaGuide(ParallaxJson documents, Computations computations) {
        this.computations = computations;
        Map<String, Object> text = documents.read(read("jmouse/parallax/formula/reference.json"), Map.class);
        this.reference = assemble(text);
    }

    public Map<String, Object> reference() {
        return reference;
    }

    /** Typesets and classifies a formula; a refusal comes back as words and a position, not as an error. */
    public Interpretation interpret(String source) {
        if (source == null || source.isBlank()) {
            throw ParallaxProblem.invalid("Type a formula.");
        }
        if (source.length() > MAXIMUM_SOURCE) {
            throw ParallaxProblem.invalid("A formula is at most " + MAXIMUM_SOURCE + " characters.");
        }
        java.util.Optional<org.jmouse.parallax.file.FamilyLines.Split> family = org.jmouse.parallax.file.FamilyLines.split(source);
        if (family.isPresent()) {
            return family(source, family.get());
        }
        // A curve `(cos(s), sin(s))  s in 0 .. 2 pi`, a point `(a, b)` or a construction step `vector(A, B)` is what jMF
        // alone does not parse: a parametric curve (jmouse `ParametricLines`) or a small construction (`GeometryLines`).
        try {
            java.util.Optional<ObjectFiles.ReadObject> geometry = org.jmouse.parallax.file.ParametricLines.read(source)
                    .or(() -> org.jmouse.parallax.file.GeometryLines.read(source));
            if (geometry.isPresent()) {
                ObjectFiles.ReadObject read = geometry.get();
                String shown = source.contains(";") ? source.substring(source.lastIndexOf(';') + 1).strip() : source.strip();
                return new Interpretation(source, shown, shown, read.definition().objectKind().name(), read.definition(),
                        read.parameters(), computations.applicableTo(read.definition(), read.parameters()), null, null);
            }
        } catch (IllegalArgumentException refused) {
            return new Interpretation(source, null, null, null, null, List.of(), List.of(), refused.getMessage(), null);
        }
        Formula formula;
        try {
            formula = FormulaLanguage.standard().parse(source);
        } catch (FormulaException refused) {
            return new Interpretation(source, null, null, null, null, List.of(), List.of(), refused.getMessage(), refused.position());
        }
        String latex = FormulaPrinter.latex(formula);
        String text = FormulaPrinter.text(formula);
        try {
            ObjectFiles.ReadObject read = ObjectFiles.infer("formula", 1, source);
            return new Interpretation(source, text, latex, read.definition().objectKind().name(), read.definition(),
                    read.parameters(), computations.applicableTo(read.definition(), read.parameters()), null, null);
        } catch (IllegalArgumentException | FormulaException unclassified) {
            return new Interpretation(source, text, latex, null, null, List.of(), List.of(), unclassified.getMessage(), null);
        }
    }

    /**
     * A line followed by `k in a .. b integer`: the member read as it would be alone, the index taken out of its
     * parameters, the names the bounds read put in.
     */
    private Interpretation family(String source, org.jmouse.parallax.file.FamilyLines.Split split) {
        Interpretation member = interpret(split.member());
        if (member.refusal() != null || member.definition() == null) {
            return new Interpretation(source, member.text(), member.latex(), null, null, List.of(), List.of(),
                    member.refusal() != null ? member.refusal() : "The member of this family is not something drawn.", member.position());
        }
        try {
            ObjectDefinition.Family definition = new ObjectDefinition.Family(split.index(), split.from(), split.to(), member.definition());
            List<ParameterDefinition> parameters = new java.util.ArrayList<>(member.parameters().stream()
                    .filter(parameter -> !parameter.name().equals(split.index())).toList());
            for (String bound : List.of(split.from(), split.to())) {
                for (String name : FormulaLanguage.standard().parse(bound).variables()) {
                    if (!ObjectFiles.isConstantName(name) && parameters.stream().noneMatch(parameter -> parameter.name().equals(name))) {
                        parameters.add(new ParameterDefinition(name, name, 1, -10, 10, 0.01, false));
                    }
                }
            }
            String shown = member.text() + "   " + split.index() + " in " + split.from() + " .. " + split.to();
            return new Interpretation(source, shown, member.latex(), definition.objectKind().name(), definition, parameters,
                    computations.applicableTo(definition, parameters), null, null);
        } catch (IllegalArgumentException | FormulaException refused) {
            return new Interpretation(source, null, null, null, null, List.of(), List.of(), refused.getMessage(), null);
        }
    }

    private static Map<String, Object> assemble(Map<String, Object> text) {
        @SuppressWarnings("unchecked")
        Map<String, String> functionSentences = (Map<String, String>) text.getOrDefault("functions", Map.of());
        @SuppressWarnings("unchecked")
        Map<String, String> constantSentences = (Map<String, String>) text.getOrDefault("constants", Map.of());
        Map<String, Object> assembled = new LinkedHashMap<>(text);
        assembled.put("functions", Arrays.stream(MathematicalFunction.values())
                .map(function -> new FunctionEntry(function.symbol(), function.arity(), function.complex().isPresent(),
                        functionSentences.getOrDefault(function.symbol(), "")))
                .toList());
        assembled.put("constants", Arrays.stream(MathematicalConstant.values())
                .map(constant -> new ConstantEntry(constant.symbol(), constant.latex(), constant.value(),
                        constantSentences.getOrDefault(constant.symbol(), "")))
                .toList());
        return Map.copyOf(assembled);
    }

    private static String read(String path) {
        try (InputStream stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException missing) {
            throw new UncheckedIOException("The resource " + path + " is missing.", missing);
        }
    }
}
