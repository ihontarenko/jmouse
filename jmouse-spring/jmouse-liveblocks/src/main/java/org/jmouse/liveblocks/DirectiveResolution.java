package org.jmouse.liveblocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every directive a document contains, answered in one pass.
 *
 * <h2>⚠️ One bad answer may not cost the page</h2>
 *
 * <p>This runs on the render path of somebody else's document, and the failure it is written against is
 * a resolver throwing on one row out of twenty. So every call is guarded and an escape becomes
 * {@link DirectiveStatus#NOT_FOUND} — a visible notice on that one block — rather than a 500 that turns
 * a page into an error screen because a single issue was deleted.
 *
 * <p>⚠️ It logs at <em>warn</em> when that happens. A resolver throwing is a defect in the resolver, and
 * a status that quietly looks like ordinary absence would hide it forever.
 *
 * <h2>⚠️ The answer is in the request's order, and every ask gets exactly one</h2>
 *
 * <p>A consumer matches a batch by name and argument, but it also renders in document order — so the
 * list comes back one-for-one, including the unaskable lines. Dropping a malformed directive would make
 * a page silently lose the notice explaining why it is malformed.
 */
public class DirectiveResolution {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectiveResolution.class);

    private final Map<String, DirectiveResolver> byName;

    public DirectiveResolution(List<DirectiveResolver> resolvers) {
        Map<String, DirectiveResolver> registered = new LinkedHashMap<>();

        for (DirectiveResolver resolver : resolvers) {
            String directive = resolver.directive().trim().toLowerCase();
            DirectiveResolver alreadyRegistered = registered.put(directive, resolver);

            // ⚠️ Two resolvers for one name is two answers to the same line, decided by bean order —
            // which is the kind of thing that works for a year and then changes on an unrelated commit.
            if (alreadyRegistered != null) {
                throw new IllegalStateException(
                        "Two resolvers claim the directive '" + directive + "': "
                        + alreadyRegistered.getClass().getName() + " and " + resolver.getClass().getName()
                        + ". A directive names exactly one answer.");
            }
        }

        this.byName = Map.copyOf(registered);
    }

    /** The directive names this product answers — what an administrator registers a namespace for. */
    public Set<String> answers() {
        return byName.keySet();
    }

    /**
     * Answer them all.
     *
     * <p>⚠️ Nothing here is de-duplicated. A page quoting the same issue twice asks twice, and the
     * caching that makes that cheap belongs in the consumer, which is the only side that knows a
     * document is being re-rendered rather than re-read.
     */
    public List<ResolvedDirective> resolve(List<Directive> directives) {
        if (directives == null || directives.isEmpty()) {
            return List.of();
        }

        return directives.stream().map(this::resolveOne).toList();
    }

    private ResolvedDirective resolveOne(Directive directive) {
        if (directive == null || !directive.isAskable()) {
            return ResolvedDirective.miss(
                    directive == null ? new Directive("", "") : directive,
                    DirectiveStatus.UNKNOWN_DIRECTIVE);
        }

        DirectiveResolver resolver = byName.get(directive.normalisedName());

        if (resolver == null) {
            return ResolvedDirective.miss(directive, DirectiveStatus.UNKNOWN_DIRECTIVE);
        }

        try {
            ResolvedDirective resolved = resolver.resolve(directive);

            // A resolver answering null is a defect too, and one that would otherwise reach the client
            // as a hole in an array where a notice should be.
            return resolved == null
                    ? ResolvedDirective.miss(directive, DirectiveStatus.NOT_FOUND)
                    : resolved;
        } catch (RuntimeException failure) {
            LOGGER.warn(
                    "Resolver {} threw on ':::{} {}' — answering NOT_FOUND so the page still renders",
                    resolver.getClass().getName(), directive.name(), directive.argument(), failure);

            return ResolvedDirective.miss(directive, DirectiveStatus.NOT_FOUND);
        }
    }

}
