package org.jmouse.security.core.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.security.core.Envelope;

import java.util.Iterator;
import java.util.List;

public class Pipeline<E> {

    private final List<Valve> valves;

    public Pipeline(List<Valve> valves) {
        this.valves = List.copyOf(valves);
    }

    public Step execute(E envelope) throws Exception {

        Chain<Envelope, Void, Step> chain = Chain.of(valves)

        return new Runner<>(valves.iterator()).next(envelope);
    }

    private record Runner<E>(Iterator<Valve> iterator) implements Cursor<E> {
        @Override
        public Step next(E envelope) throws Exception {
            return iterator.hasNext() ? iterator.next().proceed(envelope, this) : Step.CONTINUE;
        }
    }

}
