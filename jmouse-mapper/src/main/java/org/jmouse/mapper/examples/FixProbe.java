package org.jmouse.mapper.examples;

import org.jmouse.core.access.TypedValue;
import org.jmouse.mapper.Mapper;
import org.jmouse.mapper.MapperConversion;
import org.jmouse.mapper.Mappers;
import org.jmouse.mapper.binding.TypeMappingRegistry;
import org.jmouse.mapper.binding.annotation.MappingReference;
import org.jmouse.mapper.config.CollectionMappingPolicy;
import org.jmouse.mapper.config.MappingConfig;
import org.jmouse.mapper.config.MappingPolicy;
import org.jmouse.mapper.config.NullHandlingPolicy;
import org.jmouse.mapper.errors.MappingException;
import org.jmouse.mapper.strategy.MappingStrategy;
import org.jmouse.mapper.strategy.MappingStrategyRegistry;
import org.jmouse.core.access.ObjectAccessorWrapper;
import org.jmouse.core.reflection.InferredType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Verification driver for the JMF-143 fixes. Prints PASS/FAIL per claim. */
public class FixProbe {

    public static void main(String... arguments) {
        nullPolicyIsHonoured();
        collectionPolicyIsHonoured();
        strategiesAreCached();
        errorsCarryTheirPath();
        annotationsWorkOutOfTheBox();
        oneRenameIsOneLine();
    }

    /** A rename must not cost a registry rebuild, and must not cost the defaults either. */
    private static void oneRenameIsOneLine() {
        Mapper pair = Mappers.mapper(Source.class, Target.class, mapping -> mapping
                .rename("sourceName", "targetName"));

        Source source = new Source();
        source.setSourceName("renamed");
        source.setShared("carried");

        Target target = pair.map(source, Target.class);

        report("one rename, one line -> targetName=" + target.getTargetName(),
               "renamed".equals(target.getTargetName()));
        report("same-named properties still travel for free -> shared=" + target.getShared(),
               "carried".equals(target.getShared()));

        Mapper many = Mappers.mapper(rules -> rules
                .mapping(Source.class, Target.class, mapping -> mapping.rename("sourceName", "targetName")));
        report("the many-pairs form works the same",
               "renamed".equals(many.map(source, Target.class).getTargetName()));

        // A rule and an annotation covering the same target, from one map source: the DSL rule fills
        // targetName, AnnotationRuleSource fills annotated. Both must arrive, or `rules` replaced
        // the presets instead of adding to them.
        Mapper viaBuilder = Mappers.builder()
                .rules(rules -> rules
                        .mapping(Map.class, Target.class, mapping -> mapping.rename("sourceName", "targetName")))
                .build();
        Target both = viaBuilder.map(Map.of("sourceName", "renamed", "annotatedFrom", "kept"), Target.class);
        report("builder rules ADD to the defaults -> targetName=" + both.getTargetName()
                       + " annotated=" + both.getAnnotated(),
               "renamed".equals(both.getTargetName()) && "kept".equals(both.getAnnotated()));
    }

    /** PROPAGATE must clear a target property; SKIP (the default) must leave it. */
    private static void nullPolicyIsHonoured() {
        Holder skipped = mapperWith(NullHandlingPolicy.SKIP)
                .map(Map.of("kept", "new"), new Holder("kept-before", "cleared-before"));
        Holder cleared = mapperWith(NullHandlingPolicy.PROPAGATE)
                .map(Map.of("kept", "new"), new Holder("kept-before", "cleared-before"));

        report("NullHandlingPolicy.SKIP leaves the property",
               "cleared-before".equals(skipped.getCleared()));
        report("NullHandlingPolicy.PROPAGATE clears the property",
               cleared.getCleared() == null);
        report("a primitive is never nulled under PROPAGATE",
               cleared.getCount() == 7);
    }

    /** REPLACE must not append to a collection the target already holds. */
    private static void collectionPolicyIsHonoured() {
        Bag replaced = new Bag(new ArrayList<>(List.of("stale")));
        mapperWithCollections(CollectionMappingPolicy.REPLACE)
                .map(Map.of("items", List.of("a", "b")), replaced);

        Bag appended = new Bag(new ArrayList<>(List.of("stale")));
        mapperWithCollections(CollectionMappingPolicy.MERGE_APPEND)
                .map(Map.of("items", List.of("a", "b")), appended);

        report("CollectionMappingPolicy.REPLACE replaces  -> " + replaced.getItems(),
               List.of("a", "b").equals(replaced.getItems()));
        report("CollectionMappingPolicy.MERGE_APPEND appends -> " + appended.getItems(),
               List.of("stale", "a", "b").equals(appended.getItems()));
    }

    /**
     * The same {@code (sourceClass, targetType)} pair must resolve to one strategy instance.
     *
     * <h2>⚠️ Half of these checks exist to fail if the key is ever coarsened</h2>
     *
     * <p>A strategy chosen for the wrong pair does not throw — it maps the wrong way, and it does so
     * consistently, which is how it survives a test suite. The failure has happened here before:
     * {@code InferredType} once handed every {@code List<Y>} the first {@code List<X>}'s element type,
     * because the key's {@code equals} was blunter than the value it stood for.</p>
     *
     * <p>So the memo is probed from all three directions a key can be blunted in: dropping the source,
     * dropping the target, and dropping the target's generic arguments.</p>
     */
    private static void strategiesAreCached() {
        MappingStrategyRegistry registry = new MappingStrategyRegistry(Mappers.DEFAULT_CONTRIBUTORS);

        InferredType holder = InferredType.forType(Holder.class);

        MappingStrategy<?> probeOne = registry.strategyFor(new Holder(), TypedValue.of(holder), contextOf());
        MappingStrategy<?> probeTwo = registry.strategyFor(new Holder(), TypedValue.of(holder), contextOf());

        report("a strategy is resolved once per type pair", probeOne == probeTwo);

        MappingStrategy<?> different = registry.strategyFor(
                new Holder(), TypedValue.of(InferredType.forType(Bag.class)), contextOf());
        report("a different target still resolves separately", probeOne != different);

        // ⚠️ Same target, different source. A key that dropped the source class would hand a Map source
        // the bean-to-bean strategy built for a Holder — and it would fill nothing, quietly.
        MappingStrategy<?> fromMap = registry.strategyFor(
                Map.of("kept", "value"), TypedValue.of(holder), contextOf());
        report("a different SOURCE still resolves separately", probeOne != fromMap);

        // ⚠️ Same raw target class, different element type. This is the shape that already went wrong
        // once: the two InferredTypes differ only in a generic argument, and a key whose equals stops at
        // List.class puts every list through the first one's converter.
        InferredType listOfString  = InferredType.forParametrizedClass(List.class, String.class);
        InferredType listOfInteger = InferredType.forParametrizedClass(List.class, Integer.class);

        MappingStrategy<?> strings = registry.strategyFor(
                List.of("1"), TypedValue.of(listOfString), contextOf());
        MappingStrategy<?> integers = registry.strategyFor(
                List.of("1"), TypedValue.of(listOfInteger), contextOf());

        report("a target differing only in its GENERIC argument resolves separately", strings != integers);

        // ...and the memo still has to answer the same question with the same instance, or the two checks
        // above would pass for a registry that simply never caches anything.
        MappingStrategy<?> stringsAgain = registry.strategyFor(
                List.of("2"), TypedValue.of(listOfString), contextOf());

        report("...and is still memoized, so the checks above mean something", strings == stringsAgain);
    }

    /** A failure must name the property it happened at, not the root. */
    private static void errorsCarryTheirPath() {
        try {
            Mappers.defaultMapper().map(Map.of("count", "not-a-number"), new Holder());
            report("a failing property reports its path", false);
        } catch (MappingException exception) {
            String path = String.valueOf(exception.path());
            report("a failing property reports its path -> '" + path + "'", path.contains("count"));
        }
    }

    /** @MappingReference must be read by a mapper nobody configured. */
    private static void annotationsWorkOutOfTheBox() {
        Annotated annotated = Mappers.defaultMapper().map(Map.of("sourceName", "renamed"), Annotated.class);
        report("@MappingReference is honoured by the default mapper -> " + annotated.getTargetName(),
               "renamed".equals(annotated.getTargetName()));
    }

    private static org.jmouse.mapper.MappingContext contextOf() {
        return new org.jmouse.mapper.MappingContext(
                org.jmouse.mapper.ObjectMapper::new,
                new MappingStrategyRegistry(Mappers.DEFAULT_CONTRIBUTORS),
                new ObjectAccessorWrapper(),
                new MapperConversion(),
                TypeMappingRegistry.builder().build(),
                MappingPolicy.defaults(),
                MappingConfig.defaults(),
                org.jmouse.mapper.MappingScope.root(null)
        );
    }

    private static Mapper mapperWith(NullHandlingPolicy nullHandlingPolicy) {
        return Mappers.builder()
                .policy(MappingPolicy.builder().nullHandlingPolicy(nullHandlingPolicy).build())
                .build();
    }

    private static Mapper mapperWithCollections(CollectionMappingPolicy collectionMappingPolicy) {
        return Mappers.builder()
                .policy(MappingPolicy.builder().collectionMappingPolicy(collectionMappingPolicy).build())
                .build();
    }

    private static void report(String claim, boolean passed) {
        System.out.println((passed ? "PASS  " : "FAIL  ") + claim);
    }

    public static class Holder {
        private String kept;
        private String cleared;
        private int    count = 7;

        public Holder() {}

        public Holder(String kept, String cleared) {
            this.kept = kept;
            this.cleared = cleared;
        }

        public String getKept() { return kept; }
        public void setKept(String kept) { this.kept = kept; }
        public String getCleared() { return cleared; }
        public void setCleared(String cleared) { this.cleared = cleared; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }

    public static class Bag {
        private List<String> items;

        public Bag() {}

        public Bag(List<String> items) { this.items = items; }

        public List<String> getItems() { return items; }
        public void setItems(List<String> items) { this.items = items; }
    }

    public static class Source {
        private String sourceName;
        private String shared;

        public String getSourceName() { return sourceName; }
        public void setSourceName(String sourceName) { this.sourceName = sourceName; }
        public String getShared() { return shared; }
        public void setShared(String shared) { this.shared = shared; }
    }

    public static class Target {
        private String targetName;
        private String shared;
        private String annotated;

        public String getTargetName() { return targetName; }
        public void setTargetName(String targetName) { this.targetName = targetName; }
        public String getShared() { return shared; }
        public void setShared(String shared) { this.shared = shared; }
        public String getAnnotated() { return annotated; }

        @MappingReference("annotatedFrom")
        public void setAnnotated(String annotated) { this.annotated = annotated; }
    }

    public static class Annotated {
        private String targetName;

        public String getTargetName() { return targetName; }

        @MappingReference("sourceName")
        public void setTargetName(String targetName) { this.targetName = targetName; }
    }
}
