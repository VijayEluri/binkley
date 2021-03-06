package hm.binkley.annotation.processing.y;

import hm.binkley.annotation.processing.LoadedTemplate;
import hm.binkley.annotation.processing.LoadedYaml;
import hm.binkley.annotation.processing.YamlGenerateMesseger;
import hm.binkley.util.Listable;
import hm.binkley.util.YamlHelper;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nonnull;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.yaml.snakeyaml.DumperOptions.FlowStyle.FLOW;
import static org.yaml.snakeyaml.DumperOptions.ScalarStyle.PLAIN;

/**
 * Represents YAML class/enum definitions immutably and accessible from
 * FreeMarker.  Typical: <pre>
 * Foo:
 *   .meta:
 *     doc: I am the one and only Foo!
 *   bar:
 *     doc: I am bar
 *     value: 0</pre>
 *
 * @author <a href="mailto:binkley@alumni.rice.edu">B. K. Oxley (binkley)</a>
 * @todo Documentation
 */
public final class YModel
        implements Listable<YType> {
    // TODO: Some less gross place for this global
    public static final Map<ZisZuper, List<YMethod>> methods
            = new LinkedHashMap<>();

    private final String generator;

    @Nonnull
    private final Yaml yaml = YamlHelper.builder().build(dumper -> {
        dumper.setDefaultFlowStyle(FLOW);
        dumper.setDefaultScalarStyle(PLAIN);
        dumper.setTimeZone(null);
        dumper.setWidth(Integer.MAX_VALUE);
    });
    private final Consumer<Function<YamlGenerateMesseger, YamlGenerateMesseger>>
            out;
    private final List<YType> types;

    public YModel(final String generator, @Nonnull final Element root,
            @Nonnull final LoadedTemplate template,
            @Nonnull final LoadedYaml loaded, @Nonnull final Name packaj,
            @Nonnull final Function<CharSequence, Name> getName, @Nonnull
    final Consumer<Function<YamlGenerateMesseger, YamlGenerateMesseger>> out) {
        this.generator = generator;
        this.out = out;

        final Name ns = loaded.what.entrySet().stream().
                filter(e -> ".meta".equals(e.getKey())).
                map(e -> (Map<String, Object>) (Map) e.getValue()).
                flatMap(m -> m.entrySet().stream()).
                filter(e -> "namespace".equals(e.getKey())).
                map(Entry::getValue).
                map(v -> getName.apply(packaj + "." + v)).
                findFirst().
                orElse(packaj);
        // Caution - peek for side effect; DO NOT parallelize
        types = loaded.what.entrySet().stream().
                filter(e -> !".meta".equals(e.getKey())).
                peek(e -> out.accept(o -> o.atYamlBlock(e))).
                map(e -> yType(root, template, loaded, ns, e)).
                collect(toList());
    }

    @Nonnull
    @Override
    public List<YType> list() {
        return unmodifiableList(types);
    }

    @Nonnull
    private YType yType(final Element root, final LoadedTemplate template,
            final LoadedYaml loaded, final Name packaj,
            final Entry<String, Map<String, Map<String, Object>>> e) {
        final ZisZuper names = ZisZuper.from(packaj, e.getKey(), root);
        if (null == names) {
            out.accept(o -> {
                o.error("'%s' classes have at most one parent for '%s'",
                        e.getKey());
                return o;
            });
            throw new IllegalStateException("Failed YAML");
        }

        final YGenerate type = YGenerate.from(names);
        switch (type) {
        case YCLASS:
            if (null != names.zuper && !methods.keySet().stream().
                    filter(zz -> zz.zis.equals(names.zuper)).
                    findAny().
                    isPresent()) {
                out.accept(o -> {
                    o.error("Undefined parent for '%s'", e.getKey());
                    return o;
                });
                throw new IllegalStateException("Failed YAML");
            }
        }

        final Map<String, Map<String, Object>> rawValue = e.getValue();
        final WithMetaMap value = new WithMetaMap(
                null == rawValue ? emptyMap() : rawValue);

        return new YType(generator, yaml, template, loaded, names, type,
                value, out);
    }
}
