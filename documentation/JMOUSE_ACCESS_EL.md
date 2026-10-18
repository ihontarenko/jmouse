# jmouse-access-el

The .jmp reader/writer and the restricted expression implementation for authorization conditions. It turns policy source into the model from jmouse-access-policy and compiles condition expressions through a dedicated dialect. The wider general expression language is not the policy security boundary.

This is an orientation map of the implementation. The linked source is authoritative when an API or behavior changes.

## Module boundary

The build and declared dependencies are in [pom.xml](../jmouse-access-el/pom.xml). Direct dependency declarations include `jmouse-el`, `jmouse-access-policy`.

The entry points below are the major contracts, not an assertion that every class is an application service. Internal helpers and executable examples can be followed from their source packages.

## Responsibility map

| Type | Responsibility |
| --- | --- |
| [ExpressionEvaluator](../jmouse-access-el/src/main/java/org/jmouse/access/el/ExpressionEvaluator.java) | Reads .jmp source and returns the document it describes. This is the whole of stage 1's public surface. What comes back is a PolicyDocument of plain strings: no scope, no permission, no compiled condition, nothing that knows a catalogue exists. Everything that can be wrong about the model — an unregistered scope, a permission nobody declared, a condition that will not compile — belongs to the binder, so that a parser… |
| [PolicyLoader](../jmouse-access-el/src/main/java/org/jmouse/access/el/loader/PolicyLoader.java) | Several .jmp files read as one policy. Between the parser and the binder there is a job neither of them can do, and it is this one: find the files, read them, follow what they include, notice when that goes round in a circle, and put the result together. A parser cannot, because "relative to what" is a question about a load and a cycle only exists across several files; a binder cannot, because by the time it runs the… |
| [PolicySources](../jmouse-access-el/src/main/java/org/jmouse/access/el/loader/PolicySources.java) | Where policy files come from — the one thing PolicyLoader does not know how to do. The loader owns composition: parse, follow includes, detect a loop, merge. It owns none of the input/output, because "a location" means a classpath pattern in a Spring application, a directory on disk in a command-line tool, and a row in a table on the day the control room can save. All three answer these two questions. |
| [PolicySource](../jmouse-access-el/src/main/java/org/jmouse/access/el/loader/PolicySource.java) | One policy file, found and read — text, plus enough about where it came from to be talked about. Two of the three fields exist for reasons that are not "the parser needs them", because it does not: org.jmouse.access.el.ExpressionEvaluator takes characters. They are the loader's, and each answers a question the loader cannot answer without it. |
| [PolicyWriter](../jmouse-access-el/src/main/java/org/jmouse/access/el/PolicyWriter.java) | A PolicyDocument written as .jmp — including one that was never a file. ExpressionEvaluator#rewrite(String) already renders a policy that was parsed. This renders one that was not : a document projected out of a database, assembled by a form, or built in a test. That is what lets a control room show a declarative expression for a rule nobody ever declared, which is the whole reason the editor can exist. One renderer, not… |
| [PolicySourceTranslator](../jmouse-access-el/src/main/java/org/jmouse/access/el/PolicySourceTranslator.java) | A policy tree written back out as .jmp source — the one way out of the tree for this language. |
| [ExpressionExtension](../jmouse-access-el/src/main/java/org/jmouse/access/el/ExpressionExtension.java) | Everything the .jmp language is made of, registered as one extension. It is the expression language plus the policy language, not the policy language alone: a file needs literals, property access and operators to write a when clause with, and the placeholder parser to read a ${…} with. What a condition may then use is a narrower question, answered somewhere else entirely — see… |
| [AccessRecognizer](../jmouse-access-el/src/main/java/org/jmouse/access/el/lexer/AccessRecognizer.java) | Recognizes the three vocabularies a policy file is written in. Order matters and runs from most general to most specific: a word is a BasicToken first, then a LanguageToken, and only then one of the policy language's own AccessTokens. That way .jmp can add keywords without any of them shadowing a word the expression language already answers to. |
| [ExpressionConditionCompiler](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/ExpressionConditionCompiler.java) | Compiles a policy condition through jMouse EL, in the restricted dialect and nothing wider. |
| [ConditionDialect](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/ConditionDialect.java) | The cut-down expression language an authorization condition is allowed to be written in. jMouse EL is configurable by whitelist, which is the right foundation — but a whitelist of functions is not enough on its own, because two of the three problems live elsewhere. Everything below is left out deliberately. Escape hatches — a rule that can reach outside itself Removed by registering neither the parser nor the function Feature |
| [ConditionVocabulary](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/ConditionVocabulary.java) | The tokens a condition may be written with — checked before anything is compiled. |
| [FunctionCatalog](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/FunctionCatalog.java) | Every function a condition may call, and the one place all three readers of that list agree. |
| [TestCatalog](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/TestCatalog.java) | Everything an installation lets a policy condition apply with is — and nothing else. FunctionCatalog's counterpart, built for the same three readers that must agree: the compiler builds the dialect from it, ConditionCalls checks names against it at load, and a policy editor offers names from it. |
| [AccessFunction](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/AccessFunction.java) | A function a policy condition may call — and the only type the catalogue will collect . It adds one optional declaration to Function and requires nothing. It exists mostly to be a name, and the name is the whole defence. |
| [AccessTest](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/AccessTest.java) | A test a policy condition may apply — and the only type TestCatalog will collect . The mirror image of AccessFunction, for the shape a function cannot express well. A test, and not a function JMF-1 decided "a function, not a test" and was right — about consumed() . A quota has a threshold, and a threshold belongs in the policy file where a reader can see it: consumed('ai-token', '3h') >= 100000 says what the limit is; is… |
| [ConditionBinding](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/ConditionBinding.java) | The decision a condition is running inside, handed to the functions and tests it calls. |
| [ConsumedFunction](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/ConsumedFunction.java) | consumed('ai-token', '3h') — how much the caller has used, so a rule can compare it. The first AccessFunction, and the one the whole seam was opened for: deny ai:ask when consumed('ai-token', '3h') >= 100000 |
| [CountFunction](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/CountFunction.java) | count('project') — how many of something exist here, right now. @SPACE:'id-space-01' project:create deny when count('project') >= allowance('project') @SPACE:'id-space-01' membership:add deny when count('member', 'SPACE:id-space-01') >= 20 Every seat limit, project limit and member limit is this question, and until now a policy file could not ask it: consumed() answers how much was spent over time and has no way to say how… |
| [AllowanceFunction](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/AllowanceFunction.java) | allowance('ai-token') — how much of a capability this place was actually sold. The number was in two places @SPACE:'id-space-01' assistant:use deny when consumed('ai-token', '3h') >= 100000 100000 is in the policy file. The plan that sells 100 000 is an entitlement row — @ORGANIZATION:acme allow ai-token 100000 reason "…" — with an Allowance on it. So two installations on two plans need two policy files differing by a… |
| [WorkingSchedule](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/WorkingSchedule.java) | When an installation is open — the days of the week, and the hours within them. How it is written * mon-fri 09:00-18:00 a range of days mon,wed,fri 10:00-16:00 a list of days sat-sun 00:00-24:00 a weekend, all day Days are the three-letter English abbreviations — mon tue wed thu fri sat sun — which is the one place here where the short form is the name of the thing rather than a truncation of it. Times are 24-hour HH:mm. |

## Main flow

- Resolve PolicySource objects through PolicySources, parse files, follow includes, detect cycles and combine documents. File location and reading remain the source adapter responsibility. ([PolicyLoader](../jmouse-access-el/src/main/java/org/jmouse/access/el/loader/PolicyLoader.java))

- Read one .jmp document into PolicyDocument. Stage one retains names and quantities as text; registered scope lookup and grant validation happen in the policy binder. ([ExpressionEvaluator](../jmouse-access-el/src/main/java/org/jmouse/access/el/ExpressionEvaluator.java))

- Render a PolicyDocument even when it originated in a database projection rather than in a parsed file. SourceWriter quotes values so the lexer can read the same value again. ([PolicyWriter](../jmouse-access-el/src/main/java/org/jmouse/access/el/PolicyWriter.java))

- Build and use a private restricted language, validate the vocabulary, inspect mentions and return a callable condition. It must not reuse the application-wide unrestricted dialect. ([ExpressionConditionCompiler](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/ExpressionConditionCompiler.java))

- Bind the decision context for functions and tests while a predicate is evaluated. The callable receives declared views of the subject and place and the resource/action/value context. ([ConditionBinding](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/ConditionBinding.java))

- Read consumption through the counter port; CountFunction reads standing cardinality and AllowanceFunction reads the granted ceiling. The policy expression performs the comparison. ([ConsumedFunction](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/ConsumedFunction.java))

## Adoption and extension points

- Register only AccessFunction implementations intended for policy authors. The compiler, checker and discoverable vocabulary need to see one catalogue. ([FunctionCatalog](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/FunctionCatalog.java))

- Register AccessTest implementations for is tests, such as ownedBy, fresh, assured, inside and time-window tests. General expression tests are not implicitly admitted. ([TestCatalog](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/TestCatalog.java))

- Supply the days and hours used by workingHours; the time tests and now function keep time access behind the condition vocabulary. ([WorkingSchedule](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/WorkingSchedule.java))

- Adapt filesystem, resource or other source lookup without moving include composition into the parser. ([PolicySources](../jmouse-access-el/src/main/java/org/jmouse/access/el/loader/PolicySources.java))

## Constraints worth preserving

- An unregistered operator can otherwise leave unconsumed text in the general parser. Token validation is part of rejecting a forbidden condition, not cosmetic editor validation. ([ConditionVocabulary](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/ConditionVocabulary.java))

- The policy dialect deliberately excludes escape hatches and mutation features from the general language. Registering arbitrary Function beans would reopen that boundary. ([ConditionDialect](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/ConditionDialect.java))

- Scope and subject identifiers containing punctuation need quoting. Writing their raw spelling can change how a document is tokenized. ([SourceWriter](../jmouse-access-el/src/main/java/org/jmouse/access/el/SourceWriter.java))

- A file can contain several top-level statements without the optional policy wrapper. Reading only one generic expression would silently lose the rest. ([PolicyDocumentParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/PolicyDocumentParser.java))

- Evaluation exceptions propagate as ConditionFunctionFailure rather than ordinary false. ConditionAxis can then refuse safely for either allow or deny conditions; treating an erroneous deny as false would disable it. ([ExpressionConditionCompiler](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/ExpressionConditionCompiler.java))


## Source navigation

- [`org.jmouse.access.el.condition`](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/)
- [`org.jmouse.access.el`](../jmouse-access-el/src/main/java/org/jmouse/access/el/)
- [`org.jmouse.access.el.lexer`](../jmouse-access-el/src/main/java/org/jmouse/access/el/lexer/)
- [`org.jmouse.access.el.loader`](../jmouse-access-el/src/main/java/org/jmouse/access/el/loader/)
- [`org.jmouse.access.el.node`](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/)
- [`org.jmouse.access.el.parser`](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/)

Resource entry points:

- [jmp-specification.md](../jmouse-access-el/src/main/resources/jmp-specification.md)

## Source reference

Direct dependencies below come from this module's [POM](../jmouse-access-el/pom.xml); test-only dependencies are omitted. Optional dependencies still require the adopter to supply the feature at runtime.

| Dependency | Declared scope | Optional |
|---|---|---|
| `org.jmouse:jmouse-el` | compile | no |
| `org.jmouse:jmouse-access-policy` | compile | no |

### Public type map by package

Top-level public types in production source, excluding smoke/example drivers. Links point to the implementation; roles summarize source JavaDoc or the declared contract. Nested builders, records and enum members belong to their enclosing type.

#### `org.jmouse.access.el`

| Type | Responsibility / contract |
|---|---|
| [CursorMatcher](../jmouse-access-el/src/main/java/org/jmouse/access/el/CursorMatcher.java) | The shape of every .jmp construction, as a lookahead over tokens. |
| [ExpressionEvaluator](../jmouse-access-el/src/main/java/org/jmouse/access/el/ExpressionEvaluator.java) | Reads .jmp source and returns the document it describes. |
| [ExpressionExtension](../jmouse-access-el/src/main/java/org/jmouse/access/el/ExpressionExtension.java) | Everything the .jmp language is made of, registered as one extension. |
| [PolicyCapability](../jmouse-access-el/src/main/java/org/jmouse/access/el/PolicyCapability.java) | What a .jmp destination may declare it can honour — one capability per top-level declaration a policy file holds. |
| [PolicyParseException](../jmouse-access-el/src/main/java/org/jmouse/access/el/PolicyParseException.java) | A .jmp file that will not parse. |
| [PolicySourceTranslator](../jmouse-access-el/src/main/java/org/jmouse/access/el/PolicySourceTranslator.java) | A policy tree written back out as .jmp source — the one way out of the tree for this language. |
| [PolicyWriter](../jmouse-access-el/src/main/java/org/jmouse/access/el/PolicyWriter.java) | A PolicyDocument written as .jmp — including one that was never a file. |
| [SourceReader](../jmouse-access-el/src/main/java/org/jmouse/access/el/SourceReader.java) | Reads positions and raw text back out of a TokenCursor, in the policy language's own terms. |
| [SourceWriter](../jmouse-access-el/src/main/java/org/jmouse/access/el/SourceWriter.java) | Writes values back out in a form the lexer reads the same way — the mirror of SourceReader. |

#### `org.jmouse.access.el.condition`

| Type | Responsibility / contract |
|---|---|
| [AccessFunction](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/AccessFunction.java) | A function a policy condition may call — and the only type the catalogue will collect . |
| [AllowanceFunction](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/AllowanceFunction.java) | allowance('ai-token') — how much of a capability this place was actually sold. |
| [ConditionBinding](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/ConditionBinding.java) | The decision a condition is running inside, handed to the functions and tests it calls. |
| [ConditionDialect](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/ConditionDialect.java) | The cut-down expression language an authorization condition is allowed to be written in. |
| [ConditionVocabulary](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/ConditionVocabulary.java) | The tokens a condition may be written with — checked before anything is compiled. |
| [ConsumedFunction](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/ConsumedFunction.java) | consumed('ai-token', '3h') — how much the caller has used, so a rule can compare it. |
| [CountFunction](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/CountFunction.java) | count('project') — how many of something exist here, right now. |
| [ExpressionConditionCompiler](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/ExpressionConditionCompiler.java) | Compiles a policy condition through jMouse EL, in the restricted dialect and nothing wider. |
| [FunctionCatalog](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/FunctionCatalog.java) | Every function a condition may call, and the one place all three readers of that list agree. |
| [MomentPart](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/MomentPart.java) | The parts of a moment a policy condition may ask now() about. |
| [NowFunction](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/NowFunction.java) | now(part) — the clock, as a value a rule may compare. |
| [TestCatalog](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/TestCatalog.java) | Everything an installation lets a policy condition apply with is — and nothing else. |
| [WorkingSchedule](../jmouse-access-el/src/main/java/org/jmouse/access/el/condition/WorkingSchedule.java) | When an installation is open — the days of the week, and the hours within them. |

#### `org.jmouse.access.el.lexer`

| Type | Responsibility / contract |
|---|---|
| [AccessRecognizer](../jmouse-access-el/src/main/java/org/jmouse/access/el/lexer/AccessRecognizer.java) | Recognizes the three vocabularies a policy file is written in. |
| [AccessToken](../jmouse-access-el/src/main/java/org/jmouse/access/el/lexer/AccessToken.java) | The keywords a .jmp policy file adds on top of the expression language. |

#### `org.jmouse.access.el.loader`

| Type | Responsibility / contract |
|---|---|
| [PolicyLoader](../jmouse-access-el/src/main/java/org/jmouse/access/el/loader/PolicyLoader.java) | Several .jmp files read as one policy. |
| [PolicySource](../jmouse-access-el/src/main/java/org/jmouse/access/el/loader/PolicySource.java) | One policy file, found and read — text, plus enough about where it came from to be talked about. |
| [PolicySources](../jmouse-access-el/src/main/java/org/jmouse/access/el/loader/PolicySources.java) | Where policy files come from — the one thing PolicyLoader does not know how to do. |

#### `org.jmouse.access.el.node`

| Type | Responsibility / contract |
|---|---|
| [ActionDeclarationNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/ActionDeclarationNode.java) | One line of an actions block: entry.listByPurpose "List submissions of one purpose" produces purpose, tier. |
| [ActionsNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/ActionsNode.java) | actions { … } — what this installation's calls say they are doing. |
| [CapabilitiesNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/CapabilitiesNode.java) | capabilities { … } — everything a grant in this installation can be about. |
| [CapabilityDeclarationNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/CapabilityDeclarationNode.java) | One line of a capabilities block: limit seat "Seats" per organization. |
| [EntitlementNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/EntitlementNode.java) | One line of an entitlements block — a bundle, a trial, an allow or a deny, addressed at a place and optionally bounded in time. |
| [EntitlementsNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/EntitlementsNode.java) | entitlements { … } — who is on what, and until when. |
| [GrantNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/GrantNode.java) | One scoped permission: @SCOPE[:instance] permission [allow&#124;deny] [when …]. |
| [IncludeNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/IncludeNode.java) | include 'roles/workspace.jmp' — composition, recorded and not followed. |
| [PaidCapabilitiesNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/PaidCapabilitiesNode.java) | One paid a, b line — capabilities that are closed until something grants them. |
| [PermissionDeclarationNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/PermissionDeclarationNode.java) | One line of a permissions block: form:read "Read forms". |
| [PermissionsNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/PermissionsNode.java) | permissions { … } — the permissions this file says the installation has. |
| [PermissionValueNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/PermissionValueNode.java) | A permission as written: form:read, or form:*. |
| [PlanGrantNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/PlanGrantNode.java) | One line of a plan's body: storage-byte 100GB per month, seat unlimited, or a bare custody. |
| [PlanNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/PlanNode.java) | plan business "Business" order 30 extends team note "…" { … } — one named bundle. |
| [PlansNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/PlansNode.java) | plans { … } — every bundle this installation offers. |
| [PolicyBlockNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/PolicyBlockNode.java) | A { … } block, and the two things every one of them owes its contents. |
| [PolicyNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/PolicyNode.java) | policy "name" { … } — one parsed file, and everything it declared. |
| [RoleAssignmentNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/RoleAssignmentNode.java) | grants SPACE_ADMIN @SPACE:kyiv — this subject holds that role, there. |
| [RoleNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/RoleNode.java) | role SPACE_ADMIN { … } — a bundle of permissions and how far each one reaches. |
| [ScopeDeclarationNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/ScopeDeclarationNode.java) | One line of a scopes block: @SPACE place parameter=spaceId. |
| [ScopesNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/ScopesNode.java) | scopes { … } — the floors this file says the installation has. |
| [SingleScopeNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/SingleScopeNode.java) | A scope as written: @SPACE, or @SPACE:kyiv. |
| [SourceSpanNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/SourceSpanNode.java) | Where in the file a construction was written, as a node. |
| [SubjectNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/SubjectNode.java) | subject u-42 { … } — one account's role assignments and personal grants. |
| [VariableDeclarationNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/VariableDeclarationNode.java) | One line of a variables block: constant deployment "Which deployment this is". |
| [VariablesNode](../jmouse-access-el/src/main/java/org/jmouse/access/el/node/VariablesNode.java) | variables { … } — what is true of every call this installation decides. |

#### `org.jmouse.access.el.parser`

| Type | Responsibility / contract |
|---|---|
| [ActionDeclarationParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/ActionDeclarationParser.java) | Parses one line of an actions block: entry.listByPurpose "List submissions of one purpose" produces purpose, tier. |
| [ActionsParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/ActionsParser.java) | Parses actions { … } — the block that states what this installation's calls do. |
| [CapabilitiesParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/CapabilitiesParser.java) | Parses capabilities { … } — the block that states what a grant in this installation can be about. |
| [CapabilityDeclarationParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/CapabilityDeclarationParser.java) | Parses one line of a capabilities block: quota storage-byte "Storage written" per organization, space. |
| [ConditionReader](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/ConditionReader.java) | Reads what follows when, and returns it exactly as it was typed . |
| [EntitlementParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/EntitlementParser.java) | Parses one line of an entitlements block: @ORGANIZATION:acme allow custody from 2026-08-01 until 2026-09-01 reason "…". |
| [EntitlementsParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/EntitlementsParser.java) | Parses entitlements { … } — who is on what. |
| [GrantParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/GrantParser.java) | Parses one scoped permission: @SCOPE[:instance] permission [allow&#124;deny] [when …]. |
| [IncludeParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/IncludeParser.java) | Parses include 'bootstrap.jmp' — a path recorded, never followed. |
| [PaidCapabilitiesParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/PaidCapabilitiesParser.java) | Parses paid custody, parametric-search — the capabilities closed until something grants them. |
| [ParserPriority](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/ParserPriority.java) | The order the policy parsers are offered a statement in, and why that order is what it is. |
| [PermissionDeclarationParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/PermissionDeclarationParser.java) | Parses one line of a permissions block: form:read "Read forms". |
| [PermissionsParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/PermissionsParser.java) | Parses permissions { … } — the block that states this installation's permissions. |
| [PermissionValueParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/PermissionValueParser.java) | Parses a permission: form:read, or form:* for a whole namespace. |
| [PlanGrantParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/PlanGrantParser.java) | Parses one line of a plan's body: storage-byte 100GB per month, seat unlimited, or a bare custody. |
| [PlanParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/PlanParser.java) | Parses one bundle's header: plan business "Business" order 30 extends team note "…". |
| [PlansParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/PlansParser.java) | Parses plans { … } — the block holding every bundle. |
| [PolicyBlockParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/PolicyBlockParser.java) | Base for every { … } block in the policy language. |
| [PolicyDocumentParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/PolicyDocumentParser.java) | Reads a whole .jmp file, in either of the two shapes one may be written in. |
| [PolicyParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/PolicyParser.java) | Parses policy "name" { … } — the optional wrapper around a whole file. |
| [RoleAssignmentParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/RoleAssignmentParser.java) | Parses grants SPACE_ADMIN @SPACE:kyiv [when …] — a role assignment inside a subject. |
| [RoleParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/RoleParser.java) | Parses role SPACE_ADMIN { … }. |
| [ScopeDeclarationParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/ScopeDeclarationParser.java) | Parses one line of a scopes block: @SPACE place parameter=spaceId inside=@ORGANIZATION. |
| [ScopesParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/ScopesParser.java) | Parses scopes { … } — the block that states this installation's floors. |
| [SingleScopeParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/SingleScopeParser.java) | Parses a scope reference: @SPACE, @SPACE:kyiv, @SPACE:* or @SPACE:${default.space}. |
| [SubjectParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/SubjectParser.java) | Parses subject u-42 { … }. |
| [VariableDeclarationParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/VariableDeclarationParser.java) | Parses one line of a variables block: dynamic ambientType "What the workspace this call is in counts". |
| [VariablesParser](../jmouse-access-el/src/main/java/org/jmouse/access/el/parser/VariablesParser.java) | Parses variables { … } — the block that states what is true of every call. |

### Packaged resources

See [packaged resources](../jmouse-access-el/src/main/resources/) for configuration/templates owned by this module.

### Verification boundary

This documentation was checked against the current source tree. No Java build, database migration, external API call or runtime scenario is claimed by this documentation change.

Existing checks/examples to consult when changing this API:

- [ExchangeSmoke.java](../jmouse-access-el/src/main/java/org/jmouse/access/el/ExchangeSmoke.java).
- [PolicyCorpusSmoke.java](../jmouse-access-el/src/main/java/org/jmouse/access/el/PolicyCorpusSmoke.java).
- [Smoke.java](../jmouse-access-el/src/main/java/org/jmouse/access/el/Smoke.java).
