package org.jmouse.script.spring;

import org.jmouse.script.ScriptBudgets;
import org.jmouse.script.ScriptDocumentBinder;
import org.jmouse.script.ScriptLog;
import org.jmouse.script.ScriptReference;
import org.jmouse.script.ScriptRuntime;
import org.jmouse.script.ScriptTemplate;
import org.jmouse.script.StageDispatcher;
import org.jmouse.script.el.host.ScriptCatalogue;
import org.jmouse.script.el.host.ScriptHost;
import org.jmouse.script.spi.OwnTransaction;
import org.jmouse.script.spi.RealClass;
import org.jmouse.script.spi.ScriptDocuments;
import org.jmouse.script.spi.ScriptFacade;
import org.jmouse.script.spi.ScriptStage;
import org.jmouse.script.stage.StageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles the rule mechanism, so that adopting jMS is a dependency rather than a file to copy.
 *
 * <h2>⚠️ THE FIRST PRODUCT WIRED ALL OF THIS BY HAND, AND THE SECOND WAS ABOUT TO</h2>
 *
 * <p>Nine {@code @Bean} methods, three event listeners and two {@code CREATE TABLE}s, written out once
 * per product. The mechanism was extracted into a library precisely so two products would stop
 * carrying the same two thousand lines — and then the wiring became the thing being carried instead.
 * Ivan: <i>«нахера ліпить одне і те ж саме по всих проектах»</i>.</p>
 *
 * <p>So this module exists, and a product now contributes exactly what only it can know: its
 * {@link ScriptStage} beans and its {@link ScriptFacade} beans. Everything below is assembled from
 * them.</p>
 *
 * <h2>⚠️ THE ORDER IS A LINE, NOT A CIRCLE, AND THAT IS LOAD-BEARING</h2>
 *
 * <pre>
 *   host → binder → documents → runtime → dispatcher
 * </pre>
 *
 * <p>The tempting shape puts the host inside the runtime, and the container then refuses to start the
 * moment rules are stored, because binding a stored rule also needs the host:</p>
 *
 * <pre>
 *   ScriptDocumentBinder ──needs──&gt; ScriptRuntime ──needs──&gt; ScriptDocuments
 *            ^                                                      |
 *            +──────────────── StoredScriptDocuments &lt;──────────────+
 * </pre>
 *
 * <p>The cycle is real rather than an accident of wiring: <em>binding a rule</em> and <em>knowing
 * which rules there are</em> genuinely need each other if one object does both. What breaks it is
 * noticing the host needs <b>neither</b> — it is a catalogue and a set of ceilings, both facts about
 * this build rather than about anything stored. ⚠️ {@code @Lazy} would also have started, and would
 * have left a genuine circle in the design held apart by a proxy.</p>
 *
 * <h2>⚠️ EVERY BEAN IS {@code @ConditionalOnMissingBean}</h2>
 *
 * <p>A product with an opinion — a different ceiling, its own catalogue, a store that is not a
 * table — declares the one bean it cares about and keeps the rest. That is what makes this an
 * auto-configuration rather than a framework.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@AutoConfiguration
public class ScriptAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptAutoConfiguration.class);

    /**
     * Every moment this build declares.
     *
     * <p>⚠️ Spring collects the {@link ScriptStage} beans; the registry is only handed the list. Which
     * is why the library class carries no annotation and needs none — it was written to be given its
     * contents from the first day.</p>
     *
     * @param stages every stage bean on the classpath
     * @return the registry
     */
    @Bean
    @ConditionalOnMissingBean
    public StageRegistry stageRegistry(List<ScriptStage<?>> stages) {
        return new StageRegistry(stages);
    }

    /**
     * What a rule may reach, and what moments exist.
     *
     * <p>⚠️ The ceiling here is the WIDEST stage's figures, as a backstop. The number that actually
     * bounds a run is the STAGE's, passed as the request when a rule is bound for that stage — so a
     * rule assigned to a per-item moment is bound under fifty milliseconds even though this allows
     * five seconds. Leaving the host unbounded would mean a rule bound with no stage in hand had no
     * guard at all.</p>
     *
     * @param facades everything this build is willing to expose
     * @param stages  every moment this build declares
     * @return the host
     */
    @Bean
    @ConditionalOnMissingBean
    public ScriptHost scriptHost(List<ScriptFacade> facades, StageRegistry stages) {
        ScriptHost host = ScriptHost.builder()
                .catalogue(catalogueOf(facades, stages))
                .ceiling(ScriptBudgets.BACKGROUND)
                .build();

        LOGGER.info("jMS host ready: {} facade(s) {}, {} stage(s)",
                    facades.size(), facades.stream().map(ScriptFacade::name).sorted().toList(),
                    stages.names().size());

        return host;
    }

    /** Turns jMS text into something runnable, and says why when it cannot. */
    @Bean
    @ConditionalOnMissingBean
    public ScriptDocumentBinder scriptDocumentBinder(ScriptHost host) {
        return new ScriptDocumentBinder(host);
    }

    /** The body a new rule starts from — narrow, and guarded, by default. */
    @Bean
    @ConditionalOnMissingBean
    public ScriptTemplate scriptTemplate() {
        return new ScriptTemplate();
    }

    /**
     * What runs a bound rule.
     *
     * <p>⚠️ {@link ScriptDocuments} is {@code @ConditionalOnMissingBean}-supplied by whoever stores
     * rules; with nothing storing them the runtime is handed {@link ScriptDocuments#none()} and the
     * pipeline behaves exactly as it did before rules existed. That is the case the seams are allowed
     * to exist in before a store does.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public ScriptRuntime scriptRuntime(ScriptHost host, List<ScriptDocuments> documents) {
        return new ScriptRuntime(host, documents.isEmpty() ? ScriptDocuments.none() : documents.getFirst());
    }

    /** Which verb a moment gets, and the boundary each one runs inside. */
    @Bean
    @ConditionalOnMissingBean
    public StageDispatcher stageDispatcher(StageRegistry stages, ScriptRuntime runtime,
                                           OwnTransaction ownTransaction) {
        return new StageDispatcher(stages, runtime, ownTransaction);
    }

    /**
     * The three listeners that turn published events into rule runs.
     *
     * <p>⚠️ Registering this is what makes rules fire at all. A product that wants the mechanism
     * assembled and nothing listening — a migration, a test — excludes this one bean and keeps the
     * rest.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public ScriptEventBridge scriptEventBridge(StageDispatcher dispatcher) {
        return new ScriptEventBridge(dispatcher);
    }

    /**
     * ⚠️ A transaction of its own, and {@code REQUIRES_NEW} is the whole point of the bean.
     *
     * <p>An observation stage runs after the commit, when the finished transaction's resources are
     * still bound to the thread. A facade's write therefore <b>joins</b> a transaction that has already
     * ended and is never committed again: the write reports success, the rule logs that it happened,
     * and nothing is in the database.</p>
     *
     * <p>⚠️ {@code PROPAGATION_REQUIRED} here would compile, satisfy the interface, and put that bug
     * straight back.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public OwnTransaction scriptOwnTransaction(PlatformTransactionManager transactions) {
        TransactionTemplate template = new TransactionTemplate(transactions);

        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return work -> template.executeWithoutResult(status -> work.run());
    }

    /**
     * ⚠️ How to see past a proxy — the one thing the mechanism cannot work out for itself.
     *
     * <p>A facade with {@code @Transactional} methods is a CGLIB proxy, and Spring hands the proxy back
     * even from {@code return this}. Listing that object's public methods once put {@code Advised},
     * {@code getCallbacks}, {@code setTargetSource} and forty other pieces of plumbing on the editor's
     * help panel, as things a rule could call. Unwrapping is container-specific, so the library asks
     * rather than guesses — and this module is where the container is known.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public RealClass scriptRealClass() {
        return ClassUtils::getUserClass;
    }

    /** What the editor's help panel is built from. */
    @Bean
    @ConditionalOnMissingBean
    public ScriptReference scriptReference(StageRegistry stages, List<ScriptFacade> facades,
                                           RealClass realClass) {
        return new ScriptReference(stages, facades, realClass);
    }

    /**
     * What rules wrote, kept in memory.
     *
     * <p>⚠️ Two hundred lines, and a product that keeps the figure in editable settings declares its
     * own bean reading the setting on every write. The library takes a supplier precisely so that is
     * possible; a number fixed here would quietly make such a setting do nothing until a restart.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public ScriptLog scriptLog() {
        return new ScriptLog(() -> 200);
    }

    /**
     * Builds the catalogue from the contributed facades and the declared stages.
     *
     * <p>⚠️ <b>Two beans answering one facade name refuse to start.</b> Taking the last would mean a
     * household's {@code @catalogue} quietly becoming somebody else's — their rules would keep loading
     * and start meaning something new, which is the worst way for this to go wrong. Stage names are
     * checked the same way, in {@link StageRegistry}.</p>
     */
    private static ScriptCatalogue catalogueOf(List<ScriptFacade> facades, StageRegistry stages) {
        Map<String, ScriptFacade> byName = new LinkedHashMap<>();

        for (ScriptFacade facade : facades) {
            ScriptFacade declared = byName.putIfAbsent(facade.name(), facade);

            if (declared != null) {
                throw new IllegalStateException(
                        ("Two beans declare the script facade '@%s': %s and %s. A facade name is what a "
                                + "household's rules are written against, so one silently winning would "
                                + "change what those rules mean. Rename one.")
                                .formatted(facade.name(),
                                           declared.getClass().getName(),
                                           facade.getClass().getName()));
            }
        }

        ScriptCatalogue.Builder catalogue = ScriptCatalogue.builder();

        stages.names().forEach(catalogue::event);
        byName.values().forEach(facade -> catalogue.facade(facade.name(), facade.target()));

        return catalogue.build();
    }

}
