package info.loganmcgrath.cucumber.arquillian.junit;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;


/**
 * Cucumber/JUnit archive processor.
 */
public class CucumberJUnitArchiveProcessor implements ApplicationArchiveProcessor
{
    /**
     * Initializes a new instance of the CucumberJUnitArchiveProcessor class.
     */
    public CucumberJUnitArchiveProcessor()
    {
        // intentionally empty
        
    } // CucumberJUnitArchiveProcessor
    
    
    @Override
    public void process( Archive<?> applicationArchive, TestClass testClass )
    {
        ( ( LibraryContainer<?> ) applicationArchive ).addAsLibrary(
            create( JavaArchive.class ).addClass( Cucumber.class )
        );
        
    } // process
    
    
} // class CucumberJUnitArchiveProcessor
