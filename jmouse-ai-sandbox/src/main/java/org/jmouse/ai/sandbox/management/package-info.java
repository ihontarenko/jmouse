/**
 * The fourth entry point: the optional management endpoints, answered off the sandbox's own trail.
 *
 * <p>Shows the two halves of the split that makes that module worth having separately — the read ports
 * ship in {@code jmouse-ai} and are answered here from a recording trace nobody designed for them, and
 * the controllers are four thin methods over those ports that cannot reach a handler.
 *
 * <p>Run it with {@code mvn exec:java -Dexec.mainClass=org.jmouse.ai.sandbox.management.ManagementSandbox}.
 */
package org.jmouse.ai.sandbox.management;
