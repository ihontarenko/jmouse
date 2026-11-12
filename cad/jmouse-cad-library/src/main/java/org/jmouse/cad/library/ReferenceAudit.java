package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.TechnicalClass;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Whether documents name each other as they are meant to: by code name. A document a technical code names
 * ({@link TechnicalClass}: a footprint, a model, a symbol, a portrait, a frame) is named by that code wherever another
 * names it — a slug is the store's address, not a name —, every reference names a document that is held, and no two
 * documents of one set share a code. Run over the library's shipped documents and over a product's catalogue.
 */
public final class ReferenceAudit {

    private ReferenceAudit() {
    }

    /**
     * What is wrong, one line a fault; empty where nothing is.
     *
     * @param documents the documents checked, whose codes must be their own
     * @param held      the document a word names, among these and whatever they may name besides
     */
    public static List<String> faults(List<DocumentNode> documents, Function<String, Optional<DocumentNode>> held) {
        List<String> faults = new ArrayList<>();
        Map<String, String> holderOfCode = new LinkedHashMap<>();
        for (DocumentNode document : documents) {
            DocumentIndex index = DocumentIndex.of(document);
            if (index.code() != null) {
                String earlier = holderOfCode.putIfAbsent(index.code(), index.slug());
                if (earlier != null) {
                    faults.add(index.slug() + " has the code " + index.code() + ", which " + earlier + " has too");
                }
            }
            for (DocumentIndex.Reference reference : index.references()) {
                String word = DocumentIndex.slugOf(reference.slug());
                Optional<DocumentNode> named = held.apply(word);
                if (named.isEmpty()) {
                    faults.add(index.slug() + " names " + word + " (" + reference.relation() + "), which nothing holds");
                    continue;
                }
                String code = DocumentIndex.of(named.get()).code();
                if (code != null && TechnicalClass.ofCode(word).isEmpty()) {
                    faults.add(index.slug() + " names " + word + " by its slug; its code is " + code);
                }
            }
        }
        return faults;
    }
}
