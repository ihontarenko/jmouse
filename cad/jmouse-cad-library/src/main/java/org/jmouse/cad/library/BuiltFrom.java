package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.simulation.CatalogueDocuments;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Every document a document is built from, through a catalogue, each once and in the order first met: what its text names
 * ({@link DocumentIndex#references()}) — a pcb its circuit, a circuit its parts, a component its symbol and footprints —, a pcb's
 * own footprints — a mounting hole, a logo —, and the 3D models each footprint stands with; and so on, from each of those. What
 * a whole design carries of other documents, whoever made them.
 *
 * <p>A name the catalogue does not answer is passed over: what is not there is not built in.</p>
 */
public final class BuiltFrom {

    private BuiltFrom() {
    }

    /** The documents a document is built from, without the document itself. */
    public static List<DocumentNode> of(DocumentNode document, CatalogueDocuments catalogue) {
        Map<String, DocumentNode> met = new LinkedHashMap<>();
        Deque<DocumentNode> waiting = new ArrayDeque<>(List.of(document));
        while (!waiting.isEmpty()) {
            DocumentNode next = waiting.removeFirst();
            for (String named : namedBy(next)) {
                if (named.equals(document.slug()) || met.containsKey(named)) {
                    continue;
                }
                Optional<DocumentNode> found = catalogue.find(named).or(() -> catalogue.findCoded(named));
                found.ifPresent(built -> {
                    met.put(named, built);
                    waiting.addLast(built);
                });
            }
        }
        return new ArrayList<>(met.values());
    }

    /** The slugs one document names, each way it names them. */
    private static List<String> namedBy(DocumentNode document) {
        Stream<String> referenced = DocumentIndex.of(document).references().stream().map(DocumentIndex.Reference::slug);
        String kind = document.kind().word();
        if (kind.equals(PcbModule.FOOTPRINT.word())) {
            referenced = Stream.concat(referenced, FootprintDrawing.of(document).models().stream().map(FootprintDrawing.Model::reference));
        } else if (kind.equals(PcbModule.PCB.word())) {
            referenced = Stream.concat(referenced, PcbDrawing.of(document).placements().stream().flatMap(placement -> placement.footprint().stream()));
        }
        return referenced.map(DocumentIndex::slugOf).distinct().toList();
    }
}
