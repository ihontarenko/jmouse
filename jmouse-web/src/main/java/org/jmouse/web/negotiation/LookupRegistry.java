package org.jmouse.web.negotiation;

import java.util.List;

public interface LookupRegistry {

    List<MediaTypeLookup> getLookups();

    void registerLookup(MediaTypeLookup mediaTypeLookup);

}
