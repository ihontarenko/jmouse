package org.jmouse.cad.store.authority;

import org.jmouse.cad.jpa.catalogue.DocumentOwner;

import java.util.List;

/**
 * Where something is, outermost first: nothing (the installation), or a chain of owners such as a workspace and the
 * project in it. A place names its whole chain, so the host decides a grant at any link without looking a hierarchy up.
 *
 * @param chain the owners, outermost first
 */
public record CadPlace(List<DocumentOwner> chain) {

    private static final CadPlace INSTALLATION = new CadPlace(List.of());

    public CadPlace {
        chain = List.copyOf(chain);
    }

    public static CadPlace installation() {
        return INSTALLATION;
    }

    public static CadPlace of(DocumentOwner... chain) {
        return new CadPlace(List.of(chain));
    }

    public boolean isInstallation() {
        return chain.isEmpty();
    }
}
