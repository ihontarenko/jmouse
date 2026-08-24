package org.jmouse.query.spring.builder;

import java.util.Map;
import java.util.Optional;

/**
 * What arrived with a builder request — everything a subject needs to answer, and nothing else.
 *
 * <h2>⚠️ Parameters are a map, and that is not laziness</h2>
 *
 * <p>One subject narrows by a form, another by a project, a third by nothing at all. Typing that into
 * the shared surface would mean the library knowing what a form is — and then knowing what a project is,
 * and then the next one. A subject reads what it needs and ignores the rest.</p>
 *
 * <h2>⚠️ The caller is an identifier, never an account</h2>
 *
 * <p>It is here so a subject can say what {@code currentMember} means. A library holding a product's
 * account type would make every product's account the same type, which is exactly the coupling this
 * whole module exists without.</p>
 *
 * @param subject   which listing
 * @param caller    who is asking, or {@code null} where nobody is
 * @param parameters whatever the request carried
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record QueryRequest(String subject, String caller, Map<String, String> parameters) {

    public QueryRequest {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    /** One parameter, if it arrived and is not blank. */
    public Optional<String> parameter(String name) {
        String value = parameters.get(name);

        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }
}
