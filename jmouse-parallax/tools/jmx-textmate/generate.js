// Builds the jMX TextMate package from jMS's grammar. jMX is a document language around two embedded ones: jMS
// inside `behaviour` and `run`, jMF everywhere an expression stands. The grammar is jMS's repository renamed to
// `.jmx`, with jMX's documents and blocks added on top — so a jMS body is coloured by jMS's own rules, unchanged.
//
// ⚠️ Generated, never hand-edited. Run from the jmouse root, and again whenever jMS's grammar changes:
//   node jmouse-parallax/tools/jmx-textmate/generate.js jmouse-script-el/tools/jms-textmate jmouse-parallax/tools/jmx-textmate
//
// The truth about the syntax is the reader in org.jmouse.parallax.script: Kind (the kinds and their blocks),
// DocumentReader, DeclarationReader, CircuitReader, SymbolReader, PresentationReader, StructureReader, Quantity and
// ExperienceNotation. A word list here changes when one there does.
const fs = require("fs");
const path = require("path");
const [source, target] = process.argv.slice(2);

if (!source || !target) {
  console.error("usage: node generate.js <jms-textmate directory> <jmx-textmate directory>");
  process.exit(1);
}

const jms = JSON.parse(fs.readFileSync(path.join(source, "syntaxes", "jms.tmLanguage.json"), "utf8"));
const rename = value => JSON.parse(JSON.stringify(value).replace(/\.jms"/g, '.jmx"'));
const repository = rename(jms.repository);

for (const required of ["comment", "statement", "string", "function-call", "identifier"]) {
  if (!repository[required]) {
    throw new Error(`jMS's grammar no longer has #${required}; jMX's rules include it — update generate.js`);
  }
}

// ─── vocabulary, from the reader ────────────────────────────────────────────────────────────────────

/** Kind.java: every word a document may open with. */
const KINDS = [
  "curve", "surface", "hypersurface", "field", "dynamics", "iteration", "mapping", "region", "value",
  "iterated_function_system", "lindenmayer_system", "matrix", "points", "construction",
  "polytope", "program", "component", "circuit", "symbol", "family",
  "functions", "category", "collection", "course", "instrument", "graph"
];
/** Block.PaneKind and Block.PaneLayout: what a graph's pane draws on, and how the panes share the screen. */
const PANE_KINDS = ["plane", "space", "circuit", "values"];
const PANE_LAYOUTS = ["rows", "columns", "grid"];
/** Block.OutputType. */
const OUTPUT_TYPES = ["scalar", "series", "curve", "surface", "volume", "field", "image", "cloud", "path", "mesh",
  "matrix", "table", "signal", "logic", "density"];
/** Capability.java: what `without` may withhold. */
const CAPABILITIES = ["play", "run", "interact", "resample", "probe", "sweep", "check", "execute"];
/** ComponentCheck: what `measure` reads. */
const MEASUREMENTS = ["operating", "final", "levels", "maximum", "minimum", "average", "frequency", "duty", "time"];

// ─── shared pieces of regular expressions ───────────────────────────────────────────────────────────

/** Where a statement may begin: the start of a line, or after `;` or `{` on the same one. */
const LINE = "(?:^|(?<=[;{]))[ \\t]*";
/** Where a line-shaped statement ends: before `;`, a comment, a closing brace, or the end of the line. */
const LINE_END = "(?=[;#}]|$)";
const NAME = "[A-Za-z_][A-Za-z0-9_]*";
/** A slug, a hyphenated role or a reference, optionally pinned to a revision: `lm358@3`, `74hc595`, `flip-flop`. */
const SLUG = "[A-Za-z0-9][A-Za-z0-9_-]*(?:@[0-9]+)?";
const STRING = "(\"[^\"]*\"|'[^']*')";
const words = list => `(?:${list.join("|")})`;

// Quantity.java: a prefix stands only in front of a prefixable unit; standalone units take none. A compound unit is
// factors joined by * or /, each with an optional power, or 1/factor. ExperienceNotation turns Ω µ ° into Ohm u deg
// before the reader sees them, so the symbols are accepted here as written.
const PREFIX = "[fpnuµμmkMGT]";
const PREFIXABLE = "(?:Ohm|Hz|Pa|Ω|V|A|F|H|s|W|J|K|S|C|g|m|N)";
const STANDALONE = "(?:degC|deg|rad|dB|ppm|°C|°|%)";
const FACTOR = `(?:${PREFIX}?${PREFIXABLE}|${STANDALONE})(?:\\^-?[0-9]+)?`;
const UNIT = `(?:1/${FACTOR}|${FACTOR})(?:[*/]${FACTOR})*(?![A-Za-z0-9_°Ω])`;
const NUMBER = "(?<![A-Za-z0-9_])(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?";

// ─── builders ───────────────────────────────────────────────────────────────────────────────────────

const include = name => ({ include: `#${name}` });
const keyword = name => ({ name: `keyword.control.block.${name}.jmx` });
const blockBegin = { name: "punctuation.section.block.begin.jmx" };
const blockEnd = { "0": { name: "punctuation.section.block.end.jmx" } };

/** `word { lines }`: a block whose braces open on the line of its word. */
const braceBlock = (word, scope, patterns) => ({
  name: `meta.block.${scope}.jmx`,
  begin: `${LINE}\\b(${word})[ \\t]*(\\{)`,
  beginCaptures: { "1": keyword(scope), "2": blockBegin },
  end: "\\}",
  endCaptures: blockEnd,
  patterns
});

/** `word rest of the line`: a block written on one line, ending at `;`, a comment, `}` or the end of the line. */
const lineBlock = (word, scope, patterns) => ({
  name: `meta.line.${scope}.jmx`,
  begin: `${LINE}\\b(${word})\\b`,
  beginCaptures: { "1": keyword(scope) },
  end: LINE_END,
  patterns
});

/** `name =` at the start of a statement: what a line declares. */
const declares = scope => ({
  match: `${LINE}(${NAME})[ \\t]*(=)(?!=)`,
  captures: { "1": { name: scope }, "2": { name: "keyword.operator.assignment.jmx" } }
});

/** jMX's rules go beside jMS's, never over them: an overwritten jMS rule would change how a jMS body is coloured. */
const addRules = rules => {
  for (const [name, rule] of Object.entries(rules)) {
    if (repository[name]) {
      throw new Error(`#${name} is a jMS rule; give jMX's rule another name`);
    }
    repository[name] = rule;
  }
};

addRules({

  // ─── text ────────────────────────────────────────────────────────────────────────────────────────

  "long-text": {
    comment: "ExperienceNotation: \"\"\" … \"\"\" spans lines and is markdown; it must be tried before a one-line string.",
    name: "string.quoted.triple.jmx",
    begin: "\"\"\"",
    beginCaptures: { "0": { name: "punctuation.definition.string.begin.jmx" } },
    end: "\"\"\"",
    endCaptures: { "0": { name: "punctuation.definition.string.end.jmx" } }
  },
  "text": { patterns: [include("long-text"), include("string")] },

  // ─── documents ───────────────────────────────────────────────────────────────────────────────────

  "document": {
    comment: "kind \"Title\" [as slug] [type role] { … }: a kind's word followed by a quoted title. Only `set` may hold documents, but every body includes this rule so a nested one is coloured as a document rather than as noise.",
    name: "meta.document.jmx",
    begin: `${LINE}\\b(${words(KINDS)})\\b[ \\t]*${STRING}((?:[ \\t]+(?:as|type)[ \\t]+${SLUG})*)[ \\t]*(\\{)?`,
    beginCaptures: {
      "1": { name: "keyword.control.document.jmx" },
      "2": { name: "entity.name.type.document.jmx" },
      "3": { patterns: [include("document-identity")] },
      "4": blockBegin
    },
    end: "\\}",
    endCaptures: blockEnd,
    patterns: [include("document-body")]
  },
  "symbol-document": {
    comment: "A symbol document's body is a drawing written without the braces of a symbol block: size, path, pin, label.",
    name: "meta.document.symbol.jmx",
    begin: `${LINE}\\b(symbol)\\b[ \\t]*${STRING}((?:[ \\t]+(?:as|type)[ \\t]+${SLUG})*)[ \\t]*(\\{)?`,
    beginCaptures: {
      "1": { name: "keyword.control.document.jmx" },
      "2": { name: "entity.name.type.document.jmx" },
      "3": { patterns: [include("document-identity")] },
      "4": blockBegin
    },
    end: "\\}",
    endCaptures: blockEnd,
    patterns: [include("comment"), include("meta-block"), include("drawing")]
  },
  "document-identity": {
    patterns: [
      { match: `\\b(as)[ \\t]+(${SLUG})`, captures: { "1": { name: "keyword.other.as.jmx" }, "2": { name: "entity.name.tag.slug.jmx" } } },
      { match: `\\b(type)[ \\t]+(${SLUG})`, captures: { "1": { name: "keyword.other.type.jmx" }, "2": { name: "entity.name.tag.role.jmx" } } }
    ]
  },
  "document-body": {
    patterns: [
      include("comment"),
      include("symbol-document"),
      include("document"),
      include("meta-block"),
      include("header-line"),
      include("formulas-block"),
      include("formulas-line"),
      include("parameters-block"),
      include("inputs-block"),
      include("outputs-block"),
      include("state-block"),
      include("equations-block"),
      include("image-block"),
      include("display-block"),
      include("measure-block"),
      include("leads-block"),
      include("pins-block"),
      include("nodes-block"),
      include("behaviour-body"),
      include("run-body"),
      include("view-block"),
      include("expect-block"),
      include("symbol-uses"),
      include("symbol-shape"),
      include("symbol-drawing"),
      include("parts-block"),
      include("layout-block"),
      include("nets-block"),
      include("ports-block"),
      include("probes-block"),
      include("analysis-line"),
      include("sheet-line"),
      include("clock-line"),
      include("panes-block"),
      include("lines-block"),
      include("maps-block"),
      include("axiom-line"),
      include("rules-block"),
      include("turtle-block"),
      include("steps-block"),
      include("vertices-line"),
      include("edges-line"),
      include("points-block"),
      include("items-line"),
      include("instrument-line"),
      include("collection-section"),
      include("course-lesson"),
      include("course-step"),
      { match: "\\{", name: "punctuation.section.block.begin.jmx" }
    ]
  },

  // ─── what a document says about itself ───────────────────────────────────────────────────────────

  "header-line": {
    comment: "Kind.DECLARATIONS — extends ref, use a, b and units A, B: top-level lines that change what the document computes.",
    patterns: [
      {
        name: "meta.line.header.jmx",
        begin: `${LINE}\\b(extends|use)\\b`,
        beginCaptures: { "1": { name: "keyword.other.header.jmx" } },
        end: LINE_END,
        patterns: [
          { match: ",", name: "punctuation.separator.comma.jmx" },
          { match: SLUG, name: "entity.name.type.reference.jmx" }
        ]
      },
      {
        comment: "type power_density = power / area: a quantity the document names; every word in it is a type.",
        name: "meta.line.type.jmx",
        begin: `${LINE}\\b(type)[ \\t]+(${NAME})[ \\t]*(=)`,
        beginCaptures: {
          "1": { name: "keyword.other.header.jmx" },
          "2": { name: "entity.name.type.annotation.jmx" },
          "3": { name: "keyword.operator.assignment.jmx" }
        },
        end: LINE_END,
        patterns: [
          { match: "[*/^]", name: "keyword.operator.jmx" },
          { match: "\\b[0-9]+\\b", name: "constant.numeric.jmx" },
          { match: NAME, name: "entity.name.type.annotation.jmx" }
        ]
      },
      {
        name: "meta.line.header.jmx",
        begin: `${LINE}\\b(units)\\b`,
        beginCaptures: { "1": { name: "keyword.other.header.jmx" } },
        end: LINE_END,
        patterns: [
          { match: ",", name: "punctuation.separator.comma.jmx" },
          { match: NAME, name: "entity.name.tag.unit-name.jmx" }
        ]
      }
    ]
  },
  "meta-block": {
    name: "meta.block.meta.jmx",
    begin: `${LINE}\\b(meta)[ \\t]*(\\{)`,
    beginCaptures: { "1": keyword("meta"), "2": blockBegin },
    end: "\\}",
    endCaptures: blockEnd,
    patterns: [
      include("comment"),
      {
        name: "meta.block.links.jmx",
        begin: `${LINE}\\b(links)[ \\t]*(\\{)`,
        beginCaptures: { "1": { name: "keyword.other.meta-field.jmx" }, "2": blockBegin },
        end: "\\}",
        endCaptures: blockEnd,
        patterns: [
          include("comment"),
          {
            match: "(\"[^\"]*\")[ \\t]+(\"[^\"]*\")",
            captures: {
              "1": { name: "string.quoted.double.label.jmx" },
              "2": { name: "string.quoted.double.link.jmx markup.underline.link.jmx" }
            }
          },
          include("string")
        ]
      },
      {
        comment: "The fields the reader knows and checks.",
        match: `${LINE}\\b(summary|description|category|tags|source|authors|license)\\b`,
        captures: { "1": { name: "keyword.other.meta-field.jmx" } }
      },
      {
        comment: "Any other field: 'name value', kept with its type.",
        match: `${LINE}(${NAME})`,
        captures: { "1": { name: "variable.other.meta-field.jmx" } }
      },
      include("text"),
      include("quantity"),
      include("range"),
      { match: "[\\[\\],]", name: "punctuation.separator.jmx" },
      { match: SLUG, name: "entity.name.tag.jmx" }
    ]
  },

  // ─── declarations ────────────────────────────────────────────────────────────────────────────────

  "parameters-block": braceBlock("parameters", "parameters", [
    include("comment"),
    include("text"),
    declares("variable.other.parameter.jmx"),
    { match: "\\b(in|step)\\b", name: "keyword.other.marker.jmx" },
    { match: "\\b(log|integer|animate|live|exact)\\b", name: "storage.modifier.parameter.jmx" },
    { match: "\\b(per)[ \\t]+(unit)\\b", name: "storage.modifier.parameter.jmx" },
    include("formula")
  ]),
  "inputs-block": braceBlock("inputs", "inputs", [
    include("comment"),
    include("text"),
    {
      comment: "x in …, or (z) / (x, y) for a plane.",
      match: `${LINE}(?:(\\()([^)]*)(\\))|(${NAME}))`,
      captures: {
        "1": { name: "punctuation.section.parens.begin.jmx" },
        "2": { patterns: [{ match: NAME, name: "variable.parameter.input.jmx" }, { match: ",", name: "punctuation.separator.comma.jmx" }] },
        "3": { name: "punctuation.section.parens.end.jmx" },
        "4": { name: "variable.parameter.input.jmx" }
      }
    },
    { match: "\\b(in|step)\\b", name: "keyword.other.marker.jmx" },
    { match: "\\b(integer)\\b", name: "storage.modifier.input.jmx" },
    include("formula")
  ]),
  "outputs-block": braceBlock("outputs", "outputs", [
    include("comment"),
    include("text"),
    {
      comment: "name [: type] then = expression, where equation, a description or nothing.",
      match: `${LINE}(${NAME})(?:[ \\t]*(:)[ \\t]*(${NAME}))?(?:[ \\t]*(=)(?!=))?`,
      captures: {
        "1": { name: "variable.other.output.jmx" },
        "2": { name: "punctuation.separator.type.jmx" },
        "3": {
          patterns: [
            { match: `\\b${words(OUTPUT_TYPES)}\\b`, name: "support.type.output.jmx" },
            { match: NAME, name: "invalid.illegal.output-type.jmx" }
          ]
        },
        "4": { name: "keyword.operator.assignment.jmx" }
      }
    },
    { match: "\\b(where)\\b", name: "keyword.other.where.jmx" },
    include("formula")
  ]),
  "state-block": braceBlock("state", "state", [
    include("comment"),
    include("text"),
    declares("variable.other.state.jmx"),
    { match: "\\b(continuous|discrete)\\b", name: "storage.modifier.state.jmx" },
    include("logic-level"),
    include("formula")
  ]),
  "equations-block": braceBlock("equations", "equations", [
    include("comment"),
    {
      comment: "let I_through : current = … — `local` is the same word; the type, when written, stands apart (entity.name.type).",
      match: `${LINE}(let|local)[ \\t]+(${NAME})(?:[ \\t]*(:)[ \\t]*(${NAME}))?[ \\t]*(=)(?!=)`,
      captures: {
        "1": { name: "storage.type.declaration.jmx" },
        "2": { name: "variable.other.declaration.jmx" },
        "3": { name: "punctuation.separator.type.jmx" },
        "4": { name: "entity.name.type.annotation.jmx" },
        "5": { name: "keyword.operator.assignment.jmx" }
      }
    },
    {
      comment: "d(x)/dt = …",
      match: `${LINE}(d)[ \\t]*(\\()[ \\t]*(${NAME})[ \\t]*(\\))[ \\t]*(/)[ \\t]*(dt)[ \\t]*(=)(?!=)`,
      captures: {
        "1": { name: "support.function.derivative.jmx" },
        "2": { name: "punctuation.section.parens.begin.jmx" },
        "3": { name: "variable.other.state.jmx" },
        "4": { name: "punctuation.section.parens.end.jmx" },
        "5": { name: "support.function.derivative.jmx" },
        "6": { name: "support.function.derivative.jmx" },
        "7": { name: "keyword.operator.assignment.jmx" }
      }
    },
    {
      comment: "current a -> b = … and voltage a -> b = …: a branch from one pin to another.",
      match: `${LINE}(current|voltage)[ \\t]+(${NAME})[ \\t]*(->)[ \\t]*(${NAME})[ \\t]*(=)(?!=)`,
      captures: {
        "1": { name: "keyword.other.branch.jmx" },
        "2": { name: "constant.other.net.jmx" },
        "3": { name: "keyword.operator.branch.jmx" },
        "4": { name: "constant.other.net.jmx" },
        "5": { name: "keyword.operator.assignment.jmx" }
      }
    },
    {
      comment: "x_next = … and x_start = …",
      match: `${LINE}([A-Za-z_][A-Za-z0-9_]*?)(_next|_start)[ \\t]*(=)(?!=)`,
      captures: {
        "1": { name: "variable.other.state.jmx" },
        "2": { name: "keyword.other.equation-form.jmx" },
        "3": { name: "keyword.operator.assignment.jmx" }
      }
    },
    {
      comment: "0 = …: an implicit equation.",
      match: `${LINE}(0)[ \\t]*(=)(?!=)`,
      captures: { "1": { name: "constant.numeric.jmx" }, "2": { name: "keyword.operator.assignment.jmx" } }
    },
    include("circuit-function"),
    include("formula")
  ]),
  "display-block": braceBlock("display", "display", [
    include("comment"),
    include("text"),
    declares("variable.other.figure.jmx"),
    include("figure-unit"),
    include("circuit-function"),
    include("formula")
  ]),
  "measure-block": braceBlock("measure", "measure", [
    include("comment"),
    include("text"),
    declares("variable.other.figure.jmx"),
    include("figure-unit"),
    include("circuit-function"),
    { match: "\\b(signal)\\b", name: "storage.modifier.figure.jmx" },
    include("formula")
  ]),
  "figure-unit": {
    comment: "display/measure: `unit %`, `unit V`, `unit A/V^2` after the expression.",
    match: "\\b(unit)[ \\t]+([^ \\t\"#;}]+)",
    captures: { "1": { name: "keyword.other.unit-marker.jmx" }, "2": { name: "keyword.other.unit.jmx" } }
  },
  "leads-block": braceBlock("leads", "leads", [
    include("comment"),
    {
      comment: "plus red stripes black: a pin, its lead's colour, and its stripes'.",
      match: `${LINE}(${NAME})[ \\t]+(${NAME})(?:[ \\t]+(stripes)[ \\t]+(${NAME}))?`,
      captures: {
        "1": { name: "variable.other.pin.jmx" },
        "2": { name: "constant.other.colour.jmx" },
        "3": { name: "keyword.other.stripes.jmx" },
        "4": { name: "constant.other.colour.jmx" }
      }
    }
  ]),
  "image-block": braceBlock("image", "image", [
    include("comment"),
    { match: "\\b(over|start|stop|hit|when|crosses|where|value|in)\\b", name: "keyword.other.image.jmx" },
    include("formula")
  ]),
  "pins-block": braceBlock("pins", "pins", [
    include("comment"),
    include("text"),
    { match: `${LINE}(${NAME})`, captures: { "1": { name: "variable.other.pin.jmx" } } },
    { match: "\\b(analog|digital|power|ground)\\b", name: "storage.type.nature.jmx" },
    { match: "\\b(input|output)\\b", name: "storage.modifier.direction.jmx" },
    { match: "\\b[0-9]+\\b", name: "constant.numeric.pin-number.jmx" },
    { match: NAME, name: "entity.other.attribute-name.pin.jmx" }
  ]),
  "nodes-block": braceBlock("nodes", "nodes", [
    include("comment"),
    include("text"),
    {
      match: `${LINE}(?:(digital)[ \\t]+)?(${NAME})`,
      captures: { "1": { name: "storage.type.nature.jmx" }, "2": { name: "variable.other.node.jmx" } }
    }
  ]),
  "formulas-block": braceBlock("formulas", "formulas", [
    include("comment"),
    {
      comment: "f(u, v) = …: a jMF definition.",
      match: `${LINE}(${NAME})[ \\t]*(\\()([^)]*)(\\))[ \\t]*(=)(?!=)`,
      captures: {
        "1": { name: "entity.name.function.definition.jmx" },
        "2": { name: "punctuation.section.parens.begin.jmx" },
        "3": { patterns: [{ match: NAME, name: "variable.parameter.jmx" }, { match: ",", name: "punctuation.separator.comma.jmx" }] },
        "4": { name: "punctuation.section.parens.end.jmx" },
        "5": { name: "keyword.operator.assignment.jmx" }
      }
    },
    declares("variable.other.definition.jmx"),
    include("formula")
  ]),
  "formulas-line": lineBlock("formulas", "formulas", [
    { match: "\"[^\"]*\"|'[^']*'", name: "entity.name.type.reference.jmx" },
    { match: ",", name: "punctuation.separator.comma.jmx" }
  ]),

  // ─── jMS bodies: nothing of jMX is reserved in them ──────────────────────────────────────────────

  "behaviour-body": braceBlock("behaviour", "behaviour", [include("script-braces"), include("statement")]),
  "run-body": braceBlock("run", "run", [include("script-braces"), include("statement")]),
  "script-braces": {
    comment: "A brace inside a jMS body is jMS's, so it must not close the block.",
    begin: "\\{",
    end: "\\}",
    patterns: [include("script-braces"), include("statement")]
  },

  // ─── view ────────────────────────────────────────────────────────────────────────────────────────

  "view-block": braceBlock("view", "view", [
    include("comment"),
    {
      name: "meta.block.preset.jmx",
      comment: "preset \"Title\" [\"what it shows\"] { name = value, … }",
      begin: `${LINE}\\b(preset)[ \\t]*${STRING}(?:[ \\t]*(\"[^\"]*\"|'[^']*'))?[ \\t]*(\\{)`,
      beginCaptures: {
        "1": { name: "keyword.other.view.jmx" },
        "2": { name: "entity.name.section.preset.jmx" },
        "3": { name: "string.quoted.double.jmx" },
        "4": blockBegin
      },
      end: "\\}",
      endCaptures: blockEnd,
      patterns: [include("comment"), include("assignments")]
    },
    {
      name: "meta.line.camera.jmx",
      begin: `${LINE}\\b(camera)\\b`,
      beginCaptures: { "1": { name: "keyword.other.view.jmx" } },
      end: LINE_END,
      patterns: [{ match: NAME, name: "entity.other.attribute-name.camera.jmx" }, include("formula")]
    },
    {
      match: `${LINE}\\b(explain)[ \\t]+(${NAME})`,
      captures: { "1": { name: "keyword.other.view.jmx" }, "2": { name: "variable.other.explained.jmx" } }
    },
    {
      name: "meta.line.without.jmx",
      begin: `${LINE}\\b(without)\\b`,
      beginCaptures: { "1": { name: "keyword.other.view.jmx" } },
      end: LINE_END,
      patterns: [
        { match: `\\b${words(CAPABILITIES)}\\b`, name: "support.constant.capability.jmx" },
        { match: ",", name: "punctuation.separator.comma.jmx" },
        { match: NAME, name: "invalid.illegal.capability.jmx" }
      ]
    },
    {
      comment: "Anything else is a representation: its name, primary, then its settings as written.",
      match: `${LINE}(${NAME})(?:[ \\t]+(primary)\\b)?`,
      captures: { "1": { name: "entity.name.function.representation.jmx" }, "2": { name: "storage.modifier.primary.jmx" } }
    },
    { match: "\\b(along|at|over|in|count|resolution|rotate|planes|animate)\\b", name: "keyword.other.setting.jmx" },
    include("text"),
    include("formula")
  ]),

  // ─── checks ──────────────────────────────────────────────────────────────────────────────────────

  "expect-block": {
    name: "meta.block.expect.jmx",
    begin: `${LINE}\\b(expect)[ \\t]*${STRING}(?:[ \\t]*\\b(from)[ \\t]*${STRING})?[ \\t]*(\\{)`,
    beginCaptures: {
      "1": keyword("expect"),
      "2": { name: "entity.name.section.expectation.jmx" },
      "3": { name: "keyword.other.from.jmx" },
      "4": { name: "string.quoted.reference.jmx" },
      "5": blockBegin
    },
    end: "\\}",
    endCaptures: blockEnd,
    patterns: [
      include("comment"),
      {
        name: "meta.block.bench.jmx",
        begin: `${LINE}\\b(bench)\\b(?:[ \\t]+(for)\\b([^{#;]*?))?[ \\t]*(\\{)`,
        beginCaptures: {
          "1": { name: "keyword.control.block.bench.jmx" },
          "2": { name: "keyword.other.setting.jmx" },
          "3": { patterns: [include("formula")] },
          "4": blockBegin
        },
        end: "\\}",
        endCaptures: blockEnd,
        patterns: [include("part-line")]
      },
      {
        name: "meta.line.measure.jmx",
        begin: `${LINE}\\b(measure)\\b`,
        beginCaptures: { "1": { name: "keyword.control.measure.jmx" } },
        end: LINE_END,
        patterns: [
          { match: `\\b${words(MEASUREMENTS)}\\b`, name: "support.function.measure.jmx" },
          { match: "\\b(above|below|after)\\b", name: "keyword.other.measure.jmx" },
          include("circuit-function"),
          include("formula")
        ]
      },
      {
        name: "meta.line.criterion.jmx",
        begin: `${LINE}\\b(near|within|is)\\b`,
        beginCaptures: { "1": { name: "keyword.operator.criterion.jmx" } },
        end: LINE_END,
        patterns: [include("logic-level"), include("formula")]
      }
    ]
  },

  // ─── components and circuits: one netlist notation ───────────────────────────────────────────────

  "symbol-uses": {
    comment: "symbol uses diode-symbol[, other] [{ a anode; b cathode }]: the symbols that draw the component, the first the default, and the port each pin lands on.",
    patterns: [
      {
        name: "meta.block.symbol-uses.jmx",
        begin: `${LINE}\\b(symbol)[ \\t]+(uses)[ \\t]+(${SLUG}(?:[ \\t]*,[ \\t]*${SLUG})*)[ \\t]*(\\{)`,
        beginCaptures: {
          "1": keyword("symbol"),
          "2": { name: "keyword.other.uses.jmx" },
          "3": { patterns: [{ match: ",", name: "punctuation.separator.comma.jmx" }, { match: SLUG, name: "entity.name.type.reference.jmx" }] },
          "4": blockBegin
        },
        end: "\\}",
        endCaptures: blockEnd,
        patterns: [
          include("comment"),
          {
            match: `${LINE}(${NAME})[ \\t]+(${NAME})`,
            captures: { "1": { name: "entity.name.tag.port.jmx" }, "2": { name: "variable.other.pin.jmx" } }
          }
        ]
      },
      {
        match: `${LINE}\\b(symbol)[ \\t]+(uses)[ \\t]+(${SLUG}(?:[ \\t]*,[ \\t]*${SLUG})*)`,
        captures: {
          "1": keyword("symbol"),
          "2": { name: "keyword.other.uses.jmx" },
          "3": { patterns: [{ match: ",", name: "punctuation.separator.comma.jmx" }, { match: SLUG, name: "entity.name.type.reference.jmx" }] }
        }
      }
    ]
  },
  "symbol-shape": {
    comment: "symbol box { left d, clk edge; right q }: a shape or a glyph, its pins placed by side or by the glyph's ports.",
    name: "meta.block.symbol.jmx",
    begin: `${LINE}\\b(symbol)[ \\t]+(${SLUG})[ \\t]*(\\{)`,
    beginCaptures: {
      "1": keyword("symbol"),
      "2": {
        patterns: [
          { match: "\\b(box|triangle)\\b", name: "support.type.shape.jmx" },
          { match: SLUG, name: "entity.name.type.glyph.jmx" }
        ]
      },
      "3": blockBegin
    },
    end: "\\}",
    endCaptures: blockEnd,
    patterns: [
      include("comment"),
      { match: `${LINE}\\b(left|right|top|bottom|power)\\b`, captures: { "1": { name: "keyword.other.side.jmx" } } },
      { match: `${LINE}(${NAME})`, captures: { "1": { name: "keyword.other.port.jmx" } } },
      { match: "\\b(edge|bubble)\\b", name: "storage.modifier.pin.jmx" },
      { match: ",", name: "punctuation.separator.comma.jmx" },
      include("text"),
      { match: NAME, name: "variable.other.pin.jmx" }
    ]
  },
  "symbol-drawing": {
    name: "meta.block.symbol.jmx",
    begin: `${LINE}\\b(symbol)[ \\t]*(\\{)`,
    beginCaptures: { "1": keyword("symbol"), "2": blockBegin },
    end: "\\}",
    endCaptures: blockEnd,
    patterns: [include("comment"), include("drawing")]
  },
  "drawing": {
    comment: "size W x H, path \"…\", pin NAME at x, y [\"label\"], label NAME at x, y.",
    patterns: [
      {
        match: `${LINE}\\b(size)\\b`,
        captures: { "1": { name: "keyword.other.drawing.jmx" } }
      },
      {
        match: `${LINE}\\b(path)[ \\t]*("[^"]*"|'[^']*')`,
        captures: { "1": { name: "keyword.other.drawing.jmx" }, "2": { name: "string.quoted.path.jmx" } }
      },
      {
        match: `${LINE}\\b(pin|label)[ \\t]+(${NAME})[ \\t]+(at)\\b`,
        captures: {
          "1": { name: "keyword.other.drawing.jmx" },
          "2": { name: "variable.other.pin.jmx" },
          "3": { name: "keyword.other.at.jmx" }
        }
      },
      { match: "\\b(x)\\b", name: "keyword.operator.by.jmx" },
      include("text"),
      include("quantity"),
      { match: ",", name: "punctuation.separator.comma.jmx" },
      { match: "-", name: "keyword.operator.jmx" }
    ]
  },
  "parts-block": braceBlock("parts", "parts", [include("part-line")]),
  "part-line": {
    comment: "ID reference [unit U] (connections) [settings] [place x, y [rotate deg] [mirror]]: parts, internals and benches alike.",
    patterns: [
      include("comment"),
      {
        match: `${LINE}(${NAME})[ \\t]+(${SLUG})(?:[ \\t]+(unit)[ \\t]+(${NAME}))?(?:[ \\t]*(\\()([^)]*)(\\)))?`,
        captures: {
          "1": { name: "entity.name.function.part.jmx" },
          "2": {
            patterns: [
              { match: "\\bself\\b", name: "variable.language.self.jmx" },
              { match: SLUG, name: "entity.name.tag.reference.jmx" }
            ]
          },
          "3": { name: "keyword.other.unit.jmx" },
          "4": { name: "entity.name.tag.unit-name.jmx" },
          "5": { name: "punctuation.section.parens.begin.jmx" },
          "6": {
            patterns: [
              {
                match: "([A-Za-z_][A-Za-z0-9_.]*)[ \\t]*(=)",
                captures: { "1": { name: "entity.other.attribute-name.pin.jmx" }, "2": { name: "keyword.operator.assignment.jmx" } }
              },
              { match: "\\b[0-9]+\\b", name: "constant.numeric.net.jmx" },
              { match: ",", name: "punctuation.separator.comma.jmx" },
              { match: NAME, name: "constant.other.net.jmx" }
            ]
          },
          "7": { name: "punctuation.section.parens.end.jmx" }
        }
      },
      { match: "\\b(place|rotate|mirror)\\b", name: "keyword.other.placement.jmx" },
      include("assignments")
    ]
  },
  // CircuitReader.layout: `ID at x, y [rotate deg] [mirror]`, `ID ground [net] at …`, `ID junction at …` and
  // `ID wire PART.pin to PART.pin [via x y, x y …]` — a circuit's drawing, one line per mark or wire.
  "layout-block": braceBlock("layout", "layout", [
    include("comment"),
    {
      match: `${LINE}(${NAME}(?:-[A-Za-z0-9_]+)*)[ \\t]+(wire)\\b`,
      captures: { "1": { name: "entity.name.function.layout.jmx" }, "2": { name: "keyword.other.layout.jmx" } }
    },
    {
      match: `${LINE}(${NAME}(?:-[A-Za-z0-9_]+)*)[ \\t]+(?:(ground|common|junction)(?:[ \\t]+(${NAME}))?[ \\t]+)?(at)\\b`,
      captures: {
        "1": { name: "entity.name.function.layout.jmx" },
        "2": { name: "storage.type.mark.jmx" },
        "3": { name: "constant.other.net.jmx" },
        "4": { name: "keyword.other.layout.jmx" }
      }
    },
    {
      comment: "PART.pin at a wire's ends: R1.positive, V1.+, Ja.j.",
      match: "\\b([A-Za-z_][A-Za-z0-9_-]*)(\\.)([A-Za-z_][A-Za-z0-9_]*|[+\\u2212-])",
      captures: {
        "1": { name: "entity.name.function.part.jmx" },
        "2": { name: "punctuation.accessor.jmx" },
        "3": { name: "entity.other.attribute-name.pin.jmx" }
      }
    },
    { match: "\\b(to|via|rotate|mirror)\\b", name: "keyword.other.layout.jmx" },
    include("quantity"),
    { match: ",", name: "punctuation.separator.comma.jmx" },
    { match: "-", name: "keyword.operator.jmx" }
  ]),
  "nets-block": braceBlock("nets", "nets", [
    include("comment"),
    { match: `${LINE}(${NAME})`, captures: { "1": { name: "variable.other.net.jmx" } } },
    { match: "\\b(ground)\\b", name: "storage.type.nature.jmx" },
    { match: "\\b(route)\\b", name: "keyword.other.route.jmx" },
    include("quantity"),
    { match: ",", name: "punctuation.separator.comma.jmx" },
    { match: "-", name: "keyword.operator.jmx" }
  ]),
  "ports-block": braceBlock("ports", "ports", [
    include("comment"),
    {
      match: `${LINE}\\b(input|output|power|ground|analog)\\b(?:[ \\t]+(${NAME}))?`,
      captures: { "1": { name: "storage.modifier.direction.jmx" }, "2": { name: "variable.other.net.jmx" } }
    }
  ]),
  "probes-block": braceBlock("probes", "probes", [
    include("comment"),
    {
      comment: "CH1 out [against reference] 2 V/div, M1 voltage p, M2 current PS1.negative rms, M3 power RL, L1 level clock: what is measured, by its own name.",
      match: `${LINE}(${NAME})[ \\t]+(?:(voltage|current|power|level)[ \\t]+)?(${NAME}(?:\\.${NAME})?)(?:[ \\t]+(against)[ \\t]+(${NAME}))?`,
      captures: {
        "1": { name: "entity.name.function.probe.jmx" },
        "2": { name: "keyword.other.measure.jmx" },
        "3": { name: "constant.other.net.jmx" },
        "4": { name: "keyword.other.measure.jmx" },
        "5": { name: "constant.other.net.jmx" }
      }
    },
    { match: "(/)[ \\t]*(div)\\b", captures: { "1": { name: "keyword.operator.jmx" }, "2": { name: "keyword.other.unit.division.jmx" } } },
    include("quantity")
  ]),
  "analysis-line": lineBlock("analysis", "analysis", [
    { match: "\\b(transient|ac|operating|dc_sweep)\\b", name: "support.constant.analysis.jmx" },
    { match: "\\b(for|from|to|points|source|step)\\b", name: "keyword.other.setting.jmx" },
    include("formula")
  ]),
  "sheet-line": lineBlock("sheet", "sheet", [
    { match: "\\b(x)\\b", name: "keyword.operator.by.jmx" },
    { match: "\\b(grid)\\b", name: "keyword.other.setting.jmx" },
    include("quantity")
  ]),

  // ─── a graph: lines on panes, moving on one clock ─────────────────────────────────────────────────

  "clock-line": lineBlock("clock", "clock", [
    {
      comment: "clock t [from a to b] [speed s] [loop]: the parameter it moves comes first.",
      match: `(?<=\\bclock)[ \\t]+(${NAME})`,
      captures: { "1": { name: "variable.parameter.clock.jmx" } }
    },
    { match: "\\b(from|to|speed)\\b", name: "keyword.other.setting.jmx" },
    { match: "\\b(loop)\\b", name: "storage.modifier.loop.jmx" },
    include("quantity"),
    include("formula")
  ]),
  "panes-block": {
    comment: "panes [rows|columns|grid] { name plane|space|circuit|values … }",
    name: "meta.block.panes.jmx",
    begin: `${LINE}\\b(panes)(?:[ \\t]+(${words(PANE_LAYOUTS)}))?[ \\t]*(\\{)`,
    beginCaptures: { "1": keyword("panes"), "2": { name: "support.constant.layout.jmx" }, "3": blockBegin },
    end: "\\}",
    endCaptures: blockEnd,
    patterns: [
      include("comment"),
      {
        match: `${LINE}(${NAME})[ \\t]+(${words(PANE_KINDS)})\\b`,
        captures: { "1": { name: "entity.name.section.pane.jmx" }, "2": { name: "support.type.pane.jmx" } }
      },
      {
        comment: "a circuit pane plays a circuit: uses <slug> [over a .. b].",
        match: `\\b(uses)[ \\t]+(${SLUG})`,
        captures: { "1": { name: "keyword.other.uses.jmx" }, "2": { name: "entity.name.type.reference.jmx" } }
      },
      {
        match: "\\b(camera)[ \\t]+(front|side|top|angled)\\b",
        captures: { "1": { name: "keyword.other.setting.jmx" }, "2": { name: "support.constant.camera.jmx" } }
      },
      { match: "\\b(x|y|z|over)\\b(?=[ \\t]+[^ \\t])", name: "keyword.other.axis.jmx" },
      include("quantity"),
      include("formula")
    ]
  },
  "lines-block": braceBlock("lines", "lines", [
    include("comment"),
    {
      comment: "Every line opens with its name.",
      match: `${LINE}(${NAME})(?=[ \\t])`,
      captures: { "1": { name: "entity.name.function.line.jmx" } }
    },
    {
      comment: "What follows the expression, in this order: until u, at (x, y), in pane, color n, hidden.",
      match: `\\b(in)[ \\t]+(${NAME})(?=[ \\t]*(?:color\\b|hidden\\b|[;#}]|$))`,
      captures: { "1": { name: "keyword.other.in.jmx" }, "2": { name: "entity.name.section.pane.jmx" } }
    },
    { match: "\\b(until|at|from)\\b", name: "keyword.other.setting.jmx" },
    {
      match: "\\b(color)[ \\t]+([1-6])\\b",
      captures: { "1": { name: "keyword.other.setting.jmx" }, "2": { name: "constant.numeric.color.jmx" } }
    },
    { match: "\\b(hidden)\\b", name: "storage.modifier.hidden.jmx" },
    {
      comment: "A label: a text whose {…} parts are read live.",
      name: "string.quoted.label.jmx",
      begin: "\"",
      end: "\"",
      patterns: [{ name: "meta.embedded.value.jmx", begin: "\\{", end: "\\}", patterns: [include("formula")] }]
    },
    include("formula")
  ]),

  // ─── kinds with a structure of their own ─────────────────────────────────────────────────────────

  "maps-block": braceBlock("maps", "maps", [
    include("comment"),
    {
      comment: "weight : (x expression, y expression)",
      match: `${LINE}([^:#;{}]+?)[ \\t]*(:)`,
      captures: { "1": { name: "meta.weight.jmx", patterns: [include("formula")] }, "2": { name: "punctuation.separator.map.jmx" } }
    },
    include("formula")
  ]),
  "axiom-line": lineBlock("axiom", "axiom", [include("string")]),
  "rules-block": braceBlock("rules", "rules", [
    include("comment"),
    {
      match: `${LINE}([^\\s#;{}]+?)[ \\t]*(->)`,
      captures: { "1": { name: "entity.name.tag.symbol.jmx" }, "2": { name: "keyword.operator.production.jmx" } }
    },
    include("string")
  ]),
  "turtle-block": braceBlock("turtle", "turtle", [include("comment"), include("assignments")]),
  "steps-block": braceBlock("steps", "steps", [
    include("comment"),
    declares("variable.other.construction.jmx"),
    include("formula")
  ]),
  "vertices-line": lineBlock("vertices", "vertices", [include("formula")]),
  "edges-line": lineBlock("edges", "edges", [
    { match: "\\b(where)\\b", name: "keyword.other.where.jmx" },
    include("formula")
  ]),
  "points-block": braceBlock("points", "points", [include("comment"), include("formula")]),
  "items-line": lineBlock("items", "items", [
    { match: ",", name: "punctuation.separator.comma.jmx" },
    { match: SLUG, name: "entity.name.type.reference.jmx" }
  ]),
  "instrument-line": {
    comment: "An instrument's objects, representations and the object it opens: objects geometric_construction, point_set.",
    name: "meta.line.instrument.jmx",
    begin: `${LINE}\\b(objects|representations|opens)\\b`,
    beginCaptures: { "1": { name: "keyword.other.instrument.jmx" } },
    end: LINE_END,
    patterns: [
      { match: ",", name: "punctuation.separator.comma.jmx" },
      { match: SLUG, name: "entity.name.type.reference.jmx" }
    ]
  },
  "collection-section": {
    comment: "section \"Ordinary\" { items … }: a collection's section and its items.",
    name: "meta.block.section.jmx",
    begin: `${LINE}\\b(section)[ \\t]*${STRING}[ \\t]*(\\{)`,
    beginCaptures: { "1": keyword("section"), "2": { name: "entity.name.section.jmx" }, "3": blockBegin },
    end: "\\}",
    endCaptures: blockEnd,
    patterns: [include("comment"), include("items-line"), include("text")]
  },
  "course-lesson": {
    comment: "lesson \"Title\" [as slug] { summary …; minutes 10; object …; step \"…\" { … } }",
    name: "meta.block.lesson.jmx",
    begin: `${LINE}\\b(lesson)[ \\t]*${STRING}((?:[ \\t]+as[ \\t]+${SLUG})?)[ \\t]*(\\{)`,
    beginCaptures: {
      "1": keyword("lesson"),
      "2": { name: "entity.name.section.lesson.jmx" },
      "3": { patterns: [include("document-identity")] },
      "4": blockBegin
    },
    end: "\\}",
    endCaptures: blockEnd,
    patterns: [
      include("comment"),
      include("course-step"),
      { match: `${LINE}(${NAME})`, captures: { "1": { name: "entity.other.attribute-name.field.jmx" } } },
      include("text"),
      include("formula")
    ]
  },
  "course-step": {
    name: "meta.block.step.jmx",
    begin: `${LINE}\\b(step)[ \\t]*${STRING}[ \\t]*(\\{)`,
    beginCaptures: { "1": keyword("step"), "2": { name: "entity.name.section.step.jmx" }, "3": blockBegin },
    end: "\\}",
    endCaptures: blockEnd,
    patterns: [
      include("comment"),
      { match: `${LINE}(${NAME})`, captures: { "1": { name: "entity.other.attribute-name.field.jmx" } } },
      include("text"),
      include("formula")
    ]
  },

  // ─── values and expressions ──────────────────────────────────────────────────────────────────────

  "assignments": {
    comment: "name = value, separated by commas or not: a part's settings, a preset, a turtle.",
    patterns: [
      include("comment"),
      {
        match: `\\b(${NAME})[ \\t]*(=)(?!=)`,
        captures: { "1": { name: "entity.other.attribute-name.setting.jmx" }, "2": { name: "keyword.operator.assignment.jmx" } }
      },
      include("formula")
    ]
  },
  "circuit-function": {
    comment: "d(q)/dt in an equation; a pin's members — anode.current, anode.voltage — anywhere a formula is; current and level in a measurement. A pin's bare name is its voltage, so it needs nothing.",
    patterns: [
      {
        match: "(?<=[A-Za-z0-9_])(\\.)(current|voltage)\\b",
        captures: { "1": { name: "punctuation.accessor.jmx" }, "2": { name: "support.variable.member.pin.jmx" } }
      },
      { match: "\\b(d)(?=[ \\t]*\\()", name: "support.function.derivative.jmx" },
      { match: "(?<=\\))[ \\t]*(/)[ \\t]*(dt)\\b", captures: { "1": { name: "keyword.operator.jmx" }, "2": { name: "support.function.derivative.jmx" } } },
      { match: "\\b(current|power|level)\\b", name: "keyword.other.measure.jmx" }
    ]
  },
  "logic-level": { match: "\\b(X|Z)\\b", name: "constant.language.logic.jmx" },
  "quantity": {
    comment: "Quantity.java: a number and the unit written after it — 2.52 nA, 0.3 V/us, 10 kOhm, A/V^2, 1/s, 5%, 90 deg. A unit is one token with its number; '2 C' is two coulombs, not 2·C.",
    match: `(${NUMBER})(?:[ \\t]*(${UNIT}))?`,
    captures: { "1": { name: "constant.numeric.jmx" }, "2": { name: "keyword.other.unit.jmx" } }
  },
  "imaginary": {
    comment: "-0.8 + 0.156 i: the imaginary unit after a number.",
    match: `(${NUMBER})[ \\t]?(i)\\b`,
    captures: { "1": { name: "constant.numeric.jmx" }, "2": { name: "constant.language.imaginary.jmx" } }
  },
  "range": { match: "\\.\\.", name: "keyword.operator.range.jmx" },
  "tolerance": { match: "\\+-|±", name: "keyword.operator.tolerance.jmx" },
  "formula": {
    comment: "A jMF expression: numbers with units, ranges, tolerances, calls, constants and operators.",
    patterns: [
      include("comment"),
      include("text"),
      include("imaginary"),
      include("quantity"),
      include("range"),
      include("tolerance"),
      { match: "\\b(pi|tau|phi|inf|e|i)\\b", name: "constant.language.jmx" },
      include("function-call"),
      { match: "->|==|!=|<=|>=|[-+*/^<>=!]", name: "keyword.operator.jmx" },
      { match: "[(),\\[\\]:]", name: "punctuation.jmx" },
      include("identifier")
    ]
  }
});

const grammar = {
  $schema: jms.$schema,
  name: "jMouse eXperience",
  scopeName: "source.jmx",
  fileTypes: ["jmx"],
  patterns: [include("comment"), include("symbol-document"), include("document")],
  repository
};

fs.mkdirSync(path.join(target, "syntaxes"), { recursive: true });
fs.writeFileSync(path.join(target, "syntaxes", "jmx.tmLanguage.json"), JSON.stringify(grammar, null, 2) + "\n");

const manifest = JSON.parse(fs.readFileSync(path.join(source, "package.json"), "utf8"));
manifest.name = "jmx-textmate";
manifest.displayName = "jMouse eXperience (.jmx)";
manifest.description = "Syntax highlighting for jMX — the jMouse document language: objects, components, circuits and the catalogue.";
manifest.contributes.languages = [{ id: "jmx", aliases: ["jMX", "jMouse eXperience", "jmx"], extensions: [".jmx"], configuration: "./language-configuration.json" }];
manifest.contributes.grammars = [{ language: "jmx", scopeName: "source.jmx", path: "./syntaxes/jmx.tmLanguage.json" }];
fs.writeFileSync(path.join(target, "package.json"), JSON.stringify(manifest, null, 2) + "\n");
fs.copyFileSync(path.join(source, "language-configuration.json"), path.join(target, "language-configuration.json"));
console.log("written", Object.keys(repository).length, "rules; jms had:", Object.keys(jms.repository).join(", "));
