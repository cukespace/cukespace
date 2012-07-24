package info.loganmcgrath.cucumber.arquillian.junit;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;


/**
 * Cucumber/JUnit client extension.
 */
public class CucumberJUnitClientExtension implements LoadableExtension
{
    /**
     * Initializes a new instance of the CucumberJUnitClientExtension class.
     */
    public CucumberJUnitClientExtension()
    {
        // intentionally empty
        
    } // CucumberJUnitClientExtension
    
    
    @Override
    public void register( ExtensionBuilder builder )
    {
        builder.service( ApplicationArchiveProcessor.class, CucumberJUnitArchiveProcessor.class );
        
    } // register
    
    
} // class CucumberJUnitClientExtension
