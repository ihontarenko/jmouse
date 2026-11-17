package org.jmouse.cad.plugin.nexar;

import org.jmouse.settings.Parameter;
import org.jmouse.settings.Section;
import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.nexar.NexarParts;
import org.jmouse.cad.nexar.NexarSettings;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

import java.util.List;

/** Nexar's supply search (Octopart's data) as a place parts are looked for. */
public final class NexarPlugin implements Plugin {

    public static final String ID = "cad-nexar";

    /** Its sections: how it reaches Nexar, and how much a search asks for. */
    static final String CONNECTION = "connection";
    static final String SEARCHES = "searches";
    static final String CLIENT = "client-id";
    static final String SECRET = "client-secret";

    /** Its setting of how many parts one search asks Nexar for. */
    public static final String LIMIT = "limit";

    private final NexarParts parts;
    /** How many parts a search asks for while nobody changed it: what the server is configured with. */
    private final int        usualLimit;

    /** The client the server was given, used while nobody set another. */
    private final String deployedClient;
    private final String deployedSecret;

    public NexarPlugin(NexarParts parts, int usualLimit, String deployedClient, String deployedSecret) {
        this.deployedClient = deployedClient == null ? "" : deployedClient;
        this.deployedSecret = deployedSecret;
        this.parts = parts;
        this.usualLimit = usualLimit;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Nexar", NexarPlugin.class).withIcon(NexarPlugin.class, "nexar.svg");
    }

    @Override
    public List<Section> settings() {
        return List.of(
                Section.fixed(CONNECTION, "Connection", null, List.of(
                        Parameter.text(CLIENT, "Client ID", "Of an application at portal.nexar.com.", deployedClient),
                        Parameter.secret(SECRET, "Client secret", "The same application's secret.", deployedSecret))),
                Section.fixed(SEARCHES, "Searches", null, List.of(
                        Parameter.wholeNumber(LIMIT, "Parts per search",
                                "How many parts one search asks Nexar for. Each counts against the application's monthly allowance.",
                                usualLimit, 1, NexarSettings.MOST, "parts"))));
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(PartSearchProvider.POINT, parts);
    }
}
