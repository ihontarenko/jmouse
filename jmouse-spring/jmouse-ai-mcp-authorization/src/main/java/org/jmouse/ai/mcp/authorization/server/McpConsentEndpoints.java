package org.jmouse.ai.mcp.authorization.server;

import org.jmouse.ai.mcp.authorization.AuthorizationVocabulary;
import org.jmouse.ai.mcp.authorization.McpAuthorizationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The half of the flow a person drives: seeing what a client is asking for, and granting it.
 *
 * <p>⚠️ <strong>The screen itself is served here rather than by either product's own interface</strong>,
 * and that is the point of this class. Two applications used to ask the same question in two design
 * systems with two vocabularies, and the parts that mattered — where a live authorization code is about
 * to be sent, and what the client would then be able to do — were worded differently in each. One page
 * means one wording, reviewed once.
 *
 * <p>⚠️ <strong>The page is public; the two endpoints behind it are not.</strong> The page grants
 * nothing and shows nothing until it has read a token, so a product opens {@code GET} on it and leaves
 * {@code review} and {@code approve} behind whatever it requires of a signed-in caller. Getting that
 * backwards in either direction is visible immediately: a closed page answers a browser with a refusal
 * nobody can read, and open endpoints would let anything mint a code.
 *
 * <p>⚠️ <strong>Approving navigates the browser out of the product.</strong> The code has to arrive at
 * the client's own listener, so the approval's answer is a URL the page assigns rather than a redirect
 * performed here — a 302 inside an authenticated fetch is followed by the browser's own machinery and
 * the client waits forever.
 */
@RestController
public class McpConsentEndpoints {

    private final ClientAuthorizationFlow flow;
    private final ConsentPage             consentPage;

    public McpConsentEndpoints(ClientAuthorizationFlow flow, ConsentPage consentPage) {
        this.flow        = flow;
        this.consentPage = consentPage;
    }

    /** The screen a person actually sees. Self-contained: one document, no assets, no framework. */
    @GetMapping(value = McpAuthorizationProperties.CONSENT_PREFIX_EXPRESSION + McpAuthorizationProperties.Consent.PATH,
                produces = MediaType.TEXT_HTML_VALUE)
    public String consent() {
        return consentPage.render();
    }

    /** What the approval screen shows, with everything refusable already refused. */
    @PostMapping(McpAuthorizationProperties.CONSENT_PREFIX_EXPRESSION + "/review")
    public AuthorizationDtos.ClientReviewResponse review(
            @RequestBody AuthorizationDtos.ClientReviewRequest request) {
        return flow.review(request);
    }

    /** The approval, and the address the browser is then sent to. */
    @PostMapping(McpAuthorizationProperties.CONSENT_PREFIX_EXPRESSION + "/approve")
    public AuthorizationDtos.ClientApprovalResponse approve(
            @RequestBody AuthorizationDtos.ClientApprovalRequest request) {
        return flow.approve(request);
    }

    /**
     * ⚠️ Answered in the RFC's shape even though a browser is reading it, because the page passes the
     * server's own sentence straight through — every refusal in this flow names what would have been
     * accepted, which is strictly more useful than anything a screen could invent.
     */
    @ExceptionHandler(McpAuthorizationException.class)
    public ResponseEntity<AuthorizationDtos.ErrorResponse> onRefusedRequest(McpAuthorizationException refusal) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthorizationDtos.ErrorResponse(
                AuthorizationVocabulary.ERROR_INVALID_REQUEST, refusal.getMessage()));
    }
}
