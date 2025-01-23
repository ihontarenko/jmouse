package svit.web;

import svit.web.context.WebBeanContext;

public class StartApplication {

    public static void main(String... arguments) {
        WebBeanContext context = WebApplicationLauncher.launch(StartApplication.class);
    }

}
