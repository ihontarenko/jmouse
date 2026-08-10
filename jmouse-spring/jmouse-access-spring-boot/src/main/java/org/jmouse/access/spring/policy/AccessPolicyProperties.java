package org.jmouse.access.spring.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Where an application keeps its policy files, and what to call what they add up to.
 *
 * <pre>
 * jmouse:
 *   access:
 *     policy:
 *       name: innoventa
 *       locations:
 *         - classpath:policy/bootstrap.jmp
 *         - classpath:policy/*.jmp
 * </pre>
 *
 * <p>⚠️ <strong>No locations means the feature is off.</strong> Not "off and warning", not "on with
 * an empty policy" — no bean appears at all, and an application that never wanted declarative grants
 * carries none of the machinery. A location that is configured and matches nothing is the opposite
 * case and refuses to start, because somebody asked for a file that is not there.
 */
@ConfigurationProperties(prefix = AccessPolicyProperties.PREFIX)
public class AccessPolicyProperties {

    public static final String PREFIX = "jmouse.access.policy";

    /**
     * Where the {@code .jmp} files are — a resource location each, patterns allowed.
     *
     * <p>Read in order. Which file said what does not decide anything (deny wins over every allow
     * regardless of where either was written), but it decides what a reader meets first.
     */
    private List<String> locations = new ArrayList<>();

    /**
     * What the merged policy is called.
     *
     * <p>Carried into every grant's provenance, so the control room says <em>granted by policy
     * innoventa</em> rather than showing an origin of nothing.
     */
    private String name = "policy";

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
