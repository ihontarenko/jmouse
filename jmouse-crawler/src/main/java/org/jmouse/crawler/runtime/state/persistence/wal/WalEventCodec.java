package org.jmouse.crawler.runtime.state.persistence.wal;

import org.jmouse.core.Verify;
import org.jmouse.crawler.runtime.state.persistence.Codec;

public final class WalEventCodec {

    private final Codec           codec;
    private final StateEventMapping types;

    public WalEventCodec(Codec codec, StateEventMapping types) {
        this.codec = Verify.nonNull(codec, "codec");
        this.types = Verify.nonNull(types, "types");
    }

    public String encodeEvent(StateEvent e) {
        return codec.encode(WalEnvelope.of(e));
    }

    public StateEvent decodeEvent(String jsonLine) {
        WalEnvelope env = codec.decode(jsonLine, WalEnvelope.class);
        Verify.state(env.type() != null && !env.type().isBlank(), "WAL envelope missing type");

        Class<? extends StateEvent> clazz = types.resolve(env.type());

        String payloadJson = codec.encode(env.payload());
        return codec.decode(payloadJson, clazz);
    }
}
