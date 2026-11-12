package org.jmouse.cad.document.part;

import org.jmouse.core.io.Resource;

/**
 * A file a fetched document holds besides its text — a 3D model's geometry —, kept as it came and filed against the
 * document by name. Whose it is, is the document's {@code meta}.
 *
 * @param document the slug of the document that names it
 * @param name     what the document names it by: {@code SOIC-8_3.9x4.9mm_P1.27mm.step}
 * @param content  its bytes, opened when it is stored
 */
public record FetchedFile(String document, String name, Resource content) {
}
