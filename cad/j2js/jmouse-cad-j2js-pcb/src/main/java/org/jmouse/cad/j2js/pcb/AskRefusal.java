package org.jmouse.cad.j2js.pcb;

/**
 * 🛤 Why a gesture asked in a page has no answer — said as the server says it, its status and its code, so an editor that asks
 * either reads the refusal the same way.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum AskRefusal {

    /** The board is not open here: let go, or never opened — the editor opens it again. */
    FORGOTTEN(404, "not-found"),
    /** The router refuses the gesture: parts let go where they cannot stand. */
    REFUSED(403, "forbidden"),
    /** No gesture of that name, or a request that is no request of it. */
    INVALID(400, "invalid-request"),
    /** A layout named by its slot that is not held here: the editor sends it whole again. */
    LAYOUT_UNKNOWN(409, "layout-unknown"),
    /** The router failed. */
    FAILED(500, "failed");

    private final int    status;
    private final String code;

    AskRefusal(int status, String code) {
        this.status = status;
        this.code = code;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }
}
