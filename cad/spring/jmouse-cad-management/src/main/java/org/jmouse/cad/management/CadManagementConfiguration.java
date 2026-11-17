package org.jmouse.cad.management;

import org.jmouse.cad.store.about.EngineAbout;
import org.jmouse.cad.store.files.DocumentUnderlays;
import org.jmouse.documentation.Documentation;
import org.jmouse.cad.store.settings.ReaderSettings;
import org.jmouse.settings.Settings;
import org.jmouse.cad.store.plugin.EnginePluginSettings;
import org.jmouse.cad.store.exchange.DocumentExchange;
import org.jmouse.cad.store.exchange.RoutingBoards;
import org.jmouse.cad.store.settings.SettingsTree;
import org.jmouse.cad.store.parts.PartSearch;
import org.jmouse.cad.store.simulation.AnalysisEngines;
import org.jmouse.plugin.Extensions;
import org.jmouse.cad.store.activity.CadActivity;
import org.jmouse.cad.store.simulation.CatalogueChecks;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.project.CadProjects;
import org.jmouse.cad.store.editing.LanguageEditing;
import org.jmouse.cad.store.library.PartLibrary;
import org.jmouse.cad.store.showcase.FeaturedDesigns;
import org.jmouse.cad.store.simulation.Simulations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;

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
    CadJsonAnswers cadJsonAnswers(JsonMapper hostMapper) {
        return new CadJsonAnswers(hostMapper);
    }

    @Bean
    public CatalogueController cadCatalogueController(CadCatalogue catalogue) {
        return new CatalogueController(catalogue);
    }

    @Bean
    public ProjectsController cadProjectsController(CadProjects projects) {
        return new ProjectsController(projects);
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
    public ShowcaseController cadShowcaseController(CadCatalogue catalogue, PartLibrary library, Simulations simulations,
                                                    FeaturedDesigns featured, DocumentExchange exchange) {
        return new ShowcaseController(catalogue, library, simulations, featured, exchange);
    }

    @Bean
    public AboutController cadAboutController(EngineAbout about) {
        return new AboutController(about);
    }

    @Bean
    public DocumentationController cadDocumentationController(Documentation documentation) {
        return new DocumentationController(documentation);
    }

    @Bean
    public UnderlaysController cadUnderlaysController(DocumentUnderlays underlays) {
        CadAccessRules.requireDecidedInside(UnderlaysController.class);
        return new UnderlaysController(underlays);
    }

    @Bean
    public PluginController cadPluginController(Extensions extensions) {
        return new PluginController(extensions);
    }

    @Bean
    public SettingsController cadSettingsController(SettingsTree tree, EnginePluginSettings settings, Settings store, ReaderSettings reader) {
        CadAccessRules.requireDecidedInside(SettingsController.class);
        return new SettingsController(tree, settings, store, reader);
    }

    @Bean
    public RoutingController cadRoutingController(RoutingBoards boards, LanguageEditing editing, CadJsonAnswers answers) {
        return new RoutingController(boards, editing, answers);
    }

    @Bean
    public ExchangeController cadExchangeController(DocumentExchange exchange, LanguageEditing editing) {
        return new ExchangeController(exchange, editing);
    }

    @Bean
    public ServerFoldersController cadServerFoldersController(EnginePluginSettings settings) {
        CadAccessRules.requireDecidedInside(ServerFoldersController.class);
        return new ServerFoldersController(settings);
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
