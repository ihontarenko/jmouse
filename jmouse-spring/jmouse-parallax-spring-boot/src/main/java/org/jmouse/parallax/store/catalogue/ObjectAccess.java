package org.jmouse.parallax.store.catalogue;

import org.jmouse.parallax.authority.ParallaxAuthority;
import org.jmouse.parallax.authority.ParallaxCapability;
import org.jmouse.parallax.authority.ParallaxPlace;
import org.jmouse.parallax.authority.ParallaxPlaces;
import org.jmouse.parallax.jpa.catalogue.MathematicalObject;
import org.jmouse.parallax.jpa.catalogue.ObjectStore;
import org.jmouse.parallax.problem.ParallaxProblem;

/**
 * Who may see and change a mathematical object, decided in one place for every caller (catalogue routes, scenes,
 * computations, lessons): a published catalogue object is open to anybody, a catalogue draft to whoever edits the
 * catalogue, an owned object to whoever may open scenes where its owner is. A refused read answers "not found".
 */
public class ObjectAccess {

    private final ObjectStore objects;
    private final ParallaxAuthority authority;
    private final ParallaxPlaces places;

    public ObjectAccess(ObjectStore objects, ParallaxAuthority authority, ParallaxPlaces places) {
        this.objects = objects;
        this.authority = authority;
        this.places = places;
    }

    public MathematicalObject requireReadable(String idOrSlug) {
        MathematicalObject object = objects.find(idOrSlug).or(() -> objects.findBySlug(idOrSlug))
                .orElseThrow(() -> ParallaxProblem.notFound("The object"));
        if (!canRead(object)) {
            throw ParallaxProblem.notFound("The object");
        }
        return object;
    }

    public boolean canRead(MathematicalObject object) {
        if (object.isCatalogue()) {
            return object.getStatus() == MathematicalObject.Status.PUBLISHED
                    || authority.permitsGlobally(ParallaxCapability.CATALOGUE_WRITE);
        }
        return authority.permits(ParallaxCapability.SCENE_READ, placeOf(object));
    }

    /** Whether the reader may write the object's next revision — the same rule {@link #requireWritable} enforces. */
    public boolean canWrite(MathematicalObject object) {
        if (object.isCatalogue()) {
            return authority.permitsGlobally(ParallaxCapability.CATALOGUE_WRITE);
        }
        return authority.permits(ParallaxCapability.SCENE_WRITE, placeOf(object));
    }

    public void requireWritable(MathematicalObject object) {
        if (object.isCatalogue()) {
            authority.requireGlobally(ParallaxCapability.CATALOGUE_WRITE);
        } else {
            authority.require(ParallaxCapability.SCENE_WRITE, placeOf(object));
        }
    }

    /** Where an owned object is: its owner's whole chain, as the host gives it. */
    public ParallaxPlace placeOf(MathematicalObject object) {
        return places.requireOwner(object.owner().orElseThrow());
    }
}
