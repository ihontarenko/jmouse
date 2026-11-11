package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.store.about.EngineAbout;
import org.jmouse.cad.store.about.EngineAbout.About;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ℹ️ What the engine is — {@code JMOUSE_CAD_API.md} §2.8: the window every product embedding it shows the same. */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class AboutController {

    private final EngineAbout about;

    public AboutController(EngineAbout about) {
        this.about = about;
    }

    @GetMapping("/about")
    @PublicEndpoint("what the engine is and runs; nothing stored is read")
    public About about() {
        return about.about();
    }
}
