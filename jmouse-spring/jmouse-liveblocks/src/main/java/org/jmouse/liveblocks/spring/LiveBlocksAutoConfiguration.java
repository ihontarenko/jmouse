package org.jmouse.liveblocks.spring;

import org.jmouse.liveblocks.DirectiveResolution;
import org.jmouse.liveblocks.DirectiveResolver;
import org.jmouse.liveblocks.web.DirectiveResolveController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * The route, mounted only when a product says so.
 *
 * <h2>⚠️ Present is not the same as switched on</h2>
 *
 * <p>{@code jmouse.liveblocks.enabled} is not ceremony. This endpoint answers questions about this
 * product's rows to a browser on <em>another</em> origin, and ⚠️ <strong>nothing in this module guards
 * it</strong> — the product mounts it behind its own authentication, its own audience check and its own
 * CORS allowlist, because a library cannot know what any of those are. Publishing it on the strength of
 * a jar being on the classpath would open it to anyone who can reach the application.
 *
 * <p>The controller is registered as a bean here rather than component-scanned, which is what lets the
 * module sit on a classpath and contribute nothing. A {@code @ComponentScan} over somebody else's
 * package is also how a starter quietly acquires an opinion about a product's bean naming.
 *
 * <p>⚠️ <strong>A product with the switch on and no resolvers is legal</strong> and answers
 * {@code UNKNOWN_DIRECTIVE} to everything — which is the correct answer from a product nobody quotes
 * yet, and a far better failure than a bean that refuses to start.
 */
@AutoConfiguration
@ConditionalOnClass(DirectiveResolveController.class)
@ConditionalOnProperty(name = "jmouse.liveblocks.enabled", havingValue = "true")
public class LiveBlocksAutoConfiguration {

    /**
     * Whatever the product registered, indexed by directive name.
     *
     * <p>The resolvers arrive as an injected list, so adding a directive is a bean and nothing else —
     * no registry to edit, no enum to extend, and no place a new one can be forgotten.
     */
    @Bean
    @ConditionalOnMissingBean
    public DirectiveResolution directiveResolution(List<DirectiveResolver> resolvers) {
        return new DirectiveResolution(resolvers);
    }

    @Bean
    @ConditionalOnMissingBean
    public DirectiveResolveController directiveResolveController(DirectiveResolution resolution) {
        return new DirectiveResolveController(resolution);
    }

}
