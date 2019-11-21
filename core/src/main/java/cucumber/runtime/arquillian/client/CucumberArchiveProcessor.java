package cucumber.runtime.arquillian.client;

import cucumber.deps.com.thoughtworks.xstream.converters.ConverterRegistry;
import cucumber.runtime.arquillian.ArquillianCucumber;
import cucumber.runtime.arquillian.CukeSpace;
import cucumber.runtime.arquillian.api.event.StepEvent;
import cucumber.runtime.arquillian.backend.ArquillianBackend;
import cucumber.runtime.arquillian.config.CucumberConfiguration;
import cucumber.runtime.arquillian.container.ContextualObjectFactoryBase;
import cucumber.runtime.arquillian.container.CucumberContainerExtension;
import cucumber.runtime.arquillian.container.CukeSpaceCDIObjectFactory;
import cucumber.runtime.arquillian.feature.Features;
import cucumber.runtime.arquillian.glue.Glues;
import cucumber.runtime.arquillian.lifecycle.CucumberLifecycle;
import cucumber.runtime.arquillian.reporter.CucumberReporter;
import cucumber.runtime.arquillian.runner.BaseCukeSpace;
import cucumber.runtime.arquillian.shared.ClientServerFiles;
import cucumber.runtime.arquillian.stream.NotCloseablePrintStream;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.java.JavaBackend;
import gherkin.util.Mapper;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.ArchiveAsset;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.impl.base.asset.AssetUtil;
import org.jboss.shrinkwrap.impl.base.filter.IncludeRegExpPaths;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import static cucumber.runtime.arquillian.locator.JarLocation.jarLocation;
import static cucumber.runtime.arquillian.shared.ClassLoaders.load;
import static cucumber.runtime.arquillian.shared.IOs.slurp;
import static java.util.Arrays.asList;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

public class CucumberArchiveProcessor implements ApplicationArchiveProcessor {
    private static volatile StringAsset scannedAnnotations = null;

    @Inject
    private Instance<CucumberConfiguration> configuration;

    @Override
    public void process(final Archive<?> applicationArchive, final TestClass testClass) {
        if (JavaArchive.class.isInstance(applicationArchive)) {
            return;
        }

        // try to find the feature
        final Class<?> javaClass = testClass.getJavaClass();
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final Map<String, Collection<URL>> featureUrls = Features.createFeatureMap(
                configuration.get().getTempDir(), configuration.get().getFeatureHome(), javaClass, loader);

        Class testNgBase = null;
        try {
            testNgBase = loader.loadClass("cucumber.runtime.arquillian.testng.CukeSpace");
        } catch (final ClassNotFoundException e) {
            // no-op
        } catch (final NoClassDefFoundError e) {
            // no-op
        }
        final boolean junit = testNgBase == null || !testNgBase.isAssignableFrom(javaClass);
        if (featureUrls.isEmpty()
                || !LibraryContainer.class.isInstance(applicationArchive)) {
            if (junit) {
                Class<? extends Annotation> runWithType = null;
                try {
                    runWithType = (Class<? extends Annotation>) loader.loadClass("org.junit.runner.RunWith");

                } catch (final ClassNotFoundException error) {
                    // no-op
                } catch (final NoClassDefFoundError e) {
                    // no-op
                }

                if (runWithType == null) {
                    return;
                }
                final Annotation runWith = testClass.getAnnotation(runWithType);
                if (runWith == null) {
                    return;
                }

                try {
                    final Class<?> runner = Class.class.cast(runWithType.getMethod("value").invoke(runWith));
                    if ((!ArquillianCucumber.class.equals(runner) && !CukeSpace.class.equals(runner))) {
                        // not a cucumber test so skip enrichment
                        return;
                    } else {
                        // else let enrich it to avoid type not found error
                        Logger.getLogger(CucumberArchiveProcessor.class.getName()).info("No feature found for " + javaClass.getName());
                    }
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalStateException(e.getCause());
                } catch (final NoSuchMethodException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }

        final String ln = System.getProperty("line.separator");

        final LibraryContainer<?> libraryContainer = (LibraryContainer<?>) applicationArchive;

        // add feature file + list of annotations
        final JavaArchive resourceJar = create(JavaArchive.class, "cukespace-resources.jar");

        final CucumberConfiguration cucumberConfiguration = configuration.get();
        final boolean report = cucumberConfiguration.isReport() || cucumberConfiguration.isGenerateDocs();
        final String reportDirectory = cucumberConfiguration.getReportDirectory();

        addFeatures(featureUrls, ln, resourceJar);
        addCucumberAnnotations(ln, resourceJar);
        addConfiguration(resourceJar, cucumberConfiguration, report, reportDirectory);

        libraryContainer.addAsLibrary(resourceJar);

        if (report) {
            CucumberReporter.addReport(CucumberConfiguration.reportFile(reportDirectory, javaClass));
        }

        // cucumber-java and cucumber-core
        enrichWithDefaultCucumber(libraryContainer);

        LibraryContainer<?> entryPointContainer = (LibraryContainer<?>)findArchiveByTestClass(applicationArchive, testClass.getJavaClass());

        // glues
        enrichWithGlues(javaClass, entryPointContainer, ln);

        // cucumber-arquillian
        enrichWithCukeSpace(entryPointContainer, junit);

        // if scala module is available at classpath
        final Set<ArchivePath> libs = applicationArchive.getContent(new IncludeRegExpPaths("/WEB-INF/lib/.*jar")).keySet();
        tryToAdd(libs, libraryContainer, "WEB-INF/lib/scala-library-", "cucumber.api.scala.ScalaDsl", "scala.App");
    }

    protected final Archive<? extends Archive<?>> findArchiveByTestClass(Archive<?> topArchive, Class testClass) {
        Archive<?> testArchive = topArchive;

        if (!archiveContains(testArchive, testClass)) {
            for (Node node : testArchive.getContent(Filters.include(".*\\.(jar|war)")).values()) {
                if (node.getAsset() instanceof ArchiveAsset) {
                    Archive archive = ((ArchiveAsset) node.getAsset()).getArchive();

                    if (archiveContains(archive, testClass) && archive instanceof LibraryContainer) {
                        testArchive = archive;
                    }
                }
            }
        }

        return testArchive;
    }

    private boolean archiveContains(Archive<?> archive, Class<?> clazz) {
        String classPath = clazz.getCanonicalName().replace('.', '/') + ".class";
        String warClassPath = "WEB-INF/classes/" + classPath;

        return archive.contains(classPath) || archive.contains(warClassPath);
    }

    private static void addConfiguration(final JavaArchive resourceJar, final CucumberConfiguration cucumberConfiguration, final boolean report, final String reportDirectory) {
        final Properties config = new Properties();
        config.setProperty(CucumberConfiguration.COLORS, String.valueOf(cucumberConfiguration.isColorized()));
        config.setProperty(CucumberConfiguration.REPORTABLE, String.valueOf(report));
        config.setProperty(CucumberConfiguration.REPORTABLE_PATH, reportDirectory == null ? null : new File(reportDirectory).getAbsolutePath());

        if (cucumberConfiguration.getObjectFactory() != null) {
        	config.setProperty(CucumberConfiguration.OBJECT_FACTORY, cucumberConfiguration.getObjectFactory());
        }
        if (cucumberConfiguration.hasOptions()) {
        	config.setProperty(CucumberConfiguration.OPTIONS, cucumberConfiguration.getOptions());
        }

        resourceJar.addAsResource(new StringAsset(config.toString()), ClientServerFiles.CONFIG);
    }

    private static void addCucumberAnnotations(final String ln, final JavaArchive resourceJar) {
        if (scannedAnnotations == null) {
            synchronized (CucumberArchiveProcessor.class) {
                if (scannedAnnotations == null) {
                    final StringBuilder builder = new StringBuilder();
                    for (final Class<? extends Annotation> annotation : CucumberLifecycle.cucumberAnnotations()) {
                        builder.append(annotation.getName()).append(ln);
                    }
                    scannedAnnotations = new StringAsset(builder.toString());
                }
            }
        }
        resourceJar.addAsResource(scannedAnnotations, ClientServerFiles.ANNOTATION_LIST);
    }

    private static void addFeatures(final Map<String, Collection<URL>> featureUrls, final String ln, final JavaArchive resourceJar) {
        final StringBuilder featuresPaths = new StringBuilder();

        for (final Map.Entry<String, Collection<URL>> feature : featureUrls.entrySet()) {
            final Collection<URL> features = feature.getValue();
            final int size = features.size();
            if (size == 0) {
                continue;
            }

            final String key = feature.getKey();

            if (size == 1) {
                final Asset featureAsset = new StringAsset(new String(slurp(features.iterator().next())));
                resourceJar.addAsResource(featureAsset, key);
                featuresPaths.append(key).append(ln);
            } else {
                for (final URL url : features) {
                    final Asset featureAsset = new StringAsset(new String(slurp(url)));
                    final String target = key + featureName(url);
                    resourceJar.addAsResource(featureAsset, target);
                    featuresPaths.append(target).append(ln);
                }
            }
        }

        resourceJar.addAsResource(new StringAsset(featuresPaths.toString()), ClientServerFiles.FEATURES_LIST);
    }

    private static void enrichWithDefaultCucumber(final LibraryContainer<?> libraryContainer) {
        libraryContainer.addAsLibraries(
                jarLocation(Mapper.class),
                jarLocation(ResourceLoaderClassFinder.class),
                jarLocation(ConverterRegistry.class),
                jarLocation(JavaBackend.class));
        for (final String potential : asList(
                "cucumber.api.junit.Cucumber",
                "cucumber.api.testng.TestNGCucumberRunner",
                "cucumber.runtime.java8.LambdaGlueBase")) {
            try {
                libraryContainer.addAsLibraries(jarLocation(Thread.currentThread().getContextClassLoader().loadClass(potential)));
            } catch (final Throwable e) {
                // no-op
            }
        }
    }

    private static void enrichWithGlues(final Class<?> javaClass, final LibraryContainer<?> libraryContainer, final String ln) {
        final Collection<Class<?>> glues = Glues.findGlues(javaClass);
        final StringBuilder gluesStr = new StringBuilder();
        if (!glues.isEmpty()) {
            final JavaArchive gluesJar = create(JavaArchive.class, "cukespace-glues.jar");
            { // glues txt file
                for (final Class<?> g : glues) {
                    gluesStr.append(g.getName()).append(ln);
                }
                gluesJar.add(new StringAsset(gluesStr.toString()), ClientServerFiles.GLUES_LIST);
            }

            { // classes
                gluesJar.addClasses(glues.toArray(new Class<?>[glues.size()]));
                for (final Class<?> clazz : glues) {
                    Class<?> current = clazz.getSuperclass();
                    while (!Object.class.equals(current)) {
                        if (!gluesJar.contains(AssetUtil.getFullPathForClassResource(current))) {
                            gluesJar.addClass(current);
                        }
                        current = current.getSuperclass();
                    }
                }
            }

            libraryContainer.addAsLibrary(gluesJar);
        }
    }

    private static void enrichWithCukeSpace(final LibraryContainer<?> libraryContainer, final boolean junit) {
        final JavaArchive archive = create(JavaArchive.class, "cukespace-core.jar")
                .addAsServiceProvider(RemoteLoadableExtension.class, CucumberContainerExtension.class)
                .addPackage(ArquillianBackend.class.getPackage())
                .addPackage(cucumber.runtime.arquillian.api.Glues.class.getPackage())
                .addPackage(StepEvent.class.getPackage())
                .addClasses(NotCloseablePrintStream.class, CucumberReporter.class, CucumberLifecycle.class, BaseCukeSpace.class)
                .addClasses(
                        CucumberConfiguration.class, CucumberContainerExtension.class,
                        Features.class, Glues.class,
                        ContextualObjectFactoryBase.class)
                .addPackage(ClientServerFiles.class.getPackage());

        if (junit) {
            archive.addClasses(ArquillianCucumber.class, CukeSpace.class, ArquillianCucumber.InstanceControlledFrameworkMethod.class);
        } else {
            archive.addClasses(
                    cucumber.runtime.arquillian.testng.CukeSpace.class,
                    cucumber.runtime.arquillian.testng.CukeSpace.TestNGCukeSpace.class,
                    cucumber.runtime.arquillian.testng.CukeSpace.FormaterReporterFacade.class);
        }
        libraryContainer.addAsLibrary(archive);
    }

    private static void tryToAdd(final Collection<ArchivePath> paths, final LibraryContainer<?> container, final String exclusion, final String... classes) {
        final Collection<File> files = new ArrayList<File>();

        try { // if scala dsl is here, add it
            for (final String clazz : classes) {
                final File file = jarLocation(load(clazz));

                boolean found = false;
                for (final ArchivePath ap : paths) {
                    final String path = ap.get();
                    if (path.contains(exclusion) && path.endsWith(".jar")) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    files.add(file);
                }
            }
        } catch (final Exception e) {
            return; // if any jar is missing don't add it
        }

        container.addAsLibraries(files.toArray(new File[files.size()]));
    }

    private static String featureName(final URL url) {
        // file
        final File f = new File(url.getFile());
        if (f.exists()) {
            return f.getName();
        }

        // classpath
        final String path = url.getPath();
        if (path.lastIndexOf("!") < path.lastIndexOf("/")) {
            return path.substring(path.lastIndexOf("/") + 1);
        }

        // fallback
        return Math.abs(url.hashCode()) + Features.EXTENSION;
    }
}
