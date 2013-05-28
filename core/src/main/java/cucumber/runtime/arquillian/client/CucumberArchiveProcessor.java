package cucumber.runtime.arquillian.client;

import cucumber.api.junit.Cucumber;
import cucumber.runtime.arquillian.ArquillianCucumber;
import cucumber.runtime.arquillian.backend.ArquillianBackend;
import cucumber.runtime.arquillian.config.CucumberConfiguration;
import cucumber.runtime.arquillian.container.CucumberContainerExtension;
import cucumber.runtime.arquillian.feature.Features;
import cucumber.runtime.arquillian.glue.Glues;
import cucumber.runtime.arquillian.lifecycle.CucumberLifecycle;
import cucumber.runtime.arquillian.reporter.CucumberReporter;
import cucumber.runtime.arquillian.shared.ClientServerFiles;
import cucumber.runtime.arquillian.stream.NotCloseablePrintStream;
import cucumber.runtime.io.ClasspathResourceLoader;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static cucumber.runtime.arquillian.locator.JarLocation.jarLocation;
import static cucumber.runtime.arquillian.shared.ClassLoaders.load;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

public class CucumberArchiveProcessor implements ApplicationArchiveProcessor {
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
        final Map<String, Collection<URL>> featureUrls = Features.createFeatureMap(configuration.get().getFeatureHome(), javaClass, loader);

        if (featureUrls.isEmpty()
                || !LibraryContainer.class.isInstance(applicationArchive)) {
            return;
        }

        final String ln = System.getProperty("line.separator");

        final LibraryContainer<?> libraryContainer = (LibraryContainer<?>) applicationArchive;

        // add feature file + list of annotations
        final JavaArchive resourceJar = create(JavaArchive.class, "cukespace-resources.jar");

        final CucumberConfiguration cucumberConfiguration = configuration.get();
        final boolean report = cucumberConfiguration.isReport();
        final String reportDirectory = cucumberConfiguration.getReportDirectory();

        addFeatures(featureUrls, ln, resourceJar);
        addCucumberAnnotations(loader, ln, resourceJar);
        addConfiguration(resourceJar, cucumberConfiguration, report, reportDirectory);

        libraryContainer.addAsLibrary(resourceJar);

        if (report) {
            CucumberReporter.addReport(CucumberConfiguration.reportFile(reportDirectory, javaClass));
        }

        // glues
        enrichWithGlues(javaClass, libraryContainer);

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

    private static void addCucumberAnnotations(final ClassLoader loader, final String ln, final JavaArchive resourceJar) {
        final ClasspathResourceLoader classpathResourceLoader = new ClasspathResourceLoader(loader);
        final Collection<Class<? extends Annotation>> annotations = classpathResourceLoader.getAnnotations("cucumber.api");
        final StringBuilder builder = new StringBuilder();
        for (final Class<? extends Annotation> annotation : annotations) {
            builder.append(annotation.getName()).append(ln);
        }
        resourceJar.addAsResource(new StringAsset(builder.toString()), ClientServerFiles.ANNOTATION_LIST);
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
                jarLocation(JavaBackend.class), jarLocation(Cucumber.class));
    }

    private static void enrichWithGlues(final Class<?> javaClass, final LibraryContainer<?> libraryContainer) {
        final Collection<Class<?>> glues = Glues.findGlues(javaClass);
        if (!glues.isEmpty()) {
            final JavaArchive gluesJar = create(JavaArchive.class, "cukespace-glues.jar");
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
            libraryContainer.addAsLibrary(gluesJar);
        }
    }

    private static void enrichWithCukeSpace(final LibraryContainer<?> libraryContainer) {
        libraryContainer.addAsLibrary(
            create(JavaArchive.class, "cukespace-core.jar")
                .addAsServiceProvider(RemoteLoadableExtension.class, CucumberContainerExtension.class)
                .addPackage(ArquillianBackend.class.getPackage())
                .addPackage(cucumber.runtime.arquillian.api.Glues.class.getPackage())
                .addClass(NotCloseablePrintStream.class)
                .addClass(CucumberReporter.class)
                .addClass(CucumberLifecycle.class)
                .addClass(Features.class)
                .addClass(Glues.class)
                .addClass(CucumberConfiguration.class)
                .addClass(ArquillianCucumber.class)
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

    private static byte[] slurp(final URL featureUrl) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            is = featureUrl.openStream();

            final byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
        } catch (IOException e) {
            // no-op
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // no-op
                }
            }
        }
        return baos.toByteArray();
    }
}
