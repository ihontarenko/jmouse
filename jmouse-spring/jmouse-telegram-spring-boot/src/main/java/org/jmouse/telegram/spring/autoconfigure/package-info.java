/**
 * The autoconfigurations, one per thing that can be switched on.
 *
 * <p>⚠️ When these do not register, the symptom is <strong>silence</strong> — no error, no warning, an
 * application that starts and simply never sends anything.
 * {@link org.jmouse.telegram.spring.autoconfigure.TelegramDiagnostics} logs one line at startup for
 * exactly that reason: if the line is absent, the autoconfiguration did not run, and that is the fact
 * to establish before reading any Java.
 */
package org.jmouse.telegram.spring.autoconfigure;
