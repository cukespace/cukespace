package cucumber.runtime.arquillian;

import cucumber.api.CucumberOptions;
import cucumber.runtime.CucumberException;
import cucumber.runtime.Env;
import cucumber.runtime.FeatureBuilder;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.RuntimeOptionsFactory;
import cucumber.runtime.StepDefinitionMatch;
import cucumber.runtime.arquillian.api.Tags;
import cucumber.runtime.arquillian.api.event.AfterAfterHooks;
import cucumber.runtime.arquillian.api.event.AfterBeforeHooks;
import cucumber.runtime.arquillian.api.event.AfterStep;
import cucumber.runtime.arquillian.api.event.BeforeAfterHooks;
import cucumber.runtime.arquillian.api.event.BeforeBeforeHooks;
import cucumber.runtime.arquillian.api.event.BeforeStep;
import cucumber.runtime.arquillian.backend.ArquillianBackend;
import cucumber.runtime.arquillian.config.CucumberConfiguration;
import cucumber.runtime.arquillian.feature.Features;
import cucumber.runtime.arquillian.glue.Glues;
import cucumber.runtime.arquillian.reporter.CucumberReporter;
import cucumber.runtime.arquillian.shared.ClientServerFiles;
import cucumber.runtime.arquillian.shared.EventHelper;
import cucumber.runtime.io.Resource;
import cucumber.runtime.junit.FeatureRunner;
import cucumber.runtime.junit.JUnitReporter;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.PathWithLines;
import gherkin.I18n;
import gherkin.formatter.Formatter;
import gherkin.formatter.JSONFormatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Step;
import gherkin.formatter.model.Tag;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

public class ArquillianCucumber extends Arquillian {
    private static final Logger LOGGER = Logger.getLogger(ArquillianCucumber.class.getName());

    private static final String RUN_CUCUMBER_MTD = "____Cucumber_Runner_Not_A_Test";

    private List<FrameworkMethod> methods;

    public ArquillianCucumber(final Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected Description describeChild(final FrameworkMethod method)
    {
        if (!Boolean.getBoolean("cukespace.runner.standard-describe")
                && InstanceControlledFrameworkMethod.class.isInstance(method)) {
            return Description.createTestDescription(
                    InstanceControlledFrameworkMethod.class.cast(method).getOriginalClass(),
                    "____Cucumber_Runner_Not_A_Test",
                    method.getAnnotations());
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
            final Method runCucumber = ArquillianCucumber.class.getDeclaredMethod(RUN_CUCUMBER_MTD, Object.class, RunNotifier.class);
            methods.add(new InstanceControlledFrameworkMethod(this, getTestClass().getJavaClass(), runCucumber));
        } catch (final NoSuchMethodException e) {
            // no-op: will not accur...if so this exception is not your biggest issue
        }

        return methods;
    }

    @Override
    protected void runChild(final FrameworkMethod method, final RunNotifier notifier) {
        if (InstanceControlledFrameworkMethod.class.isInstance(method)) {
            InstanceControlledFrameworkMethod.class.cast(method).setNotifier(notifier);
        }
        super.runChild(method, notifier);
    }

    // the cucumber test method, only used internally - see childrenInvoker, public to avoid to setAccessible(true)
    public void ____Cucumber_Runner_Not_A_Test(final Object testInstance, final RunNotifier runNotifier) throws Exception {
        final Class<?> clazz = getTestClass().getJavaClass();
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        final InputStream configIs = tccl.getResourceAsStream(ClientServerFiles.CONFIG);
        final Properties cukespaceConfig = new Properties();
        if (configIs != null) {
            cukespaceConfig.load(configIs);
        } else { // probably on the client side
            final CucumberConfiguration config = CucumberConfiguration.instance();
            if (config.isInitialized()) {
                cukespaceConfig.setProperty(CucumberConfiguration.PERSISTENCE_EVENTS, Boolean.toString(config.arePersistenceEventsActivated()));
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

                final Set<Object> resourceFilters = new HashSet<Object>(filters);
                final PathWithLines pathWithLines = new PathWithLines(line);
                resourceFilters.addAll(pathWithLines.lines);
                builder.parse(new ClassLoaderResource(tccl, pathWithLines.path), new ArrayList<Object>(resourceFilters));
            }
        } else { // client side
            for (final Map.Entry<String, Collection<URL>> entry : Features.createFeatureMap(CucumberConfiguration.instance().getTempDir(), cukespaceConfig.getProperty(CucumberConfiguration.FEATURE_HOME), clazz, tccl).entrySet()) {
                final PathWithLines pathWithLines = new PathWithLines(entry.getKey());
                final Set<Object> resourceFilters = new HashSet<Object>(filters);
                resourceFilters.addAll(pathWithLines.lines);
                for (final URL url : entry.getValue()) {
                    builder.parse(new URLResource(pathWithLines.path, url), new ArrayList<Object>(resourceFilters));
                }
            }
        }

        if (cucumberFeatures.isEmpty()) {
            throw new IllegalArgumentException("No feature found");
        }

        final RuntimeOptions runtimeOptions;
        if (clazz.getAnnotation(CucumberOptions.class) != null) { // by class setting
            final RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(clazz);
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
            runtimeOptions.addPlugin(new JSONFormatter(reportBuilder));
        }

        final Collection<Class<?>> glues = new LinkedList<Class<?>>();
        final InputStream gluesIs = tccl.getResourceAsStream(ClientServerFiles.GLUES_LIST);
        if (gluesIs != null) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(gluesIs));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                glues.add(tccl.loadClass(line));
            }
        } else { // client side
            glues.addAll(Glues.findGlues(clazz));
        }

        final ArquillianBackend arquillianBackend = new ArquillianBackend(glues, clazz, testInstance);
        final cucumber.runtime.Runtime runtime = new cucumber.runtime.Runtime(null, tccl, Arrays.asList(arquillianBackend), runtimeOptions) {
            @Override
            public void runStep(final String featurePath, final Step step, final Reporter reporter, final I18n i18n) {
                super.runStep(featurePath, step, new Reporter() {
                    @Override
                    public void match(final Match match) { // lazy to get the method and instance
                        if (StepDefinitionMatch.class.isInstance(match)) {
                            EventHelper.matched(StepDefinitionMatch.class.cast(match));
                            EventHelper.fire(new BeforeStep(featurePath, step));
                        }
                        reporter.match(match);
                    }

                    @Override
                    public void before(final Match match, final Result result) {
                        reporter.before(match, result);
                    }

                    @Override
                    public void result(final Result result) {
                        reporter.result(result);
                    }

                    @Override
                    public void after(final Match match, final Result result) {
                        reporter.after(match, result);
                    }

                    @Override
                    public void embedding(final String mimeType, final byte[] data) {
                        reporter.embedding(mimeType, data);
                    }

                    @Override
                    public void write(final String text) {
                        reporter.write(text);
                    }
                }, i18n);
                EventHelper.fire(new AfterStep(featurePath, step));
            }

            @Override
            public void runBeforeHooks(final Reporter reporter, final Set<Tag> tags) {
                EventHelper.fire(new BeforeBeforeHooks());
                super.runBeforeHooks(reporter, tags);
                EventHelper.fire(new AfterBeforeHooks());
            }

            @Override
            public void runAfterHooks(final Reporter reporter, final Set<Tag> tags) {
                EventHelper.fire(new BeforeAfterHooks());
                super.runAfterHooks(reporter, tags);
                EventHelper.fire(new AfterAfterHooks());
            }
        };

        final Formatter formatter = runtimeOptions.formatter(tccl);
        final JUnitReporter jUnitReporter = new JUnitReporter(runtimeOptions.reporter(tccl), formatter, runtimeOptions.isStrict());
        for (final CucumberFeature feature : cucumberFeatures) {
            LOGGER.info("Running " + feature.getPath());
            new FeatureRunner(feature, runtime, jUnitReporter).run(runNotifier);
        }

        jUnitReporter.done();
        jUnitReporter.close();
        runtime.printSummary();

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
            final CucumberOptions options = clazz.getAnnotation(CucumberOptions.class);
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
        public String getAbsolutePath() {
            final URL resource = loader.getResource(path);
            if (resource == null) {
                throw new IllegalArgumentException(path + " doesn't exist");
            }
            return resource.toExternalForm();
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
            final String path = getPath();
            return path.substring(0, path.length() - extension.length()).replace('/', '.');
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
        public String getAbsolutePath() {
            return url.toExternalForm();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return url.openStream();
        }

        @Override
        public String getClassName(final String extension) {
            final String path = getPath();
            return path.substring(0, path.length() - extension.length()).replace('/', '.');
        }
    }

    private static class InstanceControlledFrameworkMethod extends FrameworkMethod {
        private final ArquillianCucumber instance;
        private final Class<?> originalClass;
        private RunNotifier notifier;

        public InstanceControlledFrameworkMethod(final ArquillianCucumber runner, final Class<?> originalClass, final Method runCucumber) {
            super(runCucumber);
            this.originalClass = originalClass;
            this.instance = runner;
        }

        @Override
        public Object invokeExplosively(final Object target, final Object... params) throws Throwable {
            instance.____Cucumber_Runner_Not_A_Test(target, notifier == null ? new RunNotifier() : notifier);
            return null;
        }

        public Class<?> getOriginalClass() {
            return originalClass;
        }

        public void setNotifier(final RunNotifier notifier) {
            this.notifier = notifier;
        }
    }
}
