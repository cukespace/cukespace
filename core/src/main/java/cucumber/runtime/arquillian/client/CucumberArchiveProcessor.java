package cucumber.runtime.arquillian.client;

import cucumber.runtime.arquillian.TestEnricherProvider;
import cucumber.runtime.arquillian.container.CucumberContainerExtension;
import cucumber.runtime.arquillian.junit.Cucumber;
import cucumber.runtime.java.JavaBackend;
import gherkin.util.Mapper;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import static cucumber.runtime.arquillian.locator.JarLocation.jarLocation;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

public class CucumberArchiveProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(final Archive<?> applicationArchive, final TestClass testClass) {
        final LibraryContainer<?> libraryContainer = (LibraryContainer<?>) applicationArchive;

        // cucumber-java and cucumber-core
        libraryContainer.addAsLibraries(jarLocation(JavaBackend.class), jarLocation(Mapper.class));
        
        libraryContainer.addAsLibrary(
            create(JavaArchive.class)
                .addAsServiceProvider(RemoteLoadableExtension.class, CucumberContainerExtension.class)
                .addPackage(Cucumber.class.getPackage())
                .addClass(CucumberContainerExtension.class)
                .addClass(TestEnricherProvider.class)
        );
    }
}
