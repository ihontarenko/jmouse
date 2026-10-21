package org.jmouse.parallax.management;

import org.jmouse.parallax.Computations;
import org.jmouse.parallax.live.LiveActivity;
import org.jmouse.parallax.authority.ParallaxAuthority;
import org.jmouse.parallax.spice.SpiceModelCarriers;
import org.jmouse.parallax.spice.SpiceSimulator;
import org.jmouse.parallax.store.catalogue.Catalogue;
import org.jmouse.parallax.store.catalogue.ComponentModels;
import org.jmouse.parallax.store.kicad.KiCadExports;
import org.jmouse.parallax.store.kicad.KiCadImports;
import org.jmouse.parallax.store.part.PartSearch;
import org.jmouse.parallax.store.catalogue.Instruments;
import org.jmouse.parallax.store.catalogue.ShippedObjects;
import org.jmouse.parallax.store.collection.Collections;
import org.jmouse.parallax.store.computation.CatalogueWarmup;
import org.jmouse.parallax.store.computation.Computing;
import org.jmouse.parallax.store.computation.Jobs;
import org.jmouse.parallax.store.document.DocumentEditing;
import org.jmouse.parallax.store.evaluation.Evaluations;
import org.jmouse.parallax.store.formula.FormulaGuide;
import org.jmouse.parallax.store.formula.FormulaNotations;
import org.jmouse.parallax.store.scene.Comparisons;
import org.jmouse.parallax.store.scene.Scenes;
import org.jmouse.parallax.store.statistics.ParallaxStatistics;
import org.jmouse.parallax.store.subject.Subjects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Optional;

/**
 * 🔌 Parallax's REST API. Imported by a product, never auto-configured — the routes appear because somebody typed
 * {@code @Import}, and the same product then gates them:
 *
 * <pre>{@code
 * @Configuration
 * @Import(ParallaxManagementConfiguration.class)
 * class ParallaxAccessConfiguration {
 *
 *     @Bean
 *     ExternalAccessRules parallaxAccessRules() {
 *         return ParallaxAccessRules.builder()
 *                 .capability(ParallaxCapability.CATALOGUE_WRITE, "catalogue:write", "GLOBAL")
 *                 …
 *                 .build();
 *     }
 * }
 * }</pre>
 *
 * <p>⚠️ <strong>Never component-scanned.</strong> A scan of {@code org.jmouse} would mount every route here without
 * the rules, which is the one failure the access engine cannot see: a route nobody declared is a route nobody
 * gates. {@link ParallaxAccessRules#build()} refuses to finish while any handler here is left without a rule or
 * {@code @PublicEndpoint}, so the import and the rules arrive together or the boot stops.</p>
 *
 * <p>⚠️ <strong>Plus three things the product owes.</strong> Its security chain lets
 * {@code /jmouse/parallax/**} through (these routes decide for themselves), and it registers access-target resolvers
 * for {@code ParallaxOwner}, {@code Scene} and {@code Comparison} — see {@link ParallaxAccessRules}.</p>
 */
@Configuration(proxyBeanMethods = false)
public class ParallaxManagementConfiguration {

    @Bean
    public CatalogueController parallaxCatalogueController(Catalogue catalogue, Instruments instruments,
                                                           Computations computations, ComponentModels componentModels,
                                                           ShippedObjects shippedObjects) {
        return new CatalogueController(catalogue, instruments, computations, componentModels, shippedObjects);
    }

    @Bean
    public ComputationController parallaxComputationController(Computing computing, Jobs jobs) {
        return new ComputationController(computing, jobs);
    }

    @Bean
    public DocumentController parallaxDocumentController(DocumentEditing editing) {
        return new DocumentController(editing);
    }

    /**
     * 🔁 SPICE in and out.
     *
     * @param carriers which component a SPICE model becomes when its name names none; the shipped catalogue's by default
     */
    @Bean
    public SpiceController parallaxSpiceController(ComponentModels componentModels,
                                                   ObjectProvider<SpiceModelCarriers> carriers, ParallaxAuthority authority,
                                                   ObjectProvider<SpiceSimulator> simulator) {
        return new SpiceController(componentModels, carriers.getIfAvailable(SpiceModelCarriers::standard), authority,
                Optional.ofNullable(simulator.getIfAvailable()));
    }

    /** 📥📤 KiCad in and out: a design's sheets imported into an owner's place, a circuit written as a schematic. */
    @Bean
    public KiCadController parallaxKiCadController(KiCadImports imports, KiCadExports exports) {
        return new KiCadController(imports, exports);
    }

    /** 🔎 Parts searched for beyond the reader's library, and one found stored in an owner's place. */
    @Bean
    public PartSearchController parallaxPartSearchController(PartSearch parts) {
        return new PartSearchController(parts);
    }

    @Bean
    public FormulaController parallaxFormulaController(FormulaGuide guide, FormulaNotations notations) {
        return new FormulaController(guide, notations);
    }

    @Bean
    public SubjectController parallaxSubjectController(Subjects subjects) {
        return new SubjectController(subjects);
    }

    @Bean
    public SceneController parallaxSceneController(Scenes scenes, Comparisons comparisons) {
        return new SceneController(scenes, comparisons);
    }

    @Bean
    public CollectionController parallaxCollectionController(Collections collections) {
        return new CollectionController(collections);
    }

    /**
     * 🛠️ The machinery's administration.
     *
     * @param liveActivity the live module's count of running circuits; without that module, none run
     */
    @Bean
    public ParallaxAdministrationController parallaxAdministrationController(Jobs jobs, CatalogueWarmup warmup,
                                                                             Evaluations evaluations,
                                                                             ParallaxStatistics statistics,
                                                                             ObjectProvider<LiveActivity> liveActivity) {
        return new ParallaxAdministrationController(jobs, warmup, evaluations, statistics,
                liveActivity.getIfAvailable(() -> () -> 0));
    }

    @Bean
    public ParallaxProblemDetails parallaxProblemDetails() {
        return new ParallaxProblemDetails();
    }

    /**
     * 🔗 An owner addressed as two path segments reaches a handler — and the access guard around it — as one argument.
     *
     * @return the configurer adding {@link OwnerAddressArgumentResolver}
     */
    @Bean
    public WebMvcConfigurer parallaxOwnerAddresses() {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new OwnerAddressArgumentResolver());
            }
        };
    }
}
