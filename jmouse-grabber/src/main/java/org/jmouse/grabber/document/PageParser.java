package org.jmouse.grabber.document;

import java.util.List;

import org.jmouse.core.MediaType;
import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.grabber.fetch.PageResponse;

/**
 * 📖 Turns bytes into a {@link PageDocument}.
 *
 * <p>A parser says what it can read, and the parsers are held in a
 * {@link org.jmouse.core.chain.Chain} rather than in a registry of this module's own — per the epic's
 * reuse rule, and because the chain already does exactly this: ask each in turn, first one that
 * answers wins.</p>
 */
public interface PageParser {

    /**
     * 📖 The HTML parser, over Jsoup. What nearly every run uses and the default.
     */
    static PageParser html() {
        return new JsoupPageParser();
    }

    /**
     * 📦 Claims these media types and parses nothing, for the route that reads the body itself.
     *
     * <p>A JSON endpoint, an image to be written to disk, a feed a handler decodes with its own
     * reader. Without it such a route never runs at all: the engine skips a page no parser claims,
     * and the handler never reaches {@link org.jmouse.grabber.PageContext#rawText()} or
     * {@code bytes()}, which exist for exactly that case.</p>
     *
     * <p>The document it produces has the right address and the body as its {@code html()}; every
     * selector matches nothing, because there is nothing to select.</p>
     *
     * <p>⚠️ <b>It takes its types explicitly and must never be given a wildcard.</b> The builder puts
     * a caller's parsers ahead of the HTML one, so a raw parser claiming {@code &#42;/&#42;} silently
     * swallows every HTML page in the run — and every selector then returns empty, which reads as the
     * site having changed its markup rather than as a misconfiguration.</p>
     */
    static PageParser raw(MediaType... types) {
        return new RawPageParser(List.of(types));
    }

    /**
     * ⛓️ The parsers, in order, as a chain that answers with a document or with {@code null}.
     *
     * <p>⚠️ Order matters and is the caller's: a parser claiming {@code text/*} placed first would
     * take the HTML pages away from the HTML parser.</p>
     */
    static Chain<Void, PageResponse, PageDocument> chainOf(List<PageParser> parsers) {
        Chain.Builder<Void, PageResponse, PageDocument> builder = Chain.builder();

        for (PageParser parser : parsers) {
            builder.add((context, response, next) -> {
                if (parser.supports(response.contentType())) {
                    return Outcome.done(parser.parse(response));
                }

                return next.proceed(context, response);
            });
        }

        return builder.withFallback((context, response) -> null).toChain();
    }

    /**
     * 🎯 Whether this parser reads that.
     *
     * @param contentType what the response declared, which may be {@code null} when it declared
     *                    nothing at all
     */
    boolean supports(MediaType contentType);

    PageDocument parse(PageResponse response);

}
