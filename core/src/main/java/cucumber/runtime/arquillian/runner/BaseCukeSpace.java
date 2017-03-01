package cucumber.runtime.arquillian.runner;

import cucumber.api.CucumberOptions;
import cucumber.api.StepDefinitionReporter;
import cucumber.runtime.Backend;
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
import cucumber.runtime.io.ResourceLoader;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public abstract class BaseCukeSpace<CUCUMBER_REPORTER, TEST_NOTIFIER> {
    private static final Logger LOGGER = Logger.getLogger(BaseCukeSpace.class.getName());

    protected abstract Class<?> getTestedClass();

    protected abstract Exception newMultipleFailureException(List<Throwable> errors);

    protected abstract void runFeature(CucumberFeature feature, CucumberRuntime cucumberRuntime, CUCUMBER_REPORTER reporter, TEST_NOTIFIER runNotifier) throws Throwable;

    protected abstract CUCUMBER_REPORTER getReporter(Reporter reporter, Formatter formatter, RuntimeOptions runtimeOptions);

    // the cucumber test method, only used internally - see childrenInvoker, public to avoid to setAccessible(true)
    public void performInternalCucumberOperations(final Object testInstance, final TEST_NOTIFIER runNotifier) throws Exception {

        final Class<?> javaTestClass = getTestedClass();
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        final InputStream configurationInputStream = classLoader.getResourceAsStream(ClientServerFiles.CONFIG);
        final Properties cukespaceConfigurationProperties = loadCucumberConfigurationProperties(configurationInputStream);

        final RuntimeOptions runtimeOptions = loadRuntimeOptions(javaTestClass, cukespaceConfigurationProperties);

        final boolean reported = Boolean.parseBoolean(cukespaceConfigurationProperties.getProperty(CucumberConfiguration.REPORTABLE, "false"));
        final StringBuilder reportBuilder = new StringBuilder();
        if (reported) {
            runtimeOptions.addPlugin(new JSONFormatter(reportBuilder));
        }

        final InputStream gluesInputStream = classLoader.getResourceAsStream(ClientServerFiles.GLUES_LIST);
        final Collection<Class<?>> glues = loadGlues(gluesInputStream, classLoader, javaTestClass);


        final ArquillianBackend arquillianBackend = new ArquillianBackend(glues, javaTestClass, testInstance);
        final CucumberRuntime cucumberRuntime = new CucumberRuntime(null, classLoader, singletonList(arquillianBackend), runtimeOptions);

        final Map<String, Collection<URL>> featuresMap = Features.createFeatureMap(CucumberConfiguration.instance().getTempDir(), cukespaceConfigurationProperties.getProperty(CucumberConfiguration.FEATURE_HOME), javaTestClass, classLoader);
        final List<CucumberFeature> cucumberFeatures = getCucumberFeatures(testInstance, classLoader, featuresMap);

        final Formatter formatter = runtimeOptions.formatter(classLoader);
        final Reporter reporter = runtimeOptions.reporter(classLoader);
        final StepDefinitionReporter stepDefinitionReporter = runtimeOptions.stepDefinitionReporter(classLoader);
        cucumberRuntime.getGlue().reportStepDefinitions(stepDefinitionReporter);
        runFeatures(cucumberFeatures, cucumberRuntime, getReporter(reporter, formatter, runtimeOptions), runNotifier);

        if (reported) {
            final String path = cukespaceConfigurationProperties.getProperty(CucumberConfiguration.REPORTABLE_PATH);
            addReportTestIntoFile(path, javaTestClass, reportBuilder);
        }

        handleCucumberTestErrors(cucumberRuntime.getErrors(), cucumberRuntime);

    }

    private static Properties loadCucumberConfigurationProperties(final InputStream configurationInputStream) throws Exception {
        if (configurationInputStream != null) {
            return loadConfigurationPropertiesFromStream(configurationInputStream);
        } else {
            return loadConfigurationPropertiesFromObject(CucumberConfiguration.instance());
        }
    }

    private static Properties loadConfigurationPropertiesFromStream(final InputStream configurationInputStream) throws Exception {
        Properties configurationProperties = new Properties();
        configurationProperties.load(configurationInputStream);
        return configurationProperties;
    }

    private static Properties loadConfigurationPropertiesFromObject(final CucumberConfiguration cucumberConfiguration) throws Exception {
        return cucumberConfiguration.getConfigurationAsProperties();
    }

    private static List<CucumberFeature> getCucumberFeatures(final Object testInstance, final ClassLoader classLoader, final Map<String, Collection<URL>> featuresMap) throws Exception {
        final List<Object> testFilters = new ArrayList<Object>(createFilters(testInstance));
        final InputStream featuresInputStream = classLoader.getResourceAsStream(ClientServerFiles.FEATURES_LIST);
        return buildFeatureList(testFilters, featuresInputStream, classLoader, featuresMap);
    }

    private static List<Object> createFilters(final Object testInstance) {
        final List<Object> filters = new ArrayList<Object>();

        final Class<?> testInstanceClass = testInstance.getClass();

        { // our API
            final Tags tags = testInstanceClass.getAnnotation(Tags.class);
            if (tags != null) {
                filters.addAll(singletonList(tags.value()));
            }
        }

        { // cucumber-api
            final CucumberOptions options = testInstanceClass.getAnnotation(CucumberOptions.class);
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

    private static List<CucumberFeature> buildFeatureList(final List<Object> testFilters, final InputStream featuresInputStream,
                                                          final ClassLoader classLoader, final Map<String, Collection<URL>> featuresMap) throws Exception {
        final List<CucumberFeature> cucumberFeatures = new ArrayList<CucumberFeature>();
        final FeatureBuilder featureBuilder = new FeatureBuilder(cucumberFeatures);

        if (featuresInputStream != null) {

            buildFeatureListFromFile(featuresInputStream, testFilters, featureBuilder, classLoader);
        } else {
            buildFeatureListFromMap(featuresMap, testFilters, featureBuilder);
        }

        featureBuilder.close();

        if (cucumberFeatures.isEmpty()) {
            if (featuresMap.values().isEmpty()) {
                System.out.println("Got no path to feature directory or feature file");
            } else if (testFilters.isEmpty()) {
                System.out.println(String.format("No features found at %s", featuresMap.values()));
            } else {
                System.out.println(String.format("None of the features at %s matched the filters: %s", featuresMap.values(), testFilters));
            }
        }

        Collections.sort(cucumberFeatures, new Comparator<CucumberFeature>() {
            @Override
            public int compare(final CucumberFeature o1, final CucumberFeature o2) {
                return o1.getPath().compareTo(o2.getPath());
            }
        });
        return cucumberFeatures;
    }

    private static void buildFeatureListFromFile(final InputStream featuresInputStream, final List<Object> testFilters,
                                                 final FeatureBuilder featureBuilder, final ClassLoader classLoader) throws Exception {
        final BufferedReader featuresFileReader = new BufferedReader(new InputStreamReader(featuresInputStream));

        String readerLine;

        while ((readerLine = featuresFileReader.readLine()) != null) {
            readerLine = readerLine.trim();
            if (readerLine.isEmpty()) {
                continue;
            }

            final PathWithLines pathWithLines = new PathWithLines(readerLine);
            testFilters.addAll(pathWithLines.lines);
            featureBuilder.parse(new ClassLoaderResource(classLoader, pathWithLines.path), new ArrayList<Object>(testFilters));
        }

        featuresFileReader.close();
    }

    private static void buildFeatureListFromMap(final Map<String, Collection<URL>> featuresMap, final List<Object> testFilters,
                                                final FeatureBuilder featureBuilder) {
        for (final Map.Entry<String, Collection<URL>> entry : featuresMap.entrySet()) {
            final PathWithLines pathWithLines = new PathWithLines(entry.getKey());
            testFilters.addAll(pathWithLines.lines);
            for (final URL url : entry.getValue()) {
                featureBuilder.parse(new URLResource(pathWithLines.path, url), new ArrayList<Object>(testFilters));
            }
        }
    }

    private static RuntimeOptions loadRuntimeOptions(final Class<?> javaTestClass, final Properties cukespaceConfigurationProperties) {
        final RuntimeOptions runtimeOptions;
        if (javaTestClass.getAnnotation(CucumberOptions.class) != null) { // by class setting
            final RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(javaTestClass);
            runtimeOptions = runtimeOptionsFactory.create();
            cleanClasspathList(runtimeOptions.getGlue());
            cleanClasspathList(runtimeOptions.getFeaturePaths());
        } else if (cukespaceConfigurationProperties.containsKey(CucumberConfiguration.OPTIONS)) { // arquillian setting
            runtimeOptions = new RuntimeOptions(new Env("cucumber-jvm"), asList((cukespaceConfigurationProperties.getProperty(CucumberConfiguration.OPTIONS, "--strict") + " --strict").split(" ")));
        } else { // default
            runtimeOptions = new RuntimeOptions(new Env("cucumber-jvm"), asList("--strict", "--plugin", "pretty", areColorsNotAvailable(cukespaceConfigurationProperties)));
        }
        return runtimeOptions;
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

    private static Collection<Class<?>> loadGlues(final InputStream gluesInputStream, final ClassLoader classLoader, final Class<?> javaTestClass) throws Exception {
        final Collection<Class<?>> glues = new LinkedList<Class<?>>();

        if (gluesInputStream != null) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(gluesInputStream));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                glues.add(classLoader.loadClass(line));
            }
            reader.close();

        } else { // client side
            glues.addAll(Glues.findGlues(javaTestClass));
        }

        return glues;
    }

    private void runFeatures(final List<CucumberFeature> cucumberFeatures, final CucumberRuntime cucumberRuntime, final CUCUMBER_REPORTER reporter, final TEST_NOTIFIER runNotifier) throws Exception {
        for (final CucumberFeature feature : cucumberFeatures) {
            LOGGER.info("Running " + feature.getPath());
            try {
                runFeature(feature, cucumberRuntime, reporter, runNotifier);
            } catch (final Exception exception) {
                throw exception;
            } catch (final Error error) {
                throw error;
            } catch (final Throwable throwable) {
                throw new IllegalStateException(throwable);
            }
        }
        if (Formatter.class.isInstance(reporter)) {
            final Formatter f = Formatter.class.cast(reporter);
            f.done();
            f.close();
        }
        cucumberRuntime.printSummary();
    }

    private static void addReportTestIntoFile(final String path, final Class<?> javaTestClass, final StringBuilder reportBuilder) throws Exception {
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
                // no-op
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }

            // add it here too for client case
            CucumberReporter.addReport(CucumberConfiguration.reportFile(path, javaTestClass));
        }
    }

    public void handleCucumberTestErrors(final List<Throwable> errors, CucumberRuntime cucumberRuntime) throws Exception {
        for (final String snippet : cucumberRuntime.getSnippets()) {
            errors.add(new CucumberException("Missing snippet: " + snippet));
        }
        if (!errors.isEmpty()) {
            throw newMultipleFailureException(errors);
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

    public static class CucumberRuntime extends cucumber.runtime.Runtime {
        public CucumberRuntime(ResourceLoader resourceLoader,
                               ClassLoader classLoader,
                               Collection<? extends Backend> backends,
                               RuntimeOptions runtimeOptions) {
            super(resourceLoader, classLoader, backends, runtimeOptions);
        }

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
                    if (StepDefinitionMatch.class.isInstance(match)) {
                        EventHelper.fire(new AfterStep(featurePath, step));
                        EventHelper.unmatch();
                    }
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
    }
}
