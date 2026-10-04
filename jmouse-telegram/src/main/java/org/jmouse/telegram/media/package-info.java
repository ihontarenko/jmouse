/**
 * Attachments: what a file is, and where its bytes come from.
 *
 * <p>⚠️ The distinction that catches people is {@link org.jmouse.telegram.media.MediaKind#PHOTO}
 * versus {@link org.jmouse.telegram.media.MediaKind#DOCUMENT} — the first is recompressed by Telegram
 * and shown inline, the second arrives byte-for-byte. For a screenshot somebody is meant to read, the
 * second is almost always what was wanted.
 */
package org.jmouse.telegram.media;
