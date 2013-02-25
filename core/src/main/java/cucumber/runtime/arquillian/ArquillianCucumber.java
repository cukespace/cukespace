package cucumber.runtime.arquillian;

import cucumber.runtime.CucumberException;
import cucumber.runtime.FeatureBuilder;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.arquillian.backend.ArquillianBackend;
import cucumber.runtime.arquillian.feature.Features;
import cucumber.runtime.arquillian.glue.Glues;
import cucumber.runtime.io.Resource;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.snippets.SummaryPrinter;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Before;
import org.junit.internal.runners.model.MultipleFailureException;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class ArquillianCucumber extends Arquillian {
    private static final String RUN_CUCUMBER_MTD = "runCucumber";

    public ArquillianCucumber(final Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override // no @Test is not an error
    protected void validateInstanceMethods(List<Throwable> errors) {
        validatePublicVoidNoArgMethods(After.class, false, errors);
        validatePublicVoidNoArgMethods(Before.class, false, errors);
        validateTestMethods(errors);
    }

    @Override
    public void filter(final Filter filter) throws NoTestsRemainException {
        if (filter.describe().startsWith("Method " + RUN_CUCUMBER_MTD + "(")) { // not the best test but it does the job
            return;
        }

        super.filter(filter);
    }

    @Override
    protected Statement childrenInvoker(final RunNotifier notifier) {
        return new Statement() {
            @Override
            public void evaluate() {
                // run @Test methods
                for (final FrameworkMethod each : ArquillianCucumber.super.getChildren()) {
                    ArquillianCucumber.super.runChild(each, notifier);
                }

                try { // run cucumber, this looks like a hack but that's to keep @Before/@After/... hooks behavior
                    final Method runCucumber = ArquillianCucumber.class.getDeclaredMethod(RUN_CUCUMBER_MTD, Object.class);
                    runCucumber.setAccessible(true);
                    final InstanceControlledFrameworkMethod runCucumberMtdFramework = new InstanceControlledFrameworkMethod(ArquillianCucumber.this, runCucumber);
                    ArquillianCucumber.super.runChild(runCucumberMtdFramework, notifier);
                } catch (final NoSuchMethodException e) {
                    // no-op: will not accur
                }
            }
        };
    }

    // the cucumber test method, only used internally - see childrenInvoker
    private void runCucumber(final Object testInstance) throws Exception {
        final Class<?> clazz = getTestClass().getJavaClass();
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        final List<CucumberFeature> cucumberFeatures = new ArrayList<CucumberFeature>();
        final FeatureBuilder builder = new FeatureBuilder(cucumberFeatures);

        for (final String path : Features.findFeatures(clazz)) {
            final ClassLoaderResource featureResource = new ClassLoaderResource(tccl, path);
            if (!featureResource.exists()) {
                continue;
            }

            builder.parse(featureResource, Collections.emptyList());
        }

        if (cucumberFeatures.isEmpty()) {
            throw new IllegalArgumentException("No feature found");
        }

        final RuntimeOptions runtimeOptions = new RuntimeOptions(new Properties(), "-f", "pretty", areColorsNotAvailable());
        runtimeOptions.strict = true;

        final cucumber.runtime.Runtime runtime = new cucumber.runtime.Runtime(null, tccl, Arrays.asList(new ArquillianBackend(Glues.findGlues(clazz), clazz, testInstance)), runtimeOptions);
        for (final CucumberFeature feature : cucumberFeatures) {
            final Formatter formatter = runtimeOptions.formatter(tccl);
            final Reporter reporter = runtimeOptions.reporter(tccl);

            feature.run(formatter, reporter, runtime);
        }

        final Formatter formatter = runtimeOptions.formatter(tccl);

        formatter.done();
        new SummaryPrinter(System.out).print(runtime);
        formatter.close();

        final List<Throwable> errors = runtime.getErrors();

        for (final String snippet : runtime.getSnippets()) {
            errors.add(new CucumberException("Missing snippet: " + snippet));
        }

        if (!errors.isEmpty()) {
            throw new MultipleFailureException(errors);
        }
    }

    private static String areColorsNotAvailable() {
        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win") // windows: no comment
                || System.getProperty("java.class.path").contains("idea_rt")) { // doesn't work in IDEa
            return "--monochrome";
        }
        return "--no-monochrome";
    }

    private static class ClassLoaderResource implements Resource {
        private final String path;
        private final ClassLoader loader;

        public ClassLoaderResource(final ClassLoader loader, final String path) {
            this.path = path;
            this.loader = loader;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            final URL resource = loader.getResource(path);
            if (resource == null) {
                throw new IllegalArgumentException(path + " doesn't exist");
            }
            return resource.openStream();
        }

        public boolean exists() {
            return loader.getResource(path) != null;
        }

        @Override
        public String getClassName() {
            return null;
        }
    }

    private static class InstanceControlledFrameworkMethod extends FrameworkMethod {
        private final ArquillianCucumber instance;

        public InstanceControlledFrameworkMethod(final ArquillianCucumber runner, final Method runCucumber) {
            super(runCucumber);
            this.instance = runner;
        }

        @Override
        public Object invokeExplosively(final Object target, final Object... params) throws Throwable {
            instance.runCucumber(target);
            return null;
        }
    }
}
