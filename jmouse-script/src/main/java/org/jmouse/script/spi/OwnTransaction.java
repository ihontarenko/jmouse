package org.jmouse.script.spi;

/**
 * Runs a piece of work in a transaction <b>of its own</b>, joining nothing.
 *
 * <h2>⚠️ THIS EXISTS BECAUSE OF A BUG THAT LOOKED LIKE SUCCESS</h2>
 *
 * <p>An observation stage runs AFTER the thing it observes has been committed. At that point the
 * finished transaction's resources may still be bound to the thread — so a facade that writes will
 * <em>join</em> that transaction, and it is never committed again. The write reports success, the rule
 * logs that it happened, and <b>nothing is in the database.</b></p>
 *
 * <p>That is not hypothetical: it is how a rename a rule plainly made, and a filing a facade plainly
 * did, both vanished with no error anywhere. A transaction of its own is what makes an observation
 * rule's write actually commit.</p>
 *
 * <h2>⚠️ "NEW" IS THE WHOLE CONTRACT</h2>
 *
 * <p>An implementation that joins an existing transaction satisfies the signature and reintroduces the
 * bug. Whatever a product uses — a transaction template set to require a new one, a unit of work, a
 * fresh session — it must be one that commits on its own.</p>
 *
 * <h2>⚠️ AND IT IS AN INTERFACE SO THAT THIS LIBRARY CARRIES NO TRANSACTION MANAGER</h2>
 *
 * <p>The mechanism was built on a container's transaction manager where it came from. Taking that
 * dependency here would put a framework into every product that wants stages — including one that has
 * no database at all. This is one line for a product to supply.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@FunctionalInterface
public interface OwnTransaction {

    /**
     * Runs the work, committing it independently of whatever the caller was in.
     *
     * @param work what to run
     */
    void run(Runnable work);

    /**
     * For a product with nothing to commit — the work simply runs.
     *
     * <p>⚠️ Correct only where no rule writes. A product whose facades write and which uses this has
     * the vanishing-write bug described above, and will not see it.</p>
     *
     * @return a transaction that is not one
     */
    static OwnTransaction none() {
        return Runnable::run;
    }

}
