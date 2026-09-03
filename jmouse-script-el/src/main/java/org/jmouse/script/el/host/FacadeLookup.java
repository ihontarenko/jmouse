package org.jmouse.script.el.host;

import org.jmouse.core.context.beans.BeanLookup;

/**
 * Resolves {@code @name} against a {@link ScriptCatalogue}, and against nothing else.
 *
 * <h2>⚠️ This class is the security boundary, and it is deliberately dull</h2>
 *
 * <p>{@link org.jmouse.el.node.expression.BeanAccessNode} asks whatever {@code BeanLookup} the
 * evaluation context carries for a bean by name. Wiring that to an application's
 * {@code BeanContext} — which is the obvious thing to do, and works immediately — hands every script
 * in the installation every bean in the container, repository and transaction manager included. There
 * is no syntax to add and no error to see; the language simply becomes a remote shell.</p>
 *
 * <p>So: a map lookup, a refusal, and no fallback. In particular</p>
 *
 * <ul>
 *   <li>a name the catalogue does not hold is <strong>refused</strong>, never resolved elsewhere;</li>
 *   <li>{@link #getBean(Class)} — resolution by <em>type</em> — is refused outright, because a script
 *       writes names and a type-based lookup could only have come from somewhere a script cannot see;</li>
 *   <li>nothing here reads a container, a service loader, or the class path.</li>
 * </ul>
 *
 * <p>An unknown name should never reach here in the first place: the binder refuses it at load, with a
 * line and a column. This is the second lock, for a context assembled by hand and for the day somebody
 * evaluates a tree that skipped the binder.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class FacadeLookup implements BeanLookup {

    private final ScriptCatalogue catalogue;

    /**
     * Constructs a lookup over one host's catalogue.
     *
     * @param catalogue what the host declared
     */
    public FacadeLookup(ScriptCatalogue catalogue) {
        this.catalogue = catalogue;
    }

    /**
     * ⚠️ Always refuses. A script names facades; it cannot name types, so nothing legitimate asks this.
     *
     * @param beanClass the type asked for
     * @param <T>       the type asked for
     * @return never
     * @throws ScriptAccessException always
     */
    @Override
    public <T> T getBean(Class<T> beanClass) {
        throw new ScriptAccessException(
                "a script resolves facades by name, never by type; nothing in a '.jms' file can ask for '%s'"
                        .formatted(beanClass == null ? "null" : beanClass.getName()));
    }

    /**
     * Resolves a declared facade.
     *
     * @param beanName  the name written after the {@code @}
     * @param beanClass ignored — the catalogue decides what the name is
     * @param <T>       the expected type
     * @return the facade the host declared under that name
     * @throws ScriptAccessException when the catalogue declares no such facade
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String beanName, Class<T> beanClass) {
        Object facade = catalogue.facade(beanName);

        if (facade == null) {
            throw new ScriptAccessException(
                    "'@%s' is not a facade this host declared; it offers %s"
                            .formatted(beanName, catalogue.facadeNames()));
        }

        return (T) facade;
    }

}
