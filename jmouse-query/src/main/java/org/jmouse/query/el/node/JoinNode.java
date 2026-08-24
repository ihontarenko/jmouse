package org.jmouse.query.el.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.query.el.SourceWriter;

/**
 * {@code join statuses on status_id key id} — a table one hop away, and how to get there.
 *
 * <p>Three names: the table, the column on <em>our</em> row that points at it, and the column on
 * <em>its</em> row that is pointed at. An attribute then reads a column of it —
 * {@code attribute issue.status.category from statuses.category text in join}.</p>
 *
 * <h2>⚠️ One hop, and not a general join</h2>
 *
 * <p>What products actually need is a normalised value: a status's category, a type's name, an epic's
 * key. Arbitrary joins would let a saved query reach anything the database holds, and the whole point of
 * a declared source is that it cannot — the schema is the confinement, not a starting point.</p>
 *
 * <h2>⚠️ The alias is keyed on the TABLE, unlike a bag's</h2>
 *
 * <p>Two attributes out of {@code statuses} are two columns of the same row, so they share one join.
 * Two attributes out of a bag are two different rows, and sharing an alias there asks one row to be two
 * things at once. Same word, opposite rule — which is why they are separate declarations.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class JoinNode extends AbstractExpression {

    private String table;
    private String localColumn;
    private String foreignColumn;

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    /** The column on the source's own row that points at the other table. */
    public String getLocalColumn() {
        return localColumn;
    }

    public void setLocalColumn(String localColumn) {
        this.localColumn = localColumn;
    }

    /** The column on the other table that is pointed at — usually its key. */
    public String getForeignColumn() {
        return foreignColumn;
    }

    public void setForeignColumn(String foreignColumn) {
        this.foreignColumn = foreignColumn;
    }

    @Override
    public String toSource() {
        return "join %s on %s key %s".formatted(
                SourceWriter.name(table), SourceWriter.name(localColumn), SourceWriter.name(foreignColumn));
    }

    @Override
    public String toString() {
        return toSource();
    }
}
