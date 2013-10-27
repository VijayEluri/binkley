/*
 * This is free and unencumbered software released into the public domain.
 *
 * Please see https://github.com/binkley/binkley/blob/master/LICENSE.md.
 */

package hm.binkley.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.kohsuke.MetaInfServices;
import org.nnsoft.guice.lifegycle.AfterInjectionModule;
import org.nnsoft.guice.lifegycle.DisposeModule;
import org.nnsoft.guice.lifegycle.Disposer;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static com.google.inject.matcher.Matchers.any;
import static java.lang.Runtime.getRuntime;

/**
 * {@code LifecycleModule} enables <a href="https://en.wikipedia.org/wiki/JSR_250">the JSR-250
 * annotations</a>, &#64;{@link PostConstruct} and &#64;{@link PreDestroy}, in Guice via the <a
 * href="http://99soft.github.io/lifegycle/">lifegycle library</a>.  Google shows several other,
 * more fully featured soutions; this is the smallest implementation.
 * <p/>
 * Guice lacks shutdown hooks.  For {@code @PreDestroy} to work you must {@link
 * #enablePreDestroy(Injector) install one yourself}: <pre>
 * Injector guice = Guice.createInjector(new LifecycleModule());
 * LifecycleModule.enablePreDestroy(guice);</pre>
 *
 * @author <a href="mailto:binkley@alumni.rice.edu">B. K. Oxley (binkley)</a>
 * @todo References to alternative approaches
 * @todo Work out relation to {@code ProvisionListener} in Guice 4
 */
@MetaInfServices(Module.class)
public final class LifecycleModule
        extends AbstractModule {
    /**
     * After creating the Guice injector, invoke this method on the injector to enable {@link
     * PreDestroy} when the JVM shuts down.
     *
     * @param guice the Guice injector, never missing
     *
     * @return the <var>guice</var> injector
     */
    @SuppressWarnings("UnusedDeclaration")
    @Nonnull
    public static Injector enablePreDestroy(@Nonnull final Injector guice) {
        getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                guice.getInstance(Disposer.class).dispose();
            }
        });
        return guice;
    }

    @Override
    protected void configure() {
        install(new AfterInjectionModule(PostConstruct.class, any()));
        install(new DisposeModule(PreDestroy.class, any()));
    }
}
