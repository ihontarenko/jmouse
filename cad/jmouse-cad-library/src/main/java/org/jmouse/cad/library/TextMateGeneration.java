package org.jmouse.cad.library;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jmouse.cad.document.DeclarationParser;
import org.jmouse.cad.document.LanguageWalk;
import org.jmouse.cad.document.MetaParser;
import org.jmouse.cad.document.ScriptBlockParser;
import org.jmouse.el.grammar.ClauseBlockParser;
import org.jmouse.el.grammar.ClauseLineParser;
import org.jmouse.el.grammar.FormulaLineParser;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.LineReader;
import org.jmouse.el.grammar.LineSet;
import org.jmouse.units.SiPrefix;
import org.jmouse.units.Unit;
import org.jmouse.units.Units;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The TextMate grammar of the document language — {@code jm-textmate}, for any editor that reads TextMate bundles —,
 * described from the language itself: its kinds, its blocks and what each holds, every line's word, its clauses and the
 * words its values may be, the lines that open with a name, the equations jMF reads, the {@code behaviour} and
 * {@code run} jMS reads, and quantities as the units library reads them. Nothing in it is listed by hand, so it cannot
 * drift from the reader: when a grammar changes, {@code check} says the bundle is stale and {@code write} renews it.
 *
 * <p>jMS's own grammar is the base — every rule of {@code jms.tmLanguage.json}, its scope renamed — so a jMS body is
 * coloured by jMS's rules, unchanged; the document's rules stand beside them and never over one.</p>
 *
 * <pre>TextMateGeneration write|check &lt;jms-textmate directory&gt; &lt;jm-textmate directory&gt;</pre>
 */
public final class TextMateGeneration {

    private static final String SCOPE = "jm";
    private static final String JMS_SCOPE = "jms";
    private static final String GRAMMAR = "syntaxes/jm.tmLanguage.json";

    /** Where a statement may begin: the start of a line, or after {@code ;} or {@code {} on the same one. */
    private static final String LINE = "(?:^|(?<=[;{]))[ \\t]*";
    /** Where a line-shaped statement ends: before {@code ;}, a comment, a closing brace, or the end of the line. */
    private static final String LINE_END = "(?=[;#}]|$)";
    private static final String NAME = "[A-Za-z_][A-Za-z0-9_]*";
    /** A slug or a reference, optionally pinned to a revision: {@code lm358@3}, {@code 74hc595}, {@code flip-flop}. */
    private static final String SLUG = "[A-Za-z0-9][A-Za-z0-9_.-]*(?:@[0-9]+)?";
    private static final String STRING = "(\"[^\"]*\")";
    private static final String NUMBER = "(?<![A-Za-z0-9_])[+-]?(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?";

    /** The words a document's head may say after its title. */
    private static final String AS = "as";
    private static final String TYPE = "type";

    /** Words the longest first: an alternation tries them in order. */
    private static final Comparator<String> LONGEST_FIRST = Comparator.comparingInt(String::length).reversed()
            .thenComparing(Comparator.naturalOrder());

    /** What the language's modules bring: every kind and block the reader knows. */

    /** Everything a walk of the language finds. */
    private TextMateGeneration() {
    }

    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 3 || !List.of("write", "check").contains(arguments[0])) {
            throw new IllegalArgumentException("TextMateGeneration write|check <jms-textmate directory> <jm-textmate directory>");
        }
        boolean writing = arguments[0].equals("write");
        Path jms = Path.of(arguments[1]);
        Path target = Path.of(arguments[2]);
        // Keys in their natural order: a rule's keys mean nothing by their order, and the bundle reads the same every run.
        ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n"))
                .withArrayIndenter(new DefaultIndenter("  ", "\n"));
        ObjectWriter writer = mapper.writer(printer);
        @SuppressWarnings("unchecked")
        Map<String, Object> base = mapper.readValue(jms.resolve("syntaxes/jms.tmLanguage.json").toFile(), Map.class);
        Map<String, String> files = new LinkedHashMap<>();
        files.put(GRAMMAR, writer.writeValueAsString(grammar(base, mapper)) + "\n");
        files.put("package.json", writer.writeValueAsString(packageManifest()) + "\n");
        files.put("language-configuration.json", writer.writeValueAsString(languageConfiguration()) + "\n");
        int stale = 0;
        for (Map.Entry<String, String> file : files.entrySet()) {
            Path path = target.resolve(file.getKey());
            if (writing) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, file.getValue(), StandardCharsets.UTF_8);
            } else if (!Files.exists(path) || !Files.readString(path, StandardCharsets.UTF_8).equals(file.getValue())) {
                stale += 1;
                System.out.println("STALE " + file.getKey());
            }
        }
        System.out.println(files.size() + " files " + (writing ? "written" : "described, " + stale + " stale"));
    }

    // ─── the grammar ────────────────────────────────────────────────────────────────────────────────

    private static Map<String, Object> grammar(Map<String, Object> base, ObjectMapper mapper) throws IOException {
        LanguageWalk language = LanguageWalk.of(CadLanguage.standard().modules());
        @SuppressWarnings("unchecked")
        Map<String, Object> repository = mapper.readValue(
                mapper.writeValueAsString(base.get("repository")).replace("." + JMS_SCOPE + "\"", "." + SCOPE + "\""), Map.class);
        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("quantity", quantity());
        rules.put("value", value());
        rules.put("term", term(language));
        rules.put("argument-name", argumentName());
        rules.put("meta-block", metaBlock());
        rules.put("document-declaration", declaration());
        rules.put("reference", reference());
        rules.put("script-body", scriptBody(language));
        rules.put("document", document(language));
        rules.put("document-term", documentTerm(language));
        rules.put("document-body", documentBody(language));
        language.blocks().forEach((word, block) -> rules.put(blockRule(word), block(block)));
        language.drawn().forEach((kind, lines) -> rules.put("drawn-" + kind, patterns(lineRules(lines))));
        for (String name : rules.keySet()) {
            if (repository.containsKey(name)) {
                throw new IllegalStateException("#" + name + " is a jMS rule; the document's rule must take another name");
            }
        }
        repository.putAll(rules);
        Map<String, Object> grammar = new LinkedHashMap<>();
        grammar.put("$schema", "https://raw.githubusercontent.com/martinring/tmlanguage/master/tmlanguage.json");
        grammar.put("name", "jM");
        grammar.put("scopeName", "source." + SCOPE);
        grammar.put("fileTypes", List.of(SCOPE));
        grammar.put("patterns", List.of(include("comment"), include("document"), include("document-term")));
        grammar.put("repository", repository);
        return grammar;
    }

    /** {@code kind "Title" [as slug] [type role] {}: a document written in blocks. */
    private static Map<String, Object> document(LanguageWalk language) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("name", "meta.document." + SCOPE);
        rule.put("begin", LINE + "\\b(" + words(language.kinds().keySet()) + ")\\b[ \\t]*" + STRING + "((?:[ \\t]+(?:" + AS + "|" + TYPE
                + ")[ \\t]+" + SLUG + ")*)[ \\t]*(\\{)?");
        rule.put("beginCaptures", Map.of("1", named("keyword.control.document"), "2", named("entity.name.type.document"),
                "3", Map.of("patterns", List.of(identity())), "4", named("punctuation.section.block.begin")));
        rule.put("end", "\\}");
        rule.put("endCaptures", Map.of("0", named("punctuation.section.block.end")));
        rule.put("patterns", List.of(include("document-body")));
        return rule;
    }

    /** {@code kind:slug("Title", type: role) {}: a document written in terms. */
    private static Map<String, Object> documentTerm(LanguageWalk language) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("name", "meta.document.term." + SCOPE);
        rule.put("begin", LINE + "\\b(" + words(language.kinds().keySet()) + ")(:)(" + SLUG + ")");
        rule.put("beginCaptures", Map.of("1", named("keyword.control.document"), "2", named("punctuation.separator.term"),
                "3", named("entity.name.tag.slug")));
        rule.put("end", "\\}");
        rule.put("endCaptures", Map.of("0", named("punctuation.section.block.end")));
        rule.put("patterns", List.of(include("document-body")));
        return rule;
    }

    private static Map<String, Object> identity() {
        return Map.of("match", "\\b(" + AS + "|" + TYPE + ")[ \\t]+(" + SLUG + ")",
                "captures", Map.of("1", named("keyword.other.identity"), "2", named("entity.name.tag.slug")));
    }

    /** What a body may hold: meta, behaviour and run, every block, every drawn line, terms and values. */
    private static Map<String, Object> documentBody(LanguageWalk language) {
        List<Object> patterns = new ArrayList<>(List.of(include("comment"), include("meta-block"), include("document-declaration"),
                include("script-body")));
        language.blocks().keySet().forEach(word -> patterns.add(include(blockRule(word))));
        language.drawn().keySet().forEach(kind -> patterns.add(include("drawn-" + kind)));
        patterns.add(include("term"));
        patterns.add(include("value"));
        // The brace a document written in terms opens its body with, after its head's arguments.
        patterns.add(Map.of("match", "\\{", "name", "punctuation.section.block.begin." + SCOPE));
        return patterns(patterns);
    }

    /**
     * {@code extends base}, {@code use heat, logic}, {@code units A, B} — or {@code extends:base} in terms —: what a
     * document declares before its blocks, the documents it names.
     */
    private static Map<String, Object> declaration() {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("name", "meta.line.declaration." + SCOPE);
        rule.put("begin", LINE + "\\b(" + words(DeclarationParser.WORDS) + ")\\b(:?)");
        rule.put("beginCaptures", Map.of("1", named("keyword.other.declaration"), "2", named("punctuation.separator.term")));
        rule.put("end", LINE_END);
        rule.put("patterns", List.of(Map.of("match", SLUG, "name", "entity.name.type.reference." + SCOPE)));
        return rule;
    }

    /**
     * {@code bipolar-transistor}, {@code soic-8-footprint}: a document named where a value stands — written without
     * spaces around its hyphens, which is how a slug differs from a subtraction.
     */
    private static Map<String, Object> reference() {
        return Map.of("match", "(?<![A-Za-z0-9_.-])[a-z0-9][a-z0-9]*(?:-[a-z0-9]*[a-z][a-z0-9]*)+(?:@[0-9]+)?(?![A-Za-z0-9_(-])",
                "name", "entity.name.type.reference." + SCOPE);
    }

    /** {@code meta { key value }}: its keys, the group a key holds in braces, and their values. */
    private static Map<String, Object> metaBlock() {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("name", "meta.block.meta." + SCOPE);
        rule.put("begin", LINE + "\\b(" + MetaParser.WORD + ")\\b[ \\t]*(\\{)");
        rule.put("beginCaptures", Map.of("1", named("keyword.control.block"), "2", named("punctuation.section.block.begin")));
        rule.put("end", "\\}");
        rule.put("endCaptures", Map.of("0", named("punctuation.section.block.end")));
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("begin", LINE + "(" + NAME + ")[ \\t]*(\\{)");
        group.put("beginCaptures", Map.of("1", named("support.type.property-name.meta"), "2", named("punctuation.section.block.begin")));
        group.put("end", "\\}");
        group.put("endCaptures", Map.of("0", named("punctuation.section.block.end")));
        group.put("patterns", List.of(include("comment"), include("string")));
        Map<String, Object> key = Map.of("match", LINE + "(" + NAME + ")(:?)(?=[ \\t])",
                "captures", Map.of("1", named("support.type.property-name.meta"), "2", named("punctuation.separator.term")));
        rule.put("patterns", List.of(include("comment"), group, key, include("value")));
        return rule;
    }

    /** {@code behaviour { … }} and {@code run { … }}: jMS, coloured by jMS's own rules. */
    private static Map<String, Object> scriptBody(LanguageWalk language) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("name", "meta.embedded.block.jms");
        rule.put("begin", LINE + "\\b(" + words(language.scripts()) + ")\\b[^{\\n]*(\\{)");
        rule.put("beginCaptures", Map.of("1", named("keyword.control.block"), "2", named("punctuation.section.block.begin")));
        rule.put("end", "\\}");
        rule.put("endCaptures", Map.of("0", named("punctuation.section.block.end")));
        rule.put("patterns", List.of(include("statement")));
        return rule;
    }

    /**
     * A block, both ways it is written: {@code word head { lines }}, its braces opened on its line, and — where it says all
     * of it there — {@code word head}, to the end of the line. Its word is its own, or the one a term writes it under —
     * {@code when failed { … }} is {@code layer(live, failed) { … }}.
     */
    private static Map<String, Object> block(ClauseBlockParser block) {
        Set<String> spellings = new TreeSet<>(Set.of(block.word()));
        block.header().map(LineGrammar::termWord).ifPresent(spellings::add);
        String word = words(spellings);
        Set<String> headWords = new TreeSet<>();
        block.header().ifPresent(header -> collect(header, headWords, new TreeSet<>()));
        List<Object> head = List.of(words("keyword.other.clause", headWords), include("term"), include("value"));
        List<Object> alternatives = new ArrayList<>();
        if (block.body() != ClauseBlockParser.Body.NONE) {
            Map<String, Object> braced = new LinkedHashMap<>();
            braced.put("name", "meta.block." + safe(block.word()) + "." + SCOPE);
            // A head holds whole texts, whatever they say — a ';' or a '#' inside quotes ends nothing.
            braced.put("begin", LINE + "\\b(" + word + ")\\b((?:\"[^\"]*\"|[^{}\\n#;\"])*)(\\{)");
            braced.put("beginCaptures", Map.of("1", named("keyword.control.block"), "2", Map.of("patterns", head),
                    "3", named("punctuation.section.block.begin")));
            braced.put("end", "\\}");
            braced.put("endCaptures", Map.of("0", named("punctuation.section.block.end")));
            braced.put("patterns", block.lines() == null ? List.of(include("value")) : lineRules(block.lines()));
            alternatives.add(braced);
        }
        if (block.body() != ClauseBlockParser.Body.REQUIRED) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("name", "meta.line." + safe(block.word()) + "." + SCOPE);
            line.put("begin", LINE + "\\b(" + word + ")\\b(?![:(])");
            line.put("beginCaptures", Map.of("1", named("keyword.control.block")));
            line.put("end", LINE_END);
            line.put("patterns", head);
            alternatives.add(line);
        }
        return patterns(alternatives);
    }

    /** What a body of lines holds: its blocks, the words its lines open with, their clauses and values, names, equations. */
    private static List<Object> lineRules(LineSet lines) {
        Set<String> blocks = new LinkedHashSet<>();
        Set<String> opening = new TreeSet<>();
        Set<String> clauses = new TreeSet<>();
        Set<String> vocabulary = new TreeSet<>();
        boolean named = false;
        boolean equations = false;
        for (LineReader reader : lines.readers()) {
            for (LineReader alternative : reader.alternatives()) {
                switch (alternative) {
                    case ClauseBlockParser block -> blocks.add(block.word());
                    case ScriptBlockParser ignored -> blocks.add("script-body");
                    case ClauseLineParser line -> {
                        LineGrammar grammar = line.grammar();
                        if (grammar.isNamed()) {
                            named = true;
                        } else {
                            opening.add(grammar.word());
                        }
                        opening.addAll(grammar.prefixes());
                        equations |= grammar.asWritten();
                        collect(grammar, clauses, vocabulary);
                    }
                    case FormulaLineParser ignored -> equations = true;
                    default -> {
                    }
                }
            }
        }
        List<Object> patterns = new ArrayList<>(List.of(include("comment")));
        blocks.forEach(word -> patterns.add(include(word.equals("script-body") ? word : blockRule(word))));
        if (!opening.isEmpty()) {
            patterns.add(Map.of("match", LINE + "\\b(" + words(opening) + ")\\b(?![:(])",
                    "captures", Map.of("1", named("keyword.other.line"))));
        }
        if (named) {
            patterns.add(Map.of("match", LINE + "(" + NAME + "(?:\\." + NAME + ")*)(?=[ \\t]+[A-Za-z0-9\"(=+-])",
                    "captures", Map.of("1", named("variable.other.declaration"))));
        }
        if (equations) {
            patterns.add(Map.of("match", "->", "name", "keyword.operator.branch." + SCOPE));
        }
        patterns.add(words("keyword.other.clause", clauses));
        patterns.add(words("constant.language.vocabulary", vocabulary));
        patterns.add(include("term"));
        patterns.add(include("value"));
        return patterns;
    }

    /** A line's clause words, and the words its slots and clauses may be, however deep its nested clauses go. */
    private static void collect(LineGrammar grammar, Set<String> clauses, Set<String> vocabulary) {
        for (LineGrammar.Slot slot : grammar.slots()) {
            vocabulary.addAll(slot.allowed());
            if (slot.mark() != null) {
                clauses.add(slot.mark());
            }
        }
        for (LineGrammar.Clause clause : grammar.clauses()) {
            clauses.add(clause.word());
            vocabulary.addAll(clause.allowed());
            if (clause.isNested()) {
                collect(clause.nested(), clauses, vocabulary);
            }
        }
    }

    /** {@code word:name(…)}, {@code word(…)}, {@code word:name}: a line or a block written as a term. */
    private static Map<String, Object> term(LanguageWalk language) {
        Set<String> heads = new TreeSet<>(language.terms());
        heads.addAll(language.kinds().keySet());
        return patterns(List.of(
                Map.of("match", "\\b(" + words(heads) + ")(?:(:)(" + SLUG + "))?(?=[ \\t]*[({])",
                        "captures", Map.of("1", named("keyword.other.term"), "2", named("punctuation.separator.term"),
                                "3", named("entity.name.tag.term"))),
                Map.of("match", "\\b(" + words(heads) + ")(:)(" + SLUG + ")",
                        "captures", Map.of("1", named("keyword.other.term"), "2", named("punctuation.separator.term"),
                                "3", named("entity.name.tag.term"))),
                include("argument-name")));
    }

    /** {@code type: bjt}, {@code U_output: 5 V}: an argument written by its name. */
    private static Map<String, Object> argumentName() {
        return Map.of("match", "(?<![A-Za-z0-9_.])(" + NAME + ")[ \\t]*(:)(?![:=])",
                "captures", Map.of("1", named("variable.parameter.argument"), "2", named("punctuation.separator.term")));
    }

    /** What a value is: a quantity, a text, a range, a tolerance, an expression as jMS colours one. */
    private static Map<String, Object> value() {
        return patterns(List.of(include("quantity"), include("string"), include("reference"),
                Map.of("match", "\\.\\.", "name", "keyword.operator.range." + SCOPE),
                Map.of("match", "\\+-|±", "name", "keyword.operator.tolerance." + SCOPE),
                include("expression")));
    }

    /**
     * {@code 4.7 kOhm}, {@code 0.3 V/us}, {@code 25 °C}, {@code 52.4%}: a number with its unit as the units library reads
     * one — a prefix only before a unit that takes one, a compound of factors joined by {@code *} or {@code /} —, each unit
     * also as a datasheet writes its sign.
     */
    private static Map<String, Object> quantity() {
        Set<String> prefixes = new TreeSet<>();
        for (SiPrefix prefix : SiPrefix.values()) {
            prefixes.addAll(spellings(String.valueOf(prefix.symbol())));
        }
        Set<String> prefixable = new TreeSet<>(LONGEST_FIRST);
        Set<String> standalone = new TreeSet<>(LONGEST_FIRST);
        for (Unit unit : Units.all()) {
            (unit.prefixable() ? prefixable : standalone).addAll(spellings(unit.symbol()));
        }
        String prefix = "(?:" + quoted(prefixes) + ")";
        String factor = "(?:" + prefix + "?(?:" + quoted(prefixable) + ")|(?:" + quoted(standalone) + "))(?:\\^-?[0-9]+)?";
        String unit = "(?:1/" + factor + "|" + factor + ")(?:[*/]" + factor + ")*(?![A-Za-z0-9_°Ω])";
        return Map.of("match", "(" + NUMBER + ")[ \\t]*(" + unit + ")",
                "captures", Map.of("1", named("constant.numeric"), "2", named("keyword.other.unit")));
    }

    /** A symbol as it is read and as a datasheet may write it: {@code Ohm} and {@code Ω}, {@code u} and {@code µ}. */
    private static Set<String> spellings(String symbol) {
        Set<String> spellings = new LinkedHashSet<>(List.of(symbol));
        Units.writtenSigns().forEach((sign, read) -> {
            if (!read.isEmpty() && symbol.contains(read) && Character.isLetter(read.charAt(0))) {
                spellings.add(symbol.replace(read, String.valueOf(sign)));
            }
        });
        return spellings;
    }

    // ─── the bundle ─────────────────────────────────────────────────────────────────────────────────

    private static Map<String, Object> packageManifest() {
        Map<String, Object> language = new LinkedHashMap<>();
        language.put("id", SCOPE);
        language.put("aliases", List.of("jM", "jMouse CAD documents", SCOPE));
        language.put("extensions", List.of("." + SCOPE));
        language.put("configuration", "./language-configuration.json");
        Map<String, Object> grammar = new LinkedHashMap<>();
        grammar.put("language", SCOPE);
        grammar.put("scopeName", "source." + SCOPE);
        grammar.put("path", "./" + GRAMMAR);
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("name", "jm-textmate");
        manifest.put("displayName", "jMouse CAD documents (.jm)");
        manifest.put("description", "Syntax highlighting for the jmCAD document language — components, circuits, symbols, "
                + "portraits, frames, footprints and boards —, described from the language's own grammar.");
        manifest.put("version", "1.0.0");
        manifest.put("publisher", "jmouse");
        manifest.put("license", "MIT");
        manifest.put("engines", Map.of("vscode", "^1.60.0"));
        manifest.put("categories", List.of("Programming Languages"));
        manifest.put("contributes", Map.of("languages", List.of(language), "grammars", List.of(grammar)));
        return manifest;
    }

    private static Map<String, Object> languageConfiguration() {
        List<List<String>> brackets = List.of(List.of("{", "}"), List.of("[", "]"), List.of("(", ")"));
        List<Map<String, String>> closing = List.of(Map.of("open", "{", "close", "}"), Map.of("open", "[", "close", "]"),
                Map.of("open", "(", "close", ")"), Map.of("open", "\"", "close", "\""));
        Map<String, Object> configuration = new LinkedHashMap<>();
        configuration.put("comments", Map.of("lineComment", "#"));
        configuration.put("brackets", brackets);
        configuration.put("autoClosingPairs", closing);
        configuration.put("surroundingPairs", closing);
        return configuration;
    }

    // ─── helpers ────────────────────────────────────────────────────────────────────────────────────

    private static String blockRule(String word) {
        return "block-" + safe(word);
    }

    private static String safe(String word) {
        return word.replaceAll("[^A-Za-z0-9]+", "-");
    }

    private static Map<String, Object> include(String rule) {
        return Map.of("include", "#" + rule);
    }

    private static Map<String, Object> named(String scope) {
        return Map.of("name", scope + "." + SCOPE);
    }

    private static Map<String, Object> patterns(List<Object> patterns) {
        return Map.of("patterns", patterns);
    }

    /** Whole words of a list as one scope: a line's clause words, the words its values may be. */
    private static Object words(String scope, Set<String> words) {
        if (words.isEmpty()) {
            return Map.of("match", "(?!)");
        }
        return Map.of("match", "(?<![A-Za-z0-9_.-])(" + words(words) + ")(?![A-Za-z0-9_:(-])", "name", scope + "." + SCOPE);
    }

    /** Words as one alternation, the longest first, so a word is never matched by the start of another. */
    private static String words(Set<String> words) {
        Set<String> longestFirst = new TreeSet<>(LONGEST_FIRST);
        longestFirst.addAll(words);
        return quoted(longestFirst);
    }

    private static String quoted(Set<String> words) {
        return String.join("|", words.stream().map(TextMateGeneration::escaped).toList());
    }

    /** A word as a regular expression matches it: its signs escaped, nothing else changed. */
    private static String escaped(String word) {
        return word.replaceAll("([\\\\.*+?^$|()\\[\\]{}%])", "\\\\$1");
    }
}
