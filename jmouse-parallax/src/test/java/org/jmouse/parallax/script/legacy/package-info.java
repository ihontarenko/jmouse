/**
 * ⚠️ <strong>A frozen copy of jMX's hand-written reader, as it stood at commit 21b5e9e1</strong> — before any of it was
 * moved onto the engine's parsers. It is the reference {@code JmxReadingEquivalenceTest} measures the reader against,
 * and nothing else reads with it.
 *
 * <p>It lives in the tests, apart from the reader, on purpose: the first reference shared the reader's own block
 * classes, so every block moved onto the engine was compared with itself. Do not edit these files; delete the package
 * when the test switches to a recorded snapshot.</p>
 */
package org.jmouse.parallax.script.legacy;
