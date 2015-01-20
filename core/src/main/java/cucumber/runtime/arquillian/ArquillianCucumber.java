package cucumber.runtime.arquillian;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
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

import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang.NullArgumentException;
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
import java.util.Map.Entry;
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
        final Properties cukespaceConfigurationProperties = loadCucumberConfigurationProperties(configurationInputStream);
        
        final HashSet<Object> testFilters = new HashSet<Object>(createFilters(testInstance));
        final InputStream featuresInputStream = threadContextClassLoader.getResourceAsStream(ClientServerFiles.FEATURES_LIST);
        final Set<Entry<String, Collection<URL>>> featuresSet = Features.createFeatureMap(CucumberConfiguration.instance().getTempDir(),cukespaceConfigurationProperties.getProperty(CucumberConfiguration.FEATURE_HOME),javaTestClass,threadContextClassLoader).entrySet();
        final List<CucumberFeature> cucumberFeatures = retrieveCucumberFeatures(testFilters,featuresInputStream,threadContextClassLoader, featuresSet);
        
        final RuntimeOptions runtimeOptions = loadRuntimeOptions(javaTestClass, cukespaceConfigurationProperties);        

        final boolean reported = Boolean.parseBoolean(cukespaceConfigurationProperties.getProperty(CucumberConfiguration.REPORTABLE, "false"));
        final StringBuilder reportBuilder = new StringBuilder();
        if (reported) {
            runtimeOptions.addFormatter(new JSONFormatter(reportBuilder));
        }

        final InputStream gluesInputStream = threadContextClassLoader.getResourceAsStream(ClientServerFiles.GLUES_LIST);
        final Collection<Class<?>> glues = loadGlues(gluesInputStream, threadContextClassLoader, javaTestClass);
        
        final CucumberRuntime cucumberRuntime = new CucumberRuntime(null, threadContextClassLoader, Arrays.asList(new ArquillianBackend(glues, javaTestClass, testInstance)), runtimeOptions);
           
        final Formatter formatter = runtimeOptions.formatter(threadContextClassLoader);
        final JUnitReporter jUnitReporter = new JUnitReporter(runtimeOptions.reporter(threadContextClassLoader), formatter, runtimeOptions.isStrict());
        
        runFeatures(cucumberFeatures, cucumberRuntime, jUnitReporter, runNotifier);
        

        if (reported) 
        {
            final String path = cukespaceConfigurationProperties.getProperty(CucumberConfiguration.REPORTABLE_PATH);
            addReportTestIntoFile(path, javaTestClass, reportBuilder);
        }

        handleCucumberTestErrors(cucumberRuntime.getErrors(), cucumberRuntime);
        
    }
    
    private static Properties loadCucumberConfigurationProperties(final InputStream configurationInputStream) throws Exception
    {
    	if (configurationInputStream != null) {
    		return loadConfigurationPropertiesFromStream(configurationInputStream);
        }
    	else
    	{
    		return loadConfigurationPropertiesFromObject(CucumberConfiguration.instance());
    	}
    }
    
    private static Properties loadConfigurationPropertiesFromStream(final InputStream configurationInputStream) throws Exception
    {
    	Properties configurationProperties = new Properties();
        configurationProperties.load(configurationInputStream);
        return configurationProperties;
    }
    
    private static Properties loadConfigurationPropertiesFromObject(final CucumberConfiguration cucumberConfiguration) throws Exception
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

        final Class<?> testInstanceClass = testInstance.getClass();

        { // our API
            final Tags tags = testInstanceClass.getAnnotation(Tags.class);
            if (tags != null) {
                filters.addAll(Arrays.asList(tags.value()));
            }
        }

        { // cucumber-junit
            final Cucumber.Options options = testInstanceClass.getAnnotation(Cucumber.Options.class);
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
    
    private static List<CucumberFeature> retrieveCucumberFeatures(final Set<Object> testFilters, final InputStream featuresInputStream, final ClassLoader classLoader, final Set<Entry<String, Collection<URL>>> featuresSet) throws Exception
    {
    	final List<CucumberFeature> cucumberFeatures = new ArrayList<CucumberFeature>();
        final FeatureBuilder featureBuilder = new FeatureBuilder(cucumberFeatures);
        
        if (featuresInputStream != null) {
            final BufferedReader featureFileReader = new BufferedReader(new InputStreamReader(featuresInputStream));
            buildFeatureListFromFile(featureFileReader, testFilters, featureBuilder, classLoader);            
        }
        else 
        {
        	buildFeatureListFromMap(featuresSet, testFilters, featureBuilder);
        }
        
        featureBuilder.close(); 
        
        if (cucumberFeatures.isEmpty()) {
            throw new IllegalArgumentException("No feature found");
        }
        
        return cucumberFeatures;
    }
    
    private static void buildFeatureListFromFile(final BufferedReader featureFileReader, final Set<Object> testFilters, final FeatureBuilder featureBuilder, final ClassLoader classLoader) throws Exception
    {
    	String readerLine;

        while ((readerLine = featureFileReader.readLine()) != null) {
            readerLine = readerLine.trim();
            if (readerLine.isEmpty()) {
                continue;
            }

            final PathWithLines pathWithLines = new PathWithLines(readerLine);
            testFilters.addAll(pathWithLines.lines);
            featureBuilder.parse(new ClassLoaderResource(classLoader, pathWithLines.path), new ArrayList<Object>(testFilters));
        }
    }
    
    private static void buildFeatureListFromMap(final Set<Entry<String, Collection<URL>>> featuresSet, final Set<Object> testFilters,  final FeatureBuilder featureBuilder)
    {
    	for (final Map.Entry<String, Collection<URL>> entry : featuresSet)
    	{
    		final PathWithLines pathWithLines = new PathWithLines(entry.getKey());
    		testFilters.addAll(pathWithLines.lines);
            for (final URL url : entry.getValue()) {
                featureBuilder.parse(new URLResource(pathWithLines.path, url), new ArrayList<Object>(testFilters));
            }
    	}
    }
    
    private static RuntimeOptions loadRuntimeOptions(final Class<?> javaTestClass, final Properties cukespaceConfigurationProperties)
    {
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
    
    private static Collection<Class<?>> loadGlues(final InputStream gluesInputStream, final ClassLoader classLoader, final Class<?> javaTestClass) throws Exception
    {
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
        } else { // client side
            glues.addAll(Glues.findGlues(javaTestClass));
        }
    	
    	return glues;
    }
    
    private static void runFeatures(final List<CucumberFeature> cucumberFeatures, final CucumberRuntime cucumberRuntime, final JUnitReporter jUnitReporter,final RunNotifier runNotifier) throws Exception
    {
    	for (final CucumberFeature feature : cucumberFeatures) {
            LOGGER.info("Running " + feature.getPath());
            new FeatureRunner(feature, cucumberRuntime, jUnitReporter).run(runNotifier);
        }
    	
    	jUnitReporter.done();
        jUnitReporter.close();
        cucumberRuntime.printSummary();
    }
    
    private static void addReportTestIntoFile(final String path, final Class<?> javaTestClass, final StringBuilder reportBuilder) throws Exception
    {
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
    
    public void handleCucumberTestErrors(final List<Throwable> errors, CucumberRuntime cucumberRuntime) throws Exception
    {
    	for (final String snippet : cucumberRuntime.getSnippets()) {
            errors.add(new CucumberException("Missing snippet: " + snippet));
        }
        if (!errors.isEmpty()) {
            throw new MultipleFailureException(errors);
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
    
    private class CucumberRuntime extends cucumber.runtime.Runtime
    {
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
    }
}
