/**
 * Buttons.
 *
 * <p>⚠️ Everything here needs {@link org.jmouse.telegram.Capability#REPLY_MARKUP}, which only a bot
 * has — buttons are a Bot API concept with no equivalent on a user account. A gateway asked to attach
 * one on behalf of a user identity refuses rather than sending the message without them, because a
 * notification whose buttons silently vanished reads as a product defect.
 */
package org.jmouse.telegram.markup;
