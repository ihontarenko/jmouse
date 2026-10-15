package org.jmouse.parallax.store.computation;

import jakarta.persistence.PersistenceException;
import org.jmouse.formula.FormulaException;
import org.jmouse.parallax.authority.ParallaxAuthority;
import org.jmouse.parallax.authority.ParallaxCapability;
import org.jmouse.parallax.owner.ParallaxOwner;
import org.jmouse.parallax.authority.ParallaxPlaces;
import org.jmouse.parallax.store.catalogue.Catalogue;
import org.jmouse.parallax.jpa.catalogue.MathematicalObject;
import org.jmouse.parallax.jpa.computation.ComputationJob;
import org.jmouse.parallax.jpa.computation.ComputedDataset;
import org.jmouse.parallax.jpa.computation.JobPlacement;
import org.jmouse.parallax.jpa.computation.JobQueue;
import org.jmouse.parallax.jpa.computation.DatasetCache;
import org.jmouse.parallax.store.computation.ComputationViews.ComputationAnswer;
import org.jmouse.parallax.store.computation.ComputationViews.JobView;
import org.jmouse.parallax.store.computation.ComputationViews.Queued;
import org.jmouse.parallax.store.computation.ComputationViews.Ready;
import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Computations;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.limits.ParallaxLimits;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.store.subject.Subjects;
import org.jmouse.parallax.support.ParallaxIdentifiers;
import org.jmouse.parallax.store.support.ParallaxJson;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The computation layer's front door. A request is prepared (object resolved and authorized, settings checked,
 * cost estimated, fingerprint taken), answered from the cache when it can be, computed at once when it is light,
 * and queued as a job when it is not. The computation itself never runs inside a database transaction.
 */
public class Computing {

    /**
     * Bumped when a computation's output changes meaning, so every cached dataset from before is ignored. 4: a circuit's
     * columns are its own names with units — `out` [V], `R1` [A] —, no longer `V(out)` / `I(R1)`. 5: a frequency
     * response's too (`out gain` [dB], `out phase` [°]), and a sine or a square is a generator component.
     */
    private static final int FORMAT_VERSION = 5;
    /** The wall time a computation answered inline may take before it is handed to the queue instead. */
    private static final Duration INLINE_DEADLINE = Duration.ofSeconds(15);

    private final Catalogue catalogue;
    private final Subjects subjects;
    private final Computations computations;
    private final DatasetCache datasets;
    private final JobQueue jobs;
    private final DatasetCodec codec;
    private final ParallaxJson documents;
    private final ParallaxAuthority authority;
    private final ParallaxPlaces places;
    private final ParallaxLimits limits;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public Computing(Catalogue catalogue, Computations computations, DatasetCache datasets,
                     JobQueue jobs, DatasetCodec codec, ParallaxJson documents, ParallaxAuthority authority,
                     ParallaxPlaces places, ParallaxLimits limits, TransactionTemplate transactions, Clock clock,
                     Subjects subjects) {
        this.subjects = subjects;
        this.catalogue = catalogue;
        this.computations = computations;
        this.datasets = datasets;
        this.jobs = jobs;
        this.codec = codec;
        this.documents = documents;
        this.authority = authority;
        this.places = places;
        this.limits = limits;
        this.transactions = transactions;
        this.clock = clock;
    }

    /** A request, resolved: what will be computed, what it costs, and the key its dataset is kept under. */
    record Prepared(CompiledObject object, Computation computation, ComputationSettings settings, String fingerprint,
                    long cost, String objectId, JobPlacement placement, boolean publishedCatalogueObject) {

        /** An inline formula computed for nobody: answered, never stored or queued. */
        boolean sandbox() {
            return objectId == null && placement == null;
        }
    }

    public ComputationAnswer compute(ComputationRequest named) {
        ComputationRequest request = subjects.bind(named);
        Prepared prepared = transactions.execute(status -> prepare(request, true));
        authorize(prepared, request);
        Optional<Ready> cached = cached(prepared);
        if (cached.isPresent()) {
            return cached.get();
        }
        long budget = limits.inlineComputationBudget();
        if (request.mode() == ComputationRequest.Mode.AUTOMATIC && prepared.cost() <= budget) {
            Optional<Ready> ready = runWithinDeadline(prepared);
            if (ready.isPresent()) {
                return ready.get();
            }
            if (prepared.sandbox()) {
                throw ParallaxProblem.limit("This formula takes too long to try without a workspace — sign in to "
                        + "compute it in yours, or lower its resolution.");
            }
        }
        return new Queued(enqueue(prepared, request));
    }

    /**
     * A frame of an animation: answered from the cache or computed now. One too heavy for the inline budget is not
     * queued — an animation of jobs is not an animation — and is simply absent.
     */
    public Optional<Ready> computeInline(ComputationRequest named) {
        ComputationRequest request = subjects.bind(named);
        Prepared prepared = transactions.execute(status -> prepare(request, true));
        authorize(prepared, request);
        Optional<Ready> cached = cached(prepared);
        if (cached.isPresent()) {
            return cached;
        }
        if (prepared.cost() > limits.inlineComputationBudget()) {
            return Optional.empty();
        }
        return runWithinDeadline(prepared);
    }

    /**
     * Computes a request judged light, but gives up after {@link #INLINE_DEADLINE}: the estimate is a typical cost, and
     * an integral that will not settle costs thousands of times more. Empty when it overran — the caller queues it,
     * leaves the frame out, or refuses.
     */
    Optional<Ready> runWithinDeadline(Prepared prepared) {
        try {
            return Optional.of(run(prepared, ComputationProgress.within(INLINE_DEADLINE)));
        } catch (ComputationProgress.ComputationCancelled overran) {
            return Optional.empty();
        }
    }

    /** The cost estimate and fingerprint of a request, without computing it: what the interface shows before it asks. */
    public Map<String, Object> estimate(ComputationRequest named) {
        ComputationRequest request = subjects.bind(named);
        Prepared prepared = transactions.execute(status -> prepare(request, true));
        Map<String, Object> estimate = new LinkedHashMap<>();
        estimate.put("cost", prepared.cost());
        estimate.put("inline", prepared.cost() <= limits.inlineComputationBudget());
        estimate.put("cached", datasets.contains(prepared.fingerprint()));
        estimate.put("fingerprint", prepared.fingerprint());
        estimate.put("settings", prepared.settings().asMap());
        return estimate;
    }

    Prepared prepare(ComputationRequest request, boolean checkReadable) {
        if (request.representation() == null) {
            throw ParallaxProblem.invalid("A representation is required.");
        }
        CompiledObject compiled;
        String objectId = null;
        JobPlacement placement = placementOf(request, null);
        boolean published = false;
        ObjectDefinition definition;
        if (request.objectId() != null) {
            MathematicalObject object = checkReadable ? catalogue.requireReadable(request.objectId())
                    : catalogue.requireReadableUnchecked(request.objectId());
            if (request.definition() != null) {
                if (!(request.definition() instanceof ObjectDefinition.Circuit circuit)) {
                    throw ParallaxProblem.invalid("Only a circuit can be varied in the emulator.");
                }
                compiled = catalogue.compileVariant(object, circuit, request.values());
            } else {
                compiled = catalogue.compile(object, request.values());
            }
            objectId = object.getId();
            definition = compiled.definition();
            published = request.definition() == null && object.isCatalogue()
                    && object.getStatus() == MathematicalObject.Status.PUBLISHED;
            placement = placementOf(request, object);
            if (request.definition() != null && placement == null) {
                // A published circuit may be tried without saving, under the same budget as an inline formula.
                objectId = null;
            }
        } else if (request.definition() != null) {
            compiled = catalogue.compileChecked(request.definition(), request.parameters(), request.values());
            definition = request.definition();
        } else {
            throw ParallaxProblem.invalid("Name a stored object or define one inline.");
        }
        try {
            compiled = compiled.withExactIntegers(request.integerValues());
        } catch (IllegalArgumentException exception) {
            throw ParallaxProblem.invalid(exception.getMessage());
        }
        Computation computation = computations.forObject(request.representation(), definition)
                .orElseThrow(() -> ParallaxProblem.invalid(request.representation() + " does not apply to a "
                        + definition.objectKind() + "."));
        ComputationSettings settings = ComputationSettings.resolve(computation.settings(), request.settings());
        // A computation estimates per evaluation; an expression that is itself an integral or a sum multiplies every one.
        long perEvaluation = computation.estimatedCost(compiled, settings);
        long weight = compiled.evaluationWeight();
        long cost = perEvaluation > Long.MAX_VALUE / weight ? Long.MAX_VALUE : perEvaluation * weight;
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("format", FORMAT_VERSION);
        identity.put("definition", definition);
        if (definition instanceof ObjectDefinition.Circuit) {
            identity.put("componentSources", org.jmouse.parallax.object.ComponentHierarchy.sources(compiled));
        }
        identity.put("parameters", compiled.parameterValues());
        identity.put("integerValues", compiled.exactIntegerValues());
        identity.put("representation", request.representation());
        identity.put("settings", settings.asMap());
        String fingerprint = ParallaxIdentifiers.sha256(documents.write(identity));
        return new Prepared(compiled, computation, settings, fingerprint, cost, objectId, placement, published);
    }

    private void authorize(Prepared prepared, ComputationRequest request) {
        if (prepared.placement() != null) {
            authority.require(ParallaxCapability.COMPUTE, places.requireOwner(prepared.placement().realm()));
            return;
        }
        boolean anonymousAllowed = limits.anonymousComputationAllowed();
        if (prepared.publishedCatalogueObject() && (anonymousAllowed || authority.actor().isPresent())
                && request.mode() == ComputationRequest.Mode.AUTOMATIC) {
            if (authority.actor().isPresent()) {
                authority.requireGlobally(ParallaxCapability.COMPUTE);
            }
            return;
        }
        // The formula playground: a formula typed here and now, with no workspace. It is answered within the
        // inline budget and never stored, so nobody's cache fills with what a passer-by typed.
        if (prepared.sandbox() && (anonymousAllowed || authority.actor().isPresent())
                && request.mode() == ComputationRequest.Mode.AUTOMATIC) {
            if (prepared.cost() > limits.inlineComputationBudget()) {
                throw ParallaxProblem.limit("This formula is too heavy to try without a workspace — sign in to compute "
                        + "it in yours, or lower its resolution.");
            }
            return;
        }
        throw ParallaxProblem.refused("Computing this needs a workspace to compute it in.");
    }

    private Optional<Ready> cached(Prepared prepared) {
        return transactions.execute(status -> datasets.findByFingerprint(prepared.fingerprint()).map(dataset -> {
            datasets.recordUse(dataset.getId(), clock.instant());
            return new Ready(dataset.getId(), dataset.getFingerprint(), dataset.getRepresentation(), dataset.getFormat(), true,
                    dataset.getComputeMillis(), dataset.getElementCount(), codec.decode(dataset.getPayload()));
        }));
    }

    /** Computes and stores; also the worker's path. */
    Ready run(Prepared prepared, ComputationProgress progress) {
        long started = System.nanoTime();
        Dataset dataset;
        try {
            dataset = prepared.computation().compute(prepared.object(), prepared.settings(), progress);
        } catch (FormulaException exception) {
            throw ParallaxProblem.invalid("invalid-formula", exception.getMessage());
        }
        long millis = (System.nanoTime() - started) / 1_000_000;
        DatasetCodec.Encoded encoded = codec.encode(dataset);
        long bytes = encoded.json().getBytes(StandardCharsets.UTF_8).length;
        if (bytes > limits.maximumDatasetBytes()) {
            throw ParallaxProblem.limit("The dataset would be " + bytes / 1024 + " KiB, over the installation's limit; "
                    + "lower the resolution or the sample count.");
        }
        String format = dataset.getClass().getSimpleName().replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
        if (prepared.sandbox()) {
            return new Ready(null, prepared.fingerprint(), prepared.computation().kind(), format, false, millis,
                    dataset.elementCount(), encoded.json());
        }
        ComputedDataset stored = store(new ComputedDataset(ParallaxIdentifiers.newIdentifier(), prepared.fingerprint(),
                prepared.computation().kind(), format, prepared.objectId(), encoded.compressed(), bytes,
                dataset.elementCount(), millis, clock.instant()));
        return new Ready(stored.getId(), stored.getFingerprint(), stored.getRepresentation(), format, false, millis,
                dataset.elementCount(), encoded.json());
    }

    private ComputedDataset store(ComputedDataset dataset) {
        try {
            return transactions.execute(status -> datasets.store(dataset));
        } catch (DataIntegrityViolationException | PersistenceException alreadyStored) {
            return transactions.execute(status -> datasets.findByFingerprint(dataset.getFingerprint()).orElseThrow());
        }
    }

    private static final EnumSet<ComputationJob.Status> ACTIVE = EnumSet.of(ComputationJob.Status.QUEUED, ComputationJob.Status.RUNNING);

    /**
     * Queues a heavy computation. One for a workspace runs there; one of a published catalogue object asked for
     * without a workspace joins the catalogue's public queue — its dataset is public anyway, and the cache shares
     * it with everyone after. The public queue is bounded so an anonymous reader cannot fill it.
     */
    private JobView enqueue(Prepared prepared, ComputationRequest request) {
        return enqueue(prepared, request, false);
    }

    JobView enqueue(Prepared prepared, ComputationRequest request, boolean warmup) {
        if (prepared.cost() > limits.maximumJobCost()) {
            throw ParallaxProblem.limit("This computation is beyond what the installation queues; lower its resolution.");
        }
        boolean publicJob = prepared.placement() == null;
        if (publicJob && !prepared.publishedCatalogueObject()) {
            throw ParallaxProblem.invalid("An object outside the published catalogue is computed in a workspace: name it.");
        }
        String requester = authority.actor().orElse(null);
        return transactions.execute(status -> jobs.findByFingerprint(prepared.fingerprint(), ACTIVE)
                .map(JobView::of)
                .orElseGet(() -> {
                    if (publicJob && !warmup && jobs.countCatalogueQueue(ACTIVE) >= limits.publicQueueLimit()) {
                        throw ParallaxProblem.busy("The catalogue's queue is full at the moment; this result will be ready "
                                + "once it frees — try again in a minute, or sign in to compute it in your workspace.");
                    }
                    return JobView.of(jobs.add(new ComputationJob(ParallaxIdentifiers.newIdentifier(), prepared.placement(),
                            request.sceneId(), prepared.objectId(), requester,
                            prepared.computation().kind(), documents.write(request), prepared.fingerprint(),
                            warmup || prepared.cost() > 100_000_000 ? 0 : 1, limits.jobAttempts(),
                            clock.instant())));
                }));
    }

    /** Whether a request's dataset is already kept, and how heavy it is — for the catalogue warm-up. */
    Optional<Prepared> uncached(ComputationRequest request) {
        Prepared prepared = transactions.execute(status -> prepare(request, false));
        if (datasets.contains(prepared.fingerprint())) {
            return Optional.empty();
        }
        return Optional.of(prepared);
    }

    long inlineBudget() {
        return limits.inlineComputationBudget();
    }

    /** A stored dataset by id, for a finished job; readable by whoever may read the job. */
    public Ready dataset(String datasetId) {
        return transactions.execute(status -> datasets.find(datasetId).map(dataset -> new Ready(dataset.getId(),
                        dataset.getFingerprint(), dataset.getRepresentation(), dataset.getFormat(), true,
                        dataset.getComputeMillis(), dataset.getElementCount(), codec.decode(dataset.getPayload())))
                .orElseThrow(() -> ParallaxProblem.notFound("The dataset")));
    }

    ComputationRequest requestOf(String storedRequest) {
        return documents.read(storedRequest, ComputationRequest.class);
    }

    /**
     * Where a request is computed: in the realm of the owned object it names, or else in the realm of the owner the
     * request names — the outermost link of that owner's chain, as the host reports it; null when it names neither
     * (the catalogue's queue, the playground). The owner it names, when it names one, is what the job is attributed to.
     *
     * @param object the stored object computed, or null for none
     */
    private JobPlacement placementOf(ComputationRequest request, MathematicalObject object) {
        ParallaxOwner realm;
        if (object != null && !object.isCatalogue()) {
            realm = object.owner().orElseThrow();
        } else if (request.owner() != null) {
            realm = places.requireOwner(request.owner()).chain().getFirst();
        } else {
            return null;
        }
        ParallaxOwner owner = request.owner() == null ? realm : request.owner();
        return new JobPlacement(owner, realm);
    }
}
