package cucumber.runtime.arquillian;

import cucumber.api.junit.Cucumber;
import cucumber.runtime.CucumberException;
import cucumber.runtime.FeatureBuilder;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.arquillian.api.Tags;
import cucumber.runtime.arquillian.backend.ArquillianBackend;
import cucumber.runtime.arquillian.config.CucumberConfiguration;
import cucumber.runtime.arquillian.feature.Features;
import cucumber.runtime.arquillian.glue.Glues;
import cucumber.runtime.arquillian.reporter.CucumberReporter;
import cucumber.runtime.arquillian.shared.ClientServerFiles;
import cucumber.runtime.io.Resource;
import cucumber.runtime.junit.RuntimeOptionsFactory;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.snippets.SummaryPrinter;
import gherkin.formatter.Formatter;
import gherkin.formatter.JSONFormatter;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

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

        final InputStream configIs = tccl.getResourceAsStream(ClientServerFiles.CONFIG);
        final Properties cukespaceConfig = new Properties();
        if (configIs != null) {
            cukespaceConfig.load(configIs);
        } else { // probably on the client side
            final CucumberConfiguration config = CucumberConfiguration.instance();
            if (config.isInitialized()) {
                cukespaceConfig.setProperty(CucumberConfiguration.COLORS, Boolean.toString(config.isColorized()));
                cukespaceConfig.setProperty(CucumberConfiguration.REPORTABLE, Boolean.toString(config.isReport()));
                cukespaceConfig.setProperty(CucumberConfiguration.REPORTABLE_PATH, config.getReportDirectory());
                if (config.getFeatureHome() != null) {
                    cukespaceConfig.setProperty(CucumberConfiguration.FEATURE_HOME, config.getFeatureHome());
                }
                if (config.hasOptions()) {
                    cukespaceConfig.setProperty(CucumberConfiguration.OPTIONS, config.getOptions());
                }
                if (config.getFeatureHome() != null) {
                    cukespaceConfig.setProperty(CucumberConfiguration.FEATURE_HOME, config.getFeatureHome());
                }
            }
        }

        final List<CucumberFeature> cucumberFeatures = new ArrayList<CucumberFeature>();
        final FeatureBuilder builder = new FeatureBuilder(cucumberFeatures);

        final List<Object> filters = createFilters(testInstance);

        final InputStream featuresIs = tccl.getResourceAsStream(ClientServerFiles.FEATURES_LIST);
        if (featuresIs != null) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(featuresIs));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                builder.parse(new ClassLoaderResource(tccl, line), filters);
            }
        } else { // client side
            for (final Map.Entry<String, Collection<URL>> entry : Features.createFeatureMap(cukespaceConfig.getProperty(CucumberConfiguration.FEATURE_HOME), clazz, tccl).entrySet()) {
                final String path = entry.getKey();

                for (final URL url : entry.getValue()) {
                    builder.parse(new URLResource(path, url), filters);
                }
            }
        }

        if (cucumberFeatures.isEmpty()) {
            throw new IllegalArgumentException("No feature found");
        }

        final RuntimeOptions runtimeOptions;
        if (clazz.getAnnotation(Cucumber.Options.class) != null) { // by class setting
            final RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(clazz);
            runtimeOptions = runtimeOptionsFactory.create();
            cleanClasspathList(runtimeOptions.glue);
            cleanClasspathList(runtimeOptions.featurePaths);
        } else if (cukespaceConfig.containsKey(CucumberConfiguration.OPTIONS)) { // arquillian setting
            runtimeOptions = new RuntimeOptions(new Properties(), cukespaceConfig.getProperty(CucumberConfiguration.OPTIONS).split(" "));
            runtimeOptions.strict = true;
        } else { // default
            runtimeOptions = new RuntimeOptions(new Properties(), "-f", "pretty", areColorsNotAvailable(cukespaceConfig));
            runtimeOptions.strict = true;
        }

        final StringBuilder reportBuilder = new StringBuilder();
        final boolean reported = Boolean.parseBoolean(cukespaceConfig.getProperty(CucumberConfiguration.REPORTABLE, "false"));
        if (reported) {
            runtimeOptions.formatters.add(new JSONFormatter(reportBuilder));
        }

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

        if (reported) {
            final String path = cukespaceConfig.getProperty(CucumberConfiguration.REPORTABLE_PATH);
            if (path != null) {
                final File destination = CucumberConfiguration.reportFile(path, clazz);
                final File parentFile = destination.getParentFile();
                if (!parentFile.exists() && !parentFile.mkdirs()) {
                    throw new IllegalArgumentException("Can't create " + parentFile.getAbsolutePath());
                }

                FileWriter writer = null;
                try {
                    writer = new FileWriter(destination);
                    writer.write(reportBuilder.toString());
                    writer.flush();
                } catch (final IOException e) {
                    if (writer != null) {
                        writer.close();
                    }
                }

                // add it here too for client case
                CucumberReporter.addReport(CucumberConfiguration.reportFile(path, clazz));
            }
        }

        final List<Throwable> errors = runtime.getErrors();
        for (final String snippet : runtime.getSnippets()) {
            errors.add(new CucumberException("Missing snippet: " + snippet));
        }
        if (!errors.isEmpty()) {
            throw new MultipleFailureException(errors);
        }
    }

    private static List<Object> createFilters(final Object testInstance) {
        final List<Object> filters = new ArrayList<Object>();

        final Class<?> clazz = testInstance.getClass();

        { // our API
            final Tags tags = clazz.getAnnotation(Tags.class);
            if (tags != null) {
                filters.addAll(Arrays.asList(tags.value()));
            }
        }

        { // cucumber-junit
            final Cucumber.Options options = clazz.getAnnotation(Cucumber.Options.class);
            if (options != null) {
                if (options.tags().length > 0) {
                    filters.addAll(Arrays.asList(options.tags()));
                }
                if (options.name().length > 0) {
                    for (final String name : options.name()) {
                        filters.add(Pattern.compile(name));
                    }
                }
            }
        }

        return filters;
    }

    // classpath: doesn't support scanning, it should be done on client side if supported, not server side
    private static void cleanClasspathList(final List<String> list) {
        final Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().startsWith("classpath:")) {
                it.remove();
            }
        }
    }

    private static String areColorsNotAvailable(final Properties cukespaceConfig) {
        if (!Boolean.parseBoolean(cukespaceConfig.getProperty("colors", "false"))) {
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

    private static class URLResource implements Resource {
        private final URL url;
        private final String path;

        public URLResource(final String path, final URL url) {
            this.url = url;
            this.path = path;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return url.openStream();
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
