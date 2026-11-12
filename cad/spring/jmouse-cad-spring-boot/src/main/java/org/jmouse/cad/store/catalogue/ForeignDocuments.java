package org.jmouse.cad.store.catalogue;

import org.jmouse.cad.library.DocumentIndex;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The documents of another product's file, read in place and kept nowhere, found before the store's: a KiCad design's
 * components, symbols and footprints are what its circuit's parts name, and none of them is stored. A reference they do
 * not hold is the store's to answer.
 */
public final class ForeignDocuments implements PartDocuments {

    private final Map<String, ReadDocument> read = new LinkedHashMap<>();
    private final PartDocuments             beside;

    /**
     * @param documents the file's documents, read
     * @param beside    where what the file does not hold is found: the catalogue, as the reader sees it
     */
    public ForeignDocuments(List<ReadDocument> documents, PartDocuments beside) {
        documents.forEach(document -> read.put(document.index().slug(), document));
        this.beside = beside;
    }

    /** The file's documents, in the order they were read. */
    public List<ReadDocument> documents() {
        return List.copyOf(read.values());
    }

    @Override
    public Optional<ReadDocument> resolve(String reference) {
        ReadDocument held = read.get(DocumentIndex.slugOf(reference));
        return held != null ? Optional.of(held) : beside.resolve(reference);
    }

    @Override
    public Optional<String> slugOf(String reference) {
        return read.containsKey(DocumentIndex.slugOf(reference)) ? Optional.of(reference) : beside.slugOf(reference);
    }
}
