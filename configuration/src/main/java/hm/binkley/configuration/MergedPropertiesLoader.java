package hm.binkley.configuration;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static java.util.Arrays.asList;
import static java.util.Collections.reverse;

/**
 * {@code MergedPropertiesLoader} needs documentation.
 *
 * @author <a href="mailto:binkley@alumni.rice.edu">B. K. Oxley (binkley)</a>
 * @todo Needs documentation.
 */
public final class MergedPropertiesLoader<E extends Exception>
        implements PropertiesLoader<E> {
    private final List<PropertiesLoader<E>> loaders;

    private MergedPropertiesLoader(@Nonnull final List<PropertiesLoader<E>> loaders) {
        this.loaders = new ArrayList<>(loaders);
        reverse(loaders);
    }

    @Nonnull
    public static <E extends Exception> MergedPropertiesLoader<E> merge(
            final List<PropertiesLoader<E>> loaders) {
        return new MergedPropertiesLoader<>(loaders);
    }

    @Nonnull
    @SafeVarargs
    public static <E extends Exception> MergedPropertiesLoader<E> merge(
            final PropertiesLoader<E>... loaders) {
        return merge(asList(loaders));
    }

    @Nonnull
    @Override
    public Properties load()
            throws E {
        final Properties properties = new Properties();
        for (final PropertiesLoader<E> loader : loaders)
            properties.putAll(loader.load());
        return properties;
    }
}
