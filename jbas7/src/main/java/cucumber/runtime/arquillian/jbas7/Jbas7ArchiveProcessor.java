package cucumber.runtime.arquillian.jbas7;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

import cucumber.runtime.io.ResourceIteratorFactory;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public class Jbas7ArchiveProcessor implements ApplicationArchiveProcessor {
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
