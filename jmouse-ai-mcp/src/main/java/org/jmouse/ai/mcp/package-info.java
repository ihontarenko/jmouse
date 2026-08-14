/**
 * Model Context Protocol, in both directions, over one catalogue.
 *
 * <p><strong>Server:</strong> publish the catalogue's tools and route every call to
 * {@code ToolDispatcher}. Nothing here decides anything — it is the shape the dispatcher takes when
 * the caller is outside the process. Where the protocol is served must agree with where credentials
 * are confined, and the two are checked against each other at startup: serving it anywhere else moves
 * the whole feature outside its security boundary, quietly.
 *
 * <p><strong>Client:</strong> connect to a remote server and register <em>its</em> tools into the same
 * catalogue, backed by a remote handler. This is the symmetry that pays for the design: the catalogue
 * becomes the union of local and remote capability, an assistant calls both identically, and "do not
 * speak the protocol to reach yourself" stops being advice — a local tool is already in the catalogue,
 * so there is nothing to connect to.
 *
 * <p>The one module allowed to see the protocol SDK, so every other module can be used by a product
 * that has never heard of it. Protocol-level authorization concerns live here; binding a credential to
 * an account is the product's and stays behind a seam — a library must not have an opinion about what
 * an account is.
 */
package org.jmouse.ai.mcp;
