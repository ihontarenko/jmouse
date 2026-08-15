/**
 * The one thing a management screen does that is not reading.
 *
 * <p>Its own package rather than a fifth interface in {@code org.jmouse.ai.view}, and the separation is
 * the point that package makes at length: everything under {@code view} reads, and a reviewer should be
 * able to establish that by looking at where a type lives rather than by reading it. A write port
 * sitting among them would cost that property its whole value.
 *
 * <p><strong>Optional, like the controllers over it.</strong> An application whose provider settings are
 * a property has nothing to administer;
 * {@link org.jmouse.ai.administration.ProviderAdministration#unavailable()} is what it gets, and it
 * refuses each write with a sentence saying which arrangement it is in.
 *
 * <p>⚠️ Nothing here returns a key. A credential travels in on a draft and never comes back out —
 * {@link org.jmouse.ai.administration.ProviderAdministration.Configuration} has no field for one.
 */
package org.jmouse.ai.administration;
