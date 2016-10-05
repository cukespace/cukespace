package cucumber.runtime.arquillian.client;

import cucumber.api.junit.Cucumber;
import cucumber.deps.com.thoughtworks.xstream.converters.ConverterRegistry;
import cucumber.runtime.arquillian.ArquillianCucumber;
import cucumber.runtime.arquillian.CukeSpace;
import cucumber.runtime.arquillian.api.event.StepEvent;
import cucumber.runtime.arquillian.backend.ArquillianBackend;
import cucumber.runtime.arquillian.config.CucumberConfiguration;
import cucumber.runtime.arquillian.container.CucumberContainerExtension;
import cucumber.runtime.arquillian.feature.Features;
import cucumber.runtime.arquillian.glue.Glues;
import cucumber.runtime.arquillian.lifecycle.CucumberLifecycle;
import cucumber.runtime.arquillian.reporter.CucumberReporter;
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
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.impl.base.asset.AssetUtil;
import org.jboss.shrinkwrap.impl.base.filter.IncludeRegExpPaths;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static cucumber.runtime.arquillian.shared.IOs.slurp;
import static cucumber.runtime.arquillian.locator.JarLocation.jarLocation;
import static cucumber.runtime.arquillian.shared.ClassLoaders.load;
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

        if (featureUrls.isEmpty()
                || !LibraryContainer.class.isInstance(applicationArchive)) {
            final RunWith runWith = testClass.getAnnotation(RunWith.class);
            if (runWith == null || (!ArquillianCucumber.class.equals(runWith.value()) && !CukeSpace.class.equals(runWith.value()))) {
                // not a cucumber test so skip enrichment
                return;
            } else {
                // else let enrich it to avoid type not found error
                Logger.getLogger(CucumberArchiveProcessor.class.getName()).info("No feature found for " + javaClass.getName());
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

        // glues
        enrichWithGlues(javaClass, libraryContainer, ln);

        // cucumber-java and cucumber-core
        enrichWithDefaultCucumber(libraryContainer);

        // cucumber-arquillian
        enrichWithCukeSpace(libraryContainer);

        // if scala module is available at classpath
        final Set<ArchivePath> libs = applicationArchive.getContent(new IncludeRegExpPaths("/WEB-INF/lib/.*jar")).keySet();
        tryToAdd(libs, libraryContainer, "WEB-INF/lib/scala-library-", "cucumber.api.scala.ScalaDsl", "scala.App");
    }

    private static void addConfiguration(final JavaArchive resourceJar, final CucumberConfiguration cucumberConfiguration, final boolean report, final String reportDirectory) {
        final StringBuilder config = new StringBuilder();
        config.append(CucumberConfiguration.COLORS).append("=").append(cucumberConfiguration.isColorized()).append("\n")
            .append(CucumberConfiguration.REPORTABLE).append("=").append(report).append("\n")
            .append(CucumberConfiguration.REPORTABLE_PATH).append("=").append(reportDirectory).append("\n");
        if (cucumberConfiguration.hasOptions()) {
            config.append(CucumberConfiguration.OPTIONS).append("=").append(cucumberConfiguration.getOptions());
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
                jarLocation(JavaBackend.class),
                jarLocation(Cucumber.class));
        try {
            final File j8 = jarLocation(Thread.currentThread().getContextClassLoader().loadClass("cucumber.runtime.java8.LambdaGlueBase"));
            libraryContainer.addAsLibraries(j8);
        } catch (final Exception e) {
            // no-op
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

    private static void enrichWithCukeSpace(final LibraryContainer<?> libraryContainer) {
        libraryContainer.addAsLibrary(
            create(JavaArchive.class, "cukespace-core.jar")
                .addAsServiceProvider(RemoteLoadableExtension.class, CucumberContainerExtension.class)
                .addPackage(ArquillianBackend.class.getPackage())
                .addPackage(cucumber.runtime.arquillian.api.Glues.class.getPackage())
                .addPackage(StepEvent.class.getPackage())
                .addClass(NotCloseablePrintStream.class)
                .addClass(CucumberReporter.class)
                .addClass(CucumberLifecycle.class)
                .addClass(Features.class)
                .addClass(Glues.class)
                .addClass(CucumberConfiguration.class)
                .addClasses(ArquillianCucumber.class, CukeSpace.class)
                .addPackage(ClientServerFiles.class.getPackage())
                .addClass(CucumberContainerExtension.class)
                // don't add JarLocation here or update Features#isServer()
        );
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
