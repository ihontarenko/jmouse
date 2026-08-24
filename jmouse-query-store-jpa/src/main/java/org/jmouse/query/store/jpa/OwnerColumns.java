package org.jmouse.query.store.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.jmouse.query.store.QueryOwner;

/**
 * 🏷️ The two columns that say what holds a saved query.
 *
 * <p>Embedded rather than two loose fields so that a listing can be written against one thing —
 * {@code query.owner = :owner} — instead of restating the pair at every call site, which is where a
 * polymorphic key normally starts leaking.</p>
 *
 * <p>⚠️ Neither column is updatable. A query that moved to another owner is a different query wearing
 * the same name, and a board pointing at it by identifier would follow the move without being asked.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Embeddable
public class OwnerColumns {

    @Column(name = "owner_type", length = QueryOwner.MAXIMUM_LENGTH, nullable = false, updatable = false)
    private String type;

    @Column(name = "owner_id", length = QueryOwner.MAXIMUM_LENGTH, nullable = false, updatable = false)
    private String identifier;

    protected OwnerColumns() {
    }

    private OwnerColumns(String type, String identifier) {
        this.type       = type;
        this.identifier = identifier;
    }

    /**
     * The columns for an owner.
     *
     * @param owner what holds the query
     * @return the embeddable
     */
    static OwnerColumns of(QueryOwner owner) {
        return new OwnerColumns(owner.type(), owner.identifier());
    }

    /**
     * The owner these columns describe.
     *
     * @return the owner
     */
    QueryOwner toReference() {
        return new QueryOwner(type, identifier);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OwnerColumns columns
               && type.equals(columns.type)
               && identifier.equals(columns.identifier);
    }

    @Override
    public int hashCode() {
        return type.hashCode() * 31 + identifier.hashCode();
    }
}
