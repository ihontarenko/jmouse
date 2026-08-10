package org.jmouse.petgallery;

import org.jmouse.web.mvc.WebApplicationLauncher;

/** Entry point for the Pet Gallery sample application. */
public class PetGalleryApplication {
    public static void main(String[] arguments) {
        new WebApplicationLauncher(PetGalleryApplication.class).launch(arguments);
    }
}