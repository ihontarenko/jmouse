package org.jmouse.query.store;

import org.jmouse.query.el.QueryLanguage;

/**
 * 📐 The two shapes a saved query comes in.
 *
 * <p>Both are jMQ, read by one parser. The difference is how much of a query the text carries, and it
 * decides only one thing: which entry point runs it.</p>
 *
 * <pre>
 *   FILTER     issue.status == 'open' and issue.assignee == currentUser()
 *
 *   DOCUMENT   view "Overdue" on issues {
 *                where   issue.dueDate &lt; now()
 *                order   issue.priority desc
 *                columns key, summary, assignee
 *              }
 * </pre>
 *
 * <h2>⚠️ Derived, never stored</h2>
 *
 * <p>A column recording the form would be a second statement of something the text already says, and
 * the two would part company the first time somebody grew a filter into a view. The same reasoning
 * keeps a query's projection off the table it is saved in.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum QueryForm {

    /** One condition, of the kind that sits in a text box above a list. */
    FILTER,

    /** A whole declaration — a view with its clauses, or a file of reusable functions. */
    DOCUMENT;

    /**
     * 🔎 Which shape this text is.
     *
     * @param language the language that would have to read it
     * @param body     the saved text
     * @return the form
     */
    public static QueryForm of(QueryLanguage language, String body) {
        return language.isDocument(body) ? DOCUMENT : FILTER;
    }

    /**
     * Whether this is a whole declaration.
     *
     * @return {@code true} for {@link #DOCUMENT}
     */
    public boolean isDocument() {
        return this == DOCUMENT;
    }
}
