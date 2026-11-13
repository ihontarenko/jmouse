package org.jmouse.settings;

/**
 * 🔐 How an application keeps a {@link ParameterKind#SECRET secret} parameter: sealed before they reach its
 * store, opened only for what uses it. Bound to its parameter by {@code context}, so a sealed value copied onto
 * another parameter does not open. An application without one keeps no secrets: changing one is refused, saying so.
 */
public interface SecretSealing {

    String seal(String secret, String context);

    String open(String sealed, String context);
}
