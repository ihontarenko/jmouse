/**
 * The half of client authorization that the protocol specifies, and the seam where the other half
 * begins.
 *
 * <p>Here: the discovery documents, the challenge method that must be S256, the redirect address that
 * must be an exact loopback. Every one of those is the same in every installation and is a requirement
 * rather than an opinion.
 *
 * <p>⚠️ <strong>Not here, and deliberately: anything that mints a credential against an account.</strong>
 * That needs to know what an account is, which ones may hold a protocol credential, and how long
 * anything lasts — all of which a library must not decide. {@code AuthorizationCodeStore} is where a
 * product takes over, and what it stores against a code is an opaque reference this package never reads.
 *
 * <p>Nothing here serves anything. There is no HTTP layer in this library and there should not be one;
 * what it owns is the shape of the documents and the rules a request is vetted against.
 */
package org.jmouse.ai.mcp.authorization;
