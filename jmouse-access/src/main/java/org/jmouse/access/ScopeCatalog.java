package org.jmouse.access;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The scopes this installation has, widest first — the vocabulary the engine is handed rather than
 * the one it was compiled with.
 *
 * <p>Everything the core needs to know about places is here: how many there are, how they order, what
 * each is called, and which two of them are {@linkplain ScopeNature#EVERYTHING the widest} and
 * {@linkplain ScopeNature#OWN_ROWS the subject's own rows}. A product registers its
 * {@link ScopeKind}s once at startup; a product with departments registers three floors and a product
 * with none registers zero, and no class above this one changes either way.
 *
 * <p><strong>It validates, loudly, at construction.</strong> A vocabulary is read on every request and
 * a wrong one fails in ways that look like a permissions bug months later — two scopes sharing a rank
 * makes the covering chain's order arbitrary, and a missing {@code EVERYTHING} silently removes the
 * link that makes an unscoped grant work at all. Refusing to start is the cheap version of that
 * failure.
 *
 * @see ScopeKind
 * @see org.jmouse.access.spi.ScopeHierarchy which places sit inside which
 */
public final class ScopeCatalog {

    private final List<ScopeKind>          widestFirst;
    private final Map<String, ScopeKind>   byName;
    private final ScopeKind                everything;
    private final ScopeKind                ownRows;
    private final ScopeReference           everythingReference;
    private final ScopeReference           ownRowsReference;

    public ScopeCatalog(List<ScopeKind> kinds) {
        this.widestFirst = kinds.stream()
                .sorted(Comparator.comparingInt(ScopeKind::rank))
                .toList();

        this.byName              = indexByName(widestFirst);
        this.everything          = onlyOneOf(widestFirst, ScopeNature.EVERYTHING).orElseThrow(
                () -> new IllegalArgumentException(
                        "A scope vocabulary needs exactly one " + ScopeNature.EVERYTHING + " scope. "
                        + "Without it the covering chain has no link that always matches, and every "
                        + "unscoped grant silently stops working."));
        this.ownRows             = onlyOneOf(widestFirst, ScopeNature.OWN_ROWS).orElse(null);
        this.everythingReference = ScopeReference.of(everything, ScopeKind.NO_INSTANCE);
        this.ownRowsReference    = ownRows == null ? null : ScopeReference.of(ownRows, ScopeKind.NO_INSTANCE);

        requireUniqueRanks(widestFirst);
        requireNoCycles(widestFirst);
        requireUniversalScopesAtTheEnds(widestFirst, everything, ownRows);
    }

    /** Every scope kind, widest first. */
    public List<ScopeKind> all() {
        return widestFirst;
    }

    /**
     * A place written the way {@link ScopeReference#describe()} writes one — {@code space:42}, or just
     * {@code installation} for a kind that names no instance.
     *
     * <p>It lives here for the same reason {@link #covering(AccessTarget, String)} does: reading a place
     * out of text needs the <strong>vocabulary</strong>, and a static method could only have got the
     * vocabulary by knowing which enum to read.
     *
     * <p>⚠️ Refuses with the kinds that would have worked, and is meant to be called <em>at load</em>. A
     * scope name only checked on the first request would boot clean and then answer the same wrong
     * answer forever.
     */
    public ScopeReference parse(String written) {
        if (written == null || written.isBlank()) {
            throw new IllegalArgumentException(
                    "a place has to say which scope it means — for example 'space:42'. " + describeKinds());
        }

        String    trimmed   = written.trim();
        int       separator = trimmed.indexOf(':');
        String    name      = separator < 0 ? trimmed : trimmed.substring(0, separator);
        String    id        = separator < 0 ? null : trimmed.substring(separator + 1);
        ScopeKind kind      = byName(name).orElse(null);

        if (kind == null) {
            throw new IllegalArgumentException(
                    "'%s' is not a scope in this installation. %s".formatted(name, describeKinds()));
        }

        if (kind.namesAnInstance() && (id == null || id.isBlank())) {
            throw new IllegalArgumentException(
                    ("'%s' names one instance, so it needs an identifier — write '%s:<id>' rather than "
                     + "'%s'.").formatted(name, name, written));
        }

        return ScopeReference.of(kind, id);
    }

    private String describeKinds() {
        return "Scopes: " + widestFirst.stream().map(ScopeKind::name).collect(Collectors.joining(", "))
               + ".";
    }

    /**
     * The scopes that are <em>places</em> — the ones a target can name an instance of, widest first.
     *
     * <p>Exactly the nestings this product has, and the only list anything has to walk. Adding a floor
     * means registering one more kind; nothing that reads this changes.
     */
    public List<ScopeKind> floors() {
        return widestFirst.stream()
                .filter(kind -> kind.nature() == ScopeNature.PLACE)
                .toList();
    }

    /** The widest scope. Always present, always first in a covering chain. */
    public ScopeKind everything() {
        return everything;
    }

    /** The widest scope as a reference — what something says when it means "not anywhere narrower". */
    public ScopeReference everythingReference() {
        return everythingReference;
    }

    /** The scope meaning "rows this subject owns", where the product has one. */
    public Optional<ScopeKind> ownRows() {
        return Optional.ofNullable(ownRows);
    }

    /** The kind this name refers to, or empty where the vocabulary has no such scope. */
    public Optional<ScopeKind> byName(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    /**
     * Every scope that covers this target, widest first — the chain a resolution matches grants on.
     *
     * <p>{@linkplain ScopeNature#EVERYTHING The widest scope} is always in it, which is what makes an
     * unscoped grant keep working. {@linkplain ScopeNature#OWN_ROWS Own rows} is in it only when the
     * target names a row this subject owns, which is what makes ownership a scope rather than a
     * separate question.
     *
     * <p>It lives on the catalog rather than on {@link ScopeReference} because the two links it adds
     * are the two the model names and the product does not: building the chain needs the vocabulary,
     * and a static method could only have got the vocabulary by knowing which enum to read.
     *
     * @param ownedRowsBelongTo whose rows count as the subject's own — its master's, for an agent
     */
    public List<ScopeReference> covering(AccessTarget target, String ownedRowsBelongTo) {
        List<ScopeReference> chain = new ArrayList<>(target.places().size() + 2);

        // The widest scope always covers, which is what makes an unscoped grant keep working.
        chain.add(everythingReference);

        // Every place the target names, widest first — and the engine never learns what any of them
        // is called. A third floor lands here without this method being edited.
        chain.addAll(target.places());

        // Own rows are in the chain only when the row being touched is this subject's own, which is
        // what makes ownership a scope rather than a separate question. For a service sub-account
        // "own" means its master's, and that is decided by the caller rather than here.
        if (ownRowsReference != null && target.hasOwner() && target.ownerId().equals(ownedRowsBelongTo)) {
            chain.add(ownRowsReference);
        }

        return chain;
    }

    private static Map<String, ScopeKind> indexByName(List<ScopeKind> kinds) {
        Map<String, ScopeKind> index = new LinkedHashMap<>();

        for (ScopeKind kind : kinds) {
            ScopeKind existing = index.put(kind.name(), kind);

            if (existing != null) {
                throw new IllegalArgumentException(
                        "Two scope kinds are both called " + kind.name() + ". The name is how a grant "
                        + "is stored and read back, so it has to say which scope on its own.");
            }
        }

        return Map.copyOf(index);
    }

    private static Optional<ScopeKind> onlyOneOf(List<ScopeKind> kinds, ScopeNature nature) {
        List<ScopeKind> matching = kinds.stream()
                .filter(kind -> kind.nature() == nature)
                .toList();

        if (matching.size() > 1) {
            throw new IllegalArgumentException(
                    "More than one scope claims to be " + nature + ": " + names(matching)
                    + ". The model has room for one of each, because the engine reaches for it by "
                    + "what it means rather than by what it is called.");
        }

        return matching.stream().findFirst();
    }

    private static void requireUniqueRanks(List<ScopeKind> kinds) {
        for (int position = 1; position < kinds.size(); position++) {
            ScopeKind earlier = kinds.get(position - 1);
            ScopeKind later   = kinds.get(position);

            if (earlier.rank() == later.rank()) {
                throw new IllegalArgumentException(
                        earlier.name() + " and " + later.name() + " share position " + earlier.rank()
                        + ". Position is no longer the width — width is `inside=` — but it still has to "
                        + "be unique, because it is what makes two otherwise equal answers come back in "
                        + "the same order every time.");
            }
        }
    }

    /**
     * ⚠️ Refuse a scope that sits inside itself, however indirectly.
     *
     * <p>A cycle in {@code inside=} makes "does this grant reach there" unanswerable and would hang the
     * walk that asks it. Refused at startup, where it is one message, rather than at the first request,
     * where it is a thread that never returns.</p>
     */
    private static void requireNoCycles(List<ScopeKind> kinds) {
        for (ScopeKind kind : kinds) {
            java.util.Set<ScopeKind> seen = new java.util.LinkedHashSet<>();

            for (java.util.Optional<ScopeKind> above = kind.inside();
                 above.isPresent(); above = above.get().inside()) {

                if (above.get().equals(kind) || !seen.add(above.get())) {
                    throw new IllegalArgumentException(
                            kind.name() + " is declared inside itself, by way of "
                            + names(List.copyOf(seen)) + ". A scope cannot contain the place that "
                            + "contains it, and the walk that answers 'does this grant reach there' "
                            + "would never end.");
                }
            }
        }
    }

    private static void requireUniversalScopesAtTheEnds(
            List<ScopeKind> widestFirst, ScopeKind everything, ScopeKind ownRows) {

        if (!widestFirst.getFirst().equals(everything)) {
            throw new IllegalArgumentException(
                    everything.name() + " is the " + ScopeNature.EVERYTHING + " scope but "
                    + widestFirst.getFirst().name() + " is ranked wider than it. Nothing can be wider "
                    + "than everything.");
        }

        if (ownRows != null && !widestFirst.getLast().equals(ownRows)) {
            throw new IllegalArgumentException(
                    ownRows.name() + " is the " + ScopeNature.OWN_ROWS + " scope but "
                    + widestFirst.getLast().name() + " is ranked narrower than it. A subject's own "
                    + "rows are the narrowest thing a grant can be about.");
        }
    }

    private static String names(List<ScopeKind> kinds) {
        return kinds.stream().map(ScopeKind::name).reduce((left, right) -> left + ", " + right).orElse("");
    }
}
