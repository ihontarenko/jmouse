
-- =============================================================================
--  V000001  The access engine's own schema
--
--  The library migrates itself, against a history table of its own, exactly as
--  jmouse-storage-jpa already does. Adoption then costs a dependency rather than
--  a negotiation about migration numbering: products in one workspace number
--  their ranges incompatibly, and a shared history would force either a reserved
--  range held forever by convention or somebody renumbering a schema that has
--  already shipped.
--
--  ⚠️ APPEND-ONLY FROM THIS FILE ONWARDS. The workspace rule that a Flyway file
--  may be edited in place applies to a product whose database can be dropped —
--  not to a library other people's data has run. Every future column on a role
--  is an ALTER TABLE in a library release. This is the price of the whole move
--  and it was accepted deliberately.
--
--  ⚠️ NOT HERE, AND EACH FOR A REASON:
--
--    memberships    — no axis reads them. "Is a member" is an assignment row at
--                     a place, and the tables carry product data (last_visited_at)
--                     with an index read on every sign-in. The SPI could not serve
--                     them anyway: it deliberately cannot enumerate, and "who is
--                     in this place" is exactly an enumeration.
--    counters       — consumption, written on EVERY metered action. An
--                     authorization library owning a write on the hot path of
--                     every adopting product is the wrong trade at any price.
--    subject areas  — a product's taxonomy. The ceiling it enforces is code, not
--                     a column here.
--
--  ⚠️ THREE THINGS THAT STOPPED BEING TRUE WHEN THESE TABLES MOVED:
--
--   1. NO FOREIGN KEY INTO A PRODUCT'S ACCOUNT TABLE. A library table cannot
--      reference one. Deleting an account therefore no longer cascades its
--      grants, and the gap is SILENT if forgotten — orphan grants sit here and
--      a projection reports them. Revoking is application code now.
--
--   2. NO CHECK ON A SCOPE. `scope_type` enumerated one product's floors;
--      another product's are different, and a third has none. Validation moved
--      entirely to boot, where the vocabulary is registered and a nonsense scope
--      fails loudly with the file and line that wrote it. The database stops
--      being the last line of defence, on purpose.
--
--   3. NO PERMISSION TABLE. A permission is stored BY NAME. The surrogate-id
--      table it replaces was a third copy of a list the code and the policy
--      document both already state, existing only to hand out 'PFR'-style keys —
--      and a row here is now readable without a join.
-- =============================================================================


-- ── access_roles ──────────────────────────────────────────────────────────────
--  A role is a named bundle of permissions and nothing else. `assignable_at` is
--  the widest place it may be handed out at, kept as a plain name because the
--  vocabulary is the product's.
CREATE TABLE access_roles
(
    id            VARCHAR(36) NOT NULL,
    role_name     VARCHAR(64) NOT NULL,
    assignable_at VARCHAR(64) NOT NULL DEFAULT 'GLOBAL',

    CONSTRAINT access_roles_pk          PRIMARY KEY (id),
    CONSTRAINT access_roles_unique_name UNIQUE (role_name)
);


-- ── access_role_permissions ───────────────────────────────────────────────────
--  What a bundle carries, and how far.
--
--  ⚠️ `scope_type` here is a KIND and never an instance. "@SPACE space:write"
--  inside a role means "as far as a place of that kind"; which place is decided
--  by where the role is assigned. A role naming an instance would grant it there
--  to everybody holding the role — the language refuses to express it, and this
--  table has no column to hold it.
--
--  ⚠️ `condition_source` is the expression a bundle entry only applies under, in
--  the policy dialect, or NULL where it always applies. Named for what it holds:
--  SOURCE TEXT, compiled on the way in and never on the way out. A row whose
--  condition names an action nothing publishes is refused at write time, because
--  a grant that can never fire is worse than one that was refused — nothing
--  anywhere says it is not working.
--
--  (`condition` alone is a reserved word in MySQL, which is the smaller reason.)
CREATE TABLE access_role_permissions
(
    role_id          VARCHAR(36)   NOT NULL,
    permission       VARCHAR(128)  NOT NULL,
    scope_type       VARCHAR(64)   NOT NULL DEFAULT 'GLOBAL',
    condition_source VARCHAR(1024),

    CONSTRAINT access_role_permissions_pk      PRIMARY KEY (role_id, permission),
    CONSTRAINT access_role_permissions_fk_role FOREIGN KEY (role_id)
        REFERENCES access_roles (id) ON DELETE CASCADE
);


-- ── access_role_assignments ───────────────────────────────────────────────────
--  Who holds a role, and where.
--
--  ⚠️ `subject_id`, not `user_id`. The SPI has always said SUBJECT, and a service
--  account or an agent is one — `access_user_permissions` would name a concept
--  this library does not have.
--
--  `scope_id` defaults to '*', which means "every place of this kind" rather than
--  a place called star. A universal scope writes it too, because a scope with no
--  instances still needs the unique key to have a value.
CREATE TABLE access_role_assignments
(
    id         VARCHAR(36) NOT NULL,
    subject_id VARCHAR(36) NOT NULL,
    role_id    VARCHAR(36) NOT NULL,
    scope_type VARCHAR(64) NOT NULL DEFAULT 'GLOBAL',
    scope_id   VARCHAR(36) NOT NULL DEFAULT '*',
    -- Kept as a plain name rather than a CHECK: a product may issue an assignment
    -- from a mechanism this library has never heard of, and refusing the row would
    -- be the library legislating about the product's own provenance vocabulary.
    source     VARCHAR(32) NOT NULL DEFAULT 'DIRECT',
    -- ⚠️ The expression this assignment only applies under, or NULL. See
    -- access_role_permissions for why it is stored as source and compiled on write.
    condition_source VARCHAR(1024),
    -- ⚠️ A plain identifier with nothing behind it. A granter whose account is
    -- deleted must leave the grant STANDING and legible — "granted by somebody who
    -- has since left" is the honest record, and a SET NULL would erase the one
    -- fact an audit asks for.
    granted_by VARCHAR(36),
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT access_role_assignments_pk      PRIMARY KEY (id),
    CONSTRAINT access_role_assignments_unique  UNIQUE (subject_id, role_id, scope_type, scope_id),
    CONSTRAINT access_role_assignments_fk_role FOREIGN KEY (role_id)
        REFERENCES access_roles (id) ON DELETE CASCADE
);

-- Resolution reads every assignment one subject holds, then narrows to the chain.
CREATE INDEX index_access_role_assignments_subject ON access_role_assignments (subject_id);
CREATE INDEX index_access_role_assignments_scope   ON access_role_assignments (scope_type, scope_id);


-- ── access_subject_permissions ────────────────────────────────────────────────
--  One permission handed to, or taken from, one subject directly.
--
--  ⚠️ `effect` is the whole reason this table is not a list of grants. DENY wins
--  over every ALLOW, from anywhere, at every level — so a row here is how an
--  administrator takes something away that a role gives, without editing the role
--  that gives it to everybody else.
--
--  `reason` is not decoration: a denial nobody can explain is a support ticket
--  that outlives whoever wrote it.
--
--  ⚠️ `subject_id` may be the reserved value '*', meaning EVERY subject. It is
--  matched by one extra predicate when a subject is read, never expanded into a
--  row per account — an expansion would grow with every registration and would
--  have to be maintained by whatever creates accounts. The CHECK is what keeps it
--  honest: '*' may only ever DENY. A universal allow written by accident appears
--  on no screen that lists what one person holds, so the row is refused by the
--  database rather than by whichever writer remembered.
CREATE TABLE access_subject_permissions
(
    id         VARCHAR(36)  NOT NULL,
    subject_id VARCHAR(36)  NOT NULL,
    permission VARCHAR(128) NOT NULL,
    effect     VARCHAR(8)   NOT NULL DEFAULT 'ALLOW'
                   CHECK (effect IN ('ALLOW', 'DENY')),
    scope_type VARCHAR(64)  NOT NULL DEFAULT 'GLOBAL',
    scope_id   VARCHAR(36)  NOT NULL DEFAULT '*',
    -- ⚠️ The expression this grant only applies under, or NULL. See
    -- access_role_permissions for why it is stored as source and compiled on write.
    condition_source VARCHAR(1024),
    reason     VARCHAR(512),
    granted_by VARCHAR(36),
    created_at TIMESTAMP    NOT NULL,

    CONSTRAINT access_subject_permissions_everybody_denies_only
        CHECK (subject_id <> '*' OR effect = 'DENY'),
    CONSTRAINT access_subject_permissions_pk     PRIMARY KEY (id),
    CONSTRAINT access_subject_permissions_unique UNIQUE (subject_id, permission, scope_type, scope_id)
);

CREATE INDEX index_access_subject_permissions_subject ON access_subject_permissions (subject_id);


-- ── access_capability_grants ──────────────────────────────────────────────────
--  The entitlement axis: what a PLACE has been granted, by whom, until when.
--
--  ⚠️ NO UNIQUE KEY over (place, capability), deliberately. A tier's allow, a
--  purchased top-up and an administrator's deny are three true facts with three
--  provenances, and collapsing them would make the answer depend on which row
--  won a race rather than on the rule.
--
--  ⚠️ A NULL allowance means NO CEILING where the capability is metered, and
--  "not metered" where it is not. The two readings are opposite and the period
--  is what separates them — see the resolver, which is the only thing that may
--  decide it.
--
--  ⚠️ EXPIRED ROWS ARE NOT DELETED and must not be filtered in a query. "Ran
--  until 12 September" and "never had it" are different answers, and a predicate
--  in the query collapses them into the same silence.
CREATE TABLE access_capability_grants
(
    id               VARCHAR(36)  NOT NULL,
    scope_type       VARCHAR(64)  NOT NULL,
    scope_id         VARCHAR(36)  NOT NULL,
    capability       VARCHAR(64)  NOT NULL,
    grant_kind       VARCHAR(8)   NOT NULL
                         CHECK (grant_kind IN ('ALLOW', 'DENY')),
    -- A product's own provenance vocabulary, and no CHECK for the same reason as
    -- above: a product may issue internal allocations nobody pays for, and should
    -- never have to name them after a payment it does not take.
    source           VARCHAR(32)  NOT NULL,
    -- ⚠️ WHICH bundle issued this, where one did. Without it, "what is this
    -- account on" has no answer at all once a plan column is gone — and a column
    -- holding it beside these rows is a second copy of what they already say.
    source_reference VARCHAR(64)  NULL,
    valid_from       TIMESTAMP    NOT NULL,
    valid_until      TIMESTAMP    NULL,
    allowance        BIGINT       NULL,
    allowance_period VARCHAR(8)   NULL
                         CHECK (allowance_period IN ('DAY', 'MONTH', 'YEAR', 'EVER')),
    reason           VARCHAR(500) NULL,
    granted_by       VARCHAR(36)  NULL,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,

    CONSTRAINT access_capability_grants_pk PRIMARY KEY (id)
);

CREATE INDEX index_access_capability_grants_place  ON access_capability_grants (scope_type, scope_id);
CREATE INDEX index_access_capability_grants_source ON access_capability_grants (source);


-- ── access_capability_switches ────────────────────────────────────────────────
--  What a place has turned OFF — axis 4, and it is a preference rather than a
--  grant.
--
--  ⚠️ The distinction is why this is its own table and its own SPI. A grant says
--  what a place MAY have and is issued by whoever decides that; a switch says
--  what it WANTS and is flipped by whoever works there. Forcing a switch into a
--  grant's shape would hand it an allowance, a validity window and a provenance
--  it has no use for, and would put an ordinary settings-screen write behind an
--  interface that is read-only precisely because issuing is a governed act.
--
--  ⚠️ DEFAULT-ON is the engine's assumption and this table names the exceptions.
--  A table listing what is ON would have to know the catalogue to be complete,
--  and would answer wrongly for every capability introduced after its rows were
--  written — a feature shipped next year would arrive switched off everywhere,
--  silently, with nobody having decided that.
CREATE TABLE access_capability_switches
(
    id         VARCHAR(36) NOT NULL,
    scope_type VARCHAR(64) NOT NULL,
    scope_id   VARCHAR(36) NOT NULL,
    capability VARCHAR(64) NOT NULL,
    enabled    BOOLEAN     NOT NULL DEFAULT TRUE,
    -- Locked on, so that nobody's personal tidying-up can take it away.
    forced     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,

    CONSTRAINT access_capability_switches_pk     PRIMARY KEY (id),
    CONSTRAINT access_capability_switches_unique UNIQUE (scope_type, scope_id, capability)
);


-- ── access_policy_revisions ───────────────────────────────────────────────────
--  The editable policy document, and every version of it that ever was.
--
--  ⚠️ THIS IS NOT AN AUDIT TRAIL, and the mistake is worth naming because it
--  looks like a simplification. What is stored here is the document IN FORCE —
--  the highest version is read at every boot and put back in force. A log cannot
--  answer "what is authorization right now", and a product's audit log
--  specifically must not try: those rows have retention, and a retention job that
--  deleted an installation's authorization would be the worst possible outcome of
--  a tidy-up. The audit row beside a save records WHO changed it and carries the
--  diff; this holds WHAT it is.
--
--  NOTHING IS EVER OVERWRITTEN. A save appends; reverting appends the OLD TEXT AS
--  A NEW VERSION rather than deleting rows, so the history of an installation's
--  authorization reads forward and a revert is itself an event somebody can see.
--
--  The counts are stored so a revision list can say how big each version is
--  without parsing every one of them to draw a list.
CREATE TABLE access_policy_revisions
(
    id            VARCHAR(36)  NOT NULL,
    -- Dense and increasing, assigned by the application. A number somebody can say
    -- out loud ("we broke it in 14") is worth more than a timestamp nobody can.
    version       INT          NOT NULL,
    source        TEXT         NOT NULL,
    note          VARCHAR(512),
    -- ⚠️ Kept by identifier AND by a label. The account may be deleted and the
    -- history must still say who did this — which a foreign key could not promise
    -- even before library tables stopped having them.
    author_id     VARCHAR(36),
    author_label  VARCHAR(255),
    reverted_from INT,
    roles         INT          NOT NULL DEFAULT 0,
    subjects      INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL,

    CONSTRAINT access_policy_revisions_pk             PRIMARY KEY (id),
    CONSTRAINT access_policy_revisions_unique_version UNIQUE (version)
);

-- The only two reads there are: "what is in force" (the highest version) and
-- "show me the history" (all of them, newest first). One index answers both.
CREATE INDEX index_access_policy_revisions_version ON access_policy_revisions (version DESC);


-- ── access_plans ──────────────────────────────────────────────────────────────
--  The TEMPLATE catalogue: which tiers exist and what each one includes.
--
--  ⚠️ NOT a grant source and NOT a membership record. Nothing joins an account to
--  a row here, and nothing here answers "what may this account do". Who is on a
--  tier is an access_capability_grants row carrying the tier as its
--  source_reference, which is exactly where ADR-0018 left it.
--
--  ⚠️ This does not resurrect what ADR-0018 deleted. The defect there was ONE FACT
--  WRITTEN TWICE — the grants said who was on a tier and organizations.plan_id
--  said it again. A catalogue of templates duplicates nothing, because no row
--  anywhere points at it as an answer. The similar name is exactly why this
--  paragraph exists.
--
--  ⚠️ Editing a tier does not reissue it. A grant is materialised from a template
--  at the moment of assignment and is a row from then on; changing what Business
--  includes changes what the NEXT account put on Business receives and nothing
--  about the accounts already on it. Any screen editing this has to say so.
CREATE TABLE access_plans
(
    code          VARCHAR(64)  NOT NULL,
    display_name  VARCHAR(128) NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    note          VARCHAR(512),
    -- The tier this one is written as a difference from, or NULL. Resolved on
    -- READ rather than flattened on write, so an editor can show an inherited
    -- line as inherited — which flattening would destroy, and which is most of
    -- what makes a ladder legible.
    extends_code  VARCHAR(64),

    CONSTRAINT access_plans_pk PRIMARY KEY (code)
);


-- ── access_plan_grants ────────────────────────────────────────────────────────
--  One capability a tier includes, with its ceiling where it has one.
--
--  ⚠️ `quantity` is TEXT and not a number, deliberately. A tier says `storage-byte
--  100GB` three lines from `workspace 25`, and only one of those is a size — what
--  `GB` means is the product's to know, not this library's. Parsing it here would
--  make the schema hold an opinion about units it cannot have.
--
--  No ceiling and no `unlimited` means a GATE: the capability is included and
--  nothing counts it.
CREATE TABLE access_plan_grants
(
    plan_code  VARCHAR(64) NOT NULL,
    capability VARCHAR(64) NOT NULL,
    quantity   VARCHAR(32),
    period     VARCHAR(8)
                   CHECK (period IN ('DAY', 'MONTH', 'YEAR', 'EVER')),
    unlimited  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT access_plan_grants_pk      PRIMARY KEY (plan_code, capability),
    CONSTRAINT access_plan_grants_fk_plan FOREIGN KEY (plan_code)
        REFERENCES access_plans (code) ON DELETE CASCADE
);
