package org.jmouse.formula;

import org.jmouse.descriptions.Description;
import org.jmouse.descriptions.Descriptions;
import org.jmouse.documentation.DocumentationPage;
import org.jmouse.documentation.DocumentationSource;
import org.jmouse.documentation.markdown.MarkdownDocumentation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 📖 jMF's own page of documentation, for whoever hosts the language: how it is written — a Markdown page kept beside this class — and,
 * after it, every function and constant it knows, each said in its own Javadoc ({@link Descriptions}). A function added to
 * {@link MathematicalFunction} is in the reference the moment it is compiled; nothing here names one.
 */
public final class FormulaDocumentation implements DocumentationSource {

    /** The page's subject: what a host asks for the language's reference by. */
    public static final String SUBJECT = "formula-language";
    /** Where the page's own words are kept. */
    private static final String PAGE = "documentation/formula-language.md";

    @Override
    public List<DocumentationPage> pages() {
        return List.of(page());
    }

    /** The language's reference page: its words, then its functions and its constants. */
    public static DocumentationPage page() {
        StringBuilder text = new StringBuilder(written()).append("\n\n## Functions\n\n| Function | Complex numbers | What it is |\n|---|---|---|\n");
        Optional<Description> functions = Descriptions.of(MathematicalFunction.class);
        for (MathematicalFunction function : MathematicalFunction.values()) {
            text.append("| `").append(function.symbol()).append(parameters(function.arity())).append("` | ")
                .append(function.complex().isPresent() ? "yes" : "real only").append(" | ")
                .append(cell(functions.flatMap(description -> description.member(function.name())).orElse(""))).append(" |\n");
        }
        text.append("\n## Constants\n\n| Constant | Value | What it is |\n|---|---|---|\n");
        Optional<Description> constants = Descriptions.of(MathematicalConstant.class);
        for (MathematicalConstant constant : MathematicalConstant.values()) {
            text.append("| `").append(constant.symbol()).append("` | ").append(Double.isInfinite(constant.value()) ? "∞"
                    : String.format(Locale.ROOT, "%.10g", constant.value())).append(" | ")
                .append(cell(constants.flatMap(description -> description.member(constant.name())).orElse(""))).append(" |\n");
        }
        return MarkdownDocumentation.pageOf(text.toString(), PAGE);
    }

    /** The page's own words, as kept beside this class. */
    private static String written() {
        try (InputStream stream = FormulaDocumentation.class.getResourceAsStream(PAGE)) {
            if (stream == null) {
                throw new IllegalStateException("jMF's page of documentation is not where it is kept: " + PAGE);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException unreadable) {
            throw new UncheckedIOException("jMF's page of documentation cannot be read", unreadable);
        }
    }

    /** A function's parameters as a call writes them: {@code (a)}, {@code (a, b)}. */
    private static String parameters(int arity) {
        return switch (arity) {
            case 1 -> "(x)";
            case 2 -> "(a, b)";
            default -> "(…)";
        };
    }

    /** Words as a table's cell holds them: one line, its bars escaped. */
    private static String cell(String words) {
        return words.replace("\n", " ").replace("|", "\\|");
    }
}
