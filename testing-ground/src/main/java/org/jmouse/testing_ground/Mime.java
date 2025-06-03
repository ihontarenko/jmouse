package org.jmouse.testing_ground;

import org.jmouse.core.MediaType;
import org.jmouse.core.MimeType;

import java.util.Locale;

public class Mime {

    public static void main(String... arguments) {
        MediaType accept  = MediaType.forString("application/*+xml; q=0.85");
        MimeType  request = MimeType.forString("application/soap+xml");

        Locale.LanguageRange.parse("fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5");

        System.out.println(accept.includes(request));
    }

}
