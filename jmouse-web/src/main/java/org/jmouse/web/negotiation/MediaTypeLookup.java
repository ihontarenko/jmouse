package org.jmouse.web.negotiation;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.MediaType;

import java.util.List;

public interface MediaTypeLookup {

    List<MediaType> lookup(HttpServletRequest request);

}
