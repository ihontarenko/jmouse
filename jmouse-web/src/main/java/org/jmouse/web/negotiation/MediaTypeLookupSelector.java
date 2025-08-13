package org.jmouse.web.negotiation;

import java.util.List;

public interface MediaTypeLookupSelector {

    List<MediaTypeLookup> getMediaTypeLookups();

    MediaTypeLookup selectMediaTypeLookup(String mediaType);

}
