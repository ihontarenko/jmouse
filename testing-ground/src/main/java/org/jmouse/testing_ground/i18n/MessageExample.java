package org.jmouse.testing_ground.i18n;

import org.jmouse.core.i18n.LocalizableMessage;
import org.jmouse.core.i18n.StandardMessageSourceBundle;

import java.util.Locale;

public class MessageExample {

    public static void main(String[] args) {
        StandardMessageSourceBundle messageSource = new StandardMessageSourceBundle(MessageExample.class.getClassLoader());

        messageSource.addNames("i18n.messages", "errors");

        String name = messageSource.getMessage("project.name", Locale.of("uk_UA"));


         // My Zais
        messageSource.setFallbackWithCode(true);
        messageSource.setFallbackPattern("[? %s ?]");

        System.out.println(messageSource.getMessage("project.description", Locale.of("uk_UA"), name));
        System.out.println(messageSource.getMessage("project.description", name));
        System.out.println(messageSource.getMessage("project.description", Locale.of("uk_UA"), name));
        System.out.println(messageSource.getMessage("error.http.500"));

        LocalizableMessage localizableMessage = LocalizableMessage.of("error.http.403", "403 default!!!");

        System.out.println(
                messageSource.getMessage(localizableMessage)
        );

        System.out.println(messageSource.getMessage("{0}! Im your father!", "Luke"));

        System.out.println(messageSource.getMessage("zais.name"));

        System.out.println(messageSource.getMessageFor(MessageExample.class, "hello"));
    }

}
