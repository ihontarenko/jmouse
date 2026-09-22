package org.jmouse.grabber;

import java.net.URI;
import java.time.Duration;
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
 * 📑 The page a handler is looking at, and everything it can do about it.
 *
 * <p>This is the type the brief calls <i>серце системи</i>, and it is deliberately wide: every method
 * here is one a handler would otherwise write, and writing it once badly in a library beats writing it
 * fifty times badly in handlers.</p>
 *
 * <p>Four groups, and knowing which one a method belongs to answers most questions about it:</p>
 * <ul>
 *   <li><b>what is on the page</b> — {@link #select}, {@link #selectAll}, {@link #text},
 *       {@link #links}, {@link #document};</li>
 *   <li><b>what came back</b> — {@link #url}, {@link #status}, {@link #headers},
 *       {@link #contentType};</li>
 *   <li><b>what to do next</b> — {@link #follow}, {@link #leaf}, {@link #skip}, {@link #stop},
 *       {@link #retryLater}, each returning the {@link Verdict} to return;</li>
 *   <li><b>what has already been done</b> — {@link #processed}, {@link #markProcessed}.</li>
 * </ul>
 */
public interface PageContext {

    // ── what is on the page ───────────────────────────────────────────────────────────────────────

    PageDocument document();

    Optional<PageElement> select(String selector);

    List<PageElement> selectAll(String selector);

    /**
     * 🔤 The text of the first match, or empty when nothing matched or the match is blank.
     */
    Optional<String> text(String selector);

    /**
     * 🔤 The text of every match.
     */
    List<String> texts(String selector);

    /**
     * 🏷️ An attribute of the first match.
     */
    Optional<String> attribute(String selector, String name);

    /**
     * 🔗 Every address this selector's matches point at, absolute and deduplicated.
     */
    List<URI> links(String selector);

    /**
     * 🧪 Pulls named fields off this page as a map, or bound into the caller's own type.
     */
    <T> Optional<T> extract(Extraction<T> extraction);

    /**
     * 🧪 The same, for a repeating block — one result per match of the extraction's own selector.
     */
    <T> List<T> extractAll(Extraction<T> extraction);

    /**
     * 📃 The body as text, for the handler that would rather read it itself.
     */
    String rawText();

    byte[] bytes();

    // ── what came back ───────────────────────────────────────────────────────────────────────────

    /**
     * 🌐 ⚠️ The FINAL address, after redirects — the one every link on this page resolves against.
     */
    URI url();

    HttpStatus status();

    Headers headers();

    /**
     * 🎯 What the server said this is, or empty when it said nothing.
     */
    Optional<MediaType> contentType();

    PageResponse response();

    // ── the visit ────────────────────────────────────────────────────────────────────────────────

    Visit visit();

    /**
     * 🪜 How many follows from a seed this page is.
     */
    int depth();

    /**
     * 🎒 What the page that discovered this one handed along.
     */
    Attributes attributes();

    /**
     * 🏷️ The name of the route handling this page.
     */
    String route();

    /**
     * 📊 The run this page belongs to — its figures, and the way to stop it from outside a verdict.
     */
    GrabRun run();

    // ── what to do next ──────────────────────────────────────────────────────────────────────────

    /**
     * ➡️ Queues every address this selector finds and says so.
     *
     * <p>The ordinary case, and it reads as {@code return page.follow(".pagination a.next");}</p>
     */
    Verdict follow(String selector);

    /**
     * ➡️ Queues these addresses and says so.
     */
    Verdict follow(Collection<URI> addresses);

    /**
     * ➡️ Queues these addresses, sending them to a named route and carrying these attributes.
     *
     * <p>⚠️ <b>The attributes are the baton</b> — the category a listing page knows and a product page
     * cannot re-derive. This overload is the reason it exists.</p>
     */
    Verdict follow(Collection<URI> addresses, String route, Attributes carried);

    /**
     * 🍃 The end of this branch — a product page rather than a listing, an article rather than an
     * index.
     */
    Verdict leaf();

    /**
     * ⏭️ Deliberately not processed, and why.
     */
    Verdict skip(String reason);

    /**
     * 🛑 End the whole run.
     */
    Verdict stop(String reason);

    /**
     * ♻️ Try this page again after a wait — a soft block, an interstitial, a half-rendered page.
     */
    Verdict retryLater(Duration after, String reason);

    // ── queueing without deciding ────────────────────────────────────────────────────────────────

    /**
     * ➕ Queues one address without saying this page is finished.
     *
     * <p>For the handler that queues in several places and decides at the end.</p>
     */
    void enqueue(URI address);

    /**
     * ➕ Queues one address, sent to a named route and carrying attributes.
     */
    void enqueue(URI address, String route, Attributes carried);

    void enqueue(Visit visit);

    // ── output ───────────────────────────────────────────────────────────────────────────────────

    /**
     * 📤 Sends something to the run's sink — a record, a map, whatever the caller's sink takes.
     */
    void emit(Object item);

    // ── what has already been done ───────────────────────────────────────────────────────────────

    /**
     * 📦 Whether this item has been handled in any run against this journal.
     *
     * <p>Keyed by whatever identifies the thing rather than the address, which is how one product
     * reachable at two addresses stays one product.</p>
     */
    boolean processed(String itemKey);

    /**
     * 📦 Records that it has.
     */
    void markProcessed(String itemKey);

}
