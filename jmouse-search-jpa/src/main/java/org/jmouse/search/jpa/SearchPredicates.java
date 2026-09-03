package org.jmouse.search.jpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jmouse.search.SearchTerms;

import java.util.Arrays;
import java.util.Collection;

/**
 * 🗄️ Turning words into the loose query that finds the candidates.
 *
 * <h3>⚠️ Criteria and not a {@code @Query}, because the shape depends on the question</h3>
 *
 * <p>A query with one predicate per term cannot be written as a fixed JPQL string at all: the number of
 * predicates is the number of words somebody typed. What both products in this family wrote instead was
 * a single {@code LIKE '%…%'} over the whole query — which is exactly why a two-word search found
 * nothing.</p>
 *
 * <h3>⚠️ It builds the LOOSE predicate on purpose — any term, not every term</h3>
 *
 * <p>This is the half people get backwards. The strict rule — <em>every term, in any field</em> — is
 * applied once, in Java, by {@code Relevance.matched()}. Writing it into the query as well would be two
 * implementations of one rule that must agree forever, and the way they stop agreeing is a row one
 * surface finds and another does not. The query's job is only to stop the scan being the whole table.</p>
 *
 * <h3>⚠️ A leading wildcard cannot use an index, and nothing here hides that</h3>
 *
 * <p>{@code LIKE '%term%'} is a scan. It is free at a few thousand rows and it is not a plan for a
 * million; where it stops being free is a measurement somebody has to take, per product, against real
 * data. When it does stop, the replacement is not this class with better SQL — it is a different
 * {@code Scorer} fed by the database's own full-text ranking, which is what {@code MatchKind.EXTERNAL}
 * exists for.</p>
 */
public final class SearchPredicates {

    private SearchPredicates() {
    }

    /**
     * Rows where <strong>any</strong> term appears in <strong>any</strong> of these attributes.
     *
     * <p>Attribute names are the entity's, and may be dotted to reach an association —
     * {@code "author.displayName"}.
     *
     * @return a predicate, or {@link CriteriaBuilder#disjunction()} — which matches nothing — when there
     *         is nothing to ask. ⚠️ Never {@code null} and never a conjunction: an empty query that
     *         quietly matched everything would answer a cleared search box with the whole table.
     */
    public static Predicate anyTermIn(
            CriteriaBuilder builder, Root<?> root, SearchTerms terms, Collection<String> attributes) {

        if (terms.empty() || attributes.isEmpty()) {
            return builder.disjunction();
        }

        return builder.or(terms.terms().stream()
                .map(term -> builder.or(attributes.stream()
                        .map(attribute -> like(builder, pathTo(root, attribute), term))
                        .toArray(Predicate[]::new)))
                .toArray(Predicate[]::new));
    }

    /** The same, spelled inline. */
    public static Predicate anyTermIn(
            CriteriaBuilder builder, Root<?> root, SearchTerms terms, String... attributes) {

        return anyTermIn(builder, root, terms, Arrays.asList(attributes));
    }

    /**
     * ⚠️ Both sides are lower-cased in the query rather than relying on the column's collation. A
     * case-insensitive collation is a schema decision that differs per product, per dialect and
     * sometimes per column, and a search that works on MySQL and quietly stops working on PostgreSQL is
     * the kind of thing nobody notices for a month.
     */
    private static Predicate like(CriteriaBuilder builder, Expression<String> column, String term) {
        return builder.like(builder.lower(column), "%" + term.toLowerCase() + "%");
    }

    private static Path<String> pathTo(Root<?> root, String attribute) {
        Path<?> path = root;

        for (String step : attribute.split("\\.")) {
            path = path.get(step);
        }

        @SuppressWarnings("unchecked")
        Path<String> text = (Path<String>) path;

        return text;
    }

}
