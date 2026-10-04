package org.jmouse.script.spi;

/**
 * Something a rule may reach, contributed by whoever owns it.
 *
 * <h2>⚠️ THE CATALOGUE IS THE SECURITY BOUNDARY</h2>
 *
 * <p>{@code .jmp} solves the same problem the other way round — it removes {@code @bean.method} from
 * the language, so a policy cannot call {@code @accountRepository.deleteAll()}. A script exists in order
 * to call the host, so the syntax stays and the <strong>set of names is closed</strong> instead. There
 * is no fallback to an application context anywhere in this library, and that is written down as a rule
 * rather than left as an implementation detail.</p>
 *
 * <h2>⚠️ WHAT A FACADE IS NOT</h2>
 *
 * <p>It is not a repository and it is not an entity. Whatever {@link #target()} returns is <em>the whole
 * of what a rule can reach through this name</em> — every public method on it is callable, and the
 * person who wrote the rule signed nothing. A facade is therefore a small class written for this
 * purpose. Hand it a repository and a rule can empty the store; hand it a domain entity and a rule can
 * walk from it into every row the model can reach.</p>
 *
 * <p>⚠️ The corollary, and the reason a facade must be its own class: <strong>adding a method to one
 * publishes it.</strong> That is the review this seam needs.</p>
 *
 * <h2>⚠️ AND NO FACADE THAT WRITES MAY USE "THE CURRENT SUBJECT"</h2>
 *
 * <p>Background work runs on an executor, outside any request. There is no security context there — so a
 * facade reaching for the current subject finds nothing, and the honest answer to "on whose authority
 * does this rule act" is <em>the installation's</em>, which is the widest there is. A facade that writes
 * must take its subject as an argument and go through {@code jmouse-access} with it, never resolve one
 * for itself.</p>
 *
 * <p>⚠️ A stage that runs ON a request inverts this and makes it sharper: there the subject is a real
 * person, and a facade that can read more than that person may see turns a script into a way of asking
 * about rows they were refused.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ScriptFacade {

    /**
     * What a rule writes after the {@code @}.
     *
     * <p>⚠️ Two beans answering one name is a configuration mistake rather than an override, and an
     * installation should refuse to start on it — a {@code @catalogue} that silently became somebody
     * else's is a set of rules that quietly mean something new.</p>
     *
     * @return the facade name, without the {@code @}
     */
    String name();

    /**
     * The object a rule's calls land on.
     *
     * @return the target; every public method on it is reachable
     */
    Object target();

    /**
     * A phrase for the editor's completion list — what this is for, in a few words.
     *
     * @return a short description, or {@code null}
     */
    default String detail() {
        return null;
    }

}
