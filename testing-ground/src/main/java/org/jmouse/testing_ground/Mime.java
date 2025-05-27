package org.jmouse.testing_ground;

import org.jmouse.core.MediaType;
import org.jmouse.core.MimeType;

public class Mime {

    public static void main(String... arguments) {
        MediaType accept  = MediaType.forString("application/*+xml; q=0.85");
        MimeType  request = MimeType.forString("application/soap+xml");

        System.out.println(accept.includes(request));
    }

}
