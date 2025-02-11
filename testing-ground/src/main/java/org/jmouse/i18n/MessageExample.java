package org.jmouse.i18n;

import org.jmouse.core.i18n.StandardResourceBundleLoader;

import java.util.Locale;

public class MessageExample {

    public static void main(String[] args) {
        StandardResourceBundleLoader messageSource = new StandardResourceBundleLoader(MessageExample.class.getClassLoader());

        messageSource.addNames("i18n.messages", "errors");

        String name = messageSource.getMessage("project.name", Locale.of("uk_UA"));

        messageSource.setFallbackWithCode(true);
        messageSource.setFallbackPattern("[? %s ?]");

        System.out.println(messageSource.getMessage("project.description", Locale.of("uk_UA"), name));
        System.out.println(messageSource.getMessage("project.description", name));
        System.out.println(messageSource.getMessage("project.description", Locale.of("uk_UA"), name));
        System.out.println(messageSource.getMessage("error.http.500"));

        System.out.println("end");
    }

}
