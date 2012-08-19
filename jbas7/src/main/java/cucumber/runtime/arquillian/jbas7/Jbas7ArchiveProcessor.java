package cucumber.runtime.arquillian.jbas7;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import cucumber.io.ResourceIteratorFactory;

/**
 * Creates archive of dependencies for running Cucumber tests within
 * JBoss AS 7.
 */
public class Jbas7ArchiveProcessor implements ApplicationArchiveProcessor {
    
    /**
     * Initializes a new instance of the Jbas7ArchiveProcessor class.
     */
    public Jbas7ArchiveProcessor() {
        // intentionally empty
    }
    
    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        ((LibraryContainer<?>) applicationArchive).addAsLibrary(
            create(JavaArchive.class)
                .addAsServiceProvider(ResourceIteratorFactory.class, Jbas7ResourceIteratorFactory.class)
                .addClass(Jbas7Resource.class)
                .addClass(Jbas7ResourceIterator.class)
                .addClass(Jbas7ResourceIteratorFactory.class)
        );
    }
}
