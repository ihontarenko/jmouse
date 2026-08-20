/**
 * Settings that decide how this application's AI behaves — declared in code, overridden in a row.
 *
 * <p>The first of them is a system prompt, and it is what the package was written for. A prompt is
 * content rather than code: it is rewritten far more often than anything around it, by whoever is
 * watching how the assistant actually answers, and making that a deploy means it is not done. But it
 * is also not <em>configuration</em> in the sense a key is — it must exist, in a good version, on an
 * installation nobody has configured at all.
 *
 * <p>So a product declares a {@link org.jmouse.ai.preferences.PreferenceDefinition} carrying the
 * shipped text, and {@link org.jmouse.ai.preferences.AiPreferences} answers with an override where one
 * was written and the declaration otherwise. Nothing is seeded, nothing breaks on a fresh database, and
 * <em>reset</em> means something.
 */
package org.jmouse.ai.preferences;
