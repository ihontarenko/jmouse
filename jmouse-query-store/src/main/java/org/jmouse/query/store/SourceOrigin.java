package org.jmouse.query.store;

/**
 * Where a source's declaration comes from — and therefore whether anybody may edit it.
 *
 * <h2>⚠️ This is the distinction that makes an editable mapping safe to offer at all</h2>
 *
 * <p>"Store the declarations in the database so they can be edited" is one sentence that covers two
 * unlike things, and applying it to both is how a product acquires a screen that quietly lies.</p>
 *
 * <ul>
 *   <li>A <strong>{@link #DERIVED}</strong> source has no author. What a query may name is decided by
 *       somebody adding a field on a form screen, so the declaration is a *consequence* of data that
 *       already exists. Storing an editable copy would create a second truth that goes stale at the
 *       next field, and then something has to lose: either the edit is silently overwritten, or the
 *       screen keeps showing a shape the engine stopped using.</li>
 *   <li>An <strong>{@link #AUTHORED}</strong> source is a document. Somebody wrote it, over tables that
 *       do not move on their own, and changing it is a deliberate act with a reason.</li>
 * </ul>
 *
 * <h2>⚠️ And an authored mapping is a privilege, not a preference</h2>
 *
 * <p>A mapping names <strong>tables and columns</strong>. Whoever may edit one may point a source at
 * any table the connection can reach and read it back through the ordinary query API — the permission
 * checks on the listing are about rows, not about which table the rows came from. So an authored source
 * is guarded twice: by a permission the subject decides, and by an allow-list of the tables a product
 * is willing to publish. Neither guard is optional, and neither belongs to the browser.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum SourceOrigin {

    /**
     * Built from something else that is already true — a form, a catalogue, a registry.
     *
     * <p>Readable, projectable, never editable. This is the default because it is the safe answer: a
     * subject that has not thought about the question cannot accidentally become writable.</p>
     */
    DERIVED,

    /**
     * Written by a person, kept as a row, edited as jMQ or through a builder.
     *
     * <p>⚠️ Declaring this is what turns the guards on, not off — see the class note.</p>
     */
    AUTHORED
}
