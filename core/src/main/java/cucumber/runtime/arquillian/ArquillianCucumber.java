package cucumber.runtime.arquillian;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import cucumber.runtime.CucumberException;
import cucumber.runtime.Env;
import cucumber.runtime.FeatureBuilder;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.RuntimeOptionsFactory;
import cucumber.runtime.SummaryPrinter;
import cucumber.runtime.arquillian.api.Tags;
import cucumber.runtime.arquillian.backend.ArquillianBackend;
import cucumber.runtime.arquillian.config.CucumberConfiguration;
import cucumber.runtime.arquillian.feature.Features;
import cucumber.runtime.arquillian.glue.Glues;
import cucumber.runtime.arquillian.reporter.CucumberReporter;
import cucumber.runtime.arquillian.shared.ClientServerFiles;
import cucumber.runtime.io.Resource;
import cucumber.runtime.model.CucumberFeature;
import gherkin.formatter.Formatter;
import gherkin.formatter.JSONFormatter;
import gherkin.formatter.Reporter;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.MultipleFailureException;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

public class ArquillianCucumber extends Arquillian {
    private static final String RUN_CUCUMBER_MTD = "runCucumber";
    private static final Class[] OPTIONS_ANNOTATIONS = new Class[]{CucumberOptions.class, Cucumber.Options.class};

    private List<FrameworkMethod> methods = null;

    public ArquillianCucumber(final Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected Description describeChild(final FrameworkMethod method)
    {
        if (!Boolean.getBoolean("cukespace.runner.standard-describe")
                && InstanceControlledFrameworkMethod.class.isInstance(method)) {
            return Description.createTestDescription(InstanceControlledFrameworkMethod.class.cast(method).getOriginalClass(), testName(method), method.getAnnotations());
        }
        return super.describeChild(method);
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        if (methods != null) {
            return methods;
        }

        methods = new LinkedList<FrameworkMethod>();

        // run @Test methods
        for (final FrameworkMethod each : ArquillianCucumber.super.computeTestMethods()) {
            methods.add(each);
        }

        try { // run cucumber, this looks like a hack but that's to keep @Before/@After/... hooks behavior
            final Method runCucumber = ArquillianCucumber.class.getDeclaredMethod(RUN_CUCUMBER_MTD, Object.class);
            runCucumber.setAccessible(true);
            final InstanceControlledFrameworkMethod runCucumberMtdFramework = new InstanceControlledFrameworkMethod(ArquillianCucumber.this, getTestClass().getJavaClass(), runCucumber);
            methods.add(runCucumberMtdFramework);
        } catch (final NoSuchMethodException e) {
            // no-op: will not accur...if so this exception is not your biggest issue
        }

        return methods;
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
        if (clazz.getAnnotation(Cucumber.Options.class) != null || clazz.getAnnotation(CucumberOptions.class) != null) { // by class setting
            final RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(clazz, OPTIONS_ANNOTATIONS);
            runtimeOptions = runtimeOptionsFactory.create();
            cleanClasspathList(runtimeOptions.getGlue());
            cleanClasspathList(runtimeOptions.getFeaturePaths());
        } else if (cukespaceConfig.containsKey(CucumberConfiguration.OPTIONS)) { // arquillian setting
            runtimeOptions = new RuntimeOptions(new Env("cucumber-jvm"), asList((cukespaceConfig.getProperty(CucumberConfiguration.OPTIONS, "--strict") + " --strict").split(" ")));
        } else { // default
            runtimeOptions = new RuntimeOptions(new Env("cucumber-jvm"), asList("--strict", "-f", "pretty", areColorsNotAvailable(cukespaceConfig)));
        }

        final boolean reported = Boolean.parseBoolean(cukespaceConfig.getProperty(CucumberConfiguration.REPORTABLE, "false"));
        final StringBuilder reportBuilder = new StringBuilder();
        if (reported) {
            runtimeOptions.addFormatter(new JSONFormatter(reportBuilder));
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

        @Override
        public String getClassName(final String extension) {
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
        public String getClassName(final String extension) {
            return null;
        }
    }

    private static class InstanceControlledFrameworkMethod extends FrameworkMethod {
        private final ArquillianCucumber instance;
        private final Class<?> originalClass;

        public InstanceControlledFrameworkMethod(final ArquillianCucumber runner, final Class<?> originalClass, final Method runCucumber) {
            super(runCucumber);
            this.originalClass = originalClass;
            this.instance = runner;
        }

        @Override
        public Object invokeExplosively(final Object target, final Object... params) throws Throwable {
            instance.runCucumber(target);
            return null;
        }

        public Class<?> getOriginalClass() {
            return originalClass;
        }
    }
}
