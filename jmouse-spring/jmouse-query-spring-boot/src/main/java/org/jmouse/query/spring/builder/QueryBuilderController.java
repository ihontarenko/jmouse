package org.jmouse.query.spring.builder;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * The two addresses a filter builder talks to, for every subject in the product.
 *
 * <h2>⚠️ Carries no authorization annotation, and that is deliberate</h2>
 *
 * <p>The gate is the product's — it is the product that knows a workspace, a permission and a module.
 * A product mounting this behind nothing has published its schema and its checker to anybody; the
 * vocabulary of a form is not a secret, but who may see which form is very much one, and only the
 * product can answer it.</p>
 *
 * <h2>⚠️ One address family, not one per listing</h2>
 *
 * <p>{@code /entries} and {@code /assets} differ by a path segment and nothing else, so a screen serving
 * both is one screen with the segment as data. Two hand-written controllers at two shapes is how the
 * same panel ends up drawn twice and offering different operators by the end of the month.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@RestController
@RequestMapping(QueryRoutes.PREFIX)
public class QueryBuilderController {

    private final QueryBuilders builders;
    private final QueryCallers  callers;

    public QueryBuilderController(QueryBuilders builders, QueryCallers callers) {
        this.builders = builders;
        this.callers = callers;
    }

    /** What a query about this subject may name, and the comparisons a row may use. */
    @GetMapping("/{subject}/schema")
    public QueryViews.Vocabulary schema(
            @PathVariable String subject, @RequestParam Map<String, String> parameters) {

        return builders.describe(request(subject, parameters));
    }

    /**
     * Rows in, jMQ out — or jMQ in, rows out. One call, because a screen needs the text, the rows and
     * the verdict together and they must agree.
     */
    @PostMapping("/{subject}/translate")
    public QueryViews.Translated translate(
            @PathVariable String subject,
            @RequestParam Map<String, String> parameters,
            @RequestBody QueryViews.Translation translation) {

        return builders.translate(request(subject, parameters), translation);
    }

    private QueryRequest request(String subject, Map<String, String> parameters) {
        return new QueryRequest(subject, callers.current(), parameters);
    }
}
