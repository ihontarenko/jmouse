package org.jmouse.cad.plugin.suppliers;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.settings.Parameter;
import org.jmouse.settings.Section;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.suppliers.SupplierParts;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;
import org.jmouse.suppliers.DigiKeyDriver;

import java.util.List;
import java.util.stream.Stream;

/** Distributors — Mouser, DigiKey — as places parts are looked for, with their stock and prices, and as what a pcb is priced at. */
public final class SuppliersPlugin implements Plugin {

    public static final String ID = "cad-suppliers";

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/plugin/cad-suppliers/materials. */
    public static final SettingsSection<SupplierMaterialsTable> MATERIALS = SettingsSection.ofPlugin(ID, "materials", "Supplier materials", SupplierMaterialsTable.class);

    /** Its setting of how many offers one search asks each distributor for. */
    public static final String LIMIT = "limit";

    /** Its setting of how many pcbs a bill of materials is priced for. */
    public static final String PCBS = "pcbs";

    /** The most pcbs a bill of materials is priced for. */
    private static final int MOST_PCBS = 100_000;

    static final String MOUSER_KEY        = "mouser-key";
    static final String DIGI_KEY_CLIENT   = "digi-key-client";
    static final String DIGI_KEY_SECRET   = "digi-key-secret";
    static final String DIGI_KEY_SITE     = "digi-key-site";
    static final String DIGI_KEY_LANGUAGE = "digi-key-language";
    static final String DIGI_KEY_CURRENCY = "digi-key-currency";

    private static final String MOUSER            = "Mouser";
    private static final String DIGI_KEY          = "DigiKey";
    private static final String SEARCH_AND_PRICES = "Searches and prices";
    /** Its sections of parameters. */
    static final String MOUSER_SECTION = "mouser";
    static final String DIGI_KEY_SECTION = "digi-key";
    static final String SEARCHES_SECTION = "searches";

    private final List<SupplierParts> places;
    private final DocumentExport      materials;
    /** How many offers a search asks for while nobody changed it: what the server is configured with. */
    private final int                 usualLimit;
    /** The most any of the distributors answers one search with. */
    private final int                 mostLimit;
    private final Deployed            deployed;

    /** What the server was given, used while nobody set another. */
    public record Deployed(String mouserKey, String digiKeyClient, String digiKeySecret, DigiKeyDriver.Locale locale) {
    }

    public SuppliersPlugin(List<SupplierParts> places, DocumentExport materials, int usualLimit, int mostLimit, Deployed deployed) {
        this.deployed = deployed;
        this.places = List.copyOf(places);
        this.materials = materials;
        this.usualLimit = usualLimit;
        this.mostLimit = mostLimit;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Suppliers", SuppliersPlugin.class).withIcon(SuppliersPlugin.class, "suppliers.svg");
    }

    @Override
    public List<Section> settings() {
        return List.of(CadSettings.declarationOf(MATERIALS),
                Section.fixed(MOUSER_SECTION, MOUSER, null, List.of(
                        Parameter.secret(MOUSER_KEY, "Mouser API key", "The Search API key of a Mouser account: mouser.com, My Account, APIs.", deployed.mouserKey()))),
                Section.fixed(DIGI_KEY_SECTION, DIGI_KEY, null, List.of(
                        Parameter.text(DIGI_KEY_CLIENT, "DigiKey client ID", "Of an application at developer.digikey.com, with its Product Information API.",
                        deployed.digiKeyClient() == null ? "" : deployed.digiKeyClient()),
                        Parameter.secret(DIGI_KEY_SECRET, "DigiKey client secret", "The same application's secret.", deployed.digiKeySecret()),
                        Parameter.text(DIGI_KEY_SITE, "Site", "Whose stock and prices are asked for: a country's code, as US or UA.", deployed.locale().site()),
                        Parameter.text(DIGI_KEY_LANGUAGE, "Language", "What its descriptions are written in, as en.", deployed.locale().language()),
                        Parameter.text(DIGI_KEY_CURRENCY, "Currency", "What its prices are in, as USD or EUR.", deployed.locale().currency()))),
                Section.fixed(SEARCHES_SECTION, SEARCH_AND_PRICES, null, List.of(
                        Parameter.wholeNumber(LIMIT, "Offers per search",
                "How many offers one search asks each distributor for. Distributors count calls against a daily allowance.",
                usualLimit, 1, mostLimit, "offers"),
                        Parameter.wholeNumber(PCBS, "PCBs to price",
                        "How many PCBs a priced bill of materials buys for: it sets the quantity, so the price break.",
                        1, 1, MOST_PCBS, "PCBs"))));
    }

    @Override
    public void register(PluginRegistration registration) {
        places.forEach(place -> registration.contribute(PartSearchProvider.POINT, place));
        registration.contribute(DocumentExport.POINT, materials);
    }
}
