/**
 * What the mechanism does not know, declared as seams the product fills in.
 *
 * <p>Each interface here exists because the answer genuinely differs between products rather than
 * because indirection is tidy. Who is calling, whether they may, where the call runs, what is
 * recorded, and where a pending confirmation is held are five questions a tool mechanism has no
 * business answering on its own.
 *
 * <p>Every seam ships a default that refuses nothing and records nothing, so a product can run a tool
 * before it has decided any of this — and so that a product which never will is not forced to write
 * five empty classes.
 *
 * <p>Observation is one seam and not three. A dispatcher that recorded an audit entry, a metric and an
 * activity timestamp through three collaborators would make every adopter copy the triple or forget
 * part of it; a single trace with three methods — outcome, refusal, failure — matches the three things
 * that can actually happen to a call, and a product composes whatever it likes behind it.
 *
 * <p>Notably absent: authorization as a dependency. {@code ToolAuthorizer} is an interface here rather
 * than a dependency on {@code jmouse-access}, which is what lets a product with no access model use
 * tools at all. A product holding both takes {@code jmouse-ai-access} and gets its tools and its HTTP
 * endpoints authorized by one engine against one policy.
 */
package org.jmouse.ai.spi;
