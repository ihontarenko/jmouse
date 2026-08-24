package org.jmouse.query.spring.builder;

/**
 * Who is asking — the one thing this module cannot work out for itself.
 *
 * <p>⚠️ An <strong>identifier</strong>, never an account. It exists so a subject can say what
 * {@code currentMember} means, and a library holding a product's account type would make every product's
 * account the same type — which is precisely the coupling this whole module exists without.</p>
 *
 * <p>A product supplies one bean reading its own security context. One that supplies none gets
 * {@link #ANONYMOUS}, and a query naming {@code currentMember} is then refused by the checker rather
 * than quietly answering about nobody.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@FunctionalInterface
public interface QueryCallers {

    /** ⚠️ Nobody, said out loud. See the class note for why this is not the same as guessing. */
    QueryCallers ANONYMOUS = () -> null;

    /**
     * @return the caller's identifier, or {@code null} where nobody is signed in
     */
    String current();
}
