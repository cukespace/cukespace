package cucumber.runtime.arquillian;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
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
import java.lang.annotation.Annotation;
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

    private static final String RUN_CUCUMBER_MTD = "runCucumber";
    private static final Class<? extends Annotation>[] OPTIONS_ANNOTATIONS = new Class[]{CucumberOptions.class, Cucumber.Options.class};

    private List<FrameworkMethod> methods;

    public ArquillianCucumber(final Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected Description describeChild(final FrameworkMethod method)
    {
        if (!Boolean.getBoolean("cukespace.runner.standard-describe")
                && InstanceControlledFrameworkMethod.class.isInstance(method)) {
            return Description.createTestDescription(
                    InstanceControlledFrameworkMethod.class.cast(method).getOriginalClass(),
                    ArquillianCucumber.RUN_CUCUMBER_MTD,
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
    public void runCucumber(final Object testInstance, final RunNotifier runNotifier) throws Exception {
        final Class<?> javaTestClass = getTestClass().getJavaClass();
        final ClassLoader threadContextClassLoader = Thread.currentThread().getContextClassLoader();

        final InputStream configurationInputStream = threadContextClassLoader.getResourceAsStream(ClientServerFiles.CONFIG);
        
        final Properties cukespaceConfigurationProperties = loadProperties(configurationInputStream);        

        final List<CucumberFeature> cucumberFeatures = new ArrayList<CucumberFeature>();
        final FeatureBuilder featureBuilder = new FeatureBuilder(cucumberFeatures);

        final List<Object> filters = createFilters(testInstance);

        final InputStream featuresInputStream = threadContextClassLoader.getResourceAsStream(ClientServerFiles.FEATURES_LIST);
        if (featuresInputStream != null) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(featuresInputStream));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                final Set<Object> resourceFilters = new HashSet<Object>(filters);
                final PathWithLines pathWithLines = new PathWithLines(line);
                resourceFilters.addAll(pathWithLines.lines);
                featureBuilder.parse(new ClassLoaderResource(threadContextClassLoader, pathWithLines.path), new ArrayList<Object>(resourceFilters));
            }
        } else { // client side
            for (final Map.Entry<String, Collection<URL>> entry : Features.createFeatureMap(CucumberConfiguration.instance().getTempDir(), cukespaceConfigurationProperties.getProperty(CucumberConfiguration.FEATURE_HOME), javaTestClass, threadContextClassLoader).entrySet()) {
                final PathWithLines pathWithLines = new PathWithLines(entry.getKey());
                final Set<Object> resourceFilters = new HashSet<Object>(filters);
                resourceFilters.addAll(pathWithLines.lines);
                for (final URL url : entry.getValue()) {
                    featureBuilder.parse(new URLResource(pathWithLines.path, url), new ArrayList<Object>(resourceFilters));
                }
            }
        }

        if (cucumberFeatures.isEmpty()) {
            throw new IllegalArgumentException("No feature found");
        }

        final RuntimeOptions runtimeOptions;
        if (javaTestClass.getAnnotation(Cucumber.Options.class) != null || javaTestClass.getAnnotation(CucumberOptions.class) != null) { // by class setting
            final RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(javaTestClass, OPTIONS_ANNOTATIONS);
            runtimeOptions = runtimeOptionsFactory.create();
            cleanClasspathList(runtimeOptions.getGlue());
            cleanClasspathList(runtimeOptions.getFeaturePaths());
        } else if (cukespaceConfigurationProperties.containsKey(CucumberConfiguration.OPTIONS)) { // arquillian setting
            runtimeOptions = new RuntimeOptions(new Env("cucumber-jvm"), asList((cukespaceConfigurationProperties.getProperty(CucumberConfiguration.OPTIONS, "--strict") + " --strict").split(" ")));
        } else { // default
            runtimeOptions = new RuntimeOptions(new Env("cucumber-jvm"), asList("--strict", "-f", "pretty", areColorsNotAvailable(cukespaceConfigurationProperties)));
        }

        final boolean reported = Boolean.parseBoolean(cukespaceConfigurationProperties.getProperty(CucumberConfiguration.REPORTABLE, "false"));
        final StringBuilder reportBuilder = new StringBuilder();
        if (reported) {
            runtimeOptions.addFormatter(new JSONFormatter(reportBuilder));
        }

        final Collection<Class<?>> glues = new LinkedList<Class<?>>();
        final InputStream gluesIs = threadContextClassLoader.getResourceAsStream(ClientServerFiles.GLUES_LIST);
        if (gluesIs != null) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(gluesIs));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                glues.add(threadContextClassLoader.loadClass(line));
            }
        } else { // client side
            glues.addAll(Glues.findGlues(javaTestClass));
        }

        final cucumber.runtime.Runtime runtime = new cucumber.runtime.Runtime(null, threadContextClassLoader, Arrays.asList(new ArquillianBackend(glues, javaTestClass, testInstance)), runtimeOptions) {
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

        final Formatter formatter = runtimeOptions.formatter(threadContextClassLoader);
        final JUnitReporter jUnitReporter = new JUnitReporter(runtimeOptions.reporter(threadContextClassLoader), formatter, runtimeOptions.isStrict());
        for (final CucumberFeature feature : cucumberFeatures) {
            LOGGER.info("Running " + feature.getPath());
            new FeatureRunner(feature, runtime, jUnitReporter).run(runNotifier);
        }

        jUnitReporter.done();
        jUnitReporter.close();
        runtime.printSummary();

        if (reported) {
            final String path = cukespaceConfigurationProperties.getProperty(CucumberConfiguration.REPORTABLE_PATH);
            if (path != null) {
                final File destination = CucumberConfiguration.reportFile(path, javaTestClass);
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
                CucumberReporter.addReport(CucumberConfiguration.reportFile(path, javaTestClass));
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
    
    private Properties loadProperties(final InputStream configurationInputStream) throws Exception
    {
    	Properties configurationProperties = null;
    	
    	if (configurationInputStream != null) {
    		configurationProperties = new Properties();
            configurationProperties.load(configurationInputStream);
            return configurationProperties;
        }
    	else
    	{
    		return loadProperties(CucumberConfiguration.instance());
    	}
    }
    
    private Properties loadProperties(final CucumberConfiguration cucumberConfiguration) throws Exception
    {
    	Properties configurationProperties = new Properties();
    	
    	if (cucumberConfiguration.isInitialized()) {
            configurationProperties.setProperty(CucumberConfiguration.PERSISTENCE_EVENTS, Boolean.toString(cucumberConfiguration.arePersistenceEventsActivated()));
            configurationProperties.setProperty(CucumberConfiguration.COLORS, Boolean.toString(cucumberConfiguration.isColorized()));
            configurationProperties.setProperty(CucumberConfiguration.REPORTABLE, Boolean.toString(cucumberConfiguration.isReport()));
            configurationProperties.setProperty(CucumberConfiguration.REPORTABLE_PATH, cucumberConfiguration.getReportDirectory());
            if (cucumberConfiguration.getFeatureHome() != null) {
                configurationProperties.setProperty(CucumberConfiguration.FEATURE_HOME, cucumberConfiguration.getFeatureHome());
            }
            if (cucumberConfiguration.hasOptions()) {
                configurationProperties.setProperty(CucumberConfiguration.OPTIONS, cucumberConfiguration.getOptions());
            }
            if (cucumberConfiguration.getFeatureHome() != null) {
                configurationProperties.setProperty(CucumberConfiguration.FEATURE_HOME, cucumberConfiguration.getFeatureHome());
            }
        }
    	
    	return configurationProperties;
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
            instance.runCucumber(target, notifier == null ? new RunNotifier() : notifier);
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
