package org.jmouse.ai.mcp.authorization.server;

import org.jmouse.ai.mcp.authorization.AuthorizationRoutes;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The one screen where a person decides whether a program may act for them, rendered once for everybody.
 *
 * <p>⚠️ <strong>One document, no assets and no framework</strong>, and that is what makes a shared screen
 * possible at all. The two products it replaces are built in different design systems with different
 * component libraries; a shared React page would have been a dependency each of them fought, and a
 * shared stylesheet would have been a second theme to keep in step. A single self-contained page owes
 * nothing to either and looks the same in both — which is the whole point, because what it says is a
 * security decision and the wording of it should not depend on which application somebody happened to
 * open.
 *
 * <p>⚠️ <strong>It validates nothing.</strong> Every parameter is vetted by the server on review
 * <em>and again</em> on approval, because the two are independent requests and only the second mints
 * anything. What this page is for is showing a person what they are agreeing to; a check written here
 * would be one anybody could skip by calling the endpoint directly.
 *
 * <p>Substitution is deliberately three placeholders and no template engine. What varies between two
 * installations is an application's name and two paths — a dependency on a rendering engine to say that
 * much would be the largest thing this module pulls in.
 */
public class ConsentPage {

    private static final String TEMPLATE = "consent.html";

    private final String rendered;

    public ConsentPage(McpAuthorizationProperties properties) {
        AuthorizationRoutes routes = properties.routes();

        this.rendered = substitute(template(), Map.of(
                "applicationName",   escapeHtml(properties.getApplicationName()),
                "reviewUrl",         escapeHtml(routes.review()),
                "approveUrl",        escapeHtml(routes.approval()),
                "signInUrl",         escapeHtml(properties.browserUrl(properties.getConsent().getSignInRoute())),
                "tokenStorageKey",   escapeJavaScript(properties.getConsent().getTokenStorageKey()),
                "tokenStorageField", escapeJavaScript(properties.getConsent().getTokenStorageField())));
    }

    /** The finished document. Built once at startup, because nothing in it varies per request. */
    public String render() {
        return rendered;
    }

    private static String template() {
        try (InputStream source = ConsentPage.class.getResourceAsStream(TEMPLATE)) {
            if (source == null) {
                throw new IllegalStateException(TEMPLATE + " is missing from this module's resources");
            }

            return new String(source.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException unreadable) {
            throw new UncheckedIOException("Could not read " + TEMPLATE, unreadable);
        }
    }

    private static String substitute(String template, Map<String, String> values) {
        String rendered = template;

        for (Map.Entry<String, String> value : values.entrySet()) {
            rendered = rendered.replace("{{" + value.getKey() + "}}", value.getValue());
        }

        return rendered;
    }

    /**
     * Escaped even though every value here comes from this installation's own configuration.
     *
     * <p>Not because a deployment is expected to attack itself, but because "this string was safe when I
     * wrote the page" is a property nobody re-checks when a new placeholder is added — and the cost of
     * being wrong is a screen a person is about to make a security decision on.
     */
    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }

    /** The same, for the two values that land inside a string literal in the page's script. */
    private static String escapeJavaScript(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("<", "\\u003C")
                    .replace("\n", "")
                    .replace("\r", "");
    }
}
