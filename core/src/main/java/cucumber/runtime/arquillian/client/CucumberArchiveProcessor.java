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
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.impl.base.asset.AssetUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import static cucumber.runtime.arquillian.locator.JarLocation.jarLocation;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

public class CucumberArchiveProcessor implements ApplicationArchiveProcessor {
    @Inject
    private Instance<CucumberConfiguration> configuration;

    @Override
    public void process(final Archive<?> applicationArchive, final TestClass testClass) {
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

        final ClasspathResourceLoader classpathResourceLoader = new ClasspathResourceLoader(loader);
        final Collection<Class<? extends Annotation>> annotations = classpathResourceLoader.getAnnotations("cucumber.api");
        final StringBuilder builder = new StringBuilder();
        for (final Class<? extends Annotation> annotation : annotations) {
            builder.append(annotation.getName()).append(ln);
        }
        resourceJar.addAsResource(new StringAsset(builder.toString()), ClientServerFiles.ANNOTATION_LIST);

        final CucumberConfiguration cucumberConfiguration = configuration.get();
        final boolean report = cucumberConfiguration.isReport();
        final String reportDirectory = cucumberConfiguration.getReportDirectory();

        final StringBuilder config = new StringBuilder();
        config.append(CucumberConfiguration.COLORS).append("=").append(cucumberConfiguration.isColorized()).append("\n")
            .append(CucumberConfiguration.REPORTABLE).append("=").append(report).append("\n")
            .append(CucumberConfiguration.REPORTABLE_PATH).append("=").append(reportDirectory).append("\n");
        if (cucumberConfiguration.hasOptions()) {
            config.append(CucumberConfiguration.OPTIONS).append("=").append(cucumberConfiguration.getOptions());
        }

        resourceJar.addAsResource(new StringAsset(config.toString()), ClientServerFiles.CONFIG);

        libraryContainer.addAsLibrary(resourceJar);

        if (report) {
            CucumberReporter.addReport(CucumberConfiguration.reportFile(reportDirectory, javaClass));
        }

        // glues
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

        // cucumber-java and cucumber-core
        libraryContainer.addAsLibraries(
                jarLocation(Mapper.class),
                jarLocation(JavaBackend.class), jarLocation(Cucumber.class));

        // cucumber-arquillian
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
                .addClass(ClientServerFiles.class)
                .addClass(CucumberContainerExtension.class)
        );

    }

    private static String featureName(final URL url) {
        final File f = new File(url.getFile());
        if (f.exists()) {
            return f.getName();
        }
        return url.hashCode() + ".feature";
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
