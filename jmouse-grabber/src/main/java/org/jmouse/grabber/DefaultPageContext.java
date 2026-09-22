package org.jmouse.grabber;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jmouse.core.MediaType;
import org.jmouse.grabber.document.PageDocument;
import org.jmouse.grabber.document.PageElement;
import org.jmouse.grabber.extract.Extraction;
import org.jmouse.grabber.fetch.PageResponse;
import org.jmouse.http.Headers;
import org.jmouse.http.HttpStatus;

/**
 * 📑 The context handed to one handler for one page.
 *
 * <p>Deliberately short: nearly everything is a delegation to the document, the response or the
 * engine. The interesting part is that the deciding methods both queue the work <b>and</b> return the
 * verdict, which is what lets a handler say what it means in one line.</p>
 */
final class DefaultPageContext implements PageContext {

    private final Visit        visit;
    private final String       route;
    private final PageResponse response;
    private final PageDocument document;
    private final GrabEngine   engine;

    DefaultPageContext(Visit visit, String route, PageResponse response, PageDocument document, GrabEngine engine) {
        this.visit = visit;
        this.route = route;
        this.response = response;
        this.document = document;
        this.engine = engine;
    }

    @Override
    public PageDocument document() {
        return document;
    }

    @Override
    public Optional<PageElement> select(String selector) {
        return document.select(selector);
    }

    @Override
    public List<PageElement> selectAll(String selector) {
        return document.selectAll(selector);
    }

    @Override
    public Optional<String> text(String selector) {
        return document.text(selector);
    }

    @Override
    public List<String> texts(String selector) {
        return document.texts(selector);
    }

    @Override
    public Optional<String> attribute(String selector, String name) {
        return document.attribute(selector, name);
    }

    @Override
    public List<URI> links(String selector) {
        return document.links(selector);
    }

    @Override
    public <T> Optional<T> extract(Extraction<T> extraction) {
        return extraction.from(document);
    }

    @Override
    public <T> List<T> extractAll(Extraction<T> extraction) {
        return extraction.allFrom(document);
    }

    @Override
    public String rawText() {
        return response.text();
    }

    @Override
    public byte[] bytes() {
        return response.body();
    }

    @Override
    public URI url() {
        return response.address();
    }

    @Override
    public HttpStatus status() {
        return response.status();
    }

    @Override
    public Headers headers() {
        return response.headers();
    }

    @Override
    public Optional<MediaType> contentType() {
        return Optional.ofNullable(response.contentType());
    }

    @Override
    public PageResponse response() {
        return response;
    }

    @Override
    public Visit visit() {
        return visit;
    }

    @Override
    public int depth() {
        return visit.depth();
    }

    @Override
    public Attributes attributes() {
        return visit.attributes();
    }

    @Override
    public String route() {
        return route;
    }

    @Override
    public GrabRun run() {
        return engine.run();
    }

    @Override
    public Verdict follow(String selector) {
        return follow(links(selector));
    }

    @Override
    public Verdict follow(Collection<URI> addresses) {
        return follow(addresses, null, Attributes.empty());
    }

    @Override
    public Verdict follow(Collection<URI> addresses, String targetRoute, Attributes carried) {
        List<Visit> queued = new ArrayList<>();

        for (URI address : addresses) {
            Visit discovered = visit.discover(address, targetRoute, carried);

            if (engine.enqueue(discovered)) {
                queued.add(discovered);
            }
        }

        return Verdict.followed(queued);
    }

    @Override
    public Verdict leaf() {
        return Verdict.leaf();
    }

    @Override
    public Verdict skip(String reason) {
        return Verdict.skipped(reason);
    }

    @Override
    public Verdict stop(String reason) {
        return Verdict.stopped(reason);
    }

    @Override
    public Verdict retryLater(Duration after, String reason) {
        return Verdict.retry(after, reason);
    }

    @Override
    public void enqueue(URI address) {
        enqueue(address, null, Attributes.empty());
    }

    @Override
    public void enqueue(URI address, String targetRoute, Attributes carried) {
        engine.enqueue(visit.discover(address, targetRoute, carried));
    }

    @Override
    public void enqueue(Visit discovered) {
        engine.enqueue(discovered);
    }

    @Override
    public void emit(Object item) {
        engine.emit(visit, item);
    }

    @Override
    public boolean processed(String itemKey) {
        return engine.journal().processed(itemKey);
    }

    @Override
    public void markProcessed(String itemKey) {
        engine.journal().markProcessed(itemKey);
    }

}
