package org.jmouse.liveblocks.web;

import org.jmouse.liveblocks.Directive;
import org.jmouse.liveblocks.DirectiveResolution;
import org.jmouse.liveblocks.ResolvedDirective;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <strong>{@code POST /api/blocks/resolve} — the one address every product that can be quoted serves.</strong>
 *
 * <p>A consumer holds a {@code namespace → url} map and nothing else; it posts a document's directives
 * to whichever product owns them, carrying the reader's own token. This is the far end of that call, and
 * <strong>the path is the contract</strong>: a product that serves it under a different address is a
 * product no consumer can be pointed at without a code change somewhere.
 *
 * <h2>⚠️ It is not scoped to a page, and that is the change from where this came from</h2>
 *
 * <p>The shape this replaces was {@code POST …/pages/{pageId}/blocks} — the document was the unit, and a
 * page-read permission gated the call. Neither survives contact with a document served by a
 * <em>different product</em>: this product has never heard of that page and cannot check anything about
 * it. What replaces the gate is stronger rather than weaker — <strong>each resolver narrows its own
 * answer to the caller</strong>, so a directive can reveal nothing the product's ordinary API would not
 * tell the same person.
 *
 * <h2>⚠️ This class gates nothing, and the product must</h2>
 *
 * <p>Exactly as {@code jmouse-ai-management} states of its own controllers: a library cannot know what a
 * product's permission model is, so it mounts a route and stops. The product puts it behind
 * authentication — and ⚠️ <strong>behind CORS that names the consumer's origin, and an audience check
 * that accepts the consumer's token</strong>, both of which are the two things that make this a
 * cross-product call rather than a local one.
 */
@RestController
@RequestMapping(LiveBlockRoutes.PREFIX)
public class DirectiveResolveController {

    /**
     * ⚠️ A backstop rather than a policy. It is not about load — it is that a request claiming two
     * hundred directives is not a page being read.
     */
    public static final int MAXIMUM_DIRECTIVES = 100;

    private final DirectiveResolution resolution;

    public DirectiveResolveController(DirectiveResolution resolution) {
        this.resolution = resolution;
    }

    /**
     * Answer a document's directives, in the order they were asked.
     *
     * <p>⚠️ <strong>Always 200 with a body, unless the batch itself is malformed.</strong> Every way a
     * single block can fail is a status inside the list — see {@code DirectiveStatus} — because a page
     * losing one embed must not look to the consumer like the product being down.
     */
    @PostMapping("/resolve")
    public ResponseEntity<List<ResolvedDirective>> resolve(@RequestBody ResolveDirectivesRequest request) {
        List<Directive> asked = request.asked();

        if (asked.size() > MAXIMUM_DIRECTIVES) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(resolution.resolve(asked));
    }

    /**
     * What this product answers.
     *
     * <p>Not needed to render anything — it is for whoever is registering the namespace, who otherwise
     * finds out which words work by writing them on a page and watching them fail.
     */
    @GetMapping("/answers")
    public List<String> answers() {
        return List.copyOf(resolution.answers());
    }

}
