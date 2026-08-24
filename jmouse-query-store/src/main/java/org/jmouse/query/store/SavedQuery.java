package org.jmouse.query.store;

import java.time.LocalDateTime;

/**
 * 💾 A query somebody kept: a name, where it belongs, and the jMQ behind it.
 *
 * <h2>An interface rather than a record, because the row is the caller's business</h2>
 *
 * <p>A store backed by persistence hands back something managed, whose changes are written when the
 * caller's transaction ends. A store backed by a map hands back a value. Both are "a saved query" to
 * everything that reads one, and neither should have to be copied into the other's shape to be read.</p>
 *
 * <h2>⚠️ The body is text, and stays text</h2>
 *
 * <p>Not a parse tree, not a column per clause. jMQ is a language somebody writes and reads back — a
 * table of clauses could not hold a condition it was not designed for, and would have to grow a column
 * every time the grammar did. Text also diffs, which is what makes a change to a shared view reviewable.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface SavedQuery {

    /** 🔑 What this query is called by machines. */
    String getIdentifier();

    /**
     * 🎯 Which described source the jMQ is written against — {@code issues}, {@code inventory}.
     *
     * <p>⚠️ This is the whole of what makes one store serve every product. The engine resolves the name
     * to that product's own target, schema and mapping; nothing about a saved row knows which tables it
     * will reach, or that tables exist.</p>
     */
    String getSource();

    /** 🏷️ What holds it — a board, a workspace, a person, the installation. */
    QueryOwner getOwner();

    /** 📛 What a person calls it. */
    String getName();

    /** 📝 A sentence about what it is for, or {@code null}. */
    String getDescription();

    /** 📄 The jMQ itself. */
    String getBody();

    /**
     * 👤 The product's own identifier for whoever wrote it.
     *
     * <p>⚠️ Never {@code null}: {@code "*"} stands for "nobody in particular", the same sentinel the
     * owner uses. A saved query is unique by owner, source, author and name, and neither engine treats
     * two {@code NULL}s as equal — so unattributed queries would be the one case the uniqueness never
     * checked.</p>
     */
    String getAuthor();

    /** 👀 Whether anyone who can reach the owner sees it, or only its author. */
    boolean isShared();

    /** ↕️ Where it sits in a list somebody arranged. */
    int getSortOrder();

    /** 🕒 When it was first saved. */
    LocalDateTime getCreatedAt();

    /** 🕒 When it last changed. */
    LocalDateTime getUpdatedAt();
}
