package org.jmouse.query.store;

import java.util.List;
import java.util.Optional;

/**
 * Where authored declarations are kept.
 *
 * <h2>⚠️ It stores TEXT and validates nothing</h2>
 *
 * <p>This port has no idea what jMQ is, cannot parse it, and must not learn to. Whether a body is well
 * formed, whether its mapping names a table this installation is willing to publish, and whether the
 * caller may write it at all are three different questions answered in three different places — by the
 * language, by the allow-list, and by the subject. A store that also validated would be a fourth
 * opinion, and the fourth opinion is the one nobody remembers to update.</p>
 *
 * <p>⚠️ Deliberately separate from {@link SavedQueries} rather than a second kind of row in it. A saved
 * query is a <em>question</em> asked of a source; this is the source itself. They differ in who may
 * write them, in how many there can be, and in what a bad one costs — one shows the wrong rows, the
 * other reaches the wrong table.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface AuthoredSources {

    /**
     * 🔎 The declaration kept for this source, if there is one.
     *
     * <p>⚠️ Empty is an ordinary answer and means <em>nobody has written one</em> — the caller then uses
     * whatever the product built in code. It never means the source does not exist.</p>
     *
     * @param owner     whose declaration
     * @param sourceKey which source
     * @return the declaration, or empty
     */
    Optional<AuthoredSource> find(QueryOwner owner, String sourceKey);

    /**
     * 💾 Write it, replacing whatever was there.
     *
     * <p>An upsert rather than a create-or-update pair: there is at most one declaration per owner and
     * source, so the caller never has to know which of the two it is doing.</p>
     *
     * @param source what to keep
     * @return what was kept, with its stamp
     */
    AuthoredSource save(AuthoredSource source);

    /**
     * 🗑 Forget it, so the source falls back to what the product built in code.
     *
     * <p>⚠️ Which is why this is not destructive in the way it looks: removing an authored declaration
     * restores the one that ships with the product rather than leaving the source undefined.</p>
     *
     * @param owner     whose declaration
     * @param sourceKey which source
     * @return whether there was one
     */
    boolean remove(QueryOwner owner, String sourceKey);

    /**
     * 📋 Everything this owner has written, for a management screen.
     *
     * @param owner whose declarations
     * @return the declarations, oldest name first
     */
    List<AuthoredSource> list(QueryOwner owner);
}
