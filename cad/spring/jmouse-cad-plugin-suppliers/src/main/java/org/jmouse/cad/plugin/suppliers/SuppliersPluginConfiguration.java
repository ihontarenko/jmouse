package org.jmouse.cad.plugin.suppliers;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.suppliers.SupplierParts;
import org.jmouse.http.client.JdkHttpTransport;
import org.jmouse.plugin.PluginSettingValues;
import org.jmouse.plugin.PluginSettings;
import org.jmouse.suppliers.DigiKeyDriver;
import org.jmouse.suppliers.MouserDriver;
import org.jmouse.suppliers.SupplierAccess;
import org.jmouse.suppliers.SupplierCredentials;
import org.jmouse.suppliers.SupplierDriver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.stream.Stream;

/** 🧩 The suppliers plugin, on the classpath: one place per distributor, each over the credentials the server is given. */
@AutoConfiguration
@EnableConfigurationProperties(SuppliersPluginConfiguration.SuppliersProperties.class)
public class SuppliersPluginConfiguration {

    private static final int USUAL_LIMIT = 10;
    private static final Duration USUAL_TIMEOUT = Duration.ofSeconds(8);

    /**
     * {@code jmouse.cad.parts.suppliers.*}: what each distributor knows this server by; one left out is listed as not
     * configured.
     *
     * @param limit   how many offers a search asks each for while nobody changed it; 0 for 10
     * @param timeout how long one call waits; null for 8 seconds
     */
    @ConfigurationProperties("jmouse.cad.parts.suppliers")
    public record SuppliersProperties(Mouser mouser, DigiKey digiKey, int limit, Duration timeout) {

        /** @param address where Mouser is called; blank for its own */
        public record Mouser(String apiKey, URI address) {
        }

        /** @param address where DigiKey is called; blank for its own */
        public record DigiKey(String clientId, String clientSecret, URI address, String site, String language, String currency) {
        }

        public SuppliersProperties {
            mouser = mouser == null ? new Mouser(null, null) : mouser;
            digiKey = digiKey == null ? new DigiKey(null, null, null, null, null, null) : digiKey;
            limit = limit <= 0 ? USUAL_LIMIT : limit;
            timeout = timeout == null ? USUAL_TIMEOUT : timeout;
        }

        DigiKeyDriver.Locale digiKeyLocale() {
            DigiKeyDriver.Locale usual = DigiKeyDriver.Locale.UNITED_STATES;
            return new DigiKeyDriver.Locale(digiKey.site() == null ? usual.site() : digiKey.site(),
                    digiKey.language() == null ? usual.language() : digiKey.language(),
                    digiKey.currency() == null ? usual.currency() : digiKey.currency());
        }
    }

    /** What distributors are called through: it never follows a redirect, as Mouser's key is in the address; closed with the server. */
    @Bean(destroyMethod = "close")
    public JdkHttpTransport suppliersHttpTransport(SuppliersProperties properties) {
        return new JdkHttpTransport(properties.timeout(), JdkHttpTransport.Redirects.NEVER);
    }

    /** The plugin over one place per distributor; how many offers a search asks for is its setting, read at each search. */
    @Bean
    public SuppliersPlugin cadSuppliersPlugin(SuppliersProperties properties, PluginSettings settings,
                                              JdkHttpTransport suppliersHttpTransport) {
        PluginSettingValues values = settings.valuesOf(SuppliersPlugin.ID);
        IntSupplier limit = () -> Math.toIntExact(values.wholeNumber(SuppliersPlugin.LIMIT));
        SupplierDriver mouser = new MouserDriver(suppliersHttpTransport);
        SupplierDriver digiKey = new DigiKeyDriver(suppliersHttpTransport, Clock.systemUTC(), () -> new DigiKeyDriver.Locale(
                values.text(SuppliersPlugin.DIGI_KEY_SITE), values.text(SuppliersPlugin.DIGI_KEY_LANGUAGE), values.text(SuppliersPlugin.DIGI_KEY_CURRENCY)));
        List<SupplierParts> places = List.of(
                new SupplierParts(mouser, () -> new SupplierAccess(mouser.name(), properties.mouser().address(),
                        new SupplierCredentials.ApiKey(values.secret(SuppliersPlugin.MOUSER_KEY)), properties.timeout()), limit,
                        "A Mouser search API key is not set: Settings, Plugins, Suppliers."),
                new SupplierParts(digiKey, () -> new SupplierAccess(digiKey.name(), properties.digiKey().address(),
                        new SupplierCredentials.ClientCredentials(values.text(SuppliersPlugin.DIGI_KEY_CLIENT), values.secret(SuppliersPlugin.DIGI_KEY_SECRET)),
                        properties.timeout()), limit,
                        "A DigiKey application's client is not set: Settings, Plugins, Suppliers."));
        int most = Stream.of(mouser, digiKey).mapToInt(SupplierDriver::mostPerSearch).max().orElseThrow();
        IntSupplier pcbs = () -> Math.toIntExact(values.wholeNumber(SuppliersPlugin.PCBS));
        SupplierMaterialsExport materials = new SupplierMaterialsExport(places, pcbs,
                CadSettings.read(SuppliersPlugin.MATERIALS));
        return new SuppliersPlugin(places, materials, Math.min(properties.limit(), most), most, new SuppliersPlugin.Deployed(
                properties.mouser().apiKey(), properties.digiKey().clientId(), properties.digiKey().clientSecret(), properties.digiKeyLocale()));
    }
}
