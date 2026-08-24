package org.jmouse.query.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * jMQ on a repository method — the face JPQL has, over a language a person can also type into a URL.
 *
 * <pre>{@code
 * public interface DeliveryQueries {
 *
 *     @JmQuery(source = "deliveries",
 *              value = "where delivery[carrier] == carrier and delivery[weight] > minWeight")
 *     List<Delivery> heavyFor(@Parameter("carrier") String carrier,
 *                             @Parameter("minWeight") double minWeight);
 * }
 * }</pre>
 *
 * <h2>⚠️ Compiled at STARTUP, not on the first call</h2>
 *
 * <p>Every annotated method is parsed and checked against its source when the repository is created. A
 * misspelled attribute therefore fails the boot, naming the method and the attribute — rather than
 * failing the first client to reach that endpoint, weeks later, as a 500 nobody can act on. It is the
 * same reasoning that refuses a saved view at save time: the refusal has to land where somebody can
 * still fix it.</p>
 *
 * <h2>⚠️ Values are BOUND, never spliced</h2>
 *
 * <p>An argument reaches the query as a named value and leaves as a {@code ?}. A carrier called
 * {@code '; DROP TABLE deliveries; --} is a carrier nobody has, not an incident — and it is the same
 * mechanism a screen uses for {@code currentMember}, not a second one written for annotations.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JmQuery {

    /**
     * The query — either a bare condition ({@code where …} optional) or a whole {@code view} block.
     *
     * <p>⚠️ Which of the two it is is not declared, and must not be: the text already says it, and a
     * second statement of the same fact would disagree the first time a filter grew into a view.</p>
     */
    String value();

    /**
     * Which declared source it is about — {@code deliveries}, {@code issues}.
     *
     * <p>Optional for a {@code view} block, which names its own target after {@code on}; required for a
     * bare condition, which cannot.</p>
     */
    String source() default "";

    /**
     * What to select when the query is a bare condition.
     *
     * <p>Defaults to every column of the source's own table. A {@code view} block's {@code columns}
     * clause wins over this.</p>
     */
    String select() default "*";
}
