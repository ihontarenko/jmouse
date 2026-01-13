# jMouse Pipeline Framework

A lightweight, format-agnostic pipeline framework built around a **canonical definition model**,
supporting **Java DSL**, **XML / JSON / YAML / Java Properties**, and **annotation-based pipelines**.

The framework enforces an explicit execution model where:

> **Processors return a structured `PipelineResult`, not a raw string.**
> **Each processor may access the previous processor result.**

---

## 1. Core Principles

- Canonical pipeline model (no runtime XML/annotation dependency)
- Explicit compilation step: definition → runtime graph
- Structured processor results (`PipelineResult`)
- Deterministic transitions
- Multiple definition sources
- Zero hidden magic

---

## 2. High-Level Architecture

```

definition/
├─ dsl            → Java DSL (authoring pipelines)
├─ model          → Canonical definition model
├─ processing     → normalize + validate
└─ loading        → XML / JSON / YAML / properties / annotations

runtime/
├─ compiler       → PipelineDefinition → PipelineChain
└─ execution      → PipelineChain.proceed(context)

````

---

## 3. PipelineResult

### 3.1 PipelineResult Contract

```java
public interface PipelineResult {

    String code();          // transition key (e.g. NEXT, ERROR, FINISH)
    Object payload();       // optional data produced by processor

    static PipelineResult of(String code) {
        return new DefaultPipelineResult(code, null);
    }

    static PipelineResult of(String code, Object payload) {
        return new DefaultPipelineResult(code, payload);
    }
}
````

### 3.2 Typical Result Codes

* `NEXT`
* `ERROR`
* `FINISH`
* custom domain-specific codes

---

## 4. Processor API

### 4.1 Basic Processor

```java
public class HelloProcessor implements PipelineProcessor {

    public String message;

    @Override
    public PipelineResult process(PipelineContext context) {
        context.getResultContext().put("message", message);
        return PipelineResult.of("NEXT");
    }
}
```

---

### 4.2 Accessing Previous Result via Context

```java
public class EnrichProcessor implements PipelineProcessor {

    @Override
    public PipelineResult process(PipelineContext context) {
        PipelineResult previous =
            context.getResultContext().getPreviousResult();

        Object payload = previous != null ? previous.payload() : null;
        return PipelineResult.of("NEXT", payload);
    }
}
```

---

### 4.3 Overloaded Processor Method (Explicit Previous Result)

If the processor implements the overloaded signature, the runtime invokes it automatically:

```java
public class TransformProcessor implements PipelineProcessor {

    public PipelineResult process(
            PipelineContext context,
            PipelineResult previousResult
    ) {
        Object input = previousResult != null ? previousResult.payload() : null;
        Object output = transform(input);
        return PipelineResult.of("NEXT", output);
    }
}
```

> **Resolution rule**
>
> * If `(PipelineContext, PipelineResult)` exists → it is used
> * Otherwise → fallback to `(PipelineContext)`

---

## 5. Pipeline Context & Result Context

```java
PipelineResult previous =
    context.getResultContext().getPreviousResult();

PipelineResult current =
    context.getResultContext().getCurrentResult();
```

The result context is automatically updated after each processor execution.

---

## 6. Java DSL Example

```java
PipelineDefinition definition =
    PipelineDefinitions.pipeline("DEFAULT", pipeline -> {

        pipeline.chain("hello-chain", chain -> {
            chain.initial("hello");

            chain.link("hello")
                 .processor(HelloProcessor.class)
                 .param("message", "Hello World", "VALUE", null)
                 .onReturn("NEXT", "finish");

            chain.link("finish")
                 .processor(FinishProcessor.class);
        });
    });
```

---

## 7. XML Definition Example

```xml
<pipeline name="DEFAULT">

    <processor-chain name="hello-chain" initial="hello">

        <processor-link name="hello">
            <processor class="com.example.HelloProcessor">
                <parameter name="message"
                           value="Hello World"
                           resolver="VALUE"/>
            </processor>
            <properties>
                <transition return="NEXT" link="finish"/>
            </properties>
        </processor-link>

        <processor-link name="finish">
            <processor class="com.example.FinishProcessor"/>
        </processor-link>

    </processor-chain>

</pipeline>
```

---

## 8. JSON Definition Example

```json
{
  "name": "DEFAULT",
  "chains": {
    "hello-chain": {
      "name": "hello-chain",
      "initial": "hello",
      "links": {
        "hello": {
          "name": "hello",
          "processor": {
            "className": "com.example.HelloProcessor",
            "parameters": [
              {
                "name": "message",
                "value": "Hello World",
                "resolver": "VALUE"
              }
            ]
          },
          "properties": {
            "transitions": {
              "NEXT": "finish"
            }
          }
        }
      }
    }
  }
}
```

---

## 9. Annotation-Based Pipelines

```java
@PipelineLink(chain = "hello-chain", name = "hello", initial = true)
@OnReturn(code = "NEXT", link = "finish")
@Param(name = "message", value = "Hello World", resolver = "VALUE")
public class HelloProcessor implements PipelineProcessor {

    public String message;

    @Override
    public PipelineResult process(PipelineContext context) {
        return PipelineResult.of("NEXT");
    }
}
```

---

## 10. Loading Definitions

```java
PipelineDefinition def =
    loader.load(new ClasspathSource("/pipeline/pipeline.xml"));
```

```java
PipelineDefinition def =
    loader.load(new AnnotationsSource(
        "DEFAULT",
        RootClassForScan.class
    ));
```

---

## 11. Compile & Run

```java
PipelineCompiler compiler =
    new DefaultPipelineCompiler(
        new PipelineProcessorFactory(),
        new DefaultProxyFactory()
    );

PipelineManager manager =
    new PipelineManager(definition, compiler);

PipelineContext context = new DefaultPipelineContext();
manager.run("hello-chain", context);
```

---

## 12. Validation Guarantees

* initial link exists
* transitions target valid links
* fallback link exists
* processor class is resolvable
* invalid pipelines fail fast (compile-time)

---

## 13. Design Summary

* Canonical-first
* Structured results
* Deterministic transitions
* Explicit execution
* No hidden runtime magic

---

**This pipeline framework is intentionally strict, explicit, and extensible.**
