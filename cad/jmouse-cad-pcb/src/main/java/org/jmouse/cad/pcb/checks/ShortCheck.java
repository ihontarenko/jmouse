package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperPieces;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.jmouse.cad.document.FindingSeverity.ERROR;

/**
 * Copper joining nets the circuit keeps apart. A short is told as the reader needs it: the nets it joins and where — for each two, the
 * chain of copper joining them, its pieces the finding's keys, the first few chains named, the middle of the first chain its point —,
 * never every piece the joined copper spreads to.
 */
final class ShortCheck implements PcbCheck {

    @Override
    public List<PcbFinding> findings(CheckContext context) {
        return context.nets().shorts().stream().map(joined -> findingOf(context, joined)).toList();
    }

    private static PcbFinding findingOf(CheckContext context, CopperNets.Short joined) {
        String joins = "Copper joins " + String.join(" and ", joined.nets()) + ", which the circuit keeps apart";
        Set<String> keys = new LinkedHashSet<>();
        List<String> named = new ArrayList<>();
        Optional<Point> at = Optional.empty();
        for (CopperNets.Joined join : joined.joins()) {
            join.chain().forEach(member -> keys.add(context.keyOf(member)));
            List<CopperNets.Member> chain = join.chain();
            if (named.size() < context.settings().namedMeetings()) {
                List<String> through = chain.subList(1, chain.size() - 1).stream()
                        .map(member -> FindingWords.labelOf(member, context.pieceOf(member))).toList();
                named.add(FindingWords.labelOf(chain.getFirst(), context.pieceOf(chain.getFirst())) + " of " + join.firstNet() + " reaches "
                          + FindingWords.labelOf(chain.getLast(), context.pieceOf(chain.getLast())) + " of " + join.secondNet()
                          + (through.isEmpty() ? " directly" : " through " + String.join(", ", through)));
            }
            if (at.isEmpty()) {
                at = touchIn(context, chain);
            }
        }
        int unnamed = joined.joins().size() - named.size();
        String where = named.isEmpty() ? "" : ": " + String.join("; ", named) + (unnamed > 0 ? "; and " + unnamed + " more" : "");
        return new PcbFinding(ERROR, joins + where, List.copyOf(keys), at);
    }

    /** Where two pieces in the middle of a chain touch: the place a short is shown at. */
    private static Optional<Point> touchIn(CheckContext context, List<CopperNets.Member> chain) {
        int middle = Math.max(0, chain.size() / 2 - 1);
        Optional<CopperPieces.Piece> first = context.pieceOf(chain.get(middle));
        Optional<CopperPieces.Piece> second = context.pieceOf(chain.get(Math.min(chain.size() - 1, middle + 1)));
        return first.isPresent() && second.isPresent() ? Optional.of(first.get().shape().gapTo(second.get().shape()).at()) : Optional.empty();
    }
}
