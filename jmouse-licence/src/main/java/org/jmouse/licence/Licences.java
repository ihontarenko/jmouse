package org.jmouse.licence;

import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.sheet.SheetField;
import org.jmouse.sheet.SheetReader;
import org.jmouse.sheet.SheetValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 🧾 The terms of the licences works are taken under, and the ruling they give for a use — the one place a program asks whether a
 * work it did not make may be built into its own, passed on, or made its own, and what it then owes. The terms are data, a sheet
 * ({@code licences.sht} beside this class): a new licence is a new entry, never code.
 *
 * <p>A licence is ruled on as its terms say: one that forbids passing on refuses every use; one known to be passed on only within
 * what it came with refuses all but embedding; then the conditions that remain once an exception has waived its own for the use —
 * share-alike, which an adoption cannot meet and a redistribution meets only under the same licence, and attribution. Of licences
 * that all hold ({@code AND}) the strictest rules; of licences any one of which may be chosen ({@code OR}), the lightest.</p>
 */
public final class Licences {

    private static final String RESOURCE = "licences.sht";

    /** Where SPDX identifiers stand in the table's {@code home} pattern. */
    private static final String IDENTIFIER = "{id}";

    private final Map<String, LicenceTerms>     licences;
    private final Map<String, LicenceException> exceptions;
    private final LicenceTerms                  unstated;
    private final LicenceTerms                  unknown;
    private final String                        home;
    private final String                        noticeFile;

    private Licences(Map<String, LicenceTerms> licences, Map<String, LicenceException> exceptions, LicenceTerms unstated,
                     LicenceTerms unknown, String home, String noticeFile) {
        this.licences = Map.copyOf(licences);
        this.exceptions = Map.copyOf(exceptions);
        this.unstated = unstated;
        this.unknown = unknown;
        this.home = home;
        this.noticeFile = noticeFile;
    }

    /** The table this library ships. */
    public static Licences standard() {
        return Shipped.STANDARD;
    }

    /** The licences a sheet writes, as {@code licences.sht} does. */
    public static Licences of(Sheet sheet) {
        Map<String, SheetEntry> licenceEntries = entriesOf(sheet, "licences").stream()
                .collect(Collectors.toMap(SheetEntry::id, Function.identity()));
        Map<String, LicenceTerms> licences = licenceEntries.values().stream().map(Licences::terms)
                .collect(Collectors.toMap(terms -> key(terms.spdx()), Function.identity()));
        Map<String, LicenceException> exceptions = entriesOf(sheet, "exceptions").stream().map(Licences::exception)
                .collect(Collectors.toMap(exception -> key(exception.spdx()), Function.identity()));
        return new Licences(licences, exceptions, named(sheet, licenceEntries, "unstated"), named(sheet, licenceEntries, "unknown"),
                headText(sheet, "home"), headText(sheet, "notice-file"));
    }

    /** The terms of a licence by its SPDX identifier, in any case; empty for one the table does not know. */
    public Optional<LicenceTerms> terms(String spdx) {
        return Optional.ofNullable(licences.get(key(spdx)));
    }

    /** An exception by its SPDX identifier, in any case; empty for one the table does not know. */
    public Optional<LicenceException> exception(String spdx) {
        return Optional.ofNullable(exceptions.get(key(spdx)));
    }

    /** Where a licence's or an exception's own text is read. */
    public String home(String spdx) {
        return home.replace(IDENTIFIER, spdx);
    }

    /** Where each licence and exception an expression names is read; none for a work that states none. */
    public List<LicenceHome> homes(String expression) {
        if (expression == null || expression.isBlank()) {
            return List.of();
        }
        return LicenceExpression.read(expression).identifiers().stream().map(spdx -> new LicenceHome(spdx, home(spdx))).toList();
    }

    /** The name of the file the credit of borrowed works travels in, beside what is passed on. */
    public String noticeFile() {
        return noticeFile;
    }

    /**
     * What a work's licence answers for a use.
     *
     * @param expression its SPDX expression; {@code null} or blank for a work that states none, held to the terms of {@code unstated}
     * @throws IllegalArgumentException for a text that is not an SPDX expression
     */
    public LicenceRuling rule(String expression, LicenceUse use) {
        if (expression == null || expression.isBlank()) {
            Judgement judged = judge(unstated, unstated.spdx(), Optional.empty(), use);
            return new LicenceRuling(null, use, judged.verdict(), "No licence is stated. " + judged.reason());
        }
        Judgement judged = judge(LicenceExpression.read(expression), use);
        return new LicenceRuling(expression.strip(), use, judged.verdict(), judged.reason());
    }

    /** A verdict and the sentence that explains it. */
    private record Judgement(Verdict verdict, String reason) {
    }

    private Judgement judge(LicenceExpression expression, LicenceUse use) {
        return switch (expression) {
            case LicenceExpression.Single single -> {
                Optional<LicenceTerms> known = terms(single.licence());
                Judgement judged = judge(known.orElse(unknown), single.licence(), single.exception(), use);
                yield known.isPresent() ? judged
                        : new Judgement(judged.verdict(), single.licence() + " is a licence this table does not know. " + judged.reason());
            }
            case LicenceExpression.AllOf all -> all.parts().stream().map(part -> judge(part, use))
                    .max(Comparator.comparing(Judgement::verdict)).orElseThrow();
            case LicenceExpression.AnyOf any -> any.parts().stream().map(part -> judge(part, use))
                    .min(Comparator.comparing(Judgement::verdict)).orElseThrow();
        };
    }

    /** @param named what the licence is called in the reason: its own identifier, even where the table does not know it */
    private Judgement judge(LicenceTerms terms, String named, Optional<String> exceptionName, LicenceUse use) {
        if (terms.redistribution() == Redistribution.FORBIDDEN) {
            return new Judgement(Verdict.REFUSED, named + " does not let it be passed on.");
        }
        if (terms.redistribution() == Redistribution.EMBEDDED_ONLY && use != LicenceUse.EMBEDDED) {
            return new Judgement(Verdict.REFUSED, "Under " + named + " it travels only inside what it came with, never as itself.");
        }
        Set<LicenceCondition> conditions = EnumSet.noneOf(LicenceCondition.class);
        if (terms.attribution() == Attribution.REQUIRED) {
            conditions.add(LicenceCondition.ATTRIBUTION);
        }
        if (terms.shareAlike() != ShareAlike.NONE) {
            conditions.add(LicenceCondition.SHARE_ALIKE);
        }
        Optional<LicenceException> exception = exceptionName.flatMap(this::exception);
        exception.ifPresent(found -> conditions.removeAll(found.waivedFor(use)));
        String waiver = exception.filter(found -> !found.waivedFor(use).isEmpty())
                .map(found -> " " + found.spdx() + " waives " + found.waivedFor(use).stream().map(LicenceCondition::word).sorted()
                        .collect(Collectors.joining(" and ")) + " for it.").orElse("");
        String unknownException = exceptionName.filter(name -> exception.isEmpty())
                .map(name -> " " + name + " is an exception this table does not know, so it waives nothing.").orElse("");
        if (conditions.contains(LicenceCondition.SHARE_ALIKE)) {
            return switch (use) {
                case ADOPTED -> new Judgement(Verdict.REFUSED, named + " keeps its own licence on it, so it cannot be made another's: "
                                                               + "draw your own instead." + unknownException);
                case REDISTRIBUTED -> new Judgement(Verdict.SAME_TERMS, "It is passed on only under " + named + ", credited."
                                                                        + unknownException);
                case EMBEDDED -> terms.shareAlike() == ShareAlike.STRONG
                        ? new Judgement(Verdict.SAME_TERMS, "What it is built into takes " + named + " too, credited." + unknownException)
                        : new Judgement(Verdict.ATTRIBUTED, "It keeps " + named + " inside what it is built into; credit it."
                                                            + unknownException);
            };
        }
        if (conditions.contains(LicenceCondition.ATTRIBUTION)) {
            return new Judgement(Verdict.ATTRIBUTED, named + " asks for its credit to travel with it." + waiver + unknownException);
        }
        return new Judgement(Verdict.FREE, named + " asks nothing of this use." + waiver);
    }

    // ── Reading the table

    private static List<SheetEntry> entriesOf(Sheet sheet, String group) {
        return sheet.entry(group).map(SheetEntry::entries).orElseThrow(() -> missing(sheet.slug(), group));
    }

    private static String headText(Sheet sheet, String field) {
        return text(sheet.field(field).map(SheetField::value).orElseThrow(() -> missing(sheet.slug(), field)));
    }

    private static LicenceTerms named(Sheet sheet, Map<String, SheetEntry> entries, String field) {
        String id = headText(sheet, field);
        return Optional.ofNullable(entries.get(id)).map(Licences::terms)
                .orElseThrow(() -> new IllegalArgumentException("'" + field + "' names '" + id + "', which is no licence of '" + sheet.slug() + "'"));
    }

    private static LicenceTerms terms(SheetEntry entry) {
        return new LicenceTerms(text(value(entry, "spdx")), entry.title().orElse(entry.id()),
                TermWord.of(Redistribution.class, text(value(entry, "redistribution"))),
                TermWord.of(Attribution.class, text(value(entry, "attribution"))),
                TermWord.of(ShareAlike.class, text(value(entry, "share-alike"))));
    }

    private static LicenceException exception(SheetEntry entry) {
        return new LicenceException(text(value(entry, "spdx")), entry.title().orElse(entry.id()),
                words(value(entry, "waives")).stream().map(word -> TermWord.of(LicenceCondition.class, word)).collect(Collectors.toSet()),
                words(value(entry, "within")).stream().map(word -> TermWord.of(LicenceUse.class, word)).collect(Collectors.toSet()));
    }

    private static SheetValue value(SheetEntry entry, String name) {
        return entry.value(name).orElseThrow(() -> missing(entry.id(), name));
    }

    /** A value written as one text or one word. */
    private static String text(SheetValue value) {
        return switch (value) {
            case SheetValue.Text(String text) -> text;
            case SheetValue.Word(String word) -> word;
            default -> throw new IllegalArgumentException("'" + value.written() + "' is written where a word or a text is");
        };
    }

    /** A value written as words between commas, or as one word. */
    private static List<String> words(SheetValue value) {
        List<SheetValue> items = value instanceof SheetValue.Sequence(List<SheetValue> sequence) ? sequence : List.of(value);
        return items.stream().map(Licences::text).toList();
    }

    private static String key(String spdx) {
        return spdx.strip().toLowerCase(Locale.ROOT);
    }

    private static IllegalArgumentException missing(String where, String name) {
        return new IllegalArgumentException("'" + where + "' writes no '" + name + "'");
    }

    /** Holds the shipped table, read the first time it is asked for. */
    private static final class Shipped {

        private static final Licences STANDARD = of(read());

        private static Sheet read() {
            try (InputStream stream = Licences.class.getResourceAsStream(RESOURCE)) {
                if (stream == null) {
                    throw new IllegalStateException("'" + RESOURCE + "' is not beside " + Licences.class.getName());
                }
                return SheetReader.read(RESOURCE, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        }
    }
}
