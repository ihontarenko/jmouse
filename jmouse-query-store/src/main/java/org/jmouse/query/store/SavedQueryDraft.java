package org.jmouse.query.store;

import java.util.Objects;

/**
 * ✍️ What a caller supplies when saving — everything a query is, minus everything the store decides.
 *
 * <p>No identifier, no timestamps: those belong to the act of saving rather than to the query, and a
 * caller made to invent them would be inventing them differently in every product.</p>
 *
 * <h2>Written as a chain, because most of it usually stays alone</h2>
 *
 * <pre>{@code
 * SavedQueryDraft draft = SavedQueryDraft
 *         .on("issues", QueryOwner.of("BOARD", board.getId()))
 *         .named("Blocked, mine")
 *         .writing("issue.status == 'blocked' and issue.assignee == currentUser()")
 *         .by(member.getId())
 *         .visibleToEveryone();
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record SavedQueryDraft(
        String source,
        QueryOwner owner,
        String name,
        String description,
        String body,
        String author,
        boolean shared,
        int sortOrder) {

    /** Longest a name may be, matching the column. */
    public static final int MAXIMUM_NAME_LENGTH = 255;

    /** Longest a description may be, matching the column. */
    public static final int MAXIMUM_DESCRIPTION_LENGTH = 512;

    /**
     * How long the body column is, in the units the mapping speaks.
     *
     * <p>⚠️ A sized column rather than a large object, and the difference is not cosmetic. A large
     * object is streamed by both engines, cannot be compared or indexed, and reads back through a driver
     * path that several tools render as a handle instead of as text. A saved query is a paragraph
     * somebody typed: it is read on every listing, searched, and diffed.</p>
     */
    public static final int BODY_COLUMN_LENGTH = 65535;

    /**
     * Longest a body may be, <strong>in characters</strong>.
     *
     * <p>⚠️ A quarter of the column, and the gap is the whole point. MySQL sizes {@code TEXT} in
     * <em>bytes</em>, while everything above it counts <em>characters</em> — so a query written in
     * Cyrillic reaches the column's limit at a quarter of the length an English one does, and the
     * database's answer to that is to truncate. A cap that cannot be reached in any alphabet is a cap
     * that behaves the same for everyone.</p>
     *
     * <p>Sixteen thousand characters is several hundred lines of jMQ — far past anything a person
     * writes.</p>
     */
    public static final int MAXIMUM_BODY_LENGTH = 16_000;

    public SavedQueryDraft {
        Objects.requireNonNull(source, "the source a saved query is written against");
        Objects.requireNonNull(owner, "the owner a saved query belongs to");

        // ⚠️ '*' rather than null, matching the owner's sentinel and for the same reason: a saved query
        // is unique by owner, source, author and name, and neither engine treats two NULLs as equal —
        // so an unattributed query would be the one case the uniqueness never checked.
        author = (author == null || author.isBlank()) ? QueryOwner.INSTALLATION_KEY : author;
    }

    /**
     * 🏗️ Start a draft against a source, for an owner.
     *
     * @param source which described source the jMQ names
     * @param owner  what holds the query
     * @return an empty draft
     */
    public static SavedQueryDraft on(String source, QueryOwner owner) {
        return new SavedQueryDraft(source, owner, null, null, null, null, false, 0);
    }

    /**
     * 📛 What a person calls it.
     *
     * @param name the name
     * @return a draft carrying it
     */
    public SavedQueryDraft named(String name) {
        return new SavedQueryDraft(source, owner, name, description, body, author, shared, sortOrder);
    }

    /**
     * 📝 A sentence about what it is for.
     *
     * @param description the sentence
     * @return a draft carrying it
     */
    public SavedQueryDraft describedAs(String description) {
        return new SavedQueryDraft(source, owner, name, description, body, author, shared, sortOrder);
    }

    /**
     * 📄 The jMQ itself, in either shape.
     *
     * @param body the query
     * @return a draft carrying it
     */
    public SavedQueryDraft writing(String body) {
        return new SavedQueryDraft(source, owner, name, description, body, author, shared, sortOrder);
    }

    /**
     * 👤 Whoever wrote it, as the product's own identifier for a person.
     *
     * @param author the person
     * @return a draft carrying it
     */
    public SavedQueryDraft by(String author) {
        return new SavedQueryDraft(source, owner, name, description, body, author, shared, sortOrder);
    }

    /**
     * 👀 Visible to everyone who can reach the owner.
     *
     * @return a draft everyone who can reach the owner will see
     */
    public SavedQueryDraft visibleToEveryone() {
        return new SavedQueryDraft(source, owner, name, description, body, author, true, sortOrder);
    }

    /**
     * ↕️ Where it sits in a list somebody arranged.
     *
     * @param sortOrder the position
     * @return a draft carrying it
     */
    public SavedQueryDraft at(int sortOrder) {
        return new SavedQueryDraft(source, owner, name, description, body, author, shared, sortOrder);
    }
}
