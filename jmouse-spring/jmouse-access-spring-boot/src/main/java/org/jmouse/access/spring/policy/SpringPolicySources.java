package org.jmouse.access.spring.policy;

import org.jmouse.access.el.loader.PolicySource;
import org.jmouse.access.el.loader.PolicySources;
import org.jmouse.access.policy.PolicyException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Policy files as Spring resources — {@code classpath:}, {@code file:}, and the patterns that go with
 * them.
 *
 * <p>Everything about composition is {@link org.jmouse.access.el.loader.PolicyLoader}'s; this only
 * answers where the characters are.
 */
public class SpringPolicySources implements PolicySources {

    private final ResourcePatternResolver resources;

    public SpringPolicySources(ResourcePatternResolver resources) {
        this.resources = resources;
    }

    /**
     * Every file a location names, in a stable order.
     *
     * <p>⚠️ Sorted rather than left as the classpath scan produced it. A pattern's matches come back
     * in whatever order the filesystem or the jar index happened to hold them, and a load order that
     * changed between restarts would make a diff of the rendered policy meaningless — which is most of
     * what writing authorization down is for.
     */
    @Override
    public List<PolicySource> at(String location) {
        try {
            List<PolicySource> found = new ArrayList<>();

            for (Resource resource : resources.getResources(location)) {
                if (resource.exists()) {
                    found.add(read(resource));
                }
            }

            found.sort(Comparator.comparing(PolicySource::location));

            return found;
        } catch (IOException unreadable) {
            throw new PolicyException(
                    "The policy location '" + location + "' could not be read: "
                    + unreadable.getMessage(), unreadable);
        }
    }

    /**
     * The file an include names, resolved beside the file that wrote it.
     *
     * <p>The including file's location is a resolved URI, so this reaches a sibling inside a jar the
     * same way it reaches one on disk.
     */
    @Override
    public Optional<PolicySource> included(String path, PolicySource from) {
        try {
            Resource resource = new UrlResource(URI.create(from.location())).createRelative(path);

            return resource.exists() ? Optional.of(read(resource)) : Optional.empty();
        } catch (IOException | IllegalArgumentException unreachable) {
            return Optional.empty();
        }
    }

    /**
     * Reads one resource, naming it by its resolved location.
     *
     * <p>⚠️ The location is the resource's <strong>URI</strong> and not the string somebody
     * configured. Two configured locations can spell one file — {@code classpath:policy/roles.jmp} and
     * a {@code classpath:policy/*.jmp} that matches it — and the loader detects a repeat and a cycle
     * by comparing locations. Comparing what was typed would read the same file twice and never
     * notice a loop.
     */
    private PolicySource read(Resource resource) throws IOException {
        return PolicySource.at(
                resource.getURI().toString(),
                resource.getContentAsString(StandardCharsets.UTF_8));
    }
}
