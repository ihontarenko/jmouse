package org.jmouse.ai.mcp.authorization.server;

import org.jmouse.ai.mcp.authorization.AuthorizationRoutes;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.template.Content;
import org.jmouse.el.template.Renderer;
import org.jmouse.el.template.Template;
import org.jmouse.el.template.TemplateEngine;
import org.jmouse.el.template.TemplateRenderer;
import org.jmouse.el.template.loader.ClasspathLoader;

/**
 * The one screen where a person decides whether a program may act for them, rendered once for everybody.
 *
 * <p>⚠️ <strong>One document, no assets and no framework</strong>, and that is what makes a shared screen
 * possible at all. The two products it replaces are built in different design systems with different
 * component libraries; a shared React page would have been a dependency each of them fought, and a shared
 * stylesheet would have been a second theme to keep in step. A single self-contained page owes nothing to
 * either and looks the same in both — which is the point, because what it says is a security decision and
 * the wording of it should not depend on which application somebody happened to open.
 *
 * <p>⚠️ <strong>It validates nothing.</strong> Every parameter is vetted by the server on review
 * <em>and again</em> on approval, because the two are independent requests and only the second mints
 * anything. What this page is for is showing a person what they are agreeing to; a check written here
 * would be one anybody could skip by calling the endpoint directly.
 *
 * <h2>Rendered by jMouse Template, and rendered once</h2>
 *
 * <p>The engine directly rather than {@code jmouse-jmt-spring-boot}: that module is a Spring MVC
 * {@code ViewResolver}, which resolves a view <em>name</em> out of one classpath prefix the product
 * configures — and a template this library owns cannot live under somebody else's {@code templates/}.
 *
 * <p>⚠️ <strong>At construction, not per request</strong>, and that is deliberate twice over. Nothing in
 * the page varies between requests, so a render per visitor would be work for nothing. And it puts a
 * template that will not parse at <strong>startup</strong>, where somebody is watching, rather than in
 * front of the first person who tries to connect a client — which is the failure that would matter, since
 * a page that will not render is a client that cannot be approved.
 *
 * <p>The template is kept deliberately dull for the same reason: seven substitutions, no conditionals, no
 * loops, no inheritance. What varies between two installations is a name and a few paths, and the picker —
 * the only part with real structure — is built in the browser from what {@code review} answers, because it
 * depends on who is signed in and this page is rendered before anybody is.
 */
public class ConsentPage {

    /** Where this module keeps its own templates, which is beside the class that loads them. */
    private static final String TEMPLATE_PREFIX = "org/jmouse/ai/mcp/authorization/server/";

    /** The house suffix for a jMouse Template. */
    private static final String TEMPLATE_SUFFIX = ".j.html";

    private static final String TEMPLATE = "consent";

    /**
     * ⚠️ <strong>Held, not built per render — the parse cache lives inside the engine.</strong>
     *
     * <p>{@code TemplateEngine} keeps its compiled templates in an instance field, so a fresh engine is a
     * cold cache and every template it is asked for is tokenized and parsed again. This one renders once
     * today, which makes the distinction free; keeping the engine is what stops it costing something the
     * day anything here needs rendering per request.
     */
    private final TemplateEngine engine;

    private final String rendered;

    public ConsentPage(McpAuthorizationProperties properties) {
        this.engine   = engine();
        this.rendered = render(properties);
    }

    /** The finished document. Built once at startup, because nothing in it varies per request. */
    public String render() {
        return rendered;
    }

    private String render(McpAuthorizationProperties properties) {
        AuthorizationRoutes                routes  = properties.routes();
        McpAuthorizationProperties.Consent consent = properties.getConsent();

        Template          template = engine.getTemplate(TEMPLATE);
        EvaluationContext context  = template.newContext();

        context.setValue("applicationName",   escapeHtml(properties.getApplicationName()));
        context.setValue("reviewUrl",         escapeHtml(routes.review()));
        context.setValue("approveUrl",        escapeHtml(routes.approval()));
        context.setValue("signInUrl",         escapeHtml(properties.browserUrl(consent.getSignInRoute())));
        context.setValue("tokenStorageKey",   escapeJavaScript(consent.getTokenStorageKey()));
        context.setValue("tokenStorageField", escapeJavaScript(consent.getTokenStorageField()));

        Renderer renderer = new TemplateRenderer(engine);
        Content  content  = renderer.render(template, context);

        return new String(content.getDataArray());
    }

    /**
     * A loader of this module's own, pointed at this module's own package.
     *
     * <p>Not the engine a product may already have as a bean: that one loads from wherever the product
     * keeps <em>its</em> views, and would answer "no such template" here — or, worse, find a different
     * file of the same name.
     */
    private static TemplateEngine engine() {
        ClasspathLoader loader = new ClasspathLoader(ConsentPage.class.getClassLoader());

        loader.setPrefix(TEMPLATE_PREFIX);
        loader.setSuffix(TEMPLATE_SUFFIX);

        TemplateEngine engine = new TemplateEngine();
        engine.setLoader(loader);

        return engine;
    }

    /**
     * Escaped on the way in, rather than trusted to the engine.
     *
     * <p>Every value here comes from this installation's own configuration, so this is not about a
     * deployment attacking itself. It is that "these strings were safe when I wrote the page" is a
     * property nobody re-checks when a placeholder is added, and the cost of being wrong is a screen
     * somebody is about to make a security decision on. Escaping at the boundary means the template's
     * only job is substitution, whatever the engine's own defaults turn out to be.
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
