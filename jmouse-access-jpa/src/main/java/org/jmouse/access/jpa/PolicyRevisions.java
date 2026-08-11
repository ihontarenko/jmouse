package org.jmouse.access.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * The editable policy document's history — append-only, read from its newest end.
 *
 * <h2>⚠️ Why the library holds this at all</h2>
 *
 * <p>A policy document that can be saved without a deploy is authorization changed by a screen, and
 * <em>what was in force in March</em> is the question that makes such a screen defensible. The table
 * is created by this module's own migration, so by the rule this whole effort turns on — <em>whoever
 * owns the table owns the mapping</em> — the entity and its queries belong here too.
 *
 * <p>What stays the product's is everything around it: which document is editable, who may save one,
 * the dry run, the lockout guard and the audit event. This answers only "keep this version" and "give
 * me the versions".
 *
 * <h2>⚠️ There are only ever two reads</h2>
 *
 * <p><em>What is in force</em> is the highest version; <em>what came before</em> is all of them,
 * newest first. One index answers both, and no third question has ever been asked — a
 * "search revisions by author" would be a fourth reason for this table to grow a query planner.
 */
public interface PolicyRevisions {

    /** The version in force — the newest saved, or nothing where none ever was. */
    Optional<Revision> inForce();

    /** The whole history, newest first, which is the order a revert control is read in. */
    List<Revision> history();

    Optional<Revision> byVersion(int version);

    /**
     * Keeps a version.
     *
     * <p>⚠️ The number is assigned <strong>here</strong> rather than taken, because assigning it is
     * reading the current maximum and adding one — and a caller doing that has a race between the read
     * and the write that the unique constraint turns into a failed save.
     *
     * @param revertedFrom which version this restores, or null where it is written fresh
     */
    Revision save(String source, String note, String authorId, String authorLabel,
                  Integer revertedFrom, int roles, int subjects);

    /**
     * One saved version.
     *
     * @param authorLabel who saved it, copied at the time. ⚠️ Kept beside the identifier so that the
     *                    history still says who did this after the account has gone
     * @param roles       how many roles the document declared, so a history entry can be read without
     *                    parsing the source it carries
     */
    record Revision(
            int           version,
            String        source,
            String        note,
            String        authorId,
            String        authorLabel,
            Integer       revertedFrom,
            int           roles,
            int           subjects,
            LocalDateTime createdAt
    ) {
    }
}
