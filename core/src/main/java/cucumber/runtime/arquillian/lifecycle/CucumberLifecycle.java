package cucumber.runtime.arquillian.lifecycle;

import cucumber.api.java.en.And;
import cucumber.api.java.en.But;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.arquillian.stream.NotCloseablePrintStream;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * These observers are for:
 * 1. get cucumber annotations from a client scan (avoid server scan hacks/specific modules)
 *  -> it is done as late as possible to be able a good TCCL even in embedded mode
 * 2. hack System.out for IDE integration
 */
public class CucumberLifecycle {
    private static final PrintStream ORIGINAL_OUT = System.out;
    private static final PrintStream NOT_CLOSEABLE_OUT = new NotCloseablePrintStream(ORIGINAL_OUT);
    private static final Collection<Class<? extends Annotation>> CUCUMBER_ANNOTATIONS = new CopyOnWriteArrayList<Class<? extends Annotation>>();

    public void init(final @Observes BeforeClass beforeClass) {
        System.setOut(NOT_CLOSEABLE_OUT);
    }

    public void reset(final @Observes AfterClass afterClass) {
        System.setOut(ORIGINAL_OUT);
        CUCUMBER_ANNOTATIONS.clear();
    }

    // do it lazily to have more change to get the right classloader even in embedded case
    public void loadCucumberAnnotations(final @Observes(precedence = 1) Before before) {
        if (!CUCUMBER_ANNOTATIONS.isEmpty()) {
            return;
        }

        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        final InputStream is = tccl.getResourceAsStream("cukespace-annotations.txt");
        if (is != null) {
            String line;
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                while ((line = reader.readLine()) != null) {
                    try {
                        CUCUMBER_ANNOTATIONS.add((Class<? extends Annotation>) tccl.loadClass(line));
                    } catch (ClassNotFoundException e) {
                        // no-op
                    }
                }
            } catch (final IOException e) {
                // no-op
            } finally {
                try {
                    is.close();
                } catch (final IOException e) {
                    // no-op
                }
            }
        }
    }

    public static Collection<Class<? extends Annotation>> cucumberAnnotations() {
        if (!CUCUMBER_ANNOTATIONS.isEmpty()) {
            return CUCUMBER_ANNOTATIONS;
        }
        return Arrays.asList(Given.class, When.class, Then.class, And.class, But.class);
    }
}
