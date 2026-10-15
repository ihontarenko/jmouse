package org.jmouse.parallax.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.parallax.store.subject.Subjects;
import org.jmouse.parallax.store.subject.Subjects.SubjectView;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A subject — a catalogue object or a typed formula — as the one object description every screen draws from. */
@RestController
@RequestMapping(ParallaxManagementRoutes.PREFIX + "/subjects")
public class SubjectController {

    private final Subjects subjects;

    public SubjectController(Subjects subjects) {
        this.subjects = subjects;
    }

    public record ResolveRequest(String reference) {
    }

    @PostMapping("/resolve")
    @PublicEndpoint("a catalogue subject is read through Catalogue.detail, which decides; an inline formula reads nothing stored")
    public SubjectView resolve(@RequestBody ResolveRequest request) {
        return subjects.resolve(request.reference());
    }
}
