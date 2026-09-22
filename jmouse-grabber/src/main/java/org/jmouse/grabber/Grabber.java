package org.jmouse.grabber;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import org.jmouse.core.Customizer;
import org.jmouse.core.chain.Chain;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.grabber.document.PageDocument;
import org.jmouse.grabber.document.PageParser;
import org.jmouse.grabber.fetch.PageFetcher;
import org.jmouse.grabber.fetch.PageResponse;
import org.jmouse.grabber.journal.VisitJournal;
import org.jmouse.grabber.journal.VisitQueue;
import org.jmouse.grabber.route.GrabRoute;
import org.jmouse.grabber.route.RouteTable;

/**
 * 🕸️ A page grabber, assembled and ready to run.
 *
 * <pre>{@code
 * Grabber grabber = Grabber.builder()
 *         .policy(policy -> policy
 *                 .concurrency(5)
 *                 .delay(Duration.ofMillis(300), Duration.ofMillis(1200))
 *                 .retries(3)
 *                 .maximumDepth(4))
 *         .journal(VisitJournal.onDisk(Path.of("./.grab/catalog")))
 *         .seed("https://example.com/catalog")
 *         .route("listing", TextMatchers.contains("/catalog"), page -> {
 *             page.emit(page.extractAll(CARD));
 *             return page.follow("a.product-link, .pagination a.next");
 *         })
 *         .route("product", TextMatchers.contains("/product/"), page -> {
 *             page.extract(PRODUCT).ifPresent(page::emit);
 *             return page.leaf();
 *         })
 *         .into(products::add)
 *         .build();
 *
 * GrabRun result = grabber.run();
 * }</pre>
 *
 * <p>⚠️ <b>Everything about a run is reachable from the builder</b>, and nothing needs a file, an
 * annotation or a container. That is the whole ask this module answers: it is configured in code, and
 * the code is meant to read like the description of the crawl.</p>
 */
public final class Grabber {

    private final GrabEngine engine;
    private final List<Visit> seeds;
    private final ItemSink    items;

    private Grabber(GrabEngine engine, List<Visit> seeds, ItemSink items) {
        this.engine = engine;
        this.seeds = seeds;
        this.items = items;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * ▶️ Runs until the queue drains, a cap is reached, or something stops it.
     *
     * @return the run's figures, which are also what a listener was given as it went
     */
    public GrabRun run() {
        engine.prime(seeds);
        engine.drain();

        try {
            items.close();
        } catch (Exception exception) {
            throw new GrabberException("The item sink failed to close", exception);
        }

        return engine.run();
    }

    /**
     * 📊 The run, before it has started or while it is going — for a caller that wants to stop it
     * from another thread.
     */
    public GrabRun status() {
        return engine.run();
    }

    /**
     * 🛠️ The fluent assembly.
     *
     * <p>It uses {@link Customizer} for its nested sections, because that is the shape every other
     * fluent surface in this library already has. A grabber whose builder read differently from
     * {@code HttpSecurity} or {@code PipelineDefinitions} would be the very drift this module was
     * written to avoid.</p>
     */
    public static final class Builder {

        private final List<Visit>      seeds      = new ArrayList<>();
        private final List<GrabRoute>  routes     = new ArrayList<>();
        private final List<PageParser> parsers    = new ArrayList<>();
        private final List<GrabListener> listeners = new ArrayList<>();

        private GrabPolicy        policy     = GrabPolicy.defaults();
        private PageFetcher       fetcher;
        private VisitJournal      journal;
        private VisitQueue        queue      = VisitQueue.breadthFirst();
        private AddressNormalizer normalizer = AddressNormalizer.standard();
        private ItemSink          items      = ItemSink.discarding();
        private FailureSink       failures   = FailureSink.logging();
        private RandomGenerator   random     = RandomGenerator.getDefault();

        private Builder() {
        }

        // ── what to visit ────────────────────────────────────────────────────────────────────────

        /**
         * 🌱 An address to start from.
         */
        public Builder seed(String address) {
            return seed(URI.create(address));
        }

        public Builder seed(URI address) {
            seeds.add(Visit.seed(address));
            return this;
        }

        /**
         * 🌱 An address to start from, with a baton already on it.
         */
        public Builder seed(URI address, Attributes carried) {
            seeds.add(Visit.seed(address, carried));
            return this;
        }

        public Builder seeds(String... addresses) {
            Arrays.stream(addresses).forEach(this::seed);
            return this;
        }

        // ── what to do with a page ───────────────────────────────────────────────────────────────

        /**
         * 🛣️ A route claiming the pages whose ADDRESS this matcher accepts.
         *
         * <p>The ordinary form, and the one that takes {@code TextMatchers.contains("/product/")}
         * directly.</p>
         */
        public Builder route(String name, Matcher<String> address, PageHandler handler) {
            routes.add(GrabRoute.onAddress(name, address, handler));
            return this;
        }

        /**
         * 🛣️ A route already built — for one that narrows the policy or identifies its pages by
         * something other than the address.
         */
        public Builder route(GrabRoute route) {
            routes.add(route);
            return this;
        }

        /**
         * 🛣️ What handles everything nothing else claimed.
         *
         * <p>⚠️ Declared last whatever order it is written in would be a kindness the table cannot
         * offer without lying about "first match wins", so it is simply a route like any other:
         * write it last.</p>
         */
        public Builder otherwise(PageHandler handler) {
            routes.add(GrabRoute.anything("otherwise", handler));
            return this;
        }

        // ── how to behave ────────────────────────────────────────────────────────────────────────

        /**
         * ⚙️ The run's policy, built here.
         */
        public Builder policy(Customizer<GrabPolicy.Builder> customizer) {
            GrabPolicy.Builder builder = policy.toBuilder();

            customizer.customize(builder);
            this.policy = builder.build();

            return this;
        }

        /**
         * ⚙️ The run's policy, already built.
         */
        public Builder policy(GrabPolicy value) {
            this.policy = value;
            return this;
        }

        // ── the seams ────────────────────────────────────────────────────────────────────────────

        /**
         * 🌐 How pages are obtained. The JDK's HTTP client unless something else is given.
         */
        public Builder fetcher(PageFetcher value) {
            this.fetcher = value;
            return this;
        }

        /**
         * 📖 An additional parser, ahead of the HTML one.
         */
        public Builder parser(PageParser value) {
            parsers.add(value);
            return this;
        }

        /**
         * 📓 Where "already done" is remembered. In memory unless something else is given — which
         * means a run is <b>not</b> resumable by default, and making it so is one line.
         */
        public Builder journal(VisitJournal value) {
            this.journal = value;
            return this;
        }

        /**
         * 🚦 The order pages come off the frontier. Breadth first unless something else is given.
         */
        public Builder queue(VisitQueue value) {
            this.queue = value;
            return this;
        }

        /**
         * 🧭 How two spellings of an address become one.
         */
        public Builder normalizer(AddressNormalizer value) {
            this.normalizer = value;
            return this;
        }

        // ── what comes out ───────────────────────────────────────────────────────────────────────

        /**
         * 📤 Where emitted things go.
         */
        public Builder into(Consumer<Object> consumer) {
            this.items = ItemSink.of(consumer);
            return this;
        }

        public Builder into(ItemSink sink) {
            this.items = sink;
            return this;
        }

        /**
         * 🚑 Where visits that could not be completed go.
         */
        public Builder onFailure(FailureSink sink) {
            this.failures = sink;
            return this;
        }

        /**
         * 👂 Something watching the run.
         */
        public Builder listener(GrabListener value) {
            listeners.add(value);
            return this;
        }

        /**
         * 🎲 The source of the random delays, so a test can make a run repeatable.
         */
        public Builder random(RandomGenerator value) {
            this.random = value;
            return this;
        }

        public Grabber build() {
            if (routes.isEmpty()) {
                throw new GrabberException("A grabber needs at least one route, or nothing will handle a page");
            }

            if (seeds.isEmpty() && (journal == null || journal.pending().isEmpty())) {
                throw new GrabberException("A grabber needs at least one seed, or an unfinished journal to resume");
            }

            List<PageParser> allParsers = new ArrayList<>(parsers);

            allParsers.add(PageParser.html());

            Chain<Void, PageResponse, PageDocument> parserChain = PageParser.chainOf(allParsers);

            GrabEngine engine = new GrabEngine(
                    RouteTable.of(routes),
                    policy,
                    fetcher == null ? PageFetcher.http() : fetcher,
                    parserChain,
                    queue,
                    journal == null ? VisitJournal.inMemory() : journal,
                    normalizer,
                    items,
                    failures,
                    listeners.isEmpty() ? GrabListener.none() : GrabListener.composite(listeners),
                    random
            );

            return new Grabber(engine, List.copyOf(seeds), items);
        }
    }

}
