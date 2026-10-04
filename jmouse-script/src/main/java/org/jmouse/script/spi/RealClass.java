package org.jmouse.script.spi;

/**
 * The class a facade really is, behind whatever the container wrapped it in.
 *
 * <h2>⚠️ THIS EXISTS BECAUSE A CATALOGUE ONCE OFFERED {@code getCallbacks()} AS A RULE METHOD</h2>
 *
 * <p>A facade whose methods are advised — transactional, cached, secured — is handed back as a
 * <b>proxy</b>, and a proxy is handed back even from {@code return this}. Asking such an object for its
 * class and listing the public methods produces the proxy's plumbing: {@code Advised},
 * {@code getCallbacks}, {@code setTargetSource} and forty more, all presented to a rule author as things
 * they may call.</p>
 *
 * <p>Unwrapping it is container-specific — a name convention, a marker interface, a utility — so a
 * library cannot do it and must be told. A product with no proxies passes {@link #plain()}.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@FunctionalInterface
public interface RealClass {

    /**
     * The class whose methods a rule may actually call.
     *
     * @param target a facade's target, possibly proxied
     * @return the class it really is
     */
    Class<?> of(Object target);

    /**
     * For a product that wraps nothing.
     *
     * <p>⚠️ Correct only where no facade is advised. A product whose facades are transactional and which
     * uses this gets a catalogue full of proxy plumbing, and will see it the moment somebody opens the
     * editor's help panel.</p>
     *
     * @return the object's own class, always
     */
    static RealClass plain() {
        return Object::getClass;
    }

}
