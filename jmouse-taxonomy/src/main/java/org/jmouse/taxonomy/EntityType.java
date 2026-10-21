package org.jmouse.taxonomy;

import java.util.Collection;
import java.util.Map;

/**
 * 🏷️ A kind of thing a category may classify, as the host that owns those things declares it.
 *
 * <h3>⚠️ Registered, never a free string</h3>
 *
 * <p>An assignment names the entity it classifies by a type and an id. If the type were whatever a caller wrote, a
 * typo would classify nothing and nobody would know — and a category's count would include rows no host can resolve.
 * So every type is a registered implementation of this port, and an assignment of an unknown type is refused.</p>
 *
 * <h3>⚠️ The host decides what a reader may see</h3>
 *
 * <p>The taxonomy keeps ids only. Whether an id still exists, what it is called and whether this reader may know it
 * exists are the host's answers, given by {@link #titlesOf}: an id left out of the answer is, to that reader, not
 * there.</p>
 */
public interface EntityType {

    /**
     * The stable name assignments are stored under — {@code parallax-object}. Renaming it orphans every assignment
     * already written.
     */
    String name();

    /**
     * 📖 What each of these entities is called, for the ones that exist and the current reader may see.
     *
     * @param identifiers the entities asked about
     * @return each visible entity's title by its id; an id missing is unknown or hidden
     */
    Map<String, String> titlesOf(Collection<String> identifiers);
}
