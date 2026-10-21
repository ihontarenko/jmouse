package org.jmouse.parallax.spring.autoconfigure;

import org.jmouse.parallax.Computations;
import org.jmouse.parallax.StandardComputations;
import org.jmouse.parallax.authority.ParallaxAuthority;
import org.jmouse.parallax.authority.ParallaxPlaces;
import org.jmouse.parallax.catalogue.HomeCategoryPolicy;
import org.jmouse.parallax.catalogue.ShippedDocumentHandler;
import org.jmouse.parallax.catalogue.StandardHomeCategoryPolicy;
import org.jmouse.parallax.jpa.catalogue.CategoryStore;
import org.jmouse.parallax.jpa.catalogue.InstrumentStore;
import org.jmouse.parallax.jpa.catalogue.ObjectStore;
import org.jmouse.parallax.jpa.collection.CollectionStore;
import org.jmouse.parallax.jpa.computation.DatasetCache;
import org.jmouse.parallax.jpa.computation.JobQueue;
import org.jmouse.parallax.jpa.evaluation.EvaluationLedger;
import org.jmouse.parallax.jpa.scene.ComparisonStore;
import org.jmouse.parallax.jpa.scene.SceneStore;
import org.jmouse.parallax.limits.ParallaxLimits;
import org.jmouse.parallax.store.catalogue.Catalogue;
import org.jmouse.parallax.store.catalogue.CatalogueImport;
import org.jmouse.parallax.store.catalogue.CatalogueProperties;
import org.jmouse.parallax.store.catalogue.ComponentModels;
import org.jmouse.parallax.store.catalogue.Instruments;
import org.jmouse.parallax.store.catalogue.ObjectAccess;
import org.jmouse.parallax.store.catalogue.ObjectDocuments;
import org.jmouse.parallax.store.catalogue.SharedDocuments;
import org.jmouse.parallax.store.catalogue.ShippedObjects;
import org.jmouse.parallax.store.collection.Collections;
import org.jmouse.parallax.store.computation.CatalogueWarmup;
import org.jmouse.parallax.store.computation.ComputationProperties;
import org.jmouse.parallax.store.computation.ComputationSchedule;
import org.jmouse.parallax.store.computation.ComputationWorker;
import org.jmouse.parallax.store.computation.Computing;
import org.jmouse.parallax.store.computation.DatasetCodec;
import org.jmouse.parallax.store.computation.Jobs;
import org.jmouse.parallax.store.document.DocumentEditing;
import org.jmouse.parallax.store.evaluation.Evaluations;
import org.jmouse.parallax.store.formula.FormulaGuide;
import org.jmouse.parallax.store.formula.FormulaNotations;
import org.jmouse.parallax.store.scene.Comparisons;
import org.jmouse.parallax.store.scene.InlineSources;
import org.jmouse.parallax.store.scene.SceneDocuments;
import org.jmouse.parallax.store.scene.Scenes;
import org.jmouse.parallax.store.statistics.ParallaxStatistics;
import org.jmouse.parallax.store.subject.Subjects;
import org.jmouse.parallax.store.support.ParallaxJson;
import org.springframework.core.io.ResourceLoader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Conditional;
import org.jmouse.parallax.store.spice.SpiceConfigured;
import org.jmouse.parallax.circuit.CircuitEngines;
import org.jmouse.parallax.spice.NgspiceProcess;
import org.jmouse.parallax.spice.SpiceEngine;
import org.jmouse.parallax.spice.SpiceSimulator;
import org.jmouse.parallax.store.spice.SpiceProperties;
import org.jmouse.parallax.store.kicad.KiCadExports;
import org.jmouse.parallax.store.kicad.KiCadImports;
import org.jmouse.parallax.store.kicad.KiCadLibraryMaps;
import org.jmouse.parallax.store.kicad.KiCadProperties;
import org.jmouse.parallax.kicad.InstalledKiCadParts;
import org.jmouse.parallax.part.PartSearchProvider;
import org.jmouse.parallax.store.part.CataloguePartSearch;
import org.jmouse.parallax.store.part.NexarPartSearch;
import org.jmouse.parallax.store.part.PartSearch;
import org.jmouse.parallax.store.part.PartSearchProperties;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;
import java.time.Clock;

/**
 * 🧮 The Parallax application services — catalogue, scenes, collections, computation, evaluation, formulas — over the
 * stores {@link ParallaxJpaAutoConfiguration} declares.
 *
 * <p>Each service keeps its own {@code @Transactional} boundaries; they apply because every bean here is proxied like
 * any of the application's own. Every bean backs off when the application declares its own.</p>
 *
 * <h3>⚠️ The product must supply three things</h3>
 *
 * <ul>
 *     <li>a {@link ParallaxAuthority} — who is acting and what they may do;</li>
 *     <li>a {@link ParallaxPlaces} — the product's owners (workspaces, projects…) as places;</li>
 *     <li>a {@link ParallaxLimits} — how much Parallax may do, asked at the moment it does it.</li>
 * </ul>
 *
 * <p>Without one, startup fails with a sentence that names it: a default that allowed everything, or placed nothing,
 * would make every refusal vanish without a trace.</p>
 *
 * <h3>The worker does not need the host's {@code @EnableScheduling}</h3>
 *
 * <p>{@link ComputationSchedule} runs the worker's poll and maintenance and the nightly dataset forgetting on a
 * scheduler of its own. {@code jmouse.parallax.computation.worker-threads: 0} still makes a process that never
 * computes in the background: its poll takes nothing.</p>
 *
 * <h3>Settings</h3>
 *
 * <ul>
 *     <li>{@code jmouse.parallax.computation.*} — {@link ComputationProperties};</li>
 *     <li>{@code jmouse.parallax.catalogue.*} — {@link CatalogueProperties};</li>
 *     <li>{@code jmouse.parallax.spice.*} — {@link SpiceProperties}: the second engine, when a command is set.</li>
 *     <li>{@code jmouse.parallax.kicad.*} — {@link KiCadProperties}: the library map a KiCad import reads, and the
 *     footprint libraries it looks a part's footprint up in, and the symbol libraries the part search reads.</li>
 *     <li>{@code jmouse.parallax.parts.*} — {@link PartSearchProperties}: how many candidates a provider answers with,
 *     and the Nexar application the supplier search signs in as.</li>
 * </ul>
 */
@AutoConfiguration(after = ParallaxJpaAutoConfiguration.class)
@ConditionalOnClass({ObjectStore.class, TransactionTemplate.class, ObjectMapper.class})
@EnableConfigurationProperties({ComputationProperties.class, CatalogueProperties.class, SpiceProperties.class,
        KiCadProperties.class, PartSearchProperties.class})
public class ParallaxStoreAutoConfiguration {

    // ── What the services share ──────────────────────────────────────────────────────────────────────────────────

    /**
     * 📐 The computation registry of the engine: every representation Parallax can compute — a circuit's batch
     * analyses by the library's own engine, or by SPICE where a simulator is configured.
     *
     * @return the registry
     */
    @Bean
    @ConditionalOnMissingBean
    public Computations parallaxComputations(ObjectProvider<SpiceSimulator> simulator) {
        CircuitEngines engines = CircuitEngines.parallaxOnly();
        SpiceSimulator spice = simulator.getIfAvailable();
        if (spice != null) {
            engines = engines.with(new SpiceEngine(spice));
        }
        return StandardComputations.registry(engines);
    }

    /**
     * 🕰️ The clock every timestamp is read from.
     *
     * @return the system clock in UTC
     */
    @Bean
    @ConditionalOnMissingBean
    public Clock parallaxClock() {
        return Clock.systemUTC();
    }

    /**
     * 🏠 The section an object of a kind is filed in when nobody chose one.
     *
     * @return the homes the shipped catalogue names
     */
    @Bean
    @ConditionalOnMissingBean
    public HomeCategoryPolicy parallaxHomeCategoryPolicy() {
        return new StandardHomeCategoryPolicy();
    }

    /**
     * 📄 The typed documents stored in text columns, written and read with the application's mapper.
     *
     * @param mapper the application's JSON mapper
     * @return the documents
     */
    @Bean
    @ConditionalOnMissingBean
    public ParallaxJson parallaxJson(ObjectMapper mapper) {
        return new ParallaxJson(mapper);
    }

    // ── Catalogue ────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * 📖 An object's jMX text read into what it describes, cached by checksum.
     *
     * @return the reader
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectDocuments parallaxObjectDocuments() {
        return new ObjectDocuments();
    }

    /**
     * ✒️ One formula in LaTeX, jMF and plain text.
     *
     * @return the notations
     */
    @Bean
    @ConditionalOnMissingBean
    public FormulaNotations parallaxFormulaNotations() {
        return new FormulaNotations();
    }

    /**
     * 🧩 The logic families and function libraries components share.
     *
     * @param properties where they are read from
     * @return the shared documents
     */
    @Bean
    @ConditionalOnMissingBean
    public SharedDocuments parallaxSharedDocuments(CatalogueProperties properties) {
        return new SharedDocuments(properties);
    }

    /**
     * 🔐 Which objects the current actor may read and write.
     *
     * @param objects   the objects
     * @param authority the product's authority
     * @param places    the product's places
     * @return the access
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectAccess parallaxObjectAccess(ObjectStore objects, ObjectProvider<ParallaxAuthority> authority,
                                             ObjectProvider<ParallaxPlaces> places) {
        return new ObjectAccess(objects, requireAuthority(authority), requirePlaces(places));
    }

    /**
     * 🔩 Component models, resolved by {@code slug@revision}.
     *
     * @return the models
     */
    @Bean
    @ConditionalOnMissingBean
    public ComponentModels parallaxComponentModels(ObjectStore objects, ObjectAccess objectAccess,
                                                   ObjectDocuments objectDocuments, ParallaxJson json,
                                                   SharedDocuments sharedDocuments, HomeCategoryPolicy homeCategories) {
        return new ComponentModels(objects, objectAccess, objectDocuments, json, sharedDocuments);
    }

    /**
     * 📚 The catalogue: objects, their revisions, sections and relations.
     *
     * @return the catalogue
     */
    @Bean
    @ConditionalOnMissingBean
    public Catalogue parallaxCatalogue(ObjectStore objects, CategoryStore categories, ObjectAccess objectAccess,
                                       ObjectProvider<ParallaxAuthority> authority, Computations computations,
                                       ParallaxJson documents, ApplicationEventPublisher events, Clock clock,
                                       FormulaNotations notations, ComponentModels componentModels,
                                       ObjectDocuments objectDocuments, ObjectProvider<ParallaxPlaces> places,
                                       SharedDocuments sharedDocuments, HomeCategoryPolicy homeCategories,
                                       ObjectProvider<ParallaxLimits> limits) {
        return new Catalogue(objects, categories, objectAccess, requireAuthority(authority), computations, documents,
                             events, clock, notations, componentModels, objectDocuments, requirePlaces(places),
                             sharedDocuments, homeCategories, requireLimits(limits));
    }

    /**
     * 🔭 The instruments — scene presets for the kinds they open.
     *
     * @return the instruments
     */
    @Bean
    @ConditionalOnMissingBean
    public Instruments parallaxInstruments(InstrumentStore instruments, ObjectStore objects,
                                           ObjectProvider<ParallaxAuthority> authority, ParallaxJson documents) {
        return new Instruments(instruments, objects, requireAuthority(authority), documents);
    }

    /**
     * 📦 The text each object has in the shipped catalogue.
     *
     * @param properties where the shipped catalogue is read from
     * @return the shipped texts
     */
    @Bean
    @ConditionalOnMissingBean
    public ShippedObjects parallaxShippedObjects(CatalogueProperties properties) {
        return new ShippedObjects(properties);
    }

    /**
     * 🚚 The shipped catalogue's import, which the product runs when it decides to.
     *
     * @param handlers the product's handlers for the kinds the catalogue reads but does not keep
     * @return the import
     */
    @Bean
    @ConditionalOnMissingBean
    public CatalogueImport parallaxCatalogueImport(Catalogue catalogue, CategoryStore categories,
                                                   Collections collections, Instruments instruments,
                                                   ObjectProvider<ShippedDocumentHandler> handlers,
                                                   CatalogueProperties properties) {
        return new CatalogueImport(catalogue, categories, collections, instruments,
                                   handlers.orderedStream().toList(), properties);
    }

    // ── Scenes, comparisons, collections ─────────────────────────────────────────────────────────────────────────

    /**
     * ✍️ Inline objects of scenes and comparisons, kept as jMX text.
     *
     * @return the inline sources
     */
    @Bean
    @ConditionalOnMissingBean
    public InlineSources parallaxInlineSources(ObjectDocuments documents) {
        return new InlineSources(documents);
    }

    /**
     * 🎞️ Scene documents, checked and hydrated.
     *
     * @return the scene documents
     */
    @Bean
    @ConditionalOnMissingBean
    public SceneDocuments parallaxSceneDocuments(ObjectAccess objectAccess, Catalogue catalogue,
                                                 Computations computations, ObjectProvider<ParallaxLimits> limits,
                                                 ParallaxJson documents, InlineSources inline) {
        return new SceneDocuments(objectAccess, catalogue, computations, requireLimits(limits), documents, inline);
    }

    /**
     * 🎬 Scenes, their revisions and snapshots.
     *
     * @return the scenes
     */
    @Bean
    @ConditionalOnMissingBean
    public Scenes parallaxScenes(SceneStore scenes, SceneDocuments documents, ObjectProvider<ParallaxPlaces> places,
                                 InstrumentStore instruments, ObjectStore objects, Catalogue catalogue,
                                 ObjectProvider<ParallaxAuthority> authority, ParallaxJson json, Clock clock) {
        return new Scenes(scenes, documents, requirePlaces(places), instruments, objects, catalogue,
                          requireAuthority(authority), json, clock);
    }

    /**
     * 🪟 Comparisons and their slots.
     *
     * @return the comparisons
     */
    @Bean
    @ConditionalOnMissingBean
    public Comparisons parallaxComparisons(ComparisonStore comparisons, ObjectProvider<ParallaxPlaces> places,
                                           ObjectAccess objectAccess, Catalogue catalogue, Computations computations,
                                           ObjectProvider<ParallaxAuthority> authority, ParallaxJson json, Clock clock,
                                           InlineSources inline) {
        return new Comparisons(comparisons, requirePlaces(places), objectAccess, catalogue, computations,
                               requireAuthority(authority), json, clock, inline);
    }

    /**
     * 🗂️ Collections — shelves and formula sets.
     *
     * @return the collections
     */
    @Bean
    @ConditionalOnMissingBean
    public Collections parallaxCollections(CollectionStore collections, ObjectStore objects, ObjectAccess objectAccess,
                                           Scenes scenes, Comparisons comparisons,
                                           ObjectProvider<ParallaxAuthority> authority,
                                           ObjectProvider<ParallaxPlaces> places, Clock clock) {
        return new Collections(collections, objects, objectAccess, scenes, comparisons, requireAuthority(authority),
                               requirePlaces(places), clock);
    }

    // ── Computation ──────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * 🎯 What is computed: a catalogue object, an inline formula or a document.
     *
     * @return the subjects
     */
    @Bean
    @ConditionalOnMissingBean
    public Subjects parallaxSubjects(Catalogue catalogue, Computations computations, FormulaNotations notations,
                                     ObjectDocuments documents) {
        return new Subjects(catalogue, computations, notations, documents);
    }

    /**
     * 🗜️ Datasets in and out of their stored form.
     *
     * @return the codec
     */
    @Bean
    @ConditionalOnMissingBean
    public DatasetCodec parallaxDatasetCodec(ParallaxJson documents) {
        return new DatasetCodec(documents);
    }

    /**
     * 🧮 Computing a representation: at once within the budget, queued above it, cached by fingerprint.
     *
     * @return the computing
     */
    @Bean
    @ConditionalOnMissingBean
    public Computing parallaxComputing(Catalogue catalogue, Computations computations, DatasetCache datasets,
                                       JobQueue jobs, DatasetCodec codec, ParallaxJson documents,
                                       ObjectProvider<ParallaxAuthority> authority,
                                       ObjectProvider<ParallaxPlaces> places, ObjectProvider<ParallaxLimits> limits,
                                       TransactionTemplate transactions, Clock clock, Subjects subjects) {
        return new Computing(catalogue, computations, datasets, jobs, codec, documents, requireAuthority(authority),
                             requirePlaces(places), requireLimits(limits), transactions, clock, subjects);
    }

    /**
     * ⏳ The queued jobs and the dataset cache, as their readers and administrators see them.
     *
     * @return the jobs
     */
    @Bean
    @ConditionalOnMissingBean
    public Jobs parallaxJobs(JobQueue jobs, DatasetCache datasets, Computing computing,
                             ObjectProvider<ParallaxAuthority> authority, ObjectProvider<ParallaxPlaces> places,
                             ObjectProvider<ParallaxLimits> limits, Clock clock) {
        return new Jobs(jobs, datasets, computing, requireAuthority(authority), requirePlaces(places),
                        requireLimits(limits), clock);
    }

    /**
     * 📊 What the machinery holds and how it is doing, for a host's system screen.
     *
     * @return the statistics
     */
    @Bean
    @ConditionalOnMissingBean
    public ParallaxStatistics parallaxStatistics(JobQueue jobs, DatasetCache datasets, ObjectStore objects,
                                                 SceneStore scenes, ObjectProvider<ParallaxAuthority> authority,
                                                 Clock clock) {
        return new ParallaxStatistics(jobs, datasets, objects, scenes, requireAuthority(authority), clock);
    }

    /**
     * ⚙️ The worker that takes queued jobs; with no worker threads it takes none.
     *
     * @return the worker
     */
    @Bean
    @ConditionalOnMissingBean
    public ComputationWorker parallaxComputationWorker(Computing computing, JobQueue jobs,
                                                       TransactionTemplate transactions,
                                                       ComputationProperties properties,
                                                       ApplicationEventPublisher events, Clock clock) {
        return new ComputationWorker(computing, jobs, transactions, properties, events, clock);
    }

    /**
     * ⏱️ The worker's poll and maintenance and the nightly forgetting, on a scheduler of their own.
     *
     * @return the schedule
     */
    @Bean
    @ConditionalOnMissingBean
    public ComputationSchedule parallaxComputationSchedule(ComputationWorker worker, Jobs jobs,
                                                           ComputationProperties properties) {
        return new ComputationSchedule(worker, jobs, properties);
    }

    /**
     * 🔥 Every published object's representations computed at their defaults after a start.
     *
     * @return the warm-up
     */
    @Bean
    @ConditionalOnMissingBean
    public CatalogueWarmup parallaxCatalogueWarmup(Catalogue catalogue, Computations computations,
                                                   Computing computing, ObjectProvider<ParallaxAuthority> authority,
                                                   ObjectProvider<ParallaxLimits> limits,
                                                   ApplicationEventPublisher events, Clock clock) {
        return new CatalogueWarmup(catalogue, computations, computing, requireAuthority(authority),
                                   requireLimits(limits), events, clock);
    }

    // ── Documents, evaluation, formulas ──────────────────────────────────────────────────────────────────────────

    /**
     * 📝 A jMX document as a form, and an edit of one span of it.
     *
     * @return the editing
     */
    @Bean
    @ConditionalOnMissingBean
    public DocumentEditing parallaxDocumentEditing(ObjectDocuments documents, ComponentModels components) {
        return new DocumentEditing(documents, components);
    }

    /**
     * ✅ The catalogue's consistency checks, run and kept.
     *
     * @return the evaluations
     */
    @Bean
    @ConditionalOnMissingBean
    public Evaluations parallaxEvaluations(Catalogue catalogue, ObjectProvider<ParallaxAuthority> authority,
                                           ApplicationEventPublisher events, EvaluationLedger ledger,
                                           TransactionTemplate transactions, Clock clock) {
        return new Evaluations(catalogue, requireAuthority(authority), events, ledger, transactions, clock);
    }

    /**
     * 📘 The formula language's reference and the interpretation of a typed formula.
     *
     * @return the guide
     */
    @Bean
    @ConditionalOnMissingBean
    public FormulaGuide parallaxFormulaGuide(ParallaxJson documents, Computations computations) {
        return new FormulaGuide(documents, computations);
    }

    // ── What only the product can answer ─────────────────────────────────────────────────────────────────────────

    private static ParallaxAuthority requireAuthority(ObjectProvider<ParallaxAuthority> candidates) {
        return requireProvided(candidates, ParallaxAuthority.class,
                "only the product knows who is acting and which of its permissions each capability means. "
                + "Declare one that answers actor(), permits() and require().");
    }

    private static ParallaxPlaces requirePlaces(ObjectProvider<ParallaxPlaces> candidates) {
        return requireProvided(candidates, ParallaxPlaces.class,
                "only the product knows its owners — workspaces, projects — and what each sits in. "
                + "Declare one that answers requireOwner(), requireAcceptsNew() and recordChange().");
    }

    private static ParallaxLimits requireLimits(ObjectProvider<ParallaxLimits> candidates) {
        return requireProvided(candidates, ParallaxLimits.class,
                "only the product decides how much may be computed, queued and kept. "
                + "Declare one, from its own settings or from constants.");
    }

    private static <T> T requireProvided(ObjectProvider<T> candidates, Class<T> type, String reason) {
        T provided = candidates.getIfAvailable();

        if (provided == null) {
            throw new IllegalStateException("jmouse-parallax needs a " + type.getSimpleName() + " bean: " + reason);
        }

        return provided;
    }

    /**
     * 🔁 SPICE, the second engine: ngspice started by the configured command, one process a run. Absent while no
     * command is configured — the routes that need it then refuse with a reason.
     */
    @Bean
    @ConditionalOnMissingBean
    @Conditional(SpiceConfigured.class)
    public SpiceSimulator parallaxSpiceSimulator(SpiceProperties properties) {
        return new NgspiceProcess(properties.command(), properties.timeout());
    }

    /**
     * 📥 KiCad in: a design's sheets imported and stored in an owner's place, its symbols matched to the catalogue's
     * components through the library map {@code jmouse.parallax.kicad.library-map} names, the footprints its parts name
     * looked up in the libraries sent with it and then in {@code jmouse.parallax.kicad.footprint-libraries}.
     */
    @Bean
    @ConditionalOnMissingBean
    public KiCadImports parallaxKiCadImports(Catalogue catalogue, ObjectStore objects, ComponentModels components,
                                             KiCadLibraryMaps maps, KiCadProperties properties,
                                             ApplicationEventPublisher events) {
        return new KiCadImports(catalogue, objects, components, maps, KiCadFootprintLibraries.in(
                properties.footprintLibraries().stream().map(Path::of).toList()), events);
    }

    /** 📤 KiCad out: a circuit written as a KiCad schematic, its parts under the library ids the library map gives back. */
    @Bean
    @ConditionalOnMissingBean
    public KiCadExports parallaxKiCadExports(ComponentModels components, KiCadLibraryMaps maps) {
        return new KiCadExports(components, maps);
    }

    /** 🗺 The library map a KiCad import and export read: {@code jmouse.parallax.kicad.library-map}. */
    @Bean
    @ConditionalOnMissingBean
    public KiCadLibraryMaps parallaxKiCadLibraryMaps(ResourceLoader resources, KiCadProperties properties) {
        return new KiCadLibraryMaps(resources, properties);
    }

    // ── Part search ──────────────────────────────────────────────────────────────────────────────────────────────

    /** 🔎 The catalogue as a part search: its published objects, nothing to fetch. */
    @Bean
    @ConditionalOnMissingBean
    public CataloguePartSearch parallaxCataloguePartSearch(Catalogue catalogue) {
        return new CataloguePartSearch(catalogue);
    }

    /**
     * 🔎 The KiCad libraries installed beside the server as a part search: {@code jmouse.parallax.kicad.symbol-libraries}
     * and {@code footprint-libraries}, indexed on a thread of their own the first time they are asked about.
     */
    @Bean
    @ConditionalOnMissingBean
    public InstalledKiCadParts parallaxInstalledKiCadParts(KiCadProperties properties) {
        return new InstalledKiCadParts(properties.symbolLibraries().stream().map(Path::of).toList(),
                properties.footprintLibraries().stream().map(Path::of).toList(),
                command -> Thread.ofVirtual().name("parallax-kicad-index").start(command));
    }

    /** 🔎 Nexar's supply search: listed always, not configured until {@code jmouse.parallax.parts.nexar.*} names a client. */
    @Bean
    @ConditionalOnMissingBean
    public NexarPartSearch parallaxNexarPartSearch(PartSearchProperties properties, ObjectMapper mapper, Clock clock) {
        return new NexarPartSearch(properties.nexar(), mapper, clock);
    }

    /** 🔎 Every part search asked at once, and a found part stored in an owner's place. */
    @Bean
    @ConditionalOnMissingBean
    public PartSearch parallaxPartSearch(List<PartSearchProvider> providers, Catalogue catalogue, ObjectStore objects,
                                         TransactionTemplate transactions, PartSearchProperties properties) {
        return new PartSearch(providers, catalogue, objects, transactions, properties.limit());
    }
}
