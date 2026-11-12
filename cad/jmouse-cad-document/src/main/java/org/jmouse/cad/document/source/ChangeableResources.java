package org.jmouse.cad.document.source;

import java.io.IOException;

/** The files of a document held in a place that lets them be changed: a folder mounted for changing. */
public interface ChangeableResources extends DocumentResources {

    /** Keeps a file under a name, replacing none: a name it holds already is refused. */
    void keep(String name, byte[] content) throws IOException;

    /** @return whether it held one of that name */
    boolean remove(String name) throws IOException;
}
