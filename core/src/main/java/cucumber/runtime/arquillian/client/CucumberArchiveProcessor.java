package cucumber.runtime.arquillian.client;

import cucumber.runtime.arquillian.ArquillianCucumber;
import cucumber.runtime.arquillian.backend.ArquillianBackend;
import cucumber.runtime.arquillian.container.CucumberContainerExtension;
import cucumber.runtime.arquillian.feature.Features;
import cucumber.runtime.arquillian.lifecycle.CucumberLifecycle;
import cucumber.runtime.arquillian.stream.NotCloseablePrintStream;
import cucumber.runtime.io.ClasspathResourceLoader;
import cucumber.runtime.java.JavaBackend;
import gherkin.util.Mapper;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.container.ResourceContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collection;

import static cucumber.runtime.arquillian.locator.JarLocation.jarLocation;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

public class CucumberArchiveProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(final Archive<?> applicationArchive, final TestClass testClass) {
        // try to find the feature
        final Class<?> javaClass = testClass.getJavaClass();
        final String featurePath = Features.featurePath(javaClass);
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final URL featureUrl = loader.getResource(featurePath);

        if (featureUrl == null
                || !LibraryContainer.class.isInstance(applicationArchive)
                || !ResourceContainer.class.isInstance(applicationArchive)) {
            return;
        }

        // add feature file + list of annotations
        final Asset featureAsset = new StringAsset(new String(slurp(featureUrl)));
        final ResourceContainer<?> resourceContainer = (ResourceContainer<?>) applicationArchive;
        resourceContainer.addAsResource(featureAsset, featurePath);

        final ClasspathResourceLoader classpathResourceLoader = new ClasspathResourceLoader(loader);
        final Collection<Class<? extends Annotation>> annotations = classpathResourceLoader.getAnnotations("cucumber.api");
        final StringBuilder builder = new StringBuilder();
        final String ln = System.getProperty("line.separator");
        for (final Class<? extends Annotation> annotation : annotations) {
            builder.append(annotation.getName()).append(ln);
        }
        resourceContainer.addAsResource(new StringAsset(builder.toString()), "cukespace-annotations.txt");

        // add libraries
        final LibraryContainer<?> libraryContainer = (LibraryContainer<?>) applicationArchive;

        // cucumber-java and cucumber-core
        libraryContainer.addAsLibraries(jarLocation(Mapper.class), jarLocation(JavaBackend.class));

        // cucumber-arquillian
        libraryContainer.addAsLibrary(
            create(JavaArchive.class, "cukespace-core.jar")
                .addAsServiceProvider(RemoteLoadableExtension.class, CucumberContainerExtension.class)
                .addPackage(ArquillianBackend.class.getPackage())
                .addClass(NotCloseablePrintStream.class)
                .addClass(CucumberLifecycle.class)
                .addClass(Features.class)
                .addClass(ArquillianCucumber.class)
                .addClass(CucumberContainerExtension.class)
        );

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
