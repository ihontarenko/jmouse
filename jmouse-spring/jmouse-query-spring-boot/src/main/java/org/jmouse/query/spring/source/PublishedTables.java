package org.jmouse.query.spring.source;

import org.jmouse.query.el.node.AttributeNode;
import org.jmouse.query.el.node.BagNode;
import org.jmouse.query.el.node.CollectionNode;
import org.jmouse.query.el.node.JoinNode;
import org.jmouse.query.el.node.SourceNode;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The tables an authored declaration is allowed to name.
 *
 * <h2>⚠️ This is the guard, and without it an editable mapping is a way to read any table</h2>
 *
 * <p>The permission checks on a listing are about <strong>rows</strong> — which of a table's rows this
 * caller may see. None of them ask which table the rows came from, because until a mapping could be
 * edited, the answer was fixed at compile time. It no longer is.</p>
 *
 * <p>So without an allow-list, whoever may write a declaration may point {@code issues} at the grants
 * table and read it back through the ordinary, fully-authorized query API. Every check would pass:
 * the caller really may run a query, and the query really is well formed. It would simply be reading
 * something nobody meant to publish.</p>
 *
 * <h2>⚠️ Empty means NOTHING is allowed, never everything</h2>
 *
 * <p>The obvious convenience — "an unconfigured allow-list permits any table" — turns forgetting to
 * configure it into silently switching the guard off, and a guard that is off by default is a guard
 * that is off in production. A product that has not published any table simply has no authored sources,
 * which is exactly what a product that has not thought about this should have.</p>
 *
 * <h2>⚠️ It reads the TREE, not the text</h2>
 *
 * <p>Matching table names in a string would be a second parser for a language that has one, and the
 * one it competes with is the one that decides what actually runs. So the check walks the NODE and asks
 * each place a table can be named: the mapping's own, a join's, a bag's, a collection's — and the
 * qualifier inside an attribute's source, which is a string on a node rather than a field, and is
 * exactly the one that was missed on the first attempt. A grammar that grows a sixth place breaks this
 * compilation rather than quietly letting the sixth one through.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class PublishedTables {

    private final Set<String> allowed;

    /**
     * 🏗️ The tables this installation is willing to publish.
     *
     * <p>⚠️ Compared without regard to case, because one engine folds identifiers and the other does
     * not — an allow-list that matched only in the spelling somebody happened to type would let the
     * same table through under one database and refuse it under another.</p>
     *
     * @param allowed the table names
     */
    public PublishedTables(Set<String> allowed) {
        this.allowed = allowed == null
                ? Set.of()
                : allowed.stream()
                        .map(name -> name.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Whether any table at all may be named — a product that published none has no authored sources. */
    public boolean publishesAnything() {
        return !allowed.isEmpty();
    }

    /**
     * Refuses a declaration that names a table this installation does not publish.
     *
     * <p>⚠️ Every offending table is named, not just the first. Somebody rewriting a mapping over
     * several joins would otherwise fix one, save, and be refused again — and the second refusal reads
     * as the first fix not having worked.</p>
     *
     * @param declared what is about to be stored
     * @throws UnpublishedTableException naming what may not be reached, and what may
     */
    public void require(SourceNode declared) {
        Set<String> refused = new LinkedHashSet<>();

        for (String table : named(declared)) {
            if (!allowed.contains(table.toLowerCase(Locale.ROOT))) {
                refused.add(table);
            }
        }

        if (!refused.isEmpty()) {
            throw new UnpublishedTableException(refused, allowed);
        }
    }

    /**
     * Every table this declaration reaches.
     *
     * <p>⚠️ FIVE places, and all five are checked. Four are fields on a node — the mapping's table, a
     * join's, a bag's, a collection's. The fifth is a qualifier inside a string, and checking only the
     * four left it as the way through.</p>
     */
    public static Set<String> named(SourceNode declared) {
        Set<String> tables = new LinkedHashSet<>();

        if (declared.getTable() != null) {
            tables.add(declared.getTable());
        }

        for (JoinNode join : declared.getJoins()) {
            if (join.getTable() != null) {
                tables.add(join.getTable());
            }
        }

        declared.getBag().map(BagNode::getTable).ifPresent(tables::add);

        for (CollectionNode collection : declared.getCollections()) {
            if (collection.getTable() != null) {
                tables.add(collection.getTable());
            }
        }

        qualifiers(declared, tables);

        return tables;
    }

    /**
     * ⚠️ The fifth place, and it is a STRING rather than a field — which is why it was missed.
     *
     * <p>A joined attribute is written {@code 'statuses.name' in join}, so the table it reads sits inside
     * the attribute's source, qualified with a dot. The four node fields above do not see it: a
     * declaration can name {@code 'access_grants.permission' in join} and carry no {@code join} block at
     * all, and every one of those four is empty.</p>
     *
     * <p>⚠️ That form cannot actually reach the table — a joined attribute is bound through the declared
     * {@code join} blocks, so one with no matching block is refused when the query is compiled. It is
     * caught here anyway, because <em>refused later, somewhere else, in different words</em> is how a
     * guard teaches people it is not really a guard. A refusal at save time says the true thing at the
     * moment somebody can act on it.</p>
     *
     * <p>⚠️ The mapping's own table and alias are not qualifiers of anything else, so they are skipped:
     * {@code i.summary} names this source's own row.</p>
     */
    private static void qualifiers(SourceNode declared, Set<String> tables) {
        Set<String> own = new LinkedHashSet<>();

        if (declared.getTable() != null) {
            own.add(declared.getTable().toLowerCase(Locale.ROOT));
        }

        if (declared.getAlias() != null) {
            own.add(declared.getAlias().toLowerCase(Locale.ROOT));
        }

        for (AttributeNode attribute : declared.getAttributes()) {
            String source = attribute.getSource();
            int    dot    = source == null ? -1 : source.indexOf('.');

            if (dot <= 0) {
                continue;
            }

            String qualifier = source.substring(0, dot);

            if (!own.contains(qualifier.toLowerCase(Locale.ROOT))) {
                tables.add(qualifier);
            }
        }
    }
}
