# jmouse-json — JSON on the jME engine

> **Agent orientation file.** Written so you do not have to read the module to use it. If something here disagrees with
> the code, the code wins — and fix this file in the same change.
>
> Module: `jmouse-json` (+ `jmouse-el`, `jmouse-core`) — `org.jmouse.json`: the entry points and configuration ·
> `org.jmouse.json.syntax`: reading the text · `org.jmouse.json.node`: the tree · `org.jmouse.json.write`: writing, by type ·
> `org.jmouse.json.bind`: reading into types.
> First consumer: `jmouse-j2js-el` — values crossing between a page and jME. It has no dependency but the engine, so it runs
> the same on the server and in the browser (compiled by `jmouse/j2js`).

## What it is in one paragraph

JSON read and written **on the engine, not beside it**. Text is cut by JSON's own `JsonSplitter` — by the engine's `Splitter`
contract, a scanner rather than the expression language's pattern —, tokenized by the engine's tokenizer and recognizer, and
parsed by parsers of the engine's `Parser` contract. The parsers build
nodes of jME's tree, and numbers are read by jME's own rule (`LiteralParser.numericLiteral`), so JSON's `5` is jME's `5`.
Text is written through jME's `SourceWriter`. Above the syntax are two layers:

- **writing by type.** A `JsonSerializer<T>` is registered for a type; the nearest one in a value's hierarchy writes the
  value.
- **reading into types.** The jMouse mapper (`jmouse-mapper`) does the reading, and a `JsonDeserializer<T>` is one of its strategies.

A registered serializer or deserializer replaces the standard one **wherever its type stands**: at the top, as a member, as
an element, as a map's value. Reading is strict RFC 8259, and a refusal says where JSON stops being JSON.

## The map

```
org.jmouse.json
├── Json                    the default mapper: read · read(type) · parse · write · write(layout) · mapper()
├── JsonMapper              immutable configuration: serializers, deserializers, inclusion, layout · builder() · toBuilder()
│   └── JsonMapper.Builder  serializer(type, s) · deserializer(type, d) · inclusion(i) · layout(l) · build()
├── JsonLayout              COMPACT | INDENTED — over SourceLayout.GLUED | SourceLayout.WORDS
├── JsonInclusion           ALL | NON_NULL — which of an object's members are written
├── JsonTexts               JSON's escapes, read (decode) and written (encode), in one place
├── JsonSyntaxException     not JSON: what was expected, what was found, line and character   (extends SyntaxErrorException)
└── JsonMappingException    JSON, but not of the type asked for — or a value that could not be written
org.jmouse.json.syntax      JsonReader · JsonSplitter · JsonRecognizer · JsonExtension · JsonParsing ·
                            JsonValueParser · JsonObjectParser · JsonArrayParser · JsonTextParser · JsonNumberParser
org.jmouse.json.node        JsonNode (value()) · JsonObjectNode · JsonMemberNode · JsonArrayNode · JsonScalarNode
org.jmouse.json.write       JsonSerializer<T> · JsonOutput (+ Members, Elements) · JsonSerializers · StandardSerializers
org.jmouse.json.bind        JsonDeserializer<T> · JsonDeserialization · JsonDeserializers · JsonBinding · JsonDeserializerContributor
```

## Reading and writing — the default mapper

```java
Object value = Json.read("{\"x\": 2, \"items\": [1, 2.5], \"name\": \"R\\u00e9sistor\"}");
// {x=2, items=[1, 2.5], name=Résistor} — a LinkedHashMap in the order written; 2 an Integer, 2.5 a Double

String compact  = Json.write(value);                        // {"x":2,"items":[1,2.5],"name":"Résistor"}
String indented = Json.write(value, JsonLayout.INDENTED);
// {
//     "x": 2,
//     "items": [
//         1,
//         2.5
//     ],
//     "name": "Résistor"
// }
```

What a text is read as:

| JSON | Java |
|---|---|
| object | `LinkedHashMap<String, Object>`, members in the order written; a name written twice keeps its last value |
| array | `ArrayList<Object>` |
| number | by jME's rule: `Integer`, `Long`, `BigInteger` for a whole number by size; `Double`, or `BigDecimal` where a double would not hold it exactly |
| string | `String`, escapes decoded |
| `true` / `false` / `null` | `Boolean` / `null` |

What a value is written as, by the standard serializers:

| Java | JSON |
|---|---|
| `null`, `Boolean` | `null`, `true` / `false` |
| `Number` | its shortest exact digits (`5.0` → `5`); `NaN` and the infinities → `null`, as `JSON.stringify` writes them |
| `CharSequence`, `Character`, `Enum` | a string (an enum by its `name()`) |
| `Map` | an object; each key by its text |
| `Iterable`, an array | an array |
| `Optional` | its value, or `null` |
| a record, a bean | an object of its readable properties, as the core's `Describer` gives them |
| any other JDK type (`LocalDate`, `URI`, `UUID`, …) | the string of its `toString()` |

## Reading into types

```java
public record Point(double x, double y) {}
public record Part(String designator, Point at, List<String> tags, Kind kind) {}

Part part = Json.read("{\"designator\": \"C3\", \"at\": {\"x\": 1, \"y\": 2.5}, \"tags\": [\"tht\"], \"kind\": \"CAPACITOR\"}",
                      Part.class);

List<Point> points = Json.read("[{\"x\": 0, \"y\": 0}, {\"x\": 3, \"y\": 4}]", TypedValue.ofList(Point.class));
Map<String, Point> named = Json.read("{\"origin\": {\"x\": 0, \"y\": 0}}", TypedValue.ofMap(String.class, Point.class));

Supplier supplier = Json.read("{\"name\": \"Digikey\", \"stock\": \"40\"}", Supplier.class);   // a bean; "40" converted to int
```

The reading is the jMouse mapper's, so whatever it maps is read here: beans by their setters, records by their canonical
constructor, collections, maps, arrays, an `Optional` (empty for a value absent or null — never `null` itself), enums, and scalars converted by the core's conversion. A type that JSON cannot fill —
`{"x": "far"}` read as a `Point` — is refused with a `JsonMappingException` that names the type. Read into `Object.class`, a
value is the plain value of the table above.

## A serializer of your own

A `JsonSerializer<T>` writes to the `JsonOutput` it is given: a scalar, or an object of members, or an array of elements. A
part it does not write itself it hands to `output.value(part)`, and that part is written by its own serializer in turn. The
output owns the commas, colons, brackets and lines; a serializer never writes a mark.

```java
/** A resistance written the way a designer writes it: "4.7k". */
public record Resistance(double ohms) {}

JsonMapper mapper = JsonMapper.builder()
        .serializer(Resistance.class, (resistance, output) -> output.text(resistance.ohms() / 1000 + "k"))
        .build();

mapper.write(new Divider(new Resistance(4700), new Resistance(1000)));   // {"top":"4.7k","bottom":"1.0k"}
mapper.write(List.of(new Resistance(2200)));                             // ["2.2k"]
```

An object or an array of your own shape:

```java
JsonMapper mapper = JsonMapper.builder()
        .serializer(Part.class, (part, output) -> output.object(members -> members
                .member("ref", part.designator())                            // through its type's serializer
                .member("at", position -> position.array(elements -> elements  // a value written by hand
                        .element(part.at().x())
                        .element(part.at().y())))))
        .build();
// {"ref":"R1","at":[1.5,2]}
```

`JsonOutput` writes:

| Method | Writes |
|---|---|
| `value(Object)` | any value, through its type's serializer (`null` as `null`) |
| `nullValue()`, `truth(boolean)`, `text(String)` | a scalar |
| `number(long)`, `number(double)`, `number(BigDecimal)`, `number(BigInteger)` | a number (`NaN` and the infinities as `null`) |
| `object(Consumer<Members>)` | `Members.member(name, Object)`: a member written by its value's serializer, left out when it holds null and the inclusion is `NON_NULL` · `Members.member(name, Consumer<JsonOutput>)`: a member written by hand |
| `array(Consumer<Elements>)` | `Elements.element(Object)` · `Elements.element(Consumer<JsonOutput>)` |
| `inclusion()` | the mapper's inclusion, for a serializer that leaves members out itself |

### Which serializer writes a value

`JsonSerializers.serializerOf(type)` looks in this order and keeps the answer for each class:

1. the serializer registered for the class itself;
2. the one for the nearest class above it;
3. the one for the nearest interface, breadth first: a class's own interfaces before those of the classes above it;
4. for an array, the array serializer;
5. the fallback, which writes a record or a bean by its properties and a JDK type as its text.

So one registration covers a whole family:

```java
JsonMapper mapper = Json.mapper().toBuilder()
        .serializer(Number.class, (number, output) -> output.text("#" + number))   // every number with no nearer serializer
        .build();
mapper.write(List.of(1, 2.5));                                                 // ["#1","#2.5"]
```

A serializer registered for `Object.class` replaces the fallback for everything no nearer serializer reaches: every record
and bean, and arrays too, since an array's class has `Object` above it.

## A deserializer of your own

A `JsonDeserializer<T>` reads its type from the value JSON gives: a map, a list or a scalar. A part it reads with
`deserialization.read(part, Type.class)` (or a `TypedValue`) is read by that type's deserializer or bound in turn. It is bound
into the mapper as a strategy for **exactly** its type, asked before every standard strategy, so it applies wherever
the mapping meets the type: at the top, as a record component, as a bean property, as an element.

```java
JsonMapper mapper = JsonMapper.builder()
        .deserializer(Resistance.class, (json, deserialization) -> {
            String written = (String) json;                                        // "4.7k"
            return new Resistance(Double.parseDouble(written.substring(0, written.length() - 1)) * 1000);
        })
        .build();

mapper.read("\"4.7k\"", Resistance.class);                                     // Resistance[ohms=4700.0]
mapper.read("[\"1k\", \"2.2k\"]", TypedValue.ofList(Resistance.class));        // [Resistance[1000.0], Resistance[2200.0]]
mapper.read("{\"top\": \"4.7k\", \"bottom\": \"1k\"}", Divider.class);          // inside a record: the mapping reaches it
```

A deserializer that reads a shape of its own, choosing the type from a discriminator:

```java
mapper = JsonMapper.builder()
        .deserializer(Shape.class, (json, deserialization) -> {
            Map<?, ?> object = (Map<?, ?>) json;
            return switch ((String) object.get("kind")) {
                case "circle" -> deserialization.read(object, Circle.class);
                case "rectangle" -> deserialization.read(object, Rectangle.class);
                default -> throw new JsonMappingException("No shape of kind " + object.get("kind"));
            };
        })
        .build();
```

`mapper.convert(json, type)` binds a value that is already read — maps, lists, scalars from anywhere — into a type, as
`read(text, type)` does after the text.

## Configuration

```java
JsonMapper mapper = JsonMapper.builder()          // every standard serializer, ALL, COMPACT
        .inclusion(JsonInclusion.NON_NULL)        // {"a":1} for {a=1, b=null}; ALL writes {"a":1,"b":null}
        .layout(JsonLayout.INDENTED)              // write(value) indents; write(value, layout) says per call
        .serializer(…)
        .deserializer(…)
        .build();

JsonMapper stricter = mapper.toBuilder().inclusion(JsonInclusion.ALL).build();   // a mapper from a mapper
```

A mapper is immutable and safe to share. `Json` is the default one, and `Json.mapper().toBuilder()` starts a mapper from it.

## The tree

`Json.parse(text)` gives the engine's tree instead of the values:

- `JsonObjectNode.members()` gives `JsonMemberNode`s, each with `name()` and `valueNode()`;
- `JsonArrayNode.elements()` gives the elements;
- `JsonScalarNode` holds a scalar;
- every node has `value()`.

The nodes are `org.jmouse.el.node.Node`s, so the engine's visitors and trivia reach them.

## Refusals

| What | Thrown | Message |
|---|---|---|
| Text that is not JSON | `JsonSyntaxException` | `Not JSON: a member's name in double quotes expected, '}' found at line 1, character 9` |
| An escape JSON has not (`\q`), `\u` without four digits, a raw control character | `JsonSyntaxException` | `Not JSON: \q is no escape of JSON` |
| `01`, `+1`, `.5`, `1.`, `- 1`, `1L`, `0x10`, `'single'`, `none`, `undefined`, a trailing comma | `JsonSyntaxException` | what was expected there |
| JSON that is not of the type asked for | `JsonMappingException` | `JSON could not be read as Point: …` |
| A serializer that threw | `JsonMappingException` | `A Resistance could not be written as JSON: …` |

## Boundaries

- **No dependency but the engine.** The module compiles to JavaScript with the rest (`jmouse/j2js`). A record or a bean
  written or read in the browser needs its members kept for reflection there: `@org.jmouse.j2js.Reflectable` on its class.
- **Speed.** A text is cut by a scanner, not by the expression language's regular expression: JSON is data, a new text every
  time, and in the browser — where TeaVM runs `java.util.regex` as compiled Java — the pattern took 410 ms for 16 KB that the
  scanner cuts in about 20. A structure that does not change is still best read once and kept.
- **Not a streaming parser.** A text is read whole into a tree, then into values. Documents of many megabytes are not what it
  is for.
- **Exactly the type.** A deserializer applies to exactly the type it is registered for, never to the types below it. A
  serializer applies to its type and every type below it with no nearer serializer.

## Engine pieces it stands on

| Piece | Module | Why |
|---|---|---|
| `Splitter`, `RawToken`, `DefaultTokenizer` | `jmouse-el` | JSON's `JsonSplitter` cuts a text into the engine's raw tokens; the engine's tokenizer makes them tokens |
| `EnumTokenRecognizer<BasicToken>`, `Parser`, `ParserContext`, `Extension` | `jmouse-el` | JSON's tokens and parsers, as any language on the engine has them |
| `LiteralParser.numericLiteral(String)` | `jmouse-el` | One rule for a number with no suffix, jME's and JSON's |
| `SourceWriter`, `TextSourceWriter`, `SourceLayout`, `block(opening, closing, body)` | `jmouse-el` | Compact and indented text, an array's `[ ]` as a block of its own |
| `Describer`, `ObjectDescriptor`, `PropertyDescriptor` | `jmouse-core` | A record's or a bean's readable properties |
| `Mapper`, `MappingStrategyContributor`, `OptionalStrategy` | `jmouse-mapper` | Reading into types; a deserializer as a strategy asked first |
| `TypedValue`, `Priority` | `jmouse-core` | The type read into; where a deserializer's strategy stands |
