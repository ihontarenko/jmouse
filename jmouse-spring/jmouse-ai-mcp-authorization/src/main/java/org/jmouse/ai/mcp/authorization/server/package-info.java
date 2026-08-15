/**
 * The three protocol endpoints a Model Context Protocol client walks before it holds a credential, and
 * the one screen a person approves it on.
 *
 * <p>Here: registration, authorization, the token endpoint, and a consent page. What each of them does is
 * dictated by RFC 6749, 7591, 7636, 8414 and 9728, which is why they are worth having once rather than
 * once per product — a client branches literally on the strings they answer with, and two
 * implementations of the same specification drift silently and are found only by a client that stops
 * connecting.
 *
 * <p>⚠️ <strong>Not here, and deliberately: what an account is.</strong> No type in this package names a
 * user, a member, an agent or an account, and the four interfaces are why —
 * {@link org.jmouse.ai.mcp.authorization.server.ApprovingSubject} says who is signed in and what they may
 * authorize, {@link org.jmouse.ai.mcp.authorization.server.CredentialIssuer} mints,
 * {@link org.jmouse.ai.mcp.authorization.server.ClientNameRegistry} remembers a claim, and
 * {@code AuthorizationCodeStore} decides where a one-time code lives. Everything that reaches them is an
 * opaque string this package never reads.
 *
 * <p>⚠️ <strong>Minting in particular is a product's, and the two that use this genuinely
 * disagree.</strong> One signs its protocol credential itself so that "this works nowhere else" is a
 * signature that does not verify; the other issues an ordinary credential and refuses non-protocol
 * surfaces with a declared check. That disagreement is about security posture rather than convention,
 * and {@code CredentialIssuer} exists so it never has to be settled.
 *
 * <p>⚠️ <strong>A separate module from {@code jmouse-ai-mcp}, and the reason is written down there:</strong>
 * that package says "Nothing here serves anything. There is no HTTP layer in this library and there
 * should not be one", and its build records the decision to stay Spring-free so a product can take it
 * without a servlet stack. This module is where the HTTP goes, so that contract stays kept.
 */
package org.jmouse.ai.mcp.authorization.server;
