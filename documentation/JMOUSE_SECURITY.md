# jmouse-security

> **Agent orientation file.** Start here for purpose, ownership and the main execution path.
> If this document and the source disagree, the code wins; update this file with the code change.
> Module: `jmouse-security`. Build/dependency declaration: [pom.xml](../jmouse-security/pom.xml).

## What it is

Authentication and authorization mechanisms independent of web transport: authentication tokens/resolvers, authority decisions, method interception, security expressions, JWT codecs, passwords, user principal services and session tracking.

Authentication establishes identity; authorization decides access to a target. SecurityContext carries the current Authentication, while SecurityEnvelope carries execution/transport context. HTTP filters and application routing belong to higher-level integration.

## The map

| Type | Responsibility |
| --- | --- |
| [Authentication](../jmouse-security/src/main/java/org/jmouse/security/Authentication.java) | Principal, credentials and authorities exposed as one authentication token. |
| [ResolversAuthenticationManager](../jmouse-security/src/main/java/org/jmouse/security/authentication/ResolversAuthenticationManager.java) | Dispatches authentication to registered credential resolvers. |
| [AuthenticationResolver](../jmouse-security/src/main/java/org/jmouse/security/authentication/AuthenticationResolver.java) | Strategy for a supported credential/token form. |
| [AuthenticationInspector](../jmouse-security/src/main/java/org/jmouse/security/authentication/AuthenticationInspector.java) | Classifies anonymous, partial and full authentication state. |
| [AuthorizationManager](../jmouse-security/src/main/java/org/jmouse/security/authorization/AuthorizationManager.java) | Access decision for a secured target. |
| [AuthorizeMethodInterceptor](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/AuthorizeMethodInterceptor.java) | Applies method authorization around proxied invocations. |
| [AuthorizeMethodManager](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/AuthorizeMethodManager.java) | Resolves method attributes and evaluates before/after authorization expressions. |
| [CompositeAnnotationExpressionAttributeRegistry](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/attribute/CompositeAnnotationExpressionAttributeRegistry.java) | Finds supported authorization annotations and caches resolved metadata. |
| [SecurityContextHolder](../jmouse-security/src/main/java/org/jmouse/security/SecurityContextHolder.java) | Static access to the configured context-holder strategy. |
| [JwtCodec](../jmouse-security/src/main/java/org/jmouse/security/jwt/JwtCodec.java) | Token encode/decode contract implemented by concrete signature algorithms. |
| [ValidatingJwtCodec](../jmouse-security/src/main/java/org/jmouse/security/jwt/ValidatingJwtCodec.java) | Adds issuer/audience claim verification after delegate decoding. |
| [Pbkdf2Sha256PasswordEncoder](../jmouse-security/src/main/java/org/jmouse/security/password/Pbkdf2Sha256PasswordEncoder.java) | Salted PBKDF2-HMAC-SHA256 password hashing and verification. |

## Execution and ownership

1. Configure principal services and authentication resolvers matching accepted credentials.
2. Authenticate the supplied token and place its resulting Authentication into the execution SecurityContext.
3. For method security, wire proxy interception, metadata registries and an expression handler/authorization manager.
4. Resolve access for the requested phase and apply the denied handler when access fails; clear or restore security context when the execution boundary ends.

## Public API and behavior

SecurityContextHolder exposes get/set/clear and supports replacing its holder strategy.

AuthoritiesAuthorizationManager and VotingAuthorizationManager provide different decision mechanisms; choose the manager matching the required policy.

Composite annotation resolution gives method annotations precedence over class annotations; among resolvers, lower order wins.

JWT algorithm codecs and JwtClaimsVerifier have separate responsibilities. ValidatingJwtCodec composes cryptographic decoding and configured claim verification.

## Wiring and extension points

- Implement AuthenticationResolver for another credential form and PermissionEvaluator for domain-object permission checks.
- Provide RoleHierarchy for implied authorities or an AccessDecisionVoter for voting decisions.
- Customize MethodAuthorizationDeniedHandler and expression-attribute resolvers rather than mixing denied behavior into every expression.
- Use a deliberate SecurityContextHolderStrategy when execution ownership differs from a plain request thread.

## Pitfalls

- An authenticated principal is not automatically authorized for every operation.
- Thread-local security state needs cleanup and does not automatically follow arbitrary asynchronous work.
- Post-authorization observes a completed invocation result; do not assume a denial undoes effects already performed by the target.
- Signature verification and issuer/audience policy are distinct configuration concerns; inspect the actual codec/decorator wiring.

## Source navigation and verification

The map above links directly to the owning implementations under [src/main/java](../jmouse-security/src/main/java).
Check those implementations when changing initialization order, error behavior or collaborator ownership.

This module currently has no Java sources under src/test in the inspected checkout.

This file describes the checked-in source. Documentation authoring did not execute integration probes or change runtime code.

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-security/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-core` | compile | no |
| `org.jmouse:jmouse-el` | compile | no |
| `org.jmouse:jmouse-context` | compile | no |
| `org.slf4j:slf4j-api` | compile | no |
| `jakarta.annotation:jakarta.annotation-api` | compile | no |
| `ch.qos.logback:logback-classic` | compile | yes |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.security`

| Type | Responsibility / contract |
|---|---|
| [Authentication](../jmouse-security/src/main/java/org/jmouse/security/Authentication.java) | 🔐 Authentication token: principal + credentials + authorities. |
| [Authority](../jmouse-security/src/main/java/org/jmouse/security/Authority.java) | Operations: `GrantedAuthority`, `ofCollection`, `authority`. |
| [BadCredentialsException](../jmouse-security/src/main/java/org/jmouse/security/BadCredentialsException.java) | Implements/extends `AuthenticationException`. |
| [DeferredSecurityContext](../jmouse-security/src/main/java/org/jmouse/security/DeferredSecurityContext.java) | Implements/extends `Supplier`. |
| [InheritableThreadLocalSecurityContextHolderStrategy](../jmouse-security/src/main/java/org/jmouse/security/InheritableThreadLocalSecurityContextHolderStrategy.java) | Implements/extends `SecurityContextHolderStrategy`. Operations: `getContext`, `setContext`, `clearContext`, `newContext`. |
| [PasswordMismatchException](../jmouse-security/src/main/java/org/jmouse/security/PasswordMismatchException.java) | Implements/extends `BadCredentialsException`. |
| [SecurityContext](../jmouse-security/src/main/java/org/jmouse/security/SecurityContext.java) | 📦 Holds the current Authentication for the execution. |
| [SecurityContextHolder](../jmouse-security/src/main/java/org/jmouse/security/SecurityContextHolder.java) | Operations: `getContext`, `setContext`, `clearContext`, `newContext`, `getContextHolderStrategy`, `setContextHolderStrategy`. |
| [SecurityContextHolderStrategy](../jmouse-security/src/main/java/org/jmouse/security/SecurityContextHolderStrategy.java) | Operations: `getContext`, `setContext`, `clearContext`, `newContext`. |
| [SecurityEnvelope](../jmouse-security/src/main/java/org/jmouse/security/SecurityEnvelope.java) | 📦 Execution envelope that carries a SecurityContext and transport-specific natives. |
| [SupplierDeferredSecurityContext](../jmouse-security/src/main/java/org/jmouse/security/SupplierDeferredSecurityContext.java) | Implements/extends `DeferredSecurityContext`. Operations: `get`. |
| [ThreadLocalSecurityContextHolderStrategy](../jmouse-security/src/main/java/org/jmouse/security/ThreadLocalSecurityContextHolderStrategy.java) | Implements/extends `SecurityContextHolderStrategy`. Operations: `getContext`, `setContext`, `clearContext`, `newContext`. |
| [UserPrincipal](../jmouse-security/src/main/java/org/jmouse/security/UserPrincipal.java) | Implements/extends `Principal`. Operations: `builder`, `getName`, `username`, `password`, `authority`, `authorities`, `enabled`, `build`, `User`, `this`, `Builder`. |
| [UserPrincipalNotFoundException](../jmouse-security/src/main/java/org/jmouse/security/UserPrincipalNotFoundException.java) | Implements/extends `AuthenticationException`. |
| [UserPrincipalRepository](../jmouse-security/src/main/java/org/jmouse/security/UserPrincipalRepository.java) | Operations: `createUser`, `updateUser`, `deleteUser`. |
| [UserPrincipalService](../jmouse-security/src/main/java/org/jmouse/security/UserPrincipalService.java) | Implements/extends `UserPrincipalRepository`. Operations: `changePassword`, `userExists`. |

#### `org.jmouse.security.access`

| Type | Responsibility / contract |
|---|---|
| [AbstractExpressionHandler](../jmouse-security/src/main/java/org/jmouse/security/access/AbstractExpressionHandler.java) | Implements/extends `ExpressionHandler`. Operations: `getExpressionLanguage`. |
| [ExpressionHandler](../jmouse-security/src/main/java/org/jmouse/security/access/ExpressionHandler.java) | 🧠 Evaluates an authorization expression for a method proxyInvocation. |
| [MethodExpressionHandler](../jmouse-security/src/main/java/org/jmouse/security/access/MethodExpressionHandler.java) | Implements/extends `ExpressionHandler`. |
| [PermissionEvaluator](../jmouse-security/src/main/java/org/jmouse/security/access/PermissionEvaluator.java) | ✅ Evaluates fine-grained permissions on a domain object or identifier. |
| [Phase](../jmouse-security/src/main/java/org/jmouse/security/access/Phase.java) | Enum values: `BEFORE`, `AFTER`. |
| [RoleHierarchy](../jmouse-security/src/main/java/org/jmouse/security/access/RoleHierarchy.java) | 🧭 Role hierarchy with reachability queries. |
| [Vote](../jmouse-security/src/main/java/org/jmouse/security/access/Vote.java) | Enum values: `GRANT`, `DENY`, `ABSTAIN`. |

#### `org.jmouse.security.access.annotation`

| Type | Responsibility / contract |
|---|---|
| [Authorize](../jmouse-security/src/main/java/org/jmouse/security/access/annotation/Authorize.java) | Annotation contract; members and defaults are defined in this source. |
| [DeniedHandler](../jmouse-security/src/main/java/org/jmouse/security/access/annotation/DeniedHandler.java) | Annotation contract; members and defaults are defined in this source. |
| [DenyAfter](../jmouse-security/src/main/java/org/jmouse/security/access/annotation/DenyAfter.java) | Annotation contract; members and defaults are defined in this source. |
| [PostAuthorize](../jmouse-security/src/main/java/org/jmouse/security/access/annotation/PostAuthorize.java) | Annotation contract; members and defaults are defined in this source. |
| [PreAuthorize](../jmouse-security/src/main/java/org/jmouse/security/access/annotation/PreAuthorize.java) | Annotation contract; members and defaults are defined in this source. |

#### `org.jmouse.security.authentication`

| Type | Responsibility / contract |
|---|---|
| [AbstractAuthentication](../jmouse-security/src/main/java/org/jmouse/security/authentication/AbstractAuthentication.java) | Implements/extends `Authentication`. Operations: `getCredentials`, `getAuthorities`, `isAuthenticated`, `setAuthenticated`, `getPrincipal`, `getName`. |
| [AbstractAuthenticationManager](../jmouse-security/src/main/java/org/jmouse/security/authentication/AbstractAuthenticationManager.java) | Implements/extends `AuthenticationManager`. Operations: `authenticate`. |
| [AbstractAuthenticationResolver](../jmouse-security/src/main/java/org/jmouse/security/authentication/AbstractAuthenticationResolver.java) | Implements/extends `AuthenticationResolver`. Operations: `authenticate`. |
| [AbstractUsernamePasswordAuthenticationResolver](../jmouse-security/src/main/java/org/jmouse/security/authentication/AbstractUsernamePasswordAuthenticationResolver.java) | Implements/extends `AbstractAuthenticationResolver`. Operations: `getPrincipalService`, `setPrincipalService`, `supports`. |
| [AnonymousAuthentication](../jmouse-security/src/main/java/org/jmouse/security/authentication/AnonymousAuthentication.java) | Implements/extends `AbstractAuthentication`. |
| [AuthenticationException](../jmouse-security/src/main/java/org/jmouse/security/authentication/AuthenticationException.java) | ❌ Thrown when authentication fails or is required. |
| [AuthenticationInspector](../jmouse-security/src/main/java/org/jmouse/security/authentication/AuthenticationInspector.java) | 🔎 AuthenticationInspector |
| [AuthenticationLevel](../jmouse-security/src/main/java/org/jmouse/security/authentication/AuthenticationLevel.java) | 🔑 Represents the strength of an established authentication. |
| [AuthenticationManager](../jmouse-security/src/main/java/org/jmouse/security/authentication/AuthenticationManager.java) | Authenticates a supplied Authentication token and returns the resulting token or throws AuthenticationException. |
| [AuthenticationResolver](../jmouse-security/src/main/java/org/jmouse/security/authentication/AuthenticationResolver.java) | 🛂 Checks a specific type of credentials and returns Authentication if successful. |
| [AuthenticationResolverRegistry](../jmouse-security/src/main/java/org/jmouse/security/authentication/AuthenticationResolverRegistry.java) | Operations: `getResolvers`, `addResolver`, `hasResolver`, `IllegalArgumentException`. |
| [BearerTokenAuthentication](../jmouse-security/src/main/java/org/jmouse/security/authentication/BearerTokenAuthentication.java) | Implements/extends `AbstractAuthentication`. |
| [ResolversAuthenticationManager](../jmouse-security/src/main/java/org/jmouse/security/authentication/ResolversAuthenticationManager.java) | Implements/extends `AbstractAuthenticationManager implements AuthenticationResolverRegistry`. Operations: `doAuthenticate`, `getResolvers`, `addResolver`. |
| [UsernamePasswordAuthentication](../jmouse-security/src/main/java/org/jmouse/security/authentication/UsernamePasswordAuthentication.java) | Implements/extends `AbstractAuthentication`. |

#### `org.jmouse.security.authentication.dao`

| Type | Responsibility / contract |
|---|---|
| [DaoAuthenticationResolver](../jmouse-security/src/main/java/org/jmouse/security/authentication/dao/DaoAuthenticationResolver.java) | Implements/extends `AbstractUsernamePasswordAuthenticationResolver`. |

#### `org.jmouse.security.authentication.jwt`

| Type | Responsibility / contract |
|---|---|
| [JwtTokenAuthenticationResolver](../jmouse-security/src/main/java/org/jmouse/security/authentication/jwt/JwtTokenAuthenticationResolver.java) | Implements/extends `AuthenticationResolver`. Operations: `authorities`, `supports`, `authenticate`. |

#### `org.jmouse.security.authorization`

| Type | Responsibility / contract |
|---|---|
| [AccessResult](../jmouse-security/src/main/java/org/jmouse/security/authorization/AccessResult.java) | Operations: `isGranted`, `getMessage`, `getAttributes`, `toString`, `super`, `this`. |
| [AuthoritiesAccessResult](../jmouse-security/src/main/java/org/jmouse/security/authorization/AuthoritiesAccessResult.java) | Implements/extends `AccessResult.AbstractAccessResult`. Operations: `getAuthorities`, `toString`. |
| [AuthoritiesAuthorizationManager](../jmouse-security/src/main/java/org/jmouse/security/authorization/AuthoritiesAuthorizationManager.java) | Implements/extends `AuthorizationManager >`. Operations: `getRoleHierarchy`, `setRoleHierarchy`, `check`. |
| [AuthorityPolicyAuthorizationManager](../jmouse-security/src/main/java/org/jmouse/security/authorization/AuthorityPolicyAuthorizationManager.java) | Implements/extends `AuthorizationManager`. Operations: `permitAll`, `check`, `toString`, `denyAll`, `not`, `anonymous`, `authenticated`, `of`, `hasRole`, `hasAuthority`, `hasAnyRole`, `hasAnyAuthority`. |
| [AuthorizationException](../jmouse-security/src/main/java/org/jmouse/security/authorization/AuthorizationException.java) | ⛔ Thrown when an authenticated principal lacks required access rights. |
| [AuthorizationManager](../jmouse-security/src/main/java/org/jmouse/security/authorization/AuthorizationManager.java) | 🚦 Decides access for a given secured target (endpoint/method/message). |
| [ExpressionAccessResult](../jmouse-security/src/main/java/org/jmouse/security/authorization/ExpressionAccessResult.java) | Implements/extends `AccessResult.AbstractAccessResult`. Operations: `toString`. |

#### `org.jmouse.security.authorization.method`

| Type | Responsibility / contract |
|---|---|
| [AbstractAuthorizeMethodInterceptor](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/AbstractAuthorizeMethodInterceptor.java) | Implements/extends `MethodInterceptor`. |
| [AbstractSecurityMethodExpressionHandler](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/AbstractSecurityMethodExpressionHandler.java) | Implements/extends `AbstractExpressionHandler`. Operations: `createContext`. |
| [AnnotationExpressionAttribute](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/AnnotationExpressionAttribute.java) | Value record: `(Phase phase, Expression expression, A attribute) implements ExpressionAttribute`. |
| [AuthorizeMethodInterceptor](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/AuthorizeMethodInterceptor.java) | 🛡️ Interceptor for enforcing method-level authorization logic. |
| [AuthorizeMethodManager](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/AuthorizeMethodManager.java) | ⚖️ Central manager responsible for method-level authorization. |
| [ExpressionAttribute](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/ExpressionAttribute.java) | Operations: `expression`. |
| [LogErrorMethodAuthorizationDeniedHandler](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/LogErrorMethodAuthorizationDeniedHandler.java) | Implements/extends `MethodAuthorizationDeniedHandler`. Operations: `handleDeniedInvocation`. |
| [MethodAuthorizationContext](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/MethodAuthorizationContext.java) | Value record: `(MethodInvocation proxyInvocation, Phase phase, Object result)`. |
| [MethodAuthorizationDeniedHandler](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/MethodAuthorizationDeniedHandler.java) | Operations: `handleDeniedInvocation`. |
| [MethodAuthorizationException](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/MethodAuthorizationException.java) | Implements/extends `AuthorizationException`. |
| [MethodInvocationAuthorizationManager](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/MethodInvocationAuthorizationManager.java) | Implements/extends `AuthorizationManager`. Operations: `check`. |
| [SecurityExpressionEvaluator](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/SecurityExpressionEvaluator.java) | 🧮 Evaluates security expressions within a given EvaluationContext. |
| [SecurityExpressionLanguage](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/SecurityExpressionLanguage.java) | Implements/extends `ExpressionLanguage`. Operations: `getSharedInstance`. |
| [SecurityExtension](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/SecurityExtension.java) | Implements/extends `Extension`. Operations: `getFunctions`. |
| [SecurityMethodExpressionHandler](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/SecurityMethodExpressionHandler.java) | Implements/extends `AbstractSecurityMethodExpressionHandler implements MethodExpressionHandler`. Operations: `evaluate`. |
| [ThrowingMethodAuthorizationDeniedHandler](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/ThrowingMethodAuthorizationDeniedHandler.java) | Implements/extends `MethodAuthorizationDeniedHandler`. Operations: `handleDeniedInvocation`. |

#### `org.jmouse.security.authorization.method.attribute`

| Type | Responsibility / contract |
|---|---|
| [AbstractExpressionAttributeRegistry](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/attribute/AbstractExpressionAttributeRegistry.java) | 🧮 Caches and resolves expression-based authorization attributes for methods. |
| [AttributeResolver](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/attribute/AttributeResolver.java) | 🧩 Strategy that knows how to convert a specific annotation into an ExpressionAttribute. |
| [AuthorizeAnnotationResolver](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/attribute/AuthorizeAnnotationResolver.java) | 🔐 Resolves Authorize annotations into ExpressionAttribute instances. |
| [CompositeAnnotationExpressionAttributeRegistry](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/attribute/CompositeAnnotationExpressionAttributeRegistry.java) | 🧮 Composite registry that discovers *any* supported annotation and converts it into an attribute. |
| [ExpressionAttributeRegistry](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/attribute/ExpressionAttributeRegistry.java) | 🧩 Contract for resolving ExpressionAttributes that define security metadata for method invocations. |

#### `org.jmouse.security.authorization.method.attribute.jsr250`

| Type | Responsibility / contract |
|---|---|
| [DenyAllAnnotationResolver](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/attribute/jsr250/DenyAllAnnotationResolver.java) | 🚫 Resolves DenyAll (JSR-250) annotations into a static ExpressionAttribute denying all invocations. |
| [Jsr250AnnotationResolver](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/attribute/jsr250/Jsr250AnnotationResolver.java) | 🧩 Base class for JSR-250 annotation resolvers (e.g. @RolesAllowed, @PermitAll, @DenyAll). |
| [PermitAllAnnotationResolver](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/attribute/jsr250/PermitAllAnnotationResolver.java) | ✅ Resolves PermitAll (JSR-250) annotations into a static ExpressionAttribute allowing all invocations. |
| [RolesAllowedAnnotationResolver](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/attribute/jsr250/RolesAllowedAnnotationResolver.java) | 🧩 Resolver for the RolesAllowed JSR-250 annotation. |

#### `org.jmouse.security.authorization.method.support`

| Type | Responsibility / contract |
|---|---|
| [AuthenticationFunctions](../jmouse-security/src/main/java/org/jmouse/security/authorization/method/support/AuthenticationFunctions.java) | Implements/extends `AuthenticationInspector`. Operations: `isAuthenticated`, `isAnonymous`, `isPartiallyAuthenticated`, `isFullyAuthenticated`, `level`. |

#### `org.jmouse.security.authorization.voting`

| Type | Responsibility / contract |
|---|---|
| [AccessDecisionStrategy](../jmouse-security/src/main/java/org/jmouse/security/authorization/voting/AccessDecisionStrategy.java) | Enum values: `AFFIRMATIVE`, `CONSENSUS`, `UNANIMOUS`. |
| [AccessDecisionVoter](../jmouse-security/src/main/java/org/jmouse/security/authorization/voting/AccessDecisionVoter.java) | 🗳️ Individual access vote. |
| [VotingAuthorizationManager](../jmouse-security/src/main/java/org/jmouse/security/authorization/voting/VotingAuthorizationManager.java) | Implements/extends `AuthorizationManager`. Operations: `check`, `getVoters`, `getStrategy`. |

#### `org.jmouse.security.jwt`

| Type | Responsibility / contract |
|---|---|
| [AbstractJwtCodec](../jmouse-security/src/main/java/org/jmouse/security/jwt/AbstractJwtCodec.java) | 🧩 Shared JWT logic: Base64URL, header/claims parse, time checks, signing input. |
| [Jwt](../jmouse-security/src/main/java/org/jmouse/security/jwt/Jwt.java) | 📦 Immutable representation of a JSON Web Token (JWT). |
| [JwtAuthorities](../jmouse-security/src/main/java/org/jmouse/security/jwt/JwtAuthorities.java) | 🧭 JwtAuthorities |
| [JwtClaimsVerifier](../jmouse-security/src/main/java/org/jmouse/security/jwt/JwtClaimsVerifier.java) | ✅ JwtClaimsVerifier |
| [JwtCodec](../jmouse-security/src/main/java/org/jmouse/security/jwt/JwtCodec.java) | Operations: `readObject`, `writeObject`, `super`. |
| [ValidatingJwtCodec](../jmouse-security/src/main/java/org/jmouse/security/jwt/ValidatingJwtCodec.java) | 🛡️ ValidatingJwtCodec |

#### `org.jmouse.security.jwt.codec`

| Type | Responsibility / contract |
|---|---|
| [EdDSAJwtCodec](../jmouse-security/src/main/java/org/jmouse/security/jwt/codec/EdDSAJwtCodec.java) | 🪶 EdDSAJwtCodec — Ed25519 codec. |
| [ES256JwtCodec](../jmouse-security/src/main/java/org/jmouse/security/jwt/codec/ES256JwtCodec.java) | 🔐 ES256JwtCodec — ECDSA P-256 (SHA-256) codec. |
| [HS256JwtCodec](../jmouse-security/src/main/java/org/jmouse/security/jwt/codec/HS256JwtCodec.java) | 🔒 HS256 codec (HMAC-SHA256). |
| [RS256JwtCodec](../jmouse-security/src/main/java/org/jmouse/security/jwt/codec/RS256JwtCodec.java) | 🔑 RS256 codec (RSA-PKCS#1 v1.5 with SHA-256). |

#### `org.jmouse.security.method`

| Type | Responsibility / contract |
|---|---|
| [MethodEnvelope](../jmouse-security/src/main/java/org/jmouse/security/method/MethodEnvelope.java) | Implements/extends `SecurityEnvelope`. Operations: `getTarget`, `getMethod`, `getArguments`, `getSecurityContext`, `setSecurityContext`, `getAttributes`, `getNative`. |

#### `org.jmouse.security.password`

| Type | Responsibility / contract |
|---|---|
| [EncoderAlgorithm](../jmouse-security/src/main/java/org/jmouse/security/password/EncoderAlgorithm.java) | Enum values: `ARGON2ID`, `BCRYPT`, `PBKDF2_SHA256`, `SCRYPT`. |
| [PasswordEncoder](../jmouse-security/src/main/java/org/jmouse/security/password/PasswordEncoder.java) | Operations: `encode`, `verify`. |
| [PasswordEncoderFactory](../jmouse-security/src/main/java/org/jmouse/security/password/PasswordEncoderFactory.java) | Operations: `getEncoder`. |
| [Pbkdf2Sha256PasswordEncoder](../jmouse-security/src/main/java/org/jmouse/security/password/Pbkdf2Sha256PasswordEncoder.java) | 🔐 Password encoder using PBKDF2 with HMAC-SHA256. |

#### `org.jmouse.security.service`

| Type | Responsibility / contract |
|---|---|
| [InMemoryUserPrincipalRepository](../jmouse-security/src/main/java/org/jmouse/security/service/InMemoryUserPrincipalRepository.java) | Implements/extends `UserPrincipalRepository`. Operations: `loadUser`, `createUser`, `updateUser`, `deleteUser`. |
| [InMemoryUserPrincipalService](../jmouse-security/src/main/java/org/jmouse/security/service/InMemoryUserPrincipalService.java) | Implements/extends `InMemoryUserPrincipalRepository implements UserPrincipalService, UserPrincipalRepository`. Operations: `changePassword`, `userExists`. |

#### `org.jmouse.security.session`

| Type | Responsibility / contract |
|---|---|
| [SessionRegistry](../jmouse-security/src/main/java/org/jmouse/security/session/SessionRegistry.java) | 🧩 SessionRegistry — central component for tracking active user sessions. |

### Packaged resources

See [packaged resources](../jmouse-security/src/main/resources/) for configuration/templates owned by this module.

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.
