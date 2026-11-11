package org.jmouse.cad.management;

import org.jmouse.cad.store.about.EngineAbout;
import org.jmouse.cad.store.exchange.DocumentExchange;
import org.jmouse.cad.store.parts.PartSearch;
import org.jmouse.cad.store.simulation.AnalysisEngines;
import org.jmouse.plugin.Extensions;
import org.jmouse.cad.store.activity.CadActivity;
import org.jmouse.cad.store.simulation.CatalogueChecks;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.editing.LanguageEditing;
import org.jmouse.cad.store.library.PartLibrary;
import org.jmouse.cad.store.library.ShippedLibrary;
import org.jmouse.cad.store.simulation.Simulations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.JacksonModule;

import java.util.List;

/**
 * 🔌 jmCAD's REST API. Imported by a product, never auto-configured — the routes appear because somebody typed
 * {@code @Import}, and the same product then gates them:
 *
 * <pre>{@code
 * @Configuration
 * @Import(CadManagementConfiguration.class)
 * class CadAccessConfiguration {
 *
 *     @Bean
 *     ExternalAccessRules cadAccessRules() {
 *         return CadAccessRules.builder()
 *                 .capability(CadCapability.CATALOGUE_WRITE, "catalogue:write", "GLOBAL")
 *                 …
 *                 .build();
 *     }
 * }
 * }</pre>
 *
 * <p>⚠️ <strong>Never component-scanned.</strong> A scan of {@code org.jmouse} would mount every route here without the
 * rules — a route nobody declared is a route nobody gates. {@link CadAccessRules#build()} refuses to finish while any
 * handler is left without a rule or {@code @PublicEndpoint}, so the import and the rules arrive together or the boot
 * stops.</p>
 *
 * <p>⚠️ <strong>Plus two things the product owes.</strong> Its security chain lets {@code /jmouse/cad/**} through
 * (these routes decide for themselves), and it registers an access-target resolver for {@code DocumentOwner}.</p>
 */
@Configuration(proxyBeanMethods = false)
public class CadManagementConfiguration {

    @Bean
    public CatalogueController cadCatalogueController(CadCatalogue catalogue, ShippedLibrary library) {
        return new CatalogueController(catalogue, library);
    }

    @Bean
    public LibraryController cadLibraryController(PartLibrary library) {
        return new LibraryController(library);
    }

    @Bean
    public SimulationController cadSimulationController(Simulations simulations, CadActivity activity,
                                                        CatalogueChecks checks) {
        return new SimulationController(simulations, activity, checks);
    }

    @Bean
    public LanguageController cadLanguageController(LanguageEditing editing) {
        return new LanguageController(editing);
    }

    @Bean
    public AboutController cadAboutController(EngineAbout about) {
        return new AboutController(about);
    }

    @Bean
    public PluginController cadPluginController(Extensions extensions) {
        return new PluginController(extensions);
    }

    @Bean
    public ExchangeController cadExchangeController(DocumentExchange exchange) {
        return new ExchangeController(exchange);
    }

    @Bean
    public EngineController cadEngineController(AnalysisEngines engines) {
        return new EngineController(engines);
    }

    @Bean
    public PartsController cadPartsController(PartSearch search) {
        CadAccessRules.requireDecidedInside(PartsController.class);
        return new PartsController(search);
    }

    @Bean
    public CadProblemDetails cadProblemDetails() {
        return new CadProblemDetails();
    }

    /** 🧾 How a drawing an editor sends is read: its values' trees are answered, never taken. */
    @Bean
    public JacksonModule cadJson() {
        return CadJson.module();
    }

    /** 🔗 An owner addressed by two path segments reaches its handler as the one string a rule can name. */
    @Bean
    public WebMvcConfigurer cadOwnerAddresses() {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new OwnerAddressArgumentResolver());
            }
        };
    }
}
